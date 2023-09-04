package com.danxiaocampus.callback.controller;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.json.JSONUtil;
import com.danxiaocampus.callback.constant.RedisConstant;
import com.danxiaocampus.callback.model.DO.CallbackInfo;
import com.danxiaocampus.callback.model.DO.ForwardInfo;
import com.danxiaocampus.callback.model.TraceServerInfo;
import com.danxiaocampus.callback.model.WxImageModerationAsyncResult;
import com.danxiaocampus.callback.service.CallbackInfoService;
import com.danxiaocampus.callback.service.ForwardInfoService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.concurrent.TimeUnit;

/**
 * @author adorabled4
 * @className WxModerateController
 * @date 2023/09/04
 */
@RestController
@Slf4j
public class WxModerateController {
    public static final String VERIFY_TOKEN = "eggcampusToken";

    @Resource
    private RestTemplate restTemplate;

    @Resource
    StringRedisTemplate stringRedisTemplate;

    @Resource
    CallbackInfoService callbackInfoService;

    @Resource
    ForwardInfoService forwardInfoService;


    /**
     * 接收traceId以及server信息
     *
     * @param serverInfo
     * @return {@link ResponseEntity}<{@link String}>
     */
    @PostMapping("/moderate/trace")
    public ResponseEntity<String> receiveTraceId(@RequestBody TraceServerInfo serverInfo) {
        if (serverInfo == null) {
            return ResponseEntity.badRequest().body("Param not available.");
        }
        long timeStamp = System.currentTimeMillis();
        // 1. 持久化到mysql
        String uri = serverInfo.getUri();
        String traceId = serverInfo.getTraceId();
        CallbackInfo callbackInfo = new CallbackInfo(serverInfo, timeStamp);
        boolean save = callbackInfoService.save(callbackInfo);
        if (!save) {
            log.error("持久化业务服务器信息失败, serverInfo: {}", serverInfo);
        }
        // 2. 缓存到redis
        String key = RedisConstant.CALLBACK_MODERATE_KEY + traceId;
        String value = uri + "-" + timeStamp;
        // 使用String来存储server信息
        stringRedisTemplate.opsForValue().set(key, value, RedisConstant.CALLBACK_MODERATE_TTL, TimeUnit.SECONDS);
        return ResponseEntity.ok("callback接收成功");
    }


    /**
     * 接收微信图像审核回调消息 : 通过traceId 来进行查询,对不同的value 转发到对应的server
     *
     * @param callbackMessage 回调消息
     * @return {@link ResponseEntity}<{@link String}>
     */
    @PostMapping("/review/image/callback")
    public ResponseEntity<String> receiveImageModerateResult(@RequestBody WxImageModerationAsyncResult callbackMessage, HttpServletRequest request) {
        // 获取转发的目标URL
        String targetUrl = getDestServer(callbackMessage);
        if (targetUrl.equals("")) {
            log.error("获取目的服务器路径失败, trace_id :{}", callbackMessage.getTraceId());
            return null;
        }
        String body = JSONUtil.toJsonStr(callbackMessage);
        if (StringUtils.isNotBlank(targetUrl)) {
            // 持久化转发信息
            ForwardInfo forwardInfo = new ForwardInfo(request);
            forwardInfo.setBody(body);
            forwardInfo.setServerUri(targetUrl);
            forwardInfoService.save(forwardInfo);
            // 进行转发
            // 1.获取到请求头
            Enumeration<String> headerNames = request.getHeaderNames();
            HttpHeaders headers = new HttpHeaders();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                String header = request.getHeader(headerName);
                headers.add(headerName, header);
            }
            // 2.构造HttpEntity，包括请求头和请求体
            HttpEntity<String> entity = new HttpEntity<>(body, headers);
            // 3.转发请求并获取响应
            ResponseEntity<String> responseEntity = restTemplate.exchange(targetUrl, HttpMethod.POST, entity, String.class);
            // 4.存储响应状态
            forwardInfo.setResponseResult(responseEntity.getStatusCode().toString());
            forwardInfoService.updateById(forwardInfo);
            log.info("审核回调消息转发成功, targetUrl:{}", targetUrl);
            return responseEntity;
        } else {
            return ResponseEntity.badRequest().body("Target URL not available.");
        }
    }

    /**
     * 验证wx消息推送
     *
     * @param request 请求
     * @return {@link String}
     */
    @GetMapping("/review/image/callback")
    public String verifyWxMessagePush(HttpServletRequest request) {
        Enumeration<String> parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String key = parameterNames.nextElement();
            log.info("key:{},value:{}", key, request.getParameter(key));
        }
        boolean checkResult = checkSignature(request);
        if (checkResult) {
            String echostr = request.getParameter("echostr");
            log.info("echostr:{}", echostr);
            return echostr;
        } else {
            return null;
        }
    }

    /**
     * 检查签名
     *
     * @param request 请求
     * @return boolean 检查结果
     */
    private boolean checkSignature(HttpServletRequest request) {
        try {
            String timestamp = request.getParameter("timestamp");
            String signature = request.getParameter("signature");
            String nonce = request.getParameter("nonce");
            String[] strs = {VERIFY_TOKEN, timestamp, nonce};
            Arrays.sort(strs);
            String joinedStr = StringUtils.join(strs);
            String s = SecureUtil.sha1(joinedStr);
            return s.equals(signature);
        } catch (RuntimeException e) {
            log.error("校验签名失败: {}", e.getLocalizedMessage());
            return false;
        }
    }

    /**
     * 获得目的回调地址
     *
     * @param callBackMessage 回电话消息
     * @return {@link String}
     */
    private String getDestServer(WxImageModerationAsyncResult callBackMessage) {
        String traceId = callBackMessage.getTraceId();
        String key = RedisConstant.CALLBACK_MODERATE_KEY + traceId;
        String server = stringRedisTemplate.opsForValue().get(key);
        if (StringUtils.isNotEmpty(server)) {
            return server;
        } else {
            // 查询mysql中是否保存有数据
            CallbackInfo callbackInfo = callbackInfoService.getInfoByTraceId(traceId);
            if (callbackInfo == null) {
                return "";
            } else {
                return callbackInfo.getServerUri();
            }
        }
    }
}

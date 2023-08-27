package com.danxiaocampus.callback.controller;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.json.JSONUtil;
import com.danxiaocampus.callback.constant.SystemConstant;
import com.danxiaocampus.callback.model.WxImageModerationAsyncResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import static com.danxiaocampus.callback.constant.SystemConstant.CALLBACK_MODERATE_KEY;

/**
 * @author adorabled4
 * @className ForwardController
 * @date : 2023/08/27/ 19:55
 **/
@RestController
@Slf4j
public class ForwardController {
    public static final String VERIFY_TOKEN = "eggcampusToken";


    @Resource
    private RestTemplate restTemplate;

    @Resource
    StringRedisTemplate stringRedisTemplate;



    /**
     * 接收微信图像审核回调消息 : 通过traceId 来进行查询,对不同的value 转发到对应的server
     *
     * @param callbackMessage 回调消息
     * @return {@link ResponseEntity}<{@link String}>
     */
    @PostMapping("/review/image/callback")
    public ResponseEntity<String> receiveImageModerateResult(@RequestBody WxImageModerationAsyncResult callbackMessage, HttpServletRequest request) {
        // 定义转发的目标URL
        String targetUrl = getDestServer(callbackMessage);
        if (StringUtils.isNotBlank(targetUrl)) {
            // 获取到请求头
            Enumeration<String> headerNames = request.getHeaderNames();
            HttpHeaders headers = new HttpHeaders();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                String header = request.getHeader(headerName);
                headers.add(headerName, header);
            }
            // 构造HttpEntity，包括请求头和请求体
            HttpEntity<String> entity = new HttpEntity<>(JSONUtil.toJsonStr(callbackMessage), headers);
            // 转发请求并获取响应
            ResponseEntity<String> responseEntity = restTemplate.exchange(targetUrl, HttpMethod.POST, entity, String.class);
            log.info("审核回调消息转发成功, targetUrl:{}",targetUrl);
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
        String key = SystemConstant.CALLBACK_MODERATE_KEY + traceId;
        String server = stringRedisTemplate.opsForValue().get(key);
        if (StringUtils.isNotEmpty(server)) {
            return server;
        } else {
            log.error("重定向回调消息失败: trace_id{}", traceId);
            return "";
        }
    }
}

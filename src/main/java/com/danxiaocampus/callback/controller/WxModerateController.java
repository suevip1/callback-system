package com.danxiaocampus.callback.controller;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.json.JSONUtil;
import com.danxiaocampus.callback.constant.RedisConstant;
import com.danxiaocampus.callback.model.DO.CallbackInfo;
import com.danxiaocampus.callback.model.TraceServerInfo;
import com.danxiaocampus.callback.model.WxImageModerationAsyncResult;
import com.danxiaocampus.callback.service.CallbackInfoService;
import com.danxiaocampus.callback.service.ForwardInfoService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
        String jsonStr = JSONUtil.toJsonStr(serverInfo);
        // 使用String来存储server信息 { k : timestamp} : {value : TraceServerInfo}
        stringRedisTemplate.opsForValue().set(key, jsonStr, RedisConstant.CALLBACK_MODERATE_TTL, TimeUnit.SECONDS);
        // 通过zset来快速获取超时的数据
        stringRedisTemplate.opsForZSet().add(RedisConstant.CALLBACK_MODERATE_ZSET_KEY, traceId, timeStamp);
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
        return forwardInfoService.forwardModerateResult(callbackMessage,request);
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

}

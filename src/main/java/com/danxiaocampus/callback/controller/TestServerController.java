package com.danxiaocampus.callback.controller;

import com.danxiaocampus.callback.Config.PropertiesConfig;
import com.danxiaocampus.callback.constant.RedisConstant;
import com.danxiaocampus.callback.model.WxImageModerationAsyncResult;
import com.danxiaocampus.callback.model.WxModerationResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Map;

/**
 * @author adorabled4
 * @className TestServerController
 * @date : 2023/08/27/ 20:29
 **/
@RequestMapping("/test")
@RestController
@Slf4j
public class TestServerController {

    @Resource
    StringRedisTemplate stringRedisTemplate;

    @Resource
    PropertiesConfig properties;


    @GetMapping("/send/moderate")
    public String simulateSendModerate(){
        /*
        ....执行图像审核逻辑
        */
        // 保存server信息到redis
        String traceId = properties.getTraceId();
        String key = RedisConstant.CALLBACK_MODERATE_KEY+traceId;
        String serverUrl = properties.getTestServerUrl();
        stringRedisTemplate.opsForValue().set(key,serverUrl);
        String s = stringRedisTemplate.opsForValue().get(key);
        if(StringUtils.isNotBlank(s)){
            log.info("test-server执行图像审核: trace_id:{}",traceId);
        }
        return null;
    }

    /**
     * 接收微信图像审核回调消息
     *
     * @param response 响应
     * @return {@link Map}
     */
    @PostMapping("/review/image/callback")
    public Map receiveImageModerateResult(@RequestBody WxImageModerationAsyncResult response) {
        WxModerationResult result = response.getResult();
        String traceId = response.getTraceId();
        log.info("test-server接收到了回调消息: {}", response);
        return null;
    }

}

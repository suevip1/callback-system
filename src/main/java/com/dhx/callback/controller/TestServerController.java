package com.dhx.callback.controller;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.dhx.callback.Config.PropertiesConfig;
import com.dhx.callback.model.TraceServerInfo;
import com.dhx.callback.model.WxImageModerationAsyncResult;
import com.dhx.callback.model.WxModerationResult;
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
    public String simulateSendModerate(@RequestParam("trace_id") String traceId) {
        /*
        ....执行图像审核逻辑
        */
        if (StringUtils.isBlank(traceId)) {
            traceId = properties.getTraceId();
        }
        log.info("test-server执行图像审核: trace_id:{}", traceId);
        // 发送信息到callback-server
        TraceServerInfo serverInfo = new TraceServerInfo();
        String serverUrl = properties.getTestServerUrl();
        serverInfo.setUri(serverUrl);
        serverInfo.setTraceId(traceId);
        String callBack = "http://localhost:9000/api/moderate/trace";
        String post = HttpUtil.post(callBack, JSONUtil.toJsonStr(serverInfo));
        log.info("回调服务器响应结果:{}", post);
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

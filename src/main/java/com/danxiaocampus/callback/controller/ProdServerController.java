package com.danxiaocampus.callback.controller;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.danxiaocampus.callback.Config.PropertiesConfig;
import com.danxiaocampus.callback.model.TraceServerInfo;
import com.danxiaocampus.callback.model.WxImageModerationAsyncResult;
import com.danxiaocampus.callback.model.WxModerationResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.Map;

/**
 * @author adorabled4
 * @className TestServerController
 * @date : 2023/08/27/ 20:29
 **/
@RequestMapping("/prod")
@RestController
@Slf4j
public class ProdServerController {

    @Resource
    PropertiesConfig properties;

    @Resource
    RestTemplate restTemplate;

    @GetMapping("/send/moderate")
    public String simulateSendModerate(@RequestParam("trace_id") String traceId) {
        // 1. 请求wx服务获取trace_id
        // 2. 保存taceId
        // 3. 发送traceId以及回调url给callback服务器
        /*
        ....执行图像审核逻辑 : 模拟图像审核逻辑 , 获取trace_id
        */
        if (StringUtils.isBlank(traceId)) {
            traceId = properties.getTraceId();
        }
        log.info("prod-server执行图像审核: trace_id:{}", traceId);
        // 发送信息到callback-server
        TraceServerInfo serverInfo = new TraceServerInfo();
        String serverUrl = properties.getProdServerUrl();
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
        log.info("prod-server接收到了回调消息: {}", response);
        return null;
    }

}

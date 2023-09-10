package com.dhx.callback.Config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author adorabled4
 * @className Properties
 * @date : 2023/08/27/ 21:12
 **/
@Component
public class PropertiesConfig {

    /**
     * 测试服务器url
     */
    @Value("${callback.test.url}")
    String testServerUrl;

    /**
     * 正式服务器url
     */
    @Value("${callback.prod.url}")
    String prodServerUrl;

    /**
     * traceId
     */
    @Value("${callback.traceId}")
    String traceId;


    public String getTestServerUrl() {
        return testServerUrl;
    }

    public String getProdServerUrl() {
        return prodServerUrl;
    }

    public String getTraceId() {
        return traceId;
    }
}

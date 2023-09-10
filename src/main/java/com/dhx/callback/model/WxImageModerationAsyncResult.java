package com.dhx.callback.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * @author adorabled4
 * @className WxImageModerationAsyncResult
 * @date : 2023/08/27/ 19:59
 **/
@Data
public class WxImageModerationAsyncResult {


    /**
     * 小程序的username
     */
    @JsonProperty(value = "ToUserName")
    private String toUserName;

    /**
     * 平台推送服务UserName
     */
    @JsonProperty(value = "FromUserName")
    private String fromUserName;

    /**
     * 发送时间
     */
    @JsonProperty(value = "CreateTime")
    private long createTime;

    /**
     * 默认为：event
     */
    @JsonProperty(value = "MsgType")
    private String msgType;

    /**
     * 默认为：wxa_media_check
     */
    @JsonProperty(value = "Event")
    private String event;

    /**
     * 小程序的appid
     */
    private String appid;

    /**
     * 审核的trace_id
     */
    @JsonProperty(value = "trace_id")
    private String traceId;

    /**
     * 可用于区分接口版本
     */
    private int version;

    /**
     * 综合结果
     */
    private WxModerationResult result;

    /**
     * 详细检测结果
     */
    private List<WxModerationDetail> detail;
}

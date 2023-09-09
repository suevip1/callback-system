package com.danxiaocampus.callback.model.DO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;

import com.danxiaocampus.callback.model.TraceServerInfo;
import lombok.Data;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * @TableName t_callback_info
 */
@TableName(value = "t_callback_info")
@Data
public class CallbackInfo implements Serializable {
    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * trace_id
     */
    private String traceId;

    /**
     * 目的服务器接口地址
     */
    private String serverUri;

    /**
     * 小程序ID(callback供多项目使用)
     */
    private String appId;

    /**
     * 是否接收到回调消息
     */
    private Integer isReceived;

    /**
     * 错误信息
     */
    private String errMsg;

    /**
     * 创建时间
     */
    private Date createTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    public CallbackInfo(TraceServerInfo serverInfo, long timeStamp) {
        String uri = serverInfo.getUri();
        String traceId = serverInfo.getTraceId();
        this.serverUri = uri;
        this.traceId = traceId;
        this.createTime = new Date(timeStamp);
        this.isReceived = 0;
    }

    public CallbackInfo() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        CallbackInfo that = (CallbackInfo) o;

        return new EqualsBuilder().append(id, that.id).append(traceId, that.traceId).append(serverUri, that.serverUri).append(appId, that.appId).append(isReceived, that.isReceived).append(errMsg, that.errMsg).append(createTime, that.createTime).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(id).append(traceId).append(serverUri).append(appId).append(isReceived).append(errMsg).append(createTime).toHashCode();
    }

    @Override
    public String toString() {
        return "CallbackInfo{" +
                "id=" + id +
                ", traceId='" + traceId + '\'' +
                ", serverUri='" + serverUri + '\'' +
                ", appId='" + appId + '\'' +
                ", isReceived=" + isReceived +
                ", errMsg='" + errMsg + '\'' +
                ", createTime=" + createTime +
                '}';
    }
}
package com.danxiaocampus.callback.model.DO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;

import com.danxiaocampus.callback.model.TraceServerInfo;
import lombok.Data;

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
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null) {
            return false;
        }
        if (getClass() != that.getClass()) {
            return false;
        }
        CallbackInfo other = (CallbackInfo) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
                && (this.getTraceId() == null ? other.getTraceId() == null : this.getTraceId().equals(other.getTraceId()))
                && (this.getServerUri() == null ? other.getServerUri() == null : this.getServerUri().equals(other.getServerUri()))
                && (this.getIsReceived() == null ? other.getIsReceived() == null : this.getIsReceived().equals(other.getIsReceived()))
                && (this.getErrMsg() == null ? other.getErrMsg() == null : this.getErrMsg().equals(other.getErrMsg()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getTraceId() == null) ? 0 : getTraceId().hashCode());
        result = prime * result + ((getServerUri() == null) ? 0 : getServerUri().hashCode());
        result = prime * result + ((getIsReceived() == null) ? 0 : getIsReceived().hashCode());
        result = prime * result + ((getErrMsg() == null) ? 0 : getErrMsg().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", traceId=").append(traceId);
        sb.append(", serverUri=").append(serverUri);
        sb.append(", isReceived=").append(isReceived);
        sb.append(", errMsg=").append(errMsg);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}
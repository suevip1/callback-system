package com.dhx.callback.model.DO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

import lombok.Data;

import javax.servlet.http.HttpServletRequest;

/**
 * @TableName t_forward_info
 */
@TableName(value = "t_forward_info")
@Data
public class ForwardInfo implements Serializable {
    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 目的服务器路径
     */
    private String serverUri;

    /**
     * 请求参数
     */
    private String param;

    /**
     * 请求体
     */
    private String body;

    /**
     * 发送时间
     */
    private Date createTime;

    /**
     * 目的服务器响应结果
     */
    private String responseResult;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    public ForwardInfo(HttpServletRequest request) {
        // 获取请求参数并转化为字符串
        StringBuilder paramBuilder = new StringBuilder();
        Map<String, String[]> params = request.getParameterMap();
        for (Map.Entry<String, String[]> entry : params.entrySet()) {
            String paramName = entry.getKey();
            String[] paramValues = entry.getValue();
            for (String paramValue : paramValues) {
                paramBuilder.append(paramName).append("=").append(paramValue).append("&");
            }
        }
        // 去掉末尾的"&"符号
        if (paramBuilder.length() > 0) {
            paramBuilder.deleteCharAt(paramBuilder.length() - 1);
        }
        this.param = paramBuilder.toString();
        this.createTime = new Date();
    }

    public ForwardInfo() {

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
        ForwardInfo other = (ForwardInfo) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
                && (this.getServerUri() == null ? other.getServerUri() == null : this.getServerUri().equals(other.getServerUri()))
                && (this.getParam() == null ? other.getParam() == null : this.getParam().equals(other.getParam()))
                && (this.getBody() == null ? other.getBody() == null : this.getBody().equals(other.getBody()))
                && (this.getCreateTime() == null ? other.getCreateTime() == null : this.getCreateTime().equals(other.getCreateTime()))
                && (this.getResponseResult() == null ? other.getResponseResult() == null : this.getResponseResult().equals(other.getResponseResult()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getServerUri() == null) ? 0 : getServerUri().hashCode());
        result = prime * result + ((getParam() == null) ? 0 : getParam().hashCode());
        result = prime * result + ((getBody() == null) ? 0 : getBody().hashCode());
        result = prime * result + ((getCreateTime() == null) ? 0 : getCreateTime().hashCode());
        result = prime * result + ((getResponseResult() == null) ? 0 : getResponseResult().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", serverUri=").append(serverUri);
        sb.append(", param=").append(param);
        sb.append(", body=").append(body);
        sb.append(", createTime=").append(createTime);
        sb.append(", responseResult=").append(responseResult);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}
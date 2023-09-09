package com.danxiaocampus.callback.model.enums;

/**
 * @author adorabled4
 * @enum CallbackError
 * @date : 2023/09/04/ 21:38
 **/
public enum CallbackError {
    WX_TIMEOUT_ERROR("微信为按时返回回调消息"),
    CALLBACK_ERROR("callback内部异常"),

    BIZ_SERVER_ERROR("业务服务器响应异常 ");

    /**
     * value
     */
    private String value;

    public String getValue() {
        return value;
    }

    CallbackError(String value) {
        this.value = value;
    }
}

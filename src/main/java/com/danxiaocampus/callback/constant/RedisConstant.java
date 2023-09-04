package com.danxiaocampus.callback.constant;

/**
 * @author adorabled4
 * @className RedisConstant
 * @date : 2023/08/27/ 19:56
 **/
public class RedisConstant {

    /**
     * 审核回调消息prefix
     */
    public static final String CALLBACK_MODERATE_KEY = "callback:moderate";

    /**
     * 1hour
     */
    public static final long CALLBACK_MODERATE_TTL = 60 * 60;
}

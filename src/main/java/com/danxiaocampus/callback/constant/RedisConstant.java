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

    /**
     * traceId集合
     */
    public static final String CALLBACK_MODERATE_ZSET_KEY = "callback:moderate:traceId";

    /**
     * 微信内容审核时长(不超过30min)
     */
    public static final long WX_MODERATE_TTL = 60 * 30;
}

package com.danxiaocampus.callback.job;

import cn.hutool.json.JSONUtil;
import com.danxiaocampus.callback.constant.RedisConstant;
import com.danxiaocampus.callback.model.DO.CallbackInfo;
import com.danxiaocampus.callback.model.TraceServerInfo;
import com.danxiaocampus.callback.model.WxImageModerationAsyncResult;
import com.danxiaocampus.callback.model.enums.CallbackError;
import com.danxiaocampus.callback.service.CallbackInfoService;
import com.danxiaocampus.callback.service.ForwardInfoService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

import java.util.Set;

import static com.danxiaocampus.callback.constant.RedisConstant.CALLBACK_MODERATE_ZSET_KEY;
import static com.danxiaocampus.callback.constant.RedisConstant.WX_MODERATE_TTL;

/**
 * run per 10 minutes
 *
 * @author adorabled4
 * @className ModerateTimeOutJob
 * @date : 2023/09/04/ 21:34
 **/
@Component
@Slf4j
public class ModerateTimeOutJob {

    @Resource
    StringRedisTemplate stringRedisTemplate;

    @Resource
    CallbackInfoService callbackInfoService;

    @Resource
    ForwardInfoService forwardInfoService;

    @Scheduled(cron = "0 */10 * * * ?")
    public void moderateTimeOut() {
        log.info("[start]开始执行检测审核超时定时任务");
        long timeStamp = System.currentTimeMillis();
        Set<String> traceIds = stringRedisTemplate.opsForZSet().rangeByScore(
                CALLBACK_MODERATE_ZSET_KEY, timeStamp - WX_MODERATE_TTL, timeStamp);
        if (traceIds == null) {
            log.info("[end]检测审核超时定时任务执行结束");
            return;
        }
        if (traceIds.size() != 0) {
            traceIds.forEach(traceId -> {
                log.info("微信为按时返回审核结果, traceId:{}", traceId);
                CallbackInfo callbackInfo = new CallbackInfo();
                callbackInfo.setTraceId(traceId);
                callbackInfo.setErrMsg(CallbackError.WX_TIMEOUT_ERROR.getValue());
                boolean update = callbackInfoService.update().eq("trace_id", traceId).set("err_msg", callbackInfo.getErrMsg()).update();
                if (!update) {
                    log.error("更新回调消息状态失败, traceId:{}", traceId);
                }
                // 删除redis中存储的trace_id相关信息
                String key = RedisConstant.CALLBACK_MODERATE_KEY + traceId;
                // remove from string
                stringRedisTemplate.delete(key);
                // remove from ZSET
                stringRedisTemplate.opsForZSet().remove(RedisConstant.CALLBACK_MODERATE_ZSET_KEY, traceId);
                // 通知业务服务器
                informTargetServer(traceId);
            });
        }
        log.info("[end]检测审核超时定时任务执行结束, 本次超时的审核任务个数:{}", traceIds.size());
    }

    /**
     * 通过业务服务器失败的审核结果
     * @param traceId
     */
    private void informTargetServer(String traceId) {
        String key = RedisConstant.CALLBACK_MODERATE_KEY + traceId;
        String serverInfoJson = stringRedisTemplate.opsForValue().get(key);
        if (StringUtils.isNotBlank(serverInfoJson)) {
            TraceServerInfo traceServerInfo = JSONUtil.toBean(serverInfoJson, TraceServerInfo.class);
            String uri = traceServerInfo.getUri();
            forwardInfoService.sendErrorModerateResult(new WxImageModerationAsyncResult(), uri);
            log.info("向服务器发送审核失败消息,targetUri:{}",uri);
        }
    }


}

package com.danxiaocampus.callback.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.danxiaocampus.callback.constant.RedisConstant;
import com.danxiaocampus.callback.model.DO.CallbackInfo;
import com.danxiaocampus.callback.model.DO.ForwardInfo;
import com.danxiaocampus.callback.model.TraceServerInfo;
import com.danxiaocampus.callback.model.WxImageModerationAsyncResult;
import com.danxiaocampus.callback.model.enums.CallbackError;
import com.danxiaocampus.callback.service.CallbackInfoService;
import com.danxiaocampus.callback.service.ForwardInfoService;
import com.danxiaocampus.callback.mapper.ForwardInfoMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;

/**
 * @author dhx
 * @description 针对表【t_forward_info】的数据库操作Service实现
 * @createDate 2023-09-04 21:09:06
 */
@Service
@Slf4j
public class ForwardInfoServiceImpl extends ServiceImpl<ForwardInfoMapper, ForwardInfo>
        implements ForwardInfoService {

    @Resource
    StringRedisTemplate stringRedisTemplate;

    @Resource
    RestTemplate restTemplate;

    @Resource
    CallbackInfoService callbackInfoService;

    @Override
    public ResponseEntity<String> forwardModerateResult(WxImageModerationAsyncResult callbackMessage, HttpServletRequest request) {
        // 获取转发的目标URL
        String targetUrl = getDestServer(callbackMessage);
        if (targetUrl.equals("")) {
            log.error("获取目的服务器路径失败, trace_id :{}", callbackMessage.getTraceId());
            return null;
        }
        try {
            String body = JSONUtil.toJsonStr(callbackMessage);
            if (StringUtils.isNotBlank(targetUrl)) {
                // 持久化转发信息
                ForwardInfo forwardInfo = new ForwardInfo(request);
                forwardInfo.setBody(body);
                forwardInfo.setServerUri(targetUrl);
                save(forwardInfo);
                // 进行转发
                // 1.获取到请求头
                Enumeration<String> headerNames = request.getHeaderNames();
                HttpHeaders headers = new HttpHeaders();
                while (headerNames.hasMoreElements()) {
                    String headerName = headerNames.nextElement();
                    String header = request.getHeader(headerName);
                    headers.add(headerName, header);
                }
                // 2.构造HttpEntity，包括请求头和请求体
                HttpEntity<String> entity = new HttpEntity<>(body, headers);
                // 3.转发请求并获取响应
                ResponseEntity<String> responseEntity = restTemplate.exchange(targetUrl, HttpMethod.POST, entity, String.class);
                // 4.存储响应状态
                forwardInfo.setResponseResult(responseEntity.getStatusCode().toString());
                updateById(forwardInfo);
                if (responseEntity.getStatusCode().is2xxSuccessful()) {
                    String traceId = callbackMessage.getTraceId();
                    String appid = callbackMessage.getAppid();
                    boolean update = callbackInfoService.update().set("is_received", 1).set("app_id", appid).eq("trace_id", traceId).update();
                    if (!update) {
                        log.info("更新回调信息失败, traceId:{}", traceId);
                    }
                } else {
                    String traceId = callbackMessage.getTraceId();
                    String appid = callbackMessage.getAppid();
                    String reasonPhrase = responseEntity.getStatusCode().getReasonPhrase();
                    boolean update = callbackInfoService.update()
                            .set("is_received", 1)
                            .set("app_id", appid)
                            .set("err_msg", CallbackError.BIZ_SERVER_ERROR + "," + reasonPhrase)
                            .eq("trace_id", traceId).update();
                    if (!update) {
                        log.info("更新回调信息失败, traceId:{}", traceId);
                    }
                }
                log.info("审核回调消息转发成功, targetUrl:{}", targetUrl);
                // 5. 删除redis中存储的trace_id相关信息
                String traceId = callbackMessage.getTraceId();
                String key = RedisConstant.CALLBACK_MODERATE_KEY + traceId;
                // remove from string
                stringRedisTemplate.delete(key);
                // remove from ZSET
                stringRedisTemplate.opsForZSet().remove(RedisConstant.CALLBACK_MODERATE_ZSET_KEY, traceId);
                return responseEntity;
            } else {
                return ResponseEntity.badRequest().body("Target URL not available.");
            }
        } catch (RuntimeException e) {
            String traceId = callbackMessage.getTraceId();
            String appid = callbackMessage.getAppid();
            log.error("回调系统内部异常,traceId:{},异常信息:{}",traceId,e.getMessage());
            boolean update = callbackInfoService.update()
                    .set("app_id", appid)
                    .set("err_msg", CallbackError.CALLBACK_ERROR + "," + e.getMessage())
                    .eq("trace_id", traceId).update();
            if (!update) {
                log.info("更新回调信息失败, traceId:{}", traceId);
            }
        }
        return null;
    }


    @Override
    public ResponseEntity<String> sendErrorModerateResult(WxImageModerationAsyncResult wxImageModerationAsyncResult, String uri) {
        // 持久化转发信息
        ForwardInfo forwardInfo = new ForwardInfo();
        forwardInfo.setServerUri(uri);
        save(forwardInfo);
        // 进行发送
        // 1.获取到请求头
        HttpHeaders headers = new HttpHeaders();
        // 2.构造HttpEntity，包括请求头和请求体
        HttpEntity<String> entity = new HttpEntity<>(headers);
        // 3.转发请求并获取响应
        ResponseEntity<String> responseEntity = restTemplate.exchange(uri, HttpMethod.POST, entity, String.class);
        // 4.存储响应状态
        forwardInfo.setResponseResult(responseEntity.getStatusCode().toString());
        updateById(forwardInfo);
        log.info("失败审核回调消息发送成功, targetUrl:{}", uri);
        return responseEntity;
    }

    /**
     * 获得目的回调地址
     *
     * @param callBackMessage 回电话消息
     * @return {@link String}
     */
    private String getDestServer(WxImageModerationAsyncResult callBackMessage) {
        String traceId = callBackMessage.getTraceId();
        String key = RedisConstant.CALLBACK_MODERATE_KEY + traceId;
        String server = stringRedisTemplate.opsForValue().get(key);
        if (StringUtils.isNotEmpty(server)) {
            TraceServerInfo serverInfo = JSONUtil.toBean(server, TraceServerInfo.class);
            return serverInfo.getUri();
        } else {
            // 查询mysql中是否保存有数据
            CallbackInfo callbackInfo = callbackInfoService.getInfoByTraceId(traceId);
            if (callbackInfo == null) {
                return "";
            } else {
                return callbackInfo.getServerUri();
            }
        }
    }
}





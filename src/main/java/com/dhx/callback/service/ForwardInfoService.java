package com.dhx.callback.service;

import com.dhx.callback.model.DO.ForwardInfo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.dhx.callback.model.WxImageModerationAsyncResult;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletRequest;

/**
* @author dhx
* @description 针对表【t_forward_info】的数据库操作Service
* @createDate 2023-09-04 21:09:06
*/
public interface ForwardInfoService extends IService<ForwardInfo> {

    ResponseEntity<String> forwardModerateResult(WxImageModerationAsyncResult callbackMessage, HttpServletRequest request);

    ResponseEntity<String> sendErrorModerateResult(WxImageModerationAsyncResult wxImageModerationAsyncResult, String uri);
}

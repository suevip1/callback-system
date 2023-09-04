package com.danxiaocampus.callback.service;

import com.danxiaocampus.callback.model.DO.CallbackInfo;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author dhx
* @description 针对表【t_callback_info】的数据库操作Service
* @createDate 2023-09-04 20:46:33
*/
public interface CallbackInfoService extends IService<CallbackInfo> {

    CallbackInfo getInfoByTraceId(String traceId);
}

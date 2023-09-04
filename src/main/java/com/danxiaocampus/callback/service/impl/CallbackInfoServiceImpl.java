package com.danxiaocampus.callback.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.danxiaocampus.callback.model.DO.CallbackInfo;
import com.danxiaocampus.callback.service.CallbackInfoService;
import com.danxiaocampus.callback.mapper.CallbackInfoMapper;
import org.springframework.stereotype.Service;

/**
* @author dhx
* @description 针对表【t_callback_info】的数据库操作Service实现
* @createDate 2023-09-04 20:46:33
*/
@Service
public class CallbackInfoServiceImpl extends ServiceImpl<CallbackInfoMapper, CallbackInfo>
    implements CallbackInfoService {


    @Override
    public CallbackInfo getInfoByTraceId(String traceId) {
        QueryWrapper<CallbackInfo> wrapper = new QueryWrapper<CallbackInfo>().eq("trace_id", traceId);
        return getBaseMapper().selectOne(wrapper);
    }
}





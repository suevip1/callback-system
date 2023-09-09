package com.danxiaocampus.callback.model;

import lombok.Data;

/**
 * @author adorabled4
 * @className WxModerationResult
 * @date : 2023/08/27/ 19:59
 **/
@Data
public class WxModerationResult {

    private WxModerationSuggestionEnum suggest;

    private Integer label;
}

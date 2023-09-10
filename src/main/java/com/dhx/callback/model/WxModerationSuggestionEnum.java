package com.dhx.callback.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author adorabled4
 * @className WxModerationSuggestion
 * @date : 2023/08/27/ 20:00
 **/
public enum WxModerationSuggestionEnum {
    @JsonProperty(value = "risky")
    RISKY,
    @JsonProperty(value = "pass")
    PASS,
    @JsonProperty(value = "review")
    REVIEW,
}

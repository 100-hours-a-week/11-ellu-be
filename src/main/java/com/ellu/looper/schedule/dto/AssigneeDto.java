package com.ellu.looper.schedule.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AssigneeDto(
    String nickname, @JsonProperty("profile_image_url") String profileImageUrl) {}

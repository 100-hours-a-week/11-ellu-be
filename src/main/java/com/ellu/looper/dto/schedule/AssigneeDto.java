package com.ellu.looper.dto.schedule;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AssigneeDto(
      String nickname,
      @JsonProperty("profile_image_url") String profileImageUrl
  ) {}

package com.ellu.looper.notification.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class InvitationProcessRequest {

  @NotBlank
  @JsonProperty("invite_status")
  private String inviteStatus;
}

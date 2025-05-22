package com.ellu.looper.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class ProjectUpdateRequest {

  private String title;

  private String color;

  private String position; // 생성자 본인의 position

  private List<AddedMember> added_members;

  private String wiki;

  @Getter
  @Builder(toBuilder = true)
  public static class AddedMember {
    @NotBlank(message = "Nickname must not be empty")
    private String nickname;

    @NotBlank(message = "Position must not be empty")
    private String position;
  }
}

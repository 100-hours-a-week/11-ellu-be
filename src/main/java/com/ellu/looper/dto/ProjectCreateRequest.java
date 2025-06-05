package com.ellu.looper.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.Getter;

@Getter
public class ProjectCreateRequest {

  @NotBlank(message = "Title must not be empty")
  private String title;

  private String color; // "#fec178" 같은 Hex Color String

  private String position; // 생성자 본인의 position

  private List<AddedMember> added_members;

  private String wiki;

  @Getter
  public static class AddedMember {
    @NotBlank(message = "Nickname must not be empty")
    private String nickname;

    @NotBlank(message = "Position must not be empty")
    private String position;
  }
}

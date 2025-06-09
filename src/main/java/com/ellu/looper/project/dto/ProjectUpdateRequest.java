package com.ellu.looper.project.dto;

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

}

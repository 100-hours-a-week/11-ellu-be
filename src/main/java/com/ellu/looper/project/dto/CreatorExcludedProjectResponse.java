package com.ellu.looper.project.dto;

import com.ellu.looper.user.dto.MemberDto;
import java.util.List;

public record CreatorExcludedProjectResponse(
    Long id,
    String title,
    String color,
    String position,
    List<MemberDto> added_members,
    String wiki) {}

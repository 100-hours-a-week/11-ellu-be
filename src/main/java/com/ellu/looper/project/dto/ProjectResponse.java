package com.ellu.looper.project.dto;

import com.ellu.looper.user.dto.MemberDto;
import java.util.List;

public record ProjectResponse(Long id, String title, String color, List<MemberDto> members, String wiki) {}

package com.ellu.looper.dto;

import java.util.List;

public record CreatorExcludedProjectResponse(Long id, String title, String color, String position, List<MemberDto> added_members, String wiki) {}

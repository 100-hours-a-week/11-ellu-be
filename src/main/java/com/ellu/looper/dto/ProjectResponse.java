package com.ellu.looper.dto;

import java.util.List;

public record ProjectResponse(Long id, String title, String color, List<MemberDto> members, String wiki) {}

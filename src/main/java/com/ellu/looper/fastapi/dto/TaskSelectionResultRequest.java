package com.ellu.looper.fastapi.dto;

import java.util.List;

public record TaskSelectionResultRequest(Long project_id, List<SchedulePreview> content) {}

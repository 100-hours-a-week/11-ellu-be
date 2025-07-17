package com.ellu.looper.schedule.dto;

import com.ellu.looper.fastapi.dto.MeetingNoteResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder(toBuilder = true)
public class PreviewMessage implements java.io.Serializable {
  private Long projectId;
  private MeetingNoteResponse response;
}

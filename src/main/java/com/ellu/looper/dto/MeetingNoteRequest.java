package com.ellu.looper.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeetingNoteRequest {
  private String meeting_note;
  private Long author_id;
  private String nickname;
  private String position;
}

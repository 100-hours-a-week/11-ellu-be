<<<<<<<< HEAD:src/main/java/com/ellu/looper/fastapi/dto/MeetingNoteRequest.java
package com.ellu.looper.fastapi.dto;
========
package com.ellu.looper.project.dto;
>>>>>>>> main:src/main/java/com/ellu/looper/project/dto/MeetingNoteRequest.java

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeetingNoteRequest {
  private Long project_id;
  private String content;
  private String nickname;
  private List<String> position;
}

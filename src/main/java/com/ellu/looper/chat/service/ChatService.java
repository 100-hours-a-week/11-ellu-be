package com.ellu.looper.chat.service;

import com.ellu.looper.chat.dto.ChatMessageResponse;
import com.ellu.looper.chat.dto.MessageRequest;
import com.ellu.looper.chat.dto.RelatedScheduleRequest;
import com.ellu.looper.chat.dto.RelatedScheduleResponse;
import com.ellu.looper.chat.entity.ChatConversation;
import com.ellu.looper.chat.entity.ChatMessage;
import com.ellu.looper.chat.repository.ChatConversationRepository;
import com.ellu.looper.chat.repository.ChatMessageRepository;
import com.ellu.looper.kafka.ChatProducer;
import com.ellu.looper.schedule.entity.Plan;
import com.ellu.looper.schedule.entity.Schedule;
import com.ellu.looper.schedule.repository.PlanRepository;
import com.ellu.looper.schedule.repository.ScheduleRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

  private final ChatProducer chatProducer;
  private final ChatMessageRepository chatMessageRepository;
  private final ChatConversationRepository conversationRepository;
  private final ScheduleRepository scheduleRepository;
  private final PlanRepository planRepository;

  public void sendChatMessage(MessageRequest request, Long userId) {
    chatProducer.sendUserMessage(userId, request);
  }

  // 지난 24시간의 대화 기록 조회
  public List<ChatMessageResponse> getRecentHistory(Long userId) {
    log.info("UserId {} fetched chat history. ", userId);
    LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
    ChatConversation recentConversation =
        conversationRepository.findTop1ByUserIdAndUpdatedAtGreaterThanEqualOrderByCreatedAtDesc(
            userId, cutoff);

    if (recentConversation == null) {
      return Collections.emptyList();
    }

    List<ChatMessage> messages =
        chatMessageRepository.findConversationMessages(recentConversation.getId());
    return messages.stream()
        .map(msg -> new ChatMessageResponse(msg.getMessageType().name(), msg.getContent()))
        .collect(Collectors.toList());
  }

  public List<RelatedScheduleResponse> getRelatedSchedules(RelatedScheduleRequest request) {
    Long userId = request.user_id();
    LocalDateTime start = request.start();
    LocalDateTime end = request.end();

    String keyword = request.task_title_keyword();
    String category = request.category();

    List<RelatedScheduleResponse> response = new ArrayList<>();

    // title, description에 keyword가 들어간 스케줄을 모두 반환
    if (keyword != null) {
      List<Schedule> keywordSchedules =
          scheduleRepository.findRelatedSchedules(userId, start, end, keyword);
      for (Schedule schedule : keywordSchedules) {
        response.add(
            new RelatedScheduleResponse(
                schedule.getTitle(),
                schedule.getDescription(),
                schedule.getStartTime(),
                schedule.getEndTime()));
      }
    }
    if (category != null) {
      // plan중에 category가 같고 created_at가 start이후인 plan 조회
      List<Plan> relatedPlans =
          planRepository.findByUserIdAndCategoryAndCreatedAtAfterAndCreatedAtBefore(
              userId, category, start, end);
      // 해당 플랜에 관련된 스케줄을 모두 반환
      for (Plan plan : relatedPlans) {
        List<Schedule> schedules = scheduleRepository.findPlanSchedules(plan.getId(), start, end);
        for (Schedule schedule : schedules) {
          response.add(
              new RelatedScheduleResponse(
                  schedule.getTitle(),
                  schedule.getDescription(),
                  schedule.getStartTime(),
                  schedule.getEndTime()));
        }
      }
    }
    if (keyword == null && category == null) {
      List<Schedule> allSchedules = scheduleRepository.findSchedulesBetween(userId, start, end);
      for (Schedule schedule : allSchedules) {
        response.add(
            new RelatedScheduleResponse(
                schedule.getTitle(),
                schedule.getDescription(),
                schedule.getStartTime(),
                schedule.getEndTime()));
      }
    }

    return response.stream().distinct().collect(Collectors.toList());
  }
}

package com.ellu.looper.stomp.controller;

import com.ellu.looper.stomp.dto.MessageRequest;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;

@Controller
public class StompController {

  private final SimpMessageSendingOperations messageTemplate;

  public StompController(SimpMessageSendingOperations messageTemplate) {
    this.messageTemplate = messageTemplate;
  }

  @MessageMapping("/{roomId}") // client에서 특정 publish/roomId 형태로 메시지를 발행 시 MessageMapping 수신
  public void sendMessage(@DestinationVariable Long roomId, MessageRequest messageRequest) {
    messageTemplate.convertAndSend("/topic/" + roomId, messageRequest); // 해당 roomId에 메시지를 발행하여 구독중인 client에게 메시지 전송
  }
}
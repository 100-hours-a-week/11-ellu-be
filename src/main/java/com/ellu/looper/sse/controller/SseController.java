package com.ellu.looper.sse.controller;

import com.ellu.looper.commons.CurrentUser;
import com.ellu.looper.sse.service.SseEmitterService;
import com.ellu.looper.sse.service.SseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
@RequestMapping("/sse")
public class SseController {

  private final SseService sseService;
  private final SseEmitterService sseEmitterService;

  @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter subscribe(@CurrentUser Long userId) {
    return sseService.subscribe(userId);
  }

  @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter streamChat(@CurrentUser Long userId) {
    return sseEmitterService.createEmitter(userId.toString());
  }
}

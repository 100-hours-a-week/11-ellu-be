package com.ellu.looper.notification.scheduler;

import com.ellu.looper.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationCleanupScheduler {

  private final NotificationService notificationService;

  @Scheduled(cron = "0 0 3 * * *") // 매일 새벽 3시에 실행됨.
  public void cleanOldNotifications() {
    notificationService.softDeleteOldNotifications();
  }
}


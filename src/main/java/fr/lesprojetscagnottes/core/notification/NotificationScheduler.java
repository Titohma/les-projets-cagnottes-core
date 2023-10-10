package fr.lesprojetscagnottes.core.notification;

import fr.lesprojetscagnottes.core.common.date.DateUtils;
import fr.lesprojetscagnottes.core.notification.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@Slf4j
public class NotificationScheduler {

    final private NotificationService notificationService;

    @Autowired
    public NotificationScheduler(
            NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Scheduled(fixedDelay = 3600000)
    @Transactional
    public void purgeNotifications() {
        LocalDateTime lastMonth = LocalDateTime.now().minusDays(30);
        notificationService.deleteByCreatedAtLessThan(DateUtils.asDate(lastMonth));
    }

}

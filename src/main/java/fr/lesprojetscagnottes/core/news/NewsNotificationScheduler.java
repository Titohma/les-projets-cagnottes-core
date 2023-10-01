package fr.lesprojetscagnottes.core.news;

import fr.lesprojetscagnottes.core.news.entity.NewsNotificationEntity;
import fr.lesprojetscagnottes.core.news.service.NewsNotificationService;
import fr.lesprojetscagnottes.core.notification.entity.NotificationEntity;
import fr.lesprojetscagnottes.core.notification.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Slf4j
public class NewsNotificationScheduler {

    final private NotificationService notificationService;
    final private NewsNotificationService newsNotificationService;

    @Autowired
    public NewsNotificationScheduler(
            NotificationService notificationService,
            NewsNotificationService newsNotificationService) {
        this.notificationService = notificationService;
        this.newsNotificationService = newsNotificationService;
    }

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void processNotifications() {
        List<NotificationEntity> notifications = notificationService.list();
        notifications.forEach(notification -> {
            log.debug("Process notification {}", notification.getId());

            switch (notification.getName()) {
                case IDEA_PUBLISHED, PROJECT_PUBLISHED -> {

                    NewsNotificationEntity newsNotification = newsNotificationService.findByNotificationId(notification.getId());
                    if (newsNotification == null) {
                        log.debug("No news notification detected");
                        newsNotification = new NewsNotificationEntity();
                        newsNotification.setNotification(notification);
                        newsNotification.setSent(false);
                        newsNotification = newsNotificationService.save(newsNotification);
                    }
                    if (!newsNotification.getSent()) {
                        log.debug("Sending news notification {}", newsNotification.getId());
                        newsNotification.setNews(newsNotificationService.sendNotification(notification, newsNotification));
                        newsNotification.setSent(true);
                        newsNotificationService.save(newsNotification);
                    }
                }
                default -> {
                }
            }

        });
    }

}

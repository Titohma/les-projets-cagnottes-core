package fr.lesprojetscagnottes.core.news.repository;

import fr.lesprojetscagnottes.core.news.entity.NewsNotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsNotificationRepository extends JpaRepository<NewsNotificationEntity, Long> {
    NewsNotificationEntity findByNotificationId(Long id);
}

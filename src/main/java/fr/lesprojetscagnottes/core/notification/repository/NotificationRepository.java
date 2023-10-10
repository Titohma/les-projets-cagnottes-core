package fr.lesprojetscagnottes.core.notification.repository;

import fr.lesprojetscagnottes.core.notification.entity.NotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Date;
import java.util.List;

public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {
    List<NotificationEntity> findAllByCreatedAtGreaterThan(Date date);

    void deleteByCreatedAtLessThan(Date date);
}

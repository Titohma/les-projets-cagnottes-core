package fr.lesprojetscagnottes.core.news.entity;

import fr.lesprojetscagnottes.core.news.model.NewsNotificationModel;
import fr.lesprojetscagnottes.core.notification.entity.NotificationEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter(AccessLevel.PUBLIC)
@Setter(AccessLevel.PUBLIC)
@Entity
@Table(name = "news_notifications")
public class NewsNotificationEntity extends NewsNotificationModel {

    @ManyToOne(fetch = FetchType.LAZY)
    private NotificationEntity notification = new NotificationEntity();

    @OneToOne
    private NewsEntity news = null;

}

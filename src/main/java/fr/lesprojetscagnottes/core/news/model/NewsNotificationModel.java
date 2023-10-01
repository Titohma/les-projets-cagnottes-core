package fr.lesprojetscagnottes.core.news.model;

import fr.lesprojetscagnottes.core.common.GenericModel;
import fr.lesprojetscagnottes.core.common.audit.AuditEntity;
import fr.lesprojetscagnottes.core.news.entity.NewsNotificationEntity;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter(AccessLevel.PUBLIC)
@Setter(AccessLevel.PUBLIC)
@MappedSuperclass
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder()
public class NewsNotificationModel extends AuditEntity<String> {

    @Column
    protected Boolean sent;

    @Transient
    protected GenericModel notification;

    @Transient
    protected GenericModel news;

    public static NewsNotificationModel fromEntity(NewsNotificationEntity entity) {

        return NewsNotificationModel.builder()
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getUpdatedBy())
                .updatedAt(entity.getUpdatedAt())
                .updatedAt(entity.getUpdatedAt())
                .updatedBy(entity.getUpdatedBy())
                .id(entity.getId())
                .notification(new GenericModel(entity.getNotification()))
                .news(new GenericModel(entity.getNews()))
                .build();
    }
}

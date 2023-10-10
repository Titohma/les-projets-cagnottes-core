package fr.lesprojetscagnottes.core.notification.service;

import com.google.gson.Gson;
import fr.lesprojetscagnottes.core.common.date.DateUtils;
import fr.lesprojetscagnottes.core.notification.entity.NotificationEntity;
import fr.lesprojetscagnottes.core.notification.model.NotificationName;
import fr.lesprojetscagnottes.core.notification.repository.NotificationRepository;
import fr.lesprojetscagnottes.core.organization.service.OrganizationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class NotificationService {

    @Autowired
    private Gson gson;

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private NotificationRepository notificationRepository;

    public List<NotificationEntity> list() {
        LocalDateTime lastWeek = LocalDateTime.now().minusDays(7);
        return notificationRepository.findAllByCreatedAtGreaterThan(DateUtils.asDate(lastWeek));
    }

    public NotificationEntity create(NotificationName name, Map<String, Object> variables, Long organizationId) {
        NotificationEntity entity = new NotificationEntity();
        entity.setName(name);
        entity.setVariables(gson.toJson(variables));
        entity.setOrganization(organizationService.findById(organizationId));
        return notificationRepository.save(entity);
    }

    public void deleteByCreatedAtLessThan(Date date) {
        notificationRepository.deleteByCreatedAtLessThan(date);
    }
}

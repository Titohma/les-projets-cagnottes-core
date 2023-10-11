package fr.lesprojetscagnottes.core.providers.slack.service;

import fr.lesprojetscagnottes.core.providers.slack.entity.SlackNotificationEntity;
import fr.lesprojetscagnottes.core.providers.slack.repository.SlackNotificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SlackNotificationService {

    @Autowired
    private SlackNotificationRepository slackNotificationRepository;

    public SlackNotificationEntity findByNotificationId(Long id) {
        return slackNotificationRepository.findByNotificationId(id);
    }

    public SlackNotificationEntity findById(Long id) {
        return slackNotificationRepository.findById(id).orElse(null);
    }

    public SlackNotificationEntity save(SlackNotificationEntity notification) {
        return slackNotificationRepository.save(notification);
    }

    public void deleteAllBySlackTeamId(Long id) {
        slackNotificationRepository.deleteAllByTeamId(id);
    }
}

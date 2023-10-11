package fr.lesprojetscagnottes.core.providers.slack;

import com.google.gson.Gson;
import fr.lesprojetscagnottes.core.notification.entity.NotificationEntity;
import fr.lesprojetscagnottes.core.notification.model.NotificationVariables;
import fr.lesprojetscagnottes.core.notification.service.NotificationService;
import fr.lesprojetscagnottes.core.providers.slack.entity.SlackNotificationEntity;
import fr.lesprojetscagnottes.core.providers.slack.entity.SlackTeamEntity;
import fr.lesprojetscagnottes.core.providers.slack.entity.SlackUserEntity;
import fr.lesprojetscagnottes.core.providers.slack.service.SlackClientService;
import fr.lesprojetscagnottes.core.providers.slack.service.SlackNotificationService;
import fr.lesprojetscagnottes.core.providers.slack.service.SlackTeamService;
import fr.lesprojetscagnottes.core.providers.slack.service.SlackUserService;
import fr.lesprojetscagnottes.core.user.entity.UserEntity;
import fr.lesprojetscagnottes.core.user.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Slf4j
public class SlackNotificationScheduler {

    @Value("${fr.lesprojetscagnottes.slack.enabled}")
    private boolean slackEnabled;

    final private SlackClientService slackClientService;

    final private SlackNotificationService slackNotificationService;

    final private SlackTeamService slackTeamService;

    final private SlackUserService slackUserService;

    final private UserService userService;

    final private Gson gson;

    final private NotificationService notificationService;

    @Autowired
    public SlackNotificationScheduler(
            SlackClientService slackClientService,
            SlackNotificationService slackNotificationService,
            SlackTeamService slackTeamService,
            SlackUserService slackUserService,
            UserService userService,
            Gson gson,
            NotificationService notificationService) {
        this.slackClientService = slackClientService;
        this.slackNotificationService = slackNotificationService;
        this.slackTeamService = slackTeamService;
        this.slackUserService = slackUserService;
        this.userService = userService;
        this.gson = gson;
        this.notificationService = notificationService;
    }

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void processNotifications() {
        if (slackEnabled) {
            List<NotificationEntity> notifications = notificationService.list();
            notifications.forEach(notification -> {
                log.debug("Process notification {}", notification.getId());
                SlackNotificationEntity slackNotification = slackNotificationService.findByNotificationId(notification.getId());
                if (slackNotification == null) {
                    log.debug("No Slack notification detected");
                    slackNotification = new SlackNotificationEntity();
                    slackNotification.setNotification(notification);
                    slackNotification.setSent(false);
                    slackNotification.setTeam(slackTeamService.findByOrganizationId(notification.getOrganization().getId()));
                    slackNotification = slackNotificationService.save(slackNotification);
                }
                if (slackNotification.getTeam() != null && !slackNotification.getSent()) {
                    log.debug("Sending Slack notification {}", slackNotification.getId());

                    NotificationVariables notificationVariables = gson.fromJson(notification.getVariables(), NotificationVariables.class);
                    if (notificationVariables.get("_user_email_") != null && notificationVariables.get("_organization_id_") != null) {
                        UserEntity user = userService.findByEmail(notificationVariables.get("_user_email_").toString());
                        log.debug("User : {}", user);
                        if (user != null) {
                            Long organizationId = Double.valueOf(notificationVariables.get("_organization_id_").toString()).longValue();
                            SlackTeamEntity slackTeam = slackTeamService.findByOrganizationId(organizationId);
                            log.debug("SlackTeam matching organization {} : {}", organizationId, slackTeam);
                            if (slackTeam != null) {
                                SlackUserEntity slackUser = slackUserService.findByUserIdAndSlackTeamId(user.getId(), slackTeam.getId());
                                log.debug("SlackUser matching user {} & slackteam {} : {}", user.getId(), slackTeam.getId(), slackUser);
                                if (slackUser != null) {
                                    notificationVariables.put("user_tagname", "<@" + slackUser.getSlackId() + ">");
                                }
                            }
                        }
                    }
                    if (notificationVariables.get("user_tagname") == null) {
                        notificationVariables.put("user_tagname", "*" + notificationVariables.get("user_fullname") + "*");
                    }
                    notificationVariables.put("slack_notification_id", slackNotification.getId());
                    notification.setVariables(gson.toJson(notificationVariables));

                    slackClientService.sendNotification(notification, slackNotification);
                    slackNotification.setSent(true);
                    slackNotificationService.save(slackNotification);
                }
            });
        }
    }

}

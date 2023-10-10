package fr.lesprojetscagnottes.core.vote.scheduler;

import fr.lesprojetscagnottes.core.news.service.NewsService;
import fr.lesprojetscagnottes.core.notification.model.NotificationName;
import fr.lesprojetscagnottes.core.notification.service.NotificationService;
import fr.lesprojetscagnottes.core.project.entity.ProjectEntity;
import fr.lesprojetscagnottes.core.project.service.ProjectService;
import fr.lesprojetscagnottes.core.user.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class VoteScheduler {

    @Value("${fr.lesprojetscagnottes.web.url}")
    private String webUrl;

    private final NotificationService notificationService;

    private final ProjectService projectService;

    @Autowired
    public VoteScheduler(NewsService newsService,
                         NotificationService notificationService,
                         UserService userService,
                         ProjectService projectService) {
        this.notificationService = notificationService;
        this.projectService = projectService;
    }
    @Scheduled(cron = "${fr.lesprojetscagnottes.core.schedule.voteforidea}")
    public void notifyVoteForIdea() {
        log.info("Vote for idea has started");
        ProjectEntity project = projectService.findLessVotedIdea();
        Map<String, Object> model = new HashMap<>();
        model.put("_organization_id_", project.getOrganization().getId());
        model.put("project_id", project.getId());
        model.put("project_title", project.getTitle());
        model.put("project_url", webUrl + "/projects/" + project.getId());
        notificationService.create(NotificationName.VOTE_ON_IDEA, model, project.getOrganization().getId());
    }
}

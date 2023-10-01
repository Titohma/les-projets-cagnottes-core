package fr.lesprojetscagnottes.core.news.service;

import com.google.gson.Gson;
import fr.lesprojetscagnottes.core.news.entity.NewsEntity;
import fr.lesprojetscagnottes.core.news.entity.NewsNotificationEntity;
import fr.lesprojetscagnottes.core.news.model.NewsType;
import fr.lesprojetscagnottes.core.news.repository.NewsNotificationRepository;
import fr.lesprojetscagnottes.core.notification.entity.NotificationEntity;
import fr.lesprojetscagnottes.core.notification.model.NotificationVariables;
import fr.lesprojetscagnottes.core.project.entity.ProjectEntity;
import fr.lesprojetscagnottes.core.project.service.ProjectService;
import fr.lesprojetscagnottes.core.user.entity.UserEntity;
import fr.lesprojetscagnottes.core.user.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.UUID;

@Slf4j
@Service
public class NewsNotificationService {

    final private Gson gson;
    final private SpringTemplateEngine templateEngine;
    final private NewsService newsService;
    final private ProjectService projectService;
    final private UserService userService;
    final private NewsNotificationRepository newsNotificationRepository;

    @Autowired
    public NewsNotificationService(
            Gson gson,
            SpringTemplateEngine templateEngine,
            NewsService newsService,
            ProjectService projectService, UserService userService, NewsNotificationRepository newsNotificationRepository) {
        this.gson = gson;
        this.templateEngine = templateEngine;
        this.newsService = newsService;
        this.projectService = projectService;
        this.userService = userService;
        this.newsNotificationRepository = newsNotificationRepository;
    }

    public NewsNotificationEntity findByNotificationId(Long id) {
        return newsNotificationRepository.findByNotificationId(id);
    }

    public NewsNotificationEntity save(NewsNotificationEntity msNotification) {
        return newsNotificationRepository.save(msNotification);
    }

    public NewsEntity sendNotification(NotificationEntity notification, NewsNotificationEntity newsNotification) {
        Context context = new Context();
        context.setVariables(gson.fromJson(notification.getVariables(), NotificationVariables.class));
        String message = templateEngine.process("news/fr/" + notification.getName(), context);

        NewsEntity news = new NewsEntity();
        news.setType(NewsType.valueOf(notification.getName().toString()));

        String projectTitle = this.getVariable(context, "project_title");
        news.setTitle(projectTitle);

        if(news.getType().equals(NewsType.IDEA_PUBLISHED) || news.getType().equals(NewsType.PROJECT_PUBLISHED)) {
            String projectIdString = this.getVariable(context, "project_id");
            if(projectIdString != null) {
                Long projectId = Math.round(Double.parseDouble(projectIdString));
                ProjectEntity projectEntity = projectService.findById(projectId);
                news.setProject(projectEntity);
            }
        }

        String userEmail = this.getVariable(context, "_user_email_");
        UserEntity user = this.userService.findByEmail(userEmail);

        news.setContent(message);
        news.setWorkspace(UUID.randomUUID().toString());
        news.setAuthor(user);
        news.setOrganization(notification.getOrganization());
        return newsService.save(news);
    }

    private String getVariable(Context context, String name) {
        if(context.getVariable(name) != null && !context.getVariable(name).toString().isEmpty()) {
            return context.getVariable(name).toString();
        } else {
            return null;
        }
    }
}

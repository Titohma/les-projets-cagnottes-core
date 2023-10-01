package fr.lesprojetscagnottes.core.news.service;

import fr.lesprojetscagnottes.core.common.exception.BadRequestException;
import fr.lesprojetscagnottes.core.common.exception.ForbiddenException;
import fr.lesprojetscagnottes.core.common.exception.NotFoundException;
import fr.lesprojetscagnottes.core.common.pagination.DataPage;
import fr.lesprojetscagnottes.core.news.entity.NewsEntity;
import fr.lesprojetscagnottes.core.news.model.NewsModel;
import fr.lesprojetscagnottes.core.news.model.NewsType;
import fr.lesprojetscagnottes.core.news.repository.NewsRepository;
import fr.lesprojetscagnottes.core.organization.entity.OrganizationEntity;
import fr.lesprojetscagnottes.core.organization.service.OrganizationService;
import fr.lesprojetscagnottes.core.project.entity.ProjectEntity;
import fr.lesprojetscagnottes.core.project.service.ProjectService;
import fr.lesprojetscagnottes.core.user.entity.UserEntity;
import fr.lesprojetscagnottes.core.user.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.Date;

@Slf4j
@Service
public class NewsService {

    final private OrganizationService organizationService;
    final private ProjectService projectService;
    final private UserService userService;
    final private NewsRepository newsRepository;

    public NewsService(
            OrganizationService organizationService,
            ProjectService projectService,
            UserService userService,
            NewsRepository newsRepository) {
        this.organizationService = organizationService;
        this.projectService = projectService;
        this.userService = userService;
        this.newsRepository = newsRepository;
    }


    public DataPage<NewsModel> listByProjects_Id(Principal principal, Long id, int offset, int limit) {

        // Verify that IDs are corrects
        if (id <= 0) {
            log.error("Impossible to get news : parameters are incorrect");
            throw new BadRequestException();
        }

        // Verify that organization exists
        ProjectEntity project = projectService.findById(id);
        if (project == null) {
            log.error("Impossible to get news : project not found");
            throw new NotFoundException();
        }

        // Verify if principal has correct privileges
        Long userLoggedInId = userService.get(principal).getId();
        if (userService.isNotAdmin(userLoggedInId) && !userService.isMemberOfOrganization(userLoggedInId, project.getOrganization().getId())) {
            log.error("Impossible to get news : principal is not a member of any of the project organizations");
            throw new ForbiddenException();
        }

        // Get and transform entities
        Page<NewsEntity> entities = newsRepository.findAllByProjectId(id, PageRequest.of(offset, limit, Sort.by("createdAt").descending()));
        DataPage<NewsModel> models = new DataPage<>(entities);
        entities.getContent().forEach(entity -> models.getContent().add(NewsModel.fromEntity(entity)));
        return models;
    }

    public NewsEntity findFirstByProjectIdAndCreatedAtGreaterThanOrderByCreatedAtDesc(Long id, Date createdAt) {
        return newsRepository.findFirstByProjectIdAndCreatedAtGreaterThanOrderByCreatedAtDesc(id, createdAt);
    }

    public NewsEntity save(NewsEntity newsEntity) {
        return this.newsRepository.save(newsEntity);
    }

    public NewsModel save(Principal principal, NewsModel newsModel) {

        // Fails if any of references are null
        if (newsModel == null) {
            log.error("Impossible to create a null news");
            throw new BadRequestException();
        }

        // Retrieve full referenced objects
        OrganizationEntity organization = organizationService.findById(newsModel.getOrganization().getId());
        ProjectEntity project = projectService.findById(newsModel.getProject().getId());

        // Verify that principal is member of organizations
        UserEntity userLoggedIn = userService.get(principal);
        if (organization != null && !userService.isMemberOfOrganization(userLoggedIn.getId(), organization.getId())) {
            log.error("Impossible to create news \"{}\" : principal {} is not member of organization {}", newsModel.getTitle(), userLoggedIn.getId(), organization.getId());
            throw new ForbiddenException();
        }

        // Update project if necessary
        if (project != null && newsModel.getType().equals(NewsType.ARTICLE)) {
            project.setLastStatusUpdate(new Date());
            project = projectService.save(project);
        }

        // Save news
        NewsEntity newsToSave = new NewsEntity();
        newsToSave.setType(newsModel.getType());
        newsToSave.setTitle(newsModel.getTitle());
        newsToSave.setContent(newsModel.getContent());
        newsToSave.setWorkspace(newsModel.getWorkspace());
        newsToSave.setAuthor(userLoggedIn);
        newsToSave.setOrganization(organization);
        newsToSave.setProject(project);
        return NewsModel.fromEntity(this.save(newsRepository.save(newsToSave)));
    }

}

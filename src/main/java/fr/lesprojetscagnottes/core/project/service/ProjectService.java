package fr.lesprojetscagnottes.core.project.service;

import fr.lesprojetscagnottes.core.common.exception.BadRequestException;
import fr.lesprojetscagnottes.core.common.exception.ForbiddenException;
import fr.lesprojetscagnottes.core.common.exception.NotFoundException;
import fr.lesprojetscagnottes.core.notification.model.NotificationName;
import fr.lesprojetscagnottes.core.notification.service.NotificationService;
import fr.lesprojetscagnottes.core.organization.entity.OrganizationEntity;
import fr.lesprojetscagnottes.core.organization.service.OrganizationService;
import fr.lesprojetscagnottes.core.project.entity.ProjectEntity;
import fr.lesprojetscagnottes.core.project.model.ProjectModel;
import fr.lesprojetscagnottes.core.project.model.ProjectStatus;
import fr.lesprojetscagnottes.core.project.repository.ProjectRepository;
import fr.lesprojetscagnottes.core.user.entity.UserEntity;
import fr.lesprojetscagnottes.core.user.model.UserModel;
import fr.lesprojetscagnottes.core.user.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.*;

@Slf4j
@Service
public class ProjectService {

    @Value("${fr.lesprojetscagnottes.web.url}")
    private String webUrl;

    private final NotificationService notificationService;

    private final OrganizationService organizationService;

    private final UserService userService;

    private final ProjectRepository projectRepository;

    @Autowired
    public ProjectService(
            NotificationService notificationService,
            OrganizationService organizationService,
            UserService userService,
            ProjectRepository projectRepository) {
        this.notificationService = notificationService;
        this.organizationService = organizationService;
        this.userService = userService;
        this.projectRepository = projectRepository;
    }

    public ProjectEntity findById(Long id) {
        return projectRepository.findById(id).orElse(null);
    }

    public ProjectModel findById(Principal principal, Long id) {

        // Verify that ID is correct
        if (id <= 0) {
            log.error("Impossible to get project by ID : ID is incorrect");
            throw new BadRequestException();
        }

        // Verify that entity exists
        ProjectEntity entity = findById(id);
        if (entity == null) {
            log.error("Impossible to get project by ID : project not found");
            throw new NotFoundException();
        }

        // Verify that principal is in project organizations
        Long userLoggedInId = userService.get(principal).getId();
        if (userService.isNotAdmin(userLoggedInId) && !userService.isMemberOfOrganization(userLoggedInId, entity.getOrganization().getId())) {
            log.error("Impossible to get project by ID : principal has not enough privileges");
            throw new ForbiddenException();
        }

        // Transform and return organization
        return ProjectModel.fromEntity(entity);
    }

    public Set<ProjectModel> getByIds(Principal principal, Set<Long> ids) {

        Long userLoggedInId = userService.get(principal).getId();
        boolean userLoggedIn_isNotAdmin = userService.isNotAdmin(userLoggedInId);
        Set<OrganizationEntity> userLoggedInOrganizations = organizationService.findAllByMembers_Id(userLoggedInId);
        Set<ProjectModel> models = new LinkedHashSet<>();

        for (Long id : ids) {

            // Retrieve full referenced objects
            ProjectEntity project = projectRepository.findById(id).orElse(null);
            if (project == null) {
                log.error("Impossible to get project {} : it doesn't exist", id);
                continue;
            }

            // Verify that principal share an organization with the user
            Set<OrganizationEntity> projectOrganizations = organizationService.findAllByProjects_Id(id);
            if (userService.hasNoACommonOrganization(userLoggedInOrganizations, projectOrganizations) && userLoggedIn_isNotAdmin) {
                log.error("Impossible to get project {} : principal {} is not in its organizations", id, userLoggedInId);
                continue;
            }

            // Add the user to returned list
            models.add(ProjectModel.fromEntity(project));
        }

        return models;
    }

    public Set<UserModel> listTeammates(Principal principal, Long id) {

        // Verify that IDs are corrects
        if (id <= 0) {
            log.error("Impossible to get teammates : parameters are incorrect");
            throw new BadRequestException();
        }

        // Verify that organization exists
        ProjectEntity project = projectRepository.findById(id).orElse(null);
        if (project == null) {
            log.error("Impossible to get teammates : project not found");
            throw new NotFoundException();
        }

        // Verify if principal has correct privileges
        Long userLoggedInId = userService.get(principal).getId();
        if (userService.isNotAdmin(userLoggedInId) && !userService.isMemberOfOrganization(userLoggedInId, project.getOrganization().getId())) {
            log.error("Impossible to get teammates : principal is not a member of any of the project organizations");
            throw new ForbiddenException();
        }

        // Get users
        Set<UserEntity> entities = userService.findAllByProjects_Id(id);
        Set<UserModel> models = new LinkedHashSet<>();
        entities.forEach(entity -> models.add(UserModel.fromEntity(entity)));
        return models;
    }

    public ProjectEntity save(ProjectEntity projectEntity) {
        return projectRepository.save(projectEntity);
    }

    public ProjectModel save(Principal principal, ProjectModel projectModel) {

        // Retrieve full referenced objects
        UserEntity leader = userService.findById(projectModel.getLeader().getId());
        OrganizationEntity organization = organizationService.findById(projectModel.getOrganization().getId());

        // Fails if organization reference is null
        if (organization == null) {
            log.error("Impossible to save project \"{}\" : organization doesn't exist", projectModel.getTitle());
            throw new NotFoundException();
        }

        // Verify that principal is member of organizations
        UserEntity userLoggedIn = userService.get(principal);
        Long userLoggedInId = userLoggedIn.getId();
        if (userService.isNotAdmin(userLoggedInId) && !userService.isMemberOfOrganization(userLoggedInId, organization.getId())) {
            log.error("Impossible to save project \"{}\" : principal {} is not member of organization", projectModel.getTitle(), userLoggedInId);
            throw new ForbiddenException();
        }

        // If project is in the idea status
        if (projectModel.getStatus() != ProjectStatus.IDEA) {

            // Fails if leader reference is null
            if (leader == null) {
                log.error("Impossible to save project \"{}\" : leader doesn't exist", projectModel.getTitle());
                throw new NotFoundException();
            }

            // Verify that principal is the leader of the project (in update mode)
            if (projectModel.getId() > 0
                    && !leader.getId().equals(userLoggedInId)
                    && userService.isNotAdmin(userLoggedInId)
                    || (projectModel.getStatus() == ProjectStatus.IDEA && projectModel.getIdeaHasLeaderCreator() && !leader.getId().equals(userLoggedInId))
            ) {
                log.error("Impossible to update project {} : principal {} is not its leader", projectModel.getTitle(), projectModel.getId());
                throw new ForbiddenException();
            }
        } else {

            // Verify that principal is the leader of the project (in update mode)
            if (leader == null && projectModel.getIdeaHasLeaderCreator()) {
                log.error("Impossible to update project {} : leader must be defined", projectModel.getTitle());
                throw new ForbiddenException();
            }

            // Verify that principal is the leader of the project (in update mode)
            if (leader != null && projectModel.getIdeaHasLeaderCreator() && !leader.getId().equals(userLoggedInId) && userService.isNotAdmin(userLoggedInId)) {
                log.error("Impossible to update project {} : principal {} is not its leader", projectModel.getTitle(), userLoggedInId);
                throw new ForbiddenException();
            }

        }


        // Get existing project
        ProjectEntity projectToSave = null;
        if (projectModel.getId() > 0) {
            projectToSave = findById(projectModel.getId());
            if (projectToSave == null) {
                log.error("Impossible to update project {} : cannot find any project in DB", projectModel.getId());
                throw new NotFoundException();
            }
        }
        if (projectToSave == null) {
            projectToSave = new ProjectEntity();
        }

        // Save previous status for notification purposes
        ProjectStatus previousStatus = projectToSave.getStatus();

        // Save project
        projectToSave.setTitle(projectModel.getTitle());
        projectToSave.setShortDescription(projectModel.getShortDescription());
        projectToSave.setLongDescription(projectModel.getLongDescription());
        projectToSave.setIdeaHasAnonymousCreator(projectModel.getIdeaHasAnonymousCreator());
        projectToSave.setIdeaHasLeaderCreator(projectModel.getIdeaHasLeaderCreator());
        projectToSave.setPeopleRequired(projectModel.getPeopleRequired());
        projectToSave.setStatus(projectModel.getStatus());
        projectToSave.setWorkspace(projectModel.getWorkspace());
        projectToSave.setLeader(leader);
        projectToSave.setOrganization(organization);
        projectToSave.getPeopleGivingTime().add(leader);
        ProjectEntity projectSaved = this.save(projectToSave);

        // Prepare & send notifications for projects
        if ((previousStatus.equals(ProjectStatus.NEW) || previousStatus.equals(ProjectStatus.DRAFT) || previousStatus.equals(ProjectStatus.IDEA))
                && projectToSave.getStatus().equals(ProjectStatus.IN_PROGRESS)) {
            Map<String, Object> model = new HashMap<>();
            model.put("_user_email_", userLoggedIn.getEmail());
            model.put("user_fullname", userLoggedIn.getFullname());
            model.put("project_id", projectToSave.getId());
            model.put("project_title", projectToSave.getTitle());
            model.put("project_url", webUrl + "/projects/" + projectToSave.getId());
            notificationService.create(NotificationName.PROJECT_PUBLISHED, model, projectToSave.getOrganization().getId());
        }

        // Prepare & send notifications for ideas
        if ((previousStatus.equals(ProjectStatus.NEW) || previousStatus.equals(ProjectStatus.DRAFT))
                && projectToSave.getStatus().equals(ProjectStatus.IDEA)) {
            Map<String, Object> model = new HashMap<>();
            model.put("_user_email_", userLoggedIn.getEmail());
            model.put("user_fullname", userLoggedIn.getFullname());
            model.put("project_id", projectToSave.getId());
            model.put("project_title", projectToSave.getTitle());
            model.put("project_url", webUrl + "/projects/" + projectToSave.getId());
            notificationService.create(NotificationName.IDEA_PUBLISHED, model, projectToSave.getOrganization().getId());
        }

        return ProjectModel.fromEntity(projectSaved);
    }

    public ProjectModel create(Principal principal, ProjectModel projectModel) {

        // Fails if any of references are null
        if (projectModel == null || projectModel.getLeader() == null || projectModel.getLeader().getId() <= 0 ||
                projectModel.getOrganization() == null || projectModel.getOrganization().getId() <= 0) {
            if (projectModel != null) {
                log.error("Impossible to save project \"{}\" : some references are missing", projectModel.getTitle());
            } else {
                log.error("Impossible to save a null project");
            }
            throw new BadRequestException();
        }

        return save(principal, projectModel);
    }

    public ProjectModel update(Principal principal, ProjectModel projectModel) {

        // Fails if any of references are null
        if (projectModel == null || projectModel.getId() <= 0 || projectModel.getLeader() == null || projectModel.getLeader().getId() < 0) {
            if (projectModel != null) {
                log.error("Impossible to update project {} : some references are missing", projectModel.getId());
            } else {
                log.error("Impossible to update a null project");
            }
            throw new BadRequestException();
        }

        return save(principal, projectModel);
    }

    public void join(Principal principal, Long id) {

        // Fails if any of references are null
        if (id < 0) {
            log.error("Impossible to join project {} : some references are missing", id);
            throw new BadRequestException();
        }

        // Retrieve full referenced objects
        ProjectEntity project = projectRepository.findById(id).orElse(null);

        // Fails if any of references are null
        if (project == null) {
            log.error("Impossible to join project {} : one or more reference(s) doesn't exist", id);
            throw new NotFoundException();
        }

        // Verify that principal is member of organizations
        UserEntity userLoggedIn = userService.get(principal);
        Long userLoggedInId = userLoggedIn.getId();
        if (!userService.isMemberOfOrganization(userLoggedInId, project.getOrganization().getId())) {
            log.error("Impossible to join project {} : principal {} is not member of organization", id, userLoggedInId);
            throw new ForbiddenException();
        }

        // Fails if project is finished
        if (project.getStatus().equals(ProjectStatus.FINISHED)) {
            log.error("Impossible to join project {} : status is finished", id);
        }

        // Add or remove member
        project.setPeopleGivingTime(userService.findAllByProjects_Id(id));
        project.getPeopleGivingTime()
                .stream()
                .filter(member -> userLoggedInId.equals(member.getId()))
                .findAny()
                .ifPresentOrElse(
                        user -> project.getPeopleGivingTime().remove(user),
                        () -> project.getPeopleGivingTime().add(userLoggedIn));
        projectRepository.save(project);
    }

    public void updateStatus(Principal principal, Long id, ProjectStatus status) {

        // Fails if any of references are null
        if (id < 0) {
            log.error("Impossible to update project status {} : some references are missing", id);
            throw new BadRequestException();
        }

        // Retrieve full referenced objects
        ProjectEntity project = projectRepository.findById(id).orElse(null);

        // Fails if any of references are null
        if (project == null) {
            log.error("Impossible to update project status {} : one or more reference(s) doesn't exist", id);
            throw new NotFoundException();
        }

        // Verify that principal has enough privileges
        UserEntity userLoggedIn = userService.get(principal);
        Long userLoggedInId = userLoggedIn.getId();
        if (userService.isNotAdmin(userLoggedInId)
                && userService.isNotManagerOfOrganization(userLoggedInId, project.getOrganization().getId())
                && !userLoggedInId.equals(project.getLeader().getId())) {
            log.error("Impossible to update project status {} : principal has not enough privileges", project.getId());
            throw new ForbiddenException();
        }

        // Update status
        project.setStatus(status);
        projectRepository.save(project);
    }

    public ProjectEntity findLessVotedIdea() {
        return projectRepository.findLessVotedIdea();
    }
}

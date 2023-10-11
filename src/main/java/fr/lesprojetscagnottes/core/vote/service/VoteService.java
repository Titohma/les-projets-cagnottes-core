package fr.lesprojetscagnottes.core.vote.service;

import fr.lesprojetscagnottes.core.common.exception.BadRequestException;
import fr.lesprojetscagnottes.core.common.exception.ForbiddenException;
import fr.lesprojetscagnottes.core.common.exception.NotFoundException;
import fr.lesprojetscagnottes.core.project.entity.ProjectEntity;
import fr.lesprojetscagnottes.core.project.service.ProjectService;
import fr.lesprojetscagnottes.core.user.entity.UserEntity;
import fr.lesprojetscagnottes.core.user.service.UserService;
import fr.lesprojetscagnottes.core.vote.entity.VoteEntity;
import fr.lesprojetscagnottes.core.vote.model.ScoreModel;
import fr.lesprojetscagnottes.core.vote.model.VoteModel;
import fr.lesprojetscagnottes.core.vote.model.VoteType;
import fr.lesprojetscagnottes.core.vote.repository.VoteRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.LinkedHashSet;
import java.util.Set;

@Slf4j
@Service
public class VoteService {

    private final ProjectService projectService;
    private final UserService userService;
    private final VoteRepository voteRepository;

    @Autowired
    public VoteService(ProjectService projectService, UserService userService, VoteRepository voteRepository) {
        this.projectService = projectService;
        this.userService = userService;
        this.voteRepository = voteRepository;
    }

    public ScoreModel getScoreByProjectId(Principal principal, Long projectId) {

        // Fails if any of references are null
        if (projectId == null || projectId < 0) {
            log.error("Impossible to get score for project {} : incorrect ID", projectId);
            throw new BadRequestException();
        }

        // Retrieve full referenced objects
        ProjectEntity project = projectService.findById(projectId);

        // Fails if project reference is null
        if (project == null) {
            log.error("Impossible to get score for project {} : project doesn't exist", projectId);
            throw new NotFoundException();
        }

        // Verify that principal is member of organizations
        UserEntity userLoggedIn = userService.get(principal);
        Long userLoggedInId = userLoggedIn.getId();
        if (userService.isNotAdmin(userLoggedInId) && !userService.isMemberOfOrganization(userLoggedInId, project.getOrganization().getId())) {
            log.error("Impossible to get score for project {} : principal {} is not member of organization", projectId, userLoggedInId);
            throw new ForbiddenException();
        }

        ScoreModel scoreModel = new ScoreModel();
        scoreModel.setProjectId(projectId);
        scoreModel.setUp(voteRepository.countByTypeAndProjectId(VoteType.UP, projectId));
        scoreModel.setDown(voteRepository.countByTypeAndProjectId(VoteType.DOWN, projectId));
        return scoreModel;
    }

    public Set<ScoreModel> getScoreByProjectIds(Principal principal, Set<Long> projectIds) {

        Long userLoggedInId = userService.get(principal).getId();
        boolean userLoggedIn_isNotAdmin = userService.isNotAdmin(userLoggedInId);
        Set<ScoreModel> models = new LinkedHashSet<>();

        for (Long id : projectIds) {

            // Retrieve full referenced objects
            ProjectEntity project = projectService.findById(id);
            if (project == null) {
                log.error("Impossible to get score for project {} : it doesn't exist", id);
                continue;
            }

            // Verify that principal share an organization with the user
            if(userLoggedIn_isNotAdmin && !userService.isMemberOfOrganization(userLoggedInId, project.getOrganization().getId())) {
                log.error("Impossible to get score for project {} : principal {} is not in its organizations", id, userLoggedInId);
                continue;
            }

            // Add the user to returned list
            ScoreModel scoreModel = new ScoreModel();
            scoreModel.setProjectId(id);
            scoreModel.setUp(voteRepository.countByTypeAndProjectId(VoteType.UP, id));
            scoreModel.setDown(voteRepository.countByTypeAndProjectId(VoteType.DOWN, id));
            models.add(scoreModel);
        }

        return models;
    }

    public VoteEntity save(VoteEntity vote) {
        return voteRepository.save(vote);
    }

    public VoteModel vote(Principal principal, VoteModel voteModel) {

        // Fails if any of references are null
        if (voteModel == null || voteModel.getProject() == null || voteModel.getProject().getId() < 0) {
            if (voteModel != null) {
                log.error("Impossible to save vote {} : some references are missing", voteModel);
            } else {
                log.error("Impossible to save a null vote");
            }
            throw new BadRequestException();
        }

        // Retrieve full referenced objects
        ProjectEntity project = projectService.findById(voteModel.getProject().getId());

        // Fails if project reference is null
        if (project == null) {
            log.error("Impossible to save vote \"{}\" : project doesn't exist", voteModel.getType());
            throw new NotFoundException();
        }

        // Verify that principal is member of organizations
        UserEntity userLoggedIn = userService.get(principal);
        Long userLoggedInId = userLoggedIn.getId();
        if (userService.isNotAdmin(userLoggedInId) && !userService.isMemberOfOrganization(userLoggedInId, project.getOrganization().getId())) {
            log.error("Impossible to save vote \"{}\" : principal {} is not member of organization", voteModel.getType(), userLoggedInId);
            throw new ForbiddenException();
        }

        // Get existing vote
        VoteEntity voteToSave = voteRepository.findByProjectIdAndUserId(project.getId(), userLoggedInId);

        if(voteToSave != null && voteToSave.getType().equals(voteModel.getType())) {
            this.delete(voteToSave);
            return null;
        } else if (voteToSave == null) {
            voteToSave = new VoteEntity();
        }

        // Save project
        voteToSave.setType(voteModel.getType());
        voteToSave.setProject(project);
        voteToSave.setUser(userLoggedIn);
        return this.save(voteToSave);
    }

    public VoteEntity getUserVote(Long userId, Long projectId) {
        return voteRepository.findByProjectIdAndUserId(projectId, userId);
    }

    public VoteModel getUserVote(Principal principal, Long projectId) {

        // Fails if any of references are null
        if (projectId == null || projectId < 0) {
            log.error("Impossible to get a null vote");
            throw new BadRequestException();
        }

        // Fails if reference is null
        ProjectEntity project = projectService.findById(projectId);
        if (project == null) {
            log.error("Impossible to get vote for project \"{}\" : project doesn't exist", projectId);
            throw new NotFoundException();
        }

        // Verify that principal is member of organizations
        UserEntity userLoggedIn = userService.get(principal);
        Long userLoggedInId = userLoggedIn.getId();
        if (userService.isNotAdmin(userLoggedInId) && !userService.isMemberOfOrganization(userLoggedInId, project.getOrganization().getId())) {
            log.error("Impossible to get vote for project \"{}\" : principal {} is not member of organization", projectId, userLoggedInId);
            throw new ForbiddenException();
        }

        return VoteModel.fromEntity(this.getUserVote(userLoggedInId, projectId));
    }

    public void delete(VoteEntity voteEntity) {
        this.voteRepository.delete(voteEntity);
    }
}

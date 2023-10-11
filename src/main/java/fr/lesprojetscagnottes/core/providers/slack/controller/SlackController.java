package fr.lesprojetscagnottes.core.providers.slack.controller;

import fr.lesprojetscagnottes.core.account.entity.AccountEntity;
import fr.lesprojetscagnottes.core.account.service.AccountService;
import fr.lesprojetscagnottes.core.budget.entity.BudgetEntity;
import fr.lesprojetscagnottes.core.budget.repository.BudgetRepository;
import fr.lesprojetscagnottes.core.common.exception.BadRequestException;
import fr.lesprojetscagnottes.core.common.exception.ForbiddenException;
import fr.lesprojetscagnottes.core.common.exception.NotFoundException;
import fr.lesprojetscagnottes.core.common.strings.StringsCommon;
import fr.lesprojetscagnottes.core.organization.entity.OrganizationEntity;
import fr.lesprojetscagnottes.core.organization.repository.OrganizationRepository;
import fr.lesprojetscagnottes.core.project.entity.ProjectEntity;
import fr.lesprojetscagnottes.core.project.service.ProjectService;
import fr.lesprojetscagnottes.core.providers.slack.entity.SlackTeamEntity;
import fr.lesprojetscagnottes.core.providers.slack.entity.SlackUserEntity;
import fr.lesprojetscagnottes.core.providers.slack.model.SlackVoteModel;
import fr.lesprojetscagnottes.core.providers.slack.repository.SlackTeamRepository;
import fr.lesprojetscagnottes.core.providers.slack.repository.SlackUserRepository;
import fr.lesprojetscagnottes.core.providers.slack.service.SlackClientService;
import fr.lesprojetscagnottes.core.user.UserGenerator;
import fr.lesprojetscagnottes.core.user.entity.UserEntity;
import fr.lesprojetscagnottes.core.user.repository.UserRepository;
import fr.lesprojetscagnottes.core.user.service.UserService;
import fr.lesprojetscagnottes.core.vote.entity.VoteEntity;
import fr.lesprojetscagnottes.core.vote.service.VoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.security.Principal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
@RequestMapping("/api")
@Tag(name = "Slack", description = "The Slack API")
@RestController
public class SlackController {

    @Value("${fr.lesprojetscagnottes.web.url}")
    private String webUrl;

    private final SpringTemplateEngine templateEngine;
    private final BudgetRepository budgetRepository;
    private final OrganizationRepository organizationRepository;
    private final SlackUserRepository slackUserRepository;
    private final SlackTeamRepository slackTeamRepository;
    private final UserRepository userRepository;
    private final AccountService accountService;
    private final ProjectService projectService;
    private final SlackClientService slackClientService;
    private final UserService userService;
    private final VoteService voteService;

    @Autowired
    public SlackController(
            SpringTemplateEngine templateEngine,
            BudgetRepository budgetRepository,
            OrganizationRepository organizationRepository,
            SlackUserRepository slackUserRepository,
            SlackTeamRepository slackTeamRepository,
            SlackClientService slackClientService,
            UserRepository userRepository,
            AccountService accountService,
            ProjectService projectService,
            UserService userService, VoteService voteService) {
        this.templateEngine = templateEngine;
        this.budgetRepository = budgetRepository;
        this.organizationRepository = organizationRepository;
        this.slackUserRepository = slackUserRepository;
        this.slackTeamRepository = slackTeamRepository;
        this.slackClientService = slackClientService;
        this.userRepository = userRepository;
        this.accountService = accountService;
        this.projectService = projectService;
        this.userService = userService;
        this.voteService = voteService;
    }

    public void hello(SlackTeamEntity slackTeam) {
        UserEntity orgAdminUser = userRepository.findByEmail(slackTeam.getUpdatedBy());

        Map<String, Object> model = new HashMap<>();
        model.put("URL", webUrl);
        model.put("contact", "*" + orgAdminUser.getFirstname() + " " + orgAdminUser.getLastname() + "*");

        Context context = new Context();
        context.setVariables(model);
        String slackMessage = templateEngine.process("slack/fr/HELLO", context);

        slackClientService.inviteBotInConversation(slackTeam);
        slackClientService.postMessage(slackTeam, slackTeam.getPublicationChannelId(), slackMessage);
    }

    @Operation(summary = "Send an hello world message", description = "Send an hello world message on the Slack workspace", tags = {"Slack"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Message sent", content = @io.swagger.v3.oas.annotations.media.Content(schema = @Schema())),
            @ApiResponse(responseCode = "400", description = "Team ID is incorrect", content = @io.swagger.v3.oas.annotations.media.Content(schema = @Schema())),
            @ApiResponse(responseCode = "403", description = "Principal has not enough privileges", content = @io.swagger.v3.oas.annotations.media.Content(schema = @Schema())),
            @ApiResponse(responseCode = "404", description = "Slack Team or organization not found", content = @io.swagger.v3.oas.annotations.media.Content(schema = @Schema()))
    })
    @PreAuthorize("hasRole('USER')")
    @RequestMapping(value = "/slack/{teamId}/hello", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public void hello(Principal principal, @PathVariable String teamId) {

        // Fails if Team ID is missing
        if (teamId == null || teamId.isEmpty()) {
            log.error("Impossible to send hello world message : Team ID is incorrect");
            throw new BadRequestException();
        }

        // Retrieve full referenced objects
        SlackTeamEntity slackTeam = slackTeamRepository.findByTeamId(teamId);

        // Verify that any of references are not null
        if (slackTeam == null) {
            log.error("Impossible to send hello world message : team {} not found", teamId);
            throw new NotFoundException();
        }

        // Verify that Slack Team is associated with an organization
        OrganizationEntity organization = slackTeam.getOrganization();
        if (organization == null) {
            log.error("Impossible to send hello world message : no organization is associated with Slack Team");
            throw new NotFoundException();
        }

        // Verify that principal has correct privileges :
        // Principal is owner of the organization OR Principal is admin
        Long userLoggedInId = userService.get(principal).getId();
        if (userService.isNotOwnerOfOrganization(userLoggedInId, organization.getId()) && userService.isNotAdmin(userLoggedInId)) {
            log.error("Impossible to send hello world message : principal {} has not enough privileges", userLoggedInId);
            throw new ForbiddenException();
        }

        // Send message
        log.debug("Envoi de hello vers {}/{}", slackTeam.getTeamId(), slackTeam.getPublicationChannelId());
        hello(slackTeam);
    }

    @Operation(summary = "Register a new member in organization", description = "Register a new member in Slack Team organization", tags = {"Slack"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User registered", content = @io.swagger.v3.oas.annotations.media.Content(schema = @Schema())),
            @ApiResponse(responseCode = "400", description = "Team ID is incorrect or body is incomplete", content = @io.swagger.v3.oas.annotations.media.Content(schema = @Schema())),
            @ApiResponse(responseCode = "403", description = "Principal has not enough privileges", content = @io.swagger.v3.oas.annotations.media.Content(schema = @Schema())),
            @ApiResponse(responseCode = "404", description = "Slack Team or organization not found", content = @io.swagger.v3.oas.annotations.media.Content(schema = @Schema()))
    })
    @PreAuthorize("hasRole('ADMIN')")
    @RequestMapping(value = "/slack/{teamId}/member", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public void updateMember(@PathVariable String teamId, @RequestBody UserEntity user) {

        // Fails if Team ID or User is missing
        if (teamId == null || teamId.isEmpty() || user == null || user.getSlackUsers().size() != 1) {
            log.error("Impossible to register a new member in organization : Team ID is incorrect or body is incomplete");
            throw new BadRequestException();
        }

        // Retrieve full referenced objects
        SlackTeamEntity slackTeam = slackTeamRepository.findByTeamId(teamId);

        // Verify that any of references are not null
        if (slackTeam == null) {
            log.error("Impossible to register a new member in organization : team {} not found", teamId);
            throw new NotFoundException();
        }

        // Verify that Slack Team is associated with an organization
        OrganizationEntity organization = slackTeam.getOrganization();
        if (organization == null) {
            log.error("Impossible to register a new member in organization : no organization is associated with Slack Team");
            throw new NotFoundException();
        }

        user.getSlackUsers().forEach(slackUser -> {

            // Create Slack User if not exists in DB
            SlackUserEntity slackUserInDb = slackUserRepository.findBySlackId(slackUser.getSlackId());
            if (slackUserInDb == null) {
                slackUserInDb = new SlackUserEntity();
                slackUserInDb.setSlackId(slackUser.getSlackId());
                slackUserInDb.setUser(null);
            }
            slackUserInDb.setEmail(slackUser.getEmail());
            slackUserInDb.setSlackTeam(slackTeam);
            slackUserInDb.setImId(slackClientService.openDirectMessageChannel(slackTeam, slackUser.getSlackId()));
            final SlackUserEntity slackUserEntityFinal = slackUserRepository.save(slackUserInDb);

            // Create User if not exists in DB
            UserEntity userInDb = userRepository.findBySlackUsers_Id(slackUserInDb.getId());
            if (userInDb == null) {
                userInDb = userService.findByEmail(slackUserInDb.getEmail());
                if (userInDb == null) {
                    userInDb = UserGenerator.newUser(user);
                }
            }
            userInDb.setEmail(user.getEmail());
            userInDb.setFirstname(user.getFirstname());
            userInDb.setLastname(user.getLastname());
            userInDb.setAvatarUrl(user.getAvatarUrl());
            userInDb.setEnabled(user.getEnabled());
            userInDb.setPassword(StringsCommon.EMPTY_STRING);
            final UserEntity userInDbFinal = userInDb;

            // If the User doesnt have the SlackUser -> Add it
            userInDbFinal.getSlackUsers().stream().filter(userSlackUser -> userSlackUser.getUser().getId().equals(userInDbFinal.getId()))
                    .findAny()
                    .ifPresentOrElse(
                            userSlackUser -> userSlackUser = slackUserEntityFinal,
                            () -> userInDbFinal.getSlackUsers().add(slackUserEntityFinal));

            final UserEntity userWithSlackUser = userRepository.save(userInDbFinal);

            // Complete SlackUser with user saved
            slackUserEntityFinal.setUser(userWithSlackUser);
            final SlackUserEntity slackUserFinal2 = slackUserRepository.save(slackUserEntityFinal);

            if (user.getEnabled()) {

                // If the SlackTeam doesnt have the SlackUser -> Add it
                slackTeam.getSlackUsers().stream().filter(slackTeamUser -> slackTeamUser.getId().equals(slackUserFinal2.getId()))
                        .findAny()
                        .ifPresentOrElse(
                                slackTeamUser -> {
                                },
                                () -> {
                                    slackTeam.getSlackUsers().add(slackUserFinal2);
                                    slackTeamRepository.save(slackTeam);
                                });

                // If the User is not member of organization => Add it
                organization.getMembers().stream().filter(member -> member.getId().equals(userWithSlackUser.getId()))
                        .findAny()
                        .ifPresentOrElse(
                                member -> {
                                },
                                () -> organization.getMembers().add(userWithSlackUser)
                        );
            } else {
                // If the User is disabled, we remove it from the organization
                organization.getMembers().stream().filter(member -> member.getId().equals(userWithSlackUser.getId()))
                        .findAny()
                        .ifPresent(member -> organization.getMembers().remove(member));
            }
            organizationRepository.save(organization);

            // Distribute usable budgets
            Set<BudgetEntity> budgets = budgetRepository.findAllByEndDateGreaterThanAndIsDistributedAndOrganizationId(new Date(), true, organization.getId());
            budgets.forEach(budget -> {
                AccountEntity account = accountService.getByBudgetAndUser(budget.getId(), userWithSlackUser.getId());
                if (account == null) {
                    account = new AccountEntity();
                    account.setAmount(budget.getAmountPerMember());
                    account.setBudget(budget);
                }
                account.setInitialAmount(budget.getAmountPerMember());
                account.setOwner(userWithSlackUser);
                accountService.save(account);
            });

            Map<String, Object> model = new HashMap<>();
            model.put("slackTeamName", slackTeam.getOrganization().getName());
            model.put("URL", webUrl);

            Context context = new Context();
            context.setVariables(model);
            String slackMessage = templateEngine.process("slack/fr/WELCOME_PRIVATE", context);

            slackClientService.postMessage(slackTeam, slackUserInDb.getImId(), slackMessage);
        });
    }

    @Operation(summary = "Vote for a project", description = "Vote for a project via buttons on Slack", tags = {"Slack"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Vote saved", content = @io.swagger.v3.oas.annotations.media.Content(schema = @Schema())),
            @ApiResponse(responseCode = "400", description = "Team ID is incorrect", content = @io.swagger.v3.oas.annotations.media.Content(schema = @Schema())),
            @ApiResponse(responseCode = "403", description = "Principal has not enough privileges", content = @io.swagger.v3.oas.annotations.media.Content(schema = @Schema())),
            @ApiResponse(responseCode = "404", description = "Slack Team or organization not found", content = @io.swagger.v3.oas.annotations.media.Content(schema = @Schema()))
    })
    @PreAuthorize("hasRole('USER')")
    @RequestMapping(value = "/slack/{teamId}/vote", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public void vote(Principal principal, @PathVariable String teamId, @RequestBody SlackVoteModel vote) {

        log.debug("{}", vote);
        // Fails if Team ID is missing
        if (teamId == null ||
                teamId.isEmpty() ||
                vote == null ||
                vote.getProjectId() <= 0L ||
                vote.getSlackUserId() == null ||
                vote.getSlackUserId().isEmpty()) {
            log.error("Impossible to save vote : Team ID is incorrect or body is incomplete");
            throw new BadRequestException();
        }

        // Retrieve full referenced objects
        SlackTeamEntity slackTeam = slackTeamRepository.findByTeamId(teamId);
        ProjectEntity project = projectService.findById(vote.getProjectId());
        SlackUserEntity slackUser = slackUserRepository.findBySlackId(vote.getSlackUserId());

        // Verify that any of references are not null
        if (slackTeam == null || project == null || slackUser == null) {
            log.error("Impossible to save vote : at least one reference is null");
            throw new NotFoundException();
        }

        // Verify that user associated is not null
        UserEntity user = userService.findById(slackUser.getUser().getId());
        if (user == null) {
            log.error("Impossible to save vote : user is null");
            throw new NotFoundException();
        }

        // Vote
        VoteEntity voteEntity = voteService.getUserVote(user.getId(), project.getId());

        if(voteEntity != null && voteEntity.getType().equals(vote.getType())) {
            this.voteService.delete(voteEntity);
            return;
        } else if (voteEntity == null) {
            voteEntity = new VoteEntity();
        }

        // Save project
        voteEntity.setType(vote.getType());
        voteEntity.setProject(project);
        voteEntity.setUser(user);
        this.voteService.save(voteEntity);
    }

}

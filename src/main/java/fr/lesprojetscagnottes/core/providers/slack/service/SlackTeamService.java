package fr.lesprojetscagnottes.core.providers.slack.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import fr.lesprojetscagnottes.core.account.service.AccountService;
import fr.lesprojetscagnottes.core.common.exception.BadRequestException;
import fr.lesprojetscagnottes.core.common.exception.ForbiddenException;
import fr.lesprojetscagnottes.core.common.exception.InternalServerException;
import fr.lesprojetscagnottes.core.common.exception.NotFoundException;
import fr.lesprojetscagnottes.core.common.service.HttpClientService;
import fr.lesprojetscagnottes.core.common.strings.StringGenerator;
import fr.lesprojetscagnottes.core.organization.entity.OrganizationEntity;
import fr.lesprojetscagnottes.core.organization.service.OrganizationService;
import fr.lesprojetscagnottes.core.providers.slack.entity.SlackTeamEntity;
import fr.lesprojetscagnottes.core.providers.slack.entity.SlackUserEntity;
import fr.lesprojetscagnottes.core.providers.slack.model.SlackTeamModel;
import fr.lesprojetscagnottes.core.providers.slack.repository.SlackTeamRepository;
import fr.lesprojetscagnottes.core.user.UserGenerator;
import fr.lesprojetscagnottes.core.user.entity.UserEntity;
import fr.lesprojetscagnottes.core.user.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.thymeleaf.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.Principal;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.Base64;
import java.util.List;

@Slf4j
@Service
public class SlackTeamService {

    @Value("${fr.lesprojetscagnottes.slack.client_id}")
    private String slackClientId;

    @Value("${fr.lesprojetscagnottes.slack.client_secret}")
    private String slackClientSecret;

    private final PasswordEncoder passwordEncoder;

    private final AccountService accountService;

    private final HttpClientService httpClientService;

    private final OrganizationService organizationService;

    private final UserService userService;

    private final SlackClientService slackClientService;

    private final SlackNotificationService slackNotificationService;

    private final SlackUserService slackUserService;

    private final SlackTeamRepository slackTeamRepository;

    @Autowired
    public SlackTeamService(PasswordEncoder passwordEncoder,
                            AccountService accountService,
                            HttpClientService httpClientService,
                            OrganizationService organizationService,
                            UserService userService,
                            SlackClientService slackClientService,
                            SlackNotificationService slackNotificationService,
                            SlackUserService slackUserService,
                            SlackTeamRepository slackTeamRepository) {
        this.passwordEncoder = passwordEncoder;
        this.accountService = accountService;
        this.httpClientService = httpClientService;
        this.organizationService = organizationService;
        this.userService = userService;
        this.slackClientService = slackClientService;
        this.slackNotificationService = slackNotificationService;
        this.slackUserService = slackUserService;
        this.slackTeamRepository = slackTeamRepository;
    }

    public SlackTeamEntity findById(Long id) {
        return slackTeamRepository.findById(id).orElse(null);
    }

    public SlackTeamEntity findByTeamId(String teamId) {
        return slackTeamRepository.findByTeamId(teamId);
    }

    public SlackTeamModel findById(Principal principal, Long id) {

        // Verify that ID is correct
        if(id <= 0) {
            log.error("Impossible to get Slack Team by ID : ID is incorrect");
            throw new BadRequestException();
        }

        // Verify that entity exists
        SlackTeamEntity entity = findById(id);
        if(entity == null) {
            log.error("Impossible to get Slack Team by ID : news not found");
            throw new NotFoundException();
        }

        // If the Slack Team is in an organization, verify that principal is in this organization
        if(entity.getOrganization() != null) {
            Long userLoggedInId = userService.get(principal).getId();
            if(userService.isNotManagerOfOrganization(userLoggedInId, entity.getOrganization().getId()) && userService.isNotAdmin(userLoggedInId)) {
                log.error("Impossible to get Slack Team by ID : principal has not enough privileges");
                throw new ForbiddenException();
            }
        }

        // Transform and return organization
        return SlackTeamModel.fromEntity(entity);
    }

    public SlackTeamEntity findByOrganizationId(Long id) {
        return slackTeamRepository.findByOrganizationId(id);
    }

    public String create(Principal principal, long org_id, String code, String redirect_uri) {

        // Verify that parameters are correct
        if(org_id <= 0 || code == null || code.isEmpty() || redirect_uri == null || redirect_uri.isEmpty()) {
            log.error("Impossible to add Slack workspace to organization : parameters are incorrect");
            throw new BadRequestException();
        }

        // Verify that organization exists
        OrganizationEntity organization = organizationService.findById(org_id);
        if(organization == null) {
            log.error("Impossible to add Slack workspace to organization : organization {} not found", org_id);
            throw new NotFoundException();
        }

        // Verify if principal has correct privileges
        Long userLoggedInId = userService.get(principal).getId();
        if(userService.isNotManagerOfOrganization(userLoggedInId, org_id) && userService.isNotAdmin(userLoggedInId)) {
            log.error("Impossible to add Slack workspace to organization : principal is not owner of organization {}", org_id);
            throw new ForbiddenException();
        }

        // Prepare Slack request
        String url = "https://slack.com/api/oauth.v2.access?client_id=" + slackClientId + "&client_secret=" + slackClientSecret + "&code=" + code + "&redirect_uri=" + redirect_uri;
        String body = "{\"code\":\"" + code + "\", \"redirect_uri\":\"" + redirect_uri + "\"}";
        log.debug("POST " + url);
        log.debug("body : " + body);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMinutes(1))
                .header("Content-Type", "application/json; charset=utf-8")
                .header("Authorization", basicAuth())
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response;

        // Send Slack request and process response
        try {
            response = httpClientService.getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            log.debug("response : " + response.body());
            Gson gson = new Gson();
            JsonObject json = gson.fromJson(response.body(), JsonObject.class);
            if (json.get("ok") != null && json.get("ok").getAsBoolean()) {
                SlackTeamEntity slackTeam;
                if(organization.getSlackTeam() != null) {
                    slackTeam = organization.getSlackTeam();
                } else {
                    slackTeam = new SlackTeamEntity();
                }
                slackTeam.setAccessToken(json.get("authed_user").getAsJsonObject().get("access_token").getAsString());
                slackTeam.setTeamId(json.get("team").getAsJsonObject().get("id").getAsString());
                slackTeam.setTeamName(json.get("team").getAsJsonObject().get("name").getAsString());
                slackTeam.setBotAccessToken(json.get("access_token").getAsString());
                slackTeam.setBotUserId(json.get("bot_user_id").getAsString());
                slackTeam.setOrganization(organization);
                slackTeam.setBotId(slackClientService.getBotId(slackTeam));
                slackTeamRepository.save(slackTeam);
            } else {
                log.error("Impossible to add Slack workspace to organization : {}", json.get("error").toString());
                throw new InternalServerException();
            }
            return response.body();

        } catch (IOException | InterruptedException e) {
            log.error("Impossible to add Slack workspace to organization");
            log.error(e.getMessage(), e);
        }
        return null;
    }

    public SlackTeamModel update(Principal principal, SlackTeamModel slackTeamModel) {
        // Verify that ID is correct
        // Fails if any of references are null
        if (slackTeamModel == null || StringUtils.isEmpty(slackTeamModel.getTeamId())) {
            if (slackTeamModel != null) {
                log.error("Impossible to update project {} : some references are missing", slackTeamModel.getId());
            } else {
                log.error("Impossible to update a null project");
            }
            throw new BadRequestException();
        }
        // Verify that entity exists
        SlackTeamEntity entity = slackTeamRepository.findByTeamId(slackTeamModel.getTeamId());
        if (entity == null) {
            log.error("Impossible to get Slack team by ID : Slack team not found");
            throw new NotFoundException();
        }

        // If user is not admin => organization where principal is member
        // Else => all organizations
        Long userLoggedInId = userService.get(principal).getId();
        if (userService.isNotOwnerOfOrganization(userLoggedInId, entity.getOrganization().getId()) && userService.isNotAdmin(userLoggedInId)) {
            log.error("Impossible to get Slack team by ID : principal has not enough privileges");
            throw new ForbiddenException();
        }

        entity.setPublicationChannelId(slackTeamModel.getPublicationChannelId());
        entity.setTeamName(slackTeamModel.getTeamName());
        // Transform and return organization
        return SlackTeamModel.fromEntity(slackTeamRepository.save(entity));
    }

    public void delete(Principal principal, long id) {

        // Verify that parameters are correct
        if(id <= 0) {
            log.error("Cannot delete Slack Team : ID is incorrect");
            throw new BadRequestException();
        }

        // Verify that Slack Team exists
        SlackTeamEntity entity = findById(id);
        if(entity == null) {
            log.error("Cannot delete Slack Team : Slack Team {} not found", id);
            throw new NotFoundException();
        }

        // Verify if principal has correct privileges
        if(entity.getOrganization() != null) {
            Long userLoggedInId = userService.get(principal).getId();
            if(userService.isNotOwnerOfOrganization(userLoggedInId, entity.getOrganization().getId()) && userService.isNotAdmin(userLoggedInId)) {
                log.error("Cannot delete Slack Team : principal is not owner of organization {}", entity.getOrganization().getId());
                throw new ForbiddenException();
            }
        }

        // Delete Slack Team
        slackNotificationService.deleteAllBySlackTeamId(id);
        slackUserService.deleteAllBySlackTeamId(id);
        slackTeamRepository.deleteById(id);
    }

    public String sync(Principal principal, long id) {

        // Verify that parameters are correct
        if(id <= 0) {
            log.error("Cannot sync Slack data with organization : ID is incorrect");
            throw new BadRequestException();
        }

        // Verify that Slack Team exists
        SlackTeamEntity entity = findById(id);
        if(entity == null) {
            log.error("Cannot sync Slack data with organization : Slack Team {} not found", id);
            throw new NotFoundException();
        }

        // Verify that Slack Team has an organization associated
        if(entity.getOrganization() == null) {
            log.error("Cannot sync Slack data with organization : No organization associated");
            throw new BadRequestException();
        }
        OrganizationEntity organization = organizationService.findById(entity.getOrganization().getId());

        // Verify if principal has correct privileges
        Long userLoggedInId = userService.get(principal).getId();
        if(userService.isNotManagerOfOrganization(userLoggedInId, organization.getId()) && userService.isNotAdmin(userLoggedInId)) {
            log.error("Impossible to sync Slack data with organization : principal is not manager of organization {}", id);
            throw new ForbiddenException();
        }

        // Get Slack users
        List<SlackUserEntity> slackUsers = slackClientService.listUsers(entity);
        log.debug("Retrieved {} Slack Users", slackUsers.size());

        // For each Slack user, retrieve its data
        UserEntity user;
        long delay;
        long tsAfterOpenIm = (new Timestamp(System.currentTimeMillis())).getTime();
        for(SlackUserEntity slackUser : slackUsers) {

            // Sync with existing Slack user
            SlackUserEntity slackUserEditted = slackUserService.findBySlackId(slackUser.getSlackId());
            if(slackUserEditted != null) {
                slackUserEditted.setName(slackUser.getName());
                slackUserEditted.setImage_192(slackUser.getImage_192());
                slackUserEditted.setEmail(slackUser.getEmail());
            } else {
                slackUserEditted = slackUser;
            }
            slackUserEditted.setSlackTeam(entity);

            log.debug("Syncing Slack User {} with local DB", slackUser.getSlackId());

            // Slack conversations.open method is Web API Tier 3 (50+ per minute) so wait 1200ms
            delay = (new Timestamp(System.currentTimeMillis())).getTime() - tsAfterOpenIm;
            if(delay > 1200) {
                delay = 1200;
            }
            try {
                Thread.sleep(1200 - delay);
            } catch (InterruptedException e) {
                log.error("Thread sleep was interrupted", e);
            }

            // Open IM with Slack user
            slackUserEditted.setImId(slackClientService.openDirectMessageChannel(entity, slackUserEditted.getSlackId()));
            tsAfterOpenIm = (new Timestamp(System.currentTimeMillis())).getTime();

            // Sync with user
            user = userService.findByEmail(slackUser.getEmail());
            if(user == null) {
                user = UserGenerator.newUser(new UserEntity());
                user.setCreatedBy("Slack Sync");
                user.setFirstname(slackUserEditted.getName());
                user.setUsername(slackUserEditted.getEmail());
                user.setEmail(slackUserEditted.getEmail());
                user.setAvatarUrl(slackUserEditted.getImage_192());
                user.setPassword(passwordEncoder.encode(StringGenerator.randomString()));
            }
            user.setUpdatedBy("Slack Sync");
            user.setEnabled(!(slackUser.getDeleted() || slackUser.getIsRestricted()));

            // Save data
            final UserEntity userInDb = userService.save(user);
            slackUserEditted.setUser(userInDb);
            slackUserService.save(slackUserEditted);

            // Add or remove user from organization according to enable parameter
            if(userInDb.getEnabled()) {
                organization.getMembers().stream().filter(member -> member.getId().equals(userInDb.getId()))
                        .findAny()
                        .ifPresentOrElse(
                                member -> member = userInDb,
                                () -> organization.getMembers().add(userInDb)
                        );
            } else {
                organization.getMembers().stream().filter(member -> member.getId().equals(userInDb.getId()))
                        .findAny()
                        .ifPresent(member -> organization.getMembers().remove(member));
            }
            organizationService.save(organization);

            // Create accounts onboarding users
            accountService.createUserAccountsForUsableBudgets(userInDb, organization.getId());
        }
        return null;
    }

    private String basicAuth() {
        return "Basic " + Base64.getEncoder().encodeToString((slackClientId + ":" + slackClientSecret).getBytes());
    }

}

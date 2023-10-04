package fr.lesprojetscagnottes.core.campaign.service;

import fr.lesprojetscagnottes.core.budget.entity.BudgetEntity;
import fr.lesprojetscagnottes.core.budget.service.BudgetService;
import fr.lesprojetscagnottes.core.campaign.entity.CampaignEntity;
import fr.lesprojetscagnottes.core.campaign.model.CampaignModel;
import fr.lesprojetscagnottes.core.campaign.model.CampaignStatus;
import fr.lesprojetscagnottes.core.campaign.repository.CampaignRepository;
import fr.lesprojetscagnottes.core.campaign.scheduler.CampaignScheduler;
import fr.lesprojetscagnottes.core.common.date.DateUtils;
import fr.lesprojetscagnottes.core.common.exception.BadRequestException;
import fr.lesprojetscagnottes.core.common.exception.ForbiddenException;
import fr.lesprojetscagnottes.core.common.exception.NotFoundException;
import fr.lesprojetscagnottes.core.common.pagination.DataPage;
import fr.lesprojetscagnottes.core.donation.entity.DonationEntity;
import fr.lesprojetscagnottes.core.donation.model.DonationModel;
import fr.lesprojetscagnottes.core.donation.service.DonationService;
import fr.lesprojetscagnottes.core.notification.model.NotificationName;
import fr.lesprojetscagnottes.core.notification.service.NotificationService;
import fr.lesprojetscagnottes.core.project.entity.ProjectEntity;
import fr.lesprojetscagnottes.core.project.model.ProjectStatus;
import fr.lesprojetscagnottes.core.project.service.ProjectService;
import fr.lesprojetscagnottes.core.user.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class CampaignService {

    @Value("${fr.lesprojetscagnottes.web.url}")
    private String webUrl;

    private final BudgetService budgetService;

    private final DonationService donationService;

    private final NotificationService notificationService;

    private final ProjectService projectService;

    private final UserService userService;

    private final CampaignRepository campaignRepository;

    private final CampaignScheduler campaignScheduler;

    @Autowired
    public CampaignService(BudgetService budgetService,
                           DonationService donationService,
                           NotificationService notificationService,
                           ProjectService projectService,
                           UserService userService,
                           CampaignRepository campaignRepository,
                           CampaignScheduler campaignScheduler) {
        this.budgetService = budgetService;
        this.donationService = donationService;
        this.notificationService = notificationService;
        this.projectService = projectService;
        this.userService = userService;
        this.campaignRepository = campaignRepository;
        this.campaignScheduler = campaignScheduler;
    }

    public CampaignModel findById(Principal principal, Long id) {
        // Verify that ID is correct
        if (id <= 0) {
            log.error("Impossible to get campaign by ID : ID is incorrect");
            throw new BadRequestException();
        }

        // Verify that entity exists
        CampaignEntity entity = campaignRepository.findById(id).orElse(null);
        if (entity == null) {
            log.error("Impossible to get campaign by ID : campaign not found");
            throw new NotFoundException();
        }

        // Verify that principal is in campaign's project organization
        Long userLoggedInId = userService.get(principal).getId();
        if (userService.isNotAdmin(userLoggedInId) && !userService.isMemberOfOrganization(userLoggedInId, entity.getProject().getOrganization().getId())) {
            log.error("Impossible to get campaign by ID : principal has not enough privileges");
            throw new ForbiddenException();
        }

        // Transform and return organization
        return CampaignModel.fromEntity(entity);
    }

    public List<CampaignModel> getByIds(Principal principal, Set<Long> ids) {
        Long userLoggedInId = userService.get(principal).getId();
        boolean userLoggedIn_isNotAdmin = userService.isNotAdmin(userLoggedInId);
        List<CampaignModel> models = new ArrayList<>();

        for (Long id : ids) {

            // Retrieve full referenced objects
            CampaignEntity entity = campaignRepository.findById(id).orElse(null);
            if (entity == null) {
                log.error("Impossible to get campaign {} : it doesn't exist", id);
                continue;
            }

            // Verify that principal is in campaign's project organization
            if (userLoggedIn_isNotAdmin && !userService.isMemberOfOrganization(userLoggedInId, entity.getProject().getOrganization().getId())) {
                log.error("Impossible to get campaign {} : principal {} is not in organizations of project's campaign", id, userLoggedInId);
                continue;
            }

            // Add the user to returned list
            models.add(CampaignModel.fromEntity(entity));
        }

        Comparator<CampaignModel> compareByFundingDeadline = Comparator.comparing(CampaignModel::getFundingDeadline);
        models.sort(compareByFundingDeadline.reversed());

        return models;
    }

    public CampaignModel create(Principal principal, @RequestBody CampaignModel campaign) {

        // Fails if any of references are null
        if (campaign == null
                || campaign.getProject() == null || campaign.getProject().getId() <= 0
                || campaign.getBudget() == null || campaign.getBudget().getId() <= 0) {
            if (campaign != null) {
                log.error("Impossible to create campaign : some references are missing");
            } else {
                log.error("Impossible to create a null campaign");
            }
            throw new BadRequestException();
        }

        // Retrieve full referenced objects
        ProjectEntity project = projectService.findById(campaign.getProject().getId());
        BudgetEntity budget = budgetService.findById(campaign.getBudget().getId());

        // Fails if any of references are null
        if (project == null || budget == null) {
            log.error("Impossible to create campaign : one or more reference(s) doesn't exist");
            throw new NotFoundException();
        }

        // Verify that principal is project leader
        Long userLoggedInId = userService.get(principal).getId();
        if (!userLoggedInId.equals(project.getLeader().getId()) && userService.isNotAdmin(userLoggedInId)) {
            log.error("Impossible to create campaign : principal {} is not project leader", userLoggedInId);
            throw new ForbiddenException();
        }

        // Verify that budgets are usable
        Set<BudgetEntity> budgetsUsable = budgetService.findAllUsableBudgetsInOrganization(new Date(), project.getOrganization().getId());
        if (!budgetsUsable.contains(budget)) {
            log.error("Impossible to create campaign : budget is not usable");
            throw new BadRequestException();
        }

        // Verify that funding deadline is coherent
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime fundingDeadline = DateUtils.asLocalDateTime(campaign.getFundingDeadline());
        LocalDateTime nowPlus3Months = now.plusMonths(3);
        if (fundingDeadline.isBefore(now) || fundingDeadline.isAfter(nowPlus3Months)) {
            log.error("Impossible to create campaign : funding deadline is incorrect");
            throw new BadRequestException();
        }

        // Verify that there are enough teammates
        // TODO: Define this value on the budget entity
        if (project.getPeopleGivingTime().size() < 3) {
            log.error("Impossible to create campaign : not enough people in teammates");
            throw new BadRequestException();
        }

        // Save campaign
        CampaignEntity campaignToSave = new CampaignEntity();
        campaignToSave.setStatus(CampaignStatus.IN_PROGRESS);
        campaignToSave.setDonationsRequired(campaign.getDonationsRequired());
        campaignToSave.setDaysRequired(campaign.getDaysRequired());
        campaignToSave.setHoursRequired(campaign.getHoursRequired());
        campaignToSave.setTotalRequired(campaign.getDonationsRequired() + budget.getCostOfDay() * campaign.getDaysRequired() + budget.getCostOfHour() * campaign.getHoursRequired());
        campaignToSave.setFundingDeadline(campaign.getFundingDeadline());
        campaignToSave.setTotalDonations(0f);
        campaignToSave.setProject(project);
        campaignToSave.setBudget(budget);
        final CampaignEntity campaignFinal = campaignRepository.save(campaignToSave);

        // If project is in draft, we force its publication
        if (project.getStatus().equals(ProjectStatus.DRAFT)) {
            project.setStatus(ProjectStatus.IN_PROGRESS);
            projectService.save(principal, project);
        }

        Map<String, Object> model = new HashMap<>();

        // Send a notification if project is in progress state
        if (project.getStatus().equals(ProjectStatus.IN_PROGRESS)) {
            model.put("project_title", project.getTitle());
            model.put("project_url", webUrl + "/projects/" + project.getId());
            model.put("profile_url", webUrl + "/profile");
            notificationService.create(NotificationName.CAMPAIGN_STARTED, model, project.getOrganization().getId());
        }
        campaign.setId(campaignFinal.getId());
        return campaign;
    }

    public CampaignModel update(Principal principal, CampaignModel campaignModel) {
        // Fails if any of references are null
        if (campaignModel == null
                || campaignModel.getProject() == null || campaignModel.getProject().getId() <= 0
                || campaignModel.getBudget() == null || campaignModel.getBudget().getId() <= 0) {
            if (campaignModel != null) {
                log.error("Impossible to update campaign : some references are missing");
            } else {
                log.error("Impossible to update a null campaign");
            }
            throw new BadRequestException();
        }

        // Retrieve full referenced objects
        ProjectEntity project = projectService.findById(campaignModel.getProject().getId());
        BudgetEntity budget = budgetService.findById(campaignModel.getBudget().getId());
        CampaignEntity campaign = campaignRepository.findById(campaignModel.getId()).orElse(null);

        // Fails if any of references are null
        if (project == null || budget == null || campaign == null) {
            log.error("Impossible to update campaign \"{}\" : one or more reference(s) doesn't exist", campaignModel.getId());
            throw new NotFoundException();
        }

        // Verify that principal is project leader
        Long userLoggedInId = userService.get(principal).getId();
        if (!userLoggedInId.equals(project.getLeader().getId()) && userService.isNotAdmin(userLoggedInId)) {
            log.error("Impossible to update campaign \"{}\" : principal {} is not project leader", campaignModel.getId(), userLoggedInId);
            throw new ForbiddenException();
        }

        // Verify that budgets are usable
        Set<BudgetEntity> budgetsUsable = budgetService.findAllUsableBudgetsInOrganization(new Date(), project.getOrganization().getId());
        if (!budgetsUsable.contains(budget)) {
            log.error("Impossible to update campaign : budgets are not all usable");
            throw new ForbiddenException();
        }

        // Save campaign
        if (campaignModel.getDonationsRequired() > campaign.getDonationsRequired()) {
            campaign.setDonationsRequired(campaignModel.getDonationsRequired());
        }
        if (budget.getCanFinanceTime()) {
            if (campaignModel.getDaysRequired() > campaign.getDaysRequired()) {
                campaign.setDaysRequired(campaignModel.getDaysRequired());
            }
            if (campaignModel.getHoursRequired() > campaign.getHoursRequired()) {
                campaign.setHoursRequired(campaignModel.getHoursRequired());
            }
        }
        campaign.setTotalRequired(campaign.getDonationsRequired() + budget.getCostOfDay() * campaign.getDaysRequired() + budget.getCostOfHour() * campaign.getHoursRequired());

        return CampaignModel.fromEntity(campaignRepository.save(campaign));
    }

    public DataPage<DonationModel> getDonations(Principal principal, long campaignId, int offset, int limit) {
        // Fails if campaign ID is missing
        if (campaignId <= 0) {
            log.error("Impossible to get donations by campaign ID : Campaign ID is incorrect");
            throw new BadRequestException();
        }

        // Retrieve full referenced objects
        CampaignEntity campaign = campaignRepository.findById(campaignId).orElse(null);

        // Verify that any of references are not null
        if (campaign == null) {
            log.error("Impossible to get donations by campaign ID : campaign {} not found", campaignId);
            throw new NotFoundException();
        }

        // Verify that principal is in one organization of the campaign
        Long userLoggedInId = userService.get(principal).getId();
        Long organizationId = campaign.getProject().getOrganization().getId();
        if (userService.isNotAdmin(userLoggedInId) && !userService.isMemberOfOrganization(userLoggedInId, organizationId)) {
            log.error("Impossible to get donations by campaign ID : user {} is not member of organization {}", userLoggedInId, organizationId);
            throw new ForbiddenException();
        }

        // Get and transform donations
        Page<DonationEntity> entities = donationService.findByCampaign_idOrderByIdAsc(campaignId, PageRequest.of(offset, limit, Sort.by("id")));
        DataPage<DonationModel> models = new DataPage<>(entities);
        entities.getContent().forEach(entity -> models.getContent().add(DonationModel.fromEntity(entity)));
        return models;
    }

    public void validate() {
        campaignScheduler.processCampaignFundingDeadlines();
    }

    public void notifyCampaignsAlmostFinished() {
        campaignScheduler.notifyCampaignsAlmostFinished();
    }

    public void notifyCampaignStatus(long id) {
        CampaignEntity campaign = campaignRepository.findById(id).orElse(null);
        if (campaign == null) {
            throw new NotFoundException();
        } else {
            long diffInMillies = Math.abs(campaign.getFundingDeadline().getTime() - new Date().getTime());
            long diff = TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS) + 1;
            campaignScheduler.notifyCampaignStatus(campaign, diff);
        }
    }
}

package fr.lesprojetscagnottes.core.budget.service;

import fr.lesprojetscagnottes.core.budget.entity.BudgetEntity;
import fr.lesprojetscagnottes.core.budget.model.BudgetModel;
import fr.lesprojetscagnottes.core.budget.repository.BudgetRepository;
import fr.lesprojetscagnottes.core.common.exception.BadRequestException;
import fr.lesprojetscagnottes.core.common.exception.ForbiddenException;
import fr.lesprojetscagnottes.core.common.exception.NotFoundException;
import fr.lesprojetscagnottes.core.content.entity.ContentEntity;
import fr.lesprojetscagnottes.core.content.service.ContentService;
import fr.lesprojetscagnottes.core.organization.entity.OrganizationEntity;
import fr.lesprojetscagnottes.core.organization.service.OrganizationService;
import fr.lesprojetscagnottes.core.user.entity.UserEntity;
import fr.lesprojetscagnottes.core.user.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.Date;
import java.util.Set;

@Slf4j
@Service
public class BudgetService {

    @Autowired
    private ContentService contentService;

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private UserService userService;

    @Autowired
    private BudgetRepository budgetRepository;

    public BudgetEntity findById(Long id) {
        return budgetRepository.findById(id).orElse(null);
    }

    public Set<BudgetEntity> findAllByEndDateGreaterThanAndIsDistributedAndOrganizationId(Date date, boolean isDistributed, Long organizationId) {
        // FIXME
        return budgetRepository.findAllByEndDateGreaterThanAndIsDistributedAndOrganizationId(date, isDistributed, organizationId);
    }

    public Set<BudgetEntity> findAllUsableBudgetsInOrganization(Date today, Long organization) {
        return budgetRepository.findAllUsableBudgetsInOrganization(today, organization);
    }

    public BudgetModel save(Principal principal, BudgetModel budgetModel) {

        // Fails if any of references are null
        if(budgetModel == null || budgetModel.getOrganization() == null || budgetModel.getSponsor() == null || budgetModel.getRules() == null
                || budgetModel.getOrganization().getId() == null || budgetModel.getSponsor().getId() == null || budgetModel.getRules().getId() == null) {
            if(budgetModel != null ) {
                log.error("Impossible to save budget {} : some references are missing", budgetModel.getName());
            } else {
                log.error("Impossible to save a null budget");
            }
            throw new BadRequestException();
        }

        // Retrieve full referenced objects
        OrganizationEntity organization = organizationService.findById(budgetModel.getOrganization().getId());
        UserEntity sponsor = userService.findById(budgetModel.getSponsor().getId());
        ContentEntity rules = contentService.findById(budgetModel.getRules().getId());

        // Fails if any of references are null
        if(organization == null || sponsor == null || rules == null) {
            log.error("Impossible to save budget {} : one or more reference(s) doesn't exist", budgetModel.getName());
            throw new NotFoundException();
        }

        // Test that user logged in has correct rights
        UserEntity userLoggedIn = userService.get(principal);
        if(userService.isNotSponsorOfOrganization(userLoggedIn.getId(), organization.getId()) && userService.isNotAdmin(userLoggedIn.getId())) {
            log.error("Impossible to save budget {} : principal {} has not enough privileges", budgetModel.getName(), userLoggedIn.getId());
            throw new ForbiddenException();
        }

        // Test that sponsor has correct rights
        Long sponsorId = budgetModel.getSponsor().getId();
        if(userService.isNotSponsorOfOrganization(sponsorId, organization.getId())) {
            log.error("Impossible to save budget {} : sponsor {} has not enough privileges", budgetModel.getName(), sponsor.getId());
            throw new ForbiddenException();
        }

        // Retrieve budget if ID is provided
        BudgetEntity budgetToSave;
        if(budgetModel.getId() > 0)  {
            budgetToSave = findById(budgetModel.getId());
            if(budgetToSave == null) {
                log.error("Impossible to save budget {} : it does not exist", budgetModel.getName());
                throw new NotFoundException();
            }
        } else {
            budgetToSave = new BudgetEntity();
        }

        // Pass new values
        budgetToSave.setName(budgetModel.getName());
        budgetToSave.setAmountPerMember(budgetModel.getAmountPerMember());
        budgetToSave.setCanFinanceTime(budgetModel.getCanFinanceTime());
        budgetToSave.setCostOfDay(budgetModel.getCostOfDay());
        budgetToSave.setCostOfHour(budgetModel.getCostOfHour());
        budgetToSave.setStartDate(budgetModel.getStartDate());
        budgetToSave.setEndDate(budgetModel.getEndDate());
        budgetToSave.setIsDistributed(budgetModel.getIsDistributed());
        budgetToSave.setOrganization(organization);
        budgetToSave.setSponsor(sponsor);
        budgetToSave.setRules(rules);

        // Save
        return BudgetModel.fromEntity(budgetRepository.save(budgetToSave));
    }

}

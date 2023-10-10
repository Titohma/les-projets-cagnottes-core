package fr.lesprojetscagnottes.core.budget.controller;

import fr.lesprojetscagnottes.core.account.entity.AccountEntity;
import fr.lesprojetscagnottes.core.account.model.AccountModel;
import fr.lesprojetscagnottes.core.account.repository.AccountRepository;
import fr.lesprojetscagnottes.core.account.service.AccountService;
import fr.lesprojetscagnottes.core.budget.entity.BudgetEntity;
import fr.lesprojetscagnottes.core.budget.model.BudgetModel;
import fr.lesprojetscagnottes.core.budget.repository.BudgetRepository;
import fr.lesprojetscagnottes.core.budget.service.BudgetService;
import fr.lesprojetscagnottes.core.campaign.entity.CampaignEntity;
import fr.lesprojetscagnottes.core.campaign.model.CampaignModel;
import fr.lesprojetscagnottes.core.campaign.repository.CampaignRepository;
import fr.lesprojetscagnottes.core.common.exception.BadRequestException;
import fr.lesprojetscagnottes.core.common.exception.ForbiddenException;
import fr.lesprojetscagnottes.core.common.exception.NotFoundException;
import fr.lesprojetscagnottes.core.common.pagination.DataPage;
import fr.lesprojetscagnottes.core.content.repository.ContentRepository;
import fr.lesprojetscagnottes.core.organization.entity.OrganizationEntity;
import fr.lesprojetscagnottes.core.organization.repository.OrganizationRepository;
import fr.lesprojetscagnottes.core.user.entity.UserEntity;
import fr.lesprojetscagnottes.core.user.model.UserModel;
import fr.lesprojetscagnottes.core.user.repository.UserRepository;
import fr.lesprojetscagnottes.core.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.LinkedHashSet;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/api")
@Tag(name = "Budgets", description = "The Budgets API")
public class BudgetController {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private ContentRepository contentRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountService accountService;

    @Autowired
    private BudgetService budgetService;

    @Autowired
    private UserService userService;

    @Operation(summary = "Get list of budgets by a list of IDs", description = "Find a list of budgets by a list of IDs", tags = { "Budgets" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Return the budgets", content = @Content(array = @ArraySchema(schema = @Schema(implementation = BudgetModel.class)))),
            @ApiResponse(responseCode = "400", description = "ID is incorrect", content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "403", description = "Principal has not enough privileges", content = @Content(schema = @Schema()))
    })
    @PreAuthorize("hasRole('USER')")
    @RequestMapping(value = "/budget", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE, params = {"ids"})
    public Set<BudgetModel> getByIds(Principal principal, @RequestParam("ids") Set<Long> ids) {

        Long userLoggedInId = userService.get(principal).getId();
        boolean userLoggedIn_isNotAdmin = userService.isNotAdmin(userLoggedInId);
        Set<OrganizationEntity> userLoggedInOrganizations = organizationRepository.findAllByMembers_Id(userLoggedInId);
        Set<BudgetModel> models = new LinkedHashSet<>();

        for(Long id : ids) {

            // Retrieve full referenced objects
            BudgetEntity budget = budgetRepository.findById(id).orElse(null);
            if(budget == null) {
                log.error("Impossible to get budget {} : it doesn't exist", id);
                continue;
            }

            // Verify that principal share an organization with the user
            if(!userLoggedInOrganizations.contains(budget.getOrganization()) && userLoggedIn_isNotAdmin) {
                log.error("Impossible to get budget {} : principal {} is not in its organization", id, userLoggedInId);
                continue;
            }

            // Add the user to returned list
            models.add(BudgetModel.fromEntity(budget));
        }

        return models;
    }

    @Operation(summary = "Get accounts using a budget", description = "Get accounts using a budget", tags = { "Budgets" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Returns corresponding accounts", content = @Content(schema = @Schema(implementation = DataPage.class))),
            @ApiResponse(responseCode = "400", description = "Budget ID is incorrect", content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "403", description = "Principal has not enough privileges", content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "404", description = "Budget not found", content = @Content(schema = @Schema()))
    })
    @PreAuthorize("hasRole('USER')")
    @RequestMapping(value = "/budget/{id}/accounts", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE, params = {"offset", "limit"})
    public DataPage<AccountModel> getAccounts(Principal principal, @PathVariable("id") Long id, @RequestParam("offset") int offset, @RequestParam("limit") int limit) {

        // Fails if budget ID is missing
        if(id <= 0) {
            log.error("Impossible to get accounts by budget ID : budget ID is incorrect");
            throw new BadRequestException();
        }

        // Verify that principal is in one organization of the campaign
        Long userLoggedInId = userService.get(principal).getId();
        if(budgetRepository.findAllByUserAndId(userLoggedInId, id).isEmpty() && userService.isNotAdmin(userLoggedInId)) {
            log.error("Impossible to get accounts by budget ID : user {} has not enough privileges", userLoggedInId);
            throw new ForbiddenException();
        }

        // Retrieve full referenced objects
        BudgetEntity budget = budgetRepository.findById(id).orElse(null);

        // Verify that any of references are not null
        if(budget == null) {
            log.error("Impossible to get accounts by budget ID : budget {} not found", id);
            throw new NotFoundException();
        }

        // Get and transform donations
        Page<AccountEntity> entities = accountRepository.findAllByBudgetIdOrderByUser(id, PageRequest.of(offset, limit, Sort.by("u.firstname").ascending().and(Sort.by("u.lastname").ascending())));
        DataPage<AccountModel> models = new DataPage<>(entities);
        entities.getContent().forEach(entity -> models.getContent().add(AccountModel.fromEntity(entity)));
        return models;
    }

    @Operation(summary = "Get users of accounts using a budget", description = "Get users of accounts using a budget", tags = { "Budgets" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Returns corresponding users", content = @Content(schema = @Schema(implementation = DataPage.class))),
            @ApiResponse(responseCode = "400", description = "Budget ID is incorrect", content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "403", description = "Principal has not enough privileges", content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "404", description = "Budget not found", content = @Content(schema = @Schema()))
    })
    @PreAuthorize("hasRole('USER')")
    @RequestMapping(value = "/budget/{id}/users", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Set<UserModel> getUsers(Principal principal, @PathVariable("id") Long id) {

        // Fails if budget ID is missing
        if(id <= 0) {
            log.error("Impossible to get users of accounts by budget ID : budget ID is incorrect");
            throw new BadRequestException();
        }

        // Verify that principal is in one organization of the campaign
        Long userLoggedInId = userService.get(principal).getId();
        if(budgetRepository.findAllByUserAndId(userLoggedInId, id).isEmpty() && userService.isNotAdmin(userLoggedInId)) {
            log.error("Impossible to get users of accounts by budget ID : user {} has not enough privileges", userLoggedInId);
            throw new ForbiddenException();
        }

        // Retrieve full referenced objects
        BudgetEntity budget = budgetRepository.findById(id).orElse(null);

        // Verify that any of references are not null
        if(budget == null) {
            log.error("Impossible to get users of accounts by budget ID : budget {} not found", id);
            throw new NotFoundException();
        }

        // Get and transform donations
        Set<UserEntity> entities = userRepository.findAllByBudgetId(id);
        Set<UserModel> models = new LinkedHashSet<>();
        entities.forEach(entity -> models.add(UserModel.fromEntity(entity)));
        return models;
    }

    @Operation(summary = "Get campaigns using a budget", description = "Get campaigns using a budget", tags = { "Budgets" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Returns corresponding campaigns", content = @Content(schema = @Schema(implementation = DataPage.class))),
            @ApiResponse(responseCode = "400", description = "Budget ID is incorrect", content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "403", description = "Principal has not enough privileges", content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "404", description = "Budget not found", content = @Content(schema = @Schema()))
    })
    @PreAuthorize("hasRole('USER')")
    @RequestMapping(value = "/budget/{id}/campaigns", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE, params = {"offset", "limit"})
    public DataPage<CampaignModel> getCampaigns(Principal principal, @PathVariable("id") Long id, @RequestParam("offset") int offset, @RequestParam("limit") int limit) {

        // Fails if budget ID is missing
        if(id <= 0) {
            log.error("Impossible to get campaigns by budget ID : budget ID is incorrect");
            throw new BadRequestException();
        }

        // Verify that principal is in one organization of the campaign
        Long userLoggedInId = userService.get(principal).getId();
        if(budgetRepository.findAllByUserAndId(userLoggedInId, id).isEmpty() && userService.isNotAdmin(userLoggedInId)) {
            log.error("Impossible to get campaigns by budget ID : user {} has not enough privileges", userLoggedInId);
            throw new ForbiddenException();
        }

        // Retrieve full referenced objects
        BudgetEntity budget = budgetRepository.findById(id).orElse(null);

        // Verify that any of references are not null
        if(budget == null) {
            log.error("Impossible to get campaigns by budget ID : budget {} not found", id);
            throw new NotFoundException();
        }

        // Get and transform donations
        Page<CampaignEntity> entities = campaignRepository.findByBudgetId(id, PageRequest.of(offset, limit, Sort.by("id").ascending()));
        DataPage<CampaignModel> models = new DataPage<>(entities);
        entities.getContent().forEach(entity -> models.getContent().add(CampaignModel.fromEntity(entity)));
        return models;
    }

    @Operation(summary = "Create a budget", description = "Create a budget", tags = { "Budgets" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Budget created", content = @Content(schema = @Schema(implementation = BudgetModel.class))),
            @ApiResponse(responseCode = "400", description = "Some references are missing", content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "403", description = "Some references doesn't exist", content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "404", description = "Principal or sponsor has not enough privileges", content = @Content(schema = @Schema()))
    })
    @PreAuthorize("hasRole('USER')")
    @RequestMapping(value = "/budget", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public BudgetModel create(Principal principal, @RequestBody BudgetModel budget) {
        return budgetService.save(principal, budget);
    }

    @Operation(summary = "Save a budget", description = "Save a budget", tags = { "Budgets" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Budget saved", content = @Content(schema = @Schema(implementation = BudgetModel.class))),
            @ApiResponse(responseCode = "200", description = "Budget saved", content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "400", description = "Body is missing", content = @Content(schema = @Schema()))
    })
    @PreAuthorize("hasRole('USER')")
    @RequestMapping(value = "/budget", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public BudgetModel save(Principal principal, @RequestBody BudgetModel budget) {
        return budgetService.save(principal, budget);
    }

    @Operation(summary = "Distribute a budget", description = "Distribute a budget between members of organization", tags = { "Budgets" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Budget distributed", content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "400", description = "ID is missing", content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "403", description = "Principal has not enough privileges", content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "404", description = "Budget not found", content = @Content(schema = @Schema()))
    })
    @RequestMapping(value = "/budget/{id}/distribute", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @PreAuthorize("hasRole('USER')")
    public void distribute(Principal principal, @PathVariable("id") Long id) {

        // Fails if any of references are null
        if(id <= 0) {
            log.error("Impossible to distribute budget : ID is missing");
            throw new BadRequestException();
        }

        // Retrieve full referenced objects
        BudgetEntity budget = budgetRepository.findById(id).orElse(null);
        if(budget == null) {
            log.error("Impossible to distribute budget {} : budget not found", id);
            throw new NotFoundException();
        }

        // Verify that principal is member of organization
        Long userLoggedInId = userService.get(principal).getId();
        Long organizationId = budget.getOrganization().getId();
        if(userService.isNotSponsorOfOrganization(userLoggedInId, organizationId) && userService.isNotAdmin(userLoggedInId)) {
            log.error("Impossible to distribute budget {} : principal {} is not sponsor of organization {}", budget.getName(), userLoggedInId, budget.getOrganization().getId());
            throw new ForbiddenException();
        }

        // Create personal accounts for all members
        Set<UserEntity> members = userRepository.findAllByOrganizations_id(organizationId);
        members.forEach(member -> {
            AccountEntity account = accountService.getByBudgetAndUser(budget.getId(), member.getId());
            if(account == null) {
                account = new AccountEntity();
                account.setAmount(budget.getAmountPerMember());
                account.setBudget(budget);
            }
            account.setInitialAmount(budget.getAmountPerMember());
            account.setOwner(member);
            accountService.save(account);
        });

        // Distribute budget
        budget.setIsDistributed(true);
        budgetRepository.save(budget);
    }

    @Operation(summary = "Delete a budget", description = "Delete a budget", tags = { "Budgets" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Budget distributed", content = @Content(array = @ArraySchema(schema = @Schema(implementation = BudgetModel.class)))),
            @ApiResponse(responseCode = "400", description = "ID is missing", content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "403", description = "Principal has not enough privileges", content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "404", description = "Budget not found", content = @Content(schema = @Schema()))
    })
    @RequestMapping(value = "/budget/{id}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @PreAuthorize("hasRole('USER')")
    public void delete(Principal principal, @PathVariable("id") Long id) {

        // Fails if any of references are null
        if(id <= 0) {
            log.error("Impossible to delete budget : ID is missing");
            throw new BadRequestException();
        }

        // Retrieve full referenced objects
        BudgetEntity budget = budgetRepository.findById(id).orElse(null);
        if(budget == null) {
            log.error("Impossible to delete budget {} : budget not found", id);
            throw new NotFoundException();
        }

        // Verify that principal is member of organization
        UserEntity userLoggedIn = userService.get(principal);
        OrganizationEntity principalOrganization = organizationRepository.findByIdAndMembers_Id(budget.getOrganization().getId(), userLoggedIn.getId());
        if(principalOrganization == null) {
            log.error("Impossible to delete budget {} : principal {} is not member of organization {}", budget.getName(), userLoggedIn.getId(), budget.getOrganization().getId());
            throw new ForbiddenException();
        }

        // Test that user logged in has correct rights
        if(userService.isNotSponsorOfOrganization(userLoggedIn.getId(), budget.getOrganization().getId()) && userService.isNotAdmin(userLoggedIn.getId())) {
            log.error("Impossible to delete budget {} : principal {} has not enough privileges", budget.getName(), userLoggedIn.getId());
            throw new ForbiddenException();
        }

        // Delete budget
        budgetRepository.deleteById(id);
    }

}

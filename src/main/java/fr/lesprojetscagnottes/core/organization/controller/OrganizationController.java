package fr.lesprojetscagnottes.core.organization.controller;

import fr.lesprojetscagnottes.core.account.service.AccountService;
import fr.lesprojetscagnottes.core.authorization.entity.OrganizationAuthorityEntity;
import fr.lesprojetscagnottes.core.authorization.model.OrganizationAuthorityModel;
import fr.lesprojetscagnottes.core.authorization.name.OrganizationAuthorityName;
import fr.lesprojetscagnottes.core.authorization.repository.OrganizationAuthorityRepository;
import fr.lesprojetscagnottes.core.budget.entity.BudgetEntity;
import fr.lesprojetscagnottes.core.budget.model.BudgetModel;
import fr.lesprojetscagnottes.core.budget.repository.BudgetRepository;
import fr.lesprojetscagnottes.core.common.exception.BadRequestException;
import fr.lesprojetscagnottes.core.common.exception.ForbiddenException;
import fr.lesprojetscagnottes.core.common.exception.NotFoundException;
import fr.lesprojetscagnottes.core.common.pagination.DataPage;
import fr.lesprojetscagnottes.core.content.entity.ContentEntity;
import fr.lesprojetscagnottes.core.content.model.ContentModel;
import fr.lesprojetscagnottes.core.content.repository.ContentRepository;
import fr.lesprojetscagnottes.core.news.entity.NewsEntity;
import fr.lesprojetscagnottes.core.news.model.NewsModel;
import fr.lesprojetscagnottes.core.news.repository.NewsRepository;
import fr.lesprojetscagnottes.core.organization.entity.OrganizationEntity;
import fr.lesprojetscagnottes.core.organization.model.OrganizationModel;
import fr.lesprojetscagnottes.core.organization.repository.OrganizationRepository;
import fr.lesprojetscagnottes.core.project.entity.ProjectEntity;
import fr.lesprojetscagnottes.core.project.model.ProjectModel;
import fr.lesprojetscagnottes.core.project.model.ProjectStatus;
import fr.lesprojetscagnottes.core.project.repository.ProjectRepository;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.*;

@Slf4j
@RequestMapping("/api")
@Tag(name = "Organizations", description = "The Organizations API")
@RestController
public class OrganizationController {

    private final BudgetRepository budgetRepository;

    private final ContentRepository contentRepository;

    private final NewsRepository newsRepository;

    private final OrganizationAuthorityRepository organizationAuthorityRepository;

    private final OrganizationRepository organizationRepository;

    private final ProjectRepository projectRepository;

    private final UserRepository userRepository;

    private final AccountService accountService;

    private final UserService userService;

    @Autowired
    public OrganizationController(
            AccountService accountService,
            BudgetRepository budgetRepository,
            ContentRepository contentRepository,
            NewsRepository newsRepository,
            OrganizationAuthorityRepository organizationAuthorityRepository,
            OrganizationRepository organizationRepository,
            ProjectRepository projectRepository,
            UserRepository userRepository,
            UserService userService) {
        this.accountService = accountService;
        this.budgetRepository = budgetRepository;
        this.contentRepository = contentRepository;
        this.newsRepository = newsRepository;
        this.organizationAuthorityRepository = organizationAuthorityRepository;
        this.organizationRepository = organizationRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.userService = userService;
    }

    @Operation(summary = "Find all organizations paginated", description = "Find all organizations paginated", tags = {"Organizations"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Return all organizations paginated", content = @io.swagger.v3.oas.annotations.media.Content(schema = @Schema(implementation = DataPage.class)))
    })
    @PreAuthorize("hasRole('USER')")
    @RequestMapping(value = "/organization", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public DataPage<OrganizationModel> list(Principal principal, @RequestParam(name = "offset", defaultValue = "0") int offset, @RequestParam(name = "limit", defaultValue = "10") int limit) {

        Pageable pageable = PageRequest.of(offset, limit);

        // If user is not admin => organizations where principal is member
        // Else => all organizations
        Page<OrganizationEntity> entities;
        long userLoggedInId = userService.get(principal).getId();
        if (userService.isNotAdmin(userLoggedInId)) {
            entities = organizationRepository.findAllByMembers_Id(userLoggedInId, pageable);
        } else {
            entities = organizationRepository.findAll(pageable);
        }

        // Transform organizations
        DataPage<OrganizationModel> models = new DataPage<>(entities);
        entities.getContent().forEach(entity -> models.getContent().add(OrganizationModel.fromEntity(entity)));
        return models;
    }

    @Operation(summary = "Find all organizations for current user", description = "Find all organizations for current user", tags = {"Organizations"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Return all organizations for current user", content = @io.swagger.v3.oas.annotations.media.Content(array = @ArraySchema(schema = @Schema(implementation = OrganizationModel.class))))
    })
    @PreAuthorize("hasRole('USER')")
    @RequestMapping(value = "/organizations", method = RequestMethod.GET)
    public Set<OrganizationModel> getAll(Principal principal) {

        // Get user organizations
        UserEntity user = userService.get(principal);
        Set<OrganizationEntity> entities = organizationRepository.findAllByMembers_Id(user.getId());

        // Convert all entities to models
        Set<OrganizationModel> models = new LinkedHashSet<>();
        entities.forEach(entity -> models.add(OrganizationModel.fromEntity(entity)));

        return models;
    }

    @Operation(summary = "Find an organization by its ID", description = "Find an organization by its ID", tags = {"Organizations"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Return the organization", content = @io.swagger.v3.oas.annotations.media.Content(schema = @Schema(implementation = OrganizationModel.class))),
            @ApiResponse(responseCode = "400", description = "ID is incorrect", content = @io.swagger.v3.oas.annotations.media.Content(schema = @Schema())),
            @ApiResponse(responseCode = "404", description = "Organization not found", content = @io.swagger.v3.oas.annotations.media.Content(schema = @Schema()))
    })
    @PreAuthorize("hasRole('USER')")
    @RequestMapping(value = "/organization/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public OrganizationModel getById(Principal principal, @PathVariable("id") Long id) {

        // Verify that ID is correct
        if (id <= 0) {
            log.error("Impossible to get organization by ID : ID is incorrect");
            throw new BadRequestException();
        }

        // If user is not admin => organization where principal is member
        // Else => all organizations
        OrganizationEntity entity;
        long userLoggedInId = userService.get(principal).getId();
        if (userService.isNotAdmin(userLoggedInId)) {
            entity = organizationRepository.findByIdAndMembers_Id(id, userLoggedInId);
        } else {
            entity = organizationRepository.findById(id).orElse(null);
        }

        // Verify that entity exists
        if (entity == null) {
            log.error("Impossible to get organization by ID : organization not found");
            throw new NotFoundException();
        }

        // Transform and return organization
        return OrganizationModel.fromEntity(entity);
    }

    @Operation(summary = "Get list of organizations by a list of IDs", description = "Find a list of organizations by a list of IDs", tags = {"Users"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Return the organizations", content = @io.swagger.v3.oas.annotations.media.Content(array = @ArraySchema(schema = @Schema(implementation = OrganizationModel.class)))),
            @ApiResponse(responseCode = "400", description = "ID is incorrect", content = @io.swagger.v3.oas.annotations.media.Content(schema = @Schema())),
            @ApiResponse(responseCode = "403", description = "Principal has not enough privileges", content = @io.swagger.v3.oas.annotations.media.Content(schema = @Schema()))
    })
    @PreAuthorize("hasRole('USER')")
    @RequestMapping(value = "/organization", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE, params = {"ids"})
    public Set<OrganizationModel> getByIds(Principal principal, @RequestParam("ids") Set<Long> ids) {

        Long userLoggedInId = userService.get(principal).getId();
        boolean userLoggedIn_isNotAdmin = userService.isNotAdmin(userLoggedInId);
        Set<OrganizationEntity> userLoggedInOrganizations = organizationRepository.findAllByMembers_Id(userLoggedInId);
        Set<OrganizationModel> models = new LinkedHashSet<>();

        for (Long id : ids) {

            // Retrieve full referenced objects
            OrganizationEntity entity = organizationRepository.findById(id).orElse(null);
            if (entity == null) {
                log.error("Impossible to get organization {} : it doesn't exist", id);
                continue;
            }

            // Verify that principal share an organization with the user
            if (!userLoggedInOrganizations.contains(entity) && userLoggedIn_isNotAdmin) {
                log.error("Impossible to get organization {} : principal {} is not in it", id, userLoggedInId);
                continue;
            }

            // Add the user to returned list
            models.add(OrganizationModel.fromEntity(entity));
        }

        return models;
    }

    @Operation(summary = "Find all budgets for an organization", description = "Find all budgets for an organization", tags = {"Organizations"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Return all budgets for an organization", content = @io.swagger.v3.oas.annotations.media.Content(array = @ArraySchema(schema = @Schema(implementation = BudgetModel.class)))),
            @ApiResponse(responseCode = "404", description = "Principal is not a member of organization", content = @io.swagger.v3.oas.annotations.media.Content(schema = @Schema()))
    })
    @PreAuthorize("hasRole('USER')")
    @RequestMapping(value = "/organization/{id}/budgets", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Set<BudgetModel> getBudgets(Principal principal, @PathVariable("id") Long organizationId) {

        // Verify that ID is correct
        if (organizationId <= 0) {
            log.error("Impossible to get budgets of organization : ID is incorrect");
            throw new BadRequestException();
        }

        // Verify that principal is member of organization
        Long userLoggedInId = userService.get(principal).getId();
        OrganizationEntity organization = organizationRepository.findByIdAndMembers_Id(organizationId, userLoggedInId);
        if (organization == null && userService.isNotAdmin(userLoggedInId)) {
            log.error("Impossible to get budgets of organization {} : principal is not a member of organization", organizationId);
            throw new ForbiddenException();
        }

        // Get budget entities
        Set<BudgetEntity> entities = budgetRepository.findAllByOrganizationIdOrderByStartDateDesc(organizationId);

        // Convert all entities to models
        Set<BudgetModel> models = new LinkedHashSet<>();
        entities.forEach(entity -> models.add(BudgetModel.fromEntity(entity)));

        return models;
    }

    @Operation(summary = "Find all usable budgets for an organization", description = "Find all usable budgets for an organization", tags = {"Organizations"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Return all usable budgets for an organization", content = @io.swagger.v3.oas.annotations.media.Content(array = @ArraySchema(schema = @Schema(implementation = BudgetModel.class)))),
            @ApiResponse(responseCode = "400", description = "ID is incorrect", content = @io.swagger.v3.oas.annotations.media.Content(schema = @Schema())),
            @ApiResponse(responseCode = "403", description = "Principal has not enough privileges", content = @io.swagger.v3.oas.annotations.media.Content(schema = @Schema())),
            @ApiResponse(responseCode = "404", description = "Organization not found", content = @io.swagger.v3.oas.annotations.media.Content(schema = @Schema()))
    })
    @PreAuthorize("hasRole('USER')")
    @RequestMapping(value = "/organization/{id}/budgets/usable", method = RequestMethod.GET)
    public Set<BudgetModel> getUsableBudgets(Principal principal, @PathVariable("id") Long id) {

        if (id < 0) {
            log.error("Impossible to get usable budgets of organization {} : ID is incorrect", id);
            throw new BadRequestException();
        }

        // Verify that any of references are not null
        OrganizationEntity organization = organizationRepository.findById(id).orElse(null);
        if (organization == null) {
            log.error("Impossible to delete organization : donation {} not found", id);
            throw new NotFoundException();
        }

        // Verify that principal is member of organization
        Long userLoggedInId = userService.get(principal).getId();
        if (!userService.isMemberOfOrganization(userLoggedInId, id) && userService.isNotAdmin(userLoggedInId)) {
            log.error("Impossible to get usable budgets of organization {} : principal {} has not enough privileges", id, userLoggedInId);
            throw new ForbiddenException();
        }

        // Retrieve all corresponding entities
        Set<BudgetEntity> entities = budgetRepository.findAllUsableBudgetsInOrganization(new Date(), id);

        // Convert all entities to models
        Set<BudgetModel> models = new LinkedHashSet<>();
        entities.forEach(entity -> models.add(BudgetModel.fromEntity(entity)));

        return models;
    }

    @Operation(summary = "Create an organization", description = "Create an organization", tags = {"Organizations"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Return the created organization", content = @io.swagger.v3.oas.annotations.media.Content(schema = @Schema(implementation = OrganizationModel.class))),
            @ApiResponse(responseCode = "400", description = "Body is incomplete", content = @io.swagger.v3.oas.annotations.media.Content(schema = @Schema()))
    })
    @PreAuthorize("hasRole('USER')")
    @RequestMapping(value = "/organization", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public OrganizationModel create(Principal principal, @RequestBody OrganizationModel organizationModel) {

        // Verify that body is complete
        if (organizationModel == null || organizationModel.getName() == null) {
            log.error("Impossible to create organization : body is incomplete");
            throw new BadRequestException();
        }

        // Create organization
        OrganizationEntity organization = new OrganizationEntity();
        organization.setName(organizationModel.getName());
        organization.setLogoUrl(organizationModel.getLogoUrl());
        organization = organizationRepository.save(organization);

        // Create authorities
        for (OrganizationAuthorityName authorityName : OrganizationAuthorityName.values()) {
            organizationAuthorityRepository.save(new OrganizationAuthorityEntity(organization, authorityName));
        }

        // Grant principal with ROLE_OWNER
        UserEntity userLoggedIn = userService.get(principal);
        OrganizationAuthorityEntity organizationAuthority = organizationAuthorityRepository.findByOrganizationIdAndName(organization.getId(), OrganizationAuthorityName.ROLE_OWNER);
        userLoggedIn.getUserOrganizationAuthorities().add(organizationAuthority);
        userRepository.save(userLoggedIn);

        return OrganizationModel.fromEntity(organization);
    }

    @Operation(summary = "Update an organization", description = "Update an organization", tags = {"Organizations"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Organization updated", content = @io.swagger.v3.oas.annotations.media.Content(schema = @Schema())),
            @ApiResponse(responseCode = "400", description = "Body is not complete", content = @io.swagger.v3.oas.annotations.media.Content(schema = @Schema())),
            @ApiResponse(responseCode = "404", description = "Organization not found", content = @io.swagger.v3.oas.annotations.media.Content(schema = @Schema()))
    })
    @PreAuthorize("hasRole('USER')")
    @RequestMapping(value = "/organization", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public void update(Principal principal, @Valid @RequestBody OrganizationModel organizationModel) {

        // Get corresponding entity
        organizationRepository.findById(organizationModel.getId()).ifPresentOrElse(organizationEntity -> {
            // Verify that principal has correct privileges :
            // Principal is owner of the organization OR Principal is admin
            final Long userLoggedInId = userService.get(principal).getId();
            if (userService.isNotOwnerOfOrganization(userLoggedInId, organizationModel.getId()) && userService.isNotAdmin(userLoggedInId)) {
                log.error("Impossible to update organization : principal {} has not enough privileges", userLoggedInId);
                throw new ForbiddenException();
            }
            // Update entity
            organizationEntity.setName(organizationModel.getName());
            organizationEntity.setLogoUrl(organizationModel.getLogoUrl());
            organizationEntity.setSocialName(organizationModel.getSocialName());
            organizationRepository.save(organizationEntity);
        }, () -> {
            log.error("Impossible to update organization : organization {} not found", organizationModel.getId());
            throw new NotFoundException();
        });
    }

    @Operation(summary = "Delete an organization by its ID", description = "Delete an organization by its ID", tags = {"Organizations"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Organization deleted", content = @io.swagger.v3.oas.annotations.media.Content(schema = @Schema())),
            @ApiResponse(responseCode = "400", description = "ID is incorrect", content = @io.swagger.v3.oas.annotations.media.Content(schema = @Schema())),
            @ApiResponse(responseCode = "403", description = "Principal has not enough privileges", content = @io.swagger.v3.oas.annotations.media.Content(schema = @Schema())),
            @ApiResponse(responseCode = "404", description = "Organization not found", content = @io.swagger.v3.oas.annotations.media.Content(schema = @Schema()))
    })
    @PreAuthorize("hasRole('USER')")
    @RequestMapping(value = "/organization/{id}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public void delete(Principal principal, @PathVariable("id") long id) {

        // Fails if campaign ID is missing
        if (id <= 0) {
            log.error("Impossible to delete organization : ID is incorrect");
            throw new BadRequestException();
        }

        // Retrieve full referenced objects
        OrganizationEntity organization = organizationRepository.findById(id).orElse(null);

        // Verify that any of references are not null
        if (organization == null) {
            log.error("Impossible to delete organization : donation {} not found", id);
            throw new NotFoundException();
        }

        // Verify that principal has correct privileges :
        // Principal is owner of the organization OR Principal is admin
        Long userLoggedInId = userService.get(principal).getId();
        if (userService.isNotOwnerOfOrganization(userLoggedInId, id) && userService.isNotAdmin(userLoggedInId)) {
            log.error("Impossible to delete organization : principal {} has not enough privileges", userLoggedInId);
            throw new ForbiddenException();
        }

        // Delete donation
        organizationRepository.deleteById(id);
    }

    @Operation(summary = "Get members of an organization", description = "Get members of an organization", tags = {"Organizations"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Return all members of organization", content = @io.swagger.v3.oas.annotations.media.Content(schema = @Schema(implementation = DataPage.class))),
            @ApiResponse(responseCode = "400", description = "ID is incorrect", content = @io.swagger.v3.oas.annotations.media.Content(schema = @Schema())),
            @ApiResponse(responseCode = "403", description = "Principal has not enough privileges", content = @io.swagger.v3.oas.annotations.media.Content(schema = @Schema())),
            @ApiResponse(responseCode = "404", description = "Organization not found", content = @io.swagger.v3.oas.annotations.media.Content(schema = @Schema()))
    })
    @PreAuthorize("hasRole('USER')")
    @RequestMapping(value = "/organization/{id}/members", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public DataPage<UserModel> getMembers(Principal principal, @PathVariable("id") long id, @RequestParam(name = "offset", defaultValue = "0") int offset, @RequestParam(name = "limit", defaultValue = "10") int limit) {

        // Verify that ID is correct
        if (id <= 0) {
            log.error("Impossible to get members of organizations : ID is incorrect");
            throw new BadRequestException();
        }

        // Verify that organization exists
        OrganizationEntity organization = organizationRepository.findById(id).orElse(null);
        if (organization == null) {
            log.error("Impossible to get members of organizations : organization {} not found", id);
            throw new NotFoundException();
        }

        // Verify if principal has correct privileges
        Long userLoggedInId = userService.get(principal).getId();
        if (!userService.isMemberOfOrganization(userLoggedInId, id) && userService.isNotAdmin(userLoggedInId)) {
            log.error("Impossible to get members of organizations : principal is not a member of organization {}", id);
            throw new ForbiddenException();
        }

        // Get and transform users
        Pageable pageable = PageRequest.of(offset, limit, Sort.by("firstname").ascending().and(Sort.by("lastname").ascending()));
        Page<UserEntity> entities = userRepository.findAllByOrganizations_id(id, pageable);
        DataPage<UserModel> models = new DataPage<>(entities);
        entities.getContent().forEach(entity -> models.getContent().add(UserModel.fromEntity(entity)));
        return models;
    }

    @Operation(summary = "Add a member to an organization", description = "Add a member to an organization", tags = {"Organizations"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Member added", content = @io.swagger.v3.oas.annotations.media.Content(schema = @Schema())),
            @ApiResponse(responseCode = "400", description = "ID is incorrect", content = @io.swagger.v3.oas.annotations.media.Content(schema = @Schema())),
            @ApiResponse(responseCode = "403", description = "Principal has not enough privileges", content = @io.swagger.v3.oas.annotations.media.Content(schema = @Schema())),
            @ApiResponse(responseCode = "404", description = "Organization or User not found", content = @io.swagger.v3.oas.annotations.media.Content(schema = @Schema()))
    })
    @PreAuthorize("hasRole('USER')")
    @RequestMapping(value = "/organization/{id}/members/{userId}", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public void addMember(Principal principal, @PathVariable long id, @PathVariable long userId) {

        // Verify that IDs are corrects
        if (id <= 0 || userId <= 0) {
            log.error("Impossible to add member to an organization : parameters are incorrect");
            throw new BadRequestException();
        }

        // Verify that organization and user exists
        OrganizationEntity organization = organizationRepository.findById(id).orElse(null);
        UserEntity user = userRepository.findById(userId).orElse(null);
        if (organization == null || user == null) {
            log.error("Impossible to add member to an organization : organization {} or user {} doesnt exist", id, userId);
            throw new NotFoundException();
        }

        // Verify if principal has correct privileges
        Long userLoggedInId = userService.get(principal).getId();
        if (userService.isNotManagerOfOrganization(userLoggedInId, id) && userService.isNotAdmin(userLoggedInId)) {
            log.error("Impossible to add member to an organization : principal is not a member of organization {}", id);
            throw new ForbiddenException();
        }

        // Add member to organization
        organization.getMembers().stream().filter(member -> member.getId().equals(user.getId()))
                .findAny()
                .ifPresentOrElse(
                        member -> {
                            log.error("Impossible to add member to an organization : User {} is already a member of organization {}", member.getId(), organization.getId());
                            throw new BadRequestException();
                        },
                        () -> {
                            organization.getMembers().add(user);
                            organizationRepository.save(organization);
                            log.info("User {} is now a member of organization {}", user.getId(), organization.getId());
                        }
                );

        // Create accounts for usable budgets
        accountService.createUserAccountsForUsableBudgets(user, organization.getId());
    }

    @Operation(summary = "Remove a member from an organization", description = "Remove a member from an organization", tags = {"Organizations"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Member removed", content = @io.swagger.v3.oas.annotations.media.Content(schema = @Schema())),
            @ApiResponse(responseCode = "400", description = "ID is incorrect", content = @io.swagger.v3.oas.annotations.media.Content(schema = @Schema())),
            @ApiResponse(responseCode = "403", description = "Principal has not enough privileges", content = @io.swagger.v3.oas.annotations.media.Content(schema = @Schema())),
            @ApiResponse(responseCode = "404", description = "Organization or User not found", content = @io.swagger.v3.oas.annotations.media.Content(schema = @Schema()))
    })
    @PreAuthorize("hasRole('USER')")
    @RequestMapping(value = "/organization/{id}/members/{userId}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public void removeMember(Principal principal, @PathVariable long id, @PathVariable long userId) {

        // Verify that IDs are corrects
        if (id <= 0 || userId <= 0) {
            log.error("Impossible to remove member from an organization : parameters are incorrect");
            throw new BadRequestException();
        }

        // Verify that organization and user exists
        OrganizationEntity organization = organizationRepository.findById(id).orElse(null);
        UserEntity user = userRepository.findById(userId).orElse(null);
        if (organization == null || user == null) {
            log.error("Impossible to remove member from an organization : organization {} or user {} doesnt exist", id, userId);
            throw new NotFoundException();
        }

        // Verify if principal has correct privileges
        Long userLoggedInId = userService.get(principal).getId();
        if (userService.isNotManagerOfOrganization(userLoggedInId, id) && userService.isNotAdmin(userLoggedInId)) {
            log.error("Impossible to remove member from an organization : principal is not a manager of organization {}", id);
            throw new ForbiddenException();
        }

        // Remove privileges from user
        Set<OrganizationAuthorityEntity> organizationAuthorities = new LinkedHashSet<>();
        user.getUserOrganizationAuthorities().stream().filter(organizationAuthority -> organizationAuthority.getOrganization().getId() == id).forEach(organizationAuthorities::add);
        user.getUserOrganizationAuthorities().removeAll(organizationAuthorities);
        userRepository.save(user);
        log.info("All Privileges for User {} on organization {} has been removed", user.getId(), organization.getId());

        // Remove member from organization
        organization.getMembers().remove(user);
        organizationRepository.save(organization);
        log.info("User {} has been removed from organization {}", user.getId(), organization.getId());
    }

    @Operation(summary = "Get paginated organization projects", description = "Get paginated organization projects", tags = {"Organizations"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Return paginated projects", content = @io.swagger.v3.oas.annotations.media.Content(schema = @Schema(implementation = DataPage.class))),
            @ApiResponse(responseCode = "400", description = "Params are incorrect", content = @io.swagger.v3.oas.annotations.media.Content(schema = @Schema())),
            @ApiResponse(responseCode = "403", description = "Principal has not enough privileges", content = @io.swagger.v3.oas.annotations.media.Content(schema = @Schema()))
    })
    @PreAuthorize("hasRole('USER')")
    @RequestMapping(value = "/organization/{id}/projects", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE, params = {"offset", "limit", "filters"})
    public DataPage<ProjectModel> getPaginatedProjects(Principal principal, @PathVariable("id") Long id, @RequestParam("offset") int offset, @RequestParam("limit") int limit, @RequestParam("filters") List<String> filters) {

        // Verify that params are correct
        if (id <= 0 || offset < 0 || limit <= 0 || filters == null) {
            log.error("Impossible to get organization projects : params are incorrect");
            throw new BadRequestException();
        }

        // Verify that organization exists
        OrganizationEntity organization = organizationRepository.findById(id).orElse(null);
        if (organization == null) {
            log.error("Impossible to get organization projects : organization not found");
            throw new NotFoundException();
        }

        // Prepare filter
        Set<ProjectStatus> status = new LinkedHashSet<>();
        for (String filter : filters) {
            status.add(ProjectStatus.valueOf(filter.toUpperCase(Locale.ROOT)));
        }
        if (status.isEmpty()) {
            status.addAll(List.of(ProjectStatus.values()));
        }

        // Get corresponding entities according to principal
        Long userLoggedInId = userService.get(principal).getId();
        Pageable pageable = PageRequest.of(offset, limit, Sort.by("title").ascending().and(Sort.by("status").ascending()));
        Page<ProjectEntity> entities;
        boolean isNotAdmin = userService.isNotAdmin(userLoggedInId);
        if (isNotAdmin) {
            entities = projectRepository.findAllByOrganizationIdAndStatusIn(id, status, pageable);
        } else {
            entities = projectRepository.findAllByStatusIn(status, pageable);
        }

        // Convert and return data
        DataPage<ProjectModel> models = new DataPage<>(entities);
        entities.getContent().forEach(entity -> models.getContent().add(ProjectModel.fromEntity(entity)));
        return models;
    }

    @Operation(summary = "Get paginated news", description = "Get paginated news", tags = {"Organizations"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Returns corresponding news", content = @Content(schema = @Schema(implementation = DataPage.class))),
            @ApiResponse(responseCode = "400", description = "Budget ID is incorrect", content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "403", description = "Principal has not enough privileges", content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "404", description = "Budget not found", content = @Content(schema = @Schema()))
    })
    @PreAuthorize("hasRole('USER')")
    @RequestMapping(value = "/organization/{id}/news", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE, params = {"offset", "limit"})
    public DataPage<NewsModel> listNews(Principal principal, @PathVariable("id") Long id, @RequestParam("offset") int offset, @RequestParam("limit") int limit) {

        // Verify that IDs are corrects
        if (id <= 0) {
            log.error("Impossible to get news : parameters are incorrect");
            throw new BadRequestException();
        }

        // Verify that organization exists
        OrganizationEntity organization = organizationRepository.findById(id).orElse(null);
        if (organization == null) {
            log.error("Impossible to get news : organization not found");
            throw new NotFoundException();
        }

        // Verify if principal has correct privileges
        Long userLoggedInId = userService.get(principal).getId();
        if (!userService.isMemberOfOrganization(userLoggedInId, id) && userService.isNotAdmin(userLoggedInId)) {
            log.error("Impossible to get news : principal is not a member of organization {}", id);
            throw new ForbiddenException();
        }

        // Get and transform donations
        Page<NewsEntity> entities = newsRepository.findAllByOrganizationIdOrOrganizationIdIsNull(id, PageRequest.of(offset, limit, Sort.by("createdAt").descending()));
        DataPage<NewsModel> models = new DataPage<>(entities);
        entities.getContent().forEach(entity -> models.getContent().add(NewsModel.fromEntity(entity)));
        return models;
    }

    @Operation(summary = "Get organization authorities", description = "Get organization authorities", tags = {"Organizations"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Return all organization authorities", content = @io.swagger.v3.oas.annotations.media.Content(array = @ArraySchema(schema = @Schema(implementation = OrganizationAuthorityModel.class)))),
            @ApiResponse(responseCode = "400", description = "ID is incorrect", content = @io.swagger.v3.oas.annotations.media.Content(schema = @Schema())),
            @ApiResponse(responseCode = "403", description = "Principal has not enough privileges", content = @io.swagger.v3.oas.annotations.media.Content(schema = @Schema())),
            @ApiResponse(responseCode = "404", description = "Organization not found", content = @io.swagger.v3.oas.annotations.media.Content(schema = @Schema()))
    })
    @PreAuthorize("hasRole('USER')")
    @RequestMapping(value = "/organization/{id}/authorities", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Set<OrganizationAuthorityModel> getOrganizationAuthorities(Principal principal, @PathVariable("id") long id) {

        // Verify that IDs are corrects
        if (id <= 0) {
            log.error("Impossible to get organization authorities : parameters are incorrect");
            throw new BadRequestException();
        }

        // Verify that organization exists
        OrganizationEntity organization = organizationRepository.findById(id).orElse(null);
        if (organization == null) {
            log.error("Impossible to get organization authorities : organization not found");
            throw new NotFoundException();
        }

        // Verify if principal has correct privileges
        Long userLoggedInId = userService.get(principal).getId();
        if (!userService.isMemberOfOrganization(userLoggedInId, id) && userService.isNotAdmin(userLoggedInId)) {
            log.error("Impossible to get organization authorities : principal is not a member of organization {}", id);
            throw new ForbiddenException();
        }

        // Get and transform organization authorities
        Set<OrganizationAuthorityEntity> entities = organizationAuthorityRepository.findAllByOrganizationId(organization.getId());
        Set<OrganizationAuthorityModel> models = new LinkedHashSet<>();
        entities.forEach(entity -> models.add(OrganizationAuthorityModel.fromEntity(entity)));
        return models;
    }

    @Operation(summary = "Get paginated organization contents", description = "Get paginated organization contents", tags = {"Organizations"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Return paginated organization contents", content = @io.swagger.v3.oas.annotations.media.Content(schema = @Schema(implementation = DataPage.class))),
            @ApiResponse(responseCode = "400", description = "ID is incorrect", content = @io.swagger.v3.oas.annotations.media.Content(schema = @Schema())),
            @ApiResponse(responseCode = "403", description = "Principal has not enough privileges", content = @io.swagger.v3.oas.annotations.media.Content(schema = @Schema())),
            @ApiResponse(responseCode = "404", description = "Organization not found", content = @io.swagger.v3.oas.annotations.media.Content(schema = @Schema()))
    })
    @PreAuthorize("hasRole('USER')")
    @RequestMapping(value = "/organization/{id}/contents", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE, params = {"offset", "limit"})
    public DataPage<ContentModel> getContents(Principal principal, @PathVariable("id") long id, @RequestParam(name = "offset") int offset, @RequestParam(name = "limit") int limit) {

        // Verify that ID is correct
        if (id <= 0) {
            log.error("Impossible to get contents of organizations : ID is incorrect");
            throw new BadRequestException();
        }

        // Verify that organization exists
        OrganizationEntity organization = organizationRepository.findById(id).orElse(null);
        if (organization == null) {
            log.error("Impossible to get contents of organizations : organization {} not found", id);
            throw new NotFoundException();
        }

        // Verify if principal has correct privileges
        Long userLoggedInId = userService.get(principal).getId();
        if (!userService.isMemberOfOrganization(userLoggedInId, id) && userService.isNotAdmin(userLoggedInId)) {
            log.error("Impossible to get contents of organizations : principal is not a member of organization {}", id);
            throw new ForbiddenException();
        }

        // Get and transform contents
        Pageable pageable = PageRequest.of(offset, limit, Sort.by("name").ascending());
        Page<ContentEntity> entities = contentRepository.findAllByOrganizationId(pageable, id);
        DataPage<ContentModel> models = new DataPage<>(entities);
        entities.getContent().forEach(entity -> models.getContent().add(ContentModel.fromEntity(entity)));
        return models;
    }

    @Operation(summary = "Get all organization contents", description = "Get all organization contents", tags = {"Organizations"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Return all organization contents", content = @io.swagger.v3.oas.annotations.media.Content(array = @ArraySchema(schema = @Schema(implementation = ContentModel.class)))),
            @ApiResponse(responseCode = "400", description = "ID is incorrect", content = @io.swagger.v3.oas.annotations.media.Content(schema = @Schema())),
            @ApiResponse(responseCode = "403", description = "Principal has not enough privileges", content = @io.swagger.v3.oas.annotations.media.Content(schema = @Schema())),
            @ApiResponse(responseCode = "404", description = "Organization not found", content = @io.swagger.v3.oas.annotations.media.Content(schema = @Schema()))
    })
    @PreAuthorize("hasRole('USER')")
    @RequestMapping(value = "/organization/{id}/contents", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Set<ContentModel> getAllContents(Principal principal, @PathVariable("id") long id) {

        // Verify that ID is correct
        if (id <= 0) {
            log.error("Impossible to get contents of organizations : ID is incorrect");
            throw new BadRequestException();
        }

        // Verify that organization exists
        OrganizationEntity organization = organizationRepository.findById(id).orElse(null);
        if (organization == null) {
            log.error("Impossible to get contents of organizations : organization {} not found", id);
            throw new NotFoundException();
        }

        // Verify if principal has correct privileges
        Long userLoggedInId = userService.get(principal).getId();
        if (!userService.isMemberOfOrganization(userLoggedInId, id) && userService.isNotAdmin(userLoggedInId)) {
            log.error("Impossible to get contents of organizations : principal is not a member of organization {}", id);
            throw new ForbiddenException();
        }

        // Get and transform contents
        Set<ContentEntity> entities = contentRepository.findAllByOrganizationId(id);
        Set<ContentModel> models = new LinkedHashSet<>();
        entities.forEach(entity -> models.add(ContentModel.fromEntity(entity)));
        return models;
    }

}

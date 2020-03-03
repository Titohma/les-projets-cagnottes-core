package fr.thomah.valyou.controller;

import com.google.gson.Gson;
import fr.thomah.valyou.exception.NotFoundException;
import fr.thomah.valyou.model.*;
import fr.thomah.valyou.repository.*;
import fr.thomah.valyou.service.SlackClientService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.*;

@RestController
@Transactional
public class ProjectController {

    private static final String WEB_URL = System.getenv("VALYOU_WEB_URL");

    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectController.class);

    @Autowired
    private Gson gson;

    @Autowired
    private SlackClientService slackClientService;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private DonationRepository donationRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private ProjectRepository repository;

    @Autowired
    private UserRepository userRepository;

    @PreAuthorize("hasRole('USER')")
    @RequestMapping(value = "/api/project", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE, params = {"offset", "limit", "filter"})
    public Page<Project> list(@RequestParam("offset") int offset, @RequestParam("limit") int limit, @RequestParam("filter") List<String> filters) {
        Pageable pageable = PageRequest.of(offset, limit);
        Set<ProjectStatus> statuses = new LinkedHashSet<>();
        for(String filter : filters) {
            statuses.add(ProjectStatus.valueOf(filter));
        }
        if(statuses.isEmpty()) {
            statuses.addAll(List.of(ProjectStatus.values()));
        }
        return repository.findAllByStatusInOrderByStatusAscFundingDeadlineAsc(statuses, pageable);
    }

    @PreAuthorize("hasRole('USER')")
    @RequestMapping(value = "/api/project", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE, params = {"memberId"})
    public Set<Project> getByMemberId(@RequestParam("memberId") Long memberId) {
        Set<Project> projectsByLeader = repository.findAllByLeaderId(memberId);
        Set<Project> projectsByPeopleGivingTime = repository.findAllByPeopleGivingTime_Id(memberId);
        projectsByLeader.addAll(projectsByPeopleGivingTime);
        return projectsByLeader;
    }

    @PreAuthorize("hasRole('USER')")
    @RequestMapping(value = "/api/project/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Project findById(@PathVariable("id") Long id) {
        Project project = repository.findById(id).orElse(null);
        if (project == null) {
            throw new NotFoundException();
        } else {
            return project;
        }
    }

    @PreAuthorize("hasRole('USER')")
    @RequestMapping(value = "/api/project/{id}/organizations", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Set<Organization> findByIdOrganizations(@PathVariable("id") Long id) {
        Project project = findById(id);
        return project.getOrganizations();
    }

    @PreAuthorize("hasRole('USER')")
    @RequestMapping(value = "/api/project", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE, params = {"budgetId", "offset", "limit"})
    public Page<Project> getByBudgetId(@RequestParam("budgetId") long budgetId, @RequestParam("offset") int offset, @RequestParam("limit") int limit) {
        Pageable pageable = PageRequest.of(offset, limit);
        return repository.findByBudgets_idOrderByIdDesc(budgetId, pageable);
    }

    @RequestMapping(value = "/api/project", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('USER')")
    public Project create(Principal user, @RequestBody String projectStr) {
        UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) user;
        final User userLoggedIn = userRepository.findByEmail(((User) token.getPrincipal()).getEmail());

        Project project = gson.fromJson(projectStr, Project.class);
        Set<Organization> organizations = project.getOrganizations();
        Set<Organization> newOrganizations = new LinkedHashSet<>();
        organizations.forEach(organization -> {
            Organization organizationInDb = organizationRepository.findById(organization.getId()).orElse(null);
            if (organizationInDb == null) {
                throw new NotFoundException();
            } else {
                organizationInDb.addProject(project);
                newOrganizations.add(organizationInDb);
            }
        });
        project.setOrganizations(newOrganizations);

        Set<Budget> budgets = project.getBudgets();
        budgets.forEach(budget -> {
            Budget budgetInDb = budgetRepository.findById(budget.getId()).orElse(null);
            if (budgetInDb == null) {
                throw new NotFoundException();
            } else {
                budgetInDb.getProjects().add(project);
                budget = budgetInDb;
            }
        });
        project.setBudgets(budgets);

        Project p = repository.save(project);

        String defaultUser = new StringBuilder("*")
                .append(userLoggedIn.getFirstname())
                .append(" ")
                .append(userLoggedIn.getLastname())
                .append("*")
                .toString();

        String endMessage = new StringBuilder(" vient de créer le projet cagnotte *")
                .append(p.getTitle())
                .append("* et vous êtes tous invités à y participer !")
                .append("\nDécouvrez le vite sur ")
                .append(WEB_URL)
                .append("/projects/")
                .append(p.getId())
                .toString();

        newOrganizations.forEach(organization -> {

            if(organization.getSlackTeam() != null) {

                StringBuilder stringBuilderUser = new StringBuilder(":rocket: ");

                if (userLoggedIn.getSlackUser() != null && organization.getId() == userLoggedIn.getSlackUser().getSlackTeam().getOrganization().getId()) {
                    stringBuilderUser.append("<@")
                            .append(userLoggedIn.getSlackUser().getSlackId())
                            .append(">");
                } else {
                    stringBuilderUser.append(defaultUser);
                }
                stringBuilderUser.append(endMessage);

                String channelId = slackClientService.joinChannel(organization.getSlackTeam());
                slackClientService.inviteInChannel(organization.getSlackTeam(), channelId);
                slackClientService.postMessage(organization.getSlackTeam(), channelId, stringBuilderUser.toString());
            }
        });


        return repository.save(project);
    }

    @RequestMapping(value = "/api/project", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('USER')")
    public Project update(@RequestBody Project project) {
        Project projectInDb = repository.findById(project.getId()).orElse(null);
        if (projectInDb == null) {
            throw new NotFoundException();
        } else {
            Set<Organization> organizations = project.getOrganizations();
            organizations.forEach(organization -> {
                Organization organizationInDb = organizationRepository.findById(organization.getId()).orElse(null);
                if (organizationInDb == null) {
                    throw new NotFoundException();
                } else {
                    if (organizationInDb.getProjects().stream().noneMatch(prj -> projectInDb.getId().equals(prj.getId()))) {
                        organizationInDb.addProject(project);
                    }
                    organization = organizationInDb;
                }
            });
            projectInDb.setOrganizations(organizations);

            Set<Budget> budgets = project.getBudgets();
            budgets.forEach(budget -> {
                Budget budgetInDb = budgetRepository.findById(budget.getId()).orElse(null);
                if (budgetInDb == null) {
                    throw new NotFoundException();
                } else {
                    if (budgetInDb.getProjects().stream().noneMatch(prj -> projectInDb.getId().equals(prj.getId()))) {
                        budgetInDb.getProjects().add(project);
                    }
                    budget = budgetInDb;
                }
            });
            project.setBudgets(budgets);

            projectInDb.setTitle(project.getTitle());
            projectInDb.setShortDescription(project.getShortDescription());
            projectInDb.setLongDescription(project.getLongDescription());
            projectInDb.setLeader(project.getLeader());
            projectInDb.setPeopleRequired(project.getPeopleRequired());
            if (project.getDonationsRequired() > projectInDb.getDonationsRequired()) {
                projectInDb.setDonationsRequired(project.getDonationsRequired());
            }
            return repository.save(projectInDb);
        }
    }

    @RequestMapping(value = "/api/project/{id}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('USER')")
    public void save(@PathVariable("id") String id, @RequestBody Project project) {
        Project projectInDb = repository.findById(Long.valueOf(id)).orElse(null);
        if (projectInDb == null) {
            throw new NotFoundException();
        } else {
            repository.save(project);
        }
    }

    @RequestMapping(value = "/api/project/{id}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @PreAuthorize("hasRole('USER')")
    public void delete(@PathVariable("id") String id) {
        repository.deleteById(Long.valueOf(id));
    }

    @PreAuthorize("hasRole('USER')")
    @RequestMapping(value = "/api/project/{id}/join", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Project join(@PathVariable("id") Long id, Principal user) {
        Project projectInDb = repository.findById(id).orElse(null);
        if (projectInDb == null) {
            throw new NotFoundException();
        } else {
            UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) user;
            final User userLoggedIn = userRepository.findByEmail(((User) token.getPrincipal()).getEmail());
            User userInPeopleGivingTime = projectInDb.getPeopleGivingTime().stream().filter(userGivingTime -> userLoggedIn.getId().equals(userGivingTime.getId())).findFirst().orElse(null);
            if (userInPeopleGivingTime == null) {
                projectInDb.addPeopleGivingTime(userLoggedIn);
            } else {
                projectInDb.getPeopleGivingTime().remove(userInPeopleGivingTime);
            }
            return repository.save(projectInDb);
        }
    }

    @PreAuthorize("hasRole('USER')")
    @RequestMapping(value = "/api/project/{id}/donations", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE, params = {"offset", "limit"})
    public Page<Donation> getDonations(@PathVariable("id") long id, @RequestParam("offset") int offset, @RequestParam("limit") int limit) {
        Pageable pageable = PageRequest.of(offset, limit);
        return donationRepository.findByProject_idOrderByIdAsc(id, pageable);
    }

    @RequestMapping(value = "/api/project/validate", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('USER')")
    public void validate() {
        processProjectFundingDeadlines();
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void processProjectFundingDeadlines() {
        LOGGER.info("[PFD] Start Project Funding Deadlines Processing");
        Set<Project> projects = repository.findAllByStatusAndFundingDeadlineLessThan(ProjectStatus.A_IN_PROGRESS, new Date());
        LOGGER.info("[PFD] " + projects.size() + " project(s) found");
        projects.forEach(project -> {
            Set<Donation> donations = donationRepository.findAllByProjectId(project.getId());
            float totalDonations = 0f;
            for (Donation donation : donations) {
                totalDonations += donation.getAmount();
            }
            LOGGER.info("[PFD][" + project.getId() + "] Project : " + project.getTitle());
            LOGGER.info("[PFD][" + project.getId() + "] Teammates : " + project.getPeopleGivingTime().size() + " / " + project.getPeopleRequired());
            LOGGER.info("[PFD][" + project.getId() + "] Donations : " + totalDonations + " € / " + project.getDonationsRequired() + " €");
            if (totalDonations >= project.getDonationsRequired()
                    && project.getPeopleGivingTime().size() >= project.getPeopleRequired()) {
                project.setStatus(ProjectStatus.B_READY);
                LOGGER.info("[PFD][" + project.getId() + "] Status => B_READY");
            } else {
                project.setStatus(ProjectStatus.C_AVORTED);
                LOGGER.info("[PFD][" + project.getId() + "] Status => C_AVORTED");
                donationRepository.deleteByProjectId(project.getId());
                LOGGER.info("[PFD][" + project.getId() + "] Donations deleted");
            }
        });
        LOGGER.info("End Project Funding Deadlines Processing");
    }

}

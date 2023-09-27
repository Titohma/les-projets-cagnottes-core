package fr.lesprojetscagnottes.core.project.model;

import fr.lesprojetscagnottes.core.common.GenericModel;
import fr.lesprojetscagnottes.core.common.audit.AuditEntity;
import fr.lesprojetscagnottes.core.common.strings.StringsCommon;
import fr.lesprojetscagnottes.core.project.entity.ProjectEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.NotNull;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter(AccessLevel.PUBLIC)
@Setter(AccessLevel.PUBLIC)
@MappedSuperclass
public class ProjectModel extends AuditEntity<String> {

    @Column(name = "title")
    @NotNull
    protected String title = StringsCommon.EMPTY_STRING;

    @Column(length = 50)
    @NotNull
    @Enumerated(EnumType.STRING)
    protected ProjectStatus status = ProjectStatus.DRAFT;

    @Column(name = "published")
    protected Boolean isPublished = false;

    @Column(name = "idea_has_anonymous_creator")
    protected Boolean ideaHasAnonymousCreator = false;

    @Column(name = "idea_has_leader_creator")
    protected Boolean ideaHasLeaderCreator = false;

    @Column(name = "last_status_update")
    protected Date lastStatusUpdate;

    @Column(name = "short_description")
    protected String shortDescription = StringsCommon.EMPTY_STRING;

    @Column(name = "long_description", columnDefinition = "TEXT")
    protected String longDescription = StringsCommon.EMPTY_STRING;

    @Column(name = "people_required")
    protected Integer peopleRequired;

    @Column
    private String workspace = StringsCommon.EMPTY_STRING;

    @Transient
    protected GenericModel leader;

    @Transient
    protected GenericModel organization;

    @Transient
    private Set<Long> campaignsRef = new LinkedHashSet<>();

    @Transient
    private Set<Long> peopleGivingTimeRef = new LinkedHashSet<>();

    @Transient
    private Set<Long> newsRef = new LinkedHashSet<>();

    public static ProjectModel fromEntity(ProjectEntity entity) {
        ProjectModel model = new ProjectModel();
        model.setCreatedAt(entity.getCreatedAt());
        model.setCreatedBy(entity.getCreatedBy());
        model.setUpdatedAt(entity.getUpdatedAt());
        model.setUpdatedBy(entity.getUpdatedBy());
        model.setId(entity.getId());
        model.setTitle(entity.getTitle());
        model.setStatus(entity.getStatus());
        model.setIsPublished(entity.getIsPublished());
        model.setIdeaHasAnonymousCreator(entity.getIdeaHasAnonymousCreator());
        model.setIdeaHasLeaderCreator(entity.getIdeaHasLeaderCreator());
        model.setLastStatusUpdate(entity.getLastStatusUpdate());
        model.setShortDescription(entity.getShortDescription());
        model.setLongDescription(entity.getLongDescription());
        model.setPeopleRequired(entity.getPeopleRequired());
        model.setWorkspace(entity.getWorkspace());
        model.setLeader(new GenericModel(entity.getLeader()));
        model.setOrganization(new GenericModel(entity.getOrganization()));
        entity.getCampaigns().forEach(campaign -> model.getCampaignsRef().add(campaign.getId()));
        entity.getPeopleGivingTime().forEach(member -> model.getPeopleGivingTimeRef().add(member.getId()));
        entity.getNews().forEach(news -> model.getNewsRef().add(news.getId()));
        return model;
    }

}

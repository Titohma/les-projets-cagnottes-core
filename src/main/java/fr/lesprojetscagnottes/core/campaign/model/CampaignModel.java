package fr.lesprojetscagnottes.core.campaign.model;

import fr.lesprojetscagnottes.core.campaign.entity.CampaignEntity;
import fr.lesprojetscagnottes.core.common.GenericModel;
import fr.lesprojetscagnottes.core.common.audit.AuditEntity;
import fr.lesprojetscagnottes.core.common.strings.StringsCommon;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.util.Date;

@Getter(AccessLevel.PUBLIC)
@Setter(AccessLevel.PUBLIC)
@MappedSuperclass
public class CampaignModel extends AuditEntity<String> {

    @Column
    @NotNull
    protected String title = StringsCommon.EMPTY_STRING;

    @Column(length = 50)
    @NotNull
    @Enumerated(EnumType.STRING)
    protected CampaignStatus status;

    @Column(name = "donations_required")
    protected Float donationsRequired;

    @Column(name = "days_required")
    protected Integer daysRequired = 0;

    @Column(name = "hours_required")
    protected Integer hoursRequired = 0;

    @Column(name = "total_required")
    protected Float totalRequired;

    @Column(name = "funding_deadline")
    @Temporal(TemporalType.TIMESTAMP)
    @NotNull
    protected Date fundingDeadline = new Date();

    @Column(name = "total_donations")
    @NotNull
    protected Float totalDonations = 0f;

    @Transient
    private GenericModel project;

    @Transient
    private GenericModel budget;

    public static CampaignModel fromEntity(CampaignEntity entity) {
        CampaignModel model = new CampaignModel();
        model.setCreatedAt(entity.getCreatedAt());
        model.setCreatedBy(entity.getCreatedBy());
        model.setUpdatedAt(entity.getUpdatedAt());
        model.setUpdatedBy(entity.getUpdatedBy());
        model.setId(entity.getId());
        model.setTitle(entity.getTitle());
        model.setStatus(entity.getStatus());
        model.setDonationsRequired(entity.getDonationsRequired());
        model.setHoursRequired(entity.getHoursRequired());
        model.setDaysRequired(entity.getDaysRequired());
        model.setTotalRequired(entity.getTotalRequired());
        model.setFundingDeadline(entity.getFundingDeadline());
        model.setTotalDonations(entity.getTotalDonations());
        model.setProject(new GenericModel(entity.getProject()));
        model.setBudget(new GenericModel(entity.getBudget()));
        return model;
    }

    @Override
    public String toString() {
        return "CampaignModel{" + "title='" + title + '\'' +
                ", status=" + status +
                ", donationsRequired=" + donationsRequired +
                ", fundingDeadline=" + fundingDeadline +
                ", totalDonations=" + totalDonations +
                ", project=" + project +
                ", budget=" + budget +
                ", id=" + id +
                '}';
    }
}

package fr.lesprojetscagnottes.core.budget.model;

import fr.lesprojetscagnottes.core.budget.entity.BudgetEntity;
import fr.lesprojetscagnottes.core.common.GenericModel;
import fr.lesprojetscagnottes.core.common.audit.AuditEntity;
import fr.lesprojetscagnottes.core.common.strings.StringsCommon;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotNull;
import java.util.Date;

@Getter(AccessLevel.PUBLIC)
@Setter(AccessLevel.PUBLIC)
@MappedSuperclass
public class BudgetModel extends AuditEntity<String> {

    @Column(name = "name")
    protected String name = StringsCommon.EMPTY_STRING;

    @Column(name = "amount_per_member")
    @NotNull
    protected float amountPerMember = 0f;

    @Column(name = "cost_of_hour")
    protected float costOfHour = 0f;

    @Column(name = "cost_of_day")
    protected float costOfDay = 0f;

    @Column(name = "can_finance_time")
    @NotNull
    protected Boolean canFinanceTime = false;

    @Column(name = "is_distributed")
    @NotNull
    protected Boolean isDistributed = false;

    @Column(name = "start_date")
    @NotNull
    protected Date startDate = new Date();

    @Column(name = "end_date")
    @NotNull
    protected Date endDate = new Date();

    @Column(name = "total_donations")
    @NotNull
    protected Float totalDonations = 0f;

    @Transient
    private GenericModel organization;

    @Transient
    private GenericModel rules;

    @Transient
    private GenericModel sponsor;

    public static BudgetModel fromEntity(BudgetEntity entity) {
        BudgetModel model = new BudgetModel();
        model.setCreatedAt(entity.getCreatedAt());
        model.setCreatedBy(entity.getCreatedBy());
        model.setUpdatedAt(entity.getUpdatedAt());
        model.setUpdatedBy(entity.getUpdatedBy());
        model.setId(entity.getId());
        model.setName(entity.getName());
        model.setAmountPerMember(entity.getAmountPerMember());
        model.setCanFinanceTime(entity.getCanFinanceTime());
        model.setCostOfDay(entity.getCostOfDay());
        model.setCostOfHour(entity.getCostOfHour());
        model.setIsDistributed(entity.getIsDistributed());
        model.setStartDate(entity.getStartDate());
        model.setEndDate(entity.getEndDate());
        model.setTotalDonations(entity.getTotalDonations());
        model.setOrganization(new GenericModel(entity.getOrganization()));
        model.setRules(new GenericModel(entity.getRules()));
        model.setSponsor(new GenericModel(entity.getSponsor()));
        return model;
    }

}

package fr.lesprojetscagnottes.core.donation.entity;

import fr.lesprojetscagnottes.core.account.entity.AccountEntity;
import fr.lesprojetscagnottes.core.campaign.entity.CampaignEntity;
import fr.lesprojetscagnottes.core.donation.model.DonationModel;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Getter(AccessLevel.PUBLIC)
@Setter(AccessLevel.PUBLIC)
@Entity
@Table(name = "donations")
public class Donation extends DonationModel {

    @ManyToOne
    private CampaignEntity campaign = new CampaignEntity();

    @ManyToOne
    private AccountEntity account = new AccountEntity();

}

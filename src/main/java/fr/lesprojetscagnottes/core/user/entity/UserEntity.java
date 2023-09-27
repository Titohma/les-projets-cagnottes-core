package fr.lesprojetscagnottes.core.user.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fr.lesprojetscagnottes.core.account.entity.AccountEntity;
import fr.lesprojetscagnottes.core.authentication.AuthenticationResponseEntity;
import fr.lesprojetscagnottes.core.authorization.entity.AuthorityEntity;
import fr.lesprojetscagnottes.core.authorization.entity.OrganizationAuthorityEntity;
import fr.lesprojetscagnottes.core.budget.entity.BudgetEntity;
import fr.lesprojetscagnottes.core.news.entity.NewsEntity;
import fr.lesprojetscagnottes.core.organization.entity.OrganizationEntity;
import fr.lesprojetscagnottes.core.project.entity.ProjectEntity;
import fr.lesprojetscagnottes.core.providers.slack.entity.SlackUserEntity;
import fr.lesprojetscagnottes.core.user.model.UserModel;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import jakarta.persistence.*;
import java.io.Serial;
import java.util.LinkedHashSet;
import java.util.Set;

@Setter(AccessLevel.PUBLIC)
@Getter(AccessLevel.PUBLIC)
@Entity
@Table(name = "users")
public class UserEntity extends UserModel implements UserDetails {

    @Serial
    private static final long serialVersionUID = 6210782306288115135L;

    @ManyToMany(fetch=FetchType.LAZY)
    @JoinTable(
            name = "user_authority",
            joinColumns = {@JoinColumn(name = "user_id", referencedColumnName = "id")},
            inverseJoinColumns = {@JoinColumn(name = "authority_id", referencedColumnName = "id")})
    private Set<AuthorityEntity> userAuthorities = new LinkedHashSet<>();

    @ManyToMany(fetch=FetchType.LAZY)
    @JoinTable(
            name = "user_authority_organizations",
            joinColumns = {@JoinColumn(name = "user_id", referencedColumnName = "id")},
            inverseJoinColumns = {@JoinColumn(name = "organization_authority_id", referencedColumnName = "id")})
    private Set<OrganizationAuthorityEntity> userOrganizationAuthorities = new LinkedHashSet<>();

    @Transient
    private Set<SimpleGrantedAuthority> authorities = new LinkedHashSet<>();

    @OneToMany(mappedBy = "owner", fetch = FetchType.LAZY)
    private Set<AccountEntity> accounts = new LinkedHashSet<>();

    @ManyToMany(mappedBy = "members", fetch = FetchType.LAZY)
    private Set<OrganizationEntity> organizations = new LinkedHashSet<>();

    @OneToMany(mappedBy = "sponsor", fetch = FetchType.LAZY)
    private Set<BudgetEntity> budgets = new LinkedHashSet<>();

    @ManyToMany(mappedBy = "peopleGivingTime", fetch = FetchType.LAZY)
    private Set<ProjectEntity> projects = new LinkedHashSet<>();

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private Set<SlackUserEntity> slackUsers = new LinkedHashSet<>();

    @OneToMany(mappedBy = "user", orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<AuthenticationResponseEntity> apiTokens = new LinkedHashSet<>();

    @OneToMany( mappedBy = "author", orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<NewsEntity> news = new LinkedHashSet<>();

    public UserEntity() {
    }

    public UserEntity(String email, String password) {
        this.email = email;
        this.password = password;
    }

    @JsonIgnore
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @JsonIgnore
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @JsonIgnore
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public void addAuthority(AuthorityEntity authority) {
        userAuthorities.add(authority);
    }

}

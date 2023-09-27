package fr.lesprojetscagnottes.core.user.model;

import fr.lesprojetscagnottes.core.common.audit.AuditEntity;
import fr.lesprojetscagnottes.core.common.strings.StringsCommon;
import fr.lesprojetscagnottes.core.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import jakarta.validation.constraints.NotNull;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;

@Slf4j
@Getter(AccessLevel.PUBLIC)
@Setter(AccessLevel.PUBLIC)
@MappedSuperclass
public class UserModel extends AuditEntity<String> {

    @Column(name = "username")
    protected String username;

    @Column(name = "password")
    @NotNull
    protected String password;

    @Column(name = "email", unique = true)
    @NotNull
    protected String email;

    @Column(name = "firstname")
    protected String firstname = StringsCommon.EMPTY_STRING;

    @Column(name = "lastname")
    protected String lastname = StringsCommon.EMPTY_STRING;

    @Column(name = "avatarUrl")
    protected String avatarUrl;

    @Column(name = "enabled")
    @NotNull
    protected Boolean enabled = false;

    @Column(name = "lastpasswordresetdate")
    @Temporal(TemporalType.TIMESTAMP)
    @NotNull
    protected Date lastPasswordResetDate = new Date();

    @Transient
    private Set<Long> userAuthoritiesRef = new LinkedHashSet<>();

    @Transient
    private Set<Long> userOrganizationAuthoritiesRef = new LinkedHashSet<>();

    @Transient
    private Set<Long> accountsRef = new LinkedHashSet<>();

    @Transient
    private Set<Long> organizationsRef = new LinkedHashSet<>();

    @Transient
    private Set<Long> budgetsRef = new LinkedHashSet<>();

    @Transient
    private Set<Long> projectsRef = new LinkedHashSet<>();

    @Transient
    private Set<Long> donationsRef = new LinkedHashSet<>();

    @Transient
    private  Set<Long> slackUsersRef = new LinkedHashSet<>();

    @Transient
    private  Set<Long> apiTokensRef = new LinkedHashSet<>();

    public static UserModel fromEntity(UserEntity entity) {
        UserModel model = new UserModel();
        model.setCreatedAt(entity.getCreatedAt());
        model.setCreatedBy(entity.getCreatedBy());
        model.setUpdatedAt(entity.getUpdatedAt());
        model.setUpdatedBy(entity.getUpdatedBy());
        model.setId(entity.getId());
        model.setUsername(entity.getUsername());
        model.setEmail(entity.getEmail());
        model.setFirstname(entity.getFirstname());
        model.setLastname(entity.getLastname());
        model.setAvatarUrl(entity.getAvatarUrl());
        model.setEnabled(entity.getEnabled());
        model.setLastPasswordResetDate(entity.getLastPasswordResetDate());
        entity.getUserAuthorities().forEach(item -> model.getUserAuthoritiesRef().add(item.getId()));
        entity.getUserOrganizationAuthorities().forEach(item -> model.getUserOrganizationAuthoritiesRef().add(item.getId()));
        return model;
    }

    public String getFullname() {
        return this.firstname + StringsCommon.SPACE + this.lastname;
    }

    public void emptyPassword() {
        this.password = "***";
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("UserModel{");
        sb.append("id='").append(id).append('\'');
        sb.append(", createdAt='").append(getCreatedAt()).append('\'');
        sb.append(", createdBy='").append(getCreatedBy()).append('\'');
        sb.append(", updatedAt='").append(getUpdatedAt()).append('\'');
        sb.append(", updatedBy='").append(getUpdatedBy()).append('\'');
        sb.append(", username='").append(username).append('\'');
        sb.append(", password='").append(password).append('\'');
        sb.append(", email='").append(email).append('\'');
        sb.append(", firstname='").append(firstname).append('\'');
        sb.append(", lastname='").append(lastname).append('\'');
        sb.append(", avatarUrl='").append(avatarUrl).append('\'');
        sb.append(", enabled=").append(enabled);
        sb.append(", lastPasswordResetDate=").append(lastPasswordResetDate);
        sb.append('}');
        return sb.toString();
    }
}

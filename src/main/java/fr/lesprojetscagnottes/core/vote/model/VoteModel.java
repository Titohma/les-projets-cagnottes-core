package fr.lesprojetscagnottes.core.vote.model;

import fr.lesprojetscagnottes.core.common.GenericModel;
import fr.lesprojetscagnottes.core.common.audit.AuditEntity;
import fr.lesprojetscagnottes.core.vote.entity.VoteEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter(AccessLevel.PUBLIC)
@Setter(AccessLevel.PUBLIC)
@MappedSuperclass
public class VoteModel extends AuditEntity<String> {

    @Column(length = 50)
    @NotNull
    @Enumerated(EnumType.STRING)
    protected VoteType type = VoteType.UP;

    @Transient
    protected GenericModel project;

    @Transient
    protected GenericModel user;

    public static VoteModel fromEntity(VoteEntity entity) {
        VoteModel model = new VoteModel();
        model.setCreatedAt(entity.getCreatedAt());
        model.setCreatedBy(entity.getCreatedBy());
        model.setUpdatedAt(entity.getUpdatedAt());
        model.setUpdatedBy(entity.getUpdatedBy());
        model.setId(entity.getId());
        model.setType(entity.getType());
        model.setProject(new GenericModel(entity.getProject()));
        model.setUser(new GenericModel(entity.getUser()));
        return model;
    }

}

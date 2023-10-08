package fr.lesprojetscagnottes.core.vote.entity;

import fr.lesprojetscagnottes.core.project.entity.ProjectEntity;
import fr.lesprojetscagnottes.core.user.entity.UserEntity;
import fr.lesprojetscagnottes.core.vote.model.VoteModel;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter(AccessLevel.PUBLIC)
@Setter(AccessLevel.PUBLIC)
@Entity
@Table(name = "votes")
public class VoteEntity extends VoteModel {

    @ManyToOne(fetch = FetchType.LAZY)
    private ProjectEntity project = new ProjectEntity();

    @ManyToOne(fetch = FetchType.LAZY)
    private UserEntity user = new UserEntity();

}

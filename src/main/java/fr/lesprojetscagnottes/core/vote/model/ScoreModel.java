package fr.lesprojetscagnottes.core.vote.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter(AccessLevel.PUBLIC)
@Setter(AccessLevel.PUBLIC)
public class ScoreModel {

    private Long projectId;
    private Long up;
    private Long down;

}

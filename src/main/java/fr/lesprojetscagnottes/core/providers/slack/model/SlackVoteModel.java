package fr.lesprojetscagnottes.core.providers.slack.model;

import fr.lesprojetscagnottes.core.common.strings.StringsCommon;
import fr.lesprojetscagnottes.core.vote.model.VoteType;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter(AccessLevel.PUBLIC)
@Setter(AccessLevel.PUBLIC)
public class SlackVoteModel {

    private VoteType type = VoteType.UP;

    private Long projectId = 0L;

    private String slackUserId = StringsCommon.EMPTY_STRING;

    @Override
    public String toString() {
        return "SlackVoteModel{" +
                "type=" + type +
                ", projectId=" + projectId +
                ", slackUserId='" + slackUserId + '\'' +
                '}';
    }
}

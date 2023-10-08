package fr.lesprojetscagnottes.core.vote.controller;

import fr.lesprojetscagnottes.core.vote.model.ScoreModel;
import fr.lesprojetscagnottes.core.vote.model.VoteModel;
import fr.lesprojetscagnottes.core.vote.service.VoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/api")
@Tag(name = "Votes", description = "The Votes API")
public class VoteController {

    private final VoteService voteService;

    @Autowired
    public VoteController(VoteService voteService) {
        this.voteService = voteService;
    }

    @Operation(summary = "Get score for a project", description = "Get score for a project by its ID", tags = { "Votes" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Return the score", content = @Content(schema = @Schema(implementation = ScoreModel.class))),
            @ApiResponse(responseCode = "400", description = "ID is incorrect", content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "403", description = "Principal has not enough privileges", content = @Content(schema = @Schema()))
    })
    @PreAuthorize("hasRole('USER')")
    @RequestMapping(value = "/vote/score", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE, params = {"projectId"})
    public ScoreModel getScoreByProjectId(Principal principal, @RequestParam("projectId") Long projectId) {
        return voteService.getScoreByProjectId(principal, projectId);
    }

    @Operation(summary = "Get score for a list of projects", description = "Get score for a list of projects by a list of IDs", tags = { "Votes" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Return the scores", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ScoreModel.class)))),
            @ApiResponse(responseCode = "400", description = "ID is incorrect", content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "403", description = "Principal has not enough privileges", content = @Content(schema = @Schema()))
    })
    @PreAuthorize("hasRole('USER')")
    @RequestMapping(value = "/vote/score", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE, params = {"projectIds"})
    public Set<ScoreModel> getScoreByProjectIds(Principal principal, @RequestParam("projectIds") Set<Long> projectIds) {
        return voteService.getScoreByProjectIds(principal, projectIds);
    }

    @Operation(summary = "Get user vote for a project", description = "Get user vote for a project by its ID", tags = { "Votes" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Return the score", content = @Content(schema = @Schema(implementation = VoteModel.class))),
            @ApiResponse(responseCode = "400", description = "ID is incorrect", content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "403", description = "Principal has not enough privileges", content = @Content(schema = @Schema()))
    })
    @PreAuthorize("hasRole('USER')")
    @RequestMapping(value = "/vote", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE, params = {"projectId"})
    public VoteModel getUserVote(Principal principal, @RequestParam("projectId") Long projectId) {
        return voteService.getUserVote(principal, projectId);
    }

    @Operation(summary = "Submit or update a vote", description = "Submit or update a vote", tags = { "Votes" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Vote processed", content = @Content(schema = @Schema(implementation = VoteModel.class))),
            @ApiResponse(responseCode = "400", description = "Some references are missing", content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "403", description = "Some references doesn't exist", content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "404", description = "Principal has not enough privileges", content = @Content(schema = @Schema()))
    })
    @PreAuthorize("hasRole('USER')")
    @RequestMapping(value = "/vote", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public VoteModel vote(Principal principal, @RequestBody VoteModel voteModel) {
        return voteService.vote(principal, voteModel);
    }

}

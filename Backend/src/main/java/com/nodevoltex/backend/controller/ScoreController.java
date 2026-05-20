package com.nodevoltex.backend.controller;

import com.nodevoltex.backend.dto.LeaderboardEntry;
import com.nodevoltex.backend.dto.ScoreRequest;
import com.nodevoltex.backend.service.ScoreService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/scores")
public class ScoreController {

    private final ScoreService scoreService;

    public ScoreController(ScoreService scoreService) {
        this.scoreService = scoreService;
    }

    @PostMapping("/submit")
    public ResponseEntity<String> submitScore(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody ScoreRequest request
    ) {
        scoreService.submitScore(userDetails.getUsername(), request);
        return ResponseEntity.ok("Score submitted successfully");
    }

    @GetMapping("/leaderboard/{mapId}")
    public ResponseEntity<List<LeaderboardEntry>> getLeaderboard(@PathVariable String mapId) {
        return ResponseEntity.ok(scoreService.getLeaderboard(mapId));
    }
}

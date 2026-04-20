package com.jadiss.cldc.backend.controller;

import com.jadiss.cldc.backend.service.LeaderboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/leaderboard")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    public LeaderboardController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    // Business logic: Return top-N ranking rows and optionally include current player's own rank.
    @GetMapping
    public Map<String, Object> getLeaderboard(
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(required = false) Long sessionId
    ) {
        return leaderboardService.getLeaderboard(limit, sessionId);
    }
}

package com.jadiss.cldc.backend.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class LeaderboardService {

    private final JdbcTemplate jdbcTemplate;

    public LeaderboardService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Business logic: Build leaderboard payload including top ranks and optional current-user rank detail.
    public Map<String, Object> getLeaderboard(int limit, Long sessionId) {
        int safeLimit = Math.max(1, Math.min(limit, 100));

        List<Map<String, Object>> topRanks = jdbcTemplate.queryForList(
                """
                WITH ranked AS (
                    SELECT
                        s.session_id,
                        s.nickname_display,
                        s.country_code,
                        COALESCE(s.final_capital, s.initial_capital) AS final_capital,
                        COALESCE(s.total_return_rate, 0) AS total_return_rate,
                        ROW_NUMBER() OVER (
                            ORDER BY COALESCE(s.final_capital, s.initial_capital) DESC,
                                     COALESCE(s.total_return_rate, 0) DESC,
                                     s.session_id ASC
                        ) AS rank_no,
                        COUNT(*) OVER () AS total_players
                    FROM game_sessions s
                    WHERE s.solved_count > 0
                )
                SELECT
                    rank_no,
                    session_id,
                    nickname_display,
                    country_code,
                    final_capital,
                    total_return_rate,
                    ROUND((rank_no::numeric / NULLIF(total_players, 0)) * 100, 3) AS top_percent
                FROM ranked
                WHERE rank_no <= ?
                ORDER BY rank_no
                """,
                safeLimit
        );

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("snapshotTime", LocalDateTime.now());
        response.put("limit", safeLimit);
        response.put("rows", topRanks);

        if (sessionId != null) {
            List<Map<String, Object>> ownRankRows = jdbcTemplate.queryForList(
                    """
                    WITH ranked AS (
                        SELECT
                            s.session_id,
                            s.nickname_display,
                            s.country_code,
                            COALESCE(s.final_capital, s.initial_capital) AS final_capital,
                            COALESCE(s.total_return_rate, 0) AS total_return_rate,
                            ROW_NUMBER() OVER (
                                ORDER BY COALESCE(s.final_capital, s.initial_capital) DESC,
                                         COALESCE(s.total_return_rate, 0) DESC,
                                         s.session_id ASC
                            ) AS rank_no,
                            COUNT(*) OVER () AS total_players
                        FROM game_sessions s
                        WHERE s.solved_count > 0
                    )
                    SELECT
                        rank_no,
                        session_id,
                        nickname_display,
                        country_code,
                        final_capital,
                        total_return_rate,
                        ROUND((rank_no::numeric / NULLIF(total_players, 0)) * 100, 3) AS top_percent
                    FROM ranked
                    WHERE session_id = ?
                    """,
                    sessionId
            );

            response.put("myRank", ownRankRows.isEmpty() ? null : ownRankRows.get(0));
        }

        return response;
    }
}

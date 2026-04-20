package com.jadiss.cldc.backend.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AiUsageService {

    private final JdbcTemplate jdbcTemplate;

    public AiUsageService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Business logic: Persist per-request AI token usage so monthly billing can be calculated later.
    public Map<String, Object> recordUsage(String userId,
                                           String workspaceId,
                                           String modelName,
                                           String requestId,
                                           int promptTokens,
                                           int completionTokens,
                                           LocalDateTime occurredAt) {
        int totalTokens = promptTokens + completionTokens;

        jdbcTemplate.update(
                """
                INSERT INTO ai_token_usage
                (user_id, workspace_id, model_name, request_id, prompt_tokens, completion_tokens, total_tokens, occurred_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                userId,
                workspaceId,
                modelName,
                requestId,
                promptTokens,
                completionTokens,
                totalTokens,
                Timestamp.valueOf(occurredAt)
        );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "RECORDED");
        result.put("userId", userId);
        result.put("modelName", modelName);
        result.put("totalTokens", totalTokens);
        return result;
    }

    // Business logic: Aggregate token usage for one user and period to support invoice generation.
    public Map<String, Object> summarizeUsage(String userId, LocalDateTime from, LocalDateTime to) {
        Map<String, Object> row = jdbcTemplate.queryForMap(
                """
                SELECT
                  COALESCE(SUM(prompt_tokens), 0) AS prompt_tokens,
                  COALESCE(SUM(completion_tokens), 0) AS completion_tokens,
                  COALESCE(SUM(total_tokens), 0) AS total_tokens,
                  COALESCE(SUM(billed_amount), 0) AS billed_amount
                FROM ai_token_usage
                WHERE user_id = ?
                  AND occurred_at >= ?
                  AND occurred_at <= ?
                """,
                userId,
                Timestamp.valueOf(from),
                Timestamp.valueOf(to)
        );

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("userId", userId);
        response.put("from", from);
        response.put("to", to);
        response.put("promptTokens", ((Number) row.get("prompt_tokens")).intValue());
        response.put("completionTokens", ((Number) row.get("completion_tokens")).intValue());
        response.put("totalTokens", ((Number) row.get("total_tokens")).intValue());
        response.put("billedAmount", (BigDecimal) row.get("billed_amount"));
        return response;
    }
}

package com.jadiss.cldc.backend.controller;

import com.jadiss.cldc.backend.service.AiUsageService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/usage/ai-tokens")
public class AiUsageController {

    private final AiUsageService aiUsageService;

    public AiUsageController(AiUsageService aiUsageService) {
        this.aiUsageService = aiUsageService;
    }

    // Business logic: Record one AI usage event linked to JWT-authenticated consumer identity from Kong headers.
    @PostMapping
    public Map<String, Object> recordUsage(
            @RequestHeader(name = "X-Consumer-Username", required = false) String consumerUsername,
            @Valid @RequestBody UsageRecordRequest request
    ) {
        String userId = request.userId().isBlank() ? (consumerUsername == null ? "anonymous" : consumerUsername) : request.userId();
        return aiUsageService.recordUsage(
                userId,
                request.workspaceId(),
                request.modelName(),
                request.requestId(),
                request.promptTokens(),
                request.completionTokens(),
                request.occurredAt()
        );
    }

    // Business logic: Return usage summary for billing and quota dashboards.
    @GetMapping("/summary")
    public Map<String, Object> summarizeUsage(
            @RequestParam String userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        return aiUsageService.summarizeUsage(userId, from, to);
    }

    public record UsageRecordRequest(
            String userId,
            String workspaceId,
            @NotBlank String modelName,
            String requestId,
            @Min(0) int promptTokens,
            @Min(0) int completionTokens,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime occurredAt
    ) {
        public UsageRecordRequest {
            if (occurredAt == null) {
                occurredAt = LocalDateTime.now();
            }
            if (userId == null) {
                userId = "";
            }
        }
    }
}

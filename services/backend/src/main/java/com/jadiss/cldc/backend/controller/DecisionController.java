package com.jadiss.cldc.backend.controller;

import com.jadiss.cldc.backend.service.DecisionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/decisions")
public class DecisionController {

    private final DecisionService decisionService;

    public DecisionController(DecisionService decisionService) {
        this.decisionService = decisionService;
    }

    // Business logic: Capture user trading action and rationale as a training-data record for future model learning.
    @PostMapping
    public Map<String, Object> saveDecision(@Valid @RequestBody SaveDecisionRequest request) {
        return decisionService.saveDecision(
                request.userId(),
                request.stockCode(),
                request.decisionType(),
                request.decisionRatio(),
                request.decisionReason(),
                request.usedIndicators()
        );
    }

    public record SaveDecisionRequest(
            @NotBlank String userId,
            @NotBlank String stockCode,
            @NotBlank String decisionType,
            @Min(0) @Max(100) double decisionRatio,
            String decisionReason,
            @NotEmpty List<String> usedIndicators
    ) {
    }
}

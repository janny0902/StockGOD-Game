package com.jadiss.cldc.backend.controller;

import com.jadiss.cldc.backend.service.GameService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/game")
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    // Business logic: Start an anonymous play session with nickname#tag assignment and country-code-only persistence.
    @PostMapping("/sessions")
    public Map<String, Object> createSession(@Valid @RequestBody CreateSessionRequest request, HttpServletRequest httpRequest) {
        String countryCode = extractCountryCode(httpRequest);
        return gameService.createSession(request.nickname(), request.languageCode(), countryCode);
    }

    // Business logic: Return current session progress for resume after disconnects.
    @GetMapping("/sessions/{sessionId}")
    public Map<String, Object> getSession(@PathVariable long sessionId) {
        return gameService.getSessionState(sessionId);
    }

    // Business logic: Fetch or create the next random chart problem for this session.
    @GetMapping("/problems/next")
    public Map<String, Object> getNextProblem(@RequestParam long sessionId) {
        return gameService.getOrCreateNextProblem(sessionId);
    }

    // Business logic: Save user answer, reason labels, and indicator usage with one submission.
    @PostMapping("/attempts/{attemptId}/submit")
    public Map<String, Object> submitAttempt(@PathVariable long attemptId, @Valid @RequestBody SubmitAttemptRequest request) {
        GameService.SubmitAttemptCommand command = new GameService.SubmitAttemptCommand(
                request.actionType(),
                request.actionRatio(),
                request.answerHorizonDays(),
                request.reasonText(),
                request.reasonCards(),
                request.indicatorKeys(),
                request.indicatorValues(),
                request.isTimedOut()
        );
        return gameService.submitAttempt(attemptId, command);
    }

    private String extractCountryCode(HttpServletRequest request) {
        String country = firstNonBlank(
                request.getHeader("CF-IPCountry"),
                request.getHeader("X-Country-Code"),
                request.getHeader("X-App-Country")
        );

        if (country == null) {
            return "ZZ";
        }

        String normalized = country.trim().toUpperCase(Locale.ROOT);
        return normalized.length() == 2 ? normalized : "ZZ";
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    public record CreateSessionRequest(
            @NotBlank @Size(max = 40) String nickname,
            @Size(max = 8) String languageCode
    ) {
    }

    public record SubmitAttemptRequest(
            @NotBlank String actionType,
            @Min(0) @Max(100) double actionRatio,
            @Min(1) @Max(30) int answerHorizonDays,
            String reasonText,
            List<String> reasonCards,
            List<String> indicatorKeys,
            Map<String, Object> indicatorValues,
            Boolean timedOutFlag
    ) {
        public boolean isTimedOut() {
            return timedOutFlag != null && timedOutFlag;
        }
    }
}

package com.jadiss.cldc.backend.service;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class GameService {

    private static final int MAX_PROBLEMS_PER_SESSION = 15;

    private final JdbcTemplate jdbcTemplate;
    private final String stockNameDbUrl;
    private final String stockNameDbUsername;
    private final String stockNameDbPassword;

    public GameService(
            JdbcTemplate jdbcTemplate,
            @Value("${spring.datasource.url}") String mainDbUrl,
            @Value("${spring.datasource.username}") String mainDbUsername,
            @Value("${spring.datasource.password}") String mainDbPassword,
            @Value("${app.stock-name-db.url:}") String stockNameDbUrl,
            @Value("${app.stock-name-db.username:}") String stockNameDbUsername,
            @Value("${app.stock-name-db.password:}") String stockNameDbPassword
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.stockNameDbUrl = isBlank(stockNameDbUrl) ? switchJdbcDatabase(mainDbUrl, "cldc_batch") : stockNameDbUrl;
        this.stockNameDbUsername = isBlank(stockNameDbUsername) ? mainDbUsername : stockNameDbUsername;
        this.stockNameDbPassword = isBlank(stockNameDbPassword) ? mainDbPassword : stockNameDbPassword;
    }

    @Transactional
    public Map<String, Object> createSession(String nickname, String languageCode, String countryCode) {
        String nicknameBase = normalizeNickname(nickname);
        String normalizedLanguageCode = normalizeLanguageCode(languageCode);
        String normalizedCountryCode = normalizeCountryCode(countryCode);

        for (int retry = 0; retry < 5; retry++) {
            Integer nextTag = jdbcTemplate.queryForObject(
                    """
                    SELECT COALESCE(MAX(nickname_tag), 0) + 1
                    FROM game_sessions
                    WHERE nickname_base = ?
                    """,
                    Integer.class,
                    nicknameBase
            );

            int tag = nextTag == null ? 1 : nextTag;
            String displayName = nicknameBase + "#" + tag;

            try {
                Long sessionId = jdbcTemplate.queryForObject(
                        """
                        INSERT INTO game_sessions (nickname_base, nickname_tag, nickname_display, language_code, country_code)
                        VALUES (?, ?, ?, ?, ?)
                        RETURNING session_id
                        """,
                        Long.class,
                        nicknameBase,
                        tag,
                        displayName,
                        normalizedLanguageCode,
                        normalizedCountryCode
                );

                return getSessionState(Objects.requireNonNull(sessionId));
            } catch (DuplicateKeyException ignored) {
                // Retry when nickname#tag races with another request.
            }
        }

        throw new IllegalStateException("Failed to assign a unique nickname tag. Try again.");
    }

    public Map<String, Object> getSessionState(long sessionId) {
        Map<String, Object> session = jdbcTemplate.queryForMap(
                """
                SELECT session_id, nickname_display, language_code, country_code,
                       started_at, ended_at, initial_capital, final_capital, total_return_rate,
                   solved_count, status, problem_set_id
                FROM game_sessions
                WHERE session_id = ?
                """,
                sessionId
        );

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("sessionId", session.get("session_id"));
        response.put("nickname", session.get("nickname_display"));
        response.put("languageCode", session.get("language_code"));
        response.put("countryCode", session.get("country_code"));
        response.put("startedAt", session.get("started_at"));
        response.put("endedAt", session.get("ended_at"));
        response.put("initialCapital", session.get("initial_capital"));
        response.put("finalCapital", session.get("final_capital"));
        response.put("totalReturnRate", session.get("total_return_rate"));
        response.put("solvedCount", session.get("solved_count"));
        response.put("status", session.get("status"));
        response.put("problemSetId", session.get("problem_set_id"));
        response.put("maxProblems", MAX_PROBLEMS_PER_SESSION);
        return response;
    }

    @Transactional
    public Map<String, Object> getOrCreateNextProblem(long sessionId) {
        getSessionState(sessionId);

        Map<String, Object> inProgressAttempt = findInProgressAttempt(sessionId);
        if (inProgressAttempt != null) {
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("resumed", true);
            response.putAll(inProgressAttempt);
            return response;
        }

        Integer solvedCount = jdbcTemplate.queryForObject(
                """
                SELECT COALESCE(COUNT(*), 0)
                FROM session_problem_attempts
                WHERE session_id = ? AND status = 'SUBMITTED'
                """,
                Integer.class,
                sessionId
        );

        int solved = solvedCount == null ? 0 : solvedCount;
        if (solved >= MAX_PROBLEMS_PER_SESSION) {
            return Map.of(
                    "completed", true,
                    "message", "Session already completed"
            );
        }

        Long assignedSetId = jdbcTemplate.queryForObject(
            "SELECT problem_set_id FROM game_sessions WHERE session_id = ?",
            Long.class,
            sessionId
        );

        if (assignedSetId == null) {
            assignedSetId = assignRandomProblemSet(sessionId);
        }

        Map<String, Object> problem = findProblemBySetSequence(assignedSetId, solved + 1);
        if (problem == null) {
            return Map.of(
                    "completed", false,
                "message", "No problem set items are available yet"
            );
        }

        BigDecimal capitalBefore = findCurrentCapital(sessionId);
        int sequenceNo = solved + 1;

        Long attemptId = jdbcTemplate.queryForObject(
                """
                INSERT INTO session_problem_attempts (
                    session_id, problem_id, sequence_no, answer_horizon_days,
                    action_type, action_ratio, reason_card_used,
                    capital_before, status
                )
                VALUES (?, ?, ?, 7, 'HOLD', 0, false, ?, 'IN_PROGRESS')
                RETURNING attempt_id
                """,
                Long.class,
                sessionId,
                ((Number) problem.get("problem_id")).longValue(),
                sequenceNo,
                capitalBefore
        );

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("resumed", false);
        response.put("attemptId", attemptId);
        response.put("sessionId", sessionId);
        response.put("sequenceNo", sequenceNo);
        response.put("problemSetId", assignedSetId);
        response.put("capitalBefore", capitalBefore);
        response.put("problem", mapProblem(problem));
        return response;
    }

    @Transactional
    public Map<String, Object> submitAttempt(long attemptId, SubmitAttemptCommand command) {
        String actionType = normalizeActionType(command.actionType());
        BigDecimal actionRatio = normalizeRatio(command.actionRatio());
        int answerHorizonDays = normalizeHorizonDays(command.answerHorizonDays());
        List<String> reasonCards = command.reasonCards() == null ? List.of() : command.reasonCards();
        List<String> indicatorKeys = command.indicatorKeys() == null ? List.of() : command.indicatorKeys();

        boolean hasReasonCard = reasonCards.stream().anyMatch(value -> value != null && !value.isBlank());
        boolean hasReasonText = command.reasonText() != null && !command.reasonText().isBlank();
        if (!hasReasonCard && !hasReasonText) {
            throw new IllegalArgumentException("Reason is required: choose at least one reason card or provide text.");
        }

        Map<String, Object> attempt = jdbcTemplate.queryForMap(
                """
                SELECT a.attempt_id, a.session_id, a.sequence_no, a.status, a.capital_before,
                       p.stock_code, p.problem_date, p.reveal_max_date,
                       s.initial_capital
                FROM session_problem_attempts a
                JOIN game_problems p ON p.problem_id = a.problem_id
                JOIN game_sessions s ON s.session_id = a.session_id
                WHERE a.attempt_id = ?
                """,
                attemptId
        );

        if (!"IN_PROGRESS".equals(attempt.get("status"))) {
            throw new IllegalStateException("This attempt is not in progress.");
        }

        LocalDate problemDate = ((Date) attempt.get("problem_date")).toLocalDate();
        LocalDate revealMaxDate = ((Date) attempt.get("reveal_max_date")).toLocalDate();
        LocalDate evaluationDate = problemDate.plusDays(answerHorizonDays);
        if (evaluationDate.isAfter(revealMaxDate)) {
            evaluationDate = revealMaxDate;
        }

        String stockCode = (String) attempt.get("stock_code");
        BigDecimal priceAtProblem = findClosePrice(stockCode, problemDate);
        BigDecimal priceAtEvaluation = findClosePrice(stockCode, evaluationDate);

        BigDecimal priceChangeRate = BigDecimal.ZERO;
        if (priceAtProblem.compareTo(BigDecimal.ZERO) > 0) {
            priceChangeRate = priceAtEvaluation.subtract(priceAtProblem)
                    .divide(priceAtProblem, 8, RoundingMode.HALF_UP);
        }

        BigDecimal effectiveExposure = BigDecimal.ZERO;
        if ("LONG".equals(actionType)) {
            effectiveExposure = actionRatio;
        } else if ("SHORT".equals(actionType)) {
            effectiveExposure = actionRatio.negate();
        }

        if (effectiveExposure.compareTo(BigDecimal.valueOf(100)) > 0) {
            effectiveExposure = BigDecimal.valueOf(100);
        }
        if (effectiveExposure.compareTo(BigDecimal.valueOf(-100)) < 0) {
            effectiveExposure = BigDecimal.valueOf(-100);
        }

        BigDecimal capitalBefore = (BigDecimal) attempt.get("capital_before");
        BigDecimal pnl = capitalBefore
                .multiply(effectiveExposure)
                .divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP)
                .multiply(priceChangeRate)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal capitalAfter = capitalBefore.add(pnl).setScale(2, RoundingMode.HALF_UP);
        BigDecimal returnRate = pnl
                .divide(capitalBefore, 8, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(4, RoundingMode.HALF_UP);

        jdbcTemplate.update(
                """
                UPDATE session_problem_attempts
                SET submitted_at = NOW(),
                    timed_out = ?,
                    answer_horizon_days = ?,
                    action_type = ?,
                    action_ratio = ?,
                    reason_text = ?,
                    reason_card_used = ?,
                    capital_after = ?,
                    return_rate = ?,
                    status = 'SUBMITTED'
                WHERE attempt_id = ?
                """,
                command.timedOut(),
                answerHorizonDays,
                actionType,
                actionRatio,
                command.reasonText(),
                hasReasonCard,
                capitalAfter,
                returnRate,
                attemptId
        );

        jdbcTemplate.update("DELETE FROM attempt_reason_cards WHERE attempt_id = ?", attemptId);
        jdbcTemplate.update("DELETE FROM attempt_indicator_usage WHERE attempt_id = ?", attemptId);

        for (String cardKey : reasonCards) {
            if (cardKey == null || cardKey.isBlank()) {
                continue;
            }
            jdbcTemplate.update(
                    "INSERT INTO attempt_reason_cards (attempt_id, card_key) VALUES (?, ?)",
                    attemptId,
                    cardKey.trim()
            );
        }

        // indicatorValues is a map like {"RSI": 65.3, "MACD": 2.1, ...}
        Map<String, Object> indicatorValues = command.indicatorValues() != null 
            ? command.indicatorValues() 
            : new HashMap<>();

        for (String indicatorKey : indicatorKeys) {
            if (indicatorKey == null || indicatorKey.isBlank()) {
                continue;
            }
            Object value = indicatorValues.get(indicatorKey);
            BigDecimal indicatorValue = null;
            if (value instanceof Number num) {
                indicatorValue = BigDecimal.valueOf(num.doubleValue());
            }
            
            jdbcTemplate.update(
                    """
                    INSERT INTO attempt_indicator_usage (attempt_id, indicator_key, usage_type, indicator_value)
                    VALUES (?, ?, 'ACTIVE', ?)
                    """,
                    attemptId,
                    indicatorKey.trim(),
                    indicatorValue
            );
        }

        long sessionId = ((Number) attempt.get("session_id")).longValue();
        Integer solvedCount = jdbcTemplate.queryForObject(
                """
                SELECT COALESCE(COUNT(*), 0)
                FROM session_problem_attempts
                WHERE session_id = ? AND status = 'SUBMITTED'
                """,
                Integer.class,
                sessionId
        );

        int solved = solvedCount == null ? 0 : solvedCount;
        BigDecimal initialCapital = (BigDecimal) attempt.get("initial_capital");
        BigDecimal totalReturnRate = capitalAfter.subtract(initialCapital)
                .divide(initialCapital, 8, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(4, RoundingMode.HALF_UP);

        jdbcTemplate.update(
                """
                UPDATE game_sessions
                SET solved_count = ?,
                    final_capital = ?,
                    total_return_rate = ?,
                    ended_at = CASE WHEN ? >= ? THEN NOW() ELSE ended_at END,
                    status = CASE WHEN ? >= ? THEN 'COMPLETED' ELSE 'ACTIVE' END
                WHERE session_id = ?
                """,
                solved,
                capitalAfter,
                totalReturnRate,
                solved,
                MAX_PROBLEMS_PER_SESSION,
                solved,
                MAX_PROBLEMS_PER_SESSION,
                sessionId
        );

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("attemptId", attemptId);
        response.put("sessionId", sessionId);
        response.put("sequenceNo", attempt.get("sequence_no"));
        response.put("actionType", actionType);
        response.put("actionRatio", actionRatio);
        response.put("evaluationDate", evaluationDate);
        response.put("priceAtProblem", priceAtProblem);
        response.put("priceAtEvaluation", priceAtEvaluation);
        response.put("capitalBefore", capitalBefore);
        response.put("capitalAfter", capitalAfter);
        response.put("problemReturnRate", returnRate);
        response.put("solvedCount", solved);
        response.put("sessionCompleted", solved >= MAX_PROBLEMS_PER_SESSION);
        response.put("sessionReturnRate", totalReturnRate);
        return response;
    }

    private String normalizeNickname(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            return "player";
        }

        String normalized = nickname.trim();
        if (normalized.length() > 40) {
            normalized = normalized.substring(0, 40);
        }
        return normalized;
    }

    private String normalizeLanguageCode(String languageCode) {
        if (languageCode == null || languageCode.isBlank()) {
            return "ko";
        }

        String normalized = languageCode.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() > 8) {
            normalized = normalized.substring(0, 8);
        }
        return normalized;
    }

    private String normalizeCountryCode(String countryCode) {
        if (countryCode == null || countryCode.isBlank()) {
            return "ZZ";
        }

        String normalized = countryCode.trim().toUpperCase(Locale.ROOT);
        if (normalized.length() != 2) {
            return "ZZ";
        }
        return normalized;
    }

    private String normalizeActionType(String actionType) {
        if (actionType == null || actionType.isBlank()) {
            throw new IllegalArgumentException("actionType is required");
        }

        String normalized = actionType.trim().toUpperCase(Locale.ROOT);
        if (!List.of("LONG", "SHORT", "HOLD").contains(normalized)) {
            throw new IllegalArgumentException("actionType must be LONG, SHORT, or HOLD");
        }
        return normalized;
    }

    private BigDecimal normalizeRatio(double ratio) {
        BigDecimal normalized = BigDecimal.valueOf(ratio).setScale(2, RoundingMode.HALF_UP);
        if (normalized.compareTo(BigDecimal.ZERO) < 0 || normalized.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("actionRatio must be between 0 and 100");
        }
        return normalized;
    }

    private int normalizeHorizonDays(int horizonDays) {
        if (horizonDays < 1 || horizonDays > 30) {
            throw new IllegalArgumentException("answerHorizonDays must be between 1 and 30");
        }
        return horizonDays;
    }

    private Map<String, Object> findInProgressAttempt(long sessionId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                """
                SELECT a.attempt_id, a.sequence_no, a.capital_before,
                       p.problem_id, p.stock_code, p.problem_date, p.reveal_max_date, p.chart_timeframe
                FROM session_problem_attempts a
                JOIN game_problems p ON p.problem_id = a.problem_id
                WHERE a.session_id = ? AND a.status = 'IN_PROGRESS'
                ORDER BY a.started_at DESC
                LIMIT 1
                """,
                sessionId
        );

        if (rows.isEmpty()) {
            return null;
        }

        Map<String, Object> row = rows.get(0);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("attemptId", row.get("attempt_id"));
        response.put("sessionId", sessionId);
        response.put("sequenceNo", row.get("sequence_no"));
        response.put("capitalBefore", row.get("capital_before"));
        response.put("problem", mapProblem(row));
        return response;
    }

    private Long assignRandomProblemSet(long sessionId) {
        List<Long> setIds = jdbcTemplate.query(
                """
                SELECT ps.problem_set_id
                FROM problem_sets ps
                JOIN (
                    SELECT psi.problem_set_id,
                           COUNT(*) AS item_count,
                           SUM(
                               CASE
                                   WHEN EXISTS (
                                       SELECT 1
                                       FROM daily_candles dc
                                       WHERE dc.stock_code = p.stock_code
                                         AND dc.base_date <= (p.problem_date - INTERVAL '180 day')
                                   ) THEN 1 ELSE 0
                               END
                           ) AS eligible_count
                    FROM problem_set_items psi
                    JOIN game_problems p ON p.problem_id = psi.problem_id
                    GROUP BY psi.problem_set_id
                ) c ON c.problem_set_id = ps.problem_set_id
                WHERE c.item_count = ?
                  AND c.eligible_count = ?
                ORDER BY RANDOM()
                LIMIT 1
                """,
                (rs, rowNum) -> rs.getLong("problem_set_id"),
                MAX_PROBLEMS_PER_SESSION,
                MAX_PROBLEMS_PER_SESSION
        );

        if (setIds.isEmpty()) {
            return null;
        }

        Long selectedSetId = setIds.get(0);
        jdbcTemplate.update(
                "UPDATE game_sessions SET problem_set_id = ? WHERE session_id = ?",
                selectedSetId,
                sessionId
        );
        return selectedSetId;
    }

    private Map<String, Object> findProblemBySetSequence(Long setId, int sequenceNo) {
        if (setId == null) {
            return null;
        }

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                """
                SELECT p.problem_id, p.stock_code, p.problem_date, p.reveal_max_date, p.chart_timeframe
                FROM problem_set_items psi
                JOIN game_problems p ON p.problem_id = psi.problem_id
                WHERE psi.problem_set_id = ?
                  AND psi.sequence_no = ?
                                    AND EXISTS (
                                            SELECT 1
                                            FROM daily_candles dc
                                            WHERE dc.stock_code = p.stock_code
                                                AND dc.base_date <= (p.problem_date - INTERVAL '180 day')
                                    )
                LIMIT 1
                """,
                setId,
                sequenceNo
        );

        return rows.isEmpty() ? null : rows.get(0);
    }

    private BigDecimal findCurrentCapital(long sessionId) {
        List<BigDecimal> latestCapital = jdbcTemplate.query(
                """
                SELECT COALESCE(capital_after, capital_before) AS capital
                FROM session_problem_attempts
                WHERE session_id = ?
                ORDER BY sequence_no DESC
                LIMIT 1
                """,
                (rs, rowNum) -> rs.getBigDecimal("capital"),
                sessionId
        );

        if (!latestCapital.isEmpty()) {
            return latestCapital.get(0);
        }

        BigDecimal initialCapital = jdbcTemplate.queryForObject(
                "SELECT initial_capital FROM game_sessions WHERE session_id = ?",
                BigDecimal.class,
                sessionId
        );

        return initialCapital == null ? BigDecimal.valueOf(10_000_000) : initialCapital;
    }

    private BigDecimal findClosePrice(String stockCode, LocalDate targetDate) {
        List<BigDecimal> prices = jdbcTemplate.query(
                """
                SELECT close_price
                FROM daily_candles
                WHERE stock_code = ? AND base_date <= ?
                ORDER BY base_date DESC
                LIMIT 1
                """,
                (rs, rowNum) -> rs.getBigDecimal("close_price"),
                stockCode,
                Date.valueOf(targetDate)
        );

        if (prices.isEmpty()) {
            return BigDecimal.ZERO;
        }

        return prices.get(0);
    }

    private Map<String, Object> mapProblem(Map<String, Object> row) {
        String stockCode = (String) row.get("stock_code");
        Map<String, Object> problem = new LinkedHashMap<>();
        problem.put("problemId", row.get("problem_id"));
        problem.put("stockCode", stockCode);
        problem.put("stockName", findStockNameByCode(stockCode));
        problem.put("problemDate", row.get("problem_date"));
        problem.put("revealMaxDate", row.get("reveal_max_date"));
        problem.put("chartTimeframe", row.get("chart_timeframe"));
        return problem;
    }

    private String findStockNameByCode(String stockCode) {
        if (stockCode == null || stockCode.isBlank()) {
            return null;
        }

        String fromCurrentDb = findStockNameFromCurrentDb(stockCode);
        if (fromCurrentDb != null && !fromCurrentDb.equals(stockCode)) {
            return fromCurrentDb;
        }

        String fromBatchDb = findStockNameFromBatchDb(stockCode);
        if (fromBatchDb != null && !fromBatchDb.isBlank()) {
            return fromBatchDb;
        }

        return stockCode;
    }

    private String findStockNameFromCurrentDb(String stockCode) {
        try {
            Boolean hasStocksTable = jdbcTemplate.queryForObject(
                    """
                    SELECT EXISTS (
                        SELECT 1
                        FROM information_schema.tables
                        WHERE table_schema = 'public' AND table_name = 'stocks'
                    )
                    """,
                    Boolean.class
            );

            if (!Boolean.TRUE.equals(hasStocksTable)) {
                return stockCode;
            }

            List<String> names = jdbcTemplate.query(
                    "SELECT stock_name FROM stocks WHERE stock_code = ? LIMIT 1",
                    (rs, rowNum) -> rs.getString("stock_name"),
                    stockCode
            );

            if (names.isEmpty() || names.get(0) == null || names.get(0).isBlank()) {
                return stockCode;
            }

            return names.get(0);
        } catch (Exception ignored) {
            return stockCode;
        }
    }

    private String findStockNameFromBatchDb(String stockCode) {
        String sql = "SELECT stock_name FROM stocks WHERE stock_code = ? LIMIT 1";
        try (
                Connection connection = DriverManager.getConnection(stockNameDbUrl, stockNameDbUsername, stockNameDbPassword);
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, stockCode);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                String name = resultSet.getString("stock_name");
                return (name == null || name.isBlank()) ? null : name;
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String switchJdbcDatabase(String jdbcUrl, String targetDbName) {
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            return jdbcUrl;
        }

        int queryIndex = jdbcUrl.indexOf('?');
        String base = queryIndex >= 0 ? jdbcUrl.substring(0, queryIndex) : jdbcUrl;
        String suffix = queryIndex >= 0 ? jdbcUrl.substring(queryIndex) : "";
        int slashIndex = base.lastIndexOf('/');
        if (slashIndex < 0) {
            return jdbcUrl;
        }

        return base.substring(0, slashIndex + 1) + targetDbName + suffix;
    }

    public record SubmitAttemptCommand(
            String actionType,
            double actionRatio,
            int answerHorizonDays,
            String reasonText,
            List<String> reasonCards,
            List<String> indicatorKeys,
            Map<String, Object> indicatorValues,
            boolean timedOut
    ) {
    }
}

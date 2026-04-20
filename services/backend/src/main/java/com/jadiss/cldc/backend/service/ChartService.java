package com.jadiss.cldc.backend.service;

import com.jadiss.cldc.backend.model.Timeframe;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ChartService {

    private static final Set<String> SUPPORTED_INDICATORS = Set.of("RSI", "MACD", "SLOW_STOCHASTIC");

    private final JdbcTemplate jdbcTemplate;

    public ChartService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Business logic: Build chart payload using DB-backed candles and indicator options requested by the client.
    public Map<String, Object> buildChartData(String stockCode,
                                              LocalDate fromDate,
                                              LocalDate toDate,
                                              Timeframe timeframe,
                                              List<String> indicators) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                """
                SELECT base_date, open_price, high_price, low_price, close_price, acc_trade_vol
                FROM daily_candles
                WHERE stock_code = ?
                  AND base_date BETWEEN ? AND ?
                ORDER BY base_date ASC
                """,
                stockCode,
                Date.valueOf(fromDate),
                Date.valueOf(toDate)
        );

        List<Candle> candles = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            candles.add(new Candle(
                    ((Date) row.get("base_date")).toLocalDate(),
                    toDecimal(row.get("open_price")),
                    toDecimal(row.get("high_price")),
                    toDecimal(row.get("low_price")),
                    toDecimal(row.get("close_price")),
                    toDecimal(row.get("acc_trade_vol"))
            ));
        }

        List<String> normalizedIndicators = indicators.stream()
                .map(v -> v == null ? "" : v.trim().toUpperCase())
                .filter(v -> !v.isBlank())
                .filter(SUPPORTED_INDICATORS::contains)
                .distinct()
                .toList();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("stockCode", stockCode);
        response.put("fromDate", fromDate);
        response.put("toDate", toDate);
        response.put("timeframe", timeframe.code());
        response.put("selectedIndicators", normalizedIndicators);
        response.put("supportedIndicators", SUPPORTED_INDICATORS);
        response.put("candles", candles.stream().map(this::toCandlePayload).toList());

        Map<String, Object> indicatorPayload = new HashMap<>();
        if (normalizedIndicators.contains("RSI")) {
            indicatorPayload.put("RSI", buildRsi(candles, 14));
        }
        if (normalizedIndicators.contains("MACD")) {
            indicatorPayload.put("MACD", buildMacd(candles, 12, 26, 9));
        }
        if (normalizedIndicators.contains("SLOW_STOCHASTIC")) {
            indicatorPayload.put("SLOW_STOCHASTIC", buildSlowStochastic(candles, 14, 3));
        }

        response.put("indicators", indicatorPayload);
        return response;
    }

    // Business logic: Expose supported chart timeframes to keep frontend selector and backend validation synchronized.
    public List<String> getSupportedTimeframes() {
        return List.of("1m", "5m", "10m", "15m", "30m", "60m", "6h", "12h", "1d", "1w", "1M");
    }

    private Map<String, Object> toCandlePayload(Candle candle) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("date", candle.date().toString());
        row.put("open", candle.open());
        row.put("high", candle.high());
        row.put("low", candle.low());
        row.put("close", candle.close());
        row.put("volume", candle.volume());
        return row;
    }

    private List<Map<String, Object>> buildRsi(List<Candle> candles, int period) {
        List<Map<String, Object>> output = new ArrayList<>();
        if (candles.size() <= period) {
            return output;
        }

        List<BigDecimal> closes = candles.stream().map(Candle::close).toList();
        BigDecimal gains = BigDecimal.ZERO;
        BigDecimal losses = BigDecimal.ZERO;

        for (int i = 1; i <= period; i++) {
            BigDecimal diff = closes.get(i).subtract(closes.get(i - 1));
            if (diff.compareTo(BigDecimal.ZERO) >= 0) {
                gains = gains.add(diff);
            } else {
                losses = losses.add(diff.abs());
            }
        }

        BigDecimal avgGain = gains.divide(BigDecimal.valueOf(period), 8, RoundingMode.HALF_UP);
        BigDecimal avgLoss = losses.divide(BigDecimal.valueOf(period), 8, RoundingMode.HALF_UP);

        for (int i = period; i < closes.size(); i++) {
            if (i > period) {
                BigDecimal diff = closes.get(i).subtract(closes.get(i - 1));
                BigDecimal gain = diff.compareTo(BigDecimal.ZERO) > 0 ? diff : BigDecimal.ZERO;
                BigDecimal loss = diff.compareTo(BigDecimal.ZERO) < 0 ? diff.abs() : BigDecimal.ZERO;

                avgGain = avgGain.multiply(BigDecimal.valueOf(period - 1)).add(gain)
                        .divide(BigDecimal.valueOf(period), 8, RoundingMode.HALF_UP);
                avgLoss = avgLoss.multiply(BigDecimal.valueOf(period - 1)).add(loss)
                        .divide(BigDecimal.valueOf(period), 8, RoundingMode.HALF_UP);
            }

            BigDecimal rsi;
            if (avgLoss.compareTo(BigDecimal.ZERO) == 0) {
                rsi = BigDecimal.valueOf(100);
            } else {
                BigDecimal rs = avgGain.divide(avgLoss, 8, RoundingMode.HALF_UP);
                rsi = BigDecimal.valueOf(100).subtract(
                        BigDecimal.valueOf(100).divide(BigDecimal.ONE.add(rs), 8, RoundingMode.HALF_UP)
                );
            }

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", candles.get(i).date().toString());
            item.put("value", rsi.setScale(4, RoundingMode.HALF_UP));
            output.add(item);
        }

        return output;
    }

    private List<Map<String, Object>> buildMacd(List<Candle> candles, int shortPeriod, int longPeriod, int signalPeriod) {
        List<Map<String, Object>> output = new ArrayList<>();
        if (candles.isEmpty()) {
            return output;
        }

        List<BigDecimal> closes = candles.stream().map(Candle::close).toList();
        List<BigDecimal> emaShort = ema(closes, shortPeriod);
        List<BigDecimal> emaLong = ema(closes, longPeriod);
        List<BigDecimal> macdLine = new ArrayList<>();

        for (int i = 0; i < closes.size(); i++) {
            BigDecimal macd = emaShort.get(i).subtract(emaLong.get(i));
            macdLine.add(macd);
        }

        List<BigDecimal> signal = ema(macdLine, signalPeriod);

        for (int i = 0; i < closes.size(); i++) {
            BigDecimal macd = macdLine.get(i);
            BigDecimal sig = signal.get(i);
            BigDecimal hist = macd.subtract(sig);

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", candles.get(i).date().toString());
            item.put("macd", macd.setScale(6, RoundingMode.HALF_UP));
            item.put("signal", sig.setScale(6, RoundingMode.HALF_UP));
            item.put("hist", hist.setScale(6, RoundingMode.HALF_UP));
            output.add(item);
        }

        return output;
    }

    private List<Map<String, Object>> buildSlowStochastic(List<Candle> candles, int lookback, int smoothPeriod) {
        List<Map<String, Object>> output = new ArrayList<>();
        if (candles.size() < lookback) {
            return output;
        }

        List<BigDecimal> kFast = new ArrayList<>();
        for (int i = 0; i < candles.size(); i++) {
            if (i < lookback - 1) {
                kFast.add(null);
                continue;
            }

            BigDecimal highest = candles.get(i).high();
            BigDecimal lowest = candles.get(i).low();
            for (int j = i - lookback + 1; j <= i; j++) {
                highest = highest.max(candles.get(j).high());
                lowest = lowest.min(candles.get(j).low());
            }

            BigDecimal denominator = highest.subtract(lowest);
            BigDecimal value = denominator.compareTo(BigDecimal.ZERO) == 0
                    ? BigDecimal.ZERO
                    : candles.get(i).close().subtract(lowest)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(denominator, 8, RoundingMode.HALF_UP);
            kFast.add(value);
        }

        List<BigDecimal> kSlow = simpleMovingAverageNullable(kFast, smoothPeriod);
        List<BigDecimal> dSlow = simpleMovingAverageNullable(kSlow, smoothPeriod);

        for (int i = 0; i < candles.size(); i++) {
            if (kSlow.get(i) == null || dSlow.get(i) == null) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", candles.get(i).date().toString());
            item.put("k", kSlow.get(i).setScale(4, RoundingMode.HALF_UP));
            item.put("d", dSlow.get(i).setScale(4, RoundingMode.HALF_UP));
            output.add(item);
        }

        return output;
    }

    private List<BigDecimal> ema(List<BigDecimal> values, int period) {
        List<BigDecimal> result = new ArrayList<>();
        if (values.isEmpty()) {
            return result;
        }

        BigDecimal multiplier = BigDecimal.valueOf(2)
                .divide(BigDecimal.valueOf(period + 1), 8, RoundingMode.HALF_UP);

        BigDecimal prev = values.get(0);
        result.add(prev);
        for (int i = 1; i < values.size(); i++) {
            BigDecimal current = values.get(i);
            BigDecimal ema = current.subtract(prev)
                    .multiply(multiplier)
                    .add(prev);
            result.add(ema);
            prev = ema;
        }
        return result;
    }

    private List<BigDecimal> simpleMovingAverageNullable(List<BigDecimal> values, int period) {
        List<BigDecimal> result = new ArrayList<>();
        for (int i = 0; i < values.size(); i++) {
            if (i < period - 1) {
                result.add(null);
                continue;
            }

            BigDecimal sum = BigDecimal.ZERO;
            boolean hasNull = false;
            for (int j = i - period + 1; j <= i; j++) {
                BigDecimal v = values.get(j);
                if (v == null) {
                    hasNull = true;
                    break;
                }
                sum = sum.add(v);
            }

            if (hasNull) {
                result.add(null);
            } else {
                result.add(sum.divide(BigDecimal.valueOf(period), 8, RoundingMode.HALF_UP));
            }
        }
        return result;
    }

    private BigDecimal toDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        return new BigDecimal(String.valueOf(value));
    }

    private record Candle(
            LocalDate date,
            BigDecimal open,
            BigDecimal high,
            BigDecimal low,
            BigDecimal close,
            BigDecimal volume
    ) {
    }
}

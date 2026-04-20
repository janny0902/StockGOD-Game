package com.jadiss.cldc.backend.controller;

import com.jadiss.cldc.backend.model.Timeframe;
import com.jadiss.cldc.backend.service.ChartService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/charts")
public class ChartController {

    private final ChartService chartService;

    public ChartController(ChartService chartService) {
        this.chartService = chartService;
    }

    // Business logic: Provide chart data for one stock and compute only indicators selected by the user.
    @GetMapping
    public Map<String, Object> getChart(
            @RequestParam @NotBlank String stockCode,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false, defaultValue = "1d") String timeframe,
            @RequestParam(required = false, defaultValue = "RSI,MACD,SLOW_STOCHASTIC") String indicators
    ) {
        List<String> selectedIndicators = Arrays.stream(indicators.split(","))
                .map(String::trim)
                .filter(v -> !v.isBlank())
                .toList();

        return chartService.buildChartData(stockCode, fromDate, toDate, Timeframe.from(timeframe), selectedIndicators);
    }

    // Business logic: Return fixed timeframe options required by chart learners for problem-solving scenarios.
    @GetMapping("/timeframes")
    public Map<String, Object> getSupportedTimeframes() {
        return Map.of("timeframes", chartService.getSupportedTimeframes());
    }
}

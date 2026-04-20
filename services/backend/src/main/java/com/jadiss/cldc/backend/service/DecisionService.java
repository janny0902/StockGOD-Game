package com.jadiss.cldc.backend.service;

import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DecisionService {

    // Business logic: Persist one user decision record and the indicator set used for this trading judgment.
    public Map<String, Object> saveDecision(String userId,
                                            String stockCode,
                                            String decisionType,
                                            double decisionRatio,
                                            String decisionReason,
                                            List<String> usedIndicators) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("userId", userId);
        response.put("stockCode", stockCode);
        response.put("decisionType", decisionType);
        response.put("decisionRatio", decisionRatio);
        response.put("decisionReason", decisionReason);
        response.put("usedIndicators", usedIndicators);
        response.put("status", "RECORDED");
        response.put("message", "Placeholder response. Replace with transactional DB insert.");
        return response;
    }
}

const baseUrl = import.meta.env.VITE_MAIN_API_BASE_URL || "http://localhost:28080";

export async function fetchHealth() {
  const response = await fetch(`${baseUrl}/health`);
  if (!response.ok) {
    throw new Error("Failed to fetch backend health");
  }
  return response.json();
}

export async function fetchSupportedTimeframes() {
  const response = await fetch(`${baseUrl}/charts/timeframes`);
  if (!response.ok) {
    throw new Error("Failed to fetch supported timeframes");
  }
  return response.json();
}

export async function fetchChartData({ stockCode, fromDate, toDate, timeframe = "1d", indicators = [] }) {
  const params = new URLSearchParams();
  params.set("stockCode", stockCode);
  params.set("fromDate", fromDate);
  params.set("toDate", toDate);
  params.set("timeframe", timeframe);
  if (indicators.length > 0) {
    params.set("indicators", indicators.join(","));
  }

  const response = await fetch(`${baseUrl}/charts?${params.toString()}`);
  if (!response.ok) {
    throw new Error("Failed to fetch chart data");
  }
  return response.json();
}

export async function createGameSession(nickname, languageCode = "ko") {
  const response = await fetch(`${baseUrl}/game/sessions`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ nickname, languageCode }),
  });
  if (!response.ok) {
    throw new Error("Failed to create game session");
  }
  return response.json();
}

export async function fetchGameSession(sessionId) {
  const response = await fetch(`${baseUrl}/game/sessions/${encodeURIComponent(sessionId)}`);
  if (!response.ok) {
    throw new Error("Failed to fetch game session");
  }
  return response.json();
}

export async function fetchNextProblem(sessionId) {
  const response = await fetch(`${baseUrl}/game/problems/next?sessionId=${encodeURIComponent(sessionId)}`);
  if (!response.ok) {
    throw new Error("Failed to fetch next problem");
  }
  return response.json();
}

export async function submitAttempt(attemptId, payload) {
  const response = await fetch(`${baseUrl}/game/attempts/${encodeURIComponent(attemptId)}/submit`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });
  if (!response.ok) {
    throw new Error("Failed to submit attempt");
  }
  return response.json();
}

export async function fetchLeaderboard(limit = 100, sessionId = null) {
  const params = new URLSearchParams();
  params.set("limit", String(limit));
  if (sessionId !== null && sessionId !== undefined) {
    params.set("sessionId", String(sessionId));
  }
  const response = await fetch(`${baseUrl}/leaderboard?${params.toString()}`);
  if (!response.ok) {
    throw new Error("Failed to fetch leaderboard");
  }
  return response.json();
}

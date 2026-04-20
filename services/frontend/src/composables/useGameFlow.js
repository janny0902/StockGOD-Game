import { ref } from "vue";
import {
  createGameSession,
  fetchChartData,
  fetchHealth,
  fetchLeaderboard,
  fetchNextProblem,
  fetchSupportedTimeframes,
  submitAttempt,
} from "../services/api";

const healthText = ref("Not loaded");
const timeframeText = ref("Not loaded");
const timeframes = ref(["1d"]);
const selectedTimeframe = ref("1d");
const startText = ref("대기 중");
const chartStatus = ref("차트를 기다리는 중");
const submitText = ref("제출 대기 중");
const revealMode = ref(false);
const sessionCompleted = ref(false);
const completedMessage = ref("");
const lastResult = ref(null);
const leaderboardRows = ref([]);
const myRankRow = ref(null);

const nickname = ref("proto");
const selectedLanguageCode = ref("ko");
const loading = ref(false);

const currentSession = ref(null);
const currentProblem = ref(null);
const currentAttemptId = ref(null);
const currentSequence = ref(0);
const lastChartPayload = ref(null);  // Store chart data for indicator extraction

const indicatorOptions = ["RSI", "MACD", "SLOW_STOCHASTIC"];
const selectedIndicators = ref([]);

const reasonCardOptions = ["TREND", "BREAKOUT", "REVERSAL", "SUPPORT_RESISTANCE", "VOLUME"];
const selectedReasonCards = ref(["TREND"]);
const reasonText = ref("추세 강도와 모멘텀을 근거로 판단했습니다.");
const actionType = ref("HOLD");
const ratioOptions = [10, 30, 50, 100];
const actionRatio = ref(30);
const answerHorizonDays = ref(30);
const evaluationDate = ref("");
const evaluationPresets = [
  { label: "1주", days: 7 },
  { label: "2주", days: 14 },
  { label: "1달", days: 30 },
];
const languageOptions = [
  { code: "us", label: "US" },
  { code: "kr", label: "KR" },
  { code: "jp", label: "JP" },
  { code: "uk", label: "UK" },
  { code: "de", label: "DE" },
];

let initialized = false;

function shiftDate(dateText, deltaDays) {
  const date = new Date(`${dateText}T00:00:00`);
  date.setDate(date.getDate() + deltaDays);
  return date.toISOString().slice(0, 10);
}

function diffDays(startDateText, endDateText) {
  const start = new Date(`${startDateText}T00:00:00`);
  const end = new Date(`${endDateText}T00:00:00`);
  return Math.round((end.getTime() - start.getTime()) / 86400000);
}

function maxEvaluationDateForProblem() {
  if (!currentProblem.value) {
    return "";
  }
  return shiftDate(currentProblem.value.problemDate, 30);
}

function formatNumber(value) {
  const number = Number(value);
  if (Number.isNaN(number)) {
    return "-";
  }
  return number.toLocaleString(undefined, { maximumFractionDigits: 2 });
}

function formatSignedPercent(value) {
  const number = Number(value);
  if (Number.isNaN(number)) {
    return "0.0";
  }
  const formatted = number.toFixed(1);
  return number > 0 ? `+${formatted}` : formatted;
}

function syncDefaultEvaluationDate() {
  if (!currentProblem.value) {
    evaluationDate.value = "";
    return;
  }
  evaluationDate.value = shiftDate(currentProblem.value.problemDate, answerHorizonDays.value || 30);
}

function syncAnswerHorizonFromDate() {
  if (!currentProblem.value || !evaluationDate.value) {
    return;
  }

  const days = diffDays(currentProblem.value.problemDate, evaluationDate.value);
  if (days < 1) {
    answerHorizonDays.value = 1;
    evaluationDate.value = shiftDate(currentProblem.value.problemDate, 1);
    return;
  }

  if (days > 30) {
    answerHorizonDays.value = 30;
    evaluationDate.value = shiftDate(currentProblem.value.problemDate, 30);
    return;
  }

  answerHorizonDays.value = days;
}

function setEvaluationPreset(days) {
  answerHorizonDays.value = days;
  if (currentProblem.value) {
    evaluationDate.value = shiftDate(currentProblem.value.problemDate, days);
  }
}

async function loadHealth() {
  try {
    const health = await fetchHealth();
    healthText.value = JSON.stringify(health, null, 2);
  } catch (error) {
    healthText.value = `Error: ${error.message}`;
  }
}

async function loadTimeframes() {
  try {
    const payload = await fetchSupportedTimeframes();
    timeframes.value = payload.timeframes || [];
    selectedTimeframe.value = timeframes.value[0] || "1d";
    timeframeText.value = JSON.stringify(payload, null, 2);
  } catch (error) {
    timeframeText.value = `Error: ${error.message}`;
  }
}

async function loadLeaderboard() {
  try {
    const payload = await fetchLeaderboard(20, currentSession.value?.sessionId ?? null);
    leaderboardRows.value = payload.rows || [];
    myRankRow.value = payload.myRank || null;
  } catch (error) {
    leaderboardRows.value = [];
    myRankRow.value = null;
  }
}

async function startSession() {
  loading.value = true;
  startText.value = "세션 생성 중...";
  submitText.value = "제출 대기 중";
  sessionCompleted.value = false;
  completedMessage.value = "";

  try {
    const session = await createGameSession(nickname.value || "player", selectedLanguageCode.value || "ko");
    currentSession.value = session;
    startText.value = `세션 생성 완료: ${session.nickname} (sessionId=${session.sessionId})`;
    await loadLeaderboard();
    await loadNextProblem();
  } catch (error) {
    startText.value = `시작 실패: ${error.message}`;
  } finally {
    loading.value = false;
  }
}

async function loadNextProblem() {
  if (!currentSession.value) {
    return;
  }

  const payload = await fetchNextProblem(currentSession.value.sessionId);
  if (payload.completed) {
    revealMode.value = false;
    currentProblem.value = null;
    currentAttemptId.value = null;
    currentSequence.value = currentSession.value.maxProblems || 15;
    chartStatus.value = payload.message || "세션 완료";
    submitText.value = "세션이 완료되었습니다.";
    sessionCompleted.value = true;
    completedMessage.value = payload.message || "15문제를 모두 완료했습니다.";
    return;
  }

  revealMode.value = false;
  lastResult.value = null;

  currentAttemptId.value = payload.attemptId;
  currentSequence.value = payload.sequenceNo;
  currentProblem.value = payload.problem;
  sessionCompleted.value = false;
  completedMessage.value = "";
  syncDefaultEvaluationDate();
  startText.value = payload.resumed
    ? `진행 중 문제를 재개했습니다. (attemptId=${payload.attemptId})`
    : `새 문제를 받았습니다. (attemptId=${payload.attemptId}, set=${payload.problemSetId})`;
}

async function loadChartBundle(isRevealChart = false) {
  if (!currentProblem.value) {
    return null;
  }

  chartStatus.value = "차트 로딩 중...";
  try {
    const fromDate = shiftDate(currentProblem.value.problemDate, -180);
    const toDate = isRevealChart && lastResult.value
      ? lastResult.value.evaluationDate
      : currentProblem.value.problemDate;

    const payload = await fetchChartData({
      stockCode: currentProblem.value.stockCode,
      fromDate,
      toDate,
      timeframe: currentProblem.value.chartTimeframe || "1d",
      indicators: selectedIndicators.value,
    });

    chartStatus.value = `캔들 ${payload.candles.length}개 로드 / 지표: ${payload.selectedIndicators.join(", ") || "없음"}`;

    if (!isRevealChart) {
      lastChartPayload.value = payload;  // Store for indicator extraction
    }

    const highlight = isRevealChart && lastResult.value
      ? {
          problemDate: currentProblem.value.problemDate,
          evaluationDate: lastResult.value.evaluationDate,
          actionType: lastResult.value.actionType,
          priceAtProblem: lastResult.value.priceAtProblem,
          priceAtEvaluation: lastResult.value.priceAtEvaluation,
        }
      : {};

    return { payload, highlight };
  } catch (error) {
    chartStatus.value = `차트 로딩 실패: ${error.message}`;
    return null;
  }
}

async function submitCurrent() {
  if (!currentAttemptId.value) {
    submitText.value = "제출할 문제가 없습니다.";
    return false;
  }

  if (selectedReasonCards.value.length === 0) {
    submitText.value = "이유카드를 하나 이상 선택해야 합니다.";
    return false;
  }

  loading.value = true;
  submitText.value = "제출 중...";
  try {
    // Extract indicator values from last chart payload
    const indicatorValues = {};
    if (lastChartPayload.value && lastChartPayload.value.indicators) {
      const indicators = lastChartPayload.value.indicators;
      
      // Extract latest values for each indicator
      if (indicators.RSI && indicators.RSI.length > 0) {
        indicatorValues.RSI = indicators.RSI[indicators.RSI.length - 1].value;
      }
      if (indicators.MACD && indicators.MACD.length > 0) {
        const latest = indicators.MACD[indicators.MACD.length - 1];
        indicatorValues.MACD = latest.macd;
      }
      if (indicators.SLOW_STOCHASTIC && indicators.SLOW_STOCHASTIC.length > 0) {
        const latest = indicators.SLOW_STOCHASTIC[indicators.SLOW_STOCHASTIC.length - 1];
        indicatorValues.SLOW_STOCHASTIC = latest.k;
      }
    }

    const result = await submitAttempt(currentAttemptId.value, {
      actionType: actionType.value,
      actionRatio: Number(actionRatio.value),
      answerHorizonDays: Number(answerHorizonDays.value),
      reasonText: "",  // No longer collecting user text
      reasonCards: selectedReasonCards.value,
      indicatorKeys: selectedIndicators.value,
      indicatorValues: indicatorValues,
      timedOutFlag: false,
    });

    lastResult.value = result;
    revealMode.value = true;
    submitText.value = `답지 공개: 문제수익률 ${result.problemReturnRate}% / 세션수익률 ${result.sessionReturnRate}%`;
    await loadLeaderboard();
    return true;
  } catch (error) {
    submitText.value = `제출 실패: ${error.message}`;
    return false;
  } finally {
    loading.value = false;
  }
}

async function nextRound() {
  if (!revealMode.value) {
    return;
  }
  await loadNextProblem();
}

function ensureInitialized() {
  if (!initialized) {
    initialized = true;
    loadLeaderboard();
  }
}

export function useGameFlow() {
  ensureInitialized();

  return {
    healthText,
    timeframeText,
    timeframes,
    selectedTimeframe,
    startText,
    chartStatus,
    submitText,
    revealMode,
    sessionCompleted,
    completedMessage,
    lastResult,
    leaderboardRows,
    myRankRow,

    nickname,
    selectedLanguageCode,
    loading,

    currentSession,
    currentProblem,
    currentAttemptId,
    currentSequence,

    indicatorOptions,
    selectedIndicators,
    reasonCardOptions,
    selectedReasonCards,
    reasonText,
    actionType,
    ratioOptions,
    actionRatio,
    answerHorizonDays,
    evaluationDate,
    evaluationPresets,
    languageOptions,

    startSession,
    loadNextProblem,
    loadChartBundle,
    submitCurrent,
    nextRound,
    loadHealth,
    loadTimeframes,
    loadLeaderboard,

    setEvaluationPreset,
    syncAnswerHorizonFromDate,
    maxEvaluationDateForProblem,
    formatNumber,
    formatSignedPercent,
  };
}

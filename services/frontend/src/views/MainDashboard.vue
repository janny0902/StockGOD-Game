<template>
  <main class="sovereign-shell">
    <header class="top-nav glass-panel">
      <div class="brand-block">
        <span class="brand-title">The Sovereign Exchange</span>
        <span class="brand-subtitle">Chart Learning Arena</span>
      </div>

      <nav class="top-nav-links">
        <a class="is-active" href="#">Market</a>
        <a href="#">Portfolio</a>
        <a href="#">Rankings</a>
        <a href="#">History</a>
        <a href="#">Intelligence</a>
      </nav>

      <div class="top-nav-actions">
        <span class="nav-chip">{{ currentSession ? currentSession.nickname : "Guest" }}</span>
        <span class="nav-chip nav-chip-accent">{{ currentSession ? `Round ${currentSequence || 0}/15` : "Standby" }}</span>
      </div>
    </header>

    <div class="page-grid">
      <section class="main-stage">
        <section class="glass-panel hero-panel" :class="{ reveal: revealMode }">
          <div class="hero-copy">
            <p class="eyebrow">{{ revealMode ? "POST-MARKET ANALYSIS" : "MISSION BRIEFING" }}</p>
            <h1>{{ revealMode ? "ANSWER REVEALED" : "Ascend to God Tier" }}</h1>
            <p>
              {{ revealMode ? "문제시점과 평가시점의 차트를 비교하고, 선택한 판단이 몇 % 수익으로 이어졌는지 확인합니다." : "로그인 없이 시작하고, 15개의 차트 문제를 풀며 AI 학습용 결정을 남깁니다." }}
            </p>
          </div>

          <div class="hero-stats">
            <div class="hero-stat">
              <span>SESSION</span>
              <strong>{{ currentSession ? currentSession.sessionId : "-" }}</strong>
            </div>
            <div class="hero-stat">
              <span>ROUND</span>
              <strong>{{ currentSequence || 0 }}/15</strong>
            </div>
            <div class="hero-stat">
              <span>STATUS</span>
              <strong>{{ loading ? "LOADING" : revealMode ? "REVEAL" : currentSession ? "READY" : "STANDBY" }}</strong>
            </div>
          </div>
        </section>

        <section v-if="!currentSession" class="glass-panel join-grid">
          <div class="join-card">
            <p class="eyebrow">SELECT DOMAIN</p>
            <div class="domain-row">
              <button
                v-for="option in languageOptions"
                :key="option.code"
                type="button"
                class="domain-chip"
                :class="{ active: selectedLanguageCode === option.code }"
                @click="selectedLanguageCode = option.code"
              >
                {{ option.label }}
              </button>
            </div>

            <label class="field-group">
              <span>Trading Alias</span>
              <div class="field-row">
                <input v-model.trim="nickname" maxlength="40" placeholder="Enter Nickname" />
                <button class="small-action" @click="startProblem" :disabled="loading">VERIFY</button>
              </div>
              <small>{{ startText }}</small>
            </label>

            <button class="start-cta" :disabled="loading" @click="startProblem">START CHALLENGE</button>
          </div>

          <div class="join-card muted">
            <p class="eyebrow">GLOBAL RANKS</p>
            <div class="leader-compact" v-if="leaderboardRows.length">
              <div v-for="row in leaderboardRows.slice(0, 5)" :key="row.session_id" class="leader-row" :class="{ top: row.rank_no === 1 }">
                <span class="leader-rank">{{ String(row.rank_no).padStart(2, "0") }}</span>
                <span class="leader-name">{{ row.nickname_display }}</span>
                <span class="leader-profit">{{ formatSignedPercent(row.total_return_rate) }}%</span>
              </div>
            </div>
            <div v-else class="empty-state">랭킹을 불러오는 중입니다.</div>
          </div>
        </section>

        <template v-else>
          <section v-if="currentProblem && !revealMode" class="stage-grid">
            <article class="glass-panel info-card">
              <p class="eyebrow">CURRENT TARGET</p>
              <h2>{{ currentProblem.stockCode ? "ASSET LOCKED" : "ASSET LOCKED" }}</h2>
              <ul class="meta-list-alt">
                <li><span>종목코드</span><strong>???</strong></li>
                <li><span>기준일</span><strong>{{ currentProblem.problemDate }}</strong></li>
                <li><span>평가한도</span><strong>{{ currentProblem.revealMaxDate }}</strong></li>
                <li><span>타임프레임</span><strong>{{ currentProblem.chartTimeframe }}</strong></li>
              </ul>
              <div class="mini-status">{{ chartStatus }}</div>
            </article>

            <article class="glass-panel action-card">
              <p class="eyebrow">DECISION CONSOLE</p>
              <div class="action-row">
                <button class="action-btn buy" :class="{ active: actionType === 'BUY' }" @click="actionType = 'BUY'">BUY</button>
                <button class="action-btn hold" :class="{ active: actionType === 'HOLD' }" @click="actionType = 'HOLD'">HOLD</button>
                <button class="action-btn sell" :class="{ active: actionType === 'SELL' }" @click="actionType = 'SELL'">SELL</button>
              </div>

              <div class="form-grid compact">
                <label>
                  비중
                  <div class="chip-wrap ratio-wrap">
                    <button
                      v-for="ratio in ratioOptions"
                      :key="ratio"
                      type="button"
                      class="chip ratio-chip"
                      :class="{ active: actionRatio === ratio }"
                      @click="actionRatio = ratio"
                    >
                      {{ ratio }}%
                    </button>
                  </div>
                </label>
                <label>
                  평가기간
                  <div class="chip-wrap ratio-wrap">
                    <button
                      v-for="preset in evaluationPresets"
                      :key="preset.days"
                      type="button"
                      class="chip ratio-chip"
                      :class="{ active: answerHorizonDays === preset.days }"
                      @click="setEvaluationPreset(preset.days)"
                    >
                      {{ preset.label }}
                    </button>
                  </div>
                  <input
                    v-model="evaluationDate"
                    type="date"
                    :min="currentProblem ? currentProblem.problemDate : undefined"
                    :max="maxEvaluationDateForProblem()"
                    @change="syncAnswerHorizonFromDate"
                  />
                </label>
              </div>

              <div class="chip-wrap reason-wrap">
                <label v-for="card in reasonCardOptions" :key="card" class="chip chip-check">
                  <input type="checkbox" :value="card" v-model="selectedReasonCards" />
                  {{ card }}
                </label>
              </div>

              <textarea v-model.trim="reasonText" rows="3" placeholder="근거를 입력하세요."></textarea>

              <button class="start-cta compact" :disabled="loading" @click="submitCurrent">SUBMIT DECISION</button>
              <div class="mini-status">{{ submitText }}</div>
            </article>
          </section>

          <section v-if="currentProblem" class="glass-panel chart-card" :class="{ reveal: revealMode }">
            <div class="card-head">
              <div>
                <p class="eyebrow">{{ revealMode ? "POST-MARKET ANALYSIS" : "MARKET SURFACE" }}</p>
                <h2>{{ revealMode ? "ANSWER REVEALED" : "Chart Surface" }}</h2>
              </div>
              <div class="card-chips">
                <span class="nav-chip">{{ currentProblem.problemDate }}</span>
                <span class="nav-chip nav-chip-accent">{{ revealMode ? lastResult?.evaluationDate : currentProblem.revealMaxDate }}</span>
              </div>
            </div>

            <div class="chart-box"></div>
          </section>

          <section v-if="revealMode && lastResult" class="glass-panel reveal-panel">
            <div class="reveal-head">
              <div>
                <p class="eyebrow">SESSION FINAL REPORT</p>
                <h2>RESULT SUMMARY</h2>
              </div>
              <div class="reveal-badges">
                <span class="reveal-badge">문제수익률 {{ lastResult.problemReturnRate }}%</span>
                <span class="reveal-badge">세션수익률 {{ lastResult.sessionReturnRate }}%</span>
                <span class="reveal-badge">평가일 {{ lastResult.evaluationDate }}</span>
              </div>
            </div>

            <div class="answer-summary">
              <div class="answer-card">
                <span>종목코드</span>
                <strong>{{ currentProblem.stockCode }}</strong>
              </div>
              <div class="answer-card">
                <span>선택한 액션</span>
                <strong>{{ lastResult.actionType }} / {{ lastResult.actionRatio }}%</strong>
              </div>
              <div class="answer-card">
                <span>문제시점 가격</span>
                <strong>{{ formatNumber(lastResult.priceAtProblem) }}</strong>
              </div>
              <div class="answer-card">
                <span>30일 뒤 가격</span>
                <strong>{{ formatNumber(lastResult.priceAtEvaluation) }}</strong>
              </div>
              <div class="answer-card highlight">
                <span>최종 수익률</span>
                <strong>{{ lastResult.problemReturnRate }}%</strong>
              </div>
            </div>

            <div class="reveal-actions">
              <button class="start-cta compact" :disabled="loading" @click="nextRound">다음 문제</button>
            </div>
          </section>

          <details class="glass-panel debug-panel">
            <summary>Debug & Service Info</summary>
            <div class="debug-grid">
              <section>
                <h3>Backend Health</h3>
                <button class="small-action" @click="loadHealth">Load Health</button>
                <pre class="status-box">{{ healthText }}</pre>
              </section>
              <section>
                <h3>Supported Timeframes</h3>
                <button class="small-action" @click="loadTimeframes">Load Timeframes</button>
                <p>Selected: {{ selectedTimeframe }}</p>
                <select v-model="selectedTimeframe">
                  <option v-for="tf in timeframes" :key="tf" :value="tf">{{ tf }}</option>
                </select>
                <pre class="status-box">{{ timeframeText }}</pre>
              </section>
            </div>
          </details>
        </template>
      </section>

      <aside class="rank-panel glass-panel">
        <div class="rank-panel-head">
          <p class="eyebrow">GLOBAL RANKS</p>
          <h2>Top Players</h2>
          <div class="rank-headers">
            <span>ENTITY</span>
            <span>PROFIT %</span>
          </div>
        </div>

        <div class="rank-list">
          <div v-for="row in leaderboardRows" :key="row.session_id" class="rank-row" :class="{ top: row.rank_no === 1 }">
            <div class="rank-left">
              <span class="rank-no">{{ String(row.rank_no).padStart(2, "0") }}</span>
              <div>
                <div class="rank-name">{{ row.nickname_display }}</div>
                <div class="rank-country">{{ row.country_code || "ZZ" }}</div>
              </div>
            </div>
            <span class="rank-profit">{{ formatSignedPercent(row.total_return_rate) }}%</span>
          </div>
        </div>

        <div class="my-rank" v-if="myRankRow">
          <div>
            <p class="eyebrow">Rank Disclosure</p>
            <h3>#{{ myRankRow.rank_no }}</h3>
            <p>{{ myRankRow.nickname_display }}</p>
          </div>
          <div class="my-rank-profit">{{ formatSignedPercent(myRankRow.total_return_rate) }}%</div>
        </div>
      </aside>
    </div>
  </main>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from "vue";
import * as echarts from "echarts";
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

const indicatorOptions = ["RSI", "MACD", "SLOW_STOCHASTIC"];
const selectedIndicators = ref(["RSI", "MACD", "SLOW_STOCHASTIC"]);

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

const chartRef = ref(null);
let chartInstance = null;

function handleWindowResize() {
  if (chartInstance) {
    chartInstance.resize();
  }
}

// Business logic: Start challenge flow without authentication to maximize training data collection participation.
async function startProblem() {
  loading.value = true;
  startText.value = "세션 생성 중...";
  submitText.value = "제출 대기 중";
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
  revealMode.value = false;
  lastResult.value = null;
  const payload = await fetchNextProblem(currentSession.value.sessionId);
  if (payload.completed) {
    currentProblem.value = null;
    currentAttemptId.value = null;
    currentSequence.value = currentSession.value.maxProblems || 15;
    chartStatus.value = payload.message || "세션 완료";
    submitText.value = "세션이 완료되었습니다.";
    return;
  }

  currentAttemptId.value = payload.attemptId;
  currentSequence.value = payload.sequenceNo;
  currentProblem.value = payload.problem;
  syncDefaultEvaluationDate();
  startText.value = payload.resumed
    ? `진행 중 문제를 재개했습니다. (attemptId=${payload.attemptId})`
    : `새 문제를 받았습니다. (attemptId=${payload.attemptId}, set=${payload.problemSetId})`;
  await reloadChart();
}

async function reloadChart() {
  if (!currentProblem.value) {
    return;
  }
  chartStatus.value = "차트 로딩 중...";
  try {
    const fromDate = shiftDate(currentProblem.value.problemDate, -180);
    const toDate = revealMode.value && lastResult.value
      ? lastResult.value.evaluationDate
      : currentProblem.value.problemDate;
    const payload = await fetchChartData({
      stockCode: currentProblem.value.stockCode,
      fromDate,
      toDate,
      timeframe: currentProblem.value.chartTimeframe || "1d",
      indicators: selectedIndicators.value,
    });

    const highlight = revealMode.value && lastResult.value
      ? {
          problemDate: currentProblem.value.problemDate,
          evaluationDate: lastResult.value.evaluationDate,
          actionType: lastResult.value.actionType,
          priceAtProblem: lastResult.value.priceAtProblem,
          priceAtEvaluation: lastResult.value.priceAtEvaluation,
        }
      : {};

    renderChart(payload, highlight);
    chartStatus.value = `캔들 ${payload.candles.length}개 로드 / 지표: ${payload.selectedIndicators.join(", ") || "없음"}`;
  } catch (error) {
    chartStatus.value = `차트 로딩 실패: ${error.message}`;
  }
}

function renderChart(payload, highlight = {}) {
  if (!chartRef.value) {
    return;
  }

  if (!chartInstance) {
    chartInstance = echarts.init(chartRef.value);
  }

  const candles = payload.candles || [];
  const labels = candles.map((c) => c.date);
  const ohlc = candles.map((c) => [Number(c.open), Number(c.close), Number(c.low), Number(c.high)]);

  const rsiMap = new Map((payload.indicators?.RSI || []).map((item) => [item.date, Number(item.value)]));
  const macdMap = new Map(
    (payload.indicators?.MACD || []).map((item) => [item.date, { macd: Number(item.macd), signal: Number(item.signal), hist: Number(item.hist) }])
  );
  const stochMap = new Map((payload.indicators?.SLOW_STOCHASTIC || []).map((item) => [item.date, { k: Number(item.k), d: Number(item.d) }]));

  const rsiSeries = labels.map((d) => (rsiMap.has(d) ? rsiMap.get(d) : null));
  const macdLine = labels.map((d) => (macdMap.has(d) ? macdMap.get(d).macd : null));
  const macdSignal = labels.map((d) => (macdMap.has(d) ? macdMap.get(d).signal : null));
  const macdHist = labels.map((d) => (macdMap.has(d) ? macdMap.get(d).hist : null));
  const stochK = labels.map((d) => (stochMap.has(d) ? stochMap.get(d).k : null));
  const stochD = labels.map((d) => (stochMap.has(d) ? stochMap.get(d).d : null));
  const markLineData = [];
  const markPointData = [];

  if (highlight.problemDate) {
    markLineData.push({ xAxis: highlight.problemDate, name: "문제시점" });
  }
  if (highlight.evaluationDate) {
    markLineData.push({ xAxis: highlight.evaluationDate, name: "정답시점" });
  }

  if (highlight.problemDate && highlight.evaluationDate) {
    markPointData.push(...buildTradeMarkers(highlight));
  }

  chartInstance.setOption({
    animation: false,
    legend: {
      data: ["Candles", "RSI", "MACD", "Signal", "Hist", "%K", "%D"],
      top: 0,
    },
    tooltip: { trigger: "axis" },
    axisPointer: { link: [{ xAxisIndex: [0, 1, 2] }] },
    grid: [
      { left: 50, right: 20, top: 30, height: "45%" },
      { left: 50, right: 20, top: "58%", height: "16%" },
      { left: 50, right: 20, top: "78%", height: "16%" },
    ],
    xAxis: [
      { type: "category", data: labels, scale: true, boundaryGap: false, axisLine: { onZero: false } },
      { type: "category", data: labels, gridIndex: 1, axisLabel: { show: false } },
      { type: "category", data: labels, gridIndex: 2 },
    ],
    yAxis: [
      { scale: true, splitArea: { show: true } },
      { gridIndex: 1, min: 0, max: 100 },
      { gridIndex: 2, scale: true },
    ],
    dataZoom: [
      { type: "inside", xAxisIndex: [0, 1, 2], start: 15, end: 100 },
      { type: "slider", xAxisIndex: [0, 1, 2], top: "95%", start: 15, end: 100 },
    ],
    series: [
      {
        name: "Candles",
        type: "candlestick",
        data: ohlc,
        markLine: {
          symbol: ["none", "none"],
          label: { formatter: (param) => param.name },
          lineStyle: { color: "#d9572b", type: "dashed" },
          data: markLineData,
        },
        markPoint: {
          symbol: "triangle",
          symbolSize: 18,
          label: {
            formatter: (param) => param.name,
            color: "#2f2b24",
            fontWeight: 700,
          },
          data: markPointData,
        },
      },
      { name: "RSI", type: "line", xAxisIndex: 1, yAxisIndex: 1, data: rsiSeries, smooth: true },
      { name: "MACD", type: "line", xAxisIndex: 2, yAxisIndex: 2, data: macdLine, smooth: true },
      { name: "Signal", type: "line", xAxisIndex: 2, yAxisIndex: 2, data: macdSignal, smooth: true },
      {
        name: "Hist",
        type: "bar",
        xAxisIndex: 2,
        yAxisIndex: 2,
        data: macdHist,
        itemStyle: {
          color: (p) => (p.value >= 0 ? "#23a55a" : "#d95140"),
        },
      },
      { name: "%K", type: "line", xAxisIndex: 1, yAxisIndex: 1, data: stochK, smooth: true },
      { name: "%D", type: "line", xAxisIndex: 1, yAxisIndex: 1, data: stochD, smooth: true },
    ],
  });
}

async function submitCurrent() {
  if (!currentAttemptId.value) {
    submitText.value = "제출할 문제가 없습니다.";
    return;
  }

  if (selectedReasonCards.value.length === 0 && !reasonText.value) {
    submitText.value = "이유카드 또는 텍스트 이유를 하나 이상 입력해야 합니다.";
    return;
  }

  loading.value = true;
  submitText.value = "제출 중...";
  try {
    const result = await submitAttempt(currentAttemptId.value, {
      actionType: actionType.value,
      actionRatio: Number(actionRatio.value),
      answerHorizonDays: Number(answerHorizonDays.value),
      reasonText: reasonText.value,
      reasonCards: selectedReasonCards.value,
      indicatorKeys: selectedIndicators.value,
      timedOutFlag: false,
    });

    lastResult.value = result;
    revealMode.value = true;
    submitText.value = `답지 공개: 문제수익률 ${result.problemReturnRate}% / 세션수익률 ${result.sessionReturnRate}%`;
    await loadLeaderboard();
    await reloadChart();
  } catch (error) {
    submitText.value = `제출 실패: ${error.message}`;
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

function setEvaluationPreset(days) {
  answerHorizonDays.value = days;
  if (currentProblem.value) {
    evaluationDate.value = shiftDate(currentProblem.value.problemDate, days);
  }
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

// Business logic: Fetch backend runtime status so operators can verify container routing and service availability.
async function loadHealth() {
  try {
    const health = await fetchHealth();
    healthText.value = JSON.stringify(health, null, 2);
  } catch (error) {
    healthText.value = `Error: ${error.message}`;
  }
}

// Business logic: Load backend-provided timeframe catalog so users can choose chart granularity from 1m to 1M.
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
    const payload = await fetchLeaderboard(5, currentSession.value?.sessionId ?? null);
    leaderboardRows.value = payload.rows || [];
    myRankRow.value = payload.myRank || null;
  } catch (error) {
    leaderboardRows.value = [];
    myRankRow.value = null;
  }
}

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

function buildTradeMarkers(highlight) {
  const action = String(highlight.actionType || "HOLD").toUpperCase();
  const entryPrice = Number(highlight.priceAtProblem);
  const exitPrice = Number(highlight.priceAtEvaluation);

  if (!Number.isFinite(entryPrice) || !Number.isFinite(exitPrice)) {
    return [];
  }

  if (action === "BUY") {
    return [
      {
        name: "매수",
        coord: [highlight.problemDate, entryPrice],
        itemStyle: { color: "#1f8a4c" },
        symbolRotate: 0,
      },
      {
        name: "매도",
        coord: [highlight.evaluationDate, exitPrice],
        itemStyle: { color: "#c53d2f" },
        symbolRotate: 180,
      },
    ];
  }

  if (action === "SELL") {
    return [
      {
        name: "매도",
        coord: [highlight.problemDate, entryPrice],
        itemStyle: { color: "#c53d2f" },
        symbolRotate: 180,
      },
      {
        name: "매수",
        coord: [highlight.evaluationDate, exitPrice],
        itemStyle: { color: "#1f8a4c" },
        symbolRotate: 0,
      },
    ];
  }

  return [
    {
      name: "관망 시작",
      coord: [highlight.problemDate, entryPrice],
      itemStyle: { color: "#5b5f66" },
      symbolRotate: 0,
    },
    {
      name: "관망 종료",
      coord: [highlight.evaluationDate, exitPrice],
      itemStyle: { color: "#5b5f66" },
      symbolRotate: 180,
    },
  ];
}

onBeforeUnmount(() => {
  window.removeEventListener("resize", handleWindowResize);
  if (chartInstance) {
    chartInstance.dispose();
    chartInstance = null;
  }
});

onMounted(() => {
  window.addEventListener("resize", handleWindowResize);
  loadLeaderboard();
});
</script>

<style scoped>
.sovereign-shell {
  min-height: 100vh;
  padding: 1rem;
  color: #dae2fd;
  font-family: "Inter", "Pretendard", "Segoe UI", sans-serif;
  background:
    radial-gradient(circle at 15% 20%, rgba(78, 222, 163, 0.12) 0, transparent 26%),
    radial-gradient(circle at 88% 18%, rgba(78, 222, 163, 0.08) 0, transparent 22%),
    linear-gradient(180deg, #0b1326 0%, #070d1c 100%);
}

.glass-panel {
  background: rgba(45, 52, 73, 0.6);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(134, 148, 138, 0.14);
  border-radius: 18px;
  box-shadow: 0 24px 48px rgba(0, 0, 0, 0.28);
}

.top-nav {
  position: sticky;
  top: 0;
  z-index: 20;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 1rem 1.25rem;
  margin-bottom: 1rem;
}

.brand-block {
  display: grid;
  gap: 0.15rem;
}

.brand-title,
.hero-copy h1,
.rank-panel h2,
.reveal-panel h2,
.info-card h2,
.chart-card h2 {
  font-family: "Space Grotesk", "Inter", sans-serif;
}

.brand-title {
  color: #4edea3;
  font-size: 1.5rem;
  font-weight: 800;
  letter-spacing: -0.03em;
}

.brand-subtitle {
  color: #8da3b6;
  font-size: 0.75rem;
  letter-spacing: 0.2em;
  text-transform: uppercase;
}

.top-nav-links {
  display: flex;
  gap: 1.25rem;
  flex-wrap: wrap;
  justify-content: center;
  font-size: 0.95rem;
}

.top-nav-links a {
  color: #8da3b6;
  text-decoration: none;
  padding-bottom: 0.15rem;
}

.top-nav-links .is-active {
  color: #4edea3;
  border-bottom: 2px solid #4edea3;
}

.top-nav-actions {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.nav-chip,
.reveal-badge {
  display: inline-flex;
  align-items: center;
  padding: 0.35rem 0.7rem;
  border-radius: 999px;
  background: rgba(17, 25, 44, 0.8);
  border: 1px solid rgba(78, 222, 163, 0.16);
  color: #dae2fd;
  font-size: 0.8rem;
}

.nav-chip-accent {
  color: #4edea3;
}

.page-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 1rem;
  max-width: 1600px;
  margin: 0 auto;
}

.main-stage {
  display: grid;
  gap: 1rem;
  min-width: 0;
}

.hero-panel,
.join-grid,
.stage-grid,
.chart-card,
.reveal-panel,
.debug-panel,
.rank-panel {
  padding: 1rem;
}

.hero-panel {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: end;
}

.hero-copy h1 {
  margin: 0.2rem 0 0;
  font-size: clamp(2rem, 4vw, 3.6rem);
  line-height: 1;
  letter-spacing: -0.05em;
}

.hero-copy p {
  max-width: 56ch;
  margin: 0.5rem 0 0;
  color: #bbcabf;
}

.eyebrow {
  margin: 0;
  color: #4edea3;
  font-size: 0.72rem;
  font-weight: 800;
  letter-spacing: 0.24em;
  text-transform: uppercase;
}

.hero-stats {
  display: flex;
  gap: 0.7rem;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.hero-stat {
  min-width: 108px;
  padding: 0.75rem 0.9rem;
  border-radius: 14px;
  background: rgba(17, 25, 44, 0.72);
  border: 1px solid rgba(78, 222, 163, 0.12);
}

.hero-stat span,
.rank-headers,
.rank-country,
.mini-status,
.field-group small {
  color: #8da3b6;
  font-size: 0.72rem;
}

.hero-stat strong {
  display: block;
  margin-top: 0.2rem;
  font-size: 1rem;
}

.join-grid,
.stage-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1.1fr);
  gap: 1rem;
}

.join-card,
.info-card,
.action-card,
.chart-card,
.reveal-panel {
  padding: 1.1rem;
  background: rgba(35, 42, 61, 0.9);
  border-radius: 18px;
}

.join-card.muted {
  background: rgba(22, 28, 42, 0.9);
}

.domain-row,
.chip-wrap,
.card-chips,
.reveal-badges {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.domain-chip,
.chip,
.small-action,
.start-cta {
  border: none;
  border-radius: 14px;
  transition: transform 0.15s ease, background 0.15s ease, box-shadow 0.15s ease;
}

.domain-chip {
  min-width: 56px;
  padding: 0.75rem 0.95rem;
  background: #222a3d;
  color: #dae2fd;
  font-weight: 800;
}

.domain-chip.active {
  background: #4edea3;
  color: #003824;
  box-shadow: 0 0 18px rgba(78, 222, 163, 0.25);
}

.field-group {
  display: grid;
  gap: 0.4rem;
  margin-top: 1rem;
}

.field-group span,
.rank-panel-head span,
.answer-card span {
  color: #8da3b6;
  font-size: 0.72rem;
  text-transform: uppercase;
  letter-spacing: 0.18em;
}

.field-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 0.6rem;
}

input,
select,
textarea {
  width: 100%;
  padding: 0.8rem 0.9rem;
  border: 1px solid rgba(134, 148, 138, 0.18);
  border-radius: 14px;
  background: #0a1020;
  color: #dae2fd;
}

textarea {
  min-height: 92px;
  resize: vertical;
}

.small-action,
.start-cta {
  background: linear-gradient(135deg, #4edea3, #10b981);
  color: #001d13;
  font-weight: 900;
  cursor: pointer;
}

.small-action {
  padding: 0.75rem 1rem;
  background: #2d3449;
  color: #4edea3;
}

.start-cta {
  margin-top: 1rem;
  width: 100%;
  padding: 1rem 1.1rem;
  font-size: 1rem;
  text-transform: uppercase;
}

.start-cta.compact {
  margin-top: 0.75rem;
}

.empty-state,
.mini-status {
  margin-top: 0.8rem;
  padding: 0.8rem 0;
}

.leader-compact,
.rank-list {
  display: grid;
  gap: 0.5rem;
}

.leader-row,
.rank-row {
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 0.75rem;
  align-items: center;
  padding: 0.8rem 0.85rem;
  border-radius: 14px;
  background: rgba(17, 25, 44, 0.55);
}

.leader-row.top,
.rank-row.top {
  background: rgba(78, 222, 163, 0.1);
  box-shadow: inset 3px 0 0 #4edea3;
}

.leader-rank,
.rank-no {
  color: #4edea3;
  font-size: 1.05rem;
  font-weight: 900;
  min-width: 1.9rem;
}

.leader-name,
.rank-name,
.my-rank h3 {
  color: #dae2fd;
  font-weight: 700;
}

.leader-profit,
.rank-profit,
.my-rank-profit {
  color: #4edea3;
  font-weight: 900;
}

.rank-panel {
  display: grid;
  grid-template-rows: auto 1fr auto;
  min-height: 760px;
}

.rank-panel-head {
  display: grid;
  gap: 0.4rem;
  padding-bottom: 0.75rem;
}

.rank-panel h2,
.info-card h2,
.chart-card h2,
.reveal-panel h2 {
  margin: 0;
  font-size: 1.6rem;
  letter-spacing: -0.04em;
}

.rank-headers {
  display: flex;
  justify-content: space-between;
}

.rank-country {
  letter-spacing: 0.08em;
}

.my-rank {
  margin-top: 1rem;
  padding: 1rem;
  border-radius: 16px;
  background: rgba(17, 25, 44, 0.92);
  border-top: 1px solid rgba(134, 148, 138, 0.16);
  display: flex;
  justify-content: space-between;
  align-items: end;
  gap: 1rem;
}

.my-rank p {
  margin: 0.25rem 0 0;
  color: #8da3b6;
}

.stage-grid {
  align-items: start;
}

.meta-list-alt {
  list-style: none;
  margin: 0.8rem 0 0;
  padding: 0;
  display: grid;
  gap: 0.55rem;
}

.meta-list-alt li {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  padding-bottom: 0.35rem;
  border-bottom: 1px solid rgba(134, 148, 138, 0.12);
}

.meta-list-alt span {
  color: #8da3b6;
}

.action-row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.65rem;
}

.action-btn {
  border: 1px solid rgba(134, 148, 138, 0.18);
  background: #171f33;
  color: #dae2fd;
  padding: 0.8rem 0.9rem;
  border-radius: 14px;
  font-weight: 900;
  cursor: pointer;
}

.action-btn.active,
.chip.active {
  box-shadow: 0 0 0 1px rgba(78, 222, 163, 0.28);
}

.action-btn.buy.active {
  background: #1f8a4c;
  color: #fff;
}

.action-btn.hold.active {
  background: #5b5f66;
  color: #fff;
}

.action-btn.sell.active {
  background: #c53d2f;
  color: #fff;
}

.compact {
  margin-top: 0.85rem;
}

.reason-wrap {
  margin-top: 0.75rem;
}

.chip {
  padding: 0.45rem 0.75rem;
  background: #171f33;
  color: #dae2fd;
  border: 1px solid rgba(134, 148, 138, 0.16);
  cursor: pointer;
}

.chip-check input {
  width: auto;
  margin-right: 0.35rem;
}

.ratio-chip {
  min-width: 64px;
}

.ratio-chip.active,
.domain-chip.active {
  transform: translateY(-1px);
}

.chart-card {
  display: grid;
  gap: 0.85rem;
}

.card-head,
.reveal-head {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 0.8rem;
  flex-wrap: wrap;
}

.chart-box {
  width: 100%;
  height: 620px;
  border-radius: 18px;
  background: #0a1020;
  border: 1px solid rgba(134, 148, 138, 0.12);
}

.reveal-panel {
  display: grid;
  gap: 0.9rem;
}

.answer-summary {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 0.7rem;
}

.answer-card {
  padding: 0.85rem;
  border-radius: 14px;
  background: rgba(17, 25, 44, 0.7);
  display: grid;
  gap: 0.4rem;
}

.answer-card.highlight {
  background: rgba(78, 222, 163, 0.14);
}

.reveal-actions {
  display: flex;
  justify-content: flex-end;
}

.debug-panel {
  padding: 1rem;
}

.debug-panel summary {
  cursor: pointer;
  font-weight: 800;
  color: #dae2fd;
}

.debug-grid {
  margin-top: 1rem;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.9rem;
}

.status-box {
  margin-top: 0.7rem;
  padding: 0.8rem;
  border-radius: 14px;
  background: rgba(17, 25, 44, 0.6);
  color: #bbcabf;
  white-space: pre-wrap;
}

@media (max-width: 1200px) {
  .page-grid {
    grid-template-columns: 1fr;
  }

  .rank-panel {
    min-height: auto;
  }
}

@media (max-width: 900px) {
  .top-nav {
    flex-direction: column;
    align-items: flex-start;
  }

  .hero-panel,
  .join-grid,
  .stage-grid,
  .debug-grid,
  .answer-summary {
    grid-template-columns: 1fr;
  }

  .hero-panel {
    align-items: start;
  }

  .chart-box {
    height: 460px;
  }
}

@media (max-width: 640px) {
  .sovereign-shell {
    padding: 0.7rem;
  }

  .top-nav,
  .hero-panel,
  .join-card,
  .info-card,
  .action-card,
  .chart-card,
  .reveal-panel,
  .rank-panel {
    border-radius: 16px;
  }

  .action-row {
    grid-template-columns: 1fr;
  }

  .chart-box {
    height: 340px;
  }

  .top-nav-links {
    gap: 0.8rem;
  }

  .answer-summary {
    grid-template-columns: 1fr;
  }
}
</style>

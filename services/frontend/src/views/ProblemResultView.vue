<template>
  <section class="view-card" v-if="game.currentProblem.value && game.lastResult.value">
    <div class="top-line">
      <div>
        <p class="eyebrow">RESULT SCREEN</p>
        <h1>{{ stockDisplayName }}</h1>
        <p class="stock-name">Answer Revealed</p>
      </div>
      <div class="chips">
        <span class="chip-lite">종목 {{ stockDisplayName }}</span>
        <span class="chip-lite">평가일 {{ game.lastResult.value.evaluationDate }}</span>
      </div>
    </div>

    <section class="summary-grid">
      <article class="panel" v-for="item in summaryItems" :key="item.label" :class="{ highlight: item.highlight }">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
      </article>
    </section>

    <section class="chart-panel panel">
      <div ref="chartRef" class="chart-box"></div>
      <p class="status">{{ game.chartStatus.value }}</p>
    </section>

    <div class="actions">
      <button class="cta" :disabled="game.loading.value" @click="next">다음 문제</button>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted } from "vue";
import { useRouter } from "vue-router";
import { useGameFlow } from "../composables/useGameFlow";
import { useEChartRenderer } from "../composables/useEChartRenderer";

const router = useRouter();
const game = useGameFlow();
const { chartRef, renderChart } = useEChartRenderer();

const stockDisplayName = computed(() => {
  const problem = game.currentProblem.value;
  if (!problem) {
    return "-";
  }

  const code = problem.stockCode || "-";
  const name = problem.stockName;
  if (!name || name === code) {
    return code;
  }
  return `${name} (${code})`;
});

const summaryItems = computed(() => {
  const result = game.lastResult.value;
  if (!result) {
    return [];
  }

  return [
    { label: "선택한 액션", value: `${result.actionType} / ${result.actionRatio}%` },
    { label: "문제시점 가격", value: game.formatNumber(result.priceAtProblem) },
    { label: "30일 뒤 가격", value: game.formatNumber(result.priceAtEvaluation) },
    { label: "문제 수익률", value: `${result.problemReturnRate}%`, highlight: true },
    { label: "세션 수익률", value: `${result.sessionReturnRate}%` },
  ];
});

async function ensureResult() {
  if (!game.currentSession.value) {
    await router.replace("/");
    return false;
  }
  if (game.sessionCompleted.value) {
    await router.replace("/summary");
    return false;
  }
  if (!game.revealMode.value || !game.lastResult.value) {
    await router.replace("/problem");
    return false;
  }
  return true;
}

async function loadRevealChart() {
  const ok = await ensureResult();
  if (!ok) {
    return;
  }
  const bundle = await game.loadChartBundle(true);
  if (bundle) {
    renderChart(bundle.payload, bundle.highlight);
  }
}

async function next() {
  await game.nextRound();
  if (game.sessionCompleted.value) {
    await router.push("/summary");
    return;
  }
  await router.push("/problem");
}

onMounted(async () => {
  await game.loadLeaderboard();
  await loadRevealChart();
});
</script>

<style scoped>
.top-line { display: flex; justify-content: space-between; gap: 1rem; align-items: end; }
h1 { margin: 0; }
.stock-name { margin: .4rem 0 0; color: #4edea3; font-weight: 800; letter-spacing: .01em; }
.chips { display: flex; gap: .5rem; flex-wrap: wrap; }
.chip-lite { padding: .35rem .65rem; border-radius: 999px; background: rgba(17,25,44,.8); border: 1px solid rgba(78,222,163,.16); }
.summary-grid { display: grid; grid-template-columns: repeat(5, minmax(0,1fr)); gap: .8rem; }
.summary-grid .panel { padding: .9rem; display: grid; gap: .35rem; }
.panel span { color: #8da3b6; font-size: .78rem; }
.panel.highlight { background: rgba(78,222,163,.14); }
.chart-panel { padding: .9rem; }
.chart-box { width: 100%; height: 560px; border-radius: 12px; background: #0a1020; }
.actions { display: flex; justify-content: flex-end; }
@media (max-width: 1000px) {
  .summary-grid { grid-template-columns: repeat(2, minmax(0,1fr)); }
  .chart-box { height: 420px; }
}
@media (max-width: 640px) {
  .summary-grid { grid-template-columns: 1fr; }
}
</style>

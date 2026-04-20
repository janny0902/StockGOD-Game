<template>
  <section class="view-card" v-if="game.currentProblem.value">
    <div class="top-line">
      <div>
        <p class="eyebrow">PROBLEM SCREEN</p>
        <h1>Round {{ game.currentSequence.value }}/15</h1>
      </div>
      <div class="chips">
        <span class="chip-lite">기준일 {{ game.currentProblem.value.problemDate }}</span>
        <span class="chip-lite">종목코드 ???</span>
      </div>
    </div>

    <section class="chart-panel panel">
      <div ref="chartRef" class="chart-box"></div>
      <p class="status">{{ game.chartStatus.value }}</p>
    </section>

    <section class="form-grid">
      <article class="panel">
        <p class="eyebrow">ACTION</p>
        <div class="action-row">
          <button class="btn buy" :class="{ active: game.actionType.value === 'LONG' }" @click="game.actionType.value = 'LONG'">LONG</button>
          <button class="btn hold" :class="{ active: game.actionType.value === 'HOLD' }" @click="game.actionType.value = 'HOLD'">HOLD</button>
          <button class="btn sell" :class="{ active: game.actionType.value === 'SHORT' }" @click="game.actionType.value = 'SHORT'">SHORT</button>
        </div>

        <label class="field">비중</label>
        <div class="row wrap">
          <button
            v-for="ratio in game.ratioOptions"
            :key="ratio"
            class="chip"
            :class="{ active: game.actionRatio.value === ratio }"
            @click="game.actionRatio.value = ratio"
          >
            {{ ratio }}%
          </button>
        </div>

        <label class="field">평가기간</label>
        <div class="row wrap">
          <button
            v-for="preset in game.evaluationPresets"
            :key="preset.days"
            class="chip"
            :class="{ active: game.answerHorizonDays.value === preset.days }"
            @click="game.setEvaluationPreset(preset.days)"
          >
            {{ preset.label }}
          </button>
        </div>
        <input
          v-model="game.evaluationDate.value"
          type="date"
          :min="game.currentProblem.value.problemDate"
          :max="game.maxEvaluationDateForProblem()"
          @change="game.syncAnswerHorizonFromDate"
        />
      </article>

      <article class="panel">
        <p class="eyebrow">REASON</p>
        <div class="row wrap">
          <label v-for="card in game.reasonCardOptions" :key="card" class="chip chk">
            <input type="checkbox" :value="card" v-model="game.selectedReasonCards.value" />
            {{ card }}
          </label>
        </div>

        <label class="field">지표 사용</label>
        <div class="row wrap">
          <label
            v-for="key in game.indicatorOptions"
            :key="key"
            class="chip chk indicator-chip"
            :class="{ selected: game.selectedIndicators.value.includes(key), unselected: !game.selectedIndicators.value.includes(key) }"
          >
            <input type="checkbox" :value="key" v-model="game.selectedIndicators.value" @change="reloadChart" />
            {{ key }}
          </label>
        </div>

        <button class="cta" :disabled="game.loading.value" @click="submit">SUBMIT DECISION</button>
        <p class="status">{{ game.submitText.value }}</p>
      </article>
    </section>
  </section>
</template>

<script setup>
import { onMounted, watch } from "vue";
import { useRouter } from "vue-router";
import { useGameFlow } from "../composables/useGameFlow";
import { useEChartRenderer } from "../composables/useEChartRenderer";

const router = useRouter();
const game = useGameFlow();
const { chartRef, renderChart } = useEChartRenderer();

async function ensureProblem() {
  if (!game.currentSession.value) {
    await router.replace("/");
    return false;
  }
  if (game.sessionCompleted.value) {
    await router.replace("/summary");
    return false;
  }
  if (game.revealMode.value) {
    await router.replace("/result");
    return false;
  }
  if (!game.currentProblem.value) {
    await game.loadNextProblem();
  }
  return !!game.currentProblem.value;
}

async function reloadChart() {
  const ok = await ensureProblem();
  if (!ok) {
    return;
  }
  const bundle = await game.loadChartBundle(false);
  if (bundle) {
    renderChart(bundle.payload, {});
  }
}

async function submit() {
  const submitted = await game.submitCurrent();
  if (submitted && game.revealMode.value) {
    await router.push("/result");
  }
}

onMounted(async () => {
  await game.loadLeaderboard();
  await reloadChart();
});

watch(
  () => game.selectedIndicators.value.join(","),
  async () => {
    await reloadChart();
  }
);
</script>

<style scoped>
.top-line { display: flex; justify-content: space-between; gap: 1rem; align-items: end; }
h1 { margin: 0; }
.chips { display: flex; gap: .5rem; flex-wrap: wrap; }
.chip-lite { padding: .35rem .65rem; border-radius: 999px; background: rgba(17,25,44,.8); border: 1px solid rgba(78,222,163,.16); }
.chart-panel { padding: .9rem; }
.chart-box { width: 100%; height: 560px; border-radius: 12px; background: #0a1020; }
.form-grid { display: grid; grid-template-columns: 1.1fr 1fr; gap: 1rem; }
.action-row { display: grid; grid-template-columns: repeat(3, minmax(0,1fr)); gap: .6rem; margin-bottom: .8rem; }
.btn { padding: .8rem; border-radius: 12px; border: 1px solid rgba(134,148,138,.2); background: #171f33; color: #dae2fd; cursor: pointer; font-weight: 800; }
.btn.buy.active { background: #1f8a4c; color: #fff; }
.btn.hold.active { background: #5b5f66; color: #fff; }
.btn.sell.active { background: #c53d2f; color: #fff; }
.field { display: block; margin: .4rem 0; color: #8da3b6; }
.chk input { width: auto; margin-right: .35rem; }
.indicator-chip.unselected { color: #ffffff; }
.indicator-chip.selected {
  color: #f3d33f;
  border-color: rgba(243, 211, 63, .55);
  background: rgba(243, 211, 63, .12);
}
textarea { min-height: 92px; }
.cta { margin-top: .8rem; width: 100%; }
@media (max-width: 1000px) {
  .form-grid { grid-template-columns: 1fr; }
  .chart-box { height: 420px; }
}
</style>

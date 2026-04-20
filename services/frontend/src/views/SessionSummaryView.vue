<template>
  <section class="view-card" v-if="game.currentSession.value">
    <div>
      <p class="eyebrow">SETTLEMENT SCREEN</p>
      <h1>Session Final Report</h1>
      <p class="muted">{{ game.completedMessage.value || "15문제 세션이 종료되었습니다." }}</p>
    </div>

    <div class="summary-grid">
      <article class="panel">
        <span>Session</span>
        <strong>#{{ game.currentSession.value.sessionId }}</strong>
      </article>
      <article class="panel">
        <span>Solved</span>
        <strong>{{ game.currentSequence.value }}/15</strong>
      </article>
      <article class="panel highlight">
        <span>Latest Session Return</span>
        <strong>{{ game.lastResult.value ? `${game.lastResult.value.sessionReturnRate}%` : "-" }}</strong>
      </article>
      <article class="panel" v-if="game.myRankRow.value">
        <span>My Rank</span>
        <strong>#{{ game.myRankRow.value.rank_no }}</strong>
      </article>
    </div>

    <article class="leaderboard panel">
      <h2>Top Leaderboard</h2>
      <div class="rank-row" v-for="row in game.leaderboardRows.value.slice(0, 10)" :key="row.session_id">
        <strong>#{{ row.rank_no }}</strong>
        <span>{{ row.nickname_display }}</span>
        <span class="profit">{{ game.formatSignedPercent(row.total_return_rate) }}%</span>
      </div>
    </article>

    <div class="actions">
      <button class="cta" @click="goHome">메인으로</button>
    </div>
  </section>
</template>

<script setup>
import { onMounted } from "vue";
import { useRouter } from "vue-router";
import { useGameFlow } from "../composables/useGameFlow";

const router = useRouter();
const game = useGameFlow();

async function goHome() {
  await router.push("/");
}

onMounted(async () => {
  if (!game.currentSession.value) {
    await router.replace("/");
    return;
  }
  await game.loadLeaderboard();
});
</script>

<style scoped>
h1 { margin: 0; }
.muted { color: #8da3b6; }
.summary-grid { display: grid; grid-template-columns: repeat(4, minmax(0,1fr)); gap: .8rem; }
.panel span { color: #8da3b6; font-size: .78rem; }
.panel strong { display: block; margin-top: .35rem; font-size: 1.2rem; }
.panel.highlight { background: rgba(78,222,163,.14); }
.leaderboard h2 { margin-top: 0; }
.rank-row { display: grid; grid-template-columns: auto 1fr auto; gap: .65rem; padding: .5rem 0; border-bottom: 1px solid rgba(134,148,138,.12); }
.rank-row:last-child { border-bottom: none; }
.profit { color: #4edea3; font-weight: 800; }
.actions { display: flex; justify-content: flex-end; }
@media (max-width: 900px) { .summary-grid { grid-template-columns: repeat(2, minmax(0,1fr)); } }
@media (max-width: 640px) { .summary-grid { grid-template-columns: 1fr; } }
</style>

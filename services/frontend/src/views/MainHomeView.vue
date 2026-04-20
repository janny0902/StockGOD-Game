<template>
  <section class="view-card">
    <div class="hero">
      <p class="eyebrow">MISSION BRIEFING</p>
      <h1>Ascend to God Tier</h1>
      <p>닉네임을 입력하고 15문제 투자 판단 세션을 시작하세요.</p>
    </div>

    <div class="home-grid">
      <article class="panel">
        <p class="eyebrow">SELECT DOMAIN</p>
        <div class="row wrap">
          <button
            v-for="option in game.languageOptions"
            :key="option.code"
            class="chip"
            :class="{ active: game.selectedLanguageCode.value === option.code }"
            @click="game.selectedLanguageCode.value = option.code"
          >
            {{ option.label }}
          </button>
        </div>

        <label class="field">
          <span>Trading Alias</span>
          <input v-model.trim="game.nickname.value" maxlength="40" placeholder="Enter Nickname" />
        </label>

        <div class="row">
          <button class="cta" :disabled="game.loading.value" @click="start">START CHALLENGE</button>
        </div>
        <p class="status">{{ game.startText.value }}</p>
      </article>

      <article class="panel">
        <p class="eyebrow">GLOBAL RANKS</p>
        <div class="rank-list" v-if="game.leaderboardRows.length">
          <div class="rank-row" v-for="row in game.leaderboardRows.slice(0, 10)" :key="row.session_id">
            <strong>#{{ row.rank_no }}</strong>
            <span>{{ row.nickname_display }}</span>
            <span class="profit">{{ game.formatSignedPercent(row.total_return_rate) }}%</span>
          </div>
        </div>
        <p class="status" v-else>랭킹을 불러오는 중입니다.</p>
      </article>
    </div>
  </section>
</template>

<script setup>
import { useRouter } from "vue-router";
import { useGameFlow } from "../composables/useGameFlow";

const router = useRouter();
const game = useGameFlow();

async function start() {
  await game.startSession();
  if (game.currentSession.value && game.currentProblem.value) {
    await router.push("/problem");
  }
}
</script>

<style scoped>
.hero h1 { margin: 0.3rem 0; font-size: clamp(2rem, 4vw, 3.4rem); }
.hero p { margin: 0; color: #bbcabf; }
.home-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; }
.field { display: grid; gap: .35rem; margin-top: .8rem; }
.field span { color: #8da3b6; font-size: .8rem; }
.cta { margin-top: .9rem; width: 100%; }
.rank-list { display: grid; gap: .45rem; }
.rank-row { display: grid; grid-template-columns: auto 1fr auto; gap: .6rem; align-items: center; padding: .6rem .7rem; border-radius: 10px; background: rgba(17,25,44,.65); }
.rank-row strong { color: #4edea3; }
.profit { color: #4edea3; font-weight: 800; }
@media (max-width: 900px) { .home-grid { grid-template-columns: 1fr; } }
</style>

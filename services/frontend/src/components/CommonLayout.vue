<template>
  <main class="shell">
    <header class="top-nav">
      <div>
        <p class="brand">The Sovereign Exchange</p>
        <p class="sub">AI Training Data Collector</p>
      </div>
      <nav class="top-links">
        <RouterLink to="/" class="link" active-class="active" exact-active-class="active">메인</RouterLink>
        <RouterLink to="/problem" class="link" active-class="active">문제</RouterLink>
        <RouterLink to="/result" class="link" active-class="active">결과</RouterLink>
        <RouterLink to="/summary" class="link" active-class="active">결산</RouterLink>
      </nav>
    </header>

    <slot />
  </main>
</template>

<script setup>
import { RouterLink } from "vue-router";
</script>

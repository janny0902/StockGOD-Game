import { createRouter, createWebHashHistory } from "vue-router";
import MainHomeView from "../views/MainHomeView.vue";
import ProblemPlayView from "../views/ProblemPlayView.vue";
import ProblemResultView from "../views/ProblemResultView.vue";
import SessionSummaryView from "../views/SessionSummaryView.vue";

const routes = [
  { path: "/", name: "home", component: MainHomeView },
  { path: "/problem", name: "problem", component: ProblemPlayView },
  { path: "/result", name: "result", component: ProblemResultView },
  { path: "/summary", name: "summary", component: SessionSummaryView },
  { path: "/:pathMatch(.*)*", redirect: "/" },
];

export const router = createRouter({
  history: createWebHashHistory(),
  routes,
});

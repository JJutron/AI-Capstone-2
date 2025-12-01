import { createRouter, createWebHistory } from "vue-router";

import Frame from "@/views/Landing.vue";
import Login from "@/views/Login.vue";
import Join from "@/views/Join.vue";

import OnboardingGender from "@/views/OnboardingGender.vue";
import OnboardingAge from "@/views/OnboardingAge.vue";

import Camera from "@/views/Camera.vue";
import Survey from "@/views/Survey.vue";
import Loading from "@/views/Loading.vue";
import Result from "@/views/Result.vue";
import Home from "@/views/Home.vue";

import Recommendation from "@/views/Recommendation.vue";
import PriceFilter from "@/views/Filter.vue";
import ProductDetail from "@/views/ProductDetail.vue";

const routes = [
  { path: "/", component: Frame },
  { path: "/login", component: Login },
  { path: "/join", component: Join },

  { path: "/onboardinggender", component: OnboardingGender },
  { path: "/onboardingage", component: OnboardingAge },

  { path: "/camera", component: Camera },
  { path: "/survey", component: Survey },
  { path: "/loading", component: Loading },
  { path: "/home", component: Home },
  { path: "/result", component: Result },

  { path: "/recommendation", component: Recommendation },
  { path: "/filter", component: PriceFilter },
  { path: "/product/:id", component: ProductDetail },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

export default router;


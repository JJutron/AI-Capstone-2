import { createRouter, createWebHistory, RouteRecordRaw } from "vue-router";

// Lazy import
const Landing = () => import("@/views/Landing.vue");
const Login = () => import("@/views/Login.vue");
const Join = () => import("@/views/Join.vue");
const OnboardingGender = () => import("@/views/OnboardingGender.vue");
const OnboardingAge = () => import("@/views/OnboardingAge.vue");
const Camera = () => import("@/views/Camera.vue");
const Survey = () => import("@/views/Survey.vue");
const Loading = () => import("@/views/Loading.vue");
const Result = () => import("@/views/Result.vue");
const Recommendation = () => import("@/views/Recommendation.vue");
const Filter = () => import("@/views/Filter.vue");
const ProductDetail = () => import("@/views/ProductDetail.vue");
const Home = () => import("@/views/Home.vue");

const routes: RouteRecordRaw[] = [
  { path: "/", name: "Landing", component: Landing },
  { path: "/login", name: "Login", component: Login },
  { path: "/join", name: "Join", component: Join },
  { path: "/onboardinggender", name: "OnboardingGender", component: OnboardingGender },
  { path: "/onboardingage", name: "OnboardingAge", component: OnboardingAge },

  { path: "/camera", name: "Camera", component: Camera },
  { path: "/survey", name: "Survey", component: Survey },
  { path: "/loading", name: "Loading", component: Loading },
  { path: "/result", name: "Result", component: Result },
  { path: "/recommendation", name: "Recommendation", component: Recommendation },
  { path: "/filter", name: "Filter", component: Filter },
  { path: "/product/:id", name: "ProductDetail", component: ProductDetail, props: true },

  { path: "/home", name: "Home", component: Home },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

export default router;

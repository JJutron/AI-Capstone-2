import { createApp } from "vue";
import App from "./App.vue";
import router from "./router";
import { createPinia } from "pinia";
import axios from "axios";
import "./style.css";

// ✅ 백엔드 주소 (로컬 개발 시에는 proxy 사용)
axios.defaults.baseURL = import.meta.env.VITE_API_BASE_URL || "/api";

// ✅ 토큰 자동 실어주는 인터셉터
axios.interceptors.request.use((config) => {
  const token = localStorage.getItem("accessToken");
  if (token) {
    config.headers = config.headers || {};
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

const app = createApp(App);
const pinia = createPinia();

app.use(pinia);
app.use(router);
app.mount("#app");
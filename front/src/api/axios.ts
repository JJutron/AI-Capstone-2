import axios from "axios";

const API_BASE = import.meta.env.VITE_API_BASE_URL || "http://52.78.47.96:8080";

const api = axios.create({
  baseURL: API_BASE,   // 👉 반드시 필요!
  withCredentials: false,
  timeout: 10000,
  headers: {
    "Content-Type": "application/json",
    Accept: "application/json",
  },
});

// 요청 인터셉터
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem("accessToken");
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// 응답 인터셉터
api.interceptors.response.use(
  (response) => response,
  (error) => {
    console.error("API Error:", error);
    if (error.response?.status === 401) {
      alert("로그인이 필요합니다.");
      localStorage.removeItem("accessToken");
      window.location.href = "/login";
    }
    return Promise.reject(error);
  }
);

// 🔥 회원가입
export const signupAPI = (payload: {
  email: string;
  password: string;
  nickname: string;
  birthDate: string;
  gender: string;
}) => api.post("/api/auth/signup", payload);

// 🔥 로그인
export const loginAPI = (payload: { email: string; password: string }) =>
  api.post("/api/auth/login", payload);

// 🔥 피부 분석 제출
export const submitAnalysisAPI = (image: File, surveyAnswers: string[]) => {
  const form = new FormData();

  form.append("file", image);

  // 🔥 서버 요구대로 survey를 JSON 문자열로 전달
  const surveyData = surveyAnswers.reduce((acc, ans, index) => {
    acc[`q${index + 1}`] = ans;
    return acc;
  }, {} as Record<string, string>);

  form.append("survey", JSON.stringify(surveyData)); // <-- 핵심

  return api.post("/api/analysis/image", form, {
    headers: {
      "Content-Type": "multipart/form-data",
    },
  });
};



// 🔥 분석 결과 조회
export const getAnalysisResultAPI = (id: number) =>
  api.get(`/api/analysis/${id}`);

export default api;

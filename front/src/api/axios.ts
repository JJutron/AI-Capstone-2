import axios from "axios";

// ✅ 프로덕션: API 서브도메인 사용, 개발: 프록시 사용
const api = axios.create({
  baseURL: import.meta.env.PROD 
    ? "https://api.vegin.academy"  // ✅ 프로덕션: API 서브도메인 (CloudFront 우회)
    : "/api",  // 개발: 프록시 사용
  withCredentials: false,
  timeout: 60000,
  headers: {
    Accept: "application/json",
  },
});

// 요청 인터셉터
api.interceptors.request.use(
  (config) => {
    // ✅ 토큰 가져오기 (undefined 문자열 체크)
    const token = localStorage.getItem("accessToken");
    if (token && token !== "undefined" && token !== "null" && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    } else if (config.headers) {
      // ✅ 토큰이 없으면 헤더 제거
      delete config.headers.Authorization;
    }
    
    // ✅ FormData인 경우 Content-Type 헤더 제거 (브라우저가 boundary 자동 설정)
    if (config.data instanceof FormData) {
      delete config.headers["Content-Type"];
      delete config.headers["content-type"];
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

  // ✅ Blob으로 변환하고 Content-Type 명시 (Spring이 SurveyDto로 파싱 가능하도록)
  const surveyBlob = new Blob([JSON.stringify(surveyData)], { 
    type: 'application/json' 
  });
  form.append("survey", surveyBlob, "survey.json");

  return api.post("/api/analysis/image", form);  // ✅ /api 경로 명시, Content-Type 헤더 제거 (FormData 자동 설정)
};

// 🔥 분석 결과 조회
export const getAnalysisResultAPI = (id: number) =>
  api.get(`/api/analysis/${id}`);  // ✅ /api 경로 명시

export default api;

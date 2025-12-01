<template>
  <div class="login">
    <div class="container">
      <img class="back" :src="back" @click="goBack" />

      <div class="title">이메일로 로그인하기</div>

      <label class="label">이메일</label>
      <div class="box">
        <input type="email" v-model="email" placeholder="이메일을 입력하세요" />
      </div>

      <label class="label">비밀번호</label>
      <div class="box password-box">
        <input :type="show ? 'text' : 'password'" v-model="password" placeholder="비밀번호를 입력하세요" />
        <img :src="show ? eyeClose : eyeOpen" class="eye" @click="show = !show" />
      </div>

      <button class="login-btn" @click="handleLogin" :disabled="loading">
        {{ loading ? "로그인 중..." : "로그인" }}
      </button>

      <p class="bottom-text">
        계정이 없으신가요? <span class="link" @click="goJoin">회원가입</span>
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { useRouter } from "vue-router";
import { loginAPI } from "@/api/axios";
import { useUserStore } from "@/stores/userStore";
import back from "@/assets/back.png";
import eyeOpen from "@/assets/eye-open.svg";
import eyeClose from "@/assets/eye-close.svg";

const router = useRouter();
const userStore = useUserStore();

const email = ref("");
const password = ref("");
const loading = ref(false);
const show = ref(false);

const handleLogin = async () => {
  if (!email.value || !password.value) return alert("모든 항목을 입력해주세요.");

  loading.value = true;
  try {
    const res = await loginAPI({
      email: email.value,
      password: password.value,
    });

    const d = res.data.data ?? res.data;  // ← 백엔드 응답 형태 대비

    // 토큰 저장
    userStore.saveToken(d.accessToken);

    // 유저 정보 저장
    userStore.user = {
      email: d.email,
      nickname: d.nickname,
      gender: d.gender,
      birthDate: d.birthDate,
    };

    alert("로그인 성공!");
    router.push("/home");
  } catch (err: any) {
    console.error("로그인 실패:", err);
    alert(err.response?.data?.message || "로그인 실패. 이메일 또는 비밀번호를 확인해주세요.");
  } finally {
    loading.value = false;
  }
};

const goBack = () => router.push("/");
const goJoin = () => router.push("/join");

// 🚫 여기 있던 API 호출 제거!
</script>


<style scoped>
.login {
  width: 100%;
  min-height: 100vh;
  background: #ffffff;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px 20px;
  box-sizing: border-box;
}

.container {
  width: 100%;
  max-width: 430px;
  display: flex;
  flex-direction: column;
}
.title {
  text-align: center;
}
.back {
  width: 30px;
  height: 30px;
  object-fit: contain;
  image-rendering: crisp-edges;  
  cursor: pointer;
  margin-bottom: 26px;
  margin-bottom: 26px;
}

.title {
  font-size: 22px;
  font-weight: 600;
  color: #27481e;
  margin-bottom: 26px;
}

.eye {
  position: absolute;
  right: 18px;
  width: 22px;
  height: 22px;
  cursor: pointer;
}

.label {
  font-size: 14px;
  margin-bottom: 6px;
  color: #27481e;
}

.box {
  width: 100%;
  height: 52px;
  background: #f3f4f6;
  border-radius: 10px;
  border: 1px solid #e5e5e5;
  margin-bottom: 14px;
  padding: 0 18px;
  display: flex;
  align-items: center;
}

.box input {
  width: 100%;
  border: none;
  outline: none;
  background: none;
  font-size: 15px;
  color: #27481e;
}

.password-box {
  position: relative;
}


.login-btn {
  width: 100%;
  height: 54px;
  background: #27481e;
  color: white;
  border-radius: 12px;
  border: none;
  font-size: 17px;
  cursor: pointer;
}

.login-btn:disabled {
  opacity: 0.6;
}

.bottom-text {
  font-size: 13px;
  text-align: center;
  margin-top: 24px;
  color: #7b7b7b;
}

.link {
  color: #27481e;
  font-weight: 600;
  cursor: pointer;
  text-decoration: underline;
}

@media (min-width: 768px) {
  .login {
    padding-top: 80px;
  }
  .title {
    font-size: 24px;
  }
  .login-btn {
    font-size: 18px;
  }
}
</style>

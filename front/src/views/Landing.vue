<template>
  <div class="frame">
    <div class="title-box">
      <h1 class="logo">VE:GIN</h1>
      <p class="subtitle">나의 비건 화장품 생활의 시작</p>
    </div>

    <button class="login-btn google">
      <img src="@/assets/google ic.png" class="icon" />
      Google로 시작하기
    </button>

    <button class="login-btn apple">
      <img src="@/assets/apple.png" class="icon" />
      Apple로 시작하기
    </button>

    <div class="divider-box">
      <div class="divider"></div>
      <p>또는</p>
      <div class="divider"></div>
    </div>

    <button class="login-btn email" @click="goLogin">
      이메일로 로그인
    </button>

    <button class="login-btn signup" @click="goJoin">
      회원가입
    </button>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from "vue-router";
const router = useRouter();

const goLogin = () => router.push("/login");
const goJoin = () => router.push("/join");
</script>

<style scoped>
.frame {
  width: 100%;
  height: 100vh;
  background: #ffffff;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 80px 24px;
  box-sizing: border-box;
}

.logo {
  font-size: 32px;
  color: #27481e;
  margin-bottom: 8px;
}

.title-box {
  text-align: center;
}
.subtitle {
  font-size: 14px;
  color: #27481e;
  margin-bottom: 40px;
}

.login-btn {
  width: 100%;
  height: 52px;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 500;
  margin-bottom: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-family: "Kyobo-Handwriting";
}

/* 버튼 스타일 */
.google {
  background: #fff;
  border: 1px solid #ddd;
}

.apple {
  background: #fff;
  border: 1px solid #ddd;
}

.email {
  background: #27481e;
  color: white;
  border: none;
}

.signup {
  background: transparent;
  border: 1px solid #ddd;
  color: #777;
}

/* Divider */
.divider-box {
  display: flex;
  align-items: center;
  width: 100%;
  margin: 12px 0;
  color: #888;
  font-size: 13px;
}

.divider {
  flex: 1;
  height: 1px;
  background: #ddd;
}

.icon {
  width: 20px;
  height: 20px;
}
</style>

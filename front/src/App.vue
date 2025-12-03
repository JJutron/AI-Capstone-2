<template>
  <router-view />
</template>

<script setup lang="ts">
import { onMounted } from "vue";
import { useRouter } from "vue-router";
import { useUserStore } from "@/stores/userStore";

const router = useRouter();
const userStore = useUserStore();

onMounted(() => {
  // OAuth2 콜백 처리: URL에서 토큰 확인
  const urlParams = new URLSearchParams(window.location.search);
  const token = urlParams.get("token");
  const email = urlParams.get("email");
  const userId = urlParams.get("userId");

  if (token) {
    // 토큰 저장
    userStore.saveToken(token);
    
    // 유저 정보 저장
    if (email) {
      userStore.saveUser({
        email: email,
        nickname: email.split("@")[0], // 기본 닉네임
      });
    }

    // 쿼리 파라미터 제거하고 홈으로 이동
    const newUrl = window.location.pathname;
    window.history.replaceState({}, "", newUrl);
    router.push("/home");
  }
});
</script>

<style>
/* 전역 기본 스타일은 style.css에서 처리 */
</style>

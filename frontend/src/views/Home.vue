<!-- src/views/Home.vue -->
<script setup lang="ts">
import { onMounted, computed } from "vue";
import { useRouter } from "vue-router";
import { useSkinStore } from "@/stores/skinStore";
import { useUserStore } from "@/stores/userStore";

const router = useRouter();
const skinStore = useSkinStore();
const userStore = useUserStore();

// ✨ 앱 실행 시 저장된 데이터 불러오기
onMounted(() => {
  skinStore.loadResultFromStorage();
  userStore.loadUser(); // 함수 없는 경우 대비
});

// 분석 결과
const result = computed(() => skinStore.analysisResult);

// 닉네임
const nickname = computed(() => userStore.user?.nickname || "사용자");

// 촬영 사진
const faceImageUrl = computed(() => result.value?.imageUrl || "");

// 날짜 포맷
const formattedDate = computed(() => {
  if (!skinStore.captureDate) return "-";
  return new Date(skinStore.captureDate).toLocaleDateString("ko-KR");
});

// 추천 화장품 (Guard 처리)
const recommendations = computed(() =>
  Array.isArray(result.value?.recommendations)
    ? result.value.recommendations.slice(0, 4)
    : []
);

const hasResult = computed(() => !!result.value);

function goDetail(item: any) {
  router.push(`/product/${item.productId}`);
}

function goCamera() {
  router.push("/camera");
}
</script>

<template>
  <div class="page-root">

    <!-- 결과 있을 때 -->
    <div class="home-page" v-if="hasResult">

      <!-- 타이틀 -->
      <div class="title-row">
        <div class="line"></div>
        <span class="title">{{ nickname }} 님의 피부 결과</span>
      </div>

      <!-- 메인 카드 -->
      <section class="main-card">
        <div class="top-area">
          <img :src="faceImageUrl" class="face-img" v-if="faceImageUrl" />
          <div class="right-info">
            <p class="mbti">{{ result.skinMbtiType || "분석 필요" }} 🌿</p>
            <p class="desc">{{ result.headline || result.skinDescription || "아직 설명이 없어요" }}</p>

            <p class="date">측정 날짜 <span>{{ formattedDate }}</span>
</p>

            <button class="retry-btn" @click="goCamera">다시 측정하기</button>
          </div>
        </div>
      </section>

      <!-- 추천 제품 -->
      <h2 class="sub-title">내가 추천 받은 화장품</h2>

      <div v-if="recommendations.length > 0" class="product-list">
        <div
          v-for="item in recommendations"
          :key="item.productId"
          class="product-card"
          @click="goDetail(item)"
        >
          <img :src="item.imageUrl" class="p-img" />
          <p class="p-name">{{ item.productName }}</p>
          <p class="p-price">{{ item.salePrice || "-" }}원</p>
        </div>
      </div>

      <div v-else class="empty">아직 추천 결과가 없어요 🧪</div>
    </div>

    <!-- 결과 없을 때 -->
    <div v-else class="empty">
      <p>아직 피부 진단 기록이 없어요 🧪</p>
      <button @click="goCamera">첫 측정하러 가기</button>
    </div>
  </div>
</template>

<style scoped>
.page-root {
  width: 100%;
  min-height: 100vh;
  background: #ffffff;
  display: flex;
  justify-content: center;
}

.home-page {
  width: 100%;
  max-width: 390px;
  padding: 16px 18px 40px;
}

/* ===== 상단 타이틀 ===== */
.title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 18px;
}

.line {
  flex: 1;
  height: 1px;
  background: #d9d9d9;
}

.title {
  font-size: 13px;
  color: #555;
  white-space: nowrap;
}

/* ===== 메인 카드 ===== */
.main-card {
  width: 100%;
  padding: 16px;
  background: #f8faf7;
  border-radius: 14px;
  margin-bottom: 26px;
}

.top-area {
  display: flex;
  gap: 16px;
}

.face-img {
  width: 120px;
  height: 150px;
  border-radius: 10px;
  object-fit: cover;
}

.right-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.mbti {
  font-size: 22px;
  font-weight: 700;
  color: #27481e;
  margin-bottom: 6px;
}

.desc {
  font-size: 13px;
  color: #555;
  margin-bottom: 12px;
}

.date {
  font-size: 13px;
  margin-bottom: 10px;
}

.date span {
  font-weight: 600;
}

.retry-btn {
  margin-top: auto;
  background: #27481e;
  color: #fff;
  border: none;
  height: 36px;
  border-radius: 8px;
  font-size: 13px;
}

/* ===== 추천 제품 섹션 ===== */
.sub-title {
  font-size: 15px;
  font-weight: 700;
  color: #222;
  margin-bottom: 14px;
}

.product-list {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.product-card {
  background: #fafafa;
  border-radius: 12px;
  padding: 10px;
  cursor: pointer;
}

.p-img {
  width: 100%;
  height: 120px;
  border-radius: 10px;
  object-fit: cover;
  margin-bottom: 8px;
}

.p-name {
  font-size: 12px;
  color: #333;
  margin-bottom: 4px;
}

.p-price {
  font-size: 12px;
  font-weight: 600;
  color: #27481e;
}

/* ===== Empty 상태 ===== */
.empty {
  text-align: center;
  margin-top: 120px;
  color: #333;
}

.empty button {
  margin-top: 20px;
  background: #27481e;
  padding: 12px 18px;
  color: #fff;
  border-radius: 10px;
  border: none;
}
</style>

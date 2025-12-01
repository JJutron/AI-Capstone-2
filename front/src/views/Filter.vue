<template>
  <div class="filter-container">
    <h2 class="title">원하는 가격대를 선택해주세요</h2>

    <div class="price-options">
      <div
        v-for="range in priceRanges"
        :key="range.id"
        class="price-box"
        @click="selectRange(range)"
      >
        {{ range.label }}
      </div>
    </div>

    <button class="back-btn" @click="goBack">뒤로가기</button>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from "vue-router";

const router = useRouter();

const priceRanges = [
  { id: 1, label: "10,000원 이하", min: 0, max: 10000 },
  { id: 2, label: "10,000원 ~ 20,000원", min: 10000, max: 20000 },
  { id: 3, label: "20,000원 ~ 30,000원", min: 20000, max: 30000 },
  { id: 4, label: "30,000원 이상", min: 30000, max: 999999 },
];

function selectRange(range: any) {
  router.push({
    path: "/recommendation",
    query: {
      min: range.min,
      max: range.max,
    },
  });
}

function goBack() {
  router.back();
}
</script>

<style scoped>
.filter-container {
  padding: 40px 24px;
  max-width: 393px;
  margin: 0 auto;
}

.title {
  text-align: center;
  font-size: 20px;
  margin-bottom: 30px;
}

.price-options {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.price-box {
  width: 100%;
  height: 60px;
  border-radius: 12px;
  border: 1.5px solid #244424;
  text-align: center;
  line-height: 60px;
  font-size: 16px;
  cursor: pointer;
  font-family: "Kyobo";
}

.back-btn {
  margin-top: 40px;
  width: 100%;
  height: 52px;
  background: #4b4b4b;
  color: white;
  border-radius: 12px;
  border: none;
  font-size: 16px;
  font-family: "Kyobo";
}
</style>

<template>
  <div class="age">

    <!-- Progress Bar -->
    <div class="progress-bar">
      <div class="progress-fill"></div>
      <span class="progress-text">66%</span>
    </div>

    <h2 class="title">나이를 골라주세요</h2>

    <!-- Wheel Picker -->
    <div class="picker-container">
      <ul 
        class="picker-list" 
        ref="picker"
        @scroll.passive="onScroll"
      >
        <li 
          v-for="n in 100" 
          :key="n"
          class="picker-item"
          :class="{ active: age === n - 1 }"
        >
          {{ n - 1 }}
        </li>
      </ul>

      <!-- 가운데 선택선 -->
      <div class="picker-highlight"></div>
    </div>

    <button class="continue-btn" @click="goNext">
      계속하기
    </button>

  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { useRouter } from "vue-router";

const router = useRouter();
const picker = ref<HTMLElement | null>(null);

const age = ref(25);   // 기본 25세

/* 1칸씩 스냅 */
const onScroll = () => {
  if (!picker.value) return;

  const itemHeight = 60; // li 높이
  const index = Math.round(picker.value.scrollTop / itemHeight);
  age.value = index;

  // 스냅 이동
  clearTimeout((picker.value as any)._scrollTimer);
  (picker.value as any)._scrollTimer = setTimeout(() => {
    picker.value!.scrollTo({
      top: index * itemHeight,
      behavior: "smooth"
    });
  }, 80);
};

onMounted(() => {
  if (!picker.value) return;
  picker.value.scrollTop = age.value * 60;
});

const goNext = () => {
  router.push("/camera"); // 다음 페이지
};
</script>

<style scoped>
.age {
  width: 393px;
  height: 852px;
  margin: 0 auto;
  background: #ffffff;
  padding: 40px 0;
  text-align: center;
  font-family: "Kyobo";
}

/* Progress Bar */
.progress-bar {
  width: 100%;
  height: 6px;
  background: #ccc;
  position: relative;
}
.progress-fill {
  width: 66%;
  height: 6px;
  background: #23411F;
}
.progress-text {
  position: absolute;
  right: 18px;
  top: -20px;
  color: #23411F;
}

/* Title */
.title {
  font-size: 22px;
  font-weight: bold;
  color: #23411F;
  margin-top: 70px;
}

/* Picker */
.picker-container {
  width: 100%;
  height: 180px;
  margin-top: 40px;
  position: relative;
  overflow: hidden;
}

.picker-list {
  height: 100%;
  overflow-y: scroll;
  scrollbar-width: none;
  padding: 60px 0;
  scroll-behavior: smooth;
}
.picker-list::-webkit-scrollbar {
  display: none;
}

.picker-item {
  height: 60px;
  font-size: 24px;
  color: #555;
  line-height: 60px;
}
.picker-item.active {
  color: #23411F;
  font-size: 28px;
  font-weight: bold;
}

/* Continue Button */
.continue-btn {
  width: 347px;
  height: 59px;
  margin-top: 70px;
  background: #23411F;
  color: white;
  border: none;
  font-size: 18px;
  border-radius: 10px;
  cursor: pointer;
}
</style>

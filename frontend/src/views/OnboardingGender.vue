<template>
  <div class="gender">

    <!-- Progress Bar -->
    <div class="progress-bar">
      <div class="progress-fill"></div>
      <span class="progress-text">33%</span>
    </div>

    <!-- Title -->
    <h2 class="title">성별을 골라주세요</h2>

    <!-- Gender Select -->
    <div class="gender-box">

      <!-- Female -->
      <div
        class="select-box"
        :class="{ active: selected === 'female' }"
        @click="selectGender('female')"
      >
        <img src="@/assets/female.svg" alt="female" class="icon" />
        <p class="label">Female</p>
      </div>

      <!-- Male -->
      <div
        class="select-box"
        :class="{ active: selected === 'male' }"
        @click="selectGender('male')"
      >
        <img src="@/assets/male.svg" alt="male" class="icon" />
        <p class="label">Male</p>
      </div>

    </div>

    <!-- Continue Button -->
    <button class="continue-btn" :disabled="!selected" @click="goNext">
      계속하기
    </button>

  </div>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { useRouter } from "vue-router";

const router = useRouter();
const selected = ref<string | null>(null);

const selectGender = (gender: string) => {
  selected.value = gender;
};

const goNext = () => {
  if (!selected.value) return;
  router.push("/onboardingage"); // 다음 페이지
};
</script>

<style scoped>
.gender {
  width: 393px;
  height: 852px;
  margin: 0 auto;
  background: #ffffff;
  padding: 40px 0;
  font-family: "Kyobo";
  text-align: center;
  position: relative;
}

/* Progress Bar Container */
.progress-bar {
  width: 100%;
  margin: 0 auto;
  height: 6px;
  background: #CCCCCC;
  position: relative;
}

/* Progress Fill */
.progress-fill {
  width: 33%;
  height: 6px;
  background: #23411F;
}

/* Progress Text */
.progress-text {
  position: absolute;
  right: 18px;
  top: -20px;
  font-size: 14px;
  color: #23411F;
}

/* Title */
.title {
  margin-top: 70px;
  font-size: 22px;
  font-weight: bold;
  color: #23411F;
}

/* Gender Select Container */
.gender-box {
  margin-top: 60px;
  display: flex;
  justify-content: center;
  gap: 28px;
}

/* Gender Box */
.select-box {
  width: 110px;
  height: 128px;
  border-radius: 16px;
  background: #23411F;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: 0.25s;
  opacity: 0.8;
}

/* Active (선택된 상태) */
.select-box.active {
  opacity: 1;
  transform: translateY(-4px);
  outline: 3px solid #92c57a;
}

/* Icon */
.icon {
  width: 40px;
  height: 40px;
  margin-bottom: 10px;
}

/* Label */
.label {
  font-size: 15px;
  color: white;
}

/* Continue Button */
.continue-btn {
  width: 347px;
  height: 59px;
  background: #23411F;
  color: white;
  border: none;
  border-radius: 10px;
  font-size: 18px;
  margin-top: 100px;
  cursor: pointer;
  font-family: "Kyobo";
  transition: 0.2s;
}

.continue-btn:disabled {
  background: #9daf9f;
  cursor: not-allowed;
}

.continue-btn:not(:disabled):hover {
  background: #1c3518;
}
</style>


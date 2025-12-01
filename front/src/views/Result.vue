<!-- src/views/Result.vue -->
<script setup lang="ts">
import { computed, onMounted } from "vue";
import { useRouter } from "vue-router";
import { useSkinStore } from "@/stores/skinStore";

const router = useRouter();
const skinStore = useSkinStore();

// store 에 있는 결과를 반응형으로 가져오기
const result = computed(() => skinStore.analysisResult);

onMounted(() => {
  if (!result.value) {
    router.replace("/camera");
  }
});

// ----- 인덱스 값 가져오기 -----
// MBTI 축: 0 ~ 3 점
const dryness = computed(() => result.value?.indices?.dryness ?? 1.5); // 건성-지성
const sensitivity = computed(
  () => result.value?.indices?.sensitivity ?? 1.5
); // 민감-둔감
const pigmentation = computed(
  () => result.value?.indices?.pigmentation ?? 1.5
); // 색소-비색소
const wrinkle = computed(() => result.value?.indices?.wrinkle ?? 1.5); // 주름-탄력

// 고민: 0 ~ 100 점
const acne = computed(() => result.value?.indices?.acne ?? 0);
const blemish = computed(() => result.value?.indices?.blemish ?? 0);

// ----- 값 보정 함수 -----
// 0~3 양극단 축 (건성↔지성 같은 MBTI 축)
const mapBiPolar = (value: number, max = 3) => {
  if (value == null || Number.isNaN(value)) return 50;

  const center = max / 2; // 1.5
  const norm = (value - center) / center; // -1 ~ 1

  // 차이 과장 (필요하면 1.2 ~ 1.6 사이로 조절)
  const boosted = norm * 1.4;

  // 다시 0~100 으로
  let res = 50 + boosted * 50;

  // 너무 끝까지 가지 않게 5%~95% 사이로
  res = Math.max(5, Math.min(95, res));

  return res;
};

// 0~100 단방향 점수 (여드름, 잡티)
const mapUniPolar = (value: number) => {
  if (value == null || Number.isNaN(value)) return 0;

  // 0~100 → 5~95 로 스케일
  let res = value * 0.9 + 5;

  res = Math.max(0, Math.min(100, res));
  return res;
};

// 라우팅
const goRecommend = () => {
  // 화장품 추천 페이지 라우트는 프로젝트에 맞게 수정
  router.push("/Recommendation");
};

const retry = () => {
  router.replace("/camera");
};
</script>

<template>
  <div class="page-root" v-if="result">
    <div class="result-page">
      <!-- 상단 질문 & MBTI 코드 -->
      <header class="header">
        <p class="question">
          {{
            result.userName
              ? `${result.userName} 님의 피부 타입은?`
              : "당신의 피부 타입은?"
          }}
        </p>
        <div class="mbti-code">
          {{ result.skinMbtiType }}
          <span class="leaf">🌿</span>
        </div>
        <p class="headline">
          {{ result.headline }}
        </p>
      </header>

      <!-- 초록 설명 박스 -->
      <section class="summary-card">
        <p class="summary-text">
          {{ result.skinDescription }}
        </p>
        <p class="summary-text small">
          {{ result.whiteListRecommendation }}
        </p>
      </section>

      <!-- 피부 타입 슬라이더 -->
      <section class="section">
        <h2 class="section-title">피부 타입</h2>

        <div class="slider-row">
          <span class="label-left">건성</span>
          <div class="bar">
            <div
              class="fill"
              :style="{ width: mapBiPolar(dryness) + '%' }"
            />
          </div>
          <span class="label-right">지성</span>
        </div>

        <div class="slider-row">
          <span class="label-left">민감</span>
          <div class="bar">
            <div
              class="fill"
              :style="{ width: mapBiPolar(sensitivity) + '%' }"
            />
          </div>
          <span class="label-right">둔감</span>
        </div>

        <div class="slider-row">
          <span class="label-left">색소</span>
          <div class="bar">
            <div
              class="fill"
              :style="{ width: mapBiPolar(pigmentation) + '%' }"
            />
          </div>
          <span class="label-right">비색소</span>
        </div>

        <div class="slider-row">
          <span class="label-left">주름</span>
          <div class="bar">
            <div
              class="fill"
              :style="{ width: mapBiPolar(wrinkle) + '%' }"
            />
          </div>
          <span class="label-right">탄력</span>
        </div>
      </section>

      <!-- 고민 슬라이더 -->
      <section class="section">
        <h2 class="section-title">고민</h2>

        <div class="slider-row">
          <span class="label-left">여드름</span>
          <div class="bar">
            <div
              class="fill"
              :style="{ width: mapUniPolar(acne) + '%' }"
            />
          </div>
        </div>

        <div class="slider-row">
          <span class="label-left">잡티</span>
          <div class="bar">
            <div
              class="fill"
              :style="{ width: mapUniPolar(blemish) + '%' }"
            />
          </div>
        </div>
      </section>

      <!-- 버튼 두 개 -->
      <div class="button-wrap">
        <button class="primary-btn" @click="goRecommend">
          화장품 추천 받기
        </button>
        <button class="secondary-btn" @click="retry">
          다시 측정하기
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page-root {
  width: 100%;
  min-height: 100vh;
  background: #f6f7f5;
  display: flex;
  justify-content: center;
}

/* 모바일 한 화면 폭으로 중앙 정렬 */
.result-page {
  width: 100%;
  max-width: 390px;
  padding: 24px 20px 40px;
  box-sizing: border-box;
  background: #ffffff;
}

/* 상단 영역 */
.header {
  margin-bottom: 16px;
}

.question {
  font-size: 13px;
  color: #555;
  margin-bottom: 4px;
}

.mbti-code {
  font-size: 22px;
  font-weight: 700;
  color: #27481e;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.leaf {
  font-size: 18px;
}

.headline {
  margin-top: 8px;
  font-size: 16px;
  color: #27481e;
}

/* 초록 설명 카드 */
.summary-card {
  margin-top: 12px;
  margin-bottom: 24px;
  padding: 12px 14px;
  border-radius: 10px;
  background: #27481e;
  color: #ffffff;
}

.summary-text {
  font-size: 13px;
  line-height: 1.5;
}

.summary-text.small {
  margin-top: 6px;
  opacity: 0.9;
}

/* 섹션 */
.section {
  margin-bottom: 24px;
}

.section-title {
  font-size: 14px;
  color: #27481e;
  margin-bottom: 10px;
}

/* 슬라이더 */
.slider-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
}

.label-left,
.label-right {
  font-size: 12px;
  color: #444;
  white-space: nowrap;
}

.bar {
  flex: 1;
  height: 8px;
  border-radius: 999px;
  background: #e2e5dd; /* 트랙 */
  overflow: hidden;
  position: relative;
}

.fill {
  height: 100%;
  border-radius: inherit;
  background: #27481e; /* 채워진 부분 */
  box-shadow: 0 0 4px rgba(0, 0, 0, 0.13);
  position: relative;
}

/* 버튼들 */
.button-wrap {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 12px;
}

.primary-btn,
.secondary-btn {
  width: 100%;
  height: 48px;
  border-radius: 12px;
  border: none;
  font-size: 15px;
  cursor: pointer;
}

.primary-btn {
  background: #27481e;
  color: #ffffff;
}

.secondary-btn {
  background: #f2f3f1;
  color: #27481e;
}
</style>

<!-- src/views/Survey.vue -->
<script setup lang="ts">
import { ref, computed, onMounted } from "vue";
import { useRouter } from "vue-router";
import { useSurveyStore } from "@/stores/surveyStore";

const router = useRouter();
const surveyStore = useSurveyStore();

const QUESTIONS = [
  {
    text: "평소에 얼굴에 유분감이 느껴지는 부위는 어디인가요?",
    options: ["없어요", "T존 일부(이마 혹은 코)", "T존 전체(이마와 코)", "얼굴 전체"],
  },
  {
    text: "세안 직후 얼굴에 홍조나 붉은 기가 보이나요?",
    options: ["전혀 안 보여요", "지금은 없지만 가끔 보여요", "부분적으로 붉게 보여요", "전체적으로 붉게 보여요"],
  },
  {
    text: "얼굴에 핏불이 도드라져 보이나요? 주변과 양볼을 자세히 봐주세요",
    options: ["전혀 안 보여요", "가끔 붉어지면 보여요", "특정부위에 눈에 띄어요", "곳곳에 많이 보여요"],
  },
  {
    text: "평소 얼굴에 건조함이 느껴지는 부위는 어디인가요?",
    options: ["없어요", "U존 일부(볼 혹은 턱)", "U존 전체(볼과 턱)", "얼굴 전체"],
  },
  {
    text: "여드름이 생기면 주로 어떤 상태인가요?",
    options: ["생기지 않아요", "하얀 피지가 깔끔하게 나와요", "피고름이 약간 나오고 끈적해요", "크게 곪고 통증이 심해요"],
  },
  {
    text: "웃거나 눈썹을 들어 올려보세요. 눈가와 이마에 주름이 생기나요?",
    options: ["전혀 생기지 않아요", "표정을 지을 때만 생겨요", "표정 짓지 않아도 약간 있어요", "표정 짓지 않아도 많이 있어요"],
  },
  {
    text: "주름의 깊이가 어떤가요? 선이 굵고 뚜렷한지, 얇고 여러 겹인지 봐주세요.",
    options: ["주름이 없어요", "잔주름이에요", "깊은 주름이에요", "잔주름과 깊은 주름 다 있어요"],
  },
  {
    text: "입을 다물고 살짝 미소 지어보세요. 입 주변에 팔자주름이 생기나요?",
    options: ["전혀 생기지 않아요", "미소 지을 때만 약간 생겨요", "미소 지을 때 진하게 생겨요", "미소 짓지 않아도 생겨요"],
  },
  {
    text: "얼굴에 주근깨, 잡티, 기미가 두드러지게 보이나요?",
    options: ["전혀 안 보여요", "거의 안 보여요", "약간 눈에 띄어요", "곳곳에 많이 보여요"],
  },
  {
    text: "외출 후 반나절이 지나면 피부가 번들거리나요?",
    options: ["외출 전보다 윤기가 없어요", "외출 전과 변함이 없어요", "약간 번들거리고 윤기가 있어요", "많이 번들거리고 기름져요"],
  },
];

const total = QUESTIONS.length;
const currentIndex = ref(0);

const currentQuestion = computed(() => QUESTIONS[currentIndex.value]);
const selectedAnswer = computed(
  () => surveyStore.answers[currentIndex.value] || ""
);
const progressPercent = computed(
  () => ((currentIndex.value + 1) / total) * 100
);
const canGoNext = computed(() => !!selectedAnswer.value);

const selectOption = (option: string) => {
  surveyStore.setAnswer(currentIndex.value, option);
};

const goNext = () => {
  if (!canGoNext.value) {
    alert("답변을 선택해주세요.");
    return;
  }
  if (currentIndex.value < total - 1) {
    currentIndex.value += 1;
  } else {
    router.push("/loading");
  }
};

const goPrev = () => {
  if (currentIndex.value > 0) currentIndex.value -= 1;
};

onMounted(() => {
  if (surveyStore.answers.length ===0 ) {
    surveyStore.reset();
  }
});
</script>

<template>
  <div class="page-root">
    <div class="app-page survey-page">
      <!-- 상단 진행바 -->
      <div class="progress-wrap">
        <div class="progress-top">
          <span class="progress-text">
            {{ currentIndex + 1 }} / {{ total }}
          </span>
        </div>
        <div class="progress-bar">
          <div
            class="progress-bar__fill"
            :style="{ width: progressPercent + '%' }"
          />
        </div>
      </div>

      <!-- 질문 -->
      <h1 class="question-text">
        {{ currentQuestion.text }}
      </h1>

      <!-- 선택지 -->
      <div class="options-grid">
        <button
          v-for="option in currentQuestion.options"
          :key="option"
          class="option-card"
          :class="{ active: selectedAnswer === option }"
          @click="selectOption(option)"
        >
          {{ option }}
        </button>
      </div>

      <!-- 네비 버튼 -->
      <div class="nav-buttons">
        <button
          class="nav-btn prev"
          :disabled="currentIndex === 0"
          @click="goPrev"
        >
          ◀ 이전
        </button>
        <button
          class="nav-btn next"
          :disabled="!canGoNext"
          @click="goNext"
        >
          {{ currentIndex === total - 1 ? "완료" : "계속하기" }}
        </button>
      </div>
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

.survey-page {
  display: flex;
  flex-direction: column;
  padding-top: 32px;
  padding-bottom: 32px;
}

/* 진행바 */
.progress-wrap {
  margin-bottom: 16px;
}

.progress-top {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 4px;
}

.progress-text {
  font-size: 12px;
  color: #27481e;
}

.progress-bar {
  width: 100%;
  height: 4px;
  background: #e5e5e5;
  border-radius: 999px;
  overflow: hidden;
}

.progress-bar__fill {
  height: 100%;
  background: #27481e;
  border-radius: 999px;
}

/* 질문 */
.question-text {
  font-size: 18px;
  line-height: 1.5;
  color: #27481e;
  text-align: center;
  margin-top: 20px;
  margin-bottom: 24px;
}

/* 옵션 그리드 */
.options-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 32px;
}

.option-card {
  width: 100%;
  aspect-ratio: 1 / 1;
  border-radius: 16px;
  border: 1px solid #27481e;
  background: #ffffff;
  font-size: 14px;
  color: #27481e;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 10px;
  cursor: pointer;
  transition: background 0.18s ease, color 0.18s ease;
}

.option-card.active {
  background: #27481e;
  color: #ffffff;
}

/* 네비게이션 버튼 */
.nav-buttons {
  margin-top: auto;
  display: flex;
  gap: 10px;
}

.nav-btn {
  flex: 1;
  height: 52px;
  border-radius: 12px;
  border: none;
  font-size: 16px;
  cursor: pointer;
}

.nav-btn.prev {
  background: #f3f4f6;
  color: #999999;
}

.nav-btn.prev:disabled {
  opacity: 0.6;
  cursor: default;
}

.nav-btn.next {
  background: #27481e;
  color: #ffffff;
}

.nav-btn.next:disabled {
  background: #cfd2d5;
  color: #ffffff;
  cursor: default;
}
</style>

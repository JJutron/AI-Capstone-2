// src/stores/surveyStore.ts
import { defineStore } from "pinia";
import { ref } from "vue";

export const useSurveyStore = defineStore("survey", () => {
  const answers = ref<string[]>([]);

  const setAnswer = (index: number, value: string) => {
    answers.value[index] = value;
  };

  const reset = () => {
    answers.value = [];
  };

  return { answers, setAnswer, reset };
});

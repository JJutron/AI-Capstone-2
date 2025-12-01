// src/stores/skinStore.ts
import { defineStore } from "pinia";
import { ref } from "vue";

export const useSkinStore = defineStore("skin", () => {
  const analysisResult = ref<any>(null);
  const faceImageUrl = ref("");
  const captureDate = ref("");

  // 추천 카테고리 분리
  const skinToner = ref([]);
  const ampoule = ref([]);
  const lotionCream = ref([]);

  const faceFile = ref<File | null>(null);

  function saveResult(data: any, imgUrl: string) {
    analysisResult.value = {
      ...data,
      recommendations: Array.isArray(data.recommendations) ? data.recommendations : [],
    };

    faceImageUrl.value = imgUrl || "";
    captureDate.value = new Date().toISOString();

    localStorage.setItem("vegin_skin_result", JSON.stringify(analysisResult.value));
    localStorage.setItem("vegin_face_image", faceImageUrl.value);
    localStorage.setItem("vegin_capture_date", captureDate.value);

    categorize(analysisResult.value.recommendations);
  }

  function categorize(list: any[]) {
    skinToner.value = list.filter((i) => i.category === "toner");
    ampoule.value = list.filter((i) => ["serum", "ampoule"].includes(i.category));
    lotionCream.value = list.filter((i) => ["cream", "lotion"].includes(i.category));
  }

  function loadResultFromStorage() {
    const stored = localStorage.getItem("vegin_skin_result");
    const img = localStorage.getItem("vegin_face_image");
    const date = localStorage.getItem("vegin_capture_date");

    if (stored) {
      analysisResult.value = JSON.parse(stored);
      categorize(analysisResult.value.recommendations);
    }

    faceImageUrl.value = img || "";
    captureDate.value = date || "";
  }

  function clearResult() {
    analysisResult.value = null;
    faceImageUrl.value = "";
    captureDate.value = "";
    localStorage.clear();
  }
 
  function setFaceImage(file: File) {
  faceFile.value = file;
  faceImageUrl.value = URL.createObjectURL(file);
  localStorage.setItem("vegin_face_image", faceImageUrl.value);
  localStorage.setItem("vegin_face_file", JSON.stringify({ name: file.name, type: file.type }));
}

  return {
    analysisResult,
    faceImageUrl,
    captureDate,
    skinToner,
    ampoule,
    lotionCream, 
    faceFile,
    setFaceImage,
    saveResult,
    loadResultFromStorage,
    clearResult,
  };
});

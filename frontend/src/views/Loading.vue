<template>
  <div class="loading-wrapper">
    <div class="spinner"></div>
    <p class="loading-text">{{ message }}</p>

    <!-- 오류 발생 시 안내 문구 -->
    <p v-if="isError" class="error-text">{{ message }}</p>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { useRouter } from "vue-router";
import { useSkinStore } from "@/stores/skinStore";
import { useSurveyStore } from "@/stores/surveyStore";
import { submitAnalysisAPI, getAnalysisResultAPI } from "@/api/axios";

const router = useRouter();
const skinStore = useSkinStore();
const surveyStore = useSurveyStore();

const message = ref("피부를 분석하고 있어요...");
const isError = ref(false);

onMounted(async () => {
  if (!skinStore.faceFile) {
    isError.value = true;
    message.value = "얼굴 사진이 없습니다. 다시 촬영해주세요.";
    setTimeout(() => router.replace("/camera"), 1500);
    return;
  }

  try {
    const uploadRes = await submitAnalysisAPI(skinStore.faceFile, surveyStore.answers);
    
    // ✅ 디버깅: 응답 구조 확인
    console.log("🔍 Upload response:", uploadRes);
    console.log("🔍 uploadRes.data:", uploadRes.data);
    console.log("🔍 uploadRes.data.data:", uploadRes.data?.data);
    
    // ✅ 응답 구조 확인 및 에러 처리
    const responseData = uploadRes.data?.data ?? uploadRes.data;
    
    if (!responseData) {
      console.error("❌ No data in response:", uploadRes);
      throw new Error("응답 데이터가 없습니다.");
    }
    
    const analysisId = responseData.analysisId;
    const imageUrl = responseData.s3Url || responseData.imageUrl;
    
    if (!analysisId) {
      console.error("❌ No analysisId in response:", responseData);
      throw new Error("분석 ID를 받지 못했습니다.");
    }

    const resultRes = await getAnalysisResultAPI(analysisId);
    const data = resultRes.data?.data ?? resultRes.data;

    const mapped = {
      imageUrl: imageUrl,
      skinMbtiType: data.skinMbtiType,
      skinType: data.skinDisplayName,
      headline: data.headline,
      skinDescription: data.skinDescription,
      whiteListIngredients: data.whiteListIngredients ?? [],
      whiteListRecommendation: data.whiteListRecommendation ?? "",
      blackListIngredients: data.blackListIngredients ?? [],
      indices: data.axis ?? {},
      visionRaw: data.concerns ?? {},
      recommendations: (data.recommendations ?? []).map((rec: any) => ({
        productId: rec.productId,
        productName: rec.productName,
        brand: rec.brand,
        salePrice: rec.salePrice,
        averageReviewScore: rec.averageReviewScore,
        totalReviewCount: rec.totalReviewCount,
        category: rec.category,
        imageUrl: rec.imageUrl,
        tags: rec.xaiKeywords ?? rec.tags ?? [],
      })),
    };

    skinStore.saveResult(mapped, imageUrl);
    router.replace("/result");
  } catch (e: any) {
    console.error("❌ Analysis error:", e);
    console.error("❌ Error response:", e.response);
    isError.value = true;
    
    // ✅ 더 자세한 에러 메시지
    if (e.response?.data?.message) {
      message.value = `분석 중 오류가 발생했습니다: ${e.response.data.message}`;
    } else if (e.message) {
      message.value = `분석 중 오류가 발생했습니다: ${e.message}`;
    } else {
      message.value = "분석 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.";
    }
  }
});
</script>

<style scoped>
.loading-wrapper {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background: white;
}

.spinner {
  width: 48px;
  height: 48px;
  border: 5px solid #dcdcdc;
  border-top-color: #4d8aff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

.loading-text {
  margin-top: 18px;
  font-size: 18px;
  color: #333;
  font-weight: 500;
}

.error-text {
  margin-top: 18px;
  font-size: 16px;
  color: red;
  font-weight: 600;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>

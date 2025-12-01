<!-- src/views/Recommendation.vue -->
<template>
  <div class="rec-container" v-if="result">

    <!-- 헤더 -->
    <header class="header">
      <div class="header-title">내 피부 타입 레포트</div>
      <div class="skin-type">{{ result.skinMbtiType }}🍃</div>
      <p class="skin-desc">
        {{ result.skinDescription || result.whiteListRecommendation }}
      </p>
    </header>

    <!-- 스킨/토너 -->
    <section class="section-top">
      <div class="section-label">스킨 · 토너 추천</div>
      <div class="section-sub">세안 후 첫 단계에 사용하면 좋은 추천템이에요.</div>
    </section>
    
    <section class="cards-wrapper">
      <div class="card-row">
        <ProductCard
          v-for="item in safeSkinToner"
          :key="item.productId"
          :item="item"
          @open="openDetail(item)"
        />
      </div>
      <div v-if="safeSkinToner.length === 0" class="empty-text">
        아직 추천된 스킨/토너가 없어요.
      </div>
    </section>

    <!-- 앰플/세럼 -->
    <section class="section-top">
      <div class="section-label">앰플 · 세럼 추천</div>
      <div class="section-sub">피부 타입에 맞춘 집중 케어 제품을 담았어요.</div>
    </section>
    
    <section class="cards-wrapper">
      <div class="card-row">
        <ProductCard
          v-for="item in safeAmpoule"
          :key="item.productId"
          :item="item"
          @open="openDetail(item)"
        />
      </div>
      <div v-if="safeAmpoule.length === 0" class="empty-text">
        아직 추천된 앰플/세럼이 없어요.
      </div>
    </section>

    <!-- 로션 / 크림 -->
    <section class="section-top">
      <div class="section-label">로션 · 크림 추천</div>
      <div class="section-sub">보습 유지력과 장벽 보호에 도움이 돼요.</div>
    </section>
    
    <section class="cards-wrapper">
      <div class="card-row">
        <ProductCard
          v-for="item in safeLotionCream"
          :key="item.productId"
          :item="item"
          @open="openDetail(item)"
        />
      </div>
      <div v-if="safeLotionCream.length === 0" class="empty-text">
        아직 추천된 로션/크림이 없어요.
      </div>
    </section>

    <!-- 버튼 -->
    <footer class="button-area">
      <button class="green-btn" @click="goHome">홈으로 가기</button>
    </footer>
  </div>

  <!-- 결과 없을 때 -->
  <div v-else class="empty">
    <p>아직 분석된 기록이 없어요 🧪</p>
    <button class="btn" @click="goCamera">측정하러 가기</button>
  </div>
</template>


<script setup lang="ts">
import { computed, onMounted } from "vue";
import { useRouter } from "vue-router";
import { useSkinStore } from "@/stores/skinStore";
import ProductCard from "@/components/ProductCard.vue";

const router = useRouter();
const store = useSkinStore();

onMounted(() => {
  store.loadResultFromStorage();
});

const result = computed(() => store.analysisResult);

// 카테고리별 안전 처리
const safeSkinToner = computed(() =>
  Array.isArray(result.value?.recommendations)
    ? result.value.recommendations.filter(r =>
        ["toner", "skintoner", "skin"].includes(r.category?.toLowerCase())
      )
    : []
);

const safeAmpoule = computed(() =>
  Array.isArray(result.value?.recommendations)
    ? result.value.recommendations.filter(r =>
        ["essence", "serum", "ampoule"].includes(r.category?.toLowerCase())
      )
    : []
);

const safeLotionCream = computed(() =>
  Array.isArray(result.value?.recommendations)
    ? result.value.recommendations.filter(r =>
        ["cream", "lotion"].includes(r.category?.toLowerCase())
      )
    : []
);

function openDetail(item: any) {
  router.push(`/product/${item.productId}`);
}

function goHome() {
  router.push("/home");
}

function goCamera() {
  router.push("/camera");
}
</script>


<style scoped>
.rec-container {
  width: 100%;
  max-width: 393px;
  margin: 0 auto;
  min-height: 100vh;
  padding: 32px 20px 90px;
  background: #f5f5f5;
  box-sizing: border-box;
}

/* ---- Header ---- */
.header {
  background: #ffffff;
  border-radius: 16px;
  padding: 20px 18px 18px;
  box-shadow: 0 8px 18px rgba(0, 0, 0, 0.03);
  margin-bottom: 20px;
}
.header-title {
  font-size: 14px;
  color: #888;
  margin-bottom: 6px;
}
.skin-type {
  font-size: 22px;
  font-weight: 800;
  color: #244424;
  margin-bottom: 6px;
}
.skin-desc {
  font-size: 13px;
  color: #3a5c2f;
  line-height: 20px;
}

/* ---- Category UI ---- */
.section-top {
  margin-top: 6px;
  margin-bottom: 8px;
  padding: 0 2px;
}
.section-label {
  font-size: 16px;
  font-weight: 700;
  color: #244424;
  margin-bottom: 4px;
}
.section-sub {
  font-size: 12px;
  color: #777;
  line-height: 18px;
}

/* ---- Card Scroll ---- */
.cards-wrapper {
  background: #ffffff;
  border-radius: 16px;
  padding: 16px 0 10px;
  box-shadow: 0 8px 18px rgba(0, 0, 0, 0.03);
  margin-bottom: 16px;
}
.card-row {
  display: flex;
  gap: 16px;
  padding: 4px 18px 12px;
  overflow-x: auto;
  scroll-snap-type: x mandatory;
  -webkit-overflow-scrolling: touch;
}
.card-row::-webkit-scrollbar {
  display: none;
}

/* ---- Empty ---- */
.empty-text {
  padding: 14px;
  font-size: 13px;
  text-align: center;
  color: #777;
}

.button-area {
  margin-top: 10px;
}

.green-btn {
  width: 100%;
  height: 52px;
  border-radius: 12px;
  border: none;
  font-size: 16px;
  background: #244424;
  color: #ffffff;
}
</style>

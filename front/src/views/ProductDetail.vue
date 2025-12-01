<template>
  <article class="product-card" @click="$emit('open', item.productId)">
    <div class="thumb-wrap">
      <a 
        :href="naverShoppingUrl" 
        target="_blank" 
        rel="noopener noreferrer"
        @click.stop
      >
        <img 
          :src="item.imageUrl" 
          alt="" 
          class="product-img" 
        />
      </a>
    </div>

    <div class="card-body">
      <h3 class="product-name">{{ item.productName }}</h3>
      <p class="product-brand">{{ item.brand }}</p>
      <p class="product-price">{{ formattedPrice }}</p>

      <div class="tags" v-if="item.tags && item.tags.length">
        <span v-for="(tag, i) in item.tags" :key="i" class="tag">#{{ tag }}</span>
      </div>

      <p class="review" v-if="item.averageReviewScore">
        ★ {{ item.averageReviewScore.toFixed(2) }} ({{ item.totalReviewCount }})
      </p>
    </div>
  </article>
</template>

<script setup lang="ts">
import { computed } from "vue";
import type { Product } from "@/stores/skinStore";

const props = defineProps<{ item: Product }>();

const formattedPrice = computed(() =>
  props.item.salePrice.toLocaleString() + "원"
);

const naverShoppingUrl = computed(() => {
  if (!props.item.productName) return "#";
  const encodedProductName = encodeURIComponent(props.item.productName);
  return `https://search.shopping.naver.com/search/all?query=${encodedProductName}`;
});
</script>

<style scoped>
.thumb-wrap {
  width: 100%;
  background: #f3f4f4;
}
.thumb-wrap a {
  display: block;
  width: 100%;
}
.product-img {
  width: 100%;
  display: block;
  cursor: pointer;
}
</style>

<style scoped>
.detail-container {
  padding: 24px;
  max-width: 393px;
  margin: 0 auto;
}

.main-img {
  width: 100%;
  border-radius: 12px;
}

.info {
  margin-top: 24px;
}

.name {
  font-size: 22px;
  font-weight: 700;
  margin-bottom: 8px;
}

.price {
  font-size: 18px;
  color: #244424;
}

.section {
  margin-top: 24px;
}

.section h3 {
  font-size: 17px;
  margin-bottom: 6px;
}

.desc {
  color: #555;
  font-size: 15px;
}

.back-btn {
  margin-top: 40px;
  width: 100%;
  height: 52px;
  background: #244424;
  color: white;
  font-family: "Kyobo";
  border: none;
  border-radius: 12px;
}
</style>

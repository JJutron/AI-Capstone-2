<template>
  <article class="product-card" @click="$emit('open', item.productId)">
    <div class="thumb-wrap" @click.stop>
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
          @click.stop
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
  const productName = props.item.productName || props.item.brand || "";
  if (!productName) {
    console.warn("ProductCard: productName이 없습니다.", props.item);
    return "#";
  }
  const encodedProductName = encodeURIComponent(productName);
  // 네이버 쇼핑 검색 URL (query 파라미터에 상품명 포함)
  return `https://search.shopping.naver.com/search/all?query=${encodedProductName}`;
});
</script>

<style scoped>
.product-card {
  flex: 0 0 70%;
  max-width: 260px;
  background: #ffffff;
  border-radius: 14px;
  box-shadow: 0 6px 14px rgba(0, 0, 0, 0.06);
  scroll-snap-align: start;
  cursor: pointer;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
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
.card-body {
  padding: 10px 12px 12px;
  text-align: left;
}
.product-name {
  font-size: 14px;
  font-weight: 700;
  margin-bottom: 2px;
  color: #222;
  font-family: "Kyobo";
}
.product-brand {
  font-size: 12px;
  color: #777;
}
.product-price {
  font-size: 13px;
  color: #244424;
  font-weight: 600;
  margin-top: 4px;
}
.tags {
  margin-top: 4px;
}
.tag {
  display: inline-block;
  font-size: 11px;
  color: #244424;
  background: #edf3ed;
  border-radius: 999px;
  padding: 2px 6px;
  margin-right: 4px;
}
.review {
  margin-top: 4px;
  font-size: 11px;
  color: #666;
}
</style>

<template>
  <div class="product-card" @click="$router.push(`/products/${product.slug || product.id}`)">
    <div class="image-wrapper">
      <el-image
        v-if="mainImage"
        :src="mainImage"
        :alt="product.name"
        class="product-image"
        fit="cover"
      />
      <button type="button" class="wishlist-btn" @click.stop="toggleWishlist">
        <svg v-if="!isWishlisted" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/>
        </svg>
        <svg v-else width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
          <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/>
        </svg>
      </button>
    </div>
    <div class="product-info">
      <div class="price-row">
        <span class="price">{{ displayPrice }}</span>
      </div>
      <h3 class="product-title">{{ product.name }}</h3>
      <p class="product-category">{{ product.categoryName || product.brand || 'Originals' }}</p>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue';
import { useWishlistStore } from '@/store/wishlistStore';

const props = defineProps({
  product: {
    type: Object,
    required: true
  }
});

const wishlistStore = useWishlistStore();
const isWishlisted = computed(() => wishlistStore.isWishlisted(props.product.id));

const toggleWishlist = async () => {
  try {
    await wishlistStore.toggle(props.product.id);
  } catch (error) {
    console.error("Failed to toggle wishlist:", error);
  }
};

onMounted(() => {
  wishlistStore.ensureLoaded();
});

const mainImage = computed(() => {
  const images = props.product.images || [];
  const main = images.find(img => img.isMain);
  return main ? main.url : (images[0]?.url || '');
});

const displayPrice = computed(() => {
  const variants = props.product.variants || [];
  if (variants.length === 0) return '0 ₫';
  
  const minPrice = Math.min(...variants.map(v => v.price || 0));
  return minPrice.toLocaleString('vi-VN') + ' ₫';
});
</script>

<style scoped>
.product-card {
  margin-bottom: 30px;
  cursor: pointer;
  background: #fff;
  transition: all 0.2s ease;
  border: 1px solid transparent; /* Tránh nhảy layout khi hover thêm border */
}

.product-card:hover {
  border: 1px solid #000;
}

.image-wrapper {
  position: relative;
  width: 100%;
  padding-bottom: 100%; /* Giữ tỉ lệ 1:1 hoặc điều chỉnh cho phù hợp */
  background: #f5f5f5;
  overflow: hidden;
}

.product-image {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
}

.wishlist-btn {
  position: absolute;
  top: 15px;
  right: 15px;
  z-index: 2;
  font-size: 18px;
  color: #000;
  background: transparent;
  border: none;
  padding: 5px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.2s;
}

.wishlist-btn:hover {
  transform: scale(1.1);
}

.product-info {
  padding: 10px 5px;
}

.price-row {
  margin-bottom: 6px;
}

.price {
  font-size: 14px;
  font-weight: 600;
  color: #000;
}

.product-title {
  font-size: 14px;
  margin: 0 0 4px;
  font-weight: 400;
  text-transform: uppercase;
  color: #000;
  letter-spacing: 0.5px;
  line-height: 1.4;
}

.product-category {
  font-size: 13px;
  color: #767677;
  margin: 0;
  font-weight: 300;
}

@media (max-width: 768px) {
  .product-card {
    margin-bottom: 18px;
  }

  .wishlist-btn {
    top: 10px;
    right: 10px;
    padding: 4px;
  }

  .product-title {
    font-size: 13px;
  }
}
</style>

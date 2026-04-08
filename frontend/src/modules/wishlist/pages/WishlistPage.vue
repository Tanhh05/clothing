<template>
  <div class="wishlist-page">
    <div class="container">
      <div class="header">
        <h1>SẢN PHẨM YÊU THÍCH</h1>
        <p v-if="!loading">{{ products.length }} sản phẩm</p>
      </div>

      <div v-if="loading" class="loading-state">ĐANG TẢI...</div>

      <div v-else-if="products.length === 0" class="empty-state">
        <h3>BẠN CHƯA CÓ SẢN PHẨM YÊU THÍCH</h3>
        <p>Hãy bấm vào biểu tượng trái tim tại danh sách sản phẩm để thêm.</p>
        <router-link to="/products">
          <el-button type="primary" class="go-products-btn">KHÁM PHÁ SẢN PHẨM</el-button>
        </router-link>
      </div>

      <div v-else class="product-grid">
        <ProductCard
          v-for="product in products"
          :key="product.id"
          :product="product"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from "vue";
import { ElMessage } from "element-plus";
import ProductCard from "@/modules/product/components/ProductCard.vue";
import { useWishlistStore } from "@/store/wishlistStore";
import { productApi } from "@/modules/product/api/productApi";

const wishlistStore = useWishlistStore();
const loading = ref(false);
const products = ref([]);

const loadWishlistProducts = async () => {
  loading.value = true;
  try {
    await wishlistStore.fetchWishlist(true);
    const ids = [...wishlistStore.productIds];
    if (!ids.length) {
      products.value = [];
      return;
    }

    const responses = await Promise.all(ids.map((id) => productApi.getById(id)));
    const mapped = responses.map((res) => res.data).filter(Boolean);
    products.value = ids
      .map((id) => mapped.find((p) => p.id === id))
      .filter(Boolean);
  } catch (error) {
    console.error("Failed to load wishlist products:", error);
    ElMessage.error("Không thể tải danh sách yêu thích");
  } finally {
    loading.value = false;
  }
};

watch(
  () => wishlistStore.productIds.join(","),
  () => {
    if (!loading.value) {
      loadWishlistProducts();
    }
  }
);

onMounted(() => {
  loadWishlistProducts();
});
</script>

<style scoped lang="scss">
.wishlist-page {
  padding: 40px 0 60px;
}

.container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 30px;
}

.header {
  margin-bottom: 30px;

  h1 {
    font-size: 36px;
    font-weight: 900;
    margin: 0 0 8px;
  }

  p {
    margin: 0;
    color: #666;
    font-size: 14px;
  }
}

.loading-state {
  font-size: 14px;
  color: #444;
}

.empty-state {
  text-align: center;
  padding: 80px 20px;

  h3 {
    margin: 0 0 12px;
    font-size: 24px;
    font-weight: 900;
  }

  p {
    margin: 0 0 24px;
    color: #666;
  }

  .go-products-btn {
    border-radius: 0;
    background: #000;
    border-color: #000;
    font-weight: 800;
    letter-spacing: 0.5px;
    padding: 0 28px;
  }
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;

  @media (max-width: 1200px) {
    grid-template-columns: repeat(3, 1fr);
  }

  @media (max-width: 900px) {
    grid-template-columns: repeat(2, 1fr);
  }

  @media (max-width: 640px) {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .wishlist-page {
    padding: 24px 0 40px;
  }

  .container {
    padding: 0 12px;
  }

  .header {
    margin-bottom: 18px;
  }

  .header h1 {
    font-size: 24px;
    line-height: 1.2;
  }

  .empty-state {
    padding: 52px 12px;
  }
}
</style>

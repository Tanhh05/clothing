<template>
  <div class="wishlist-page client-page-shell">
    <div class="container">
      <div class="header">
        <h1>SẢN PHẨM YÊU THÍCH</h1>
        <p v-if="!loading">{{ products.length }} sản phẩm • Tự động cảnh báo khi giảm giá</p>
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

      <div v-if="products.length" class="price-alerts">
        <h3>Wishlist thông minh: cảnh báo giảm giá</h3>
        <div class="alert-list">
          <div v-for="product in products" :key="`alert-${product.id}`" class="alert-item">
            <div class="alert-meta">
              <strong>{{ product.name }}</strong>
              <p>Giá hiện tại: {{ formatCurrency(getCurrentMinPrice(product)) }}</p>
            </div>
            <el-input-number
              v-model="priceAlertTargets[product.id]"
              :min="0"
              :step="10000"
              :controls="false"
              placeholder="Nhập giá mục tiêu"
            />
            <el-button @click="applySmartTarget(product)">Đề xuất</el-button>
            <el-button type="primary" @click="savePriceAlert(product.id)">Lưu</el-button>
          </div>
        </div>
      </div>

      <div v-if="wishlistDeals.length" class="deal-box">
        <h3>Deal đang giảm theo wishlist của bạn</h3>
        <article v-for="deal in wishlistDeals" :key="`deal-${deal.productId}`" class="deal-item">
          <div>
            <strong>{{ deal.productName }}</strong>
            <p>
              Đã giảm còn {{ formatCurrency(deal.currentMinPrice) }}
              • mục tiêu {{ formatCurrency(deal.targetPrice) }}
              • thấp hơn {{ formatCurrency(deal.diff) }}
            </p>
          </div>
          <router-link :to="`/products/${deal.productSlug || deal.productId}`">
            <el-button type="primary" plain>Xem ngay</el-button>
          </router-link>
        </article>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from "vue";
import { ElMessage } from "@/utils/dialogMessage";
import ProductCard from "@/modules/product/components/ProductCard.vue";
import { useWishlistStore } from "@/store/wishlistStore";
import { productApi } from "@/modules/product/api/productApi";
import { wishlistApi } from "@/modules/wishlist/api/wishlistApi";

const wishlistStore = useWishlistStore();
const loading = ref(false);
const products = ref([]);
const priceAlertTargets = ref({});
const wishlistDeals = ref([]);

const formatCurrency = (value) => new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(Number(value) || 0);
const getCurrentMinPrice = (product) => {
  if (!product?.variants?.length) return 0;
  return product.variants
    .map((item) => Number(item?.price || 0))
    .filter((price) => Number.isFinite(price) && price >= 0)
    .reduce((min, price) => (price < min ? price : min), Number.MAX_SAFE_INTEGER) || 0;
};
const suggestTarget = (product) => {
  const minPrice = getCurrentMinPrice(product);
  if (minPrice <= 0) return 0;
  const suggested = Math.floor((minPrice * 0.9) / 10000) * 10000;
  return Math.max(0, suggested);
};
const hydrateTargets = () => {
  const nextTargets = { ...priceAlertTargets.value };
  products.value.forEach((product) => {
    const stored = wishlistStore.getAlertTarget(product.id);
    if (nextTargets[product.id] == null) {
      nextTargets[product.id] = stored > 0 ? stored : suggestTarget(product);
    } else if (stored > 0) {
      nextTargets[product.id] = stored;
    }
  });
  priceAlertTargets.value = nextTargets;
};

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
    hydrateTargets();
    await loadDeals();
  } catch (error) {
    console.error("Failed to load wishlist products:", error);
    ElMessage.error("Không thể tải danh sách yêu thích");
  } finally {
    loading.value = false;
  }
};

const savePriceAlert = async (productId) => {
  try {
    const targetPrice = Math.max(0, Number(priceAlertTargets.value[productId] || 0));
    await wishlistStore.upsertPriceAlert(productId, targetPrice);
    priceAlertTargets.value[productId] = targetPrice;
    ElMessage.success("Đã lưu cảnh báo giá");
    await loadDeals();
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || "Không thể lưu cảnh báo giá");
  }
};

const applySmartTarget = (product) => {
  const nextTarget = suggestTarget(product);
  priceAlertTargets.value[product.id] = nextTarget;
};

const loadDeals = async () => {
  try {
    const { data } = await wishlistApi.getDeals();
    wishlistDeals.value = Array.isArray(data) ? data : [];
  } catch {
    wishlistDeals.value = [];
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

.price-alerts,
.deal-box {
  margin-top: 18px;
  border: 1px solid #e5e7eb;
  padding: 12px;
}

.alert-list {
  display: grid;
  gap: 8px;
}

.alert-item {
  display: grid;
  grid-template-columns: minmax(220px, 1fr) 180px auto auto;
  gap: 8px;
  align-items: center;
}

.alert-meta {
  strong {
    display: block;
    font-size: 14px;
    margin-bottom: 4px;
  }

  p {
    margin: 0;
    color: #64748b;
    font-size: 13px;
  }
}

.deal-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 10px 0;
  border-top: 1px dashed #e5e7eb;

  &:first-of-type {
    border-top: none;
    padding-top: 0;
  }

  p {
    margin: 4px 0 0;
    color: #374151;
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

  .alert-item {
    grid-template-columns: 1fr;
  }

  .deal-item {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>

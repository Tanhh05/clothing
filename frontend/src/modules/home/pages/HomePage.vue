<template>
  <div class="home-wrapper client-page-shell">
    <!-- Hero Section -->
    <section
      v-if="currentBanner"
      class="hero-single"
      :style="{ backgroundImage: `url('${currentBanner.imageUrl}')` }"
    >
      <a :href="currentBanner.linkUrl || '#'" class="hero-link">
        <div class="hero-overlay">
          <p class="hero-title">{{ currentBanner.title || "Bộ sưu tập mới" }}</p>
          <span class="hero-cta">Khám phá ngay</span>
        </div>
      </a>
    </section>

    <div class="home-content">
    <!-- Category Section -->
    <section class="category-section">
      <h2 class="section-title">DANH MỤC NỔI BẬT</h2>
      <div v-loading="loading" class="category-list">
        <router-link
          v-for="cat in topCategories"
          :key="cat.id"
          :to="`/products?category=${cat.id}`"
          class="category-chip"
        >
          {{ cat.name }}
        </router-link>
      </div>
    </section>

    <!-- Featured Products -->
    <section class="featured-products">
      <h2 class="section-title">HÀNG MỚI VỀ</h2>
      <div v-loading="loading" style="min-height: 200px;">
        <el-row :gutter="20" v-if="featuredProducts.length > 0">
          <el-col :xs="24" :sm="12" :md="6" :lg="6" v-for="product in featuredProducts" :key="product.id">
            <ProductCard :product="product" />
          </el-col>
        </el-row>
        <el-empty v-else description="No products found"></el-empty>
      </div>
    </section>

    <!-- Trending Section -->
    <section class="featured-products">
      <h2 class="section-title">XU HƯỚNG TỪNG TUẦN</h2>
      <div v-loading="loading" style="min-height: 200px;">
        <el-row :gutter="20" v-if="trendingProducts.length > 0">
          <el-col :xs="24" :sm="12" :md="6" :lg="6" v-for="product in trendingProducts" :key="product.id">
            <ProductCard :product="product" />
          </el-col>
        </el-row>
        <el-empty v-else description="No products found"></el-empty>
      </div>
    </section>

    <section v-if="recentViewedProducts.length" class="featured-products">
      <h2 class="section-title">ĐÃ XEM GẦN ĐÂY</h2>
      <div style="min-height: 200px;">
        <el-row :gutter="20">
          <el-col :xs="24" :sm="12" :md="6" :lg="6" v-for="product in recentViewedProducts" :key="`recent-${product.id}`">
            <ProductCard :product="product" />
          </el-col>
        </el-row>
      </div>
    </section>

    <!-- Recommended Section -->
    <section class="featured-products">
      <h2 class="section-title">CÓ THỂ BẠN CŨNG THÍCH</h2>
      <div v-loading="loading" style="min-height: 200px;">
        <el-row :gutter="20" v-if="recommendedProducts.length > 0">
          <el-col :xs="24" :sm="12" :md="6" :lg="6" v-for="product in recommendedProducts" :key="product.id">
            <ProductCard :product="product" />
          </el-col>
        </el-row>
        <el-empty v-else description="No products found"></el-empty>
      </div>
    </section>
  </div>
</div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import api from '@/services/api';
import ProductCard from '@/modules/product/components/ProductCard.vue';
import { getRecentlyViewedProducts } from '@/modules/product/utils/recentlyViewed';

const loading = ref(false);
const allProducts = ref([]);
const topCategories = ref([]);
const featuredProducts = ref([]);
const trendingProducts = ref([]);
const recommendedProducts = ref([]);
const currentBanner = ref(null);
const recentViewedProducts = computed(() => getRecentlyViewedProducts().slice(0, 4));

const getProductStock = (product) => {
  return (product.variants || []).reduce((sum, variant) => sum + (variant.stock || 0), 0);
};

const getProductMinPrice = (product) => {
  const prices = (product.variants || []).map(v => v.price).filter(p => typeof p === 'number');
  if (!prices.length) return 0;
  return Math.min(...prices);
};

const pickUnique = (source, usedIds, size) => {
  const list = [];
  for (const item of source) {
    if (!usedIds.has(item.id)) {
      list.push(item);
      usedIds.add(item.id);
      if (list.length >= size) break;
    }
  }
  return list;
};

const fillToSize = (primary, fallback, size) => {
  const merged = [...primary];
  if (merged.length >= size) {
    return merged.slice(0, size);
  }
  for (const item of fallback) {
    if (!merged.find((p) => p.id === item.id)) {
      merged.push(item);
      if (merged.length >= size) break;
    }
  }
  // Không nhân bản sản phẩm nếu dữ liệu ít; chấp nhận hiển thị ít hơn `size`.
  return merged.slice(0, size);
};

const buildHomeSections = () => {
  const newest = [...allProducts.value]
    .sort((a, b) => (b.id || 0) - (a.id || 0));

  const byTrend = [...allProducts.value]
    .sort((a, b) => getProductStock(b) - getProductStock(a) || getProductMinPrice(b) - getProductMinPrice(a));

  const byRecommend = [...allProducts.value]
    .sort((a, b) => getProductMinPrice(a) - getProductMinPrice(b) || (a.name || '').localeCompare(b.name || ''));

  const used = new Set();
  const featured = pickUnique(newest, used, 4);
  const trending = pickUnique(byTrend, used, 4);
  const recommended = pickUnique(byRecommend, used, 4);

  featuredProducts.value = fillToSize(featured, newest, 4);
  trendingProducts.value = fillToSize(trending, byTrend, 4);
  recommendedProducts.value = fillToSize(recommended, byRecommend, 4);
};

const fetchProducts = async () => {
  loading.value = true;
  try {
    const [productsResp, categoriesResp, bannersResp] = await Promise.all([
      api.get('/products', { params: { page: 0, size: 24, sortBy: 'id', direction: 'desc' }}),
      api.get('/categories', { params: { page: 0, size: 50, sortBy: 'id', direction: 'asc' }}),
      api.get('/banners')
    ]);

    allProducts.value = productsResp.data?.content || [];
    topCategories.value = (categoriesResp.data?.content || []).filter(cat => cat.parentId == null).slice(0, 8);
    currentBanner.value = Array.isArray(bannersResp.data) && bannersResp.data.length ? bannersResp.data[0] : null;
    buildHomeSections();
  } catch (error) {
    console.error('Failed to fetch home products:', error);
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  fetchProducts();
});
</script>

<style scoped>
.home-wrapper {
  display: flex;
  flex-direction: column;
  padding-bottom: 4rem;
}

.home-content {
  display: flex;
  flex-direction: column;
  gap: 4rem;
  padding-left: 20px;
  padding-right: 20px;
  max-width: 1400px;
  margin: 0 auto;
  width: 100%;
  box-sizing: border-box;
}

.category-section {
  .category-list {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;
    justify-content: center;
  }

  .category-chip {
    text-decoration: none;
    color: #111;
    border: 1px solid #ddd;
    padding: 10px 16px;
    font-size: 13px;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.4px;
    transition: all 0.2s ease;

    &:hover {
      border-color: #000;
      background: #000;
      color: #fff;
    }
  }
}

/* Hero Split Section */
.hero-single {
  position: relative;
  height: 80vh;
  min-height: 560px;
  width: 100%;
  margin-bottom: 4rem;
  background-size: cover;
  background-position: center;
  overflow: hidden;
}

.hero-link {
  display: flex;
  align-items: flex-end;
  width: 100%;
  height: 100%;
  text-decoration: none;
  color: inherit;
}

.hero-overlay {
  width: 100%;
  padding: 36px;
  background: linear-gradient(to top, rgba(0,0,0,0.62) 0%, rgba(0,0,0,0) 65%);
}

.hero-title {
  margin: 0 0 12px;
  color: #fff;
  font-size: 32px;
  font-weight: 800;
  letter-spacing: 0.6px;
  text-transform: uppercase;
}

.hero-cta {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #fff;
  color: #fff;
  padding: 8px 14px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.4px;
  text-transform: uppercase;
}

.hero-split {
  display: flex;
  height: 80vh;
  min-height: 600px;
  width: 100%;
  margin-bottom: 4rem;
}

.banner-item {
  flex: 1;
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: flex-end;
  padding: 40px;
  background-size: cover;
  background-position: center;
  transition: flex 0.4s ease;
}

.banner-item:hover {
  flex: 1.2;
}

.audi-banner {
  background-image: url('https://brand.assets.adidas.com/image/upload/f_auto,q_auto,fl_lossy/global_audi_jersey_teamwear_dna_fanwear_motorsports_ss26_launch_nh_vertical_navigation_header_1a4949f4e6.jpg');
}

.kids-banner {
  background-image: url('https://brand.assets.adidas.com/image/upload/f_auto,q_auto,fl_lossy/WC_26_Family_shopper_spot_highlight_KIDS_5897b66e52.png');
}

.sports-banner {
  background-image: url('https://brand.assets.adidas.com/image/upload/f_auto,q_auto,fl_lossy/WC_26_Away_kit_spot_highlight_MEN_WOMEN_clothing_a2111f4129.png');
}

.banner-overlay {
  z-index: 2;
  color: #fff;
  text-shadow: 0 2px 4px rgba(0,0,0,0.5);
  
  p {
    font-size: 24px;
    font-weight: 900;
    margin-bottom: 15px;
    letter-spacing: 1px;
  }
}

.banner-item::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(to top, rgba(0,0,0,0.6) 0%, transparent 60%);
  opacity: 0.8;
  transition: opacity 0.3s;
}

.banner-item:hover::after {
  opacity: 1;
}

@media (max-width: 768px) {
  .hero-single {
    min-height: 360px;
    height: 48vh;
  }

  .hero-overlay {
    padding: 20px;
  }

  .hero-title {
    font-size: 20px;
  }

  .hero-split {
    flex-direction: column;
    height: auto;
  }
  .banner-item {
    height: 350px;
    flex: none;
  }
}

/* Featured Products Section */
.featured-products {
  margin-bottom: 2rem;
}

.section-title {
  text-align: center;
  font-size: 2.2rem;
  font-weight: 700;
  margin-bottom: 2.5rem;
  color: #1a1a1a;
  position: relative;
  text-transform: uppercase;
}

.section-title::after {
  content: '';
  display: block;
  width: 60px;
  height: 4px;
  background: #1a1a1a;
  margin: 1rem auto 0;
  border-radius: 2px;
}

.view-all {
  text-align: center;
  margin-top: 2rem;
}

/* Animations */
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

@media (max-width: 992px) {
  .home-content {
    gap: 3rem;
    padding-left: 16px;
    padding-right: 16px;
  }

  .hero-split {
    min-height: 480px;
    margin-bottom: 3rem;
  }

  .banner-item {
    padding: 24px;
  }

  .section-title {
    font-size: 1.8rem;
    margin-bottom: 2rem;
  }
}

@media (max-width: 768px) {
  .home-content {
    gap: 2.5rem;
    padding-left: 12px;
    padding-right: 12px;
  }

  .hero-split {
    margin-bottom: 2.25rem;
  }

  .banner-item {
    height: 280px;
    padding: 18px;
  }

  .category-section .category-list {
    gap: 8px;
  }

  .category-section .category-chip {
    padding: 8px 12px;
    font-size: 12px;
  }

  .section-title {
    font-size: 1.45rem;
    margin-bottom: 1.25rem;
  }
}
</style>

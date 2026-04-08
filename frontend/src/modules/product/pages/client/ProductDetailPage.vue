<template>
  <div class="product-detail-container" v-loading="loading">
    <div v-if="product" class="product-layout">
      <!-- LEFT: Image Gallery -->
       
      <div class="product-gallery">
        <div class="breadcrumb-nav">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">Trang Chủ</el-breadcrumb-item>
            <el-breadcrumb-item :to="{ path: '/products', query: { category: product.categoryId } }">
              {{ product.categoryName }}
            </el-breadcrumb-item>
            <el-breadcrumb-item>{{ product.name }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        
        <div class="image-grid">
          <div v-for="(img, index) in product.images" :key="img.id" class="image-wrapper" :class="{ 'full-width': index === 0 }">
            <el-image :src="img.url" fit="cover" class="product-img">
              <template #placeholder>
                <div class="image-slot">Loading<span class="dot">...</span></div>
              </template>
            </el-image>
          </div>
        </div>
      </div>

      <!-- RIGHT: Product Info -->
      <div class="product-info-sidebar">
        <div class="sidebar-content">
          <div class="category-info">
            {{ product.categoryName }} • {{ product.brand }}
          </div>
          
          <div class="rating-stars">
            <el-rate :model-value="ratingValue" disabled show-score text-color="#000" :score-template="ratingScoreTemplate" />
          </div>

          <h1 class="product-title">{{ product.name.toUpperCase() }}</h1>
          <div class="product-price">{{ formatPrice(selectedVariant?.price || product.variants[0]?.price) }}</div>

          <p class="promo-text">
            Sản phẩm này không được hưởng chiết khấu từ các chương trình khuyến mãi hoặc ưu đãi khác.
          </p>

          <div class="selection-section">
            <label class="selection-label">Màu sắc: <span>{{ selectedColor }}</span></label>
            <div class="color-options">
              <button 
                v-for="color in uniqueColors" 
                :key="color"
                class="color-btn"
                :class="{ active: selectedColor === color }"
                @click="selectedColor = color"
              >
                {{ color }}
              </button>
            </div>
          </div>

          <div class="selection-section">
            <div class="selection-header">
              <label class="selection-label">Kích cỡ</label>
              <button type="button" class="size-guide" @click="openInfoDialog('sizeGuide')">Hướng dẫn chọn kích cỡ</button>
            </div>
            <div class="size-grid">
              <button 
                v-for="v in filteredVariantsBySize" 
                :key="v.id"
                class="size-btn"
                :class="{ active: selectedSize === v.size, 'out-of-stock': v.stock <= 0 }"
                :disabled="v.stock <= 0"
                @click="selectedSize = v.size"
              >
                {{ v.size }}
              </button>
            </div>
          </div>

          <div class="info-alert" v-if="selectedSize">
            <el-icon><InfoFilled /></el-icon>
            <span><strong>Đúng kích cỡ.</strong> Chúng tôi khuyên bạn nên đặt theo kích cỡ thông thường.</span>
          </div>

          <div class="cart-actions">
            <el-button type="primary" class="add-to-cart-btn" @click="handleAddToCart">
              THÊM VÀO GIỎ HÀNG <el-icon class="el-icon--right"><ShoppingBag /></el-icon>
            </el-button>
            <button type="button" class="wishlist-btn" @click="handleToggleWishlist">
              <el-icon><StarFilled v-if="isWishlisted" /><Star v-else /></el-icon>
            </button>
          </div>

          <div class="secondary-actions">
            <el-button class="find-similar-btn" @click="scrollToRelated">Tìm mẫu tương tự <el-icon class="el-icon--right"><Right /></el-icon></el-button>
          </div>

          <div class="value-props">
            <button type="button" class="prop-item" @click="openInfoDialog('shipping')">
              <div class="prop-header">
                <span>FREE SHIPPING FOR ADICLUB MEMBERS!</span>
                <el-icon><Right /></el-icon>
              </div>
            </button>
            <button type="button" class="prop-item" @click="openInfoDialog('returns')">
              <div class="prop-header">
                <span>HOÀN TRẢ DỄ DÀNG</span>
                <el-icon><Right /></el-icon>
              </div>
            </button>
          </div>

          <div class="adiclub-info">
            <div class="adiclub-header">
              <strong>Mua sắm và nhận</strong>
              <span class="adiclub-logo">TWENTY</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <section ref="relatedSectionRef" v-if="relatedProducts.length" class="discovery-section">
      <h2 class="discovery-title">SẢN PHẨM TƯƠNG TỰ</h2>
      <div class="discovery-grid">
        <ProductCard
          v-for="item in relatedProducts"
          :key="`related-${item.id}`"
          :product="item"
        />
      </div>
    </section>

    <section v-if="recentViewedProducts.length" class="discovery-section">
      <h2 class="discovery-title">ĐÃ XEM GẦN ĐÂY</h2>
      <div class="discovery-grid">
        <ProductCard
          v-for="item in recentViewedProducts"
          :key="`recent-${item.id}`"
          :product="item"
        />
      </div>
    </section>

    <el-dialog
      v-model="infoDialogVisible"
      :title="infoDialogTitle"
      width="480px"
      class="product-info-dialog"
    >
      <p class="dialog-content">{{ infoDialogContent }}</p>
      <template #footer>
        <el-button @click="infoDialogVisible = false">Đóng</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch } from 'vue';
import { useRoute } from 'vue-router';
import { productApi } from '../../api/productApi';
import { ShoppingBag, Star, StarFilled, InfoFilled, Right } from '@element-plus/icons-vue';
import { useCartStore } from '@/store/cartStore';
import { useWishlistStore } from '@/store/wishlistStore';
import { ElMessage } from 'element-plus';
import ProductCard from '@/modules/product/components/ProductCard.vue';
import { getRecentlyViewedProducts, trackRecentlyViewedProduct } from '@/modules/product/utils/recentlyViewed';

const route = useRoute();
const cartStore = useCartStore();
const wishlistStore = useWishlistStore();
const loading = ref(true);
const product = ref(null);

const selectedColor = ref('');
const selectedSize = ref('');
const infoDialogVisible = ref(false);
const infoDialogTitle = ref('');
const infoDialogContent = ref('');
const relatedProducts = ref([]);
const recentViewedProducts = ref([]);
const relatedSectionRef = ref(null);

const formatPrice = (price) => {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
};

const uniqueColors = computed(() => {
  if (!product.value) return [];
  return [...new Set(product.value.variants.map(v => v.color))];
});

const filteredVariantsBySize = computed(() => {
  if (!product.value) return [];
  // For the current selected color, show all sizes
  return product.value.variants.filter(v => v.color === selectedColor.value);
});

const selectedVariant = computed(() => {
  if (!product.value || !selectedColor.value || !selectedSize.value) return null;
  return product.value.variants.find(v => v.color === selectedColor.value && v.size === selectedSize.value);
});

const ratingValue = computed(() => {
  const value = Number(product.value?.ratingAvg);
  return Number.isFinite(value) ? value : 0;
});

const ratingScoreTemplate = computed(() => {
  const count = Number(product.value?.reviewCount ?? 0);
  const safeCount = Number.isFinite(count) && count >= 0 ? count : 0;
  return `{value} (${safeCount})`;
});

const openInfoDialog = (type) => {
  const contentMap = {
    sizeGuide: {
      title: 'Hướng dẫn chọn kích cỡ',
      content: 'Đo vòng ngực, eo và hông của bạn, sau đó đối chiếu với bảng size S/M/L/XL trong phần mô tả sản phẩm. Nếu số đo nằm giữa hai size, ưu tiên size lớn hơn để mặc thoải mái.'
    },
    shipping: {
      title: 'Chính sách giao hàng',
      content: 'Thành viên được miễn phí vận chuyển theo điều kiện chương trình. Thời gian giao hàng nội thành dự kiến 1-3 ngày, ngoại thành 3-5 ngày làm việc.'
    },
    returns: {
      title: 'Chính sách hoàn trả',
      content: 'Bạn có thể yêu cầu hoàn trả trong vòng 7 ngày kể từ khi nhận hàng nếu sản phẩm còn nguyên tem, chưa qua sử dụng và có đầy đủ phụ kiện đi kèm.'
    }
  };
  const selected = contentMap[type];
  if (!selected) return;
  infoDialogTitle.value = selected.title;
  infoDialogContent.value = selected.content;
  infoDialogVisible.value = true;
};

const isWishlisted = computed(() => {
  if (!product.value?.id) return false;
  return wishlistStore.isWishlisted(product.value.id);
});

const fetchProduct = async () => {
  loading.value = true;
  try {
    const { data } = await productApi.getById(route.params.slug);
    product.value = data;
    trackRecentlyViewedProduct(data);
    recentViewedProducts.value = getRecentlyViewedProducts().filter((item) => item.id !== data.id).slice(0, 4);
    
    // Set initial selections
    if (product.value.variants.length > 0) {
      selectedColor.value = product.value.variants[0].color;
      // We don't pre-select size to match Adidas UX
    }

    const relatedResp = await productApi.getProducts({
      page: 0,
      size: 12,
      category: product.value.categoryId,
      sortBy: 'id',
      direction: 'desc'
    });
    const source = Array.isArray(relatedResp.data?.content) ? relatedResp.data.content : [];
    relatedProducts.value = source.filter((item) => item.id !== product.value.id).slice(0, 4);
  } catch (error) {
    console.error(error);
    ElMessage.error('Không thể lấy thông tin sản phẩm');
  } finally {
    loading.value = false;
  }
};

const scrollToRelated = () => {
  if (!relatedProducts.value.length || !relatedSectionRef.value) {
    ElMessage.info('Hiện chưa có sản phẩm tương tự');
    return;
  }
  relatedSectionRef.value.scrollIntoView({ behavior: 'smooth', block: 'start' });
};

const handleAddToCart = async () => {
  if (!selectedSize.value) {
    ElMessage.warning('Vui lòng chọn kích cỡ');
    return;
  }
  
  if (product.value && selectedVariant.value) {
    await cartStore.addItem(product.value, selectedVariant.value, 1);
  }
};

const handleToggleWishlist = async () => {
  if (!product.value?.id) return;
  try {
    await wishlistStore.toggle(product.value.id);
  } catch (error) {
    console.error("Failed to toggle wishlist:", error);
  }
};

onMounted(() => {
  wishlistStore.ensureLoaded();
  fetchProduct();
});

watch(
  () => route.params.slug,
  () => {
    fetchProduct();
  }
);
</script>

<style scoped lang="scss">
.product-detail-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
}

.product-layout {
  display: flex;
  gap: 40px;
  position: relative;
}

/* LEFT: GALLERY */
.product-gallery {
  flex: 1;
  
  .breadcrumb-nav {
    font-size: 13px;
    margin-bottom: 20px;
    color: #333;

    :deep(.el-breadcrumb__inner) {
      color: #444;
      font-weight: 500;
      text-decoration: none;
    }

    :deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner) {
      color: #111;
      font-weight: 700;
    }

    :deep(a.el-breadcrumb__inner:hover) {
      color: #000;
      text-decoration: underline;
    }
  }

  .image-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 15px;

    .image-wrapper {
      background: #f5f5f5;
      
      &.full-width {
        grid-column: span 1; // You can change this to 2 if you want the first image to be hero-style
      }

      .product-img {
        width: 100%;
        display: block;
      }
    }
  }
}

/* RIGHT: SIDEBAR */
.product-info-sidebar {
  width: 400px;
  
  .sidebar-content {
    position: sticky;
    top: 100px;
  }

  .category-info {
    font-size: 13px;
    font-weight: 500;
    margin-bottom: 10px;
  }

  .rating-stars {
    margin-bottom: 15px;
  }

  .product-title {
    font-size: 32px;
    font-weight: 900;
    line-height: 1.1;
    margin-bottom: 10px;
    letter-spacing: -0.5px;
  }

  .product-price {
    font-size: 18px;
    font-weight: 800;
    margin-bottom: 30px;
  }

  .promo-text {
    font-size: 14px;
    line-height: 1.4;
    margin-bottom: 30px;
  }

  .selection-section {
    margin-bottom: 30px;
    
    .selection-label {
      display: block;
      font-size: 14px;
      font-weight: 800;
      margin-bottom: 15px;
      
      span { font-weight: 400; }
    }

      .selection-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 15px;
      
      .size-guide {
        border: none;
        background: transparent;
        padding: 0;
        font-size: 13px;
        text-decoration: underline;
        color: #000;
        cursor: pointer;
      }
    }
  }

  .color-options {
    display: flex;
    gap: 10px;
    
    .color-btn {
      padding: 8px 15px;
      border: 1px solid #ebedee;
      background: #fff;
      font-weight: 700;
      cursor: pointer;
      
      &:hover { border-color: #000; }
      &.active { border-color: #000; background: #000; color: #fff; }
    }
  }

  .size-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 5px;
    
    .size-btn {
      height: 40px;
      border: 1px solid #ebedee;
      background: #fff;
      font-weight: 600;
      font-size: 13px;
      cursor: pointer;
      
      &:hover { border-color: #000; }
      &.active { border-color: #000; background: #000; color: #fff; }
      &.out-of-stock {
        color: #ccc;
        cursor: not-allowed;
        background: #f9f9f9;
        text-decoration: line-through;
      }
    }
  }

  .info-alert {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 15px;
    border: 1px solid #ebedee;
    margin-bottom: 30px;
    font-size: 13px;
    line-height: 1.4;
  }

  .cart-actions {
    display: flex;
    gap: 10px;
    margin-bottom: 15px;
    
    .add-to-cart-btn {
      flex: 1;
      height: 55px;
      background: #000;
      border-color: #000;
      border-radius: 0;
      font-weight: 900;
      font-size: 13px;
      letter-spacing: 1px;
      justify-content: space-between;
      padding: 0 20px;
      
      &:hover { background: #333; }
    }

    .wishlist-btn {
      width: 55px;
      height: 55px;
      border: 1px solid #ebedee;
      background: #fff;
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      font-size: 20px;
      
      &:hover { border-color: #000; }
    }
  }

  .find-similar-btn {
    width: 100%;
    height: 50px;
    border: 1px solid #000;
    border-radius: 0;
    background: #fff;
    color: #000;
    font-weight: 800;
    text-transform: uppercase;
    font-size: 13px;
    letter-spacing: 1px;
    margin-bottom: 40px;
    justify-content: space-between;
    padding: 0 20px;
    
    &:hover { background: #f5f5f5; }
  }

  .value-props {
    margin-bottom: 40px;
    
    .prop-item {
      width: 100%;
      border: none;
      background: transparent;
      padding: 15px 0;
      border-bottom: 1px solid #ebedee;
      cursor: pointer;
      text-align: left;
      
      .prop-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        font-size: 12px;
        font-weight: 700;
        letter-spacing: 0.5px;
      }
    }
  }

  .adiclub-info {
    .adiclub-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 10px;
      
      .adiclub-logo {
        font-weight: 900;
        font-size: 18px;
        letter-spacing: 1px;
      }
    }
    
    p {
      display: flex;
      align-items: center;
      gap: 5px;
      font-size: 13px;
      color: #666;
    }
  }
}

.dialog-content {
  margin: 0;
  color: #333;
  line-height: 1.55;
}

.discovery-section {
  margin-top: 48px;

  .discovery-title {
    margin: 0 0 18px;
    font-size: 24px;
    font-weight: 900;
    letter-spacing: -0.3px;
  }

  .discovery-grid {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 16px;
  }
}

@media (max-width: 1024px) {
  .product-layout {
    flex-direction: column;
  }
  
  .product-info-sidebar {
    width: 100%;
    
    .sidebar-content {
      position: static;
    }
  }
  
  .image-grid {
    grid-template-columns: 1fr !important;
  }

  .discovery-section .discovery-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .product-detail-container {
    padding: 12px;
  }

  .product-layout {
    gap: 20px;
  }

  .product-gallery {
    .breadcrumb-nav {
      font-size: 12px;
      line-height: 1.4;
      word-break: break-word;
    }

    .image-grid {
      gap: 10px;
    }
  }

  .product-info-sidebar {
    .product-title {
      font-size: 24px;
    }

    .selection-section {
      margin-bottom: 20px;
    }

    .selection-header {
      flex-wrap: wrap;
      gap: 8px;
    }

    .size-grid {
      grid-template-columns: repeat(3, 1fr);
    }

    .cart-actions {
      .add-to-cart-btn {
        font-size: 12px;
        padding: 0 12px;
      }
    }
  }
}

@media (max-width: 480px) {
  .product-info-sidebar {
    .size-grid {
      grid-template-columns: repeat(2, 1fr);
    }

    .color-options {
      flex-wrap: wrap;
    }

    .find-similar-btn {
      font-size: 12px;
      padding: 0 14px;
    }
  }

  .discovery-section .discovery-grid {
    grid-template-columns: 1fr;
  }
}
</style>

<template>
  <div class="cart-page-container client-page-shell">
    <div v-loading="cartStore.loading" class="cart-wrapper">
      <div v-if="!cartStore.isEmpty" class="cart-layout">
        <!-- LEFT: Cart Items -->
        <div class="cart-main">
          <div class="cart-header">
            <h1 class="cart-title">GIỎ HÀNG CỦA BẠN <span>({{ cartStore.totalItems }} sản phẩm)</span></h1>
            <p class="cart-subtitle">
              Các mặt hàng trong giỏ hàng của bạn không được bảo lưu — hãy kiểm tra ngay để đặt hàng.
            </p>
          </div>

          <div class="cart-items-list">
            <div v-for="item in displayCartItems" :key="item.id" class="cart-item-card">
              <div class="item-image-box">
                <el-image :src="item.productImage" fit="cover" class="item-img">
                  <template #placeholder>
                    <div class="image-slot">...</div>
                  </template>
                </el-image>
              </div>
              <div class="item-content">
                <div class="item-top">
                  <router-link :to="`/products/${item.productSlug || item.productId}`" class="item-name">
                    {{ item.productName.toUpperCase() }}
                  </router-link>
                  <div class="item-utils">
                    <el-icon class="util-icon" @click="handleRemove(item)"><Delete /></el-icon>
                    <button
                      type="button"
                      class="wishlist-icon-btn"
                      :class="{ active: isWishlisted(item.productId) }"
                      @click="handleToggleWishlist(item.productId)"
                      aria-label="Thêm vào yêu thích"
                    >
                      <svg v-if="!isWishlisted(item.productId)" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7">
                        <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/>
                      </svg>
                      <svg v-else width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
                        <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/>
                      </svg>
                    </button>
                  </div>
                </div>
                <p class="item-meta">{{ item.color }}</p>
                <p class="item-meta">Kích cỡ: {{ item.size }}</p>

                <div class="item-bottom">
                  <div class="quantity-control">
                    <el-select v-model="item.quantity" @change="val => handleUpdateQuantity(item.id, val)" size="default" class="qty-select">
                      <el-option v-for="n in 10" :key="n" :label="n" :value="n" />
                    </el-select>
                  </div>
                  <div class="item-price">{{ formatCurrency(item.price * item.quantity) }}</div>
                </div>
              </div>
            </div>
          </div>

          <!-- Recommendations -->
          <div class="cart-recommendations">
            <h2 class="section-heading">GỢI Ý CHO BẠN</h2>
            <div v-if="recommendedProducts.length > 0" class="rec-row">
              <router-link
                v-for="product in recommendedProducts"
                :key="product.id"
                :to="`/products/${product.slug || product.id}`"
                class="rec-item"
              >
                <el-image :src="product.image" fit="cover" class="rec-img">
                  <template #placeholder>
                    <div class="rec-img-placeholder"></div>
                  </template>
                </el-image>
                <div class="rec-info">
                  <div class="rec-price">{{ formatCurrency(product.price) }}</div>
                  <div class="rec-name">{{ product.name }}</div>
                </div>
              </router-link>
            </div>
            <div v-else class="rec-empty">
              Chưa có gợi ý phù hợp ở thời điểm này.
            </div>
          </div>
        </div>

        <!-- RIGHT: Order Summary -->
        <div class="cart-sidebar">
          <div class="summary-box">
            <h2 class="summary-title">TÓM TẮT ĐƠN HÀNG</h2>

            <div class="sum-row">
              <span>{{ cartStore.totalItems }} sản phẩm</span>
              <span>{{ formatCurrency(cartStore.totalPrice) }}</span>
            </div>
            <div class="sum-row">
              <span>Phí ship tạm tính</span>
              <span>{{ estimatedShippingFee === 0 ? "Miễn phí" : formatCurrency(estimatedShippingFee) }}</span>
            </div>

            <div class="free-ship-progress">
              <div class="progress-head">
                <span>Tiến độ miễn phí ship</span>
                <strong>{{ freeShipProgress }}%</strong>
              </div>
              <el-progress :percentage="freeShipProgress" :stroke-width="8" :show-text="false" />
              <p v-if="remainingForFreeShip > 0">
                Mua thêm {{ formatCurrency(remainingForFreeShip) }} để được miễn phí ship.
              </p>
              <p v-else>Bạn đã đạt ưu đãi miễn phí ship.</p>
            </div>

            <div class="sum-total">
              <span class="total-label">Tạm tính</span>
              <span class="total-value">{{ formatCurrency(estimatedGrandTotalAfterDiscount) }}</span>
            </div>
            <p class="tax-note">(Đã bao gồm thuế)</p>
            <p class="tax-note" v-if="estimatedShippingFee > 0">
              Miễn phí ship từ {{ formatCurrency(storeSettingsStore.freeShippingThreshold) }}
            </p>
            <p class="tax-note">{{ storeSettingsStore.shippingPolicy }}</p>

            <div class="promo-link">
              <el-icon><Ticket /></el-icon>
              <span v-if="bestVoucher?.code">
                Đề xuất mã tốt nhất: <strong>{{ bestVoucher.code }}</strong>
                (giảm {{ formatCurrency(bestVoucher.discountAmount || 0) }})
              </span>
              <span v-else>Chưa có mã khuyến mãi phù hợp</span>
            </div>
            <el-select
              v-model="selectedVoucherCode"
              class="checkout-select"
              clearable
              placeholder="Chọn mã khuyến mãi"
              @change="handleVoucherChange"
            >
              <el-option
                v-for="voucher in eligibleVouchers"
                :key="voucher.id"
                :label="`${voucher.code}${voucher.minOrderValue ? ` - Tối thiểu ${formatCurrency(voucher.minOrderValue)}` : ''}`"
                :value="voucher.code"
              />
            </el-select>
            <p class="tax-note" v-if="selectedVoucherCode">
              Đang áp dụng: <strong>{{ selectedVoucherCode }}</strong> (giảm {{ formatCurrency(appliedVoucherDiscount) }})
            </p>
            <el-button type="primary" class="checkout-submit-btn" @click="goCheckout">
              ĐẾN TRANG THANH TOÁN <el-icon class="el-icon--right"><Right /></el-icon>
            </el-button>
          </div>
        </div>
      </div>

      <!-- Empty State -->
      <div v-else class="empty-layout">
        <h1 class="empty-title">GIỎ HÀNG CỦA BẠN ĐANG TRỐNG</h1>
        <p class="empty-msg">Bạn chưa có sản phẩm nào trong giỏ hàng. Hãy lấp đầy nó bằng những món đồ yêu thích nhé!</p>
        <router-link to="/products">
          <el-button type="primary" class="start-shopping-btn">MUA SẮM NGAY</el-button>
        </router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue';
import { useCartStore } from '@/store/cartStore';
import { Delete, Right, Ticket } from '@element-plus/icons-vue';
import { useRouter } from 'vue-router';
import { ElMessage } from "@/utils/dialogMessage";
import api from '@/services/api';
import { useWishlistStore } from '@/store/wishlistStore';
import { useStoreSettingsStore } from '@/store/storeSettingsStore';
import { voucherApi } from '@/modules/voucher/api/voucherApi';
import { useConfirmDialog } from '@/composables/useConfirmDialog';

const cartStore = useCartStore();
const wishlistStore = useWishlistStore();
const storeSettingsStore = useStoreSettingsStore();
const router = useRouter();
const { confirm } = useConfirmDialog();
const productCatalog = ref([]);
const bestVoucher = ref(null);
const availableVouchers = ref([]);
const removeConfirming = ref(false);
const estimatedShippingFee = computed(() => {
  const baseFee = storeSettingsStore.defaultShippingFee;
  const threshold = storeSettingsStore.freeShippingThreshold;
  if (!Number.isFinite(baseFee) || baseFee < 0) return 0;
  if (!Number.isFinite(threshold) || threshold <= 0) return baseFee;
  if (cartStore.totalPrice >= threshold) return 0;
  return baseFee;
});

const estimatedGrandTotal = computed(() => cartStore.totalPrice + estimatedShippingFee.value);
const selectedVoucherCode = computed({
  get: () => cartStore.preferredVoucherCode || '',
  set: (value) => cartStore.setPreferredVoucherCode(value)
});
const eligibleVouchers = computed(() => {
  const subTotal = Number(cartStore.totalPrice || 0);
  return availableVouchers.value.filter((voucher) => {
    if (String(voucher?.status || '').toUpperCase() !== 'ACTIVE') return false;
    return Number(voucher?.minOrderValue || 0) <= subTotal;
  });
});
const selectedVoucher = computed(() =>
  eligibleVouchers.value.find((voucher) => voucher.code === selectedVoucherCode.value) || null
);
const calculateDiscount = (voucher, subTotal) => {
  if (!voucher || subTotal <= 0) return 0;
  const discountType = String(voucher.discountType || '').trim().toUpperCase();
  const discountValue = Number(voucher.discountValue || 0);
  if (discountValue <= 0) return 0;
  if (discountType === 'PERCENT') return Math.min(subTotal, Math.round(subTotal * (discountValue / 100)));
  if (discountType === 'AMOUNT') return Math.min(subTotal, discountValue);
  return 0;
};
const appliedVoucherDiscount = computed(() => {
  const subTotal = Number(cartStore.totalPrice || 0);
  if (subTotal <= 0) return 0;
  if (!selectedVoucherCode.value) return 0;
  if (selectedVoucherCode.value && bestVoucher.value?.code === selectedVoucherCode.value) {
    return Number(bestVoucher.value?.discountAmount || 0);
  }
  if (selectedVoucher.value) {
    return calculateDiscount(selectedVoucher.value, subTotal);
  }
  return 0;
});
const estimatedGrandTotalAfterDiscount = computed(() =>
  Math.max(0, estimatedGrandTotal.value - appliedVoucherDiscount.value)
);
const remainingForFreeShip = computed(() => {
  const threshold = Number(storeSettingsStore.freeShippingThreshold || 0);
  if (!Number.isFinite(threshold) || threshold <= 0) return 0;
  return Math.max(0, threshold - Number(cartStore.totalPrice || 0));
});
const freeShipProgress = computed(() => {
  const threshold = Number(storeSettingsStore.freeShippingThreshold || 0);
  if (!Number.isFinite(threshold) || threshold <= 0) return 100;
  const percent = Math.round((Number(cartStore.totalPrice || 0) / threshold) * 100);
  return Math.max(0, Math.min(100, percent));
});

const variantLookup = computed(() => {
  const lookup = new Map();
  for (const product of productCatalog.value) {
    for (const variant of product.variants || []) {
      lookup.set(variant.id, { product, variant });
    }
  }
  return lookup;
});

const displayCartItems = computed(() => {
  return cartStore.items.map((item) => {
    const matched = variantLookup.value.get(item.variantId);
    const product = matched?.product;
    const variant = matched?.variant;
    return {
      ...item,
      productId: item.productId ?? product?.id ?? null,
      productSlug: product?.slug || null,
      productName: item.productName ?? product?.name ?? item.sku ?? 'Sản phẩm',
      productImage: item.productImage ?? product?.images?.[0]?.url ?? '',
      size: item.size ?? variant?.size ?? 'N/A',
      color: item.color ?? variant?.color ?? 'N/A'
    };
  });
});

const recommendedProducts = computed(() => {
  const cartProductIds = new Set(displayCartItems.value.map((item) => item.productId).filter(Boolean));
  const cartCategoryIds = new Set(
    displayCartItems.value
      .map((item) => {
        const matched = variantLookup.value.get(item.variantId);
        return matched?.product?.categoryId;
      })
      .filter(Boolean)
  );

  return productCatalog.value
    .filter((product) => !cartProductIds.has(product.id))
    .map((product) => {
      let score = 0;
      if (cartCategoryIds.has(product.categoryId)) score += 10;
      if (String(product.status || '').toUpperCase() === 'ACTIVE') score += 1;
      return { product, score };
    })
    .sort((a, b) => b.score - a.score || Number(b.product.id || 0) - Number(a.product.id || 0))
    .slice(0, 4)
    .map(({ product }) => ({
      id: product.id,
      slug: product.slug,
      name: product.name,
      price: product.variants?.[0]?.price ?? 0,
      image: product.images?.[0]?.url ?? ''
    }));
});

const formatCurrency = (value) => {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value);
};

const handleUpdateQuantity = async (itemId, qty) => {
  await cartStore.updateQuantity(itemId, qty);
};

const handleRemove = async (item) => {
  if (!item?.id || removeConfirming.value) return;
  removeConfirming.value = true;
  try {
    await confirm({
      title: 'Xác nhận',
      message: `Bạn có chắc muốn xóa ${item.productName || 'sản phẩm này'} khỏi giỏ hàng?`,
      confirmButtonText: 'OK',
      cancelButtonText: 'Cancel',
      onConfirm: async () => {
        await cartStore.removeItem(item.id);
      }
    });
    ElMessage({
      type: 'success',
      message: 'Đã xóa sản phẩm khỏi giỏ hàng',
      offset: 72
    });
  } catch (e) {
    // cancelled
  } finally {
    removeConfirming.value = false;
  }
};

const goCheckout = () => router.push('/checkout');

const isWishlisted = (productId) => {
  if (!productId) return false;
  return wishlistStore.isWishlisted(productId);
};

const handleToggleWishlist = async (productId) => {
  if (!productId) return;
  try {
    await wishlistStore.toggle(productId);
  } catch (error) {
    console.error('Failed to toggle wishlist from cart:', error);
  }
};

const fetchCatalogProducts = async () => {
  try {
    const response = await api.get('/products', {
      params: { page: 0, size: 120, sortBy: 'id', direction: 'desc' }
    });
    productCatalog.value = Array.isArray(response.data?.content) ? response.data.content : [];
  } catch (error) {
    console.error('Failed to fetch product catalog:', error);
  }
};

const fetchBestVoucher = async () => {
  try {
    const { data } = await voucherApi.getBestVoucher(cartStore.totalPrice || 0);
    bestVoucher.value = data || null;
    if (!selectedVoucherCode.value && bestVoucher.value?.code) {
      selectedVoucherCode.value = bestVoucher.value.code;
    }
  } catch (error) {
    bestVoucher.value = null;
  }
};

const fetchAvailableVouchers = async () => {
  try {
    const { data } = await voucherApi.getPublicVouchers();
    availableVouchers.value = Array.isArray(data) ? data : [];
  } catch (_error) {
    availableVouchers.value = [];
  }
};

const handleVoucherChange = (value) => {
  selectedVoucherCode.value = String(value || '').trim().toUpperCase();
};

onMounted(async () => {
  await storeSettingsStore.fetchPublicSettings();
  await wishlistStore.ensureLoaded();
  await Promise.all([
    cartStore.fetchCart(),
    fetchCatalogProducts(),
    fetchAvailableVouchers()
  ]);
  await fetchBestVoucher();
});

watch(
  () => cartStore.totalPrice,
  async () => {
    await Promise.all([fetchBestVoucher(), fetchCatalogProducts(), fetchAvailableVouchers()]);
    if (selectedVoucherCode.value && !eligibleVouchers.value.some((voucher) => voucher.code === selectedVoucherCode.value)) {
      selectedVoucherCode.value = bestVoucher.value?.code || '';
    }
    if (!selectedVoucherCode.value && bestVoucher.value?.code) {
      selectedVoucherCode.value = bestVoucher.value.code;
    }
  }
);
</script>

<style scoped lang="scss">
.cart-page-container {
  max-width: 1300px;
  margin: 0 auto;
  padding: 60px 20px;
}

.cart-layout {
  display: grid;
  grid-template-columns: 1fr 420px;
  gap: 50px;
}

.cart-header {
  margin-bottom: 40px;

  .cart-title {
    font-size: 40px;
    font-weight: 950;
    margin-bottom: 10px;
    letter-spacing: -1px;

    span {
      font-size: 18px;
      font-weight: 500;
      color: #666;
      margin-left: 10px;
      letter-spacing: 0;
    }
  }

  .cart-subtitle {
    font-size: 14px;
    color: #111;
  }
}

.cart-items-list {
  display: flex;
  flex-direction: column;
  gap: 1px;
  margin-bottom: 80px;
}

.cart-item-card {
  display: flex;
  padding: 0;
  border: 1px solid #ebedee;
  height: 220px;

  .item-image-box {
    width: 220px;
    height: 100%;
    background: #f5f5f5;

    .item-img {
      width: 100%;
      height: 100%;
    }
  }

  .item-content {
    flex: 1;
    padding: 20px 25px;
    display: flex;
    flex-direction: column;

    .item-top {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: 6px;

      .item-name {
        font-size: 15px;
        font-weight: 800;
        color: #000;
        text-decoration: none;
        max-width: 80%;
        line-height: 1.3;

        &:hover { text-decoration: underline; }
      }

      .item-utils {
        display: flex;
        gap: 15px;

        .util-icon {
          font-size: 22px;
          cursor: pointer;
          transition: color 0.2s;
          &:hover { color: #888; }
        }

        .wishlist-icon-btn {
          border: none;
          background: transparent;
          padding: 0;
          line-height: 1;
          cursor: pointer;
          display: inline-flex;
          align-items: center;
          justify-content: center;
          color: #111;
          transition: color 0.2s, transform 0.2s;

          &:hover {
            color: #d33;
            transform: scale(1.05);
          }

          &.active {
            color: #d33;
          }
        }
      }
    }

    .item-meta {
      font-size: 13px;
      color: #666;
      margin-bottom: 4px;
    }

    .item-bottom {
      margin-top: auto;
      display: flex;
      justify-content: space-between;
      align-items: center;

      .qty-select {
        width: 100px;
        :deep(.el-input__wrapper) {
          border-radius: 0;
          box-shadow: 0 0 0 1px #ebedee inset !important;
        }
      }

      .item-price {
        font-size: 18px;
        font-weight: 800;
      }
    }
  }
}

/* SIDEBAR SUMMARY */
.cart-sidebar {
  .summary-box {
    position: sticky;
    top: 100px;
  }

  .summary-title {
    font-size: 32px;
    font-weight: 950;
    margin-bottom: 30px;
    letter-spacing: -0.5px;
  }

  .sum-row {
    display: flex;
    justify-content: space-between;
    margin-bottom: 18px;
    font-size: 15px;
  }

  .sum-total {
    display: flex;
    justify-content: space-between;
    margin-top: 30px;
    padding-top: 25px;
    border-top: 1px solid #ebedee;

    .total-label { font-size: 18px; font-weight: 900; }
    .total-value { font-size: 18px; font-weight: 900; }
  }

  .free-ship-progress {
    margin: 6px 0 18px;
    padding: 10px 12px;
    border: 1px solid #ebedee;
    background: #f8fafc;

    .progress-head {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 8px;
      font-size: 12px;
      color: #334155;
      font-weight: 700;
    }

    p {
      margin: 8px 0 0;
      font-size: 12px;
      color: #475569;
    }
  }

  .tax-note {
    font-size: 12px;
    color: #666;
    margin-bottom: 35px;
  }

  .promo-link {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 14px;

    a {
      font-size: 14px;
      color: #000;
      text-decoration: underline;
      font-weight: 700;
      text-underline-offset: 3px;
    }

    .el-icon { font-size: 18px; }
  }

  .checkout-select {
    width: 100%;
    margin-bottom: 12px;
  }

  .checkout-form {
    margin-bottom: 16px;

    .checkout-label {
      display: block;
      margin-bottom: 6px;
      font-size: 12px;
      font-weight: 700;
      color: #333;
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }

    .checkout-select {
      width: 100%;
      margin-bottom: 12px;
    }

    .selected-payment-hint {
      margin-top: -4px;
      margin-bottom: 14px;
      padding: 10px 12px;
      display: flex;
      align-items: center;
      gap: 10px;
      border: 1px solid #e7d9ff;
      background: #faf6ff;
      color: #1f1f1f;
      font-size: 13px;
      font-weight: 600;
    }

    .selected-payment-logo {
      width: 28px;
      height: 28px;
      object-fit: contain;
      flex: 0 0 28px;
    }

    :deep(.el-textarea__inner) {
      border-radius: 0;
      box-shadow: none;
      border-color: #dcdfe6;
      font-family: inherit;
    }
  }

  .checkout-submit-btn {
    width: 100%;
    height: 60px;
    background: #000;
    border-color: #000;
    border-radius: 0;
    font-weight: 900;
    font-size: 13px;
    letter-spacing: 1px;
    display: flex;
    justify-content: space-between;
    padding: 0 25px;
    margin-bottom: 50px;

    &:hover { background: #333; border-color: #333; }
  }

  .payment-trust {
    .trust-label {
      font-size: 12px;
      font-weight: 800;
      margin-bottom: 18px;
      color: #111;
    }

    .trust-logos {
      display: flex;
      gap: 12px;
      flex-wrap: wrap;

      img {
        height: 24px;
        object-fit: contain;
      }
    }
  }
}

/* RECOMMENDATIONS */
.cart-recommendations {
  .section-heading {
    font-size: 36px;
    font-weight: 950;
    margin-bottom: 40px;
  }

  .rec-row {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 15px;
  }

  .rec-item {
    text-decoration: none;
    color: inherit;

    .rec-img {
      width: 100%;
      height: 280px;
      margin-bottom: 15px;
      display: block;
    }

    .rec-img-placeholder {
      height: 280px;
      background: #f5f5f5;
    }

    .rec-price { font-weight: 800; font-size: 15px; }
    .rec-name { font-size: 14px; margin-top: 5px; color: #333; }
  }

  .rec-empty {
    font-size: 14px;
    color: #777;
  }
}

/* EMPTY STATE */
.empty-layout {
  text-align: center;
  padding: 120px 20px;

  .empty-title {
    font-size: 36px;
    font-weight: 950;
    margin-bottom: 25px;
  }

  .empty-msg {
    margin-bottom: 40px;
    color: #444;
    font-size: 16px;
  }

  .start-shopping-btn {
    height: 55px;
    padding: 0 50px;
    background: #000;
    border-radius: 0;
    font-weight: 900;
    letter-spacing: 1px;
  }
}

@media (max-width: 1024px) {
  .cart-page-container {
    padding: 32px 16px;
  }

  .cart-layout {
    grid-template-columns: 1fr;
  }

  .cart-sidebar {
    .summary-box {
      position: static;
    }
  }

  .rec-row {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 768px) {
  .cart-page-container {
    padding: 24px 12px;
  }

  .cart-header {
    margin-bottom: 24px;

    .cart-title {
      font-size: 28px;

      span {
        display: block;
        margin-left: 0;
        margin-top: 6px;
      }
    }
  }

  .cart-items-list {
    margin-bottom: 40px;
  }

  .cart-item-card {
    flex-direction: column;
    height: auto;

    .item-image-box {
      width: 100%;
      height: 260px;
    }

    .item-content {
      padding: 14px;

      .item-top .item-name {
        max-width: 100%;
      }

      .item-bottom {
        gap: 12px;
      }
    }
  }

  .cart-sidebar .summary-title {
    font-size: 24px;
    margin-bottom: 20px;
  }

  .cart-sidebar .checkout-submit-btn {
    margin-bottom: 30px;
  }

  .cart-recommendations .section-heading {
    font-size: 24px;
    margin-bottom: 20px;
  }

  .cart-recommendations .rec-row {
    grid-template-columns: 1fr;
  }

  .cart-recommendations .rec-item .rec-img,
  .cart-recommendations .rec-item .rec-img-placeholder {
    height: 220px;
  }

  .empty-layout {
    padding: 70px 12px;

    .empty-title {
      font-size: 26px;
    }

    .empty-msg {
      font-size: 14px;
    }
  }
}
</style>

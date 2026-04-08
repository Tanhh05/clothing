<template>
  <div class="cart-page-container">
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
                    <el-icon class="util-icon" @click="handleRemove(item.id)"><Delete /></el-icon>
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
              <span>Giao hàng</span>
              <span>{{ shippingFee === 0 ? "Miễn phí" : formatCurrency(shippingFee) }}</span>
            </div>
            
            <div class="sum-total">
              <span class="total-label">Tổng</span>
              <span class="total-value">{{ formatCurrency(grandTotal) }}</span>
            </div>
            <p class="tax-note">(Đã bao gồm thuế)</p>
            <p class="tax-note" v-if="shippingFee > 0">
              Miễn phí ship từ {{ formatCurrency(storeSettingsStore.freeShippingThreshold) }}
            </p>
            <p class="tax-note" v-if="shippingFee > 0 && selectedProvinceName">
              Khu vực giao hàng: {{ isNearProvince(selectedProvinceName) ? 'Gần' : 'Xa' }}
            </p>
            <p class="tax-note">{{ storeSettingsStore.shippingPolicy }}</p>

            <div class="promo-link">
              <el-icon><Ticket /></el-icon>
              <a href="#">Sử dụng mã khuyến mãi</a>
            </div>

            <div class="checkout-form">
              <label class="checkout-label">Phương thức thanh toán</label>
              <el-select v-model="paymentMethod" class="checkout-select">
                <el-option
                  v-for="option in paymentMethodOptions"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                />
              </el-select>
              <div v-if="paymentMethod === 'MOMO'" class="selected-payment-hint">
                <img :src="momoLogo" alt="MoMo" class="selected-payment-logo">
                <span>Thanh toán qua ví điện tử MoMo</span>
              </div>

              <label class="checkout-label">Tỉnh / Thành</label>
              <el-select
                v-model="selectedProvinceId"
                class="checkout-select"
                placeholder="Chọn tỉnh / thành"
                :loading="addressLoading.provinces"
                filterable
              >
                <el-option
                  v-for="p in provinces"
                  :key="p.id"
                  :label="p.name"
                  :value="p.id"
                />
              </el-select>

              <label class="checkout-label">Quận / Huyện</label>
              <el-select
                v-model="selectedDistrictId"
                class="checkout-select"
                placeholder="Chọn quận / huyện"
                :disabled="!selectedProvinceId"
                :loading="addressLoading.districts"
                filterable
              >
                <el-option
                  v-for="d in districts"
                  :key="d.id"
                  :label="d.name"
                  :value="d.id"
                />
              </el-select>

              <label class="checkout-label">Phường / Xã</label>
              <el-select
                v-model="selectedWardId"
                class="checkout-select"
                placeholder="Chọn phường / xã"
                :disabled="!selectedDistrictId"
                :loading="addressLoading.wards"
                filterable
              >
                <el-option
                  v-for="w in wards"
                  :key="w.id"
                  :label="w.name"
                  :value="w.id"
                />
              </el-select>

              <label class="checkout-label">Số nhà, tên đường</label>
              <el-input
                v-model="shippingAddressLine"
                type="textarea"
                :rows="2"
                placeholder="Ví dụ: 123 Nguyễn Trãi"
              />
            </div>

            <el-button type="primary" class="checkout-submit-btn" @click="submitCheckout" :loading="submittingOrder">
              THANH TOÁN <el-icon class="el-icon--right"><Right /></el-icon>
            </el-button>

            <div class="payment-trust">
              <p class="trust-label">PHƯƠNG THỨC THANH TOÁN ĐƯỢC CHẤP NHẬN</p>
              <div class="trust-logos">
                <img
                  v-for="method in paymentMethods"
                  :key="method.alt"
                  :src="method.src"
                  :alt="method.alt"
                  :title="method.alt"
                >
              </div>
            </div>
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
import { ElMessage, ElMessageBox } from 'element-plus';
import api from '@/services/api';
import { useWishlistStore } from '@/store/wishlistStore';
import { useAuthStore } from '@/store/authStore';
import { orderApi } from '@/modules/order/api/orderApi';
import { addressApi } from '@/modules/address/api/addressApi';
import { useStoreSettingsStore } from '@/store/storeSettingsStore';
import visaLogo from '@/assets/payments/visa.svg';
import mastercardLogo from '@/assets/payments/mastercard.svg';
import jcbLogo from '@/assets/payments/jcb.svg';
import momoLogo from '@/assets/payments/momo.svg';

const cartStore = useCartStore();
const wishlistStore = useWishlistStore();
const authStore = useAuthStore();
const storeSettingsStore = useStoreSettingsStore();
const router = useRouter();
const productCatalog = ref([]);
const paymentMethod = ref('COD');
const shippingAddressLine = ref('');
const submittingOrder = ref(false);
const provinces = ref([]);
const districts = ref([]);
const wards = ref([]);
const selectedProvinceId = ref('');
const selectedDistrictId = ref('');
const selectedWardId = ref('');
const addressLoading = ref({
  provinces: false,
  districts: false,
  wards: false
});

const paymentMethods = [
  { alt: 'Visa', src: visaLogo },
  { alt: 'Mastercard', src: mastercardLogo },
  { alt: 'JCB', src: jcbLogo },
  { alt: 'MoMo', src: momoLogo }
];

const paymentMethodOptions = computed(() => {
  const options = [];
  if (storeSettingsStore.enableCOD) {
    options.push({ label: "Thanh toán khi nhận hàng (COD)", value: "COD" });
  }
  if (storeSettingsStore.enableMomo) {
    options.push({ label: "Ví điện tử MoMo", value: "MOMO" });
  }
  return options;
});

const FAR_DISTANCE_SURCHARGE = 20000;
const NEAR_PROVINCES = new Set([
  'ho chi minh',
  'binh duong',
  'dong nai',
  'long an',
  'tay ninh',
  'ba ria vung tau'
]);

const normalizeProvince = (province) => {
  if (!province) return '';
  let normalized = province
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .replace(/[^a-z0-9\s]/g, ' ')
    .replace(/\s+/g, ' ')
    .trim();
  if (normalized.startsWith('thanh pho ')) normalized = normalized.slice('thanh pho '.length).trim();
  else if (normalized.startsWith('tp ')) normalized = normalized.slice('tp '.length).trim();
  else if (normalized.startsWith('tinh ')) normalized = normalized.slice('tinh '.length).trim();
  return normalized;
};

const isNearProvince = (province) => NEAR_PROVINCES.has(normalizeProvince(province));

const shippingFee = computed(() => {
  const baseFee = storeSettingsStore.defaultShippingFee;
  const threshold = storeSettingsStore.freeShippingThreshold;
  if (!Number.isFinite(baseFee) || baseFee < 0) return 0;
  if (!Number.isFinite(threshold) || threshold <= 0) {
    if (!selectedProvinceName.value) return baseFee;
    return isNearProvince(selectedProvinceName.value) ? baseFee : baseFee + FAR_DISTANCE_SURCHARGE;
  }
  if (cartStore.totalPrice >= threshold) return 0;
  if (!selectedProvinceName.value) return baseFee;
  return isNearProvince(selectedProvinceName.value) ? baseFee : baseFee + FAR_DISTANCE_SURCHARGE;
});

const grandTotal = computed(() => cartStore.totalPrice + shippingFee.value);

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
  const cartProductIds = new Set(displayCartItems.value.map(item => item.productId).filter(Boolean));
  const cartCategoryIds = new Set(
    displayCartItems.value
      .map(item => {
        const matched = variantLookup.value.get(item.variantId);
        return matched?.product?.categoryId;
      })
      .filter(Boolean)
  );

  const scored = productCatalog.value
    .filter(product => !cartProductIds.has(product.id))
    .map(product => {
      let score = 0;
      if (cartCategoryIds.has(product.categoryId)) {
        score += 10;
      }
      if (String(product.status || '').toUpperCase() === 'ACTIVE') {
        score += 1;
      }
      return { product, score };
    })
    .sort((a, b) => b.score - a.score || b.product.id - a.product.id)
    .slice(0, 4)
    .map(({ product }) => ({
      id: product.id,
      slug: product.slug,
      name: product.name,
      price: product.variants?.[0]?.price ?? 0,
      image: product.images?.[0]?.url ?? ''
    }));

  return scored;
});

const formatCurrency = (value) => {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value);
};

const handleUpdateQuantity = async (itemId, qty) => {
  await cartStore.updateQuantity(itemId, qty);
};

const handleRemove = async (itemId) => {
  try {
    await ElMessageBox.confirm('Bạn có chắc chắn muốn xóa sản phẩm này khỏi giỏ hàng?', 'Xóa sản phẩm', {
      confirmButtonText: 'Xóa',
      cancelButtonText: 'Hủy',
      type: 'warning'
    });
    await cartStore.removeItem(itemId);
  } catch (e) {
    // cancelled
  }
};

const selectedProvinceName = computed(() => provinces.value.find((p) => p.id === selectedProvinceId.value)?.name || '');
const selectedDistrictName = computed(() => districts.value.find((d) => d.id === selectedDistrictId.value)?.name || '');
const selectedWardName = computed(() => wards.value.find((w) => w.id === selectedWardId.value)?.name || '');

const fetchProvinces = async () => {
  try {
    addressLoading.value.provinces = true;
    const { data } = await addressApi.getProvinces();
    provinces.value = Array.isArray(data) ? data : [];
  } catch (error) {
    ElMessage.error('Không thể tải danh sách tỉnh/thành');
  } finally {
    addressLoading.value.provinces = false;
  }
};

const fetchDistricts = async (provinceId) => {
  if (!provinceId) {
    districts.value = [];
    return;
  }
  try {
    addressLoading.value.districts = true;
    const { data } = await addressApi.getDistricts(provinceId);
    districts.value = Array.isArray(data) ? data : [];
  } catch (error) {
    ElMessage.error('Không thể tải danh sách quận/huyện');
    districts.value = [];
  } finally {
    addressLoading.value.districts = false;
  }
};

const fetchWards = async (districtId) => {
  if (!districtId) {
    wards.value = [];
    return;
  }
  try {
    addressLoading.value.wards = true;
    const { data } = await addressApi.getWards(districtId);
    wards.value = Array.isArray(data) ? data : [];
  } catch (error) {
    ElMessage.error('Không thể tải danh sách phường/xã');
    wards.value = [];
  } finally {
    addressLoading.value.wards = false;
  }
};

const submitCheckout = async () => {
  if (!authStore.isAuthenticated) {
    ElMessage.warning('Vui lòng đăng nhập để thanh toán');
    router.push('/auth/login');
    return;
  }

  if (!selectedProvinceId.value || !selectedDistrictId.value || !selectedWardId.value) {
    ElMessage.warning('Vui lòng chọn đầy đủ tỉnh/huyện/xã');
    return;
  }

  if (!shippingAddressLine.value.trim()) {
    ElMessage.warning('Vui lòng nhập số nhà, tên đường');
    return;
  }
  if (!paymentMethod.value) {
    ElMessage.warning('Hiện chưa có phương thức thanh toán khả dụng');
    return;
  }

  try {
    submittingOrder.value = true;
    const fullAddress = [
      shippingAddressLine.value.trim(),
      selectedWardName.value,
      selectedDistrictName.value,
      selectedProvinceName.value
    ].filter(Boolean).join(', ');

    const payload = {
      paymentMethod: paymentMethod.value,
      address: fullAddress,
      province: selectedProvinceName.value
    };
    const { data } = await orderApi.createOrder(payload);
    ElMessage.success(`Đặt hàng thành công. Mã đơn #${data?.id ?? ''}`);
    if (paymentMethod.value === 'MOMO' && data?.paymentUrl) {
      window.location.href = data.paymentUrl;
      return;
    }
    if (paymentMethod.value === 'MOMO' && !data?.paymentUrl) {
      ElMessage.error('Không lấy được link thanh toán MoMo');
      return;
    }
    shippingAddressLine.value = '';
    selectedProvinceId.value = '';
    selectedDistrictId.value = '';
    selectedWardId.value = '';
    districts.value = [];
    wards.value = [];
    await cartStore.fetchCart();
    router.push('/orders');
  } catch (error) {
    ElMessage.error(error.response?.data?.message || 'Không thể tạo đơn hàng');
  } finally {
    submittingOrder.value = false;
  }
};

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
      params: { page: 0, size: 100, sortBy: 'id', direction: 'desc' }
    });
    productCatalog.value = response.data?.content || [];
  } catch (error) {
    console.error('Failed to fetch product catalog for recommendations:', error);
  }
};

onMounted(async () => {
  await storeSettingsStore.fetchPublicSettings();
  if (!paymentMethodOptions.value.some((item) => item.value === paymentMethod.value)) {
    paymentMethod.value = paymentMethodOptions.value[0]?.value || 'COD';
  }
  await wishlistStore.ensureLoaded();
  await Promise.all([
    cartStore.fetchCart(),
    fetchCatalogProducts(),
    fetchProvinces()
  ]);
});

watch(selectedProvinceId, async (value) => {
  selectedDistrictId.value = '';
  selectedWardId.value = '';
  wards.value = [];
  await fetchDistricts(value);
});

watch(selectedDistrictId, async (value) => {
  selectedWardId.value = '';
  await fetchWards(value);
});

watch(paymentMethodOptions, (options) => {
  if (!options.some((item) => item.value === paymentMethod.value)) {
    paymentMethod.value = options[0]?.value || '';
  }
});
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

  .tax-note {
    font-size: 12px;
    color: #666;
    margin-bottom: 35px;
  }

  .promo-link {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 40px;
    
    a {
      font-size: 14px;
      color: #000;
      text-decoration: underline;
      font-weight: 700;
      text-underline-offset: 3px;
    }
    
    .el-icon { font-size: 18px; }
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

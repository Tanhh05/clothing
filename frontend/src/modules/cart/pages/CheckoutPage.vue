<template>
  <div class="checkout-page">
    <div v-if="cartStore.isEmpty" class="empty-layout">
      <el-empty description="Giỏ hàng đang trống">
        <router-link to="/cart">
          <el-button type="primary">Về giỏ hàng</el-button>
        </router-link>
      </el-empty>
    </div>

    <template v-else>


      <el-row :gutter="20" class="checkout-layout">
        <el-col :xs="24" :lg="15">
          <el-space direction="vertical" :size="16" fill class="left-stack">

            <el-card shadow="hover">
              <template #header>
                <div class="card-title">
                  <span>Địa chỉ giao hàng</span>
                  <el-tag v-if="savedAddresses.length" type="warning" effect="plain">Đã lưu {{ savedAddresses.length }} địa chỉ</el-tag>
                </div>
              </template>

              <el-form label-position="top" class="checkout-form">
                <el-form-item label="Địa chỉ đã lưu">
                  <el-select
                    v-model="selectedSavedAddressId"
                    placeholder="Chọn địa chỉ đã lưu"
                    clearable
                    filterable
                    @change="applySavedAddress"
                  >
                    <el-option
                      v-for="address in savedAddresses"
                      :key="address.id"
                      :label="`${address.recipientName} - ${address.phone} - ${address.addressLine}`"
                      :value="address.id"
                    />
                  </el-select>
                </el-form-item>

                <el-row :gutter="12">
                  <el-col :xs="24">
                    <el-form-item label="Người nhận">
                      <el-input v-model="firstName" placeholder="Nhập tên người nhận" />
                    </el-form-item>
                  </el-col>
                </el-row>

                <el-form-item label="Số nhà / Tên đường">
                  <el-input v-model="shippingAddressLine" placeholder="Ví dụ: 123 Nguyễn Trãi" />
                </el-form-item>

                <el-row :gutter="12">
                  <el-col :xs="24" :sm="12">
                    <el-form-item label="Tỉnh / Thành phố">
                      <el-select
                        v-model="selectedProvinceId"
                        placeholder="Chọn tỉnh/thành"
                        :loading="addressLoading.provinces"
                        filterable
                      >
                        <el-option v-for="p in provinces" :key="p.id" :label="p.name" :value="p.id" />
                      </el-select>
                    </el-form-item>
                  </el-col>
                  <el-col :xs="24" :sm="12">
                    <el-form-item label="Quận / Huyện">
                      <el-select
                        v-model="selectedDistrictId"
                        placeholder="Chọn quận/huyện"
                        :disabled="!selectedProvinceId"
                        :loading="addressLoading.districts"
                        filterable
                      >
                        <el-option v-for="d in districts" :key="d.id" :label="d.name" :value="d.id" />
                      </el-select>
                    </el-form-item>
                  </el-col>
                </el-row>

                <el-form-item label="Phường / Xã">
                  <el-select
                    v-model="selectedWardId"
                    placeholder="Chọn phường/xã"
                    :disabled="!selectedDistrictId"
                    :loading="addressLoading.wards"
                    filterable
                  >
                    <el-option v-for="w in wards" :key="w.id" :label="w.name" :value="w.id" />
                  </el-select>
                </el-form-item>

                <el-form-item label="Số điện thoại">
                  <el-input v-model="phoneNumber" placeholder="Nhập số điện thoại" />
                </el-form-item>
              </el-form>
            </el-card>

            <el-card shadow="hover">
              <template #header>
                <div class="card-title">
                  <span>Phương thức thanh toán</span>
                </div>
              </template>

              <div class="payment-list">
                <button
                  v-for="option in paymentMethodOptions"
                  :key="option.key"
                  type="button"
                  class="payment-item"
                  :class="{
                    active: paymentMethod === option.value,
                    disabled: option.disabled
                  }"
                  :disabled="option.disabled"
                  @click="selectPaymentMethod(option)"
                >
                  <span class="payment-check">
                    <span v-if="paymentMethod === option.value && !option.disabled" class="tick">✓</span>
                  </span>
                  <span class="payment-content">
                    <span class="payment-title">{{ option.label }}</span>
                    <span v-if="option.description" class="payment-desc">{{ option.description }}</span>
                    <span v-if="option.logos?.length" class="payment-logos">
                      <img
                        v-for="logo in option.logos"
                        :key="logo.src"
                        :src="logo.src"
                        :alt="logo.alt"
                        class="payment-logo"
                      >
                    </span>
                  </span>
                </button>
              </div>

              <el-alert
                v-if="!hasAvailablePaymentMethod"
                title="Hiện chưa có phương thức thanh toán khả dụng"
                type="warning"
                :closable="false"
                show-icon
                class="mt-12"
              />

              <div v-if="paymentMethod === 'MOMO'" class="selected-payment-hint">
                <img :src="momoLogo" alt="MoMo" class="selected-payment-logo">
                <span>Bạn sẽ được chuyển sang ví MoMo sau khi đặt hàng.</span>
              </div>
            </el-card>
          </el-space>
        </el-col>

        <el-col :xs="24" :lg="9">
          <div class="sticky-side">
            <el-card shadow="hover" class="order-card">
              <template #header>
                <div class="card-title">
                  <span>Đơn hàng của bạn</span>
                  <el-button link type="primary" @click="router.push('/cart')">Chỉnh sửa</el-button>
                </div>
              </template>

              <div class="sum-row">
                <span>{{ cartStore.totalItems }} sản phẩm</span>
                <strong>{{ formatCurrency(cartStore.totalPrice) }}</strong>
              </div>
              <div class="sum-row">
                <span>Phí vận chuyển</span>
                <strong>{{ formatCurrency(shippingFee) }}</strong>
              </div>
              <div class="sum-row">
                <span>Giảm voucher</span>
                <strong class="discount-text">- {{ formatCurrency(appliedVoucherDiscount) }}</strong>
              </div>
              <div class="sum-row total">
                <span>Tổng thanh toán</span>
                <strong>{{ formatCurrency(grandTotalAfterDiscount) }}</strong>
              </div>

              <p v-if="autoAppliedVoucherCode" class="small-note">Đang tự áp mã tốt nhất: <strong>{{ autoAppliedVoucherCode }}</strong></p>

              <div class="voucher-box">
                <el-select
                  v-model="selectedVoucherCode"
                  clearable
                  filterable
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

              </div>

              <el-alert
                v-if="!eligibleVouchers.length"
                title="Hiện chưa có voucher phù hợp với giỏ hàng này"
                type="info"
                :closable="false"
                show-icon
                class="mt-12"
              />

              <div
                v-for="(item, index) in previewItems"
                :key="`${item.productId || item.productName || 'item'}-${item.size || ''}-${item.color || ''}-${index}`"
                class="mini-item"
              >
                <el-image :src="item.productImage" fit="cover" class="mini-image" />
                <div>
                  <p class="mini-name">{{ item.productName }}</p>
                  <p class="mini-price">{{ formatCurrency(item.price || 0) }}</p>
                  <p class="mini-attr">Kích cỡ: {{ item.size || "-" }} / Số lượng: {{ item.quantity || 1 }}</p>
                  <p class="mini-attr">Màu: {{ item.color || "-" }}</p>
                </div>
              </div>

              <el-button
                type="primary"
                size="large"
                class="submit-btn"
                @click="submitCheckout"
                :loading="submittingOrder"
              >
                Hoàn tất đặt hàng
              </el-button>
            </el-card>
          </div>
        </el-col>
      </el-row>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { useCartStore } from '@/store/cartStore';
import { useAuthStore } from '@/store/authStore';
import { useStoreSettingsStore } from '@/store/storeSettingsStore';
import { orderApi } from '@/modules/order/api/orderApi';
import { addressApi } from '@/modules/address/api/addressApi';
import { userAddressApi } from '@/modules/address/api/userAddressApi';
import { voucherApi } from '@/modules/voucher/api/voucherApi';
import momoLogo from '@/assets/payments/momo.svg';
import visaLogo from '@/assets/payments/visa.svg';
import masterCardLogo from '@/assets/payments/mastercard.svg';
import jcbLogo from '@/assets/payments/jcb.svg';

const router = useRouter();
const cartStore = useCartStore();
const authStore = useAuthStore();
const storeSettingsStore = useStoreSettingsStore();

const savedAddresses = ref([]);
const selectedSavedAddressId = ref(null);
const bestVoucher = ref(null);
const availableVouchers = ref([]);
const voucherClearedByUser = ref(false);
const applyingSavedAddress = ref(false);
const paymentMethod = ref('COD');
const firstName = ref('');
const shippingAddressLine = ref('');
const phoneNumber = ref('');
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

const FAR_DISTANCE_SURCHARGE = 20000;
const NEAR_PROVINCES = new Set(['ho chi minh', 'binh duong', 'dong nai', 'long an', 'tay ninh', 'ba ria vung tau']);

const paymentMethodOptions = computed(() => {
  return [
    {
      key: 'ATM_CARD',
      value: 'ATM_CARD',
      label: 'Thanh toán bằng ATM/Thẻ nội địa',
      disabled: true
    },
    {
      key: 'PAYME',
      value: 'PAYME',
      label: 'Thanh toán qua cổng Payme',
      description: 'PayME',
      disabled: true
    },
    {
      key: 'PAYOO',
      value: 'PAYOO',
      label: 'Thanh toán qua ví Payoo',
      logos: [
        { src: visaLogo, alt: 'Visa' },
        { src: masterCardLogo, alt: 'Mastercard' },
        { src: jcbLogo, alt: 'JCB' }
      ],
      disabled: true
    },
    {
      key: 'MOMO',
      value: 'MOMO',
      label: 'Thanh toán qua ví MoMo',
      logos: [{ src: momoLogo, alt: 'MoMo' }],
      disabled: !storeSettingsStore.enableMomo
    },
    {
      key: 'VNPAY',
      value: 'VNPAY',
      label: 'Thanh toán qua cổng VNPAY',
      description: 'VNPAY',
      disabled: true
    },
    {
      key: 'COD',
      value: 'COD',
      label: 'Thanh toán khi giao hàng (COD)',
      disabled: !storeSettingsStore.enableCOD
    }
  ];
});
const hasAvailablePaymentMethod = computed(() => paymentMethodOptions.value.some((option) => !option.disabled));

const selectedProvinceName = computed(() => provinces.value.find((p) => p.id === selectedProvinceId.value)?.name || '');
const selectedDistrictName = computed(() => districts.value.find((d) => d.id === selectedDistrictId.value)?.name || '');
const selectedWardName = computed(() => wards.value.find((w) => w.id === selectedWardId.value)?.name || '');

const normalizeProvince = (province) => {
  if (!province) return '';
  let normalized = province.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().replace(/[^a-z0-9\s]/g, ' ').replace(/\s+/g, ' ').trim();
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

const eligibleVouchers = computed(() => {
  const subTotal = Number(cartStore.totalPrice || 0);
  return availableVouchers.value.filter((voucher) => {
    if (String(voucher?.status || '').toUpperCase() !== 'ACTIVE') return false;
    return Number(voucher?.minOrderValue || 0) <= subTotal;
  });
});

const selectedVoucherCode = computed({
  get: () => cartStore.preferredVoucherCode || '',
  set: (value) => cartStore.setPreferredVoucherCode(value)
});
const autoAppliedVoucherCode = computed(() => (bestVoucher.value?.autoApplied ? bestVoucher.value?.code || '' : ''));
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
  if (voucherClearedByUser.value && !selectedVoucherCode.value) {
    return 0;
  }
  if (selectedVoucherCode.value && bestVoucher.value?.code === selectedVoucherCode.value) {
    return Number(bestVoucher.value?.discountAmount || 0);
  }
  if (selectedVoucher.value) {
    return calculateDiscount(selectedVoucher.value, subTotal);
  }
  if (bestVoucher.value?.code) {
    return Number(bestVoucher.value?.discountAmount || 0);
  }
  return 0;
});
const grandTotalAfterDiscount = computed(() => Math.max(0, grandTotal.value - appliedVoucherDiscount.value));
const appliedVoucherCode = computed(() => {
  if (selectedVoucherCode.value) return selectedVoucherCode.value;
  if (voucherClearedByUser.value) return '';
  return bestVoucher.value?.code || '';
});
const contactText = computed(() => String(authStore.profile?.email || authStore.username || '').trim() || 'Đăng nhập để điền thông tin liên hệ');
const previewItems = computed(() => (Array.isArray(cartStore.items) ? cartStore.items : []));

const formatCurrency = (value) => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value);

const fetchProvinces = async () => {
  try {
    addressLoading.value.provinces = true;
    const { data } = await addressApi.getProvinces();
    provinces.value = Array.isArray(data) ? data : [];
  } catch (_error) {
    ElMessage.error('Không thể tải danh sách tỉnh/thành');
  } finally {
    addressLoading.value.provinces = false;
  }
};

const fetchDistricts = async (provinceId) => {
  if (!provinceId) return (districts.value = []);
  try {
    addressLoading.value.districts = true;
    const { data } = await addressApi.getDistricts(provinceId);
    districts.value = Array.isArray(data) ? data : [];
  } catch (_error) {
    districts.value = [];
    ElMessage.error('Không thể tải danh sách quận/huyện');
  } finally {
    addressLoading.value.districts = false;
  }
};

const fetchWards = async (districtId) => {
  if (!districtId) return (wards.value = []);
  try {
    addressLoading.value.wards = true;
    const { data } = await addressApi.getWards(districtId);
    wards.value = Array.isArray(data) ? data : [];
  } catch (_error) {
    wards.value = [];
    ElMessage.error('Không thể tải danh sách phường/xã');
  } finally {
    addressLoading.value.wards = false;
  }
};

const fetchSavedAddresses = async () => {
  if (!authStore.isAuthenticated) return;
  try {
    const { data } = await userAddressApi.getMyAddresses();
    savedAddresses.value = Array.isArray(data) ? data : [];
    const defaultAddress = savedAddresses.value.find((item) => item.isDefault) || savedAddresses.value[0];
    if (defaultAddress) {
      selectedSavedAddressId.value = defaultAddress.id;
      await applySavedAddress(defaultAddress.id);
    }
  } catch (_error) {
    savedAddresses.value = [];
  }
};

const applySavedAddress = async (id) => {
  const selected = savedAddresses.value.find((item) => item.id === id);
  if (!selected) return;

  applyingSavedAddress.value = true;
  shippingAddressLine.value = selected.addressLine || '';
  firstName.value = selected.recipientName || firstName.value;
  phoneNumber.value = selected.phone || phoneNumber.value;

  const province = provinces.value.find((item) => item.name === selected.province);
  if (!province) {
    applyingSavedAddress.value = false;
    return;
  }

  selectedProvinceId.value = province.id;
  await fetchDistricts(province.id);

  const district = districts.value.find((item) => item.name === selected.district);
  if (!district) {
    selectedDistrictId.value = '';
    selectedWardId.value = '';
    wards.value = [];
    applyingSavedAddress.value = false;
    return;
  }

  selectedDistrictId.value = district.id;
  await fetchWards(district.id);

  const ward = wards.value.find((item) => item.name === selected.ward);
  selectedWardId.value = ward?.id || '';
  applyingSavedAddress.value = false;
};

const fetchBestVoucher = async () => {
  try {
    const { data } = await voucherApi.getBestVoucher(cartStore.totalPrice || 0);
    bestVoucher.value = data || null;
    if (!selectedVoucherCode.value && !voucherClearedByUser.value && bestVoucher.value?.code) {
      selectedVoucherCode.value = bestVoucher.value.code;
    }
  } catch (_error) {
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

const selectPaymentMethod = (option) => {
  if (!option || option.disabled) return;
  paymentMethod.value = option.value;
};

const handleVoucherChange = (value) => {
  selectedVoucherCode.value = String(value || '').trim().toUpperCase();
  voucherClearedByUser.value = !selectedVoucherCode.value;
};

const submitCheckout = async () => {
  if (!authStore.isAuthenticated) {
    ElMessage.warning('Vui lòng đăng nhập để thanh toán');
    router.push('/auth/login');
    return;
  }
  if (!selectedProvinceId.value || !selectedDistrictId.value || !selectedWardId.value) return ElMessage.warning('Vui lòng chọn đầy đủ tỉnh/huyện/xã');
  if (!shippingAddressLine.value.trim()) return ElMessage.warning('Vui lòng nhập số nhà, tên đường');
  if (!paymentMethod.value) return ElMessage.warning('Hiện chưa có phương thức thanh toán khả dụng');

  try {
    submittingOrder.value = true;
    const fullAddress = [shippingAddressLine.value.trim(), selectedWardName.value, selectedDistrictName.value, selectedProvinceName.value]
      .filter(Boolean)
      .join(', ');

    const payload = {
      paymentMethod: paymentMethod.value,
      address: fullAddress,
      province: selectedProvinceName.value,
      voucherCode: appliedVoucherCode.value || undefined
    };

    const { data } = await orderApi.createOrder(payload);
    ElMessage.success(`Đặt hàng thành công. Mã đơn #${data?.id ?? ''}`);
    if (paymentMethod.value === 'MOMO' && data?.paymentUrl) {
      window.location.href = data.paymentUrl;
      return;
    }
    await cartStore.fetchCart();
    router.push('/orders');
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || 'Không thể tạo đơn hàng');
  } finally {
    submittingOrder.value = false;
  }
};

onMounted(async () => {
  await authStore.fetchProfile().catch(() => null);
  await storeSettingsStore.fetchPublicSettings();
  const firstAvailablePayment = paymentMethodOptions.value.find((item) => !item.disabled);
  if (!paymentMethodOptions.value.some((item) => !item.disabled && item.value === paymentMethod.value)) {
    paymentMethod.value = firstAvailablePayment?.value || '';
  }

  await cartStore.fetchCart();
  await fetchProvinces();
  await fetchSavedAddresses();
  await fetchAvailableVouchers();
  await fetchBestVoucher();
});

watch(selectedProvinceId, async (value) => {
  if (applyingSavedAddress.value) return;
  selectedDistrictId.value = '';
  selectedWardId.value = '';
  wards.value = [];
  await fetchDistricts(value);
});

watch(selectedDistrictId, async (value) => {
  if (applyingSavedAddress.value) return;
  selectedWardId.value = '';
  await fetchWards(value);
});

watch(
  () => cartStore.totalPrice,
  async () => {
    await fetchBestVoucher();
    if (selectedVoucherCode.value && !eligibleVouchers.value.some((voucher) => voucher.code === selectedVoucherCode.value)) {
      selectedVoucherCode.value = voucherClearedByUser.value ? '' : bestVoucher.value?.code || '';
    }
    if (!selectedVoucherCode.value && !voucherClearedByUser.value && bestVoucher.value?.code) {
      selectedVoucherCode.value = bestVoucher.value.code;
    }
  }
);
</script>

<style scoped lang="scss">
.checkout-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 16px 48px;
}

.checkout-page :deep(.el-card) {
  border: none;
  box-shadow: none !important;
  background: transparent;
}

.head-card {
  margin-bottom: 16px;
}

.head-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;

  h1 {
    margin: 0;
    font-size: 30px;
    font-weight: 700;
    color: var(--el-text-color-primary);
  }

  p {
    margin: 6px 0 0;
    color: var(--el-text-color-secondary);
  }
}

.left-stack {
  width: 100%;
}

.card-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  font-weight: 600;
}

.checkout-form :deep(.el-select),
.checkout-form :deep(.el-input) {
  width: 100%;
}

.checkbox-group {
  display: grid;
  gap: 10px;
}

.payment-list {
  display: grid;
  gap: 6px;
}

.payment-item {
  border: none;
  background: transparent;
  width: 100%;
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 8px 0;
  cursor: pointer;
  text-align: left;
  color: var(--el-text-color-primary);
}

.payment-item.disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.payment-check {
  margin-top: 2px;
  width: 18px;
  height: 18px;
  border: 1px solid #bfc5cf;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.payment-item.active .payment-check {
  background: #2b2f36;
  border-color: #2b2f36;
}

.tick {
  font-size: 12px;
  line-height: 1;
  color: #fff;
}

.payment-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.payment-title {
  font-size: 16px;
  font-weight: 700;
  line-height: 1.2;
}

.payment-desc {
  font-size: 14px;
  font-weight: 700;
  color: #2b2f36;
}

.payment-logos {
  display: flex;
  align-items: center;
  gap: 8px;
}

.payment-logo {
  height: 16px;
  width: auto;
  border: none !important;
  box-shadow: none !important;
}

.selected-payment-hint {
  margin-top: 12px;
  padding: 10px 12px;
  border-radius: 10px;
  background: var(--el-color-info-light-9);
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--el-text-color-primary);
}

.selected-payment-logo {
  width: 32px;
  height: 32px;
  object-fit: contain;
  border: none !important;
  box-shadow: none !important;
}

.sticky-side {
  position: sticky;
  top: 16px;
}

.order-card {
  border-radius: 12px;
}

.sum-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  color: var(--el-text-color-regular);
}

.sum-row strong {
  color: var(--el-text-color-primary);
  font-size: 15px;
}

.discount-text {
  color: var(--el-color-danger) !important;
}

.sum-row.total {
  margin-top: 4px;
  padding-top: 12px;
  font-weight: 600;
}

.small-note {
  margin: 0 0 10px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.voucher-box {
  margin-top: 14px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
  align-items: center;
}

.voucher-box :deep(.el-select) {
  width: 100%;
}

.mini-item {
  display: grid;
  grid-template-columns: 86px minmax(0, 1fr);
  gap: 10px;
  margin-top: 14px;
  padding-top: 14px;
}

.mini-image {
  width: 86px;
  height: 86px;
  border-radius: 0 !important;
  background: var(--el-fill-color-light);
  border: none !important;
  box-shadow: none !important;
}

.mini-image :deep(.el-image__wrapper),
.mini-image :deep(.el-image__inner) {
  border: none !important;
  box-shadow: none !important;
}

.mini-name {
  margin: 0 0 6px;
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.mini-price {
  margin: 0 0 4px;
  color: var(--el-color-primary);
  font-weight: 600;
}

.mini-attr {
  margin: 0;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.submit-btn {
  width: 100%;
  margin-top: 16px;
}

.mt-12 {
  margin-top: 12px;
}

.empty-layout {
  padding: 84px 0;
}

@media (max-width: 768px) {
  .checkout-page {
    padding: 24px 12px;
  }

  .head-content {
    flex-direction: column;
    align-items: flex-start;
  }

  .head-content h1 {
    font-size: 24px;
  }

  .voucher-box {
    grid-template-columns: 1fr;
  }

  .sticky-side {
    position: static;
  }
}
</style>

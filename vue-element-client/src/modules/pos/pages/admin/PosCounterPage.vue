<template>
  <section class="pos-page" v-loading="loading">
    <el-container class="pos-shell">
      <el-header class="pos-header">
        <el-row :gutter="10" align="middle">
          <el-col :xs="24" :lg="8">
            <div class="pos-search-box">
              <el-input ref="searchInputRef" v-model="productKeyword" clearable placeholder="Tìm sản phẩm / mã vạch">
                <template #prefix>
                  <el-icon><Search /></el-icon>
                </template>
              </el-input>

              <el-card v-if="productKeyword.trim()" shadow="always" class="pos-search-dropdown">
                <el-empty
                  v-if="!filteredProductVariants.length && !productSearchLoading"
                  description="Không có sản phẩm phù hợp"
                  :image-size="56"
                />

                <div
                  v-else
                  ref="productSearchListRef"
                  class="pos-search-list"
                  @scroll.passive="handleProductSearchScroll"
                >
                  <button
                    v-for="entry in filteredProductVariants"
                    :key="entry.variant.variantId"
                    type="button"
                    class="pos-search-item-compact pos-search-item-btn"
                    @click="addVariantFromSearch(entry.product, entry.variant)"
                  >
                    <el-image :src="entry.product.imageUrl || fallbackImage" fit="cover" class="pos-search-thumb" @error="onVariantImageError" />
                    <div class="pos-search-content">
                      <div class="pos-search-row-1">
                        <strong class="pos-search-name">
                          {{ entry.product.productName }}
                          <span class="pos-variant-chip">{{ String(entry.variant.size || "-").toUpperCase() }}</span>
                        </strong>
                        <strong class="pos-search-price">{{ formatCurrency(entry.variant.price || 0) }}</strong>
                      </div>
                      <div class="pos-search-sku">{{ entry.variant.sku || "-" }}</div>
                      <div class="pos-search-meta">
                        Tồn: {{ Number(entry.variant.stock || 0) }}
                      </div>
                    </div>
                  </button>
                  <div v-if="productSearchLoading" class="pos-search-loading">Đang tải...</div>
                  <div v-else-if="productSearchHasMore" class="pos-search-hint">Cuộn xuống để tải thêm</div>
                  <el-button text type="primary" class="pos-search-footer" @click="openProductCreateDrawer">
                    + Thêm mới hàng hóa
                  </el-button>
                </div>
              </el-card>
            </div>
          </el-col>
          <el-col :xs="24" :lg="11" class="pos-tab-col">
            <el-tabs v-model="activeInvoiceId" type="card" closable @tab-remove="removeInvoiceTab">
              <el-tab-pane v-for="tab in invoices" :key="tab.id" :name="tab.id" :label="tab.code" />
            </el-tabs>
            <el-button type="primary" :icon="Plus" @click="createInvoiceTab"></el-button>
          </el-col>
          <el-col :xs="24" :lg="5" class="pos-header-actions">
            <el-button :icon="Refresh" @click="bootstrap">Làm mới</el-button>
            <el-button :icon="Printer" @click="printInvoice">In</el-button>
            <el-button @click="goBackToAdmin">Thoát</el-button>
          </el-col>
        </el-row>
      </el-header>

      <el-main class="pos-main">
        <el-row :gutter="10" class="pos-row">
          <el-col :xs="24" :lg="17" class="pos-left-col">
            <el-card shadow="never" class="pos-card pos-order-card">
              <el-table :data="activeInvoice.items" size="small" stripe empty-text="Chưa có sản phẩm trong hóa đơn">
                <el-table-column label="Sản phẩm" min-width="240">
                  <template #default="{ row }">
                    <el-space direction="vertical" alignment="flex-start" :size="2">
                      <strong>{{ row.productName }}</strong>
                      <el-space :size="6" wrap>
                        <el-select
                          :model-value="normalizeVariantAttr(row.size)"
                          size="small"
                          placeholder="Size"
                          style="width: 92px"
                          @change="(size) => handleCartSizeChange(row, size)"
                        >
                          <el-option
                            v-for="item in cartSizeOptions(row)"
                            :key="`cart-size-${row.variantId}-${item.value}`"
                            :label="item.label"
                            :value="item.value"
                          />
                        </el-select>
                        <el-select
                          :model-value="normalizeVariantAttr(row.color)"
                          size="small"
                          placeholder="Màu"
                          style="width: 110px"
                          @change="(color) => handleCartColorChange(row, color)"
                        >
                          <el-option
                            v-for="item in cartColorOptions(row)"
                            :key="`cart-color-${row.variantId}-${item.value}`"
                            :label="item.label"
                            :value="item.value"
                          />
                        </el-select>
                        <span>{{ row.sku }}</span>
                      </el-space>
                    </el-space>
                  </template>
                </el-table-column>
                <el-table-column label="SL" width="120" align="center" header-align="center">
                  <template #default="{ row }">
                    <el-input-number
                      :model-value="row.quantity"
                      :min="1"
                      :max="row.maxStock || 9999"
                      size="small"
                      controls-position="right"
                      class="pos-qty-input"
                      @change="(value) => updateItemQty(row.variantId, value)"
                    />
                  </template>
                </el-table-column>
                <el-table-column label="Giá" width="130">
                  <template #default="{ row }">{{ formatCurrency(row.price) }}</template>
                </el-table-column>
                <el-table-column label="Thành tiền" width="140">
                  <template #default="{ row }">{{ formatCurrency(row.quantity * row.price) }}</template>
                </el-table-column>
                <el-table-column width="79">
                  <template #default="{ row }">
                    <el-button type="danger" plain :icon="Delete" size="small" @click="removeItem(row.variantId)">Xóa</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-card>

            <el-card shadow="never" class="pos-card pos-note-card">
              <el-input v-model="activeInvoice.note" placeholder="Ghi chú đơn hàng" clearable>
                <template #prefix>
                  <el-icon><Tickets /></el-icon>
                </template>
              </el-input>
            </el-card>
          </el-col>

          <el-col :xs="24" :lg="7" class="pos-right-col">
            <el-card shadow="never" class="pos-card pos-sidebar-card">
              <el-form label-position="top">
                <el-form-item label="Khách hàng" class="pos-customer-form-item">
                  <div class="pos-customer-search">
                    <el-input
                      ref="customerInputRef"
                      v-model="customerKeyword"
                      clearable
                      placeholder="Tìm khách hàng (F4)"
                      class="pos-customer-input"
                      @keyup.enter="applyCustomerSearch()"
                      @clear="clearCustomerSearch"
                    >
                      <template #prefix>
                        <el-icon><Search /></el-icon>
                      </template>
                    </el-input>
                    <el-button :icon="Plus" class="pos-customer-add-btn" @click="handleAddCustomerClick" />
                  </div>
                  <div
                    v-if="showCustomerSuggestions"
                    ref="customerSuggestionListRef"
                    class="pos-customer-suggestion-list"
                    @scroll.passive="handleCustomerSuggestionScroll"
                  >
                    <button
                      v-for="customer in filteredCustomerSuggestions"
                      :key="customer.id"
                      type="button"
                      class="pos-customer-suggestion-item"
                      @click="selectCustomerSuggestion(customer)"
                    >
                      {{ customerLabel(customer) }}
                    </button>
                    <div v-if="customerSearchLoading" class="pos-customer-loading">Đang tải...</div>
                    <div v-else-if="customerSearchHasMore" class="pos-customer-hint">Cuộn xuống để tải thêm</div>
                  </div>
                </el-form-item>

                <el-form-item label="Voucher">
                  <el-select
                    v-model="activeInvoice.voucherCode"
                    filterable
                    clearable
                    placeholder="Không áp dụng"
                    @change="validateVoucherSelection"
                  >
                    <el-option
                      v-for="voucher in voucherOptions"
                      :key="voucher.code"
                      :label="voucherLabel(voucher)"
                      :value="voucher.code"
                    />
                  </el-select>
                  <el-button text :icon="MagicStick" @click="applyBestVoucher">Gợi ý voucher</el-button>
                  <el-text v-if="activeInvoice.voucherCode" type="primary">
                    Đã áp mã: {{ activeInvoice.voucherCode }} (giảm {{ formatCurrency(voucherDiscount) }})
                  </el-text>
                </el-form-item>

                <el-form-item label="Loại đơn hàng">
                  <el-radio-group v-model="fulfillmentMode">
                    <el-radio value="COUNTER" @click="handleFulfillmentRadioClick('COUNTER')">Mua tại quầy</el-radio>
                    <el-radio value="DELIVERY" @click="handleFulfillmentRadioClick('DELIVERY')">Mua giao hàng</el-radio>
                  </el-radio-group>
                </el-form-item>
                <el-alert
                  v-if="activeInvoice.shipEnabled"
                  title="Đang bật giao hàng. Vui lòng nhập địa chỉ trong ngăn kéo bên phải."
                  type="info"
                  :closable="false"
                  show-icon
                />
                <el-alert
                  v-else
                  title="Đơn mua tại quầy (không tính phí ship)"
                  type="success"
                  :closable="false"
                  show-icon
                />

                <el-descriptions :column="1" border>
                  <el-descriptions-item label="Tổng tiền hàng">{{ formatCurrency(subTotal) }}</el-descriptions-item>
                  <el-descriptions-item label="Tạm tính (gồm phí ship)">{{ formatCurrency(subTotal + shippingFee) }}</el-descriptions-item>
                  <el-descriptions-item label="Giảm giá">{{ formatCurrency(voucherDiscount + safeManualDiscount) }}</el-descriptions-item>
                  <el-descriptions-item label="Phí ship">{{ formatCurrency(shippingFee) }}</el-descriptions-item>
                  <el-descriptions-item label="Khách cần trả">{{ formatCurrency(totalAmount) }}</el-descriptions-item>
                </el-descriptions>

                <el-form-item label="Thanh toán">
                  <el-radio-group v-model="activeInvoice.paymentMethod">
                    <el-radio value="CASH">Tiền mặt</el-radio>
                    <el-radio value="BANK_TRANSFER">Chuyển khoản</el-radio>
                  </el-radio-group>
                </el-form-item>
              </el-form>

              <el-card v-if="activeInvoice.paymentMethod === 'BANK_TRANSFER'" shadow="never" class="pos-card pos-qr-card">
                <el-alert
                  :title="
                    activeInvoice.orderId && activeInvoice.paymentUrl
                      ? `Đơn #${activeInvoice.orderId} đang chờ thanh toán trên MoMo IoT`
                      : 'Nhấn THANH TOÁN để chuyển sang giao diện MoMo IoT'
                  "
                  type="info"
                  :closable="false"
                  show-icon
                />
                <el-button
                  v-if="activeInvoice.orderId && activeInvoice.paymentUrl"
                  @click="openMomoIotPage(activeInvoice.orderId, activeInvoice.paymentUrl)"
                >
                  Mở lại MoMo IoT
                </el-button>
              </el-card>

              <el-button :icon="Select" type="primary" class="pos-checkout-btn" @click="checkoutInvoice">THANH TOÁN</el-button>
            </el-card>
          </el-col>
        </el-row>
      </el-main>
    </el-container>

    <el-drawer
      v-model="deliveryDrawerVisible"
      title="Thông tin giao hàng"
      direction="rtl"
      size="clamp(360px, 29.2vw, 520px)"
      :modal="false"
      :with-header="true"
      :append-to-body="true"
      class="pos-ship-drawer"
    >
      <el-form label-position="top">
        <el-form-item label="Tên người nhận">
          <el-input v-model="activeInvoice.recipientName" />
        </el-form-item>
        <el-form-item label="Số điện thoại">
          <el-input v-model="activeInvoice.phone" />
        </el-form-item>
        <el-form-item label="Tỉnh/Thành">
          <el-select
            v-model="activeInvoice.provinceId"
            filterable
            placeholder="Tỉnh/Thành phố"
            :loading="addressLoading.provinces"
            @change="handleProvinceChange"
          >
            <el-option v-for="item in provinces" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="Quận/Huyện">
          <el-select
            v-model="activeInvoice.districtId"
            filterable
            placeholder="Quận/Huyện"
            :disabled="!activeInvoice.provinceId"
            :loading="addressLoading.districts"
            @change="handleDistrictChange"
          >
            <el-option v-for="item in districts" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="Phường/Xã">
          <el-select
            v-model="activeInvoice.wardId"
            filterable
            placeholder="Phường/Xã"
            :disabled="!activeInvoice.districtId"
            :loading="addressLoading.wards"
            @change="handleWardChange"
          >
            <el-option v-for="item in wards" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="Địa chỉ chi tiết">
          <el-input v-model="activeInvoice.address" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="Phương thức giao hàng">
          <el-radio-group v-model="activeInvoice.deliveryMethod">
            <el-radio value="GHN">GHN</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-alert
          :title="`Phí ship tự động: ${formatCurrency(shippingFee)}`"
          type="info"
          :closable="false"
          show-icon
          v-loading="addressLoading.shippingFee"
        />
      </el-form>
      <template #footer>
        <el-button @click="closeShipDrawer">Đóng</el-button>
        <el-button type="primary" @click="confirmDeliveryMethod">Xác nhận phương thức giao hàng</el-button>
      </template>
    </el-drawer>

    <ProductAdminPage
      v-if="productCreateDrawerVisible"
      create-only
      @close-request="productCreateDrawerVisible = false"
      @created="handleProductCreated"
    />
  </section>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { useRouter } from "vue-router";
import {
  Delete,
  MagicStick,
  Plus,
  Printer,
  Refresh,
  Search,
  Select,
  Tickets
} from "@element-plus/icons-vue";
import { productApi } from "@/modules/product/api/productApi";
import { customerApi } from "@/modules/customer/api/customerApi";
import { voucherApi } from "@/modules/voucher/api/voucherApi";
import { orderApi } from "@/modules/order/api/orderApi";
import { posDraftApi } from "@/modules/pos/api/posDraftApi";
import { addressApi } from "@/modules/address/api/addressApi";
import ProductAdminPage from "@/modules/product/pages/admin/ProductAdminPage.vue";
import { useConfirmDialog } from "@/composables/useConfirmDialog";

const loading = ref(false);
const router = useRouter();
const productKeyword = ref("");
const customerKeyword = ref("");
const searchInputRef = ref(null);
const customerInputRef = ref(null);
const productSearchListRef = ref(null);
const customerSuggestionListRef = ref(null);
const products = ref([]);
const customers = ref([]);
const vouchers = ref([]);
const provinces = ref([]);
const districts = ref([]);
const wards = ref([]);
const addressLoading = ref({
  provinces: false,
  districts: false,
  wards: false,
  shippingFee: false
});
const SEARCH_PAGE_SIZE = 5;
const productSearchPage = ref(0);
const productSearchHasMore = ref(false);
const productSearchLoading = ref(false);
const customerSearchPage = ref(0);
const customerSearchHasMore = ref(false);
const customerSearchLoading = ref(false);
const fallbackImage = "https://via.placeholder.com/72x72?text=IMG";
const deliveryDrawerVisible = ref(false);
const productCreateDrawerVisible = ref(false);
const productSelections = ref({});
const POS_DRAFT_STORAGE_KEY_PREFIX = "pos_counter_draft_v1";
const POS_TERMINAL_ID_KEY = "pos_counter_terminal_id";
const POS_DRAFT_TTL_MS = 24 * 60 * 60 * 1000;
const terminalId = ref("");
let saveLocalDraftTimer = null;
let saveServerDraftTimer = null;
let isSavingServerDraft = false;
let isRestoringDraft = false;
let productSearchDebounceTimer = null;
let customerSearchDebounceTimer = null;

const invoiceSeed = ref(1);

const createInvoice = () => {
  const id = `inv_${Date.now()}_${Math.random().toString(36).slice(2, 6)}`;
  const code = `HD${String(invoiceSeed.value).padStart(3, "0")}`;
  invoiceSeed.value += 1;
  return {
    id,
    code,
    orderId: null,
    paymentUrl: "",
    status: "DRAFT",
    items: [],
    customerId: null,
    customer: null,
    voucherCode: "",
    manualDiscount: 0,
    shipEnabled: false,
    deliveryMethod: "GHN",
    deliveryMethodConfirmed: false,
    shippingFee: 0,
    recipientName: "",
    phone: "",
    address: "",
    provinceId: "",
    districtId: "",
    wardId: "",
    province: "",
    district: "",
    ward: "",
    paymentMethod: "CASH",
    paidAmount: 0,
    paidAt: null,
    note: ""
  };
};

const hasInvoiceMeaningfulData = (invoice) => {
  if (!invoice) return false;
  return Boolean(
    (Array.isArray(invoice.items) && invoice.items.length > 0)
      || invoice.customerId
      || String(invoice.voucherCode || "").trim()
      || Number(invoice.manualDiscount || 0) > 0
      || Number(invoice.shippingFee || 0) > 0
      || String(invoice.note || "").trim()
      || String(invoice.status || "").toUpperCase() === "WAITING_PAYMENT"
      || invoice.orderId
      || String(invoice.paymentUrl || "").trim()
  );
};

const getOrCreateTerminalId = () => {
  try {
    const existing = localStorage.getItem(POS_TERMINAL_ID_KEY);
    if (existing && existing.trim()) return existing.trim();
    const generated = `pos-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
    localStorage.setItem(POS_TERMINAL_ID_KEY, generated);
    return generated;
  } catch (_error) {
    return `pos-${Date.now()}`;
  }
};

const localDraftStorageKey = () => `${POS_DRAFT_STORAGE_KEY_PREFIX}_${terminalId.value || "default"}`;

const initialInvoice = createInvoice();
const invoices = ref([initialInvoice]);
const activeInvoiceId = ref(initialInvoice.id);
const { confirm, showAlert } = useConfirmDialog();
const notifyInfo = (message) => void showAlert(message, "Thông báo");
const notifySuccess = (message) => void showAlert(message, "Thành công");
const notifyWarning = (message) => void showAlert(message, "Cảnh báo");
const notifyError = (message) => void showAlert(message, "Lỗi");

const activeInvoice = computed(() => invoices.value.find((item) => item.id === activeInvoiceId.value) || null);

const normalizedProducts = computed(() => {
  return products.value
    .map((product) => {
      const variants = (product.variants || [])
        .filter((variant) => String(variant.status || "").toUpperCase() !== "INACTIVE")
        .map((variant) => ({
          variantId: variant.id,
          sku: variant.sku,
          size: variant.size,
          color: variant.color,
          stock: Number(variant.stock || 0),
          price: Number(variant.price || 0)
        }));
      return {
        id: product.id,
        productName: product.name,
        imageUrl: product.mainImageUrl || product.images?.find((img) => img?.isMain)?.url || product.images?.[0]?.url || "",
        variants
      };
    })
    .filter((product) => product.variants.length > 0);
});

const filteredProductVariants = computed(() => {
  return normalizedProducts.value.flatMap((product) =>
    product.variants.map((variant) => ({
      product,
      variant
    }))
  );
});

watch(activeInvoiceId, async () => {
  if (!activeInvoice.value) return;
  customerKeyword.value = activeInvoice.value.customer
    ? customerLabel(activeInvoice.value.customer)
    : "";
  if (activeInvoice.value.provinceId) {
    await fetchDistricts(activeInvoice.value.provinceId);
  } else {
    districts.value = [];
  }
  if (activeInvoice.value.districtId) {
    await fetchWards(activeInvoice.value.districtId);
  } else {
    wards.value = [];
  }
});

watch(
  () => activeInvoice.value?.shipEnabled,
  async (enabled) => {
    if (!enabled) {
      if (activeInvoice.value) {
        activeInvoice.value.shippingFee = 0;
      }
      return;
    }
    await hydrateShippingFromCustomer();
    recalculateShippingFee();
  }
);

watch(productKeyword, (value) => {
  const keyword = String(value || "").trim();
  if (productSearchDebounceTimer) {
    clearTimeout(productSearchDebounceTimer);
    productSearchDebounceTimer = null;
  }
  if (!keyword) {
    products.value = [];
    productSearchPage.value = 0;
    productSearchHasMore.value = false;
    return;
  }
  productSearchDebounceTimer = setTimeout(() => {
    void loadProductSearch(true);
  }, 220);
});

watch(customerKeyword, (value) => {
  const keyword = String(value || "").trim();
  if (customerSearchDebounceTimer) {
    clearTimeout(customerSearchDebounceTimer);
    customerSearchDebounceTimer = null;
  }
  if (!keyword) {
    customers.value = [];
    customerSearchPage.value = 0;
    customerSearchHasMore.value = false;
    return;
  }
  customerSearchDebounceTimer = setTimeout(() => {
    void loadCustomerSearch(true);
  }, 220);
});

const clearPosDraftStorage = () => {
  try {
    localStorage.removeItem(localDraftStorageKey());
  } catch (_error) {
    // Ignore storage errors.
  }
};

const buildDraftPayload = () => ({
  savedAt: Date.now(),
  activeInvoiceId: activeInvoiceId.value,
  invoices: invoices.value.map((invoice) => ({
    ...invoice,
    customer: null
  }))
});

const savePosDraftToStorage = () => {
  if (isRestoringDraft) return;
  const hasMeaningfulData = invoices.value.some(hasInvoiceMeaningfulData);
  if (!hasMeaningfulData) {
    clearPosDraftStorage();
    return;
  }
  try {
    localStorage.setItem(localDraftStorageKey(), JSON.stringify(buildDraftPayload()));
  } catch (_error) {
    // Ignore storage quota/runtime errors to avoid blocking POS flow.
  }
};

const deletePosDraftOnServer = async () => {
  if (!terminalId.value) return;
  try {
    await posDraftApi.deleteDraft(terminalId.value);
  } catch (_error) {
    // Keep local fallback and continue POS flow.
  }
};

const savePosDraftToServer = async () => {
  if (isRestoringDraft || isSavingServerDraft || !terminalId.value) return;
  const hasMeaningfulData = invoices.value.some(hasInvoiceMeaningfulData);
  if (!hasMeaningfulData) {
    await deletePosDraftOnServer();
    return;
  }
  isSavingServerDraft = true;
  try {
    await posDraftApi.saveDraft({
      terminalId: terminalId.value,
      payload: buildDraftPayload()
    });
  } catch (_error) {
    // Keep local fallback and continue POS flow.
  } finally {
    isSavingServerDraft = false;
  }
};

const schedulePosDraftSave = () => {
  if (isRestoringDraft) return;
  if (saveLocalDraftTimer) {
    clearTimeout(saveLocalDraftTimer);
  }
  saveLocalDraftTimer = setTimeout(() => {
    savePosDraftToStorage();
  }, 700);
  if (saveServerDraftTimer) {
    clearTimeout(saveServerDraftTimer);
  }
  saveServerDraftTimer = setTimeout(() => {
    void savePosDraftToServer();
  }, 5000);
};

const applyDraftPayload = async (parsed, sourceLabel) => {
  const savedAt = Number(parsed?.savedAt || 0);
  const draftInvoices = Array.isArray(parsed?.invoices) ? parsed.invoices : [];
  if (!savedAt || Date.now() - savedAt > POS_DRAFT_TTL_MS || !draftInvoices.length) {
    return false;
  }

  try {
    await confirm({
      title: "Khôi phục nháp POS",
      message: `Phát hiện dữ liệu nháp chưa hoàn tất (${sourceLabel}). Bạn có muốn khôi phục?`,
      confirmButtonText: "Khôi phục",
      cancelButtonText: "Bỏ qua"
    });
  } catch {
    return false;
  }

  isRestoringDraft = true;
  try {
    const restoredInvoices = draftInvoices.map((draft) => {
      const fallback = createInvoice();
      const customerId = draft?.customerId ?? null;
      return {
        ...fallback,
        ...draft,
        id: String(draft?.id || fallback.id),
        code: String(draft?.code || fallback.code),
        customerId,
        customer: customerId ? customerOptions.value.find((item) => item.id === customerId) || null : null,
        items: Array.isArray(draft?.items) ? draft.items : [],
        paymentUrl: String(draft?.paymentUrl || "")
      };
    });

    const maxCode = restoredInvoices.reduce((max, invoice) => {
      const matched = String(invoice.code || "").match(/^HD(\d+)$/);
      const number = matched ? Number(matched[1]) : 0;
      return Math.max(max, Number.isFinite(number) ? number : 0);
    }, 0);
    if (maxCode > 0) {
      invoiceSeed.value = maxCode + 1;
    }

    invoices.value = restoredInvoices;
    const restoredActiveId = String(parsed?.activeInvoiceId || "");
    activeInvoiceId.value = restoredInvoices.some((item) => item.id === restoredActiveId)
      ? restoredActiveId
      : restoredInvoices[0].id;
    notifySuccess(`Đã khôi phục nháp POS từ ${sourceLabel}`);
    return true;
  } finally {
    isRestoringDraft = false;
  }
};

const restorePosDraftFromServer = async () => {
  if (!terminalId.value) return false;
  try {
    const { data } = await posDraftApi.getDraft(terminalId.value);
    const payload = data?.payload;
    if (!payload || typeof payload !== "object") return false;
    return await applyDraftPayload(payload, "server");
  } catch (_error) {
    return false;
  }
};

const restorePosDraftFromStorage = async () => {
  let raw = null;
  try {
    raw = localStorage.getItem(localDraftStorageKey());
  } catch (_error) {
    raw = null;
  }
  if (!raw) return false;

  let parsed = null;
  try {
    parsed = JSON.parse(raw);
  } catch (_error) {
    clearPosDraftStorage();
    return false;
  }
  const restored = await applyDraftPayload(parsed, "local");
  if (!restored) {
    clearPosDraftStorage();
  }
  return restored;
};

const customerOptions = computed(() =>
  customers.value.filter((item) => String(item.status || "").toUpperCase() === "ACTIVE")
);
const voucherOptions = computed(() =>
  vouchers.value.filter((item) => String(item.status || "").toUpperCase() === "ACTIVE")
);
const filteredCustomerSuggestions = computed(() => {
  if (!customerKeyword.value.trim()) return [];
  return customerOptions.value;
});
const showCustomerSuggestions = computed(
  () => Boolean(customerKeyword.value.trim()) && (filteredCustomerSuggestions.value.length > 0 || customerSearchLoading.value)
);
const cartQuantity = computed(() => {
  if (!activeInvoice.value) return 0;
  return activeInvoice.value.items.reduce((sum, item) => sum + Number(item.quantity || 0), 0);
});

const subTotal = computed(() => {
  if (!activeInvoice.value) return 0;
  return activeInvoice.value.items.reduce((sum, item) => sum + item.quantity * item.price, 0);
});

const selectedVoucher = computed(() => {
  if (!activeInvoice.value?.voucherCode) return null;
  return voucherOptions.value.find((item) => item.code === activeInvoice.value.voucherCode) || null;
});

const voucherDiscount = computed(() => {
  const voucher = selectedVoucher.value;
  const subtotal = subTotal.value;
  if (!voucher || subtotal <= 0) return 0;
  const min = Number(voucher.minOrderValue || 0);
  if (subtotal < min) return 0;
  const type = String(voucher.discountType || "").toUpperCase();
  const value = Number(voucher.discountValue || 0);
  if (value <= 0) return 0;
  if (type === "PERCENT") return Math.min(subtotal, Math.round((subtotal * value) / 100));
  return Math.min(subtotal, value);
});

const safeManualDiscount = computed(() => {
  if (!activeInvoice.value) return 0;
  return Math.max(0, Math.min(Number(activeInvoice.value.manualDiscount || 0), subTotal.value));
});

const shippingFee = computed(() => {
  if (!activeInvoice.value?.shipEnabled) return 0;
  return Math.max(0, Number(activeInvoice.value.shippingFee || 0));
});

const totalAmount = computed(() => Math.max(0, subTotal.value - voucherDiscount.value - safeManualDiscount.value + shippingFee.value));
const paidAmount = computed(() => Math.max(0, Number(activeInvoice.value?.paidAmount || 0)));
const changeAmount = computed(() => Math.max(0, paidAmount.value - totalAmount.value));
const isMomoTransfer = computed(() => activeInvoice.value?.paymentMethod === "BANK_TRANSFER");
const resetShippingState = () => {
  if (!activeInvoice.value) return;
  activeInvoice.value.shippingFee = 0;
  activeInvoice.value.recipientName = "";
  activeInvoice.value.phone = "";
  activeInvoice.value.address = "";
  activeInvoice.value.provinceId = "";
  activeInvoice.value.districtId = "";
  activeInvoice.value.wardId = "";
  activeInvoice.value.province = "";
  activeInvoice.value.district = "";
  activeInvoice.value.ward = "";
  activeInvoice.value.deliveryMethodConfirmed = false;
  deliveryDrawerVisible.value = false;
};
const fulfillmentMode = computed({
  get: () => (activeInvoice.value?.shipEnabled ? "DELIVERY" : "COUNTER"),
  set: (mode) => {
    if (!activeInvoice.value) return;
    const isDelivery = mode === "DELIVERY";
    activeInvoice.value.shipEnabled = isDelivery;
    activeInvoice.value.deliveryMethodConfirmed = false;
    deliveryDrawerVisible.value = isDelivery;
    if (!isDelivery) {
      resetShippingState();
      activeInvoice.value.shipEnabled = false;
    }
    if (isDelivery && activeInvoice.value.customerId) {
      void hydrateShippingFromCustomer().then(() => recalculateShippingFee());
    }
  }
});
const handleFulfillmentRadioClick = (mode) => {
  if (!activeInvoice.value) return;
  if (mode === "DELIVERY") {
    if (activeInvoice.value.shipEnabled) {
      deliveryDrawerVisible.value = true;
      return;
    }
    fulfillmentMode.value = "DELIVERY";
    return;
  }
  fulfillmentMode.value = "COUNTER";
};

const activeStep = computed(() => {
  if (!activeInvoice.value) return 0;
  if (!activeInvoice.value.items.length) return 0;
  if (!activeInvoice.value.customerId) return 2;
  if (!activeInvoice.value.voucherCode && !safeManualDiscount.value) return 3;
  if (activeInvoice.value.status !== "PAID") return 4;
  if (activeInvoice.value.shipEnabled) return 6;
  return 5;
});

const formatCurrency = (value) =>
  Number(value || 0).toLocaleString("vi-VN", { style: "currency", currency: "VND", maximumFractionDigits: 0 });

const onVariantImageError = (event) => {
  if (!event?.target) return;
  event.target.src = fallbackImage;
};

const normalizeVariantAttr = (value) => String(value ?? "").trim();
const toVariantOption = (value) => {
  const normalized = normalizeVariantAttr(value);
  return {
    value: normalized,
    label: normalized || "-"
  };
};

const getProductSelection = (product) => {
  if (!productSelections.value[product.id]) {
    const firstVariant = product.variants[0] || {};
    productSelections.value[product.id] = {
      size: normalizeVariantAttr(firstVariant.size),
      color: normalizeVariantAttr(firstVariant.color),
      tempQty: 1
    };
  }
  const sizeSet = new Set(product.variants.map((variant) => normalizeVariantAttr(variant.size)));
  const selection = productSelections.value[product.id];
  if (!sizeSet.has(selection.size)) {
    selection.size = normalizeVariantAttr(product.variants[0]?.size);
  }
  syncColorSelection(product);
  return selection;
};

const sizeOptions = (product) => {
  return [...new Set(product.variants.map((variant) => normalizeVariantAttr(variant.size)))].map(toVariantOption);
};

const colorOptions = (product) => {
  const selection = productSelections.value[product.id] || {
    size: normalizeVariantAttr(product.variants[0]?.size)
  };
  const colorsBySize = product.variants
    .filter((variant) => normalizeVariantAttr(variant.size) === selection.size)
    .map((variant) => normalizeVariantAttr(variant.color));
  const colors = colorsBySize.length ? colorsBySize : product.variants.map((variant) => normalizeVariantAttr(variant.color));
  return [...new Set(colors)].map(toVariantOption);
};

const syncColorSelection = (product) => {
  const selection = productSelections.value[product.id];
  if (!selection) return;
  const validColors = colorOptions(product).map((item) => item.value);
  if (!validColors.includes(selection.color)) {
    selection.color = validColors[0] || "";
  }
};

const selectedVariant = (product) => {
  const selection = getProductSelection(product);
  return (
    product.variants.find(
      (variant) =>
        normalizeVariantAttr(variant.size) === selection.size &&
        normalizeVariantAttr(variant.color) === selection.color
    ) ||
    product.variants.find((variant) => normalizeVariantAttr(variant.size) === selection.size) ||
    product.variants[0] ||
    null
  );
};

const selectedVariantStock = (product) => {
  return Number(selectedVariant(product)?.stock || 1);
};

const selectedVariantPrice = (product) => {
  return formatCurrency(selectedVariant(product)?.price || 0);
};

const getItemVariants = (item) => {
  if (Array.isArray(item?.productVariants) && item.productVariants.length) {
    return item.productVariants;
  }
  const product = normalizedProducts.value.find((entry) => entry.id === item?.productId);
  return product?.variants || [];
};

const cartSizeOptions = (item) => {
  const variants = getItemVariants(item);
  return [...new Set(variants.map((variant) => normalizeVariantAttr(variant.size)))].map(toVariantOption);
};

const cartColorOptions = (item, size = item?.size) => {
  const normalizedSize = normalizeVariantAttr(size);
  const variants = getItemVariants(item);
  const colorsBySize = variants
    .filter((variant) => normalizeVariantAttr(variant.size) === normalizedSize)
    .map((variant) => normalizeVariantAttr(variant.color));
  const colors = colorsBySize.length ? colorsBySize : variants.map((variant) => normalizeVariantAttr(variant.color));
  return [...new Set(colors)].map(toVariantOption);
};

const resolveItemVariant = (item, size = item?.size, color = item?.color) => {
  const variants = getItemVariants(item);
  const normalizedSize = normalizeVariantAttr(size);
  const normalizedColor = normalizeVariantAttr(color);
  return (
    variants.find(
      (variant) =>
        normalizeVariantAttr(variant.size) === normalizedSize && normalizeVariantAttr(variant.color) === normalizedColor
    ) ||
    variants.find((variant) => normalizeVariantAttr(variant.size) === normalizedSize) ||
    variants[0] ||
    null
  );
};

const applyItemVariant = (item, nextVariant) => {
  if (!activeInvoice.value || !item || !nextVariant) return;
  const duplicated = activeInvoice.value.items.find((entry) => entry !== item && entry.variantId === nextVariant.variantId);
  if (duplicated) {
    const mergedQty = Number(duplicated.quantity || 0) + Number(item.quantity || 0);
    if (mergedQty > Number(nextVariant.stock || 0)) {
      notifyWarning("Không thể đổi biến thể vì vượt tồn kho");
      return;
    }
    duplicated.quantity = mergedQty;
    duplicated.maxStock = Number(nextVariant.stock || duplicated.maxStock || 0);
    activeInvoice.value.items = activeInvoice.value.items.filter((entry) => entry !== item);
    return;
  }
  item.variantId = nextVariant.variantId;
  item.sku = nextVariant.sku;
  item.size = nextVariant.size;
  item.color = nextVariant.color;
  item.price = Number(nextVariant.price || 0);
  item.maxStock = Number(nextVariant.stock || 0);
  if (item.quantity > item.maxStock) {
    item.quantity = Math.max(1, item.maxStock);
    notifyWarning("Số lượng được điều chỉnh theo tồn kho của biến thể mới");
  }
};

const handleCartSizeChange = (item, size) => {
  const colors = cartColorOptions(item, size);
  const fallbackColor = colors.find((entry) => entry.value === normalizeVariantAttr(item.color))?.value || colors[0]?.value || "";
  const nextVariant = resolveItemVariant(item, size, fallbackColor);
  applyItemVariant(item, nextVariant);
};

const handleCartColorChange = (item, color) => {
  const nextVariant = resolveItemVariant(item, item?.size, color);
  applyItemVariant(item, nextVariant);
};

const customerLabel = (customer) => {
  const name = String(customer?.fullName || customer?.username || "").trim();
  const phone = String(customer?.phone || "").trim();
  if (name && phone) return `${name} · ${phone}`;
  if (name) return name;
  if (phone) return phone;
  return "Khách hàng";
};

const voucherLabel = (voucher) => {
  const rule =
    String(voucher.discountType || "").toUpperCase() === "PERCENT"
      ? `${voucher.discountValue}%`
      : formatCurrency(voucher.discountValue);
  return `${voucher.code} · ${rule} · Min ${formatCurrency(voucher.minOrderValue)}`;
};

const createInvoiceTab = () => {
  const tab = createInvoice();
  invoices.value.push(tab);
  activeInvoiceId.value = tab.id;
};

const goBackToAdmin = () => {
  router.push("/admin");
};

const openProductCreateDrawer = async () => {
  if (productCreateDrawerVisible.value) {
    productCreateDrawerVisible.value = false;
    await nextTick();
  }
  productCreateDrawerVisible.value = true;
};

const handleProductCreated = async () => {
  productCreateDrawerVisible.value = false;
  await loadProducts();
};

const removeInvoiceTab = async (tabId) => {
  if (invoices.value.length <= 1) {
    notifyWarning("Cần giữ ít nhất một hóa đơn");
    return;
  }
  const tab = invoices.value.find((item) => item.id === tabId);
  if (!tab) return;
  try {
    await confirm({
      title: "Đóng hóa đơn",
      message: `Bạn có chắc muốn đóng ${tab.code}?`,
      confirmButtonText: "Đóng hóa đơn",
      cancelButtonText: "Hủy"
    });
  } catch {
    return;
  }
  const index = invoices.value.findIndex((item) => item.id === tabId);
  if (index === -1) return;
  invoices.value.splice(index, 1);
  if (activeInvoiceId.value === tabId) {
    activeInvoiceId.value = invoices.value[Math.max(0, index - 1)].id;
  }
};

const ensureActiveInvoice = () => {
  if (!activeInvoice.value) createInvoiceTab();
};

const addVariantToInvoice = (product, variant, qty = 1) => {
  if (!activeInvoice.value || !product || !variant) return false;
  const normalizedQty = Math.max(1, Number(qty || 1));
  if (normalizedQty > Number(variant.stock || 0)) {
    notifyWarning("Số lượng vượt tồn kho");
    return false;
  }
  const existing = activeInvoice.value.items.find((item) => item.variantId === variant.variantId);
  if (existing) {
    const nextQty = existing.quantity + normalizedQty;
    if (nextQty > existing.maxStock) {
      notifyWarning("Không thể thêm, vượt tồn kho");
      return false;
    }
    existing.quantity = nextQty;
  } else {
    activeInvoice.value.items.push({
      variantId: variant.variantId,
      productId: product.id,
      productName: product.productName,
      sku: variant.sku,
      size: variant.size,
      color: variant.color,
      quantity: normalizedQty,
      price: variant.price,
      maxStock: variant.stock,
      productVariants: product.variants.map((entry) => ({ ...entry }))
    });
  }
  return true;
};

const addProductToInvoice = (product) => {
  ensureActiveInvoice();
  const selection = getProductSelection(product);
  const variant = selectedVariant(product);
  if (!variant) {
    notifyWarning("Sản phẩm chưa có biến thể hợp lệ");
    return;
  }
  const qty = Math.max(1, Number(selection.tempQty || 1));
  if (!addVariantToInvoice(product, variant, qty)) return;
  selection.tempQty = 1;
  productKeyword.value = "";
};

const addVariantFromSearch = (product, variant) => {
  ensureActiveInvoice();
  if (addVariantToInvoice(product, variant, 1)) {
    productKeyword.value = "";
    notifySuccess(`Đã thêm ${variant.sku || `${variant.size || "-"} / ${variant.color || "-"}`}`);
  }
};

const updateItemQty = (variantId, nextQty) => {
  const item = activeInvoice.value?.items.find((row) => row.variantId === variantId);
  if (!item) return;
  const normalized = Math.max(1, Math.min(Number(nextQty || 1), Number(item.maxStock || 1)));
  item.quantity = normalized;
};

const removeItem = (variantId) => {
  if (!activeInvoice.value) return;
  activeInvoice.value.items = activeInvoice.value.items.filter((item) => item.variantId !== variantId);
};

const handleCustomerChange = async (customerId) => {
  if (!activeInvoice.value) return;
  activeInvoice.value.customer = customerOptions.value.find((item) => item.id === customerId) || null;
  await hydrateShippingFromCustomer();
  if (activeInvoice.value.shipEnabled) {
    await recalculateShippingFee();
  }
};

const clearCustomerSearch = () => {
  if (!activeInvoice.value) return;
  activeInvoice.value.customerId = null;
  activeInvoice.value.customer = null;
  customerKeyword.value = "";
  customerSearchPage.value = 0;
  customerSearchHasMore.value = false;
  customers.value = [];
  activeInvoice.value.shipEnabled = false;
  resetShippingState();
};

const applyCustomerSearch = async (silent = false) => {
  if (!activeInvoice.value) return;
  const keyword = customerKeyword.value.trim().toLowerCase();
  if (!keyword) {
    clearCustomerSearch();
    return;
  }
  if (!filteredCustomerSuggestions.value.length) {
    await loadCustomerSearch(true);
  }
  const matched = filteredCustomerSuggestions.value[0] || null;
  if (!matched) {
    if (!silent) {
      notifyWarning("Không tìm thấy khách hàng phù hợp");
    }
    return;
  }
  activeInvoice.value.customerId = matched.id;
  await handleCustomerChange(matched.id);
  customerKeyword.value = customerLabel(matched);
};

const selectCustomerSuggestion = async (customer) => {
  if (!activeInvoice.value || !customer) return;
  activeInvoice.value.customerId = customer.id;
  await handleCustomerChange(customer.id);
  customerKeyword.value = customerLabel(customer);
};

const reachedScrollBottom = (target) => {
  if (!target) return false;
  return target.scrollTop + target.clientHeight >= target.scrollHeight - 18;
};

const handleProductSearchScroll = async () => {
  if (!productKeyword.value.trim()) return;
  if (!productSearchHasMore.value || productSearchLoading.value) return;
  if (!reachedScrollBottom(productSearchListRef.value)) return;
  await loadProductSearch(false);
};

const handleCustomerSuggestionScroll = async () => {
  if (!customerKeyword.value.trim()) return;
  if (!customerSearchHasMore.value || customerSearchLoading.value) return;
  if (!reachedScrollBottom(customerSuggestionListRef.value)) return;
  await loadCustomerSearch(false);
};

const handleAddCustomerClick = () => {
  notifyInfo("Vui lòng tạo khách hàng ở màn hình quản lý khách hàng");
};

const normalizeAddressName = (value) => String(value || "").trim().toLowerCase();

const findAddressUnitByName = (units, name) => {
  const normalized = normalizeAddressName(name);
  if (!normalized) return null;
  return units.find((item) => normalizeAddressName(item.name) === normalized) || null;
};

const hydrateShippingFromCustomer = async () => {
  if (!activeInvoice.value || !activeInvoice.value.customerId) return;
  const customer = customerOptions.value.find((item) => item.id === activeInvoice.value.customerId) || null;
  if (customer) {
    activeInvoice.value.recipientName = customer.fullName || customer.username || "";
    activeInvoice.value.phone = customer.phone || "";
  }
  activeInvoice.value.address = "";
  activeInvoice.value.province = "";
  activeInvoice.value.district = "";
  activeInvoice.value.ward = "";
  activeInvoice.value.provinceId = "";
  activeInvoice.value.districtId = "";
  activeInvoice.value.wardId = "";
  activeInvoice.value.shippingFee = 0;
  try {
    const { data } = await customerApi.getDefaultAddress(activeInvoice.value.customerId);
    if (!data) return;

    activeInvoice.value.recipientName = data.recipientName || customer?.fullName || activeInvoice.value.recipientName || "";
    activeInvoice.value.phone = data.phone || customer?.phone || activeInvoice.value.phone || "";
    activeInvoice.value.address = data.addressLine || activeInvoice.value.address || "";
    activeInvoice.value.province = data.province || "";
    activeInvoice.value.district = data.district || "";
    activeInvoice.value.ward = data.ward || "";

    if (!provinces.value.length) {
      await fetchProvinces();
    }
    const province = findAddressUnitByName(provinces.value, data.province);
    if (!province) return;

    activeInvoice.value.provinceId = province.id;
    await fetchDistricts(province.id);
    const district = findAddressUnitByName(districts.value, data.district);
    if (!district) return;

    activeInvoice.value.districtId = district.id;
    await fetchWards(district.id);
    const ward = findAddressUnitByName(wards.value, data.ward);
    if (!ward) return;

    activeInvoice.value.wardId = ward.id;
  } catch (_error) {
    // Customer may not have default address, keep fallback from profile.
  }
};

const validateVoucherSelection = () => {
  if (!activeInvoice.value?.voucherCode) return;
  if (!selectedVoucher.value) {
    notifyWarning("Voucher không hợp lệ");
    activeInvoice.value.voucherCode = "";
    return;
  }
  if (subTotal.value < Number(selectedVoucher.value.minOrderValue || 0)) {
    notifyWarning("Đơn chưa đạt điều kiện tối thiểu của voucher");
  }
};

const applyBestVoucher = async () => {
  if (subTotal.value <= 0) {
    notifyWarning("Chưa có sản phẩm để áp voucher");
    return;
  }
  try {
    const { data } = await voucherApi.getBestVoucher(subTotal.value);
    if (!data?.code) {
      activeInvoice.value.voucherCode = "";
      notifyInfo("Không có voucher phù hợp");
      return;
    }
    activeInvoice.value.voucherCode = data.code;
    notifySuccess(`Đã áp voucher tốt nhất: ${data.code}`);
  } catch (error) {
    console.error(error);
    notifyError("Không gợi ý được voucher");
  }
};

const validateBeforeCheckout = () => {
  if (!activeInvoice.value.items.length) {
    notifyWarning("Hóa đơn chưa có sản phẩm");
    return false;
  }
  if (activeInvoice.value.voucherCode && voucherDiscount.value <= 0) {
    notifyWarning("Mã giảm giá đã chọn không còn hợp lệ hoặc đơn chưa đủ điều kiện áp dụng");
    return false;
  }
  if (activeInvoice.value.shipEnabled) {
    const required = [
      activeInvoice.value.recipientName,
      activeInvoice.value.phone,
      activeInvoice.value.address,
      activeInvoice.value.provinceId,
      activeInvoice.value.districtId,
      activeInvoice.value.wardId
    ].every((value) => String(value || "").trim());
    if (!required) {
      notifyWarning("Vui lòng nhập đầy đủ thông tin ship");
      return false;
    }
    if (!activeInvoice.value.deliveryMethod) {
      notifyWarning("Vui lòng chọn phương thức giao hàng");
      return false;
    }
    if (!activeInvoice.value.deliveryMethodConfirmed) {
      notifyWarning("Vui lòng xác nhận phương thức giao hàng");
      return false;
    }
  } else {
    activeInvoice.value.shippingFee = 0;
    activeInvoice.value.deliveryMethodConfirmed = false;
  }
  return true;
};

const checkoutInvoice = async () => {
  if (activeInvoice.value?.status === "PAID" && activeInvoice.value?.orderId) {
    notifyInfo("Hóa đơn này đã thanh toán");
    return;
  }
  const isBankTransfer = activeInvoice.value?.paymentMethod === "BANK_TRANSFER";
  if (isBankTransfer && activeInvoice.value?.status === "WAITING_PAYMENT" && activeInvoice.value?.orderId) {
    if (activeInvoice.value.paymentUrl) {
      openMomoIotPage(activeInvoice.value.orderId, activeInvoice.value.paymentUrl);
      return;
    }
    notifyWarning("Hóa đơn đang chờ thanh toán. Vui lòng tạo hóa đơn mới nếu cần.");
    return;
  }
  if (!validateBeforeCheckout()) return;
  try {
    const payload = {
      customerId: activeInvoice.value.customerId || null,
      items: activeInvoice.value.items.map((item) => ({
        variantId: item.variantId,
        quantity: item.quantity
      })),
      paymentMethod: activeInvoice.value.paymentMethod,
      voucherCode: activeInvoice.value.voucherCode || null,
      manualDiscount: safeManualDiscount.value,
      shippingFee: shippingFee.value,
      paidAmount: isMomoTransfer.value ? 0 : Math.max(paidAmount.value, totalAmount.value),
      shipEnabled: Boolean(activeInvoice.value.shipEnabled),
      recipientName: activeInvoice.value.recipientName || null,
      phone: activeInvoice.value.phone || null,
      province: activeInvoice.value.province || null,
      district: activeInvoice.value.district || null,
      ward: activeInvoice.value.ward || null,
      address: activeInvoice.value.address || null,
      note: activeInvoice.value.note || null
    };
    const { data } = await orderApi.posCheckout(payload);
    activeInvoice.value.orderId = data?.id || null;
    if (data?.status === "WAITING_PAYMENT" && data?.paymentMethod === "MOMO" && data?.paymentUrl) {
      activeInvoice.value.status = "WAITING_PAYMENT";
      activeInvoice.value.paymentUrl = data.paymentUrl;
      activeInvoice.value.paidAt = null;
      openMomoIotPage(data.id, data.paymentUrl);
      notifySuccess(`Đã chuyển sang giao diện MoMo IoT cho ${activeInvoice.value.code}`);
    } else {
      activeInvoice.value.status = "PAID";
      activeInvoice.value.paymentUrl = "";
      activeInvoice.value.paidAt = new Date().toISOString();
      notifySuccess(`Đã thanh toán ${activeInvoice.value.code}${data?.id ? ` (Order #${data.id})` : ""}`);
      clearInvoiceData();
    }
    await loadProducts();
  } catch (error) {
    console.error(error);
    const message = error?.response?.data?.message || "Thanh toán thất bại";
    const normalizedMessage = String(message).toLowerCase();
    if (
      normalizedMessage.includes("số tiền khách đưa chưa đủ")
      || normalizedMessage.includes("paidamount is not enough")
      || normalizedMessage.includes("paid amount is not enough")
    ) {
      await showAlert(message, "Thông báo thanh toán");
      return;
    }
    notifyError(message);
  }
};

const openMomoIotPage = (orderId, payUrl) => {
  const safeOrderId = Number(orderId || 0);
  const safePayUrl = String(payUrl || "").trim();
  if (!safeOrderId || !safePayUrl) {
    notifyWarning("Không tìm thấy thông tin thanh toán MoMo cho hóa đơn này");
    return;
  }
  const popup = window.open(safePayUrl, "_blank", "noopener,noreferrer");
  if (!popup) {
    notifyWarning("Trình duyệt đang chặn popup. Hãy cho phép popup để mở MoMo mà vẫn giữ màn POS.");
  }
};

const clearInvoiceData = () => {
  if (!activeInvoice.value) return;
  deliveryDrawerVisible.value = false;
  customerKeyword.value = "";
  productKeyword.value = "";
  const reset = createInvoice();
  activeInvoice.value.code = reset.code;
  activeInvoice.value.orderId = null;
  activeInvoice.value.paymentUrl = "";
  activeInvoice.value.status = "DRAFT";
  activeInvoice.value.items = [];
  activeInvoice.value.customerId = null;
  activeInvoice.value.customer = null;
  activeInvoice.value.voucherCode = "";
  activeInvoice.value.manualDiscount = 0;
  activeInvoice.value.shipEnabled = false;
  activeInvoice.value.deliveryMethod = "GHN";
  activeInvoice.value.deliveryMethodConfirmed = false;
  activeInvoice.value.shippingFee = 0;
  activeInvoice.value.recipientName = "";
  activeInvoice.value.phone = "";
  activeInvoice.value.address = "";
  activeInvoice.value.provinceId = "";
  activeInvoice.value.districtId = "";
  activeInvoice.value.wardId = "";
  activeInvoice.value.province = "";
  activeInvoice.value.district = "";
  activeInvoice.value.ward = "";
  activeInvoice.value.paymentMethod = "CASH";
  activeInvoice.value.paidAmount = 0;
  activeInvoice.value.paidAt = null;
  activeInvoice.value.note = "";
};

const clearCurrentInvoice = async () => {
  if (!activeInvoice.value) return;
  try {
    await confirm({
      title: "Xóa trắng hóa đơn",
      message: `Bạn có chắc muốn xóa toàn bộ sản phẩm và thông tin trên ${activeInvoice.value.code}?`,
      confirmButtonText: "Xóa trắng",
      cancelButtonText: "Hủy"
    });
  } catch {
    return;
  }
  clearInvoiceData();
};

const printInvoice = () => {
  if (!activeInvoice.value || !activeInvoice.value.items.length) {
    notifyWarning("Chưa có dữ liệu hóa đơn để in");
    return;
  }
  const invoice = activeInvoice.value;
  const htmlRows = invoice.items
    .map(
      (item) =>
        `<tr><td>${item.productName}<br/><small>${item.sku} ${item.size || ""}/${item.color || ""}</small></td><td>${item.quantity}</td><td>${formatCurrency(
          item.price
        )}</td><td>${formatCurrency(item.quantity * item.price)}</td></tr>`
    )
    .join("");

  const html = `<!doctype html><html><head><meta charset="utf-8"/><title>${invoice.code}</title>
  <style>body{font-family:"Pretendard",Arial,sans-serif;padding:16px}table{width:100%;border-collapse:collapse}th,td{border:1px solid #ddd;padding:8px;text-align:left}.sum{margin-top:10px;text-align:right}</style>
  </head><body>
  <h2>HÓA ĐƠN ${invoice.code}</h2>
  <p>Thời gian: ${new Date().toLocaleString("vi-VN")}</p>
  <p>Khách hàng: ${invoice.customer ? customerLabel(invoice.customer) : "Khách lẻ"}</p>
  <table><thead><tr><th>Sản phẩm</th><th>SL</th><th>Đơn giá</th><th>Thành tiền</th></tr></thead><tbody>${htmlRows}</tbody></table>
  <div class="sum"><p>Tạm tính: ${formatCurrency(subTotal.value)}</p><p>Giảm giá: -${formatCurrency(
    voucherDiscount.value + safeManualDiscount.value
  )}</p><p>Phí ship: ${formatCurrency(shippingFee.value)}</p><h3>Tổng cộng: ${formatCurrency(totalAmount.value)}</h3></div>
  </body></html>`;

  const printWindow = window.open("", "_blank", "width=820,height=720");
  if (!printWindow) {
    notifyError("Trình duyệt chặn popup in");
    return;
  }
  printWindow.document.write(html);
  printWindow.document.close();
  printWindow.focus();
  printWindow.print();
};

const mergeById = (currentItems, nextItems) => {
  const map = new Map();
  [...(currentItems || []), ...(nextItems || [])].forEach((item) => {
    if (!item?.id) return;
    map.set(item.id, item);
  });
  return Array.from(map.values());
};

const loadProductSearch = async (reset = false) => {
  const keyword = productKeyword.value.trim();
  if (!keyword) {
    products.value = [];
    productSearchPage.value = 0;
    productSearchHasMore.value = false;
    return;
  }
  if (productSearchLoading.value) return;

  const page = reset ? 0 : productSearchPage.value;
  productSearchLoading.value = true;
  try {
    const { data } = await productApi.getProducts({
      page,
      size: SEARCH_PAGE_SIZE,
      sortBy: "id",
      direction: "desc",
      q: keyword
    });
    const fetchedItems = (data?.content || []).filter((item) => String(item.status || "").toUpperCase() === "ACTIVE");
    products.value = reset ? fetchedItems : mergeById(products.value, fetchedItems);
    const nextPage = Number(data?.page ?? page) + 1;
    const totalPages = Number(data?.totalPages || 0);
    productSearchPage.value = nextPage;
    productSearchHasMore.value = nextPage < totalPages;
  } catch (error) {
    console.error(error);
    if (reset) {
      products.value = [];
    }
    productSearchHasMore.value = false;
    notifyError("Không tải được danh sách sản phẩm");
  } finally {
    productSearchLoading.value = false;
  }
};

const loadCustomerSearch = async (reset = false) => {
  const keyword = customerKeyword.value.trim();
  if (!keyword) {
    if (reset) {
      customers.value = [];
    }
    customerSearchPage.value = 0;
    customerSearchHasMore.value = false;
    return;
  }
  if (customerSearchLoading.value) return;

  const page = reset ? 0 : customerSearchPage.value;
  customerSearchLoading.value = true;
  try {
    const { data } = await customerApi.getCustomers({
      page,
      size: SEARCH_PAGE_SIZE,
      sortBy: "id",
      direction: "desc",
      status: "ACTIVE",
      q: keyword
    });
    const fetchedItems = data?.content || [];
    customers.value = reset ? fetchedItems : mergeById(customers.value, fetchedItems);
    const nextPage = Number(data?.page ?? page) + 1;
    const totalPages = Number(data?.totalPages || 0);
    customerSearchPage.value = nextPage;
    customerSearchHasMore.value = nextPage < totalPages;
  } catch (error) {
    console.error(error);
    if (reset) {
      customers.value = [];
    }
    customerSearchHasMore.value = false;
    notifyError("Không tải được danh sách khách hàng");
  } finally {
    customerSearchLoading.value = false;
  }
};

const loadProducts = async () => {
  await loadProductSearch(true);
};

const loadCustomers = async () => {
  const { data } = await customerApi.getCustomers({
    page: 0,
    size: SEARCH_PAGE_SIZE,
    sortBy: "id",
    direction: "desc",
    status: "ACTIVE"
  });
  customers.value = data?.content || [];
};

const loadVouchers = async () => {
  const { data } = await voucherApi.getPublicVouchers();
  vouchers.value = Array.isArray(data) ? data : [];
};

const fetchProvinces = async () => {
  try {
    addressLoading.value.provinces = true;
    const { data } = await addressApi.getProvinces();
    provinces.value = Array.isArray(data) ? data : [];
  } catch (_error) {
    provinces.value = [];
    notifyError("Không thể tải danh sách tỉnh/thành từ GHN");
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
  } catch (_error) {
    districts.value = [];
    notifyError("Không thể tải danh sách quận/huyện");
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
  } catch (_error) {
    wards.value = [];
    notifyError("Không thể tải danh sách phường/xã");
  } finally {
    addressLoading.value.wards = false;
  }
};

const handleProvinceChange = async (provinceId) => {
  if (!activeInvoice.value) return;
  const selected = provinces.value.find((item) => item.id === provinceId);
  activeInvoice.value.province = selected?.name || "";
  activeInvoice.value.districtId = "";
  activeInvoice.value.wardId = "";
  activeInvoice.value.district = "";
  activeInvoice.value.ward = "";
  activeInvoice.value.deliveryMethodConfirmed = false;
  activeInvoice.value.shippingFee = 0;
  wards.value = [];
  await fetchDistricts(provinceId);
};

const handleDistrictChange = async (districtId) => {
  if (!activeInvoice.value) return;
  const selected = districts.value.find((item) => item.id === districtId);
  activeInvoice.value.district = selected?.name || "";
  activeInvoice.value.wardId = "";
  activeInvoice.value.ward = "";
  activeInvoice.value.deliveryMethodConfirmed = false;
  activeInvoice.value.shippingFee = 0;
  await fetchWards(districtId);
};

const handleWardChange = async (wardId) => {
  if (!activeInvoice.value) return;
  const selected = wards.value.find((item) => item.id === wardId);
  activeInvoice.value.ward = selected?.name || "";
  activeInvoice.value.deliveryMethodConfirmed = false;
  await recalculateShippingFee();
};

const closeShipDrawer = () => {
  deliveryDrawerVisible.value = false;
};

const confirmDeliveryMethod = async () => {
  if (!activeInvoice.value) return;
  activeInvoice.value.shipEnabled = true;
  if (!activeInvoice.value.deliveryMethod) {
    notifyWarning("Vui lòng chọn phương thức giao hàng");
    return;
  }
  // Confirm shipping mode first, fee can be recalculated once address is complete.
  const hasFullAddress = [
    activeInvoice.value.provinceId,
    activeInvoice.value.districtId,
    activeInvoice.value.wardId
  ].every((value) => String(value || "").trim());
  if (hasFullAddress) {
    await recalculateShippingFee();
  } else {
    activeInvoice.value.shippingFee = 0;
  }
  activeInvoice.value.deliveryMethodConfirmed = true;
  notifySuccess(`Đã xác nhận phương thức giao hàng: ${activeInvoice.value.deliveryMethod}`);
  closeShipDrawer();
};

const recalculateShippingFee = async () => {
  if (!activeInvoice.value || !activeInvoice.value.shipEnabled) return;
  const districtId = Number(activeInvoice.value.districtId || 0);
  const wardCode = String(activeInvoice.value.wardId || "").trim();
  if (!districtId || !wardCode) {
    activeInvoice.value.shippingFee = 0;
    return;
  }
  try {
    addressLoading.value.shippingFee = true;
    const { data } = await addressApi.getShippingFee(districtId, wardCode);
    const fee = Number(data?.fee ?? data ?? 0);
    activeInvoice.value.shippingFee = Math.max(0, Number.isFinite(fee) ? fee : 0);
  } catch (error) {
    console.error(error);
    activeInvoice.value.shippingFee = 0;
    notifyError("Không tính được phí ship GHN");
  } finally {
    addressLoading.value.shippingFee = false;
  }
};

const bootstrap = async () => {
  loading.value = true;
  try {
    await Promise.all([loadProducts(), loadCustomers(), loadVouchers(), fetchProvinces()]);
    if (!invoices.value.length) createInvoiceTab();
  } catch (error) {
    console.error(error);
    notifyError("Không tải được dữ liệu POS");
  } finally {
    loading.value = false;
  }
};

const isTypingElement = (target) => {
  const tagName = String(target?.tagName || "").toLowerCase();
  if (tagName === "input" || tagName === "textarea" || tagName === "select") return true;
  return Boolean(target?.isContentEditable);
};

const adjustLastItemQuantity = (delta) => {
  const lastItem = activeInvoice.value?.items?.[activeInvoice.value.items.length - 1];
  if (!lastItem) return;
  const maxStock = Math.max(1, Number(lastItem.maxStock || 1));
  const nextQty = Math.max(1, Math.min(maxStock, Number(lastItem.quantity || 1) + Number(delta || 0)));
  updateItemQty(lastItem.variantId, nextQty);
};

const removeLastItem = () => {
  const lastItem = activeInvoice.value?.items?.[activeInvoice.value.items.length - 1];
  if (!lastItem) return;
  removeItem(lastItem.variantId);
};

const handleKeydown = (event) => {
  const key = String(event.key || "").toUpperCase();
  const typing = isTypingElement(event.target);
  if (key === "F2") {
    event.preventDefault();
    searchInputRef.value?.focus?.();
    return;
  }
  if (key === "F4") {
    event.preventDefault();
    customerInputRef.value?.focus?.();
    return;
  }
  if (key === "F8") {
    event.preventDefault();
    checkoutInvoice();
    return;
  }
  if (key === "F9") {
    event.preventDefault();
    printInvoice();
    return;
  }

  if (typing) return;

  if (key === "DELETE") {
    event.preventDefault();
    removeLastItem();
    return;
  }

  if (event.key === "+" || event.key === "=") {
    event.preventDefault();
    adjustLastItemQuantity(1);
    return;
  }

  if (event.key === "-" || event.key === "_") {
    event.preventDefault();
    adjustLastItemQuantity(-1);
  }
};

onMounted(() => {
  terminalId.value = getOrCreateTerminalId();
  bootstrap().then(async () => {
    const restoredFromServer = await restorePosDraftFromServer();
    if (!restoredFromServer) {
      await restorePosDraftFromStorage();
    }
  });
  window.addEventListener("keydown", handleKeydown);
});

onBeforeUnmount(() => {
  if (productSearchDebounceTimer) {
    clearTimeout(productSearchDebounceTimer);
    productSearchDebounceTimer = null;
  }
  if (customerSearchDebounceTimer) {
    clearTimeout(customerSearchDebounceTimer);
    customerSearchDebounceTimer = null;
  }
  if (saveLocalDraftTimer) {
    clearTimeout(saveLocalDraftTimer);
    saveLocalDraftTimer = null;
  }
  if (saveServerDraftTimer) {
    clearTimeout(saveServerDraftTimer);
    saveServerDraftTimer = null;
  }
  savePosDraftToStorage();
  void savePosDraftToServer();
  window.removeEventListener("keydown", handleKeydown);
});

watch(invoices, schedulePosDraftSave, { deep: true });
watch(activeInvoiceId, schedulePosDraftSave);
</script>

<style scoped>
.pos-page {
  height: 100vh;
  min-height: 100dvh;
  background: var(--el-bg-color-page);
}

.pos-shell {
  height: 100%;
  min-height: 0;
}

.pos-header {
  height: auto;
  padding: 8px 10px 4px;
  border-bottom: 1px solid var(--el-border-color-light);
  background: var(--el-bg-color);
  overflow: visible;
}

.pos-search-box {
  position: relative;
}

.pos-search-dropdown {
  position: absolute;
  top: calc(100% + 6px);
  left: 0;
  width: 100%;
  z-index: 30;
  max-height: 420px;
}

.pos-search-dropdown :deep(.el-card__body) {
  max-height: 380px;
  overflow: hidden;
  padding: 8px;
}

.pos-search-item-compact {
  display: flex;
  gap: 10px;
  align-items: center;
  padding: 10px;
  border: 1px solid var(--el-border-color-light);
  background: #edf2fb;
  cursor: pointer;
}

.pos-search-item-compact + .pos-search-item-compact {
  margin-top: 8px;
}

.pos-search-item-btn {
  width: 100%;
  text-align: left;
  border-radius: 6px;
  appearance: none;
  font: inherit;
}

.pos-search-item-btn:hover {
  border-color: var(--el-color-primary-light-5);
  background: #e7eefc;
}

.pos-search-thumb {
  width: 44px;
  height: 44px;
  flex: 0 0 auto;
  border: 1px solid var(--el-border-color-lighter);
}

.pos-search-content {
  display: grid;
  gap: 2px;
  min-width: 0;
  width: 100%;
}

.pos-search-row-1 {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
}

.pos-search-name {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.pos-variant-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  margin-left: 6px;
  padding: 0 6px;
  border-radius: 999px;
  background: #dbeafe;
  color: #1d4ed8;
  font-size: 12px;
  line-height: 18px;
}

.pos-search-price {
  color: var(--el-color-primary);
  min-width: 88px;
  text-align: right;
  white-space: nowrap;
}

.pos-search-sku {
  line-height: 1.25;
  font-weight: 500;
}

.pos-search-meta {
  line-height: 1.25;
  color: var(--el-text-color-secondary);
}

.pos-search-footer {
  margin-top: 8px;
  width: 100%;
  justify-content: center;
  border-top: 1px solid var(--el-border-color-lighter);
}

.pos-tab-col {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.pos-tab-col :deep(.el-tabs) {
  flex: 1 1 auto;
  min-width: 0;
}

.pos-tab-col :deep(.el-tabs__header) {
  margin: 0;
}

.pos-header-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  flex-wrap: wrap;
}

.pos-main {
  padding: 10px;
  overflow: auto;
  min-height: 0;
}

.pos-row {
  height: calc(100vh - 102px);
  min-height: 0;
}

.pos-left-col {
  display: flex;
  flex-direction: column;
  gap: 10px;
  height: 100%;
  min-height: 0;
  min-width: 0;
}

.pos-right-col {
  height: 100%;
  min-height: 0;
  min-width: 0;
}

.pos-card {
  border-radius: 0;
}

.pos-order-card {
  flex: 1 1 auto;
  min-height: 0;
}

.pos-order-card :deep(.el-card__body) {
  height: 100%;
}

.pos-order-card :deep(.el-table) {
  height: 100%;
}

.pos-order-card :deep(.el-table__body-wrapper) {
  overflow-x: auto;
}

.pos-qty-input {
  width: 100%;
}

.pos-note-card {
  flex: 0 0 auto;
}

.pos-sidebar-card {
  height: 100%;
  overflow: auto;
}

.pos-customer-form-item :deep(.el-form-item__content) {
  display: block;
}

.pos-customer-search {
  display: grid;
  grid-template-columns: 1fr 38px;
  gap: 6px;
  width: 100%;
}

.pos-customer-input {
  width: 100%;
}

.pos-customer-input :deep(.el-input__wrapper),
.pos-customer-input :deep(.el-input-group__append) {
  background: #f3f4f6;
}

.pos-customer-add-btn {
  width: 38px;
  padding: 0;
}

.pos-customer-suggestion-list {
  margin-top: 6px;
  border: 1px solid var(--el-border-color-light);
  max-height: 180px;
  overflow: auto;
  background: var(--el-bg-color);
}

.pos-customer-suggestion-item {
  width: 100%;
  text-align: left;
  background: transparent;
  border: 0;
  border-bottom: 1px solid var(--el-border-color-lighter);
  padding: 8px 10px;
  cursor: pointer;
  color: var(--el-text-color-primary);
}

.pos-customer-suggestion-item:last-child {
  border-bottom: 0;
}

.pos-customer-suggestion-item:hover {
  background: var(--el-fill-color-light);
}

.pos-customer-loading,
.pos-customer-hint {
  padding: 8px 10px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.pos-checkout-btn {
  width: 100%;
  height: 42px;
  margin-top: 8px;
}

.pos-ship-drawer :deep(.el-drawer) {
  border-radius: 0;
  top: 62px;
  right: 0;
  height: calc(100vh - 62px);
}

.pos-search-list {
  width: 100%;
  max-height: 360px;
  overflow: auto;
}

.pos-search-loading,
.pos-search-hint {
  padding: 8px 6px;
  font-size: 12px;
  text-align: center;
  color: var(--el-text-color-secondary);
}

.pos-pagination {
  margin-top: 10px;
  justify-content: flex-end;
}

.pos-qr-card {
  margin-bottom: 10px;
}

.pos-qr-card :deep(.el-card__body) {
  display: grid;
  gap: 8px;
  justify-items: start;
}

@media (max-width: 1200px) {
  .pos-header-actions {
    justify-content: flex-start;
  }

  .pos-main {
    height: auto;
    overflow: auto;
  }

  .pos-row {
    height: auto;
  }

  .pos-left-col,
  .pos-right-col {
    height: auto;
  }

  .pos-ship-drawer :deep(.el-drawer) {
    top: 0;
    right: 0;
    height: 100vh;
  }
}

@media (max-width: 992px) {
  .pos-header {
    padding: 8px;
  }

  .pos-tab-col {
    margin-top: 6px;
  }

  .pos-header-actions {
    margin-top: 6px;
    width: 100%;
  }

  .pos-header-actions :deep(.el-button) {
    flex: 1 1 auto;
  }

  .pos-order-card :deep(.el-table),
  .pos-order-card :deep(.el-table__inner-wrapper) {
    min-width: 700px;
  }
}

@media (max-width: 768px) {
  .pos-main {
    padding: 8px;
  }

  .pos-search-dropdown {
    position: static;
    margin-top: 6px;
    max-height: none;
  }

  .pos-search-dropdown :deep(.el-card__body) {
    max-height: none;
  }

  .pos-search-list {
    max-height: 280px;
  }

  .pos-customer-suggestion-list {
    max-height: 220px;
  }

  .pos-customer-search {
    grid-template-columns: 1fr 34px;
  }

  .pos-order-card :deep(.el-table),
  .pos-order-card :deep(.el-table__inner-wrapper) {
    min-width: 760px;
  }

}
</style>

<template>
  <section class="pos-counter-page admin-page-shell" v-loading="loading">
    <el-card shadow="never" class="kv-header-card">
      <div class="kv-header">
        <div class="kv-header-title">
          <strong>Bán hàng tại quầy</strong>
          <span>F2/F3 tìm hàng · F8 thanh toán · F9 in hóa đơn</span>
        </div>
        <el-space wrap>
          <el-button @click="goBackToAdmin">Quay lại</el-button>
          <el-button :icon="Refresh" @click="bootstrap">Làm mới</el-button>
          <el-button type="primary" :icon="Plus" @click="createInvoiceTab">Tạo hóa đơn</el-button>
        </el-space>
      </div>
      <el-tabs v-model="activeInvoiceId" type="card" closable @tab-remove="removeInvoiceTab">
        <el-tab-pane
          v-for="tab in invoices"
          :key="tab.id"
          :name="tab.id"
          :label="tab.code + (tab.status === 'PAID' ? ' • Đã thanh toán' : '')"
        />
      </el-tabs>
    </el-card>

    <div class="kv-layout">
      <el-card shadow="never" class="kv-products">
        <div class="kv-panel-head">
          <el-space>
            <el-icon><Search /></el-icon>
            <strong>Danh sách hàng hóa</strong>
          </el-space>
          <el-space>
            <el-tag effect="plain">{{ filteredProducts.length }} sản phẩm</el-tag>
            <el-tag type="success" effect="plain">{{ cartQuantity }} món trong giỏ</el-tag>
          </el-space>
        </div>

        <el-input
          ref="searchInputRef"
          v-model="productKeyword"
          clearable
          placeholder="Tìm theo tên sản phẩm, SKU, size, màu"
          class="kv-search"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>

        <div class="kv-variant-list">
          <article v-for="product in paginatedProducts" :key="product.id" class="kv-variant-card">
            <img
              :src="product.imageUrl || fallbackImage"
              alt="product"
              class="kv-variant-thumb"
              @error="onVariantImageError"
            />
            <div class="kv-variant-main">
              <p class="kv-variant-name">{{ product.productName }}</p>
              <p class="kv-variant-meta">
                {{ product.variants.length }} biến thể
              </p>
              <div class="kv-variant-attrs">
                <el-select
                  v-model="getProductSelection(product).size"
                  placeholder="Size"
                  class="kv-variant-select"
                  @change="syncColorSelection(product)"
                >
                  <el-option
                    v-for="item in sizeOptions(product)"
                    :key="`size-${product.id}-${item.value}`"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
                <el-select
                  v-model="getProductSelection(product).color"
                  placeholder="Màu"
                  class="kv-variant-select"
                >
                  <el-option
                    v-for="item in colorOptions(product)"
                    :key="`color-${product.id}-${item.value}`"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </div>
              <p class="kv-variant-pick">
                Giá: <strong>{{ selectedVariantPrice(product) }}</strong> · Tồn: {{ selectedVariantStock(product) }}
              </p>
            </div>
            <el-input-number
              v-model="getProductSelection(product).tempQty"
              :min="1"
              :max="selectedVariantStock(product)"
              controls-position="right"
              size="small"
            />
            <el-button type="primary" :icon="CirclePlus" @click="addProductToInvoice(product)">Thêm</el-button>
          </article>
        </div>
        <div class="kv-pagination">
          <el-pagination
            v-model:current-page="productPage"
            :page-size="PRODUCTS_PER_PAGE"
            :total="filteredProducts.length"
            layout="prev, pager, next"
            background
            small
          />
        </div>
      </el-card>

      <el-card shadow="never" class="kv-order">
        <div class="kv-panel-head">
          <el-space>
            <el-icon><Tickets /></el-icon>
            <strong>{{ activeInvoice.code }}</strong>
            <el-tag v-if="activeInvoice.orderId" effect="plain">Order #{{ activeInvoice.orderId }}</el-tag>
          </el-space>
          <el-space>
            <el-tag effect="plain">{{ activeInvoice.items.length }} dòng SP</el-tag>
            <el-tag :type="activeInvoice.status === 'PAID' ? 'success' : 'warning'" effect="dark">
              {{ activeInvoice.status === "PAID" ? "ĐÃ THANH TOÁN" : "TẠM GIỮ" }}
            </el-tag>
          </el-space>
        </div>

        <el-table :data="activeInvoice.items" size="small" stripe class="kv-cart-table" empty-text="Chưa có sản phẩm">
          <el-table-column label="Sản phẩm" min-width="220">
            <template #default="{ row }">
              <div class="item-cell">
                <strong>{{ row.productName }}</strong>
                <span>{{ row.sku }} · {{ row.size || "-" }} / {{ row.color || "-" }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="SL" width="120">
            <template #default="{ row }">
              <el-input-number
                :model-value="row.quantity"
                :min="1"
                :max="row.maxStock || 9999"
                size="small"
                controls-position="right"
                @change="(value) => updateItemQty(row.variantId, value)"
              />
            </template>
          </el-table-column>
          <el-table-column label="Giá" width="130">
            <template #default="{ row }">{{ formatCurrency(row.price) }}</template>
          </el-table-column>
          <el-table-column label="Thành tiền" width="130">
            <template #default="{ row }">{{ formatCurrency(row.quantity * row.price) }}</template>
          </el-table-column>
          <el-table-column width="66">
            <template #default="{ row }">
              <el-button type="danger" text :icon="Delete" @click="removeItem(row.variantId)" />
            </template>
          </el-table-column>
        </el-table>

        <div class="kv-form-grid">
          <el-form-item label="Khách hàng">
            <el-select
              v-model="activeInvoice.customerId"
              filterable
              clearable
              placeholder="Khách lẻ"
              @change="handleCustomerChange"
            >
              <el-option label="Khách lẻ" :value="null" />
              <el-option
                v-for="customer in customerOptions"
                :key="customer.id"
                :label="customerLabel(customer)"
                :value="customer.id"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="Voucher">
            <el-space wrap>
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
              <el-button plain :icon="MagicStick" @click="applyBestVoucher">Gợi ý</el-button>
            </el-space>
          </el-form-item>

          <el-form-item label="Giảm tay">
            <el-input-number v-model="activeInvoice.manualDiscount" :min="0" :step="1000" controls-position="right" />
          </el-form-item>

          <el-form-item label="Giao hàng">
            <el-switch v-model="activeInvoice.shipEnabled" />
          </el-form-item>
        </div>

        <el-collapse-transition>
          <div v-if="activeInvoice.shipEnabled" class="kv-ship-box">
            <el-alert type="info" show-icon :closable="false" title="Đơn ship: nhập thông tin người nhận." />
            <div class="kv-ship-grid">
              <el-input v-model="activeInvoice.recipientName" placeholder="Tên người nhận" />
              <el-input v-model="activeInvoice.phone" placeholder="Số điện thoại" />
              <el-select
                v-model="activeInvoice.provinceId"
                filterable
                placeholder="Tỉnh/Thành phố"
                :loading="addressLoading.provinces"
                @change="handleProvinceChange"
              >
                <el-option v-for="item in provinces" :key="item.id" :label="item.name" :value="item.id" />
              </el-select>
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
              <el-input-number v-model="activeInvoice.shippingFee" :min="0" :step="5000" controls-position="right" />
            </div>
            <el-input
              v-model="activeInvoice.address"
              type="textarea"
              :rows="2"
              placeholder="Số nhà, tên đường, toà nhà..."
            />
          </div>
        </el-collapse-transition>

        <div class="kv-payment-row">
          <el-form-item label="Thanh toán">
            <el-radio-group v-model="activeInvoice.paymentMethod">
              <el-radio-button label="CASH">Tiền mặt</el-radio-button>
              <el-radio-button label="BANK_TRANSFER">Chuyển khoản</el-radio-button>
              <el-radio-button label="CARD">Thẻ</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="Khách đưa">
            <el-input-number v-model="activeInvoice.paidAmount" :min="0" :step="10000" controls-position="right" />
          </el-form-item>
        </div>

        <div class="kv-summary">
          <div><span>Tạm tính</span><strong>{{ formatCurrency(subTotal) }}</strong></div>
          <div><span>Giảm voucher</span><strong>- {{ formatCurrency(voucherDiscount) }}</strong></div>
          <div><span>Giảm thủ công</span><strong>- {{ formatCurrency(safeManualDiscount) }}</strong></div>
          <div><span>Phí ship</span><strong>{{ formatCurrency(shippingFee) }}</strong></div>
          <div><span>Khách thanh toán</span><strong>{{ formatCurrency(paidAmount) }}</strong></div>
          <div class="kv-total"><span>Tổng cần thu</span><strong>{{ formatCurrency(totalAmount) }}</strong></div>
          <div><span>Tiền thừa</span><strong>{{ formatCurrency(changeAmount) }}</strong></div>
        </div>

        <div class="kv-action-row">
          <el-button type="warning" plain :icon="Delete" @click="clearCurrentInvoice">Xóa trắng</el-button>
          <el-button type="success" plain :icon="Printer" @click="printInvoice">In hóa đơn</el-button>
          <el-button type="primary" :icon="Select" @click="checkoutInvoice">Thanh toán ngay</el-button>
        </div>
      </el-card>
    </div>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import {
  CirclePlus,
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
import { addressApi } from "@/modules/address/api/addressApi";
import { useConfirmDialog } from "@/composables/useConfirmDialog";

const loading = ref(false);
const router = useRouter();
const productKeyword = ref("");
const searchInputRef = ref(null);
const products = ref([]);
const customers = ref([]);
const vouchers = ref([]);
const provinces = ref([]);
const districts = ref([]);
const wards = ref([]);
const addressLoading = ref({
  provinces: false,
  districts: false,
  wards: false
});
const PRODUCTS_PER_PAGE = 5;
const productPage = ref(1);
const fallbackImage = "https://via.placeholder.com/72x72?text=IMG";
const productSelections = ref({});

const invoiceSeed = ref(1);

const createInvoice = () => {
  const id = `inv_${Date.now()}_${Math.random().toString(36).slice(2, 6)}`;
  const code = `HD${String(invoiceSeed.value).padStart(3, "0")}`;
  invoiceSeed.value += 1;
  return {
    id,
    code,
    orderId: null,
    status: "DRAFT",
    items: [],
    customerId: null,
    customer: null,
    voucherCode: "",
    manualDiscount: 0,
    shipEnabled: false,
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
    paidAt: null
  };
};

const initialInvoice = createInvoice();
const invoices = ref([initialInvoice]);
const activeInvoiceId = ref(initialInvoice.id);
const { confirm } = useConfirmDialog();

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

const filteredProducts = computed(() => {
  const q = productKeyword.value.trim().toLowerCase();
  if (!q) return normalizedProducts.value;
  return normalizedProducts.value.filter((product) => {
    const variantBlob = product.variants
      .map((variant) => `${variant.sku || ""} ${variant.size || ""} ${variant.color || ""}`)
      .join(" ")
      .toLowerCase();
    const blob = `${product.productName || ""} ${variantBlob}`.toLowerCase();
    return blob.includes(q);
  });
});

const paginatedProducts = computed(() => {
  const start = (productPage.value - 1) * PRODUCTS_PER_PAGE;
  return filteredProducts.value.slice(start, start + PRODUCTS_PER_PAGE);
});

watch(productKeyword, () => {
  productPage.value = 1;
});

watch(filteredProducts, (items) => {
  const maxPage = Math.max(1, Math.ceil(items.length / PRODUCTS_PER_PAGE));
  if (productPage.value > maxPage) {
    productPage.value = maxPage;
  }
});

watch(activeInvoiceId, async () => {
  if (!activeInvoice.value) return;
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

const customerOptions = computed(() =>
  customers.value.filter((item) => String(item.status || "").toUpperCase() === "ACTIVE")
);
const voucherOptions = computed(() =>
  vouchers.value.filter((item) => String(item.status || "").toUpperCase() === "ACTIVE")
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

const customerLabel = (customer) => {
  const name = customer.fullName || customer.username || "N/A";
  const phone = customer.phone || "N/A";
  return `#${customer.id} · ${name} · ${phone}`;
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

const removeInvoiceTab = async (tabId) => {
  if (invoices.value.length <= 1) {
    ElMessage.warning("Cần giữ ít nhất một hóa đơn");
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

const addProductToInvoice = (product) => {
  ensureActiveInvoice();
  const selection = getProductSelection(product);
  const variant = selectedVariant(product);
  if (!variant) {
    ElMessage.warning("Sản phẩm chưa có biến thể hợp lệ");
    return;
  }
  const qty = Math.max(1, Number(selection.tempQty || 1));
  if (qty > Number(variant.stock || 0)) {
    ElMessage.warning("Số lượng vượt tồn kho");
    return;
  }

  const existing = activeInvoice.value.items.find((item) => item.variantId === variant.variantId);
  if (existing) {
    const nextQty = existing.quantity + qty;
    if (nextQty > existing.maxStock) {
      ElMessage.warning("Không thể thêm, vượt tồn kho");
      return;
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
      quantity: qty,
      price: variant.price,
      maxStock: variant.stock
    });
  }
  selection.tempQty = 1;
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

const handleCustomerChange = (customerId) => {
  if (!activeInvoice.value) return;
  activeInvoice.value.customer = customerOptions.value.find((item) => item.id === customerId) || null;
};

const validateVoucherSelection = () => {
  if (!activeInvoice.value?.voucherCode) return;
  if (!selectedVoucher.value) {
    ElMessage.warning("Voucher không hợp lệ");
    activeInvoice.value.voucherCode = "";
    return;
  }
  if (subTotal.value < Number(selectedVoucher.value.minOrderValue || 0)) {
    ElMessage.warning("Đơn chưa đạt điều kiện tối thiểu của voucher");
  }
};

const applyBestVoucher = async () => {
  if (subTotal.value <= 0) {
    ElMessage.warning("Chưa có sản phẩm để áp voucher");
    return;
  }
  try {
    const { data } = await voucherApi.getBestVoucher(subTotal.value);
    if (!data?.code) {
      activeInvoice.value.voucherCode = "";
      ElMessage.info("Không có voucher phù hợp");
      return;
    }
    activeInvoice.value.voucherCode = data.code;
    ElMessage.success(`Đã áp voucher tốt nhất: ${data.code}`);
  } catch (error) {
    console.error(error);
    ElMessage.error("Không gợi ý được voucher");
  }
};

const validateBeforeCheckout = () => {
  if (!activeInvoice.value.items.length) {
    ElMessage.warning("Hóa đơn chưa có sản phẩm");
    return false;
  }
  if (activeInvoice.value.shipEnabled) {
    const required = [
      activeInvoice.value.recipientName,
      activeInvoice.value.phone,
      activeInvoice.value.address
    ].every((value) => String(value || "").trim());
    if (!required) {
      ElMessage.warning("Vui lòng nhập đầy đủ thông tin ship");
      return false;
    }
  }
  if (paidAmount.value < totalAmount.value) {
    ElMessage.warning("Số tiền khách đưa chưa đủ");
    return false;
  }
  return true;
};

const checkoutInvoice = async () => {
  if (activeInvoice.value?.status === "PAID" && activeInvoice.value?.orderId) {
    ElMessage.info("Hóa đơn này đã thanh toán");
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
      paidAmount: paidAmount.value,
      shipEnabled: Boolean(activeInvoice.value.shipEnabled),
      recipientName: activeInvoice.value.recipientName || null,
      phone: activeInvoice.value.phone || null,
      province: activeInvoice.value.province || null,
      district: activeInvoice.value.district || null,
      ward: activeInvoice.value.ward || null,
      address: activeInvoice.value.address || null
    };
    const { data } = await orderApi.posCheckout(payload);
    activeInvoice.value.status = "PAID";
    activeInvoice.value.paidAt = new Date().toISOString();
    activeInvoice.value.orderId = data?.id || null;
    ElMessage.success(`Đã thanh toán ${activeInvoice.value.code}${data?.id ? ` (Order #${data.id})` : ""}`);
    await loadProducts();
  } catch (error) {
    console.error(error);
    ElMessage.error(error?.response?.data?.message || "Thanh toán thất bại");
  }
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
  const keepCode = activeInvoice.value.code;
  const reset = createInvoice();
  activeInvoice.value.items = [];
  activeInvoice.value.customerId = null;
  activeInvoice.value.customer = null;
  activeInvoice.value.voucherCode = "";
  activeInvoice.value.manualDiscount = 0;
  activeInvoice.value.shipEnabled = false;
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
  activeInvoice.value.status = "DRAFT";
  activeInvoice.value.paidAt = null;
  activeInvoice.value.orderId = null;
  activeInvoice.value.code = keepCode;
  activeInvoice.value.id = activeInvoice.value.id || reset.id;
};

const printInvoice = () => {
  if (!activeInvoice.value || !activeInvoice.value.items.length) {
    ElMessage.warning("Chưa có dữ liệu hóa đơn để in");
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
  <style>body{font-family:Arial,sans-serif;padding:16px}table{width:100%;border-collapse:collapse}th,td{border:1px solid #ddd;padding:8px;text-align:left}.sum{margin-top:10px;text-align:right}</style>
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
    ElMessage.error("Trình duyệt chặn popup in");
    return;
  }
  printWindow.document.write(html);
  printWindow.document.close();
  printWindow.focus();
  printWindow.print();
};

const loadProducts = async () => {
  const collected = [];
  let page = 0;
  let totalPages = 1;
  while (page < totalPages && page < 10) {
    const { data } = await productApi.getProducts({
      page,
      size: 100,
      sortBy: "id",
      direction: "desc"
    });
    collected.push(...(data?.content || []));
    totalPages = Number(data?.totalPages || 1);
    page += 1;
  }
  products.value = collected.filter((item) => String(item.status || "").toUpperCase() === "ACTIVE");
};

const loadCustomers = async () => {
  const { data } = await customerApi.getCustomers({
    page: 0,
    size: 100,
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
    ElMessage.error("Không thể tải danh sách tỉnh/thành từ GHN");
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
    ElMessage.error("Không thể tải danh sách quận/huyện");
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
    ElMessage.error("Không thể tải danh sách phường/xã");
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
  wards.value = [];
  await fetchDistricts(provinceId);
};

const handleDistrictChange = async (districtId) => {
  if (!activeInvoice.value) return;
  const selected = districts.value.find((item) => item.id === districtId);
  activeInvoice.value.district = selected?.name || "";
  activeInvoice.value.wardId = "";
  activeInvoice.value.ward = "";
  await fetchWards(districtId);
};

const handleWardChange = (wardId) => {
  if (!activeInvoice.value) return;
  const selected = wards.value.find((item) => item.id === wardId);
  activeInvoice.value.ward = selected?.name || "";
};

const bootstrap = async () => {
  loading.value = true;
  try {
    await Promise.all([loadProducts(), loadCustomers(), loadVouchers(), fetchProvinces()]);
    if (!invoices.value.length) createInvoiceTab();
  } catch (error) {
    console.error(error);
    ElMessage.error("Không tải được dữ liệu POS");
  } finally {
    loading.value = false;
  }
};

const handleKeydown = (event) => {
  const key = String(event.key || "").toUpperCase();
  if (key === "F2") {
    event.preventDefault();
    searchInputRef.value?.focus?.();
  }
  if (key === "F8") {
    event.preventDefault();
    checkoutInvoice();
  }
  if (key === "F9") {
    event.preventDefault();
    printInvoice();
  }
};

onMounted(() => {
  bootstrap();
  window.addEventListener("keydown", handleKeydown);
});

onBeforeUnmount(() => {
  window.removeEventListener("keydown", handleKeydown);
});
</script>

<style scoped lang="scss">
.pos-counter-page {
  display: grid;
  gap: 14px;
  --kv-border: #d6dce6;
  --kv-bg: #f5f7fb;
  --kv-primary: #0f4f9f;
  --kv-soft: #eef3fb;

  :deep(.el-card),
  :deep(.el-card__body),
  :deep(.el-button),
  :deep(.el-input__wrapper),
  :deep(.el-textarea__inner),
  :deep(.el-select__wrapper),
  :deep(.el-input-number),
  :deep(.el-input-number__decrease),
  :deep(.el-input-number__increase),
  :deep(.el-tabs__item),
  :deep(.el-tabs--card > .el-tabs__header .el-tabs__nav),
  :deep(.el-table),
  :deep(.el-table__inner-wrapper),
  :deep(.el-tag),
  :deep(.el-alert),
  :deep(.el-radio-button__inner) {
    border-radius: 0 !important;
  }
}

.kv-header-card,
.kv-products,
.kv-order {
  border-radius: 0;
  border-color: var(--kv-border);
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.06);

  :deep(.el-card__body) {
    padding: 14px;
  }
}

.kv-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 10px;
  padding-bottom: 10px;
  border-bottom: 1px solid #e2e8f0;
}

.kv-header-title {
  display: grid;
}

.kv-header-title strong {
  font-size: 18px;
  color: #0f172a;
}

.kv-header-title span {
  font-size: 12px;
  color: #64748b;
}

.kv-layout {
  display: grid;
  gap: 14px;
  grid-template-columns: 1.2fr 1fr;
}

.kv-panel-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.kv-search {
  margin-bottom: 10px;
}

.kv-variant-list {
  display: grid;
  gap: 10px;
}

.kv-pagination {
  margin-top: 10px;
  display: flex;
  justify-content: flex-end;
}

.kv-variant-card {
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr) auto auto;
  align-items: center;
  gap: 10px;
  padding: 10px;
  border: 1px solid var(--kv-border);
  border-radius: 0;
  background: var(--kv-bg);
  transition: border-color 0.2s ease, background 0.2s ease;
}

.kv-variant-card:hover {
  border-color: #94a3b8;
  background: #ffffff;
}

.kv-variant-thumb {
  width: 72px;
  height: 72px;
  object-fit: cover;
  border: 1px solid var(--kv-border);
  background: #ffffff;
}

.kv-variant-main {
  display: grid;
  gap: 5px;
}

.kv-variant-name {
  margin: 0;
  font-weight: 700;
  color: #111827;
}

.kv-variant-meta {
  margin: 0;
  font-size: 12px;
  color: #64748b;
}

.kv-variant-pick {
  margin: 0;
  font-size: 12px;
  color: #475569;
}

.kv-variant-pick strong {
  color: #0f172a;
}

.kv-variant-attrs {
  display: flex;
  gap: 8px;
  align-items: center;
}

.kv-variant-select {
  width: 100%;
}

.kv-variant-price-row strong {
  color: #b42318;
}

.kv-variant-price-row span {
  font-size: 12px;
  color: #475569;
}

.kv-cart-table {
  margin-bottom: 10px;
  border: 1px solid #e2e8f0;
}

.item-cell {
  display: grid;
}

.item-cell span {
  font-size: 12px;
  color: #64748b;
}

.kv-form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px 12px;
  padding: 10px;
  border: 1px solid #e2e8f0;
  background: #fafcff;
}

.kv-ship-box {
  display: grid;
  gap: 10px;
  margin-top: 6px;
  border: 1px solid #dbe7ff;
  background: var(--kv-soft);
  padding: 10px;
}

.kv-ship-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.kv-payment-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px 12px;
  margin-top: 8px;
}

.kv-summary {
  margin-top: 8px;
  border: 1px solid var(--kv-border);
  border-radius: 0;
  background: #f8fafc;
  overflow: hidden;
}

.kv-summary > div {
  display: flex;
  justify-content: space-between;
  padding: 8px 10px;
  border-bottom: 1px dashed #dbe2ea;
}

.kv-summary > div:last-child {
  border-bottom: none;
}

.kv-summary span {
  color: #475569;
}

.kv-summary strong {
  color: #0f172a;
}

.kv-total {
  background: #dbeafe;
}

.kv-total span,
.kv-total strong {
  color: var(--kv-primary);
  font-size: 15px;
}

.kv-action-row {
  margin-top: 12px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  position: sticky;
  bottom: 0;
  background: #ffffff;
  padding-top: 8px;
  border-top: 1px solid #e2e8f0;
}

@media (max-width: 1440px) {
  .kv-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 900px) {
  .kv-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .kv-form-grid,
  .kv-payment-row {
    grid-template-columns: 1fr;
  }

  .kv-ship-grid {
    grid-template-columns: 1fr;
  }

  .kv-variant-card,
  .kv-action-row {
    grid-template-columns: 1fr;
  }
}
</style>

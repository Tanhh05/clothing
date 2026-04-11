<template>
  <section class="product-admin-page admin-page-shell" v-loading="loading">
    <div class="inventory-panel">
      <div class="panel-header">
        <div class="panel-actions">
          <el-input
            v-model="keyword"
            placeholder="Tìm theo tên / thương hiệu"
            clearable
            class="search-input"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          <div class="action-buttons">
            <el-button @click="refreshProducts">Làm mới</el-button>
            <el-button plain @click="openImportDialog">Import XLSX</el-button>
            <el-button plain @click="openInventoryDrawer">Tồn kho</el-button>
            <el-button plain @click="openDeletedDrawer">Đã xóa</el-button>
            <el-button type="primary" @click="openCreate">+ Thêm sản phẩm</el-button>
          </div>
        </div>
      </div>

      <div v-if="selectedIds.length" class="bulk-toolbar">
        <p>Đã chọn {{ selectedIds.length }} sản phẩm</p>
        <div class="bulk-actions">
          <el-button size="small" @click="bulkSetStatus('ACTIVE')">Đặt ACTIVE</el-button>
          <el-button size="small" @click="bulkSetStatus('INACTIVE')">Đặt INACTIVE</el-button>
          <el-button size="small" type="danger" plain @click="bulkSoftDelete">Xóa mềm</el-button>
          <el-button size="small" @click="clearSelection">Bỏ chọn</el-button>
        </div>
      </div>

      <div class="table-wrap">
        <BaseTable
          ref="productTableRef"
          :data="filteredProducts"
          border
          stripe
          size="small"
          class="inventory-table"
          empty-text="Không có sản phẩm"
          table-layout="fixed"
          @selection-change="handleSelectionChange"
        >
          <el-table-column type="selection" width="48" />
          <el-table-column label="Sản phẩm" min-width="300">
            <template #default="{ row }">
              <div class="product-cell">
                <img
                  :src="row.mainImageUrl || row.images?.[0]?.url || fallbackImage"
                  alt="thumbnail"
                  class="thumb"
                  @error="onThumbError"
                />
                <div>
                  <p class="name">#{{ row.id }} · {{ row.name || "N/A" }}</p>
                  <p class="meta">{{ row.slug || "-" }}</p>
                  <p class="meta">{{ row.brand || "Không thương hiệu" }} · {{ row.categoryName || "N/A" }}</p>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="Thông số" min-width="210">
            <template #default="{ row }">
              <div class="product-stats">
                <span><strong>Giá:</strong> {{ formatPrice(readProductPrice(row)) }}</span>
                <span><strong>Tồn kho:</strong> {{ readProductStock(row) }}</span>
                <span><strong>Biến thể:</strong> {{ Array.isArray(row.variants) ? row.variants.length : 0 }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="Trạng thái" width="118">
            <template #default="{ row }">
              <el-tag :type="readStatusTag(row.status)">{{ row.status || "UNKNOWN" }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="Thao tác" width="165">
            <template #default="{ row }">
              <div class="action-cell">
                <el-button size="small" @click="openEdit(row)">Sửa</el-button>
                <el-button size="small" type="danger" plain @click="deleteItem(row)">Xóa</el-button>
              </div>
            </template>
          </el-table-column>
        </BaseTable>
      </div>
      <div class="pagination-wrap">
        <el-pagination
          layout="total, prev, pager, next"
          :total="totalElements"
          :current-page="currentPage"
          :page-size="pageSize"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <el-drawer
      v-model="drawerVisible"
      :title="drawerMode === 'create' ? 'Thêm sản phẩm' : 'Cập nhật sản phẩm'"
      direction="rtl"
      size="42%"
      class="product-form-drawer"
    >
      <el-form :model="productForm" label-position="top">
        <el-form-item label="Tên sản phẩm">
          <el-input v-model="productForm.name" />
        </el-form-item>
        <el-form-item label="Thương hiệu">
          <el-input v-model="productForm.brand" />
        </el-form-item>
        <el-form-item label="Danh mục">
          <el-select v-model="productForm.categoryId" style="width: 100%" placeholder="Chọn danh mục">
            <el-option
              v-for="category in categoryOptions"
              :key="category.id"
              :label="category.name"
              :value="category.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="Ảnh sản phẩm">
          <el-upload
            v-model:file-list="productForm.uploadFiles"
            class="product-image-uploader"
            list-type="picture-card"
            :auto-upload="false"
            :multiple="true"
            accept="image/*"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
          <p class="upload-tip">Chọn nhiều ảnh từ máy. Ảnh đầu tiên sẽ là ảnh chính.</p>
        </el-form-item>
        <el-form-item label="Mô tả">
          <el-input v-model="productForm.description" type="textarea" :rows="3" />
        </el-form-item>
        <div class="variant-block">
          <div class="variant-head">
            <h4>Biến thể sản phẩm</h4>
            <el-button size="small" type="primary" plain @click="addVariant">+ Thêm biến thể</el-button>
          </div>

          <div v-for="(variant, index) in productForm.variants" :key="`variant-${index}`" class="variant-item">
            <div class="variant-item-head">
              <p>Biến thể #{{ index + 1 }}</p>
              <el-button
                v-if="productForm.variants.length > 1"
                size="small"
                type="danger"
                plain
                @click="removeVariant(index)"
              >
                Xóa
              </el-button>
            </div>

            <el-form-item label="SKU">
              <div class="sku-row">
                <el-input v-model="variant.sku" :disabled="variant.autoSku" />
                <el-switch
                  :model-value="variant.autoSku"
                  inline-prompt
                  active-text="AUTO"
                  inactive-text="MANUAL"
                  @change="(value) => handleAutoSkuToggle(index, value)"
                />
              </div>
            </el-form-item>
            <el-row :gutter="10">
              <el-col :span="12">
                <el-form-item label="Giá">
                  <el-input v-model.number="variant.price" type="number" min="0" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="Tồn kho">
                  <el-input v-model.number="variant.stock" type="number" min="0" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="10">
              <el-col :span="24">
                <el-form-item label="Cân nặng (kg)">
                  <el-input v-model.number="variant.weight" type="number" min="0" step="0.1" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="10">
              <el-col :span="12">
                <el-form-item>
                  <template #label>
                    <div class="inline-label">
                      <span>Size</span>
                      <el-button
                        size="small"
                        circle
                        plain
                        class="inline-plus-btn"
                        @click="openOptionPanel('size', index)"
                      >
                        <el-icon><Plus /></el-icon>
                      </el-button>
                    </div>
                  </template>
                  <el-select
                    v-model="variant.size"
                    style="width: 100%"
                    filterable
                    allow-create
                    default-first-option
                    @change="onVariantSizeChange"
                    placeholder="Chọn hoặc nhập size"
                  >
                    <el-option v-for="size in sizeOptions" :key="size" :label="size" :value="size" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item>
                  <template #label>
                    <div class="inline-label">
                      <span>Màu</span>
                      <el-button
                        size="small"
                        circle
                        plain
                        class="inline-plus-btn"
                        @click="openOptionPanel('color', index)"
                      >
                        <el-icon><Plus /></el-icon>
                      </el-button>
                    </div>
                  </template>
                  <el-select
                    v-model="variant.color"
                    style="width: 100%"
                    filterable
                    allow-create
                    default-first-option
                    @change="onVariantColorChange"
                    placeholder="Chọn hoặc nhập màu"
                  >
                    <el-option v-for="color in colorOptions" :key="color" :label="color" :value="color" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
          </div>
        </div>
      </el-form>

      <transition name="option-panel-fade">
        <div v-if="optionPanelVisible" class="option-side-panel-backdrop" @click="closeOptionPanel" />
      </transition>
      <transition name="option-panel-slide">
        <aside v-if="optionPanelVisible" class="option-side-panel">
          <div class="option-side-panel-head">
            <h5>{{ optionPanelType === "size" ? "Thêm size mới" : "Thêm màu mới" }}</h5>
            <el-button text @click="closeOptionPanel">Đóng</el-button>
          </div>
          <el-form label-position="top">
            <el-form-item :label="optionPanelType === 'size' ? 'Tên size' : 'Tên màu'">
              <el-input
                v-model="optionPanelValue"
                :placeholder="optionPanelType === 'size' ? 'VD: XXL' : 'VD: Xanh navy'"
                :disabled="optionPanelSubmitting"
                @keyup.enter="confirmCreateOption"
              />
            </el-form-item>
          </el-form>
          <div class="option-side-panel-actions">
            <el-button :disabled="optionPanelSubmitting" @click="closeOptionPanel">Hủy</el-button>
            <el-button type="primary" :loading="optionPanelSubmitting" @click="confirmCreateOption">Thêm</el-button>
          </div>
        </aside>
      </transition>

      <template #footer>
        <el-button @click="drawerVisible = false">Hủy</el-button>
        <el-button type="primary" :loading="submitting" @click="submitProduct">
          {{ drawerMode === "create" ? "Tạo sản phẩm" : "Lưu thay đổi" }}
        </el-button>
      </template>
    </el-drawer>

    <el-drawer
      v-model="deletedDrawerVisible"
      title="Sản phẩm đã xóa"
      direction="rtl"
      size="34%"
      class="deleted-product-drawer"
    >
      <div class="deleted-drawer-head">
        <p>{{ deletedProducts.length }} sản phẩm</p>
        <el-button size="small" :loading="deletedLoading" @click="loadDeletedProducts">Làm mới</el-button>
      </div>

      <div v-loading="deletedLoading">
        <div v-if="!deletedLoading && !deletedProducts.length" class="deleted-empty">
          Không có sản phẩm đã xóa.
        </div>

        <div v-else class="deleted-list">
          <article v-for="item in deletedProducts" :key="item.id" class="deleted-card">
            <div class="deleted-main">
              <p class="deleted-name">{{ item.name || "N/A" }}</p>
              <p class="deleted-meta">
                #{{ item.id }} • {{ item.brand || "Không thương hiệu" }} • {{ item.categoryName || "N/A" }}
              </p>
            </div>
            <el-button
              type="success"
              plain
              size="small"
              :loading="restoringId === item.id"
              @click="restoreDeletedProduct(item)"
            >
              Khôi phục
            </el-button>
          </article>
        </div>
      </div>
    </el-drawer>

    <el-drawer
      v-model="inventoryDrawerVisible"
      title="Cảnh báo tồn kho"
      direction="rtl"
      size="78%"
      class="inventory-drawer"
    >
      <div class="deleted-drawer-head">
        <p>{{ lowStockItems.length }} biến thể stock thấp</p>
        <el-button size="small" :loading="inventoryLoading" @click="loadInventoryAlerts">Làm mới</el-button>
      </div>

      <div v-loading="inventoryLoading" class="inventory-alert-wrap">
        <BaseTable
          :data="lowStockItems"
          border
          stripe
          size="small"
          table-layout="fixed"
          empty-text="Không có biến thể nào dưới ngưỡng tồn kho."
        >
          <el-table-column prop="variantId" label="Variant ID" width="110" />
          <el-table-column prop="productId" label="Product ID" width="110" />
          <el-table-column prop="productName" label="Sản phẩm" min-width="220" />
          <el-table-column prop="sku" label="SKU" min-width="170" />
          <el-table-column prop="stock" label="Tồn kho" width="100" />
          <el-table-column label="Lịch sử" width="120">
            <template #default="{ row }">
              <el-button size="small" @click="loadInventoryLogs(row.variantId)">Xem log</el-button>
            </template>
          </el-table-column>
        </BaseTable>
      </div>

      <section class="inventory-log-box">
        <h4>Lịch sử nhập/xuất</h4>
        <BaseTable :data="inventoryLogs" border stripe size="small" table-layout="fixed" empty-text="Chưa có log">
          <el-table-column prop="createdAt" label="Thời gian" min-width="165">
            <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column prop="sku" label="SKU" min-width="140" />
          <el-table-column prop="type" label="Loại" min-width="100" />
          <el-table-column prop="quantity" label="SL" min-width="80" />
          <el-table-column label="Tồn kho" min-width="120">
            <template #default="{ row }">{{ row.beforeStock }} → {{ row.afterStock }}</template>
          </el-table-column>
          <el-table-column prop="note" label="Ghi chú" min-width="220" show-overflow-tooltip />
        </BaseTable>
      </section>
    </el-drawer>

    <el-dialog
      v-model="importDialogVisible"
      title="Import sản phẩm từ XLSX"
      width="760px"
      class="product-import-dialog"
    >
      <div class="import-toolbar">
        <el-checkbox v-model="importDryRun">Dry-run (chỉ kiểm tra)</el-checkbox>
        <el-checkbox v-model="importUpsertBySku">Update nếu SKU đã tồn tại</el-checkbox>
        <el-button link type="primary" @click="downloadImportTemplate">Tải file mẫu</el-button>
      </div>

      <div class="import-guide">
        <p>File cần có sheet đầu tiên, hàng đầu là tiêu đề. Thứ tự cột:</p>
        <code>name | brand | description | category(id/slug/name) | status | imageUrl | sku | price | stock | weight | size | color | slug</code>
      </div>

      <el-upload
        v-model:file-list="importFileList"
        drag
        :auto-upload="false"
        :multiple="false"
        :limit="1"
        accept=".xlsx"
        class="import-upload"
      >
        <el-icon class="el-icon--upload"><Upload /></el-icon>
        <div class="el-upload__text">
          Kéo thả file .xlsx vào đây hoặc <em>chọn file</em>
        </div>
      </el-upload>

      <div v-if="importResult" class="import-result">
        <div class="import-result-summary">
          <el-tag type="warning">Batch: {{ importResult.batchId || "-" }}</el-tag>
          <el-tag type="info">Tổng dòng: {{ importResult.totalRows || 0 }}</el-tag>
          <el-tag type="success">Thành công: {{ importResult.successCount || 0 }}</el-tag>
          <el-tag type="danger">Thất bại: {{ importResult.failedCount || 0 }}</el-tag>
        </div>
        <BaseTable :data="importResult.results || []" border stripe size="small" table-layout="fixed" max-height="320">
          <el-table-column prop="rowNumber" label="Dòng" width="72" />
          <el-table-column prop="name" label="Tên sản phẩm" min-width="180" />
          <el-table-column label="Trạng thái" width="120">
            <template #default="{ row }">
              <el-tag :type="row.status === 'SUCCESS' ? 'success' : 'danger'">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="message" label="Chi tiết" min-width="220" show-overflow-tooltip />
        </BaseTable>
      </div>

      <template #footer>
        <el-button @click="importDialogVisible = false">Đóng</el-button>
        <el-button type="primary" :loading="importSubmitting" @click="submitImportXlsx">Import</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, onMounted, ref, watch } from "vue";
import { ElMessage } from "element-plus";
import { Plus, Search, Upload } from "@element-plus/icons-vue";
import { useProductStore } from "@/modules/product/store/productStore";
import { productApi } from "@/modules/product/api/productApi";
import api from "@/services/api";
import { useConfirmDialog } from "@/composables/useConfirmDialog";

const store = useProductStore();
const { confirm } = useConfirmDialog();

const loading = computed(() => store.loading);
const products = computed(() => store.products || []);
const totalElements = computed(() => Number(store.totalElements || 0));
const currentPage = computed(() => Number(store.page || 0) + 1);
const pageSize = ref(10);
const keyword = ref("");
const fallbackImage = "https://via.placeholder.com/64x64?text=IMG";
const productTableRef = ref(null);

const drawerVisible = ref(false);
const drawerMode = ref("create");
const submitting = ref(false);
const selectedIds = ref([]);
const deletedDrawerVisible = ref(false);
const deletedProducts = ref([]);
const deletedLoading = ref(false);
const confirmDialogPending = ref(false);
const restoringId = ref(null);
const inventoryDrawerVisible = ref(false);
const inventoryLoading = ref(false);
const lowStockItems = ref([]);
const inventoryLogs = ref([]);
const editingId = ref(null);
const categoryOptions = ref([]);
const sizeOptions = ref([]);
const colorOptions = ref([]);
const optionPanelVisible = ref(false);
const optionPanelType = ref("size");
const optionPanelValue = ref("");
const optionPanelVariantIndex = ref(null);
const optionPanelSubmitting = ref(false);
const importDialogVisible = ref(false);
const importSubmitting = ref(false);
const importFileList = ref([]);
const importResult = ref(null);
const importDryRun = ref(false);
const importUpsertBySku = ref(false);
let productFilterTimer = null;

const normalizeSkuToken = (value, fallback = "NA") => {
  const normalized = String(value || "")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toUpperCase()
    .replace(/[^A-Z0-9]+/g, "");
  return normalized || fallback;
};

const generateAutoSku = (variant, index) => {
  const nameToken = normalizeSkuToken(productForm.value?.name, "SP");
  const brandToken = normalizeSkuToken(productForm.value?.brand, "GEN");
  const sizeToken = normalizeSkuToken(variant?.size, "SZ");
  const colorToken = normalizeSkuToken(variant?.color, "CL");
  return `${nameToken}-${brandToken}-${sizeToken}-${colorToken}-${String(index + 1).padStart(2, "0")}`;
};

const createEmptyVariant = () => ({
  id: null,
  sku: "",
  autoSku: true,
  price: 0,
  stock: 0,
  weight: 0.2,
  size: sizeOptions.value[0] || "M",
  color: colorOptions.value[0] || ""
});

const productForm = ref({
  name: "",
  brand: "",
  description: "",
  status: "ACTIVE",
  categoryId: null,
  uploadFiles: [],
  variants: [createEmptyVariant()]
});

const readProductPrice = (product) => {
  if (Number.isFinite(Number(product?.minPrice))) return Number(product.minPrice);
  if (Number.isFinite(Number(product?.price))) return Number(product.price);

  const variantPrices = Array.isArray(product?.variants)
    ? product.variants.map((v) => Number(v?.price)).filter((v) => Number.isFinite(v))
    : [];

  return variantPrices.length ? Math.min(...variantPrices) : 0;
};

const readProductStock = (product) => {
  const variants = Array.isArray(product?.variants) ? product.variants : [];
  if (!variants.length) return 0;
  return variants.reduce((sum, v) => sum + (Number(v?.stock) || 0), 0);
};

const readStatusTag = (status) => {
  if (status === "ACTIVE") return "success";
  if (status === "INACTIVE") return "info";
  return "warning";
};

const filteredProducts = computed(() => products.value);

const formatPrice = (value) => {
  return new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(Number(value) || 0);
};

const formatDateTime = (value) => {
  if (!value) return "N/A";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "N/A";
  return date.toLocaleString("vi-VN");
};

const refreshProducts = async () => {
  await store.fetchProducts({
    size: pageSize.value,
    page: 0,
    sortBy: "id",
    direction: "desc",
    q: keyword.value?.trim() || undefined
  });
  selectedIds.value = [];
};

const openImportDialog = () => {
  importDialogVisible.value = true;
  importFileList.value = [];
  importResult.value = null;
  importDryRun.value = false;
  importUpsertBySku.value = false;
};

const downloadImportTemplate = async () => {
  try {
    const response = await productApi.downloadImportTemplate();
    const blob = new Blob([response.data], {
      type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    });
    const link = document.createElement("a");
    link.href = URL.createObjectURL(blob);
    link.download = "product-import-template.xlsx";
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(link.href);
  } catch (error) {
    console.error(error);
    ElMessage.error("Không tải được file mẫu");
  }
};

const submitImportXlsx = async () => {
  const firstFile = Array.isArray(importFileList.value) && importFileList.value.length
    ? importFileList.value[0]
    : null;
  const rawFile = firstFile?.raw instanceof File ? firstFile.raw : null;
  if (!rawFile) {
    ElMessage.warning("Vui lòng chọn file .xlsx");
    return;
  }

  importSubmitting.value = true;
  try {
    const { data } = await productApi.importProductsXlsx(rawFile, {
      dryRun: importDryRun.value,
      upsertBySku: importUpsertBySku.value
    });
    importResult.value = data || null;
    if (!importDryRun.value) {
      await refreshProducts();
    }
    if (Number(data?.failedCount || 0) === 0) {
      ElMessage.success(
        importDryRun.value
          ? `Validate thành công ${Number(data?.successCount || 0)} dòng`
          : `Import thành công ${Number(data?.successCount || 0)} sản phẩm`
      );
    } else {
      ElMessage.warning(
        `${importDryRun.value ? "Validate" : "Import"} xong: thành công ${Number(data?.successCount || 0)}, lỗi ${Number(data?.failedCount || 0)}`
      );
    }
  } catch (error) {
    console.error(error);
    ElMessage.error(error?.response?.data?.message || "Import XLSX thất bại");
  } finally {
    importSubmitting.value = false;
  }
};

const fetchPage = async (page = 0) => {
  await store.fetchProducts({
    size: pageSize.value,
    page,
    sortBy: "id",
    direction: "desc",
    q: keyword.value?.trim() || undefined
  });
};

const handlePageChange = async (nextPage) => {
  await fetchPage(Math.max(0, nextPage - 1));
};

const handleSelectionChange = (rows) => {
  selectedIds.value = Array.isArray(rows) ? rows.map((row) => row.id).filter(Boolean) : [];
};

const clearSelection = () => {
  selectedIds.value = [];
  productTableRef.value?.clearSelection?.();
};

const defaultFormValue = () => ({
  name: "",
  brand: "",
  description: "",
  status: "ACTIVE",
  categoryId: categoryOptions.value[0]?.id || null,
  uploadFiles: [],
  variants: [createEmptyVariant()]
});

const normalizeOptionValue = (value) => String(value || "").trim();

const mergeOptionValues = (sources) => {
  const map = new Map();
  sources.forEach((value) => {
    const normalized = normalizeOptionValue(value);
    if (!normalized) return;
    const key = normalized.toLowerCase();
    if (!map.has(key)) {
      map.set(key, normalized);
    }
  });
  return Array.from(map.values());
};

const upsertOptionValue = (targetOptions, rawValue, { toTop = false } = {}) => {
  const normalized = normalizeOptionValue(rawValue);
  if (!normalized) return null;

  const existing = Array.isArray(targetOptions.value) ? targetOptions.value : [];
  const rest = existing.filter((item) => item.toLowerCase() !== normalized.toLowerCase());
  const next = toTop ? [normalized, ...rest] : [...rest, normalized];
  targetOptions.value = mergeOptionValues(next);
  return normalized;
};

const onVariantSizeChange = (value) => {
  upsertOptionValue(sizeOptions, value);
};

const onVariantColorChange = (value) => {
  upsertOptionValue(colorOptions, value);
};

const openOptionPanel = (type, variantIndex) => {
  optionPanelType.value = type === "color" ? "color" : "size";
  optionPanelVariantIndex.value = Number.isInteger(variantIndex) ? variantIndex : null;
  optionPanelValue.value = "";
  optionPanelVisible.value = true;
};

const closeOptionPanel = () => {
  optionPanelVisible.value = false;
  optionPanelValue.value = "";
  optionPanelVariantIndex.value = null;
  optionPanelSubmitting.value = false;
};

const confirmCreateOption = async () => {
  const inputValue = normalizeOptionValue(optionPanelValue.value);
  if (!inputValue) {
    ElMessage.warning("Vui lòng nhập giá trị");
    return;
  }

  optionPanelSubmitting.value = true;
  try {
    const { data } = await productApi.createVariantOption(optionPanelType.value, inputValue);
    const savedValue = normalizeOptionValue(data?.value || inputValue);
    const targetOptions = optionPanelType.value === "size" ? sizeOptions : colorOptions;
    const added = upsertOptionValue(targetOptions, savedValue, { toTop: true });

    const variantIndex = optionPanelVariantIndex.value;
    if (variantIndex != null && productForm.value.variants?.[variantIndex]) {
      if (optionPanelType.value === "size") {
        productForm.value.variants[variantIndex].size = added;
      } else {
        productForm.value.variants[variantIndex].color = added;
      }
    }

    ElMessage.success(
      optionPanelType.value === "size"
        ? `Đã thêm size "${added}"`
        : `Đã thêm màu "${added}"`
    );
    closeOptionPanel();
  } catch (error) {
    console.error(error);
    ElMessage.error(error?.response?.data?.message || "Không thể thêm thuộc tính");
  } finally {
    optionPanelSubmitting.value = false;
  }
};

const mapToPayload = () => {
  const form = productForm.value;
  const existingImages = (Array.isArray(form.uploadFiles) ? form.uploadFiles : [])
    .filter((file) => typeof file?.url === "string" && file.url.trim() && !(file.raw instanceof File))
    .map((file, index) => ({ url: file.url.trim(), isMain: index === 0 }));

  return {
    name: form.name?.trim() || "",
    brand: form.brand?.trim() || "",
    description: form.description?.trim() || "",
    status: form.status || "ACTIVE",
    categoryId: Number(form.categoryId),
    variants: (Array.isArray(form.variants) ? form.variants : []).map((variant) => {
      const size = upsertOptionValue(sizeOptions, variant?.size);
      const color = upsertOptionValue(colorOptions, variant?.color);
      return {
        id: variant?.id ?? null,
        sku: String(variant?.sku || "").trim(),
        price: Number(variant?.price || 0),
        stock: Number(variant?.stock || 0),
        weight: Number(variant?.weight || 0),
        status: "ACTIVE",
        size: size || null,
        color: color || null
      };
    }),
    images: existingImages
  };
};

const addVariant = () => {
  const nextIndex = productForm.value.variants.length;
  const variant = createEmptyVariant();
  variant.sku = generateAutoSku(variant, nextIndex);
  productForm.value.variants.push(variant);
};

const removeVariant = (index) => {
  if (productForm.value.variants.length <= 1) return;
  productForm.value.variants.splice(index, 1);
};

const getSelectedFiles = () => {
  const files = Array.isArray(productForm.value.uploadFiles) ? productForm.value.uploadFiles : [];
  return files.map((f) => f.raw).filter((raw) => raw instanceof File);
};

const buildMultipartPayload = (payload, files) => {
  const formData = new FormData();
  formData.append("data", JSON.stringify(payload));
  files.forEach((file) => formData.append("files", file));
  return formData;
};

const onThumbError = (event) => {
  if (!event?.target) return;
  event.target.src = fallbackImage;
};

const loadCategories = async () => {
  try {
    const { data } = await api.get("/categories", {
      params: { page: 0, size: 100, sortBy: "name", direction: "asc" }
    });
    categoryOptions.value = data?.content || [];
    if (!productForm.value.categoryId && categoryOptions.value.length) {
      productForm.value.categoryId = categoryOptions.value[0].id;
    }
  } catch (error) {
    console.error(error);
    ElMessage.error("Không tải được danh mục");
  }
};

const loadVariantOptions = async () => {
  try {
    const [{ data: sizeData }, { data: colorData }] = await Promise.all([
      productApi.getVariantOptions("size"),
      productApi.getVariantOptions("color")
    ]);

    const backendSizes = Array.isArray(sizeData) ? sizeData : [];
    const backendColors = Array.isArray(colorData) ? colorData : [];
    const currentVariantSizes = (productForm.value?.variants || []).map((variant) => variant?.size);
    const currentVariantColors = (productForm.value?.variants || []).map((variant) => variant?.color);
    const currentProductSizes = (products.value || [])
      .flatMap((product) => product?.variants || [])
      .map((variant) => variant?.size);
    const currentProductColors = (products.value || [])
      .flatMap((product) => product?.variants || [])
      .map((variant) => variant?.color);

    sizeOptions.value = mergeOptionValues([
      ...sizeOptions.value,
      ...backendSizes,
      ...currentVariantSizes,
      ...currentProductSizes
    ]);
    colorOptions.value = mergeOptionValues([
      ...colorOptions.value,
      ...backendColors,
      ...currentVariantColors,
      ...currentProductColors
    ]);
  } catch (error) {
    console.error(error);
    ElMessage.error("Không tải được danh sách size/màu");
  }
};

const openEdit = (item) => {
  const itemVariants = Array.isArray(item.variants) && item.variants.length ? item.variants : [createEmptyVariant()];
  const fallbackCategoryId =
    categoryOptions.value.find((c) => c.name === item.categoryName)?.id || categoryOptions.value[0]?.id || null;

  drawerMode.value = "edit";
  editingId.value = item.id;
  productForm.value = {
    name: item.name || "",
    brand: item.brand || "",
    description: item.description || "",
    status: item.status || "ACTIVE",
    categoryId: item.categoryId || fallbackCategoryId,
    uploadFiles: Array.isArray(item.images)
      ? item.images
          .map((img, index) => ({
            name: `image-${index + 1}`,
            url: img?.url || "",
            status: "success"
          }))
          .filter((file) => file.url)
      : [],
    variants: itemVariants.map((variant, index) => ({
      id: variant?.id ?? null,
      sku: variant?.sku || `${(item.slug || "SKU").toUpperCase()}-${index + 1}`,
      autoSku: false,
      price: Number(variant?.price || 0),
      stock: Number(variant?.stock || 0),
      weight: Number(variant?.weight || 0.2),
      size: variant?.size || sizeOptions.value[0] || "M",
      color: variant?.color || colorOptions.value[0] || ""
    }))
  };
  closeOptionPanel();
  drawerVisible.value = true;
};

const openCreate = () => {
  drawerMode.value = "create";
  editingId.value = null;
  productForm.value = defaultFormValue();
  productForm.value.variants = productForm.value.variants.map((variant, index) => ({
    ...variant,
    autoSku: true,
    sku: generateAutoSku(variant, index)
  }));
  closeOptionPanel();
  drawerVisible.value = true;
};

const handleAutoSkuToggle = (index, value) => {
  const variant = productForm.value.variants?.[index];
  if (!variant) return;
  variant.autoSku = Boolean(value);
  if (variant.autoSku) {
    variant.sku = generateAutoSku(variant, index);
  }
};

const submitProduct = async () => {
  const payload = mapToPayload();
  const selectedFiles = getSelectedFiles();
  const hasExistingImages = payload.images.length > 0;
  if (drawerMode.value === "create" && !selectedFiles.length) {
    ElMessage.warning("Vui lòng chọn ít nhất 1 ảnh từ máy");
    return;
  }
  if (drawerMode.value === "edit" && !selectedFiles.length && !hasExistingImages) {
    ElMessage.warning("Sản phẩm cần tối thiểu 1 ảnh");
    return;
  }
  const hasInvalidVariant =
    !payload.variants.length || payload.variants.some((variant) => !variant.sku);
  if (!payload.name || !payload.categoryId || hasInvalidVariant) {
    ElMessage.warning("Vui lòng nhập tên, danh mục và SKU cho tất cả biến thể");
    return;
  }
  submitting.value = true;
  try {
    if (drawerMode.value === "create") {
      if (selectedFiles.length) {
        await productApi.createProduct(buildMultipartPayload(payload, selectedFiles));
      } else {
        await productApi.createProduct(payload);
      }
      ElMessage.success("Tạo sản phẩm thành công");
    } else if (editingId.value) {
      if (selectedFiles.length) {
        await productApi.updateProduct(editingId.value, buildMultipartPayload(payload, selectedFiles));
      } else {
        await productApi.updateProduct(editingId.value, payload);
      }
      ElMessage.success("Cập nhật sản phẩm thành công");
    }
    drawerVisible.value = false;
    await refreshProducts();
    await loadVariantOptions();
  } catch (error) {
    console.error(error);
    ElMessage.error(error?.response?.data?.message || "Lưu sản phẩm thất bại");
  } finally {
    submitting.value = false;
  }
};

const deleteItem = async (item) => {
  if (confirmDialogPending.value) return;
  confirmDialogPending.value = true;
  try {
    await confirm({
      title: "Xác nhận",
      message: `Bạn có chắc muốn xóa ${item.name || "sản phẩm này"}?`,
      confirmButtonText: "OK",
      cancelButtonText: "Cancel",
      onConfirm: async () => {
        await productApi.deleteProduct(item.id);
      }
    });
    ElMessage.success("Đã xóa sản phẩm");
    await refreshProducts();
    if (deletedDrawerVisible.value) {
      await loadDeletedProducts();
    }
  } catch (error) {
    if (error.message !== "cancel") {
      console.error(error);
      ElMessage.error("Xóa sản phẩm thất bại");
    }
  } finally {
    confirmDialogPending.value = false;
  }
};

const bulkSetStatus = async (status) => {
  if (!selectedIds.value.length) return;
  try {
    await productApi.bulkUpdateStatus(selectedIds.value, status);
    ElMessage.success(`Đã cập nhật trạng thái ${status}`);
    clearSelection();
    await refreshProducts();
  } catch (error) {
    console.error(error);
    ElMessage.error(error?.response?.data?.message || "Không thể cập nhật trạng thái hàng loạt");
  }
};

const bulkSoftDelete = async () => {
  if (!selectedIds.value.length || confirmDialogPending.value) return;
  confirmDialogPending.value = true;
  try {
    await confirm({
      title: "Xác nhận",
      message: `Bạn có chắc muốn xóa mềm ${selectedIds.value.length} sản phẩm đã chọn?`,
      confirmButtonText: "OK",
      cancelButtonText: "Cancel",
      onConfirm: async () => {
        await productApi.bulkDelete(selectedIds.value);
      }
    });
    ElMessage.success("Đã xóa mềm sản phẩm đã chọn");
    clearSelection();
    await refreshProducts();
    if (deletedDrawerVisible.value) {
      await loadDeletedProducts();
    }
  } catch (error) {
    if (error.message !== "cancel") {
      console.error(error);
      ElMessage.error(error?.response?.data?.message || "Không thể xóa hàng loạt");
    }
  } finally {
    confirmDialogPending.value = false;
  }
};

const loadDeletedProducts = async () => {
  deletedLoading.value = true;
  try {
    const { data } = await productApi.getDeletedProducts();
    deletedProducts.value = Array.isArray(data) ? data : [];
  } catch (error) {
    console.error(error);
    ElMessage.error(error?.response?.data?.message || "Không tải được sản phẩm đã xóa");
  } finally {
    deletedLoading.value = false;
  }
};

const openDeletedDrawer = async () => {
  deletedDrawerVisible.value = true;
  await loadDeletedProducts();
};

const loadInventoryAlerts = async () => {
  inventoryLoading.value = true;
  try {
    const { data } = await productApi.getInventoryAlerts(5);
    lowStockItems.value = Array.isArray(data) ? data : [];
  } catch (error) {
    console.error(error);
    ElMessage.error(error?.response?.data?.message || "Không tải được cảnh báo tồn kho");
  } finally {
    inventoryLoading.value = false;
  }
};

const loadInventoryLogs = async (variantId = null) => {
  try {
    const { data } = await productApi.getInventoryLogs(variantId);
    inventoryLogs.value = Array.isArray(data) ? data : [];
  } catch (error) {
    console.error(error);
    ElMessage.error(error?.response?.data?.message || "Không tải được lịch sử tồn kho");
  }
};

const openInventoryDrawer = async () => {
  inventoryDrawerVisible.value = true;
  await Promise.all([loadInventoryAlerts(), loadInventoryLogs()]);
};

const restoreDeletedProduct = async (item) => {
  try {
    await ElMessageBox.confirm(`Khôi phục sản phẩm "${item.name || item.id}"?`, "Xác nhận", {
      type: "info",
      confirmButtonText: "Khôi phục",
      cancelButtonText: "Hủy"
    });
    restoringId.value = item.id;
    await productApi.restoreProduct(item.id);
    ElMessage.success("Khôi phục sản phẩm thành công");
    await Promise.all([refreshProducts(), loadDeletedProducts()]);
  } catch (error) {
    if (error !== "cancel") {
      console.error(error);
      ElMessage.error(error?.response?.data?.message || "Khôi phục sản phẩm thất bại");
    }
  } finally {
    restoringId.value = null;
  }
};

onMounted(() => {
  loadCategories();
  loadVariantOptions();
  fetchPage(0);
});

watch(keyword, () => {
  if (productFilterTimer) clearTimeout(productFilterTimer);
  productFilterTimer = setTimeout(() => {
    fetchPage(0);
  }, 300);
});

watch(
  () => ({
    name: productForm.value.name,
    brand: productForm.value.brand,
    variants: (productForm.value.variants || []).map((variant) => ({
      size: variant.size,
      color: variant.color,
      autoSku: variant.autoSku
    }))
  }),
  () => {
    (productForm.value.variants || []).forEach((variant, index) => {
      if (variant.autoSku) {
        variant.sku = generateAutoSku(variant, index);
      }
    });
  },
  { deep: true }
);
</script>

<style scoped lang="scss">
.product-admin-page {
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: 100%;
  box-sizing: border-box;
}

.inventory-panel {
  border: 1px solid #dce1e7;
  padding: 10px;
  background: #fff;
  box-sizing: border-box;
}

.panel-header {
  margin-bottom: 10px;
}

.panel-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  flex-wrap: wrap;
}

.action-buttons {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.bulk-toolbar {
  margin-bottom: 8px;
  border: 1px dashed #cfd8e3;
  padding: 8px 10px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;

  p {
    margin: 0;
    color: #475569;
    font-size: 13px;
  }
}

.bulk-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.search-input {
  width: 280px;
}

.table-wrap {
  width: 100%;
}

.inventory-table {
  width: 100%;
}

.pagination-wrap {
  margin-top: 10px;
  display: flex;
  justify-content: flex-end;
}

.product-form-drawer {
  :deep(.el-drawer__body) {
    padding: 16px;
    overflow-y: auto;
    position: relative;
  }
}

.deleted-product-drawer {
  :deep(.el-drawer__body) {
    padding: 14px;
  }
}

.inventory-drawer {
  :deep(.el-drawer__body) {
    padding: 14px;
    overflow-y: auto;
  }
}

.deleted-drawer-head {
  margin-bottom: 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;

  p {
    margin: 0;
    font-size: 13px;
    color: #6b7280;
  }
}

.deleted-empty {
  border: 1px dashed #d1d5db;
  padding: 14px;
  text-align: center;
  color: #6b7280;
}

.deleted-list {
  display: grid;
  gap: 8px;
}

.inventory-log-box {
  margin-top: 14px;

  h4 {
    margin: 0 0 8px;
    font-size: 14px;
    font-weight: 700;
  }
}

.inventory-alert-wrap {
  width: 100%;
}

.deleted-card {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.deleted-main {
  min-width: 0;
}

.deleted-name {
  margin: 0 0 4px;
  font-size: 14px;
  font-weight: 700;
  color: #111827;
}

.deleted-meta {
  margin: 0;
  color: #6b7280;
  font-size: 12px;
  word-break: break-word;
}

.product-image-uploader {
  width: 100%;
}

.upload-tip {
  margin: 8px 0 0;
  font-size: 12px;
  color: #6b7280;
}

.variant-block {
  border: 1px solid #e5e7eb;
  background: #fafafa;
  padding: 12px;
  margin-top: 8px;

  h4 {
    margin: 0 0 10px;
    font-size: 13px;
    font-weight: 700;
    text-transform: uppercase;
    color: #374151;
  }
}

.variant-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 10px;
}

.variant-item {
  border: 1px solid #e5e7eb;
  background: #fff;
  border-radius: 8px;
  padding: 10px;
}

.variant-item + .variant-item {
  margin-top: 10px;
}

.variant-item-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;

  p {
    margin: 0;
    font-size: 12px;
    font-weight: 700;
    color: #4b5563;
    text-transform: uppercase;
  }
}

.sku-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
  align-items: center;
}

.inline-label {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.inline-plus-btn {
  width: 20px;
  height: 20px;
  min-height: 20px;
  min-width: 20px;
  padding: 0;
}

.option-side-panel-backdrop {
  position: absolute;
  inset: 0;
  background: rgba(17, 24, 39, 0.28);
  z-index: 10;
}

.option-side-panel {
  position: absolute;
  top: 0;
  right: 0;
  width: min(320px, 100%);
  height: 100%;
  background: #ffffff;
  border-left: 1px solid #e5e7eb;
  box-shadow: -10px 0 24px rgba(0, 0, 0, 0.14);
  z-index: 11;
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.option-side-panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;

  h5 {
    margin: 0;
    font-size: 14px;
    font-weight: 700;
  }
}

.option-side-panel-actions {
  margin-top: auto;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.option-panel-slide-enter-active,
.option-panel-slide-leave-active {
  transition: transform 0.22s ease;
}

.option-panel-slide-enter-from,
.option-panel-slide-leave-to {
  transform: translateX(100%);
}

.option-panel-fade-enter-active,
.option-panel-fade-leave-active {
  transition: opacity 0.2s ease;
}

.option-panel-fade-enter-from,
.option-panel-fade-leave-to {
  opacity: 0;
}

.product-cell {
  display: flex;
  align-items: center;
  gap: 10px;

  .thumb {
    width: 44px;
    height: 44px;
    object-fit: cover;
    border-radius: 4px;
    border: 1px solid #e5e7eb;
  }

  .name {
    margin: 0;
    font-weight: 600;
  }

  .meta {
    margin: 0;
    font-size: 12px;
    color: #6b7280;
  }
}

.product-stats {
  display: grid;
  gap: 3px;
}

.product-stats span {
  font-size: 12px;
  color: #334155;
}

.action-cell {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.import-guide {
  margin-bottom: 12px;
  color: #374151;
  font-size: 13px;
  line-height: 1.5;

  p {
    margin: 0 0 6px;
    font-weight: 600;
  }

  code {
    display: block;
    background: #f8fafc;
    border: 1px solid #e2e8f0;
    padding: 8px;
    white-space: normal;
    word-break: break-word;
  }
}

.import-toolbar {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 10px;
  flex-wrap: wrap;
}

.import-upload {
  margin-bottom: 12px;
}

.import-result {
  margin-top: 10px;
}

.import-result-summary {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
}

@media (max-width: 1100px) {
  .panel-actions {
    flex-direction: column;
    align-items: stretch;
  }
}

@media (max-width: 700px) {
  .panel-header {
    align-items: flex-start;
    flex-direction: column;
    gap: 10px;
  }

  .panel-actions {
    width: 100%;
    flex-direction: column;
    align-items: stretch;
  }

  .search-input {
    width: 100%;
  }

  .action-buttons {
    width: 100%;
    display: grid;
    grid-template-columns: 1fr 1fr;
  }

  .product-form-drawer {
    :deep(.el-drawer) {
      width: 100% !important;
      max-width: 100% !important;
    }
  }

  .inventory-drawer {
    :deep(.el-drawer) {
      width: 100% !important;
      max-width: 100% !important;
    }
  }
}
</style>

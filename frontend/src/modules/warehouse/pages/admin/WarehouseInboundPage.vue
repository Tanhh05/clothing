<template>
  <section class="warehouse-page">
    <section class="panel filter-panel">
      <div class="toolbar">
        <el-input
          v-model="filters.q"
          placeholder="Tìm theo mã phiếu hoặc nhà cung cấp"
          clearable
          class="search-input"
          @keyup.enter="handleSearch"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-date-picker
          v-model="filters.range"
          type="daterange"
          range-separator="~"
          start-placeholder="Từ ngày"
          end-placeholder="Đến ngày"
          format="DD/MM/YYYY"
          value-format="x"
          class="date-range"
        />
        <el-button :icon="Search" type="primary" @click="handleSearch">Lọc</el-button>
        <el-button :icon="RefreshRight" @click="resetFilters">Làm mới</el-button>
        <el-button type="primary" @click="openCreate">Tạo phiếu nhập</el-button>
      </div>
    </section>

    <section class="panel">
      <el-table
        v-loading="loading"
        :data="pageData.content"
        border
        stripe
        size="small"
        table-layout="fixed"
        empty-text="Chưa có phiếu nhập"
      >
        <el-table-column prop="code" label="Mã phiếu" min-width="130" />
        <el-table-column prop="supplier" label="Nhà cung cấp" min-width="180" />
        <el-table-column prop="createdAt" label="Ngày nhập" min-width="170">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column prop="itemCount" label="Số dòng" width="95" />
        <el-table-column prop="totalQuantity" label="Tổng SL" width="110">
          <template #default="{ row }">{{ formatNumber(row.totalQuantity) }}</template>
        </el-table-column>
        <el-table-column label="Tổng chi phí" min-width="150">
          <template #default="{ row }">{{ formatCurrency(row.totalCost) }}</template>
        </el-table-column>
        <el-table-column label="Thao tác" width="110" align="center">
          <template #default="{ row }">
            <el-button text type="primary" :icon="View" @click="openDetail(row.id)">Xem</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="page"
          layout="total, prev, pager, next"
          :total="pageData.totalElements"
          @current-change="fetchReceipts"
        />
      </div>
    </section>

    <el-drawer v-model="createDrawerVisible" title="Tạo phiếu nhập kho" direction="rtl" size="56%">
      <el-form label-position="top" class="create-form">
        <section class="create-meta-card">
          <div class="create-meta-grid">
            <el-form-item label="Mã phiếu">
              <el-input v-model="createForm.code" placeholder="VD: NK-2026-001">
                <template #append>
                  <el-button @click="generateCode">Tạo nhanh</el-button>
                </template>
              </el-input>
            </el-form-item>
            <el-form-item label="Nhà cung cấp">
              <el-input v-model="createForm.supplier" placeholder="Tên nhà cung cấp" />
            </el-form-item>
            <el-form-item label="Ngày nhập">
              <el-date-picker
                v-model="createForm.createdAt"
                type="datetime"
                style="width: 100%"
                format="DD/MM/YYYY HH:mm:ss"
              />
            </el-form-item>
          </div>
        </section>

        <div class="item-head">
          <h4>Dòng nhập kho</h4>
          <div class="line-actions">
            <el-button size="small" @click="addLine">+ Thêm dòng</el-button>
            <el-button
              size="small"
              plain
              @click="createForm.items = [{ sku: '', quantity: 1, cost: 0 }]"
            >
              Reset dòng
            </el-button>
          </div>
        </div>

        <div class="line-table">
          <div v-for="(line, index) in createForm.items" :key="index" class="line-row">
            <div class="line-index">#{{ index + 1 }}</div>
            <div class="line-col sku">
              <label>SKU</label>
              <el-select
                v-model="line.sku"
                placeholder="Nhập hoặc chọn SKU"
                filterable
                remote
                reserve-keyword
                clearable
                :remote-method="handleSkuQuery"
                @visible-change="handleSkuVisibleChange"
              >
                <el-option
                  v-for="sku in visibleSkuSuggestions"
                  :key="sku"
                  :label="sku"
                  :value="sku"
                />
              </el-select>
            </div>
            <div class="line-col qty">
              <label>Số lượng</label>
              <el-input-number v-model="line.quantity" :min="1" :step="1" style="width: 100%" />
            </div>
            <div class="line-col cost">
              <label>Đơn giá</label>
              <el-input-number v-model="line.cost" :min="0" :step="1000" style="width: 100%" />
            </div>
            <div class="line-col total">
              <label>Thành tiền</label>
              <div class="line-total">{{ formatCurrency(calcLineTotal(line)) }}</div>
            </div>
            <div class="line-col action">
              <el-button text type="danger" :disabled="createForm.items.length <= 1" @click="removeLine(index)">
                Xóa
              </el-button>
            </div>
          </div>
        </div>

        <div class="draft-summary">
          <div class="summary-item">
            <p>Dòng nhập</p>
            <strong>{{ createForm.items.length }}</strong>
          </div>
          <div class="summary-item">
            <p>Tổng số lượng</p>
            <strong>{{ formatNumber(draftTotals.totalQuantity) }}</strong>
          </div>
          <div class="summary-item">
            <p>Tổng chi phí</p>
            <strong>{{ formatCurrency(draftTotals.totalCost) }}</strong>
          </div>
        </div>
      </el-form>

      <template #footer>
        <div class="drawer-footer">
          <el-button @click="createDrawerVisible = false">Hủy</el-button>
          <el-button type="primary" :loading="creating" @click="saveReceipt">Lưu phiếu</el-button>
        </div>
      </template>
    </el-drawer>

    <el-drawer v-model="detailDrawerVisible" title="Chi tiết phiếu nhập" direction="rtl" size="52%">
      <template v-if="detailLoading">
        <el-skeleton :rows="6" animated />
      </template>
      <template v-else-if="detailData">
        <section class="detail-overview">
          <div class="meta-block">
            <p class="meta-key">Mã phiếu</p>
            <h4>{{ detailData.code }}</h4>
          </div>
          <div class="meta-block">
            <p class="meta-key">Nhà cung cấp</p>
            <h4>{{ detailData.supplier }}</h4>
          </div>
          <div class="meta-block">
            <p class="meta-key">Ngày nhập</p>
            <h4>{{ formatDateTime(detailData.createdAt) }}</h4>
          </div>
        </section>

        <section class="detail-stats">
          <article class="mini-stat">
            <p>Số dòng</p>
            <h5>{{ detailData.itemCount || 0 }}</h5>
          </article>
          <article class="mini-stat">
            <p>Tổng số lượng</p>
            <h5>{{ formatNumber(detailData.totalQuantity) }}</h5>
          </article>
          <article class="mini-stat">
            <p>Tổng chi phí</p>
            <h5>{{ formatCurrency(detailData.totalCost) }}</h5>
          </article>
        </section>

        <el-table :data="detailData.items || []" border stripe size="small" table-layout="fixed">
          <el-table-column label="#" width="56">
            <template #default="{ $index }">{{ $index + 1 }}</template>
          </el-table-column>
          <el-table-column prop="sku" label="SKU" min-width="140" />
          <el-table-column prop="quantity" label="SL nhập" width="100" />
          <el-table-column prop="unitCost" label="Đơn giá" min-width="140">
            <template #default="{ row }">{{ formatCurrency(row.unitCost) }}</template>
          </el-table-column>
          <el-table-column prop="lineTotal" label="Thành tiền" min-width="140">
            <template #default="{ row }">{{ formatCurrency(row.lineTotal) }}</template>
          </el-table-column>
          <el-table-column prop="currentStock" label="Tồn hiện tại" width="120" align="right" />
        </el-table>
      </template>
      <template v-else>
        <el-empty description="Không có dữ liệu chi tiết" />
      </template>
    </el-drawer>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { RefreshRight, Search, View } from "@element-plus/icons-vue";
import { warehouseApi } from "@/modules/warehouse/api/warehouseApi";

const loading = ref(false);
const creating = ref(false);

const page = ref(1);
const size = ref(10);
const pageData = reactive({
  content: [],
  totalElements: 0
});

const filters = reactive({
  q: "",
  range: []
});

const createDrawerVisible = ref(false);
const detailDrawerVisible = ref(false);
const detailLoading = ref(false);
const detailData = ref(null);
const skuSuggestions = ref([]);
const skuKeyword = ref("");

const defaultCreateForm = () => ({
  code: "",
  supplier: "",
  createdAt: new Date(),
  items: [{ sku: "", quantity: 1, cost: 0 }]
});

const createForm = ref(defaultCreateForm());

const draftTotals = computed(() => {
  const lines = Array.isArray(createForm.value.items) ? createForm.value.items : [];
  const totalQuantity = lines.reduce((sum, item) => sum + Number(item.quantity || 0), 0);
  const totalCost = lines.reduce((sum, item) => sum + (Number(item.quantity || 0) * Number(item.cost || 0)), 0);
  return { totalQuantity, totalCost };
});

const formatCurrency = (value) =>
  new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(Number(value) || 0);
const formatNumber = (value) => new Intl.NumberFormat("vi-VN").format(Number(value) || 0);
const formatDateTime = (value) => (value ? new Date(value).toLocaleString("vi-VN") : "N/A");
const calcLineTotal = (line) => Number(line?.quantity || 0) * Number(line?.cost || 0);

const toLocalDateTime = (value) => {
  if (!value) return null;
  const date = new Date(value);
  const yyyy = date.getFullYear();
  const mm = String(date.getMonth() + 1).padStart(2, "0");
  const dd = String(date.getDate()).padStart(2, "0");
  const hh = String(date.getHours()).padStart(2, "0");
  const mi = String(date.getMinutes()).padStart(2, "0");
  const ss = String(date.getSeconds()).padStart(2, "0");
  return `${yyyy}-${mm}-${dd}T${hh}:${mi}:${ss}`;
};

const dateRangeToParams = () => {
  if (!Array.isArray(filters.range) || filters.range.length !== 2) {
    return { from: undefined, to: undefined };
  }
  const start = new Date(Number(filters.range[0]));
  const end = new Date(Number(filters.range[1]));
  start.setHours(0, 0, 0, 0);
  end.setHours(23, 59, 59, 999);
  return {
    from: toLocalDateTime(start),
    to: toLocalDateTime(end)
  };
};

const fetchReceipts = async () => {
  loading.value = true;
  try {
    const { from, to } = dateRangeToParams();
    const { data } = await warehouseApi.getInboundReceiptPage({
      page: Math.max(0, page.value - 1),
      size: size.value,
      sortBy: "id",
      direction: "desc",
      q: filters.q?.trim() || undefined,
      from,
      to
    });
    pageData.content = Array.isArray(data?.content) ? data.content : [];
    pageData.totalElements = Number(data?.totalElements || 0);
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || "Không tải được danh sách phiếu nhập");
  } finally {
    loading.value = false;
  }
};

const preloadSkuSuggestions = async (keyword = "") => {
  try {
    const { data } = await warehouseApi.getSkuSuggestions(keyword);
    skuSuggestions.value = Array.isArray(data) ? data.filter(Boolean) : [];
  } catch (error) {
    skuSuggestions.value = [];
  }
};

const filteredSkuSuggestions = computed(() => {
  const lower = skuKeyword.value.toLowerCase();
  return skuSuggestions.value.filter((sku) =>
    lower ? String(sku).toLowerCase().includes(lower) : true
  );
});

const visibleSkuSuggestions = computed(() => filteredSkuSuggestions.value);

const handleSkuQuery = async (queryString) => {
  const keyword = String(queryString || "").trim();
  skuKeyword.value = keyword;
  if (!skuSuggestions.value.length || keyword.length >= 2) {
    await preloadSkuSuggestions(keyword);
  }
};

const handleSkuVisibleChange = async (visible) => {
  if (!visible) return;
  skuKeyword.value = "";
  if (!skuSuggestions.value.length) {
    await preloadSkuSuggestions("");
  }
};

const handleSearch = async () => {
  page.value = 1;
  await fetchReceipts();
};

const resetFilters = async () => {
  filters.q = "";
  filters.range = [];
  page.value = 1;
  await fetchReceipts();
};

const generateCode = () => {
  const now = new Date();
  const y = now.getFullYear();
  const m = String(now.getMonth() + 1).padStart(2, "0");
  const d = String(now.getDate()).padStart(2, "0");
  const s = String(now.getSeconds()).padStart(2, "0");
  createForm.value.code = `NK-${y}${m}${d}-${s}`;
};

const openCreate = () => {
  createForm.value = defaultCreateForm();
  createDrawerVisible.value = true;
};

const addLine = () => {
  createForm.value.items.push({
    sku: "",
    quantity: 1,
    cost: 0
  });
};

const checkSkuExists = async (sku) => {
  try {
    const { data } = await warehouseApi.getSkuSuggestions(sku);
    const list = Array.isArray(data) ? data : [];
    const target = String(sku || "").trim().toUpperCase();
    return list.some((item) => String(item || "").trim().toUpperCase() === target);
  } catch (error) {
    return false;
  }
};

const removeLine = (index) => {
  if (createForm.value.items.length <= 1) {
    ElMessage.warning("Phiếu nhập cần ít nhất 1 dòng SKU");
    return;
  }
  createForm.value.items.splice(index, 1);
};

const validateCreatePayload = () => {
  const code = String(createForm.value.code || "").trim().toUpperCase();
  const supplier = String(createForm.value.supplier || "").trim();
  if (!code) {
    ElMessage.warning("Vui lòng nhập mã phiếu");
    return null;
  }
  if (!supplier) {
    ElMessage.warning("Vui lòng nhập nhà cung cấp");
    return null;
  }
  if (!createForm.value.createdAt) {
    ElMessage.warning("Vui lòng chọn ngày nhập");
    return null;
  }

  const normalized = createForm.value.items
    .map((item) => ({
      sku: String(item.sku || "").trim().toUpperCase(),
      quantity: Number(item.quantity || 0),
      cost: Number(item.cost || 0)
    }))
    .filter((item) => item.sku);

  if (!normalized.length) {
    ElMessage.warning("Vui lòng nhập ít nhất 1 SKU hợp lệ");
    return null;
  }

  const skuSet = new Set();
  for (const item of normalized) {
    if (skuSet.has(item.sku)) {
      ElMessage.warning(`SKU bị trùng trong phiếu: ${item.sku}`);
      return null;
    }
    skuSet.add(item.sku);
    if (item.quantity <= 0) {
      ElMessage.warning(`Số lượng phải > 0: ${item.sku}`);
      return null;
    }
    if (item.cost < 0) {
      ElMessage.warning(`Đơn giá phải >= 0: ${item.sku}`);
      return null;
    }
  }

  return {
    code,
    supplier,
    createdAt: toLocalDateTime(createForm.value.createdAt),
    items: normalized
  };
};

const saveReceipt = async () => {
  const payload = validateCreatePayload();
  if (!payload) return;

  creating.value = true;
  try {
    const uniqueSkus = [...new Set(payload.items.map((item) => item.sku))];
    const skuChecks = await Promise.all(uniqueSkus.map((sku) => checkSkuExists(sku)));
    const missingSkus = uniqueSkus.filter((_, index) => !skuChecks[index]);
    if (missingSkus.length) {
      ElMessage.error(`SKU chưa tồn tại trong hệ thống: ${missingSkus.join(", ")}`);
      return;
    }

    await warehouseApi.createInboundReceipt(payload);
    createDrawerVisible.value = false;
    ElMessage.success("Đã tạo phiếu nhập kho");
    await fetchReceipts();
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || "Tạo phiếu nhập thất bại");
  } finally {
    creating.value = false;
  }
};

const openDetail = async (id) => {
  detailDrawerVisible.value = true;
  detailLoading.value = true;
  detailData.value = null;
  try {
    const { data } = await warehouseApi.getInboundReceiptById(id);
    detailData.value = data || null;
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || "Không tải được chi tiết phiếu nhập");
  } finally {
    detailLoading.value = false;
  }
};

onMounted(async () => {
  await Promise.all([fetchReceipts(), preloadSkuSuggestions("")]);
});
</script>

<style scoped lang="scss">
.warehouse-page { display: flex; flex-direction: column; gap: 10px; }

.panel { border: 1px solid #dce1e7; background: #fff; padding: 10px; }
.filter-panel { padding-bottom: 10px; }

.toolbar { display: grid; grid-template-columns: 1.6fr 1fr auto auto auto; gap: 8px; align-items: center; }
.search-input, .date-range { width: 100%; }
.pagination-wrap { margin-top: 10px; display: flex; justify-content: flex-end; }

.create-form { display: flex; flex-direction: column; gap: 12px; }
.create-meta-card { border: 1px solid #e5e7eb; background: #fbfdff; border-radius: 10px; padding: 12px; }
.create-meta-grid { display: grid; grid-template-columns: 1.1fr 1fr 1fr; gap: 10px 12px; align-items: end; }
.item-head { margin: 2px 0 0; display: flex; justify-content: space-between; align-items: center; }
.item-head h4 { margin: 0; font-size: 13px; text-transform: uppercase; }
.line-actions { display: flex; gap: 8px; }

.line-table { display: flex; flex-direction: column; gap: 8px; }
.line-row { border: 1px solid #e5e7eb; border-radius: 8px; padding: 10px; display: grid; grid-template-columns: auto minmax(220px, 2.2fr) 1fr 1fr 1.1fr auto; gap: 8px; align-items: end; background: #fff; }
.line-index { width: 30px; height: 30px; border-radius: 999px; background: #f3f4f6; color: #4b5563; display: flex; align-items: center; justify-content: center; font-size: 12px; font-weight: 700; }
.line-col { display: flex; flex-direction: column; gap: 6px; }
.line-col label { font-size: 12px; color: #6b7280; font-weight: 600; text-transform: uppercase; letter-spacing: 0.4px; }
.line-col.action { align-items: flex-end; justify-content: flex-end; padding-bottom: 2px; }
.line-total { height: 32px; display: flex; align-items: center; font-weight: 700; color: #111827; background: #f8fafc; border: 1px dashed #d1d5db; border-radius: 6px; padding: 0 10px; }
.draft-summary { margin-top: 6px; display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 10px; background: #f8fafc; border: 1px solid #e5e7eb; border-radius: 8px; padding: 10px 12px; }
.summary-item p { margin: 0; font-size: 11px; text-transform: uppercase; color: #6b7280; letter-spacing: 0.4px; }
.summary-item strong { margin-top: 4px; display: block; font-size: 16px; color: #111827; }
.drawer-footer { display: flex; justify-content: flex-end; gap: 10px; width: 100%; }

.detail-overview { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 10px; margin-bottom: 10px; }
.meta-block { border: 1px solid #e5e7eb; border-radius: 10px; background: #fff; padding: 10px 12px; }
.meta-key { margin: 0; font-size: 11px; text-transform: uppercase; letter-spacing: 0.5px; color: #6b7280; }
.meta-block h4 { margin: 6px 0 0; font-size: 14px; color: #111827; line-height: 1.3; }
.detail-stats { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 10px; margin-bottom: 12px; }
.mini-stat { border: 1px solid #dbe4f0; background: linear-gradient(180deg, #f8fbff 0%, #f5f8fd 100%); border-radius: 10px; padding: 10px 12px; }
.mini-stat p { margin: 0; font-size: 11px; text-transform: uppercase; color: #6b7280; letter-spacing: 0.5px; }
.mini-stat h5 { margin: 6px 0 0; font-size: 17px; }

@media (max-width: 1000px) {
  .toolbar { grid-template-columns: 1fr; }
  .pagination-wrap { justify-content: center; }
  .create-meta-grid { grid-template-columns: 1fr; }
  .line-actions { width: 100%; }
  .line-actions .el-button { flex: 1; }
  .line-row { grid-template-columns: 1fr; }
  .line-index { width: 26px; height: 26px; }
  .line-col.action { align-items: flex-start; }
  .draft-summary { grid-template-columns: 1fr; gap: 8px; }
  .detail-overview { grid-template-columns: 1fr; }
  .detail-stats { grid-template-columns: 1fr; }
}
</style>

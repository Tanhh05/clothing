<template>
  <section class="warehouse-page">
    <header class="head">
      <div>
        <p class="eyebrow">Admin panel</p>
        <h2>Warehouse Inbound</h2>
        <p class="sub-text">Quản lý phiếu nhập kho, kiểm soát số lượng và chi phí nhập.</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreate">Tạo phiếu nhập</el-button>
    </header>

    <section class="stats-grid">
      <article class="stat-card">
        <p class="stat-label">Số phiếu trên trang</p>
        <h3>{{ pageData.content.length }}</h3>
      </article>
      <article class="stat-card">
        <p class="stat-label">Tổng dòng nhập</p>
        <h3>{{ stats.totalLines }}</h3>
      </article>
      <article class="stat-card">
        <p class="stat-label">Tổng SL nhập</p>
        <h3>{{ formatNumber(stats.totalQuantity) }}</h3>
      </article>
      <article class="stat-card">
        <p class="stat-label">Tổng chi phí</p>
        <h3>{{ formatCurrency(stats.totalCost) }}</h3>
      </article>
    </section>

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
      </div>
    </section>

    <section class="panel">
      <el-table v-loading="loading" :data="pageData.content" border stripe empty-text="Chưa có phiếu nhập">
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
          v-model:page-size="size"
          layout="total, prev, pager, next, sizes"
          :total="pageData.totalElements"
          :page-sizes="[10, 20, 50]"
          @current-change="fetchReceipts"
          @size-change="handleSizeChange"
        />
      </div>
    </section>

    <el-drawer v-model="createDrawerVisible" title="Tạo phiếu nhập kho" direction="rtl" size="44%">
      <el-form label-position="top">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="Mã phiếu">
              <el-input v-model="createForm.code" placeholder="VD: NK-2026-001" />
            </el-form-item>
          </el-col>
          <el-col :span="12" class="align-end">
            <el-button @click="generateCode">Tạo mã nhanh</el-button>
          </el-col>
        </el-row>

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

        <div class="item-head">
          <h4>Danh sách SKU nhập</h4>
          <el-button size="small" @click="addLine">+ Dòng</el-button>
        </div>

        <article v-for="(line, index) in createForm.items" :key="index" class="line-item">
          <el-row :gutter="10">
            <el-col :span="11">
              <el-input v-model="line.sku" placeholder="SKU" />
            </el-col>
            <el-col :span="5">
              <el-input-number v-model="line.quantity" :min="1" :step="1" style="width: 100%" />
            </el-col>
            <el-col :span="6">
              <el-input-number v-model="line.cost" :min="0" :step="1000" style="width: 100%" />
            </el-col>
            <el-col :span="2">
              <el-button text type="danger" @click="removeLine(index)">X</el-button>
            </el-col>
          </el-row>
        </article>

        <div class="draft-summary">
          <span>Dòng: {{ createForm.items.length }}</span>
          <span>SL: {{ formatNumber(draftTotals.totalQuantity) }}</span>
          <span>Chi phí: {{ formatCurrency(draftTotals.totalCost) }}</span>
        </div>
      </el-form>

      <template #footer>
        <el-button @click="createDrawerVisible = false">Hủy</el-button>
        <el-button type="primary" :loading="creating" @click="saveReceipt">Lưu phiếu</el-button>
      </template>
    </el-drawer>

    <el-drawer v-model="detailDrawerVisible" title="Chi tiết phiếu nhập" direction="rtl" size="46%">
      <template v-if="detailLoading">
        <el-skeleton :rows="6" animated />
      </template>
      <template v-else-if="detailData">
        <div class="detail-meta">
          <p><strong>Mã phiếu:</strong> {{ detailData.code }}</p>
          <p><strong>Nhà cung cấp:</strong> {{ detailData.supplier }}</p>
          <p><strong>Ngày nhập:</strong> {{ formatDateTime(detailData.createdAt) }}</p>
          <p><strong>Số dòng:</strong> {{ detailData.itemCount }}</p>
          <p><strong>Tổng SL:</strong> {{ formatNumber(detailData.totalQuantity) }}</p>
          <p><strong>Tổng chi phí:</strong> {{ formatCurrency(detailData.totalCost) }}</p>
        </div>
        <el-table :data="detailData.items || []" border stripe>
          <el-table-column prop="sku" label="SKU" min-width="140" />
          <el-table-column prop="quantity" label="SL nhập" width="100" />
          <el-table-column prop="unitCost" label="Đơn giá" min-width="140">
            <template #default="{ row }">{{ formatCurrency(row.unitCost) }}</template>
          </el-table-column>
          <el-table-column prop="lineTotal" label="Thành tiền" min-width="140">
            <template #default="{ row }">{{ formatCurrency(row.lineTotal) }}</template>
          </el-table-column>
          <el-table-column prop="currentStock" label="Tồn hiện tại" width="120" />
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
import { Plus, RefreshRight, Search, View } from "@element-plus/icons-vue";
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

const defaultCreateForm = () => ({
  code: "",
  supplier: "",
  createdAt: new Date(),
  items: [{ sku: "", quantity: 1, cost: 0 }]
});

const createForm = ref(defaultCreateForm());

const stats = computed(() => {
  const totalLines = pageData.content.reduce((sum, item) => sum + Number(item.itemCount || 0), 0);
  const totalQuantity = pageData.content.reduce((sum, item) => sum + Number(item.totalQuantity || 0), 0);
  const totalCost = pageData.content.reduce((sum, item) => sum + Number(item.totalCost || 0), 0);
  return { totalLines, totalQuantity, totalCost };
});

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

const handleSizeChange = async (nextSize) => {
  size.value = nextSize;
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
  createForm.value.items.push({ sku: "", quantity: 1, cost: 0 });
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

onMounted(fetchReceipts);
</script>

<style scoped lang="scss">
.warehouse-page { display: flex; flex-direction: column; gap: 16px; }
.head { padding: 16px; border: 1px solid #dce1e7; background: #fbfbfc; display: flex; justify-content: space-between; align-items: center; gap: 12px; }
.head h2 { margin: 0; font-size: 24px; font-weight: 800; text-transform: uppercase; }
.eyebrow { margin: 0 0 6px; text-transform: uppercase; letter-spacing: 1px; font-size: 11px; color: #6b7280; }
.sub-text { margin: 6px 0 0; color: #6b7280; font-size: 13px; }

.panel { border: 1px solid #dce1e7; background: #fff; padding: 14px; }
.filter-panel { padding-bottom: 10px; }

.stats-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; }
.stat-card { border: 1px solid #dce1e7; background: #fff; padding: 14px; }
.stat-label { margin: 0; font-size: 12px; color: #6b7280; text-transform: uppercase; }
.stat-card h3 { margin: 8px 0 0; font-size: 20px; }

.toolbar { display: grid; grid-template-columns: 1.6fr 1fr auto auto; gap: 8px; align-items: center; }
.search-input, .date-range { width: 100%; }
.pagination-wrap { margin-top: 14px; display: flex; justify-content: flex-end; }

.align-end { display: flex; align-items: flex-end; justify-content: flex-end; }
.item-head { margin: 12px 0 8px; display: flex; justify-content: space-between; align-items: center; }
.item-head h4 { margin: 0; font-size: 13px; text-transform: uppercase; }
.line-item { border: 1px solid #e5e7eb; padding: 10px; margin-bottom: 8px; border-radius: 8px; }
.draft-summary { margin-top: 10px; display: flex; gap: 16px; font-size: 13px; color: #111827; }
.detail-meta { margin-bottom: 12px; display: grid; gap: 6px; }
.detail-meta p { margin: 0; }

@media (max-width: 1000px) {
  .stats-grid { grid-template-columns: 1fr 1fr; }
  .toolbar { grid-template-columns: 1fr; }
  .pagination-wrap { justify-content: center; }
}
</style>

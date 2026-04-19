<template>
  <section class="admin-orders-page admin-page-shell" v-loading="loading">
    <div class="panel">
      <div class="panel-head">
        <div class="filters-row">
          <el-input v-model="keyword" placeholder="Tìm theo mã đơn / địa chỉ" clearable class="search-input" />
          <el-select v-model="statusFilter" clearable placeholder="Trạng thái GHN" class="status-filter">
            <el-option v-for="item in statusFilterOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="→"
            start-placeholder="Từ ngày"
            end-placeholder="Đến ngày"
            format="DD/MM/YYYY"
            value-format="YYYY-MM-DD"
            class="date-filter"
          />
          <el-button @click="clearFilters">Xóa lọc</el-button>
        </div>
        <div class="head-actions">
          <el-button @click="fetchOrders">Làm mới</el-button>
          <el-button plain :disabled="!selectedOrderIds.length" @click="exportSelectedInvoices">
            Xuất hóa đơn
          </el-button>
        </div>
      </div>

      <div class="table-wrap">
        <BaseTable
          ref="orderTableRef"
          :data="orders"
          border
          stripe
          size="small"
          class="orders-table"
          empty-text="Chưa có đơn hàng"
          table-layout="fixed"
          @selection-change="handleOrderSelectionChange"
        >
          <el-table-column type="selection" width="48" />
          <el-table-column label="Đơn hàng" min-width="220">
            <template #default="{ row }">
              <div class="order-cell">
                <strong>#{{ row.id }}</strong>
                <span>{{ customerDisplay(row) }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="Tổng quan" min-width="270">
            <template #default="{ row }">
              <div class="summary-cell">
                <span><strong>TT:</strong> {{ paymentLabel(row.paymentMethod) }}</span>
                <span><strong>Tổng:</strong> {{ formatCurrency(row.totalPrice) }}</span>
                <el-tag size="small" :type="statusTagType(row.shippingStatus)">{{ shippingStatusLabel(row.shippingStatus) }}</el-tag>
                <span><strong>GHN:</strong> {{ row.shippingCode || "Chưa có mã" }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="Ngày tạo" width="160">
            <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="Thao tác" width="110">
            <template #default="{ row }">
              <div class="action-cell">
                <el-button size="small" @click="openDetails(row)">Chi tiết</el-button>
              </div>
            </template>
          </el-table-column>
        </BaseTable>
      </div>
      <div class="pagination-wrap">
        <el-pagination
          layout="total, prev, pager, next"
          :total="totalElements"
          :current-page="page + 1"
          :page-size="size"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <el-drawer
      v-model="detailsVisible"
      title="Chi tiết đơn hàng"
      direction="rtl"
      size="48%"
      :with-header="true"
      class="order-detail-drawer"
    >
      <template v-if="selectedOrder">
        <div class="detail-layout">
          <section class="detail-card detail-meta">
            <h4 class="card-title">Thông tin đơn</h4>
            <div class="meta-grid">
              <p><strong>Mã đơn:</strong> #{{ selectedOrder.id }}</p>
              <p><strong>Khách hàng:</strong> {{ customerDisplay(selectedOrder) }}</p>
              <p><strong>Địa chỉ:</strong> {{ selectedOrder.address || "N/A" }}</p>
              <p><strong>Tổng tiền:</strong> {{ formatCurrency(selectedOrder.totalPrice) }}</p>
              <p><strong>Mã vận đơn GHN:</strong> {{ selectedOrder.shippingCode || "Chưa có" }}</p>
              <p><strong>Trạng thái GHN:</strong> {{ shippingStatusLabel(selectedOrder.shippingStatus) }}</p>
            </div>
          </section>

          <section class="detail-card status-box">
            <h4 class="card-title">Trạng thái</h4>
            <p><strong>Trạng thái hệ thống:</strong> <el-tag>{{ statusLabel(selectedOrder.status) }}</el-tag></p>
            <p><strong>Trạng thái GHN:</strong> <el-tag type="info">{{ shippingStatusLabel(selectedOrder.shippingStatus) }}</el-tag></p>
          </section>

          <section class="detail-card">
            <h4 class="card-title">Sản phẩm trong đơn</h4>
            <BaseTable :data="selectedOrder.items || []" border stripe size="small" table-layout="fixed" empty-text="Không có sản phẩm">
              <el-table-column prop="productName" label="Sản phẩm" min-width="220" />
              <el-table-column prop="sku" label="SKU" width="130" />
              <el-table-column prop="quantity" label="SL" width="80" />
              <el-table-column label="Đơn giá" width="130">
                <template #default="{ row }">{{ formatCurrency(row.price) }}</template>
              </el-table-column>
              <el-table-column label="Thành tiền" width="140">
                <template #default="{ row }">{{ formatCurrency(row.lineTotal) }}</template>
              </el-table-column>
            </BaseTable>
          </section>

          <section class="detail-card history-box">
            <h4 class="card-title">Lịch sử trạng thái</h4>
            <el-timeline>
              <el-timeline-item
                v-for="(step, idx) in sortedHistory(selectedOrder.statusHistory)"
                :key="`${selectedOrder.id}-${idx}-${step.status}`"
                :timestamp="formatDateTime(step.changedAt)"
                placement="top"
              >
                {{ statusLabel(step.status) }}
              </el-timeline-item>
            </el-timeline>
          </section>
        </div>
      </template>
    </el-drawer>
  </section>
</template>

<script setup>
import { onMounted, ref, watch } from "vue";
import { ElMessage } from "element-plus";
import { useConfirmDialog } from "@/composables/useConfirmDialog";
import { orderApi } from "@/modules/order/api/orderApi";

const loading = ref(false);
const { confirm } = useConfirmDialog();
const orders = ref([]);
const totalElements = ref(0);
const keyword = ref("");
const statusFilter = ref("");
const dateRange = ref([]);
const detailsVisible = ref(false);
const selectedOrder = ref(null);
const page = ref(0);
const size = ref(10);
const orderTableRef = ref(null);
const selectedOrderIds = ref([]);
const ghnShippingStatusOptions = ref([]);
let filterTimer = null;

const statusFilterOptions = ref([]);

const normalizeStatus = (status) => String(status || "").trim().toUpperCase();

const formatCurrency = (amount) => {
  return new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(amount || 0);
};

const formatDateTime = (value) => {
  if (!value) return "N/A";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString("vi-VN");
};

const paymentLabel = (method) => {
  const value = String(method || "").toUpperCase();
  if (value === "COD") return "COD";
  if (value === "MOMO") return "MoMo";
  return method || "N/A";
};

const customerDisplay = (order) => {
  return order?.customerName || `User #${order?.userId ?? "N/A"}`;
};

const INTERNAL_STATUS_LABELS = {
  PENDING: "Chờ xử lý",
  PROCESSING: "Đang xử lý",
  CONFIRMED: "Đã xác nhận",
  SHIPPED: "Đang giao",
  DELIVERED: "Đã giao",
  CANCELLED: "Đã hủy",
  FAILED: "Thất bại",
  FAILED_INSUFFICIENT_STOCK: "Thiếu tồn kho",
  FAILED_DELIVERY: "Giao thất bại",
  RETURN_REQUESTED: "Yêu cầu trả hàng",
  REFUNDED: "Đã hoàn tiền",
  ON_HOLD: "Tạm giữ"
};

const GHN_STATUS_LABELS = {
  ready_to_pick: "Sẵn sàng lấy hàng",
  picking: "Đang lấy hàng",
  picked: "Đã lấy hàng",
  storing: "Đang lưu kho",
  sorting: "Đang phân loại",
  transporting: "Đang trung chuyển",
  delivering: "Đang giao hàng",
  delivered: "Đã giao hàng",
  money_collect_transporting: "Đang đối soát COD",
  cancel: "Đã hủy",
  delivery_fail: "Giao thất bại",
  returned: "Đã hoàn hàng",
  return: "Đang hoàn hàng",
  waiting_to_return: "Chờ hoàn hàng",
  return_transporting: "Đang vận chuyển hoàn",
  return_sorting: "Đang phân loại hàng hoàn",
  returning: "Đang hoàn về shop",
  exception: "Sự cố vận chuyển"
};

const humanizeStatusCode = (value) => {
  const normalized = String(value || "").trim();
  if (!normalized) return "N/A";
  return normalized
    .replace(/[_-]+/g, " ")
    .toLowerCase()
    .replace(/\b\w/g, (char) => char.toUpperCase());
};

const statusLabel = (status) => {
  const value = normalizeStatus(status);
  return INTERNAL_STATUS_LABELS[value] || humanizeStatusCode(value);
};

const shippingStatusLabel = (status) => {
  const value = String(status || "").trim().toLowerCase();
  if (!value) return "N/A";
  return GHN_STATUS_LABELS[value] || humanizeStatusCode(value);
};

const statusTagType = (status) => {
  const value = normalizeStatus(status);
  if (value.includes("DELIVER") || value.includes("SUCCESS") || value.includes("REFUND")) return "success";
  if (value.includes("FAIL") || value.includes("CANCEL") || value.includes("RETURN")) return "danger";
  if (value.includes("PENDING") || value.includes("PROCESS") || value.includes("CONFIRM") || value.includes("SHIP")) return "warning";
  return "info";
};

const sortedHistory = (history) => {
  return [...(history || [])].sort((a, b) => new Date(b.changedAt) - new Date(a.changedAt));
};

const toUniqueNormalizedList = (values, normalizer = normalizeStatus) => {
  const unique = new Set();
  (Array.isArray(values) ? values : []).forEach((item) => {
    const normalized = normalizer(item);
    if (normalized) {
      unique.add(normalized);
    }
  });
  return [...unique].sort((a, b) => a.localeCompare(b));
};

const refreshStatusFilterOptions = () => {
  statusFilterOptions.value = ghnShippingStatusOptions.value.map((code) => ({
    value: code,
    label: shippingStatusLabel(code)
  }));
  if (statusFilter.value && !ghnShippingStatusOptions.value.includes(statusFilter.value)) {
    statusFilter.value = "";
  }
};

const mergeStatusOptionsFromOrders = (rows) => {
  const ghnStatuses = toUniqueNormalizedList((rows || []).map((row) => row?.shippingStatus), (value) => String(value || "").trim().toLowerCase());
  ghnShippingStatusOptions.value = toUniqueNormalizedList([...ghnShippingStatusOptions.value, ...ghnStatuses], (value) => String(value || "").trim().toLowerCase());
  refreshStatusFilterOptions();
};

const fetchAdminStatusOptions = async () => {
  try {
    const { data } = await orderApi.getAdminStatusOptions();
    ghnShippingStatusOptions.value = toUniqueNormalizedList(data?.ghnShippingStatuses, (value) => String(value || "").trim().toLowerCase());
    refreshStatusFilterOptions();
  } catch (error) {
    console.error(error);
    statusFilterOptions.value = [];
  }
};

const buildQueryParams = (nextPage = page.value, nextSize = size.value) => {
  const [fromDate, toDate] = Array.isArray(dateRange.value) ? dateRange.value : [];
  return {
    page: Math.max(0, nextPage),
    size: nextSize,
    sortBy: "id",
    direction: "desc",
    q: keyword.value?.trim() || undefined,
    shippingStatus: statusFilter.value || undefined,
    fromDate: fromDate || undefined,
    toDate: toDate || undefined
  };
};

const fetchOrders = async (nextPage = page.value) => {
  loading.value = true;
  try {
    const { data } = await orderApi.getAdminOrders(buildQueryParams(nextPage));
    orders.value = Array.isArray(data?.content) ? data.content : [];
    mergeStatusOptionsFromOrders(orders.value);
    totalElements.value = Number(data?.totalElements || 0);
    page.value = Number(data?.page || 0);
    selectedOrderIds.value = [];
    orderTableRef.value?.clearSelection?.();
  } catch (error) {
    console.error(error);
    ElMessage.error(error?.response?.data?.message || "Không tải được danh sách đơn hàng");
  } finally {
    loading.value = false;
  }
};

const handlePageChange = (nextPage) => {
  fetchOrders(Math.max(0, nextPage - 1));
};

watch([statusFilter, dateRange], () => {
  fetchOrders(0);
});
watch(keyword, () => {
  if (filterTimer) clearTimeout(filterTimer);
  filterTimer = setTimeout(() => fetchOrders(0), 300);
});

const clearFilters = () => {
  keyword.value = "";
  statusFilter.value = "";
  dateRange.value = [];
  fetchOrders(0);
};

const handleOrderSelectionChange = (rows) => {
  selectedOrderIds.value = Array.isArray(rows) ? rows.map((row) => row.id).filter(Boolean) : [];
};

const escapeHtml = (value) => {
  return String(value ?? "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
};

const renderInvoiceSection = (order) => {
  const invoiceNo = `INV-${order.id}`;
  const createdAt = formatDateTime(order.createdAt);
  const customer = customerDisplay(order);
  const address = order.address || "N/A";
  const payment = paymentLabel(order.paymentMethod);
  const status = statusLabel(order.status);
  const rows = Array.isArray(order.items) ? order.items : [];
  const subtotal = rows.reduce((sum, row) => sum + (Number(row?.lineTotal) || 0), 0);
  const grandTotal = Number(order.totalPrice || subtotal || 0);

  const itemRowsHtml = rows.length
    ? rows
        .map((item, index) => `
          <tr>
            <td>${index + 1}</td>
            <td>${escapeHtml(item.productName || "N/A")}</td>
            <td>${escapeHtml(item.sku || "N/A")}</td>
            <td>${Number(item.quantity || 0)}</td>
            <td>${formatCurrency(item.price)}</td>
            <td>${formatCurrency(item.lineTotal)}</td>
          </tr>
        `)
        .join("")
    : `<tr><td colspan="6" style="text-align:center;">Không có sản phẩm</td></tr>`;

  return `
    <section class="invoice">
          <section class="head">
            <div class="brand">
              <h2>CLOTHING STORE</h2>
              <p>Hotline: 0900 000 000</p>
              <p>Email: support@clothing.local</p>
            </div>
            <div class="meta">
              <h3>Hóa đơn bán hàng</h3>
              <p>Số hóa đơn: ${invoiceNo}</p>
              <p>Ngày tạo: ${escapeHtml(createdAt)}</p>
            </div>
          </section>

          <section class="box">
            <p><strong>Khách hàng:</strong> ${escapeHtml(customer)}</p>
            <p><strong>Địa chỉ:</strong> ${escapeHtml(address)}</p>
            <p><strong>Thanh toán:</strong> ${escapeHtml(payment)}</p>
            <p><strong>Trạng thái đơn:</strong> ${escapeHtml(status)}</p>
          </section>

          <table>
            <thead>
              <tr>
                <th style="width: 42px;">#</th>
                <th>Sản phẩm</th>
                <th style="width: 130px;">SKU</th>
                <th style="width: 70px;">SL</th>
                <th style="width: 120px;">Đơn giá</th>
                <th style="width: 130px;">Thành tiền</th>
              </tr>
            </thead>
            <tbody>${itemRowsHtml}</tbody>
          </table>

          <section class="summary">
            <p><span>Tạm tính</span><strong>${formatCurrency(subtotal)}</strong></p>
            <p><span>Phí vận chuyển</span><strong>${formatCurrency(0)}</strong></p>
            <p class="total"><span>Tổng cộng</span><strong>${formatCurrency(grandTotal)}</strong></p>
          </section>

          <p class="footer">Ghi chú: Hóa đơn được xuất từ hệ thống quản trị và có giá trị lưu trữ nội bộ.</p>
    </section>
  `;
};

const printInvoices = (ordersToPrint) => {
  if (!Array.isArray(ordersToPrint) || !ordersToPrint.length) {
    ElMessage.warning("Không có hóa đơn để in");
    return;
  }

  const html = `
    <!doctype html>
    <html lang="vi">
      <head>
        <meta charset="UTF-8" />
        <title>Hoa don</title>
        <style>
          @page { size: A4; margin: 16mm; }
          body { font-family: Arial, sans-serif; color: #0f172a; margin: 0; }
          .invoice { width: 100%; page-break-after: always; }
          .invoice:last-child { page-break-after: auto; }
          .head { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 16px; }
          .brand h2 { margin: 0; font-size: 22px; }
          .brand p { margin: 4px 0 0; color: #475569; font-size: 12px; }
          .meta { text-align: right; }
          .meta h3 { margin: 0; font-size: 18px; text-transform: uppercase; }
          .meta p { margin: 4px 0 0; font-size: 12px; color: #475569; }
          .box { border: 1px solid #cbd5e1; padding: 10px 12px; margin-bottom: 14px; }
          .box p { margin: 4px 0; font-size: 13px; }
          table { width: 100%; border-collapse: collapse; margin-top: 8px; }
          th, td { border: 1px solid #cbd5e1; padding: 8px; font-size: 12px; }
          th { background: #f8fafc; text-align: left; }
          .summary { margin-top: 14px; margin-left: auto; width: 300px; }
          .summary p { display: flex; justify-content: space-between; margin: 6px 0; font-size: 13px; }
          .summary .total { font-weight: 700; font-size: 14px; border-top: 1px solid #cbd5e1; padding-top: 8px; }
          .footer { margin-top: 18px; font-size: 12px; color: #64748b; }
        </style>
      </head>
      <body>
        ${ordersToPrint.map((order) => renderInvoiceSection(order)).join("")}
      </body>
    </html>
  `;

  const popup = window.open("", "_blank", "width=1024,height=768");
  if (!popup) {
    ElMessage.error("Trình duyệt đang chặn popup. Vui lòng cho phép popup để xuất hóa đơn.");
    return;
  }
  popup.document.open();
  popup.document.write(html);
  popup.document.close();
  popup.focus();
  setTimeout(() => {
    popup.print();
  }, 250);
};

const fetchAllOrdersForCurrentFilter = async () => {
  const all = [];
  let pageIndex = 0;
  while (true) {
    const { data } = await orderApi.getAdminOrders(buildQueryParams(pageIndex, 100));
    const content = Array.isArray(data?.content) ? data.content : [];
    all.push(...content);
    const last = Boolean(data?.last);
    if (last || !content.length) {
      break;
    }
    pageIndex += 1;
  }
  return all;
};

const exportSelectedInvoices = async () => {
  if (!selectedOrderIds.value.length) {
    ElMessage.warning("Vui lòng chọn ít nhất 1 đơn hàng");
    return;
  }
  let selectedOrders = orders.value.filter((order) => selectedOrderIds.value.includes(order.id));
  if (!selectedOrders.length) {
    ElMessage.warning("Không tìm thấy đơn hàng đã chọn");
    return;
  }
  const isSelectAllCurrentPage = selectedOrderIds.value.length === orders.value.length && orders.value.length > 0;
  if (isSelectAllCurrentPage && totalElements.value > orders.value.length) {
    try {
      await confirm({
        title: "Xác nhận in hóa đơn",
        message: "Bạn đã chọn tất cả đơn trên trang hiện tại. In toàn bộ đơn theo bộ lọc hiện tại?",
        confirmButtonText: "In toàn bộ",
        cancelButtonText: "Chỉ in trang này",
        onConfirm: async () => {
          selectedOrders = await fetchAllOrdersForCurrentFilter();
          printInvoices(selectedOrders);
        }
      });
    } catch {
      // user chooses "Chỉ in trang này"
      printInvoices(selectedOrders);
    }
  } else {
    printInvoices(selectedOrders);
  }
};

const openDetails = (order) => {
  selectedOrder.value = order;
  detailsVisible.value = true;
};

onMounted(() => {
  fetchAdminStatusOptions();
  fetchOrders();
});
</script>

<style scoped lang="scss">
.admin-orders-page {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.panel {
  border: 1px solid #dce1e7;
  background: #fff;
  padding: 10px;
}

.panel-head {
  margin-bottom: 10px;
  display: grid !important;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: start;
  gap: 8px;
}

.filters-row {
  display: grid !important;
  grid-template-columns: minmax(280px, 1.5fr) minmax(160px, 180px) minmax(240px, 280px) auto;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.head-actions {
  display: flex !important;
  align-items: center;
  gap: 8px;
  flex-wrap: nowrap;
  justify-content: flex-end;
}

.search-input {
  width: 100%;
}

.status-filter {
  width: 100%;
}

.date-filter {
  width: 100%;
}

.table-wrap {
  width: 100%;
}

.orders-table {
  width: 100%;
}

.order-cell {
  display: grid;
  gap: 2px;
}

.order-cell strong {
  font-size: 13px;
  color: #0f172a;
}

.order-cell span {
  font-size: 12px;
  color: #64748b;
}

.summary-cell {
  display: grid;
  gap: 4px;
}

.summary-cell span {
  font-size: 12px;
  color: #334155;
}

.action-cell {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.pagination-wrap {
  margin-top: 10px;
  display: flex;
  justify-content: flex-end;
}

.detail-layout {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.detail-card {
  border: 1px solid #e5e7eb;
  background: #fff;
  padding: 12px;
}

.card-title {
  margin: 0 0 10px;
  font-size: 14px;
  font-weight: 800;
  text-transform: uppercase;
  color: #111827;
}

.detail-meta {
  margin-bottom: 0;
}

.meta-grid {
  display: grid;
  gap: 6px;

  p {
    margin: 0;
    font-size: 14px;
  }
}

.status-box {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  justify-content: flex-start;
  gap: 12px;
  background: #f9fafb;

  p {
    margin: 0;
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 14px;
  }
}

.status-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-start;
}

.history-box {
  margin-top: 0;
}

.order-detail-drawer {
  :deep(.el-drawer__body) {
    padding: 16px;
    overflow-y: auto;
  }
}

@media (max-width: 760px) {
  .panel-head {
    grid-template-columns: 1fr;
    align-items: stretch;
  }

  .head-actions {
    width: 100%;
    justify-content: flex-start;
    flex-wrap: wrap;
  }

  .filters-row {
    grid-template-columns: 1fr;
  }

  .search-input {
    width: 100%;
  }

  .status-filter,
  .date-filter {
    width: 100%;
  }

  .order-detail-drawer {
    :deep(.el-drawer) {
      width: 100% !important;
      max-width: 100% !important;
    }
  }
}

@media (max-width: 1280px) {
  .panel-head {
    grid-template-columns: 1fr;
    align-items: stretch;
  }

  .head-actions {
    justify-content: flex-start;
    flex-wrap: wrap;
  }

  .filters-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>

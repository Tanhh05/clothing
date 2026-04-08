<template>
  <section class="admin-orders-page" v-loading="loading">
    <header class="head">
      <div>
        <p class="eyebrow">Admin panel</p>
        <h2>Order Management</h2>
        <p class="sub-text">Xác nhận và cập nhật trạng thái đơn hàng cho khách.</p>
      </div>
      <div class="head-actions">
        <el-button @click="fetchOrders">Làm mới</el-button>
        <el-button plain :disabled="!selectedOrderIds.length" @click="exportSelectedInvoices">
          Xuất hóa đơn
        </el-button>
      </div>
    </header>

    <div class="overview-grid">
      <article class="overview-card">
        <p class="label">Total orders</p>
        <p class="value">{{ stats.total }}</p>
      </article>
      <article class="overview-card">
        <p class="label">Pending</p>
        <p class="value">{{ stats.pending }}</p>
      </article>
      <article class="overview-card">
        <p class="label">Processing</p>
        <p class="value">{{ stats.processing }}</p>
      </article>
      <article class="overview-card">
        <p class="label">Delivered</p>
        <p class="value">{{ stats.delivered }}</p>
      </article>
    </div>

    <div class="panel">
      <div class="panel-head">
        <div class="filters-row">
          <el-input v-model="keyword" placeholder="Tìm theo mã đơn / địa chỉ" clearable class="search-input" />
          <el-select v-model="statusFilter" clearable placeholder="Trạng thái" class="status-filter">
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
      </div>

      <div v-if="selectedOrderIds.length" class="bulk-toolbar">
        <p>Đã chọn {{ selectedOrderIds.length }} đơn</p>
        <div class="bulk-actions">
          <el-select v-model="bulkStatus" placeholder="Chọn trạng thái" class="bulk-status-select">
            <el-option v-for="item in statusFilterOptions" :key="`bulk-${item.value}`" :label="item.label" :value="item.value" />
          </el-select>
          <el-button type="primary" :loading="bulkSubmitting" @click="applyBulkStatus">Cập nhật hàng loạt</el-button>
          <el-button @click="clearOrderSelection">Bỏ chọn</el-button>
        </div>
      </div>

      <div class="table-wrap">
        <el-table
          ref="orderTableRef"
          :data="orders"
          border
          stripe
          class="orders-table"
          empty-text="Chưa có đơn hàng"
          table-layout="auto"
          @selection-change="handleOrderSelectionChange"
        >
          <el-table-column type="selection" width="48" />
          <el-table-column prop="id" label="Mã đơn" width="84" />
          <el-table-column label="Khách hàng" min-width="150">
            <template #default="{ row }">
              {{ customerDisplay(row) }}
            </template>
          </el-table-column>
          <!-- <el-table-column label="Địa chỉ" min-width="200" show-overflow-tooltip>
            <template #default="{ row }">{{ row.address || "N/A" }}</template>
          </el-table-column> -->
          <el-table-column label="Thanh toán" width="110">
            <template #default="{ row }">{{ paymentLabel(row.paymentMethod) }}</template>
          </el-table-column>
          <el-table-column label="Tổng tiền" width="130">
            <template #default="{ row }">{{ formatCurrency(row.totalPrice) }}</template>
          </el-table-column>
          <el-table-column label="Ngày tạo" width="150">
            <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="Trạng thái hiện tại" width="145">
            <template #default="{ row }">
              <el-tag>{{ statusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="Thao tác" width="104">
            <template #default="{ row }">
              <el-button size="small" @click="openDetails(row)">Chi tiết</el-button>
            </template>
          </el-table-column>
          <el-table-column label="Nhanh" width="150">
            <template #default="{ row }">
              <el-button
                size="small"
                type="primary"
                :disabled="updatingOrderId === row.id || !nextStatus(row.status)"
                :loading="updatingOrderId === row.id"
                @click="updateStatus(row)"
              >
                {{ nextStatus(row.status) ? statusLabel(nextStatus(row.status)) : "Hoàn tất" }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
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
            </div>
          </section>

          <section class="detail-card status-box">
            <h4 class="card-title">Xử lý trạng thái</h4>
            <p><strong>Trạng thái hiện tại:</strong> <el-tag>{{ statusLabel(selectedOrder.status) }}</el-tag></p>
            <div class="status-actions">
              <el-button
                type="primary"
                :disabled="updatingOrderId === selectedOrder.id || !nextStatus(selectedOrder.status)"
                :loading="updatingOrderId === selectedOrder.id"
                @click="updateStatus(selectedOrder)"
              >
                {{ nextStatus(selectedOrder.status) ? `Chuyển → ${statusLabel(nextStatus(selectedOrder.status))}` : "Đã hoàn tất" }}
              </el-button>
              <el-button
                type="warning"
                plain
                :disabled="updatingOrderId === selectedOrder.id || !canMarkCancelled(selectedOrder.status)"
                @click="markCancelled(selectedOrder)"
              >
                Khách hủy
              </el-button>
              <el-button
                type="danger"
                plain
                :disabled="updatingOrderId === selectedOrder.id || !canMarkFailed(selectedOrder.status)"
                @click="markFailed(selectedOrder)"
              >
                Bom hàng
              </el-button>
              <el-button
                type="danger"
                plain
                :disabled="updatingOrderId === selectedOrder.id || !canMarkFailedDelivery(selectedOrder.status)"
                @click="markFailedDelivery(selectedOrder)"
              >
                Giao thất bại
              </el-button>
              <el-button
                type="success"
                plain
                :disabled="updatingOrderId === selectedOrder.id || !canMarkRefunded(selectedOrder.status)"
                @click="markRefunded(selectedOrder)"
              >
                Hoàn tiền
              </el-button>
            </div>
          </section>

          <section class="detail-card">
            <h4 class="card-title">Sản phẩm trong đơn</h4>
            <el-table :data="selectedOrder.items || []" border size="small" empty-text="Không có sản phẩm">
              <el-table-column prop="productName" label="Sản phẩm" min-width="220" />
              <el-table-column prop="sku" label="SKU" width="130" />
              <el-table-column prop="quantity" label="SL" width="80" />
              <el-table-column label="Đơn giá" width="130">
                <template #default="{ row }">{{ formatCurrency(row.price) }}</template>
              </el-table-column>
              <el-table-column label="Thành tiền" width="140">
                <template #default="{ row }">{{ formatCurrency(row.lineTotal) }}</template>
              </el-table-column>
            </el-table>
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
import { computed, onMounted, ref, watch } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { orderApi } from "@/modules/order/api/orderApi";

const loading = ref(false);
const orders = ref([]);
const totalElements = ref(0);
const keyword = ref("");
const statusFilter = ref("");
const dateRange = ref([]);
const detailsVisible = ref(false);
const selectedOrder = ref(null);
const updatingOrderId = ref(null);
const page = ref(0);
const size = ref(20);
const orderTableRef = ref(null);
const selectedOrderIds = ref([]);
const bulkStatus = ref("");
const bulkSubmitting = ref(false);
let filterTimer = null;

const statusFlow = ["PENDING", "PROCESSING", "CONFIRMED", "SHIPPED", "DELIVERED"];
const statusFilterOptions = [
  { value: "PENDING", label: "Chờ xử lý" },
  { value: "PROCESSING", label: "Đang xử lý" },
  { value: "CONFIRMED", label: "Đã xác nhận" },
  { value: "SHIPPED", label: "Đang giao" },
  { value: "DELIVERED", label: "Đã giao" },
  { value: "CANCELLED", label: "Khách hủy" },
  { value: "FAILED", label: "Bom hàng" },
  { value: "FAILED_DELIVERY", label: "Giao thất bại" },
  { value: "REFUNDED", label: "Đã hoàn tiền" }
];

const normalizeStatus = (status) => String(status || "").trim().toUpperCase();

const stats = computed(() => {
  const rows = orders.value;
  return {
    total: rows.length,
    pending: rows.filter((o) => normalizeStatus(o.status) === "PENDING").length,
    processing: rows.filter((o) => ["PROCESSING", "CONFIRMED", "SHIPPED"].includes(normalizeStatus(o.status))).length,
    delivered: rows.filter((o) => normalizeStatus(o.status) === "DELIVERED").length
  };
});

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

const statusLabel = (status) => {
  const value = normalizeStatus(status);
  const map = {
    PENDING: "Chờ xử lý",
    PROCESSING: "Đang xử lý",
    CONFIRMED: "Đã xác nhận",
    SHIPPED: "Đang giao",
    DELIVERED: "Đã giao",
    CANCELLED: "Khách hủy",
    FAILED: "Bom hàng",
    FAILED_INSUFFICIENT_STOCK: "Thiếu tồn kho",
    FAILED_DELIVERY: "Giao thất bại",
    RETURN_REQUESTED: "Yêu cầu trả hàng",
    REFUNDED: "Đã hoàn tiền",
    ON_HOLD: "Tạm giữ"
  };
  return map[value] || value || "N/A";
};

const nextStatus = (currentStatus) => {
  const normalized = normalizeStatus(currentStatus);
  const index = statusFlow.indexOf(normalized);
  if (index === -1 || index === statusFlow.length - 1) return null;
  return statusFlow[index + 1];
};

const isTerminalStatus = (status) => {
  const value = normalizeStatus(status);
  return ["DELIVERED", "CANCELLED", "FAILED", "FAILED_INSUFFICIENT_STOCK", "FAILED_DELIVERY", "REFUNDED"].includes(value);
};

const canMarkCancelled = (status) => {
  const value = normalizeStatus(status);
  return !isTerminalStatus(value) && value !== "SHIPPED";
};

const canMarkFailed = (status) => {
  return !isTerminalStatus(status);
};

const canMarkFailedDelivery = (status) => {
  const value = normalizeStatus(status);
  return !isTerminalStatus(value) && value === "SHIPPED";
};

const canMarkRefunded = (status) => {
  const value = normalizeStatus(status);
  return ["CANCELLED", "FAILED", "FAILED_DELIVERY", "RETURN_REQUESTED"].includes(value);
};

const sortedHistory = (history) => {
  return [...(history || [])].sort((a, b) => new Date(b.changedAt) - new Date(a.changedAt));
};

const buildQueryParams = (nextPage = page.value, nextSize = size.value) => {
  const [fromDate, toDate] = Array.isArray(dateRange.value) ? dateRange.value : [];
  return {
    page: Math.max(0, nextPage),
    size: nextSize,
    sortBy: "id",
    direction: "desc",
    q: keyword.value?.trim() || undefined,
    status: statusFilter.value || undefined,
    fromDate: fromDate || undefined,
    toDate: toDate || undefined
  };
};

const fetchOrders = async (nextPage = page.value) => {
  loading.value = true;
  try {
    const { data } = await orderApi.getAdminOrders(buildQueryParams(nextPage));
    orders.value = Array.isArray(data?.content) ? data.content : [];
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

const clearOrderSelection = () => {
  selectedOrderIds.value = [];
  orderTableRef.value?.clearSelection?.();
};

const applyBulkStatus = async () => {
  if (!selectedOrderIds.value.length) return;
  const normalized = normalizeStatus(bulkStatus.value);
  if (!normalized) {
    ElMessage.warning("Vui lòng chọn trạng thái để cập nhật");
    return;
  }
  bulkSubmitting.value = true;
  try {
    await orderApi.bulkUpdateStatus(selectedOrderIds.value, normalized);
    ElMessage.success(`Đã cập nhật ${selectedOrderIds.value.length} đơn hàng`);
    clearOrderSelection();
    await fetchOrders(page.value);
  } catch (error) {
    console.error(error);
    ElMessage.error(error?.response?.data?.message || "Không cập nhật được trạng thái hàng loạt");
  } finally {
    bulkSubmitting.value = false;
  }
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
      await ElMessageBox.confirm(
        "Bạn đã chọn tất cả đơn trên trang hiện tại. In toàn bộ đơn theo bộ lọc hiện tại?",
        "Xác nhận in hóa đơn",
        { type: "info", confirmButtonText: "In toàn bộ", cancelButtonText: "Chỉ in trang này" }
      );
      selectedOrders = await fetchAllOrdersForCurrentFilter();
    } catch {
      // user chooses "Chỉ in trang này"
    }
  }
  printInvoices(selectedOrders);
};

const openDetails = (order) => {
  selectedOrder.value = order;
  detailsVisible.value = true;
};

const updateStatus = async (order, targetStatus = null) => {
  const candidateStatus = targetStatus || nextStatus(order.status);
  const normalizedNextStatus = normalizeStatus(candidateStatus);
  if (!normalizedNextStatus || normalizedNextStatus === normalizeStatus(order.status)) return;

  updatingOrderId.value = order.id;
  try {
    const { data } = await orderApi.updateOrderStatus(order.id, normalizedNextStatus);
    const updated = data || {};
    const index = orders.value.findIndex((o) => o.id === order.id);
    if (index !== -1) {
      orders.value[index] = updated;
    }
    if (selectedOrder.value?.id === order.id) {
      selectedOrder.value = updated;
    }
    ElMessage.success(`Đã cập nhật đơn #${order.id} -> ${normalizedNextStatus}`);
    await fetchOrders(page.value);
  } catch (error) {
    console.error(error);
    ElMessage.error(error?.response?.data?.message || "Cập nhật trạng thái thất bại");
  } finally {
    updatingOrderId.value = null;
  }
};

const markCancelled = async (order) => {
  try {
    await ElMessageBox.confirm(`Đánh dấu đơn #${order.id} là KHÁCH HỦY?`, "Xác nhận", {
      type: "warning",
      confirmButtonText: "Xác nhận",
      cancelButtonText: "Hủy"
    });
    await updateStatus(order, "CANCELLED");
  } catch (error) {
    if (error !== "cancel") {
      ElMessage.error("Không thể cập nhật trạng thái khách hủy");
    }
  }
};

const markFailed = async (order) => {
  try {
    await ElMessageBox.confirm(`Đánh dấu đơn #${order.id} là BOM HÀNG?`, "Xác nhận", {
      type: "warning",
      confirmButtonText: "Xác nhận",
      cancelButtonText: "Hủy"
    });
    await updateStatus(order, "FAILED");
  } catch (error) {
    if (error !== "cancel") {
      ElMessage.error("Không thể cập nhật trạng thái bom hàng");
    }
  }
};

const markFailedDelivery = async (order) => {
  try {
    await ElMessageBox.confirm(`Đánh dấu đơn #${order.id} là GIAO THẤT BẠI?`, "Xác nhận", {
      type: "warning",
      confirmButtonText: "Xác nhận",
      cancelButtonText: "Hủy"
    });
    await updateStatus(order, "FAILED_DELIVERY");
  } catch (error) {
    if (error !== "cancel") {
      ElMessage.error("Không thể cập nhật trạng thái giao thất bại");
    }
  }
};

const markRefunded = async (order) => {
  try {
    await ElMessageBox.confirm(`Đánh dấu đơn #${order.id} là ĐÃ HOÀN TIỀN?`, "Xác nhận", {
      type: "warning",
      confirmButtonText: "Xác nhận",
      cancelButtonText: "Hủy"
    });
    await updateStatus(order, "REFUNDED");
  } catch (error) {
    if (error !== "cancel") {
      ElMessage.error("Không thể cập nhật trạng thái hoàn tiền");
    }
  }
};

onMounted(() => {
  fetchOrders();
});
</script>

<style scoped lang="scss">
.admin-orders-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.head {
  padding: 16px;
  border: 1px solid #dce1e7;
  background: #fbfbfc;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;

  h2 {
    margin: 0;
    font-size: 24px;
    font-weight: 800;
    text-transform: uppercase;
  }

  .eyebrow {
    margin: 0 0 6px;
    text-transform: uppercase;
    letter-spacing: 1px;
    font-size: 11px;
    color: #6b7280;
  }

  .sub-text {
    margin: 6px 0 0;
    color: #6b7280;
    font-size: 13px;
  }
}

.head-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.overview-card {
  border: 1px solid #e2e7ed;
  padding: 16px;
  background: #fff;

  .label {
    margin: 0 0 8px;
    color: #6b7280;
    font-size: 12px;
    text-transform: uppercase;
  }

  .value {
    margin: 0;
    font-size: 30px;
    font-weight: 800;
  }
}

.panel {
  border: 1px solid #dce1e7;
  background: #fff;
  padding: 16px;
}

.panel-head {
  margin-bottom: 14px;
}

.filters-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.bulk-toolbar {
  margin-bottom: 12px;
  border: 1px dashed #cfd8e3;
  padding: 10px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;

  p {
    margin: 0;
    font-size: 13px;
    color: #475569;
  }
}

.bulk-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.search-input {
  width: 320px;
}

.status-filter {
  width: 180px;
}

.date-filter {
  width: 280px;
}

.bulk-status-select {
  width: 180px;
}

.table-wrap {
  width: 100%;
  overflow-x: auto;
}

.orders-table {
  width: 100%;
  min-width: 1100px;
}

.pagination-wrap {
  margin-top: 14px;
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

@media (max-width: 1100px) {
  .overview-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .head {
    flex-direction: column;
    align-items: flex-start;
  }

  .head-actions {
    width: 100%;
    display: grid;
    grid-template-columns: 1fr 1fr;
  }

  .search-input {
    width: 100%;
  }

  .status-filter,
  .date-filter,
  .bulk-status-select {
    width: 100%;
  }

  .overview-grid {
    grid-template-columns: 1fr;
  }

  .order-detail-drawer {
    :deep(.el-drawer) {
      width: 100% !important;
      max-width: 100% !important;
    }
  }
}
</style>

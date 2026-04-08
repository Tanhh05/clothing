<template>
  <section class="revenue-page" v-loading="loading">
    <header class="head">
      <div>
        <p class="eyebrow">Admin panel</p>
        <h2>Revenue Dashboard</h2>
        <p class="sub-text">Theo dõi doanh thu, trạng thái đơn và hiệu suất bán hàng.</p>
      </div>
      <div class="head-actions">
        <el-select v-model="rangeKey" style="width: 160px">
          <el-option label="Hôm nay" value="today" />
          <el-option label="7 ngày" value="7d" />
          <el-option label="30 ngày" value="30d" />
          <el-option label="Toàn thời gian" value="all" />
        </el-select>
        <el-button @click="logDrawerVisible = true">Nhật ký</el-button>
        <el-button @click="fetchOrders">Làm mới</el-button>
      </div>
    </header>

    <section class="dashboard-section">
      <div class="section-head">
        <h3>Tổng quan kỳ chọn</h3>
        <span>{{ periodLabel }}</span>
      </div>
      <div class="kpi-grid">
        <article class="kpi-card">
          <p class="label">Doanh thu kỳ chọn</p>
          <p class="value">{{ formatCurrency(stats.periodRevenue) }}</p>
          <p class="helper">Doanh thu đã giao thành công</p>
        </article>
        <article class="kpi-card">
          <p class="label">Tổng đơn kỳ chọn</p>
          <p class="value">{{ stats.periodOrders }}</p>
          <p class="helper">Tổng số đơn trong kỳ</p>
        </article>
        <article class="kpi-card">
          <p class="label">Tỉ lệ giao thành công</p>
          <p class="value">{{ stats.deliveryRate }}%</p>
          <p class="helper">Đơn đã giao / đơn đã xử lý</p>
        </article>
        <article class="kpi-card">
          <p class="label">Giá trị đơn TB</p>
          <p class="value">{{ formatCurrency(stats.avgOrderValue) }}</p>
          <p class="helper">Theo kỳ đã chọn</p>
        </article>
      </div>
      <div class="health-grid">
        <article class="kpi-card kpi-card--compact">
          <p class="label">Đơn chờ xử lý</p>
          <p class="value">{{ summary.pendingOrders || 0 }}</p>
          <p class="helper">PENDING + PROCESSING + CONFIRMED</p>
        </article>
        <article class="kpi-card kpi-card--compact">
          <p class="label">Tỷ lệ hủy 30 ngày</p>
          <p class="value">{{ Number(summary.cancelRate30d || 0).toFixed(1) }}%</p>
          <p class="helper">CANCELLED/FAILED/REFUNDED trên tổng đơn</p>
        </article>
      </div>
    </section>

    <section class="dashboard-section">
      <div class="section-head">
        <h3>Phân tích doanh thu</h3>
      </div>
      <div class="analytics-grid">
        <article class="panel">
          <div class="panel-head">
            <h3>Phân bổ trạng thái đơn</h3>
          </div>
          <div class="status-list">
            <div v-for="item in statusRows" :key="item.key" class="status-row">
              <div class="row-top">
                <span>{{ item.label }}</span>
                <span>{{ item.count }} ({{ item.percent }}%)</span>
              </div>
              <div class="bar-bg">
                <div class="bar-fill" :style="{ width: `${item.percent}%`, backgroundColor: item.color }" />
              </div>
            </div>
          </div>
        </article>

        <article class="panel">
          <div class="panel-head">
            <h3>Doanh thu 7 ngày gần nhất</h3>
          </div>
          <div class="mini-chart">
            <div v-for="day in recentRevenueBars" :key="day.key" class="bar-col">
              <div class="bar-wrap">
                <div class="bar" :style="{ height: `${day.height}%` }" />
              </div>
              <p class="bar-label">{{ day.label }}</p>
              <p class="bar-value">{{ formatCompact(day.value) }}</p>
            </div>
          </div>
        </article>
      </div>
    </section>

    <section class="dashboard-section">
      <div class="section-head">
        <h3>Dữ liệu bán hàng</h3>
      </div>
      <div class="data-grid">
        <article class="panel">
          <div class="panel-head">
            <h3>Top sản phẩm 30 ngày</h3>
          </div>
          <div class="table-scroll">
            <el-table :data="summary.topProducts30d || []" border size="small" empty-text="Chưa có dữ liệu">
              <el-table-column prop="productId" label="#" width="74" />
              <el-table-column prop="productName" label="Sản phẩm" min-width="170" />
              <el-table-column prop="totalQuantity" label="Đã bán" width="110" />
              <el-table-column label="Doanh thu" width="140">
                <template #default="{ row }">{{ formatCurrency(row.totalRevenue) }}</template>
              </el-table-column>
            </el-table>
          </div>
        </article>

        <article class="panel panel--full">
          <div class="panel-head">
            <h3>Đơn hàng gần đây</h3>
          </div>
          <div class="table-scroll">
            <el-table :data="pagedRecentOrders" border stripe table-layout="auto" empty-text="Chưa có đơn">
              <el-table-column prop="id" label="Mã đơn" width="88" />
              <el-table-column label="Khách hàng" min-width="160">
                <template #default="{ row }">{{ row.customerName || `User #${row.userId || "N/A"}` }}</template>
              </el-table-column>
              <el-table-column label="Trạng thái" width="140">
                <template #default="{ row }">
                  <el-tag>{{ statusLabel(row.status) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="Thanh toán" width="120">
                <template #default="{ row }">{{ row.paymentMethod || "N/A" }}</template>
              </el-table-column>
              <el-table-column label="Tổng tiền" width="140">
                <template #default="{ row }">{{ formatCurrency(row.totalPrice) }}</template>
              </el-table-column>
              <el-table-column label="Ngày tạo" width="170">
                <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
              </el-table-column>
            </el-table>
          </div>
          <div class="table-pagination">
            <el-pagination
              layout="total, prev, pager, next"
              :total="recentOrders.length"
              :page-size="recentOrdersPageSize"
              :current-page="recentOrdersPage"
              @current-change="handleRecentOrdersPageChange"
            />
          </div>
        </article>
      </div>
    </section>

    <el-drawer
      v-model="logDrawerVisible"
      direction="rtl"
      size="46%"
      :with-header="false"
      class="audit-log-drawer"
    >
      <section class="drawer-inner">
        <div class="drawer-head">
          <h3>Nhật ký thao tác gần đây</h3>
          <el-button @click="logDrawerVisible = false">Đóng</el-button>
        </div>
        <div class="table-scroll">
          <el-table :data="pagedAuditLogs" border size="small" empty-text="Chưa có log">
            <el-table-column prop="createdAt" label="Thời gian" width="165">
              <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
            </el-table-column>
            <el-table-column prop="actor" label="Người thao tác" width="130" />
            <el-table-column prop="action" label="Hành động" width="180" />
            <el-table-column label="Chi tiết" min-width="220">
              <template #default="{ row }">{{ row.detail || "-" }}</template>
            </el-table-column>
          </el-table>
        </div>
        <div class="table-pagination">
          <el-pagination
            layout="total, prev, pager, next"
            :total="auditLogs.length"
            :page-size="auditLogsPageSize"
            :current-page="auditLogsPage"
            @current-change="handleAuditLogsPageChange"
          />
        </div>
      </section>
    </el-drawer>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { ElMessage } from "element-plus";
import { orderApi } from "@/modules/order/api/orderApi";
import { adminApi } from "@/modules/dashboard/api/adminApi";

const loading = ref(false);
const orders = ref([]);
const rangeKey = ref("7d");
const summary = ref({
  revenueToday: 0,
  revenue7d: 0,
  revenue30d: 0,
  ordersToday: 0,
  orders7d: 0,
  orders30d: 0,
  pendingOrders: 0,
  cancelRate30d: 0,
  statusCounts30d: {},
  topProducts30d: []
});
const auditLogs = ref([]);
const logDrawerVisible = ref(false);
const recentOrdersPage = ref(1);
const recentOrdersPageSize = ref(8);
const auditLogsPage = ref(1);
const auditLogsPageSize = ref(10);

const normalizeStatus = (status) => String(status || "").trim().toUpperCase();

const statusLabel = (status) => {
  const map = {
    PENDING: "Chờ xử lý",
    PROCESSING: "Đang xử lý",
    CONFIRMED: "Đã xác nhận",
    SHIPPED: "Đang giao",
    DELIVERED: "Đã giao",
    CANCELLED: "Khách hủy",
    FAILED: "Bom hàng",
    FAILED_DELIVERY: "Giao thất bại",
    REFUNDED: "Hoàn tiền"
  };
  return map[normalizeStatus(status)] || normalizeStatus(status) || "N/A";
};

const periodLabel = computed(() => {
  if (rangeKey.value === "today") return "Theo ngày hiện tại";
  if (rangeKey.value === "7d") return "Trong 7 ngày gần nhất";
  if (rangeKey.value === "30d") return "Trong 30 ngày gần nhất";
  return "Toàn bộ thời gian";
});

const toDate = (value) => {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? null : date;
};

const isInRange = (date) => {
  if (!(date instanceof Date)) return false;
  const now = new Date();
  if (rangeKey.value === "all") return true;
  if (rangeKey.value === "today") {
    return date.toDateString() === now.toDateString();
  }
  const days = rangeKey.value === "7d" ? 7 : 30;
  const start = new Date(now);
  start.setHours(0, 0, 0, 0);
  start.setDate(start.getDate() - (days - 1));
  return date >= start && date <= now;
};

const filteredOrders = computed(() => {
  return orders.value.filter((order) => isInRange(toDate(order.createdAt)));
});

const stats = computed(() => {
  const rows = filteredOrders.value;
  const fallbackPeriodOrders = rows.length;
  const fallbackPeriodRevenue = rows
    .filter((row) => normalizeStatus(row.status) === "DELIVERED")
    .reduce((sum, row) => sum + (Number(row.totalPrice) || 0), 0);

  const periodRevenue = (() => {
    if (rangeKey.value === "today") return Number(summary.value.revenueToday || 0);
    if (rangeKey.value === "7d") return Number(summary.value.revenue7d || 0);
    if (rangeKey.value === "30d") return Number(summary.value.revenue30d || 0);
    return fallbackPeriodRevenue;
  })();
  const periodOrders = (() => {
    if (rangeKey.value === "today") return Number(summary.value.ordersToday || 0);
    if (rangeKey.value === "7d") return Number(summary.value.orders7d || 0);
    if (rangeKey.value === "30d") return Number(summary.value.orders30d || 0);
    return fallbackPeriodOrders;
  })();

  const processed = rows.filter((row) => {
    const status = normalizeStatus(row.status);
    return ["DELIVERED", "CANCELLED", "FAILED", "FAILED_DELIVERY", "REFUNDED"].includes(status);
  }).length;

  const delivered = rows.filter((row) => normalizeStatus(row.status) === "DELIVERED").length;
  const deliveryRate = processed > 0 ? Math.round((delivered / processed) * 100) : 0;
  const avgOrderValue = periodOrders > 0 ? Math.round(periodRevenue / periodOrders) : 0;

  return {
    periodOrders,
    periodRevenue,
    deliveryRate,
    avgOrderValue
  };
});

const statusRows = computed(() => {
  const rows = filteredOrders.value;
  const summaryCounts = summary.value?.statusCounts30d || {};
  const total = rangeKey.value === "30d"
    ? Object.values(summaryCounts).reduce((sum, count) => sum + Number(count || 0), 0) || 1
    : rows.length || 1;
  const config = [
    { key: "PENDING", label: "Chờ xử lý", color: "#94a3b8" },
    { key: "PROCESSING", label: "Đang xử lý", color: "#60a5fa" },
    { key: "SHIPPED", label: "Đang giao", color: "#f59e0b" },
    { key: "DELIVERED", label: "Đã giao", color: "#10b981" },
    { key: "CANCELLED", label: "Khách hủy", color: "#ef4444" },
    { key: "FAILED", label: "Bom hàng", color: "#b91c1c" }
  ];
  return config.map((item) => {
    const count = rangeKey.value === "30d"
      ? Number(summaryCounts[item.key] || 0)
      : rows.filter((row) => normalizeStatus(row.status) === item.key).length;
    return {
      ...item,
      count,
      percent: Math.round((count / total) * 100)
    };
  });
});

const recentRevenueBars = computed(() => {
  const now = new Date();
  const days = [];
  for (let i = 6; i >= 0; i -= 1) {
    const date = new Date(now);
    date.setHours(0, 0, 0, 0);
    date.setDate(now.getDate() - i);
    days.push(date);
  }

  const values = days.map((day) => {
    const next = new Date(day);
    next.setDate(day.getDate() + 1);
    const value = orders.value
      .filter((row) => normalizeStatus(row.status) === "DELIVERED")
      .filter((row) => {
        const d = toDate(row.createdAt);
        return d && d >= day && d < next;
      })
      .reduce((sum, row) => sum + (Number(row.totalPrice) || 0), 0);

    return {
      key: day.toISOString(),
      label: `${day.getDate()}/${day.getMonth() + 1}`,
      value
    };
  });

  const max = Math.max(...values.map((v) => v.value), 1);
  return values.map((v) => ({
    ...v,
    height: Math.max(8, Math.round((v.value / max) * 100))
  }));
});

const recentOrders = computed(() => {
  return [...orders.value]
    .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
    .slice(0, 200);
});

const pagedRecentOrders = computed(() => {
  const start = (recentOrdersPage.value - 1) * recentOrdersPageSize.value;
  return recentOrders.value.slice(start, start + recentOrdersPageSize.value);
});

const pagedAuditLogs = computed(() => {
  const start = (auditLogsPage.value - 1) * auditLogsPageSize.value;
  return auditLogs.value.slice(start, start + auditLogsPageSize.value);
});

const formatCurrency = (amount) => {
  return new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(Number(amount) || 0);
};

const formatCompact = (amount) => {
  return new Intl.NumberFormat("vi-VN", { notation: "compact", compactDisplay: "short" }).format(Number(amount) || 0);
};

const formatDateTime = (value) => {
  const date = toDate(value);
  if (!date) return "N/A";
  return date.toLocaleString("vi-VN");
};

const fetchSummary = async () => {
  const { data } = await orderApi.getAdminSummary();
  summary.value = data || summary.value;
};

const fetchAuditLogs = async () => {
  const { data } = await adminApi.getAuditLogs();
  auditLogs.value = Array.isArray(data) ? data : [];
};

const handleRecentOrdersPageChange = (page) => {
  recentOrdersPage.value = Math.max(1, Number(page) || 1);
};

const handleAuditLogsPageChange = (page) => {
  auditLogsPage.value = Math.max(1, Number(page) || 1);
};

const fetchOrders = async () => {
  loading.value = true;
  try {
    const [ordersRes] = await Promise.all([
      orderApi.getAllOrders(),
      fetchSummary(),
      fetchAuditLogs()
    ]);
    orders.value = Array.isArray(ordersRes?.data) ? ordersRes.data : [];
    recentOrdersPage.value = 1;
    auditLogsPage.value = 1;
  } catch (error) {
    console.error(error);
    ElMessage.error(error?.response?.data?.message || "Không tải được dữ liệu doanh thu");
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  fetchOrders();
});
</script>

<style scoped lang="scss">
.revenue-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
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

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.health-grid {
  margin-top: 12px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.dashboard-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.section-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;

  h3 {
    margin: 0;
    font-size: 18px;
    font-weight: 800;
    color: #0f172a;
  }

  span {
    font-size: 12px;
    color: #64748b;
  }
}

.kpi-card {
  border: 1px solid #e2e7ed;
  padding: 16px;
  background: #fff;

  .label {
    margin: 0 0 6px;
    color: #6b7280;
    font-size: 12px;
    text-transform: uppercase;
  }

  .value {
    margin: 0;
    font-size: 28px;
    font-weight: 800;
    color: #111827;
  }

  .helper {
    margin: 8px 0 0;
    color: #9aa2ac;
    font-size: 12px;
  }
}

.kpi-card--compact {
  .value {
    font-size: 24px;
  }
}

.analytics-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.data-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 12px;
}

.panel {
  border: 1px solid #dce1e7;
  background: #fff;
  padding: 14px;
  min-width: 0;
}

.table-pagination {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}

.drawer-inner {
  display: flex;
  flex-direction: column;
  gap: 12px;
  height: 100%;
}

.drawer-head {
  display: flex;
  align-items: center;
  justify-content: space-between;

  h3 {
    margin: 0;
    font-size: 18px;
    font-weight: 800;
  }
}

.table-scroll {
  width: 100%;
  overflow-x: auto;
}

.panel-head {
  margin-bottom: 12px;

  h3 {
    margin: 0;
    font-size: 16px;
    font-weight: 800;
  }
}

.status-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.row-top {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #475569;
  margin-bottom: 5px;
}

.bar-bg {
  width: 100%;
  height: 8px;
  background: #e9edf3;
  border-radius: 999px;
}

.bar-fill {
  height: 100%;
  border-radius: 999px;
}

.mini-chart {
  height: 240px;
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 10px;
  align-items: end;
}

.bar-col {
  display: grid;
  gap: 6px;
  justify-items: center;
}

.bar-wrap {
  height: 160px;
  width: 24px;
  background: #edf2f7;
  border-radius: 8px;
  display: flex;
  align-items: flex-end;
}

.bar {
  width: 100%;
  background: linear-gradient(180deg, #7db6ff 0%, #4d9dff 100%);
  border-radius: 8px;
}

.bar-label {
  margin: 0;
  font-size: 11px;
  color: #64748b;
}

.bar-value {
  margin: 0;
  font-size: 11px;
  color: #0f172a;
  font-weight: 600;
}

@media (max-width: 1200px) {
  .kpi-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .analytics-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 900px) {
  .head {
    flex-direction: column;
    align-items: flex-start;
  }

  .health-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 820px) {
  :deep(.audit-log-drawer) {
    width: 92% !important;
  }
}

@media (max-width: 680px) {
  .kpi-grid {
    grid-template-columns: 1fr;
  }
}
</style>

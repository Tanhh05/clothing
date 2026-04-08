<template>
  <div class="orders-page">
    <div class="orders-container">
      <div class="orders-header">
        <div>
          <h1>ĐƠN HÀNG CỦA TÔI</h1>
          <p v-if="!store.loading">
            {{ filteredOrders.length }} / {{ sortedOrders.length }} đơn
          </p>
        </div>
        <el-button :loading="store.loading" @click="store.fetchMyOrders">Làm mới</el-button>
      </div>

      <div v-if="store.loading" class="loading-wrap" v-loading="store.loading"></div>

      <el-alert
        v-else-if="store.error"
        :title="store.error"
        type="error"
        show-icon
        :closable="false"
      />

      <div v-else-if="sortedOrders.length === 0" class="empty-state">
        <h3>BẠN CHƯA CÓ ĐƠN HÀNG NÀO</h3>
        <p>Hãy thêm sản phẩm vào giỏ và tiến hành thanh toán để tạo đơn mới.</p>
        <router-link to="/products">
          <el-button type="primary" class="go-shopping-btn">MUA SẮM NGAY</el-button>
        </router-link>
      </div>

      <template v-else>
        <div class="overview-grid">
          <article class="overview-card">
            <p class="label">Tổng đơn</p>
            <p class="value">{{ sortedOrders.length }}</p>
          </article>
          <article class="overview-card">
            <p class="label">Đang xử lý</p>
            <p class="value">{{ inProgressCount }}</p>
          </article>
          <article class="overview-card">
            <p class="label">Đã giao</p>
            <p class="value">{{ deliveredCount }}</p>
          </article>
          <article class="overview-card">
            <p class="label">Đã hủy / lỗi</p>
            <p class="value">{{ failedCount }}</p>
          </article>
        </div>

        <div class="filters-panel">
          <el-input
            v-model="keyword"
            placeholder="Tìm theo mã đơn, sản phẩm, SKU, địa chỉ"
            clearable
            class="search-input"
          />

          <el-segmented v-model="statusFilter" :options="statusFilterOptions" class="status-segment" />

          <div class="filter-actions">
            <el-select v-model="pageSize" size="default" class="page-size-select">
              <el-option :value="10" label="10 / trang" />
              <el-option :value="20" label="20 / trang" />
              <el-option :value="30" label="30 / trang" />
            </el-select>
          </div>
        </div>

        <div v-if="pagedOrders.length === 0" class="empty-filter">
          Không tìm thấy đơn phù hợp với bộ lọc hiện tại.
        </div>

        <el-collapse v-else v-model="expandedOrders" class="order-collapse">
          <el-collapse-item
            v-for="order in pagedOrders"
            :key="order.id"
            :name="String(order.id)"
            class="order-collapse-item"
          >
            <template #title>
              <div class="order-summary">
                <div class="summary-main">
                  <strong>#{{ order.id }}</strong>
                  <span>{{ formatDateTime(order.createdAt) }}</span>
                </div>
                <div class="summary-meta">
                  <span>{{ itemCount(order) }} sản phẩm</span>
                  <strong>{{ formatCurrency(order.totalPrice) }}</strong>
                  <el-tag :type="statusType(order.status)" effect="dark" size="small">
                    {{ statusLabel(order.status) }}
                  </el-tag>
                </div>
              </div>
            </template>

            <div class="order-detail-grid">
              <div class="meta-block">
                <p><strong>Thanh toán:</strong> {{ paymentLabel(order.paymentMethod) }}</p>
                <p><strong>Địa chỉ:</strong> {{ order.address || "N/A" }}</p>
              </div>

              <div class="items-block">
                <h4>Sản phẩm</h4>
                <div v-for="item in order.items || []" :key="item.id" class="item-row">
                  <div class="item-left">
                    <span class="name">{{ item.productName || "Sản phẩm" }}</span>
                    <span class="sku">{{ item.sku || "-" }}</span>
                    <span class="qty">x{{ item.quantity }}</span>
                  </div>
                  <span class="line-total">{{ formatCurrency(item.lineTotal) }}</span>
                </div>
              </div>
            </div>

            <div v-if="order.statusHistory?.length" class="order-history">
              <h4>Tiến trình đơn hàng</h4>
              <el-timeline>
                <el-timeline-item
                  v-for="(step, idx) in orderedHistory(order.statusHistory)"
                  :key="`${order.id}-${idx}-${step.status}`"
                  :timestamp="formatDateTime(step.changedAt)"
                  placement="top"
                >
                  {{ statusLabel(step.status) }}
                </el-timeline-item>
              </el-timeline>
            </div>
          </el-collapse-item>
        </el-collapse>

        <div class="pagination-wrap">
          <el-pagination
            layout="total, prev, pager, next"
            :total="filteredOrders.length"
            :current-page="currentPage"
            :page-size="pageSize"
            @current-change="handlePageChange"
          />
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from "vue";
import { useOrderStore } from "@/modules/order/store/orderStore";

const store = useOrderStore();

const keyword = ref("");
const statusFilter = ref("ALL");
const pageSize = ref(10);
const currentPage = ref(1);
const expandedOrders = ref([]);

const statusMap = {
  PENDING: "Chờ xử lý",
  PROCESSING: "Đang xử lý",
  CONFIRMED: "Đã xác nhận",
  SHIPPED: "Đang giao",
  DELIVERED: "Đã giao",
  CANCELLED: "Đã hủy",
  FAILED: "Thất bại",
  FAILED_INSUFFICIENT_STOCK: "Thất bại - hết hàng"
};

const statusFilterOptions = computed(() => {
  const entries = Object.entries(statusMap).map(([value, label]) => ({ label, value }));
  return [{ label: "Tất cả", value: "ALL" }, ...entries];
});

const sortedOrders = computed(() => {
  return [...store.orders].sort((a, b) => {
    const timeA = new Date(a?.createdAt || 0).getTime();
    const timeB = new Date(b?.createdAt || 0).getTime();
    if (timeA !== timeB) return timeB - timeA;
    return (b?.id || 0) - (a?.id || 0);
  });
});

const containsKeyword = (order, query) => {
  const textChunks = [
    String(order?.id || ""),
    String(order?.address || ""),
    String(order?.paymentMethod || ""),
    ...(Array.isArray(order?.items)
      ? order.items.flatMap((item) => [String(item?.productName || ""), String(item?.sku || "")])
      : [])
  ];
  return textChunks.join(" ").toLowerCase().includes(query);
};

const filteredOrders = computed(() => {
  const query = keyword.value.trim().toLowerCase();
  return sortedOrders.value.filter((order) => {
    const byStatus = statusFilter.value === "ALL" || String(order?.status || "").toUpperCase() === statusFilter.value;
    if (!byStatus) return false;
    if (!query) return true;
    return containsKeyword(order, query);
  });
});

const pagedOrders = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  return filteredOrders.value.slice(start, start + pageSize.value);
});

const inProgressCount = computed(() => {
  const progressStatuses = new Set(["PENDING", "PROCESSING", "CONFIRMED", "SHIPPED"]);
  return sortedOrders.value.filter((order) => progressStatuses.has(String(order?.status || "").toUpperCase())).length;
});

const deliveredCount = computed(() => {
  return sortedOrders.value.filter((order) => String(order?.status || "").toUpperCase() === "DELIVERED").length;
});

const failedCount = computed(() => {
  const failedStatuses = new Set(["FAILED", "FAILED_INSUFFICIENT_STOCK", "CANCELLED"]);
  return sortedOrders.value.filter((order) => failedStatuses.has(String(order?.status || "").toUpperCase())).length;
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
  if ((method || "").toUpperCase() === "COD") return "Thanh toán khi nhận hàng (COD)";
  if ((method || "").toUpperCase() === "MOMO") return "Ví điện tử MoMo";
  return method || "N/A";
};

const statusLabel = (status) => {
  return statusMap[status] || status || "N/A";
};

const statusType = (status) => {
  const value = (status || "").toUpperCase();
  if (["DELIVERED", "CONFIRMED"].includes(value)) return "success";
  if (["SHIPPED", "PROCESSING", "PENDING"].includes(value)) return "warning";
  if (["FAILED", "FAILED_INSUFFICIENT_STOCK", "CANCELLED"].includes(value)) return "danger";
  return "info";
};

const orderedHistory = (history) => {
  return [...(history || [])].sort((a, b) => new Date(b.changedAt) - new Date(a.changedAt));
};

const itemCount = (order) => {
  if (!Array.isArray(order?.items)) return 0;
  return order.items.reduce((sum, item) => sum + (Number(item?.quantity) || 0), 0);
};

const handlePageChange = (page) => {
  currentPage.value = Math.max(1, Number(page) || 1);
};

watch([keyword, statusFilter, pageSize], () => {
  currentPage.value = 1;
  expandedOrders.value = [];
});

watch(filteredOrders, (orders) => {
  const maxPage = Math.max(1, Math.ceil(orders.length / pageSize.value));
  if (currentPage.value > maxPage) {
    currentPage.value = maxPage;
  }
});

onMounted(() => {
  store.fetchMyOrders();
});
</script>

<style scoped lang="scss">
.orders-page {
  padding: 28px 0 56px;
}

.orders-container {
  max-width: 1120px;
  margin: 0 auto;
  padding: 0 20px;
}

.orders-header {
  margin-bottom: 14px;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 10px;

  h1 {
    margin: 0 0 6px;
    font-size: 30px;
    font-weight: 900;
    letter-spacing: -0.3px;
  }

  p {
    margin: 0;
    color: #64748b;
  }
}

.loading-wrap {
  min-height: 200px;
}

.empty-state {
  text-align: center;
  padding: 80px 20px;

  h3 {
    margin: 0 0 10px;
    font-size: 24px;
    font-weight: 900;
  }

  p {
    margin: 0 0 20px;
    color: #666;
  }

  .go-shopping-btn {
    border-radius: 0;
    background: #000;
    border-color: #000;
    font-weight: 800;
  }
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 12px;
}

.overview-card {
  border: 1px solid #e2e8f0;
  background: #fff;
  padding: 12px;

  .label {
    margin: 0;
    font-size: 12px;
    color: #64748b;
    text-transform: uppercase;
    letter-spacing: 0.3px;
  }

  .value {
    margin: 6px 0 0;
    font-size: 24px;
    font-weight: 900;
    line-height: 1;
  }
}

.filters-panel {
  border: 1px solid #e2e8f0;
  background: #f8fafc;
  padding: 10px;
  margin-bottom: 12px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
}

.search-input {
  width: 100%;
}

.status-segment {
  grid-column: 1 / -1;
  width: fit-content;
  max-width: 100%;
}

.filter-actions {
  justify-self: end;
}

.page-size-select {
  width: 120px;
}

.empty-filter {
  border: 1px dashed #cbd5e1;
  padding: 14px;
  text-align: center;
  color: #64748b;
  background: #fff;
}

.order-collapse {
  border: 0;
}

:deep(.order-collapse .el-collapse-item__header) {
  height: auto;
  line-height: normal;
  padding: 10px 0;
  border-bottom: 1px solid #eef2f7;
}

:deep(.order-collapse .el-collapse-item__wrap) {
  border-bottom: 1px solid #eef2f7;
}

:deep(.order-collapse .el-collapse-item__content) {
  padding-bottom: 10px;
}

.order-summary {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.summary-main {
  display: flex;
  flex-direction: column;
  gap: 3px;

  strong {
    font-size: 16px;
    color: #0f172a;
  }

  span {
    font-size: 12px;
    color: #64748b;
  }
}

.summary-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
  font-size: 13px;

  strong {
    font-size: 15px;
    color: #0f172a;
  }
}

.order-detail-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 10px;
  margin-bottom: 8px;
}

.meta-block {
  display: grid;
  gap: 4px;
  font-size: 14px;

  p {
    margin: 0;
    color: #334155;
    line-height: 1.4;
  }
}

.items-block {
  h4 {
    margin: 0 0 8px;
    font-size: 13px;
    text-transform: uppercase;
    font-weight: 800;
    color: #334155;
  }
}

.item-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  padding: 8px 0;
  border-top: 1px dashed #e2e8f0;

  .item-left {
    display: grid;
    gap: 2px;
    min-width: 0;
  }

  .name {
    font-weight: 700;
    font-size: 13px;
    word-break: break-word;
  }

  .sku {
    color: #64748b;
    font-size: 12px;
    word-break: break-all;
  }

  .qty {
    color: #64748b;
    font-size: 12px;
  }

  .line-total {
    font-size: 13px;
    font-weight: 700;
    white-space: nowrap;
  }
}

.order-history {
  margin-top: 8px;

  h4 {
    margin: 0 0 8px;
    font-size: 13px;
    text-transform: uppercase;
    font-weight: 800;
    color: #334155;
  }
}

.pagination-wrap {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 900px) {
  .overview-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .filters-panel {
    grid-template-columns: 1fr;
  }

  .filter-actions {
    justify-self: start;
  }
}

@media (max-width: 700px) {
  .orders-page {
    padding: 20px 0 40px;
  }

  .orders-container {
    padding: 0 12px;
  }

  .orders-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .orders-header h1 {
    font-size: 24px;
  }

  .overview-grid {
    grid-template-columns: 1fr;
  }

  .summary-meta {
    justify-content: flex-start;
  }

  .pagination-wrap {
    justify-content: center;
  }
}
</style>

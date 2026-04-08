<template>
  <section class="notification-client-page">
    <header class="page-head">
      <div>
        <h1>TRUNG TÂM THÔNG BÁO</h1>
        <p>Cập nhật khuyến mãi, trạng thái đơn hàng và tin tức hệ thống của bạn.</p>
      </div>
      <div class="head-actions">
        <el-button @click="fetchNotifications" :loading="loading">Làm mới</el-button>
        <el-button type="primary" @click="markAllAsRead" :disabled="unreadCount === 0">Đánh dấu đã đọc tất cả</el-button>
      </div>
    </header>

    <section class="filters">
      <el-segmented v-model="statusFilter" :options="filterOptions" />
      <el-input v-model="keyword" placeholder="Tìm theo tiêu đề hoặc nội dung" clearable class="search-input" />
    </section>

    <div v-if="loading" class="loading-wrap" v-loading="loading"></div>

    <el-alert
      v-else-if="errorMessage"
      :title="errorMessage"
      type="error"
      show-icon
      :closable="false"
    />

    <div v-else-if="filteredNotifications.length === 0" class="empty-state">
      <h3>CHƯA CÓ THÔNG BÁO PHÙ HỢP</h3>
      <p>Không có dữ liệu tương ứng với bộ lọc hiện tại.</p>
    </div>

    <div v-else class="notification-list">
      <article
        v-for="item in filteredNotifications"
        :key="item.id"
        class="notification-card"
        :class="{ unread: !item.isRead }"
      >
        <div class="card-main">
          <h4>{{ item.title || "Thông báo" }}</h4>
          <p>{{ item.content || "Không có nội dung" }}</p>
          <small>{{ formatDateTime(item.createdAt) }}</small>
        </div>
        <div class="card-actions">
          <el-tag size="small" :type="item.isRead ? 'info' : 'danger'" effect="light">
            {{ item.isRead ? "Đã đọc" : "Chưa đọc" }}
          </el-tag>
          <el-button
            v-if="!item.isRead"
            text
            size="small"
            @click="markAsRead(item)"
          >
            Đánh dấu đã đọc
          </el-button>
        </div>
      </article>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { ElMessage } from "element-plus";
import { notificationClientApi } from "@/modules/notification/api/notificationClientApi";

const notifications = ref([]);
const loading = ref(false);
const errorMessage = ref("");
const statusFilter = ref("ALL");
const keyword = ref("");

const filterOptions = [
  { label: "Tất cả", value: "ALL" },
  { label: "Chưa đọc", value: "UNREAD" },
  { label: "Đã đọc", value: "READ" }
];

const unreadCount = computed(() => notifications.value.filter((item) => !item.isRead).length);

const formatDateTime = (value) => {
  if (!value) return "N/A";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value);
  return date.toLocaleString("vi-VN");
};

const filteredNotifications = computed(() => {
  const term = keyword.value.trim().toLowerCase();
  return notifications.value.filter((item) => {
    if (statusFilter.value === "UNREAD" && item.isRead) return false;
    if (statusFilter.value === "READ" && !item.isRead) return false;
    if (!term) return true;
    const text = `${item.title || ""} ${item.content || ""}`.toLowerCase();
    return text.includes(term);
  });
});

const fetchNotifications = async () => {
  loading.value = true;
  errorMessage.value = "";
  try {
    const { data } = await notificationClientApi.getMyNotifications();
    notifications.value = Array.isArray(data) ? data : [];
  } catch (error) {
    errorMessage.value = error?.response?.data?.message || "Không thể tải thông báo";
  } finally {
    loading.value = false;
  }
};

const markAsRead = async (item) => {
  if (!item || item.isRead) return;
  try {
    await notificationClientApi.markAsRead(item.id);
    item.isRead = true;
  } catch (error) {
    ElMessage.error("Không thể cập nhật trạng thái thông báo");
  }
};

const markAllAsRead = async () => {
  try {
    await notificationClientApi.markAllAsRead();
    notifications.value = notifications.value.map((item) => ({ ...item, isRead: true }));
    ElMessage.success("Đã đánh dấu tất cả là đã đọc");
  } catch (error) {
    ElMessage.error("Không thể đánh dấu tất cả là đã đọc");
  }
};

onMounted(fetchNotifications);
</script>

<style scoped lang="scss">
.notification-client-page {
  max-width: 980px;
  margin: 0 auto;
  padding: 30px 20px 50px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  border: 1px solid #e2e8f0;
  background: #fff;
  padding: 16px;

  h1 {
    margin: 0 0 8px;
    font-size: 28px;
    font-weight: 900;
    letter-spacing: -0.4px;
  }

  p {
    margin: 0;
    color: #64748b;
  }
}

.head-actions {
  display: flex;
  gap: 8px;
}

.filters {
  border: 1px solid #e2e8f0;
  background: #fff;
  padding: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

.search-input {
  max-width: 360px;
}

.loading-wrap {
  min-height: 180px;
}

.empty-state {
  text-align: center;
  padding: 64px 20px;
  border: 1px solid #e2e8f0;
  background: #fff;

  h3 {
    margin: 0 0 8px;
    font-size: 22px;
    font-weight: 900;
  }

  p {
    margin: 0;
    color: #64748b;
  }
}

.notification-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.notification-card {
  border: 1px solid #e2e8f0;
  background: #fff;
  padding: 14px;
  display: flex;
  justify-content: space-between;
  gap: 12px;

  &.unread {
    border-color: #111827;
    box-shadow: inset 3px 0 0 #111827;
  }
}

.card-main {
  h4 {
    margin: 0 0 6px;
    font-size: 16px;
    font-weight: 800;
  }

  p {
    margin: 0 0 8px;
    color: #1f2937;
    white-space: pre-wrap;
  }

  small {
    color: #64748b;
  }
}

.card-actions {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6px;
}

@media (max-width: 768px) {
  .page-head,
  .filters,
  .notification-card {
    flex-direction: column;
    align-items: flex-start;
  }

  .search-input {
    max-width: 100%;
    width: 100%;
  }

  .head-actions {
    width: 100%;
    flex-wrap: wrap;
  }
}
</style>

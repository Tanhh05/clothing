<template>
  <section class="customer-admin-page" v-loading="loading">
    <header class="management-head">
      <div>
        <p class="eyebrow">Admin panel</p>
        <h2>Customer Management</h2>
        <p class="sub-text">Quản lý trạng thái tài khoản khách hàng và theo dõi dữ liệu người dùng.</p>
      </div>
      <div class="head-actions">
        <el-button @click="fetchCustomers">Làm mới</el-button>
      </div>
    </header>

    <div class="overview-grid">
      <article class="overview-card">
        <p class="label">Total users</p>
        <p class="value">{{ stats.total }}</p>
        <p class="helper">Tổng tài khoản trong hệ thống</p>
      </article>
      <article class="overview-card">
        <p class="label">Active</p>
        <p class="value">{{ stats.active }}</p>
        <p class="helper">Tài khoản đang hoạt động</p>
      </article>
      <article class="overview-card">
        <p class="label">Inactive</p>
        <p class="value">{{ stats.inactive }}</p>
        <p class="helper">Tài khoản đã khóa/tạm dừng</p>
      </article>
    </div>

    <div class="panel">
      <div class="panel-head">
        <el-input
          v-model="keyword"
          clearable
          placeholder="Tìm theo username / email / tên / SĐT"
          class="search-input"
          @keyup.enter="fetchCustomers"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-select v-model="statusFilter" style="width: 160px">
          <el-option label="Tất cả trạng thái" value="" />
          <el-option label="ACTIVE" value="ACTIVE" />
          <el-option label="INACTIVE" value="INACTIVE" />
        </el-select>
        <el-button type="primary" @click="fetchCustomers">Tìm</el-button>
      </div>

      <div class="table-wrap">
        <el-table
          :data="customers"
          border
          stripe
          table-layout="fixed"
          empty-text="Không có khách hàng"
          class="customer-table"
        >
          <el-table-column prop="id" label="#" width="72" />
          <el-table-column prop="username" label="Username" min-width="130" show-overflow-tooltip />
          <el-table-column prop="fullName" label="Họ tên" min-width="150" show-overflow-tooltip />
          <el-table-column prop="email" label="Email" min-width="210" show-overflow-tooltip />
          <el-table-column prop="phone" label="SĐT" min-width="120" show-overflow-tooltip />
          <el-table-column label="Vai trò" min-width="130">
            <template #default="{ row }">
              <el-tag
                v-for="role in roleList(row.roles)"
                :key="`${row.id}-${role}`"
                size="small"
                class="role-tag"
                :type="role === 'ADMIN' ? 'warning' : 'info'"
              >
                {{ role }}
              </el-tag>
            </template>
          </el-table-column>
          <!-- <el-table-column label="Trạng thái" width="120">
            <template #default="{ row }">
              <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'">
                {{ row.status || "N/A" }}
              </el-tag>
            </template>
          </el-table-column> -->
          <el-table-column label="Ngày tạo" width="162">
            <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="Thao tác" width="120">
            <template #default="{ row }">
              <el-button
                size="small"
                :type="row.status === 'ACTIVE' ? 'danger' : 'success'"
                plain
                :loading="updatingId === row.id"
                @click="toggleStatus(row)"
              >
                {{ row.status === "ACTIVE" ? "Khóa" : "Mở" }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="mobile-list">
        <article v-for="row in customers" :key="`mobile-${row.id}`" class="mobile-card">
          <div class="card-head">
            <p class="card-name">{{ row.fullName || row.username || "N/A" }}</p>
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'">
              {{ row.status || "N/A" }}
            </el-tag>
          </div>
          <p class="card-line"><strong>ID:</strong> #{{ row.id }}</p>
          <p class="card-line"><strong>Username:</strong> {{ row.username || "N/A" }}</p>
          <p class="card-line"><strong>Email:</strong> {{ row.email || "N/A" }}</p>
          <p class="card-line"><strong>SĐT:</strong> {{ row.phone || "N/A" }}</p>
          <p class="card-line">
            <strong>Vai trò:</strong>
            <span class="mobile-roles">
              <el-tag
                v-for="role in roleList(row.roles)"
                :key="`mobile-${row.id}-${role}`"
                size="small"
                :type="role === 'ADMIN' ? 'warning' : 'info'"
              >
                {{ role }}
              </el-tag>
            </span>
          </p>
          <p class="card-line"><strong>Ngày tạo:</strong> {{ formatDateTime(row.createdAt) }}</p>
          <el-button
            size="small"
            class="mobile-action"
            :type="row.status === 'ACTIVE' ? 'danger' : 'success'"
            plain
            :loading="updatingId === row.id"
            @click="toggleStatus(row)"
          >
            {{ row.status === "ACTIVE" ? "Khóa tài khoản" : "Mở tài khoản" }}
          </el-button>
        </article>
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
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { ElMessage } from "element-plus";
import { Search } from "@element-plus/icons-vue";
import { customerApi } from "@/modules/customer/api/customerApi";

const loading = ref(false);
const updatingId = ref(null);
const customers = ref([]);

const page = ref(0);
const size = ref(20);
const totalElements = ref(0);
const keyword = ref("");
const statusFilter = ref("");

const roleList = (roles) => (Array.isArray(roles) ? roles : []);

const stats = computed(() => {
  const rows = customers.value;
  return {
    total: totalElements.value,
    active: rows.filter((u) => String(u?.status || "").toUpperCase() === "ACTIVE").length,
    inactive: rows.filter((u) => String(u?.status || "").toUpperCase() === "INACTIVE").length
  };
});

const formatDateTime = (value) => {
  if (!value) return "N/A";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString("vi-VN");
};

const fetchCustomers = async () => {
  loading.value = true;
  try {
    const { data } = await customerApi.getCustomers({
      page: page.value,
      size: size.value,
      sortBy: "id",
      direction: "desc",
      q: keyword.value?.trim() || undefined,
      status: statusFilter.value || undefined
    });
    customers.value = data?.content || [];
    totalElements.value = Number(data?.totalElements || 0);
  } catch (error) {
    console.error(error);
    ElMessage.error(error?.response?.data?.message || "Không tải được danh sách khách hàng");
  } finally {
    loading.value = false;
  }
};

const toggleStatus = async (row) => {
  const current = String(row?.status || "").toUpperCase();
  const next = current === "ACTIVE" ? "INACTIVE" : "ACTIVE";
  updatingId.value = row.id;
  try {
    const { data } = await customerApi.updateCustomerStatus(row.id, next);
    const index = customers.value.findIndex((item) => item.id === row.id);
    if (index !== -1) customers.value[index] = data;
    ElMessage.success(next === "ACTIVE" ? "Đã mở tài khoản" : "Đã khóa tài khoản");
  } catch (error) {
    console.error(error);
    ElMessage.error(error?.response?.data?.message || "Cập nhật trạng thái thất bại");
  } finally {
    updatingId.value = null;
  }
};

const handlePageChange = (nextPage) => {
  page.value = Math.max(0, nextPage - 1);
  fetchCustomers();
};

onMounted(() => {
  fetchCustomers();
});
</script>

<style scoped lang="scss">
.customer-admin-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.management-head {
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
    letter-spacing: 0.3px;
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

.overview-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.overview-card {
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
    font-size: 30px;
    font-weight: 800;
    color: #111827;
  }

  .helper {
    margin: 8px 0 0;
    color: #9aa2ac;
    font-size: 12px;
  }
}

.panel {
  border: 1px solid #dce1e7;
  background: #fff;
  padding: 16px;
}

.panel-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
}

.search-input {
  max-width: 420px;
  width: 100%;
}

.role-tag + .role-tag {
  margin-left: 6px;
}

.table-wrap {
  width: 100%;
  overflow: hidden;
}

.customer-table {
  width: 100%;
}

.mobile-list {
  display: none;
}

.mobile-card {
  border: 1px solid #dce1e7;
  border-radius: 10px;
  padding: 12px;
  background: #fff;
}

.mobile-card + .mobile-card {
  margin-top: 10px;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
}

.card-name {
  margin: 0;
  font-size: 14px;
  font-weight: 700;
  color: #111827;
}

.card-line {
  margin: 0 0 6px;
  font-size: 12px;
  color: #374151;
  word-break: break-word;
}

.mobile-roles {
  display: inline-flex;
  gap: 6px;
  flex-wrap: wrap;
  margin-left: 6px;
}

.mobile-action {
  width: 100%;
  margin-top: 6px;
}

.pagination-wrap {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 900px) {
  .overview-grid {
    grid-template-columns: 1fr;
  }

  .management-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .panel-head {
    flex-wrap: wrap;
  }

  .panel-head :deep(.el-select) {
    width: 100% !important;
  }

  .table-wrap {
    display: none;
  }

  .mobile-list {
    display: block;
  }

  .pagination-wrap {
    justify-content: center;
  }
}
</style>

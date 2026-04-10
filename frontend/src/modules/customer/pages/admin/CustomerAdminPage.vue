<template>
  <section class="customer-admin-page admin-page-shell" v-loading="loading">
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
        <el-button @click="fetchCustomers">Làm mới</el-button>
      </div>

      <div class="table-wrap">
        <BaseTable
          :data="customers"
          border
          stripe
          size="small"
          table-layout="fixed"
          empty-text="Không có khách hàng"
          class="customer-table"
        >
          <el-table-column label="Khách hàng" min-width="290">
            <template #default="{ row }">
              <div class="customer-cell">
                <strong>#{{ row.id }} · {{ row.fullName || row.username || "N/A" }}</strong>
                <span>@{{ row.username || "N/A" }}</span>
                <span>{{ row.email || "N/A" }}</span>
                <span>{{ row.phone || "N/A" }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="Vai trò / Trạng thái" min-width="180">
            <template #default="{ row }">
              <div class="role-status-cell">
                <div class="role-row">
                  <el-tag
                    v-for="role in roleList(row.roles)"
                    :key="`${row.id}-${role}`"
                    size="small"
                    class="role-tag"
                    :type="role === 'ADMIN' ? 'warning' : 'info'"
                  >
                    {{ role }}
                  </el-tag>
                </div>
                <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'" size="small">
                  {{ row.status || "N/A" }}
                </el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="Ngày tạo" width="162">
            <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="Thao tác" width="135">
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
        </BaseTable>
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
import { onMounted, ref } from "vue";
import { ElMessage } from "element-plus";
import { Search } from "@element-plus/icons-vue";
import { customerApi } from "@/modules/customer/api/customerApi";

const loading = ref(false);
const updatingId = ref(null);
const customers = ref([]);

const page = ref(0);
const size = ref(10);
const totalElements = ref(0);
const keyword = ref("");
const statusFilter = ref("");

const roleList = (roles) => (Array.isArray(roles) ? roles : []);

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
  gap: 10px;
}

.panel {
  border: 1px solid #dce1e7;
  background: #fff;
  padding: 10px;
}

.panel-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
  flex-wrap: wrap;
}

.search-input {
  max-width: 420px;
  width: 100%;
}

.role-tag + .role-tag {
  margin-left: 6px;
}

.customer-cell {
  display: grid;
  gap: 2px;
}

.customer-cell strong {
  font-size: 13px;
  color: #111827;
  word-break: break-word;
}

.customer-cell span {
  font-size: 12px;
  color: #64748b;
  word-break: break-word;
}

.role-status-cell {
  display: grid;
  gap: 6px;
}

.role-row {
  display: inline-flex;
  flex-wrap: wrap;
  gap: 6px;
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
  margin-top: 10px;
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 900px) {
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

<template>
  <section v-loading="loading" class="customers-admin-page admin-page-shell admin-page">
    <div class="admin-card inventory-panel">
      <div class="panel-header">
        <div class="panel-actions admin-toolbar">
          <el-input
            v-model="keyword"
            clearable
            placeholder="Tìm theo username/email/tên/sđt"
            class="search-input"
            @keyup.enter.native="reload(0)"
          >
            <i slot="prefix" class="el-input__icon el-icon-search" />
          </el-input>
          <div class="action-buttons">
            <el-select v-model="status" clearable placeholder="Trạng thái" class="status-select" @change="reload(0)">
              <el-option label="Đang hoạt động" value="ACTIVE" />
              <el-option label="Ngưng hoạt động" value="INACTIVE" />
            </el-select>
            <el-button class="admin-ghost-btn" @click="reload(0)">Làm mới</el-button>
          </div>
        </div>
      </div>

      <div class="table-wrap">
        <el-table
          :data="customers"
          border
          stripe
          :size="elementSize"
          class="inventory-table admin-table"
          empty-text="Chưa có khách hàng"
          table-layout="fixed"
        >
          <el-table-column label="ID" width="90" prop="id" />
          <el-table-column label="Tài khoản" min-width="170" prop="username" />
          <el-table-column label="Họ tên" min-width="170" prop="fullName" />
          <el-table-column label="Email" min-width="220" prop="email" />
          <el-table-column label="Số điện thoại" min-width="140" prop="phone" />
          <el-table-column label="Trạng thái" width="130" align="center">
            <template slot-scope="{ row }">
              <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">{{ formatStatus(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="Vai trò" width="170">
            <template slot-scope="{ row }">
              {{ Array.isArray(row.roles) ? row.roles.join(', ') : '-' }}
            </template>
          </el-table-column>
          <el-table-column label="Thao tác" width="120" align="center">
            <template slot-scope="{ row }">
              <div class="admin-action-cell">
                <el-tooltip :content="row.status === 'ACTIVE' ? 'Khóa khách hàng' : 'Mở khách hàng'" placement="top">
                  <el-button
                    :size="elementSize"
                    :icon="row.status === 'ACTIVE' ? 'el-icon-lock' : 'el-icon-unlock'"
                    :type="row.status === 'ACTIVE' ? 'warning' : 'success'"
                    class="admin-action-btn"
                    circle
                    :loading="updatingId === row.id"
                    @click="toggleStatus(row)"
                  />
                </el-tooltip>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <pagination
        class="admin-pagination"
        :total="totalElements"
        :page.sync="currentPage"
        :limit.sync="pageSize"
        @pagination="handlePagination"
      />
    </div>
  </section>
</template>

<script>
import { fetchAdminCustomers, updateAdminCustomerStatus } from '@/api/admin-management'
import Pagination from '@/components/Pagination'

export default {
  name: 'CustomersManagement',
  components: { Pagination },
  data() {
    return {
      loading: false,
      updatingId: null,
      customers: [],
      keyword: '',
      status: '',
      pageSize: 20,
      totalElements: 0,
      currentPage: 1
    }
  },
  computed: {
    elementSize() {
      const size = this.$store.getters.size
      return size === 'default' ? undefined : size
    }
  },
  created() {
    this.reload()
  },
  methods: {
    toSafePage(value, fallback = 0) {
      const n = Number(value)
      return Number.isFinite(n) && n >= 0 ? Math.floor(n) : fallback
    },
    async reload(page = 0) {
      const safePage = this.toSafePage(page, 0)
      this.loading = true
      try {
        const data = await fetchAdminCustomers({
          page: safePage,
          size: this.pageSize,
          q: this.keyword ? this.keyword.trim() : undefined,
          status: this.status || undefined
        })
        this.customers = Array.isArray(data && data.content) ? data.content : []
        this.totalElements = Number((data && data.totalElements) || 0)
        const responsePage = this.toSafePage(data && data.page, safePage)
        this.currentPage = responsePage + 1
      } catch (error) {
        this.$message.error((error && error.response && error.response.data && error.response.data.message) || 'Không tải được khách hàng')
      } finally {
        this.loading = false
      }
    },
    handlePagination({ page, limit }) {
      this.pageSize = Number(limit) || this.pageSize
      this.reload(this.toSafePage(Number(page) - 1, 0))
    },
    formatStatus(status) {
      const normalized = String(status || '').toUpperCase()
      if (normalized === 'ACTIVE') return 'Đang hoạt động'
      if (normalized === 'INACTIVE') return 'Ngưng hoạt động'
      return 'Không có'
    },
    async toggleStatus(row) {
      if (!row || !row.id) return
      const nextStatus = row.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
      this.updatingId = row.id
      try {
        await updateAdminCustomerStatus(row.id, nextStatus)
        this.$message.success(`Đã đổi trạng thái thành ${this.formatStatus(nextStatus)}`)
        await this.reload(this.toSafePage(this.currentPage - 1, 0))
      } catch (error) {
        this.$message.error((error && error.response && error.response.data && error.response.data.message) || 'Không cập nhật được trạng thái khách hàng')
      } finally {
        this.updatingId = null
      }
    }
  }
}
</script>

<style scoped>
.customers-admin-page {
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: 100%;
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

.table-wrap {
  width: 100%;
}

.inventory-table {
  width: 100%;
}
</style>

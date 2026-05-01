<template>
  <section v-loading="loading" class="admin-page">
    <div class="admin-card inventory-panel">
      <div class="admin-toolbar">
        <el-input
          v-model="keyword"
          clearable
          placeholder="Tìm theo mã đơn / khách hàng"
          class="search-input"
          @keyup.enter.native="reload(0)"
        >
          <i slot="prefix" class="el-input__icon el-icon-search" />
        </el-input>
        <el-select v-model="status" clearable placeholder="Trạng thái" class="status-select" @change="reload(0)">
          <el-option v-for="item in statusOptions" :key="item" :label="formatStatusLabel(item)" :value="item" />
        </el-select>
        <el-button class="admin-ghost-btn" @click="reload(0)">Làm mới</el-button>
      </div>

      <div v-if="selectedIds.length" class="admin-toolbar">
        <span>Đã chọn {{ selectedIds.length }} đơn</span>
        <el-select v-model="bulkStatus" clearable placeholder="Trạng thái mới" class="status-select">
          <el-option v-for="item in statusOptions" :key="`bulk-${item}`" :label="formatStatusLabel(item)" :value="item" />
        </el-select>
        <el-button type="primary" class="admin-primary-btn" :loading="bulkUpdating" @click="applyBulkStatus">Cập nhật hàng loạt</el-button>
      </div>

      <el-table
        :data="orders"
        border
        stripe
        :size="elementSize"
        class="admin-table"
        empty-text="Chưa có đơn hàng"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="48" />
        <el-table-column label="Đơn hàng" min-width="220">
          <template slot-scope="{ row }">
            <div class="title-cell">
              <strong>#{{ row.id || 'Không có' }}</strong>
              <span>{{ row.customerName || row.username || row.address || 'Khách lẻ' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="Tổng tiền" width="150">
          <template slot-scope="{ row }">
            {{ formatPrice(row.totalPrice) }}
          </template>
        </el-table-column>
        <el-table-column label="Thanh toán" width="130" prop="paymentMethod" />
        <el-table-column label="Vận chuyển" width="130" prop="shippingStatus" />
        <el-table-column label="Trạng thái" width="130">
          <template slot-scope="{ row }">
            <el-tag :type="statusTag(row.status)">{{ formatStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Tạo lúc" width="170">
          <template slot-scope="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
      </el-table>

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
import {
  fetchAdminOrders,
  fetchAdminOrderStatusOptions,
  bulkUpdateAdminOrderStatus
} from '@/api/admin-management'
import Pagination from '@/components/Pagination'

const DEFAULT_ORDER_STATUSES = ['PENDING', 'CONFIRMED', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED']
const ORDER_STATUS_LABELS = {
  PENDING: 'Chờ xác nhận',
  CONFIRMED: 'Đã xác nhận',
  PROCESSING: 'Đang xử lý',
  SHIPPED: 'Đang giao',
  DELIVERED: 'Đã giao',
  CANCELLED: 'Đã hủy',
  COMPLETED: 'Hoàn tất',
  FAILED: 'Thất bại',
  RETURNED: 'Đã hoàn trả'
}

export default {
  name: 'OrdersManagement',
  components: { Pagination },
  data() {
    return {
      loading: false,
      bulkUpdating: false,
      orders: [],
      keyword: '',
      status: '',
      bulkStatus: '',
      pageSize: 20,
      totalElements: 0,
      currentPage: 1,
      selectedIds: [],
      statusOptions: DEFAULT_ORDER_STATUSES
    }
  },
  computed: {
    elementSize() {
      const size = this.$store.getters.size
      return size === 'default' ? undefined : size
    }
  },
  created() {
    this.loadStatusOptions()
    this.reload()
  },
  methods: {
    async loadStatusOptions() {
      try {
        const data = await fetchAdminOrderStatusOptions()
        const fromApi = Array.isArray(data && data.statuses) ? data.statuses : []
        if (fromApi.length) {
          this.statusOptions = fromApi
        }
      } catch (error) {
        // Fallback to DEFAULT_ORDER_STATUSES
      }
    },
    async reload(page = 0) {
      this.loading = true
      try {
        const data = await fetchAdminOrders({
          page,
          size: this.pageSize,
          q: this.keyword ? this.keyword.trim() : undefined,
          status: this.status || undefined
        })
        this.orders = Array.isArray(data && data.content) ? data.content : []
        this.totalElements = Number((data && data.totalElements) || 0)
        this.currentPage = Number((data && data.page) || page) + 1
        this.selectedIds = []
      } catch (error) {
        this.$message.error((error && error.response && error.response.data && error.response.data.message) || 'Không tải được đơn hàng')
      } finally {
        this.loading = false
      }
    },
    handlePagination({ page, limit }) {
      this.pageSize = Number(limit) || this.pageSize
      this.reload(Math.max(0, Number(page) - 1))
    },
    handleSelectionChange(rows) {
      this.selectedIds = Array.isArray(rows) ? rows.map(item => item.id).filter(Boolean) : []
    },
    async applyBulkStatus() {
      if (!this.selectedIds.length || !this.bulkStatus) {
        this.$message.warning('Vui lòng chọn đơn hàng và trạng thái')
        return
      }
      this.bulkUpdating = true
      try {
        await bulkUpdateAdminOrderStatus(this.selectedIds, this.bulkStatus)
        this.$message.success('Đã cập nhật trạng thái hàng loạt')
        this.bulkStatus = ''
        await this.reload(Math.max(0, this.currentPage - 1))
      } catch (error) {
        this.$message.error((error && error.response && error.response.data && error.response.data.message) || 'Không cập nhật hàng loạt được')
      } finally {
        this.bulkUpdating = false
      }
    },
    statusTag(status) {
      const normalized = String(status || '').toUpperCase()
      if (normalized === 'DELIVERED') return 'success'
      if (normalized === 'CANCELLED') return 'danger'
      if (normalized === 'SHIPPED') return 'warning'
      return 'info'
    },
    formatStatusLabel(status) {
      const normalized = String(status || '').toUpperCase()
      return ORDER_STATUS_LABELS[normalized] || normalized || 'Không xác định'
    },
    formatPrice(value) {
      return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(Number(value) || 0)
    },
    formatDate(value) {
      if (!value) return 'Không có'
      const d = new Date(value)
      if (Number.isNaN(d.getTime())) return 'Không có'
      return d.toLocaleString('vi-VN')
    }
  }
}
</script>

<template>
  <div v-loading="loading" class="dashboard-page admin-page">
    <el-row :gutter="14" class="stats-row">
      <el-col :xs="24" :sm="12" :lg="6">
        <div class="stat-card admin-card">
          <p class="stat-label">Doanh thu hôm nay</p>
          <p class="stat-value">{{ formatPrice(summary.revenueToday) }}</p>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <div class="stat-card admin-card">
          <p class="stat-label">Doanh thu 7 ngày</p>
          <p class="stat-value">{{ formatPrice(summary.revenue7d) }}</p>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <div class="stat-card admin-card">
          <p class="stat-label">Doanh thu 30 ngày</p>
          <p class="stat-value">{{ formatPrice(summary.revenue30d) }}</p>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <div class="stat-card admin-card">
          <p class="stat-label">Đơn chờ xử lý</p>
          <p class="stat-value">{{ summary.pendingOrders || 0 }}</p>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="14" class="chart-row">
      <el-col :xs="24" :lg="16">
        <div class="chart-card admin-card">
          <div class="card-head">
            <h3>Xu hướng doanh thu 7 ngày</h3>
            <el-button type="text" @click="$router.push('/orders/index')">Xem đơn hàng</el-button>
          </div>
          <revenue-line-chart class-name="chart-box" :labels="revenueLabels" :values="revenueSeries" />
        </div>
      </el-col>
      <el-col :xs="24" :lg="8">
        <div class="chart-card admin-card">
          <div class="card-head">
            <h3>Trạng thái đơn 30 ngày</h3>
          </div>
          <order-status-pie-chart class-name="chart-box" :status-counts="summary.statusCounts30d" />
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="14">
      <el-col :xs="24" :lg="12">
        <div class="table-card admin-card">
          <div class="card-head">
            <h3>Top sản phẩm (30 ngày)</h3>
          </div>
          <el-table :data="summary.topProducts30d || []" border stripe :size="elementSize" class="admin-table" empty-text="Chưa có dữ liệu">
            <el-table-column label="Sản phẩm" min-width="220">
              <template slot-scope="{ row }">{{ row.productName || `#${row.productId || '-'}` }}</template>
            </el-table-column>
            <el-table-column label="Số lượng" width="120" align="right">
              <template slot-scope="{ row }">{{ row.totalQuantity || 0 }}</template>
            </el-table-column>
            <el-table-column label="Doanh thu" min-width="150" align="right">
              <template slot-scope="{ row }">{{ formatPrice(row.totalRevenue) }}</template>
            </el-table-column>
          </el-table>
        </div>
      </el-col>
      <el-col :xs="24" :lg="12">
        <div class="table-card admin-card">
          <div class="card-head">
            <h3>Top người mua (30 ngày)</h3>
          </div>
          <el-table :data="summary.topBuyers30d || []" border stripe :size="elementSize" class="admin-table" empty-text="Chưa có dữ liệu">
            <el-table-column label="Khách hàng" min-width="220">
              <template slot-scope="{ row }">{{ row.buyerName || `User #${row.userId || '-'}` }}</template>
            </el-table-column>
            <el-table-column label="Đơn đã mua" width="120" align="right">
              <template slot-scope="{ row }">{{ row.totalOrders || 0 }}</template>
            </el-table-column>
            <el-table-column label="Tổng chi" min-width="150" align="right">
              <template slot-scope="{ row }">{{ formatPrice(row.totalSpent) }}</template>
            </el-table-column>
          </el-table>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="14" class="mt-14">
      <el-col :xs="24">
        <div class="table-card admin-card">
          <div class="card-head">
            <h3>Đơn hàng gần nhất</h3>
          </div>
          <el-table :data="recentOrders" border stripe :size="elementSize" class="admin-table" empty-text="Chưa có đơn hàng">
            <el-table-column label="Mã đơn" width="100">
              <template slot-scope="{ row }">#{{ row.id || '-' }}</template>
            </el-table-column>
            <el-table-column label="Khách hàng" min-width="200">
              <template slot-scope="{ row }">
                {{ row.customerName || row.username || row.address || 'Khách lẻ' }}
              </template>
            </el-table-column>
            <el-table-column label="Tổng tiền" min-width="140">
              <template slot-scope="{ row }">{{ formatPrice(row.totalPrice) }}</template>
            </el-table-column>
            <el-table-column label="Thanh toán" width="130" prop="paymentMethod" />
            <el-table-column label="Trạng thái" width="130">
              <template slot-scope="{ row }">
                <el-tag :type="orderStatusTag(row.status)">{{ formatOrderStatusLabel(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="Ngày tạo" width="180">
              <template slot-scope="{ row }">{{ formatDate(row.createdAt) }}</template>
            </el-table-column>
          </el-table>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { fetchAdminOrders, fetchAdminSummary } from '@/api/admin-management'
import RevenueLineChart from '@/components/Charts/RevenueLineChart'
import OrderStatusPieChart from '@/components/Charts/OrderStatusPieChart'
import { getLocalJson, setLocalJson } from '@/utils/local-cache'

const DASHBOARD_CACHE_KEY = 'clothing_admin_dashboard_cache_v1'

export default {
  name: 'DashboardAdmin',
  components: {
    RevenueLineChart,
    OrderStatusPieChart
  },
  data() {
    return {
      loading: false,
      summary: {
        revenueToday: 0,
        revenue7d: 0,
        revenue30d: 0,
        pendingOrders: 0,
        statusCounts30d: {},
        topProducts30d: [],
        topBuyers30d: []
      },
      recentOrders: [],
      revenueSeries: [],
      revenueLabels: []
    }
  },
  computed: {
    elementSize() {
      const size = this.$store.getters.size
      return size === 'default' ? undefined : size
    }
  },
  created() {
    this.loadDashboard()
  },
  activated() {
    // Refresh when navigating back so revenue/orders reflects latest edits.
    if (this.loading) return
    this.loadDashboard()
  },
  methods: {
    async loadDashboard() {
      const cached = getLocalJson(DASHBOARD_CACHE_KEY, null)
      if (cached) {
        this.applyDashboardData(cached.summary, cached.orders)
      }
      this.loading = true
      try {
        const [summaryData, ordersData] = await Promise.all([
          fetchAdminSummary(),
          fetchAdminOrders({ page: 0, size: 100, sortBy: 'id', direction: 'desc' })
        ])

        const orders = Array.isArray(ordersData && ordersData.content) ? ordersData.content : []
        this.applyDashboardData(summaryData, orders)
        setLocalJson(DASHBOARD_CACHE_KEY, {
          summary: summaryData || this.summary,
          orders
        })
      } catch (error) {
        if (!cached) {
          this.$message.error('Không tải được dữ liệu tổng quan doanh thu')
        }
      } finally {
        this.loading = false
      }
    },
    applyDashboardData(summaryData, ordersData) {
      this.summary = summaryData || this.summary
      const orders = Array.isArray(ordersData) ? ordersData : []
      this.recentOrders = orders.slice(0, 10)
      this.buildRevenueSeries(orders)
    },
    buildRevenueSeries(orders) {
      const days = []
      const values = []
      const today = new Date()
      today.setHours(0, 0, 0, 0)

      for (let i = 6; i >= 0; i -= 1) {
        const day = new Date(today)
        day.setDate(today.getDate() - i)
        const nextDay = new Date(day)
        nextDay.setDate(day.getDate() + 1)

        const dayRevenue = orders
          .filter((order) => {
            const createdAt = new Date(order.createdAt)
            const status = String(order.status || '').toUpperCase()
            return !Number.isNaN(createdAt.getTime()) &&
              createdAt >= day &&
              createdAt < nextDay &&
              ['DELIVERED', 'COMPLETED'].includes(status)
          })
          .reduce((sum, order) => sum + Number(order.totalPrice || 0), 0)

        days.push(`${String(day.getDate()).padStart(2, '0')}/${String(day.getMonth() + 1).padStart(2, '0')}`)
        values.push(dayRevenue)
      }

      this.revenueLabels = days
      this.revenueSeries = values
    },
    formatPrice(value) {
      return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(Number(value) || 0)
    },
    formatDate(value) {
      if (!value) return 'Không có'
      const date = new Date(value)
      if (Number.isNaN(date.getTime())) return 'Không có'
      return date.toLocaleString('vi-VN')
    },
    orderStatusTag(status) {
      const normalized = String(status || '').toUpperCase()
      if (normalized === 'DELIVERED' || normalized === 'COMPLETED') return 'success'
      if (normalized === 'CANCELLED') return 'danger'
      if (normalized === 'SHIPPED') return 'warning'
      return 'info'
    },
    formatOrderStatusLabel(status) {
      const normalized = String(status || '').toUpperCase()
      if (normalized === 'PENDING') return 'Chờ xác nhận'
      if (normalized === 'CONFIRMED') return 'Đã xác nhận'
      if (normalized === 'PROCESSING') return 'Đang xử lý'
      if (normalized === 'SHIPPED') return 'Đang giao'
      if (normalized === 'DELIVERED') return 'Đã giao'
      if (normalized === 'COMPLETED') return 'Hoàn tất'
      if (normalized === 'CANCELLED') return 'Đã hủy'
      return 'Không có'
    }
  }
}
</script>

<style lang="scss" scoped>
.dashboard-page {
  padding: 20px;
  background: #f0f2f5;
  min-height: calc(100vh - 84px);
}

.stats-row {
  margin-bottom: 14px;
}

.stat-card {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 0;
  padding: 14px;
  margin-bottom: 12px;
}

.stat-label {
  margin: 0 0 8px;
  color: #909399;
  font-size: 13px;
}

.stat-value {
  margin: 0;
  color: #303133;
  font-size: 26px;
  font-weight: 700;
}

.chart-row {
  margin-bottom: 14px;
}

.mt-14 {
  margin-top: 14px;
  margin-bottom: 14px;
}

.chart-card,
.table-card {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 0;
  padding: 14px;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;

  h3 {
    margin: 0;
    font-size: 16px;
    color: #303133;
  }
}

.chart-box {
  width: 100%;
  height: 360px;
}
</style>

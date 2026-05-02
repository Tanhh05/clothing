<template>
  <section v-loading="loading" class="inventory-admin-page admin-page-shell admin-page">
    <div class="admin-card inventory-panel">
      <div class="panel-header">
        <div class="panel-actions admin-toolbar">
          <el-input
            v-model="keyword"
            clearable
            placeholder="Tìm theo tên sản phẩm / SKU"
            class="search-input"
          >
            <i slot="prefix" class="el-input__icon el-icon-search" />
          </el-input>
          <div class="action-buttons">
            <el-button class="admin-ghost-btn" :loading="loading" @click="refreshAll">Làm mới</el-button>
          </div>
        </div>
      </div>

      <div class="table-wrap">
        <el-table
          :data="pagedLowStockItems"
          border
          stripe
          :size="elementSize"
          class="inventory-table admin-table"
          table-layout="fixed"
          empty-text="Không có biến thể nào dưới ngưỡng tồn kho."
        >
          <el-table-column prop="productName" label="Sản phẩm" min-width="220" />
          <el-table-column prop="sku" label="SKU" min-width="170" />
          <el-table-column prop="stock" label="Tồn kho" width="100" />
          <el-table-column
            prop="threshold"
            label="Ngưỡng cảnh báo"
            width="160"
            label-class-name="inventory-nowrap-header"
          />
          <el-table-column label="Lịch sử" width="120" align="center">
            <template slot-scope="{ row }">
              <el-button :size="elementSize" class="admin-ghost-btn" @click="loadInventoryLogs(row.variantId)">Xem log</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <pagination
        class="admin-pagination"
        :total="filteredLowStockItems.length"
        :page.sync="alertPage"
        :limit.sync="alertPageSize"
        @pagination="handleAlertPagination"
      />

      <section class="inventory-log-box">
        <h4 class="log-title">Lịch sử nhập/xuất</h4>
        <el-table
          :data="pagedInventoryLogs"
          border
          stripe
          :size="elementSize"
          class="inventory-table admin-table"
          table-layout="fixed"
          empty-text="Chưa có log"
        >
          <el-table-column prop="createdAt" label="Thời gian" min-width="165">
            <template slot-scope="{ row }">{{ formatDate(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column prop="sku" label="SKU" min-width="140" />
          <el-table-column prop="type" label="Loại" min-width="100" />
          <el-table-column prop="quantity" label="SL" min-width="80" />
          <el-table-column label="Tồn kho" min-width="120">
            <template slot-scope="{ row }">{{ row.beforeStock }} → {{ row.afterStock }}</template>
          </el-table-column>
          <el-table-column prop="note" label="Ghi chú" min-width="220" show-overflow-tooltip />
        </el-table>
        <pagination
          class="admin-pagination"
          :total="inventoryLogs.length"
          :page.sync="logPage"
          :limit.sync="logPageSize"
          @pagination="handleLogPagination"
        />
      </section>
    </div>
  </section>
</template>

<script>
import { fetchInventoryAlerts, fetchInventoryLogs } from '@/api/product'
import Pagination from '@/components/Pagination'

export default {
  name: 'InventoryManagementPage',
  components: { Pagination },
  data() {
    return {
      loading: false,
      keyword: '',
      lowStockItems: [],
      inventoryLogs: [],
      selectedVariantId: null,
      alertPage: 1,
      alertPageSize: 20,
      logPage: 1,
      logPageSize: 20
    }
  },
  computed: {
    elementSize() {
      const size = this.$store.getters.size
      return size === 'default' ? undefined : size
    },
    filteredLowStockItems() {
      const q = String(this.keyword || '').trim().toLowerCase()
      if (!q) return this.lowStockItems
      return this.lowStockItems.filter((item) => {
        const productName = String(item.productName || '').toLowerCase()
        const sku = String(item.sku || '').toLowerCase()
        return productName.includes(q) || sku.includes(q)
      })
    },
    pagedLowStockItems() {
      const start = (this.alertPage - 1) * this.alertPageSize
      return this.filteredLowStockItems.slice(start, start + this.alertPageSize)
    },
    pagedInventoryLogs() {
      const start = (this.logPage - 1) * this.logPageSize
      return this.inventoryLogs.slice(start, start + this.logPageSize)
    }
  },
  watch: {
    keyword() {
      this.alertPage = 1
    }
  },
  created() {
    this.loadInventoryAlerts()
    this.loadInventoryLogs(null)
  },
  methods: {
    refreshAll() {
      this.loadInventoryAlerts()
      this.loadInventoryLogs(this.selectedVariantId)
    },
    handleAlertPagination({ page, limit }) {
      this.alertPage = page
      this.alertPageSize = limit
    },
    handleLogPagination({ page, limit }) {
      this.logPage = page
      this.logPageSize = limit
    },
    async loadInventoryAlerts() {
      this.loading = true
      try {
        const data = await fetchInventoryAlerts(5)
        this.lowStockItems = Array.isArray(data) ? data : []
        if (this.alertPage > 1) {
          const maxPage = Math.max(1, Math.ceil(this.filteredLowStockItems.length / this.alertPageSize))
          if (this.alertPage > maxPage) this.alertPage = maxPage
        }
      } catch (error) {
        this.$message.error((error && error.response && error.response.data && error.response.data.message) || 'Không tải được cảnh báo tồn kho')
      } finally {
        this.loading = false
      }
    },
    async loadInventoryLogs(variantId) {
      this.selectedVariantId = variantId || null
      try {
        const data = await fetchInventoryLogs(variantId)
        this.inventoryLogs = Array.isArray(data) ? data : []
        this.logPage = 1
      } catch (error) {
        this.$message.error((error && error.response && error.response.data && error.response.data.message) || 'Không tải được lịch sử tồn kho')
      }
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

<style scoped>
.inventory-admin-page {
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

::v-deep .inventory-nowrap-header .cell {
  white-space: nowrap;
}

.inventory-log-box {
  margin-top: 14px;
  padding-top: 10px;
  border-top: 1px solid #e5e7eb;
}

.log-title {
  margin: 0 0 8px;
  font-size: 14px;
  font-weight: 600;
}
</style>

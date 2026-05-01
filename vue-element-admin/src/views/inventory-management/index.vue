<template>
  <section v-loading="loading" class="admin-page">
    <div class="admin-card inventory-panel">
      <div class="panel-header">
        <div class="panel-actions admin-toolbar">
          <div class="action-buttons">
            <el-button class="admin-ghost-btn" :loading="loading" @click="loadInventoryAlerts">Làm mới</el-button>
          </div>
        </div>
      </div>

      <div class="table-wrap">
        <el-table
          :data="lowStockItems"
          border
          stripe
          :size="elementSize"
          class="inventory-table admin-table"
          table-layout="fixed"
          empty-text="Không có biến thể nào dưới ngưỡng tồn kho."
        >
          <el-table-column prop="variantId" label="ID biến thể" width="110" />
          <el-table-column prop="productId" label="ID sản phẩm" width="110" />
          <el-table-column prop="productName" label="Sản phẩm" min-width="220" />
          <el-table-column prop="sku" label="SKU" min-width="170" />
          <el-table-column prop="stock" label="Tồn kho" width="100" />
          <el-table-column label="Lịch sử" width="120" align="center">
            <template slot-scope="{ row }">
              <el-button :size="elementSize" @click="loadInventoryLogs(row.variantId)">Xem log</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <section class="inventory-log-box">
        <h4>Lịch sử nhập/xuất</h4>
        <el-table
          :data="inventoryLogs"
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
      </section>
    </div>
  </section>
</template>

<script>
import { fetchInventoryAlerts, fetchInventoryLogs } from '@/api/product'

export default {
  name: 'InventoryManagementPage',
  data() {
    return {
      loading: false,
      lowStockItems: [],
      inventoryLogs: []
    }
  },
  computed: {
    elementSize() {
      const size = this.$store.getters.size
      return size === 'default' ? undefined : size
    }
  },
  created() {
    this.loadInventoryAlerts()
    this.loadInventoryLogs(null)
  },
  methods: {
    async loadInventoryAlerts() {
      this.loading = true
      try {
        const data = await fetchInventoryAlerts(5)
        this.lowStockItems = Array.isArray(data) ? data : []
      } catch (error) {
        this.$message.error((error && error.response && error.response.data && error.response.data.message) || 'Không tải được cảnh báo tồn kho')
      } finally {
        this.loading = false
      }
    },
    async loadInventoryLogs(variantId) {
      try {
        const data = await fetchInventoryLogs(variantId)
        this.inventoryLogs = Array.isArray(data) ? data : []
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
.inventory-log-box {
  margin-top: 14px;
}
</style>

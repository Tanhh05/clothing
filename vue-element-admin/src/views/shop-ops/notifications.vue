<template>
  <section v-loading="loading" class="admin-page">
    <div class="admin-card inventory-panel">
      <div class="admin-toolbar">
        <h3 class="title">Thông báo admin</h3>
        <el-button class="admin-ghost-btn" @click="loadData">Làm mới</el-button>
      </div>
      <el-table :data="rows" border stripe class="admin-table" empty-text="Không có thông báo">
        <el-table-column label="ID" width="90" prop="id" />
        <el-table-column label="Tiêu đề" min-width="220" prop="title" />
        <el-table-column label="Nội dung" min-width="260" prop="content" show-overflow-tooltip />
        <el-table-column label="Ngày tạo" width="180">
          <template slot-scope="{ row }">{{ formatDate(row.createdAt) }}</template>
        </el-table-column>
      </el-table>
    </div>
  </section>
</template>

<script>
import { fetchAdminNotifications } from '@/api/shop-operations'

export default {
  name: 'ShopOpNotificationsPage',
  data() {
    return { loading: false, rows: [] }
  },
  created() {
    this.loadData()
  },
  methods: {
    async loadData() {
      this.loading = true
      try {
        const data = await fetchAdminNotifications()
        this.rows = Array.isArray(data) ? data : []
      } catch (e) {
        this.$message.error('Không tải được thông báo admin')
      } finally {
        this.loading = false
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


<template>
  <section v-loading="loading" class="admin-page">
    <div class="admin-card inventory-panel">
      <div class="admin-toolbar">
        <h3 class="title">Nhật ký hệ thống</h3>
        <el-button class="admin-ghost-btn" @click="loadData">Làm mới</el-button>
      </div>
      <el-table :data="rows" border stripe class="admin-table" empty-text="Không có nhật ký hệ thống">
        <el-table-column label="ID" width="90" prop="id" />
        <el-table-column label="Người dùng" width="150" prop="username" />
        <el-table-column label="Hành động" min-width="200" prop="action" />
        <el-table-column label="Mô tả" min-width="260" prop="details" show-overflow-tooltip />
        <el-table-column label="Thời gian" width="180">
          <template slot-scope="{ row }">{{ formatDate(row.createdAt) }}</template>
        </el-table-column>
      </el-table>
    </div>
  </section>
</template>

<script>
import { fetchAdminAuditLogs } from '@/api/shop-operations'

export default {
  name: 'ShopOpAuditLogsPage',
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
        const data = await fetchAdminAuditLogs()
        this.rows = Array.isArray(data) ? data : []
      } catch (e) {
        this.$message.error('Không tải được nhật ký hệ thống')
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

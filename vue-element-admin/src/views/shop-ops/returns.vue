<template>
  <section v-loading="loading" class="admin-page">
    <div class="admin-card inventory-panel">
      <div class="admin-toolbar">
        <h3 class="title">Đổi trả</h3>
        <el-button class="admin-ghost-btn" @click="loadData">Làm mới</el-button>
      </div>
      <el-table :data="rows" border stripe class="admin-table" empty-text="Không có yêu cầu đổi trả">
        <el-table-column label="ID" width="90" prop="id" />
        <el-table-column label="Đơn hàng" width="120" prop="orderId" />
        <el-table-column label="Khách hàng" min-width="180" prop="customer" />
        <el-table-column label="Lý do" min-width="220" prop="reason" show-overflow-tooltip />
        <el-table-column label="Trạng thái" width="140" prop="status" />
        <el-table-column label="Ngày tạo" width="180">
          <template slot-scope="{ row }">{{ formatDate(row.requestedAt) }}</template>
        </el-table-column>
        <el-table-column label="Thao tác" width="190" align="center">
          <template slot-scope="{ row }">
            <el-button size="mini" type="primary" plain @click="openStatusDialog(row)">Cập nhật trạng thái</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog :visible.sync="dialogVisible" title="Cập nhật trạng thái đổi trả" width="520px">
      <el-form label-position="top">
        <el-form-item label="Trạng thái mới">
          <el-select v-model="statusForm.status" style="width: 100%">
            <el-option v-for="s in statusOptions" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="Ghi chú">
          <el-input v-model="statusForm.note" type="textarea" :rows="3" maxlength="1000" show-word-limit />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="dialogVisible = false">Hủy</el-button>
        <el-button type="primary" :loading="saving" @click="submitStatus">Lưu</el-button>
      </div>
    </el-dialog>
  </section>
</template>

<script>
import { fetchAdminReturns, updateAdminReturnStatus } from '@/api/shop-operations'

export default {
  name: 'ShopOpReturnsPage',
  data() {
    return {
      loading: false,
      saving: false,
      rows: [],
      dialogVisible: false,
      selectedId: null,
      statusForm: { status: 'APPROVED', note: '' },
      statusOptions: ['PENDING', 'APPROVED', 'REJECTED', 'COMPLETED']
    }
  },
  created() {
    this.loadData()
  },
  methods: {
    async loadData() {
      this.loading = true
      try {
        const data = await fetchAdminReturns()
        this.rows = Array.isArray(data) ? data : []
      } catch (e) {
        this.$message.error('Không tải được dữ liệu đổi trả')
      } finally {
        this.loading = false
      }
    },
    openStatusDialog(row) {
      this.selectedId = row.id
      this.statusForm = { status: row.status || 'APPROVED', note: row.resolutionNote || '' }
      this.dialogVisible = true
    },
    async submitStatus() {
      if (!this.selectedId) return
      this.saving = true
      try {
        await updateAdminReturnStatus(this.selectedId, {
          status: String(this.statusForm.status || '').trim(),
          note: String(this.statusForm.note || '').trim() || null
        })
        this.$message.success('Đã cập nhật trạng thái đổi trả')
        this.dialogVisible = false
        await this.loadData()
      } catch (e) {
        this.$message.error('Không cập nhật được trạng thái đổi trả')
      } finally {
        this.saving = false
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

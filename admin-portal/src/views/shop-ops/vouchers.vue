<template>
  <section v-loading="loading" class="shopop-page admin-page-shell admin-page">
    <div class="admin-card inventory-panel">
      <div class="panel-header">
        <div class="panel-actions admin-toolbar">
          <h3 class="title">Mã giảm giá</h3>
          <div class="action-buttons">
            <el-button type="primary" class="admin-primary-btn" :size="elementSize" @click="openCreate">+ Thêm voucher</el-button>
            <el-button class="admin-ghost-btn" :size="elementSize" @click="loadData">Làm mới</el-button>
          </div>
        </div>
      </div>
      <div class="table-wrap">
        <el-table :data="rows" border stripe :size="elementSize" class="inventory-table admin-table" empty-text="Không có voucher" table-layout="fixed">
          <el-table-column label="ID" width="90" prop="id" />
          <el-table-column label="Mã" min-width="160" prop="code" />
          <el-table-column label="Loại" width="140">
            <template slot-scope="{ row }">
              {{ formatDiscountType(row.discountType) }}
            </template>
          </el-table-column>
          <el-table-column label="Giá trị" width="130" prop="discountValue" />
          <el-table-column label="Đơn tối thiểu" width="140" prop="minOrderValue" />
          <el-table-column label="Số lượt" width="100" prop="maxUsage" />
          <el-table-column label="Trạng thái" width="120">
            <template slot-scope="{ row }">
              {{ formatStatus(row.status) }}
            </template>
          </el-table-column>
          <el-table-column label="Thao tác" width="170" align="center">
            <template slot-scope="{ row }">
              <el-button :size="elementSize" plain @click="openEdit(row)">Sửa</el-button>
              <el-button :size="elementSize" type="danger" plain @click="remove(row)">Xóa</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <el-dialog :visible.sync="dialogVisible" :title="editingId ? 'Sửa voucher' : 'Thêm voucher'" width="640px">
      <el-form :model="form" label-position="top">
        <el-row :gutter="10">
          <el-col :span="12"><el-form-item label="Mã"><el-input v-model="form.code" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="Loại giảm"><el-select v-model="form.discountType" style="width:100%"><el-option label="Phần trăm" value="PERCENT" /><el-option label="Cố định" value="FIXED" /></el-select></el-form-item></el-col>
        </el-row>
        <el-row :gutter="10">
          <el-col :span="8"><el-form-item label="Giá trị"><el-input v-model.number="form.discountValue" type="number" min="1" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="Đơn tối thiểu"><el-input v-model.number="form.minOrderValue" type="number" min="0" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="Số lượt"><el-input v-model.number="form.maxUsage" type="number" min="1" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="10">
          <el-col :span="12">
            <el-form-item label="Bắt đầu">
              <el-date-picker v-model="form.startAt" type="datetime" style="width:100%" value-format="yyyy-MM-dd'T'HH:mm:ss" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Kết thúc">
              <el-date-picker v-model="form.endAt" type="datetime" style="width:100%" value-format="yyyy-MM-dd'T'HH:mm:ss" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="Trạng thái">
          <el-select v-model="form.status" style="width:100%">
            <el-option label="Đang hoạt động" value="ACTIVE" />
            <el-option label="Ngưng hoạt động" value="INACTIVE" />
          </el-select>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="dialogVisible = false">Hủy</el-button>
        <el-button type="primary" :loading="saving" @click="submit">{{ editingId ? 'Lưu' : 'Tạo' }}</el-button>
      </div>
    </el-dialog>
  </section>
</template>

<script>
import { createAdminVoucher, deleteAdminVoucher, fetchAdminVouchers, updateAdminVoucher } from '@/api/shop-operations'

export default {
  name: 'ShopOpVouchersPage',
  data() {
    return {
      loading: false,
      saving: false,
      rows: [],
      dialogVisible: false,
      editingId: null,
      form: this.defaultForm()
    }
  },
  created() {
    this.loadData()
  },
  computed: {
    elementSize() {
      const size = this.$store.getters.size
      return size === 'default' ? undefined : size
    }
  },
  methods: {
    formatDiscountType(type) {
      const normalized = String(type || '').toUpperCase()
      if (normalized === 'PERCENT') return 'Phần trăm'
      if (normalized === 'FIXED') return 'Cố định'
      return 'Không có'
    },
    formatStatus(status) {
      const normalized = String(status || '').toUpperCase()
      if (normalized === 'ACTIVE') return 'Đang hoạt động'
      if (normalized === 'INACTIVE') return 'Ngưng hoạt động'
      return 'Không có'
    },
    defaultForm() {
      return {
        code: '',
        discountType: 'PERCENT',
        discountValue: 10,
        minOrderValue: 0,
        maxUsage: 100,
        startAt: null,
        endAt: null,
        status: 'ACTIVE'
      }
    },
    async loadData() {
      this.loading = true
      try {
        const data = await fetchAdminVouchers()
        this.rows = Array.isArray(data) ? data : []
      } catch (e) {
        this.$message.error('Không tải được voucher')
      } finally {
        this.loading = false
      }
    },
    openCreate() {
      this.editingId = null
      this.form = this.defaultForm()
      this.dialogVisible = true
    },
    openEdit(row) {
      this.editingId = row.id
      this.form = {
        code: row.code || '',
        discountType: row.discountType || 'PERCENT',
        discountValue: Number(row.discountValue || 1),
        minOrderValue: Number(row.minOrderValue || 0),
        maxUsage: Number(row.maxUsage || 1),
        startAt: row.startAt || null,
        endAt: row.endAt || null,
        status: row.status || 'ACTIVE'
      }
      this.dialogVisible = true
    },
    async submit() {
      this.saving = true
      try {
        const payload = {
          code: String(this.form.code || '').trim(),
          discountType: this.form.discountType,
          discountValue: Number(this.form.discountValue || 0),
          minOrderValue: Number(this.form.minOrderValue || 0),
          maxUsage: Number(this.form.maxUsage || 0),
          startAt: this.form.startAt || null,
          endAt: this.form.endAt || null,
          status: this.form.status
        }
        if (this.editingId) await updateAdminVoucher(this.editingId, payload)
        else await createAdminVoucher(payload)
        this.$message.success(this.editingId ? 'Đã cập nhật voucher' : 'Đã tạo voucher')
        this.dialogVisible = false
        await this.loadData()
      } catch (e) {
        this.$message.error('Không lưu được voucher')
      } finally {
        this.saving = false
      }
    },
    remove(row) {
      this.$confirm(`Xóa voucher ${row.code}?`, 'Xác nhận', { type: 'warning' }).then(async() => {
        await deleteAdminVoucher(row.id)
        this.$message.success('Đã xóa voucher')
        await this.loadData()
      }).catch(() => {})
    }
  }
}
</script>

<style scoped>
.shopop-page {
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

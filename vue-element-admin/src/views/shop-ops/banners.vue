<template>
  <section v-loading="loading" class="shopop-page admin-page-shell admin-page">
    <div class="admin-card inventory-panel">
      <div class="panel-header">
        <div class="panel-actions admin-toolbar">
          <h3 class="title">Biểu ngữ</h3>
          <div class="action-buttons">
            <el-button type="primary" class="admin-primary-btn" :size="elementSize" @click="openCreate">+ Thêm banner</el-button>
            <el-button class="admin-ghost-btn" :size="elementSize" @click="loadData">Làm mới</el-button>
          </div>
        </div>
      </div>
      <div class="table-wrap">
        <el-table :data="rows" border stripe :size="elementSize" class="inventory-table admin-table" empty-text="Không có banner" table-layout="fixed">
          <el-table-column label="ID" width="90" prop="id" />
          <el-table-column label="Tiêu đề" min-width="180" prop="title" />
          <el-table-column label="Ảnh" min-width="220" prop="imageUrl" show-overflow-tooltip />
          <el-table-column label="Link" min-width="220" prop="linkUrl" show-overflow-tooltip />
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

    <el-dialog :visible.sync="dialogVisible" :title="editingId ? 'Sửa banner' : 'Thêm banner'" width="640px">
      <el-form :model="form" label-position="top">
        <el-form-item label="Tiêu đề"><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="URL ảnh"><el-input v-model="form.imageUrl" /></el-form-item>
        <el-form-item label="Link"><el-input v-model="form.linkUrl" /></el-form-item>
        <el-row :gutter="10">
          <el-col :span="12"><el-form-item label="Bắt đầu"><el-date-picker v-model="form.startAt" type="datetime" style="width:100%" value-format="yyyy-MM-dd'T'HH:mm:ss" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="Kết thúc"><el-date-picker v-model="form.endAt" type="datetime" style="width:100%" value-format="yyyy-MM-dd'T'HH:mm:ss" /></el-form-item></el-col>
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
import { createAdminBanner, deleteAdminBanner, fetchAdminBanners, updateAdminBanner } from '@/api/shop-operations'

export default {
  name: 'ShopOpBannersPage',
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
    formatStatus(status) {
      const normalized = String(status || '').toUpperCase()
      if (normalized === 'ACTIVE') return 'Đang hoạt động'
      if (normalized === 'INACTIVE') return 'Ngưng hoạt động'
      return 'Không có'
    },
    defaultForm() {
      return {
        title: '',
        imageUrl: '',
        linkUrl: '',
        startAt: null,
        endAt: null,
        status: 'ACTIVE'
      }
    },
    async loadData() {
      this.loading = true
      try {
        const data = await fetchAdminBanners()
        this.rows = Array.isArray(data) ? data : []
      } catch (e) {
        this.$message.error('Không tải được banner')
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
        title: row.title || '',
        imageUrl: row.imageUrl || '',
        linkUrl: row.linkUrl || '',
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
          title: String(this.form.title || '').trim() || null,
          imageUrl: String(this.form.imageUrl || '').trim() || null,
          linkUrl: String(this.form.linkUrl || '').trim() || null,
          startAt: this.form.startAt || null,
          endAt: this.form.endAt || null,
          status: this.form.status || 'ACTIVE'
        }
        if (this.editingId) await updateAdminBanner(this.editingId, payload)
        else await createAdminBanner(payload)
        this.$message.success(this.editingId ? 'Đã cập nhật banner' : 'Đã tạo banner')
        this.dialogVisible = false
        await this.loadData()
      } catch (e) {
        this.$message.error('Không lưu được banner')
      } finally {
        this.saving = false
      }
    },
    remove(row) {
      this.$confirm(`Xóa banner #${row.id}?`, 'Xác nhận', { type: 'warning' }).then(async() => {
        await deleteAdminBanner(row.id)
        this.$message.success('Đã xóa banner')
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

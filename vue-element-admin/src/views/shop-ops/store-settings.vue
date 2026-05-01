<template>
  <section v-loading="loading" class="admin-page">
    <div class="admin-card inventory-panel">
      <div class="admin-toolbar">
        <h3 class="title">Cài đặt shop</h3>
        <el-button type="primary" :disabled="!settings" @click="openEdit">Chỉnh sửa</el-button>
        <el-button class="admin-ghost-btn" @click="loadData">Làm mới</el-button>
      </div>

      <el-descriptions v-if="settings" :column="2" border>
        <el-descriptions-item label="Tên cửa hàng">{{ settings.storeName || 'Không có' }}</el-descriptions-item>
        <el-descriptions-item label="SĐT">{{ settings.phone || 'Không có' }}</el-descriptions-item>
        <el-descriptions-item label="Email">{{ settings.email || 'Không có' }}</el-descriptions-item>
        <el-descriptions-item label="Địa chỉ">{{ settings.address || 'Không có' }}</el-descriptions-item>
      </el-descriptions>

      <el-empty v-else description="Chưa có dữ liệu cài đặt" />
    </div>

    <el-dialog :visible.sync="dialogVisible" title="Cập nhật cài đặt shop" width="760px">
      <el-form :model="form" label-position="top">
        <el-row :gutter="10">
          <el-col :span="12"><el-form-item label="Tên cửa hàng"><el-input v-model="form.storeName" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="Đường dây nóng"><el-input v-model="form.hotline" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="10">
          <el-col :span="12"><el-form-item label="Email hỗ trợ"><el-input v-model="form.supportEmail" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="Địa chỉ"><el-input v-model="form.address" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="10">
          <el-col :span="12"><el-form-item label="Phí ship mặc định"><el-input v-model.number="form.defaultShippingFee" type="number" min="0" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="Ngưỡng free ship"><el-input v-model.number="form.freeShippingThreshold" type="number" min="0" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="10">
          <el-col :span="12"><el-form-item label="Bật COD"><el-switch v-model="form.enableCOD" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="Bật Momo"><el-switch v-model="form.enableMomo" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="Chính sách vận chuyển"><el-input v-model="form.shippingPolicy" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="Chính sách đổi trả"><el-input v-model="form.returnPolicy" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="dialogVisible = false">Hủy</el-button>
        <el-button type="primary" :loading="saving" @click="submit">Lưu</el-button>
      </div>
    </el-dialog>
  </section>
</template>

<script>
import { fetchAdminStoreSettings, updateAdminStoreSettings } from '@/api/shop-operations'

export default {
  name: 'ShopOpStoreSettingsPage',
  data() {
    return {
      loading: false,
      saving: false,
      dialogVisible: false,
      settings: null,
      form: this.defaultForm()
    }
  },
  created() {
    this.loadData()
  },
  methods: {
    defaultForm() {
      return {
        storeName: '',
        hotline: '',
        supportEmail: '',
        address: '',
        defaultShippingFee: 0,
        freeShippingThreshold: 0,
        enableCOD: true,
        enableMomo: true,
        shippingPolicy: '',
        returnPolicy: ''
      }
    },
    async loadData() {
      this.loading = true
      try {
        const data = await fetchAdminStoreSettings()
        this.settings = data || null
      } catch (e) {
        this.$message.error('Không tải được cài đặt shop')
      } finally {
        this.loading = false
      }
    },
    openEdit() {
      if (!this.settings) return
      this.form = {
        storeName: this.settings.storeName || '',
        hotline: this.settings.hotline || '',
        supportEmail: this.settings.supportEmail || '',
        address: this.settings.address || '',
        defaultShippingFee: Number(this.settings.defaultShippingFee || 0),
        freeShippingThreshold: Number(this.settings.freeShippingThreshold || 0),
        enableCOD: Boolean(this.settings.enableCOD),
        enableMomo: Boolean(this.settings.enableMomo),
        shippingPolicy: this.settings.shippingPolicy || '',
        returnPolicy: this.settings.returnPolicy || ''
      }
      this.dialogVisible = true
    },
    async submit() {
      this.saving = true
      try {
        const payload = {
          storeName: String(this.form.storeName || '').trim(),
          hotline: String(this.form.hotline || '').trim(),
          supportEmail: String(this.form.supportEmail || '').trim(),
          address: String(this.form.address || '').trim(),
          defaultShippingFee: Number(this.form.defaultShippingFee || 0),
          freeShippingThreshold: Number(this.form.freeShippingThreshold || 0),
          enableCOD: Boolean(this.form.enableCOD),
          enableMomo: Boolean(this.form.enableMomo),
          shippingPolicy: String(this.form.shippingPolicy || '').trim(),
          returnPolicy: String(this.form.returnPolicy || '').trim()
        }
        await updateAdminStoreSettings(payload)
        this.$message.success('Đã cập nhật cài đặt shop')
        this.dialogVisible = false
        await this.loadData()
      } catch (e) {
        this.$message.error('Không cập nhật được cài đặt shop')
      } finally {
        this.saving = false
      }
    }
  }
}
</script>

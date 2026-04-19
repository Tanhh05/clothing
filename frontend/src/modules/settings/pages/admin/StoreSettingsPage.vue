<template>
  <section class="settings-page admin-page-shell">
    <header class="head">
      <div>
        <p class="eyebrow">Admin panel</p>
        <h2>Store Settings</h2>
        <p class="sub-text">Cấu hình thông tin cửa hàng, vận chuyển và thanh toán.</p>
      </div>
      <el-button type="primary" @click="saveSettings">Lưu cấu hình</el-button>
    </header>

    <section class="panel">
      <el-form label-position="top">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="Tên cửa hàng">
              <el-input v-model="settings.storeName" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Hotline">
              <el-input v-model="settings.hotline" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="Email hỗ trợ">
          <el-input v-model="settings.supportEmail" />
        </el-form-item>
        <el-form-item label="Địa chỉ cửa hàng">
          <el-input v-model="settings.address" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
    </section>

    <section class="panel">
      <div class="panel-head"><h3>Vận chuyển & Thanh toán</h3></div>
      <el-row :gutter="12">
        <el-col :span="8">
          <el-form-item label="Phí ship mặc định (VND)">
            <el-input-number v-model="settings.defaultShippingFee" :min="0" style="width: 100%" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="Miễn ship từ (VND)">
            <el-input-number v-model="settings.freeShippingThreshold" :min="0" style="width: 100%" />
          </el-form-item>
        </el-col>
      </el-row>
      <div class="switch-list">
        <div class="switch-item">
          <span>Bật COD</span>
          <el-switch v-model="settings.enableCOD" />
        </div>
        <div class="switch-item">
          <span>Bật MoMo</span>
          <el-switch v-model="settings.enableMomo" />
        </div>
      </div>
    </section>

    <section class="panel">
      <div class="panel-head"><h3>Chính sách</h3></div>
      <div class="policy-grid">
        <el-form-item label="Chính sách giao hàng" class="policy-field">
          <el-input v-model="settings.shippingPolicy" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="Chính sách đổi trả" class="policy-field">
          <el-input v-model="settings.returnPolicy" type="textarea" :rows="3" />
        </el-form-item>
      </div>
    </section>
  </section>
</template>

<script setup>
import { onMounted, reactive } from "vue";
import { ElMessage } from "element-plus";
import { storeSettingsApi } from "@/modules/settings/api/storeSettingsApi";

const settings = reactive({
  storeName: "Clothing Store",
  hotline: "0900 000 000",
  supportEmail: "support@clothing.local",
  address: "TP.HCM, Việt Nam",
  defaultShippingFee: 30000,
  freeShippingThreshold: 500000,
  enableCOD: true,
  enableMomo: true,
  shippingPolicy: "Giao hàng toàn quốc trong 2-5 ngày làm việc.",
  returnPolicy: "Đổi trả trong 7 ngày với sản phẩm còn nguyên tem."
});

const fetchSettings = async () => {
  try {
    const { data } = await storeSettingsApi.getSettings();
    Object.assign(settings, data || {});
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || "Không tải được cấu hình cửa hàng");
  }
};

const saveSettings = async () => {
  try {
    await storeSettingsApi.updateSettings(settings);
    ElMessage.success("Đã lưu cấu hình cửa hàng");
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || "Lưu cấu hình thất bại");
  }
};

onMounted(fetchSettings);
</script>

<style scoped lang="scss">
.settings-page { display: flex; flex-direction: column; gap: 16px; }
.head { padding: 16px; border: 1px solid #dce1e7; background: #fbfbfc; display: flex; justify-content: space-between; align-items: center; gap: 12px; }
.head h2 { margin: 0; font-size: 24px; font-weight: 800; text-transform: uppercase; }
.eyebrow { margin: 0 0 6px; text-transform: uppercase; letter-spacing: 1px; font-size: 11px; color: #6b7280; }
.sub-text { margin: 6px 0 0; color: #6b7280; font-size: 13px; }
.panel { border: 1px solid #dce1e7; background: #fff; padding: 14px; }
.panel-head h3 { margin: 0 0 10px; font-size: 15px; font-weight: 800; }
.switch-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin-top: 6px;
}

.switch-item {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 10px 12px;
  min-height: 52px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.policy-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.policy-field {
  margin-bottom: 0;
}

.policy-field :deep(.el-textarea__inner) {
  min-height: 108px !important;
}

@media (max-width: 760px) {
  .head { flex-direction: column; align-items: flex-start; }
  .switch-list { grid-template-columns: 1fr; }
  .policy-grid { grid-template-columns: 1fr; }
}
</style>

<template>
  <section class="voucher-page">
    <header class="head">
      <div>
        <p class="eyebrow">Admin panel</p>
        <h2>Voucher Management</h2>
        <p class="sub-text">Tạo mã giảm giá, hạn dùng và điều kiện áp dụng.</p>
      </div>
      <div class="head-actions">
        <el-button @click="resetForm">Làm mới form</el-button>
        <el-button type="primary" @click="openCreate">+ Tạo voucher</el-button>
      </div>
    </header>

    <section class="panel">
      <el-table :data="vouchers" border stripe empty-text="Chưa có voucher">
        <el-table-column prop="code" label="Mã" width="140" />
        <el-table-column prop="discountType" label="Loại" width="110" />
        <el-table-column prop="discountValue" label="Giá trị" width="120" />
        <el-table-column prop="minOrderValue" label="Đơn tối thiểu" width="140">
          <template #default="{ row }">{{ formatCurrency(row.minOrderValue) }}</template>
        </el-table-column>
        <el-table-column prop="maxUsage" label="Lượt dùng" width="100" />
        <el-table-column prop="startAt" label="Bắt đầu" width="170">
          <template #default="{ row }">{{ formatDate(row.startAt) }}</template>
        </el-table-column>
        <el-table-column prop="endAt" label="Kết thúc" width="170">
          <template #default="{ row }">{{ formatDate(row.endAt) }}</template>
        </el-table-column>
        <el-table-column prop="status" label="Trạng thái" width="110">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Thao tác" width="180">
          <template #default="{ row }">
            <el-button size="small" @click="openEdit(row)">Sửa</el-button>
            <el-button size="small" type="danger" plain @click="removeVoucher(row.id)">Xóa</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-drawer v-model="drawerVisible" :title="editingId ? 'Sửa voucher' : 'Tạo voucher'" direction="rtl" size="34%">
      <el-form label-position="top">
        <el-form-item label="Mã voucher">
          <el-input v-model="form.code" placeholder="VD: SUMMER20" />
        </el-form-item>
        <el-row :gutter="10">
          <el-col :span="12">
            <el-form-item label="Loại giảm">
              <el-select v-model="form.discountType" style="width: 100%">
                <el-option label="PERCENT" value="PERCENT" />
                <el-option label="AMOUNT" value="AMOUNT" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Giá trị giảm">
              <el-input-number v-model="form.discountValue" :min="1" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="10">
          <el-col :span="12">
            <el-form-item label="Đơn tối thiểu">
              <el-input-number v-model="form.minOrderValue" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Tổng lượt dùng">
              <el-input-number v-model="form.maxUsage" :min="1" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="10">
          <el-col :span="12">
            <el-form-item label="Ngày bắt đầu">
              <el-date-picker v-model="form.startAt" type="datetime" style="width: 100%" format="DD/MM/YYYY HH:mm" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Ngày kết thúc">
              <el-date-picker v-model="form.endAt" type="datetime" style="width: 100%" format="DD/MM/YYYY HH:mm" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="Trạng thái">
          <el-select v-model="form.status" style="width: 100%">
            <el-option label="ACTIVE" value="ACTIVE" />
            <el-option label="INACTIVE" value="INACTIVE" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="drawerVisible = false">Hủy</el-button>
        <el-button type="primary" @click="saveVoucher">Lưu</el-button>
      </template>
    </el-drawer>
  </section>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { ElMessage } from "element-plus";
import { voucherApi } from "@/modules/voucher/api/voucherApi";

const drawerVisible = ref(false);
const editingId = ref(null);
const vouchers = ref([]);
const form = ref(defaultForm());

function defaultForm() {
  return {
    code: "",
    discountType: "PERCENT",
    discountValue: 10,
    minOrderValue: 0,
    maxUsage: 100,
    startAt: new Date(),
    endAt: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000),
    status: "ACTIVE"
  };
}

const formatDate = (value) => (value ? new Date(value).toLocaleString("vi-VN") : "N/A");
const formatCurrency = (value) => new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(Number(value) || 0);
const toLocalDateTime = (value) => {
  if (!value) return null;
  const date = new Date(value);
  const yyyy = date.getFullYear();
  const mm = String(date.getMonth() + 1).padStart(2, "0");
  const dd = String(date.getDate()).padStart(2, "0");
  const hh = String(date.getHours()).padStart(2, "0");
  const mi = String(date.getMinutes()).padStart(2, "0");
  const ss = String(date.getSeconds()).padStart(2, "0");
  return `${yyyy}-${mm}-${dd}T${hh}:${mi}:${ss}`;
};

const fetchVouchers = async () => {
  try {
    const { data } = await voucherApi.getAdminVouchers();
    vouchers.value = Array.isArray(data) ? data : [];
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || "Không tải được voucher");
  }
};

const openCreate = () => {
  editingId.value = null;
  form.value = defaultForm();
  drawerVisible.value = true;
};

const openEdit = (row) => {
  editingId.value = row.id;
  form.value = {
    ...row,
    startAt: row.startAt ? new Date(row.startAt) : null,
    endAt: row.endAt ? new Date(row.endAt) : null
  };
  drawerVisible.value = true;
};

const resetForm = () => {
  form.value = defaultForm();
};

const saveVoucher = async () => {
  const code = String(form.value.code || "").trim().toUpperCase();
  if (!code) {
    ElMessage.warning("Vui lòng nhập mã voucher");
    return;
  }
  const payload = {
    ...form.value,
    code,
    startAt: toLocalDateTime(form.value.startAt),
    endAt: toLocalDateTime(form.value.endAt)
  };
  try {
    if (editingId.value) {
      await voucherApi.updateVoucher(editingId.value, payload);
    } else {
      await voucherApi.createVoucher(payload);
    }
    await fetchVouchers();
    drawerVisible.value = false;
    ElMessage.success("Đã lưu voucher");
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || "Lưu voucher thất bại");
  }
};

const removeVoucher = async (id) => {
  try {
    await voucherApi.deleteVoucher(id);
    vouchers.value = vouchers.value.filter((item) => item.id !== id);
    ElMessage.success("Đã xóa voucher");
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || "Xóa voucher thất bại");
  }
};

onMounted(fetchVouchers);
</script>

<style scoped lang="scss">
.voucher-page { display: flex; flex-direction: column; gap: 16px; }
.head { padding: 16px; border: 1px solid #dce1e7; background: #fbfbfc; display: flex; justify-content: space-between; align-items: center; gap: 12px; }
.head h2 { margin: 0; font-size: 24px; font-weight: 800; text-transform: uppercase; }
.eyebrow { margin: 0 0 6px; text-transform: uppercase; letter-spacing: 1px; font-size: 11px; color: #6b7280; }
.sub-text { margin: 6px 0 0; color: #6b7280; font-size: 13px; }
.head-actions { display: flex; gap: 8px; }
.panel { border: 1px solid #dce1e7; background: #fff; padding: 14px; }
@media (max-width: 760px) { .head { flex-direction: column; align-items: flex-start; } .head-actions { width: 100%; display: grid; grid-template-columns: 1fr 1fr; } }
</style>

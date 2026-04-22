<template>
  <section class="voucher-page admin-page-shell">
    <section class="panel">
      <div class="panel-head">
        <el-button @click="resetForm">Làm mới</el-button>
        <el-button type="primary" @click="openCreate">+ Tạo voucher</el-button>
      </div>
      <BaseTable :data="vouchers" border stripe size="small" empty-text="Chưa có voucher" table-layout="fixed">
        <el-table-column label="Voucher" min-width="220">
          <template #default="{ row }">
            <div class="voucher-cell">
              <strong>{{ row.code }}</strong>
              <el-tag size="small" :type="row.status === 'ACTIVE' ? 'success' : 'info'">
                {{ row.status }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="Điều kiện áp dụng" min-width="320">
          <template #default="{ row }">
            <div class="condition-cell">
              <span>{{ formatDiscount(row.discountType, row.discountValue) }}</span>
              <span>Đơn tối thiểu: {{ formatCurrency(row.minOrderValue) }}</span>
              <span>Tổng lượt dùng: {{ row.maxUsage || 0 }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="Thời gian" min-width="250">
          <template #default="{ row }">
            <div class="time-cell">
              <span><strong>Bắt đầu:</strong> {{ formatDate(row.startAt) }}</span>
              <span><strong>Kết thúc:</strong> {{ formatDate(row.endAt) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="Thao tác" width="170">
          <template #default="{ row }">
            <el-button size="small" @click="openEdit(row)">Sửa</el-button>
            <el-button size="small" type="danger" plain @click="removeVoucher(row.id)">Xóa</el-button>
          </template>
        </el-table-column>
      </BaseTable>
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
import { ElMessage } from "@/utils/dialogMessage";
import { voucherApi } from "@/modules/voucher/api/voucherApi";
import { useConfirmDialog } from "@/composables/useConfirmDialog";

const drawerVisible = ref(false);
const editingId = ref(null);
const vouchers = ref([]);
const form = ref(defaultForm());
const { confirm } = useConfirmDialog();

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
const formatDiscount = (type, value) => {
  const normalized = String(type || "").toUpperCase();
  if (normalized === "PERCENT" || normalized === "PERCENTAGE") {
    return `Giảm ${Number(value) || 0}%`;
  }
  return `Giảm ${formatCurrency(value)}`;
};
const normalizeDiscountType = (value) => {
  const normalized = String(value || "").toUpperCase();
  if (normalized === "PERCENTAGE") return "PERCENT";
  if (normalized === "FIXED") return "AMOUNT";
  return normalized || "PERCENT";
};
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
    discountType: normalizeDiscountType(row.discountType),
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
    code,
    discountType: normalizeDiscountType(form.value.discountType),
    discountValue: Number(form.value.discountValue || 0),
    minOrderValue: Number(form.value.minOrderValue || 0),
    maxUsage: Number(form.value.maxUsage || 0),
    status: String(form.value.status || "ACTIVE").toUpperCase(),
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
    await confirm({
      title: "Xác nhận",
      message: `Xóa voucher #${id}?`,
      confirmButtonText: "Xóa",
      cancelButtonText: "Hủy",
      onConfirm: async () => {
        await voucherApi.deleteVoucher(id);
        vouchers.value = vouchers.value.filter((item) => item.id !== id);
        ElMessage.success("Đã xóa voucher");
      }
    });
  } catch (error) {
    if (error.message !== "cancel") {
      ElMessage.error(error?.response?.data?.message || "Xóa voucher thất bại");
    }
  }
};

onMounted(fetchVouchers);
</script>

<style scoped lang="scss">
.voucher-page { display: flex; flex-direction: column; gap: 10px; }
.panel { border: 1px solid #dce1e7; background: #fff; padding: 10px; }
.panel-head { margin-bottom: 10px; display: flex; gap: 8px; flex-wrap: wrap; }
.voucher-cell { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.voucher-cell strong { font-size: 13px; color: #0f172a; word-break: break-all; }
.condition-cell { display: grid; gap: 3px; }
.condition-cell span { font-size: 12px; color: #334155; }
.time-cell { display: grid; gap: 3px; }
.time-cell span { font-size: 12px; color: #334155; }
@media (max-width: 760px) { .panel-head { width: 100%; display: grid; grid-template-columns: 1fr 1fr; } }
</style>

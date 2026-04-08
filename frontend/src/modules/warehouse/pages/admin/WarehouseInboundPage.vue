<template>
  <section class="warehouse-page">
    <header class="head">
      <div>
        <p class="eyebrow">Admin panel</p>
        <h2>Warehouse Inbound</h2>
        <p class="sub-text">Tạo phiếu nhập kho, theo dõi chi phí nhập và số lượng.</p>
      </div>
      <el-button type="primary" @click="openCreate">+ Tạo phiếu nhập</el-button>
    </header>

    <section class="panel">
      <el-table :data="receipts" border stripe empty-text="Chưa có phiếu nhập">
        <el-table-column prop="code" label="Mã phiếu" width="140" />
        <el-table-column prop="supplier" label="Nhà cung cấp" min-width="180" />
        <el-table-column prop="createdAt" label="Ngày nhập" width="170">
          <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column prop="itemCount" label="Số dòng" width="95" />
        <el-table-column label="Tổng chi phí" width="150">
          <template #default="{ row }">{{ formatCurrency(row.totalCost) }}</template>
        </el-table-column>
      </el-table>
    </section>

    <el-drawer v-model="drawerVisible" title="Tạo phiếu nhập kho" direction="rtl" size="42%">
      <el-form label-position="top">
        <el-form-item label="Mã phiếu">
          <el-input v-model="form.code" placeholder="VD: NK-2026-001" />
        </el-form-item>
        <el-form-item label="Nhà cung cấp">
          <el-input v-model="form.supplier" placeholder="Tên nhà cung cấp" />
        </el-form-item>
        <el-form-item label="Ngày nhập">
          <el-date-picker v-model="form.createdAt" type="datetime" style="width: 100%" />
        </el-form-item>

        <div class="item-head">
          <h4>Danh sách nhập</h4>
          <el-button size="small" @click="addLine">+ Dòng</el-button>
        </div>
        <article v-for="(line, index) in form.items" :key="index" class="line-item">
          <el-row :gutter="10">
            <el-col :span="10"><el-input v-model="line.sku" placeholder="SKU" /></el-col>
            <el-col :span="6"><el-input-number v-model="line.quantity" :min="1" style="width: 100%" /></el-col>
            <el-col :span="6"><el-input-number v-model="line.cost" :min="0" style="width: 100%" /></el-col>
            <el-col :span="2"><el-button text type="danger" @click="removeLine(index)">X</el-button></el-col>
          </el-row>
        </article>
      </el-form>
      <template #footer>
        <el-button @click="drawerVisible = false">Hủy</el-button>
        <el-button type="primary" @click="saveReceipt">Lưu phiếu</el-button>
      </template>
    </el-drawer>
  </section>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { ElMessage } from "element-plus";
import { warehouseApi } from "@/modules/warehouse/api/warehouseApi";

const receipts = ref([]);
const drawerVisible = ref(false);
const form = ref(defaultForm());

function defaultForm() {
  return {
    code: "",
    supplier: "",
    createdAt: new Date(),
    items: [{ sku: "", quantity: 1, cost: 0 }]
  };
}

const formatCurrency = (value) => new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(Number(value) || 0);
const formatDate = (value) => (value ? new Date(value).toLocaleString("vi-VN") : "N/A");

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

const fetchReceipts = async () => {
  try {
    const { data } = await warehouseApi.getInboundReceipts();
    receipts.value = Array.isArray(data) ? data : [];
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || "Không tải được phiếu nhập");
  }
};

const openCreate = () => {
  form.value = defaultForm();
  drawerVisible.value = true;
};

const addLine = () => form.value.items.push({ sku: "", quantity: 1, cost: 0 });
const removeLine = (index) => form.value.items.splice(index, 1);

const saveReceipt = async () => {
  if (!form.value.code || !form.value.supplier || !form.value.items.length) {
    ElMessage.warning("Vui lòng nhập đủ thông tin phiếu nhập");
    return;
  }
  const payload = {
    code: form.value.code.trim(),
    supplier: form.value.supplier.trim(),
    createdAt: toLocalDateTime(form.value.createdAt),
    items: form.value.items
      .filter((line) => line.sku?.trim())
      .map((line) => ({
        sku: line.sku.trim(),
        quantity: Number(line.quantity) || 0,
        cost: Number(line.cost) || 0
      }))
  };
  if (!payload.items.length) {
    ElMessage.warning("Vui lòng nhập ít nhất 1 SKU hợp lệ");
    return;
  }
  try {
    await warehouseApi.createInboundReceipt(payload);
    await fetchReceipts();
    drawerVisible.value = false;
    ElMessage.success("Đã tạo phiếu nhập kho");
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || "Tạo phiếu nhập thất bại");
  }
};

onMounted(fetchReceipts);
</script>

<style scoped lang="scss">
.warehouse-page { display: flex; flex-direction: column; gap: 16px; }
.head { padding: 16px; border: 1px solid #dce1e7; background: #fbfbfc; display: flex; justify-content: space-between; align-items: center; gap: 12px; }
.head h2 { margin: 0; font-size: 24px; font-weight: 800; text-transform: uppercase; }
.eyebrow { margin: 0 0 6px; text-transform: uppercase; letter-spacing: 1px; font-size: 11px; color: #6b7280; }
.sub-text { margin: 6px 0 0; color: #6b7280; font-size: 13px; }
.panel { border: 1px solid #dce1e7; background: #fff; padding: 14px; }
.item-head { margin: 12px 0 8px; display: flex; justify-content: space-between; align-items: center; }
.item-head h4 { margin: 0; font-size: 13px; text-transform: uppercase; }
.line-item { border: 1px solid #e5e7eb; padding: 10px; margin-bottom: 8px; border-radius: 8px; }
</style>

<template>
  <section class="returns-page">
    <header class="head">
      <div>
        <p class="eyebrow">Admin panel</p>
        <h2>Return & Refund</h2>
        <p class="sub-text">Duyệt yêu cầu đổi trả, theo dõi hoàn tiền.</p>
      </div>
      <el-select v-model="statusFilter" clearable placeholder="Lọc trạng thái" style="width: 180px">
        <el-option label="REQUESTED" value="REQUESTED" />
        <el-option label="APPROVED" value="APPROVED" />
        <el-option label="REJECTED" value="REJECTED" />
        <el-option label="REFUNDED" value="REFUNDED" />
      </el-select>
    </header>

    <section class="panel">
      <el-table :data="requests" border stripe empty-text="Chưa có yêu cầu">
        <el-table-column prop="orderId" label="Đơn hàng" width="90" />
        <el-table-column prop="customer" label="Khách hàng" min-width="170" />
        <el-table-column prop="reason" label="Lý do" min-width="240" show-overflow-tooltip />
        <el-table-column prop="requestedAt" label="Ngày gửi" width="160">
          <template #default="{ row }">{{ formatDate(row.requestedAt) }}</template>
        </el-table-column>
        <el-table-column prop="status" label="Trạng thái" width="120">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Thao tác" width="220">
          <template #default="{ row }">
            <el-button size="small" type="primary" plain :disabled="row.status !== 'REQUESTED'" @click="setStatus(row.id, 'APPROVED')">Duyệt</el-button>
            <el-button size="small" type="danger" plain :disabled="row.status !== 'REQUESTED'" @click="setStatus(row.id, 'REJECTED')">Từ chối</el-button>
            <el-button size="small" type="success" plain :disabled="row.status !== 'APPROVED'" @click="setStatus(row.id, 'REFUNDED')">Hoàn tiền</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </section>
</template>

<script setup>
import { onMounted, ref, watch } from "vue";
import { ElMessage } from "element-plus";
import { returnApi } from "@/modules/returns/api/returnApi";

const requests = ref([]);
const statusFilter = ref("");

const formatDate = (value) => (value ? new Date(value).toLocaleString("vi-VN") : "N/A");

const fetchRequests = async () => {
  try {
    const params = statusFilter.value ? { status: statusFilter.value } : undefined;
    const { data } = await returnApi.getAdminReturns(params);
    requests.value = Array.isArray(data) ? data : [];
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || "Không tải được yêu cầu đổi trả");
  }
};

const statusType = (status) => {
  if (status === "REQUESTED") return "warning";
  if (status === "APPROVED") return "primary";
  if (status === "REFUNDED") return "success";
  return "danger";
};

const setStatus = async (id, nextStatus) => {
  try {
    await returnApi.updateReturnStatus(id, nextStatus);
    await fetchRequests();
    ElMessage.success(`Đã cập nhật ${nextStatus}`);
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || "Cập nhật trạng thái thất bại");
  }
};

watch(statusFilter, fetchRequests);
onMounted(fetchRequests);
</script>

<style scoped lang="scss">
.returns-page { display: flex; flex-direction: column; gap: 16px; }
.head { padding: 16px; border: 1px solid #dce1e7; background: #fbfbfc; display: flex; justify-content: space-between; align-items: center; gap: 12px; }
.head h2 { margin: 0; font-size: 24px; font-weight: 800; text-transform: uppercase; }
.eyebrow { margin: 0 0 6px; text-transform: uppercase; letter-spacing: 1px; font-size: 11px; color: #6b7280; }
.sub-text { margin: 6px 0 0; color: #6b7280; font-size: 13px; }
.panel { border: 1px solid #dce1e7; background: #fff; padding: 14px; }
@media (max-width: 760px) { .head { flex-direction: column; align-items: flex-start; } }
</style>

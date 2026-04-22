<template>
  <section class="returns-admin admin-page-shell">
    <section class="panel" v-loading="loading">
      <div class="panel-head">
        <el-select v-model="statusFilter" clearable placeholder="Trạng thái" class="status-filter">
          <el-option v-for="option in statusOptions" :key="option.value" :label="option.label" :value="option.value" />
        </el-select>
        <el-button :loading="loading" @click="fetchRequests">Làm mới</el-button>
      </div>
      <BaseTable :data="requests" border stripe size="small" empty-text="Chưa có yêu cầu đổi trả" table-layout="fixed">
        <el-table-column label="Yêu cầu" min-width="240">
          <template #default="{ row }">
            <div class="request-cell">
              <strong>#{{ row.id }} / Đơn #{{ row.orderId }}</strong>
              <span>{{ row.customer || "-" }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="Nội dung" min-width="320">
          <template #default="{ row }">
            <div class="content-cell">
              <el-tag size="small" :type="row.returnType === 'REFUND' ? 'danger' : 'warning'">
                {{ row.returnType === "REFUND" ? "Hoàn tiền" : "Đổi hàng" }}
              </el-tag>
              <div class="reason-cell">
                <strong>{{ reasonCodeLabel(row.reasonCode) }}</strong>
                <span>{{ row.reasonDetail || row.reason || "-" }}</span>
              </div>
              <div v-if="Array.isArray(row.items) && row.items.length" class="items-cell">
                <span v-for="item in row.items" :key="item.id">{{ item.sku }} x{{ item.requestedQuantity }}</span>
              </div>
              <span v-else class="muted">Không có sản phẩm đổi trả</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="requestedAt" label="Ngày gửi" width="170">
          <template #default="{ row }">{{ formatDate(row.requestedAt) }}</template>
        </el-table-column>

        <el-table-column prop="status" label="Trạng thái" width="150">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="Hành động" width="170">
          <template #default="{ row }">
            <el-dropdown
              v-if="nextActions(row).length"
              trigger="click"
              @command="(nextStatus) => onUpdateStatus(row, nextStatus)"
            >
              <el-button size="small" type="primary" plain>
                Cập nhật bước
                <el-icon class="el-icon--right"><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item
                    v-for="action in nextActions(row)"
                    :key="action.status"
                    :command="action.status"
                  >
                    {{ action.label }}
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <el-button v-else size="small" disabled>Hoàn tất</el-button>
          </template>
        </el-table-column>
      </BaseTable>
    </section>
  </section>
</template>

<script setup>
import { onMounted, ref, watch } from "vue";
import { ElMessageBox } from "element-plus";
import { ElMessage } from "@/utils/dialogMessage";
import { ArrowDown } from "@element-plus/icons-vue";
import { returnApi } from "@/modules/returns/api/returnApi";

const requests = ref([]);
const statusFilter = ref("");
const loading = ref(false);

const statusOptions = [
  { value: "REQUESTED", label: "Đã gửi yêu cầu" },
  { value: "UNDER_REVIEW", label: "Đang xem xét" },
  { value: "RETURN_APPROVED", label: "Đã duyệt trả hàng" },
  { value: "IN_TRANSIT_BACK", label: "Đang hoàn hàng" },
  { value: "RETURN_RECEIVED", label: "Shop đã nhận hàng trả" },
  { value: "REFUND_PROCESSING", label: "Đang hoàn tiền" },
  { value: "REFUNDED", label: "Đã hoàn tiền" },
  { value: "EXCHANGE_PROCESSING", label: "Đang đổi hàng" },
  { value: "EXCHANGED", label: "Đã đổi hàng" },
  { value: "RETURN_REJECTED", label: "Từ chối yêu cầu" }
];

const reasonCodeLabel = (code) => {
  const map = {
    NOT_AS_DESCRIBED: "Không giống mô tả",
    DEFECTIVE: "Hàng lỗi/hư hỏng",
    WRONG_ITEM: "Giao sai sản phẩm",
    WRONG_SIZE: "Sai kích cỡ",
    CHANGE_MIND: "Không còn nhu cầu",
    MISSING_PARTS: "Thiếu phụ kiện",
    OTHER: "Lý do khác"
  };
  return map[String(code || "").toUpperCase()] || code || "N/A";
};

const statusLabel = (status) => {
  const map = {
    REQUESTED: "Đã gửi yêu cầu",
    UNDER_REVIEW: "Đang xem xét",
    RETURN_APPROVED: "Đã duyệt trả hàng",
    IN_TRANSIT_BACK: "Đang hoàn hàng",
    RETURN_RECEIVED: "Shop đã nhận hàng trả",
    REFUND_PROCESSING: "Đang hoàn tiền",
    REFUNDED: "Đã hoàn tiền",
    EXCHANGE_PROCESSING: "Đang đổi hàng",
    EXCHANGED: "Đã đổi hàng",
    RETURN_REJECTED: "Từ chối yêu cầu"
  };
  return map[String(status || "").toUpperCase()] || status || "N/A";
};

const statusType = (status) => {
  const value = String(status || "").toUpperCase();
  if (["REQUESTED", "UNDER_REVIEW", "IN_TRANSIT_BACK"].includes(value)) return "warning";
  if (["RETURN_APPROVED", "RETURN_RECEIVED", "REFUND_PROCESSING", "EXCHANGE_PROCESSING"].includes(value)) return "primary";
  if (["REFUNDED", "EXCHANGED"].includes(value)) return "success";
  return "danger";
};

const formatDate = (value) => (value ? new Date(value).toLocaleString("vi-VN") : "N/A");

const fetchRequests = async () => {
  loading.value = true;
  try {
    const params = statusFilter.value ? { status: statusFilter.value } : undefined;
    const { data } = await returnApi.getAdminReturns(params);
    requests.value = Array.isArray(data) ? data : [];
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || "Không tải được yêu cầu đổi trả");
  } finally {
    loading.value = false;
  }
};

const nextActions = (row) => {
  const current = String(row?.status || "").toUpperCase();
  const isRefund = String(row?.returnType || "").toUpperCase() === "REFUND";
  if (current === "REQUESTED") {
    return [
      { status: "UNDER_REVIEW", label: "Chuyển sang Đang xem xét" },
      { status: "RETURN_REJECTED", label: "Từ chối yêu cầu" }
    ];
  }
  if (current === "UNDER_REVIEW") {
    return [
      { status: "RETURN_APPROVED", label: "Duyệt yêu cầu" },
      { status: "RETURN_REJECTED", label: "Từ chối yêu cầu" }
    ];
  }
  if (current === "RETURN_APPROVED") {
    return [{ status: "IN_TRANSIT_BACK", label: "Đã tạo hoàn hàng / đang về kho" }];
  }
  if (current === "IN_TRANSIT_BACK") {
    return [{ status: "RETURN_RECEIVED", label: "Xác nhận đã nhận hàng hoàn" }];
  }
  if (current === "RETURN_RECEIVED") {
    return isRefund
      ? [{ status: "REFUND_PROCESSING", label: "Bắt đầu hoàn tiền" }]
      : [{ status: "EXCHANGE_PROCESSING", label: "Bắt đầu đổi hàng" }];
  }
  if (current === "REFUND_PROCESSING") {
    return [{ status: "REFUNDED", label: "Hoàn tiền thành công" }];
  }
  if (current === "EXCHANGE_PROCESSING") {
    return [{ status: "EXCHANGED", label: "Đổi hàng thành công" }];
  }
  return [];
};

const onUpdateStatus = async (row, nextStatus) => {
  if (!nextStatus) return;
  try {
    const { value } = await ElMessageBox.prompt("Ghi chú xử lý (không bắt buộc)", "Cập nhật trạng thái", {
      confirmButtonText: "Xác nhận",
      cancelButtonText: "Bỏ qua",
      inputType: "textarea",
      inputValue: row?.resolutionNote || ""
    });
    await returnApi.updateReturnStatus(row.id, nextStatus, value || "");
    ElMessage.success("Đã cập nhật trạng thái đổi trả");
  } catch (error) {
    if (error === "cancel" || error === "close") return;
    ElMessage.error(error?.response?.data?.message || "Không thể cập nhật trạng thái");
  } finally {
    await fetchRequests();
  }
};

watch(statusFilter, fetchRequests);
onMounted(fetchRequests);
</script>

<style scoped lang="scss">
.returns-admin { display: flex; flex-direction: column; gap: 10px; }
.panel { border: 1px solid #dce1e7; background: #fff; padding: 10px; }
.panel-head { margin-bottom: 10px; display: flex; gap: 8px; flex-wrap: wrap; }
.status-filter { width: 220px; }
.request-cell { display: grid; gap: 2px; }
.request-cell strong { font-size: 12px; color: #0f172a; }
.request-cell span { font-size: 12px; color: #475569; }
.content-cell { display: grid; gap: 6px; }
.reason-cell { display: grid; gap: 2px; }
.reason-cell strong { font-size: 12px; color: #0f172a; }
.reason-cell span { font-size: 12px; color: #475569; }
.items-cell { display: flex; flex-wrap: wrap; gap: 6px; }
.items-cell span { font-size: 12px; color: #0f172a; background: #f1f5f9; padding: 2px 6px; border-radius: 999px; }
.muted { font-size: 12px; color: #64748b; }
@media (max-width: 960px) {
  .status-filter { width: 100%; }
}
</style>

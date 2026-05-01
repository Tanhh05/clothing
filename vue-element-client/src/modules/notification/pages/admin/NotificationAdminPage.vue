<template>
  <section class="notification-page admin-page-shell">
    <header class="head">
      <div>
        <p class="eyebrow">Admin panel</p>
        <h2>Notification Center</h2>
        <p class="sub-text">Gửi thông báo đến khách hàng theo nhóm đối tượng.</p>
      </div>
      <el-button type="primary" @click="sendNotification">Gửi thông báo</el-button>
    </header>

    <section class="panel">
      <el-form label-position="top">
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="Nhóm nhận">
              <el-select v-model="form.audience" style="width: 100%">
                <el-option label="Tất cả khách hàng" value="ALL" />
                <el-option label="Đã mua hàng" value="PURCHASED" />
                <el-option label="Có sản phẩm wishlist" value="WISHLIST" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="Kênh gửi">
              <el-select v-model="form.channel" style="width: 100%">
                <el-option label="In-app" value="IN_APP" />
                <el-option label="Email" value="EMAIL" />
                <el-option label="In-app + Email" value="BOTH" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="Thời điểm gửi">
              <el-select v-model="form.sendMode" style="width: 100%">
                <el-option label="Gửi ngay" value="NOW" />
                <el-option label="Lên lịch" value="SCHEDULED" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="Tiêu đề">
          <el-input v-model="form.title" placeholder="VD: Flash sale cuối tuần" />
        </el-form-item>
        <el-form-item label="Nội dung">
          <el-input v-model="form.content" type="textarea" :rows="4" placeholder="Nhập nội dung thông báo" />
        </el-form-item>
        <el-form-item v-if="form.sendMode === 'SCHEDULED'" label="Thời gian gửi">
          <el-date-picker v-model="form.scheduledAt" type="datetime" style="width: 100%" />
        </el-form-item>
      </el-form>
    </section>

    <section class="panel">
      <div class="panel-head"><h3>Lịch sử gửi</h3></div>
      <BaseTable :data="history" border stripe size="small" table-layout="fixed" empty-text="Chưa có thông báo">
        <el-table-column prop="createdAt" label="Thời gian" width="170">
          <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column prop="title" label="Tiêu đề" min-width="220" />
        <el-table-column prop="audience" label="Nhóm nhận" width="130" />
        <el-table-column prop="channel" label="Kênh" width="120" />
        <el-table-column prop="status" label="Trạng thái" width="120">
          <template #default="{ row }"><el-tag :type="row.status === 'SENT' ? 'success' : 'warning'">{{ row.status }}</el-tag></template>
        </el-table-column>
      </BaseTable>
    </section>
  </section>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { ElMessage } from "@/utils/dialogMessage";
import { notificationAdminApi } from "@/modules/notification/api/notificationAdminApi";

const form = ref({
  audience: "ALL",
  channel: "IN_APP",
  sendMode: "NOW",
  title: "",
  content: "",
  scheduledAt: new Date()
});

const history = ref([]);

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

const fetchHistory = async () => {
  try {
    const { data } = await notificationAdminApi.getHistory();
    history.value = Array.isArray(data) ? data : [];
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || "Không tải được lịch sử thông báo");
  }
};

const sendNotification = async () => {
  if (!form.value.title.trim() || !form.value.content.trim()) {
    ElMessage.warning("Vui lòng nhập tiêu đề và nội dung");
    return;
  }
  try {
    await notificationAdminApi.create({
      audience: form.value.audience,
      channel: form.value.channel,
      sendMode: form.value.sendMode,
      title: form.value.title.trim(),
      content: form.value.content.trim(),
      scheduledAt: form.value.sendMode === "SCHEDULED" ? toLocalDateTime(form.value.scheduledAt) : null
    });
    await fetchHistory();
    ElMessage.success(form.value.sendMode === "NOW" ? "Đã gửi thông báo" : "Đã lên lịch thông báo");
    form.value.title = "";
    form.value.content = "";
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || "Gửi thông báo thất bại");
  }
};

onMounted(fetchHistory);
</script>

<style scoped lang="scss">
.notification-page { display: flex; flex-direction: column; gap: 16px; }
.head { padding: 16px; border: 1px solid #dce1e7; background: #fbfbfc; display: flex; justify-content: space-between; align-items: center; gap: 12px; }
.head h2 { margin: 0; font-size: 24px; font-weight: 800; text-transform: uppercase; }
.eyebrow { margin: 0 0 6px; text-transform: uppercase; letter-spacing: 1px; font-size: 11px; color: #6b7280; }
.sub-text { margin: 6px 0 0; color: #6b7280; font-size: 13px; }
.panel { border: 1px solid #dce1e7; background: #fff; padding: 14px; }
.panel-head h3 { margin: 0 0 10px; font-size: 15px; font-weight: 800; }
@media (max-width: 760px) { .head { flex-direction: column; align-items: flex-start; } }
</style>

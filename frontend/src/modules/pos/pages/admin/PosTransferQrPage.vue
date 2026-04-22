<template>
  <section class="pos-transfer-page" v-loading="loading">
    <el-card shadow="never" class="pos-transfer-card">
      <template #header>
        <div class="pos-transfer-header">
          <span>Thanh toán chuyển khoản POS</span>
          <el-tag type="info">Order #{{ orderId || "-" }}</el-tag>
        </div>
      </template>

      <el-descriptions :column="1" border>
        <el-descriptions-item label="Mã hóa đơn">{{ invoiceCode || "-" }}</el-descriptions-item>
        <el-descriptions-item label="Mã đơn">{{ orderId || "-" }}</el-descriptions-item>
        <el-descriptions-item label="Trạng thái">
          <el-tag :type="paid ? 'success' : 'warning'">
            {{ paid ? "Đã thanh toán" : "Chờ thanh toán" }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>

      <div class="pos-transfer-qr-wrap">
        <el-image v-if="qrImageUrl" :src="qrImageUrl" fit="contain" class="pos-transfer-qr" />
        <el-empty v-else description="Không có dữ liệu QR hợp lệ" />
      </div>

      <el-space wrap>
        <el-button type="primary" :loading="checking" @click="checkPaymentStatus">Kiểm tra thanh toán</el-button>
        <el-button @click="goPos">Về màn POS</el-button>
        <el-button @click="closePage">Đóng trang</el-button>
      </el-space>
    </el-card>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { paymentApi } from "@/modules/order/api/paymentApi";
import { useConfirmDialog } from "@/composables/useConfirmDialog";

const route = useRoute();
const router = useRouter();
const { showAlert } = useConfirmDialog();

const loading = ref(false);
const checking = ref(false);
const paid = ref(false);
let pollTimer = null;

const orderId = computed(() => Number(route.query.orderId || 0));
const paymentUrl = computed(() => String(route.query.paymentUrl || "").trim());
const invoiceCode = computed(() => String(route.query.invoiceCode || "").trim());
const qrImageUrl = computed(() => {
  const url = paymentUrl.value;
  if (!url) return "";
  return `https://api.qrserver.com/v1/create-qr-code/?size=360x360&data=${encodeURIComponent(url)}`;
});

const stopPolling = () => {
  if (pollTimer) {
    clearInterval(pollTimer);
    pollTimer = null;
  }
};

const checkPaymentStatus = async () => {
  if (!orderId.value || paid.value) return;
  checking.value = true;
  try {
    const { data } = await paymentApi.getOrderPaymentStatus(orderId.value);
    if (data?.paid) {
      paid.value = true;
      stopPolling();
      await showAlert(`Đơn #${orderId.value} đã thanh toán thành công`, "Thanh toán thành công");
    }
  } catch (error) {
    console.error(error);
  } finally {
    checking.value = false;
  }
};

const startPolling = () => {
  stopPolling();
  pollTimer = setInterval(() => {
    void checkPaymentStatus();
  }, 4000);
};

const goPos = () => {
  router.push("/admin/pos");
};

const closePage = () => {
  window.close();
};

onMounted(async () => {
  if (!orderId.value || !paymentUrl.value) {
    await showAlert("Thiếu thông tin thanh toán. Vui lòng quay lại màn POS.", "Lỗi dữ liệu");
    return;
  }
  startPolling();
});

onBeforeUnmount(() => {
  stopPolling();
});
</script>

<style scoped>
.pos-transfer-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 16px;
  background: var(--el-bg-color-page);
}

.pos-transfer-card {
  width: min(620px, 100%);
  border-radius: 0;
}

.pos-transfer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.pos-transfer-qr-wrap {
  margin: 16px 0;
  display: flex;
  justify-content: center;
}

.pos-transfer-qr {
  width: 320px;
  height: 320px;
}
</style>

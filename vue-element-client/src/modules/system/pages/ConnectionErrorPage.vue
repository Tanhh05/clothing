<template>
  <section class="connection-error-page">
    <el-result
      icon="error"
      title="Mất kết nối tới máy chủ"
      sub-title="Đang tự động kiểm tra lại kết nối. Khi kết nối ổn định hệ thống sẽ quay về trang chủ."
    >
      <template #extra>
        <el-button :loading="checking" type="primary" @click="checkNow">
          Kiểm tra lại
        </el-button>
      </template>
    </el-result>
  </section>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";

const router = useRouter();
const route = useRoute();
const checking = ref(false);
let timerId = null;

const healthUrl = `${import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api"}/store-settings`;
const redirectPath = computed(() => {
  const raw = String(route.query?.from || "").trim();
  if (!raw || raw === "/system/connection-error") return "/";
  return raw;
});

const checkNow = async () => {
  if (checking.value) return;
  checking.value = true;
  try {
    const response = await fetch(healthUrl, { method: "GET" });
    if (response.ok) {
      router.replace(redirectPath.value);
    }
  } catch (error) {
    // keep waiting for next check
  } finally {
    checking.value = false;
  }
};

onMounted(() => {
  checkNow();
  timerId = window.setInterval(checkNow, 4000);
});

onUnmounted(() => {
  if (timerId) {
    window.clearInterval(timerId);
    timerId = null;
  }
});
</script>

<style scoped lang="scss">
.connection-error-page {
  min-height: 72vh;
  display: grid;
  place-items: center;
}
</style>

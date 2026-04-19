<template>
  <section class="system-page">
    <el-result
      icon="info"
      title="404 - Không tìm thấy dữ liệu"
      :sub-title="subTitle"
    >
      <template #extra>
        <el-button type="primary" @click="$router.push('/')">Về trang chủ</el-button>
      </template>
    </el-result>
  </section>
</template>

<script setup>
import { computed } from "vue";
import { useRoute } from "vue-router";

const route = useRoute();
const subTitle = computed(() => {
  const msg = String(route.query?.message || "").trim();
  return msg || "Dữ liệu bạn tìm kiếm không tồn tại hoặc đã bị xóa.";
});
</script>

<style scoped lang="scss">
.system-page {
  min-height: 72vh;
  display: grid;
  place-items: center;
}
</style>

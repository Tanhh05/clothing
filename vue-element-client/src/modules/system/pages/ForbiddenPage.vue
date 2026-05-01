<template>
  <section class="system-page">
    <el-result
      icon="warning"
      title="403 - Không có quyền truy cập"
      sub-title="Bạn không có quyền thực hiện thao tác này."
    >
      <template #extra>
        <el-button type="primary" @click="$router.push('/')">Về trang chủ</el-button>
      </template>
    </el-result>
  </section>
</template>

<style scoped lang="scss">
.system-page {
  min-height: 72vh;
  display: grid;
  place-items: center;
}
</style>

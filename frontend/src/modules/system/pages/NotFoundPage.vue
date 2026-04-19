<template>
  <section class="not-found-page">
    <el-result
      icon="info"
      title="404 - Không tìm thấy trang"
      sub-title="Trang bạn truy cập không tồn tại hoặc đã bị thay đổi."
    >
      <template #extra>
        <el-button type="primary" @click="$router.push('/')">Về trang chủ</el-button>
      </template>
    </el-result>
  </section>
</template>

<style scoped lang="scss">
.not-found-page {
  min-height: 72vh;
  display: grid;
  place-items: center;
}
</style>

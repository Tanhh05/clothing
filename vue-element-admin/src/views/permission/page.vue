<template>
  <div class="app-container">
    <el-alert
      type="info"
      :closable="false"
      title="Trang quyền: hệ thống đang dùng role từ token đăng nhập."
    />
  </div>
</template>

<script>
export default {
  name: 'PagePermission'
}
</script>

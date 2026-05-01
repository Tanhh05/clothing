<template>
  <section v-loading="loading" class="admin-page">
    <div class="admin-card inventory-panel">
      <div class="admin-toolbar">
        <h3>Phân quyền theo backend</h3>
        <el-button class="admin-ghost-btn" @click="loadProfile">Làm mới</el-button>
      </div>

      <el-alert
        type="info"
        :closable="false"
        title="Quyền hiển thị menu và truy cập trang đang lấy từ role backend (/api/user/me)."
      />

      <el-descriptions :column="2" border style="margin-top: 14px">
        <el-descriptions-item label="Tài khoản">{{ profile.username || 'Không có' }}</el-descriptions-item>
        <el-descriptions-item label="Họ tên">{{ profile.fullName || 'Không có' }}</el-descriptions-item>
        <el-descriptions-item label="Email">{{ profile.email || 'Không có' }}</el-descriptions-item>
        <el-descriptions-item label="Trạng thái">{{ profile.status || 'Không có' }}</el-descriptions-item>
      </el-descriptions>

      <div style="margin-top: 14px">
        <p><strong>Roles hiện tại:</strong></p>
        <el-tag
          v-for="role in roles"
          :key="role"
          type="success"
          style="margin-right: 8px; margin-bottom: 8px"
        >
          {{ role }}
        </el-tag>
        <el-empty v-if="!roles.length" description="Không có role từ backend" />
      </div>
    </div>
  </section>
</template>

<script>
import request from '@/utils/request'

export default {
  name: 'PermissionIndex',
  data() {
    return {
      loading: false,
      profile: {},
      roles: []
    }
  },
  created() {
    this.loadProfile()
  },
  methods: {
    async loadProfile() {
      this.loading = true
      try {
        const data = await request({
          url: '/user/me',
          method: 'get'
        })
        this.profile = data || {}
        this.roles = Array.isArray(data && data.roles) ? data.roles : []
      } catch (error) {
        this.$message.error('Không tải được dữ liệu phân quyền từ backend')
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

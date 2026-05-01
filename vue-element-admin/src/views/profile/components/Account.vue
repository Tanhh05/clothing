<template>
  <div class="account-settings">
    <el-form ref="profileFormRef" :model="profileForm" label-position="top">
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="Tên đăng nhập">
            <el-input v-model.trim="profileForm.username" placeholder="Nhập tên đăng nhập" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="Email">
            <el-input v-model.trim="profileForm.email" placeholder="Nhập email" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="Họ và tên">
            <el-input v-model.trim="profileForm.fullName" placeholder="Nhập họ tên" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="Số điện thoại">
            <el-input v-model.trim="profileForm.phone" placeholder="Nhập số điện thoại" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item>
        <el-button type="primary" :loading="profileSaving" @click="submitProfile">Lưu thông tin</el-button>
      </el-form-item>
    </el-form>

    <el-divider />

    <el-form ref="passwordFormRef" :model="passwordForm" label-position="top">
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="Mật khẩu hiện tại">
            <el-input v-model="passwordForm.currentPassword" type="password" show-password />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="Mật khẩu mới">
            <el-input v-model="passwordForm.newPassword" type="password" show-password />
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item>
        <el-button type="warning" :loading="passwordSaving" @click="submitPassword">Đổi mật khẩu</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script>
import { updateMyProfile, changeMyPassword } from '@/api/user'

export default {
  props: {
    user: {
      type: Object,
      default: () => ({})
    }
  },
  data() {
    return {
      profileSaving: false,
      passwordSaving: false,
      profileForm: {
        username: '',
        email: '',
        fullName: '',
        phone: ''
      },
      passwordForm: {
        currentPassword: '',
        newPassword: ''
      }
    }
  },
  watch: {
    user: {
      immediate: true,
      deep: true,
      handler(next) {
        this.profileForm = {
          username: (next && next.username) || '',
          email: (next && next.email) || '',
          fullName: (next && (next.fullName || next.name)) || '',
          phone: (next && next.phone) || ''
        }
      }
    }
  },
  methods: {
    async submitProfile() {
      this.profileSaving = true
      try {
        await updateMyProfile({
          username: String(this.profileForm.username || '').trim() || null,
          email: String(this.profileForm.email || '').trim() || null,
          fullName: String(this.profileForm.fullName || '').trim() || null,
          phone: String(this.profileForm.phone || '').trim() || null
        })
        this.$message.success('Đã cập nhật thông tin tài khoản')
        this.$emit('updated')
      } catch (error) {
        this.$message.error((error && error.response && error.response.data && error.response.data.message) || 'Không thể cập nhật thông tin')
      } finally {
        this.profileSaving = false
      }
    },
    async submitPassword() {
      const currentPassword = String(this.passwordForm.currentPassword || '')
      const newPassword = String(this.passwordForm.newPassword || '')
      if (!currentPassword || !newPassword) {
        this.$message.warning('Vui lòng nhập mật khẩu hiện tại và mật khẩu mới')
        return
      }
      if (newPassword.length < 6) {
        this.$message.warning('Mật khẩu mới tối thiểu 6 ký tự')
        return
      }
      this.passwordSaving = true
      try {
        await changeMyPassword({ currentPassword, newPassword })
        this.$message.success('Đã đổi mật khẩu')
        this.passwordForm = { currentPassword: '', newPassword: '' }
      } catch (error) {
        this.$message.error((error && error.response && error.response.data && error.response.data.message) || 'Không đổi được mật khẩu')
      } finally {
        this.passwordSaving = false
      }
    }
  }
}
</script>

<style scoped>
.account-settings {
  max-width: 900px;
}
</style>

<template>
  <section class="register-page client-page-shell">
    <el-card shadow="never" class="register-card">


      <el-form label-position="top" @submit.prevent="onSubmit">
        <el-form-item label="Tên đăng nhập">
          <el-input v-model="form.username" placeholder="Nhập tên đăng nhập" clearable />
        </el-form-item>
        <el-form-item label="Email">
          <el-input v-model="form.email" placeholder="Nhập email" clearable />
        </el-form-item>
        <el-form-item label="Mật khẩu">
          <el-input v-model="form.password" type="password" show-password placeholder="Nhập mật khẩu" />
        </el-form-item>
        <el-form-item label="Họ và tên">
          <el-input v-model="form.fullName" placeholder="Nhập họ và tên" clearable />
        </el-form-item>
        <el-form-item label="Số điện thoại">
          <el-input v-model="form.phone" placeholder="Nhập số điện thoại" clearable />
        </el-form-item>
        <el-button type="primary" class="submit-btn" :loading="submitting" @click="onSubmit">Đăng ký</el-button>
      </el-form>

      <p class="login-link">
        Đã có tài khoản?
        <RouterLink to="/auth/login">Đăng nhập</RouterLink>
      </p>
    </el-card>
  </section>
</template>

<script setup>
import { reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { useAuthStore } from "@/store/authStore";

const router = useRouter();
const authStore = useAuthStore();
const submitting = ref(false);

const form = reactive({
  username: "",
  email: "",
  password: "",
  fullName: "",
  phone: ""
});

async function onSubmit() {
  if (!String(form.username || "").trim()) return ElMessage.warning("Vui lòng nhập tên đăng nhập");
  if (!String(form.email || "").trim()) return ElMessage.warning("Vui lòng nhập email");
  if (!String(form.password || "").trim()) return ElMessage.warning("Vui lòng nhập mật khẩu");

  submitting.value = true;
  try {
    await authStore.register(form);
    ElMessage.success("Đăng ký thành công, vui lòng đăng nhập");
    router.push("/auth/login");
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || "Đăng ký thất bại");
  } finally {
    submitting.value = false;
  }
}
</script>

<style scoped lang="scss">
.register-page {
  min-height: calc(100vh - 140px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px 12px 40px;
}

.register-card {
  width: 100%;
  max-width: 520px;
  border-radius: 0;

  :deep(.el-card__body) {
    padding: 22px;
  }
}

.head {
  margin-bottom: 8px;

  h1 {
    margin: 0 0 6px;
    font-size: 30px;
    font-weight: 900;
    letter-spacing: -0.4px;
  }

  p {
    margin: 0;
    color: #64748b;
    font-size: 14px;
  }
}

.submit-btn {
  width: 100%;
  margin-top: 4px;
}

.login-link {
  margin: 14px 0 0;
  text-align: center;
  font-size: 14px;
  color: #475569;

  a {
    margin-left: 4px;
    color: #111827;
    font-weight: 700;
    text-decoration: none;
  }
}
</style>

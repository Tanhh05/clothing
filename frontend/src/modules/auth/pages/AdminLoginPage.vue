<template>
  <section class="admin-login admin-auth-shell">
    <div class="admin-login__card">
      <p class="hint">Chỉ tài khoản quản trị mới được truy cập trang này.</p>

      <el-form class="stack" @submit.prevent="onSubmit">
        <el-form-item>
          <el-input v-model="form.usernameOrEmail" placeholder="Tên đăng nhập hoặc email" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" show-password placeholder="Mật khẩu" />
        </el-form-item>
        <el-button type="primary" native-type="submit" :loading="submitting" class="submit-btn">
          {{ submitting ? "Đang đăng nhập..." : "Đăng nhập admin" }}
        </el-button>
      </el-form>
    </div>
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
  usernameOrEmail: "",
  password: ""
});

async function onSubmit() {
  if (!form.usernameOrEmail || !form.password) {
    ElMessage.warning("Vui lòng nhập đầy đủ thông tin");
    return;
  }

  submitting.value = true;
  try {
    await authStore.loginAdmin(form);
    ElMessage.success("Đăng nhập admin thành công");
    router.push("/admin");
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || error?.message || "Đăng nhập thất bại");
  } finally {
    submitting.value = false;
  }
}
</script>

<style scoped lang="scss">
.admin-login {
  min-height: 100vh;
  padding: 24px;
  display: grid;
  place-items: center;
  background:
    radial-gradient(circle at top right, rgba(17, 24, 39, 0.08), transparent 40%),
    linear-gradient(120deg, #f8fafc 0%, #eff4f8 100%);
}

.admin-login__card {
  width: min(420px, 100%);
  border: 1px solid #dce1e7;
  background: #fff;
  padding: 24px;

  h1 {
    margin: 0;
    font-size: 28px;
    font-weight: 800;
    text-transform: uppercase;
    letter-spacing: 0.5px;
  }

  .hint {
    margin: 10px 0 18px;
    color: #6b7280;
    font-size: 13px;
  }
}

.stack {
  display: grid;
  gap: 10px;

  :deep(.el-form-item) {
    margin-bottom: 0;
  }

  :deep(.el-input__wrapper) {
    min-height: 44px;
    border-radius: 0;
  }
}

.submit-btn {
  width: 100%;
  min-height: 44px;
  border-radius: 0;
  font-weight: 700;
}
</style>

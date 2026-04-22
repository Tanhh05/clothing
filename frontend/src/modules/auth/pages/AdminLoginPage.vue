<template>
  <section class="admin-login admin-auth-shell">
    <div class="admin-login__shell">
      <div class="admin-login__brand">
        <p class="kicker">ADMIN PORTAL</p>
        <h1>Đăng nhập quản trị</h1>
        <p class="hint">Chỉ tài khoản có quyền admin mới truy cập được khu vực này.</p>
      </div>

      <el-form class="stack" @submit.prevent="onSubmit">
        <el-form-item>
          <el-input v-model="form.usernameOrEmail" placeholder="Tên đăng nhập hoặc email" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" show-password placeholder="Mật khẩu" />
        </el-form-item>
        <el-button type="primary" native-type="submit" :loading="submitting" class="submit-btn">
          {{ submitting ? "Đang đăng nhập..." : "Đăng nhập" }}
        </el-button>
      </el-form>
    </div>
  </section>
</template>

<script setup>
import { reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "@/utils/dialogMessage";
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
  padding: 20px;
  display: grid;
  place-items: center;
  background: #f3f5f7;
}

.admin-login__shell {
  width: min(430px, 100%);
  border: 1px solid #d8dee6;
  background: #fff;
  padding: 22px;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.06);
}

.admin-login__brand {
  margin-bottom: 14px;

  .kicker {
    margin: 0 0 6px;
    font-size: 11px;
    letter-spacing: 1.1px;
    color: #64748b;
    font-weight: 700;
  }

  h1 {
    margin: 0;
    font-size: 24px;
    font-weight: 700;
    color: #0f172a;
  }

  .hint {
    margin: 8px 0 0;
    color: #6b7280;
    font-size: 12px;
  }
}

.stack {
  display: grid;
  gap: 12px;

  :deep(.el-form-item) {
    margin-bottom: 0;
  }

  :deep(.el-input__wrapper) {
    min-height: 46px;
    border-radius: 8px;
    box-shadow: 0 0 0 1px #d7dee8 inset;
  }
}

.submit-btn {
  width: 100%;
  min-height: 46px;
  border-radius: 8px;
  font-weight: 600;
  letter-spacing: 0.2px;
}
</style>

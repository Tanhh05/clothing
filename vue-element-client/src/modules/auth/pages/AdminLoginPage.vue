<template>
  <section class="login-page client-page-shell">
    <el-card shadow="never" class="login-card">

      <el-form label-position="top" @submit.prevent="onSubmit">
        <el-form-item label="Tên đăng nhập hoặc Email">
          <el-input v-model="form.usernameOrEmail" placeholder="Nhập tên đăng nhập hoặc email" clearable />
        </el-form-item>
        <el-form-item label="Mật khẩu">
          <el-input v-model="form.password" type="password" show-password placeholder="Nhập mật khẩu" />
        </el-form-item>
        <el-button type="primary" native-type="submit" class="submit-btn" :loading="submitting">Đăng nhập</el-button>
      </el-form>
    </el-card>
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
.login-page {
  min-height: calc(100vh - 140px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px 12px 40px;
}

.login-card {
  width: 100%;
  max-width: 460px;
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
</style>

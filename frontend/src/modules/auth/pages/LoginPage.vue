<template>
  <section class="login-page client-page-shell">
    <el-card shadow="never" class="login-card">
      <div class="head">
      </div>

      <el-form label-position="top" @submit.prevent="onSubmit">
        <el-form-item label="Tên đăng nhập hoặc Email">
          <el-input v-model="form.usernameOrEmail" placeholder="Nhập tên đăng nhập hoặc email" clearable />
        </el-form-item>
        <el-form-item label="Mật khẩu">
          <el-input v-model="form.password" type="password" show-password placeholder="Nhập mật khẩu" />
        </el-form-item>
        <el-button type="primary" class="submit-btn" :loading="submitting" @click="onSubmit">Đăng nhập</el-button>
      </el-form>

      <el-divider>HOẶC</el-divider>
      <div class="google-wrap">
        <div ref="googleButtonRef"></div>
      </div>

      <p class="register-link">
        Chưa có tài khoản?
        <RouterLink to="/auth/register">Tạo tài khoản</RouterLink>
      </p>
    </el-card>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "@/utils/dialogMessage";
import { useAuthStore } from "@/store/authStore";

const router = useRouter();
const authStore = useAuthStore();
const googleButtonRef = ref(null);
const googleClientId = import.meta.env.VITE_GOOGLE_CLIENT_ID;
const submitting = ref(false);

const form = reactive({
  usernameOrEmail: "",
  password: ""
});

async function onSubmit() {
  if (!String(form.usernameOrEmail || "").trim()) {
    ElMessage.warning("Vui lòng nhập tên đăng nhập hoặc email");
    return;
  }
  if (!String(form.password || "").trim()) {
    ElMessage.warning("Vui lòng nhập mật khẩu");
    return;
  }
  submitting.value = true;
  try {
    await authStore.login(form);
    router.push("/products");
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || "Đăng nhập thất bại");
  } finally {
    submitting.value = false;
  }
}

async function handleGoogleCredential(response) {
  if (!response?.credential) {
    return;
  }
  try {
    await authStore.loginWithGoogle(response.credential);
    router.push("/products");
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || "Đăng nhập Google thất bại");
  }
}

onMounted(() => {
  if (!googleClientId || !googleButtonRef.value) {
    return;
  }

  loadGoogleScript().then(() => {
    if (!window.google?.accounts?.id) {
      return;
    }
    window.google.accounts.id.initialize({
      client_id: googleClientId,
      callback: handleGoogleCredential,
      auto_select: false
    });

    window.google.accounts.id.renderButton(googleButtonRef.value, {
      theme: "outline",
      size: "large",
      shape: "rectangular",
      text: "continue_with",
      width: 280
    });
  });
});

function loadGoogleScript() {
  if (window.google?.accounts?.id) {
    return Promise.resolve();
  }
  return new Promise((resolve, reject) => {
    const existing = document.querySelector('script[src="https://accounts.google.com/gsi/client"]');
    if (existing) {
      existing.addEventListener("load", () => resolve(), { once: true });
      existing.addEventListener("error", () => reject(new Error("Failed to load Google script")), { once: true });
      return;
    }

    const script = document.createElement("script");
    script.src = "https://accounts.google.com/gsi/client";
    script.async = true;
    script.defer = true;
    script.onload = () => resolve();
    script.onerror = () => reject(new Error("Failed to load Google script"));
    document.head.appendChild(script);
  });
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

.google-wrap {
  display: flex;
  justify-content: center;
  min-height: 44px;
}

.register-link {
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

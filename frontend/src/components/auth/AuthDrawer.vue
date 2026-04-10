<template>
  <el-drawer
    v-model="visible"
    direction="rtl"
    size="450px"
    :with-header="false"
    class="auth-drawer"
    @closed="resetMode"
  >
    <div class="drawer-header">
      <div class="logo-area">
        <el-icon v-if="!authStore.isAuthenticated && authMode !== 'select'" class="back-icon" @click="authMode = 'select'"><Back /></el-icon>
        <span class="logo-text">TWENTY</span>
      </div>
      <el-icon class="close-icon" @click="visible = false"><Close /></el-icon>
    </div>

    <div class="drawer-content" v-loading="loading">
      <!-- AUTHENTICATED VIEW -->
      <div v-if="authStore.isAuthenticated" class="view-authenticated fade-in">
        <h1 class="auth-title">HỒ SƠ CỦA TÔI</h1>
        <div class="profile-summary">
          <div class="avatar">{{ profileInitial }}</div>
          <div class="summary-content">
            <p class="welcome-text">XIN CHÀO, <span>{{ displayName.toUpperCase() }}</span></p>
            <p class="welcome-sub">Rất vui được gặp lại bạn!</p>
          </div>
        </div>

        <div class="account-menu compact">
          <button type="button" class="menu-item" @click="goToAccount">Thông tin cá nhân</button>
          <button type="button" class="menu-item" @click="goToOrders">Đơn hàng của tôi</button>
          <button type="button" class="menu-item" @click="goToWishlist">Sản phẩm yêu thích</button>
        </div>

        <el-button type="primary" class="submit-btn logout-btn" @click="handleLogout">
          ĐĂNG XUẤT <el-icon class="el-icon--right"><Right /></el-icon>
        </el-button>
      </div>

      <!-- GUEST VIEWS -->
      <template v-else>
        <!-- SELECT VIEW -->
        <div v-if="authMode === 'select'" class="view-select fade-in">
          <h1 class="auth-title">ĐĂNG NHẬP HOẶC ĐĂNG KÝ</h1>
          <p class="auth-subtitle">
            Tận hưởng quyền truy cập chỉ dành cho hội viên vào các sản phẩm, trải nghiệm, ưu đãi độc quyền và hơn thế nữa.
          </p>

          <div class="social-login-grid">
            <div ref="googleButtonRef" class="google-button-host"></div>
            <el-button v-if="!googleReady" class="retry-google-btn" @click="handleGoogleLogin">
              THỬ TẢI LẠI GOOGLE
            </el-button>
          </div>

          <div class="divider">
            <span>HOẶC</span>
          </div>

          <div class="action-buttons">
            <el-button class="mode-btn primary" @click="authMode = 'login'">
              ĐĂNG NHẬP <el-icon class="el-icon--right"><Right /></el-icon>
            </el-button>
            <el-button class="mode-btn secondary" @click="authMode = 'register'">
              ĐĂNG KÝ <el-icon class="el-icon--right"><Right /></el-icon>
            </el-button>
          </div>
        </div>

        <!-- LOGIN VIEW -->
        <div v-else-if="authMode === 'login'" class="view-login fade-in">
          <h1 class="auth-title">ĐĂNG NHẬP</h1>
          <el-form :model="loginForm" @submit.prevent="handleLogin" class="auth-form">
            <div class="form-group">
              <label>EMAIL HOẶC TÊN ĐĂNG NHẬP *</label>
              <el-input v-model="loginForm.usernameOrEmail" placeholder="" />
            </div>
            <div class="form-group">
              <label>MẬT KHẨU *</label>
              <el-input v-model="loginForm.password" type="password" show-password />
            </div>

            <div class="checkbox-group stay-logged">
              <el-checkbox v-model="loginForm.stayLogged">
                Giữ đăng nhập cho tôi. Áp dụng cho mọi tùy chọn.
              </el-checkbox>
              <a href="#" class="info-link">Thêm thông tin</a>
            </div>

            <el-button type="primary" class="submit-btn" native-type="submit" :loading="loading">
              ĐĂNG NHẬP <el-icon class="el-icon--right"><Right /></el-icon>
            </el-button>
            
            <div class="sub-actions">
              <a @click="authMode = 'register'">Chưa có tài khoản? Đăng ký ngay</a>
            </div>
          </el-form>
        </div>

        <!-- REGISTER VIEW -->
        <div v-else-if="authMode === 'register'" class="view-register fade-in">
          <h1 class="auth-title">ĐĂNG KÝ THÀNH VIÊN</h1>
          <el-form :model="registerForm" @submit.prevent="handleRegister" class="auth-form">
            <div class="form-group">
              <label>HỌ VÀ TÊN *</label>
              <el-input v-model="registerForm.fullName" placeholder="" />
            </div>
            <div class="form-row">
              <div class="form-group">
                <label>SỐ ĐIỆN THOẠI *</label>
                <el-input v-model="registerForm.phone" placeholder="" />
              </div>
            </div>
            <div class="form-group">
              <label>ĐỊA CHỈ EMAIL *</label>
              <el-input v-model="registerForm.email" placeholder="" />
            </div>
            <div class="form-group">
              <label>TÊN ĐĂNG NHẬP *</label>
              <el-input v-model="registerForm.username" placeholder="" />
            </div>
            <div class="form-group">
              <label>MẬT KHẨU (Ít nhất 8 ký tự) *</label>
              <el-input v-model="registerForm.password" type="password" show-password />
            </div>

            <div class="checkbox-group">
              <el-checkbox v-model="agreements.age">Có, tôi trên 16 tuổi</el-checkbox>
            </div>

            <div class="checkbox-group">
              <el-checkbox v-model="agreements.terms">
                Tôi chấp thuận <a href="#">Chính sách Bảo mật</a> và <a href="#">Điều khoản sử dụng</a>. *
              </el-checkbox>
            </div>

            <el-button type="primary" class="submit-btn" native-type="submit" :loading="loading">
              ĐĂNG KÝ <el-icon class="el-icon--right"><Right /></el-icon>
            </el-button>

            <div class="sub-actions">
              <a @click="authMode = 'login'">Đã có tài khoản? Đăng nhập ngay</a>
            </div>
          </el-form>
        </div>
      </template>
    </div>
  </el-drawer>
</template>

<script setup>
import { reactive, ref, computed, onMounted, watch, nextTick } from 'vue';
import { Close, Right, Back } from '@element-plus/icons-vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/store/authStore';
import { useCartStore } from '@/store/cartStore';
import { useWishlistStore } from '@/store/wishlistStore';
import { ElMessage } from 'element-plus';

const props = defineProps({
  modelValue: Boolean
});

const emit = defineEmits(['update:modelValue']);
const router = useRouter();

const authStore = useAuthStore();
const cartStore = useCartStore();
const wishlistStore = useWishlistStore();
const loading = ref(false);
const googleReady = ref(false);
const googleButtonRef = ref(null);
const authMode = ref('select'); // select | login | register
const googleClientId = import.meta.env.VITE_GOOGLE_CLIENT_ID;

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
});

const loginForm = reactive({
  usernameOrEmail: '',
  password: '',
  stayLogged: false
});

const registerForm = reactive({
  username: '',
  email: '',
  password: '',
  fullName: '',
  phone: ''
});

const agreements = reactive({
  age: false,
  terms: false
});

const displayName = computed(() => {
  return authStore.profile?.fullName?.trim()
    || authStore.profile?.username
    || authStore.username
    || 'THÀNH VIÊN';
});

const profileInitial = computed(() => {
  const firstChar = displayName.value?.trim()?.charAt(0);
  return (firstChar || 'U').toUpperCase();
});

const resetMode = () => {
  authMode.value = 'select';
  loginForm.usernameOrEmail = '';
  loginForm.password = '';
};

// --- AUTH LOGIC ---
const handleLogout = () => {
  authStore.clearClientAuth();
  cartStore.clearCartLocal();
  wishlistStore.resetServerState();
  ElMessage.info('Đã đăng xuất');
  resetMode();
  visible.value = false;
};

const goToWishlist = () => {
  visible.value = false;
  router.push('/wishlist');
};

const goToOrders = () => {
  visible.value = false;
  router.push('/orders');
};

const goToAccount = () => {
  visible.value = false;
  router.push('/account');
};

// --- GOOGLE LOGIN LOGIC ---
const loadGoogleScript = () => {
  return new Promise((resolve, reject) => {
    if (window.google?.accounts?.id) {
      resolve();
      return;
    }
    const script = document.createElement('script');
    script.src = 'https://accounts.google.com/gsi/client';
    script.async = true;
    script.defer = true;
    script.onload = () => resolve();
    script.onerror = () => reject(new Error('Failed to load Google SDK'));
    document.head.appendChild(script);
  });
};

const handleCredentialResponse = async (response) => {
  if (!response?.credential) return;
  
  loading.value = true;
  try {
    await authStore.loginWithGoogle(response.credential);
    await cartStore.syncLocalCart();
    await wishlistStore.syncLocalWishlist();
    ElMessage.success('Đăng nhập Google thành công!');
    visible.value = false;
  } catch (error) {
    ElMessage.error('Đăng nhập Google thất bại');
    console.error(error);
  } finally {
    loading.value = false;
  }
};

const initializeGoogle = () => {
  if (!window.google?.accounts?.id || !googleClientId || !googleButtonRef.value) return;

  googleButtonRef.value.innerHTML = '';
  const hostWidth = Math.floor(googleButtonRef.value.clientWidth || 340);
  const buttonWidth = Math.max(220, Math.min(360, hostWidth));
  window.google.accounts.id.initialize({
    client_id: googleClientId,
    callback: handleCredentialResponse,
    auto_select: false,
    use_fedcm_for_prompt: false,
    ux_mode: 'popup'
  });
  window.google.accounts.id.renderButton(googleButtonRef.value, {
    type: 'standard',
    theme: 'outline',
    size: 'large',
    text: 'continue_with',
    shape: 'rectangular',
    width: buttonWidth
  });
  requestAnimationFrame(() => {
    googleReady.value = Boolean(googleButtonRef.value?.children?.length);
  });
};

const handleGoogleLogin = async () => {
  try {
    await loadGoogleScript();
    initializeGoogle();
  } catch (err) {
    ElMessage.error('Không thể nạp ứng dụng Google');
  }
};

onMounted(async () => {
  try {
    await loadGoogleScript();
    initializeGoogle();
  } catch (err) {
    console.error('Initial Google SDK load failed');
  }
});

watch(
  () => [visible.value, authStore.isAuthenticated],
  async ([isOpen, isAuthenticated]) => {
    if (!isOpen || !isAuthenticated) return;
    try {
      await authStore.fetchProfile();
    } catch (error) {
      // skip noisy toast on drawer open
    }
  },
  { immediate: true }
);

watch(
  () => [visible.value, authMode.value],
  async ([isOpen, mode]) => {
    if (!isOpen || mode !== 'select') return;
    await nextTick();
    await handleGoogleLogin();
  }
);

const handleLogin = async () => {
  if (!loginForm.usernameOrEmail || !loginForm.password) {
    ElMessage.warning('Vui lòng nhập đầy đủ thông tin');
    return;
  }

  loading.value = true;
  try {
    await authStore.login({
      usernameOrEmail: loginForm.usernameOrEmail,
      password: loginForm.password
    });
    await cartStore.syncLocalCart();
    await wishlistStore.syncLocalWishlist();
    ElMessage.success('Đăng nhập thành công!');
    visible.value = false;
  } catch (error) {
    ElMessage.error(error.response?.data?.message || 'Đăng nhập thất bại');
  } finally {
    loading.value = false;
  }
};

const handleRegister = async () => {
  if (!registerForm.email || !registerForm.password || !registerForm.username) {
    ElMessage.warning('Vui lòng nhập đầy đủ các trường bắt buộc');
    return;
  }
  if (!agreements.terms || !agreements.age) {
    ElMessage.warning('Bạn cần chấp thuận các điều khoản');
    return;
  }

  loading.value = true;
  try {
    await authStore.register(registerForm);
    ElMessage.success('Đăng ký thành công! Hãy đăng nhập để tiếp tục.');
    authMode.value = 'login';
  } catch (error) {
    ElMessage.error(error.response?.data?.message || 'Đăng ký thất bại');
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped lang="scss">
.auth-drawer {
  :deep(.el-drawer__body) {
    padding: 0;
  }
}

.drawer-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 40px;
  border-bottom: 1px solid #ebedee;

  .logo-area {
    display: flex;
    align-items: center;
    gap: 20px;

    .back-icon {
      font-size: 20px;
      cursor: pointer;
      &:hover { color: #888; }
    }

    .logo-text {
      font-size: 24px;
      font-weight: 900;
      letter-spacing: 2px;
      color: #000;
      font-family: 'Inter', sans-serif;
    }
  }

  .close-icon {
    font-size: 24px;
    cursor: pointer;
    font-weight: bold;
    &:hover { color: #888; }
  }
}

.drawer-content {
  padding: 40px;
  height: calc(100vh - 80px);
  overflow-y: auto;

  .auth-title {
    font-size: 24px;
    font-weight: 900;
    margin-bottom: 20px;
    letter-spacing: 1px;
    text-transform: uppercase;
  }

  .auth-subtitle {
    font-size: 14px;
    line-height: 1.6;
    margin-bottom: 30px;
    color: #333;
  }
}

/* AUTHENTICATED VIEW */
.profile-summary {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 24px;
  padding: 14px;
  border: 1px solid #ebedee;
  background: #fafafa;

  .avatar {
    width: 42px;
    height: 42px;
    border-radius: 999px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: #000;
    color: #fff;
    font-size: 16px;
    font-weight: 800;
  }

  .summary-content {
    min-width: 0;
  }

  .welcome-text {
    margin: 0 0 4px;
    font-size: 14px;
    font-weight: 800;
    line-height: 1.35;
    word-break: break-word;

    span {
      color: #000;
      text-decoration: underline;
    }
  }

  .welcome-sub {
    margin: 0;
    font-size: 12px;
    color: #666;
  }
}

.account-menu.compact {
  display: grid;
  gap: 10px;
  margin-bottom: 24px;
  border: none;

  .menu-item {
    width: 100%;
    height: 46px;
    border: 1px solid #ebedee;
    background: #fff;
    font-size: 13px;
    font-weight: 700;
    text-align: left;
    padding: 0 14px;
    cursor: pointer;
    transition: all 0.2s;
    color: #111;
  }

  .menu-item:hover {
    border-color: #000;
    background: #f7f7f7;
  }
}

.profile-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;

  .auth-title {
    margin: 0;
  }
}

.back-profile-btn {
  width: 34px;
  height: 34px;
  border: 1px solid #000;
  background: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.profile-form {
  margin-top: 12px;
}

.logout-btn {
  margin-top: 0;
}

/* SECTION VIEWS */
.action-buttons {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.mode-btn {
  width: 100%;
  height: 54px;
  font-weight: 800;
  font-size: 13px;
  letter-spacing: 1px;
  justify-content: space-between;
  padding: 0 18px;
  border-radius: 0;
  margin: 0 !important;

  &.primary {
    background: #000;
    border-color: #000;
    color: #fff;
    &:hover { background: #333; border-color: #333; }
  }

  &.secondary {
    background: #fff;
    border-color: #000;
    color: #000;
    &:hover { background: #f5f5f5; border-color: #000; }
  }
}

.divider {
  text-align: center;
  position: relative;
  margin: 28px 0 24px;
  
  &::before {
    content: '';
    position: absolute;
    left: 0;
    top: 50%;
    width: 100%;
    height: 1px;
    background: #ebedee;
  }
  
  span {
    position: relative;
    background: #fff;
    padding: 0 15px;
    font-size: 12px;
    font-weight: 800;
    color: #777;
    text-transform: uppercase;
  }
}

.social-login-grid {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 10px;
  margin-bottom: 10px;

  .google-button-host {
    width: 100%;
    display: flex;
    justify-content: center;
    min-height: 44px;
    border: 1px solid #d3d7db;
    border-radius: 4px;
    padding: 2px;
    box-sizing: border-box;
  }

  .retry-google-btn {
    width: 100%;
    border-radius: 0;
    font-weight: 700;
  }
}

.view-select {
  max-width: 380px;
  margin: 10px auto 0;

  .auth-title {
    font-size: 22px;
    line-height: 1.18;
    margin-bottom: 16px;
  }

  .auth-subtitle {
    font-size: 14px;
    line-height: 1.45;
    color: #2f2f2f;
    margin-bottom: 24px;
  }
}

.auth-form {
  .form-group {
    margin-bottom: 25px;
    
    label {
      display: block;
      font-size: 11px;
      font-weight: 800;
      color: #767677;
      margin-bottom: 8px;
    }

    :deep(.el-input__wrapper) {
      border: 1px solid #000;
      box-shadow: none !important;
      border-radius: 0;
      height: 50px;
      padding: 0 15px;
      
      &.is-focus {
        border-color: #000;
      }
    }
  }
}

.checkbox-group {
  margin-bottom: 15px;
  display: flex;
  flex-direction: column;

  :deep(.el-checkbox) {
    height: auto;
    white-space: normal;
    align-items: flex-start;
    
    .el-checkbox__label {
      font-size: 13px;
      line-height: 1.4;
      color: #000;
      padding-top: 2px;
      
      a {
        color: #000;
        text-decoration: underline;
      }
    }
    
    .el-checkbox__input.is-checked .el-checkbox__inner {
      background-color: #000;
      border-color: #000;
    }
  }

  .info-link {
    font-size: 13px;
    color: #000;
    text-decoration: underline;
    margin-left: 24px;
    margin-top: 4px;
  }
}

.stay-logged {
  margin-top: 10px;
  margin-bottom: 30px;
}

.submit-btn {
  width: 100%;
  height: 55px;
  background-color: #000;
  border-color: #000;
  color: #fff;
  font-weight: 800;
  font-size: 13px;
  letter-spacing: 1px;
  display: flex;
  justify-content: space-between;
  padding: 0 20px;
  border-radius: 0;

  &:hover {
    background-color: #333;
    border-color: #333;
  }
}

.sub-actions {
  margin-top: 25px;
  text-align: center;
  
  a {
    font-size: 14px;
    font-weight: 700;
    color: #000;
    text-decoration: underline;
    cursor: pointer;
    
    &:hover { color: #666; }
  }
}

.fade-in {
  animation: fadeIn 0.3s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

@media (max-width: 768px) {
  .auth-drawer {
    :deep(.el-drawer) {
      width: 100% !important;
      max-width: 100% !important;
    }
  }

  .drawer-header {
    padding: 14px 16px;

    .logo-area {
      gap: 12px;
    }

    .logo-area .logo-text {
      font-size: 20px;
    }
  }

  .drawer-content {
    padding: 18px 16px 24px;
    height: calc(100vh - 64px);

    .auth-title {
      font-size: 20px;
      margin-bottom: 14px;
    }
  }

  .view-select {
    max-width: none;
    margin-top: 6px;
  }

  .view-select .auth-title {
    font-size: 22px;
    margin-bottom: 14px;
  }

  .view-select .auth-subtitle {
    font-size: 14px;
    margin-bottom: 20px;
  }

  .mode-btn {
    height: 52px;
    font-size: 13px;
    letter-spacing: 0.8px;
  }
}

@media (max-width: 480px) {
  .view-select .auth-title {
    font-size: 20px;
  }

  .view-select .auth-subtitle {
    font-size: 13px;
    line-height: 1.4;
  }

  .mode-btn {
    height: 50px;
    font-size: 14px;
  }
}
</style>

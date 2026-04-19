<template>
  <header class="app-header">
    <div class="promo-top" @click="isPromoOpen = !isPromoOpen">
      <span>MIỄN PHÍ VẬN CHUYỂN CHO THÀNH VIÊN ADICLUB VÀ TẤT CẢ ĐƠN HÀNG TRÊN ỨNG DỤNG
</span>
      <el-icon><ArrowDown v-if="!isPromoOpen" /><ArrowUp v-else /></el-icon>
    </div>

    <el-collapse-transition>
      <div v-show="isPromoOpen" class="promo-panel">
        <div class="promo-panel-inner">
          <div class="promo-close" @click="isPromoOpen = false">
            <el-icon><Close /></el-icon>
          </div>
          <div class="promo-grid">
            <div class="promo-col">
              <h4>GIAO HÀNG MIỄN PHÍ CHO THÀNH VIÊN CỦA ADICLUB</h4>
              <p>Đăng ký thành viên adiClub để hưởng thụ dịch vụ giao hàng miễn phí! Hoặc bạn chỉ được nhận ưu đãi miễn phí giao hàng với hóa đơn có trị giá ít nhất 1.6 triệu đồng</p>
              <a href="#" class="promo-link">THAM GIA NGAY</a>
            </div>
            <div class="promo-col">
              <h4>TRẢ HÀNG DỄ DÀNG</h4>
              <p>Nếu bạn không hài lòng với đơn hàng của mình, bạn có thể được hoàn lại tiền. Vui lòng xem Chính Sách Trả Hàng của chúng tôi để biết thêm chi tiết.</p>
              <a href="#" class="promo-link">TRẢ HÀNG DỄ DÀNG</a>
            </div>
          </div>
        </div>
      </div>
    </el-collapse-transition>

    <el-menu
      mode="horizontal"
      :ellipsis="false"
      class="main-nav"
      background-color="#fff"
      text-color="#000"
      active-text-color="#000"
    >
      <!-- LOGO -->
      <el-menu-item index="logo" class="logo-item">
        <RouterLink to="/" class="logo-text">TWENTY</RouterLink>
      </el-menu-item>

      <div class="flex-grow" />

      <!-- MENU -->
      <template v-for="item in topMenuItems" :key="item.key">
        <el-sub-menu
          v-if="megaMenuMap[item.key] && megaMenuMap[item.key].length > 0"
          :index="item.key"
          popper-class="mega-menu-popper"
        >
          <template #title>
            <span class="menu-label">{{ item.label }}</span>
          </template>

          <div class="mega-menu-content">
            <div
              v-for="column in megaMenuMap[item.key]"
              :key="column.id"
              class="mega-column"
            >
              <h4 class="menu-heading">
                <RouterLink :to="`/products?category=${column.id}`">
                  {{ column.title }}
                </RouterLink>
              </h4>
              <el-menu-item
                v-for="link in column.links"
                :key="link.id"
                :index="`/products?category=${link.id}`"
              >
                <RouterLink :to="`/products?category=${link.id}`">
                  {{ link.name }}
                </RouterLink>
              </el-menu-item>
            </div>
          </div>
        </el-sub-menu>

        <el-menu-item v-else :index="item.to">
          <RouterLink :to="item.to">
            <span class="menu-label">{{ item.label }}</span>
          </RouterLink>
        </el-menu-item>
      </template>

      <div class="flex-grow" />

      <!-- TOOLS -->
      <div class="tools-container">
        <div class="utility-links">
          <a href="#">tìm cửa hàng</a>
          <a href="#">trợ giúp</a>
          <RouterLink to="/orders">trình theo dõi đơn hàng</RouterLink>
          <RouterLink v-if="authStore.isAuthenticated" to="/account">tài khoản</RouterLink>
          <RouterLink v-if="!authStore.isAuthenticated" to="/auth/login">đăng nhập</RouterLink>
          <a v-if="authStore.isAuthenticated" href="#" @click.prevent="handleLogout">đăng xuất</a>
        </div>

        <div class="actions">
          <div class="search-wrapper" @focusin="isSearchOverlayOpen = true" @focusout="handleSearchBlur">
            <el-input
              v-model="searchQuery"
              placeholder="Search"
              class="search-input"
              :prefix-icon="Search"
              @input="handleSearchInput"
              clearable
            />

            <!-- Search Overlay -->
            <transition name="fade">
              <div v-if="isSearchOverlayOpen && (searchResults.products.length > 0 || searchResults.suggestions.length > 0)" class="search-overlay">
                <div class="search-overlay-content">
                  <!-- Left: Suggestions -->
                  <div class="search-suggestions-col">
                    <h5 class="overlay-title">Suggestions</h5>
                    <ul class="suggestion-list">
                      <li v-for="s in searchResults.suggestions" :key="s.term" class="suggestion-item" @click="applySuggestion(s.term)">
                        <span class="suggestion-term">{{ s.term }}</span>
                        <span class="suggestion-count">{{ s.count }}</span>
                      </li>
                    </ul>
                    <div class="view-all-link" @click="goToSearch">
                      Xem tất cả "{{ searchQuery }}"
                    </div>
                  </div>

                  <!-- Right: Products -->
                  <div class="search-products-col">
                    <h5 class="overlay-title">Sản phẩm</h5>
                    <div class="search-product-list">
                      <div v-for="p in searchResults.products" :key="p.id" class="search-product-item" @click="goToProduct(p)">
                        <div class="search-prod-img">
                          <img :src="p.mainImageUrl" :alt="p.name">
                        </div>
                        <div class="search-prod-info">
                          <p class="search-prod-brand">{{ p.brand || 'Twenty Performance' }}</p>
                          <p class="search-prod-name">{{ p.name }}</p>
                          <p class="search-prod-price">{{ formatCurrency(p.minPrice) }}</p>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </transition>
          </div>

          <el-popover
            v-if="authStore.isAuthenticated"
            placement="bottom-end"
            :width="360"
            trigger="click"
            popper-class="notification-popper"
          >
            <template #reference>
              <el-badge :value="unreadCount" :hidden="unreadCount === 0" class="notification-badge">
                <button type="button" class="action-icon icon-btn" aria-label="Thông báo">
                  <el-icon><Bell /></el-icon>
                </button>
              </el-badge>
            </template>
            <div class="notification-popover">
              <div class="notification-head">
                <strong>Thông báo</strong>
                <el-button
                  text
                  size="small"
                  :loading="notificationLoading"
                  :disabled="notificationLoading || !notifications.length"
                  @click="markAllNotificationsAsRead"
                >
                  Đánh dấu đã đọc
                </el-button>
              </div>
              <div v-if="notificationLoading" class="notification-empty">Đang tải thông báo...</div>
              <div v-else-if="notifications.length" class="notification-list">
                <button
                  v-for="item in notifications"
                  :key="item.id"
                  type="button"
                  class="notification-item"
                  :class="{ unread: !item.isRead }"
                  @click="markNotificationAsRead(item)"
                >
                  <p class="noti-title">{{ item.title }}</p>
                  <p class="noti-content">{{ item.content }}</p>
                  <small class="noti-time">{{ formatDateTime(item.createdAt) }}</small>
                </button>
              </div>
              <p v-else class="notification-empty">{{ notificationError || "Chưa có thông báo" }}</p>
            </div>
          </el-popover>

          <button type="button" class="action-icon icon-btn" @click="goToAccount" aria-label="Tài khoản">
            <el-icon><User /></el-icon>
          </button>
          <el-badge :value="wishlistCount" :hidden="wishlistCount === 0" class="wishlist-badge">
            <button type="button" class="action-icon icon-btn" @click="goToWishlist" aria-label="Yêu thích">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7">
                <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/>
              </svg>
            </button>
          </el-badge>

          <RouterLink to="/cart" class="cart-link">
            <el-badge :value="cartStore.totalItems" :hidden="cartStore.isEmpty" class="cart-badge">
              <el-icon class="action-icon"><ShoppingBag /></el-icon>
            </el-badge>
          </RouterLink>
        </div>
      </div>
    </el-menu>

  </header>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { Search, User, ShoppingBag, ArrowDown, ArrowUp, Close, Bell } from '@element-plus/icons-vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import api from '@/services/api';
import { useAuthStore } from '@/store/authStore';
import { useCartStore } from '@/store/cartStore';
import { useWishlistStore } from '@/store/wishlistStore';
import { useConfirmDialog } from '@/composables/useConfirmDialog';
import { notificationClientApi } from '@/modules/notification/api/notificationClientApi';

const router = useRouter();
const { confirm } = useConfirmDialog();
const isPromoOpen = ref(false);
const searchQuery = ref('');
const isSearchOverlayOpen = ref(false);
const searchResults = ref({ products: [], suggestions: [] });
let searchTimeout = null;
let notificationInterval = null;

const categories = ref([]);
const authStore = useAuthStore();
const cartStore = useCartStore();
const wishlistStore = useWishlistStore();
const wishlistCount = computed(() => wishlistStore.productIds.length);
const notifications = ref([]);
const unreadCount = ref(0);
const notificationLoading = ref(false);
const notificationError = ref("");
const pollingInFlight = ref(false);

const formatCurrency = (val) => {
  if (!val) return 'Liên hệ';
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val);
};

const formatDateTime = (value) => {
  if (!value) return '';
  return new Date(value).toLocaleString('vi-VN');
};

const handleSearchInput = () => {
  if (searchTimeout) clearTimeout(searchTimeout);

  if (!searchQuery.value.trim()) {
    searchResults.value = { products: [], suggestions: [] };
    return;
  }

  searchTimeout = setTimeout(async () => {
    try {
      const response = await api.get('/search/products', {
        params: { q: searchQuery.value, size: 5 }
      });
      const payload = response.data;
      if (Array.isArray(payload)) {
        searchResults.value = { products: payload, suggestions: [] };
      } else {
        searchResults.value = {
          products: payload?.products || [],
          suggestions: payload?.suggestions || []
        };
      }
    } catch (error) {
      console.error('Search failed:', error);
    }
  }, 300);
};

const handleSearchBlur = () => {
  // Delay to allow click on results
  setTimeout(() => {
    isSearchOverlayOpen.value = false;
  }, 200);
};

const applySuggestion = (term) => {
  searchQuery.value = term;
  handleSearchInput();
};

const goToProduct = (product) => {
  router.push(`/products/${product.slug || product.id}`);
  isSearchOverlayOpen.value = false;
};

const goToSearch = () => {
  // Navigate to full search results page
  router.push(`/products?q=${searchQuery.value}`);
  isSearchOverlayOpen.value = false;
};

const goToWishlist = () => {
  router.push('/wishlist');
};

const goToAccount = () => {
  if (authStore.isAuthenticated) {
    router.push('/account');
    return;
  }
  router.push('/auth/login');
};

const categoriesByParent = computed(() => {
  const map = {};
  for (const item of categories.value) {
    const key = item.parentId == null ? 'root' : String(item.parentId);
    if (!map[key]) {
      map[key] = [];
    }
    map[key].push(item);
  }
  return map;
});

const topMenuItems = computed(() => {
  const roots = categoriesByParent.value.root || [];
  return roots.map((item) => ({
    key: String(item.id),
    label: (item.name || '').toUpperCase(),
    to: `/products?category=${item.id}`
  }));
});

const megaMenuMap = computed(() => {
  const roots = categoriesByParent.value.root || [];
  const map = {};

  for (const root of roots) {
    const children = categoriesByParent.value[String(root.id)] || [];
    map[String(root.id)] = children.map((child) => {
      const grandChildren = categoriesByParent.value[String(child.id)] || [];
      return {
        id: child.id,
        title: child.name,
        links: grandChildren.length
          ? grandChildren.map((node) => ({ id: node.id, name: node.name }))
          : [{ id: child.id, name: `All ${child.name}` }]
      };
    });
  }

  return map;
});

const fetchAllCategories = async () => {
  const merged = [];
  let page = 0;
  const size = 100;
  let hasMore = true;

  while (hasMore) {
    const response = await api.get('/categories', {
      params: { page, size, sortBy: 'id', direction: 'asc' }
    });
    const data = response?.data || {};
    const items = data.content || [];
    merged.push(...items);
    hasMore = data.last === false && items.length > 0;
    page += 1;
  }

  categories.value = merged;
};

const fetchNotifications = async () => {
  if (!authStore.isAuthenticated) {
    notifications.value = [];
    unreadCount.value = 0;
    notificationError.value = "";
    return;
  }
  if (pollingInFlight.value) {
    return;
  }
  pollingInFlight.value = true;
  notificationLoading.value = true;
  notificationError.value = "";
  try {
    const [listRes, unreadRes] = await Promise.all([
      notificationClientApi.getMyNotifications(),
      notificationClientApi.getUnreadCount()
    ]);
    notifications.value = Array.isArray(listRes?.data) ? listRes.data : [];
    unreadCount.value = Number(unreadRes?.data?.unread || 0);
  } catch (error) {
    if (error?.response?.status === 401) {
      notifications.value = [];
      unreadCount.value = 0;
      notificationError.value = "Phiên đăng nhập đã hết hạn.";
      stopNotificationPolling();
      return;
    }
    notificationError.value = "Không thể tải thông báo.";
    console.error('Failed to fetch notifications:', error);
  } finally {
    notificationLoading.value = false;
    pollingInFlight.value = false;
  }
};

const markNotificationAsRead = async (item) => {
  if (!item || item.isRead) {
    return;
  }
  try {
    await notificationClientApi.markAsRead(item.id);
    item.isRead = true;
    unreadCount.value = Math.max(0, unreadCount.value - 1);
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || 'Không thể đánh dấu thông báo đã đọc');
    console.error('Failed to mark notification as read:', error);
  }
};

const markAllNotificationsAsRead = async () => {
  try {
    await notificationClientApi.markAllAsRead();
    notifications.value = notifications.value.map((item) => ({ ...item, isRead: true }));
    unreadCount.value = 0;
    ElMessage.success('Đã đánh dấu tất cả thông báo là đã đọc');
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || 'Không thể đánh dấu tất cả thông báo đã đọc');
    console.error('Failed to mark all notifications as read:', error);
  }
};

const startNotificationPolling = () => {
  stopNotificationPolling();
  if (!authStore.isAuthenticated) {
    return;
  }
  fetchNotifications();
  notificationInterval = window.setInterval(fetchNotifications, 15000);
};

const stopNotificationPolling = () => {
  if (notificationInterval) {
    clearInterval(notificationInterval);
    notificationInterval = null;
  }
};

const handleLogout = async () => {
  try {
    // Show confirmation dialog
    await confirm({
      title: 'Xác nhận đăng xuất',
      message: 'Bạn có chắc chắn muốn đăng xuất?',
      confirmButtonText: 'Có',
      cancelButtonText: 'Không',
      onConfirm: async () => {
        // Clear auth store
        authStore.logout();

        // Clear localStorage
        localStorage.removeItem('auth');
        localStorage.removeItem('clothing_auth');

        // Clear cart store
        cartStore.clear();

        // Clear wishlist
        wishlistStore.clear();

        // Stop notification polling
        stopNotificationPolling();

        // Redirect to home
        router.push('/');
        ElMessage.success('Đã đăng xuất');
      }
    });
  } catch (error) {
    // User cancelled the dialog, do nothing
    if (error.message !== 'cancel') {
      console.error('Logout failed:', error);
    }
  }
};

onMounted(async () => {
  try {
    await fetchAllCategories();
    if (authStore.isAuthenticated) {
      // Đầu tiên đồng bộ giỏ hàng khách lên server
      await cartStore.syncLocalCart();
      // Sau đó fetch giỏ hàng cuối cùng từ server
      await cartStore.fetchCart();
      await wishlistStore.syncLocalWishlist();
      startNotificationPolling();
    } else {
      await wishlistStore.ensureLoaded();
      stopNotificationPolling();
    }
  } catch (error) {
    console.error('Failed to fetch initial data:', error);
  }
});

watch(
  () => authStore.isAuthenticated,
  (isAuthenticated) => {
    if (isAuthenticated) {
      startNotificationPolling();
      return;
    }
    stopNotificationPolling();
    notifications.value = [];
    unreadCount.value = 0;
  }
);

onBeforeUnmount(() => {
  stopNotificationPolling();
});
</script>

<style scoped lang="scss">
.app-header {
  border-bottom: 1px solid #e5e5e5;
  position: relative;
}

.promo-top {
  height: 36px;
  background: #000;
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  cursor: pointer;
  transition: opacity 0.2s;

  &:hover {
    opacity: 0.9;
  }
}

.promo-panel {
  background: #fff;
  border-bottom: 1px solid #e5e5e5;
  overflow: hidden;
  position: relative;
  z-index: 100;
}

.promo-panel-inner {
  position: relative;
  padding: 40px 60px;
  max-width: 1000px;
  margin: 0 auto;
}

.promo-close {
  position: absolute;
  top: 15px;
  right: 15px;
  cursor: pointer;
  border: 1px solid #000;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;

  &:hover {
    background: #000;
    color: #fff;
  }
}

.promo-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 80px;
}

.promo-col {
  h4 {
    font-size: 16px;
    font-weight: 800;
    margin-bottom: 15px;
    text-transform: uppercase;
  }

  p {
    font-size: 13px;
    line-height: 1.6;
    margin-bottom: 20px;
    color: #333;
  }

  .promo-link {
    font-size: 13px;
    font-weight: 700;
    color: #000;
    text-decoration: underline;
    text-transform: uppercase;
    text-underline-offset: 4px;

    &:hover {
      color: #666;
    }
  }
}

.main-nav {
  display: flex;
  align-items: center;
  padding: 0 20px;
  height: 72px;
  border-bottom: none !important;
}

.flex-grow {
  flex-grow: 1;
}

.logo-item {
  opacity: 1 !important;
  padding: 0 !important;
  &:hover {
    background-color: transparent !important;
  }
}

.logo-text {
  font-size: 24px;
  font-weight: 900;
  letter-spacing: 2px;
  color: #000;
  text-decoration: none;
  font-family: 'Inter', sans-serif;
}

.menu-label {
  font-weight: 700;
  letter-spacing: 1px;
}

:deep(.mega-menu-popper) {
  width: 100vw;
  left: 0 !important;
}

.mega-menu-content {
  display: flex;
  gap: 30px;
  padding: 30px 40px;
  background: #fff;
  justify-content: center;
}

.mega-column {
  display: flex;
  flex-direction: column;
  min-width: 150px;

  .menu-heading {
    margin: 0 0 10px;
    font-size: 14px;

    a {
      color: #000;
      text-decoration: none;
    }
  }

  .el-menu-item {
    height: 30px;
    line-height: 30px;
    padding: 0;

    a {
      color: #555;
      text-decoration: none;
      width: 100%;

      &:hover {
        text-decoration: underline;
      }
    }
  }
}

.tools-container {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;

  .utility-links {
    display: flex;
    gap: 15px;
    font-size: 11px;

    a {
      color: #666;
      text-decoration: none;

      &:hover {
        text-decoration: underline;
      }
    }
  }

  .actions {
    display: flex;
    align-items: center;
    gap: 20px;

    .search-wrapper {
      position: relative;
    }

    .search-input {
      width: 200px;
      :deep(.el-input__wrapper) {
        border-radius: 0;
        background-color: #f5f5f5;
        box-shadow: none;
        &:hover { box-shadow: 0 0 0 1px #ccc inset; }
        &.is-focus { box-shadow: 0 0 0 1px #000 inset; background: #fff; }
      }
    }

    .action-icon {
      font-size: 20px;
      cursor: pointer;
    }

    .icon-btn {
      border: none;
      background: transparent;
      padding: 0;
      line-height: 1;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      color: inherit;
    }

    .cart-badge {
      margin-right: 12px;
    }

    .wishlist-badge {
      margin-right: 2px;
    }
  }
}

/* SEARCH OVERLAY */
.search-overlay {
  position: absolute;
  top: calc(100% + 15px);
  right: 0;
  width: 700px;
  background: #fff;
  box-shadow: 0 10px 40px rgba(0,0,0,0.15);
  z-index: 1000;
  border-top: 1px solid #000;
  padding: 30px;
}

.search-overlay-content {
  display: grid;
  grid-template-columns: 240px 1fr;
  gap: 40px;
}

.overlay-title {
  font-size: 16px;
  font-weight: 800;
  text-transform: uppercase;
  margin-bottom: 25px;
  letter-spacing: 0.5px;
}

.suggestion-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.suggestion-item {
  padding: 10px 0;
  display: flex;
  justify-content: space-between;
  cursor: pointer;
  border-bottom: 1px solid #f5f5f5;
  transition: all 0.2s;

  &:hover {
    .suggestion-term { text-decoration: underline; }
  }

  .suggestion-term {
    font-size: 14px;
    font-weight: 700;
  }

  .suggestion-count {
    font-size: 12px;
    color: #888;
  }
}

.view-all-link {
  margin-top: 25px;
  font-size: 14px;
  font-weight: 800;
  text-decoration: underline;
  cursor: pointer;
}

.search-product-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.search-product-item {
  display: flex;
  gap: 20px;
  padding: 10px;
  cursor: pointer;
  transition: background 0.2s;

  &:hover {
    background: #f9f9f9;
  }

  .search-prod-img {
    width: 80px;
    height: 100px;
    background: #f5f5f5;

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
  }

  .search-prod-info {
    flex: 1;

    .search-prod-brand {
      font-size: 11px;
      color: #666;
      text-transform: uppercase;
      margin-bottom: 4px;
    }

    .search-prod-name {
      font-size: 13px;
      font-weight: 700;
      margin-bottom: 6px;
      line-height: 1.3;
    }

    .search-prod-price {
      font-size: 14px;
      font-weight: 800;
    }
  }
}

.fade-enter-active, .fade-leave-active {
  transition: opacity 0.2s, transform 0.2s;
}
.fade-enter-from, .fade-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

.cart-link {
  text-decoration: none;
  color: inherit;
  display: flex;
  align-items: center;
}

.notification-popover {
  .notification-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 8px;
  }

  .notification-list {
    max-height: 340px;
    overflow: auto;
    display: grid;
    gap: 8px;
  }

  .notification-item {
    width: 100%;
    text-align: left;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
    padding: 10px;
    background: #fff;
    cursor: pointer;
  }

  .notification-item.unread {
    border-color: #111;
    background: #f7f8fa;
  }

  .noti-title {
    margin: 0 0 4px;
    font-size: 13px;
    font-weight: 700;
  }

  .noti-content {
    margin: 0 0 4px;
    font-size: 12px;
    color: #4b5563;
    line-height: 1.35;
  }

  .noti-time {
    font-size: 11px;
    color: #9ca3af;
  }

  .notification-empty {
    margin: 10px 0;
    color: #6b7280;
    font-size: 13px;
  }
}

@media (max-width: 1200px) {
  .main-nav {
    padding: 0 12px;
  }

  .tools-container {
    .utility-links {
      display: none;
    }

    .actions {
      gap: 14px;

      .search-input {
        width: 170px;
      }
    }
  }
}

@media (max-width: 992px) {
  .promo-top {
    font-size: 10px;
    text-align: center;
    padding: 0 10px;
    line-height: 1.3;
    height: auto;
    min-height: 36px;
  }

  .promo-panel-inner {
    padding: 24px 16px;
  }

  .promo-grid {
    grid-template-columns: 1fr;
    gap: 28px;
  }

  .main-nav {
    height: auto;
    min-height: 72px;
  }

  .logo-text {
    font-size: 20px;
  }

  .menu-label {
    font-size: 12px;
  }

  .tools-container .actions .search-input {
    width: 140px;
  }

  .search-overlay {
    width: min(700px, calc(100vw - 24px));
    right: -8px;
    padding: 18px;
  }

  .search-overlay-content {
    grid-template-columns: 1fr;
    gap: 20px;
  }
}

@media (max-width: 768px) {
  .main-nav {
    :deep(.el-menu-item),
    :deep(.el-sub-menu__title) {
      padding: 0 8px !important;
      font-size: 12px;
    }
  }

  .logo-item {
    margin-right: 2px;
  }

  .menu-label {
    letter-spacing: 0.3px;
    font-size: 11px;
  }

  .tools-container .actions {
    gap: 10px;

    .search-input {
      width: 120px;
    }

    .action-icon {
      font-size: 18px;
    }
  }

  .search-overlay {
    top: calc(100% + 10px);
    right: -2px;
    padding: 14px;
  }
}

@media (max-width: 576px) {
  .main-nav {
    padding: 0 8px;

    :deep(.el-menu-item:not(.logo-item)),
    :deep(.el-sub-menu) {
      display: none !important;
    }
  }

  .tools-container .actions {
    .search-input {
      width: 108px;
    }
  }

  .promo-col {
    h4 {
      font-size: 14px;
    }

    p {
      font-size: 12px;
    }
  }
}
</style>

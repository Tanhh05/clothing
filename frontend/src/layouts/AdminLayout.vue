<template>
  <div class="admin-layout" :class="{ collapsed: sidebarCollapsed }">
    <aside class="sidebar">
      <p class="palette-label">ADMIN TWENTY</p>

      <nav class="menu">
        <RouterLink :to="dashboardItem.to" class="menu-item" :class="{ active: isActive(dashboardItem.to) }">
          <el-icon class="item-icon"><component :is="dashboardItem.icon" /></el-icon>
          <span class="item-label">{{ dashboardItem.label }}</span>
        </RouterLink>

        <div
          v-for="group in menuGroups"
          :key="group.key"
          class="menu-group"
          :class="{ open: openGroupKey === group.key, active: isGroupActive(group) }"
        >
          <button type="button" class="menu-group-trigger" @click="toggleGroup(group.key)">
            <el-icon class="item-icon"><component :is="group.icon" /></el-icon>
            <span class="item-label">{{ group.label }}</span>
            <el-icon class="arrow-icon"><ArrowDown /></el-icon>
          </button>
          <div v-show="!sidebarCollapsed && openGroupKey === group.key" class="menu-submenu">
            <RouterLink
              v-for="item in group.items"
              :key="item.to"
              :to="item.to"
              class="menu-subitem"
              :class="{ active: isActive(item.to) }"
            >
              {{ item.label }}
            </RouterLink>
          </div>
        </div>
      </nav>
    </aside>

    <section class="content-wrap">
      <header class="topbar">
        <div class="topbar-left">
          <el-button class="collapse-btn" circle @click="toggleSidebar">
            <el-icon><Expand /></el-icon>
          </el-button>
          <h1 class="title">Overview</h1>
        </div>

        <div class="topbar-actions">
          <span class="welcome-text">Welcome: {{ adminDisplayName }}</span>
          <div class="search-box" ref="searchBoxRef">
            <el-input
              v-model="searchQuery"
              placeholder="Search"
              clearable
              class="global-search-input"
              @focus="handleSearchFocus"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>

            <div v-if="showSearchDropdown" class="search-dropdown">
              <div class="search-dropdown-head">
                <span class="search-title">Kết quả cho "{{ searchQuery.trim() }}"</span>
                <span class="search-total">{{ totalSearchResult }} mục</span>
              </div>
              <div v-if="searchLoading" class="search-state">Đang tìm...</div>
              <div v-else-if="!hasAnySearchResult" class="search-state">Không có kết quả</div>
              <template v-else>
                <div v-if="searchResults.products.length" class="search-group products">
                  <p class="group-title">Products</p>
                  <button
                    v-for="item in searchResults.products"
                    :key="`p-${item.id}`"
                    class="search-item"
                    @click="goToProduct(item)"
                  >
                    <span class="item-main">{{ item.name }}</span>
                    <span class="item-meta">#{{ item.id }} • {{ item.slug || "N/A" }}</span>
                    <span class="item-tag">Product</span>
                  </button>
                </div>

                <div v-if="searchResults.categories.length" class="search-group categories">
                  <p class="group-title">Categories</p>
                  <button
                    v-for="item in searchResults.categories"
                    :key="`c-${item.id}`"
                    class="search-item"
                    @click="goToCategory(item)"
                  >
                    <span class="item-main">{{ item.name }}</span>
                    <span class="item-meta">#{{ item.id }} • {{ item.slug || "N/A" }}</span>
                    <span class="item-tag">Category</span>
                  </button>
                </div>

                <div v-if="searchResults.orders.length" class="search-group orders">
                  <p class="group-title">Orders</p>
                  <button
                    v-for="item in searchResults.orders"
                    :key="`o-${item.id}`"
                    class="search-item"
                    @click="goToOrder(item)"
                  >
                    <span class="item-main">Đơn #{{ item.id }} • {{ item.customerName || `User #${item.userId || "N/A"}` }}</span>
                    <span class="item-meta">{{ item.status || "N/A" }} • {{ item.address || "N/A" }}</span>
                    <span class="item-tag">Order</span>
                  </button>
                </div>
              </template>
            </div>
          </div>
          <button type="button" class="action-btn" @click="openNotificationPanel" aria-label="Thông báo">
            <el-icon><Bell /></el-icon>
          </button>
          <button type="button" class="action-btn" @click="openAdminPanel" aria-label="Tài khoản quản trị">
            <el-icon><User /></el-icon>
          </button>
        </div>
      </header>

      <main class="content">
        <RouterView />
      </main>

      <el-drawer
        v-model="adminPanelVisible"
        direction="rtl"
        size="360px"
        :with-header="false"
        class="admin-profile-drawer"
      >
        <div class="admin-panel" v-loading="adminProfileLoading">
          <el-card shadow="never" class="info-card">
            <template #header>
              <strong>Thông tin tài khoản admin</strong>
            </template>
            <el-descriptions :column="1" border size="small" class="admin-descriptions">
              <el-descriptions-item label="Username">
                {{ adminDisplayName }}
              </el-descriptions-item>
              <el-descriptions-item label="User ID">
                #{{ adminUserIdText }}
              </el-descriptions-item>
              <el-descriptions-item label="Email">
                {{ adminEmailText }}
              </el-descriptions-item>
              <el-descriptions-item label="Vai trò">
                <div class="role-tags">
                  <el-tag v-for="role in adminRoleList" :key="role" size="small" type="success">{{ role }}</el-tag>
                </div>
              </el-descriptions-item>
            </el-descriptions>
          </el-card>

          <div class="panel-actions">
            <el-button type="danger" plain class="logout-btn" @click="handleLogout">Đăng xuất</el-button>
          </div>
        </div>
      </el-drawer>
    </section>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import {
  Search,
  Bell,
  User,
  ArrowDown,
  House,
  Box,
  ShoppingCart,
  Tickets,
  CollectionTag,
  UserFilled,
  PictureFilled,
  Expand,
  Discount,
  Van,
  RefreshLeft,
  ChatDotRound,
  Setting
} from "@element-plus/icons-vue";
import { ElMessage } from "element-plus";
import { useAuthStore } from "@/store/authStore";
import { searchApi } from "@/modules/search/api/searchApi";
import { categoryApi } from "@/modules/category/api/categoryApi";
import { orderApi } from "@/modules/order/api/orderApi";

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const sidebarCollapsed = ref(false);
const adminPanelVisible = ref(false);
const adminProfileLoading = ref(false);
const searchBoxRef = ref(null);
const searchQuery = ref("");
const searchLoading = ref(false);
const searchOpen = ref(false);
const searchResults = ref({
  products: [],
  categories: [],
  orders: []
});
let searchTimer = null;

const normalizeRoles = (rawRoles) => {
  const roles = Array.isArray(rawRoles) ? rawRoles : rawRoles ? [rawRoles] : [];
  return roles
    .map((role) => String(role || "").toUpperCase().replace(/^ROLE_/, ""))
    .filter(Boolean);
};

const adminRoleList = computed(() => {
  const roles = normalizeRoles(authStore.adminRoles);
  return roles.length ? roles : ["ADMIN"];
});

const adminDisplayName = computed(() => {
  const profile = authStore.adminProfile || {};
  return (
    profile.fullName ||
    profile.name ||
    profile.username ||
    authStore.adminUsername ||
    "Admin"
  );
});

const adminUserIdText = computed(() => {
  const profile = authStore.adminProfile || {};
  return profile.id || authStore.adminUserId || "N/A";
});

const adminEmailText = computed(() => {
  const profile = authStore.adminProfile || {};
  return profile.email || "N/A";
});

const dashboardItem = { label: "Doanh thu", to: "/admin", icon: House };
const menuGroups = [
  {
    key: "catalog",
    label: "Sản phẩm",
    icon: Box,
    items: [
      { label: "Sản phẩm", to: "/admin/products" },
      { label: "Danh mục", to: "/admin/categories" },
      { label: "Banner", to: "/admin/banners" }
    ]
  },
  {
    key: "sales",
    label: "Bán hàng",
    icon: Tickets,
    items: [
      { label: "Đơn hàng", to: "/admin/orders" },
      { label: "Bán tại quầy", to: "/admin/pos" },
      { label: "Voucher", to: "/admin/vouchers" },
      { label: "Đổi trả", to: "/admin/returns" }
    ]
  },
  {
    key: "operation",
    label: "Vận hành",
    icon: Van,
    items: [
      { label: "Nhập kho", to: "/admin/warehouse-inbound" },
      { label: "Khách hàng", to: "/admin/customers" },
      { label: "Thông báo", to: "/admin/notifications" },
      { label: "Cài đặt", to: "/admin/settings" }
    ]
  }
];
const openGroupKey = ref("");

const toggleSidebar = () => {
  sidebarCollapsed.value = !sidebarCollapsed.value;
};

const isActive = (to) => {
  if (to === "/admin") return route.path === "/admin";
  return route.path.startsWith(to);
};

const isGroupActive = (group) => group.items.some((item) => isActive(item.to));

const findGroupByPath = (path) => {
  for (const group of menuGroups) {
    if (group.items.some((item) => path.startsWith(item.to))) {
      return group.key;
    }
  }
  return "";
};

const toggleGroup = (groupKey) => {
  if (sidebarCollapsed.value) {
    sidebarCollapsed.value = false;
    openGroupKey.value = groupKey;
    return;
  }
  openGroupKey.value = openGroupKey.value === groupKey ? "" : groupKey;
};

const hasAnySearchResult = computed(() => {
  return (
    searchResults.value.products.length > 0 ||
    searchResults.value.categories.length > 0 ||
    searchResults.value.orders.length > 0
  );
});

const showSearchDropdown = computed(() => {
  return searchOpen.value && searchQuery.value.trim().length >= 2;
});

const totalSearchResult = computed(() => {
  return (
    searchResults.value.products.length +
    searchResults.value.categories.length +
    searchResults.value.orders.length
  );
});

const runGlobalSearch = async (query) => {
  const q = query.trim();
  if (q.length < 2) {
    searchResults.value = { products: [], categories: [], orders: [] };
    return;
  }

  searchLoading.value = true;
  try {
    const [productRes, categoryRes, orderRes] = await Promise.all([
      searchApi.searchProducts(q, 6),
      categoryApi.getCategories({ page: 0, size: 100, sortBy: "id", direction: "desc" }),
      orderApi.getAllOrders()
    ]);

    const products = Array.isArray(productRes?.data) ? productRes.data.slice(0, 6) : [];

    const allCategories = Array.isArray(categoryRes?.data?.content) ? categoryRes.data.content : [];
    const categories = allCategories
      .filter((item) => {
        const name = String(item?.name || "").toLowerCase();
        const slug = String(item?.slug || "").toLowerCase();
        const text = q.toLowerCase();
        return name.includes(text) || slug.includes(text);
      })
      .slice(0, 6);

    const allOrders = Array.isArray(orderRes?.data) ? orderRes.data : [];
    const orders = allOrders
      .filter((item) => {
        const id = String(item?.id || "");
        const customer = String(item?.customerName || "").toLowerCase();
        const address = String(item?.address || "").toLowerCase();
        const text = q.toLowerCase();
        return id.includes(text) || customer.includes(text) || address.includes(text);
      })
      .slice(0, 6);

    searchResults.value = { products, categories, orders };
  } catch (error) {
    console.error(error);
    searchResults.value = { products: [], categories: [], orders: [] };
  } finally {
    searchLoading.value = false;
  }
};

watch(searchQuery, (value) => {
  if (searchTimer) clearTimeout(searchTimer);
  searchTimer = setTimeout(() => {
    runGlobalSearch(value);
  }, 300);
});

watch(
  () => route.path,
  (path) => {
    const matchedGroup = findGroupByPath(path);
    if (matchedGroup) {
      openGroupKey.value = matchedGroup;
    }
  },
  { immediate: true }
);

const closeSearch = () => {
  searchOpen.value = false;
};

const handleSearchFocus = () => {
  searchOpen.value = true;
};

const handleClickOutside = (event) => {
  if (!searchBoxRef.value) return;
  if (searchBoxRef.value.contains(event.target)) return;
  closeSearch();
};

const goToProduct = (product) => {
  closeSearch();
  router.push("/admin/products");
};

const goToCategory = (category) => {
  closeSearch();
  router.push("/admin/categories");
};

const goToOrder = (order) => {
  closeSearch();
  router.push("/admin/orders");
};

const openAdminPanel = async () => {
  adminPanelVisible.value = true;
  adminProfileLoading.value = true;
  try {
    await authStore.fetchAdminProfile();
  } catch (error) {
    console.error(error);
  } finally {
    adminProfileLoading.value = false;
  }
};

const openNotificationPanel = () => {
  ElMessage.info("Khu vực thông báo admin đang được cập nhật");
};

const handleLogout = () => {
  adminPanelVisible.value = false;
  authStore.clearAdminAuth();
  ElMessage.success("Đăng xuất thành công");
  router.push("/admin/login");
};

onMounted(() => {
  document.addEventListener("click", handleClickOutside);
});

onBeforeUnmount(() => {
  document.removeEventListener("click", handleClickOutside);
  if (searchTimer) clearTimeout(searchTimer);
});
</script>

<style scoped lang="scss">
.admin-layout {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 270px 1fr;
  background: #f7f9fc;
  color: #111827;
  transition: grid-template-columns 0.2s ease;
}

.admin-layout.collapsed {
  grid-template-columns: 88px 1fr;
}

.sidebar {
  background: #fff;
  border-right: 1px solid #e9edf3;
  padding: 22px 16px;
}

.palette-label {
  margin: 0 0 14px;
  text-align: center;
  font-size: 16px;
  color: #7b8794;
  font-weight: 500;
}

.menu {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 8px;
  border: 1px solid transparent;
  color: #343c46;
  font-size: 15px;
  font-weight: 500;
  text-decoration: none;
  transition: all 0.2s ease;

  .item-icon {
    font-size: 17px;
    color: #8e97a3;
  }

  .item-label {
    flex: 1;
    min-width: 0;
  }

  .arrow-icon {
    color: #a1a9b4;
    font-size: 14px;
  }

  &:hover {
    border-color: #e5eaf1;
    background: #f7faff;
  }

  &.active {
    color: #4d9dff;

    .item-icon {
      color: #4d9dff;
    }
  }

  &.disabled {
    opacity: 0.35;
    cursor: not-allowed;
  }
}

.menu-group {
  border-radius: 8px;
}

.menu-group-trigger {
  width: 100%;
  border: 1px solid transparent;
  background: transparent;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 8px;
  color: #343c46;
  font-size: 15px;
  font-weight: 500;
  text-align: left;
  cursor: pointer;
  transition: all 0.2s ease;

  .item-icon {
    font-size: 17px;
    color: #8e97a3;
  }

  .item-label {
    flex: 1;
    min-width: 0;
  }

  .arrow-icon {
    color: #a1a9b4;
    font-size: 14px;
    transition: transform 0.2s ease;
  }

  &:hover {
    border-color: #e5eaf1;
    background: #f7faff;
  }
}

.menu-group.open .menu-group-trigger .arrow-icon {
  transform: rotate(180deg);
}

.menu-group.active .menu-group-trigger {
  color: #4d9dff;

  .item-icon {
    color: #4d9dff;
  }
}

.menu-submenu {
  margin-top: 6px;
  margin-left: 10px;
  padding-left: 14px;
  border-left: 1px solid #e5eaf1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.menu-subitem {
  border-radius: 8px;
  padding: 8px 10px;
  color: #4b5563;
  font-size: 14px;
  font-weight: 500;
  text-decoration: none;
  transition: all 0.2s ease;

  &:hover {
    background: #f7faff;
  }

  &.active {
    color: #4d9dff;
    background: #f1f7ff;
  }
}

.admin-layout.collapsed {
  .palette-label {
    font-size: 10px;
    margin-bottom: 10px;
    letter-spacing: 0.5px;
  }

  .menu-item {
    justify-content: center;
    padding: 12px 8px;

    .item-label,
    .arrow-icon {
      display: none;
    }
  }

  .menu-group-trigger {
    justify-content: center;
    padding: 12px 8px;

    .item-label,
    .arrow-icon {
      display: none;
    }
  }
}

.content-wrap {
  padding: 22px 26px;
  min-width: 0;
}

.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;

  .title {
    margin: 0;
    font-size: 24px;
    font-weight: 800;
  }
}

.topbar-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.collapse-btn {
  border-color: #d2d8e0;
}

.topbar-actions {
  display: flex;
  align-items: center;
  gap: 20px;
  flex-wrap: wrap;
  justify-content: flex-end;

  .search-box {
    width: 220px;
    position: relative;

    :deep(.global-search-input .el-input__wrapper) {
      border-radius: 0;
      background-color: #f5f5f5;
      box-shadow: none;
      border: 0;
      padding: 8px 10px;
      transition: box-shadow 0.2s ease, background-color 0.2s ease;

      &:hover {
        box-shadow: 0 0 0 1px #ccc inset;
      }

      &.is-focus {
        box-shadow: 0 0 0 1px #000 inset;
        background: #fff;
      }
    }

    :deep(.global-search-input .el-input__inner) {
      font-size: 14px;
    }

    .search-dropdown {
      position: absolute;
      top: calc(100% + 6px);
      left: 0;
      right: 0;
      background: #fff;
      border: 1px solid #d3dae5;
      border-radius: 12px;
      box-shadow: 0 14px 38px rgba(15, 23, 42, 0.15);
      padding: 10px;
      max-height: 420px;
      overflow-y: auto;
      z-index: 60;
    }

    .search-dropdown-head {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 8px;
      padding: 2px 2px 8px;
      border-bottom: 1px solid #eef2f7;
    }

    .search-title {
      font-size: 12px;
      color: #4b5563;
      max-width: 230px;
      white-space: nowrap;
      text-overflow: ellipsis;
      overflow: hidden;
    }

    .search-total {
      font-size: 11px;
      font-weight: 700;
      color: #1f2937;
      background: #eef2f7;
      border-radius: 999px;
      padding: 2px 8px;
    }

    .search-state {
      font-size: 12px;
      color: #6b7280;
      padding: 10px 8px;
    }

    .search-group {
      padding: 8px;
      border-radius: 10px;
      border: 1px solid #edf1f7;
      background: #fbfcfe;
    }

    .search-group.products .group-title::before {
      background: #4d9dff;
    }

    .search-group.categories .group-title::before {
      background: #10b981;
    }

    .search-group.orders .group-title::before {
      background: #f59e0b;
    }

    .search-group + .search-group {
      margin-top: 8px;
    }

    .group-title {
      margin: 0 0 8px;
      color: #6b7280;
      font-size: 11px;
      text-transform: uppercase;
      letter-spacing: 0.5px;
      font-weight: 700;
      display: flex;
      align-items: center;
      gap: 6px;

      &::before {
        content: "";
        width: 7px;
        height: 7px;
        border-radius: 50%;
        background: #9ca3af;
      }
    }

    .search-item {
      width: 100%;
      border: 0;
      background: #fff;
      text-align: left;
      padding: 9px;
      border-radius: 8px;
      cursor: pointer;
      display: grid;
      grid-template-columns: 1fr auto;
      row-gap: 2px;
      column-gap: 8px;
      border: 1px solid #e9eef5;

      &:hover {
        background: #f4f8ff;
        border-color: #d5e4f8;
      }
    }

    .search-item + .search-item {
      margin-top: 6px;
    }

    .item-main {
      color: #111827;
      font-size: 13px;
      font-weight: 600;
      line-height: 1.3;
      grid-column: 1 / 2;
    }

    .item-meta {
      color: #6b7280;
      font-size: 11px;
      line-height: 1.3;
      grid-column: 1 / 2;
      overflow: hidden;
      white-space: nowrap;
      text-overflow: ellipsis;
    }

    .item-tag {
      grid-column: 2 / 3;
      grid-row: 1 / 3;
      align-self: center;
      font-size: 10px;
      font-weight: 700;
      text-transform: uppercase;
      color: #4b5563;
      background: #edf2f9;
      border-radius: 999px;
      padding: 2px 8px;
    }
  }

  .action-btn {
    border: none;
    background: transparent;
    padding: 0;
    line-height: 1;
    width: 20px;
    height: 20px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    color: #111;
    font-size: 20px;
    cursor: pointer;
    transition: color 0.2s ease;

    &:hover {
      color: #000;
    }
  }

  .welcome-text {
    font-size: 13px;
    color: #4b5563;
    white-space: nowrap;
  }
}

.admin-profile-drawer {
  :deep(.el-drawer__body) {
    padding: 0;
  }
}

.admin-panel {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 16px;
  gap: 14px;
  background: #f8fafc;
}

.info-card {
  :deep(.el-card__header) {
    padding: 12px 14px;
  }

  :deep(.el-card__body) {
    padding: 12px;
  }

  :deep(.el-descriptions__label) {
    width: 96px;
    font-weight: 600;
  }

  :deep(.el-descriptions__content) {
    word-break: break-word;
  }
}

.role-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.panel-actions {
  margin-top: auto;
}

.logout-btn {
  width: 100%;
}

.content {
  background: #fff;
  border: 1px solid #dce1e7;
  padding: 20px;
  min-width: 0;
  overflow-x: auto;
  overflow-y: visible;
}

@media (max-width: 900px) {
  .admin-layout {
    grid-template-columns: 1fr;
  }

  .sidebar {
    border-right: 0;
    border-bottom: 1px solid #dce1e7;
  }

  .admin-layout.collapsed {
    grid-template-columns: 1fr;
  }

  .admin-layout.collapsed .menu-item {
    justify-content: flex-start;
    padding: 12px 14px;

    .item-label {
      display: inline;
    }
  }

  .admin-layout.collapsed .menu-group-trigger {
    justify-content: flex-start;
    padding: 12px 14px;

    .item-label,
    .arrow-icon {
      display: inline;
    }
  }

  .topbar {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }

  .topbar-actions .search-box {
    width: 100%;
  }

  .topbar-actions {
    width: 100%;
    justify-content: flex-start;
  }

  .content-wrap {
    padding: 14px;
  }

  .content {
    padding: 12px;
  }
}

@media (max-width: 700px) {
  .topbar-actions {
    gap: 8px;
  }

  .topbar-actions .search-box {
    width: 180px;
  }

  .topbar-actions .action-btn {
    width: 18px;
    height: 18px;
    font-size: 18px;
  }

  .topbar-actions .welcome-text {
    display: none;
  }

  .admin-profile-drawer {
    :deep(.el-drawer) {
      width: 100% !important;
      max-width: 100% !important;
    }
  }
}
</style>

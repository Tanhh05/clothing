import { createRouter, createWebHistory } from "vue-router";
import { applyGuards } from "./guards";
import ClientLayout from "@/layouts/ClientLayout.vue";
import AdminLayout from "@/layouts/AdminLayout.vue";
import AuthLayout from "@/layouts/AuthLayout.vue";

const routes = [
  {
    path: "/",
    component: ClientLayout,
    children: [
      {
        path: "",
        component: () => import("@/modules/home/pages/HomePage.vue")
      },
      {
        path: "products",
        component: () => import("@/modules/product/pages/client/ProductListPage.vue")
      },
      {
        path: "products/:slug",
        component: () => import("@/modules/product/pages/client/ProductDetailPage.vue")
      },
      {
        path: "cart",
        component: () => import("@/modules/cart/pages/CartPage.vue")
      },
      {
        path: "search",
        component: () => import("@/modules/search/pages/SearchPage.vue")
      },
      {
        path: "orders",
        component: () => import("@/modules/order/pages/client/OrderListPage.vue"),
        meta: { requiresAuth: true }
      },
      {
        path: "wishlist",
        component: () => import("@/modules/wishlist/pages/WishlistPage.vue"),
        meta: { requiresAuth: true }
      },
      {
        path: "notifications",
        component: () => import("@/modules/notification/pages/client/NotificationPage.vue"),
        meta: { requiresAuth: true }
      },
      {
        path: "account",
        component: () => import("@/modules/auth/pages/AccountPage.vue"),
        meta: { requiresAuth: true }
      }
    ]
  },
  {
    path: "/auth",
    component: AuthLayout,
    children: [
      {
        path: "login",
        name: "login",
        component: () => import("@/modules/auth/pages/LoginPage.vue"),
        meta: { guestOnly: true }
      },
      {
        path: "register",
        component: () => import("@/modules/auth/pages/RegisterPage.vue"),
        meta: { guestOnly: true }
      }
    ]
  },
  {
    path: "/admin/login",
    component: () => import("@/modules/auth/pages/AdminLoginPage.vue")
  },
  {
    path: "/admin",
    component: AdminLayout,
    meta: { requiresAuth: true, roles: ["ADMIN"] },
    children: [
      {
        path: "",
        component: () => import("@/modules/dashboard/pages/AdminDashboardPage.vue")
      },
      {
        path: "products",
        component: () => import("@/modules/product/pages/admin/ProductAdminPage.vue")
      },
      {
        path: "categories",
        component: () => import("@/modules/category/pages/admin/CategoryAdminPage.vue")
      },
      {
        path: "banners",
        component: () => import("@/modules/banner/pages/admin/BannerAdminPage.vue")
      },
      {
        path: "customers",
        component: () => import("@/modules/customer/pages/admin/CustomerAdminPage.vue")
      },
      {
        path: "orders",
        component: () => import("@/modules/order/pages/admin/OrderAdminPage.vue")
      },
      {
        path: "vouchers",
        component: () => import("@/modules/voucher/pages/admin/VoucherAdminPage.vue")
      },
      {
        path: "warehouse-inbound",
        alias: ["warehouse-inbounds"],
        component: () => import("@/modules/warehouse/pages/admin/WarehouseInboundPage.vue")
      },
      {
        path: "returns",
        component: () => import("@/modules/returns/pages/admin/ReturnRefundAdminPage.vue")
      },
      {
        path: "notifications",
        component: () => import("@/modules/notification/pages/admin/NotificationAdminPage.vue")
      },
      {
        path: "settings",
        component: () => import("@/modules/settings/pages/admin/StoreSettingsPage.vue")
      }
    ]
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

applyGuards(router);

export default router;

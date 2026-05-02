import { createRouter, createWebHistory } from "vue-router";
import { applyGuards } from "./guards";

const routes = [
  {
    path: "/",
    component: () => import("@/layouts/ClientLayout.vue"),
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
        path: "products/category/:categorySlug",
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
        path: "checkout",
        component: () => import("@/modules/cart/pages/CheckoutPage.vue"),
        meta: { requiresAuth: true }
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
        path: "account",
        component: () => import("@/modules/auth/pages/AccountPage.vue"),
        meta: { requiresAuth: true }
      }
    ]
  },
  {
    path: "/auth",
    component: () => import("@/layouts/AuthLayout.vue"),
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
    path: "/system/connection-error",
    component: () => import("@/modules/system/pages/ConnectionErrorPage.vue")
  },
  {
    path: "/system/forbidden",
    component: () => import("@/modules/system/pages/ForbiddenPage.vue")
  },
  {
    path: "/system/not-found",
    component: () => import("@/modules/system/pages/ApiNotFoundPage.vue")
  },
  {
    path: "/:pathMatch(.*)*",
    component: () => import("@/modules/system/pages/NotFoundPage.vue")
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

applyGuards(router);

export default router;

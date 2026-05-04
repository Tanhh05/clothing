import Vue from 'vue'
import Router from 'vue-router'

Vue.use(Router)

/* Layout */
import Layout from '@/layout'
/**
 * Note: sub-menu only appear when route children.length >= 1
 * Detail see: https://panjiachen.github.io/vue-element-admin-site/guide/essentials/router-and-nav.html
 *
 * hidden: true                   if set true, item will not show in the sidebar(default is false)
 * alwaysShow: true               if set true, will always show the root menu
 *                                if not set alwaysShow, when item has more than one children route,
 *                                it will becomes nested mode, otherwise not show the root menu
 * redirect: noRedirect           if set noRedirect will no redirect in the breadcrumb
 * name:'router-name'             the name is used by <keep-alive> (must set!!!)
 * meta : {
    roles: ['admin','editor']    control the page roles (you can set multiple roles)
    title: 'title'               the name show in sidebar and breadcrumb (recommend set)
    icon: 'svg-name'/'el-icon-x' the icon show in the sidebar
    noCache: true                if set true, the page will no be cached(default is false)
    affix: true                  if set true, the tag will affix in the tags-view
    breadcrumb: false            if set false, the item will hidden in breadcrumb(default is true)
    activeMenu: '/example/list'  if set path, the sidebar will highlight the path you set
  }
 */

/**
 * constantRoutes
 * a base page that does not have permission requirements
 * all roles can be accessed
 */
export const constantRoutes = [
  {
    path: '/redirect',
    component: Layout,
    hidden: true,
    children: [
      {
        path: '/redirect/:path(.*)',
        component: () => import('@/views/redirect/index')
      }
    ]
  },
  {
    path: '/login',
    component: () => import('@/views/login/index'),
    hidden: true
  },
  {
    path: '/auth-redirect',
    component: () => import('@/views/login/auth-redirect'),
    hidden: true
  },
  {
    path: '/404',
    component: () => import('@/views/error-page/404'),
    hidden: true
  },
  {
    path: '/401',
    component: () => import('@/views/error-page/401'),
    hidden: true
  },
  {
    path: '/overview',
    component: Layout,
    redirect: '/overview/dashboard',
    alwaysShow: true,
    name: 'OverviewGroup',
    meta: { title: 'Tổng quan', icon: 'dashboard', roles: ['admin'] },
    children: [
      {
        path: 'dashboard',
        component: () => import('@/views/dashboard/index'),
        name: 'Dashboard',
        meta: { title: 'Tổng quan shop', affix: true, roles: ['admin'] }
      }
    ]
  },
  {
    path: '/',
    redirect: '/overview/dashboard',
    hidden: true
  },
  {
    path: '/dashboard',
    redirect: '/overview/dashboard',
    hidden: true
  },
  {
    path: '/profile',
    component: Layout,
    redirect: '/profile/index',
    hidden: true,
    children: [
      {
        path: 'index',
        component: () => import('@/views/profile/index'),
        name: 'Profile',
        meta: { title: 'Hồ sơ', icon: 'user', noCache: true }
      }
    ]
  }
]

/**
 * asyncRoutes
 * the routes that need to be dynamically loaded based on user roles
 */
export const asyncRoutes = [
  {
    path: '/sales',
    component: Layout,
    redirect: '/sales/orders',
    alwaysShow: true,
    name: 'SalesGroup',
    meta: {
      title: 'Bán hàng',
      icon: 'el-icon-s-order',
      roles: ['admin']
    },
    children: [
      {
        path: 'orders',
        component: () => import('@/views/orders/index'),
        name: 'OrderManagement',
        meta: { title: 'Quản lý đơn hàng', roles: ['admin'] }
      },
      {
        path: 'returns',
        component: () => import('@/views/shop-ops/returns'),
        name: 'ShopOpReturns',
        meta: { title: 'Đổi trả', roles: ['admin'] }
      }
    ]
  },
  {
    path: '/catalog',
    component: Layout,
    redirect: '/catalog/products',
    alwaysShow: true,
    name: 'CatalogGroup',
    meta: {
      title: 'Sản phẩm',
      icon: 'el-icon-goods',
      roles: ['admin']
    },
    children: [
      {
        path: 'products',
        component: () => import('@/views/products/index'),
        name: 'ProductManagement',
        meta: { title: 'Quản lý sản phẩm', roles: ['admin'] }
      },
      {
        path: 'inventory-management',
        component: () => import('@/views/inventory-management/index'),
        name: 'CatalogInventoryManagement',
        meta: { title: 'Quản lý tồn kho', roles: ['admin'] }
      },
      {
        path: 'categories',
        component: () => import('@/views/categories/index'),
        name: 'CategoryManagement',
        meta: { title: 'Quản lý danh mục', roles: ['admin'] }
      }
    ]
  },
  {
    path: '/customers',
    component: Layout,
    redirect: '/customers/management',
    alwaysShow: true,
    name: 'CustomerGroup',
    meta: {
      title: 'Khách hàng',
      icon: 'el-icon-user',
      roles: ['admin']
    },
    children: [
      {
        path: 'management',
        component: () => import('@/views/customers/index'),
        name: 'CustomerManagement',
        meta: { title: 'Quản lý khách hàng', roles: ['admin'] }
      }
    ]
  },
  {
    path: '/operations',
    component: Layout,
    redirect: '/operations/warehouse-inbounds',
    alwaysShow: true,
    name: 'OperationsGroup',
    meta: {
      title: 'Kho & vận hành',
      icon: 'el-icon-setting',
      roles: ['admin']
    },
    children: [
      {
        path: 'warehouse-inbounds',
        component: () => import('@/views/shop-ops/warehouse-inbounds'),
        name: 'ShopOpWarehouseInbounds',
        meta: { title: 'Nhập kho', roles: ['admin'] }
      },
      {
        path: 'vouchers',
        component: () => import('@/views/shop-ops/vouchers'),
        name: 'ShopOpVouchers',
        meta: { title: 'Voucher', roles: ['admin'] }
      },
      {
        path: 'banners',
        component: () => import('@/views/shop-ops/banners'),
        name: 'ShopOpBanners',
        meta: { title: 'Banner', roles: ['admin'] }
      }
    ]
  },
  {
    path: '/system-admin',
    component: Layout,
    redirect: '/system-admin/notifications',
    alwaysShow: true,
    name: 'SystemAdminGroup',
    meta: {
      title: 'Quản trị hệ thống',
      icon: 'el-icon-setting',
      roles: ['admin']
    },
    children: [
      {
        path: 'notifications',
        component: () => import('@/views/shop-ops/notifications'),
        name: 'ShopOpNotifications',
        meta: { title: 'Thông báo admin', roles: ['admin'] }
      },
      {
        path: 'audit-logs',
        component: () => import('@/views/shop-ops/audit-logs'),
        name: 'ShopOpAuditLogs',
        meta: { title: 'Nhật ký hệ thống', roles: ['admin'] }
      },
      {
        path: 'store-settings',
        component: () => import('@/views/shop-ops/store-settings'),
        name: 'ShopOpStoreSettings',
        meta: { title: 'Cài đặt shop', roles: ['admin'] }
      }
    ]
  },
  {
    path: '/permission',
    component: Layout,
    redirect: '/permission/index',
    alwaysShow: true,
    name: 'Permission',
    meta: {
      title: 'Phân quyền',
      icon: 'lock',
      roles: ['admin']
    },
    children: [
      {
        path: 'index',
        component: () => import('@/views/permission/index'),
        name: 'PermissionIndex',
        meta: { title: 'Quyền truy cập', roles: ['admin'] }
      }
    ]
  },

  // 404 page must be placed at the end !!!
  { path: '*', redirect: '/404', hidden: true }
]

const createRouter = () => new Router({
  // mode: 'history', // require service support
  scrollBehavior: () => ({ y: 0 }),
  routes: constantRoutes
})

const router = createRouter()

// Detail see: https://github.com/vuejs/vue-router/issues/1234#issuecomment-357941465
export function resetRouter() {
  const newRouter = createRouter()
  router.matcher = newRouter.matcher // reset router
}

export default router

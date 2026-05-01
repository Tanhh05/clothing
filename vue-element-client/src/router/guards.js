import { useAuthStore } from "@/store/authStore";

function normalizeRoles(rawRoles) {
  const roles = Array.isArray(rawRoles) ? rawRoles : rawRoles ? [rawRoles] : [];
  return roles
    .map((role) => String(role || "").toUpperCase().replace(/^ROLE_/, ""))
    .filter(Boolean);
}

export function applyGuards(router) {
  router.beforeEach((to) => {
    const authStore = useAuthStore();
    const clientRoles = normalizeRoles(authStore.roles);
    const adminRoles = normalizeRoles(authStore.adminRoles);
    const hasAdminRole = adminRoles.includes("ADMIN");
    const isAdminLogin = to.path === "/admin/login";
    const isAdminRoute = to.path.startsWith("/admin") && !isAdminLogin;

    if (isAdminLogin) {
      if (authStore.isAdminAuthenticated && hasAdminRole) {
        return "/admin";
      }
      return true;
    }

    if (to.meta.guestOnly && authStore.isAuthenticated) {
      return "/products";
    }

    if (isAdminRoute && !authStore.isAdminAuthenticated) {
      return "/admin/login";
    }

    if (isAdminRoute && !hasAdminRole) {
      return "/admin/login";
    }

    if (!isAdminRoute && to.meta.requiresAuth && !authStore.isAuthenticated) {
      return "/auth/login";
    }

    if (to.meta.roles?.length) {
      const normalizedRequiredRoles = to.meta.roles.map((role) => String(role).toUpperCase().replace(/^ROLE_/, ""));
      const roleSource = isAdminRoute ? adminRoles : clientRoles;
      const canAccess = normalizedRequiredRoles.some((role) => roleSource.includes(role));
      if (!canAccess) {
        return "/products";
      }
    }

    return true;
  });
}

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

    if (to.meta.guestOnly && authStore.isAuthenticated) {
      return "/products";
    }

    if (to.meta.requiresAuth && !authStore.isAuthenticated) {
      return "/auth/login";
    }

    if (to.meta.roles?.length) {
      const normalizedRequiredRoles = to.meta.roles.map((role) => String(role).toUpperCase().replace(/^ROLE_/, ""));
      const canAccess = normalizedRequiredRoles.some((role) => clientRoles.includes(role));
      if (!canAccess) {
        return "/products";
      }
    }

    return true;
  });
}

import { defineStore } from "pinia";
import { authApi } from "@/modules/auth/api/authApi";
import { loadAuthFromStorage, clearAuthStorage, saveAuthToStorage } from "@/utils/storage";

const initialAuth = loadAuthFromStorage();
const hasLegacyToken = Boolean(initialAuth.token);
const legacyIsAdmin = Boolean(initialAuth.adminSession);

const resolveClientToken = () => {
  if (initialAuth.clientToken !== undefined) return initialAuth.clientToken || "";
  if (hasLegacyToken && !legacyIsAdmin) return initialAuth.token || "";
  return "";
};

const resolveAdminToken = () => {
  if (initialAuth.adminToken !== undefined) return initialAuth.adminToken || "";
  if (hasLegacyToken && legacyIsAdmin) return initialAuth.token || "";
  return "";
};

const resolveClientRoles = () => {
  if (Array.isArray(initialAuth.clientRoles)) return initialAuth.clientRoles;
  if (hasLegacyToken && !legacyIsAdmin) return initialAuth.roles || [];
  return [];
};

const resolveAdminRoles = () => {
  if (Array.isArray(initialAuth.adminRoles)) return initialAuth.adminRoles;
  if (hasLegacyToken && legacyIsAdmin) return initialAuth.roles || [];
  return [];
};

const resolveClientUsername = () => {
  if (initialAuth.clientUsername !== undefined) return initialAuth.clientUsername || "";
  if (hasLegacyToken && !legacyIsAdmin) return initialAuth.username || "";
  return "";
};

const resolveAdminUsername = () => {
  if (initialAuth.adminUsername !== undefined) return initialAuth.adminUsername || "";
  if (hasLegacyToken && legacyIsAdmin) return initialAuth.username || "";
  return "";
};

const resolveClientUserId = () => {
  if (initialAuth.clientUserId !== undefined) return initialAuth.clientUserId || null;
  if (hasLegacyToken && !legacyIsAdmin) return initialAuth.userId || null;
  return null;
};

const resolveAdminUserId = () => {
  if (initialAuth.adminUserId !== undefined) return initialAuth.adminUserId || null;
  if (hasLegacyToken && legacyIsAdmin) return initialAuth.userId || null;
  return null;
};

const normalizeRoles = (rawRoles) => {
  const roles = Array.isArray(rawRoles) ? rawRoles : rawRoles ? [rawRoles] : [];
  return roles
    .map((role) => String(role || "").toUpperCase().replace(/^ROLE_/, ""))
    .filter(Boolean);
};

export const useAuthStore = defineStore("authStore", {
  state: () => ({
    token: resolveClientToken(),
    roles: resolveClientRoles(),
    username: resolveClientUsername(),
    userId: resolveClientUserId(),
    profile: null,
    adminToken: resolveAdminToken(),
    adminRoles: resolveAdminRoles(),
    adminUsername: resolveAdminUsername(),
    adminUserId: resolveAdminUserId(),
    adminProfile: null
  }),
  getters: {
    isAuthenticated: (state) => Boolean(state.token),
    isAdminAuthenticated: (state) => Boolean(state.adminToken),
    hasAdminRole: (state) => normalizeRoles(state.adminRoles).includes("ADMIN")
  },
  actions: {
    persistStorage() {
      saveAuthToStorage({
        clientToken: this.token,
        clientRoles: this.roles,
        clientUsername: this.username,
        clientUserId: this.userId,
        adminToken: this.adminToken,
        adminRoles: this.adminRoles,
        adminUsername: this.adminUsername,
        adminUserId: this.adminUserId
      });
    },
    persistClientAuth(data) {
      this.token = data.accessToken;
      this.roles = data.roles || [];
      this.username = data.username;
      this.userId = data.userId;
      this.persistStorage();
    },
    persistAdminAuth(data) {
      this.adminToken = data.accessToken;
      this.adminRoles = data.roles || [];
      this.adminUsername = data.username;
      this.adminUserId = data.userId;
      this.persistStorage();
    },
    async login(payload) {
      return this.loginClient(payload);
    },
    async loginClient(payload) {
      const { data } = await authApi.login(payload);
      this.persistClientAuth(data);
      await this.fetchProfile();
      return data;
    },
    async loginWithGoogle(idToken) {
      const { data } = await authApi.googleLogin({ idToken });
      this.persistClientAuth(data);
      await this.fetchProfile();
      return data;
    },
    async loginAdmin(payload) {
      const { data } = await authApi.login(payload);
      const roles = normalizeRoles(data?.roles);
      if (!roles.includes("ADMIN")) {
        this.clearAdminAuth();
        throw new Error("Tài khoản không có quyền quản trị");
      }
      this.persistAdminAuth(data);
      return data;
    },
    async register(payload) {
      const { data } = await authApi.register(payload);
      return data;
    },
    async fetchProfile(force = false) {
      if (!this.isAuthenticated) {
        this.profile = null;
        return null;
      }
      if (!force && this.profile) {
        return this.profile;
      }
      const { data } = await authApi.getMe();
      this.profile = data;
      return data;
    },
    async updateProfile(payload) {
      const { data } = await authApi.updateMe(payload);
      this.profile = data;
      return data;
    },
    async fetchAdminProfile(force = false) {
      if (!this.isAdminAuthenticated) {
        this.adminProfile = null;
        return null;
      }
      if (!force && this.adminProfile) {
        return this.adminProfile;
      }
      const { data } = await authApi.getMe();
      this.adminProfile = data;
      return data;
    },
    clearClientAuth() {
      this.token = "";
      this.roles = [];
      this.username = "";
      this.userId = null;
      this.profile = null;
      this.persistStorage();
    },
    clearAdminAuth() {
      this.adminToken = "";
      this.adminRoles = [];
      this.adminUsername = "";
      this.adminUserId = null;
      this.adminProfile = null;
      this.persistStorage();
    },
    clearAuth() {
      this.clearClientAuth();
      this.clearAdminAuth();
      clearAuthStorage();
    }
  }
});

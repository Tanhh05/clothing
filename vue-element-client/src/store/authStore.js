import { defineStore } from "pinia";
import { authApi } from "@/modules/auth/api/authApi";
import { loadAuthFromStorage, clearAuthStorage, saveAuthToStorage } from "@/utils/storage";

const initialAuth = loadAuthFromStorage();

export const useAuthStore = defineStore("authStore", {
  state: () => ({
    token: initialAuth.token || "",
    roles: Array.isArray(initialAuth.roles) ? initialAuth.roles : [],
    username: initialAuth.username || "",
    userId: initialAuth.userId ?? null,
    profile: null
  }),
  getters: {
    isAuthenticated: (state) => Boolean(state.token)
  },
  actions: {
    persistStorage() {
      saveAuthToStorage({
        token: this.token,
        roles: this.roles,
        username: this.username,
        userId: this.userId
      });
    },
    persistAuth(data) {
      this.token = data.accessToken;
      this.roles = data.roles || [];
      this.username = data.username;
      this.userId = data.userId;
      this.persistStorage();
    },
    async login(payload) {
      const { data } = await authApi.login(payload);
      this.persistAuth(data);
      await this.fetchProfile();
      return data;
    },
    async loginWithGoogle(idToken) {
      const { data } = await authApi.googleLogin({ idToken });
      this.persistAuth(data);
      await this.fetchProfile();
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
    clearClientAuth() {
      this.token = "";
      this.roles = [];
      this.username = "";
      this.userId = null;
      this.profile = null;
      this.persistStorage();
    },
    clearAuth() {
      this.clearClientAuth();
      clearAuthStorage();
    },
    logout() {
      this.clearAuth();
    }
  }
});

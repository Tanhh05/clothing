import api from "./api";
import { useAuthStore } from "@/store/authStore";

function setupInterceptors(pinia) {
  api.interceptors.request.use((config) => {
    const authStore = useAuthStore(pinia);
    const isAdminPath = window.location.pathname.startsWith("/admin");
    const token = isAdminPath ? authStore.adminToken : authStore.token;
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  });

  api.interceptors.response.use(
    (response) => response,
    (error) => {
      const status = error?.response?.status;
      if (status === 401) {
        const authStore = useAuthStore(pinia);
        const isAdminPath = window.location.pathname.startsWith("/admin");
        if (isAdminPath) {
          authStore.clearAdminAuth();
        } else {
          authStore.clearClientAuth();
        }
      }
      return Promise.reject(error);
    }
  );
}

export default setupInterceptors;

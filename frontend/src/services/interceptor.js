import api from "./api";
import { useAuthStore } from "@/store/authStore";

let requestInterceptorId = null;
let responseInterceptorId = null;

function isAdminContext() {
  if (typeof window === "undefined") return false;
  return window.location.pathname.startsWith("/admin");
}

function setupInterceptors(pinia) {
  if (requestInterceptorId !== null) {
    api.interceptors.request.eject(requestInterceptorId);
  }
  if (responseInterceptorId !== null) {
    api.interceptors.response.eject(responseInterceptorId);
  }

  requestInterceptorId = api.interceptors.request.use((config) => {
    const authStore = useAuthStore(pinia);
    const token = isAdminContext()
      ? (authStore.adminToken || authStore.token)
      : (authStore.token || authStore.adminToken);
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  });

  responseInterceptorId = api.interceptors.response.use(
    (response) => response,
    (error) => {
      const status = error?.response?.status;
      if (status === 401) {
        const authStore = useAuthStore(pinia);
        if (isAdminContext()) {
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

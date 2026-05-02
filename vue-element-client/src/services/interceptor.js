import api from "./api";
import { useAuthStore } from "@/store/authStore";
import router from "@/router";

let requestInterceptorId = null;
let responseInterceptorId = null;

function setupInterceptors(pinia) {
  if (requestInterceptorId !== null) {
    api.interceptors.request.eject(requestInterceptorId);
  }
  if (responseInterceptorId !== null) {
    api.interceptors.response.eject(responseInterceptorId);
  }

  requestInterceptorId = api.interceptors.request.use((config) => {
    const authStore = useAuthStore(pinia);
    const token = authStore.token;
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
        authStore.clearClientAuth();
      } else if (status === 403 || status === 404) {
        const targetPath = status === 403 ? "/system/forbidden" : "/system/not-found";
        if (router.currentRoute.value.path !== targetPath) {
          router.push({
            path: targetPath,
            query: {
              message: String(error?.response?.data?.message || "")
            }
          });
        }
      } else if (!error?.response) {
        const currentPath = router.currentRoute.value.path;
        if (currentPath !== "/system/connection-error") {
          router.push({
            path: "/system/connection-error",
            query: {
              from: currentPath
            }
          });
        }
      }
      return Promise.reject(error);
    }
  );
}

export default setupInterceptors;

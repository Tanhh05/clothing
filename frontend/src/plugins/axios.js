import axios from "axios";

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api",
  timeout: 10000
});

api.interceptors.request.use(
  (config) => {
    const legacy = JSON.parse(localStorage.getItem("auth") || "{}");
    const scoped = JSON.parse(localStorage.getItem("clothing_auth") || "{}");
    const isAdminPath = typeof window !== "undefined" && window.location.pathname.startsWith("/admin");
    const token = isAdminPath
      ? (scoped.adminToken || legacy.token || "")
      : (scoped.clientToken || legacy.token || "");
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

export default api;

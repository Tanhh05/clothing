import api from "@/services/api";

export const authApi = {
  login(payload) {
    const normalized = {
      usernameOrEmail: String(payload?.usernameOrEmail || payload?.username || "").trim(),
      password: String(payload?.password || "")
    };
    return api.post("/auth/login", normalized);
  },
  googleLogin(payload) {
    return api.post("/auth/google", payload);
  },
  register(payload) {
    return api.post("/auth/register", payload);
  },
  getMe() {
    return api.get("/user/me");
  },
  updateMe(payload) {
    return api.put("/user/me", payload);
  }
};

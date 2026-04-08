import api from "@/services/api";

export const authApi = {
  login(payload) {
    return api.post("/auth/login", payload);
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

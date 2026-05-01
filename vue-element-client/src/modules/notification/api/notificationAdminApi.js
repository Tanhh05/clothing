import api from "@/services/api";

export const notificationAdminApi = {
  getHistory() {
    return api.get("/admin/notifications");
  },
  create(payload) {
    return api.post("/admin/notifications", payload);
  }
};

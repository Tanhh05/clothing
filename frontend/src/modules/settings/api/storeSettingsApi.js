import api from "@/services/api";

export const storeSettingsApi = {
  getPublicSettings() {
    return api.get("/store-settings");
  },
  getSettings() {
    return api.get("/admin/store-settings");
  },
  updateSettings(payload) {
    return api.put("/admin/store-settings", payload);
  }
};

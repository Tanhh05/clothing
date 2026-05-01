import api from "@/services/api";

export const bannerApi = {
  getAdminBanners() {
    return api.get("/admin/banners");
  },
  createBanner(payload) {
    if (payload instanceof FormData) {
      return api.post("/admin/banners", payload, {
        headers: {
          "Content-Type": "multipart/form-data"
        }
      });
    }
    return api.post("/admin/banners", payload);
  },
  updateBanner(id, payload) {
    if (payload instanceof FormData) {
      return api.put(`/admin/banners/${id}`, payload, {
        headers: {
          "Content-Type": "multipart/form-data"
        }
      });
    }
    return api.put(`/admin/banners/${id}`, payload);
  },
  deleteBanner(id) {
    return api.delete(`/admin/banners/${id}`);
  }
};

import api from "@/services/api";

export const voucherApi = {
  getAdminVouchers() {
    return api.get("/admin/vouchers");
  },
  createVoucher(payload) {
    return api.post("/admin/vouchers", payload);
  },
  updateVoucher(id, payload) {
    return api.put(`/admin/vouchers/${id}`, payload);
  },
  deleteVoucher(id) {
    return api.delete(`/admin/vouchers/${id}`);
  }
};

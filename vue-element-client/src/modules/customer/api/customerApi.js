import api from "@/services/api";

export const customerApi = {
  getCustomers(params) {
    return api.get("/admin/users", { params });
  },
  getDefaultAddress(userId) {
    return api.get(`/user/addresses/admin/users/${userId}/default`);
  },
  updateCustomerStatus(id, status) {
    return api.patch(`/admin/users/${id}/status`, { status });
  }
};

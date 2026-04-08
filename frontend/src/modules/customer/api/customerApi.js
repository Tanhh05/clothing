import api from "@/services/api";

export const customerApi = {
  getCustomers(params) {
    return api.get("/admin/users", { params });
  },
  updateCustomerStatus(id, status) {
    return api.patch(`/admin/users/${id}/status`, { status });
  }
};

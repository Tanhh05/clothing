import api from "@/services/api";

export const returnApi = {
  getAdminReturns(params) {
    return api.get("/admin/returns", { params });
  },
  updateReturnStatus(id, status) {
    return api.patch(`/admin/returns/${id}/status`, { status });
  }
};

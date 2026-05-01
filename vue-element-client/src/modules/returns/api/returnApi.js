import api from "@/services/api";

export const returnApi = {
  getAdminReturns(params) {
    return api.get("/admin/returns", { params });
  },
  updateReturnStatus(id, status, note) {
    return api.patch(`/admin/returns/${id}/status`, { status, note });
  },
  getMyReturns(params) {
    return api.get("/returns/my", { params });
  },
  createReturnRequest(payload) {
    return api.post("/returns", payload);
  }
};

import api from "@/services/api";

export const warehouseApi = {
  getInboundReceipts() {
    return api.get("/admin/warehouse-inbounds");
  },
  getInboundReceiptPage(params) {
    return api.get("/admin/warehouse-inbounds/page", { params });
  },
  getInboundReceiptById(id) {
    return api.get(`/admin/warehouse-inbounds/${id}`);
  },
  getSkuSuggestions(q = "") {
    return api.get("/admin/warehouse-inbounds/skus", { params: { q } });
  },
  createInboundReceipt(payload) {
    return api.post("/admin/warehouse-inbounds", payload);
  }
};

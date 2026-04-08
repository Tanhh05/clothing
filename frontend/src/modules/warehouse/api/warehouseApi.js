import api from "@/services/api";

export const warehouseApi = {
  getInboundReceipts() {
    return api.get("/admin/warehouse-inbounds");
  },
  createInboundReceipt(payload) {
    return api.post("/admin/warehouse-inbounds", payload);
  }
};

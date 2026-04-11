import api from "@/services/api";

export const orderApi = {
  createOrder(payload) {
    return api.post("/orders", payload);
  },
  getMyOrders() {
    return api.get("/orders/my");
  },
  reorder(orderId) {
    return api.post(`/orders/my/${orderId}/reorder`);
  },
  getAllOrders() {
    return api.get("/orders");
  },
  getAdminOrders(params) {
    return api.get("/orders/admin", { params });
  },
  getAdminSummary() {
    return api.get("/orders/summary");
  },
  updateOrderStatus(orderId, payload) {
    if (typeof payload === "string") {
      return api.patch(`/orders/${orderId}/status`, { status: payload });
    }
    return api.patch(`/orders/${orderId}/status`, payload || {});
  },
  bulkUpdateStatus(ids, status) {
    return api.patch("/orders/bulk/status", { ids, status });
  },
  syncOrderStatusWithGhn(orderId) {
    return api.post(`/orders/${orderId}/status/sync-ghn`);
  }
};

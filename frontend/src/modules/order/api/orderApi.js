import api from "@/services/api";

export const orderApi = {
  createOrder(payload) {
    return api.post("/orders", payload);
  },
  getMyOrders() {
    return api.get("/orders/my");
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
  updateOrderStatus(orderId, status) {
    return api.patch(`/orders/${orderId}/status`, { status });
  },
  bulkUpdateStatus(ids, status) {
    return api.patch("/orders/bulk/status", { ids, status });
  }
};

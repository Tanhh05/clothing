import api from "@/services/api";

export const paymentApi = {
  getOrderPaymentStatus(orderId) {
    return api.get(`/payments/orders/${orderId}/status`);
  }
};

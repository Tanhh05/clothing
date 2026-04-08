import { defineStore } from "pinia";
import { orderApi } from "@/modules/order/api/orderApi";

export const useOrderStore = defineStore("orderStore", {
  state: () => ({
    orders: [],
    loading: false,
    error: ""
  }),
  actions: {
    async fetchMyOrders() {
      this.loading = true;
      this.error = "";
      try {
        const { data } = await orderApi.getMyOrders();
        this.orders = data || [];
      } catch (error) {
        this.error = error?.response?.data?.message || "Không thể tải danh sách đơn hàng";
        this.orders = [];
      } finally {
        this.loading = false;
      }
    }
  }
});

import api from "@/services/api";

export const reviewApi = {
  getByProduct(productId) {
    return api.get(`/reviews/products/${productId}`);
  },
  create(payload) {
    return api.post("/reviews", payload);
  }
};

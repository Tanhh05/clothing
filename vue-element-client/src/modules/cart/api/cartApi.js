import api from "@/services/api";

export const cartApi = {
  getCart() {
    return api.get("/cart");
  },
  addItem(variantId, quantity) {
    return api.post("/cart/items", { variantId, quantity });
  },
  updateItem(cartItemId, quantity) {
    return api.put(`/cart/items/${cartItemId}`, { quantity });
  },
  removeItem(cartItemId) {
    return api.delete(`/cart/items/${cartItemId}`);
  }
};

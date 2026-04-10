import api from "@/services/api";

export const wishlistApi = {
  getWishlist() {
    return api.get("/wishlist");
  },
  addItem(productId) {
    return api.post("/wishlist/items", { productId });
  },
  removeItem(productId) {
    return api.delete(`/wishlist/items/${productId}`);
  },
  upsertPriceAlert(productId, targetPrice) {
    return api.post("/wishlist/price-alerts", { productId, targetPrice });
  },
  getDeals() {
    return api.get("/wishlist/deals");
  }
};

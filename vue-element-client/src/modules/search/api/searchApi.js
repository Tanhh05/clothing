import api from "@/services/api";

export const searchApi = {
  searchProducts(q, size = 10) {
    return api.get("/search/products", { params: { q, size } });
  }
};

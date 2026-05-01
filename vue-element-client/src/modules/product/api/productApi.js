import api from "@/services/api";

export const productApi = {
  getProducts(params) {
    return api.get("/products", { params });
  },
  getRecommendations(productIds = [], limit = 4) {
    return api.get("/products/recommendations", {
      params: { productIds, limit }
    });
  },
  getById(idOrSlug) {
    return api.get(`/products/${encodeURIComponent(idOrSlug)}`);
  },
  createProduct(payload) {
    if (payload instanceof FormData) {
      return api.post("/products", payload, {
        headers: {
          "Content-Type": "multipart/form-data"
        }
      });
    }
    return api.post("/products", payload);
  },
  updateProduct(id, payload) {
    if (payload instanceof FormData) {
      return api.put(`/products/${id}`, payload, {
        headers: {
          "Content-Type": "multipart/form-data"
        }
      });
    }
    return api.put(`/products/${id}`, payload);
  },
  deleteProduct(id) {
    return api.delete(`/products/${id}`);
  },
  getDeletedProducts() {
    return api.get("/products/deleted");
  },
  restoreProduct(id) {
    return api.put(`/products/${id}/restore`);
  },
  bulkDelete(ids) {
    return api.post("/products/bulk/delete", { ids });
  },
  bulkRestore(ids) {
    return api.post("/products/bulk/restore", { ids });
  },
  bulkUpdateStatus(ids, status) {
    return api.patch("/products/bulk/status", { ids, status });
  },
  getInventoryAlerts(threshold = 5) {
    return api.get("/products/inventory-alerts", { params: { threshold } });
  },
  getInventoryLogs(variantId) {
    return api.get("/products/inventory-logs", { params: { variantId } });
  },
  getVariantOptions(type) {
    return api.get("/products/variant-options", { params: { type } });
  },
  createVariantOption(type, value) {
    return api.post(`/products/variant-options?type=${encodeURIComponent(type)}`, { value });
  },
  importProductsXlsx(file, options = {}) {
    const formData = new FormData();
    formData.append("file", file);
    return api.post("/products/import/xlsx", formData, {
      params: {
        dryRun: Boolean(options.dryRun),
        upsertBySku: Boolean(options.upsertBySku)
      },
      headers: {
        "Content-Type": "multipart/form-data"
      }
    });
  },
  downloadImportTemplate() {
    return api.get("/products/import/template", {
      responseType: "blob"
    });
  },
  subscribeStockAlert(payload) {
    return api.post("/user/stock-alerts", payload);
  },
  unsubscribeStockAlert(payload) {
    return api.delete("/user/stock-alerts", { data: payload });
  }
};

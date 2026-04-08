import { defineStore } from "pinia";
import { productApi } from "@/modules/product/api/productApi";

export const useProductStore = defineStore("productStore", {
  state: () => ({
    products: [],
    loading: false,
    
    // Pagination data
    page: 0,
    size: 12, // Default size x4 columns
    totalElements: 0,
    totalPages: 0,
    
    // Filters (optional for later expansion)
    filters: {
      category: null,
      sortBy: 'id',
      direction: 'desc',
      q: ''
    }
  }),
  
  actions: {
    async fetchProducts(params = {}) {
      this.loading = true;
      try {
        // Merge state params with incoming overrides
        const finalParams = {
          page: this.page,
          size: this.size,
          sortBy: this.filters.sortBy,
          direction: this.filters.direction,
          ...this.filters,
          ...params
        };

        const { data } = await productApi.getProducts(finalParams);
        
        // Handle PageResponse structure
        this.products = data.content || [];
        this.totalElements = data.totalElements || 0;
        this.totalPages = data.totalPages || 0;
        this.page = data.page || 0;
        this.size = data.size || 12;
      } catch (error) {
        console.error("Failed to fetch products:", error);
      } finally {
        this.loading = false;
      }
    },

    setPage(page) {
      this.page = page;
      this.fetchProducts();
    },

    setSize(size) {
      this.size = size;
      this.page = 0; // Reset to page 0 on size change
      this.fetchProducts();
    }
  }
});

import { defineStore } from "pinia";
import { searchApi } from "@/modules/search/api/searchApi";

export const useSearchStore = defineStore("searchStore", {
  state: () => ({
    keyword: "",
    results: []
  }),
  actions: {
    async search(keyword) {
      this.keyword = keyword;
      if (!keyword?.trim()) {
        this.results = [];
        return;
      }
      const { data } = await searchApi.searchProducts(keyword, 10);
      this.results = data || [];
    }
  }
});

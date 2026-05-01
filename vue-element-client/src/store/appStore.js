import { defineStore } from "pinia";

export const useAppStore = defineStore("appStore", {
  state: () => ({
    loading: false,
    theme: "light"
  }),
  actions: {
    setLoading(value) {
      this.loading = value;
    },
    setTheme(theme) {
      this.theme = theme;
    }
  }
});

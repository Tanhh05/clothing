import { defineStore } from "pinia";
import { storeSettingsApi } from "@/modules/settings/api/storeSettingsApi";

const defaults = {
  storeName: "Clothing Store",
  hotline: "0900 000 000",
  supportEmail: "support@clothing.local",
  address: "TP.HCM, Việt Nam",
  defaultShippingFee: 30000,
  freeShippingThreshold: 500000,
  enableCOD: true,
  enableMomo: true,
  shippingPolicy: "Giao hàng toàn quốc trong 2-5 ngày làm việc.",
  returnPolicy: "Đổi trả trong 7 ngày với sản phẩm còn nguyên tem."
};

export const useStoreSettingsStore = defineStore("storeSettingsStore", {
  state: () => ({
    settings: { ...defaults },
    loading: false,
    loaded: false
  }),
  getters: {
    storeName: (state) => state.settings.storeName,
    hotline: (state) => state.settings.hotline,
    supportEmail: (state) => state.settings.supportEmail,
    address: (state) => state.settings.address,
    defaultShippingFee: (state) => Number(state.settings.defaultShippingFee || 0),
    freeShippingThreshold: (state) => Number(state.settings.freeShippingThreshold || 0),
    enableCOD: (state) => Boolean(state.settings.enableCOD),
    enableMomo: (state) => Boolean(state.settings.enableMomo),
    shippingPolicy: (state) => state.settings.shippingPolicy,
    returnPolicy: (state) => state.settings.returnPolicy
  },
  actions: {
    async fetchPublicSettings(force = false) {
      if (this.loading) return;
      if (this.loaded && !force) return;
      this.loading = true;
      try {
        const { data } = await storeSettingsApi.getPublicSettings();
        this.settings = { ...defaults, ...(data || {}) };
        this.loaded = true;
      } catch {
        this.settings = { ...defaults };
      } finally {
        this.loading = false;
      }
    }
  }
});

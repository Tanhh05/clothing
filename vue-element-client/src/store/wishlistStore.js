import { defineStore } from "pinia";
import { wishlistApi } from "@/modules/wishlist/api/wishlistApi";
import { useAuthStore } from "@/store/authStore";
import { ElMessage } from "@/utils/dialogMessage";

export const useWishlistStore = defineStore("wishlistStore", {
  state: () => ({
    serverProductIds: [],
    priceAlertTargets: {},
    loading: false,
    loadedServer: false,
    fetchPromise: null
  }),
  getters: {
    productIds(state) {
      return state.serverProductIds;
    },
    productIdSet() {
      return new Set(this.productIds);
    },
    targetPriceByProduct(state) {
      return state.priceAlertTargets || {};
    }
  },
  actions: {
    applyWishlistPayload(data) {
      this.serverProductIds = (data?.productIds || [])
        .map((id) => Number(id))
        .filter((id) => Number.isFinite(id));
      const nextTargets = {};
      Object.entries(data?.priceAlertTargets || {}).forEach(([productId, targetPrice]) => {
        const normalizedProductId = Number(productId);
        const normalizedTarget = Number(targetPrice || 0);
        if (Number.isFinite(normalizedProductId) && Number.isFinite(normalizedTarget)) {
          nextTargets[normalizedProductId] = Math.max(0, normalizedTarget);
        }
      });
      this.priceAlertTargets = nextTargets;
      this.loadedServer = true;
    },

    getAlertTarget(productId) {
      const normalizedId = Number(productId);
      if (!Number.isFinite(normalizedId)) return 0;
      return Number(this.priceAlertTargets?.[normalizedId] || 0);
    },

    isWishlisted(productId) {
      return this.productIdSet.has(Number(productId));
    },

    async fetchWishlist(force = false) {
      const authStore = useAuthStore();
      if (!authStore.isAuthenticated) {
        this.serverProductIds = [];
        this.loadedServer = false;
        return;
      }
      if (!force && this.loadedServer) {
        return;
      }
      if (this.fetchPromise) {
        await this.fetchPromise;
        return;
      }

      this.fetchPromise = (async () => {
        this.loading = true;
        try {
          const { data } = await wishlistApi.getWishlist();
          this.applyWishlistPayload(data);
        } finally {
          this.loading = false;
        }
      })();

      try {
        await this.fetchPromise;
      } finally {
        this.fetchPromise = null;
      }
    },

    async ensureLoaded() {
      const authStore = useAuthStore();
      if (!authStore.isAuthenticated) {
        this.serverProductIds = [];
        this.loadedServer = false;
        return;
      }
      await this.fetchWishlist(false);
    },

    async add(productId, silent = false) {
      const normalizedId = Number(productId);
      if (!Number.isFinite(normalizedId)) return;

      const authStore = useAuthStore();
      if (!authStore.isAuthenticated) {
        if (!silent) ElMessage.warning("Vui lòng đăng nhập để thêm sản phẩm yêu thích");
        return;
      }

      try {
        const { data } = await wishlistApi.addItem(normalizedId);
        this.applyWishlistPayload(data);
        if (!silent) ElMessage.success("Đã thêm vào yêu thích");
      } catch (error) {
        if (!silent) ElMessage.error("Không thể thêm sản phẩm yêu thích");
        throw error;
      }
    },

    async remove(productId, silent = false) {
      const normalizedId = Number(productId);
      if (!Number.isFinite(normalizedId)) return;

      const authStore = useAuthStore();
      if (!authStore.isAuthenticated) {
        if (!silent) ElMessage.warning("Vui lòng đăng nhập để quản lý yêu thích");
        return;
      }

      try {
        const { data } = await wishlistApi.removeItem(normalizedId);
        this.applyWishlistPayload(data);
        if (!silent) ElMessage.info("Đã bỏ khỏi yêu thích");
      } catch (error) {
        if (!silent) ElMessage.error("Không thể bỏ yêu thích");
        throw error;
      }
    },

    async toggle(productId) {
      if (this.isWishlisted(productId)) {
        await this.remove(productId);
      } else {
        await this.add(productId);
      }
    },

    async syncLocalWishlist() {
      const authStore = useAuthStore();
      if (!authStore.isAuthenticated) return;
      await this.fetchWishlist(true);
    },

    async upsertPriceAlert(productId, targetPrice) {
      const normalizedId = Number(productId);
      const normalizedTarget = Math.max(0, Number(targetPrice || 0));
      if (!Number.isFinite(normalizedId)) return;
      await wishlistApi.upsertPriceAlert(normalizedId, normalizedTarget);
      this.priceAlertTargets = {
        ...this.priceAlertTargets,
        [normalizedId]: normalizedTarget
      };
    },

    resetServerState() {
      this.serverProductIds = [];
      this.priceAlertTargets = {};
      this.loadedServer = false;
      this.fetchPromise = null;
      this.loading = false;
    }
  }
});

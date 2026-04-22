import { defineStore } from 'pinia';
import { cartApi } from '../modules/cart/api/cartApi';
import { useAuthStore } from './authStore';
import { ElMessage } from "@/utils/dialogMessage";

export const useCartStore = defineStore('cartStore', {
  state: () => ({
    cartData: null,
    localItems: JSON.parse(localStorage.getItem('cart_local_items') || '[]'),
    preferredVoucherCode: localStorage.getItem('cart_preferred_voucher_code') || '',
    loading: false
  }),
  getters: {
    items() {
      const authStore = useAuthStore();
      // Nếu đã đăng nhập, ưu tiên dữ liệu Server. Nếu chưa, lấy dữ liệu Local.
      return authStore.isAuthenticated ? (this.cartData?.items || []) : this.localItems;
    },
    totalItems() {
      return this.items.reduce((acc, item) => acc + item.quantity, 0);
    },
    totalPrice() {
      const authStore = useAuthStore();
      if (authStore.isAuthenticated) return this.cartData?.totalPrice || 0;
      return this.items.reduce((acc, item) => acc + (item.price * item.quantity), 0);
    },
    isEmpty() {
      return this.items.length === 0;
    }
  },
  actions: {
    async fetchCart() {
      const authStore = useAuthStore();
      if (!authStore.isAuthenticated) return;
      
      this.loading = true;
      try {
        const response = await cartApi.getCart();
        this.cartData = response.data;
        if ((this.cartData?.items || []).length === 0) this.clearPreferredVoucherCode();
        console.log('Cart fetched from server:', this.cartData);
      } catch (error) {
        console.error('Failed to fetch cart:', error);
        // Nếu lỗi 401 thì có thể token hết hạn, không xóa local để bảo toàn dữ liệu
      } finally {
        this.loading = false;
      }
    },

    async addItem(product, variant, quantity = 1) {
      const authStore = useAuthStore();
      
      if (authStore.isAuthenticated) {
        try {
          const response = await cartApi.addItem(variant.id, quantity);
          this.cartData = response.data;
          ElMessage.success('Đã thêm vào giỏ hàng tài khoản');
          return true;
        } catch (error) {
          // Nếu lỗi server, thử lưu tạm vào local để không mất liên lạc
          ElMessage.error('Lỗi server, đang lưu tạm vào máy của bạn');
          this.addGuestItem(product, variant, quantity);
          return false;
        }
      } else {
        return this.addGuestItem(product, variant, quantity);
      }
    },

    addGuestItem(product, variant, quantity) {
      const items = [...this.localItems];
      const existingIndex = items.findIndex(item => item.variantId === variant.id);
      
      if (existingIndex !== -1) {
        items[existingIndex].quantity += quantity;
      } else {
        items.push({
          id: 'local_' + Date.now() + Math.random().toString(36).substr(2, 5),
          variantId: variant.id,
          productId: product.id,
          productName: product.name,
          productImage: product.images?.[0]?.url || '',
          size: variant.size,
          color: variant.color,
          price: variant.price,
          quantity: quantity
        });
      }
      this.localItems = items;
      this.saveLocal();
      ElMessage.success('Đã lưu vào giỏ hàng tạm thời');
      return true;
    },

    async updateQuantity(itemId, quantity) {
      const authStore = useAuthStore();
      if (authStore.isAuthenticated && !String(itemId).startsWith('local_')) {
        try {
          const response = await cartApi.updateItem(itemId, quantity);
          this.cartData = response.data;
          if ((this.cartData?.items || []).length === 0) this.clearPreferredVoucherCode();
        } catch (error) {
          ElMessage.error('Không thể cập nhật số lượng trên máy chủ');
        }
      } else {
        const items = [...this.localItems];
        const item = items.find(i => i.id === itemId);
        if (item) {
          if (quantity <= 0) return this.removeItem(itemId);
          item.quantity = quantity;
          this.localItems = items;
          this.saveLocal();
        }
      }
    },

    async removeItem(itemId) {
      const authStore = useAuthStore();
      if (authStore.isAuthenticated && !String(itemId).startsWith('local_')) {
        try {
          const response = await cartApi.removeItem(itemId);
          this.cartData = response.data;
          if ((this.cartData?.items || []).length === 0) this.clearPreferredVoucherCode();
        } catch (error) {
          ElMessage.error('Không thể xóa trên máy chủ');
        }
      } else {
        this.localItems = this.localItems.filter(i => i.id !== itemId);
        this.saveLocal();
        if (this.localItems.length === 0) this.clearPreferredVoucherCode();
      }
    },

    async syncLocalCart() {
      if (this.localItems.length === 0) return;
      
      const authStore = useAuthStore();
      if (!authStore.isAuthenticated) return;

      console.log('Starting sync of', this.localItems.length, 'items');
      this.loading = true;
      
      const failedItems = [];
      
      for (const item of this.localItems) {
        try {
          await cartApi.addItem(item.variantId, item.quantity);
        } catch (error) {
          console.error('Failed to sync item:', item.productName, error);
          failedItems.push(item);
        }
      }

      // CHỈ XÓA những item đã đồng bộ thành công
      this.localItems = failedItems;
      this.saveLocal();

      if (failedItems.length === 0) {
        // Fetch cart mới nhất
        await this.fetchCart();
        ElMessage.success('Đồng bộ giỏ hàng thành công');
      } else {
        ElMessage.warning(`Không thể đồng bộ ${failedItems.length} sản phẩm. Chúng tôi vẫn giữ chúng trong máy của bạn.`);
      }
      
      this.loading = false;
    },

    saveLocal() {
      localStorage.setItem('cart_local_items', JSON.stringify(this.localItems));
    },

    clearCartLocal() {
      this.localItems = [];
      this.cartData = null;
      localStorage.removeItem('cart_local_items');
      this.clearPreferredVoucherCode();
    },

    setPreferredVoucherCode(code) {
      const normalized = String(code || '').trim().toUpperCase();
      this.preferredVoucherCode = normalized;
      if (normalized) {
        localStorage.setItem('cart_preferred_voucher_code', normalized);
      } else {
        localStorage.removeItem('cart_preferred_voucher_code');
      }
    },

    clearPreferredVoucherCode() {
      this.preferredVoucherCode = '';
      localStorage.removeItem('cart_preferred_voucher_code');
    }
  }
});

<template>
  <footer class="app-footer">
    <div class="footer-main">
      <div class="footer-grid">
        <div class="footer-col">
          <h3>SẢN PHẨM</h3>
          <ul>
            <li v-if="loadingProducts">Đang tải...</li>
            <li v-else v-for="item in productLinks" :key="item.id">
              <router-link v-if="item.slug || typeof item.id === 'number'" :to="`/products/${item.slug || item.id}`">{{ item.name }}</router-link>
              <a v-else href="#">{{ item.name }}</a>
            </li>
          </ul>
        </div>

        <div class="footer-col">
          <h3>THỂ THAO</h3>
          <ul>
            <li v-if="loadingCategories">Đang tải...</li>
            <li v-else v-for="item in sportLinks" :key="item.id">
              <router-link v-if="typeof item.id === 'number'" :to="{ path: '/products', query: { category: String(item.id) } }">{{ item.name }}</router-link>
              <a v-else href="#">{{ item.name }}</a>
            </li>
          </ul>
        </div>

        <div class="footer-col">
          <h3>BỘ SƯU TẬP</h3>
          <ul>
            <li v-if="loadingCategories">Đang tải...</li>
            <li v-else v-for="item in collectionLinks" :key="item.id">
              <router-link v-if="typeof item.id === 'number'" :to="{ path: '/products', query: { category: String(item.id) } }">{{ item.name }}</router-link>
              <a v-else href="#">{{ item.name }}</a>
            </li>
          </ul>
        </div>

        <div class="footer-col">
          <h3>THÔNG TIN VỀ CÔNG TY</h3>
          <ul>
            <li><a href="#">Giới Thiệu Về Chúng Tôi</a></li>
            <li><a href="#">Cơ Hội Nghề Nghiệp</a></li>
            <li><a href="#">Tin tức</a></li>
            <li><a href="#">adidas stories</a></li>
          </ul>
        </div>

        <div class="footer-col">
          <h3>HỖ TRỢ</h3>
          <ul>
            <li><a href="#">Trợ Giúp</a></li>
            <li><a href="#">Công cụ tìm kiếm cửa hàng</a></li>
            <li><a href="#">Biểu Đồ Kích Cỡ</a></li>
            <li><a href="#">Thanh toán</a></li>
            <li><a href="#">Giao hàng</a></li>
            <li><a href="#">Trả Hàng & Hoàn Tiền</a></li>
            <li><a href="#">khuyến mãi</a></li>
            <li><a href="#">Trợ Giúp Dịch Vụ Khách Hàng</a></li>
            <li><a href="#">Sitemap</a></li>
          </ul>
        </div>

        <div class="footer-col">
          <h3>THEO DÕI CHÚNG TÔI</h3>
          <div class="social-links">
            <a :href="emailLink"><el-icon><Share /></el-icon></a>
            <a :href="phoneLink"><el-icon><Phone /></el-icon></a>
            <a :href="mapLink" target="_blank" rel="noopener noreferrer"><el-icon><Location /></el-icon></a>
          </div>
        </div>
      </div>
    </div>

    <div class="footer-bottom">
      <div class="bottom-content">
        <p class="legal-note">Thông tin footer được đồng bộ theo dữ liệu hệ thống.</p>
        <p class="copyright">© {{ currentYear }} {{ storeName }}. All Rights Reserved.</p>
      </div>
    </div>
  </footer>
</template>

<script setup>
import { Share, Phone, Location } from "@element-plus/icons-vue";
import { computed, onMounted, ref } from "vue";
import { productApi } from "@/modules/product/api/productApi";
import { categoryApi } from "@/modules/category/api/categoryApi";
import { useStoreSettingsStore } from "@/store/storeSettingsStore";

const storeSettingsStore = useStoreSettingsStore();
const loadingProducts = ref(false);
const loadingCategories = ref(false);
const productLinks = ref([]);
const sportLinks = ref([]);
const collectionLinks = ref([]);

const fallbackProducts = [
  { id: "fp-1", name: "Giày", slug: null },
  { id: "fp-2", name: "Quần áo", slug: null },
  { id: "fp-3", name: "Phụ kiện", slug: null },
  { id: "fp-4", name: "Hàng Mới Về", slug: null },
  { id: "fp-5", name: "Release Dates", slug: null },
  { id: "fp-6", name: "Top Sellers", slug: null },
  { id: "fp-7", name: "Member exclusives", slug: null },
  { id: "fp-8", name: "Outlet", slug: null }
];

const fallbackSports = [
  { id: "fs-1", name: "Chạy" },
  { id: "fs-2", name: "Đánh gôn" },
  { id: "fs-3", name: "Gym & Training" },
  { id: "fs-4", name: "Bóng đá" },
  { id: "fs-5", name: "Bóng Rổ" },
  { id: "fs-6", name: "Quần vợt" },
  { id: "fs-7", name: "Ngoai troi" },
  { id: "fs-8", name: "Motorsport" }
];

const fallbackCollections = [
  { id: "fc-1", name: "Ultra Boost" },
  { id: "fc-2", name: "Predator" },
  { id: "fc-3", name: "Superstar" },
  { id: "fc-4", name: "Stan Smith" },
  { id: "fc-5", name: "Adicolor" },
  { id: "fc-6", name: "Samba" }
];

const storeName = computed(() => storeSettingsStore.storeName);
const supportEmail = computed(() => storeSettingsStore.supportEmail);
const address = computed(() => storeSettingsStore.address);
const emailLink = computed(() => (supportEmail.value ? `mailto:${supportEmail.value}` : "mailto:"));
const phoneLink = computed(() => {
  const hotline = storeSettingsStore.hotline;
  if (!hotline) return "tel:";
  return `tel:${String(hotline).replace(/[^\d+]/g, "")}`;
});
const mapLink = computed(() => {
  if (!address.value) return "https://maps.google.com";
  return `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(address.value)}`;
});
const currentYear = new Date().getFullYear();

const fetchFooterData = async () => {
  loadingProducts.value = true;
  loadingCategories.value = true;
  try {
    const [productsResp, categoriesResp] = await Promise.all([
      productApi.getProducts({ page: 0, size: 8, sortBy: "id", direction: "desc" }),
      categoryApi.getCategories({ page: 0, size: 100, sortBy: "id", direction: "asc" })
    ]);

    const products = Array.isArray(productsResp.data?.content) ? productsResp.data.content : [];
    productLinks.value = products.slice(0, 8);

    const categories = Array.isArray(categoriesResp.data?.content) ? categoriesResp.data.content : [];
    const rootCategories = categories.filter((item) => item.parentId == null);
    const childCategories = categories.filter((item) => item.parentId != null);

    sportLinks.value = rootCategories.slice(0, 8);
    collectionLinks.value = childCategories.slice(0, 6);
  } catch (error) {
    console.error("Failed to fetch footer data:", error);
    productLinks.value = [];
    sportLinks.value = [];
    collectionLinks.value = [];
  } finally {
    loadingProducts.value = false;
    loadingCategories.value = false;
  }

  if (!productLinks.value.length) productLinks.value = fallbackProducts;
  if (!sportLinks.value.length) sportLinks.value = fallbackSports;
  if (!collectionLinks.value.length) collectionLinks.value = fallbackCollections;
};

onMounted(() => {
  fetchFooterData();
});
</script>

<style scoped lang="scss">
.app-footer {
  width: 100%;
  background: #ffffff;
  border-top: 1px solid #e5e7eb;
}

.footer-main {
  padding: 40px 20px;
  max-width: 1240px;
  margin: 0 auto;
}

.footer-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 24px;
}

.footer-col {
  padding: 0;

  h3 {
    font-size: 13px;
    font-weight: 800;
    margin-bottom: 14px;
    text-transform: uppercase;
    letter-spacing: 0.9px;
    color: #0f172a;
  }

  ul {
    list-style: none;
    padding: 0;
    margin: 0;

    li {
      margin-bottom: 8px;

      a {
        color: #1e293b;
        text-decoration: none;
        font-size: 13px;
        line-height: 1.45;
        transition: color 0.2s ease, transform 0.2s ease;

        &:hover {
          color: #2563eb;
          transform: translateX(2px);
        }
      }
    }

    li:last-child {
      margin-bottom: 0;
    }
  }
}

.social-links {
  display: flex;
  gap: 10px;
  font-size: 18px;

  a {
    color: #0f172a;
    width: 34px;
    height: 34px;
    border-radius: 50%;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    transition: all 0.2s ease;
    background: #e2e8f0;
  }

  a:hover {
    color: #ffffff;
    background: #111827;
    transform: translateY(-2px);
  }
}

.footer-bottom {
  background: #000000;
  padding: 22px 20px;
  color: #cbd5e1;
  font-size: 12px;
}

.bottom-content {
  max-width: 1240px;
  margin: 0 auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.legal-note {
  margin: 0;
  color: #94a3b8;
}

.copyright {
  margin: 0;
  font-weight: 600;
  color: #e2e8f0;
}

@media (max-width: 1100px) {
  .footer-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 768px) {
  .footer-main {
    padding: 36px 16px;
  }

  .footer-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 18px;
  }

  .bottom-content {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
}

@media (max-width: 480px) {
  .footer-grid {
    grid-template-columns: 1fr;
  }
}
</style>

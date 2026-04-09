<template>
  <div class="product-list-page">
    <div class="container-fluid">
      <div class="list-controls">
        <div class="list-stats">
          <span v-if="!store.loading">{{ store.totalElements }} SẢN PHẨM</span>
          <span v-else>ĐANG TẢI...</span>
        </div>

        <div class="list-actions">
          <el-input
            v-model="searchKeyword"
            class="search-input"
            placeholder="Tìm theo tên sản phẩm"
            clearable
            @keyup.enter="applySearch"
            @clear="applySearch"
          />
          <el-button class="search-btn" @click="applySearch">TÌM</el-button>
          <el-select v-model="store.filters.sortBy" class="sort-select" @change="handleSortChange">
            <el-option label="MỚI NHẤT" value="id" />
            <el-option label="GIÁ" value="price" />
            <el-option label="TÊN" value="name" />
          </el-select>
          <el-select v-model="store.filters.direction" class="direction-select" @change="handleSortChange">
            <el-option label="GIẢM DẦN" value="desc" />
            <el-option label="TĂNG DẦN" value="asc" />
          </el-select>
          <el-input model-value="10 / trang" class="size-select" disabled />
        </div>
      </div>

      <div class="product-layout">
        <aside class="filter-sidebar">
          <div class="filter-group">
            <h4 class="filter-title">DANH MỤC</h4>
            <div class="filter-list">
              <div
                v-for="cat in categories"
                :key="cat.id"
                class="filter-item"
                :class="{ active: store.filters.category === cat.id }"
                @click="handleCategoryFilter(cat.id)"
              >
                {{ cat.name }}
              </div>
            </div>
          </div>
        </aside>

        <main class="product-main">
          <div v-if="store.loading" class="loading-grid">
            <div v-for="n in 8" :key="n" class="skeleton-card"></div>
          </div>

          <div v-else-if="store.products.length === 0" class="empty-state">
            <h3>KHÔNG TÌM THẤY SẢN PHẨM</h3>
            <p>Chúng tôi không tìm thấy kết quả phù hợp với lựa chọn của bạn. Vui lòng thử lại với các bộ lọc khác.</p>
            <el-button @click="resetFilters">XÓA TẤT CẢ BỘ LỌC</el-button>
          </div>

          <div v-else class="product-grid">
            <div v-for="product in store.products" :key="product.id" class="product-card-wrap">
              <button
                type="button"
                class="compare-toggle"
                :class="{ active: isCompared(product.id) }"
                @click.stop="toggleCompare(product)"
              >
                {{ isCompared(product.id) ? "Đã chọn so sánh" : "So sánh" }}
              </button>
              <ProductCard :product="product" />
            </div>
          </div>

          <div v-if="store.totalPages > 1" class="pagination-container">
            <el-pagination
              v-model:current-page="currentPage"
              :page-size="store.size"
              :total="store.totalElements"
              layout="prev, pager, next"
              @current-change="handlePageChange"
              background
            />
          </div>
        </main>
      </div>
    </div>

    <div v-if="comparedProducts.length" class="compare-bar">
      <div class="compare-content">
        <strong>So sánh sản phẩm ({{ comparedProducts.length }}/3)</strong>
        <div class="compare-items">
          <span v-for="item in comparedProducts" :key="`compare-${item.id}`" class="compare-tag">
            {{ item.name }}
            <button type="button" @click="removeCompare(item.id)">×</button>
          </span>
        </div>
      </div>
      <div class="compare-actions">
        <el-button @click="clearCompare">Xóa</el-button>
        <el-button type="primary" :disabled="comparedProducts.length < 2" @click="compareDialogVisible = true">Xem so sánh</el-button>
      </div>
    </div>

    <el-dialog v-model="compareDialogVisible" title="So sánh sản phẩm" width="860px">
      <div class="compare-table-wrap">
        <table class="compare-table">
          <thead>
            <tr>
              <th>Tiêu chí</th>
              <th v-for="item in comparedProducts" :key="`head-${item.id}`">{{ item.name }}</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>Danh mục</td>
              <td v-for="item in comparedProducts" :key="`cat-${item.id}`">{{ item.categoryName || "N/A" }}</td>
            </tr>
            <tr>
              <td>Thương hiệu</td>
              <td v-for="item in comparedProducts" :key="`brand-${item.id}`">{{ item.brand || "N/A" }}</td>
            </tr>
            <tr>
              <td>Giá từ</td>
              <td v-for="item in comparedProducts" :key="`price-${item.id}`">{{ formatPrice(minVariantPrice(item)) }}</td>
            </tr>
            <tr>
              <td>Màu sắc</td>
              <td v-for="item in comparedProducts" :key="`color-${item.id}`">{{ colorSummary(item) }}</td>
            </tr>
            <tr>
              <td>Kích cỡ</td>
              <td v-for="item in comparedProducts" :key="`size-${item.id}`">{{ sizeSummary(item) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import api from "@/services/api";
import ProductCard from "@/modules/product/components/ProductCard.vue";
import { useProductStore } from "@/modules/product/store/productStore";

const store = useProductStore();
const route = useRoute();
const router = useRouter();
const categories = ref([]);
const searchKeyword = ref("");
const compareDialogVisible = ref(false);

const COMPARE_KEY = "clothing_compare_products";
const MAX_COMPARE_COUNT = 3;

const parseCompareStorage = () => {
  try {
    const raw = localStorage.getItem(COMPARE_KEY);
    const parsed = JSON.parse(raw || "[]");
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
};

const comparedProducts = ref(parseCompareStorage());

const persistCompare = () => {
  localStorage.setItem(COMPARE_KEY, JSON.stringify(comparedProducts.value));
};

const currentPage = computed({
  get: () => store.page + 1,
  set: (val) => {
    store.page = val - 1;
  }
});

const handlePageChange = (val) => {
  store.setPage(val - 1);
  window.scrollTo({ top: 0, behavior: "smooth" });
};

const handleSortChange = () => {
  store.page = 0;
  store.fetchProducts();
};

const handleCategoryFilter = (catId) => {
  const nextCategory = store.filters.category === catId ? null : catId;
  const nextQuery = { ...route.query };
  if (nextCategory == null) {
    delete nextQuery.category;
  } else {
    nextQuery.category = String(nextCategory);
  }
  router.push({ path: "/products", query: nextQuery });
};

const applySearch = () => {
  const nextQuery = { ...route.query };
  const term = searchKeyword.value.trim();
  if (term) {
    nextQuery.q = term;
  } else {
    delete nextQuery.q;
  }
  router.push({ path: "/products", query: nextQuery });
};

const resetFilters = () => {
  store.filters.sortBy = "id";
  store.filters.direction = "desc";
  store.page = 0;
  searchKeyword.value = "";
  router.push({ path: "/products", query: {} });
};

const fetchCategories = async () => {
  try {
    const response = await api.get("/categories", { params: { size: 50 } });
    categories.value = response.data.content || [];
  } catch (error) {
    console.error("Failed to fetch categories:", error);
  }
};

const syncFiltersFromRoute = () => {
  const categoryQuery = Array.isArray(route.query.category) ? route.query.category[0] : route.query.category;
  const qQuery = Array.isArray(route.query.q) ? route.query.q[0] : route.query.q;

  const hasCategory = typeof categoryQuery === "string" && categoryQuery.trim() !== "";
  const parsedCategory = hasCategory ? Number(categoryQuery) : NaN;
  store.filters.category = Number.isFinite(parsedCategory) ? parsedCategory : null;
  store.filters.q = typeof qQuery === "string" ? qQuery.trim() : "";
  searchKeyword.value = store.filters.q;
  store.page = 0;
  store.fetchProducts();
};

const isCompared = (id) => comparedProducts.value.some((item) => item.id === id);

const toCompareSummary = (product) => ({
  id: product.id,
  name: product.name || "Sản phẩm",
  brand: product.brand || "",
  categoryName: product.categoryName || "",
  variants: Array.isArray(product.variants) ? product.variants : []
});

const toggleCompare = (product) => {
  if (isCompared(product.id)) {
    comparedProducts.value = comparedProducts.value.filter((item) => item.id !== product.id);
    persistCompare();
    return;
  }
  if (comparedProducts.value.length >= MAX_COMPARE_COUNT) {
    ElMessage.warning("Bạn chỉ có thể so sánh tối đa 3 sản phẩm");
    return;
  }
  comparedProducts.value = [...comparedProducts.value, toCompareSummary(product)];
  persistCompare();
};

const removeCompare = (id) => {
  comparedProducts.value = comparedProducts.value.filter((item) => item.id !== id);
  persistCompare();
};

const clearCompare = () => {
  comparedProducts.value = [];
  persistCompare();
};

const minVariantPrice = (product) => {
  const prices = (product.variants || []).map((variant) => Number(variant?.price)).filter((value) => Number.isFinite(value));
  if (!prices.length) return 0;
  return Math.min(...prices);
};

const uniqueSummary = (values) => {
  const normalized = [...new Set(values.filter(Boolean).map((item) => String(item).trim()).filter(Boolean))];
  if (!normalized.length) return "N/A";
  if (normalized.length <= 4) return normalized.join(", ");
  return `${normalized.slice(0, 4).join(", ")} +${normalized.length - 4}`;
};

const colorSummary = (product) => uniqueSummary((product.variants || []).map((item) => item.color));
const sizeSummary = (product) => uniqueSummary((product.variants || []).map((item) => item.size));
const formatPrice = (value) => new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(value || 0);

watch(() => route.query, syncFiltersFromRoute, { immediate: true });

onMounted(() => {
  fetchCategories();
});
</script>

<style scoped lang="scss">
.product-list-page {
  padding: 40px 0;
  min-height: 100vh;
}

.container-fluid {
  max-width: 1600px;
  margin: 0 auto;
  padding: 0 40px;
}

.list-controls {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
  border-bottom: 1px solid #ebedee;
  padding-bottom: 20px;

  .list-stats {
    font-weight: 800;
    font-size: 14px;
    letter-spacing: 0.5px;
  }

  .list-actions {
    display: flex;
    align-items: center;
    gap: 10px;
  }

  .search-input {
    width: 220px;
  }

  .search-btn {
    border-radius: 0;
    font-weight: 700;
  }

  .sort-select,
  .direction-select,
  .size-select {
    :deep(.el-input__wrapper) {
      border-radius: 0;
      box-shadow: none;
      border: 1px solid #ebedee;
    }
  }

  .sort-select {
    width: 150px;
  }

  .direction-select {
    width: 150px;
  }

  .size-select {
    width: 120px;
  }
}

.product-layout {
  display: grid;
  grid-template-columns: 260px 1fr;
  gap: 40px;
}

.filter-sidebar {
  .filter-group {
    margin-bottom: 40px;

    .filter-title {
      font-size: 14px;
      font-weight: 800;
      margin-bottom: 20px;
      letter-spacing: 1px;
    }

    .filter-list {
      display: flex;
      flex-direction: column;
      gap: 12px;

      .filter-item {
        font-size: 13px;
        color: #333;
        cursor: pointer;
        padding: 4px 0;
        transition: all 0.2s;

        &:hover {
          text-decoration: underline;
        }

        &.active {
          font-weight: 900;
          text-decoration: underline;
        }
      }
    }
  }
}

.product-main {
  .product-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 15px;
    row-gap: 40px;
  }

  .product-card-wrap {
    position: relative;
  }

  .compare-toggle {
    position: absolute;
    z-index: 4;
    left: 12px;
    bottom: 16px;
    border: 1px solid #d1d5db;
    background: rgba(255, 255, 255, 0.94);
    color: #0f172a;
    font-size: 11px;
    font-weight: 700;
    padding: 6px 10px;
    cursor: pointer;
    transition: all 0.2s ease;

    &:hover {
      border-color: #000;
    }

    &.active {
      background: #000;
      color: #fff;
      border-color: #000;
    }
  }

  @media (max-width: 1400px) {
    .product-grid {
      grid-template-columns: repeat(3, 1fr);
    }
  }

  @media (max-width: 1100px) {
    .product-grid {
      grid-template-columns: repeat(2, 1fr);
    }
  }

  @media (max-width: 768px) {
    .product-grid {
      grid-template-columns: 1fr;
    }
  }

  .pagination-container {
    margin-top: 80px;
    display: flex;
    justify-content: center;
    border-top: 1px solid #ebedee;
    padding-top: 40px;

    :deep(.el-pagination) {
      .btn-next,
      .btn-prev,
      .el-pager li {
        background-color: transparent;
        border: 1px solid #ebedee;
        border-radius: 0;
        height: 40px;
        width: 40px;
        font-weight: 700;

        &.is-active {
          background-color: #000 !important;
          color: #fff !important;
          border-color: #000;
        }
      }
    }
  }
}

.loading-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 15px;

  .skeleton-card {
    height: 400px;
    background: #f5f5f5;
    position: relative;
    overflow: hidden;

    &::after {
      content: "";
      position: absolute;
      top: 0;
      right: 0;
      bottom: 0;
      left: 0;
      background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.4), transparent);
      animation: shimmer 1.5s infinite;
    }
  }
}

@keyframes shimmer {
  0% {
    transform: translateX(-100%);
  }

  100% {
    transform: translateX(100%);
  }
}

.empty-state {
  text-align: center;
  padding: 100px 0;

  h3 {
    font-weight: 900;
    margin-bottom: 15px;
  }

  p {
    color: #666;
    margin-bottom: 30px;
  }

  .el-button {
    border-radius: 0;
    background: #000;
    color: #fff;
    border: none;
    font-weight: 800;
    height: 50px;
    padding: 0 30px;
  }
}

.compare-bar {
  position: sticky;
  bottom: 0;
  z-index: 20;
  background: #0f172a;
  color: #f8fafc;
  padding: 12px 20px;
  margin-top: 28px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.compare-content {
  display: flex;
  align-items: center;
  gap: 14px;
}

.compare-items {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.compare-tag {
  background: rgba(148, 163, 184, 0.18);
  border: 1px solid rgba(148, 163, 184, 0.3);
  padding: 4px 8px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;

  button {
    border: none;
    background: transparent;
    color: #fff;
    cursor: pointer;
    font-size: 14px;
    line-height: 1;
  }
}

.compare-actions {
  display: flex;
  gap: 8px;
}

.compare-table-wrap {
  overflow-x: auto;
}

.compare-table {
  width: 100%;
  border-collapse: collapse;

  th,
  td {
    border: 1px solid #e2e8f0;
    padding: 10px 12px;
    text-align: left;
    vertical-align: top;
    font-size: 13px;
  }

  th {
    font-size: 12px;
    text-transform: uppercase;
    letter-spacing: 0.5px;
    background: #f8fafc;
    font-weight: 800;
  }
}

@media (max-width: 900px) {
  .container-fluid {
    padding: 0 16px;
  }

  .list-controls {
    align-items: flex-start;
    flex-direction: column;
    gap: 12px;
  }

  .list-controls .list-actions {
    width: 100%;
    flex-wrap: wrap;
  }

  .list-controls .search-input {
    width: 100%;
  }

  .sort-select,
  .direction-select,
  .size-select {
    width: 100% !important;
    max-width: 320px;
  }

  .product-layout {
    grid-template-columns: 1fr;
  }

  .filter-sidebar {
    display: none;
  }

  .compare-bar {
    flex-direction: column;
    align-items: flex-start;
  }

  .compare-content {
    flex-direction: column;
    align-items: flex-start;
  }
}

@media (max-width: 640px) {
  .product-list-page {
    padding: 24px 0;
  }

  .container-fluid {
    padding: 0 12px;
  }

  .product-main .product-grid {
    row-gap: 20px;
  }

  .loading-grid {
    grid-template-columns: 1fr;
  }

  .loading-grid .skeleton-card {
    height: 320px;
  }

  .empty-state {
    padding: 64px 0;
  }
}
</style>

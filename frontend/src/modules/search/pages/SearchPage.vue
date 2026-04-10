<template>
  <BasePage
    class="client-page-shell"
    title="Tìm kiếm sản phẩm"
    subtitle="Tìm nhanh sản phẩm theo tên, thương hiệu hoặc danh mục."
  >
    <BaseCard class="search-page-card">
      <div class="search-toolbar">
        <BaseInput
          v-model="keyword"
          placeholder="Nhập từ khoá tìm kiếm..."
          clearable
          @enter="onSearch"
        />
        <BaseButton :icon="Search" :loading="searching" @click="onSearch">
          Tìm kiếm
        </BaseButton>
      </div>

      <el-empty
        v-if="!searching && !store.results.length && hasSearched"
        description="Không tìm thấy sản phẩm phù hợp"
      />

      <div v-else-if="store.results.length" class="search-result">
        <div class="search-result__meta">
          <el-tag type="info" effect="plain">{{ store.results.length }} kết quả</el-tag>
        </div>
        <BaseTable :data="store.results" border stripe>
          <el-table-column prop="name" label="Sản phẩm" min-width="220" />
          <el-table-column prop="categoryName" label="Danh mục" min-width="180" />
          <el-table-column label="Giá thấp nhất" min-width="160">
            <template #default="{ row }">
              {{ formatCurrency(row.minPrice) }}
            </template>
          </el-table-column>
          <el-table-column label="Chi tiết" width="120" align="center">
            <template #default="{ row }">
              <router-link :to="`/products/${row.slug || row.id}`">
                <BaseButton type="primary" link>Xem</BaseButton>
              </router-link>
            </template>
          </el-table-column>
        </BaseTable>
      </div>
    </BaseCard>
  </BasePage>
</template>

<script setup>
import { ref } from "vue";
import { Search } from "@element-plus/icons-vue";
import BaseButton from "@/components/base/BaseButton.vue";
import BaseInput from "@/components/base/BaseInput.vue";
import BaseCard from "@/components/base/BaseCard.vue";
import BasePage from "@/components/base/BasePage.vue";
import { useSearchStore } from "@/modules/search/store/searchStore";

const keyword = ref("");
const searching = ref(false);
const hasSearched = ref(false);
const store = useSearchStore();

async function onSearch() {
  searching.value = true;
  try {
    await store.search(keyword.value);
    hasSearched.value = true;
  } finally {
    searching.value = false;
  }
}

function formatCurrency(value) {
  const amount = Number(value || 0);
  if (!Number.isFinite(amount) || amount <= 0) return "Liên hệ";
  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND"
  }).format(amount);
}
</script>

<style scoped lang="scss">
.search-page-card {
  :deep(.el-card__body) {
    display: flex;
    flex-direction: column;
    gap: 16px;
  }
}

.search-toolbar {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 10px;
}

.search-result__meta {
  margin-bottom: 12px;
}

@media (max-width: 768px) {
  .search-toolbar {
    grid-template-columns: 1fr;
  }
}
</style>

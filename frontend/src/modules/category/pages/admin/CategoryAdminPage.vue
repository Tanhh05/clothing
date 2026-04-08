<template>
  <section class="category-admin-page" v-loading="loading">
    <header class="management-head">
      <div>
        <p class="eyebrow">Admin panel</p>
        <h2>Category Management</h2>
        <p class="sub-text">Quản lý danh mục, danh mục cha/con và cấu trúc hiển thị sản phẩm.</p>
      </div>
      <div class="head-actions">
        <el-button @click="loadCategories">Làm mới</el-button>
        <el-button type="primary" @click="openCreate">+ Thêm danh mục</el-button>
      </div>
    </header>

    <div class="overview-grid">
      <article class="overview-card">
        <p class="label">Total categories</p>
        <p class="value">{{ stats.total }}</p>
        <p class="helper">Tổng số danh mục</p>
      </article>
      <article class="overview-card">
        <p class="label">Root categories</p>
        <p class="value">{{ stats.root }}</p>
        <p class="helper">Danh mục cấp cha</p>
      </article>
      <article class="overview-card">
        <p class="label">Child categories</p>
        <p class="value">{{ stats.child }}</p>
        <p class="helper">Danh mục có cha</p>
      </article>
    </div>

    <div class="inventory-panel">
      <div class="panel-header">
        <div>
          <h3>Danh sách danh mục</h3>
          <p>{{ filteredCategories.length }} danh mục phù hợp</p>
        </div>

        <div class="panel-actions">
          <el-input
            v-model="keyword"
            placeholder="Tìm theo tên / slug"
            clearable
            class="search-input"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </div>
      </div>

      <div class="table-wrap">
        <el-table
          :data="filteredCategories"
          border
          stripe
          class="category-table"
          empty-text="Không có danh mục"
          table-layout="fixed"
        >
          <el-table-column prop="id" label="#" width="80" />
          <el-table-column prop="name" label="Tên danh mục" min-width="220" show-overflow-tooltip />
          <el-table-column prop="slug" label="Slug" min-width="220" show-overflow-tooltip />
          <el-table-column label="Danh mục cha" min-width="220" show-overflow-tooltip>
            <template #default="{ row }">
              {{ parentName(row.parentId) }}
            </template>
          </el-table-column>
          <el-table-column label="Thao tác" width="170" fixed="right">
            <template #default="{ row }">
              <el-button size="small" @click="openEdit(row)">Sửa</el-button>
              <el-button size="small" type="danger" plain @click="deleteCategory(row)">Xóa</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <div class="pagination-wrap">
        <el-pagination
          layout="total, prev, pager, next"
          :total="totalElements"
          :current-page="page + 1"
          :page-size="size"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <el-drawer
      v-model="drawerVisible"
      :title="drawerMode === 'create' ? 'Thêm danh mục' : 'Cập nhật danh mục'"
      direction="rtl"
      size="34%"
      class="category-form-drawer"
    >
      <el-form :model="categoryForm" label-position="top">
        <el-form-item label="Tên danh mục">
          <el-input v-model="categoryForm.name" />
        </el-form-item>
        <el-form-item label="Slug (tùy chọn)">
          <el-input v-model="categoryForm.slug" placeholder="bo-trong-de-tu-sinh" />
        </el-form-item>
        <el-form-item label="Danh mục cha (tùy chọn)">
          <el-select
            v-model="categoryForm.parentId"
            style="width: 100%"
            clearable
            placeholder="Chọn danh mục cha"
          >
            <el-option
              v-for="item in parentOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="drawerVisible = false">Hủy</el-button>
        <el-button type="primary" :loading="submitting" @click="submitCategory">
          {{ drawerMode === "create" ? "Tạo danh mục" : "Lưu thay đổi" }}
        </el-button>
      </template>
    </el-drawer>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { Search } from "@element-plus/icons-vue";
import { categoryApi } from "@/modules/category/api/categoryApi";

const loading = ref(false);
const submitting = ref(false);
const categories = ref([]);
const keyword = ref("");
const page = ref(0);
const size = ref(20);
const totalElements = ref(0);

const drawerVisible = ref(false);
const drawerMode = ref("create");
const editingId = ref(null);
const categoryForm = ref({
  name: "",
  slug: "",
  parentId: null
});

const idNameMap = computed(() => {
  const map = new Map();
  categories.value.forEach((item) => map.set(item.id, item.name));
  return map;
});

const parentName = (parentId) => {
  if (!parentId) return "Không có";
  return idNameMap.value.get(parentId) || `#${parentId}`;
};

const parentOptions = computed(() => {
  if (!editingId.value) return categories.value;
  return categories.value.filter((item) => item.id !== editingId.value);
});

const filteredCategories = computed(() => {
  const q = keyword.value.trim().toLowerCase();
  if (!q) return categories.value;

  return categories.value.filter((item) => {
    const name = String(item?.name || "").toLowerCase();
    const slug = String(item?.slug || "").toLowerCase();
    return name.includes(q) || slug.includes(q);
  });
});

const stats = computed(() => {
  const total = categories.value.length;
  const root = categories.value.filter((item) => !item.parentId).length;
  const child = total - root;
  return { total, root, child };
});

const loadCategories = async () => {
  loading.value = true;
  try {
    const { data } = await categoryApi.getCategories({
      page: page.value,
      size: size.value,
      sortBy: "id",
      direction: "desc"
    });
    categories.value = data?.content || [];
    totalElements.value = Number(data?.totalElements || 0);
  } catch (error) {
    console.error(error);
    ElMessage.error("Không tải được danh mục");
  } finally {
    loading.value = false;
  }
};

const openCreate = () => {
  drawerMode.value = "create";
  editingId.value = null;
  categoryForm.value = {
    name: "",
    slug: "",
    parentId: null
  };
  drawerVisible.value = true;
};

const openEdit = (item) => {
  drawerMode.value = "edit";
  editingId.value = item.id;
  categoryForm.value = {
    name: item.name || "",
    slug: item.slug || "",
    parentId: item.parentId || null
  };
  drawerVisible.value = true;
};

const submitCategory = async () => {
  const payload = {
    name: categoryForm.value.name?.trim() || "",
    slug: categoryForm.value.slug?.trim() || null,
    parentId: categoryForm.value.parentId || null
  };

  if (!payload.name) {
    ElMessage.warning("Vui lòng nhập tên danh mục");
    return;
  }

  submitting.value = true;
  try {
    if (drawerMode.value === "create") {
      await categoryApi.createCategory(payload);
      ElMessage.success("Tạo danh mục thành công");
    } else if (editingId.value) {
      await categoryApi.updateCategory(editingId.value, payload);
      ElMessage.success("Cập nhật danh mục thành công");
    }
    drawerVisible.value = false;
    await loadCategories();
  } catch (error) {
    console.error(error);
    ElMessage.error(error?.response?.data?.message || "Lưu danh mục thất bại");
  } finally {
    submitting.value = false;
  }
};

const deleteCategory = async (item) => {
  try {
    await ElMessageBox.confirm(`Xóa danh mục "${item.name}"?`, "Xác nhận", {
      type: "warning",
      confirmButtonText: "Xóa",
      cancelButtonText: "Hủy"
    });

    await categoryApi.deleteCategory(item.id);
    ElMessage.success("Đã xóa danh mục");
    await loadCategories();
  } catch (error) {
    if (error !== "cancel") {
      console.error(error);
      ElMessage.error(error?.response?.data?.message || "Xóa danh mục thất bại");
    }
  }
};

const handlePageChange = (nextPage) => {
  page.value = Math.max(0, nextPage - 1);
  loadCategories();
};

onMounted(() => {
  loadCategories();
});
</script>

<style scoped lang="scss">
.category-admin-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  width: 100%;
  box-sizing: border-box;
}

.management-head {
  padding: 16px;
  border: 1px solid #dce1e7;
  background: #fbfbfc;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;

  h2 {
    margin: 0;
    font-size: 24px;
    font-weight: 800;
    letter-spacing: 0.3px;
    text-transform: uppercase;
  }

  .eyebrow {
    margin: 0 0 6px;
    text-transform: uppercase;
    letter-spacing: 1px;
    font-size: 11px;
    color: #6b7280;
  }

  .sub-text {
    margin: 6px 0 0;
    color: #6b7280;
    font-size: 13px;
  }
}

.head-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.overview-card {
  border: 1px solid #e2e7ed;
  padding: 16px;
  background: #fff;

  .label {
    margin: 0 0 6px;
    color: #6b7280;
    font-size: 12px;
    text-transform: uppercase;
    letter-spacing: 0.5px;
  }

  .value {
    margin: 0;
    font-size: 30px;
    font-weight: 800;
    color: #111827;
  }

  .helper {
    margin: 8px 0 0;
    color: #9aa2ac;
    font-size: 12px;
  }
}

.inventory-panel {
  border: 1px solid #dce1e7;
  padding: 16px;
  background: #fff;
}

.panel-header {
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;

  h3 {
    margin: 0;
    font-size: 18px;
    font-weight: 800;
  }

  p {
    margin: 4px 0 0;
    color: #6b7280;
    font-size: 12px;
  }
}

.panel-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.search-input {
  width: 280px;
}

.category-table {
  width: 100%;
  min-width: 900px;
}

.table-wrap {
  width: 100%;
  overflow-x: auto;
}

.pagination-wrap {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
}

.category-form-drawer {
  :deep(.el-drawer__body) {
    padding: 16px;
    overflow-y: auto;
  }
}

@media (max-width: 1000px) {
  .overview-grid {
    grid-template-columns: 1fr;
  }

  .management-head {
    flex-direction: column;
    align-items: flex-start;
  }
}

@media (max-width: 700px) {
  .panel-header {
    align-items: flex-start;
    flex-direction: column;
    gap: 10px;
  }

  .panel-actions {
    width: 100%;
    flex-direction: column;
    align-items: stretch;
  }

  .search-input {
    width: 100%;
  }

  .category-form-drawer {
    :deep(.el-drawer) {
      width: 100% !important;
      max-width: 100% !important;
    }
  }
}
</style>

<template>
  <section class="category-admin-page admin-page-shell" v-loading="loading">
    <div class="inventory-panel">
      <div class="panel-header">
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
          <el-button @click="loadCategories">Làm mới</el-button>
          <el-button type="primary" @click="openCreate">+ Thêm danh mục</el-button>
        </div>
      </div>

      <div class="table-wrap">
        <BaseTable
          :data="filteredCategories"
          border
          stripe
          size="small"
          class="category-table"
          empty-text="Không có danh mục"
          table-layout="fixed"
        >
          <el-table-column prop="id" label="#" width="80" />
          <el-table-column prop="name" label="Tên danh mục" min-width="220" show-overflow-tooltip />
          <el-table-column prop="slug" label="Slug" min-width="220" show-overflow-tooltip />
          <el-table-column label="Điều hướng menu" width="140">
            <template #default="{ row }">
              <el-tag :type="row.showInMenu ? 'success' : 'info'" size="small">
                {{ row.showInMenu ? "Hiển thị" : "Ẩn" }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="Trạng thái" width="120">
            <template #default="{ row }">
              <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'" size="small">
                {{ row.status === "ACTIVE" ? "Active" : "Inactive" }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="displayOrder" label="Thứ tự" width="90" />
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
        </BaseTable>
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
      v-model="dialogVisible"
      :title="drawerMode === 'create' ? 'Thêm danh mục' : 'Cập nhật danh mục'"
      direction="rtl"
      size="42%"
      class="category-form-drawer"
      :close-on-click-modal="false"
    >
      <el-form :model="categoryForm" label-position="top" class="category-form-grid">
        <el-row :gutter="14">
          <el-col :xs="24" :md="12">
            <el-form-item label="Tiêu đề (Tên menu hoặc danh mục)">
              <el-input v-model="categoryForm.name" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="Tiêu đề phụ">
              <el-input v-model="categoryForm.subtitle" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="Tiêu đề URL (bắt buộc)">
              <el-input v-model="categoryForm.slug" placeholder="gioi-thieu" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="Trang">
              <el-select v-model="categoryForm.pageType" style="width: 100%">
                <el-option label="Trang đơn" value="TRANG_DON" />
                <el-option label="Trang blog" value="TRANG_BLOG" />
                <el-option label="Trang chủ" value="TRANG_CHU" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="Loại menu">
              <el-radio-group v-model="categoryForm.menuType" @change="handleMenuTypeChange">
                <el-radio-button label="parent">Menu cha</el-radio-button>
                <el-radio-button label="child">Menu con</el-radio-button>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="Menu cha" :required="categoryForm.menuType === 'child'">
              <el-select
                v-model="categoryForm.parentId"
                style="width: 100%"
                clearable
                filterable
                placeholder="Chọn menu cha"
                :disabled="categoryForm.menuType !== 'child'"
              >
                <el-option
                  v-for="item in parentOptions"
                  :key="item.id"
                  :label="item.name"
                  :value="item.id"
                />
              </el-select>
              <p v-if="categoryForm.menuType === 'child' && !parentOptions.length" class="upload-tip">
                Chưa có menu cha nào. Hãy tạo menu cha trước.
              </p>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="Thứ tự">
              <el-input-number v-model="categoryForm.displayOrder" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="Trạng thái">
              <el-select v-model="categoryForm.status" style="width: 100%">
                <el-option label="Active" value="ACTIVE" />
                <el-option label="Inactive" value="INACTIVE" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="Nội dung ngắn">
              <el-input v-model="categoryForm.shortContent" type="textarea" :rows="3" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="Hình ảnh">
              <el-upload
                v-model:file-list="categoryForm.imageUploadFiles"
                list-type="picture-card"
                :auto-upload="false"
                :limit="1"
                accept="image/*"
                :on-change="syncImageFromUploadList"
                :on-remove="syncImageFromUploadList"
              >
                <el-icon><Plus /></el-icon>
              </el-upload>
              <p class="upload-tip">Chọn 1 ảnh từ máy. Ảnh sẽ được upload khi lưu danh mục.</p>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item class="checkbox-field">
              <el-checkbox v-model="categoryForm.showInMenu">
                Điều hướng menu (hiển thị ở đầu trang)
              </el-checkbox>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">Hủy</el-button>
          <el-button type="primary" :loading="submitting" @click="submitCategory">
            {{ drawerMode === "create" ? "Tạo danh mục" : "Lưu thay đổi" }}
          </el-button>
        </div>
      </template>
    </el-drawer>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { ElMessage } from "@/utils/dialogMessage";
import { useConfirmDialog } from "@/composables/useConfirmDialog";
import { Search, Plus } from "@element-plus/icons-vue";
import { categoryApi } from "@/modules/category/api/categoryApi";
import { uploadApi } from "@/modules/upload/api/uploadApi";

const { confirm } = useConfirmDialog();

const loading = ref(false);
const submitting = ref(false);
const categories = ref([]);
const keyword = ref("");
const page = ref(0);
const size = ref(10);
const totalElements = ref(0);

const dialogVisible = ref(false);
const drawerMode = ref("create");
const editingId = ref(null);
const categoryForm = ref({
  name: "",
  slug: "",
  imageUrl: "",
  imageUploadFiles: [],
  subtitle: "",
  pageType: "TRANG_DON",
  menuType: "parent",
  shortContent: "",
  displayOrder: 0,
  showInMenu: false,
  status: "ACTIVE",
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
    const subtitle = String(item?.subtitle || "").toLowerCase();
    return name.includes(q) || slug.includes(q) || subtitle.includes(q);
  });
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
    imageUrl: "",
    imageUploadFiles: [],
    subtitle: "",
    pageType: "TRANG_DON",
    menuType: "parent",
    shortContent: "",
    displayOrder: 0,
    showInMenu: false,
    status: "ACTIVE",
    parentId: null
  };
  dialogVisible.value = true;
};

const openEdit = (item) => {
  drawerMode.value = "edit";
  editingId.value = item.id;
  categoryForm.value = {
    name: item.name || "",
    slug: item.slug || "",
    imageUrl: item.imageUrl || "",
    imageUploadFiles: item.imageUrl
      ? [{ name: "category-image", url: item.imageUrl }]
      : [],
    subtitle: item.subtitle || "",
    pageType: item.pageType || "TRANG_DON",
    menuType: item.parentId ? "child" : "parent",
    shortContent: item.shortContent || "",
    displayOrder: Number(item.displayOrder || 0),
    showInMenu: Boolean(item.showInMenu),
    status: item.status || "ACTIVE",
    parentId: item.parentId || null
  };
  dialogVisible.value = true;
};

const submitCategory = async () => {
  let resolvedImageUrl = categoryForm.value.imageUrl?.trim() || null;
  const uploadFiles = Array.isArray(categoryForm.value.imageUploadFiles)
    ? categoryForm.value.imageUploadFiles
    : [];
  const rawImage = uploadFiles.find((file) => file?.raw instanceof File)?.raw || null;
  if (rawImage) {
    try {
      resolvedImageUrl = await uploadApi.uploadPublicFile(rawImage, "categories");
    } catch (error) {
      console.error(error);
      ElMessage.error(error?.message || "Upload ảnh thất bại");
      return;
    }
  } else if (!uploadFiles.length) {
    resolvedImageUrl = null;
  }

  const payload = {
    name: categoryForm.value.name?.trim() || "",
    slug: categoryForm.value.slug?.trim() || "",
    imageUrl: resolvedImageUrl,
    subtitle: categoryForm.value.subtitle?.trim() || null,
    pageType: categoryForm.value.pageType || "TRANG_DON",
    shortContent: categoryForm.value.shortContent?.trim() || null,
    displayOrder: Number(categoryForm.value.displayOrder || 0),
    showInMenu: Boolean(categoryForm.value.showInMenu),
    status: categoryForm.value.status || "ACTIVE",
    parentId: categoryForm.value.menuType === "child" ? (categoryForm.value.parentId || null) : null
  };

  if (!payload.name) {
    ElMessage.warning("Vui lòng nhập tên danh mục");
    return;
  }
  if (!payload.slug) {
    ElMessage.warning("Vui lòng nhập tiêu đề URL");
    return;
  }
  if (categoryForm.value.menuType === "child" && !payload.parentId) {
    ElMessage.warning("Vui lòng chọn menu cha cho menu con");
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
    dialogVisible.value = false;
    await loadCategories();
  } catch (error) {
    console.error(error);
    ElMessage.error(error?.response?.data?.message || "Lưu danh mục thất bại");
  } finally {
    submitting.value = false;
  }
};

const syncImageFromUploadList = () => {
  const list = Array.isArray(categoryForm.value.imageUploadFiles) ? categoryForm.value.imageUploadFiles : [];
  if (!list.length) {
    categoryForm.value.imageUrl = "";
    return;
  }
  const first = list[0];
  if (first?.url && !(first?.raw instanceof File)) {
    categoryForm.value.imageUrl = first.url;
  }
};

const handleMenuTypeChange = (nextType) => {
  if (nextType === "parent") {
    categoryForm.value.parentId = null;
  }
};

const deleteCategory = async (item) => {
  try {
    await confirm({
      title: "Xác nhận",
      message: `Xóa danh mục "${item.name}"?`,
      confirmButtonText: "Xóa",
      cancelButtonText: "Hủy",
      onConfirm: async () => {
        await categoryApi.deleteCategory(item.id);
        ElMessage.success("Đã xóa danh mục");
        await loadCategories();
      }
    });
  } catch (error) {
    if (error.message !== "cancel") {
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
  gap: 10px;
  width: 100%;
  box-sizing: border-box;
}

.inventory-panel {
  border: 1px solid #dce1e7;
  padding: 10px;
  background: #fff;
}

.panel-header { margin-bottom: 10px; }

.panel-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.search-input {
  width: 280px;
}

.category-table {
  width: 100%;
  min-width: 1200px;
}

.upload-tip {
  margin: 6px 0 0;
  color: #6b7280;
  font-size: 12px;
}

.table-wrap {
  width: 100%;
  overflow-x: auto;
}

.pagination-wrap {
  margin-top: 10px;
  display: flex;
  justify-content: flex-end;
}

.category-form-grid {
  margin-top: 6px;
}

.checkbox-field {
  margin-bottom: 0;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.category-form-drawer {
  :deep(.el-drawer__header) {
    margin-bottom: 0;
    padding: 18px 20px 12px;
    border-bottom: 1px solid #eef1f5;
  }

  :deep(.el-drawer__body) {
    padding: 16px 20px 8px;
    overflow-y: auto;
  }

  :deep(.el-drawer__footer) {
    padding: 12px 20px 18px;
    border-top: 1px solid #eef1f5;
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

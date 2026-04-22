<template>
  <section class="banner-admin-page admin-page-shell" v-loading="loading">
    <div class="panel">
      <div class="panel-head">
        <el-input
          v-model="keyword"
          clearable
          placeholder="Tìm theo tiêu đề / trạng thái"
          class="search-input"
        />
        <el-button @click="fetchBanners">Làm mới</el-button>
        <el-button type="primary" @click="openCreate">+ Thêm banner</el-button>
      </div>
      <div class="table-wrap">
        <BaseTable :data="pagedBanners" border stripe size="small" table-layout="fixed" class="banner-table" empty-text="Không có banner">
          <el-table-column prop="id" label="#" width="70" />
          <el-table-column label="Desktop" width="120">
            <template #default="{ row }">
              <img :src="row.imageUrl" alt="banner" class="thumb" />
            </template>
          </el-table-column>
          <el-table-column prop="title" label="Tiêu đề" min-width="170" show-overflow-tooltip />
          <el-table-column label="Base" width="100">
            <template #default="{ row }">
              <el-tag :type="row.baseStatus === 'ACTIVE' ? 'success' : 'info'">{{ row.baseStatus }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="Display" width="120">
            <template #default="{ row }">
              <el-tag :type="displayTagType(row.status)">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="Lịch chạy" min-width="200">
            <template #default="{ row }">
              <span>{{ formatSchedule(row.startAt, row.endAt) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="Thao tác" width="170" fixed="right">
            <template #default="{ row }">
              <el-button size="small" @click="openEdit(row)">Sửa</el-button>
              <el-button size="small" type="danger" plain @click="deleteBanner(row)">Xóa</el-button>
            </template>
          </el-table-column>
        </BaseTable>
      </div>
      <div class="pagination-wrap">
        <el-pagination
          layout="total, prev, pager, next"
          :total="filteredBanners.length"
          :current-page="page + 1"
          :page-size="size"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <el-drawer
      v-model="drawerVisible"
      :title="drawerMode === 'create' ? 'Thêm banner' : 'Cập nhật banner'"
      direction="rtl"
      size="38%"
      class="banner-form-drawer"
    >
      <el-form :model="bannerForm" label-position="top">
        <el-form-item label="Tiêu đề">
          <el-input v-model="bannerForm.title" />
        </el-form-item>
        <el-form-item label="Base status">
          <el-select v-model="bannerForm.baseStatus" style="width: 100%">
            <el-option label="ACTIVE" value="ACTIVE" />
            <el-option label="DRAFT" value="DRAFT" />
          </el-select>
        </el-form-item>
        <el-row :gutter="10">
          <el-col :span="12">
            <el-form-item label="Start at">
              <el-date-picker
                v-model="bannerForm.startAt"
                type="datetime"
                value-format="YYYY-MM-DDTHH:mm:ss"
                format="DD/MM/YYYY HH:mm"
                placeholder="Bắt đầu hiển thị"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="End at">
              <el-date-picker
                v-model="bannerForm.endAt"
                type="datetime"
                value-format="YYYY-MM-DDTHH:mm:ss"
                format="DD/MM/YYYY HH:mm"
                placeholder="Kết thúc hiển thị"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="Ảnh desktop banner">
          <el-upload
            v-model:file-list="bannerForm.desktopUploadFiles"
            list-type="picture-card"
            :auto-upload="false"
            :limit="12"
            multiple
            accept="image/*"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
          <p class="upload-tip">Desktop: chọn 1-12 ảnh. Từ 2 ảnh trở lên tự ghép thành 1 banner ngang.</p>
        </el-form-item>
        <el-row :gutter="10" class="preview-row">
          <el-col :span="24">
            <div class="preview-box">
              <p class="preview-label">Preview desktop</p>
              <img v-if="desktopPreviewUrl" :src="desktopPreviewUrl" alt="desktop preview" class="preview-img desktop" />
              <div v-else class="preview-empty">Chưa có ảnh</div>
            </div>
          </el-col>
        </el-row>
      </el-form>

      <template #footer>
        <el-button @click="drawerVisible = false">Hủy</el-button>
        <el-button type="primary" :loading="submitting" @click="submitBanner">
          {{ drawerMode === "create" ? "Tạo banner" : "Lưu thay đổi" }}
        </el-button>
      </template>
    </el-drawer>
  </section>
</template>

<script setup>
import { computed, onMounted, ref, watch } from "vue";
import { ElMessage } from "@/utils/dialogMessage";
import { useConfirmDialog } from "@/composables/useConfirmDialog";
import { Plus } from "@element-plus/icons-vue";
import { bannerApi } from "@/modules/banner/api/bannerApi";

const { confirm } = useConfirmDialog();

const loading = ref(false);
const submitting = ref(false);
const banners = ref([]);
const keyword = ref("");
const page = ref(0);
const size = ref(10);

const drawerVisible = ref(false);
const drawerMode = ref("create");
const editingId = ref(null);
const bannerForm = ref({
  title: "",
  baseStatus: "DRAFT",
  startAt: null,
  endAt: null,
  desktopUploadFiles: [],
  currentDesktopUrl: ""
});
const BANNER_WIDTH = 1600;
const BANNER_HEIGHT = 520;
const GRID_GAP = 8;

const filteredBanners = computed(() => {
  const q = keyword.value.trim().toLowerCase();
  if (!q) return banners.value;
  return banners.value.filter((item) => {
    const title = String(item?.title || "").toLowerCase();
    const status = String(item?.status || "").toLowerCase();
    const baseStatus = String(item?.baseStatus || "").toLowerCase();
    return title.includes(q) || status.includes(q) || baseStatus.includes(q);
  });
});

const desktopPreviewUrl = computed(() => {
  const file = bannerForm.value.desktopUploadFiles?.[0]?.raw;
  if (file instanceof File) {
    return URL.createObjectURL(file);
  }
  return bannerForm.value.currentDesktopUrl || "";
});

const pagedBanners = computed(() => {
  const start = page.value * size.value;
  return filteredBanners.value.slice(start, start + size.value);
});

const fetchBanners = async () => {
  loading.value = true;
  try {
    const { data } = await bannerApi.getAdminBanners();
    banners.value = Array.isArray(data) ? data : [];
    page.value = 0;
  } catch (error) {
    console.error(error);
    ElMessage.error(error?.response?.data?.message || "Không tải được banner");
  } finally {
    loading.value = false;
  }
};

const handlePageChange = (nextPage) => {
  page.value = Math.max(0, nextPage - 1);
};

const openCreate = () => {
  drawerMode.value = "create";
  editingId.value = null;
  bannerForm.value = {
    title: "",
    baseStatus: "DRAFT",
    startAt: null,
    endAt: null,
    desktopUploadFiles: [],
    currentDesktopUrl: ""
  };
  drawerVisible.value = true;
};

const openEdit = (item) => {
  drawerMode.value = "edit";
  editingId.value = item.id;
  bannerForm.value = {
    title: item.title || "",
    baseStatus: item.baseStatus || "DRAFT",
    startAt: item.startAt || null,
    endAt: item.endAt || null,
    desktopUploadFiles: [],
    currentDesktopUrl: item.imageUrl || ""
  };
  drawerVisible.value = true;
};

const loadImageFromFile = (file) =>
  new Promise((resolve, reject) => {
    const objectUrl = URL.createObjectURL(file);
    const image = new Image();
    image.onload = () => {
      URL.revokeObjectURL(objectUrl);
      resolve(image);
    };
    image.onerror = () => {
      URL.revokeObjectURL(objectUrl);
      reject(new Error("Không đọc được ảnh"));
    };
    image.src = objectUrl;
  });

const drawCoverImage = (ctx, image, x, y, width, height) => {
  const sourceRatio = image.width / image.height;
  const targetRatio = width / height;
  let sourceWidth = image.width;
  let sourceHeight = image.height;
  let sourceX = 0;
  let sourceY = 0;

  if (sourceRatio > targetRatio) {
    sourceWidth = image.height * targetRatio;
    sourceX = (image.width - sourceWidth) / 2;
  } else {
    sourceHeight = image.width / targetRatio;
    sourceY = (image.height - sourceHeight) / 2;
  }

  ctx.drawImage(image, sourceX, sourceY, sourceWidth, sourceHeight, x, y, width, height);
};

const buildRects = (count) => {
  if (count <= 1) {
    return [{ x: 0, y: 0, width: BANNER_WIDTH, height: BANNER_HEIGHT }];
  }
  if (count === 2) {
    const w = (BANNER_WIDTH - GRID_GAP) / 2;
    return [
      { x: 0, y: 0, width: w, height: BANNER_HEIGHT },
      { x: w + GRID_GAP, y: 0, width: w, height: BANNER_HEIGHT }
    ];
  }

  // 3+ images: keep a triptych-style layout like the design sample.
  // Each column has equal width; with many images they stack vertically in columns.
  const columns = Math.min(3, count);
  const columnWidth = (BANNER_WIDTH - GRID_GAP * (columns - 1)) / columns;
  const columnItemCounts = Array.from({ length: columns }, (_, columnIndex) =>
    Math.floor((count + columns - 1 - columnIndex) / columns)
  );
  const rowProgress = new Array(columns).fill(0);
  const rects = [];

  for (let index = 0; index < count; index += 1) {
    const col = index % columns;
    const totalRowsInColumn = columnItemCounts[col];
    const cellHeight =
      totalRowsInColumn > 0
        ? (BANNER_HEIGHT - GRID_GAP * (totalRowsInColumn - 1)) / totalRowsInColumn
        : BANNER_HEIGHT;
    const row = rowProgress[col];
    rowProgress[col] += 1;

    rects.push({
      x: col * (columnWidth + GRID_GAP),
      y: row * (cellHeight + GRID_GAP),
      width: columnWidth,
      height: cellHeight
    });
  }

  return rects;
};

const composeBannerFile = async (files, width, height) => {
  const images = await Promise.all(files.map((file) => loadImageFromFile(file)));
  const canvas = document.createElement("canvas");
  canvas.width = width;
  canvas.height = height;
  const ctx = canvas.getContext("2d");
  if (!ctx) {
    throw new Error("Không tạo được canvas");
  }

  ctx.fillStyle = "#f3f4f6";
  ctx.fillRect(0, 0, width, height);

  const rects = buildRects(images.length).map((rect) => ({
    x: (rect.x / BANNER_WIDTH) * width,
    y: (rect.y / BANNER_HEIGHT) * height,
    width: (rect.width / BANNER_WIDTH) * width,
    height: (rect.height / BANNER_HEIGHT) * height
  }));
  images.forEach((image, index) => {
    const rect = rects[index];
    if (!rect) return;
    drawCoverImage(ctx, image, rect.x, rect.y, rect.width, rect.height);
  });

  const blob = await new Promise((resolve, reject) => {
    canvas.toBlob(
      (result) => {
        if (result) resolve(result);
        else reject(new Error("Không thể xuất ảnh banner"));
      },
      "image/jpeg",
      0.92
    );
  });

  return new File([blob], `banner-${Date.now()}.jpg`, { type: "image/jpeg" });
};

const submitBanner = async () => {
  if (bannerForm.value.startAt && bannerForm.value.endAt && bannerForm.value.endAt < bannerForm.value.startAt) {
    ElMessage.warning("Thời gian kết thúc phải lớn hơn hoặc bằng thời gian bắt đầu");
    return;
  }

  const payload = {
    title: bannerForm.value.title?.trim() || null,
    linkUrl: null,
    status: bannerForm.value.baseStatus || "DRAFT",
    startAt: bannerForm.value.startAt || null,
    endAt: bannerForm.value.endAt || null
  };

  const desktopRawFiles = (bannerForm.value.desktopUploadFiles || [])
    .map((item) => item?.raw)
    .filter((item) => item instanceof File);

  if (drawerMode.value === "create" && !desktopRawFiles.length) {
    ElMessage.warning("Vui lòng chọn ít nhất 1 ảnh desktop");
    return;
  }

  submitting.value = true;
  try {
    const formData = new FormData();
    formData.append("data", JSON.stringify(payload));

    if (desktopRawFiles.length) {
      const desktopFile =
        desktopRawFiles.length === 1
          ? desktopRawFiles[0]
          : await composeBannerFile(desktopRawFiles, BANNER_WIDTH, BANNER_HEIGHT);
      formData.append("desktopFile", desktopFile);
    }

    if (drawerMode.value === "create") {
      await bannerApi.createBanner(formData);
      ElMessage.success("Tạo banner thành công");
    } else if (editingId.value) {
      await bannerApi.updateBanner(editingId.value, formData);
      ElMessage.success("Cập nhật banner thành công");
    }
    drawerVisible.value = false;
    await fetchBanners();
  } catch (error) {
    console.error(error);
    ElMessage.error(error?.response?.data?.message || "Lưu banner thất bại");
  } finally {
    submitting.value = false;
  }
};

const displayTagType = (status) => {
  if (status === "ACTIVE") return "success";
  if (status === "SCHEDULED") return "warning";
  if (status === "EXPIRED") return "danger";
  return "info";
};

const formatDate = (value) => {
  if (!value) return "N/A";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString("vi-VN");
};

const formatSchedule = (startAt, endAt) => {
  if (!startAt && !endAt) return "Luôn hiển thị";
  return `${formatDate(startAt)} → ${formatDate(endAt)}`;
};

const deleteBanner = async (item) => {
  try {
    await confirm({
      title: "Xác nhận",
      message: `Xóa banner "${item.title || item.id}"?`,
      confirmButtonText: "Xóa",
      cancelButtonText: "Hủy",
      onConfirm: async () => {
        await bannerApi.deleteBanner(item.id);
        ElMessage.success("Đã xóa banner");
        await fetchBanners();
      }
    });
  } catch (error) {
    if (error.message !== "cancel") {
      console.error(error);
      ElMessage.error(error?.response?.data?.message || "Xóa banner thất bại");
    }
  }
};

onMounted(() => {
  fetchBanners();
});

watch(keyword, () => {
  page.value = 0;
});
</script>

<style scoped lang="scss">
.banner-admin-page {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.panel {
  border: 1px solid #dce1e7;
  background: #fff;
  padding: 10px;
}

.panel-head { margin-bottom: 10px; display: flex; gap: 8px; flex-wrap: wrap; }

.search-input {
  width: 320px;
}

.table-wrap {
  width: 100%;
  overflow-x: auto;
}

.banner-table {
  width: 100%;
  min-width: 900px;
}

.pagination-wrap {
  margin-top: 10px;
  display: flex;
  justify-content: flex-end;
}

.thumb {
  width: 88px;
  height: 48px;
  object-fit: cover;
  border-radius: 6px;
  border: 1px solid #e5e7eb;
}

.banner-form-drawer {
  :deep(.el-drawer__body) {
    padding: 16px;
  }
}

.upload-tip {
  margin: 8px 0 0;
  font-size: 12px;
  color: #6b7280;
}

.preview-row {
  margin-top: 8px;
}

.preview-box {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 10px;
  background: #fafafa;
}

.preview-label {
  margin: 0 0 8px;
  font-size: 12px;
  color: #6b7280;
}

.preview-img {
  width: 100%;
  object-fit: cover;
  border-radius: 6px;
  border: 1px solid #d1d5db;
}

.preview-img.desktop {
  aspect-ratio: 16 / 5;
}

.preview-empty {
  font-size: 12px;
  color: #9ca3af;
  border: 1px dashed #d1d5db;
  border-radius: 6px;
  text-align: center;
  padding: 14px;
}

@media (max-width: 900px) {
  .search-input {
    width: 100%;
  }

  .banner-form-drawer {
    :deep(.el-drawer) {
      width: 100% !important;
      max-width: 100% !important;
    }
  }
}
</style>

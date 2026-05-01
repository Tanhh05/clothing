<template>
  <section v-loading="loading" class="product-admin-page admin-page-shell admin-page">
    <div v-if="!isCreateOnly" class="inventory-panel">
      <div class="panel-header">
        <div class="panel-actions admin-toolbar">
          <el-input
            v-model="keyword"
            placeholder="Tìm theo tên / thương hiệu"
            clearable
            class="search-input"
          >
            <i slot="prefix" class="el-input__icon el-icon-search" />
          </el-input>
          <div class="action-buttons">
            <el-button class="admin-ghost-btn" @click="refreshProducts">Làm mới</el-button>
            <el-button plain class="admin-ghost-btn" @click="openImportDialog">Import XLSX</el-button>
            <el-button plain class="admin-ghost-btn" @click="goToInventoryPage">Tồn kho</el-button>
            <el-button plain class="admin-ghost-btn" @click="openDeletedDrawer">Đã xóa</el-button>
            <el-button type="primary" class="admin-primary-btn" @click="openCreate">+ Thêm sản phẩm</el-button>
          </div>
        </div>
      </div>

      <div v-if="selectedIds.length" class="bulk-toolbar">
        <p>Đã chọn {{ selectedIds.length }} sản phẩm</p>
        <div class="bulk-actions">
          <el-button :size="elementSize" @click="bulkSetStatus('ACTIVE')">Đặt đang hoạt động</el-button>
          <el-button :size="elementSize" @click="bulkSetStatus('INACTIVE')">Đặt ngưng hoạt động</el-button>
          <el-button :size="elementSize" type="danger" plain @click="bulkSoftDelete">Xóa mềm</el-button>
          <el-button :size="elementSize" @click="clearSelection">Bỏ chọn</el-button>
        </div>
      </div>

      <div class="table-wrap">
        <el-table
          ref="productTableRef"
          :data="displayRows"
          border
          stripe
          :size="elementSize"
          class="inventory-table admin-table"
          empty-text="Không có sản phẩm"
          table-layout="fixed"
          @selection-change="handleSelectionChange"
        >
          <el-table-column type="selection" width="48" align="center" />
          <el-table-column label="Ảnh" width="86" align="center">
            <template slot-scope="{ row }">
              <div class="product-cell">
                <img
                  :src="row.imageUrl || fallbackImage"
                  alt="thumbnail"
                  class="thumb"
                  @error="onThumbError"
                >
              </div>
            </template>
          </el-table-column>
          <el-table-column label="Mã vạch" min-width="140" prop="barcode" />
          <el-table-column label="Tên sản phẩm" min-width="220">
            <template slot-scope="{ row }">
              <div>
                <p class="name">{{ row.productName || 'Không có' }}</p>
                <p class="meta">{{ row.categoryName || "Không có" }}</p>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="Biến thể" min-width="150" prop="variantLabel" />
          <el-table-column label="Thương hiệu" min-width="150" prop="brand" />
          <el-table-column label="Tồn" width="78" prop="stock" align="center" />
          <el-table-column label="Trạng thái" width="112" align="center">
            <template slot-scope="{ row }">
              <el-tag :type="readStatusTag(row.status)">{{ formatStatusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="Thao tác" width="120" align="center">
            <template slot-scope="{ row }">
              <div class="action-cell admin-action-cell">
                <el-tooltip content="Sửa" placement="top">
                  <el-button
                    :size="elementSize"
                    icon="el-icon-edit-outline"
                    class="admin-action-btn admin-action-edit"
                    circle
                    @click="openEditByRow(row)"
                  />
                </el-tooltip>
                <el-tooltip content="Xóa" placement="top">
                  <el-button
                    :size="elementSize"
                    icon="el-icon-delete"
                    class="admin-action-btn admin-action-delete"
                    circle
                    @click="deleteItemByRow(row)"
                  />
                </el-tooltip>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <pagination
        class="admin-pagination"
        :total="totalElements"
        :page.sync="currentPage"
        :limit.sync="pageSize"
        @pagination="handleListPagination"
      />
    </div>

    <el-drawer
      :visible.sync="drawerVisible"
      :title="drawerMode === 'create' ? 'Thêm sản phẩm' : 'Cập nhật sản phẩm'"
      direction="rtl"
      :size="drawerSize"
      :append-to-body="true"
      :with-header="true"
      :show-close="true"
      :close-on-click-modal="true"
      class="product-form-drawer"
      @closed="handleDrawerClosed"
    >
      <div class="drawer-scroll-content">
        <el-form :model="productForm" label-position="top">
          <el-form-item label="Tên sản phẩm">
            <el-input v-model="productForm.name" />
          </el-form-item>
          <el-form-item label="Thương hiệu">
            <el-input v-model="productForm.brand" />
          </el-form-item>
          <el-form-item label="Danh mục">
            <el-select v-model="productForm.categoryId" style="width: 100%" placeholder="Chọn danh mục">
              <el-option
                v-for="category in categoryOptions"
                :key="category.id"
                :label="category.name"
                :value="category.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="Ảnh sản phẩm">
            <el-upload
              class="product-image-uploader"
              action="#"
              list-type="picture-card"
              :auto-upload="false"
              :multiple="true"
              accept="image/*"
              :file-list="productForm.uploadFiles"
              :on-change="handleProductUploadChange"
              :on-remove="handleProductUploadRemove"
            >
              <i class="el-icon-plus" />
            </el-upload>
            <p class="upload-tip">Chọn nhiều ảnh từ máy. Ảnh đầu tiên sẽ là ảnh chính.</p>
          </el-form-item>
          <el-form-item label="Mô tả">
            <el-input v-model="productForm.description" type="textarea" :rows="3" />
          </el-form-item>
          <div class="variant-block">
            <div class="variant-head">
              <h4>Biến thể sản phẩm</h4>
              <el-button :size="elementSize" type="primary" plain @click="addVariant">+ Thêm biến thể</el-button>
            </div>

            <div v-for="(variant, index) in productForm.variants" :key="`variant-${index}`" class="variant-item">
              <div class="variant-item-head">
                <p>Biến thể #{{ index + 1 }}</p>
                <el-button
                  v-if="productForm.variants.length > 1"
                  :size="elementSize"
                  type="danger"
                  plain
                  @click="removeVariant(index)"
                >
                  Xóa
                </el-button>
              </div>

              <el-form-item label="SKU">
                <div class="sku-row">
                  <el-input v-model="variant.sku" :disabled="variant.autoSku" />
                  <el-switch
                    v-model="variant.autoSku"
                    active-text="AUTO"
                    inactive-text="MANUAL"
                    @change="(value) => handleAutoSkuToggle(index, value)"
                  />
                </div>
              </el-form-item>
              <el-row :gutter="10">
                <el-col :span="12">
                  <el-form-item label="Giá">
                    <el-input v-model.number="variant.price" type="number" min="0" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="Tồn kho">
                    <el-input v-model.number="variant.stock" type="number" min="0" />
                  </el-form-item>
                </el-col>
              </el-row>
              <el-row :gutter="10">
                <el-col :span="24">
                  <el-form-item label="Cân nặng (kg)">
                    <el-input v-model.number="variant.weight" type="number" min="0" step="0.1" />
                  </el-form-item>
                </el-col>
              </el-row>
              <el-row :gutter="10">
                <el-col :span="12">
                  <el-form-item>
                    <template slot="label">
                      <div class="inline-label">
                        <span>Size</span>
                        <el-button
                          :size="elementSize"
                          circle
                          plain
                          class="inline-plus-btn"
                          @click="openOptionPanel('size', index)"
                        >
                          <i class="el-icon-plus" />
                        </el-button>
                      </div>
                    </template>
                    <el-select
                      v-model="variant.size"
                      style="width: 100%"
                      filterable
                      allow-create
                      default-first-option
                      placeholder="Chọn hoặc nhập size"
                      @change="onVariantSizeChange"
                    >
                      <el-option v-for="size in sizeOptions" :key="size" :label="size" :value="size" />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item>
                    <template slot="label">
                      <div class="inline-label">
                        <span>Màu</span>
                        <el-button
                          :size="elementSize"
                          circle
                          plain
                          class="inline-plus-btn"
                          @click="openOptionPanel('color', index)"
                        >
                          <i class="el-icon-plus" />
                        </el-button>
                      </div>
                    </template>
                    <el-select
                      v-model="variant.color"
                      style="width: 100%"
                      filterable
                      allow-create
                      default-first-option
                      placeholder="Chọn hoặc nhập màu"
                      @change="onVariantColorChange"
                    >
                      <el-option v-for="color in colorOptions" :key="color" :label="color" :value="color" />
                    </el-select>
                  </el-form-item>
                </el-col>
              </el-row>
            </div>
          </div>
        </el-form>

        <transition name="option-panel-fade">
          <div v-if="optionPanelVisible" class="option-side-panel-backdrop" @click="closeOptionPanel" />
        </transition>
        <transition name="option-panel-slide">
          <aside v-if="optionPanelVisible" class="option-side-panel">
            <div class="option-side-panel-head">
              <h5>{{ optionPanelType === "size" ? "Thêm size mới" : "Thêm màu mới" }}</h5>
              <el-button type="text" @click="closeOptionPanel">Đóng</el-button>
            </div>
            <el-form label-position="top">
              <el-form-item :label="optionPanelType === 'size' ? 'Tên size' : 'Tên màu'">
                <el-input
                  v-model="optionPanelValue"
                  :placeholder="optionPanelType === 'size' ? 'VD: XXL' : 'VD: Xanh navy'"
                  :disabled="optionPanelSubmitting"
                  @keyup.enter.native="confirmCreateOption"
                />
              </el-form-item>
            </el-form>
            <div class="option-side-panel-actions">
              <el-button :disabled="optionPanelSubmitting" @click="closeOptionPanel">Hủy</el-button>
              <el-button type="primary" :loading="optionPanelSubmitting" @click="confirmCreateOption">Thêm</el-button>
            </div>
          </aside>
        </transition>
      </div>

      <div class="drawer-actions">
        <el-button @click="handleCancelCreate">Hủy</el-button>
        <el-button type="primary" :loading="submitting" @click="submitProduct">
          {{ drawerMode === "create" ? "Tạo sản phẩm" : "Lưu thay đổi" }}
        </el-button>
      </div>
    </el-drawer>

    <el-drawer
      v-if="!isCreateOnly"
      :visible.sync="deletedDrawerVisible"
      title="Sản phẩm đã xóa"
      direction="rtl"
      size="34%"
      class="deleted-product-drawer"
    >
      <div class="deleted-drawer-head">
        <p>{{ deletedProducts.length }} sản phẩm</p>
        <el-button :size="elementSize" :loading="deletedLoading" @click="loadDeletedProducts">Làm mới</el-button>
      </div>
      <div v-loading="deletedLoading">
        <div v-if="!deletedLoading && !deletedProducts.length" class="deleted-empty">
          Không có sản phẩm đã xóa.
        </div>
        <div v-else class="deleted-list">
          <article v-for="item in deletedProducts" :key="item.id" class="deleted-card">
            <div class="deleted-main">
              <p class="deleted-name">{{ item.name || "Không có" }}</p>
              <p class="deleted-meta">
                #{{ item.id }} • {{ item.brand || "Không thương hiệu" }} • {{ item.categoryName || "Không có" }}
              </p>
            </div>
            <el-button
              type="success"
              plain
              :size="elementSize"
              :loading="restoringId === item.id"
              @click="restoreDeletedProduct(item)"
            >
              Khôi phục
            </el-button>
          </article>
        </div>
      </div>
    </el-drawer>

    <el-drawer
      v-if="!isCreateOnly"
      :visible.sync="inventoryDrawerVisible"
      title="Cảnh báo tồn kho"
      direction="rtl"
      size="78%"
      class="inventory-drawer"
    >
      <div class="deleted-drawer-head">
        <p>{{ lowStockItems.length }} biến thể stock thấp</p>
        <el-button :size="elementSize" :loading="inventoryLoading" @click="loadInventoryAlerts">Làm mới</el-button>
      </div>
      <div v-loading="inventoryLoading" class="inventory-alert-wrap">
        <el-table
          :data="lowStockItems"
          border
          stripe
          :size="elementSize"
          table-layout="fixed"
          empty-text="Không có biến thể nào dưới ngưỡng tồn kho."
        >
          <el-table-column prop="variantId" label="ID biến thể" width="110" />
          <el-table-column prop="productId" label="ID sản phẩm" width="110" />
          <el-table-column prop="productName" label="Sản phẩm" min-width="220" />
          <el-table-column prop="sku" label="SKU" min-width="170" />
          <el-table-column prop="stock" label="Tồn kho" width="100" />
          <el-table-column label="Lịch sử" width="120">
            <template slot-scope="{ row }">
              <el-button :size="elementSize" @click="loadInventoryLogs(row.variantId)">Xem log</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <section class="inventory-log-box">
        <h4>Lịch sử nhập/xuất</h4>
        <el-table :data="inventoryLogs" border stripe :size="elementSize" table-layout="fixed" empty-text="Chưa có log">
          <el-table-column prop="createdAt" label="Thời gian" min-width="165">
            <template slot-scope="{ row }">{{ formatDateTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column prop="sku" label="SKU" min-width="140" />
          <el-table-column prop="type" label="Loại" min-width="100" />
          <el-table-column prop="quantity" label="SL" min-width="80" />
          <el-table-column label="Tồn kho" min-width="120">
            <template slot-scope="{ row }">{{ row.beforeStock }} → {{ row.afterStock }}</template>
          </el-table-column>
          <el-table-column prop="note" label="Ghi chú" min-width="220" show-overflow-tooltip />
        </el-table>
      </section>
    </el-drawer>

    <el-dialog
      v-if="!isCreateOnly"
      :visible.sync="importDialogVisible"
      title="Import sản phẩm từ XLSX"
      width="760px"
      class="product-import-dialog"
    >
      <div class="import-toolbar">
        <el-checkbox v-model="importDryRun">Dry-run (chỉ kiểm tra)</el-checkbox>
        <el-checkbox v-model="importUpsertBySku">Update nếu SKU đã tồn tại</el-checkbox>
        <el-button type="text" @click="downloadImportTemplate">Tải file mẫu</el-button>
      </div>
      <div class="import-guide">
        <p>File cần có sheet đầu tiên, hàng đầu là tiêu đề. Thứ tự cột:</p>
        <code>name | brand | description | category(id/slug/name) | status | imageUrl | sku | price | stock | weight | size | color | slug</code>
      </div>

      <el-upload
        drag
        action="#"
        :auto-upload="false"
        :multiple="false"
        :limit="1"
        accept=".xlsx"
        class="import-upload"
        :file-list="importFileList"
        :on-change="handleImportFileChange"
        :on-remove="handleImportFileRemove"
      >
        <i class="el-icon-upload" />
        <div class="el-upload__text">Kéo thả file .xlsx vào đây hoặc <em>chọn file</em></div>
      </el-upload>

      <div v-if="importResult" class="import-result">
        <div class="import-result-summary">
          <el-tag type="warning">Batch: {{ importResult.batchId || "-" }}</el-tag>
          <el-tag type="info">Tổng dòng: {{ importResult.totalRows || 0 }}</el-tag>
          <el-tag type="success">Thành công: {{ importResult.successCount || 0 }}</el-tag>
          <el-tag type="danger">Thất bại: {{ importResult.failedCount || 0 }}</el-tag>
        </div>
        <el-table :data="importResult.results || []" border stripe :size="elementSize" table-layout="fixed" max-height="320">
          <el-table-column prop="rowNumber" label="Dòng" width="72" />
          <el-table-column prop="name" label="Tên sản phẩm" min-width="180" />
          <el-table-column label="Trạng thái" width="120">
            <template slot-scope="{ row }">
              <el-tag :type="row.status === 'SUCCESS' ? 'success' : 'danger'">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="message" label="Chi tiết" min-width="220" show-overflow-tooltip />
        </el-table>
      </div>
      <div slot="footer">
        <el-button @click="importDialogVisible = false">Đóng</el-button>
        <el-button type="primary" :loading="importSubmitting" @click="submitImportXlsx">Import</el-button>
      </div>
    </el-dialog>
  </section>
</template>

<script>
import {
  fetchProducts,
  fetchDeletedProducts,
  fetchProductByKey,
  createProduct,
  updateProduct,
  deleteProduct,
  restoreProduct,
  fetchCategories,
  fetchVariantOptions,
  createVariantOption,
  bulkDeleteProducts,
  bulkUpdateProductStatus,
  fetchInventoryAlerts,
  fetchInventoryLogs,
  importProductsXlsx,
  downloadProductImportTemplate
} from '@/api/product'
import Pagination from '@/components/Pagination'
import { getLocalJson, setLocalJson } from '@/utils/local-cache'

const fallbackImage = require('@/assets/images/product-fallback.svg')
const PRODUCT_PAGE_CACHE_KEY = 'clothing_admin_products_page_cache_v1'

const normalizeOptionValue = value => String(value || '').trim()

const normalizeSkuToken = (value, fallback = 'NA') => {
  const normalized = String(value || '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toUpperCase()
    .replace(/[^A-Z0-9]+/g, '')
  return normalized || fallback
}

export default {
  name: 'ProductManagement',
  components: { Pagination },
  props: {
    createOnly: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      loading: false,
      products: [],
      totalElements: 0,
      currentPage: 1,
      pageSize: 10,
      keyword: '',
      fallbackImage,
      drawerVisible: false,
      drawerMode: 'create',
      submitting: false,
      selectedIds: [],
      deletedDrawerVisible: false,
      deletedProducts: [],
      deletedLoading: false,
      confirmDialogPending: false,
      restoringId: null,
      inventoryDrawerVisible: false,
      inventoryLoading: false,
      lowStockItems: [],
      inventoryLogs: [],
      editingId: null,
      categoryOptions: [],
      sizeOptions: [],
      colorOptions: [],
      optionPanelVisible: false,
      optionPanelType: 'size',
      optionPanelValue: '',
      optionPanelVariantIndex: null,
      optionPanelSubmitting: false,
      importDialogVisible: false,
      importSubmitting: false,
      importFileList: [],
      importResult: null,
      importDryRun: false,
      importUpsertBySku: false,
      debounceTimer: null,
      viewportWidth: window.innerWidth || 1366,
      hasBootstrapped: false,
      productForm: {
        name: '',
        brand: '',
        description: '',
        status: 'ACTIVE',
        categoryId: null,
        uploadFiles: [],
        variants: []
      }
    }
  },
  computed: {
    isCreateOnly() {
      return Boolean(this.createOnly)
    },
    elementSize() {
      const size = this.$store.getters.size
      return size === 'default' ? undefined : size
    },
    drawerSize() {
      if (this.viewportWidth <= 767) return '100%'
      if (this.viewportWidth <= 1199) return this.isCreateOnly ? '94%' : '88%'
      return this.isCreateOnly ? '760px' : '42%'
    },
    displayRows() {
      const rows = []
      ;(this.products || []).forEach((product) => {
        const variants = Array.isArray(product && product.variants) ? product.variants : []
        const imageUrl = product.mainImageUrl || this.firstImageUrl(product) || this.fallbackImage
        if (!variants.length) {
          rows.push({
            productId: product.id,
            imageUrl,
            barcode: `SP-${product.id || 'Không có'}`,
            productName: product.name || 'Không có',
            variantLabel: '-',
            brand: product.brand,
            categoryName: product.categoryName,
            stock: 0,
            status: product.status || 'Không xác định'
          })
          return
        }
        variants.forEach((variant) => {
          rows.push({
            productId: product.id,
            imageUrl,
            barcode: variant && variant.sku ? variant.sku : `SP-${product.id || 'Không có'}`,
            productName: product.name || 'Không có',
            variantLabel: this.formatVariantLabel(variant),
            brand: product.brand,
            categoryName: product.categoryName,
            stock: Number((variant && variant.stock) || 0),
            status: product.status || 'Không xác định'
          })
        })
      })
      return rows
    }
  },
  watch: {
    keyword() {
      if (this.isCreateOnly) return
      if (this.debounceTimer) clearTimeout(this.debounceTimer)
      this.debounceTimer = setTimeout(() => {
        this.fetchPage(0)
      }, 300)
    },
    drawerVisible(value) {
      if (this.isCreateOnly && !value) {
        this.$emit('close-request')
      }
    },
    'productForm.name': 'syncAutoSkus',
    'productForm.brand': 'syncAutoSkus',
    'productForm.variants': {
      deep: true,
      handler() {
        this.syncAutoSkus()
      }
    }
  },
  created() {
    this.bootstrap()
  },
  activated() {
    // Keep-alive pages can show stale data after edits in other pages.
    // Refresh current list whenever user comes back to this tab.
    if (this.isCreateOnly || !this.hasBootstrapped || this.loading) return
    this.fetchPage(Math.max(0, Number(this.currentPage) - 1))
  },
  mounted() {
    window.addEventListener('resize', this.handleViewportResize)
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.handleViewportResize)
  },
  methods: {
    handleViewportResize() {
      this.viewportWidth = window.innerWidth || 1366
    },
    async bootstrap() {
      await Promise.all([this.loadCategories(), this.loadVariantOptions()])
      if (this.isCreateOnly) {
        this.hasBootstrapped = true
        this.openCreate()
        return
      }
      await this.fetchPage(0)
      this.hasBootstrapped = true
    },
    defaultFormValue() {
      return {
        name: '',
        brand: '',
        description: '',
        status: 'ACTIVE',
        categoryId: this.categoryOptions[0] ? this.categoryOptions[0].id : null,
        uploadFiles: [],
        variants: [this.createEmptyVariant()]
      }
    },
    createEmptyVariant() {
      return {
        id: null,
        sku: '',
        autoSku: true,
        price: 0,
        stock: 0,
        weight: 0.2,
        size: this.sizeOptions[0] || 'M',
        color: this.colorOptions[0] || ''
      }
    },
    generateAutoSku(variant, index) {
      const nameToken = normalizeSkuToken(this.productForm.name, 'SP')
      const brandToken = normalizeSkuToken(this.productForm.brand, 'GEN')
      const sizeToken = normalizeSkuToken(variant && variant.size, 'SZ')
      const colorToken = normalizeSkuToken(variant && variant.color, 'CL')
      return `${nameToken}-${brandToken}-${sizeToken}-${colorToken}-${String(index + 1).padStart(2, '0')}`
    },
    syncAutoSkus() {
      const variants = Array.isArray(this.productForm.variants) ? this.productForm.variants : []
      variants.forEach((variant, index) => {
        if (variant && variant.autoSku) {
          variant.sku = this.generateAutoSku(variant, index)
        }
      })
    },
    readProductPrice(product) {
      if (Number.isFinite(Number(product && product.minPrice))) return Number(product.minPrice)
      if (Number.isFinite(Number(product && product.price))) return Number(product.price)
      const prices = Array.isArray(product && product.variants)
        ? product.variants.map(v => Number(v && v.price)).filter(v => Number.isFinite(v))
        : []
      return prices.length ? Math.min(...prices) : 0
    },
    readProductStock(product) {
      const variants = Array.isArray(product && product.variants) ? product.variants : []
      if (!variants.length) return 0
      return variants.reduce((sum, v) => sum + (Number(v && v.stock) || 0), 0)
    },
    readStatusTag(status) {
      if (status === 'ACTIVE') return 'success'
      if (status === 'INACTIVE') return 'info'
      return 'warning'
    },
    formatStatusLabel(status) {
      const normalized = String(status || '').toUpperCase()
      if (normalized === 'ACTIVE') return 'Đang hoạt động'
      if (normalized === 'INACTIVE') return 'Ngưng hoạt động'
      return 'Không xác định'
    },
    buildVariantDisplayName(product, variant) {
      const baseName = String((product && product.name) || 'Không có')
      const size = String((variant && variant.size) || '').trim()
      const color = String((variant && variant.color) || '').trim()
      const suffix = [color, size].filter(Boolean).join(' - ')
      return suffix ? `${baseName} - ${suffix}` : baseName
    },
    formatVariantLabel(variant) {
      const size = String((variant && variant.size) || '').trim()
      const color = String((variant && variant.color) || '').trim()
      const text = [color, size].filter(Boolean).join(' - ')
      return text || '-'
    },
    formatPrice(value) {
      return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(Number(value) || 0)
    },
    formatDateTime(value) {
      if (!value) return 'Không có'
      const date = new Date(value)
      if (Number.isNaN(date.getTime())) return 'Không có'
      return date.toLocaleString('vi-VN')
    },
    firstImageUrl(row) {
      if (!Array.isArray(row && row.images) || !row.images.length) return ''
      return row.images[0] && row.images[0].url ? row.images[0].url : ''
    },
    onThumbError(event) {
      if (!event || !event.target) return
      event.target.src = this.fallbackImage
    },
    async loadCategories() {
      try {
        const data = await fetchCategories()
        this.categoryOptions = Array.isArray(data && data.content) ? data.content : []
        if (!this.productForm.categoryId && this.categoryOptions.length) {
          this.productForm.categoryId = this.categoryOptions[0].id
        }
      } catch (error) {
        this.categoryOptions = []
        this.$message.error('Không tải được danh mục')
      }
    },
    mergeOptionValues(values) {
      const map = new Map()
      values.forEach(value => {
        const normalized = normalizeOptionValue(value)
        if (!normalized) return
        const key = normalized.toLowerCase()
        if (!map.has(key)) map.set(key, normalized)
      })
      return Array.from(map.values())
    },
    upsertOptionValue(type, rawValue, toTop) {
      const normalized = normalizeOptionValue(rawValue)
      if (!normalized) return null
      const key = type === 'size' ? 'sizeOptions' : 'colorOptions'
      const existing = Array.isArray(this[key]) ? this[key] : []
      const rest = existing.filter(item => String(item).toLowerCase() !== normalized.toLowerCase())
      const next = toTop ? [normalized].concat(rest) : rest.concat(normalized)
      this[key] = this.mergeOptionValues(next)
      return normalized
    },
    async loadVariantOptions() {
      try {
        const [sizeData, colorData] = await Promise.all([
          fetchVariantOptions('size'),
          fetchVariantOptions('color')
        ])
        this.sizeOptions = this.mergeOptionValues([].concat(sizeData || []))
        this.colorOptions = this.mergeOptionValues([].concat(colorData || []))
      } catch (error) {
        this.$message.error('Không tải được danh sách size/màu')
      }
    },
    async fetchPage(page = 0) {
      const query = {
        size: this.pageSize,
        page,
        sortBy: 'id',
        direction: 'desc',
        q: this.keyword ? this.keyword.trim() : undefined
      }
      const cacheKey = this.buildProductCacheKey(query)
      const cached = this.getCachedProductPage(cacheKey)
      if (cached) {
        this.applyProductPageData(cached, page)
      }
      this.loading = true
      try {
        const data = await fetchProducts(query)
        this.applyProductPageData(data, page)
        this.cacheProductPage(cacheKey, data)
      } catch (error) {
        if (!cached) {
          this.$message.error((error && error.response && error.response.data && error.response.data.message) || 'Không tải được sản phẩm')
        }
      } finally {
        this.loading = false
      }
    },
    buildProductCacheKey(query) {
      const q = query || {}
      return JSON.stringify({
        page: Number(q.page || 0),
        size: Number(q.size || 20),
        sortBy: q.sortBy || 'id',
        direction: q.direction || 'desc',
        q: q.q || ''
      })
    },
    getCachedProductPage(cacheKey) {
      const cache = getLocalJson(PRODUCT_PAGE_CACHE_KEY, {})
      if (!cache || typeof cache !== 'object') return null
      const pageData = cache[cacheKey]
      if (!pageData || typeof pageData !== 'object') return null
      return pageData
    },
    cacheProductPage(cacheKey, data) {
      const cache = getLocalJson(PRODUCT_PAGE_CACHE_KEY, {})
      const nextCache = Object.assign({}, cache, { [cacheKey]: data })
      setLocalJson(PRODUCT_PAGE_CACHE_KEY, nextCache)
    },
    applyProductPageData(data, fallbackPage = 0) {
      this.products = Array.isArray(data && data.content) ? data.content : []
      this.totalElements = Number((data && data.totalElements) || 0)
      const pageNumber = Number((data && (data.pageNumber != null ? data.pageNumber : data.page)) || fallbackPage)
      this.currentPage = pageNumber + 1
    },
    async refreshProducts() {
      await this.fetchPage(0)
      this.clearSelection()
    },
    handleListPagination({ page, limit }) {
      this.pageSize = Number(limit) || this.pageSize
      this.fetchPage(Math.max(0, Number(page) - 1))
    },
    handleSelectionChange(rows) {
      const ids = Array.isArray(rows) ? rows.map(row => row.productId).filter(Boolean) : []
      this.selectedIds = Array.from(new Set(ids))
    },
    openEditByRow(row) {
      if (!row || !row.productId) return
      this.openEdit({ id: row.productId })
    },
    deleteItemByRow(row) {
      if (!row || !row.productId) return
      this.deleteItem({ id: row.productId, name: row.displayName })
    },
    clearSelection() {
      this.selectedIds = []
      if (this.$refs.productTableRef && this.$refs.productTableRef.clearSelection) {
        this.$refs.productTableRef.clearSelection()
      }
    },
    openCreate() {
      this.drawerMode = 'create'
      this.editingId = null
      this.productForm = this.defaultFormValue()
      this.productForm.variants = this.productForm.variants.map((variant, index) => ({
        ...variant,
        autoSku: true,
        sku: this.generateAutoSku(variant, index)
      }))
      this.closeOptionPanel()
      this.drawerVisible = true
    },
    async openEdit(item) {
      try {
        const detail = await fetchProductByKey(item.id)
        const variants = Array.isArray(detail && detail.variants) && detail.variants.length
          ? detail.variants
          : [this.createEmptyVariant()]
        const fallbackCategoryId = this.categoryOptions.find(c => c.name === detail.categoryName || c.name === item.categoryName)
        this.drawerMode = 'edit'
        this.editingId = detail.id
        this.productForm = {
          name: detail.name || '',
          brand: detail.brand || '',
          description: detail.description || '',
          status: detail.status || 'ACTIVE',
          categoryId: detail.categoryId || (fallbackCategoryId && fallbackCategoryId.id) || (this.categoryOptions[0] && this.categoryOptions[0].id) || null,
          uploadFiles: Array.isArray(detail.images)
            ? detail.images.map((img, index) => ({
              name: `image-${index + 1}`,
              url: (img && img.url) || '',
              status: 'success'
            })).filter(file => file.url)
            : [],
          variants: variants.map((variant, index) => ({
            id: variant.id == null ? null : variant.id,
            sku: variant.sku || `${String(detail.slug || 'SKU').toUpperCase()}-${index + 1}`,
            autoSku: false,
            price: Number(variant.price || 0),
            stock: Number(variant.stock || 0),
            weight: Number(variant.weight || 0.2),
            size: variant.size || this.sizeOptions[0] || 'M',
            color: variant.color || this.colorOptions[0] || ''
          }))
        }
        this.closeOptionPanel()
        this.drawerVisible = true
      } catch (error) {
        this.$message.error((error && error.response && error.response.data && error.response.data.message) || 'Không tải được chi tiết sản phẩm')
      }
    },
    handleAutoSkuToggle(index, value) {
      const variant = this.productForm.variants && this.productForm.variants[index]
      if (!variant) return
      variant.autoSku = Boolean(value)
      if (variant.autoSku) {
        variant.sku = this.generateAutoSku(variant, index)
      }
    },
    addVariant() {
      const nextIndex = this.productForm.variants.length
      const variant = this.createEmptyVariant()
      variant.sku = this.generateAutoSku(variant, nextIndex)
      this.productForm.variants.push(variant)
    },
    removeVariant(index) {
      if (this.productForm.variants.length <= 1) return
      this.productForm.variants.splice(index, 1)
    },
    handleProductUploadChange(file, fileList) {
      this.productForm.uploadFiles = fileList.slice()
    },
    handleProductUploadRemove(file, fileList) {
      this.productForm.uploadFiles = fileList.slice()
    },
    onVariantSizeChange(value) {
      this.upsertOptionValue('size', value, false)
    },
    onVariantColorChange(value) {
      this.upsertOptionValue('color', value, false)
    },
    openOptionPanel(type, variantIndex) {
      this.optionPanelType = type === 'color' ? 'color' : 'size'
      this.optionPanelVariantIndex = Number.isInteger(variantIndex) ? variantIndex : null
      this.optionPanelValue = ''
      this.optionPanelVisible = true
    },
    closeOptionPanel() {
      this.optionPanelVisible = false
      this.optionPanelValue = ''
      this.optionPanelVariantIndex = null
      this.optionPanelSubmitting = false
    },
    async confirmCreateOption() {
      const inputValue = normalizeOptionValue(this.optionPanelValue)
      if (!inputValue) {
        this.$message.warning('Vui lòng nhập giá trị')
        return
      }
      this.optionPanelSubmitting = true
      try {
        const data = await createVariantOption(this.optionPanelType, inputValue)
        const savedValue = normalizeOptionValue((data && data.value) || inputValue)
        const added = this.upsertOptionValue(this.optionPanelType, savedValue, true)
        const variantIndex = this.optionPanelVariantIndex
        if (variantIndex != null && this.productForm.variants && this.productForm.variants[variantIndex]) {
          if (this.optionPanelType === 'size') {
            this.productForm.variants[variantIndex].size = added
          } else {
            this.productForm.variants[variantIndex].color = added
          }
        }
        this.$message.success(this.optionPanelType === 'size' ? `Đã thêm size "${added}"` : `Đã thêm màu "${added}"`)
        this.closeOptionPanel()
      } catch (error) {
        this.$message.error((error && error.response && error.response.data && error.response.data.message) || 'Không thể thêm tùy chọn')
      } finally {
        this.optionPanelSubmitting = false
      }
    },
    mapToPayload() {
      const form = this.productForm
      const existingImages = (Array.isArray(form.uploadFiles) ? form.uploadFiles : [])
        .filter(file => typeof file.url === 'string' && file.url.trim() && !(file.raw instanceof File))
        .map((file, index) => ({ url: file.url.trim(), isMain: index === 0 }))
      return {
        name: (form.name || '').trim(),
        brand: (form.brand || '').trim(),
        description: (form.description || '').trim(),
        status: form.status || 'ACTIVE',
        categoryId: Number(form.categoryId),
        variants: (Array.isArray(form.variants) ? form.variants : []).map(variant => ({
          id: variant.id == null ? null : variant.id,
          sku: String(variant.sku || '').trim(),
          price: Number(variant.price || 0),
          stock: Number(variant.stock || 0),
          weight: Number(variant.weight || 0),
          status: 'ACTIVE',
          size: this.upsertOptionValue('size', variant.size, false),
          color: this.upsertOptionValue('color', variant.color, false)
        })),
        images: existingImages
      }
    },
    getSelectedFiles() {
      const files = Array.isArray(this.productForm.uploadFiles) ? this.productForm.uploadFiles : []
      return files.map(file => file.raw).filter(raw => raw instanceof File)
    },
    buildMultipartPayload(payload, files) {
      const formData = new FormData()
      formData.append('data', JSON.stringify(payload))
      files.forEach(file => formData.append('files', file))
      return formData
    },
    async submitProduct() {
      const payload = this.mapToPayload()
      const selectedFiles = this.getSelectedFiles()
      const hasExistingImages = payload.images.length > 0
      if (this.drawerMode === 'create' && !selectedFiles.length) {
        this.$message.warning('Vui lòng chọn ít nhất 1 ảnh từ máy')
        return
      }
      if (this.drawerMode === 'edit' && !selectedFiles.length && !hasExistingImages) {
        this.$message.warning('Sản phẩm cần tối thiểu 1 ảnh')
        return
      }
      const hasInvalidVariant = !payload.variants.length || payload.variants.some(variant => !variant.sku)
      if (!payload.name || !payload.categoryId || hasInvalidVariant) {
        this.$message.warning('Vui lòng nhập tên, danh mục và SKU cho tất cả biến thể')
        return
      }
      this.submitting = true
      try {
        if (this.drawerMode === 'create') {
          if (selectedFiles.length) {
            await createProduct(this.buildMultipartPayload(payload, selectedFiles))
          } else {
            await createProduct(payload)
          }
          this.$message.success('Tạo sản phẩm thành công')
          this.$emit('created')
        } else if (this.editingId) {
          if (selectedFiles.length) {
            await updateProduct(this.editingId, this.buildMultipartPayload(payload, selectedFiles))
          } else {
            await updateProduct(this.editingId, payload)
          }
          this.$message.success('Cập nhật sản phẩm thành công')
        }
        this.drawerVisible = false
        await this.refreshProducts()
        await this.loadVariantOptions()
      } catch (error) {
        this.$message.error((error && error.response && error.response.data && error.response.data.message) || 'Lưu sản phẩm thất bại')
      } finally {
        this.submitting = false
      }
    },
    async deleteItem(item) {
      if (this.confirmDialogPending) return
      this.confirmDialogPending = true
      this.$confirm(`Bạn có chắc muốn xóa ${item.name || 'sản phẩm này'}?`, 'Xác nhận', {
        confirmButtonText: 'Đồng ý',
        cancelButtonText: 'Hủy',
        type: 'warning'
      }).then(async() => {
        await deleteProduct(item.id)
        this.$message.success('Đã xóa sản phẩm')
        await this.refreshProducts()
        if (this.deletedDrawerVisible) {
          await this.loadDeletedProducts()
        }
      }).catch(() => {}).finally(() => {
        this.confirmDialogPending = false
      })
    },
    async bulkSetStatus(status) {
      if (!this.selectedIds.length) return
      try {
        await bulkUpdateProductStatus(this.selectedIds, status)
        this.$message.success(`Đã cập nhật trạng thái ${status}`)
        this.clearSelection()
        await this.refreshProducts()
      } catch (error) {
        this.$message.error((error && error.response && error.response.data && error.response.data.message) || 'Không thể cập nhật trạng thái hàng loạt')
      }
    },
    async bulkSoftDelete() {
      if (!this.selectedIds.length || this.confirmDialogPending) return
      this.confirmDialogPending = true
      this.$confirm(`Bạn có chắc muốn xóa mềm ${this.selectedIds.length} sản phẩm đã chọn?`, 'Xác nhận', {
        confirmButtonText: 'Đồng ý',
        cancelButtonText: 'Hủy',
        type: 'warning'
      }).then(async() => {
        await bulkDeleteProducts(this.selectedIds)
        this.$message.success('Đã xóa mềm sản phẩm đã chọn')
        this.clearSelection()
        await this.refreshProducts()
        if (this.deletedDrawerVisible) {
          await this.loadDeletedProducts()
        }
      }).catch(() => {}).finally(() => {
        this.confirmDialogPending = false
      })
    },
    async loadDeletedProducts() {
      this.deletedLoading = true
      try {
        const data = await fetchDeletedProducts()
        this.deletedProducts = Array.isArray(data) ? data : []
      } catch (error) {
        this.$message.error((error && error.response && error.response.data && error.response.data.message) || 'Không tải được sản phẩm đã xóa')
      } finally {
        this.deletedLoading = false
      }
    },
    async openDeletedDrawer() {
      this.deletedDrawerVisible = true
      await this.loadDeletedProducts()
    },
    async restoreDeletedProduct(item) {
      if (this.restoringId) return
      this.$confirm(`Khôi phục sản phẩm "${item.name || item.id}"?`, 'Xác nhận', {
        confirmButtonText: 'Khôi phục',
        cancelButtonText: 'Hủy',
        type: 'warning'
      }).then(async() => {
        this.restoringId = item.id
        try {
          await restoreProduct(item.id)
          this.$message.success('Khôi phục sản phẩm thành công')
          await Promise.all([this.refreshProducts(), this.loadDeletedProducts()])
        } finally {
          this.restoringId = null
        }
      }).catch(() => {})
    },
    async loadInventoryAlerts() {
      this.inventoryLoading = true
      try {
        const data = await fetchInventoryAlerts(5)
        this.lowStockItems = Array.isArray(data) ? data : []
      } catch (error) {
        this.$message.error((error && error.response && error.response.data && error.response.data.message) || 'Không tải được cảnh báo tồn kho')
      } finally {
        this.inventoryLoading = false
      }
    },
    async loadInventoryLogs(variantId) {
      try {
        const data = await fetchInventoryLogs(variantId)
        this.inventoryLogs = Array.isArray(data) ? data : []
      } catch (error) {
        this.$message.error((error && error.response && error.response.data && error.response.data.message) || 'Không tải được lịch sử tồn kho')
      }
    },
    goToInventoryPage() {
      this.$router.push('/catalog/inventory-management')
    },
    openImportDialog() {
      this.importDialogVisible = true
      this.importFileList = []
      this.importResult = null
      this.importDryRun = false
      this.importUpsertBySku = false
    },
    async downloadImportTemplate() {
      try {
        const blob = await downloadProductImportTemplate()
        const url = URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = url
        link.download = 'product-import-template.xlsx'
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)
        URL.revokeObjectURL(url)
      } catch (error) {
        this.$message.error('Không tải được file mẫu')
      }
    },
    handleImportFileChange(file, fileList) {
      this.importFileList = fileList.slice(-1)
    },
    handleImportFileRemove(file, fileList) {
      this.importFileList = fileList.slice()
    },
    async submitImportXlsx() {
      const firstFile = Array.isArray(this.importFileList) && this.importFileList.length ? this.importFileList[0] : null
      const rawFile = firstFile && firstFile.raw instanceof File ? firstFile.raw : null
      if (!rawFile) {
        this.$message.warning('Vui lòng chọn file .xlsx')
        return
      }
      this.importSubmitting = true
      try {
        const data = await importProductsXlsx(rawFile, {
          dryRun: this.importDryRun,
          upsertBySku: this.importUpsertBySku
        })
        this.importResult = data || null
        if (!this.importDryRun) {
          await this.refreshProducts()
        }
        if (Number((data && data.failedCount) || 0) === 0) {
          this.$message.success(this.importDryRun
            ? `Validate thành công ${Number((data && data.successCount) || 0)} dòng`
            : `Import thành công ${Number((data && data.successCount) || 0)} sản phẩm`)
        } else {
          this.$message.warning(
            `${this.importDryRun ? 'Validate' : 'Import'} xong: thành công ${Number((data && data.successCount) || 0)}, lỗi ${Number((data && data.failedCount) || 0)}`
          )
        }
      } catch (error) {
        this.$message.error((error && error.response && error.response.data && error.response.data.message) || 'Import XLSX thất bại')
      } finally {
        this.importSubmitting = false
      }
    },
    handleCancelCreate() {
      this.drawerVisible = false
      if (this.isCreateOnly) {
        this.$emit('close-request')
      }
    },
    handleDrawerClosed() {
      if (this.isCreateOnly) {
        this.$emit('close-request')
      }
    }
  }
}
</script>

<style scoped lang="scss">
.product-admin-page {
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
  box-sizing: border-box;
}

.panel-header {
  margin-bottom: 10px;
}

.panel-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  flex-wrap: wrap;
}

.action-buttons {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.bulk-toolbar {
  margin-bottom: 8px;
  border: 1px dashed #cfd8e3;
  padding: 8px 10px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;

  p {
    margin: 0;
    color: #475569;
    font-size: 13px;
  }
}

.bulk-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.search-input {
  width: 280px;
}

.table-wrap {
  width: 100%;
}

.inventory-table {
  width: 100%;
  ::v-deep .el-table__cell {
    padding: 8px 0;
    vertical-align: middle;
  }
  ::v-deep .cell {
    line-height: 1.35;
  }
}

.pagination-wrap {
  margin-top: 10px;
  display: flex;
  justify-content: flex-end;
}

.product-form-drawer {
  ::v-deep .el-drawer__body {
    padding: 16px;
    overflow: hidden;
    position: relative;
    display: flex;
    flex-direction: column;
  }
}

.drawer-scroll-content {
  position: relative;
  overflow-y: auto;
  flex: 1;
  padding-bottom: 12px;
}

.drawer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding-top: 10px;
  border-top: 1px solid #e5e7eb;
  background: #fff;
}

.deleted-product-drawer {
  ::v-deep .el-drawer__body {
    padding: 14px;
  }
}

.inventory-drawer {
  ::v-deep .el-drawer__body {
    padding: 14px;
    overflow-y: auto;
  }
}

.deleted-drawer-head {
  margin-bottom: 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;

  p {
    margin: 0;
    font-size: 13px;
    color: #6b7280;
  }
}

.deleted-empty {
  border: 1px dashed #d1d5db;
  padding: 14px;
  text-align: center;
  color: #6b7280;
}

.deleted-list {
  display: grid;
  gap: 8px;
}

.inventory-log-box {
  margin-top: 14px;

  h4 {
    margin: 0 0 8px;
    font-size: 14px;
    font-weight: 700;
  }
}

.inventory-alert-wrap {
  width: 100%;
}

.deleted-card {
  border: 1px solid #e5e7eb;
  border-radius: 0;
  padding: 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.deleted-main {
  min-width: 0;
}

.deleted-name {
  margin: 0 0 4px;
  font-size: 14px;
  font-weight: 700;
  color: #111827;
}

.deleted-meta {
  margin: 0;
  color: #6b7280;
  font-size: 12px;
  word-break: break-word;
}

.product-image-uploader {
  width: 100%;
}

.upload-tip {
  margin: 8px 0 0;
  font-size: 12px;
  color: #6b7280;
}

.variant-block {
  border: 1px solid #e5e7eb;
  background: #fafafa;
  padding: 12px;
  margin-top: 8px;

  h4 {
    margin: 0 0 10px;
    font-size: 13px;
    font-weight: 700;
    text-transform: uppercase;
    color: #374151;
  }
}

.variant-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 10px;
}

.variant-item {
  border: 1px solid #e5e7eb;
  background: #fff;
  border-radius: 0;
  padding: 10px;
}

.variant-item + .variant-item {
  margin-top: 10px;
}

.variant-item-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;

  p {
    margin: 0;
    font-size: 12px;
    font-weight: 700;
    color: #4b5563;
    text-transform: uppercase;
  }
}

.sku-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
  align-items: center;
}

.inline-label {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.inline-plus-btn {
  width: 20px;
  height: 20px;
  min-height: 20px;
  min-width: 20px;
  padding: 0;
}

.option-side-panel-backdrop {
  position: absolute;
  inset: 0;
  background: rgba(17, 24, 39, 0.28);
  z-index: 10;
}

.option-side-panel {
  position: absolute;
  top: 0;
  right: 0;
  width: min(320px, 100%);
  height: 100%;
  background: #ffffff;
  border-left: 1px solid #e5e7eb;
  box-shadow: -10px 0 24px rgba(0, 0, 0, 0.14);
  z-index: 11;
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.option-side-panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;

  h5 {
    margin: 0;
    font-size: 14px;
    font-weight: 700;
  }
}

.option-side-panel-actions {
  margin-top: auto;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.option-panel-slide-enter-active,
.option-panel-slide-leave-active {
  transition: transform 0.22s ease;
}

.option-panel-slide-enter,
.option-panel-slide-leave-to {
  transform: translateX(100%);
}

.option-panel-fade-enter-active,
.option-panel-fade-leave-active {
  transition: opacity 0.2s ease;
}

.option-panel-fade-enter,
.option-panel-fade-leave-to {
  opacity: 0;
}

.product-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;

  .thumb {
    width: 40px;
    height: 40px;
    object-fit: cover;
    border-radius: 0;
    border: 1px solid #e5e7eb;
  }

  .name {
    margin: 0;
    font-weight: 600;
  }

  .meta {
    margin: 0;
    font-size: 12px;
    color: #6b7280;
  }
}

.product-stats {
  display: grid;
  gap: 3px;
}

.product-stats span {
  font-size: 12px;
  color: #334155;
}

.action-cell {
  display: flex;
  gap: 8px;
  justify-content: center;
  align-items: center;
  flex-wrap: nowrap;
}

::v-deep .admin-action-cell .el-button.is-circle {
  width: 30px;
  height: 30px;
  padding: 0;
}

.import-guide {
  margin-bottom: 12px;
  color: #374151;
  font-size: 13px;
  line-height: 1.5;

  p {
    margin: 0 0 6px;
    font-weight: 600;
  }

  code {
    display: block;
    background: #f8fafc;
    border: 1px solid #e2e8f0;
    padding: 8px;
    white-space: normal;
    word-break: break-word;
  }
}

.import-toolbar {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 10px;
  flex-wrap: wrap;
}

.import-upload {
  margin-bottom: 12px;
}

.import-result {
  margin-top: 10px;
}

.import-result-summary {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
}

@media (max-width: 1100px) {
  .panel-actions {
    flex-direction: column;
    align-items: stretch;
  }
}

@media (max-width: 700px) {
  .panel-actions {
    width: 100%;
    flex-direction: column;
    align-items: stretch;
  }

  .search-input {
    width: 100%;
  }

  .action-buttons {
    width: 100%;
    display: grid;
    grid-template-columns: 1fr 1fr;
  }

  .drawer-actions {
    flex-direction: column-reverse;
    align-items: stretch;
  }

  .product-form-drawer {
    ::v-deep .el-drawer {
      width: 100% !important;
      max-width: 100% !important;
    }
  }

  .inventory-drawer {
    ::v-deep .el-drawer {
      width: 100% !important;
      max-width: 100% !important;
    }
  }
}
</style>

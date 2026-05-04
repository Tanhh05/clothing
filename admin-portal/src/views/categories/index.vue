<template>
  <section v-loading="loading" class="categories-admin-page admin-page-shell admin-page">
    <div class="admin-card inventory-panel">
      <div class="panel-header">
        <div class="panel-actions admin-toolbar">
          <el-input
            v-model="keyword"
            placeholder="Tìm theo tên / slug / mô tả"
            clearable
            class="search-input"
            @keyup.enter.native="reload(0)"
          >
            <i slot="prefix" class="el-input__icon el-icon-search" />
          </el-input>
          <div class="action-buttons">
            <el-button class="admin-ghost-btn" @click="reload(0)">Làm mới</el-button>
            <el-button type="primary" class="admin-primary-btn" @click="openCreate">+ Thêm danh mục</el-button>
          </div>
        </div>
      </div>

      <div class="table-wrap">
        <el-table
          :data="categories"
          border
          stripe
          :size="elementSize"
          class="inventory-table admin-table"
          empty-text="Chưa có danh mục"
          table-layout="fixed"
        >
          <el-table-column label="Tên danh mục" min-width="220" prop="name" />
          <el-table-column label="Slug" min-width="200" prop="slug" />
          <el-table-column label="Mô tả ngắn" min-width="240" prop="shortContent" show-overflow-tooltip />
          <el-table-column label="Danh mục cha" min-width="150">
            <template slot-scope="{ row }">
              {{ resolveCategoryName(row.parentId) }}
            </template>
          </el-table-column>
          <el-table-column label="Trạng thái" width="120" align="center">
            <template slot-scope="{ row }">
              <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">{{ formatStatus(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="Thao tác" width="120" align="center">
            <template slot-scope="{ row }">
              <div class="admin-action-cell">
                <el-tooltip content="Sửa" placement="top">
                  <el-button
                    :size="elementSize"
                    icon="el-icon-edit-outline"
                    class="admin-action-btn admin-action-edit is-icon-only"
                    circle
                    @click="openEdit(row)"
                  />
                </el-tooltip>
                <el-tooltip content="Xóa" placement="top">
                  <el-button
                    :size="elementSize"
                    icon="el-icon-delete"
                    class="admin-action-btn admin-action-delete is-icon-only"
                    circle
                    @click="handleDelete(row)"
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
        @pagination="handlePagination"
      />
    </div>

    <el-dialog :visible.sync="dialogVisible" :title="dialogMode === 'create' ? 'Thêm danh mục' : 'Sửa danh mục'" width="620px">
      <el-form :model="form" label-position="top">
        <el-form-item label="Tên danh mục">
          <el-input v-model="form.name" placeholder="Nhập tên danh mục" />
        </el-form-item>
        <el-row :gutter="10">
          <el-col :span="12">
            <el-form-item label="Slug">
              <el-input v-model="form.slug" placeholder="vd: ao-thun" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Danh mục cha">
              <el-select v-model="form.parentId" clearable filterable style="width: 100%" placeholder="Không có danh mục cha">
                <el-option
                  v-for="item in parentCategoryOptions"
                  :key="item.id"
                  :label="item.name"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="10">
          <el-col :span="12">
            <el-form-item label="Thứ tự hiển thị">
              <el-input v-model.number="form.displayOrder" type="number" min="0" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Trạng thái">
              <el-select v-model="form.status" style="width: 100%">
                <el-option label="Đang hoạt động" value="ACTIVE" />
                <el-option label="Ngưng hoạt động" value="INACTIVE" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="Mô tả ngắn">
          <el-input v-model="form.shortContent" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="dialogVisible = false">Hủy</el-button>
        <el-button type="primary" :loading="saving" @click="submitForm">
          {{ dialogMode === 'create' ? 'Tạo danh mục' : 'Lưu thay đổi' }}
        </el-button>
      </div>
    </el-dialog>
  </section>
</template>

<script>
import {
  fetchCategoriesAdmin,
  createCategoryAdmin,
  updateCategoryAdmin,
  deleteCategoryAdmin
} from '@/api/admin-management'
import Pagination from '@/components/Pagination'

export default {
  name: 'CategoriesManagement',
  components: { Pagination },
  data() {
    return {
      loading: false,
      saving: false,
      categories: [],
      keyword: '',
      debounceTimer: null,
      categoryNameMap: {},
      pageSize: 20,
      totalElements: 0,
      currentPage: 1,
      dialogVisible: false,
      dialogMode: 'create',
      editingId: null,
      form: {
        name: '',
        slug: '',
        shortContent: '',
        parentId: null,
        displayOrder: 0,
        status: 'ACTIVE'
      }
    }
  },
  computed: {
    elementSize() {
      const size = this.$store.getters.size
      return size === 'default' ? undefined : size
    },
    parentCategoryOptions() {
      return Object.entries(this.categoryNameMap)
        .map(([id, name]) => ({ id: Number(id), name }))
        .sort((a, b) => String(a.name || '').localeCompare(String(b.name || ''), 'vi'))
    }
  },
  watch: {
    keyword() {
      if (this.debounceTimer) clearTimeout(this.debounceTimer)
      this.debounceTimer = setTimeout(() => {
        this.reload(0)
      }, 300)
    }
  },
  created() {
    this.reload()
  },
  methods: {
    resolveCategoryName(parentId) {
      if (!parentId) return '-'
      return this.categoryNameMap[parentId] || `#${parentId}`
    },
    formatStatus(status) {
      const normalized = String(status || '').toUpperCase()
      if (normalized === 'ACTIVE') return 'Đang hoạt động'
      if (normalized === 'INACTIVE') return 'Ngưng hoạt động'
      return 'Không có'
    },
    resetForm() {
      this.form = {
        name: '',
        slug: '',
        shortContent: '',
        parentId: null,
        displayOrder: 0,
        status: 'ACTIVE'
      }
    },
    openCreate() {
      this.dialogMode = 'create'
      this.editingId = null
      this.resetForm()
      this.dialogVisible = true
    },
    openEdit(row) {
      this.dialogMode = 'edit'
      this.editingId = row.id
      this.form = {
        name: row.name || '',
        slug: row.slug || '',
        shortContent: row.shortContent || '',
        parentId: row.parentId || null,
        displayOrder: Number(row.displayOrder || 0),
        status: row.status || 'ACTIVE'
      }
      this.dialogVisible = true
    },
    async reload(page = 0) {
      this.loading = true
      try {
        const data = await fetchCategoriesAdmin({
          page,
          size: this.pageSize,
          sortBy: 'id',
          direction: 'asc',
          q: this.keyword ? this.keyword.trim() : undefined
        })
        this.categories = Array.isArray(data && data.content) ? data.content : []
        this.totalElements = Number((data && data.totalElements) || 0)
        this.currentPage = Number((data && data.page) || page) + 1
        await this.loadParentCategoryOptions()
      } catch (error) {
        this.$message.error((error && error.response && error.response.data && error.response.data.message) || 'Không tải được danh mục')
      } finally {
        this.loading = false
      }
    },
    async loadParentCategoryOptions() {
      try {
        const data = await fetchCategoriesAdmin({
          page: 0,
          size: 500,
          sortBy: 'name',
          direction: 'asc'
        })
        const all = Array.isArray(data && data.content) ? data.content : []
        this.categoryNameMap = all.reduce((acc, item) => {
          if (item && item.id != null) acc[item.id] = item.name || `#${item.id}`
          return acc
        }, {})
      } catch (error) {
        this.categoryNameMap = {}
      }
    },
    async submitForm() {
      if (!String(this.form.name || '').trim()) {
        this.$message.warning('Tên danh mục là bắt buộc')
        return
      }
      this.saving = true
      try {
        const payload = {
          name: String(this.form.name || '').trim(),
          slug: String(this.form.slug || '').trim() || undefined,
          shortContent: String(this.form.shortContent || '').trim() || undefined,
          parentId: this.form.parentId || null,
          displayOrder: Number(this.form.displayOrder || 0),
          status: this.form.status || 'ACTIVE'
        }
        if (this.dialogMode === 'create') {
          await createCategoryAdmin(payload)
          this.$message.success('Đã tạo danh mục')
        } else if (this.editingId) {
          await updateCategoryAdmin(this.editingId, payload)
          this.$message.success('Đã cập nhật danh mục')
        }
        this.dialogVisible = false
        await this.reload(Math.max(0, this.currentPage - 1))
      } catch (error) {
        this.$message.error((error && error.response && error.response.data && error.response.data.message) || 'Không lưu được danh mục')
      } finally {
        this.saving = false
      }
    },
    handlePagination({ page, limit }) {
      this.pageSize = Number(limit) || this.pageSize
      this.reload(Math.max(0, Number(page) - 1))
    },
    handleDelete(row) {
      this.$confirm(`Xóa danh mục "${row.name}"?`, 'Xác nhận', {
        confirmButtonText: 'Đồng ý',
        cancelButtonText: 'Hủy',
        type: 'warning'
      }).then(async() => {
        await deleteCategoryAdmin(row.id)
        this.$message.success('Đã xóa danh mục')
        await this.reload(Math.max(0, this.currentPage - 1))
      }).catch(() => {})
    }
  }
}
</script>

<style scoped>
.categories-admin-page {
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: 100%;
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

.table-wrap {
  width: 100%;
}

.inventory-table {
  width: 100%;
}

.admin-action-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  justify-content: center;
}
</style>

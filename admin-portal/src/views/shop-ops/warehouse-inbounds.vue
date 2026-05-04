<template>
  <section v-loading="loading" class="shopop-page admin-page-shell admin-page">
    <div class="admin-card inventory-panel">
      <div class="panel-header">
        <div class="panel-actions admin-toolbar">
          <h3 class="title">Nhập kho</h3>
          <div class="action-buttons">
            <el-button type="primary" class="admin-primary-btn" :size="elementSize" @click="openCreate">+ Tạo phiếu nhập</el-button>
            <el-button class="admin-ghost-btn" :size="elementSize" @click="loadData">Làm mới</el-button>
          </div>
        </div>
      </div>
      <div class="table-wrap">
        <el-table :data="rows" border stripe :size="elementSize" class="inventory-table admin-table" empty-text="Không có phiếu nhập" table-layout="fixed">
          <el-table-column label="ID" width="90" prop="id" />
          <el-table-column label="Mã phiếu" min-width="160" prop="code" />
          <el-table-column label="Nhà cung cấp" min-width="180" prop="supplier" />
          <el-table-column label="Tổng SL" width="120" prop="totalQuantity" />
          <el-table-column label="Tổng chi phí" width="140" prop="totalCost" />
          <el-table-column label="Ngày tạo" width="180">
            <template slot-scope="{ row }">{{ formatDate(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="Thao tác" width="130" align="center">
            <template slot-scope="{ row }">
              <el-button :size="elementSize" plain @click="openDetail(row)">Chi tiết</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <el-dialog :visible.sync="createVisible" title="Tạo phiếu nhập kho" width="860px">
      <el-form :model="form" label-position="top">
        <el-row :gutter="10">
          <el-col :span="8"><el-form-item label="Mã phiếu"><el-input v-model="form.code" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="Nhà cung cấp"><el-input v-model="form.supplier" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="Ngày tạo"><el-date-picker v-model="form.createdAt" type="datetime" style="width:100%" value-format="yyyy-MM-dd'T'HH:mm:ss" /></el-form-item></el-col>
        </el-row>

        <el-table :data="form.items" border stripe size="mini">
          <el-table-column label="SKU" min-width="220">
            <template slot-scope="{ row }">
              <el-autocomplete v-model="row.sku" :fetch-suggestions="querySku" placeholder="Nhập SKU" style="width: 100%" />
            </template>
          </el-table-column>
          <el-table-column label="SL" width="120">
            <template slot-scope="{ row }"><el-input v-model.number="row.quantity" type="number" min="1" /></template>
          </el-table-column>
          <el-table-column label="Giá vốn" width="150">
            <template slot-scope="{ row }"><el-input v-model.number="row.cost" type="number" min="0" /></template>
          </el-table-column>
          <el-table-column width="90" align="center">
            <template slot="header">
              <el-button size="mini" type="primary" plain @click="addItem">+ Dòng</el-button>
            </template>
            <template slot-scope="{ $index }">
              <el-button size="mini" type="danger" plain @click="removeItem($index)">Xóa</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-form>
      <div slot="footer">
        <el-button @click="createVisible = false">Hủy</el-button>
        <el-button type="primary" :loading="saving" @click="submitCreate">Tạo phiếu</el-button>
      </div>
    </el-dialog>

    <el-dialog :visible.sync="detailVisible" title="Chi tiết phiếu nhập" width="860px">
      <div v-if="detail">
        <el-descriptions :column="3" border style="margin-bottom: 12px">
          <el-descriptions-item label="Mã phiếu">{{ detail.code }}</el-descriptions-item>
          <el-descriptions-item label="Nhà cung cấp">{{ detail.supplier }}</el-descriptions-item>
          <el-descriptions-item label="Ngày tạo">{{ formatDate(detail.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="Số dòng">{{ detail.itemCount }}</el-descriptions-item>
          <el-descriptions-item label="Tổng SL">{{ detail.totalQuantity }}</el-descriptions-item>
          <el-descriptions-item label="Tổng chi phí">{{ detail.totalCost }}</el-descriptions-item>
        </el-descriptions>
        <el-table :data="detail.items || []" border stripe size="mini">
          <el-table-column label="SKU" min-width="220" prop="sku" />
          <el-table-column label="SL nhập" width="120" prop="quantity" />
          <el-table-column label="Giá vốn" width="140" prop="unitCost" />
          <el-table-column label="Thành tiền" width="140" prop="lineTotal" />
          <el-table-column label="Tồn hiện tại" width="130" prop="currentStock" />
        </el-table>
      </div>
      <el-empty v-else description="Không có dữ liệu chi tiết" />
    </el-dialog>
  </section>
</template>

<script>
import {
  createWarehouseInbound,
  fetchWarehouseInboundDetail,
  fetchWarehouseInboundPage,
  fetchWarehouseInboundSkus
} from '@/api/shop-operations'

export default {
  name: 'ShopOpWarehouseInboundsPage',
  data() {
    return {
      loading: false,
      saving: false,
      rows: [],
      createVisible: false,
      detailVisible: false,
      detail: null,
      form: this.defaultForm()
    }
  },
  created() {
    this.loadData()
  },
  computed: {
    elementSize() {
      const size = this.$store.getters.size
      return size === 'default' ? undefined : size
    }
  },
  methods: {
    defaultForm() {
      return {
        code: '',
        supplier: '',
        createdAt: null,
        items: [{ sku: '', quantity: 1, cost: 0 }]
      }
    },
    async loadData() {
      this.loading = true
      try {
        const data = await fetchWarehouseInboundPage({ page: 0, size: 20 })
        this.rows = Array.isArray(data && data.content) ? data.content : []
      } catch (e) {
        this.$message.error('Không tải được dữ liệu nhập kho')
      } finally {
        this.loading = false
      }
    },
    openCreate() {
      this.form = this.defaultForm()
      this.createVisible = true
    },
    addItem() {
      this.form.items.push({ sku: '', quantity: 1, cost: 0 })
    },
    removeItem(index) {
      if (this.form.items.length <= 1) return
      this.form.items.splice(index, 1)
    },
    async querySku(queryString, cb) {
      try {
        const data = await fetchWarehouseInboundSkus(queryString)
        const items = (Array.isArray(data) ? data : []).map(sku => ({ value: sku }))
        cb(items)
      } catch (e) {
        cb([])
      }
    },
    async submitCreate() {
      this.saving = true
      try {
        const payload = {
          code: String(this.form.code || '').trim(),
          supplier: String(this.form.supplier || '').trim(),
          createdAt: this.form.createdAt || null,
          items: (this.form.items || []).map(item => ({
            sku: String(item.sku || '').trim(),
            quantity: Number(item.quantity || 0),
            cost: Number(item.cost || 0)
          }))
        }
        await createWarehouseInbound(payload)
        this.$message.success('Đã tạo phiếu nhập kho')
        this.createVisible = false
        await this.loadData()
      } catch (e) {
        this.$message.error('Không tạo được phiếu nhập kho')
      } finally {
        this.saving = false
      }
    },
    async openDetail(row) {
      this.detailVisible = true
      this.detail = null
      try {
        const data = await fetchWarehouseInboundDetail(row.id)
        this.detail = data || null
      } catch (e) {
        this.$message.error('Không tải được chi tiết phiếu nhập')
      }
    },
    formatDate(value) {
      if (!value) return 'Không có'
      const d = new Date(value)
      if (Number.isNaN(d.getTime())) return 'Không có'
      return d.toLocaleString('vi-VN')
    }
  }
}
</script>

<style scoped>
.shopop-page {
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
</style>

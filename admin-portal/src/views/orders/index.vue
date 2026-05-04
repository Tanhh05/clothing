<template>
  <section v-loading="loading" class="admin-page">
    <div class="admin-card inventory-panel">
      <div class="admin-toolbar">
        <el-input
          v-model="keyword"
          clearable
          placeholder="Tìm theo mã đơn / khách hàng"
          class="search-input"
          @keyup.enter.native="reload(0)"
        >
          <i slot="prefix" class="el-input__icon el-icon-search" />
        </el-input>
        <el-select v-model="status" clearable placeholder="Trạng thái" class="status-select" @change="reload(0)">
          <el-option v-for="item in statusOptions" :key="item" :label="formatStatusLabel(item)" :value="item" />
        </el-select>
        <el-button class="admin-ghost-btn" @click="reload(0)">Làm mới</el-button>
      </div>

      <div v-if="selectedIds.length" class="bulk-toolbar">
        <p>Đã chọn {{ selectedIds.length }} đơn</p>
        <div class="bulk-actions">
          <el-button :size="elementSize" class="admin-ghost-btn" :loading="printing" @click="printSelectedInvoices">In hóa đơn</el-button>
          <el-button :size="elementSize" type="primary" class="admin-primary-btn" :loading="exportingExcel" @click="exportSelectedInvoicesExcel">Xuất Excel</el-button>
          <el-button :size="elementSize" @click="clearSelection">Bỏ chọn</el-button>
        </div>
      </div>

      <el-table
        ref="orderTableRef"
        :data="orders"
        border
        stripe
        :size="elementSize"
        class="admin-table"
        empty-text="Chưa có đơn hàng"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="48" />
        <el-table-column label="Đơn hàng" min-width="300">
          <template slot-scope="{ row }">
            <div class="title-cell">
              <strong>#{{ row.id || 'Không có' }}</strong>
              <span>{{ row.customerName || row.username || 'Khách lẻ' }}</span>
              <span class="sub-line">{{ row.address || 'Không có địa chỉ' }}</span>
              <span class="sub-line">Voucher: {{ row.appliedVoucherCode || 'Không có' }}</span>
              <span class="sub-line">Ghi chú: {{ row.note || 'Không có' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="SL SP" width="80" align="center">
          <template slot-scope="{ row }">
            {{ countItems(row.items) }}
          </template>
        </el-table-column>
        <el-table-column label="Tiền" min-width="210">
          <template slot-scope="{ row }">
            <div class="money-breakdown">
              <span>Tạm tính: {{ formatPrice(row.subTotal) }}</span>
              <span>Ship: {{ formatPrice(row.shippingFee) }}</span>
              <span>Giảm: {{ formatPrice(row.discountAmount) }}</span>
              <span class="money-total">Tổng: {{ formatPrice(row.totalPrice) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="TT / VC / Trạng thái" min-width="190">
          <template slot-scope="{ row }">
            <div class="status-stack">
              <span>TT: {{ row.paymentMethod || 'Không có' }}</span>
              <span>VC: {{ row.shippingStatus || 'Không có' }}</span>
              <span class="sub-line">{{ row.shippingCode || 'Không có mã vận đơn' }}</span>
              <el-tag :type="statusTag(row.status)">{{ formatStatusLabel(row.status) }}</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="Tạo lúc" width="150">
          <template slot-scope="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="Thao tác" width="86" align="center" fixed="right">
          <template slot-scope="{ row }">
            <el-button :size="elementSize" type="text" @click="openInvoiceDetail(row)">Chi tiết</el-button>
          </template>
        </el-table-column>
      </el-table>

      <pagination
        class="admin-pagination"
        :total="totalElements"
        :page.sync="currentPage"
        :limit.sync="pageSize"
        @pagination="handlePagination"
      />
    </div>

    <el-drawer
      title="Chi tiết hóa đơn"
      :visible.sync="invoiceDialogVisible"
      direction="rtl"
      size="46%"
      append-to-body
    >
      <div v-if="activeInvoice" class="invoice-detail">
        <div class="invoice-detail-grid">
          <div class="info-card">
            <h4>Thông tin đơn</h4>
            <p><span>Mã đơn:</span> #{{ activeInvoice.id || 'Không có' }}</p>
            <p><span>Trạng thái:</span> {{ formatStatusLabel(activeInvoice.status) }}</p>
            <p><span>Thanh toán:</span> {{ activeInvoice.paymentMethod || 'Không có' }}</p>
            <p><span>Vận chuyển:</span> {{ activeInvoice.shippingStatus || 'Không có' }}</p>
            <p><span>Mã vận đơn:</span> {{ activeInvoice.shippingCode || 'Không có' }}</p>
            <p><span>Tạo lúc:</span> {{ formatDate(activeInvoice.createdAt) }}</p>
          </div>
          <div class="info-card">
            <h4>Khách hàng</h4>
            <p><span>Tên:</span> {{ activeInvoice.customerName || 'Khách lẻ' }}</p>
            <p><span>Địa chỉ:</span> {{ activeInvoice.address || 'Không có' }}</p>
            <p><span>Voucher:</span> {{ activeInvoice.appliedVoucherCode || 'Không có' }}</p>
            <p><span>Ghi chú:</span> {{ activeInvoice.note || 'Không có' }}</p>
          </div>
        </div>

        <el-table
          :data="activeInvoice.items || []"
          size="mini"
          border
          stripe
          class="admin-table"
          empty-text="Không có sản phẩm"
          style="margin-top: 8px;"
        >
          <el-table-column label="SKU" min-width="140" prop="sku" />
          <el-table-column label="Sản phẩm" min-width="220" prop="productName" />
          <el-table-column label="SL" width="80" align="center" prop="quantity" />
          <el-table-column label="Đơn giá" width="140">
            <template slot-scope="{ row }">
              {{ formatPrice(row.price) }}
            </template>
          </el-table-column>
          <el-table-column label="Thành tiền" width="150">
            <template slot-scope="{ row }">
              {{ formatPrice(row.lineTotal || Number(row.quantity || 0) * Number(row.price || 0)) }}
            </template>
          </el-table-column>
        </el-table>

        <div class="invoice-summary">
          <p><span>Tổng số lượng:</span><strong>{{ countItems(activeInvoice.items) }}</strong></p>
          <p><span>Tạm tính:</span><strong>{{ formatPrice(activeInvoice.subTotal) }}</strong></p>
          <p><span>Phí ship:</span><strong>{{ formatPrice(activeInvoice.shippingFee) }}</strong></p>
          <p><span>Giảm giá:</span><strong>{{ formatPrice(activeInvoice.discountAmount) }}</strong></p>
          <p class="grand"><span>Tổng tiền:</span><strong>{{ formatPrice(activeInvoice.totalPrice) }}</strong></p>
        </div>
      </div>
    </el-drawer>

  </section>
</template>

<script>
import {
  fetchAdminOrders,
  fetchAdminOrderStatusOptions,
  fetchAdminInvoicesForPrint,
  exportAdminInvoicesExcel
} from '@/api/admin-management'
import Pagination from '@/components/Pagination'

const DEFAULT_ORDER_STATUSES = ['PENDING', 'CONFIRMED', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED']
const ORDER_STATUS_LABELS = {
  PENDING: 'Chờ xác nhận',
  CONFIRMED: 'Đã xác nhận',
  PROCESSING: 'Đang xử lý',
  SHIPPED: 'Đang giao',
  DELIVERED: 'Đã giao',
  CANCELLED: 'Đã hủy',
  COMPLETED: 'Hoàn tất',
  FAILED: 'Thất bại',
  RETURNED: 'Đã hoàn trả'
}

export default {
  name: 'OrdersManagement',
  components: { Pagination },
  data() {
    return {
      loading: false,
      printing: false,
      exportingExcel: false,
      orders: [],
      keyword: '',
      status: '',
      pageSize: 20,
      totalElements: 0,
      currentPage: 1,
      selectedIds: [],
      invoiceDialogVisible: false,
      activeInvoice: null,
      statusOptions: DEFAULT_ORDER_STATUSES
    }
  },
  computed: {
    elementSize() {
      const size = this.$store.getters.size
      return size === 'default' ? undefined : size
    }
  },
  created() {
    this.loadStatusOptions()
    this.reload()
  },
  methods: {
    async loadStatusOptions() {
      try {
        const data = await fetchAdminOrderStatusOptions()
        const fromApi = Array.isArray(data && data.statuses) ? data.statuses : []
        if (fromApi.length) {
          this.statusOptions = fromApi
        }
      } catch (error) {
        // Fallback to DEFAULT_ORDER_STATUSES
      }
    },
    async reload(page = 0) {
      this.loading = true
      try {
        const data = await fetchAdminOrders({
          page,
          size: this.pageSize,
          q: this.keyword ? this.keyword.trim() : undefined,
          status: this.status || undefined
        })
        this.orders = Array.isArray(data && data.content) ? data.content : []
        this.totalElements = Number((data && data.totalElements) || 0)
        this.currentPage = Number((data && data.page) || page) + 1
        this.selectedIds = []
      } catch (error) {
        this.$message.error((error && error.response && error.response.data && error.response.data.message) || 'Không tải được đơn hàng')
      } finally {
        this.loading = false
      }
    },
    handlePagination({ page, limit }) {
      this.pageSize = Number(limit) || this.pageSize
      this.reload(Math.max(0, Number(page) - 1))
    },
    handleSelectionChange(rows) {
      this.selectedIds = Array.isArray(rows) ? rows.map(item => item.id).filter(Boolean) : []
    },
    clearSelection() {
      this.selectedIds = []
      if (this.$refs.orderTableRef && typeof this.$refs.orderTableRef.clearSelection === 'function') {
        this.$refs.orderTableRef.clearSelection()
      }
    },
    openInvoiceDetail(row) {
      this.activeInvoice = row || null
      this.invoiceDialogVisible = true
    },
    async printSelectedInvoices() {
      if (!this.selectedIds.length || this.printing) return
      this.printing = true
      try {
        const invoices = await fetchAdminInvoicesForPrint(this.selectedIds)
        const rows = Array.isArray(invoices) ? invoices : []
        if (!rows.length) {
          this.$message.warning('Không có dữ liệu hóa đơn để in')
          return
        }
        const printWindow = window.open('', '_blank')
        if (!printWindow) {
          this.$message.error('Không mở được cửa sổ in')
          return
        }
        const html = this.buildPrintHtml(rows)
        printWindow.document.open()
        printWindow.document.write(html)
        printWindow.document.close()
        printWindow.focus()
        printWindow.print()
      } catch (error) {
        this.$message.error((error && error.response && error.response.data && error.response.data.message) || 'Không in hóa đơn được')
      } finally {
        this.printing = false
      }
    },
    async exportSelectedInvoicesExcel() {
      if (!this.selectedIds.length || this.exportingExcel) return
      this.exportingExcel = true
      try {
        const blob = await exportAdminInvoicesExcel(this.selectedIds)
        const url = window.URL.createObjectURL(blob)
        const link = document.createElement('a')
        const now = new Date()
        const stamp = `${now.getFullYear()}${String(now.getMonth() + 1).padStart(2, '0')}${String(now.getDate()).padStart(2, '0')}_${String(now.getHours()).padStart(2, '0')}${String(now.getMinutes()).padStart(2, '0')}${String(now.getSeconds()).padStart(2, '0')}`
        link.href = url
        link.download = `hoa-don-${stamp}.xlsx`
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)
        window.URL.revokeObjectURL(url)
      } catch (error) {
        this.$message.error((error && error.response && error.response.data && error.response.data.message) || 'Không xuất Excel được')
      } finally {
        this.exportingExcel = false
      }
    },
    buildPrintHtml(invoices) {
      const sections = invoices.map(order => {
        const items = Array.isArray(order.items) ? order.items : []
        const totalQuantity = items.reduce((acc, item) => acc + Number(item && item.quantity ? item.quantity : 0), 0)
        const shippingInfo = [order.shippingProvider, order.shippingCode].filter(Boolean).join(' - ')
        const statusHistory = Array.isArray(order.statusHistory) ? order.statusHistory : []
        const statusHistoryRows = statusHistory.map(entry => {
          const statusText = this.escapeHtml(this.formatStatusLabel(entry && entry.status))
          const changedAt = this.escapeHtml(this.formatDate(entry && entry.changedAt))
          return `<li><span>${statusText}</span><span>${changedAt}</span></li>`
        }).join('')
        const itemRows = items.map(item => {
          const sku = this.escapeHtml(item && item.sku ? item.sku : '')
          const name = this.escapeHtml(item && item.productName ? item.productName : '')
          const quantity = Number(item && item.quantity ? item.quantity : 0)
          const priceNumber = Number(item && item.price ? item.price : 0)
          const price = this.escapeHtml(this.formatPrice(priceNumber))
          const lineTotal = this.escapeHtml(this.formatPrice(item && item.lineTotal ? item.lineTotal : quantity * priceNumber))
          return `<tr><td>${sku}</td><td>${name}</td><td>${quantity}</td><td>${price}</td><td>${lineTotal}</td></tr>`
        }).join('')
        return `
          <section class="invoice">
            <div class="invoice-head">
              <div>
                <h2>Hóa đơn #${this.escapeHtml(order.id || '')}</h2>
                <p class="meta">Ngày tạo: ${this.escapeHtml(this.formatDate(order.createdAt))}</p>
              </div>
              <div class="total-box">
                <p>Tổng thanh toán</p>
                <strong>${this.escapeHtml(this.formatPrice(order.totalPrice))}</strong>
              </div>
            </div>

            <div class="invoice-grid">
              <div class="card">
                <h4>Thông tin đơn</h4>
                <p><span>Trạng thái đơn:</span> ${this.escapeHtml(this.formatStatusLabel(order.status))}</p>
                <p><span>Vận chuyển:</span> ${this.escapeHtml(order.shippingStatus || 'Không có')}</p>
                <p><span>Mã vận đơn:</span> ${this.escapeHtml(shippingInfo || 'Không có')}</p>
                <p><span>Thanh toán:</span> ${this.escapeHtml(order.paymentMethod || 'Không có')}</p>
              </div>
              <div class="card">
                <h4>Người nhận</h4>
                <p><span>Khách hàng:</span> ${this.escapeHtml(order.customerName || 'Khách lẻ')}</p>
                <p><span>Địa chỉ:</span> ${this.escapeHtml(order.address || 'Không có')}</p>
                <p><span>Ghi chú:</span> ${this.escapeHtml(order.note || 'Không có')}</p>
              </div>
            </div>

            <table>
              <thead><tr><th>SKU</th><th>Sản phẩm</th><th>SL</th><th>Đơn giá</th><th>Thành tiền</th></tr></thead>
              <tbody>${itemRows}</tbody>
            </table>

            <div class="summary">
              <p><span>Tổng số lượng:</span><strong>${this.escapeHtml(totalQuantity)}</strong></p>
              <p><span>Tạm tính:</span><strong>${this.escapeHtml(this.formatPrice(order.subTotal))}</strong></p>
              <p><span>Phí ship:</span><strong>${this.escapeHtml(this.formatPrice(order.shippingFee))}</strong></p>
              <p><span>Giảm giá:</span><strong>${this.escapeHtml(this.formatPrice(order.discountAmount))}</strong></p>
              <p class="grand"><span>Tổng tiền:</span><strong>${this.escapeHtml(this.formatPrice(order.totalPrice))}</strong></p>
            </div>

            ${statusHistoryRows ? `<div class="history"><h4>Lịch sử trạng thái</h4><ul>${statusHistoryRows}</ul></div>` : ''}
          </section>
        `
      }).join('<div class="page-break"></div>')

      return `
        <!doctype html>
        <html lang="vi">
        <head>
          <meta charset="utf-8" />
          <title>In hóa đơn</title>
          <style>
            body { font-family: Arial, sans-serif; color: #111; padding: 20px; font-size: 13px; }
            .invoice { margin-bottom: 20px; border: 1px solid #dfe4ea; padding: 12px; }
            .invoice-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; margin-bottom: 10px; }
            .invoice-head h2 { margin: 0 0 6px; font-size: 20px; }
            .meta { margin: 0; color: #4b5563; }
            .total-box { border: 1px solid #d1d5db; padding: 8px 10px; text-align: right; min-width: 180px; }
            .total-box p { margin: 0 0 4px; color: #6b7280; font-size: 12px; }
            .total-box strong { font-size: 18px; }
            .invoice-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; margin-bottom: 10px; }
            .card { border: 1px solid #e5e7eb; padding: 8px 10px; }
            .card h4 { margin: 0 0 8px; font-size: 13px; }
            .card p { margin: 4px 0; display: flex; gap: 8px; }
            .card p span { min-width: 100px; color: #6b7280; }
            table { width: 100%; border-collapse: collapse; margin: 8px 0; }
            th, td { border: 1px solid #ddd; padding: 6px 8px; text-align: left; }
            th { background: #f6f7f9; }
            .summary { margin-top: 8px; margin-left: auto; width: 360px; }
            .summary p { margin: 0; display: flex; justify-content: space-between; padding: 4px 0; border-bottom: 1px dashed #e5e7eb; }
            .summary .grand { border-bottom: 0; padding-top: 8px; font-size: 15px; }
            .history { margin-top: 10px; }
            .history h4 { margin: 0 0 6px; font-size: 13px; }
            .history ul { margin: 0; padding: 0; list-style: none; border: 1px solid #e5e7eb; }
            .history li { display: flex; justify-content: space-between; gap: 10px; padding: 6px 8px; border-bottom: 1px solid #f1f5f9; }
            .history li:last-child { border-bottom: 0; }
            .page-break { page-break-after: always; }
            .page-break:last-child { display: none; }
          </style>
        </head>
        <body>${sections}</body>
        </html>
      `
    },
    escapeHtml(value) {
      return String(value == null ? '' : value)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;')
    },
    statusTag(status) {
      const normalized = String(status || '').toUpperCase()
      if (normalized === 'DELIVERED') return 'success'
      if (normalized === 'CANCELLED') return 'danger'
      if (normalized === 'SHIPPED') return 'warning'
      return 'info'
    },
    formatStatusLabel(status) {
      const normalized = String(status || '').toUpperCase()
      return ORDER_STATUS_LABELS[normalized] || normalized || 'Không xác định'
    },
    countItems(items) {
      const list = Array.isArray(items) ? items : []
      return list.reduce((acc, item) => acc + Number(item && item.quantity ? item.quantity : 0), 0)
    },
    formatPrice(value) {
      return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(Number(value) || 0)
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

<style scoped lang="scss">
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

.sub-line {
  color: #6b7280;
  font-size: 12px;
  margin-top: 2px;
}

.money-breakdown {
  display: flex;
  flex-direction: column;
  gap: 2px;
  font-size: 12px;
  color: #374151;
}

.money-total {
  font-weight: 600;
  margin-top: 2px;
}

.status-stack {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.invoice-detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.info-card {
  border: 1px solid #e5e7eb;
  padding: 6px 8px;
}

.info-card h4 {
  margin: 0 0 6px;
  font-size: 12px;
}

.info-card p {
  margin: 2px 0;
  display: flex;
  gap: 8px;
  font-size: 12px;
  line-height: 1.35;
}

.info-card p span {
  min-width: 74px;
  color: #6b7280;
}

.invoice-summary {
  margin-top: 8px;
  margin-left: auto;
  width: 280px;
}

.invoice-summary p {
  margin: 0;
  display: flex;
  justify-content: space-between;
  padding: 3px 0;
  border-bottom: 1px dashed #e5e7eb;
  font-size: 12px;
}

.invoice-summary .grand {
  border-bottom: 0;
  padding-top: 6px;
  font-size: 13px;
}
</style>

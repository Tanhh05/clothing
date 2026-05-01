import request from '@/utils/request'

export function fetchProducts(params) {
  const safeParams = params || {}
  const normalizedSize = Math.min(Math.max(Number(safeParams.size || 20), 1), 100)
  return request({
    url: '/products',
    method: 'get',
    params: {
      page: Number(safeParams.page || 0),
      size: Number.isNaN(normalizedSize) ? 20 : normalizedSize,
      sortBy: safeParams.sortBy || 'id',
      direction: safeParams.direction || 'desc',
      category: safeParams.category || undefined,
      q: safeParams.q || undefined
    }
  })
}

export function fetchDeletedProducts() {
  return request({
    url: '/products/deleted',
    method: 'get'
  })
}

export function fetchProductByKey(productKey) {
  return request({
    url: `/products/${productKey}`,
    method: 'get'
  })
}

export function createProduct(data) {
  return request({
    url: '/products',
    method: 'post',
    data
  })
}

export function updateProduct(id, data) {
  return request({
    url: `/products/${id}`,
    method: 'put',
    data
  })
}

export function deleteProduct(id) {
  return request({
    url: `/products/${id}`,
    method: 'delete'
  })
}

export function restoreProduct(id) {
  return request({
    url: `/products/${id}/restore`,
    method: 'put'
  })
}

export function fetchVariantOptions(type) {
  return request({
    url: '/products/variant-options',
    method: 'get',
    params: { type }
  })
}

export function createVariantOption(type, value) {
  return request({
    url: '/products/variant-options',
    method: 'post',
    params: { type },
    data: { value }
  })
}

export function fetchCategories() {
  return request({
    url: '/categories',
    method: 'get',
    params: {
      page: 0,
      size: 100,
      sortBy: 'id',
      direction: 'asc'
    }
  })
}

export function bulkDeleteProducts(ids) {
  return request({
    url: '/products/bulk/delete',
    method: 'post',
    data: { ids }
  })
}

export function bulkRestoreProducts(ids) {
  return request({
    url: '/products/bulk/restore',
    method: 'post',
    data: { ids }
  })
}

export function bulkUpdateProductStatus(ids, status) {
  return request({
    url: '/products/bulk/status',
    method: 'patch',
    data: { ids, status }
  })
}

export function fetchInventoryAlerts(threshold = 5) {
  return request({
    url: '/products/inventory-alerts',
    method: 'get',
    params: { threshold }
  })
}

export function fetchInventoryLogs(variantId) {
  return request({
    url: '/products/inventory-logs',
    method: 'get',
    params: { variantId: variantId || undefined }
  })
}

export function importProductsXlsx(file, options = {}) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/products/import/xlsx',
    method: 'post',
    params: {
      dryRun: Boolean(options.dryRun),
      upsertBySku: Boolean(options.upsertBySku)
    },
    data: formData
  })
}

export function downloadProductImportTemplate() {
  return request({
    url: '/products/import/template',
    method: 'get',
    responseType: 'blob'
  })
}

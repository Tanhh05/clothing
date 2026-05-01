import request from '@/utils/request'

function toSafePage(value, fallback = 0) {
  const n = Number(value)
  return Number.isFinite(n) && n >= 0 ? Math.floor(n) : fallback
}

function toSafeSize(value, fallback = 20) {
  const n = Number(value)
  if (!Number.isFinite(n)) return fallback
  return Math.min(Math.max(Math.floor(n), 1), 100)
}

export function fetchAdminSummary() {
  return request({
    url: '/orders/summary',
    method: 'get'
  })
}

export function fetchAdminOrders(params) {
  const safeParams = params || {}
  return request({
    url: '/orders/admin',
    method: 'get',
    params: {
      page: toSafePage(safeParams.page, 0),
      size: toSafeSize(safeParams.size, 20),
      sortBy: safeParams.sortBy || 'id',
      direction: safeParams.direction || 'desc',
      q: safeParams.q || undefined,
      status: safeParams.status || undefined
    }
  })
}

export function fetchAdminOrderStatusOptions() {
  return request({
    url: '/orders/admin/status-options',
    method: 'get'
  })
}

export function updateAdminOrderStatus(orderId, payload) {
  return request({
    url: `/orders/${orderId}/status`,
    method: 'patch',
    data: payload
  })
}

export function bulkUpdateAdminOrderStatus(ids, status) {
  return request({
    url: '/orders/bulk/status',
    method: 'patch',
    data: { ids, status }
  })
}

export function fetchCategoriesAdmin(params) {
  const safeParams = params || {}
  return request({
    url: '/categories',
    method: 'get',
    params: {
      page: toSafePage(safeParams.page, 0),
      size: toSafeSize(safeParams.size, 20),
      sortBy: safeParams.sortBy || 'id',
      direction: safeParams.direction || 'asc',
      q: safeParams.q || undefined
    }
  })
}

export function createCategoryAdmin(payload) {
  return request({
    url: '/categories',
    method: 'post',
    data: payload
  })
}

export function updateCategoryAdmin(id, payload) {
  return request({
    url: `/categories/${id}`,
    method: 'put',
    data: payload
  })
}

export function deleteCategoryAdmin(id) {
  return request({
    url: `/categories/${id}`,
    method: 'delete'
  })
}

export function fetchAdminCustomers(params) {
  const safeParams = params || {}
  return request({
    url: '/admin/users',
    method: 'get',
    params: {
      page: toSafePage(safeParams.page, 0),
      size: toSafeSize(safeParams.size, 20),
      sortBy: safeParams.sortBy || 'id',
      direction: safeParams.direction || 'desc',
      q: safeParams.q || undefined,
      status: safeParams.status || undefined
    }
  })
}

export function updateAdminCustomerStatus(id, status) {
  return request({
    url: `/admin/users/${id}/status`,
    method: 'patch',
    data: { status }
  })
}

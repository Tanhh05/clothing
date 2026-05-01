import request from '@/utils/request'

export function fetchAdminVouchers() {
  return request({
    url: '/admin/vouchers',
    method: 'get'
  })
}

export function createAdminVoucher(payload) {
  return request({
    url: '/admin/vouchers',
    method: 'post',
    data: payload
  })
}

export function updateAdminVoucher(id, payload) {
  return request({
    url: `/admin/vouchers/${id}`,
    method: 'put',
    data: payload
  })
}

export function deleteAdminVoucher(id) {
  return request({
    url: `/admin/vouchers/${id}`,
    method: 'delete'
  })
}

export function fetchAdminBanners() {
  return request({
    url: '/admin/banners',
    method: 'get'
  })
}

export function createAdminBanner(payload) {
  return request({
    url: '/admin/banners',
    method: 'post',
    data: payload
  })
}

export function updateAdminBanner(id, payload) {
  return request({
    url: `/admin/banners/${id}`,
    method: 'put',
    data: payload
  })
}

export function deleteAdminBanner(id) {
  return request({
    url: `/admin/banners/${id}`,
    method: 'delete'
  })
}

export function fetchAdminReturns(status) {
  return request({
    url: '/admin/returns',
    method: 'get',
    params: { status: status || undefined }
  })
}

export function updateAdminReturnStatus(id, payload) {
  return request({
    url: `/admin/returns/${id}/status`,
    method: 'patch',
    data: payload
  })
}

export function fetchWarehouseInboundList() {
  return request({
    url: '/admin/warehouse-inbounds',
    method: 'get'
  })
}

export function fetchWarehouseInboundPage(params = {}) {
  return request({
    url: '/admin/warehouse-inbounds/page',
    method: 'get',
    params: {
      page: Number(params.page || 0),
      size: Number(params.size || 10),
      sortBy: params.sortBy || 'id',
      direction: params.direction || 'desc',
      q: params.q || undefined
    }
  })
}

export function fetchWarehouseInboundDetail(id) {
  return request({
    url: `/admin/warehouse-inbounds/${id}`,
    method: 'get'
  })
}

export function fetchWarehouseInboundSkus(q) {
  return request({
    url: '/admin/warehouse-inbounds/skus',
    method: 'get',
    params: { q: q || undefined }
  })
}

export function createWarehouseInbound(payload) {
  return request({
    url: '/admin/warehouse-inbounds',
    method: 'post',
    data: payload
  })
}

export function fetchAdminAuditLogs() {
  return request({
    url: '/admin/audit-logs',
    method: 'get'
  })
}

export function fetchAdminNotifications() {
  return request({
    url: '/admin/notifications',
    method: 'get'
  })
}

export function fetchAdminStoreSettings() {
  return request({
    url: '/admin/store-settings',
    method: 'get'
  })
}

export function updateAdminStoreSettings(payload) {
  return request({
    url: '/admin/store-settings',
    method: 'put',
    data: payload
  })
}

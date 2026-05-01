import request from '@/utils/request'

export function login(data) {
  const usernameOrEmail = String(data?.usernameOrEmail || data?.username || '').trim()
  const password = String(data?.password || '')
  if (!usernameOrEmail || !password) {
    return Promise.reject(new Error('Vui lòng nhập tên đăng nhập/email và mật khẩu'))
  }

  const payload = {
    usernameOrEmail,
    password
  }
  return request({
    url: '/auth/login',
    method: 'post',
    data: payload
  }).then(res => {
    const token = res?.accessToken || ''
    const refreshToken = res?.refreshToken || ''
    return { data: { token, refreshToken }}
  })
}

export function getInfo() {
  return request({
    url: '/user/me',
    method: 'get'
  }).then(res => {
    const roles = Array.isArray(res?.roles)
      ? res.roles
        .map(role => String(role || '').trim().toLowerCase().replace(/^role_/, ''))
        .filter(Boolean)
      : []
    const fullName = res?.fullName || res?.username || 'Admin'
    return {
      data: {
        roles,
        name: fullName,
        avatar: 'https://wpimg.wallstcn.com/f778738c-e4f8-4870-b634-56703b4acafe.gif',
        introduction: `Clothing user: ${res?.username || 'N/A'}`
      }
    }
  })
}

export function getMyProfile() {
  return request({
    url: '/user/me',
    method: 'get'
  })
}

export function updateMyProfile(payload) {
  return request({
    url: '/user/me',
    method: 'put',
    data: payload
  })
}

export function changeMyPassword(payload) {
  return request({
    url: '/user/me/password',
    method: 'patch',
    data: payload
  })
}

export function getMyNotifications() {
  return request({
    url: '/user/notifications',
    method: 'get'
  })
}

export function markNotificationAsRead(id) {
  return request({
    url: `/user/notifications/${id}/read`,
    method: 'patch'
  })
}

export function logout(refreshToken) {
  const safeRefreshToken = String(refreshToken || '').trim()
  if (!safeRefreshToken) {
    return Promise.resolve({ data: true })
  }
  return request({
    url: '/auth/logout',
    method: 'post',
    data: { refreshToken: safeRefreshToken }
  }).catch(() => {
    return { data: true }
  })
}

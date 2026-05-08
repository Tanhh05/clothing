import axios from 'axios'
import { MessageBox, Message } from 'element-ui'
import store from '@/store'
import { getToken } from '@/utils/auth'

const ADMIN_ERROR_ENDPOINT = process.env.VUE_APP_ADMIN_ERROR_ENDPOINT || '/admin/client-errors'
const ERROR_REPORT_TIMEOUT = 5000

// create an axios instance
const service = axios.create({
  baseURL: process.env.VUE_APP_BASE_API, // url = base url + request url
  // withCredentials: true, // send cookies when cross-domain requests
  timeout: 30000 // request timeout
})

const reporter = axios.create({
  baseURL: process.env.VUE_APP_BASE_API,
  timeout: ERROR_REPORT_TIMEOUT
})

function isAdminErrorReportRequest(error) {
  const url = String(error?.config?.url || '')
  return url.includes(ADMIN_ERROR_ENDPOINT)
}

function mapUserFriendlyError(error) {
  const status = error?.response?.status
  const body = error?.response?.data
  const apiMessage = body?.message || body?.data?.message
  const timeout = error?.code === 'ECONNABORTED'
  const networkDown = !status

  if (timeout || networkDown) {
    return 'Không thể kết nối hệ thống dữ liệu. Vui lòng thử lại sau.'
  }
  if ([500, 502, 503, 504].includes(status)) {
    return 'Hệ thống dữ liệu đang gián đoạn. Vui lòng thử lại sau.'
  }
  return apiMessage || error.message || 'Có lỗi xảy ra'
}

function reportClientError(error) {
  if (isAdminErrorReportRequest(error)) return

  const token = getToken()
  const payload = {
    type: 'ADMIN_FE_DATA_LOAD_ERROR',
    message: error?.response?.data?.message || error?.message || 'Unknown error',
    status: error?.response?.status || null,
    method: String(error?.config?.method || '').toUpperCase(),
    requestUrl: String(error?.config?.url || ''),
    pageUrl: window.location.href,
    time: new Date().toISOString()
  }

  const headers = token ? { Authorization: `Bearer ${token}` } : undefined
  reporter.post(ADMIN_ERROR_ENDPOINT, payload, { headers }).catch(() => {})
}

// request interceptor
service.interceptors.request.use(
  config => {
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => {
    // do something with request error
    console.log(error) // for debug
    return Promise.reject(error)
  }
)

// response interceptor
service.interceptors.response.use(
  response => {
    const payload = response?.data
    if (payload && typeof payload === 'object' && Object.prototype.hasOwnProperty.call(payload, 'data')) {
      return payload.data
    }
    return payload
  },
  error => {
    const status = error?.response?.status
    const message = mapUserFriendlyError(error)
    const url = String(error?.config?.url || '')
    const isAuthLoginRequest = url.includes('/auth/login')

    if (status === 401 && !isAuthLoginRequest) {
      MessageBox.confirm('Phiên đăng nhập đã hết hạn. Đăng nhập lại?', 'Thông báo', {
        confirmButtonText: 'Đăng nhập lại',
        cancelButtonText: 'Hủy',
        type: 'warning'
      }).then(() => {
        store.dispatch('user/resetToken').then(() => {
          location.reload()
        })
      }).catch(() => {})
    }

    if (!isAdminErrorReportRequest(error)) {
      Message({
        message,
        type: 'error',
        duration: 5 * 1000
      })
    }
    reportClientError(error)
    return Promise.reject(error)
  }
)

export default service

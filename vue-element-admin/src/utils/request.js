import axios from 'axios'
import { MessageBox, Message } from 'element-ui'
import store from '@/store'
import { getToken } from '@/utils/auth'

// create an axios instance
const service = axios.create({
  baseURL: process.env.VUE_APP_BASE_API, // url = base url + request url
  // withCredentials: true, // send cookies when cross-domain requests
  timeout: 30000 // request timeout
})

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
    return response.data
  },
  error => {
    const status = error?.response?.status
    const message = error?.response?.data?.message || error.message || 'Error'
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

    Message({
      message,
      type: 'error',
      duration: 5 * 1000
    })
    return Promise.reject(error)
  }
)

export default service

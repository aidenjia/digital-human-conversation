import type { AxiosError, AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import Axios from 'axios'
import { getToken, notifyUnauthorized } from '@/utils/auth'

let reLoginTag = true
const REQUEST_URL = import.meta.env.VITE_API_BASE

const codeMessage: Record<number, string> = {
  200: '服务器成功返回请求的数据。',
  201: '新建或修改数据成功。',
  202: '一个请求已经进入后台排队（异步任务）。',
  204: '删除数据成功。',
  400: '发出的请求有错误，服务器没有进行新建或修改数据的操作。',
  401: '【401】未获得授权。',
  403: '【403】访问被禁止。',
  404: '【404】未找到对应的服务。',
  406: '请求的格式不可得。',
  410: '请求的资源被永久删除，且不会再得到的。',
  422: '当创建一个对象时，发生一个验证错误。',
  500: '服务器发生错误，请检查服务器。',
  502: '网关错误。',
  503: '服务不可用，服务器暂时过载或维护。',
  504: '网关超时。',
}

const request = Axios.create({
  baseURL: REQUEST_URL || '',
  headers: {
    'Content-Type': 'application/json',
  },
})

request.interceptors.request.use(async (config: InternalAxiosRequestConfig<unknown>) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (response: AxiosResponse) => {
    if (response.status == 200) {
      const data = response.data as api.BaseRes<unknown>
      // 多数接口 code=0；试听等接口使用 HTTP 风格 code=200
      if (data.code == 0 || data.code == 200) {
        data.success = true
      } else {
        data.success = false
      }
      return data as unknown as AxiosResponse
    }
    throw new Error('接口请求失败， response=' + JSON.stringify(response))
  },
  async (error: AxiosError) => {
    const data = error.response?.data as api.BaseRes<unknown>

    if (data) {
      if (data.code == 401 || data.code == 409) {
        if (reLoginTag) {
          reLoginTag = false
          notifyUnauthorized()
          setTimeout(() => {
            reLoginTag = true
          }, 1000)
        }
        return Promise.reject(error)
      }
    }

    if (error.response?.status === 401 || error.response?.status === 409) {
      if (reLoginTag) {
        reLoginTag = false
        notifyUnauthorized()
        setTimeout(() => {
          reLoginTag = true
        }, 1000)
      }
      return Promise.reject(error)
    }

    if (error.response?.status && error.response.status != 200) {
      const errorText = codeMessage[error.response.status] || `请求错误 ${error.response.status}`
      throw new Error('接口请求失败， response=' + JSON.stringify(errorText))
    }

    return Promise.reject(error)
  },
)

export default request

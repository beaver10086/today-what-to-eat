import axios from 'axios'

export const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 10_000,
})

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('what-to-eat-token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

request.interceptors.response.use(
  (response) => {
    const body = response.data as { code?: number; message?: string; data?: unknown }
    if (typeof body?.code === 'number' && body.code !== 0) {
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return body && 'data' in body ? { ...response, data: body.data } : response
  },
  (error: unknown) => {
    if (axios.isAxiosError(error)) {
      const message = (error.response?.data as { message?: string } | undefined)?.message
      if (error.response?.status === 401) {
        localStorage.removeItem('what-to-eat-token')
        localStorage.removeItem('what-to-eat-user')
        window.dispatchEvent(new Event('session-expired'))
      }
      if (message) return Promise.reject(new Error(message))
    }
    return Promise.reject(error)
  },
)

import axios, { AxiosInstance } from 'axios'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/v1'

const client: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json'
  }
})

// Add token to requests if it exists
client.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// Handle errors globally
client.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('accessToken')
      localStorage.removeItem('refreshToken')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export interface LoginRequest {
  email: string
  password: string
}

export interface LoginResponse {
  userId: string
  requiresOtp: boolean
  otpChannels: string[]
}

export interface OtpVerifyRequest {
  userId: string
  otp: string
  channel: string
}

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  user: {
    id: string
    name: string
    email: string
    phone: string
    role: string
    businessId: string
    preferredLanguage: string
  }
}

export interface RegisterRequest {
  name: string
  phone: string
  email: string
  password: string
  businessName: string
  businessType: string
}

export interface ApiResponse<T> {
  success: boolean
  data: T | null
  message: string
  errors: any[]
}

export const authApi = {
  login: async (req: LoginRequest) => {
    const res = await client.post<ApiResponse<LoginResponse>>('/auth/login', req)
    return res.data
  },

  verifyOtp: async (req: OtpVerifyRequest) => {
    const res = await client.post<ApiResponse<AuthResponse>>('/auth/verify-otp', req)
    if (res.data.success && res.data.data) {
      localStorage.setItem('accessToken', res.data.data.accessToken)
      localStorage.setItem('refreshToken', res.data.data.refreshToken)
      localStorage.setItem('user', JSON.stringify(res.data.data.user))
    }
    return res.data
  },

  register: async (req: RegisterRequest) => {
    const res = await client.post<ApiResponse<any>>('/auth/register', req)
    return res.data
  },

  refreshToken: async (refreshToken: string) => {
    const res = await client.post<ApiResponse<AuthResponse>>('/auth/refresh', { refreshToken })
    if (res.data.success && res.data.data) {
      localStorage.setItem('accessToken', res.data.data.accessToken)
      localStorage.setItem('refreshToken', res.data.data.refreshToken)
    }
    return res.data
  }
}

export default client


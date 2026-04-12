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

// ── Domain Models ─────────────────────────────────────────────────────────────

export interface ProductResponse {
  id: string; businessId: string; sku: string; name: string; description: string
  buyingPrice: number; sellingPrice: number; profitPerItem: number; profitMargin: number
  currentStock: number; lowStockThreshold: number; isLowStock: boolean; isOutOfStock: boolean
  category: string; imageUrl: string | null; createdAt: string; updatedAt: string
}

export interface OrderItemResponse {
  id: string; productId: string; productName: string; quantity: number
  unitPrice: number; buyingPrice: number; lineTotal: number; lineProfit: number
}

export interface OrderResponse {
  id: string; orderNumber: string; businessId: string; customerId: string | null
  customerName: string; customerPhone: string; deliveryLocation: string
  items: OrderItemResponse[]; paymentStatus: string; deliveryStatus: string
  paymentMethod: string; mpesaTransactionCode: string | null; subtotal: number
  notes: string; createdAt: string; updatedAt: string
}

export interface PagedResponse<T> {
  data: T[]; total: number; page: number; pageSize: number; hasMore: boolean
}

export interface CustomerResponse {
  id: string; businessId: string; name: string; phone: string; email: string | null
  location: string; notes: string; loyaltyPoints: number; totalOrders: number
  totalSpent: number; isRepeatCustomer: boolean; createdAt: string
}

export interface ExpenseResponse {
  id: string; businessId: string; category: string; amount: number; description: string
  expenseDate: string; receiptUrl: string | null; recordedAt: string
}

export interface PaymentResponse {
  id: string; mpesaTransactionCode: string; phoneNumber: string; amount: number
  payerName: string; isReconciled: boolean; orderId: string | null; createdAt: string
}

export interface ProfitSummaryResponse {
  period: string; totalRevenue: number; totalCostOfGoods: number
  grossProfit: number; grossMargin: number; totalExpenses: number
  netProfit: number; netMargin: number; cashflowIn: number; cashflowOut: number
}

export interface TaxRateResponse {
  id: string; taxType: string; name: string; rate: number; ratePercent: number
  isActive: boolean; isInclusive: boolean; appliesTo: string; description: string
}

export interface TaxRemittanceResponse {
  id: string; taxType: string; periodStart: string; periodEnd: string
  taxableAmount: number; taxAmount: number; status: string
  receiptNumber: string | null; filedAt: string | null
}

export interface TaxSummaryResponse {
  totalVatDue: number; totalTotDue: number; totalWhtDue: number; period: string
}

export interface KraProfileResponse {
  pin: string; companyName: string; vatRegistrationNumber: string
  sdcId: string; serialNumber: string; environment: string
}

export interface KraComplianceStatus {
  pin: string | null; isEtimsRegistered: boolean; isVatRegistered: boolean
  complianceScore: number; etimsTransmissionRate: number
  pendingReturns: { returnType: string; period: string; dueDate: string; isOverdue: boolean; estimatedAmount: number }[]
  overdueReturns: { returnType: string; period: string; dueDate: string; isOverdue: boolean; estimatedAmount: number }[]
  recommendations: string[]; lastEtimsTransmission: string | null
}

export interface EtimsInvoiceResponse {
  id: string; invoiceNumber: string; orderId: string
  etimsInvoiceNumber: string | null; status: string
  taxableAmount: number; taxAmount: number; totalAmount: number
  qrCodeUrl: string | null; submittedAt: string | null; createdAt: string
}

export interface TaxReturnResponse {
  id: string; returnType: 'VAT3' | 'TOT' | 'WHT'
  periodLabel: string; dueDate: string; status: string
  netVatPayable?: number; totAmount?: number; whtAmount?: number
  iTaxAcknowledgementNo: string | null; csvDownloadReady: boolean
}

export interface SocialChannel {
  id: string; platform: string; channelName: string; externalId: string
  phoneNumber: string | null; isActive: boolean; autoReplyEnabled: boolean
  webhookVerifyToken: string; webhookUrl: string; unreadCount: number
}

export interface ConversationSummary {
  id: string; platform: string; channelName: string; customerName: string
  customerPhone: string | null; status: string; unreadCount: number
  lastMessage: string; lastMessageAt: string; isAiHandled: boolean
  assignedOrderId: string | null
}

export interface InboxStats {
  totalUnread: number; openCount: number; pendingPaymentCount: number
}

export interface SocialMessage {
  id: string; direction: string; senderType: string; content: string
  messageType: string; createdAt: string; isAiGenerated: boolean
}

export interface ConversationDetail extends ConversationSummary {
  messages: SocialMessage[]
}

export interface AiReply { suggestedReply: string }

export interface CsTransactionRecord {
  id: string; orderId: string; csTransactionId: string; amount: number; currency: string
  status: string; type: string; cardLast4: string; cardType: string; approvalCode: string
  reconciliationId: string; createdAt: string
}

export interface SavedCardResponse {
  id: string; last4: string; type: string; expiry: string; holder: string; isDefault: boolean
}

export interface StkPushResponse {
  checkoutRequestId: string; merchantRequestId: string
  responseCode: string; responseDescription: string
}

// ── API Service Objects ───────────────────────────────────────────────────────

export const productApi = {
  list: async (q?: string, lowStock?: boolean) => {
    const params = new URLSearchParams()
    if (q) params.set('q', q)
    if (lowStock !== undefined) params.set('lowStock', String(lowStock))
    const res = await client.get<ApiResponse<ProductResponse[]>>(`/products?${params}`)
    return res.data
  },
  get: async (id: string) => {
    const res = await client.get<ApiResponse<ProductResponse>>(`/products/${id}`)
    return res.data
  },
  create: async (data: any) => {
    const res = await client.post<ApiResponse<ProductResponse>>('/products', data)
    return res.data
  },
  update: async (id: string, data: any) => {
    const res = await client.put<ApiResponse<ProductResponse>>(`/products/${id}`, data)
    return res.data
  },
  delete: async (id: string) => {
    const res = await client.delete<ApiResponse<null>>(`/products/${id}`)
    return res.data
  },
  updateStock: async (id: string, data: any) => {
    const res = await client.post<ApiResponse<ProductResponse>>(`/products/${id}/stock`, data)
    return res.data
  },
}

export const orderApi = {
  list: async (status?: string, page?: number, pageSize?: number) => {
    const params = new URLSearchParams()
    if (status) params.set('status', status)
    if (page !== undefined) params.set('page', String(page))
    if (pageSize !== undefined) params.set('pageSize', String(pageSize))
    const res = await client.get<ApiResponse<PagedResponse<OrderResponse>>>(`/orders?${params}`)
    return res.data
  },
  get: async (id: string) => {
    const res = await client.get<ApiResponse<OrderResponse>>(`/orders/${id}`)
    return res.data
  },
  create: async (data: any) => {
    const res = await client.post<ApiResponse<OrderResponse>>('/orders', data)
    return res.data
  },
  updatePaymentStatus: async (id: string, data: { status: string; mpesaTransactionCode?: string }) => {
    const res = await client.patch<ApiResponse<OrderResponse>>(`/orders/${id}/payment-status`, data)
    return res.data
  },
  updateDeliveryStatus: async (id: string, data: { status: string }) => {
    const res = await client.patch<ApiResponse<OrderResponse>>(`/orders/${id}/delivery-status`, data)
    return res.data
  },
}

export const customerApi = {
  list: async (q?: string) => {
    const params = q ? `?q=${encodeURIComponent(q)}` : ''
    const res = await client.get<ApiResponse<CustomerResponse[]>>(`/customers${params}`)
    return res.data
  },
  top: async (limit?: number) => {
    const params = limit !== undefined ? `?limit=${limit}` : ''
    const res = await client.get<ApiResponse<CustomerResponse[]>>(`/customers/top${params}`)
    return res.data
  },
  get: async (id: string) => {
    const res = await client.get<ApiResponse<CustomerResponse>>(`/customers/${id}`)
    return res.data
  },
  create: async (data: any) => {
    const res = await client.post<ApiResponse<CustomerResponse>>('/customers', data)
    return res.data
  },
  update: async (id: string, data: any) => {
    const res = await client.put<ApiResponse<CustomerResponse>>(`/customers/${id}`, data)
    return res.data
  },
}

export const expenseApi = {
  list: async (category?: string, startDate?: string, endDate?: string) => {
    const params = new URLSearchParams()
    if (category) params.set('category', category)
    if (startDate) params.set('startDate', startDate)
    if (endDate) params.set('endDate', endDate)
    const res = await client.get<ApiResponse<ExpenseResponse[]>>(`/expenses?${params}`)
    return res.data
  },
  create: async (data: any) => {
    const res = await client.post<ApiResponse<ExpenseResponse>>('/expenses', data)
    return res.data
  },
  delete: async (id: string) => {
    const res = await client.delete<ApiResponse<null>>(`/expenses/${id}`)
    return res.data
  },
}

export const paymentApi = {
  list: async (unreconciled?: boolean) => {
    const params = unreconciled !== undefined ? `?unreconciled=${unreconciled}` : ''
    const res = await client.get<ApiResponse<PaymentResponse[]>>(`/payments${params}`)
    return res.data
  },
  initiate: async (data: { orderId: string; phoneNumber: string }) => {
    const res = await client.post<ApiResponse<StkPushResponse>>('/payments/initiate', data)
    return res.data
  },
  reconcile: async (id: string, data: { orderId: string }) => {
    const res = await client.post<ApiResponse<null>>(`/payments/${id}/reconcile`, data)
    return res.data
  },
}

export const reportApi = {
  profitSummary: async (startDate: string, endDate: string) => {
    const res = await client.get<ApiResponse<ProfitSummaryResponse>>(
      `/reports/profit-summary?startDate=${startDate}&endDate=${endDate}`
    )
    return res.data
  },
}

export const taxApi = {
  getRates: async () => {
    const res = await client.get<ApiResponse<TaxRateResponse[]>>('/tax/rates')
    return res.data
  },
  createRate: async (data: any) => {
    const res = await client.post<ApiResponse<TaxRateResponse>>('/tax/rates', data)
    return res.data
  },
  updateRate: async (id: string, data: any) => {
    const res = await client.put<ApiResponse<TaxRateResponse>>(`/tax/rates/${id}`, data)
    return res.data
  },
  toggleRate: async (id: string) => {
    const res = await client.patch<ApiResponse<TaxRateResponse>>(`/tax/rates/${id}/toggle`)
    return res.data
  },
  deleteRate: async (id: string) => {
    const res = await client.delete<ApiResponse<null>>(`/tax/rates/${id}`)
    return res.data
  },
  seedDefaults: async () => {
    const res = await client.post<ApiResponse<null>>('/tax/rates/seed-defaults')
    return res.data
  },
  getRemittances: async (taxType?: string) => {
    const params = taxType ? `?taxType=${taxType}` : ''
    const res = await client.get<ApiResponse<TaxRemittanceResponse[]>>(`/tax/remittances${params}`)
    return res.data
  },
  createRemittance: async (data: any) => {
    const res = await client.post<ApiResponse<TaxRemittanceResponse>>('/tax/remittances', data)
    return res.data
  },
  updateRemittanceStatus: async (id: string, data: any) => {
    const res = await client.patch<ApiResponse<TaxRemittanceResponse>>(`/tax/remittances/${id}/status`, data)
    return res.data
  },
  getSummary: async (from: string, to: string) => {
    const res = await client.get<ApiResponse<TaxSummaryResponse>>(`/tax/summary?from=${from}&to=${to}`)
    return res.data
  },
}

export const kraApi = {
  getProfile: async () => {
    const res = await client.get<ApiResponse<KraProfileResponse>>('/kra/profile')
    return res.data
  },
  saveProfile: async (data: any) => {
    const res = await client.post<ApiResponse<KraProfileResponse>>('/kra/profile', data)
    return res.data
  },
  getCompliance: async () => {
    const res = await client.get<ApiResponse<KraComplianceStatus>>('/kra/compliance')
    return res.data
  },
  getEtimsHistory: async () => {
    const res = await client.get<ApiResponse<EtimsInvoiceResponse[]>>('/kra/etims/history')
    return res.data
  },
  getEtimsPending: async () => {
    const res = await client.get<ApiResponse<EtimsInvoiceResponse[]>>('/kra/etims/pending')
    return res.data
  },
  transmitEtims: async (data: { orderId: string }) => {
    const res = await client.post<ApiResponse<EtimsInvoiceResponse>>('/kra/etims/transmit', data)
    return res.data
  },
  retryEtims: async () => {
    const res = await client.post<ApiResponse<null>>('/kra/etims/retry')
    return res.data
  },
  generateVat3: async (data: { periodStart: string; periodEnd: string }) => {
    const res = await client.post<ApiResponse<TaxReturnResponse>>('/kra/returns/vat3', data)
    return res.data
  },
  generateTot: async (data: { periodStart: string; periodEnd: string }) => {
    const res = await client.post<ApiResponse<TaxReturnResponse>>('/kra/returns/tot', data)
    return res.data
  },
  generateWht: async (data: { periodStart: string; periodEnd: string }) => {
    const res = await client.post<ApiResponse<TaxReturnResponse>>('/kra/returns/wht', data)
    return res.data
  },
  markReturnSubmitted: async (id: string) => {
    const res = await client.patch<ApiResponse<TaxReturnResponse>>(`/kra/returns/${id}/submitted`)
    return res.data
  },
  getReturns: async () => {
    const res = await client.get<ApiResponse<TaxReturnResponse[]>>('/kra/returns')
    return res.data
  },
}

export const socialApi = {
  getChannels: async () => {
    const res = await client.get<ApiResponse<SocialChannel[]>>('/social/channels')
    return res.data
  },
  createChannel: async (data: any) => {
    const res = await client.post<ApiResponse<SocialChannel>>('/social/channels', data)
    return res.data
  },
  deleteChannel: async (id: string) => {
    const res = await client.delete<ApiResponse<null>>(`/social/channels/${id}`)
    return res.data
  },
  updateChannelSettings: async (id: string, data: any) => {
    const res = await client.patch<ApiResponse<SocialChannel>>(`/social/channels/${id}/settings`, data)
    return res.data
  },
  getInbox: async () => {
    const res = await client.get<ApiResponse<ConversationSummary[]>>('/social/inbox')
    return res.data
  },
  getInboxStats: async () => {
    const res = await client.get<ApiResponse<InboxStats>>('/social/inbox/stats')
    return res.data
  },
  getConversation: async (id: string) => {
    const res = await client.get<ApiResponse<ConversationDetail>>(`/social/conversations/${id}`)
    return res.data
  },
  updateConversationStatus: async (id: string, data: { status: string }) => {
    const res = await client.patch<ApiResponse<ConversationSummary>>(`/social/conversations/${id}/status`, data)
    return res.data
  },
  sendMessage: async (data: any) => {
    const res = await client.post<ApiResponse<SocialMessage>>('/social/messages/send', data)
    return res.data
  },
  getAiReply: async (data: any) => {
    const res = await client.post<ApiResponse<AiReply>>('/social/messages/ai-reply', data)
    return res.data
  },
}

export const cyberSourceApi = {
  getTransactions: async () => {
    const res = await client.get<ApiResponse<CsTransactionRecord[]>>('/payments/card/transactions')
    return res.data
  },
  getSavedCards: async () => {
    const res = await client.get<ApiResponse<SavedCardResponse[]>>('/payments/card/saved-cards')
    return res.data
  },
  deleteSavedCard: async (id: string) => {
    const res = await client.delete<ApiResponse<null>>(`/payments/card/saved-cards/${id}`)
    return res.data
  },
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


import axios, { AxiosInstance } from 'axios'

// The custom API hostname is not provisioned in every environment. Keep the
// deployed App Runner endpoint as the working fallback; production builds can
// still override it with VITE_API_BASE_URL when DNS is configured.
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'https://sddgmezqj2.us-east-1.awsapprunner.com/v1'
const LAST_ACTIVITY_KEY = 'sessionLastActivity'

export const client: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
    'X-Client-Platform': 'web'
  }
})

// Add token to requests if it exists
client.interceptors.request.use((config) => {
  if (localStorage.getItem('accessToken')) localStorage.setItem(LAST_ACTIVITY_KEY, String(Date.now()))
  const token = localStorage.getItem('accessToken')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// Handle errors globally
client.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config
    if (error.response?.status === 401 && originalRequest && !originalRequest._retry) {
      const storedRefreshToken = localStorage.getItem('refreshToken')
      if (storedRefreshToken) {
        originalRequest._retry = true
        try {
          const res = await client.post<ApiResponse<AuthResponse>>('/auth/refresh', { refreshToken: storedRefreshToken })
          if (res.data.success && res.data.data) {
            localStorage.setItem('accessToken', res.data.data.accessToken)
            localStorage.setItem('refreshToken', res.data.data.refreshToken)
            originalRequest.headers.Authorization = `Bearer ${res.data.data.accessToken}`
            return client(originalRequest)
          }
        } catch {
          // refresh failed — fall through to logout
        }
      }
      localStorage.removeItem('accessToken')
      localStorage.removeItem('refreshToken')
      localStorage.removeItem('isAuthenticated')
      localStorage.removeItem(LAST_ACTIVITY_KEY)
      localStorage.removeItem('user')
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
  accessToken?: string
  refreshToken?: string
  user?: AuthResponse['user']
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
    businessId: string | null
    businessName?: string | null
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

export interface SessionTimeoutConfig {
  businessId: string
  webTimeoutSeconds: number
  androidTimeoutSeconds: number
  desktopTimeoutSeconds: number
  updatedAt?: string | null
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
  tenantId?: string | null; wabaId?: string | null; phoneNumberId?: string | null
  metaBusinessId?: string | null
  connectionStatus?: 'CONNECTED' | 'ACTION_REQUIRED' | 'DISCONNECTED'
  onboardingMethod?: 'MANUAL' | 'META_EMBEDDED_SIGNUP'
  lastVerifiedAt?: string | null
  webhookVerifyToken: string; webhookUrl: string; unreadCount: number
}

export interface MetaOnboardingConfiguration {
  configured: boolean
  appId: string | null
  configurationId: string | null
  graphApiVersion: string
  missing: string[]
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

export interface CsGuestChargeRequest {
  businessId: string
  orderId: string
  amount: number
  currency: string
  transientToken?: string
  cardNumber?: string
  cardExpiryMonth?: string
  cardExpiryYear?: string
  cardCvv?: string
  cardholderName?: string
  billingEmail?: string
  billingPhone?: string
}

export interface CsChargeResponse {
  transactionId: string
  csTransactionId: string | null
  status: string
  approvalCode: string | null
  amount: number
  currency: string
  cardLast4: string | null
  cardType: string | null
  reconciliationId: string | null
  savedCardId: string | null
  errorMessage: string | null
  errorReason: string | null
}

export interface StkPushResponse {
  checkoutRequestId: string; merchantRequestId: string
  responseCode: string; responseDescription: string
}

export interface UserResponse {
  id: string
  name: string
  email: string
  phone: string
  role: string
  businessId: string
  preferredLanguage: string
  isActive?: boolean
}

export interface InviteUserRequest {
  name: string
  email: string
  phone: string
  password: string
  role?: string   // 'ADMIN' | 'STAFF', defaults to 'STAFF'
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
    const payload = {
      ...data,
      clientReference: data.clientReference || crypto.randomUUID()
    }
    const res = await client.post<ApiResponse<OrderResponse>>('/orders', payload)
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
  cancel: async (id: string) => {
    const res = await client.post<ApiResponse<OrderResponse>>(`/orders/${id}/cancel`)
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
  initiate: async (data: { orderId: string; phoneNumber: string; accountType?: string }) => {
    const res = await client.post<ApiResponse<StkPushResponse>>('/payments/initiate', data)
    return res.data
  },
  transactionQuery: async (transactionId: string) => {
    const res = await client.post<ApiResponse<string>>('/payments/mpesa/transaction-query', { transactionId })
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
  getMetaConfiguration: async () => {
    const res = await client.get<ApiResponse<MetaOnboardingConfiguration>>('/social/meta/configuration')
    return res.data
  },
  completeMetaEmbeddedSignup: async (data: {
    code: string
    wabaId: string
    phoneNumberId: string
    metaBusinessId: string
    channelName?: string
  }) => {
    const res = await client.post<ApiResponse<SocialChannel>>('/social/meta/embedded-signup/complete', data)
    return res.data
  },
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
  verifyChannel: async (id: string) => {
    const res = await client.post<ApiResponse<{
      connected: boolean
      connectionStatus: string
      phoneNumber?: string | null
      displayName?: string | null
    }>>(`/social/channels/${id}/verify`)
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

export const userApi = {
  list: async (businessId?: string) => {
    const res = await client.get<ApiResponse<UserResponse[]>>('/users', {
      params: businessId ? { businessId } : undefined,
    })
    return res.data
  },
  invite: async (data: InviteUserRequest, businessId?: string) => {
    const res = await client.post<ApiResponse<UserResponse>>('/users', data, {
      params: businessId ? { businessId } : undefined,
    })
    return res.data
  },
  updateRole: async (id: string, role: string, businessId?: string) => {
    const res = await client.patch<ApiResponse<UserResponse>>(`/users/${id}/role`, { role }, {
      params: businessId ? { businessId } : undefined,
    })
    return res.data
  },
  setStatus: async (id: string, isActive: boolean, businessId?: string) => {
    const res = await client.patch<ApiResponse<UserResponse>>(`/users/${id}/status`, { isActive }, {
      params: businessId ? { businessId } : undefined,
    })
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
  getGuestCaptureContext: async (origin: string, businessId?: string) => {
    const res = await client.get<ApiResponse<{ captureContextJwt: string }>>('/payments/card/capture-context', {
      params: { origin, businessId }
    })
    return res.data
  },
  guestCharge: async (req: CsGuestChargeRequest) => {
    const res = await client.post<ApiResponse<CsChargeResponse>>('/payments/card/guest-charge', req)
    return res.data
  },
  generatePaymentLink: async (req: { orderId: string; amount: number; description?: string; customerName?: string; customerEmail?: string; customerPhone?: string; expiryHours?: number }) => {
    const res = await client.post<ApiResponse<{ linkUrl: string; orderId: string; amount: number; clientReference: string; expiresAt: string }>>('/payments/card/manage/generate-link', req)
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

export interface BusinessResponse {
  id: string
  name: string
  type: string
  ownerPhone: string
  ownerEmail: string
  subscriptionTier: string
  subscriptionEnabled: boolean
  isActive: boolean
  createdAt: string
}

export interface UpdateBusinessStatusRequest {
  isActive: boolean
}

export interface UpdateSubscriptionRequest {
  enabled: boolean
  tier?: 'FREEMIUM' | 'PREMIUM'
}

export interface CreateBusinessWithAdminRequest {
  businessName: string
  businessType: string
  adminName: string
  adminEmail: string
  adminPhone: string
  adminPassword: string
}

export interface BusinessWithAdminResponse {
  business: BusinessResponse
  admin: UserResponse
}

export interface LinkUserToBusinessRequest {
  businessId: string
  role?: string
}

export interface BusinessProfileRequest {
  name: string
  owner: string
  phone: string
  email: string
  type: string
  county: string
  address: string
  kraPin: string
  paybillNumber: string
  accountNumber: string
  receiptHeader?: string
  receiptFooter?: string
  receiptShowTax?: boolean
  receiptShowCustomer?: boolean
}

export interface BusinessProfileResponse {
  id: string
  name: string
  owner: string
  phone: string
  email: string
  type: string
  county: string
  address: string
  kraPin: string
  paybillNumber: string
  accountNumber: string
  subscriptionTier: string
  subscriptionEnabled: boolean
  receiptHeader?: string
  receiptFooter?: string
  receiptShowTax?: boolean
  receiptShowCustomer?: boolean
}

export interface MpesaConfigResponse {
  businessId: string
  shortCode: string
  callbackUrl?: string
  environment: string
  accountType: string
  passkeyConfigured: boolean
  updatedAt: string
}

export interface MpesaConfigRequest {
  shortCode: string
  callbackUrl?: string
  passKey?: string
  environment: string
  accountType: string
}

export interface CyberSourceConfigResponse {
  businessId: string
  merchantId: string
  merchantKeyId: string
  profileId?: string
  accessKey?: string
  environment: string
  secretConfigured: boolean
  updatedAt: string
}

export interface CyberSourceConfigRequest {
  merchantId: string
  merchantKeyId: string
  merchantSecretKey?: string
  profileId?: string
  accessKey?: string
  environment: string
}

export const businessApi = {
  getProfile: async () => {
    const res = await client.get<ApiResponse<BusinessProfileResponse>>('/business/profile')
    return res.data
  },
  updateProfile: async (data: BusinessProfileRequest) => {
    const res = await client.put<ApiResponse<BusinessProfileResponse>>('/business/profile', data)
    return res.data
  },
}

export const settingsApi = {
  getSessionTimeouts: async () => {
    const res = await client.get<ApiResponse<SessionTimeoutConfig>>('/settings/session-timeouts')
    return res.data
  },
  updateSessionTimeouts: async (data: Pick<SessionTimeoutConfig, 'webTimeoutSeconds' | 'androidTimeoutSeconds' | 'desktopTimeoutSeconds'>) => {
    const res = await client.put<ApiResponse<SessionTimeoutConfig>>('/settings/session-timeouts', data)
    return res.data
  },
  getMpesa: async () => {
    const res = await client.get<ApiResponse<MpesaConfigResponse>>('/settings/mpesa')
    return res.data
  },
  getMpesaChannels: async () => {
    const res = await client.get<ApiResponse<MpesaConfigResponse[]>>('/settings/mpesa/channels')
    return res.data
  },
  updateMpesa: async (data: MpesaConfigRequest) => {
    const res = await client.put<ApiResponse<MpesaConfigResponse>>('/settings/mpesa', data)
    return res.data
  },
  getCyberSource: async () => {
    const res = await client.get<ApiResponse<CyberSourceConfigResponse>>('/settings/cybersource')
    return res.data
  },
  updateCyberSource: async (data: CyberSourceConfigRequest) => {
    const res = await client.put<ApiResponse<CyberSourceConfigResponse>>('/settings/cybersource', data)
    return res.data
  },
}

export const superAdminApi = {
  listBusinesses: async () => {
    const res = await client.get<ApiResponse<BusinessResponse[]>>('/admin/businesses')
    return res.data
  },
  createBusiness: async (data: { businessName: string; businessType: string }) => {
    const res = await client.post<ApiResponse<BusinessResponse>>('/admin/businesses/simple', data)
    return res.data
  },
  createBusinessWithAdmin: async (data: CreateBusinessWithAdminRequest) => {
    const res = await client.post<ApiResponse<BusinessWithAdminResponse>>('/admin/businesses', data)
    return res.data
  },
  linkUserToBusiness: async (userId: string, data: LinkUserToBusinessRequest) => {
    const res = await client.put<ApiResponse<UserResponse>>(`/admin/users/${userId}/business`, data)
    return res.data
  },
  setBusinessStatus: async (businessId: string, data: UpdateBusinessStatusRequest) => {
    const res = await client.patch<ApiResponse<BusinessResponse>>(`/admin/businesses/${businessId}/status`, data)
    return res.data
  },
  updateSubscription: async (businessId: string, data: UpdateSubscriptionRequest) => {
    const res = await client.patch<ApiResponse<BusinessResponse>>(`/admin/businesses/${businessId}/subscription`, data)
    return res.data
  },
  getMpesaCallbackUrl: async () => {
    const res = await client.get<ApiResponse<{ key: string; value: string }>>('/admin/settings/mpesa-callback')
    return res.data
  },
  saveMpesaCallbackUrl: async (value: string) => {
    const res = await client.put<ApiResponse<{ key: string; value: string }>>('/admin/settings/mpesa-callback', { value })
    return res.data
  },
}

export default client

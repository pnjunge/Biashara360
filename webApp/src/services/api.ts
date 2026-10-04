import axios, { AxiosInstance } from 'axios'

// The custom API hostname is not provisioned in every environment. Keep the
// deployed App Runner endpoint as the working fallback; production builds can
// Deployments can override this with VITE_API_BASE_URL.
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'https://api.biashara360.co.ke/v1'
const LAST_ACTIVITY_KEY = 'sessionLastActivity'

export const client: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
    'X-Client-Platform': 'web'
  }
})

// Add token and active branch to requests if they exist
client.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  const branchId = localStorage.getItem('selectedBranchId')
  if (branchId) {
    config.headers['X-Branch-ID'] = branchId
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
export interface PinLoginRequest { pin: string; email?: string }

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
  userCount: number
}

export interface SubscriptionBand { id: string; name: string; minUsers: number; maxUsers: number; monthlyPrice: number }

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
  category: string; barcode?: string | null; imageUrl: string | null; isActive?: boolean; createdAt: string; updatedAt: string
}

export interface PurchaseLineItem {
  productId: string
  productName: string
  sku: string
  quantity: number
  unitCost: number
  totalCost: number
}

export interface PurchaseInvoice {
  id: string
  businessId: string
  invoiceNumber: string
  supplierName: string
  supplierPhone?: string | null
  totalAmount: number
  paymentStatus: string
  paymentMethod: string
  notes?: string | null
  items: PurchaseLineItem[]
  invoiceDate: string
  createdAt: string
  updatedAt: string
}

export interface CreatePurchaseInvoiceRequest {
  invoiceNumber: string
  supplierName: string
  supplierPhone?: string | null
  totalAmount?: number | null
  paymentStatus?: string | null
  paymentMethod?: string | null
  notes?: string | null
  invoiceDate?: string | null
  items: Array<{
    productId: string
    productName: string
    sku?: string | null
    quantity: number
    unitCost: number
    lineTotal?: number | null
    totalCost?: number | null
  }>
}

export interface OrderItemResponse {
  id: string; productId: string; productName: string; quantity: number
  unitPrice: number; buyingPrice: number; lineTotal: number; lineProfit: number
  modifiers?: MenuOption[]; itemNote?: string; discountAmount?: number; complimentary?: boolean
}

export interface OrderResponse {
  id: string; orderNumber: string; businessId: string; customerId: string | null
  customerName: string; customerPhone: string; deliveryLocation: string
  items: OrderItemResponse[]; paymentStatus: string; deliveryStatus: string
  paymentMethod: string; mpesaTransactionCode: string | null; subtotal: number
  baseAmount?: number; taxIncluded?: boolean; taxRate?: number; taxAmount?: number
  salesChannel: string
  serviceType?: string; hospitalityTableId?: string | null; serverUserId?: string | null
  guestCount?: number; tabStatus?: string
  branchId?: string | null; branchName?: string | null
  notes: string; createdAt: string; updatedAt: string
}

export interface PublicBusinessProfile {
  id: string
  name: string
  phone: string
  email: string
  address: string
  county: string
  kraPin: string
  receiptHeader: string
  receiptFooter: string
  receiptLogo?: string | null
  receiptLogoWidthMm: number
  receiptLogoHeightMm: number
  receiptShowTax: boolean
  receiptShowCustomer: boolean
}

export interface PublicBranchInfo {
  id: string
  name: string
  code?: string | null
  phone?: string | null
  address?: string | null
  receiptHeader?: string | null
  receiptFooter?: string | null
}

export interface PublicKraFiscal {
  invoiceNumber?: string | null
  qrCodeContent?: string | null
  qrCodeBase64?: string | null
  sdcId?: string | null
  rcptSign?: string | null
}

export interface PublicReceiptResponse {
  order: OrderResponse
  business: PublicBusinessProfile
  branch?: PublicBranchInfo | null
  receiptUrl: string
  kraFiscal?: PublicKraFiscal | null
}

export interface SendEReceiptRequest {
  channel: 'SMS' | 'EMAIL' | 'WHATSAPP' | string
  recipient?: string
}

export interface SendEReceiptResponse {
  channel: string
  recipient: string
  status: string
  whatsappUrl?: string | null
  messageText?: string | null
}

export interface HospitalityTable { id:string; name:string; area:string; capacity:number; status:string; openOrderId:string|null; openAmount:number; openOrderCount:number; waiterUserId?:string|null; mergedIntoTableId?:string|null; positionX?:number; positionY?:number; shape?:string }
export interface KitchenTicket { id:string; orderId:string; orderNumber:string; tableName:string|null; station:string; status:string; notes:string; items:OrderItemResponse[]; createdAt:string }
export interface MenuOption { name:string; priceDelta:number }
export interface HospitalityOperations {
  reservations:Array<{id:string;tableId:string|null;customerName:string;customerPhone:string;guestCount:number;reservedAt:string;durationMinutes:number;status:string;notes:string}>
  menuProfiles:Array<{productId:string;preparationStation:string|null;mealPeriods:string[];sizes:MenuOption[];extras:MenuOption[];variants:MenuOption[];comboProductIds:string[];soldOut:boolean;happyHourPrice:number|null;happyHourStart:string|null;happyHourEnd:string|null;ageRestricted:boolean;minimumAge:number|null}>
  ingredients:Array<{id:string;name:string;unit:string;quantity:number;reorderLevel:number;unitCost:number;isLowStock:boolean}>
  shifts:Array<{id:string;openedBy:string;openedAt:string;closedAt:string|null;openingFloat:number;expectedCash:number|null;actualCash:number|null;mpesaTotal:number|null;cardTotal:number|null;tipsTotal:number;expensesTotal:number;status:string;variance:number|null;actualMpesa:number|null;actualCard:number|null;mpesaVariance:number|null;cardVariance:number|null;totalVariance:number|null}>
  suppliers:Array<{id:string;name:string;phone:string;email:string|null;address:string|null;isActive:boolean}>
  purchaseOrders:Array<{id:string;orderNumber:string;supplierId:string;status:string;totalCost:number;orderedAt:string;receivedAt:string|null}>
  approvals:Array<{id:string;actionType:string;entityType:string;entityId:string;requestedBy:string;approvedBy:string|null;status:string;reason:string;requestedAt:string;amount?:number|null;quantity?:number|null;eventType?:string|null}>
}
export interface HospitalityDashboard { enabled:boolean; tables:HospitalityTable[]; openTabs:OrderResponse[]; tickets:KitchenTicket[]; shiftOpen?: boolean }

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
  expenseDate: string; receiptUrl: string | null
  branchId?: string | null; branchName?: string | null
  recordedAt: string
}

export interface PaymentResponse {
  id: string; businessId: string; orderId: string | null; transactionCode: string
  amount: number; payerPhone: string; payerName: string; method: string
  status: string; channel: string; reconciled: boolean; notes?: string
  transactionDate: string
}

export interface ProfitSummaryResponse {
  period: string; totalRevenue: number; totalCostOfGoods: number
  grossProfit: number; grossMargin: number; totalExpenses: number
  netProfit: number; netMargin: number; cashflowIn: number; cashflowOut: number
  dailyRevenue?: Array<{ date: string; revenue: number }>
}

export interface ReportBreakdown { label: string; count: number; amount: number }
export interface PaymentReportResponse {
  period: string; totalTransactions: number; totalAmount: number; reconciledAmount: number
  byMethod: ReportBreakdown[]; byChannel: ReportBreakdown[]
  payments: Array<{ transactionCode: string; orderId: string | null; amount: number; payerName: string; payerPhone: string; method: string; channel: string; status: string; reconciled: boolean; transactionDate: string }>
}
export interface OrderReportResponse {
  period: string; totalOrders: number; totalValue: number; paidValue: number
  byPaymentMethod: ReportBreakdown[]; byChannel: ReportBreakdown[]
  orders: Array<{ orderId: string; orderNumber: string; customerName: string; subtotal: number; paymentStatus: string; deliveryStatus: string; serviceType: string; tabStatus: string; paymentMethod: string; salesChannel: string; createdAt: string }>
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
  periodYear: number; periodMonth: number
  periodLabel: string; dueDate: string; status: string
  netVatPayable?: number; totAmount?: number; whtAmount?: number
  iTaxAcknowledgementNo: string | null; csvDownloadReady: boolean
}

export interface CsvExportResponse {
  fileName: string; format: string; rowCount: number; periodLabel: string
  downloadBase64: string; contentType: string
  uploadInstructions: {
    portalUrl: string; menuPath: string[]; fileFormatRequired: string; steps: string[]
  }
}

export interface SocialChannel {
  id: string; platform: string; channelName: string; externalId: string
  phoneNumber: string | null; isActive: boolean; autoReplyEnabled: boolean
  tenantId?: string | null; wabaId?: string | null; phoneNumberId?: string | null
  metaBusinessId?: string | null
  connectionStatus?: 'CONNECTED' | 'ACTION_REQUIRED' | 'DISCONNECTED'
  onboardingMethod?: 'MANUAL' | 'META_EMBEDDED_SIGNUP' | 'META_BUSINESS_LOGIN'
  lastVerifiedAt?: string | null
  webhookVerifyToken: string; webhookUrl: string; unreadCount: number
}

export interface MetaOnboardingConfiguration {
  configured: boolean
  appId: string | null
  configurationId: string | null
  businessLoginConfigured: boolean
  businessLoginConfigurationId: string | null
  graphApiVersion: string
  missing: string[]
  businessLoginMissing: string[]
}

export interface MetaBusinessAsset {
  platform: 'FACEBOOK' | 'INSTAGRAM'
  accountId: string
  name: string
  pageId: string
  pageName: string
  username?: string | null
  pictureUrl?: string | null
}

export interface MetaBusinessDiscovery {
  sessionToken: string
  assets: MetaBusinessAsset[]
}

export interface StorefrontProduct {
  id: string
  sku: string
  name: string
  description: string
  sellingPrice: number
  availableQuantity: number
  category: string
  imageUrl: string | null
}

export interface Storefront {
  businessId: string
  storefrontSlug: string
  businessName: string
  businessType: string
  county: string | null
  address: string | null
  currency: string
  welcomeMessage: string
  themeColor: string
  headline: string
  description: string
  bannerUrl: string | null
  layout: 'GRID' | 'LIST'
  tables?: Array<{ id: string; name: string; area: string }>
  services: ServiceCatalogItem[]
  products: StorefrontProduct[]
}

export interface StorefrontCheckoutResult {
  orderId: string
  orderNumber: string
  clientReference: string
  amount: number
  paymentStatus: string
  paymentMethod: 'MPESA' | 'COD' | 'CARD'
  customerMessage: string | null
  checkoutRequestId: string | null
}

export interface StorefrontOrderStatus {
  orderId: string
  orderNumber: string
  amount: number
  paymentStatus: string
  deliveryStatus: string
}

export const storefrontApi = {
  get: async (businessId: string) => {
    const res = await client.get<ApiResponse<Storefront>>(`/public/store/${encodeURIComponent(businessId)}`)
    return res.data
  },
  checkout: async (businessId: string, data: {
    clientReference: string
    customerName: string
    customerPhone: string
    deliveryLocation: string
    paymentMethod: 'MPESA' | 'COD' | 'CARD'
    notes?: string
    items: Array<{ productId: string; quantity: number }>
  }) => {
    const res = await client.post<ApiResponse<StorefrontCheckoutResult>>(
      `/public/store/${encodeURIComponent(businessId)}/checkout`, data
    )
    return res.data
  },
  retryMpesa: async (businessId: string, orderId: string, data: { clientReference: string; customerPhone: string }) => {
    const res = await client.post<ApiResponse<StorefrontCheckoutResult>>(
      `/public/store/${encodeURIComponent(businessId)}/orders/${encodeURIComponent(orderId)}/mpesa`, data
    )
    return res.data
  },
  orderStatus: async (businessId: string, orderId: string, reference: string) => {
    const params = new URLSearchParams({ reference })
    const res = await client.get<ApiResponse<StorefrontOrderStatus>>(
      `/public/store/${encodeURIComponent(businessId)}/orders/${encodeURIComponent(orderId)}?${params}`
    )
    return res.data
  },
  bookAppointment: async (businessId: string, data: { serviceId: string; resourceId?: string | null; customerName: string; customerPhone?: string; startsAt: string; durationMinutes?: number; notes?: string }) => {
    const res = await client.post<ApiResponse<ServiceAppointment>>(`/public/store/${encodeURIComponent(businessId)}/appointments`, data)
    return res.data
  },
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
  responseCode: string; responseDescription: string; customerMessage?: string
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
  status?: string // 'ACTIVE' | 'DISABLED' | 'LOCKED'
  lastLoginAt?: string | null
  hasPinSet?: boolean
  isPinLocked?: boolean
  pinLockedUntil?: string | null
  assignedGroups?: string[]
  assignedGroupIds?: string[]
  assignedRoles?: string[]
  assignedRoleIds?: string[]
  permissions?: string[]
  branchId?: string | null
  branchName?: string | null
  assignedBranchIds?: string[]
  assignedBranchNames?: string[]
  createdAt?: string
  updatedAt?: string
}

export interface EditUserRequest {
  name: string
  email: string
  phone: string
  role?: string
  branchId?: string | null
  branchIds?: string[]
  roleIds?: string[]
}

export interface InviteUserRequest {
  name: string
  email: string
  phone: string
  role?: string   // 'STAFF' | 'MANAGER' | 'ADMIN'
  password?: string
  groupId?: string
  groupIds?: string[]
  roleIds?: string[]
  branchId?: string | null
  branchIds?: string[]
}

export interface ReassignUserRequest {
  role?: string
  groupIds?: string[]
  roleIds?: string[]
  businessId?: string
  branchId?: string | null
}

export interface PermissionDefinition {
  id: string
  code: string
  module: string
  action: string
  name: string
  description: string
}

export interface MenuDefinition { key: string; label: string }
export interface AccessRole {
  id: string
  name: string
  description: string
  allowedMenus: string[]
  permissions?: string[]
  isActive: boolean
}
export interface AccessGroup { id: string; name: string; description: string; allowedMenus: string[]; roleIds?: string[]; userIds: string[]; isActive: boolean }
export interface AccessConfig {
  menus: MenuDefinition[]
  enabledMenus: string[]
  roles: AccessRole[]
  groups: AccessGroup[]
  permissions?: PermissionDefinition[]
}
export interface InventoryCategory { id: string; name: string; isActive: boolean; productCount: number; imageUrl?: string | null }

export interface Supplier {
  id: string
  name: string
  phone: string
  email?: string | null
  address?: string | null
  isActive: boolean
}

export interface ServiceCatalogItem {
  id: string; name: string; description: string; category: string
  durationMinutes: number; price: number; isActive: boolean; createdAt: string; updatedAt: string
}
export interface ServiceResource { id: string; name: string; type: string; isActive: boolean }
export interface ServiceAppointment {
  id: string; serviceId: string; serviceName: string; resourceId?: string | null; resourceName?: string | null
  customerId?: string | null; customerName: string; customerPhone: string; staffUserId?: string | null
  startsAt: string; durationMinutes: number; status: string; notes: string; orderId?: string | null
  createdAt: string; updatedAt: string
}
export interface ServiceSchedule { services: ServiceCatalogItem[]; resources: ServiceResource[]; appointments: ServiceAppointment[] }

// ── API Service Objects ───────────────────────────────────────────────────────

export const productApi = {
  list: async (q?: string, lowStock?: boolean, includeInactive?: boolean) => {
    const params = new URLSearchParams()
    if (q) params.set('q', q)
    if (lowStock !== undefined) params.set('lowStock', String(lowStock))
    if (includeInactive !== undefined) params.set('includeInactive', String(includeInactive))
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
  updateStatus: async (id: string, isActive: boolean) => {
    const res = await client.put<ApiResponse<ProductResponse>>(`/products/${id}/status`, { isActive })
    return res.data
  },
  listCategories: async () => {
    const res = await client.get<ApiResponse<InventoryCategory[]>>('/products/categories')
    return res.data
  },
  createCategory: async (name: string, imageUrl?: string) => {
    const res = await client.post<ApiResponse<InventoryCategory>>('/products/categories', { name, imageUrl: imageUrl || null })
    return res.data
  },
  updateCategory: async (id: string, data: { name?: string; isActive?: boolean; imageUrl?: string }) => {
    const res = await client.put<ApiResponse<InventoryCategory>>(`/products/categories/${id}`, data)
    return res.data
  },
}

export const purchaseApi = {
  list: async () => {
    const res = await client.get<ApiResponse<PurchaseInvoice[]>>('/purchases')
    return res.data
  },
  get: async (id: string) => {
    const res = await client.get<ApiResponse<PurchaseInvoice>>(`/purchases/${id}`)
    return res.data
  },
  create: async (data: CreatePurchaseInvoiceRequest) => {
    const res = await client.post<ApiResponse<PurchaseInvoice>>('/purchases', data)
    return res.data
  }
}

export const orderApi = {
  list: async (status?: string, page?: number, pageSize?: number, branchId?: string) => {
    const params = new URLSearchParams()
    if (status) params.set('status', status)
    if (page !== undefined) params.set('page', String(page))
    if (pageSize !== undefined) params.set('pageSize', String(pageSize))
    if (branchId) params.set('branchId', branchId)
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
  void: async (id:string) => (await client.post<ApiResponse<OrderResponse>>(`/orders/${id}/void`)).data,
  sendEReceipt: async (id: string, data: SendEReceiptRequest) => {
    const res = await client.post<ApiResponse<SendEReceiptResponse>>(`/orders/${id}/send-ereceipt`, data)
    return res.data
  },
}

export const receiptApi = {
  getPublicReceipt: async (orderId: string) => {
    const res = await client.get<ApiResponse<PublicReceiptResponse>>(`/public/receipts/${encodeURIComponent(orderId)}`)
    return res.data
  },
  sendEReceipt: async (orderId: string, data: SendEReceiptRequest) => {
    const res = await client.post<ApiResponse<SendEReceiptResponse>>(`/orders/${encodeURIComponent(orderId)}/send-ereceipt`, data)
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
  list: async (category?: string, startDate?: string, endDate?: string, branchId?: string) => {
    const params = new URLSearchParams()
    if (category) params.set('category', category)
    if (startDate) params.set('startDate', startDate)
    if (endDate) params.set('endDate', endDate)
    if (branchId) params.set('branchId', branchId)
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
  payments: async (startDate: string, endDate: string) => {
    const params = new URLSearchParams({ startDate, endDate })
    const res = await client.get<ApiResponse<PaymentReportResponse>>(`/reports/payments?${params}`)
    return res.data
  },
  orders: async (startDate: string, endDate: string) => {
    const params = new URLSearchParams({ startDate, endDate })
    const res = await client.get<ApiResponse<OrderReportResponse>>(`/reports/orders?${params}`)
    return res.data
  },
}

export interface CreateReportScheduleRequest {
  branchId?: string | null
  name: string
  reportType: 'SALES_SUMMARY' | 'PAYMENTS' | 'PROFIT_LOSS' | 'LOW_STOCK'
  frequency: 'DAILY' | 'WEEKLY' | 'MONTHLY'
  timeOfDay?: string // HH:mm
  dayOfWeek?: number | null // 1..7
  dayOfMonth?: number | null // 1..28
  channels: 'EMAIL' | 'WHATSAPP' | 'EMAIL,WHATSAPP'
  emailRecipients?: string | null
  whatsappRecipients?: string | null
  isActive?: boolean
}

export interface UpdateReportScheduleRequest {
  branchId?: string | null
  name?: string
  reportType?: 'SALES_SUMMARY' | 'PAYMENTS' | 'PROFIT_LOSS' | 'LOW_STOCK'
  frequency?: 'DAILY' | 'WEEKLY' | 'MONTHLY'
  timeOfDay?: string
  dayOfWeek?: number | null
  dayOfMonth?: number | null
  channels?: 'EMAIL' | 'WHATSAPP' | 'EMAIL,WHATSAPP'
  emailRecipients?: string | null
  whatsappRecipients?: string | null
  isActive?: boolean
}

export interface ReportScheduleResponse {
  id: string
  businessId: string
  branchId?: string | null
  branchName?: string | null
  name: string
  reportType: 'SALES_SUMMARY' | 'PAYMENTS' | 'PROFIT_LOSS' | 'LOW_STOCK'
  frequency: 'DAILY' | 'WEEKLY' | 'MONTHLY'
  timeOfDay: string
  dayOfWeek?: number | null
  dayOfMonth?: number | null
  channels: string
  emailRecipients?: string | null
  whatsappRecipients?: string | null
  isActive: boolean
  lastRunAt?: string | null
  lastStatus?: 'SUCCESS' | 'PARTIAL' | 'FAILED' | null
  lastError?: string | null
  createdAt: string
  updatedAt: string
}

export interface ReportScheduleLogResponse {
  id: string
  scheduleId: string
  businessId: string
  reportType: string
  period: string
  channels: string
  status: 'SUCCESS' | 'PARTIAL' | 'FAILED'
  summaryText?: string | null
  recipientsCount: number
  errorMessage?: string | null
  createdAt: string
}

export interface SendReportNowResult {
  success: boolean
  message: string
  logId?: string | null
  summaryText?: string | null
  whatsappUrls?: string[]
}

export const reportScheduleApi = {
  list: async () => {
    const res = await client.get<ApiResponse<ReportScheduleResponse[]>>('/report-schedules')
    return res.data
  },
  get: async (id: string) => {
    const res = await client.get<ApiResponse<ReportScheduleResponse>>(`/report-schedules/${id}`)
    return res.data
  },
  create: async (data: CreateReportScheduleRequest) => {
    const res = await client.post<ApiResponse<ReportScheduleResponse>>('/report-schedules', data)
    return res.data
  },
  update: async (id: string, data: UpdateReportScheduleRequest) => {
    const res = await client.put<ApiResponse<ReportScheduleResponse>>(`/report-schedules/${id}`, data)
    return res.data
  },
  delete: async (id: string) => {
    const res = await client.delete<ApiResponse<void>>(`/report-schedules/${id}`)
    return res.data
  },
  toggleActive: async (id: string, isActive: boolean) => {
    const res = await client.post<ApiResponse<ReportScheduleResponse>>(`/report-schedules/${id}/toggle-active`, { isActive })
    return res.data
  },
  sendNow: async (id: string) => {
    const res = await client.post<ApiResponse<SendReportNowResult>>(`/report-schedules/${id}/send-now`)
    return res.data
  },
  getLogs: async (id?: string) => {
    const endpoint = id ? `/report-schedules/${id}/logs` : '/report-schedules/logs/all'
    const res = await client.get<ApiResponse<ReportScheduleLogResponse[]>>(endpoint)
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
  generateVat3: async (data: { periodYear: number; periodMonth: number }) => {
    const res = await client.post<ApiResponse<TaxReturnResponse>>('/kra/returns/vat3', data)
    return res.data
  },
  generateTot: async (data: { periodYear: number; periodMonth: number }) => {
    const res = await client.post<ApiResponse<TaxReturnResponse>>('/kra/returns/tot', data)
    return res.data
  },
  generateWht: async (data: { periodYear: number; periodMonth: number }) => {
    const res = await client.post<ApiResponse<TaxReturnResponse>>('/kra/returns/wht', data)
    return res.data
  },
  markReturnSubmitted: async (id: string, ackNo: string) => {
    const params = new URLSearchParams({ ackNo })
    const res = await client.patch<ApiResponse<null>>(`/kra/returns/${id}/submitted?${params.toString()}`)
    return res.data
  },
  exportCsv: async (data: { returnType: TaxReturnResponse['returnType']; periodYear: number; periodMonth: number; format?: string }) => {
    const res = await client.post<ApiResponse<CsvExportResponse>>('/kra/export/csv', data)
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
  discoverMetaBusinessAssets: async (code: string) => {
    const res = await client.post<ApiResponse<MetaBusinessDiscovery>>('/social/meta/business-login/discover', { code })
    return res.data
  },
  connectMetaBusinessAssets: async (data: {
    sessionToken: string
    selections: Array<{ platform: 'FACEBOOK' | 'INSTAGRAM'; accountId: string; channelName?: string }>
  }) => {
    const res = await client.post<ApiResponse<SocialChannel[]>>('/social/meta/business-login/connect', data)
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
  auditLogs: async (limit = 100, businessId?: string) => {
    const res = await client.get<ApiResponse<AuditLogResponse[]>>('/audit-logs', {
      params: { ...(businessId ? { businessId } : {}), limit },
    })
    return res.data
  },
  invite: async (data: InviteUserRequest, businessId?: string) => {
    const res = await client.post<ApiResponse<UserResponse>>('/users', data, {
      params: businessId ? { businessId } : undefined,
    })
    return res.data
  },
  edit: async (id: string, data: EditUserRequest, businessId?: string) => {
    const res = await client.put<ApiResponse<UserResponse>>(`/users/${id}`, data, {
      params: businessId ? { businessId } : undefined,
    })
    return res.data
  },
  unlock: async (id: string, businessId?: string) => {
    const res = await client.post<ApiResponse<UserResponse>>(`/users/${id}/unlock`, {}, {
      params: businessId ? { businessId } : undefined,
    })
    return res.data
  },
  resetPassword: async (id: string, newPassword: string, businessId?: string) => {
    const res = await client.post<ApiResponse<UserResponse>>(`/users/${id}/reset-password`, { newPassword }, {
      params: businessId ? { businessId } : undefined,
    })
    return res.data
  },
  setStaffPin: async (id: string, pin: string, businessId?: string) => {
    const res = await client.put<ApiResponse<UserResponse>>(`/users/${id}/pin`, { pin }, {
      params: businessId ? { businessId } : undefined,
    })
    return res.data
  },
  removeStaffPin: async (id: string, businessId?: string) => {
    const res = await client.delete<ApiResponse<UserResponse>>(`/users/${id}/pin`, {
      params: businessId ? { businessId } : undefined,
    })
    return res.data
  },
  assignBranches: async (id: string, branchIds: string[], primaryBranchId?: string, businessId?: string) => {
    const res = await client.put<ApiResponse<UserResponse>>(`/users/${id}/branches`, { branchIds, primaryBranchId }, {
      params: businessId ? { businessId } : undefined,
    })
    return res.data
  },
  getActivity: async (id: string, businessId?: string) => {
    const res = await client.get<ApiResponse<AuditLogResponse[]>>(`/users/${id}/activity`, {
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
  updateGroups: async (id: string, groupIds: string[], businessId?: string) => {
    const res = await client.put<ApiResponse<UserResponse>>(`/users/${id}/groups`, { groupIds }, {
      params: businessId ? { businessId } : undefined,
    })
    return res.data
  },
  updateRoles: async (id: string, roleIds: string[], businessId?: string) => {
    const res = await client.put<ApiResponse<UserResponse>>(`/users/${id}/roles`, { roleIds }, {
      params: businessId ? { businessId } : undefined,
    })
    return res.data
  },
  reassign: async (id: string, data: ReassignUserRequest, businessId?: string) => {
    const res = await client.put<ApiResponse<UserResponse>>(`/users/${id}/reassign`, data, {
      params: businessId ? { businessId } : undefined,
    })
    return res.data
  },
}

export interface AuditLogResponse {
  id: string
  businessId: string | null
  actorUserId: string | null
  actorName?: string | null
  targetUserId: string | null
  targetName?: string | null
  action: string
  resourceType?: string | null
  resourceId?: string | null
  ipAddress?: string | null
  details?: string | null
  createdAt: string
}

export interface AuditLogFilter {
  limit?: number
  businessId?: string
  action?: string
  search?: string
  startDate?: string
  endDate?: string
}

export const auditLogApi = {
  list: async (filters: AuditLogFilter = {}) => {
    const res = await client.get<ApiResponse<AuditLogResponse[]>>('/audit-logs', {
      params: filters,
    })
    return res.data
  },
}

export const accessApi = {
  me: async () => (await client.get<ApiResponse<{ enabledMenus: string[]; permissions?: string[] }>>('/access/me')).data,
  config: async (businessId?: string) => (await client.get<ApiResponse<AccessConfig>>('/access/config', { params: businessId ? { businessId } : undefined })).data,
  updateMenus: async (enabledMenus: string[], businessId?: string) => (await client.put<ApiResponse<AccessConfig>>('/access/config/menus', { enabledMenus }, { params: businessId ? { businessId } : undefined })).data,
  createRole: async (data: { name: string; description: string; allowedMenus?: string[]; permissions?: string[]; isActive?: boolean }, businessId?: string) => (await client.post<ApiResponse<AccessRole>>('/access/config/roles', data, { params: businessId ? { businessId } : undefined })).data,
  updateRole: async (id: string, data: { name: string; description: string; allowedMenus?: string[]; permissions?: string[]; isActive: boolean }, businessId?: string) => (await client.put<ApiResponse<AccessRole>>(`/access/config/roles/${id}`, data, { params: businessId ? { businessId } : undefined })).data,
  deleteRole: async (id: string, businessId?: string) => (await client.delete<ApiResponse<boolean>>(`/access/config/roles/${id}`, { params: businessId ? { businessId } : undefined })).data,
  createGroup: async (data: { name: string; description: string; allowedMenus?: string[]; roleIds?: string[] }, businessId?: string) => (await client.post<ApiResponse<AccessGroup>>('/access/config/groups', data, { params: businessId ? { businessId } : undefined })).data,
  updateGroup: async (id: string, data: { name: string; description: string; allowedMenus?: string[]; roleIds?: string[]; isActive: boolean }, businessId?: string) => (await client.put<ApiResponse<AccessGroup>>(`/access/config/groups/${id}`, data, { params: businessId ? { businessId } : undefined })).data,
  deleteGroup: async (id: string, businessId?: string) => (await client.delete<ApiResponse<boolean>>(`/access/config/groups/${id}`, { params: businessId ? { businessId } : undefined })).data,
  assignUsers: async (groupId: string, userIds: string[], businessId?: string) => (await client.put<ApiResponse<AccessGroup>>(`/access/config/groups/${groupId}/users`, { userIds }, { params: businessId ? { businessId } : undefined })).data,
}

export const hospitalityApi = {
  status: async () => (await client.get<ApiResponse<{enabled:boolean; shiftOpen?: boolean}>>('/hospitality/status')).data,
  dashboard: async () => (await client.get<ApiResponse<HospitalityDashboard>>('/hospitality')).data,
  setEnabled: async (enabled:boolean) => (await client.put<ApiResponse<HospitalityDashboard>>('/hospitality/enabled',{enabled})).data,
  createTable: async (data:{name:string;area:string;capacity:number}) => (await client.post<ApiResponse<HospitalityTable>>('/hospitality/tables',data)).data,
  updateTable: async (id:string,data:{name:string;area:string;capacity:number}) => (await client.put<ApiResponse<HospitalityTable>>(`/hospitality/tables/${id}`,data)).data,
  createOrder: async (data:{tableId?:string;serviceType:string;guestCount:number;customerName:string;customerPhone:string;notes:string;ageVerified?:boolean;items:Array<{productId:string;quantity:number;unitPrice:number;modifiers?:MenuOption[];itemNote?:string;discountAmount?:number;complimentary?:boolean}>}) => (await client.post<ApiResponse<OrderResponse>>('/hospitality/orders',data)).data,
  updateTicket: async (id:string,status:string) => (await client.patch<ApiResponse<KitchenTicket>>(`/hospitality/tickets/${id}`,{status})).data,
  transferTab: async (orderId:string,tableId:string) => (await client.post<ApiResponse<OrderResponse>>(`/hospitality/tabs/${orderId}/transfer`,{tableId})).data,
  closeTab: async (orderId:string,paymentMethod:string) => (await client.post<ApiResponse<OrderResponse>>(`/hospitality/tabs/${orderId}/close`,{paymentMethod})).data,
}

export const hospitalityOpsApi = {
  staff: async () => (await client.get(`/hospitality/operations/staff`)).data,
  dashboard: async () => (await client.get<ApiResponse<HospitalityOperations>>('/hospitality/operations')).data,
  reservation: async (data:any) => (await client.post('/hospitality/operations/reservations',data)).data,
  reservationStatus: async (id:string,status:string) => (await client.patch(`/hospitality/operations/reservations/${id}/${status}`)).data,
  table: async (id:string,data:any) => (await client.put(`/hospitality/operations/tables/${id}`,data)).data,
  menu: async (productId:string,data:any) => (await client.put(`/hospitality/operations/menu/${productId}`,data)).data,
  ingredient: async (data:any) => (await client.post('/hospitality/operations/ingredients',data)).data,
  getRecipe: async (productId:string) => (await client.get(`/hospitality/operations/recipes/${productId}`)).data,
  recipe: async (productId:string,lines:any[]) => (await client.put(`/hospitality/operations/recipes/${productId}`,{lines})).data,
  barStock: async (data:any) => (await client.post('/hospitality/operations/bar-stock',data)).data,
  openShift: async (data:any) => (await client.post('/hospitality/operations/shifts/open',data)).data,
  closeShift: async (id:string,data:any) => (await client.post(`/hospitality/operations/shifts/${id}/close`,data)).data,
  supplier: async (data:any) => (await client.post('/hospitality/operations/suppliers',data)).data,
  purchaseOrder: async (data:any) => (await client.post('/hospitality/operations/purchase-orders',data)).data,
  receivePurchaseOrder: async (id:string) => (await client.post(`/hospitality/operations/purchase-orders/${id}/receive`)).data,
  approval: async (data:any) => (await client.post('/hospitality/operations/approvals',data)).data,
  decideApproval: async (id:string,approved:boolean) => (await client.post(`/hospitality/operations/approvals/${id}/decision`,{approved})).data,
  splitBill: async (orderId:string,payments:any[]) => (await client.post(`/hospitality/operations/tabs/${orderId}/split`,{payments})).data,
  report: async (startDate:string,endDate:string) => (await client.get(`/hospitality/operations/report`,{params:{startDate,endDate}})).data,
}

export const supplierApi = {
  list: async () => (await client.get<ApiResponse<Supplier[]>>('/suppliers')).data,
  create: async (data: { name: string; phone?: string; email?: string; address?: string }) =>
    (await client.post<ApiResponse<Supplier>>('/suppliers', data)).data,
  update: async (id: string, data: { name: string; phone?: string; email?: string; address?: string }) =>
    (await client.put<ApiResponse<Supplier>>(`/suppliers/${id}`, data)).data,
  delete: async (id: string) =>
    (await client.delete<ApiResponse<boolean>>(`/suppliers/${id}`)).data,
}

export const servicesApi = {
  status: async () => (await client.get<ApiResponse<{ enabled: boolean }>>('/services/status')).data,
  setEnabled: async (enabled: boolean) => (await client.put<ApiResponse<{ enabled: boolean }>>('/services/enabled', { enabled })).data,
  schedule: async (params?: { from?: string; to?: string }) => (await client.get<ApiResponse<ServiceSchedule>>('/services', { params })).data,
  catalog: async () => (await client.get<ApiResponse<ServiceCatalogItem[]>>('/services/catalog')).data,
  createCatalog: async (data: { name: string; description: string; category: string; durationMinutes: number; price: number; isActive?: boolean }) => (await client.post<ApiResponse<ServiceCatalogItem>>('/services/catalog', data)).data,
  updateCatalog: async (id: string, data: { name: string; description: string; category: string; durationMinutes: number; price: number; isActive: boolean }) => (await client.put<ApiResponse<ServiceCatalogItem>>(`/services/catalog/${id}`, data)).data,
  createResource: async (data: { name: string; type: string; isActive?: boolean }) => (await client.post<ApiResponse<ServiceResource>>('/services/resources', data)).data,
  updateResource: async (id: string, data: { name: string; type: string; isActive: boolean }) => (await client.put<ApiResponse<ServiceResource>>(`/services/resources/${id}`, data)).data,
  appointments: async (params?: { from?: string; to?: string }) => (await client.get<ApiResponse<ServiceAppointment[]>>('/services/appointments', { params })).data,
  createAppointment: async (data: { serviceId: string; resourceId?: string | null; customerId?: string | null; customerName: string; customerPhone?: string; staffUserId?: string | null; startsAt: string; durationMinutes?: number; notes?: string }) => (await client.post<ApiResponse<ServiceAppointment>>('/services/appointments', data)).data,
  updateAppointment: async (id: string, data: { serviceId: string; resourceId?: string | null; customerId?: string | null; customerName: string; customerPhone?: string; staffUserId?: string | null; startsAt: string; durationMinutes?: number; notes?: string }) => (await client.put<ApiResponse<ServiceAppointment>>(`/services/appointments/${id}`, data)).data,
  updateAppointmentStatus: async (id: string, status: string) => (await client.patch<ApiResponse<ServiceAppointment>>(`/services/appointments/${id}/status`, { status })).data,
  checkoutAppointment: async (id: string, data: { paymentMethod: 'CASH' | 'MPESA' | 'CARD'; discountAmount: number; addOns: Array<{ productId: string; quantity: number }> }) => (await client.post<ApiResponse<OrderResponse>>(`/services/appointments/${id}/checkout`, data)).data,
  seedTemplates: async () => (await client.post<ApiResponse<ServiceSchedule>>('/services/templates')).data,
}

export const cyberSourceApi = {
  getTransactions: async () => {
    const res = await client.get<ApiResponse<CsTransactionRecord[]>>('/payments/card/manage/transactions')
    return res.data
  },
  getSavedCards: async () => {
    const res = await client.get<ApiResponse<SavedCardResponse[]>>('/payments/card/manage/saved-cards')
    return res.data
  },
  deleteSavedCard: async (id: string) => {
    const res = await client.delete<ApiResponse<null>>(`/payments/card/manage/saved-cards/${id}`)
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
  socialLogin: async (credential: { provider: 'google' | 'facebook'; token: string }) => {
    return (await client.post<ApiResponse<{ profile: { name: string; email: string }; login?: LoginResponse }>>('/auth/social/login', credential)).data
  },
  login: async (req: LoginRequest) => {
    const res = await client.post<ApiResponse<LoginResponse>>('/auth/login', req)
    return res.data
  },
  loginWithPin: async (req: PinLoginRequest) => {
    const res = await client.post<ApiResponse<LoginResponse>>('/auth/pin-login', req)
    return res.data
  },
  setLoginPin: async (data: { currentPassword: string; pin?: string; disable?: boolean }) => {
    const res = await client.post<ApiResponse<null>>('/auth/login-pin', data)
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

  register: async (req: RegisterRequest & { socialCredential?: { provider: 'google' | 'facebook'; token: string } }) => {
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
  },

  forgotPassword: async (email: string) => {
    const res = await client.post<ApiResponse<null>>('/auth/forgot-password', { email })
    return res.data
  },

  resetPassword: async (data: { token: string; newPassword: string }) => {
    const res = await client.post<ApiResponse<null>>('/auth/reset-password', data)
    return res.data
  },

  changePassword: async (data: { currentPassword: string; newPassword: string }) => {
    const res = await client.post<ApiResponse<null>>('/auth/change-password', data)
    return res.data
  }
}

export const subscriptionApi = {
  bands: async () => (await client.get<ApiResponse<SubscriptionBand[]>>('/subscriptions/bands')).data,
  checkout: async (data: { userCount: number; paymentMethod: 'MPESA'|'CARD'; phoneNumber: string }) => (await client.post<ApiResponse<OrderResponse>>('/subscriptions/checkout', data)).data,
}

export interface BusinessResponse {
  id: string
  name: string
  type: string
  ownerPhone: string
  ownerEmail: string
  subscriptionTier: string
  subscriptionEnabled: boolean
  servicesEnabled?: boolean
  hospitalityEnabled: boolean
  isActive: boolean
  createdAt: string
  isTrial?: boolean
  subscriptionValidUntil?: string | null
  daysRemaining?: number | null
  isExpired?: boolean
}

export interface UpdateBusinessStatusRequest {
  isActive: boolean
}

export interface UpdateSubscriptionRequest {
  enabled: boolean
  tier?: 'FREEMIUM' | 'TRIAL' | 'PREMIUM'
  isTrial?: boolean
  extendDays?: number
  validUntil?: string
  maxUsers?: number
  note?: string
}

export interface ExtendSubscriptionRequest {
  extendDays: number
  isTrial?: boolean
  tier?: 'FREEMIUM' | 'TRIAL' | 'PREMIUM'
  note?: string
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
  receiptLogo?: string | null
  receiptLogoWidthMm?: number
  receiptLogoHeightMm?: number
  receiptShowTax?: boolean
  receiptShowCustomer?: boolean
  storefrontThemeColor?: string
  storefrontHeadline?: string
  storefrontDescription?: string
  storefrontBannerUrl?: string | null
  storefrontLayout?: 'GRID' | 'LIST'
  dayStartTime?: string
  dayCloseTime?: string
}

export interface BusinessProfileResponse {
  id: string
  storefrontSlug: string
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
  isTrial?: boolean
  subscriptionValidUntil?: string | null
  daysRemaining?: number | null
  isExpired?: boolean
  servicesEnabled?: boolean
  hospitalityEnabled: boolean
  receiptHeader?: string
  receiptFooter?: string
  receiptLogo?: string | null
  receiptLogoWidthMm?: number
  receiptLogoHeightMm?: number
  receiptShowTax?: boolean
  receiptShowCustomer?: boolean
  storefrontThemeColor?: string
  storefrontHeadline?: string
  storefrontDescription?: string
  storefrontBannerUrl?: string | null
  storefrontLayout?: 'GRID' | 'LIST'
  dayStartTime?: string
  dayCloseTime?: string
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

export interface BranchRequest {
  name: string
  code?: string
  phone?: string
  email?: string
  address?: string
  city?: string
  county?: string
  isHeadOffice?: boolean
  receiptHeader?: string
  receiptFooter?: string
}

export interface BranchResponse {
  id: string
  businessId: string
  name: string
  code: string
  phone?: string | null
  email?: string | null
  address?: string | null
  city?: string | null
  county?: string | null
  isHeadOffice: boolean
  isActive: boolean
  receiptHeader?: string | null
  receiptFooter?: string | null
  createdAt: string
  updatedAt: string
}

export const branchApi = {
  getAll: async (includeInactive = false) => {
    const res = await client.get<ApiResponse<BranchResponse[]>>(`/branches?includeInactive=${includeInactive}`)
    return res.data
  },
  getById: async (id: string) => {
    const res = await client.get<ApiResponse<BranchResponse>>(`/branches/${id}`)
    return res.data
  },
  create: async (data: BranchRequest) => {
    const res = await client.post<ApiResponse<BranchResponse>>('/branches', data)
    return res.data
  },
  update: async (id: string, data: BranchRequest) => {
    const res = await client.put<ApiResponse<BranchResponse>>(`/branches/${id}`, data)
    return res.data
  },
  delete: async (id: string) => {
    const res = await client.delete<ApiResponse<void>>(`/branches/${id}`)
    return res.data
  },
  setHeadOffice: async (id: string) => {
    const res = await client.post<ApiResponse<BranchResponse>>(`/branches/${id}/set-head-office`)
    return res.data
  }
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
  extendSubscription: async (businessId: string, data: ExtendSubscriptionRequest) => {
    const res = await client.post<ApiResponse<BusinessResponse>>(`/admin/businesses/${businessId}/subscription/extend`, data)
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
  getEmailStatus: async () => {
    const res = await client.get<ApiResponse<{
      configured: boolean
      host: string
      port: number
      username: string
      fromEmail: string
      fromName: string
    }>>('/admin/email/status')
    return res.data
  },
  updateEmailSettings: async (data: {
    host?: string
    port?: number
    username?: string
    password?: string
    fromEmail?: string
    fromName?: string
  }) => {
    const res = await client.put<ApiResponse<{
      configured: boolean
      host: string
      port: number
      username: string
      fromEmail: string
      fromName: string
    }>>('/admin/email/settings', data)
    return res.data
  },
  sendTestEmail: async (email: string) => {
    const res = await client.post<ApiResponse<void>>('/admin/email/test', { email })
    return res.data
  },
  getSubscriptions: async () => {
    const res = await client.get<ApiResponse<PlatformSubscriptionSummaryResponse>>('/admin/subscriptions')
    return res.data
  },
  getPlatformExpenses: async () => {
    const res = await client.get<ApiResponse<PlatformExpensesSummaryResponse>>('/admin/platform-expenses')
    return res.data
  },
  createPlatformExpense: async (data: CreatePlatformExpenseRequest) => {
    const res = await client.post<ApiResponse<PlatformExpenseResponse>>('/admin/platform-expenses', data)
    return res.data
  },
}

export interface SubscriptionRecordResponse {
  businessId: string
  businessName: string
  businessType: string
  ownerEmail: string
  ownerPhone: string
  subscriptionTier: string
  isTrial: boolean
  subscriptionEnabled: boolean
  userCount: number
  maxUsers: number
  validUntil: string | null
  daysRemaining: number | null
  isExpired: boolean
  monthlyRevenue: number
  createdAt: string
}

export interface PlatformSubscriptionSummaryResponse {
  totalTenants: number
  activeSubscriptions: number
  trialSubscriptions: number
  expiredSubscriptions: number
  totalEstimatedMRR: number
  subscriptions: SubscriptionRecordResponse[]
}

export interface PlatformExpenseResponse {
  id: string
  title: string
  category: string
  amount: number
  currency: string
  vendor: string
  expenseDate: string
  notes: string
  createdBy: string | null
  createdAt: string
}

export interface CreatePlatformExpenseRequest {
  title: string
  category: string
  amount: number
  currency?: string
  vendor?: string
  expenseDate?: string
  notes?: string
}

export interface PlatformExpensesSummaryResponse {
  totalAmount: number
  currency: string
  byCategory: Record<string, number>
  expenses: PlatformExpenseResponse[]
}

export const adminApi = superAdminApi

// ─── App Releases (Signed Android & Desktop Apps) ────────────────────────────
export interface AppRelease {
  id: string
  platform: 'ANDROID' | 'WINDOWS' | 'LINUX' | 'MACOS' | 'IOS'
  version: string
  buildNumber: number
  fileName: string
  fileSizeBytes: number
  sha256?: string
  downloadUrl: string
  releaseNotes?: string
  minOsVersion?: string
  isSigned: boolean
  isActive: boolean
  downloadCount: number
  uploadedBy?: string
  createdAt: string
  updatedAt: string
}

export const appReleaseApi = {
  /** Converts backend-relative paths (/v1/downloads/files/..) into absolute API URLs. */
  resolveUrl: (url: string) => {
    if (/^https?:\/\//i.test(url)) return url
    const origin = API_BASE_URL.replace(/\/v1\/?$/, '')
    return `${origin}${url.startsWith('/') ? '' : '/'}${url}`
  },
  // Public
  getReleases: async () => {
    const res = await client.get<ApiResponse<Record<string, AppRelease>>>('/downloads/releases')
    return res.data
  },
  getPublicList: async () => {
    const res = await client.get<ApiResponse<AppRelease[]>>('/downloads/list')
    return res.data
  },
  // Admin / SuperAdmin
  list: async (platform?: string) => {
    const res = await client.get<ApiResponse<AppRelease[]>>(`/admin/app-releases${platform ? `?platform=${platform}` : ''}`)
    return res.data
  },
  upload: async (formData: FormData, onProgress?: (percent: number) => void) => {
    const res = await client.post<ApiResponse<AppRelease>>('/admin/app-releases/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: (progressEvent) => {
        if (progressEvent.total && onProgress) {
          onProgress(Math.round((progressEvent.loaded * 100) / progressEvent.total))
        }
      }
    })
    return res.data
  },
  createExternal: async (data: {
    platform: string
    version: string
    buildNumber?: number
    fileName?: string
    downloadUrl: string
    releaseNotes?: string
    minOsVersion?: string
    isSigned?: boolean
    isActive?: boolean
  }) => {
    const res = await client.post<ApiResponse<AppRelease>>('/admin/app-releases/external', data)
    return res.data
  },
  toggleStatus: async (id: string, isActive: boolean) => {
    const res = await client.patch<ApiResponse<AppRelease>>(`/admin/app-releases/${id}/status`, { isActive })
    return res.data
  },
  delete: async (id: string) => {
    const res = await client.delete<ApiResponse<void>>(`/admin/app-releases/${id}`)
    return res.data
  }
}

export default client


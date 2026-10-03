import React, { useState, useEffect, useMemo } from 'react'
import { useSearchParams, useNavigate } from 'react-router-dom'
import {
  Users, Shield, Activity, Plus, Search, Building2, MapPin, Key, Lock, Unlock,
  RotateCcw, Edit2, Check, X, ChevronRight, Filter, Calendar, RefreshCw,
  Clock, ArrowRightLeft, ShieldCheck, CheckCircle2, AlertTriangle, Eye, Sliders,
  UserCheck, UserX, FileText, Phone, Mail, Laptop, Store
} from 'lucide-react'
import { Card, Btn, Input, Select, Modal, DataTable, StatusBadge, PageHeader, KpiCard } from '../components/ui'
import {
  userApi, accessApi, superAdminApi, branchApi, auditLogApi,
  UserResponse, InviteUserRequest, EditUserRequest, BranchResponse,
  AccessConfig, AccessRole, AccessGroup, PermissionDefinition,
  AuditLogResponse, BusinessResponse
} from '../services/api'
import { useAuth } from '../App'

type ActiveTab = 'users' | 'roles' | 'audit'

// ── Standard Roles Presets (9 roles) ──────────────────────────────────────────
const STANDARD_ROLE_PRESETS: Record<string, { label: string; description: string; permissions: string[]; menus: string[] }> = {
  PLATFORM_ADMIN: {
    label: 'Platform Admin',
    description: 'Manage the entire Biashara360 platform, businesses, config and administrators',
    permissions: [
      'users.view', 'users.create', 'users.edit', 'users.delete', 'users.roles_manage',
      'orders.view', 'orders.create', 'orders.edit', 'orders.cancel', 'orders.refund',
      'products.view', 'products.create', 'products.edit', 'products.delete',
      'inventory.view', 'inventory.adjust', 'inventory.transfer', 'inventory.suppliers',
      'payments.view', 'payments.process', 'payments.refund',
      'reports.view', 'reports.financial', 'reports.export',
      'settings.general', 'settings.security', 'settings.branches', 'settings.tax', 'settings.integrations'
    ],
    menus: ['DASHBOARD', 'POS', 'ORDERS', 'CUSTOMERS', 'PRODUCTS', 'INVENTORY', 'PAYMENTS', 'REPORTS', 'SETTINGS']
  },
  BUSINESS_OWNER: {
    label: 'Business Owner',
    description: 'Full access to own business, financial reports, all branches and configurations',
    permissions: [
      'users.view', 'users.create', 'users.edit', 'users.delete', 'users.roles_manage',
      'orders.view', 'orders.create', 'orders.edit', 'orders.cancel', 'orders.refund',
      'products.view', 'products.create', 'products.edit', 'products.delete',
      'inventory.view', 'inventory.adjust', 'inventory.transfer', 'inventory.suppliers',
      'payments.view', 'payments.process', 'payments.refund',
      'reports.view', 'reports.financial', 'reports.export',
      'settings.general', 'settings.security', 'settings.branches', 'settings.tax', 'settings.integrations'
    ],
    menus: ['DASHBOARD', 'POS', 'ORDERS', 'CUSTOMERS', 'PRODUCTS', 'INVENTORY', 'PAYMENTS', 'REPORTS', 'SETTINGS']
  },
  BUSINESS_ADMIN: {
    label: 'Business Admin',
    description: 'Manage users, outlets, products, orders and configurations',
    permissions: [
      'users.view', 'users.create', 'users.edit', 'users.delete', 'users.roles_manage',
      'orders.view', 'orders.create', 'orders.edit', 'orders.cancel', 'orders.refund',
      'products.view', 'products.create', 'products.edit', 'products.delete',
      'inventory.view', 'inventory.adjust', 'inventory.transfer', 'inventory.suppliers',
      'payments.view', 'payments.process', 'payments.refund',
      'reports.view', 'reports.financial', 'reports.export',
      'settings.general', 'settings.branches', 'settings.tax'
    ],
    menus: ['DASHBOARD', 'POS', 'ORDERS', 'CUSTOMERS', 'PRODUCTS', 'INVENTORY', 'PAYMENTS', 'REPORTS', 'SETTINGS']
  },
  MANAGER: {
    label: 'Manager',
    description: 'Manage assigned outlets, orders, inventory and reports',
    permissions: [
      'users.view',
      'orders.view', 'orders.create', 'orders.edit', 'orders.cancel', 'orders.refund',
      'products.view', 'products.create', 'products.edit', 'products.delete',
      'inventory.view', 'inventory.adjust', 'inventory.transfer', 'inventory.suppliers',
      'payments.view', 'payments.process', 'payments.refund',
      'reports.view', 'reports.financial', 'reports.export',
      'settings.general', 'settings.branches'
    ],
    menus: ['DASHBOARD', 'POS', 'ORDERS', 'CUSTOMERS', 'PRODUCTS', 'INVENTORY', 'PAYMENTS', 'REPORTS']
  },
  CASHIER: {
    label: 'Cashier',
    description: 'POS sales, payments, orders, receipts and cash register',
    permissions: [
      'orders.view', 'orders.create',
      'products.view',
      'payments.view', 'payments.process'
    ],
    menus: ['POS', 'ORDERS', 'PAYMENTS', 'CUSTOMERS']
  },
  KITCHEN_STAFF: {
    label: 'Kitchen/Order Staff',
    description: 'View order queue, kitchen tickets and process assigned orders',
    permissions: [
      'orders.view', 'orders.edit',
      'products.view'
    ],
    menus: ['ORDERS', 'HOSPITALITY_KITCHEN']
  },
  INVENTORY_STAFF: {
    label: 'Inventory Staff',
    description: 'Products, stock counts, adjustments and stock movements',
    permissions: [
      'products.view', 'products.create', 'products.edit',
      'inventory.view', 'inventory.adjust', 'inventory.transfer', 'inventory.suppliers'
    ],
    menus: ['PRODUCTS', 'INVENTORY', 'SUPPLIERS']
  },
  ACCOUNTANT: {
    label: 'Accountant',
    description: 'Sales reconciliation, settlements, expenses and financial reports',
    permissions: [
      'orders.view',
      'payments.view',
      'reports.view', 'reports.financial', 'reports.export',
      'settings.tax'
    ],
    menus: ['REPORTS', 'PAYMENTS', 'ORDERS', 'EXPENSES']
  },
  VIEWER_AUDITOR: {
    label: 'Viewer/Auditor',
    description: 'Read-only access for compliance audit and record inspection',
    permissions: [
      'users.view',
      'orders.view',
      'products.view',
      'inventory.view',
      'payments.view',
      'reports.view'
    ],
    menus: ['DASHBOARD', 'ORDERS', 'PRODUCTS', 'INVENTORY', 'REPORTS']
  },
}

// ── Granular Permission Matrix Definitions ────────────────────────────────────
interface PermissionMatrixModule {
  key: string
  label: string
  description: string
  view?: { code: string; label: string }
  create?: { code: string; label: string }
  edit?: { code: string; label: string }
  delete?: { code: string; label: string }
  special?: { code: string; label: string }
}

const PERMISSION_MODULES: PermissionMatrixModule[] = [
  {
    key: 'users',
    label: 'Users & Access',
    description: 'User profiles, passwords, PINs, outlet assignment & roles',
    view: { code: 'users.view', label: 'View Staff & Roles' },
    create: { code: 'users.create', label: 'Create New Users' },
    edit: { code: 'users.edit', label: 'Edit Users & Outlets' },
    delete: { code: 'users.delete', label: 'Disable / Deactivate' },
    special: { code: 'users.roles_manage', label: 'Manage Roles & Matrix' },
  },
  {
    key: 'orders',
    label: 'Orders & POS',
    description: 'Point of sale, order creation, order modification and refunds',
    view: { code: 'orders.view', label: 'View Orders & Bills' },
    create: { code: 'orders.create', label: 'Create POS Orders' },
    edit: { code: 'orders.edit', label: 'Modify Open Orders' },
    delete: { code: 'orders.cancel', label: 'Void / Cancel Orders' },
    special: { code: 'orders.refund', label: 'Authorize Refunds' },
  },
  {
    key: 'products',
    label: 'Catalog & Products',
    description: 'Product definitions, categories, prices and barcode tags',
    view: { code: 'products.view', label: 'View Products Catalog' },
    create: { code: 'products.create', label: 'Create New Products' },
    edit: { code: 'products.edit', label: 'Edit Prices & Info' },
    delete: { code: 'products.delete', label: 'Delete Products' },
  },
  {
    key: 'inventory',
    label: 'Stock & Inventory',
    description: 'Stock levels, stock audits, warehouse transfers and purchase orders',
    view: { code: 'inventory.view', label: 'View Stock Quantities' },
    create: { code: 'inventory.transfer', label: 'Branch Transfers' },
    edit: { code: 'inventory.suppliers', label: 'Supplier Orders (PO)' },
    special: { code: 'inventory.adjust', label: 'Stock Write-offs / Adjust' },
  },
  {
    key: 'payments',
    label: 'Payments & Cash',
    description: 'Card terminal, M-Pesa STK, cash drawer and reversals',
    view: { code: 'payments.view', label: 'View Transactions' },
    create: { code: 'payments.process', label: 'Collect Payments' },
    special: { code: 'payments.refund', label: 'Payment Reversals' },
  },
  {
    key: 'reports',
    label: 'Reports & Analytics',
    description: 'Sales summaries, financial statements, eTIMS tax and data export',
    view: { code: 'reports.view', label: 'View Analytics' },
    edit: { code: 'reports.financial', label: 'Financial & Profit P&L' },
    special: { code: 'reports.export', label: 'Export Data & CSV' },
  },
  {
    key: 'settings',
    label: 'Settings & Security',
    description: 'Store profiles, branch setup, tax compliance and security policies',
    view: { code: 'settings.general', label: 'View Store Settings' },
    create: { code: 'settings.branches', label: 'Manage Outlets/Branches' },
    edit: { code: 'settings.tax', label: 'Tax & eTIMS Setup' },
    delete: { code: 'settings.security', label: 'Security & PIN Policies' },
    special: { code: 'settings.integrations', label: 'Integrations & API' },
  },
]

function formatRelativeTime(dateStr?: string | null): string {
  if (!dateStr) return 'Never'
  try {
    const d = new Date(dateStr)
    const now = new Date()
    const diffMs = now.getTime() - d.getTime()
    if (diffMs < 0) return 'Just now'
    const diffSec = Math.floor(diffMs / 1000)
    if (diffSec < 60) return `${diffSec}s ago`
    const diffMin = Math.floor(diffSec / 60)
    if (diffMin < 60) return `${diffMin}m ago`
    const diffHour = Math.floor(diffMin / 60)
    if (diffHour < 24) return `${diffHour}h ago`
    const diffDay = Math.floor(diffHour / 24)
    if (diffDay === 1) return 'Yesterday'
    if (diffDay < 7) return `${diffDay}d ago`
    return d.toLocaleDateString('en-KE', { month: 'short', day: 'numeric', year: 'numeric' })
  } catch {
    return '—'
  }
}

export function UserCreationPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const navigate = useNavigate()
  const { user: currentUser } = useAuth()
  const isSuperAdmin = currentUser?.role === 'SUPERADMIN'

  // Tab State
  const tabFromUrl = (searchParams.get('tab') as ActiveTab) || 'users'
  const [activeTab, setActiveTab] = useState<ActiveTab>(tabFromUrl)

  useEffect(() => {
    const t = searchParams.get('tab') as ActiveTab
    if (t && ['users', 'roles', 'audit'].includes(t) && t !== activeTab) {
      setActiveTab(t)
    }
  }, [searchParams])

  const handleTabChange = (tab: ActiveTab) => {
    setActiveTab(tab)
    setSearchParams({ tab })
  }

  // ── Businesses list (SUPERADMIN only) ──
  const [businesses, setBusinesses] = useState<BusinessResponse[]>([])
  const [selectedBusinessId, setSelectedBusinessId] = useState('')
  const [bizLoading, setBizLoading] = useState(false)
  const accessBusinessId = isSuperAdmin ? selectedBusinessId : undefined

  // ── Data States ──
  const [users, setUsers] = useState<UserResponse[]>([])
  const [branches, setBranches] = useState<BranchResponse[]>([])
  const [accessConfig, setAccessConfig] = useState<AccessConfig | null>(null)
  const [auditLogs, setAuditLogs] = useState<AuditLogResponse[]>([])
  const [usersLoading, setUsersLoading] = useState(false)
  const [auditLoading, setAuditLoading] = useState(false)

  // ── Feedback alerts ──
  const [globalMessage, setGlobalMessage] = useState<{ ok: boolean; text: string } | null>(null)
  const showToast = (text: string, ok = true) => {
    setGlobalMessage({ ok, text })
    setTimeout(() => setGlobalMessage(null), 5000)
  }

  // ── Filters ──
  const [userSearch, setUserSearch] = useState('')
  const [userBranchFilter, setUserBranchFilter] = useState('ALL')
  const [userStatusFilter, setUserStatusFilter] = useState<'ALL' | 'ACTIVE' | 'DISABLED' | 'LOCKED'>('ALL')
  const [userRoleFilter, setUserRoleFilter] = useState('ALL')

  // ── Modals: User Operations ──
  const [showAddUser, setShowAddUser] = useState(false)
  const [addUserForm, setAddUserForm] = useState<InviteUserRequest>({
    name: '', email: '', phone: '', role: 'STAFF', password: '', branchId: ''
  })
  const [addUserBranchIds, setAddUserBranchIds] = useState<string[]>([])
  const [addUserRoleIds, setAddUserRoleIds] = useState<string[]>([])
  const [addUserGroupId, setAddUserGroupId] = useState('')
  const [addUserSaving, setAddUserSaving] = useState(false)
  const [addUserError, setAddUserError] = useState('')

  // Edit User Modal
  const [editModalUser, setEditModalUser] = useState<UserResponse | null>(null)
  const [editForm, setEditForm] = useState<EditUserRequest>({ name: '', email: '', phone: '' })
  const [editBranchIds, setEditBranchIds] = useState<string[]>([])
  const [editSaving, setEditSaving] = useState(false)
  const [editError, setEditError] = useState('')

  // Reset Password Modal
  const [resetPassUser, setResetPassUser] = useState<UserResponse | null>(null)
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [resetPassSaving, setResetPassSaving] = useState(false)
  const [resetPassError, setResetPassError] = useState('')

  // Set / Reset PIN Modal
  const [pinModalUser, setPinModalUser] = useState<UserResponse | null>(null)
  const [pinValue, setPinValue] = useState('')
  const [pinSaving, setPinSaving] = useState(false)
  const [pinError, setPinError] = useState('')

  // Unlock User State
  const [unlockingUserId, setUnlockingUserId] = useState<string | null>(null)

  // Reassign Roles & Branches Modal
  const [reassignModalUser, setReassignModalUser] = useState<UserResponse | null>(null)
  const [reassignRole, setReassignRole] = useState('STAFF')
  const [reassignPrimaryBranch, setReassignPrimaryBranch] = useState('')
  const [reassignBranchIds, setReassignBranchIds] = useState<string[]>([])
  const [reassignGroupIds, setReassignGroupIds] = useState<string[]>([])
  const [reassignRoleIds, setReassignRoleIds] = useState<string[]>([])
  const [reassignSaving, setReassignSaving] = useState(false)
  const [reassignError, setReassignError] = useState('')

  // User Activity Modal
  const [activityModalUser, setActivityModalUser] = useState<UserResponse | null>(null)
  const [userActivities, setUserActivities] = useState<AuditLogResponse[]>([])
  const [activityLoading, setActivityLoading] = useState(false)

  // ── Role Management & Permission Matrix State ──
  const [showRoleModal, setShowRoleModal] = useState(false)
  const [editingRoleId, setEditingRoleId] = useState<string | null>(null)
  const [roleDraft, setRoleDraft] = useState({
    name: '',
    description: '',
    allowedMenus: [] as string[],
    permissions: [] as string[]
  })
  const [roleSaving, setRoleSaving] = useState(false)
  const [roleError, setRoleError] = useState('')

  // ── SuperAdmin Create Business & Admin Modal ──
  const [showCreateAdmin, setShowCreateAdmin] = useState(false)
  const [adminForm, setAdminForm] = useState({
    businessName: '', businessType: 'RETAIL', adminName: '', adminEmail: '', adminPhone: '', adminPassword: ''
  })
  const [adminSaving, setAdminSaving] = useState(false)
  const [adminError, setAdminError] = useState('')

  // ── Audit Logs Filter State ──
  const [auditSearch, setAuditSearch] = useState('')
  const [auditActionFilter, setAuditActionFilter] = useState('ALL')
  const [auditDatePreset, setAuditDatePreset] = useState<'ALL' | 'TODAY' | '7D' | '30D'>('ALL')

  // ── Data Fetching ──
  const loadBusinesses = () => {
    if (!isSuperAdmin) return
    setBizLoading(true)
    superAdminApi.listBusinesses().then(res => {
      if (res.success && res.data) {
        setBusinesses(res.data)
        if (!selectedBusinessId && res.data.length > 0) {
          setSelectedBusinessId(res.data[0].id)
        }
      }
    }).catch(() => {}).finally(() => setBizLoading(false))
  }

  const loadUsers = () => {
    if (isSuperAdmin && !selectedBusinessId) {
      setUsers([])
      return
    }
    setUsersLoading(true)
    userApi.list(accessBusinessId).then(res => {
      if (res.success && res.data) setUsers(res.data)
    }).catch(() => {}).finally(() => setUsersLoading(false))
  }

  const loadBranches = () => {
    if (isSuperAdmin && !selectedBusinessId) {
      setBranches([])
      return
    }
    branchApi.getAll(false).then(res => {
      if (res.success && res.data) setBranches(res.data)
    }).catch(() => {})
  }

  const loadAccessConfig = () => {
    if (isSuperAdmin && !selectedBusinessId) {
      setAccessConfig(null)
      return
    }
    accessApi.config(accessBusinessId).then(res => {
      if (res.success && res.data) setAccessConfig(res.data)
    }).catch(() => {})
  }

  const loadAuditLogs = () => {
    if (isSuperAdmin && !selectedBusinessId) {
      setAuditLogs([])
      return
    }
    setAuditLoading(true)
    userApi.auditLogs(200, accessBusinessId).then(res => {
      if (res.success && res.data) setAuditLogs(res.data)
    }).catch(() => {}).finally(() => setAuditLoading(false))
  }

  useEffect(() => {
    loadBusinesses()
  }, [isSuperAdmin])

  useEffect(() => {
    loadUsers()
    loadBranches()
    loadAccessConfig()
    loadAuditLogs()
  }, [isSuperAdmin, selectedBusinessId])

  // Helper toggle
  const toggleArrayItem = (list: string[], item: string) =>
    list.includes(item) ? list.filter(x => x !== item) : [...list, item]

  // ── User Filtering ──
  const filteredUsers = useMemo(() => {
    return users.filter(u => {
      const q = userSearch.toLowerCase().trim()
      const matchesSearch = !q ||
        u.name.toLowerCase().includes(q) ||
        u.email.toLowerCase().includes(q) ||
        (u.phone && u.phone.includes(q)) ||
        u.role.toLowerCase().includes(q) ||
        (u.branchName && u.branchName.toLowerCase().includes(q))

      const matchesBranch = userBranchFilter === 'ALL' ||
        (userBranchFilter === 'HEAD_OFFICE' && !u.branchId) ||
        u.branchId === userBranchFilter ||
        (u.assignedBranchIds && u.assignedBranchIds.includes(userBranchFilter))

      const status = (u.status || (u.isActive === false ? 'DISABLED' : (u.isPinLocked ? 'LOCKED' : 'ACTIVE'))).toUpperCase()
      const matchesStatus = userStatusFilter === 'ALL' || status === userStatusFilter

      const matchesRole = userRoleFilter === 'ALL' || u.role === userRoleFilter

      return matchesSearch && matchesBranch && matchesStatus && matchesRole
    })
  }, [users, userSearch, userBranchFilter, userStatusFilter, userRoleFilter])

  // ── User KPI Stats ──
  const userStats = useMemo(() => {
    const total = users.length
    const active = users.filter(u => u.isActive !== false && u.status !== 'DISABLED').length
    const locked = users.filter(u => u.status === 'LOCKED' || u.isPinLocked).length
    const multiBranch = users.filter(u => (u.assignedBranchIds?.length || 0) > 1).length
    return { total, active, locked, multiBranch }
  }, [users])

  // ── Handler: Create User ──
  const handleCreateUser = async () => {
    if (!addUserForm.name || !addUserForm.email || !addUserForm.phone) {
      setAddUserError('Name, email, and phone number are required.')
      return
    }
    if (!addUserForm.password || addUserForm.password.length < 6) {
      setAddUserError('Password must be at least 6 characters.')
      return
    }
    setAddUserSaving(true)
    setAddUserError('')
    try {
      const payload: InviteUserRequest = {
        ...addUserForm,
        branchId: addUserForm.branchId || undefined,
        groupId: addUserGroupId || undefined,
        groupIds: addUserGroupId ? [addUserGroupId] : [],
        roleIds: addUserRoleIds,
      }
      const res = await userApi.invite(payload, accessBusinessId)
      if (res.success && res.data) {
        // If multi-branch assigned, update branches
        if (addUserBranchIds.length > 0) {
          await userApi.assignBranches(res.data.id, addUserBranchIds, addUserForm.branchId || undefined, accessBusinessId)
        }
        setShowAddUser(false)
        setAddUserForm({ name: '', email: '', phone: '', role: 'STAFF', password: '', branchId: '' })
        setAddUserBranchIds([])
        setAddUserRoleIds([])
        setAddUserGroupId('')
        loadUsers()
        loadAccessConfig()
        showToast(`User ${res.data.name} created successfully!`)
      } else {
        setAddUserError(res.message || 'Failed to create user.')
      }
    } catch (e: any) {
      setAddUserError(e.response?.data?.message || 'Network error creating user.')
    } finally {
      setAddUserSaving(false)
    }
  }

  // ── Handler: Open Edit User ──
  const openEditModal = (u: UserResponse) => {
    setEditModalUser(u)
    setEditForm({
      name: u.name,
      email: u.email,
      phone: u.phone || '',
      branchId: u.branchId || undefined,
    })
    setEditBranchIds(u.assignedBranchIds || (u.branchId ? [u.branchId] : []))
    setEditError('')
  }

  const handleSaveEditUser = async () => {
    if (!editModalUser) return
    if (!editForm.name.trim() || !editForm.email.trim() || !editForm.phone.trim()) {
      setEditError('Name, email, and phone are required.')
      return
    }
    setEditSaving(true)
    setEditError('')
    try {
      const res = await userApi.edit(editModalUser.id, editForm, accessBusinessId)
      if (res.success && res.data) {
        // Update multi-branch assignments if changed
        await userApi.assignBranches(editModalUser.id, editBranchIds, editForm.branchId || undefined, accessBusinessId)
        showToast(`User details for ${editForm.name} updated successfully!`)
        setEditModalUser(null)
        loadUsers()
      } else {
        setEditError(res.message || 'Failed to update user.')
      }
    } catch (e: any) {
      setEditError(e.response?.data?.message || 'Network error updating user.')
    } finally {
      setEditSaving(false)
    }
  }

  // ── Handler: Unlock Account ──
  const handleUnlockUser = async (u: UserResponse) => {
    setUnlockingUserId(u.id)
    try {
      const res = await userApi.unlock(u.id, accessBusinessId)
      if (res.success) {
        showToast(`Account for ${u.name} has been unlocked and PIN retry counter reset.`)
        loadUsers()
        loadAuditLogs()
      } else {
        showToast(res.message || 'Could not unlock account.', false)
      }
    } catch (e: any) {
      showToast(e.response?.data?.message || 'Network error unlocking account.', false)
    } finally {
      setUnlockingUserId(null)
    }
  }

  // ── Handler: Reset Password ──
  const handleSaveResetPassword = async () => {
    if (!resetPassUser) return
    if (!newPassword || newPassword.length < 6) {
      setResetPassError('Password must be at least 6 characters.')
      return
    }
    if (newPassword !== confirmPassword) {
      setResetPassError('Passwords do not match.')
      return
    }
    setResetPassSaving(true)
    setResetPassError('')
    try {
      const res = await userApi.resetPassword(resetPassUser.id, newPassword, accessBusinessId)
      if (res.success) {
        showToast(`Password for ${resetPassUser.name} has been reset.`)
        setResetPassUser(null)
        setNewPassword('')
        setConfirmPassword('')
        loadAuditLogs()
      } else {
        setResetPassError(res.message || 'Could not reset password.')
      }
    } catch (e: any) {
      setResetPassError(e.response?.data?.message || 'Network error resetting password.')
    } finally {
      setResetPassSaving(false)
    }
  }

  // ── Handler: Staff PIN ──
  const handleSavePin = async (remove = false) => {
    if (!pinModalUser) return
    if (!remove && (!/^\d{6}$/.test(pinValue))) {
      setPinError('Staff PIN must be exactly 6 digits.')
      return
    }
    setPinSaving(true)
    setPinError('')
    try {
      const res = remove
        ? await userApi.removeStaffPin(pinModalUser.id, accessBusinessId)
        : await userApi.setStaffPin(pinModalUser.id, pinValue, accessBusinessId)
      if (res.success) {
        showToast(remove ? `PIN removed for ${pinModalUser.name}.` : `6-digit PIN set for ${pinModalUser.name}.`)
        setPinModalUser(null)
        setPinValue('')
        loadUsers()
        loadAuditLogs()
      } else {
        setPinError(res.message || 'Could not update staff PIN.')
      }
    } catch (e: any) {
      setPinError(e.response?.data?.message || 'Network error updating staff PIN.')
    } finally {
      setPinSaving(false)
    }
  }

  // ── Handler: Toggle Status ──
  const handleToggleStatus = async (u: UserResponse) => {
    const willActivate = u.isActive === false || u.status === 'DISABLED'
    const verb = willActivate ? 'activate' : 'deactivate'
    if (!window.confirm(`Are you sure you want to ${verb} ${u.name}?`)) return
    try {
      const res = await userApi.setStatus(u.id, willActivate, accessBusinessId)
      if (res.success) {
        showToast(`User ${u.name} is now ${willActivate ? 'Active' : 'Deactivated'}.`)
        loadUsers()
        loadAuditLogs()
      } else {
        showToast(res.message || `Could not ${verb} user.`, false)
      }
    } catch (e: any) {
      showToast(e.response?.data?.message || `Network error updating status.`, false)
    }
  }

  // ── Handler: Open Reassign ──
  const openReassignModal = (u: UserResponse) => {
    setReassignModalUser(u)
    setReassignRole(u.role)
    setReassignPrimaryBranch(u.branchId || '')
    setReassignBranchIds(u.assignedBranchIds || (u.branchId ? [u.branchId] : []))
    setReassignGroupIds(u.assignedGroupIds || [])
    setReassignRoleIds(u.assignedRoleIds || [])
    setReassignError('')
  }

  const handleSaveReassign = async () => {
    if (!reassignModalUser) return
    setReassignSaving(true)
    setReassignError('')
    try {
      const res = await userApi.reassign(
        reassignModalUser.id,
        {
          role: reassignRole,
          branchId: reassignPrimaryBranch ? reassignPrimaryBranch : null,
          groupIds: reassignGroupIds,
          roleIds: reassignRoleIds,
        },
        accessBusinessId
      )
      if (res.success) {
        // Also update multi-branches
        await userApi.assignBranches(reassignModalUser.id, reassignBranchIds, reassignPrimaryBranch || undefined, accessBusinessId)
        showToast(`Assignments updated for ${reassignModalUser.name}!`)
        setReassignModalUser(null)
        loadUsers()
        loadAccessConfig()
      } else {
        setReassignError(res.message || 'Failed to reassign user.')
      }
    } catch (e: any) {
      setReassignError(e.response?.data?.message || 'Network error while reassigning user.')
    } finally {
      setReassignSaving(false)
    }
  }

  // ── Handler: Open User Activity ──
  const openActivityModal = (u: UserResponse) => {
    setActivityModalUser(u)
    setActivityLoading(true)
    userApi.getActivity(u.id, accessBusinessId).then(res => {
      if (res.success && res.data) setUserActivities(res.data)
      else setUserActivities([])
    }).catch(() => setUserActivities([])).finally(() => setActivityLoading(false))
  }

  // ── Handler: Role & Matrix Operations ──
  const openCreateRole = () => {
    setEditingRoleId(null)
    setRoleDraft({
      name: '',
      description: '',
      allowedMenus: ['DASHBOARD', 'POS', 'ORDERS'],
      permissions: ['orders.view', 'orders.create', 'products.view', 'payments.view', 'payments.process']
    })
    setRoleError('')
    setShowRoleModal(true)
  }

  const openEditRole = (role: AccessRole) => {
    setEditingRoleId(role.id)
    setRoleDraft({
      name: role.name,
      description: role.description || '',
      allowedMenus: role.allowedMenus || [],
      permissions: role.permissions || []
    })
    setRoleError('')
    setShowRoleModal(true)
  }

  const applyRolePreset = (presetKey: string) => {
    const preset = STANDARD_ROLE_PRESETS[presetKey]
    if (!preset) return
    setRoleDraft(prev => ({
      ...prev,
      name: prev.name || preset.label,
      description: prev.description || preset.description,
      permissions: Array.from(new Set([...preset.permissions])),
      allowedMenus: Array.from(new Set([...preset.menus])),
    }))
  }

  const togglePermission = (code: string) => {
    setRoleDraft(prev => ({
      ...prev,
      permissions: toggleArrayItem(prev.permissions, code)
    }))
  }

  const toggleModulePermissions = (module: PermissionMatrixModule) => {
    const codes: string[] = [
      module.view?.code,
      module.create?.code,
      module.edit?.code,
      module.delete?.code,
      module.special?.code,
    ].filter(Boolean) as string[]

    const allSelected = codes.every(c => roleDraft.permissions.includes(c))
    setRoleDraft(prev => ({
      ...prev,
      permissions: allSelected
        ? prev.permissions.filter(p => !codes.includes(p))
        : Array.from(new Set([...prev.permissions, ...codes]))
    }))
  }

  const toggleVerbColumn = (verb: 'view' | 'create' | 'edit' | 'delete' | 'special') => {
    const codes = PERMISSION_MODULES.map(m => m[verb]?.code).filter(Boolean) as string[]
    const allSelected = codes.every(c => roleDraft.permissions.includes(c))
    setRoleDraft(prev => ({
      ...prev,
      permissions: allSelected
        ? prev.permissions.filter(p => !codes.includes(p))
        : Array.from(new Set([...prev.permissions, ...codes]))
    }))
  }

  const handleSaveRole = async () => {
    if (!roleDraft.name.trim()) {
      setRoleError('Role name is required.')
      return
    }
    if (roleDraft.permissions.length === 0) {
      setRoleError('Select at least one permission in the matrix.')
      return
    }
    setRoleSaving(true)
    setRoleError('')
    try {
      const payload = {
        name: roleDraft.name.trim(),
        description: roleDraft.description.trim(),
        allowedMenus: roleDraft.allowedMenus,
        permissions: roleDraft.permissions,
        isActive: true
      }
      const res = editingRoleId
        ? await accessApi.updateRole(editingRoleId, payload, accessBusinessId)
        : await accessApi.createRole(payload, accessBusinessId)

      if (res.success) {
        showToast(editingRoleId ? `Role ${roleDraft.name} updated!` : `Role ${roleDraft.name} created!`)
        setShowRoleModal(false)
        loadAccessConfig()
      } else {
        setRoleError(res.message || 'Could not save role.')
      }
    } catch (e: any) {
      setRoleError(e.response?.data?.message || 'Network error saving role.')
    } finally {
      setRoleSaving(false)
    }
  }

  const handleDeleteRole = async (role: AccessRole) => {
    if (!window.confirm(`Delete role "${role.name}"? This cannot be undone.`)) return
    try {
      const res = await accessApi.deleteRole(role.id, accessBusinessId)
      if (res.success) {
        showToast(`Role "${role.name}" deleted.`)
        loadAccessConfig()
      } else {
        showToast(res.message || 'Could not delete role.', false)
      }
    } catch (e: any) {
      showToast(e.response?.data?.message || 'Could not delete role.', false)
    }
  }

  // ── Handler: SuperAdmin Create Business & Admin ──
  const handleCreateBusinessAdmin = async () => {
    const { businessName, businessType, adminName, adminEmail, adminPhone, adminPassword } = adminForm
    if (!businessName || !businessType || !adminName || !adminEmail || !adminPhone || !adminPassword) {
      setAdminError('All fields are required.')
      return
    }
    setAdminSaving(true)
    setAdminError('')
    try {
      const res = await superAdminApi.createBusinessWithAdmin(adminForm)
      if (res.success) {
        setShowCreateAdmin(false)
        setAdminForm({ businessName: '', businessType: 'RETAIL', adminName: '', adminEmail: '', adminPhone: '', adminPassword: '' })
        loadBusinesses()
        showToast('Business & Admin created successfully!')
      } else {
        setAdminError(res.message || 'Failed to create business admin.')
      }
    } catch (e: any) {
      setAdminError(e.response?.data?.message || 'Network error.')
    } finally {
      setAdminSaving(false)
    }
  }

  // ── Filtered Audit Logs ──
  const filteredAuditLogs = useMemo(() => {
    return auditLogs.filter(log => {
      const q = auditSearch.toLowerCase().trim()
      const matchesSearch = !q ||
        (log.actorName && log.actorName.toLowerCase().includes(q)) ||
        (log.targetName && log.targetName.toLowerCase().includes(q)) ||
        log.action.toLowerCase().includes(q) ||
        (log.details && log.details.toLowerCase().includes(q)) ||
        (log.ipAddress && log.ipAddress.includes(q))

      const matchesAction = auditActionFilter === 'ALL' || log.action.toUpperCase().includes(auditActionFilter)

      let matchesDate = true
      if (auditDatePreset !== 'ALL') {
        const logDate = new Date(log.createdAt).getTime()
        const now = Date.now()
        if (auditDatePreset === 'TODAY') {
          const startOfToday = new Date().setHours(0, 0, 0, 0)
          matchesDate = logDate >= startOfToday
        } else if (auditDatePreset === '7D') {
          matchesDate = logDate >= (now - 7 * 86400000)
        } else if (auditDatePreset === '30D') {
          matchesDate = logDate >= (now - 30 * 86400000)
        }
      }

      return matchesSearch && matchesAction && matchesDate
    })
  }, [auditLogs, auditSearch, auditActionFilter, auditDatePreset])

  return (
    <div className="fade-in" style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
      {/* ── Global Alert Banner ── */}
      {globalMessage && (
        <div style={{
          display: 'flex', alignItems: 'center', gap: 10, padding: '12px 18px',
          borderRadius: 8, fontSize: 13, fontWeight: 600,
          background: globalMessage.ok ? 'rgba(16, 185, 129, 0.12)' : 'rgba(239, 68, 68, 0.12)',
          color: globalMessage.ok ? '#065f46' : '#991b1b',
          border: `1px solid ${globalMessage.ok ? 'rgba(16, 185, 129, 0.3)' : 'rgba(239, 68, 68, 0.3)'}`
        }}>
          {globalMessage.ok ? <CheckCircle2 size={16} /> : <AlertTriangle size={16} />}
          <span style={{ flex: 1 }}>{globalMessage.text}</span>
          <button type="button" onClick={() => setGlobalMessage(null)} style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'inherit' }}>
            <X size={15} />
          </button>
        </div>
      )}

      {/* ── SuperAdmin Multi-Tenant Switcher ── */}
      {isSuperAdmin && (
        <Card style={{ padding: '14px 18px', background: 'linear-gradient(135deg, #1e293b 0%, #0f172a 100%)', color: 'white' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 12 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
              <div style={{ width: 36, height: 36, borderRadius: 8, background: 'rgba(59, 130, 246, 0.2)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#60a5fa' }}>
                <Building2 size={20} />
              </div>
              <div>
                <div style={{ fontSize: 11, fontWeight: 700, color: '#94a3b8', textTransform: 'uppercase', letterSpacing: '0.05em' }}>Platform SuperAdmin</div>
                <div style={{ fontSize: 15, fontWeight: 700 }}>Tenant Data Isolation & Merchant Switcher</div>
              </div>
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 10, flexWrap: 'wrap' }}>
              <div style={{ minWidth: 260 }}>
                <select
                  value={selectedBusinessId}
                  onChange={e => setSelectedBusinessId(e.target.value)}
                  style={{
                    width: '100%', padding: '8px 12px', borderRadius: 8, fontSize: 13,
                    background: '#334155', color: 'white', border: '1px solid #475569', outline: 'none'
                  }}
                >
                  {businesses.map(b => (
                    <option key={b.id} value={b.id}>{b.name} ({b.type}) · {b.subscriptionTier}</option>
                  ))}
                </select>
              </div>
              <Btn variant="secondary" small onClick={() => setShowCreateAdmin(true)} icon={<Plus size={13} />}>
                Add Business & Admin
              </Btn>
            </div>
          </div>
        </Card>
      )}

      {/* ── Page Navigation Header & Tab Controls ── */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: 16 }}>
        <div>
          <h1 style={{ margin: 0, fontSize: 24, letterSpacing: '-0.5px' }}>Access Control & User Management</h1>
          <p style={{ margin: '4px 0 0', color: 'var(--b360-text-secondary)', fontSize: 13 }}>
            Manage staff accounts, multi-outlet assignments, granular permissions matrix, and system audit trail
          </p>
        </div>

        {/* Tab Switcher Pills */}
        <div style={{
          display: 'flex', padding: 4, background: 'var(--b360-surface)',
          border: '1px solid var(--b360-border)', borderRadius: 10, gap: 4
        }}>
          <button
            type="button"
            onClick={() => handleTabChange('users')}
            style={{
              display: 'flex', alignItems: 'center', gap: 7, padding: '7px 16px',
              borderRadius: 7, border: 'none', cursor: 'pointer', fontSize: 13, fontWeight: activeTab === 'users' ? 700 : 500,
              background: activeTab === 'users' ? 'var(--b360-blue)' : 'transparent',
              color: activeTab === 'users' ? '#ffffff' : 'var(--b360-text-secondary)',
              transition: 'all 0.15s ease'
            }}
          >
            <Users size={15} />
            <span>Users & Staff</span>
            <span style={{
              fontSize: 11, padding: '1px 6px', borderRadius: 10,
              background: activeTab === 'users' ? 'rgba(255,255,255,0.25)' : 'var(--b360-border)',
              color: activeTab === 'users' ? '#fff' : 'inherit'
            }}>
              {users.length}
            </span>
          </button>

          <button
            type="button"
            onClick={() => handleTabChange('roles')}
            style={{
              display: 'flex', alignItems: 'center', gap: 7, padding: '7px 16px',
              borderRadius: 7, border: 'none', cursor: 'pointer', fontSize: 13, fontWeight: activeTab === 'roles' ? 700 : 500,
              background: activeTab === 'roles' ? 'var(--b360-blue)' : 'transparent',
              color: activeTab === 'roles' ? '#ffffff' : 'var(--b360-text-secondary)',
              transition: 'all 0.15s ease'
            }}
          >
            <Shield size={15} />
            <span>Roles & Permissions</span>
            <span style={{
              fontSize: 11, padding: '1px 6px', borderRadius: 10,
              background: activeTab === 'roles' ? 'rgba(255,255,255,0.25)' : 'var(--b360-border)',
              color: activeTab === 'roles' ? '#fff' : 'inherit'
            }}>
              {accessConfig?.roles.length || 0}
            </span>
          </button>

          <button
            type="button"
            onClick={() => handleTabChange('audit')}
            style={{
              display: 'flex', alignItems: 'center', gap: 7, padding: '7px 16px',
              borderRadius: 7, border: 'none', cursor: 'pointer', fontSize: 13, fontWeight: activeTab === 'audit' ? 700 : 500,
              background: activeTab === 'audit' ? 'var(--b360-blue)' : 'transparent',
              color: activeTab === 'audit' ? '#ffffff' : 'var(--b360-text-secondary)',
              transition: 'all 0.15s ease'
            }}
          >
            <Activity size={15} />
            <span>Audit Trail</span>
          </button>
        </div>
      </div>

      {/* ═══════════════════════════════════════════════════════════════════════
          TAB 1: USERS DIRECTORY & LIFECYCLE
          ═══════════════════════════════════════════════════════════════════════ */}
      {activeTab === 'users' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          {/* KPI Statistics Overview */}
          <div className="responsive-grid responsive-grid-4" style={{ gap: 14 }}>
            <div style={{ background: 'white', borderRadius: 'var(--radius-md)', padding: '16px 20px', border: '1px solid var(--b360-border)', display: 'flex', alignItems: 'center', gap: 14 }}>
              <div style={{ background: 'rgba(59, 130, 246, 0.1)', borderRadius: '50%', width: 44, height: 44, display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--b360-blue)' }}>
                <Users size={20} />
              </div>
              <div>
                <div style={{ fontSize: 12, color: 'var(--b360-text-secondary)', fontWeight: 600 }}>Total Staff</div>
                <div style={{ fontSize: 22, fontWeight: 800 }}>{userStats.total}</div>
              </div>
            </div>

            <div style={{ background: 'white', borderRadius: 'var(--radius-md)', padding: '16px 20px', border: '1px solid var(--b360-border)', display: 'flex', alignItems: 'center', gap: 14 }}>
              <div style={{ background: 'rgba(16, 185, 129, 0.1)', borderRadius: '50%', width: 44, height: 44, display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--b360-green)' }}>
                <UserCheck size={20} />
              </div>
              <div>
                <div style={{ fontSize: 12, color: 'var(--b360-text-secondary)', fontWeight: 600 }}>Active Accounts</div>
                <div style={{ fontSize: 22, fontWeight: 800 }}>{userStats.active}</div>
              </div>
            </div>

            <div style={{ background: 'white', borderRadius: 'var(--radius-md)', padding: '16px 20px', border: '1px solid var(--b360-border)', display: 'flex', alignItems: 'center', gap: 14 }}>
              <div style={{ background: 'rgba(139, 92, 246, 0.1)', borderRadius: '50%', width: 44, height: 44, display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#8b5cf6' }}>
                <MapPin size={20} />
              </div>
              <div>
                <div style={{ fontSize: 12, color: 'var(--b360-text-secondary)', fontWeight: 600 }}>Multi-Branch Staff</div>
                <div style={{ fontSize: 22, fontWeight: 800 }}>{userStats.multiBranch}</div>
              </div>
            </div>

            <div style={{ background: 'white', borderRadius: 'var(--radius-md)', padding: '16px 20px', border: '1px solid var(--b360-border)', display: 'flex', alignItems: 'center', gap: 14 }}>
              <div style={{ background: userStats.locked > 0 ? 'rgba(239, 68, 68, 0.1)' : 'rgba(100, 116, 139, 0.1)', borderRadius: '50%', width: 44, height: 44, display: 'flex', alignItems: 'center', justifyContent: 'center', color: userStats.locked > 0 ? 'var(--b360-red)' : 'var(--b360-text-secondary)' }}>
                <Lock size={20} />
              </div>
              <div>
                <div style={{ fontSize: 12, color: 'var(--b360-text-secondary)', fontWeight: 600 }}>Locked Accounts</div>
                <div style={{ fontSize: 22, fontWeight: 800 }}>{userStats.locked}</div>
              </div>
            </div>
          </div>

          {/* Search, Filter Bar & Create Action */}
          <Card style={{ padding: '14px 18px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 12 }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 10, flex: 1, minWidth: 260, flexWrap: 'wrap' }}>
                <div style={{ position: 'relative', flex: 1, minWidth: 200, maxWidth: 360 }}>
                  <Search size={15} style={{ position: 'absolute', left: 12, top: '50%', transform: 'translateY(-50%)', color: 'var(--b360-text-secondary)' }} />
                  <input
                    type="text"
                    value={userSearch}
                    onChange={e => setUserSearch(e.target.value)}
                    placeholder="Search staff by name, email, phone, role..."
                    style={{
                      width: '100%', padding: '8px 12px 8px 36px', borderRadius: 8,
                      border: '1px solid var(--b360-border)', fontSize: 13, outline: 'none'
                    }}
                  />
                  {userSearch && (
                    <button
                      type="button"
                      onClick={() => setUserSearch('')}
                      style={{ position: 'absolute', right: 10, top: '50%', transform: 'translateY(-50%)', background: 'none', border: 'none', cursor: 'pointer', color: 'var(--b360-text-secondary)' }}
                    >
                      <X size={14} />
                    </button>
                  )}
                </div>

                {/* Filter by Branch */}
                <select
                  value={userBranchFilter}
                  onChange={e => setUserBranchFilter(e.target.value)}
                  style={{ padding: '8px 12px', borderRadius: 8, border: '1px solid var(--b360-border)', fontSize: 13, background: 'var(--b360-surface)' }}
                >
                  <option value="ALL">All Branches</option>
                  <option value="HEAD_OFFICE">Floating / Head Office Only</option>
                  {branches.map(b => (
                    <option key={b.id} value={b.id}>{b.name} ({b.code || 'Outlet'})</option>
                  ))}
                </select>

                {/* Filter by Status */}
                <select
                  value={userStatusFilter}
                  onChange={e => setUserStatusFilter(e.target.value as any)}
                  style={{ padding: '8px 12px', borderRadius: 8, border: '1px solid var(--b360-border)', fontSize: 13, background: 'var(--b360-surface)' }}
                >
                  <option value="ALL">All Statuses</option>
                  <option value="ACTIVE">Active Only</option>
                  <option value="DISABLED">Disabled Only</option>
                  <option value="LOCKED">Locked Accounts Only</option>
                </select>

                {/* Filter by Account Role */}
                <select
                  value={userRoleFilter}
                  onChange={e => setUserRoleFilter(e.target.value)}
                  style={{ padding: '8px 12px', borderRadius: 8, border: '1px solid var(--b360-border)', fontSize: 13, background: 'var(--b360-surface)' }}
                >
                  <option value="ALL">All Account Roles</option>
                  <option value="ADMIN">Admin</option>
                  <option value="MANAGER">Manager</option>
                  <option value="STAFF">Staff</option>
                </select>
              </div>

              <div style={{ display: 'flex', gap: 8 }}>
                <Btn variant="secondary" small onClick={() => { loadUsers(); loadAccessConfig(); }} icon={<RefreshCw size={13} />}>
                  Refresh
                </Btn>
                <Btn small onClick={() => { setShowAddUser(true); setAddUserError('') }} icon={<Plus size={14} />}>
                  Create User
                </Btn>
              </div>
            </div>
          </Card>

          {/* Users Table */}
          <Card style={{ padding: 0 }}>
            {usersLoading ? (
              <div style={{ padding: 36, textAlign: 'center', color: 'var(--b360-text-secondary)' }}>
                <RefreshCw size={24} className="spin" style={{ marginBottom: 10 }} />
                <div>Loading staff directory...</div>
              </div>
            ) : filteredUsers.length === 0 ? (
              <div style={{ padding: 40, textAlign: 'center', color: 'var(--b360-text-secondary)' }}>
                <Users size={36} style={{ opacity: 0.4, marginBottom: 12 }} />
                <div style={{ fontWeight: 600, fontSize: 14 }}>No staff accounts found</div>
                <div style={{ fontSize: 12, marginTop: 4 }}>Try clearing active filters or click "Create User" above.</div>
              </div>
            ) : (
              <DataTable
                headers={['Staff Member', 'Contact Info', 'Branch / Outlets', 'Role & Permissions', 'Status', 'Last Login', 'Quick Actions']}
                rows={filteredUsers.map(u => {
                  const status = (u.status || (u.isActive === false ? 'DISABLED' : (u.isPinLocked ? 'LOCKED' : 'ACTIVE'))).toUpperCase()
                  const isLocked = status === 'LOCKED' || u.isPinLocked

                  return [
                    // Col 1: Staff Member
                    <div key="name" style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                      <div style={{
                        width: 34, height: 34, borderRadius: '50%',
                        background: 'linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%)',
                        color: 'white', display: 'flex', alignItems: 'center', justifyContent: 'center',
                        fontWeight: 700, fontSize: 13
                      }}>
                        {u.name.slice(0, 2).toUpperCase()}
                      </div>
                      <div>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                          <span style={{ fontWeight: 700, fontSize: 13 }}>{u.name}</span>
                          {u.id === currentUser?.id && (
                            <span style={{ fontSize: 10, background: 'rgba(59, 130, 246, 0.1)', color: '#1d4ed8', padding: '1px 6px', borderRadius: 8, fontWeight: 700 }}>
                              You
                            </span>
                          )}
                        </div>
                        <div style={{ fontSize: 11, color: 'var(--b360-text-secondary)', display: 'flex', alignItems: 'center', gap: 4 }}>
                          {u.hasPinSet ? <span style={{ color: '#059669', display: 'flex', alignItems: 'center', gap: 2 }}><Key size={10} /> PIN configured</span> : <span>No PIN</span>}
                        </div>
                      </div>
                    </div>,

                    // Col 2: Contact
                    <div key="contact" style={{ fontSize: 12 }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 5 }}>
                        <Mail size={12} color="var(--b360-text-secondary)" />
                        <span>{u.email}</span>
                      </div>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 5, color: 'var(--b360-text-secondary)', marginTop: 2 }}>
                        <Phone size={12} />
                        <span>{u.phone || '—'}</span>
                      </div>
                    </div>,

                    // Col 3: Branch / Outlets
                    <div key="branch" style={{ fontSize: 12 }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
                        <MapPin size={12} color={u.branchId ? '#059669' : '#64748b'} />
                        <span style={{ fontWeight: 600 }}>{u.branchName || (u.branchId ? 'Assigned' : 'All Branches (Floating)')}</span>
                      </div>
                      {u.assignedBranchNames && u.assignedBranchNames.length > 1 && (
                        <div style={{ marginTop: 3 }}>
                          <span style={{
                            fontSize: 10, background: 'rgba(139, 92, 246, 0.1)', color: '#7c3aed',
                            padding: '1px 6px', borderRadius: 6, fontWeight: 600
                          }} title={`Multi-outlets: ${u.assignedBranchNames.join(', ')}`}>
                            +{u.assignedBranchNames.length - 1} additional outlets
                          </span>
                        </div>
                      )}
                    </div>,

                    // Col 4: Role & Permissions
                    <div key="role" style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                        <span style={{
                          fontSize: 11, fontWeight: 700, padding: '2px 8px', borderRadius: 6,
                          background: u.role === 'ADMIN' ? 'rgba(99, 102, 241, 0.12)' : u.role === 'MANAGER' ? 'rgba(14, 165, 233, 0.12)' : 'rgba(100, 116, 139, 0.12)',
                          color: u.role === 'ADMIN' ? '#4f46e5' : u.role === 'MANAGER' ? '#0284c7' : '#475569',
                          border: `1px solid ${u.role === 'ADMIN' ? 'rgba(99, 102, 241, 0.25)' : u.role === 'MANAGER' ? 'rgba(14, 165, 233, 0.25)' : 'rgba(100, 116, 139, 0.25)'}`
                        }}>
                          {u.role}
                        </span>
                        {u.permissions && u.permissions.length > 0 && (
                          <span style={{ fontSize: 10, color: 'var(--b360-text-secondary)', fontWeight: 600 }}>
                            {u.permissions.length} perms
                          </span>
                        )}
                      </div>
                      {u.assignedGroups && u.assignedGroups.length > 0 && (
                        <div style={{ fontSize: 11, color: 'var(--b360-blue)' }}>
                          {u.assignedGroups.join(', ')}
                        </div>
                      )}
                    </div>,

                    // Col 5: Status
                    <div key="status">
                      {isLocked ? (
                        <span style={{
                          display: 'inline-flex', alignItems: 'center', gap: 4,
                          padding: '3px 8px', borderRadius: 6, fontSize: 11, fontWeight: 700,
                          background: '#fef3c7', color: '#b45309', border: '1px solid #fde68a'
                        }}>
                          <Lock size={11} /> Locked
                        </span>
                      ) : (
                        <StatusBadge status={status === 'ACTIVE' ? 'ACTIVE' : 'INACTIVE'} />
                      )}
                    </div>,

                    // Col 6: Last Login
                    <div key="lastLogin" style={{ fontSize: 12 }}>
                      <div style={{ fontWeight: 500, color: u.lastLoginAt ? 'inherit' : 'var(--b360-text-secondary)' }}>
                        {formatRelativeTime(u.lastLoginAt)}
                      </div>
                      {u.lastLoginAt && (
                        <div style={{ fontSize: 10, color: 'var(--b360-text-secondary)' }}>
                          {new Date(u.lastLoginAt).toLocaleTimeString('en-KE', { hour: '2-digit', minute: '2-digit' })}
                        </div>
                      )}
                    </div>,

                    // Col 7: Actions Menu
                    <div key="actions" style={{ display: 'flex', alignItems: 'center', gap: 6, flexWrap: 'wrap' }}>
                      {/* Unlock Action if Locked */}
                      {isLocked && (
                        <Btn
                          small
                          variant="primary"
                          onClick={() => handleUnlockUser(u)}
                          disabled={unlockingUserId === u.id}
                          icon={<Unlock size={12} />}
                          style={{ background: '#d97706', borderColor: '#b45309' }}
                        >
                          {unlockingUserId === u.id ? 'Unlocking…' : 'Unlock'}
                        </Btn>
                      )}

                      {/* Edit User */}
                      <Btn
                        small
                        variant="secondary"
                        onClick={() => openEditModal(u)}
                        title="Edit profile & outlet"
                        icon={<Edit2 size={12} />}
                      >
                        Edit
                      </Btn>

                      {/* Reassign Roles & Branches */}
                      <Btn
                        small
                        variant="secondary"
                        onClick={() => openReassignModal(u)}
                        title="Reassign role level & branches"
                        icon={<ArrowRightLeft size={12} />}
                      >
                        Reassign
                      </Btn>

                      {/* Reset Password */}
                      <Btn
                        small
                        variant="secondary"
                        onClick={() => { setResetPassUser(u); setNewPassword(''); setConfirmPassword(''); setResetPassError('') }}
                        title="Reset user password"
                        icon={<Key size={12} />}
                      >
                        Pass
                      </Btn>

                      {/* Staff PIN */}
                      <Btn
                        small
                        variant="secondary"
                        onClick={() => { setPinModalUser(u); setPinValue(''); setPinError('') }}
                        title="Set 6-digit staff PIN"
                        icon={<Lock size={12} />}
                      >
                        PIN
                      </Btn>

                      {/* User Activity */}
                      <Btn
                        small
                        variant="secondary"
                        onClick={() => openActivityModal(u)}
                        title="View audit activity"
                        icon={<Activity size={12} />}
                      />

                      {/* Activate / Deactivate */}
                      {u.id !== currentUser?.id && (
                        <Btn
                          small
                          variant={u.isActive === false || u.status === 'DISABLED' ? 'secondary' : 'danger'}
                          onClick={() => handleToggleStatus(u)}
                          title={u.isActive === false ? 'Enable user' : 'Disable user'}
                        >
                          {u.isActive === false || u.status === 'DISABLED' ? 'Enable' : 'Disable'}
                        </Btn>
                      )}
                    </div>
                  ]
                })}
              />
            )}
          </Card>
        </div>
      )}

      {/* ═══════════════════════════════════════════════════════════════════════
          TAB 2: ROLES & GRANULAR PERMISSIONS MATRIX
          ═══════════════════════════════════════════════════════════════════════ */}
      {activeTab === 'roles' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 12 }}>
            <div>
              <h2 style={{ margin: 0, fontSize: 18 }}>Roles & Permission Sets</h2>
              <p style={{ margin: '3px 0 0', color: 'var(--b360-text-secondary)', fontSize: 13 }}>
                Avoid hard-coding access by role name. Define granular permissions and compose roles with customized privileges.
              </p>
            </div>
            <Btn onClick={openCreateRole} icon={<Plus size={14} />}>
              Create Custom Role
            </Btn>
          </div>

          {/* Configured Roles List */}
          <div className="responsive-grid responsive-grid-3" style={{ gap: 14 }}>
            {accessConfig?.roles.map(role => {
              const permCount = role.permissions?.length || 0
              return (
                <Card key={role.id} style={{ padding: 18, display: 'flex', flexDirection: 'column', gap: 10, opacity: role.isActive ? 1 : 0.65 }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 10 }}>
                    <div>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                        <span style={{ fontWeight: 700, fontSize: 15 }}>{role.name}</span>
                        <span style={{
                          fontSize: 10, padding: '1px 6px', borderRadius: 8, fontWeight: 700,
                          background: role.isActive ? 'rgba(16, 185, 129, 0.1)' : 'rgba(100, 116, 139, 0.1)',
                          color: role.isActive ? '#059669' : '#64748b'
                        }}>
                          {role.isActive ? 'ACTIVE' : 'DISABLED'}
                        </span>
                      </div>
                      <div style={{ fontSize: 12, color: 'var(--b360-text-secondary)', marginTop: 4, minHeight: 34 }}>
                        {role.description || 'No description provided.'}
                      </div>
                    </div>
                  </div>

                  <div style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 12, borderTop: '1px solid var(--b360-border)', paddingTop: 10 }}>
                    <ShieldCheck size={14} color="var(--b360-blue)" />
                    <span style={{ fontWeight: 600 }}>{permCount} Granular Permissions</span>
                    <span style={{ color: 'var(--b360-text-secondary)', marginLeft: 'auto' }}>
                      {role.allowedMenus?.length || 0} menus
                    </span>
                  </div>

                  <div style={{ display: 'flex', gap: 6, marginTop: 'auto', paddingTop: 6 }}>
                    <Btn small variant="secondary" onClick={() => openEditRole(role)} icon={<Sliders size={13} />}>
                      Configure Permissions
                    </Btn>
                    <Btn small variant="danger" onClick={() => handleDeleteRole(role)} icon={<X size={13} />}>
                      Delete
                    </Btn>
                  </div>
                </Card>
              )
            })}
          </div>

          {/* Standard Role Presets Info Card */}
          <Card style={{ padding: 20 }}>
            <h3 style={{ margin: '0 0 8px', fontSize: 15 }}>Biashara360 Standard Roles Framework</h3>
            <p style={{ fontSize: 12, color: 'var(--b360-text-secondary)', margin: '0 0 14px', lineHeight: 1.5 }}>
              The system includes 9 battle-tested standard role archetypes. You can use these presets directly or clone them to customize permission scopes:
            </p>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(260px, 1fr))', gap: 10 }}>
              {Object.entries(STANDARD_ROLE_PRESETS).map(([key, preset]) => (
                <div key={key} style={{
                  padding: 12, borderRadius: 8, border: '1px solid var(--b360-border)',
                  background: 'var(--b360-surface)', display: 'flex', flexDirection: 'column', gap: 6
                }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <span style={{ fontWeight: 700, fontSize: 13 }}>{preset.label}</span>
                    <span style={{ fontSize: 10, background: 'rgba(59, 130, 246, 0.1)', color: '#1d4ed8', padding: '1px 6px', borderRadius: 8, fontWeight: 700 }}>
                      {preset.permissions.length} perms
                    </span>
                  </div>
                  <div style={{ fontSize: 11, color: 'var(--b360-text-secondary)' }}>{preset.description}</div>
                  <button
                    type="button"
                    onClick={() => {
                      openCreateRole()
                      setTimeout(() => applyRolePreset(key), 50)
                    }}
                    style={{
                      alignSelf: 'flex-start', marginTop: 4, background: 'none', border: 'none',
                      color: 'var(--b360-blue)', fontSize: 11, fontWeight: 600, cursor: 'pointer', padding: 0
                    }}
                  >
                    Apply as template →
                  </button>
                </div>
              ))}
            </div>
          </Card>
        </div>
      )}

      {/* ═══════════════════════════════════════════════════════════════════════
          TAB 3: SYSTEM AUDIT TRAIL
          ═══════════════════════════════════════════════════════════════════════ */}
      {activeTab === 'audit' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          <Card style={{ padding: '14px 18px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 12 }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 10, flex: 1, minWidth: 260, flexWrap: 'wrap' }}>
                <div style={{ position: 'relative', flex: 1, minWidth: 200, maxWidth: 360 }}>
                  <Search size={15} style={{ position: 'absolute', left: 12, top: '50%', transform: 'translateY(-50%)', color: 'var(--b360-text-secondary)' }} />
                  <input
                    type="text"
                    value={auditSearch}
                    onChange={e => setAuditSearch(e.target.value)}
                    placeholder="Search audit trail by actor, action, IP, target..."
                    style={{
                      width: '100%', padding: '8px 12px 8px 36px', borderRadius: 8,
                      border: '1px solid var(--b360-border)', fontSize: 13, outline: 'none'
                    }}
                  />
                  {auditSearch && (
                    <button
                      type="button"
                      onClick={() => setAuditSearch('')}
                      style={{ position: 'absolute', right: 10, top: '50%', transform: 'translateY(-50%)', background: 'none', border: 'none', cursor: 'pointer', color: 'var(--b360-text-secondary)' }}
                    >
                      <X size={14} />
                    </button>
                  )}
                </div>

                {/* Filter by Category / Verb */}
                <select
                  value={auditActionFilter}
                  onChange={e => setAuditActionFilter(e.target.value)}
                  style={{ padding: '8px 12px', borderRadius: 8, border: '1px solid var(--b360-border)', fontSize: 13, background: 'var(--b360-surface)' }}
                >
                  <option value="ALL">All Event Types</option>
                  <option value="AUTH">Authentication & Sign-in</option>
                  <option value="USER">User & Profile Updates</option>
                  <option value="ROLE">Role & Permissions</option>
                  <option value="LOCK">Lock / Unlock Events</option>
                  <option value="ORDER">Orders & Refunds</option>
                  <option value="INVENTORY">Inventory Adjustments</option>
                  <option value="PAYMENT">Payments & Reversals</option>
                </select>

                {/* Filter by Date Range */}
                <select
                  value={auditDatePreset}
                  onChange={e => setAuditDatePreset(e.target.value as any)}
                  style={{ padding: '8px 12px', borderRadius: 8, border: '1px solid var(--b360-border)', fontSize: 13, background: 'var(--b360-surface)' }}
                >
                  <option value="ALL">All Time</option>
                  <option value="TODAY">Today Only</option>
                  <option value="7D">Last 7 Days</option>
                  <option value="30D">Last 30 Days</option>
                </select>
              </div>

              <div style={{ display: 'flex', gap: 8 }}>
                <Btn variant="secondary" small onClick={loadAuditLogs} icon={<RefreshCw size={13} />}>
                  Live Refresh
                </Btn>
                <Btn variant="secondary" small onClick={() => navigate('/audit-logs')} icon={<FileText size={13} />}>
                  Detailed Audit Explorer
                </Btn>
              </div>
            </div>
          </Card>

          {/* Audit Logs Table */}
          <Card style={{ padding: 0 }}>
            {auditLoading ? (
              <div style={{ padding: 36, textAlign: 'center', color: 'var(--b360-text-secondary)' }}>
                <RefreshCw size={24} className="spin" style={{ marginBottom: 10 }} />
                <div>Loading audit records...</div>
              </div>
            ) : filteredAuditLogs.length === 0 ? (
              <div style={{ padding: 40, textAlign: 'center', color: 'var(--b360-text-secondary)' }}>
                <Activity size={36} style={{ opacity: 0.4, marginBottom: 12 }} />
                <div style={{ fontWeight: 600, fontSize: 14 }}>No audit records match the selected filter</div>
              </div>
            ) : (
              <DataTable
                headers={['Timestamp', 'Action', 'Actor', 'Target User / Resource', 'IP Address', 'Details']}
                rows={filteredAuditLogs.map(log => [
                  // Timestamp
                  <div key="time" style={{ fontSize: 12 }}>
                    <div style={{ fontWeight: 600 }}>{new Date(log.createdAt).toLocaleDateString('en-KE')}</div>
                    <div style={{ color: 'var(--b360-text-secondary)', fontSize: 11 }}>
                      {new Date(log.createdAt).toLocaleTimeString('en-KE', { hour: '2-digit', minute: '2-digit', second: '2-digit' })}
                    </div>
                  </div>,

                  // Action Badge
                  <div key="action">
                    <span style={{
                      display: 'inline-flex', alignItems: 'center', gap: 4,
                      padding: '2px 8px', borderRadius: 6, fontSize: 11, fontWeight: 700,
                      background: log.action.includes('LOCK') ? '#fef3c7' : log.action.includes('UNLOCK') || log.action.includes('LOGIN') ? '#ecfdf5' : 'rgba(59, 130, 246, 0.1)',
                      color: log.action.includes('LOCK') ? '#b45309' : log.action.includes('UNLOCK') || log.action.includes('LOGIN') ? '#047857' : '#1d4ed8',
                      border: `1px solid ${log.action.includes('LOCK') ? '#fde68a' : log.action.includes('UNLOCK') ? '#a7f3d0' : 'rgba(59, 130, 246, 0.25)'}`
                    }}>
                      {log.action.replace(/_/g, ' ')}
                    </span>
                  </div>,

                  // Actor
                  <div key="actor" style={{ fontSize: 12, fontWeight: 500 }}>
                    {log.actorName || 'System'}
                  </div>,

                  // Target
                  <div key="target" style={{ fontSize: 12 }}>
                    <span style={{ fontWeight: 600 }}>{log.targetName || '—'}</span>
                    {log.resourceType && (
                      <div style={{ fontSize: 10, color: 'var(--b360-text-secondary)' }}>
                        {log.resourceType} {log.resourceId ? `#${log.resourceId.slice(0, 8)}` : ''}
                      </div>
                    )}
                  </div>,

                  // IP
                  <div key="ip" style={{ fontSize: 11, color: 'var(--b360-text-secondary)', fontFamily: 'monospace' }}>
                    {log.ipAddress || '—'}
                  </div>,

                  // Details
                  <div key="details" style={{ fontSize: 12, maxWidth: 360, color: '#334155' }}>
                    {log.details || '—'}
                  </div>
                ])}
              />
            )}
          </Card>
        </div>
      )}

      {/* ═══════════════════════════════════════════════════════════════════════
          MODAL: CREATE NEW USER
          ═══════════════════════════════════════════════════════════════════════ */}
      {showAddUser && (
        <Modal
          title="Create Staff Account"
          wide
          onClose={() => setShowAddUser(false)}
          footer={
            <>
              <Btn variant="secondary" onClick={() => setShowAddUser(false)}>Cancel</Btn>
              <Btn onClick={handleCreateUser} disabled={addUserSaving}>
                {addUserSaving ? 'Creating…' : 'Create User'}
              </Btn>
            </>
          }
        >
          <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
            {addUserError && (
              <div style={{ padding: '8px 12px', background: 'rgba(239, 68, 68, 0.1)', color: 'var(--b360-red)', borderRadius: 6, fontSize: 13 }}>
                {addUserError}
              </div>
            )}

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
              <Input
                label="Full Name *"
                value={addUserForm.name}
                onChange={v => setAddUserForm(p => ({ ...p, name: v }))}
                placeholder="e.g. Grace Wanjiku"
              />
              <Input
                label="Email Address *"
                value={addUserForm.email}
                onChange={v => setAddUserForm(p => ({ ...p, email: v }))}
                placeholder="grace@example.com"
                type="email"
              />
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
              <Input
                label="Phone Number *"
                value={addUserForm.phone}
                onChange={v => setAddUserForm(p => ({ ...p, phone: v }))}
                placeholder="+254 7XX XXX XXX"
              />
              <Input
                label="Temporary Password *"
                value={addUserForm.password || ''}
                onChange={v => setAddUserForm(p => ({ ...p, password: v }))}
                type="password"
                placeholder="Min 6 characters"
              />
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
              <Select
                label="Account Level Role *"
                value={addUserForm.role || 'STAFF'}
                onChange={v => setAddUserForm(p => ({ ...p, role: v }))}
                options={[
                  { value: 'STAFF', label: 'Staff (Operational pos & floor)' },
                  { value: 'MANAGER', label: 'Manager (Store supervisor)' },
                  { value: 'ADMIN', label: 'Admin (Business administrator)' },
                ]}
              />

              <Select
                label="Primary Branch / Outlet"
                value={addUserForm.branchId || ''}
                onChange={v => setAddUserForm(p => ({ ...p, branchId: v }))}
                options={[
                  { value: '', label: 'All Branches (Floating / Head Office)' },
                  ...branches.map(b => ({
                    value: b.id,
                    label: `${b.name} (${b.code || 'No Code'})${b.isHeadOffice ? ' · HQ' : ''}`
                  }))
                ]}
              />
            </div>

            {/* Multi-Branch Outlets Assignment */}
            {branches.length > 0 && (
              <div>
                <label style={{ fontSize: 12, fontWeight: 700, display: 'block', marginBottom: 6 }}>
                  Authorized Outlet Access (Multi-Branch Support)
                </label>
                <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8, padding: 10, background: 'var(--b360-surface)', borderRadius: 8, border: '1px solid var(--b360-border)' }}>
                  {branches.map(b => {
                    const isChecked = addUserBranchIds.includes(b.id)
                    return (
                      <label key={b.id} style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: 12, cursor: 'pointer', padding: '4px 8px', borderRadius: 6, background: isChecked ? 'rgba(59, 130, 246, 0.1)' : 'transparent' }}>
                        <input
                          type="checkbox"
                          checked={isChecked}
                          onChange={() => setAddUserBranchIds(prev => toggleArrayItem(prev, b.id))}
                        />
                        <span>{b.name} ({b.code || 'Outlet'})</span>
                      </label>
                    )
                  })}
                </div>
              </div>
            )}

            {/* Direct Roles Selection */}
            {accessConfig?.roles && accessConfig.roles.length > 0 && (
              <div>
                <label style={{ fontSize: 12, fontWeight: 700, display: 'block', marginBottom: 6 }}>
                  Assign Direct RBAC Access Roles (Optional)
                </label>
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, minmax(0, 1fr))', gap: 6, maxHeight: 130, overflowY: 'auto', padding: 8, border: '1px solid var(--b360-border)', borderRadius: 8 }}>
                  {accessConfig.roles.filter(r => r.isActive).map(r => {
                    const isChecked = addUserRoleIds.includes(r.id)
                    return (
                      <label key={r.id} style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 12, cursor: 'pointer' }}>
                        <input
                          type="checkbox"
                          checked={isChecked}
                          onChange={() => setAddUserRoleIds(prev => toggleArrayItem(prev, r.id))}
                        />
                        <span><strong>{r.name}</strong> ({r.permissions?.length || 0} rights)</span>
                      </label>
                    )
                  })}
                </div>
              </div>
            )}
          </div>
        </Modal>
      )}

      {/* ═══════════════════════════════════════════════════════════════════════
          MODAL: EDIT USER & OUTLET ASSIGNMENT
          ═══════════════════════════════════════════════════════════════════════ */}
      {editModalUser && (
        <Modal
          title={`Edit User · ${editModalUser.name}`}
          onClose={() => setEditModalUser(null)}
          footer={
            <>
              <Btn variant="secondary" onClick={() => setEditModalUser(null)}>Cancel</Btn>
              <Btn onClick={handleSaveEditUser} disabled={editSaving}>
                {editSaving ? 'Saving…' : 'Save Changes'}
              </Btn>
            </>
          }
        >
          <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
            {editError && (
              <div style={{ padding: '8px 12px', background: 'rgba(239, 68, 68, 0.1)', color: 'var(--b360-red)', borderRadius: 6, fontSize: 13 }}>
                {editError}
              </div>
            )}

            <Input
              label="Full Name *"
              value={editForm.name}
              onChange={v => setEditForm(p => ({ ...p, name: v }))}
            />

            <Input
              label="Email Address *"
              value={editForm.email}
              onChange={v => setEditForm(p => ({ ...p, email: v }))}
              type="email"
            />

            <Input
              label="Phone Number *"
              value={editForm.phone}
              onChange={v => setEditForm(p => ({ ...p, phone: v }))}
            />

            <Select
              label="Primary Home Branch"
              value={editForm.branchId || ''}
              onChange={v => setEditForm(p => ({ ...p, branchId: v }))}
              options={[
                { value: '', label: 'All Branches (Floating / Head Office)' },
                ...branches.map(b => ({
                  value: b.id,
                  label: `${b.name} (${b.code || 'No Code'})${b.isHeadOffice ? ' · HQ' : ''}`
                }))
              ]}
            />

            {/* Multi-Branch Outlets Checkbox List */}
            <div>
              <label style={{ fontSize: 12, fontWeight: 700, display: 'block', marginBottom: 6 }}>
                Authorized Outlets (Multi-Branch Access)
              </label>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 6, maxHeight: 150, overflowY: 'auto', padding: 8, border: '1px solid var(--b360-border)', borderRadius: 8 }}>
                {branches.map(b => {
                  const isChecked = editBranchIds.includes(b.id)
                  return (
                    <label key={b.id} style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 12, cursor: 'pointer' }}>
                      <input
                        type="checkbox"
                        checked={isChecked}
                        onChange={() => setEditBranchIds(prev => toggleArrayItem(prev, b.id))}
                      />
                      <span>{b.name} ({b.code || 'Outlet'}){b.isHeadOffice ? ' · HQ' : ''}</span>
                    </label>
                  )
                })}
              </div>
            </div>
          </div>
        </Modal>
      )}

      {/* ═══════════════════════════════════════════════════════════════════════
          MODAL: RESET USER PASSWORD
          ═══════════════════════════════════════════════════════════════════════ */}
      {resetPassUser && (
        <Modal
          title={`Reset Password · ${resetPassUser.name}`}
          onClose={() => setResetPassUser(null)}
          footer={
            <>
              <Btn variant="secondary" onClick={() => setResetPassUser(null)}>Cancel</Btn>
              <Btn onClick={handleSaveResetPassword} disabled={resetPassSaving}>
                {resetPassSaving ? 'Resetting…' : 'Reset Password'}
              </Btn>
            </>
          }
        >
          <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
            {resetPassError && (
              <div style={{ padding: '8px 12px', background: 'rgba(239, 68, 68, 0.1)', color: 'var(--b360-red)', borderRadius: 6, fontSize: 13 }}>
                {resetPassError}
              </div>
            )}
            <p style={{ fontSize: 13, color: 'var(--b360-text-secondary)', margin: 0 }}>
              Set a temporary or new password for <strong>{resetPassUser.name}</strong> ({resetPassUser.email}).
            </p>
            <Input
              label="New Password *"
              type="password"
              value={newPassword}
              onChange={setNewPassword}
              placeholder="Minimum 6 characters"
            />
            <Input
              label="Confirm New Password *"
              type="password"
              value={confirmPassword}
              onChange={setConfirmPassword}
              placeholder="Re-enter new password"
            />
          </div>
        </Modal>
      )}

      {/* ═══════════════════════════════════════════════════════════════════════
          MODAL: STAFF 6-DIGIT PIN
          ═══════════════════════════════════════════════════════════════════════ */}
      {pinModalUser && (
        <Modal
          title={`Staff POS PIN · ${pinModalUser.name}`}
          onClose={() => setPinModalUser(null)}
          footer={
            <>
              {pinModalUser.hasPinSet && (
                <Btn variant="danger" onClick={() => handleSavePin(true)} disabled={pinSaving}>
                  Remove PIN
                </Btn>
              )}
              <div style={{ marginLeft: 'auto', display: 'flex', gap: 8 }}>
                <Btn variant="secondary" onClick={() => setPinModalUser(null)}>Cancel</Btn>
                <Btn onClick={() => handleSavePin(false)} disabled={pinSaving || pinValue.length !== 6}>
                  {pinSaving ? 'Saving…' : 'Save PIN'}
                </Btn>
              </div>
            </>
          }
        >
          <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
            {pinError && (
              <div style={{ padding: '8px 12px', background: 'rgba(239, 68, 68, 0.1)', color: 'var(--b360-red)', borderRadius: 6, fontSize: 13 }}>
                {pinError}
              </div>
            )}
            <div style={{ fontSize: 13, color: 'var(--b360-text-secondary)' }}>
              Configure a 6-digit fast sign-in PIN for <strong>{pinModalUser.name}</strong> for rapid POS cashier access.
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 8, padding: 16, background: 'var(--b360-surface)', borderRadius: 8 }}>
              <label style={{ fontSize: 12, fontWeight: 700 }}>Enter 6-Digit Numeric PIN</label>
              <input
                type="password"
                maxLength={6}
                value={pinValue}
                onChange={e => setPinValue(e.target.value.replace(/\D/g, '').slice(0, 6))}
                placeholder="••••••"
                style={{
                  fontSize: 24, letterSpacing: '0.3em', textAlign: 'center',
                  padding: '8px 16px', borderRadius: 8, border: '2px solid var(--b360-blue)',
                  width: 180, outline: 'none'
                }}
              />
              <div style={{ fontSize: 11, color: 'var(--b360-text-secondary)' }}>
                {pinValue.length}/6 digits entered
              </div>
            </div>
          </div>
        </Modal>
      )}

      {/* ═══════════════════════════════════════════════════════════════════════
          MODAL: REASSIGN ROLES, GROUPS & BRANCHES
          ═══════════════════════════════════════════════════════════════════════ */}
      {reassignModalUser && (
        <Modal
          title={`Reassign Roles & Access · ${reassignModalUser.name}`}
          wide
          onClose={() => setReassignModalUser(null)}
          footer={
            <>
              <Btn variant="secondary" onClick={() => setReassignModalUser(null)}>Cancel</Btn>
              <Btn onClick={handleSaveReassign} disabled={reassignSaving}>
                {reassignSaving ? 'Saving…' : 'Save Assignments'}
              </Btn>
            </>
          }
        >
          <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
            {reassignError && (
              <div style={{ padding: '8px 12px', background: 'rgba(239, 68, 68, 0.1)', color: 'var(--b360-red)', borderRadius: 6, fontSize: 13 }}>
                {reassignError}
              </div>
            )}

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
              <Select
                label="Account Level Role *"
                value={reassignRole}
                onChange={setReassignRole}
                options={[
                  { value: 'STAFF', label: 'Staff' },
                  { value: 'MANAGER', label: 'Manager' },
                  { value: 'ADMIN', label: 'Admin' },
                ]}
                disabled={reassignModalUser.id === currentUser?.id}
              />

              <Select
                label="Primary Branch / Outlet"
                value={reassignPrimaryBranch}
                onChange={setReassignPrimaryBranch}
                options={[
                  { value: '', label: 'All Branches (Floating / Head Office)' },
                  ...branches.map(b => ({
                    value: b.id,
                    label: `${b.name} (${b.code || 'No Code'})${b.isHeadOffice ? ' · HQ' : ''}`
                  }))
                ]}
              />
            </div>

            {/* Outlets Multi-Selection */}
            <div>
              <label style={{ fontSize: 12, fontWeight: 700, display: 'block', marginBottom: 6 }}>
                Multi-Branch Outlets Authorization
              </label>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, minmax(0, 1fr))', gap: 6, maxHeight: 130, overflowY: 'auto', padding: 8, border: '1px solid var(--b360-border)', borderRadius: 8 }}>
                {branches.map(b => {
                  const isChecked = reassignBranchIds.includes(b.id)
                  return (
                    <label key={b.id} style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 12, cursor: 'pointer' }}>
                      <input
                        type="checkbox"
                        checked={isChecked}
                        onChange={() => setReassignBranchIds(prev => toggleArrayItem(prev, b.id))}
                      />
                      <span>{b.name} ({b.code || 'Outlet'})</span>
                    </label>
                  )
                })}
              </div>
            </div>

            {/* Custom Roles Assignment */}
            {accessConfig?.roles && accessConfig.roles.length > 0 && (
              <div>
                <label style={{ fontSize: 12, fontWeight: 700, display: 'block', marginBottom: 6 }}>
                  Direct Access Roles & Matrix Rights
                </label>
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, minmax(0, 1fr))', gap: 6, maxHeight: 130, overflowY: 'auto', padding: 8, border: '1px solid var(--b360-border)', borderRadius: 8 }}>
                  {accessConfig.roles.filter(r => r.isActive).map(r => {
                    const isChecked = reassignRoleIds.includes(r.id)
                    return (
                      <label key={r.id} style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 12, cursor: 'pointer' }}>
                        <input
                          type="checkbox"
                          checked={isChecked}
                          onChange={() => setReassignRoleIds(prev => toggleArrayItem(prev, r.id))}
                        />
                        <span><strong>{r.name}</strong> ({r.permissions?.length || 0} permissions)</span>
                      </label>
                    )
                  })}
                </div>
              </div>
            )}
          </div>
        </Modal>
      )}

      {/* ═══════════════════════════════════════════════════════════════════════
          MODAL: USER AUDIT ACTIVITY
          ═══════════════════════════════════════════════════════════════════════ */}
      {activityModalUser && (
        <Modal
          title={`User Activity Trail · ${activityModalUser.name}`}
          wide
          onClose={() => setActivityModalUser(null)}
          footer={<Btn onClick={() => setActivityModalUser(null)}>Close</Btn>}
        >
          {activityLoading ? (
            <div style={{ padding: 32, textAlign: 'center', color: 'var(--b360-text-secondary)' }}>
              Loading user activity history…
            </div>
          ) : userActivities.length === 0 ? (
            <div style={{ padding: 32, textAlign: 'center', color: 'var(--b360-text-secondary)' }}>
              No recorded activity events for this user.
            </div>
          ) : (
            <DataTable
              headers={['When', 'Action', 'Target', 'IP Address', 'Details']}
              rows={userActivities.map(log => [
                new Date(log.createdAt).toLocaleString('en-KE'),
                <span key="action" style={{
                  fontSize: 11, fontWeight: 700, padding: '2px 6px', borderRadius: 4,
                  background: 'rgba(59, 130, 246, 0.1)', color: '#1d4ed8'
                }}>
                  {log.action.replace(/_/g, ' ')}
                </span>,
                log.targetName || '—',
                <span key="ip" style={{ fontFamily: 'monospace', fontSize: 11 }}>{log.ipAddress || '—'}</span>,
                log.details || '—'
              ])}
            />
          )}
        </Modal>
      )}

      {/* ═══════════════════════════════════════════════════════════════════════
          MODAL: ROLE CONFIGURATION & PERMISSION MATRIX
          ═══════════════════════════════════════════════════════════════════════ */}
      {showRoleModal && (
        <Modal
          title={editingRoleId ? `Configure Role · ${roleDraft.name}` : 'Create Custom RBAC Role'}
          wide
          onClose={() => setShowRoleModal(false)}
          footer={
            <>
              <div style={{ marginRight: 'auto', fontSize: 12, fontWeight: 600, color: 'var(--b360-text-secondary)' }}>
                {roleDraft.permissions.length} granular permissions selected
              </div>
              <Btn variant="secondary" onClick={() => setShowRoleModal(false)}>Cancel</Btn>
              <Btn onClick={handleSaveRole} disabled={roleSaving}>
                {roleSaving ? 'Saving…' : (editingRoleId ? 'Save Role' : 'Create Role')}
              </Btn>
            </>
          }
        >
          <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            {roleError && (
              <div style={{ padding: '8px 12px', background: 'rgba(239, 68, 68, 0.1)', color: 'var(--b360-red)', borderRadius: 6, fontSize: 13 }}>
                {roleError}
              </div>
            )}

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 2fr', gap: 12 }}>
              <Input
                label="Role Name *"
                value={roleDraft.name}
                onChange={v => setRoleDraft(p => ({ ...p, name: v }))}
                placeholder="e.g. Senior Cashier, Floor Supervisor"
              />
              <Input
                label="Description"
                value={roleDraft.description}
                onChange={v => setRoleDraft(p => ({ ...p, description: v }))}
                placeholder="e.g. Front desk POS sales, refunds authorization and end of day"
              />
            </div>

            {/* Quick Template Presets */}
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
              <span style={{ fontSize: 12, fontWeight: 700, color: 'var(--b360-text-secondary)' }}>Apply Archetype Preset:</span>
              {Object.entries(STANDARD_ROLE_PRESETS).map(([key, preset]) => (
                <button
                  key={key}
                  type="button"
                  onClick={() => applyRolePreset(key)}
                  style={{
                    padding: '3px 8px', borderRadius: 6, fontSize: 11, fontWeight: 600,
                    border: '1px solid var(--b360-border)', background: 'var(--b360-surface)',
                    cursor: 'pointer', transition: 'all 0.15s ease'
                  }}
                >
                  {preset.label}
                </button>
              ))}
            </div>

            {/* ── Granular Permission Matrix Table ── */}
            <div>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
                <span style={{ fontSize: 13, fontWeight: 700 }}>Granular Permission Matrix</span>
                <div style={{ display: 'flex', gap: 8 }}>
                  <button
                    type="button"
                    onClick={() => {
                      const allCodes = PERMISSION_MODULES.flatMap(m => [
                        m.view?.code, m.create?.code, m.edit?.code, m.delete?.code, m.special?.code
                      ]).filter(Boolean) as string[]
                      setRoleDraft(p => ({ ...p, permissions: Array.from(new Set(allCodes)) }))
                    }}
                    style={{ fontSize: 11, background: 'none', border: 'none', color: 'var(--b360-blue)', cursor: 'pointer', fontWeight: 600 }}
                  >
                    Select All
                  </button>
                  <span style={{ color: 'var(--b360-border)' }}>|</span>
                  <button
                    type="button"
                    onClick={() => setRoleDraft(p => ({ ...p, permissions: [] }))}
                    style={{ fontSize: 11, background: 'none', border: 'none', color: 'var(--b360-red)', cursor: 'pointer', fontWeight: 600 }}
                  >
                    Clear All
                  </button>
                </div>
              </div>

              <div style={{ overflowX: 'auto', border: '1px solid var(--b360-border)', borderRadius: 8 }}>
                <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 12 }}>
                  <thead>
                    <tr style={{ background: '#f8fafc', borderBottom: '1px solid var(--b360-border)', textAlign: 'left' }}>
                      <th style={{ padding: '10px 14px', width: 220 }}>Module / Feature</th>
                      <th style={{ padding: '10px 10px', textAlign: 'center' }}>
                        <div>View (Read)</div>
                        <button type="button" onClick={() => toggleVerbColumn('view')} style={{ fontSize: 10, color: 'var(--b360-blue)', background: 'none', border: 'none', cursor: 'pointer' }}>Toggle</button>
                      </th>
                      <th style={{ padding: '10px 10px', textAlign: 'center' }}>
                        <div>Create (Add)</div>
                        <button type="button" onClick={() => toggleVerbColumn('create')} style={{ fontSize: 10, color: 'var(--b360-blue)', background: 'none', border: 'none', cursor: 'pointer' }}>Toggle</button>
                      </th>
                      <th style={{ padding: '10px 10px', textAlign: 'center' }}>
                        <div>Edit (Update)</div>
                        <button type="button" onClick={() => toggleVerbColumn('edit')} style={{ fontSize: 10, color: 'var(--b360-blue)', background: 'none', border: 'none', cursor: 'pointer' }}>Toggle</button>
                      </th>
                      <th style={{ padding: '10px 10px', textAlign: 'center' }}>
                        <div>Delete (Void)</div>
                        <button type="button" onClick={() => toggleVerbColumn('delete')} style={{ fontSize: 10, color: 'var(--b360-blue)', background: 'none', border: 'none', cursor: 'pointer' }}>Toggle</button>
                      </th>
                      <th style={{ padding: '10px 10px', textAlign: 'center' }}>
                        <div>Approve / Adjust</div>
                        <button type="button" onClick={() => toggleVerbColumn('special')} style={{ fontSize: 10, color: 'var(--b360-blue)', background: 'none', border: 'none', cursor: 'pointer' }}>Toggle</button>
                      </th>
                      <th style={{ padding: '10px 10px', textAlign: 'center', width: 80 }}>Row</th>
                    </tr>
                  </thead>
                  <tbody>
                    {PERMISSION_MODULES.map(module => {
                      const renderCell = (perm?: { code: string; label: string }) => {
                        if (!perm) return <td style={{ padding: 8, textAlign: 'center', color: '#cbd5e1' }}>—</td>
                        const isChecked = roleDraft.permissions.includes(perm.code)
                        return (
                          <td key={perm.code} style={{ padding: 8, textAlign: 'center' }}>
                            <label style={{ display: 'inline-flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer' }} title={perm.label}>
                              <input
                                type="checkbox"
                                checked={isChecked}
                                onChange={() => togglePermission(perm.code)}
                                style={{ width: 16, height: 16, cursor: 'pointer' }}
                              />
                            </label>
                          </td>
                        )
                      }

                      return (
                        <tr key={module.key} style={{ borderBottom: '1px solid var(--b360-border)' }}>
                          <td style={{ padding: '10px 14px' }}>
                            <div style={{ fontWeight: 700 }}>{module.label}</div>
                            <div style={{ fontSize: 11, color: 'var(--b360-text-secondary)', marginTop: 2 }}>{module.description}</div>
                          </td>
                          {renderCell(module.view)}
                          {renderCell(module.create)}
                          {renderCell(module.edit)}
                          {renderCell(module.delete)}
                          {renderCell(module.special)}
                          <td style={{ padding: 8, textAlign: 'center' }}>
                            <button
                              type="button"
                              onClick={() => toggleModulePermissions(module)}
                              style={{ fontSize: 11, color: 'var(--b360-blue)', background: 'none', border: 'none', cursor: 'pointer', fontWeight: 600 }}
                            >
                              Toggle
                            </button>
                          </td>
                        </tr>
                      )
                    })}
                  </tbody>
                </table>
              </div>
            </div>

            {/* Allowed Menus Selection */}
            {accessConfig?.menus && (
              <div>
                <label style={{ fontSize: 12, fontWeight: 700, display: 'block', marginBottom: 6 }}>
                  Visible Application Menus
                </label>
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, minmax(0, 1fr))', gap: 6, padding: 10, border: '1px solid var(--b360-border)', borderRadius: 8, background: 'var(--b360-surface)' }}>
                  {accessConfig.menus.map(menu => {
                    const isChecked = roleDraft.allowedMenus.includes(menu.key)
                    return (
                      <label key={menu.key} style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: 12, cursor: 'pointer' }}>
                        <input
                          type="checkbox"
                          checked={isChecked}
                          onChange={() => setRoleDraft(p => ({ ...p, allowedMenus: toggleArrayItem(p.allowedMenus, menu.key) }))}
                        />
                        <span>{menu.label}</span>
                      </label>
                    )
                  })}
                </div>
              </div>
            )}
          </div>
        </Modal>
      )}

      {/* ═══════════════════════════════════════════════════════════════════════
          MODAL: SUPERADMIN CREATE BUSINESS & ADMIN
          ═══════════════════════════════════════════════════════════════════════ */}
      {showCreateAdmin && (
        <Modal
          title="Create New Business & Admin"
          onClose={() => setShowCreateAdmin(false)}
          footer={
            <>
              <Btn variant="secondary" onClick={() => setShowCreateAdmin(false)}>Cancel</Btn>
              <Btn onClick={handleCreateBusinessAdmin} disabled={adminSaving}>
                {adminSaving ? 'Creating…' : 'Create Business'}
              </Btn>
            </>
          }
        >
          <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
            {adminError && (
              <div style={{ padding: '8px 12px', background: 'rgba(239, 68, 68, 0.1)', color: 'var(--b360-red)', borderRadius: 6, fontSize: 13 }}>
                {adminError}
              </div>
            )}
            <Input
              label="Business Name *"
              value={adminForm.businessName}
              onChange={v => setAdminForm(p => ({ ...p, businessName: v }))}
              placeholder="e.g. Kamau Supplies & Retail"
            />
            <Input
              label="Business Type *"
              value={adminForm.businessType}
              onChange={v => setAdminForm(p => ({ ...p, businessType: v }))}
              placeholder="e.g. Retail, Grocery, Hospitality"
            />
            <Input
              label="Admin Full Name *"
              value={adminForm.adminName}
              onChange={v => setAdminForm(p => ({ ...p, adminName: v }))}
              placeholder="e.g. Jane Mwangi"
            />
            <Input
              label="Admin Email Address *"
              value={adminForm.adminEmail}
              onChange={v => setAdminForm(p => ({ ...p, adminEmail: v }))}
              type="email"
              placeholder="jane@example.com"
            />
            <Input
              label="Admin Phone Number *"
              value={adminForm.adminPhone}
              onChange={v => setAdminForm(p => ({ ...p, adminPhone: v }))}
              placeholder="+254 7XX XXX XXX"
            />
            <Input
              label="Admin Initial Password *"
              value={adminForm.adminPassword}
              onChange={v => setAdminForm(p => ({ ...p, adminPassword: v }))}
              type="password"
              placeholder="Min 6 characters"
            />
          </div>
        </Modal>
      )}
    </div>
  )
}

export default UserCreationPage

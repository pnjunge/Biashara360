import React, { useState, useEffect } from 'react'
import { useSearchParams, useNavigate } from 'react-router-dom'
import {
  Building2, Shield, Wifi, CreditCard, Lock, Bell, CheckCircle, AlertTriangle,
  Receipt, Save, ExternalLink, Zap, Key, RefreshCw, Layers, ImagePlus, Trash2,
  Settings as SettingsIcon, Users, MessageSquare, FileText, Clock, ChevronDown,
  ChevronRight, ShieldCheck, Mail, Send, GitBranch, MapPin, Plus, Edit2, Check, Store
} from 'lucide-react'
import { Card, Btn, Input, Select, Modal, DataTable, StatusBadge, KpiCard } from '../components/ui'
import {
  settingsApi, businessApi, kraApi, authApi, hospitalityApi, servicesApi, adminApi, branchApi,
  BusinessProfileRequest, MpesaConfigResponse, SessionTimeoutConfig, BranchRequest, BranchResponse
} from '../services/api'
import { useAuth } from '../App'

type SettingsTab = 'general' | 'storefront' | 'cybersource' | 'kra' | 'mpesa' | 'security' | 'notifications' | 'branches'
type SecuritySection = 'authentication' | 'session' | 'access'
type SettingsNavItem = { label: string; tab?: SettingsTab; path?: string; security?: SecuritySection }
type SettingsNavGroup = { label: string; icon: React.ReactNode; items: SettingsNavItem[] }

const Section = ({ title, children }: { title: string; children: React.ReactNode }) => (
  <Card style={{ padding: 22, marginBottom: 16 }}>
    <h3 style={{ fontWeight: 700, marginBottom: 16, fontSize: 15 }}>{title}</h3>
    <div style={{ borderTop: '1px solid var(--b360-border)', paddingTop: 16, display: 'flex', flexDirection: 'column', gap: 14 }}>{children}</div>
  </Card>
)

const Toggle = ({ label, checked, onChange, disabled = false }: { label: string; checked: boolean; onChange: (v: boolean) => void; disabled?: boolean }) => (
  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
    <span style={{ fontSize: 13 }}>{label}</span>
    <button type="button" role="switch" aria-checked={checked} aria-label={label} disabled={disabled} onClick={() => onChange(!checked)} style={{
      width: 44, height: 24, borderRadius: 12, cursor: disabled ? 'not-allowed' : 'pointer', transition: 'background 0.2s', border:0, padding:0,
      background: checked ? 'var(--b360-green)' : '#D1D5DB', position: 'relative'
    }}>
      <div style={{ position: 'absolute', top: 2, left: checked ? 22 : 2, width: 20, height: 20, borderRadius: '50%', background: 'white', transition: 'left 0.2s', boxShadow: '0 1px 3px rgba(0,0,0,0.2)' }} />
    </button>
  </div>
)

export function SettingsPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const navigate = useNavigate()
  const { user } = useAuth()
  const isMerchantAdmin = (user?.role || '').toUpperCase() === 'ADMIN'
  const isSuperAdmin = (user?.role || '').toUpperCase() === 'SUPERADMIN'

  const initialTab = (searchParams.get('tab') as SettingsTab) || 'security'
  const [activeTab, setActiveTab] = useState<SettingsTab>(initialTab)
  const [securitySection, setSecuritySection] = useState<SecuritySection>('authentication')

  useEffect(() => {
    const tabFromUrl = searchParams.get('tab') as SettingsTab
    if (tabFromUrl && tabFromUrl !== activeTab) {
      setActiveTab(tabFromUrl)
    }
  }, [searchParams])

  const handleTabChange = (tab: SettingsTab) => {
    setActiveTab(tab)
    setSearchParams({ tab })
  }

  // ── 1. General Business Profile & Receipt State ──────────────────────────────
  const [profile, setProfile] = useState<BusinessProfileRequest>({
    name: '', owner: '', phone: '', email: '', type: '', county: '', address: '',
    kraPin: '', paybillNumber: '', accountNumber: ''
  })
  const [receiptHeader, setReceiptHeader] = useState('Thank you for shopping with us!')
  const [receiptFooter, setReceiptFooter] = useState('Goods once sold are not returnable.')
  const [storefrontSlug, setStorefrontSlug] = useState('')
  const [profileLoading, setProfileLoading] = useState(false)
  const [profileSaving, setProfileSaving] = useState(false)
  const [profileMsg, setProfileMsg] = useState<{ ok: boolean; text: string } | null>(null)
  const [servicesEnabled, setServicesEnabled] = useState(false)
  const [servicesSaving, setServicesSaving] = useState(false)
  const [hospitalityEnabled, setHospitalityEnabled] = useState(false)
  const [hospitalitySaving, setHospitalitySaving] = useState(false)

  // ── 2. CyberSource Configuration State ───────────────────────────────────────
  const [csMerchantId, setCsMerchantId] = useState('')
  const [csMerchantKeyId, setCsMerchantKeyId] = useState('')
  const [csMerchantSecretKey, setCsMerchantSecretKey] = useState('')
  const [csProfileId, setCsProfileId] = useState('')
  const [csAccessKey, setCsAccessKey] = useState('')
  const [csIsSandbox, setCsIsSandbox] = useState(true)
  const [csLoading, setCsLoading] = useState(false)
  const [csSaving, setCsSaving] = useState(false)
  const [csMsg, setCsMsg] = useState<{ ok: boolean; text: string } | null>(null)

  // ── 3. KRA & eTIMS Setup State ───────────────────────────────────────────────
  const [kraPin, setKraPin] = useState('')
  const [kraCompanyName, setKraCompanyName] = useState('')
  const [kraVatNo, setKraVatNo] = useState('')
  const [kraSdcId, setKraSdcId] = useState('')
  const [kraSerialNo, setKraSerialNo] = useState('')
  const [kraEnv, setKraEnv] = useState<'sandbox' | 'production'>('sandbox')
  const [kraSaving, setKraSaving] = useState(false)
  const [kraMsg, setKraMsg] = useState<{ ok: boolean; text: string } | null>(null)

  // ── 4. M-Pesa Setup State ──────────────────────────────────────────────────
  const [mpAccountType, setMpAccountType] = useState('paybill')
  const [mpShortCode, setMpShortCode] = useState('')
  const [mpPassKey, setMpPassKey] = useState('')
  const [mpPasskeyConfigured, setMpPasskeyConfigured] = useState(false)
  const [mpEnvironment, setMpEnvironment] = useState('sandbox')
  const [mpCallbackUrl, setMpCallbackUrl] = useState('')
  const [mpChannels, setMpChannels] = useState<MpesaConfigResponse[]>([])
  const [mpLoading, setMpLoading] = useState(false)
  const [mpSaving, setMpSaving] = useState(false)
  const [mpMsg, setMpMsg] = useState<{ ok: boolean; text: string } | null>(null)

  // ── 5. Security & Session Timeouts State ────────────────────────────────────
  const [sessionTimeouts, setSessionTimeouts] = useState<SessionTimeoutConfig>({
    businessId: '',
    webTimeoutSeconds: 1800,
    androidTimeoutSeconds: 3600,
    desktopTimeoutSeconds: 7200
  })
  const [twoFA, setTwoFA] = useState(true)
  const [secSaving, setSecSaving] = useState(false)
  const [secMsg, setSecMsg] = useState<{ ok: boolean; text: string } | null>(null)
  const [pinPassword, setPinPassword] = useState('')
  const [loginPin, setLoginPin] = useState('')
  const [confirmLoginPin, setConfirmLoginPin] = useState('')
  const [pinSaving, setPinSaving] = useState(false)

  // ── 6. Notifications State ─────────────────────────────────────────────────
  const [smsAlerts, setSmsAlerts] = useState(true)
  const [emailAlerts, setEmailAlerts] = useState(false)
  const [subscriptionTier, setSubscriptionTier] = useState('FREEMIUM')
  const [subscriptionEnabled, setSubscriptionEnabled] = useState(true)

  // ── 7. Outlook 365 / SMTP Email State ─────────────────────────────────────
  const [emailStatus, setEmailStatus] = useState<{
    configured: boolean
    host: string
    port: number
    username: string
    fromEmail: string
    fromName: string
  } | null>(null)
  const [testRecipient, setTestRecipient] = useState(user?.email || '')
  const [sendingTest, setSendingTest] = useState(false)
  const [testResult, setTestResult] = useState<{ success: boolean; message: string } | null>(null)
  const [editingSmtp, setEditingSmtp] = useState(false)
  const [smtpForm, setSmtpForm] = useState({
    host: 'smtp.gmail.com',
    port: 587,
    username: '',
    password: '',
    fromEmail: '',
    fromName: 'Biashara360'
  })
  const [savingSmtp, setSavingSmtp] = useState(false)

  useEffect(() => {
    if (activeTab === 'notifications' && isSuperAdmin) {
      adminApi.getEmailStatus()
        .then((res: any) => {
          if (res?.data) {
            setEmailStatus(res.data)
            setSmtpForm(prev => ({
              ...prev,
              host: res.data.host || 'smtp.gmail.com',
              port: res.data.port || 587,
              username: res.data.username || '',
              fromEmail: res.data.fromEmail || '',
              fromName: res.data.fromName || 'Biashara360'
            }))
          }
        })
        .catch(console.error)
    }
  }, [activeTab, isSuperAdmin])

  const handleSaveSmtp = async () => {
    setSavingSmtp(true)
    setTestResult(null)
    try {
      const res = await adminApi.updateEmailSettings(smtpForm)
      if (res.data) {
        setEmailStatus(res.data)
        setEditingSmtp(false)
        setTestResult({ success: true, message: 'SMTP settings updated successfully!' })
      }
    } catch (err: any) {
      setTestResult({ success: false, message: err?.response?.data?.message || 'Failed to update SMTP settings' })
    } finally {
      setSavingSmtp(false)
    }
  }

  const handleSendTestEmail = async () => {
    if (!testRecipient.trim()) return
    setSendingTest(true)
    setTestResult(null)
    try {
      const res = await adminApi.sendTestEmail(testRecipient.trim())
      setTestResult({ success: res.success, message: res.message || 'Test email sent!' })
    } catch (err: any) {
      setTestResult({ success: false, message: err?.response?.data?.message || 'Failed to send test email' })
    } finally {
      setSendingTest(false)
    }
  }

  // Load Tab-Specific Data
  useEffect(() => {
    if (activeTab === 'general' || activeTab === 'storefront' || activeTab === 'notifications') {
      setProfileLoading(true)
      businessApi.getProfile().then(res => {
        if (res.success && res.data) {
          const d = res.data
          setStorefrontSlug(d.storefrontSlug)
          setSubscriptionTier(d.subscriptionTier || 'FREEMIUM')
          setSubscriptionEnabled(d.subscriptionEnabled !== false)
          setHospitalityEnabled(d.hospitalityEnabled === true)
          setServicesEnabled(d.servicesEnabled === true)
          setProfile({
            name: d.name || '', owner: d.owner || '', phone: d.phone || '',
            email: d.email || '', type: d.type || '', county: d.county || '',
            address: d.address || '', kraPin: d.kraPin || '',
            paybillNumber: d.paybillNumber || '', accountNumber: d.accountNumber || '',
            receiptHeader: d.receiptHeader, receiptFooter: d.receiptFooter,
            receiptLogo: d.receiptLogo, receiptLogoWidthMm: d.receiptLogoWidthMm, receiptLogoHeightMm: d.receiptLogoHeightMm, receiptShowTax: d.receiptShowTax,
            receiptShowCustomer: d.receiptShowCustomer,
            storefrontThemeColor: d.storefrontThemeColor || '#0F766E',
            storefrontHeadline: d.storefrontHeadline || 'Shop with us online',
            storefrontDescription: d.storefrontDescription || '',
            storefrontBannerUrl: d.storefrontBannerUrl || null,
            storefrontLayout: d.storefrontLayout || 'GRID',
            dayStartTime: d.dayStartTime || '06:00', dayCloseTime: d.dayCloseTime || '23:00'
          })
          setReceiptHeader(d.receiptHeader || 'Welcome to our store!')
          setReceiptFooter(d.receiptFooter || 'Thank you for shopping with us!')
          if (d.kraPin) setKraPin(d.kraPin)
          if (d.name) setKraCompanyName(d.name)
        }
      }).catch(() => {}).finally(() => setProfileLoading(false))
    } else if (activeTab === 'cybersource') {
      setCsLoading(true)
      settingsApi.getCyberSource().then(res => {
        if (res.success && res.data) {
          setCsMerchantId(res.data.merchantId || '')
          setCsMerchantKeyId(res.data.merchantKeyId || '')
          setCsProfileId(res.data.profileId || '')
          setCsAccessKey(res.data.accessKey || '')
          setCsIsSandbox(res.data.environment === 'sandbox')
        }
      }).catch(() => {}).finally(() => setCsLoading(false))
    } else if (activeTab === 'mpesa') {
      setMpLoading(true)
      settingsApi.getMpesaChannels().then(res => {
        if (res.success && res.data) {
          setMpChannels(res.data)
        }
      }).catch(() => {}).finally(() => setMpLoading(false))
    } else if (activeTab === 'security') {
      settingsApi.getSessionTimeouts().then(res => {
        if (res.success && res.data) setSessionTimeouts(res.data)
      }).catch(() => {})
    }
  }, [activeTab])

  useEffect(() => {
    const config = mpChannels.find(channel => channel.accountType === mpAccountType)
    setMpShortCode(config?.shortCode || '')
    setMpCallbackUrl(config?.callbackUrl || '')
    setMpPasskeyConfigured(config?.passkeyConfigured || false)
    setMpEnvironment(config?.environment || 'sandbox')
    setMpPassKey('')
  }, [mpAccountType, mpChannels])

  // ── Save Handlers ──────────────────────────────────────────────────────────

  const handleSaveGeneral = async () => {
    setProfileSaving(true)
    setProfileMsg(null)
    try {
      const res = await businessApi.updateProfile({ ...profile, receiptHeader, receiptFooter })
      setProfileMsg({ ok: res.success, text: res.message || (res.success ? 'Business profile updated' : 'Failed to save profile') })
    } catch (e: any) {
      setProfileMsg({ ok: false, text: e.response?.data?.message || 'Network error' })
    } finally {
      setProfileSaving(false)
    }
  }

  const handleReceiptLogo = (file?: File) => {
    if (!file) return
    if (!['image/png', 'image/jpeg', 'image/webp'].includes(file.type)) {
      setProfileMsg({ ok: false, text: 'Receipt logo must be a PNG, JPEG, or WebP image.' })
      return
    }
    if (file.size > 500 * 1024) {
      setProfileMsg({ ok: false, text: 'Receipt logo must be smaller than 500 KB.' })
      return
    }
    const reader = new FileReader()
    reader.onload = () => {
      setProfile(current => ({ ...current, receiptLogo: String(reader.result) }))
      setProfileMsg(null)
    }
    reader.readAsDataURL(file)
  }

  const handleServicesToggle = async (enabled: boolean) => {
    if (!isMerchantAdmin) return
    setServicesSaving(true)
    setProfileMsg(null)
    try {
      const res = await servicesApi.setEnabled(enabled)
      if (!res.success) throw new Error(res.message || 'Could not update Appointments & Services')
      setServicesEnabled(enabled)
      window.dispatchEvent(new CustomEvent('services-mode-changed', { detail: { enabled } }))
      setProfileMsg({ ok: true, text: `Appointments & Services ${enabled ? 'enabled' : 'disabled'}.` })
    } catch (e: any) {
      setProfileMsg({ ok: false, text: e.response?.data?.message || e.message || 'Could not update Appointments & Services.' })
    } finally { setServicesSaving(false) }
  }

  const handleHospitalityToggle = async (enabled: boolean) => {
    if (!isMerchantAdmin) return
    if (!enabled && !window.confirm('Disable hospitality mode? Settle all open tabs and complete active kitchen or bar tickets first.')) return
    setHospitalitySaving(true)
    setProfileMsg(null)
    try {
      const res = await hospitalityApi.setEnabled(enabled)
      if (!res.success) throw new Error(res.message || 'Could not update hospitality mode')
      setHospitalityEnabled(enabled)
      window.dispatchEvent(new CustomEvent('hospitality-mode-changed', { detail: { enabled } }))
      setProfileMsg({ ok: true, text: `Hospitality mode ${enabled ? 'enabled' : 'disabled'}.` })
    } catch (e: any) {
      setProfileMsg({ ok: false, text: e.response?.data?.message || e.message || 'Could not update hospitality mode.' })
    } finally {
      setHospitalitySaving(false)
    }
  }

  const handleSaveCyberSource = async () => {
    setCsSaving(true)
    setCsMsg(null)
    try {
      const res = await settingsApi.updateCyberSource({
        merchantId: csMerchantId,
        merchantKeyId: csMerchantKeyId,
        profileId: csProfileId,
        accessKey: csAccessKey,
        ...(csMerchantSecretKey.trim() ? { merchantSecretKey: csMerchantSecretKey.trim() } : {}),
        environment: csIsSandbox ? 'sandbox' : 'production'
      })
      if (res.success) {
        setCsMsg({ ok: true, text: 'CyberSource configuration updated successfully' })
        setCsMerchantSecretKey('')
      } else {
        setCsMsg({ ok: false, text: res.message || 'Failed to save CyberSource settings' })
      }
    } catch (e: any) {
      setCsMsg({ ok: false, text: e.response?.data?.message || 'Network error' })
    } finally {
      setCsSaving(false)
    }
  }

  const handleSaveKra = async () => {
    setKraSaving(true)
    setKraMsg(null)
    try {
      const res = await kraApi.saveProfile({
        pin: kraPin,
        companyName: kraCompanyName,
        vatRegistrationNumber: kraVatNo,
        sdcId: kraSdcId,
        serialNumber: kraSerialNo,
        environment: kraEnv
      })
      if (res.success) {
        setKraMsg({ ok: true, text: 'KRA iTax profile and eTIMS device saved successfully' })
      } else {
        setKraMsg({ ok: false, text: res.message || 'Failed to save KRA settings' })
      }
    } catch (e: any) {
      setKraMsg({ ok: false, text: e.response?.data?.message || 'Network error' })
    } finally {
      setKraSaving(false)
    }
  }

  const handleSaveMpesa = async () => {
    setMpSaving(true)
    setMpMsg(null)
    try {
      const res = await settingsApi.updateMpesa({
        shortCode: mpShortCode,
        ...(mpPassKey.trim() ? { passKey: mpPassKey.trim() } : {}),
        environment: mpEnvironment,
        accountType: mpAccountType,
        callbackUrl: mpCallbackUrl
      })
      if (res.success) {
        if (res.data) {
          setMpChannels(prev => [
            ...prev.filter(c => c.accountType !== mpAccountType),
            res.data as MpesaConfigResponse
          ])
          setMpPasskeyConfigured(res.data.passkeyConfigured)
          setMpPassKey('')
        }
        setMpMsg({ ok: true, text: 'M-Pesa Daraja channel updated' })
      } else {
        setMpMsg({ ok: false, text: res.message || 'Failed to save M-Pesa config' })
      }
    } catch (e: any) {
      setMpMsg({ ok: false, text: e.response?.data?.message || 'Network error' })
    } finally {
      setMpSaving(false)
    }
  }

  const handleSaveSecurity = async () => {
    setSecSaving(true)
    setSecMsg(null)
    try {
      const res = await settingsApi.updateSessionTimeouts(sessionTimeouts)
      if (res.success && res.data) {
        window.dispatchEvent(new CustomEvent('session-timeout-updated', { detail: res.data.webTimeoutSeconds }))
      }
      setSecMsg({ ok: res.success, text: res.message || (res.success ? 'Security policy saved' : 'Failed to save security policy') })
    } catch (e: any) {
      setSecMsg({ ok: false, text: e.response?.data?.message || 'Network error' })
    } finally {
      setSecSaving(false)
    }
  }

  const handleLoginPin = async (disable = false) => {
    if (!pinPassword) return setSecMsg({ok:false,text:'Enter your current password.'})
    if (!disable && (!/^\d{6}$/.test(loginPin) || loginPin !== confirmLoginPin)) return setSecMsg({ok:false,text:'Enter matching 6-digit PINs.'})
    setPinSaving(true); setSecMsg(null)
    try {
      const res = await authApi.setLoginPin({currentPassword:pinPassword,pin:disable ? undefined : loginPin,disable})
      setSecMsg({ok:res.success,text:res.message || (disable ? 'PIN login disabled' : 'PIN login enabled')})
      if (res.success) { setPinPassword(''); setLoginPin(''); setConfirmLoginPin('') }
    } catch (e:any) { setSecMsg({ok:false,text:e.response?.data?.message || 'Could not update PIN login.'}) }
    finally { setPinSaving(false) }
  }

  const [changeCurrPass, setChangeCurrPass] = useState('')
  const [changeNewPass, setChangeNewPass] = useState('')
  const [changeConfirmPass, setChangeConfirmPass] = useState('')
  const [passSaving, setPassSaving] = useState(false)

  const handleChangePassword = async () => {
    if (!changeCurrPass) return setSecMsg({ ok: false, text: 'Enter your current password.' })
    if (!changeNewPass || changeNewPass.length < 8) return setSecMsg({ ok: false, text: 'New password must be at least 8 characters.' })
    if (!/^(?=.*[a-z])(?=.*\d)(?=.*[@$!%*?&_\-+=.])[A-Za-z\d@$!%*?&_\-+=.]{8,}$/.test(changeNewPass)) {
      return setSecMsg({ ok: false, text: 'Password must be at least 8 characters with a letter, a number, and a special character (@, $, !, %, *, ?, &, _, -, +, =, .).' })
    }
    if (changeNewPass !== changeConfirmPass) return setSecMsg({ ok: false, text: 'New passwords do not match.' })
    if (changeCurrPass === changeNewPass) return setSecMsg({ ok: false, text: 'New password must be different from current password.' })
    setPassSaving(true)
    setSecMsg(null)
    try {
      const res = await authApi.changePassword({ currentPassword: changeCurrPass, newPassword: changeNewPass })
      setSecMsg({ ok: res.success, text: res.message || 'Password updated successfully!' })
      if (res.success) {
        setChangeCurrPass('')
        setChangeNewPass('')
        setChangeConfirmPass('')
      }
    } catch (e: any) {
      const errorMsg = e.response?.data?.error?.details?.fields?.[0]?.message
        || e.response?.data?.error?.message
        || e.response?.data?.message
        || 'Could not update password.'
      setSecMsg({ ok: false, text: errorMsg })
    } finally {
      setPassSaving(false)
    }
  }

  // ── 8. Branches & Outlets State ──────────────────────────────────────────
  const [branches, setBranches] = useState<BranchResponse[]>([])
  const [branchesLoading, setBranchesLoading] = useState(false)
  const [branchModalOpen, setBranchModalOpen] = useState(false)
  const [editingBranch, setEditingBranch] = useState<BranchResponse | null>(null)
  const [branchForm, setBranchForm] = useState<BranchRequest>({
    name: '',
    code: '',
    phone: '',
    email: '',
    address: '',
    city: '',
    county: '',
    isHeadOffice: false,
    receiptHeader: '',
    receiptFooter: ''
  })
  const [branchSaving, setBranchSaving] = useState(false)
  const [branchMsg, setBranchMsg] = useState<{ ok: boolean; text: string } | null>(null)

  const loadBranches = async () => {
    setBranchesLoading(true)
    try {
      const res = await branchApi.getAll(true)
      if (res.success && res.data) {
        setBranches(res.data)
      }
    } catch (err: any) {
      console.error('Failed to load branches', err)
    } finally {
      setBranchesLoading(false)
    }
  }

  useEffect(() => {
    if (activeTab === 'branches') {
      loadBranches()
    }
  }, [activeTab])

  const openCreateBranch = () => {
    setEditingBranch(null)
    setBranchForm({
      name: '',
      code: '',
      phone: '',
      email: '',
      address: '',
      city: '',
      county: '',
      isHeadOffice: branches.length === 0,
      receiptHeader: '',
      receiptFooter: ''
    })
    setBranchMsg(null)
    setBranchModalOpen(true)
  }

  const openEditBranch = (b: BranchResponse) => {
    setEditingBranch(b)
    setBranchForm({
      name: b.name,
      code: b.code || '',
      phone: b.phone || '',
      email: b.email || '',
      address: b.address || '',
      city: b.city || '',
      county: b.county || '',
      isHeadOffice: b.isHeadOffice,
      receiptHeader: b.receiptHeader || '',
      receiptFooter: b.receiptFooter || ''
    })
    setBranchMsg(null)
    setBranchModalOpen(true)
  }

  const handleSaveBranch = async () => {
    if (!branchForm.name.trim()) {
      setBranchMsg({ ok: false, text: 'Branch name is required' })
      return
    }
    setBranchSaving(true)
    setBranchMsg(null)
    try {
      if (editingBranch) {
        const res = await branchApi.update(editingBranch.id, branchForm)
        if (res.success && res.data) {
          setBranchMsg({ ok: true, text: 'Branch updated successfully' })
          setBranchModalOpen(false)
          loadBranches()
          window.dispatchEvent(new CustomEvent('branch-changed', { detail: { branchId: res.data.id } }))
        } else {
          setBranchMsg({ ok: false, text: res.message || 'Failed to update branch' })
        }
      } else {
        const res = await branchApi.create(branchForm)
        if (res.success && res.data) {
          setBranchMsg({ ok: true, text: 'Branch created successfully' })
          setBranchModalOpen(false)
          loadBranches()
          window.dispatchEvent(new CustomEvent('branch-changed', { detail: { branchId: res.data.id } }))
        } else {
          setBranchMsg({ ok: false, text: res.message || 'Failed to create branch' })
        }
      }
    } catch (err: any) {
      setBranchMsg({ ok: false, text: err?.response?.data?.message || err?.message || 'Error saving branch' })
    } finally {
      setBranchSaving(false)
    }
  }

  const handleSetHeadOffice = async (b: BranchResponse) => {
    if (b.isHeadOffice) return
    if (!window.confirm(`Set "${b.name}" as the primary Head Office branch?`)) return
    try {
      const res = await branchApi.setHeadOffice(b.id)
      if (res.success) {
        setBranchMsg({ ok: true, text: `"${b.name}" is now the Head Office.` })
        loadBranches()
        window.dispatchEvent(new CustomEvent('branch-changed', { detail: { branchId: b.id } }))
      } else {
        setBranchMsg({ ok: false, text: res.message || 'Failed to update head office' })
      }
    } catch (err: any) {
      setBranchMsg({ ok: false, text: err?.response?.data?.message || 'Error setting head office' })
    }
  }

  const handleDeleteBranch = async (b: BranchResponse) => {
    if (b.isHeadOffice) {
      alert('The Head Office branch cannot be deactivated. Designate another branch as Head Office first.')
      return
    }
    if (!window.confirm(`Are you sure you want to deactivate branch "${b.name}"?`)) return
    try {
      const res = await branchApi.delete(b.id)
      if (res.success) {
        setBranchMsg({ ok: true, text: `Branch "${b.name}" deactivated.` })
        loadBranches()
        window.dispatchEvent(new CustomEvent('branch-changed', { detail: {} }))
      } else {
        setBranchMsg({ ok: false, text: res.message || 'Failed to deactivate branch' })
      }
    } catch (err: any) {
      setBranchMsg({ ok: false, text: err?.response?.data?.message || 'Error deactivating branch' })
    }
  }

  const settingsGroups: SettingsNavGroup[] = [
    {
      label: 'Business', icon: <Building2 size={19} />, items: [
        { label: 'Store Profile', tab: 'general' as SettingsTab },
        { label: 'Branches & Outlets', tab: 'branches' as SettingsTab },
      ]
    },
    {
      label: 'Payments & Integrations', icon: <CreditCard size={19} />, items: [
        { label: 'CyberSource Card', tab: 'cybersource' as SettingsTab },
        { label: 'M-Pesa Daraja', tab: 'mpesa' as SettingsTab },
      ]
    },
    {
      label: 'Tax & Compliance', icon: <FileText size={19} />, items: [
        { label: 'Tax Settings', path: '/tax' },
        { label: 'KRA & eTIMS', tab: 'kra' as SettingsTab },
      ]
    },
    {
      label: 'Security & Access', icon: <ShieldCheck size={19} />, items: [
        { label: 'Authentication', tab: 'security' as SettingsTab, security: 'authentication' },
        { label: 'Session Management', tab: 'security' as SettingsTab, security: 'session' },
        { label: 'Access Policies', tab: 'security' as SettingsTab, security: 'access' },
      ]
    },
    {
      label: 'Users & Permissions', icon: <Users size={19} />, items: [
        { label: 'Users', path: '/users' },
        { label: 'Roles & Permissions', path: '/users' },
      ]
    },
    {
      label: 'Notifications', icon: <Bell size={19} />, items: [
        ...(isSuperAdmin ? [{ label: 'Email / SMS / Push', tab: 'notifications' as SettingsTab }] : []),
      ]
    },
    {
      label: 'Social', icon: <MessageSquare size={19} />, items: [
        { label: 'Social Setup', path: '/social-onboarding' },
      ]
    },
    {
      label: 'System', icon: <SettingsIcon size={19} />, items: [
        { label: 'General Settings', tab: 'general' as SettingsTab },
      ]
    },
  ]

  const activeLabel = activeTab === 'security'
    ? 'Security & Access'
    : activeTab === 'branches'
    ? 'Branches & Outlets'
    : settingsGroups.flatMap(group => group.items).find(item => item.tab === activeTab)?.label || 'Store Profile'

  const contentDescription = activeTab === 'security'
    ? 'Manage authentication, session settings and access policies'
    : activeTab === 'branches'
    ? 'Manage your physical branches, stores, regional outlets, and eTIMS branch codes'
    : 'Manage your system configuration'

  return (
    <div className="fade-in" style={{ maxWidth: 'none', width: '100%' }}>
      <div className="settings-layout" style={{
        display: 'grid', gridTemplateColumns: '270px minmax(0, 1fr)', gap: 0,
        border: '1px solid var(--b360-border)', borderRadius: 12, overflow: 'hidden',
        background: 'var(--b360-surface)', minHeight: 600
      }}>
        <aside style={{ background: 'white', padding: 14, borderRight: '1px solid var(--b360-border)' }}>
          <div style={{ padding: '10px 10px 20px' }}>
            <h1 style={{ margin: 0, fontSize: 26, letterSpacing: '-0.5px' }}>Settings</h1>
            <p style={{ margin: '5px 0 0', color: 'var(--b360-text-secondary)', fontSize: 13 }}>Manage your system configuration</p>
          </div>
          {settingsGroups.map(group => {
            const groupActive = group.label === 'Security & Access' && activeTab === 'security'
            return (
              <div key={group.label} style={{ marginBottom: 10 }}>
                <div style={{
                  display: 'flex', alignItems: 'center', gap: 10, padding: '10px 10px 7px',
                  color: groupActive ? 'var(--b360-blue)' : 'var(--b360-text)', fontWeight: 700, fontSize: 13
                }}>
                  <span style={{ display: 'inline-flex', color: groupActive ? 'var(--b360-blue)' : 'var(--b360-text)' }}>{group.icon}</span>
                  <span>{group.label}</span>
                  <ChevronDown size={15} style={{ marginLeft: 'auto', color: groupActive ? 'var(--b360-blue)' : 'var(--b360-text-secondary)' }} />
                </div>
                <div style={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                  {group.items.map(item => {
                    const itemActive = item.tab === activeTab && (!item.security || item.security === securitySection)
                    return (
                      <button
                        key={item.label}
                        type="button"
                        onClick={() => {
                          if (item.path) navigate(item.path)
                          if (item.tab) {
                            handleTabChange(item.tab)
                            if (item.security) setSecuritySection(item.security)
                          }
                        }}
                        style={{
                          display: 'flex', alignItems: 'center', width: '100%', gap: 8, padding: '9px 12px 9px 42px',
                          border: 'none', borderLeft: itemActive ? '4px solid var(--b360-blue)' : '4px solid transparent',
                          borderRadius: 5, background: itemActive ? 'var(--b360-blue-bg)' : 'transparent',
                          color: itemActive ? 'var(--b360-blue)' : 'var(--b360-text-secondary)',
                          fontSize: 13, fontWeight: itemActive ? 700 : 500, textAlign: 'left', cursor: 'pointer'
                        }}
                      >
                        <span style={{ flex: 1 }}>{item.label}</span>
                        {itemActive && <ChevronRight size={14} />}
                      </button>
                    )
                  })}
                </div>
              </div>
            )
          })}
        </aside>

        <main style={{ minWidth: 0, padding: '28px 34px 34px', background: '#fbfdff' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, color: 'var(--b360-text-secondary)', fontSize: 12, marginBottom: 22 }}>
            <span>Settings</span><ChevronRight size={14} />
            <span>{activeLabel}</span>
            {activeTab === 'security' && <><ChevronRight size={14} /><span>{securitySection === 'authentication' ? 'Authentication' : securitySection === 'session' ? 'Session Management' : 'Access Policies'}</span></>}
          </div>
          <h2 style={{ margin: 0, fontSize: 27, letterSpacing: '-0.5px' }}>{activeLabel}</h2>
          <p style={{ margin: '5px 0 22px', color: 'var(--b360-text-secondary)', fontSize: 14 }}>{contentDescription}</p>

          {activeTab === 'security' && (
            <div style={{ display: 'flex', gap: 28, borderBottom: '1px solid var(--b360-border)', marginBottom: 24 }}>
              {[
                { key: 'authentication' as const, label: 'Authentication', icon: <Lock size={18} /> },
                { key: 'session' as const, label: 'Session Management', icon: <Clock size={18} /> },
                { key: 'access' as const, label: 'Access Policies', icon: <Shield size={18} /> },
              ].map(tab => (
                <button key={tab.key} type="button" onClick={() => setSecuritySection(tab.key)} style={{
                  display: 'flex', alignItems: 'center', gap: 9, padding: '0 4px 14px', border: 'none',
                  borderBottom: securitySection === tab.key ? '2px solid var(--b360-blue)' : '2px solid transparent',
                  background: 'transparent', color: securitySection === tab.key ? 'var(--b360-blue)' : 'var(--b360-text-secondary)',
                  fontWeight: securitySection === tab.key ? 700 : 500, fontSize: 14, cursor: 'pointer', whiteSpace: 'nowrap'
                }}>{tab.icon}{tab.label}</button>
              ))}
            </div>
          )}

      {/* ── TAB 1: STORE PROFILE & RECEIPT TEMPLATES ── */}
      {activeTab === 'general' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          {profileMsg && (
            <div style={{ padding: 12, background: profileMsg.ok ? 'var(--b360-green-bg)' : 'var(--b360-red-bg)', color: profileMsg.ok ? 'var(--b360-green)' : 'var(--b360-red)', borderRadius: 8, fontSize: 13, fontWeight: 600 }}>
              {profileMsg.text}
            </div>
          )}

          {profileLoading ? (
            <div style={{ padding: 32, textAlign: 'center', color: 'var(--b360-text-secondary)' }}>Loading business profile…</div>
          ) : (
            <>
              <Section title="Appointments & Services">
                <Toggle label={servicesEnabled ? 'Appointments & Services is enabled' : 'Enable Appointments & Services'} checked={servicesEnabled} onChange={handleServicesToggle} disabled={!isMerchantAdmin || servicesSaving} />
                <div style={{fontSize:12,color:'var(--b360-text-secondary)',lineHeight:1.5}}>Enables the service catalog, resources, appointments, and online shop booking. Disabling preserves existing records for when you reactivate the module.</div>
                {!isMerchantAdmin && <div style={{fontSize:12,color:'var(--b360-amber)'}}>Only a business administrator can change this setting.</div>}
                {servicesSaving && <div style={{fontSize:12,color:'var(--b360-text-secondary)'}}>Updating Appointments & Services…</div>}
              </Section>

              <Section title="Business Information">
                <Input label="Business Name *" value={profile.name} onChange={v => setProfile(p => ({ ...p, name: v }))} />
                <Input label="Owner Name" value={profile.owner} onChange={v => setProfile(p => ({ ...p, owner: v }))} />
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 14 }}>
                  <Input label="Phone Number" value={profile.phone} onChange={v => setProfile(p => ({ ...p, phone: v }))} />
                  <Input label="Email Address" value={profile.email} onChange={v => setProfile(p => ({ ...p, email: v }))} />
                </div>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 14 }}>
                  <Input label="Business Type" value={profile.type} onChange={v => setProfile(p => ({ ...p, type: v }))} />
                  <Input label="County" value={profile.county} onChange={v => setProfile(p => ({ ...p, county: v }))} />
                </div>
                <Input label="Physical Address" value={profile.address} onChange={v => setProfile(p => ({ ...p, address: v }))} />
              </Section>

              <Section title="Receipt Template Configurations">
                <div>
                  <label style={{ fontSize: 12, fontWeight: 600, color: 'var(--b360-text-secondary)', display: 'block', marginBottom: 6 }}>Receipt Logo</label>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                    <div style={{ width: 96, height: 64, border: '1px dashed var(--b360-border)', borderRadius: 8, display: 'flex', alignItems: 'center', justifyContent: 'center', overflow: 'hidden', background: 'var(--b360-surface)' }}>
                      {profile.receiptLogo ? <img src={profile.receiptLogo} alt="Receipt logo" style={{ maxWidth: '100%', maxHeight: '100%', objectFit: 'contain' }} /> : <ImagePlus size={22} color="var(--b360-text-secondary)" />}
                    </div>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
                      <label className="btn" style={{ cursor: 'pointer', padding: '8px 12px', border: '1px solid var(--b360-border)', borderRadius: 8, fontSize: 12, fontWeight: 600, width: 'fit-content' }}>
                        Choose image<input type="file" accept="image/png,image/jpeg,image/webp" hidden onChange={event => handleReceiptLogo(event.target.files?.[0])} />
                      </label>
                      {profile.receiptLogo && <button type="button" onClick={() => setProfile(current => ({ ...current, receiptLogo: null }))} style={{ border: 0, background: 'transparent', color: 'var(--b360-red)', cursor: 'pointer', fontSize: 12, display: 'flex', alignItems: 'center', gap: 4, width: 'fit-content' }}><Trash2 size={12} /> Remove</button>}
                    </div>
                  </div>
                  <div style={{ fontSize: 11, color: 'var(--b360-text-secondary)', marginTop: 5 }}>PNG, JPEG, or WebP; maximum 500 KB. It appears at the top of printed receipts.</div>
                </div>
                <Input label="Receipt Header Message" value={receiptHeader} onChange={setReceiptHeader} placeholder="e.g. Welcome to Kamau Store!" />
                <Input label="Receipt Footer Message" value={receiptFooter} onChange={setReceiptFooter} placeholder="e.g. Thank you for your purchase!" />
              </Section>

              <Section title="Operating Day">
                <div className="responsive-grid responsive-grid-2" style={{gap:14}}>
                  <Input label="Start of day" type="time" value={profile.dayStartTime || '06:00'} onChange={v => setProfile(p => ({...p, dayStartTime:v}))} />
                  <Input label="Close of day" type="time" value={profile.dayCloseTime || '23:00'} onChange={v => setProfile(p => ({...p, dayCloseTime:v}))} />
                </div>
                <div style={{fontSize:12,color:'var(--b360-text-secondary)',lineHeight:1.5}}>These times define the merchant operating day. A closing time earlier than the start time means the business closes after midnight.</div>
              </Section>

              <Section title="Hospitality Mode">
                <Toggle
                  label={hospitalityEnabled ? 'Hospitality mode is enabled' : 'Enable hospitality mode'}
                  checked={hospitalityEnabled}
                  onChange={handleHospitalityToggle}
                  disabled={!isMerchantAdmin || hospitalitySaving}
                />
                <div style={{fontSize:12,color:'var(--b360-text-secondary)',lineHeight:1.5}}>
                  Enables tables, open tabs, kitchen and bar tickets, reservations, shifts, and hospitality operations across supported channels.
                </div>
                {!isMerchantAdmin && <div style={{fontSize:12,color:'var(--b360-amber)'}}>Only a business administrator can change this setting.</div>}
                {hospitalitySaving && <div style={{fontSize:12,color:'var(--b360-text-secondary)'}}>Updating hospitality mode…</div>}
              </Section>

              <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
                <Btn icon={<Save size={14} />} onClick={handleSaveGeneral} disabled={profileSaving}>
                  {profileSaving ? 'Saving Profile…' : 'Save Store Profile'}
                </Btn>
              </div>
            </>
          )}
        </div>
      )}

      {activeTab === 'storefront' && (
        <div style={{ display:'flex', flexDirection:'column', gap:16 }}>
          {profileMsg && <div style={{ padding:12, background:profileMsg.ok ? 'var(--b360-green-bg)' : 'var(--b360-red-bg)', color:profileMsg.ok ? 'var(--b360-green)' : 'var(--b360-red)', borderRadius:8, fontSize:13, fontWeight:600 }}>{profileMsg.text}</div>}
          {profileLoading ? <div style={{padding:32,textAlign:'center',color:'var(--b360-text-secondary)'}}>Loading storefront settings…</div> : <>
            {storefrontSlug && <Section title="Customer ordering"><p>Share your online shop or print a QR code for each table. Customers can order without an account.</p><a href={`/shop/${encodeURIComponent(storefrontSlug)}/qr`} target="_blank" rel="noreferrer">Open shop and table QR codes</a></Section>}
            <Section title="Storefront Appearance">
              <Input label="Welcome headline" value={profile.storefrontHeadline || ''} onChange={value => setProfile(current => ({...current, storefrontHeadline:value}))} placeholder="Shop with us online" />
              <Input label="Store description" value={profile.storefrontDescription || ''} onChange={value => setProfile(current => ({...current, storefrontDescription:value}))} placeholder="Tell customers about your store" />
              <Input label="HTTPS banner image" value={profile.storefrontBannerUrl || ''} onChange={value => setProfile(current => ({...current, storefrontBannerUrl:value || null}))} placeholder="https://example.com/banner.jpg" />
              <div style={{display:'grid',gridTemplateColumns:'1fr 1fr',gap:14}}>
                <label style={{fontSize:12,fontWeight:600}}>Theme color<div style={{display:'flex',gap:8,marginTop:5}}><input type="color" value={profile.storefrontThemeColor || '#0F766E'} onChange={event => setProfile(current => ({...current,storefrontThemeColor:event.target.value.toUpperCase()}))} style={{width:48,height:40,padding:2,border:'1px solid var(--b360-border)',borderRadius:8}}/><input value={profile.storefrontThemeColor || '#0F766E'} maxLength={7} onChange={event => setProfile(current => ({...current,storefrontThemeColor:event.target.value.toUpperCase()}))} style={{minWidth:0,flex:1,padding:'9px 12px',border:'1px solid var(--b360-border)',borderRadius:8}}/></div></label>
                <label style={{fontSize:12,fontWeight:600}}>Product layout<select value={profile.storefrontLayout || 'GRID'} onChange={event => setProfile(current => ({...current,storefrontLayout:event.target.value as 'GRID'|'LIST'}))} style={{display:'block',width:'100%',marginTop:5,padding:'10px 12px',border:'1px solid var(--b360-border)',borderRadius:8,background:'white'}}><option value="GRID">Product grid</option><option value="LIST">Product list</option></select></label>
              </div>
            </Section>
            <Section title="Live Branding Preview">
              <div style={{padding:24,borderRadius:14,color:'white',background:profile.storefrontThemeColor || '#0F766E',backgroundImage:profile.storefrontBannerUrl ? `linear-gradient(#0008,#0008),url(${profile.storefrontBannerUrl})` : undefined,backgroundSize:'cover',backgroundPosition:'center'}}>
                <div style={{fontSize:11,textTransform:'uppercase',letterSpacing:2,fontWeight:800,opacity:.85}}>{profile.name || 'Your business'}</div>
                <h2 style={{margin:'7px 0',fontSize:28}}>{profile.storefrontHeadline || 'Shop with us online'}</h2>
                <p style={{margin:0,opacity:.9}}>{profile.storefrontDescription || 'Your store description will appear here.'}</p>
              </div>
              <div style={{fontSize:12,color:'var(--b360-text-secondary)'}}>Products will be displayed using the selected {profile.storefrontLayout === 'LIST' ? 'list' : 'grid'} layout.</div>
            </Section>
            <div style={{display:'flex',justifyContent:'flex-end'}}><Btn icon={<Save size={14}/>} onClick={handleSaveGeneral} disabled={profileSaving}>{profileSaving ? 'Saving…' : 'Save Storefront'}</Btn></div>
          </>}
        </div>
      )}

      {/* ── TAB 2: CYBERSOURCE CARD GATEWAY ── */}
      {activeTab === 'cybersource' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          {csMsg && (
            <div style={{ padding: 12, background: csMsg.ok ? 'var(--b360-green-bg)' : 'var(--b360-red-bg)', color: csMsg.ok ? 'var(--b360-green)' : 'var(--b360-red)', borderRadius: 8, fontSize: 13, fontWeight: 600 }}>
              {csMsg.text}
            </div>
          )}

          {csLoading ? (
            <div style={{ padding: 32, textAlign: 'center', color: 'var(--b360-text-secondary)' }}>Loading CyberSource settings…</div>
          ) : (
            <>
              <Section title="CyberSource Secure Acceptance Hosted Checkout Configuration">
                <Input
                  label="Merchant ID (Organization ID) *"
                  value={csMerchantId}
                  onChange={setCsMerchantId}
                  placeholder="e.g. biashara360_merchant"
                />
                <Input
                  label="Merchant Key ID (REST API Key ID) *"
                  value={csMerchantKeyId}
                  onChange={setCsMerchantKeyId}
                  placeholder="e.g. 9c7c25eb-xxxx-xxxx-xxxx-xxxxxxx"
                />
                <Input
                  label="Secure Acceptance Profile ID *"
                  value={csProfileId}
                  onChange={setCsProfileId}
                  placeholder="e.g. 3C4D5E6F-7A8B-9C0D-1E2F-3A4B5C6D7E8F"
                />
                <Input
                  label="Secure Acceptance Access Key *"
                  value={csAccessKey}
                  onChange={setCsAccessKey}
                  placeholder="e.g. 1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d"
                />
                <Input
                  label="Shared Secret Key (HMAC Signing Secret) *"
                  value={csMerchantSecretKey}
                  onChange={setCsMerchantSecretKey}
                  type="password"
                  placeholder="Leave blank to keep current secret key"
                />

                <div style={{ padding: 14, background: 'rgba(59, 130, 246, 0.08)', borderRadius: 10, border: '1px solid rgba(59, 130, 246, 0.2)', fontSize: 12, lineHeight: 1.6, color: 'var(--b360-text-secondary)', marginTop: 8 }}>
                  <strong style={{ color: 'var(--b360-text-primary)' }}>CyberSource Business Center Setup Checklist:</strong>
                  <ul style={{ margin: '6px 0 0 16px', padding: 0 }}>
                    <li><strong>Merchant Notification URL (Webhook):</strong> <code>https://api.biashara360.co.ke/v1/public/payments/card/sa-notify</code></li>
                    <li><strong>Customer Response & Cancel URL:</strong> <code>https://api.biashara360.co.ke/v1/public/payments/card/sa-return</code></li>
                    <li><strong>Transaction Type:</strong> <code>sale</code></li>
                  </ul>
                </div>

                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: 14 }}>
                  <div>
                    <span style={{ fontSize: 13, fontWeight: 600, display: 'block' }}>Active Sandbox Environment</span>
                    <span style={{ fontSize: 11, color: 'var(--b360-text-secondary)' }}>Toggle off to deploy credentials on live CyberSource production rails</span>
                  </div>
                  <div onClick={() => setCsIsSandbox(!csIsSandbox)} style={{
                    width: 44, height: 24, borderRadius: 12, cursor: 'pointer', transition: 'background 0.2s',
                    background: csIsSandbox ? 'var(--b360-green)' : '#D1D5DB', position: 'relative'
                  }}>
                    <div style={{ position: 'absolute', top: 2, left: csIsSandbox ? 22 : 2, width: 20, height: 20, borderRadius: '50%', background: 'white', transition: 'left 0.2s', boxShadow: '0 1px 3px rgba(0,0,0,0.2)' }} />
                  </div>
                </div>
              </Section>

              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <Btn variant="secondary" icon={<CreditCard size={14} />} onClick={() => navigate('/card-payments')}>
                  Manage Payment Links & Cards
                </Btn>
                <Btn icon={<Save size={14} />} onClick={handleSaveCyberSource} disabled={csSaving}>
                  {csSaving ? 'Saving Config…' : 'Save CyberSource Gateway'}
                </Btn>
              </div>
            </>
          )}
        </div>
      )}

      {/* ── TAB 3: KRA & ETIMS SETUP ── */}
      {activeTab === 'kra' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          {kraMsg && (
            <div style={{ padding: 12, background: kraMsg.ok ? 'var(--b360-green-bg)' : 'var(--b360-red-bg)', color: kraMsg.ok ? 'var(--b360-green)' : 'var(--b360-red)', borderRadius: 8, fontSize: 13, fontWeight: 600 }}>
              {kraMsg.text}
            </div>
          )}

          {kraEnv === 'production' && (
            <div style={{ background: '#FFF3E0', border: '1px solid #FFB300', borderRadius: 10, padding: '12px 16px', display: 'flex', gap: 10, alignItems: 'flex-start' }}>
              <AlertTriangle size={18} color="#FF8F00" style={{ flexShrink: 0, marginTop: 1 }} />
              <div style={{ fontSize: 13 }}>
                <strong>Production Mode Active:</strong> All transmitted sales will be posted directly to KRA's live iTax eTIMS system.
              </div>
            </div>
          )}

          <Section title="KRA Taxpayer Profile">
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 14 }}>
              <div>
                <label style={{ fontSize: 12, fontWeight: 600, display: 'block', marginBottom: 5 }}>KRA PIN *</label>
                <input
                  value={kraPin}
                  onChange={e => setKraPin(e.target.value.toUpperCase())}
                  placeholder="P051234567X"
                  style={{ width: '100%', padding: '9px 12px', borderRadius: 8, border: '1px solid var(--b360-border)', fontSize: 13, fontFamily: 'monospace', fontWeight: 700 }}
                />
              </div>
              <div>
                <label style={{ fontSize: 12, fontWeight: 600, display: 'block', marginBottom: 5 }}>Registered Company Name *</label>
                <input
                  value={kraCompanyName}
                  onChange={e => setKraCompanyName(e.target.value)}
                  placeholder="Company Name"
                  style={{ width: '100%', padding: '9px 12px', borderRadius: 8, border: '1px solid var(--b360-border)', fontSize: 13 }}
                />
              </div>
              <div>
                <label style={{ fontSize: 12, fontWeight: 600, display: 'block', marginBottom: 5 }}>VAT Registration Number</label>
                <input
                  value={kraVatNo}
                  onChange={e => setKraVatNo(e.target.value)}
                  placeholder="Same as PIN if VAT registered"
                  style={{ width: '100%', padding: '9px 12px', borderRadius: 8, border: '1px solid var(--b360-border)', fontSize: 13 }}
                />
              </div>
              <div>
                <label style={{ fontSize: 12, fontWeight: 600, display: 'block', marginBottom: 5 }}>Target Environment</label>
                <select
                  value={kraEnv}
                  onChange={e => setKraEnv(e.target.value as any)}
                  style={{ width: '100%', padding: '9px 12px', borderRadius: 8, border: '1px solid var(--b360-border)', fontSize: 13, background: 'white' }}
                >
                  <option value="sandbox">Sandbox (Testing)</option>
                  <option value="production">Production (Live KRA)</option>
                </select>
              </div>
            </div>
          </Section>

          <Section title="eTIMS Virtual Device Controller (SDC)">
            <div style={{ fontSize: 12, color: 'var(--b360-text-secondary)', marginBottom: 8, lineHeight: 1.5 }}>
              Register at <a href="https://etims.kra.go.ke" target="_blank" rel="noreferrer" style={{ color: 'var(--b360-green)' }}>etims.kra.go.ke</a> to obtain your assigned SDC ID and Virtual Control Unit serial number.
            </div>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 14 }}>
              <div>
                <label style={{ fontSize: 12, fontWeight: 600, display: 'block', marginBottom: 5 }}>SDC ID</label>
                <input
                  value={kraSdcId}
                  onChange={e => setKraSdcId(e.target.value)}
                  placeholder="From KRA eTIMS portal"
                  style={{ width: '100%', padding: '9px 12px', borderRadius: 8, border: '1px solid var(--b360-border)', fontSize: 13, fontFamily: 'monospace' }}
                />
              </div>
              <div>
                <label style={{ fontSize: 12, fontWeight: 600, display: 'block', marginBottom: 5 }}>Device Serial Number</label>
                <input
                  value={kraSerialNo}
                  onChange={e => setKraSerialNo(e.target.value)}
                  placeholder="VSCU assigned by KRA"
                  style={{ width: '100%', padding: '9px 12px', borderRadius: 8, border: '1px solid var(--b360-border)', fontSize: 13, fontFamily: 'monospace' }}
                />
              </div>
            </div>
          </Section>

          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <Btn variant="secondary" icon={<ExternalLink size={14} />} onClick={() => navigate('/kra')}>
              View eTIMS Invoices & Returns
            </Btn>
            <Btn icon={<Save size={14} />} onClick={handleSaveKra} disabled={kraSaving}>
              {kraSaving ? 'Saving Profile…' : 'Save KRA & eTIMS Profile'}
            </Btn>
          </div>
        </div>
      )}

      {/* ── TAB 4: M-PESA DARAJA ── */}
      {activeTab === 'mpesa' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          {mpMsg && (
            <div style={{ padding: 12, background: mpMsg.ok ? 'var(--b360-green-bg)' : 'var(--b360-red-bg)', color: mpMsg.ok ? 'var(--b360-green)' : 'var(--b360-red)', borderRadius: 8, fontSize: 13, fontWeight: 600 }}>
              {mpMsg.text}
            </div>
          )}

          {mpLoading ? (
            <div style={{ padding: 32, textAlign: 'center', color: 'var(--b360-text-secondary)' }}>Loading M-Pesa channels…</div>
          ) : (
            <>
              <Section title="Daraja API Setup">
                <Select
                  label="Channel Account Type"
                  value={mpAccountType}
                  onChange={setMpAccountType}
                  options={[
                    { value: 'paybill', label: mpChannels.some(c => c.accountType === 'paybill') ? 'Paybill — Configured' : 'Paybill — Not configured' },
                    { value: 'till', label: mpChannels.some(c => c.accountType === 'till') ? 'Till — Not configured' : 'Till — Configured' }
                  ]}
                />
                <Input
                  label={mpPasskeyConfigured ? 'Replace Lipa na M-Pesa Passkey' : 'Lipa na M-Pesa Passkey *'}
                  value={mpPassKey}
                  onChange={setMpPassKey}
                  type="password"
                  placeholder={mpPasskeyConfigured ? '••••••••••••••••' : 'Enter passkey from Safaricom'}
                />
                <Input
                  label="Business Shortcode (Paybill / Till) *"
                  value={mpShortCode}
                  onChange={setMpShortCode}
                  placeholder="e.g. 174379"
                />
                <Input
                  label="Callback URL *"
                  value={mpCallbackUrl}
                  onChange={setMpCallbackUrl}
                  placeholder="https://api.biashara360.co.ke/v1/payments/mpesa/callback"
                />
                <Select
                  label="Environment"
                  value={mpEnvironment}
                  onChange={setMpEnvironment}
                  options={[
                    { value: 'sandbox', label: 'Sandbox' },
                    { value: 'production', label: 'Production' }
                  ]}
                />
              </Section>

              <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
                <Btn icon={<Save size={14} />} onClick={handleSaveMpesa} disabled={mpSaving}>
                  {mpSaving ? 'Saving Channel…' : 'Save M-Pesa Channel'}
                </Btn>
              </div>
            </>
          )}
        </div>
      )}

      {/* ── TAB 5: SECURITY & TIMEOUTS ── */}
      {activeTab === 'security' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          {secMsg && (
            <div style={{ padding: 12, background: secMsg.ok ? 'var(--b360-green-bg)' : 'var(--b360-red-bg)', color: secMsg.ok ? 'var(--b360-green)' : 'var(--b360-red)', borderRadius: 8, fontSize: 13, fontWeight: 600 }}>
              {secMsg.text}
            </div>
          )}

          {securitySection === 'session' && isMerchantAdmin && (
            <Section title="Session Timeout Policies">
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 14 }}>
                <Input
                  label="Web Timeout (mins)"
                  value={String(Math.round(sessionTimeouts.webTimeoutSeconds / 60))}
                  onChange={v => setSessionTimeouts(s => ({ ...s, webTimeoutSeconds: (Number(v) || 30) * 60 }))}
                  type="number"
                />
                <Input
                  label="Android Timeout (mins)"
                  value={String(Math.round(sessionTimeouts.androidTimeoutSeconds / 60))}
                  onChange={v => setSessionTimeouts(s => ({ ...s, androidTimeoutSeconds: (Number(v) || 60) * 60 }))}
                  type="number"
                />
                <Input
                  label="Desktop Timeout (mins)"
                  value={String(Math.round(sessionTimeouts.desktopTimeoutSeconds / 60))}
                  onChange={v => setSessionTimeouts(s => ({ ...s, desktopTimeoutSeconds: (Number(v) || 120) * 60 }))}
                  type="number"
                />
              </div>
            </Section>
          )}

          {securitySection === 'authentication' && <>
            <Section title="Two-Factor Authentication (2FA)">
              <Toggle label="Enable Two-Factor Authentication (2FA) for Admin Logins" checked={twoFA} onChange={setTwoFA} />
              <p style={{fontSize:12,color:'var(--b360-text-secondary)',margin:0,lineHeight:1.5}}>When enabled, admin users will be required to enter a verification code in addition to their password.</p>
            </Section>

            <Section title="PIN Login">
              <p style={{fontSize:12,color:'var(--b360-text-secondary)',margin:0,lineHeight:1.5}}>Create a personal six-digit PIN for faster sign-in. Your current password is required, and existing OTP rules still apply.</p>
              <Input label="Current password" type="password" value={pinPassword} onChange={setPinPassword} />
              <div className="responsive-grid responsive-grid-2" style={{gap:14}}>
                <Input label="New 6-digit PIN" type="password" value={loginPin} onChange={value=>setLoginPin(value.replace(/\D/g,'').slice(0,6))} />
                <Input label="Confirm PIN" type="password" value={confirmLoginPin} onChange={value=>setConfirmLoginPin(value.replace(/\D/g,'').slice(0,6))} />
              </div>
              <div style={{display:'flex',gap:8,justifyContent:'flex-end',flexWrap:'wrap'}}><Btn variant="secondary" disabled={pinSaving || !pinPassword} onClick={()=>handleLoginPin(true)}>Disable PIN</Btn><Btn disabled={pinSaving || !pinPassword || loginPin.length!==6 || confirmLoginPin.length!==6} onClick={()=>handleLoginPin(false)}>{pinSaving ? 'Saving…' : 'Set PIN'}</Btn></div>
            </Section>

            <Section title="Change Account Password">
              <p style={{fontSize:12,color:'var(--b360-text-secondary)',margin:0,lineHeight:1.5}}>Update your login password. Must be at least 8 characters and include a letter, number, and special character (@, $, !, %, *, ?, &, _, -, +, =, .).</p>
              <Input label="Current password" type="password" value={changeCurrPass} onChange={setChangeCurrPass} />
              <div className="responsive-grid responsive-grid-2" style={{gap:14}}>
                <Input label="New password (min 8 chars)" type="password" value={changeNewPass} onChange={setChangeNewPass} />
                <Input label="Confirm new password" type="password" value={changeConfirmPass} onChange={setChangeConfirmPass} />
              </div>
              <div style={{display:'flex',justifyContent:'flex-end'}}><Btn disabled={passSaving || !changeCurrPass || !changeNewPass || changeNewPass.length < 8 || changeNewPass !== changeConfirmPass} onClick={handleChangePassword}>{passSaving ? 'Updating Password…' : 'Update Password'}</Btn></div>
            </Section>
          </>}

          {securitySection === 'access' && (
            <Section title="Access Policies">
              <p style={{ margin: 0, fontSize: 13, color: 'var(--b360-text-secondary)', lineHeight: 1.6 }}>
                Manage roles, permission groups, and user access to business areas from Users &amp; Permissions.
              </p>
              <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
                <Btn variant="secondary" onClick={() => navigate('/users')}>Open Roles &amp; Permissions</Btn>
              </div>
            </Section>
          )}

          {securitySection === 'session' && !isMerchantAdmin && (
            <Section title="Session Management">
              <p style={{ margin: 0, fontSize: 13, color: 'var(--b360-text-secondary)' }}>Session timeout policies can only be changed by a merchant administrator.</p>
            </Section>
          )}

          {securitySection === 'session' && isMerchantAdmin && (
            <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
              <Btn icon={<Save size={14} />} onClick={handleSaveSecurity} disabled={secSaving}>
                {secSaving ? 'Saving Security…' : 'Save Security Policy'}
              </Btn>
            </div>
          )}
        </div>
      )}

      {/* ── TAB 6: NOTIFICATIONS & SUBSCRIPTION ── */}
      {activeTab === 'notifications' && isSuperAdmin && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          {/* Email Service Configuration (Oracle OCI, Gmail & Outlook 365) */}
          <Section title="Email Service (SMTP)">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 10 }}>
              <div>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                  <Mail size={18} style={{ color: emailStatus?.configured ? '#16a34a' : '#d97706' }} />
                  <span style={{ fontWeight: 600, fontSize: 14 }}>
                    {emailStatus?.host?.includes('oraclecloud.com') || emailStatus?.host?.includes('oracle')
                      ? 'Oracle Cloud (OCI) Email Delivery'
                      : emailStatus?.host?.includes('gmail')
                        ? 'Google Gmail SMTP'
                        : emailStatus?.host?.includes('office365')
                          ? 'Microsoft Outlook 365 SMTP'
                          : 'Custom SMTP Server'}
                  </span>
                  <span style={{
                    padding: '2px 8px',
                    borderRadius: 999,
                    fontSize: 11,
                    fontWeight: 700,
                    color: emailStatus?.configured ? '#047857' : '#b45309',
                    background: emailStatus?.configured ? '#d1fae5' : '#fef3c7',
                  }}>
                    {emailStatus?.configured ? 'Connected / Configured' : 'Credentials Missing'}
                  </span>
                </div>
                <div style={{ fontSize: 12, color: 'var(--b360-text-secondary)', marginTop: 4 }}>
                  Host: <strong>{emailStatus?.host || 'smtp.email.af-johannesburg-1.oci.oraclecloud.com'}</strong> : <strong>{emailStatus?.port || 587}</strong> (STARTTLS)
                  {emailStatus?.fromEmail && <> &bull; Sender: <strong>{emailStatus.fromEmail}</strong> ({emailStatus.fromName || 'Biashara360'})</>}
                </div>
              </div>
              <Btn variant="secondary" small onClick={() => setEditingSmtp(!editingSmtp)}>
                {editingSmtp ? 'Close Settings' : 'Configure SMTP'}
              </Btn>
            </div>

            {testResult && (
              <div style={{
                padding: '10px 14px',
                borderRadius: 8,
                fontSize: 13,
                display: 'flex',
                alignItems: 'center',
                gap: 8,
                background: testResult.success ? '#d1fae5' : '#fee2e2',
                color: testResult.success ? '#065f46' : '#991b1b',
                border: `1px solid ${testResult.success ? '#a7f3d0' : '#fecaca'}`
              }}>
                {testResult.success ? <CheckCircle size={16} /> : <AlertTriangle size={16} />}
                <span>{testResult.message}</span>
              </div>
            )}

            {/* Test Email Section */}
            <div style={{ background: '#f8fafc', padding: 14, borderRadius: 8, border: '1px solid #e2e8f0' }}>
              <div style={{ fontWeight: 600, fontSize: 13, marginBottom: 8, color: '#334155' }}>
                Test Email Connection
              </div>
              <div style={{ display: 'flex', gap: 10, alignItems: 'center', flexWrap: 'wrap' }}>
                <div style={{ flex: 1, minWidth: 220 }}>
                  <Input
                    label=""
                    placeholder="Recipient email address (e.g. your email)"
                    value={testRecipient}
                    onChange={setTestRecipient}
                    type="email"
                  />
                </div>
                <Btn
                  disabled={sendingTest || !testRecipient.trim()}
                  onClick={handleSendTestEmail}
                  small
                  icon={<Send size={14} />}
                >
                  {sendingTest ? 'Sending...' : 'Send Test Email'}
                </Btn>
              </div>
            </div>

            {/* Edit SMTP Settings Form */}
            {editingSmtp && (
              <div style={{ background: '#fafafa', padding: 16, borderRadius: 8, border: '1px solid #e5e7eb', marginTop: 8 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12, flexWrap: 'wrap', gap: 8 }}>
                  <h4 style={{ fontWeight: 600, fontSize: 13, margin: 0, color: '#1f2937' }}>
                    SMTP Server Configuration
                  </h4>
                  {/* Provider Quick Presets */}
                  <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
                    <button
                      type="button"
                      onClick={() => setSmtpForm(p => ({
                        ...p,
                        host: 'smtp.email.af-johannesburg-1.oci.oraclecloud.com',
                        port: 587,
                        fromEmail: p.fromEmail || 'noreply@biashara360.co.ke',
                        fromName: p.fromName || 'Biashara360'
                      }))}
                      style={{
                        padding: '4px 8px', fontSize: 11, fontWeight: 600, borderRadius: 6,
                        border: '1px solid #d1d5db',
                        background: (smtpForm.host.includes('oraclecloud') || smtpForm.host.includes('oracle')) ? '#fef3c7' : 'white',
                        color: (smtpForm.host.includes('oraclecloud') || smtpForm.host.includes('oracle')) ? '#b45309' : '#374151',
                        cursor: 'pointer'
                      }}
                    >
                      Oracle Cloud (OCI) Preset
                    </button>
                    <button
                      type="button"
                      onClick={() => setSmtpForm(p => ({ ...p, host: 'smtp.gmail.com', port: 587 }))}
                      style={{
                        padding: '4px 8px', fontSize: 11, fontWeight: 600, borderRadius: 6,
                        border: '1px solid #d1d5db', background: smtpForm.host.includes('gmail') ? '#e0f2fe' : 'white',
                        color: smtpForm.host.includes('gmail') ? '#0369a1' : '#374151', cursor: 'pointer'
                      }}
                    >
                      Gmail Preset
                    </button>
                    <button
                      type="button"
                      onClick={() => setSmtpForm(p => ({ ...p, host: 'smtp.office365.com', port: 587 }))}
                      style={{
                        padding: '4px 8px', fontSize: 11, fontWeight: 600, borderRadius: 6,
                        border: '1px solid #d1d5db', background: smtpForm.host.includes('office365') ? '#e0f2fe' : 'white',
                        color: smtpForm.host.includes('office365') ? '#0369a1' : '#374151', cursor: 'pointer'
                      }}
                    >
                      Outlook 365 Preset
                    </button>
                  </div>
                </div>

                {/* Region selector helper for Oracle Cloud */}
                {(smtpForm.host.includes('oraclecloud') || smtpForm.host.includes('oracle')) && (
                  <div style={{ marginBottom: 12, padding: '8px 12px', background: '#fffbeb', borderRadius: 6, border: '1px solid #fde68a', display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
                    <span style={{ fontSize: 12, fontWeight: 600, color: '#92400e' }}>OCI Region Endpoint:</span>
                    {[
                      { name: 'Frankfurt (eu-frankfurt-1)', host: 'smtp.email.eu-frankfurt-1.oci.oraclecloud.com' },
                      { name: 'Johannesburg (af-johannesburg-1)', host: 'smtp.email.af-johannesburg-1.oci.oraclecloud.com' },
                      { name: 'Ashburn (us-ashburn-1)', host: 'smtp.email.us-ashburn-1.oci.oraclecloud.com' },
                      { name: 'London (uk-london-1)', host: 'smtp.email.uk-london-1.oci.oraclecloud.com' },
                      { name: 'Phoenix (us-phoenix-1)', host: 'smtp.email.us-phoenix-1.oci.oraclecloud.com' },
                    ].map(r => (
                      <button
                        key={r.host}
                        type="button"
                        onClick={() => setSmtpForm(p => ({ ...p, host: r.host }))}
                        style={{
                          padding: '2px 8px', fontSize: 11, borderRadius: 4, cursor: 'pointer',
                          background: smtpForm.host === r.host ? '#f59e0b' : '#ffffff',
                          color: smtpForm.host === r.host ? '#ffffff' : '#92400e',
                          border: '1px solid #fcd34d', fontWeight: smtpForm.host === r.host ? 700 : 500
                        }}
                      >
                        {r.name}
                      </button>
                    ))}
                  </div>
                )}

                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: 12 }}>
                  <Input
                    label="SMTP Host"
                    placeholder="smtp.email.eu-frankfurt-1.oci.oraclecloud.com"
                    value={smtpForm.host}
                    onChange={v => setSmtpForm(p => ({ ...p, host: v }))}
                  />
                  <Input
                    label="SMTP Port"
                    placeholder="587"
                    type="number"
                    value={String(smtpForm.port)}
                    onChange={v => setSmtpForm(p => ({ ...p, port: parseInt(v) || 587 }))}
                  />
                  <Input
                    label="Account Username / OCID"
                    placeholder="e.g. ocid1.user.oc1... or email"
                    value={smtpForm.username}
                    onChange={v => setSmtpForm(p => ({ ...p, username: v, fromEmail: p.fromEmail || (v.includes('@') ? v : '') }))}
                  />
                  <Input
                    label="Password / SMTP Secret"
                    placeholder="Enter SMTP Password (or leave blank to keep unchanged)"
                    type="password"
                    value={smtpForm.password}
                    onChange={v => setSmtpForm(p => ({ ...p, password: v }))}
                  />
                  <Input
                    label="From Email (Approved Sender)"
                    placeholder="e.g. noreply@biashara360.co.ke"
                    value={smtpForm.fromEmail}
                    onChange={v => setSmtpForm(p => ({ ...p, fromEmail: v }))}
                  />
                  <Input
                    label="From Sender Name"
                    placeholder="Biashara360"
                    value={smtpForm.fromName}
                    onChange={v => setSmtpForm(p => ({ ...p, fromName: v }))}
                  />
                </div>

                {/* Helpful Note for Provider Requirements */}
                {(smtpForm.host.includes('oraclecloud') || smtpForm.host.includes('oracle')) ? (
                  <div style={{ marginTop: 12, padding: 12, background: '#fffbeb', borderRadius: 6, fontSize: 12, color: '#92400e', lineHeight: 1.5, border: '1px solid #fde68a' }}>
                    <strong>Oracle Cloud Infrastructure (OCI) Email Delivery Setup:</strong>
                    <ol style={{ margin: '6px 0 0 16px', padding: 0 }}>
                      <li><strong>Email Domain:</strong> In OCI Console &rarr; <em>Developer Services &gt; Email Delivery &gt; Email Domains</em>, create your domain (e.g., <code>biashara360.co.ke</code>).</li>
                      <li><strong>DNS Verification:</strong> Add DKIM and SPF (<code>v=spf1 include:rp.oracleemaildelivery.com ~all</code>) to your DNS manager.</li>
                      <li><strong>Approved Sender:</strong> Under <em>Email Delivery &gt; Approved Senders</em>, add your sender address (e.g., <code>noreply@biashara360.co.ke</code>). It must match the <em>From Email</em> above.</li>
                      <li><strong>SMTP Credentials:</strong> In <em>Identity &gt; Users &gt; [User] &gt; SMTP Credentials</em>, click <em>Generate SMTP Credentials</em>. Paste the generated username OCID into <em>Account Username / OCID</em> and secret key into <em>Password</em>.</li>
                    </ol>
                  </div>
                ) : smtpForm.host.includes('gmail') ? (
                  <div style={{ marginTop: 12, padding: 10, background: '#fef3c7', borderRadius: 6, fontSize: 12, color: '#92400e', lineHeight: 1.5 }}>
                    <strong>Google Gmail Setup Requirement:</strong> Google does not allow standard account passwords for SMTP. You must generate a 16-character <strong>App Password</strong>:
                    <ol style={{ margin: '6px 0 0 16px', padding: 0 }}>
                      <li>Enable <strong>2-Step Verification</strong> in your Google Account.</li>
                      <li>Visit <a href="https://myaccount.google.com/apppasswords" target="_blank" rel="noreferrer" style={{ color: '#b45309', textDecoration: 'underline' }}>myaccount.google.com/apppasswords</a>.</li>
                      <li>Create an app password named <em>Biashara360</em>.</li>
                      <li>Paste the generated 16-character code into the password field above.</li>
                    </ol>
                  </div>
                ) : (
                  <div style={{ marginTop: 12, padding: 10, background: '#eff6ff', borderRadius: 6, fontSize: 12, color: '#1e40af' }}>
                    <strong>Microsoft 365 Setup Note:</strong> Ensure <em>Authenticated SMTP (SMTP AUTH)</em> is enabled for this mailbox in Microsoft 365 Admin Center. If Multi-Factor Authentication is active, generate and use an <em>App Password</em>.
                  </div>
                )}

                <div style={{ marginTop: 14, display: 'flex', gap: 10, justifyContent: 'flex-end' }}>
                  <Btn variant="secondary" small onClick={() => setEditingSmtp(false)}>Cancel</Btn>
                  <Btn small disabled={savingSmtp} onClick={handleSaveSmtp}>{savingSmtp ? 'Saving...' : 'Save SMTP Settings'}</Btn>
                </div>
              </div>
            )}
          </Section>

          <Section title="Alerts & Notification Preferences">
            <Toggle label="SMS Alerts (Payment confirmation, low inventory)" checked={smsAlerts} onChange={setSmsAlerts} />
            <Toggle label="Email Alerts (Daily sales summary, tax reminders)" checked={emailAlerts} onChange={setEmailAlerts} />
          </Section>

          <Section title="Subscription Plan & Tier">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <div>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                  <div style={{ fontWeight: 700, fontSize: 15 }}>
                    {subscriptionTier === 'PREMIUM' ? 'Premium Plan' : 'Freemium Plan'}
                  </div>
                  <span style={{
                    padding: '3px 8px',
                    borderRadius: 999,
                    fontSize: 11,
                    fontWeight: 700,
                    color: subscriptionEnabled ? '#047857' : '#b91c1c',
                    background: subscriptionEnabled ? '#d1fae5' : '#fee2e2',
                  }}>
                    {subscriptionEnabled ? 'Active' : 'Disabled'}
                  </span>
                </div>
                <div style={{ fontSize: 12, color: 'var(--b360-text-secondary)', marginTop: 2 }}>
                  {subscriptionEnabled
                    ? (subscriptionTier === 'PREMIUM'
                      ? 'Premium features are enabled for this business.'
                      : 'Up to 100 products & 50 orders per month.')
                    : 'Access is disabled. Contact Biashara360 support to reactivate this subscription.'}
                </div>
              </div>
              <Btn
                disabled={!subscriptionEnabled || subscriptionTier === 'PREMIUM'}
                onClick={() => window.open('mailto:sales@biashara360.co.ke?subject=Upgrade Biashara360 Plan', '_blank')}
              >
                Upgrade to Premium →
              </Btn>
            </div>
          </Section>
        </div>
      )}

      {/* ── TAB 8: BRANCHES & OUTLETS ── */}
      {activeTab === 'branches' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
          {branchMsg && (
            <div style={{
              padding: 14,
              background: branchMsg.ok ? 'var(--b360-green-bg)' : 'var(--b360-red-bg)',
              color: branchMsg.ok ? 'var(--b360-green)' : 'var(--b360-red)',
              borderRadius: 8,
              fontSize: 13,
              fontWeight: 600,
              display: 'flex',
              alignItems: 'center',
              gap: 8
            }}>
              {branchMsg.ok ? <CheckCircle size={16} /> : <AlertTriangle size={16} />}
              <span>{branchMsg.text}</span>
            </div>
          )}

          {/* Quick Metrics */}
          <div className="responsive-grid responsive-grid-4" style={{ gap: 12 }}>
            <KpiCard
              title="Total Outlets"
              value={String(branches.length)}
              change="Configured branches"
              icon={<Store size={18} />}
              color="var(--b360-blue)"
            />
            <KpiCard
              title="Active Locations"
              value={String(branches.filter(b => b.isActive).length)}
              change="Accepting transactions"
              icon={<MapPin size={18} />}
              color="var(--b360-green)"
            />
            <KpiCard
              title="Head Office"
              value={branches.find(b => b.isHeadOffice)?.code || 'MAIN'}
              change={branches.find(b => b.isHeadOffice)?.name || 'Primary HQ'}
              icon={<Building2 size={18} />}
              color="var(--b360-amber)"
            />
            <KpiCard
              title="eTIMS Branches"
              value={String(branches.filter(b => !!b.code).length)}
              change="Ready with bhfId"
              icon={<GitBranch size={18} />}
              color="#8B5CF6"
            />
          </div>

          <Card style={{ padding: 22 }}>
            <div style={{
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              flexWrap: 'wrap',
              gap: 12,
              marginBottom: 16
            }}>
              <div>
                <h3 style={{ margin: 0, fontWeight: 700, fontSize: 16, display: 'flex', alignItems: 'center', gap: 8 }}>
                  <Store size={18} color="var(--b360-green)" />
                  Merchant Outlets & Branches
                </h3>
                <p style={{ margin: '4px 0 0', fontSize: 13, color: 'var(--b360-text-secondary)' }}>
                  Manage multiple physical outlets, cash registers, and regional stores. Orders, expenses, and staff are organized by branch.
                </p>
              </div>
              <Btn icon={<Plus size={15} />} onClick={openCreateBranch}>
                Add New Branch
              </Btn>
            </div>

            {branchesLoading ? (
              <div style={{ padding: 36, textAlign: 'center', color: 'var(--b360-text-secondary)' }}>
                Loading branches…
              </div>
            ) : branches.length === 0 ? (
              <div style={{ padding: 36, textAlign: 'center', color: 'var(--b360-text-secondary)' }}>
                No branches configured. Click "Add New Branch" to create your first outlet.
              </div>
            ) : (
              <div className="ui-table-wrap" style={{ overflowX: 'auto', borderRadius: 'var(--radius-md)', border: '1px solid var(--b360-border)' }}>
                <table style={{ width: '100%', minWidth: 780, borderCollapse: 'collapse' }}>
                  <thead>
                    <tr style={{ background: 'var(--b360-surface)', borderBottom: '2px solid var(--b360-border)' }}>
                      <th style={{ padding: '12px 16px', textAlign: 'left', fontSize: 11, fontWeight: 700, color: 'var(--b360-text-secondary)', textTransform: 'uppercase', letterSpacing: 0.5 }}>Branch Name</th>
                      <th style={{ padding: '12px 16px', textAlign: 'left', fontSize: 11, fontWeight: 700, color: 'var(--b360-text-secondary)', textTransform: 'uppercase', letterSpacing: 0.5 }}>Code (eTIMS)</th>
                      <th style={{ padding: '12px 16px', textAlign: 'left', fontSize: 11, fontWeight: 700, color: 'var(--b360-text-secondary)', textTransform: 'uppercase', letterSpacing: 0.5 }}>Location</th>
                      <th style={{ padding: '12px 16px', textAlign: 'left', fontSize: 11, fontWeight: 700, color: 'var(--b360-text-secondary)', textTransform: 'uppercase', letterSpacing: 0.5 }}>Contact</th>
                      <th style={{ padding: '12px 16px', textAlign: 'left', fontSize: 11, fontWeight: 700, color: 'var(--b360-text-secondary)', textTransform: 'uppercase', letterSpacing: 0.5 }}>Receipts</th>
                      <th style={{ padding: '12px 16px', textAlign: 'left', fontSize: 11, fontWeight: 700, color: 'var(--b360-text-secondary)', textTransform: 'uppercase', letterSpacing: 0.5 }}>Status</th>
                      <th style={{ padding: '12px 16px', textAlign: 'right', fontSize: 11, fontWeight: 700, color: 'var(--b360-text-secondary)', textTransform: 'uppercase', letterSpacing: 0.5 }}>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {branches.map(b => (
                      <tr key={b.id} style={{ borderBottom: '1px solid var(--b360-border)', transition: 'background 0.1s' }}>
                        <td style={{ padding: '14px 16px', fontSize: 13, color: 'var(--b360-text)' }}>
                          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                            <span style={{ fontWeight: 700 }}>{b.name}</span>
                            {b.isHeadOffice && (
                              <span style={{
                                fontSize: 10,
                                fontWeight: 800,
                                background: 'rgba(16, 185, 129, 0.15)',
                                color: 'var(--b360-green)',
                                border: '1px solid rgba(16, 185, 129, 0.3)',
                                borderRadius: 12,
                                padding: '2px 8px',
                                textTransform: 'uppercase',
                                letterSpacing: '0.04em'
                              }}>
                                HQ
                              </span>
                            )}
                          </div>
                        </td>
                        <td style={{ padding: '14px 16px', fontSize: 13 }}>
                          <span style={{
                            fontFamily: 'monospace',
                            fontWeight: 700,
                            padding: '3px 7px',
                            borderRadius: 6,
                            background: 'rgba(100, 116, 139, 0.1)',
                            color: '#334155'
                          }}>
                            {b.code}
                          </span>
                        </td>
                        <td style={{ padding: '14px 16px', fontSize: 13, color: 'var(--b360-text-secondary)' }}>
                          {[b.address, b.city, b.county].filter(Boolean).join(', ') || '—'}
                        </td>
                        <td style={{ padding: '14px 16px', fontSize: 12, color: 'var(--b360-text-secondary)' }}>
                          {b.phone && <div>{b.phone}</div>}
                          {b.email && <div style={{ fontSize: 11, color: '#94a3b8' }}>{b.email}</div>}
                          {!b.phone && !b.email && '—'}
                        </td>
                        <td style={{ padding: '14px 16px', fontSize: 12 }}>
                          {b.receiptHeader || b.receiptFooter ? (
                            <span style={{
                              fontSize: 11,
                              color: '#0284c7',
                              background: 'rgba(14, 165, 233, 0.1)',
                              padding: '2px 8px',
                              borderRadius: 6,
                              fontWeight: 600
                            }}>
                              Customized
                            </span>
                          ) : (
                            <span style={{ fontSize: 11, color: 'var(--b360-text-secondary)' }}>Default</span>
                          )}
                        </td>
                        <td style={{ padding: '14px 16px' }}>
                          <StatusBadge status={b.isActive ? 'ACTIVE' : 'INACTIVE'} />
                        </td>
                        <td style={{ padding: '14px 16px', textAlign: 'right' }}>
                          <div style={{ display: 'inline-flex', gap: 6, alignItems: 'center' }}>
                            <Btn small variant="secondary" onClick={() => openEditBranch(b)} icon={<Edit2 size={12} />}>
                              Edit
                            </Btn>
                            {!b.isHeadOffice && b.isActive && (
                              <Btn small variant="secondary" onClick={() => handleSetHeadOffice(b)} icon={<Check size={12} />}>
                                Make HQ
                              </Btn>
                            )}
                            {!b.isHeadOffice && (
                              <Btn small variant="danger" onClick={() => handleDeleteBranch(b)} icon={<Trash2 size={12} />}>
                                Deactivate
                              </Btn>
                            )}
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </Card>

          {/* Add / Edit Branch Modal */}
          {branchModalOpen && (
            <Modal
              title={editingBranch ? `Edit Branch · ${editingBranch.name}` : 'Add New Branch'}
              onClose={() => setBranchModalOpen(false)}
              footer={
                <>
                  <Btn variant="secondary" onClick={() => setBranchModalOpen(false)}>
                    Cancel
                  </Btn>
                  <Btn onClick={handleSaveBranch} disabled={branchSaving} icon={<Save size={14} />}>
                    {branchSaving ? 'Saving…' : editingBranch ? 'Update Branch' : 'Create Branch'}
                  </Btn>
                </>
              }
            >
              <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
                <Input
                  label="Branch Name *"
                  placeholder="e.g. Westlands Branch, Mombasa Road Depot"
                  value={branchForm.name}
                  onChange={v => setBranchForm(prev => ({ ...prev, name: v }))}
                />
                <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                  <Input
                    label="Branch Code (eTIMS bhfId)"
                    placeholder="e.g. MAIN, 01, WTL"
                    value={branchForm.code || ''}
                    onChange={v => setBranchForm(prev => ({ ...prev, code: v.toUpperCase() }))}
                  />
                  <span style={{ fontSize: 11, color: 'var(--b360-text-secondary)' }}>
                    Unique identifier for receipts and KRA eTIMS branch registration (00 = HQ, 01, 02, etc.).
                  </span>
                </div>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                  <Input
                    label="Phone Number"
                    placeholder="+254 7XX XXX XXX"
                    value={branchForm.phone || ''}
                    onChange={v => setBranchForm(prev => ({ ...prev, phone: v }))}
                  />
                  <Input
                    label="Email Address"
                    placeholder="branch@business.co.ke"
                    value={branchForm.email || ''}
                    onChange={v => setBranchForm(prev => ({ ...prev, email: v }))}
                  />
                </div>
                <Input
                  label="Physical Address"
                  placeholder="e.g. Sarit Centre, 2nd Floor"
                  value={branchForm.address || ''}
                  onChange={v => setBranchForm(prev => ({ ...prev, address: v }))}
                />
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                  <Input
                    label="City / Town"
                    placeholder="e.g. Nairobi, Mombasa, Kisumu"
                    value={branchForm.city || ''}
                    onChange={v => setBranchForm(prev => ({ ...prev, city: v }))}
                  />
                  <Input
                    label="County"
                    placeholder="e.g. Nairobi, Kiambu, Nakuru"
                    value={branchForm.county || ''}
                    onChange={v => setBranchForm(prev => ({ ...prev, county: v }))}
                  />
                </div>

                <div style={{
                  padding: 12,
                  background: 'var(--b360-surface)',
                  borderRadius: 8,
                  border: '1px solid var(--b360-border)',
                  marginTop: 4
                }}>
                  <Toggle
                    label="Primary Head Office (HQ)"
                    checked={branchForm.isHeadOffice || false}
                    onChange={v => setBranchForm(prev => ({ ...prev, isHeadOffice: v }))}
                    disabled={editingBranch?.isHeadOffice}
                  />
                  <div style={{ fontSize: 11, color: 'var(--b360-text-secondary)', marginTop: 4 }}>
                    {editingBranch?.isHeadOffice
                      ? 'This branch is currently the primary Head Office.'
                      : 'Setting this branch as Head Office will make it the default location for corporate reporting and main eTIMS registration.'}
                  </div>
                </div>

                <div style={{ borderTop: '1px solid var(--b360-border)', paddingTop: 12, marginTop: 4 }}>
                  <div style={{ fontWeight: 700, fontSize: 13, marginBottom: 8, color: 'var(--b360-text)' }}>
                    Branch Receipt Overrides (Optional)
                  </div>
                  <div style={{ fontSize: 12, color: 'var(--b360-text-secondary)', marginBottom: 12 }}>
                    Leave blank to use default business receipt headers and footers.
                  </div>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
                    <Input
                      label="Receipt Header"
                      placeholder="e.g. Welcome to Biashara360 - Westlands Branch"
                      value={branchForm.receiptHeader || ''}
                      onChange={v => setBranchForm(prev => ({ ...prev, receiptHeader: v }))}
                    />
                    <Input
                      label="Receipt Footer"
                      placeholder="e.g. Westlands Branch: Return window is 7 days."
                      value={branchForm.receiptFooter || ''}
                      onChange={v => setBranchForm(prev => ({ ...prev, receiptFooter: v }))}
                    />
                  </div>
                </div>
              </div>
            </Modal>
          )}
        </div>
      )}
        </main>
      </div>
    </div>
  )
}

export default SettingsPage

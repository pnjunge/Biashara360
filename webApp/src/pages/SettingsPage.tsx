import React, { useState, useEffect } from 'react'
import { useSearchParams, useNavigate } from 'react-router-dom'
import {
  Building2, Shield, Wifi, CreditCard, Lock, Bell, CheckCircle, AlertTriangle,
  Receipt, Save, ExternalLink, Zap, Key, RefreshCw, Layers
} from 'lucide-react'
import { PageHeader, Card, Btn, Input, Select } from '../components/ui'
import {
  settingsApi, businessApi, kraApi, authApi, BusinessProfileRequest,
  MpesaConfigResponse, SessionTimeoutConfig
} from '../services/api'
import { useAuth } from '../App'

type SettingsTab = 'general' | 'storefront' | 'cybersource' | 'kra' | 'mpesa' | 'security' | 'notifications'

export function SettingsPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const navigate = useNavigate()
  const { user } = useAuth()
  const isMerchantAdmin = (user?.role || '').toUpperCase() === 'ADMIN'

  const initialTab = (searchParams.get('tab') as SettingsTab) || 'general'
  const [activeTab, setActiveTab] = useState<SettingsTab>(initialTab)

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
  const [profileLoading, setProfileLoading] = useState(false)
  const [profileSaving, setProfileSaving] = useState(false)
  const [profileMsg, setProfileMsg] = useState<{ ok: boolean; text: string } | null>(null)

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

  // Load Tab-Specific Data
  useEffect(() => {
    if (activeTab === 'general' || activeTab === 'storefront' || activeTab === 'notifications') {
      setProfileLoading(true)
      businessApi.getProfile().then(res => {
        if (res.success && res.data) {
          const d = res.data
          setSubscriptionTier(d.subscriptionTier || 'FREEMIUM')
          setSubscriptionEnabled(d.subscriptionEnabled !== false)
          setProfile({
            name: d.name || '', owner: d.owner || '', phone: d.phone || '',
            email: d.email || '', type: d.type || '', county: d.county || '',
            address: d.address || '', kraPin: d.kraPin || '',
            paybillNumber: d.paybillNumber || '', accountNumber: d.accountNumber || '',
            receiptHeader: d.receiptHeader, receiptFooter: d.receiptFooter,
            receiptLogo: d.receiptLogo, receiptShowTax: d.receiptShowTax,
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

  const Section = ({ title, children }: { title: string; children: React.ReactNode }) => (
    <Card style={{ padding: 22, marginBottom: 16 }}>
      <h3 style={{ fontWeight: 700, marginBottom: 16, fontSize: 15 }}>{title}</h3>
      <div style={{ borderTop: '1px solid var(--b360-border)', paddingTop: 16, display: 'flex', flexDirection: 'column', gap: 14 }}>{children}</div>
    </Card>
  )

  const Toggle = ({ label, checked, onChange }: { label: string; checked: boolean; onChange: (v: boolean) => void }) => (
    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
      <span style={{ fontSize: 13 }}>{label}</span>
      <div onClick={() => onChange(!checked)} style={{
        width: 44, height: 24, borderRadius: 12, cursor: 'pointer', transition: 'background 0.2s',
        background: checked ? 'var(--b360-green)' : '#D1D5DB', position: 'relative'
      }}>
        <div style={{ position: 'absolute', top: 2, left: checked ? 22 : 2, width: 20, height: 20, borderRadius: '50%', background: 'white', transition: 'left 0.2s', boxShadow: '0 1px 3px rgba(0,0,0,0.2)' }} />
      </div>
    </div>
  )

  const tabs = [
    { key: 'storefront', label: 'Storefront', icon: <ExternalLink size={15} /> },
    { key: 'general', label: 'Store Profile', icon: <Building2 size={15} /> },
    { key: 'cybersource', label: 'CyberSource Card', icon: <CreditCard size={15} /> },
    { key: 'kra', label: 'KRA & eTIMS', icon: <Shield size={15} /> },
    { key: 'mpesa', label: 'M-Pesa Daraja', icon: <Wifi size={15} /> },
    { key: 'security', label: 'Security & Timeouts', icon: <Lock size={15} /> },
    { key: 'notifications', label: 'Notifications', icon: <Bell size={15} /> },
  ]

  return (
    <div className="fade-in" style={{ maxWidth: 760 }}>
      <PageHeader title="Unified Settings Hub" />

      {/* Navigation Tab Bar */}
      <div style={{
        display: 'flex', gap: 4, overflowX: 'auto', borderBottom: '2px solid var(--b360-border)',
        marginBottom: 20, paddingBottom: 2
      }}>
        {tabs.map(t => {
          const isActive = activeTab === t.key
          return (
            <button
              key={t.key}
              onClick={() => handleTabChange(t.key as SettingsTab)}
              style={{
                display: 'flex', alignItems: 'center', gap: 7, padding: '10px 16px',
                border: 'none', background: 'none', cursor: 'pointer',
                fontWeight: isActive ? 700 : 500, fontSize: 13,
                color: isActive ? 'var(--b360-green)' : 'var(--b360-text-secondary)',
                borderBottom: isActive ? '3px solid var(--b360-green)' : '3px solid transparent',
                marginBottom: '-3px', whiteSpace: 'nowrap', transition: 'all 0.15s ease-in-out'
              }}
            >
              {t.icon}
              {t.label}
            </button>
          )
        })}
      </div>

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

          {isMerchantAdmin && (
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

          <Section title="Two-Factor Authentication (2FA)">
            <Toggle label="Enable Two-Factor Authentication (2FA) for Admin Logins" checked={twoFA} onChange={setTwoFA} />
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

          <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
            <Btn icon={<Save size={14} />} onClick={handleSaveSecurity} disabled={secSaving}>
              {secSaving ? 'Saving Security…' : 'Save Security Policy'}
            </Btn>
          </div>
        </div>
      )}

      {/* ── TAB 6: NOTIFICATIONS & SUBSCRIPTION ── */}
      {activeTab === 'notifications' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
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
    </div>
  )
}

export default SettingsPage

import React, { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  MessageSquare, Check, ArrowRight, ArrowLeft, Copy, Globe, RefreshCw,
  Cpu, CheckCircle2, AlertCircle, Info, Settings, ShieldAlert, Zap,
  Play, Link as LinkIcon, ExternalLink
} from 'lucide-react'
import { socialApi, SocialChannel, MetaOnboardingConfiguration } from '../services/api'
import { PageHeader, Card, Btn, Input, ProgressBar, AlertBanner } from '../components/ui'

// ── Platform Brand Styling ───────────────────────────────────────────────────
const PLATFORMS = [
  {
    id: 'WHATSAPP',
    name: 'WhatsApp Business',
    color: '#25D366',
    bg: '#E8FBF0',
    icon: '💬',
    desc: 'Connect via Meta Cloud API to send automated catalogs, payment pings, and chat alerts.',
    docsUrl: 'https://developers.facebook.com/docs/whatsapp/cloud-api'
  },
  {
    id: 'INSTAGRAM',
    name: 'Instagram Professional',
    color: '#E1306C',
    bg: '#FDE8F0',
    icon: '📸',
    desc: 'Automate replies to direct messages (DMs) and post comments to boost story engagements.',
    docsUrl: 'https://developers.facebook.com/docs/instagram-api'
  },
  {
    id: 'FACEBOOK',
    name: 'Facebook Page',
    color: '#1877F2',
    bg: '#E7F0FE',
    icon: '👥',
    desc: 'Handle Messenger inquiries and sync comments from promotional posts seamlessly.',
    docsUrl: 'https://developers.facebook.com/docs/messenger-platform'
  },
  {
    id: 'TIKTOK',
    name: 'TikTok Business',
    color: '#000000',
    bg: '#F0F0F0',
    icon: '🎵',
    desc: 'Engage with customer inquiries on direct messages and sync TikTok Shop updates.',
    docsUrl: 'https://developers.tiktok.com/doc/business-api-direct-messages'
  }
]

// Steps for the Onboarding wizard
const STEPS = [
  { label: 'Select Channel' },
  { label: 'Connect Account' },
  { label: 'Webhook Setup' },
  { label: 'Verify Integration' },
  { label: 'AI Persona Setup' }
]

export default function SocialOnboardingPage() {
  const navigate = useNavigate()

  // Onboarding States
  const [currentStep, setCurrentStep] = useState(0)
  const [selectedPlatform, setSelectedPlatform] = useState<string>('')
  const [channels, setChannels] = useState<SocialChannel[]>([])
  const [loading, setLoading] = useState(true)
  const [metaConfiguration, setMetaConfiguration] = useState<MetaOnboardingConfiguration | null>(null)
  const [metaAuthorizationCode, setMetaAuthorizationCode] = useState('')

  // Credentials form
  const [channelName, setChannelName] = useState('')
  const [externalId, setExternalId] = useState('')
  const [wabaId, setWabaId] = useState('')
  const [metaBusinessId, setMetaBusinessId] = useState('')
  const [phoneNumber, setPhoneNumber] = useState('')
  const [accessToken, setAccessToken] = useState('')

  // AI Prompt settings
  const [autoReplyEnabled, setAutoReplyEnabled] = useState(true)
  const [aiPersonaPrompt, setAiPersonaPrompt] = useState(
    'Hujambo! I am the automated customer assistant for our business. I speak a mix of English and Swahili (Sheng). I am here to help you browse products, check pricing, and complete your purchase. Be friendly and keep responses short!'
  )

  // Action status states
  const [savingChannel, setSavingChannel] = useState(false)
  const [createdChannel, setCreatedChannel] = useState<SocialChannel | null>(null)
  const [copiedText, setCopiedText] = useState<'url' | 'token' | null>(null)
  const [errorMsg, setErrorMsg] = useState('')

  // Real provider connection verification
  const [verificationStage, setVerificationStage] = useState<'idle' | 'testing' | 'success' | 'failed'>('idle')
  const [verificationLogs, setVerificationLogs] = useState<string[]>([])
  const [verifyProgress, setVerifyProgress] = useState(0)

  useEffect(() => {
    loadChannels()
    socialApi.getMetaConfiguration()
      .then(res => setMetaConfiguration(res.data || null))
      .catch(() => setMetaConfiguration(null))
  }, [])

  useEffect(() => {
    const receiveMetaSignup = (event: MessageEvent) => {
      if (event.origin !== 'https://www.facebook.com' && event.origin !== 'https://web.facebook.com') return
      let payload: any = event.data
      if (typeof payload === 'string') {
        try { payload = JSON.parse(payload) } catch { return }
      }
      if (payload?.type !== 'WA_EMBEDDED_SIGNUP') return
      const data = payload.data || {}
      if (data.waba_id) setWabaId(String(data.waba_id))
      if (data.phone_number_id) setExternalId(String(data.phone_number_id))
      if (data.business_id) setMetaBusinessId(String(data.business_id))
      setErrorMsg('')
    }
    window.addEventListener('message', receiveMetaSignup)
    return () => window.removeEventListener('message', receiveMetaSignup)
  }, [])

  useEffect(() => {
    if (
      selectedPlatform !== 'WHATSAPP' ||
      !metaAuthorizationCode ||
      !wabaId ||
      !externalId ||
      !metaBusinessId ||
      savingChannel ||
      createdChannel
    ) return

    const complete = async () => {
      const code = metaAuthorizationCode
      setMetaAuthorizationCode('')
      setSavingChannel(true)
      setErrorMsg('')
      try {
        const res = await socialApi.completeMetaEmbeddedSignup({
          code,
          wabaId,
          phoneNumberId: externalId,
          metaBusinessId,
          channelName
        })
        if (!res.success || !res.data) {
          setErrorMsg(res.message || 'Meta could not complete the WhatsApp connection.')
          return
        }
        setCreatedChannel(res.data)
        setChannels(prev => [...prev.filter(channel => channel.id !== res.data!.id), res.data!])
        setCurrentStep(3)
      } catch (e: any) {
        setErrorMsg(e.response?.data?.message || 'Meta could not complete the WhatsApp connection.')
      } finally {
        setSavingChannel(false)
      }
    }
    complete()
  }, [
    selectedPlatform,
    metaAuthorizationCode,
    wabaId,
    externalId,
    metaBusinessId,
    channelName,
    savingChannel,
    createdChannel
  ])

  const launchEmbeddedSignup = () => {
    if (!metaConfiguration?.configured || !metaConfiguration.appId || !metaConfiguration.configurationId) {
      setErrorMsg('Meta Embedded Signup is not configured for this deployment.')
      return
    }
    const start = () => (window as any).FB.login(
      (response: any) => {
        const code = response?.authResponse?.code
        if (code) {
          setMetaAuthorizationCode(String(code))
          setErrorMsg('')
        } else {
          setErrorMsg('Meta sign-in was cancelled or did not grant access.')
        }
      },
      {
        config_id: metaConfiguration.configurationId,
        response_type: 'code',
        override_default_response_type: true,
        extras: { setup: {} }
      }
    )
    if ((window as any).FB) {
      start()
      return
    }
    ;(window as any).fbAsyncInit = () => {
      ;(window as any).FB.init({
        appId: metaConfiguration.appId,
        cookie: true,
        xfbml: true,
        version: metaConfiguration.graphApiVersion || 'v25.0'
      })
      start()
    }
    const existing = document.getElementById('facebook-jssdk')
    if (!existing) {
      const script = document.createElement('script')
      script.id = 'facebook-jssdk'
      script.async = true
      script.defer = true
      script.crossOrigin = 'anonymous'
      script.src = 'https://connect.facebook.net/en_US/sdk.js'
      document.body.appendChild(script)
    }
  }

  const loadChannels = async () => {
    setLoading(true)
    try {
      const res = await socialApi.getChannels()
      if (res.success && res.data) {
        setChannels(res.data)
      }
    } catch (_) {
      // ignore
    } finally {
      setLoading(false)
    }
  }

  const copyToClipboard = (text: string, type: 'url' | 'token') => {
    navigator.clipboard.writeText(text)
    setCopiedText(type)
    setTimeout(() => setCopiedText(null), 2000)
  }

  const handleSelectPlatform = (platformId: string) => {
    setSelectedPlatform(platformId)
    // Pre-fill name suggestion
    setChannelName(`${PLATFORMS.find(p => p.id === platformId)?.name} Integration`)
    setCurrentStep(1)
  }

  const handleSaveCredentials = async () => {
    if (!channelName.trim()) {
      setErrorMsg('Please specify a display name for this channel.')
      return
    }
    if (!externalId.trim()) {
      setErrorMsg(selectedPlatform === 'WHATSAPP' ? 'Phone Number ID is required.' : 'External ID is required.')
      return
    }
    if (selectedPlatform === 'WHATSAPP' && (!wabaId.trim() || !metaBusinessId.trim())) {
      setErrorMsg('WABA ID and Meta Business ID are required.')
      return
    }
    if (selectedPlatform !== 'WHATSAPP' && !accessToken.trim()) {
      setErrorMsg('Developer Access Token is required.')
      return
    }

    setSavingChannel(true)
    setErrorMsg('')
    try {
      const res = await socialApi.createChannel({
        platform: selectedPlatform,
        channelName,
        externalId,
        phoneNumber: selectedPlatform === 'WHATSAPP' ? phoneNumber : null,
        accessToken: selectedPlatform === 'WHATSAPP' ? '' : accessToken,
        wabaId: selectedPlatform === 'WHATSAPP' ? wabaId : null,
        phoneNumberId: selectedPlatform === 'WHATSAPP' ? externalId : null,
        metaBusinessId: selectedPlatform === 'WHATSAPP' ? metaBusinessId : null,
        autoReplyEnabled: false, // Save with false, activate during Step 5 AI Persona
        aiPersonaPrompt: ''
      })

      if (res.success && res.data) {
        setCreatedChannel(res.data)
        setChannels(prev => [...prev, res.data!])
        setCurrentStep(2) // Move to Webhook Setup
      } else {
        setErrorMsg(res.message || 'Failed to register credentials.')
      }
    } catch (e: any) {
      setErrorMsg(e.response?.data?.message || 'Network error occurred while connecting channel.')
    } finally {
      setSavingChannel(false)
    }
  }

  const runVerificationScan = async () => {
    if (!createdChannel) return
    setVerificationStage('testing')
    setVerifyProgress(0.25)
    setVerificationLogs(['Checking the merchant authorization with Meta...'])
    try {
      const res = await socialApi.verifyChannel(createdChannel.id)
      if (res.success && res.data?.connected) {
        setVerificationLogs([
          'Merchant authorization confirmed.',
          'WhatsApp Business phone number is accessible.',
          'Webhook subscription is active.',
          'Connection is ready.'
        ])
        setVerificationStage('success')
        setVerifyProgress(1)
      } else {
        setVerificationLogs([res.message || 'Meta reported that the connection needs attention.'])
        setVerificationStage('failed')
        setVerifyProgress(1)
      }
    } catch (e: any) {
      setVerificationLogs([e.response?.data?.message || 'Unable to verify the Meta connection.'])
      setVerificationStage('failed')
      setVerifyProgress(1)
    }
  }

  const handleSaveAIPersona = async () => {
    if (!createdChannel) return
    setSavingChannel(true)
    setErrorMsg('')
    try {
      const res = await socialApi.updateChannelSettings(createdChannel.id, {
        channelName: createdChannel.channelName,
        autoReplyEnabled,
        aiPersonaPrompt
      })

      if (res.success) {
        // Complete wizard, go to inbox
        navigate('/social')
      } else {
        setErrorMsg(res.message || 'Failed to update AI Persona.')
      }
    } catch (e: any) {
      setErrorMsg(e.response?.data?.message || 'Network error occurred saving AI persona settings.')
    } finally {
      setSavingChannel(false)
    }
  }

  const handleBack = () => {
    if (currentStep > 0) {
      setCurrentStep(currentStep - 1)
      setErrorMsg('')
    }
  }

  const platformMeta = PLATFORMS.find(p => p.id === selectedPlatform)
  const overallProgress = (channels.length / PLATFORMS.length)

  return (
    <div className="fade-in" style={{ display: 'flex', flexDirection: 'column', gap: 24, maxWidth: 1000, margin: '0 auto', paddingBottom: 40 }}>
      {/* ── Page Header ── */}
      <PageHeader
        title="Social Media Commerce Setup"
        action={
          <Btn variant="secondary" onClick={() => navigate('/social')}>
            Go to Inbox
          </Btn>
        }
      />

      {/* ── Setup Summary Dashboard Card ── */}
      <Card style={{ padding: 24, display: 'flex', flexDirection: 'column', gap: 16, background: 'linear-gradient(135deg, #ffffff 0%, #f9fbfd 100%)' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 12 }}>
          <div>
            <h3 style={{ fontSize: 16, fontWeight: 700, margin: 0, color: 'var(--b360-text)' }}>Integration Status</h3>
            <span style={{ fontSize: 13, color: 'var(--b360-text-secondary)' }}>
              Configure platforms to allow AI-guided orders and instant Mpesa checkouts inside DMs.
            </span>
          </div>
          <div style={{ textAlign: 'right', minWidth: 150 }}>
            <span style={{ fontSize: 13, fontWeight: 700, color: 'var(--b360-green)' }}>
              {channels.length} of {PLATFORMS.length} Channels Connected
            </span>
            <div style={{ marginTop: 6 }}>
              <ProgressBar value={overallProgress} />
            </div>
          </div>
        </div>

        {/* Small platforms indicator badges */}
        <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap', borderTop: '1px solid var(--b360-border)', paddingTop: 16 }}>
          {PLATFORMS.map(p => {
            const isConnected = channels.some(c => c.platform === p.id && c.isActive)
            return (
              <div
                key={p.id}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 8,
                  padding: '6px 12px',
                  borderRadius: 20,
                  fontSize: 12,
                  fontWeight: 600,
                  background: isConnected ? p.bg : 'var(--b360-surface)',
                  color: isConnected ? p.color : 'var(--b360-text-secondary)',
                  border: `1px solid ${isConnected ? p.color + '33' : 'var(--b360-border)'}`
                }}
              >
                <span>{p.icon}</span>
                <span>{p.name}</span>
                <span
                  style={{
                    width: 6,
                    height: 6,
                    borderRadius: '50%',
                    background: isConnected ? 'var(--b360-green)' : '#94A3B8',
                    display: 'inline-block'
                  }}
                />
              </div>
            )
          })}
        </div>
      </Card>

      {/* ── Stepper Indicator ── */}
      {currentStep > 0 && (
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '0 8px', overflowX: 'auto', gap: 16 }}>
          {STEPS.map((step, idx) => {
            const isActive = idx === currentStep
            const isCompleted = idx < currentStep
            return (
              <div
                key={idx}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 8,
                  opacity: isActive || isCompleted ? 1 : 0.5,
                  transition: 'opacity 0.2s',
                  whiteSpace: 'nowrap'
                }}
              >
                <div
                  style={{
                    width: 24,
                    height: 24,
                    borderRadius: '50%',
                    background: isCompleted ? 'var(--b360-green)' : isActive ? 'var(--b360-blue)' : '#e2e8f0',
                    color: isCompleted || isActive ? 'white' : 'var(--b360-text-secondary)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    fontSize: 11,
                    fontWeight: 700
                  }}
                >
                  {isCompleted ? <Check size={12} strokeWidth={3} /> : idx + 1}
                </div>
                <span style={{ fontSize: 13, fontWeight: isActive ? 700 : 500, color: isActive ? 'var(--b360-text)' : 'var(--b360-text-secondary)' }}>
                  {step.label}
                </span>
                {idx < STEPS.length - 1 && (
                  <div style={{ width: 32, height: 1, background: '#cbd5e1', marginLeft: 8 }} />
                )}
              </div>
            )
          })}
        </div>
      )}

      {/* ── STEP 0: Select Platform ── */}
      {currentStep === 0 && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          <h2 style={{ fontSize: 18, fontWeight: 800, color: 'var(--b360-text)', margin: 0 }}>
            Choose a Social Platform to Connect
          </h2>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: 16 }}>
            {PLATFORMS.map(p => {
              const isConnected = channels.some(c => c.platform === p.id && c.isActive)
              return (
                <div
                  key={p.id}
                  onClick={() => handleSelectPlatform(p.id)}
                  style={{
                    background: 'white',
                    borderRadius: 'var(--radius-md)',
                    border: `1.5px solid ${isConnected ? p.color : 'var(--b360-border)'}`,
                    padding: 24,
                    cursor: 'pointer',
                    transition: 'transform 0.2s, box-shadow 0.2s',
                    position: 'relative',
                    overflow: 'hidden',
                    display: 'flex',
                    flexDirection: 'column',
                    justifyContent: 'space-between',
                    boxShadow: 'var(--shadow-sm)'
                  }}
                  onMouseEnter={e => {
                    e.currentTarget.style.transform = 'translateY(-2px)'
                    e.currentTarget.style.boxShadow = 'var(--shadow-md)'
                  }}
                  onMouseLeave={e => {
                    e.currentTarget.style.transform = 'translateY(0)'
                    e.currentTarget.style.boxShadow = 'var(--shadow-sm)'
                  }}
                >
                  {isConnected && (
                    <div
                      style={{
                        position: 'absolute',
                        top: 0,
                        right: 0,
                        background: p.color,
                        color: 'white',
                        padding: '4px 10px',
                        fontSize: 10,
                        fontWeight: 700,
                        borderBottomLeftRadius: 8
                      }}
                    >
                      CONNECTED
                    </div>
                  )}
                  <div>
                    <div
                      style={{
                        width: 48,
                        height: 48,
                        background: p.bg,
                        borderRadius: 12,
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        fontSize: 24,
                        marginBottom: 16
                      }}
                    >
                      {p.icon}
                    </div>
                    <h3 style={{ fontSize: 16, fontWeight: 700, margin: '0 0 8px 0', color: 'var(--b360-text)' }}>
                      {p.name}
                    </h3>
                    <p style={{ fontSize: 12, color: 'var(--b360-text-secondary)', lineHeight: 1.5, margin: 0 }}>
                      {p.desc}
                    </p>
                  </div>

                  <div style={{ marginTop: 24, display: 'flex', alignItems: 'center', gap: 6, fontSize: 13, fontWeight: 700, color: p.color }}>
                    <span>{isConnected ? 'Add Another Account' : 'Connect Now'}</span>
                    <ArrowRight size={14} />
                  </div>
                </div>
              )
            })}
          </div>
        </div>
      )}

      {/* ── STEP 1: Enter Credentials ── */}
      {currentStep === 1 && platformMeta && (
        <Card style={{ padding: 28, display: 'flex', flexDirection: 'column', gap: 20 }}>
          <div style={{ display: 'flex', gap: 16, alignItems: 'center', borderBottom: '1px solid var(--b360-border)', paddingBottom: 16 }}>
            <div style={{ width: 40, height: 40, borderRadius: 10, background: platformMeta.bg, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 20 }}>
              {platformMeta.icon}
            </div>
            <div>
              <h2 style={{ fontSize: 16, fontWeight: 800, margin: 0, color: 'var(--b360-text)' }}>
                {selectedPlatform === 'WHATSAPP' ? 'Connect WhatsApp with Meta' : `Set Up Credentials for ${platformMeta.name}`}
              </h2>
              <span style={{ fontSize: 12, color: 'var(--b360-text-secondary)' }}>
                {selectedPlatform === 'WHATSAPP'
                  ? 'Meta securely authorizes your business account. No tokens or technical identifiers need to be copied.'
                  : 'Input API and identifier credentials generated from your developer profile.'}
              </span>
            </div>
          </div>

          {errorMsg && <AlertBanner message={errorMsg} icon={<AlertCircle size={16} />} color="var(--b360-red)" />}

          {selectedPlatform === 'WHATSAPP' && (
            <div style={{ padding: 16, border: '1px solid var(--b360-border)', borderRadius: 10, background: 'var(--b360-surface)' }}>
              <div style={{ fontWeight: 700, fontSize: 13, marginBottom: 8 }}>Meta Embedded Signup</div>
              <div style={{ fontSize: 12, color: 'var(--b360-text-secondary)', marginBottom: 12 }}>
                Sign in with Meta, select your business and phone number, then approve Biashara360. The connection is completed automatically.
              </div>
              {!metaConfiguration?.configured && metaConfiguration && (
                <AlertBanner
                  message={`Embedded Signup needs administrator configuration: ${metaConfiguration.missing.join(', ')}`}
                  icon={<ShieldAlert size={16} />}
                  color="var(--b360-red)"
                />
              )}
              <div style={{ marginTop: 12 }}>
                <Btn onClick={launchEmbeddedSignup} disabled={savingChannel || !metaConfiguration?.configured}>
                  {savingChannel ? 'Completing Connection...' : 'Continue with Meta'}
                </Btn>
              </div>
            </div>
          )}

          {selectedPlatform !== 'WHATSAPP' && (
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 20, flexWrap: 'wrap' }}>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
              <Input
                label="Integration Display Name *"
                placeholder="e.g. Primary Customer Line"
                value={channelName}
                onChange={setChannelName}
              />

              <Input
                label={
                  selectedPlatform === 'WHATSAPP'
                    ? 'Phone Number ID *'
                    : selectedPlatform === 'FACEBOOK' || selectedPlatform === 'INSTAGRAM'
                    ? 'Meta Page/Account ID *'
                    : 'TikTok Client App ID *'
                }
                placeholder="e.g. 102938475610293"
                value={externalId}
                onChange={setExternalId}
              />

            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 5 }}>
                <label style={{ fontSize: 12, fontWeight: 600, color: 'var(--b360-text-secondary)' }}>
                  Developer System User/Page Token *
                </label>
                <textarea
                  placeholder="Paste your access token here..."
                  value={accessToken}
                  onChange={e => setAccessToken(e.target.value)}
                  style={{
                    padding: '10px 14px',
                    border: '1px solid var(--b360-border)',
                    borderRadius: 'var(--radius-sm)',
                    fontSize: 13,
                    outline: 'none',
                    fontFamily: 'monospace',
                    background: 'white',
                    color: 'var(--b360-text)',
                    height: 106,
                    resize: 'none'
                  }}
                />
                <span style={{ fontSize: 11, color: 'var(--b360-text-secondary)', display: 'flex', alignItems: 'center', gap: 4, marginTop: 4 }}>
                  <Info size={12} /> Make sure this token has permanent expiry (system user token) so connection does not expire.
                </span>
              </div>
            </div>
          </div>
          )}

          <div style={{ borderTop: '1px solid var(--b360-border)', paddingTop: 20, display: 'flex', justifyContent: 'space-between' }}>
            <Btn variant="secondary" onClick={() => setCurrentStep(0)} icon={<ArrowLeft size={14} />}>
              Back
            </Btn>
            {selectedPlatform !== 'WHATSAPP' && (
              <Btn onClick={handleSaveCredentials} disabled={savingChannel}>
                {savingChannel ? 'Saving Details...' : 'Save & Continue'}
              </Btn>
            )}
          </div>
        </Card>
      )}

      {/* ── STEP 2: Webhook Configuration ── */}
      {currentStep === 2 && createdChannel && platformMeta && (
        <Card style={{ padding: 28, display: 'flex', flexDirection: 'column', gap: 20 }}>
          <div style={{ display: 'flex', gap: 16, alignItems: 'center', borderBottom: '1px solid var(--b360-border)', paddingBottom: 16 }}>
            <div style={{ width: 40, height: 40, borderRadius: 10, background: platformMeta.bg, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 20 }}>
              {platformMeta.icon}
            </div>
            <div>
              <h2 style={{ fontSize: 16, fontWeight: 800, margin: 0, color: 'var(--b360-text)' }}>
                Configure Webhooks in Meta Developers Console
              </h2>
              <span style={{ fontSize: 12, color: 'var(--b360-text-secondary)' }}>
                Input these webhook details in the App Dashboard to route inbound customer queries into Biashara360.
              </span>
            </div>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1.2fr 0.8fr', gap: 24 }}>
            {/* Values display */}
            <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
              <div style={{ background: 'var(--b360-surface)', padding: 16, borderRadius: 8, display: 'flex', flexDirection: 'column', gap: 12 }}>
                <div>
                  <label style={{ fontSize: 11, fontWeight: 700, color: 'var(--b360-text-secondary)', textTransform: 'uppercase', display: 'block', marginBottom: 6 }}>
                    Webhook URL (Callback Endpoint)
                  </label>
                  <div style={{ display: 'flex', gap: 8 }}>
                    <input
                      readOnly
                      value={createdChannel.webhookUrl}
                      style={{ flex: 1, padding: 8, background: '#f8fafc', border: '1px solid var(--b360-border)', borderRadius: 6, fontSize: 12, fontFamily: 'monospace' }}
                    />
                    <Btn variant="secondary" small onClick={() => copyToClipboard(createdChannel.webhookUrl, 'url')}>
                      {copiedText === 'url' ? 'Copied' : <Copy size={14} />}
                    </Btn>
                  </div>
                </div>

                <div>
                  <label style={{ fontSize: 11, fontWeight: 700, color: 'var(--b360-text-secondary)', textTransform: 'uppercase', display: 'block', marginBottom: 6 }}>
                    Webhook Verify Token
                  </label>
                  <div style={{ display: 'flex', gap: 8 }}>
                    <input
                      readOnly
                      value={createdChannel.webhookVerifyToken}
                      style={{ flex: 1, padding: 8, background: '#f8fafc', border: '1px solid var(--b360-border)', borderRadius: 6, fontSize: 12, fontFamily: 'monospace' }}
                    />
                    <Btn variant="secondary" small onClick={() => copyToClipboard(createdChannel.webhookVerifyToken, 'token')}>
                      {copiedText === 'token' ? 'Copied' : <Copy size={14} />}
                    </Btn>
                  </div>
                </div>
              </div>

              <div style={{ background: '#f8fafc', padding: 16, borderRadius: 8, borderLeft: '4px solid var(--b360-blue)' }}>
                <span style={{ fontSize: 13, fontWeight: 700, display: 'block', marginBottom: 6, color: 'var(--b360-text)' }}>
                  💡 Next Setup Action
                </span>
                <p style={{ fontSize: 12, color: 'var(--b360-text-secondary)', lineHeight: 1.5, margin: 0 }}>
                  Copy these configurations, log into your Meta App dashboard, add the **{selectedPlatform === 'WHATSAPP' ? 'WhatsApp' : 'Webhooks'}** product, and paste these values into Webhook configuration setup.
                </p>
              </div>
            </div>

            {/* Checklist guide */}
            <div style={{ borderLeft: '1px solid var(--b360-border)', paddingLeft: 24 }}>
              <h4 style={{ fontSize: 13, fontWeight: 700, margin: '0 0 12px 0', color: 'var(--b360-text)' }}>
                Meta Console Checklist
              </h4>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
                {[
                  'Open Meta Developers Console',
                  'Select/Create your Commerce App',
                  'Configure Webhook product',
                  'Paste URL & Verification Token',
                  'Verify and Save settings',
                  selectedPlatform === 'WHATSAPP' ? 'Subscribe to "messages" field' : 'Subscribe to messaging pings'
                ].map((item, idx) => (
                  <div key={idx} style={{ display: 'flex', gap: 10, alignItems: 'center' }}>
                    <div style={{ background: 'var(--b360-green-bg)', color: 'var(--b360-green)', width: 18, height: 18, borderRadius: '50%', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                      <Check size={10} strokeWidth={3} />
                    </div>
                    <span style={{ fontSize: 12, color: 'var(--b360-text)' }}>{item}</span>
                  </div>
                ))}
              </div>

              <div style={{ marginTop: 24 }}>
                <a
                  href={platformMeta.docsUrl}
                  target="_blank"
                  rel="noreferrer"
                  style={{ display: 'inline-flex', alignItems: 'center', gap: 6, fontSize: 12, fontWeight: 600, color: 'var(--b360-blue)', textDecoration: 'none' }}
                >
                  <span>Open Developer Docs</span>
                  <ExternalLink size={12} />
                </a>
              </div>
            </div>
          </div>

          <div style={{ borderTop: '1px solid var(--b360-border)', paddingTop: 20, display: 'flex', justifyContent: 'space-between' }}>
            <Btn variant="secondary" onClick={handleBack} icon={<ArrowLeft size={14} />}>
              Back
            </Btn>
            <Btn onClick={() => setCurrentStep(3)} icon={<ArrowRight size={14} />}>
              I have configured Webhooks
            </Btn>
          </div>
        </Card>
      )}

      {/* ── STEP 3: Verify Integration ── */}
      {currentStep === 3 && createdChannel && platformMeta && (
        <Card style={{ padding: 28, display: 'flex', flexDirection: 'column', gap: 20 }}>
          <div style={{ display: 'flex', gap: 16, alignItems: 'center', borderBottom: '1px solid var(--b360-border)', paddingBottom: 16 }}>
            <div style={{ width: 40, height: 40, borderRadius: 10, background: platformMeta.bg, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 20 }}>
              {platformMeta.icon}
            </div>
            <div>
              <h2 style={{ fontSize: 16, fontWeight: 800, margin: 0, color: 'var(--b360-text)' }}>
                Verify and Test Channel Connection
              </h2>
              <span style={{ fontSize: 12, color: 'var(--b360-text-secondary)' }}>
                Confirm that Meta still authorizes this WhatsApp business account and phone number.
              </span>
            </div>
          </div>

          {verificationStage === 'idle' && (
            <div style={{ padding: '32px 0', textAlign: 'center', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 16 }}>
              <div style={{ width: 64, height: 64, borderRadius: '50%', background: 'var(--b360-blue-bg)', color: 'var(--b360-blue)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                <Zap size={32} />
              </div>
              <div>
                <h3 style={{ fontSize: 15, fontWeight: 700, margin: '0 0 6px 0', color: 'var(--b360-text)' }}>
                  Ready to Verify
                </h3>
                <p style={{ fontSize: 12, color: 'var(--b360-text-secondary)', maxWidth: 400, margin: '0 auto', lineHeight: 1.5 }}>
                  Biashara360 will securely query Meta for the selected business phone number. No test message is sent to customers.
                </p>
              </div>
              <Btn onClick={runVerificationScan} icon={<Play size={14} />}>
                Verify Connection
              </Btn>
            </div>
          )}

          {verificationStage === 'testing' && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span style={{ fontSize: 13, fontWeight: 700, color: 'var(--b360-text)' }}>
                  Verifying Meta Authorization...
                </span>
                <span style={{ fontSize: 12, fontWeight: 600, color: 'var(--b360-blue)' }}>
                  {Math.round(verifyProgress * 100)}%
                </span>
              </div>
              <ProgressBar value={verifyProgress} color="var(--b360-blue)" />

              {/* Console logs box */}
              <div
                style={{
                  background: '#0F172A',
                  borderRadius: 8,
                  padding: 16,
                  fontFamily: 'monospace',
                  fontSize: 12,
                  color: '#38BDF8',
                  minHeight: 150,
                  maxHeight: 250,
                  overflowY: 'auto',
                  display: 'flex',
                  flexDirection: 'column',
                  gap: 8
                }}
              >
                {verificationLogs.map((log, idx) => (
                  <div key={idx} style={{ display: 'flex', gap: 8 }}>
                    <span style={{ color: '#64748B' }}>&gt;</span>
                    <span>{log}</span>
                  </div>
                ))}
                <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                  <RefreshCw size={12} className="spin" style={{ color: '#E2E8F0' }} />
                  <span style={{ color: '#64748B' }}>Working...</span>
                </div>
              </div>
            </div>
          )}

          {verificationStage === 'success' && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 24, alignItems: 'center', padding: '24px 0' }}>
              <div style={{ width: 64, height: 64, borderRadius: '50%', background: 'var(--b360-green-bg)', color: 'var(--b360-green)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                <CheckCircle2 size={36} />
              </div>
              <div style={{ textAlign: 'center' }}>
                <h3 style={{ fontSize: 18, fontWeight: 800, color: 'var(--b360-green)', margin: '0 0 6px 0' }}>
                  WhatsApp Connected
                </h3>
                <p style={{ fontSize: 13, color: 'var(--b360-text-secondary)', maxWidth: 450, margin: '0 auto', lineHeight: 1.5 }}>
                  Meta confirmed access to your WhatsApp Business phone number and Biashara360 subscribed the account to webhook updates.
                </p>
              </div>

              <div style={{ borderTop: '1px solid var(--b360-border)', width: '100%', paddingTop: 20, display: 'flex', justifyContent: 'center' }}>
                <Btn onClick={() => setCurrentStep(4)} icon={<ArrowRight size={14} />}>
                  Proceed to AI Setup
                </Btn>
              </div>
            </div>
          )}

          {verificationStage === 'failed' && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 16, alignItems: 'center', padding: '24px 0' }}>
              <AlertCircle size={40} color="var(--b360-red)" />
              <h3 style={{ margin: 0, fontSize: 16 }}>Connection needs attention</h3>
              <p style={{ margin: 0, color: 'var(--b360-text-secondary)', fontSize: 13, textAlign: 'center' }}>
                {verificationLogs[0] || 'Meta could not verify this connection.'}
              </p>
              <Btn onClick={runVerificationScan}>Try Again</Btn>
            </div>
          )}

          {currentStep === 3 && verificationStage !== 'success' && (
            <div style={{ borderTop: '1px solid var(--b360-border)', paddingTop: 20, display: 'flex', justifyContent: 'space-between' }}>
              <Btn variant="secondary" onClick={handleBack} icon={<ArrowLeft size={14} />}>
                Back
              </Btn>
              {selectedPlatform !== 'WHATSAPP' && (
                <Btn variant="secondary" onClick={() => setCurrentStep(4)} icon={<ArrowRight size={14} />}>
                  Skip Verification
                </Btn>
              )}
            </div>
          )}
        </Card>
      )}

      {/* ── STEP 4: AI Persona Configuration ── */}
      {currentStep === 4 && createdChannel && platformMeta && (
        <Card style={{ padding: 28, display: 'flex', flexDirection: 'column', gap: 20 }}>
          <div style={{ display: 'flex', gap: 16, alignItems: 'center', borderBottom: '1px solid var(--b360-border)', paddingBottom: 16 }}>
            <div style={{ width: 40, height: 40, borderRadius: 10, background: 'var(--b360-green-bg)', color: 'var(--b360-green)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 20 }}>
              <Cpu size={22} />
            </div>
            <div>
              <h2 style={{ fontSize: 16, fontWeight: 800, margin: 0, color: 'var(--b360-text)' }}>
                Configure AI Sales Assistant
              </h2>
              <span style={{ fontSize: 12, color: 'var(--b360-text-secondary)' }}>
                Define how the auto-reply sales assistant communicates with your buyers.
              </span>
            </div>
          </div>

          {errorMsg && <AlertBanner message={errorMsg} icon={<AlertCircle size={16} />} color="var(--b360-red)" />}

          <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
            {/* Auto reply toggler */}
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', background: 'var(--b360-surface)', padding: '14px 20px', borderRadius: 8 }}>
              <div>
                <span style={{ fontSize: 13, fontWeight: 700, display: 'block', color: 'var(--b360-text)' }}>
                  Enable AI Auto-Replies
                </span>
                <span style={{ fontSize: 11, color: 'var(--b360-text-secondary)' }}>
                  When enabled, AI will suggest and trigger automatic replies when buyers ask about catalog items.
                </span>
              </div>
              <div
                onClick={() => setAutoReplyEnabled(!autoReplyEnabled)}
                style={{
                  width: 48,
                  height: 26,
                  borderRadius: 13,
                  cursor: 'pointer',
                  background: autoReplyEnabled ? 'var(--b360-green)' : '#CBD5E1',
                  position: 'relative',
                  transition: 'background 0.2s'
                }}
              >
                <div
                  style={{
                    position: 'absolute',
                    top: 3,
                    left: autoReplyEnabled ? 25 : 3,
                    width: 20,
                    height: 20,
                    borderRadius: '50%',
                    background: 'white',
                    boxShadow: '0 1px 3px rgba(0,0,0,0.15)',
                    transition: 'left 0.2s'
                  }}
                />
              </div>
            </div>

            {/* Persona prompt */}
            {autoReplyEnabled && (
              <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
                <label style={{ fontSize: 12, fontWeight: 600, color: 'var(--b360-text-secondary)' }}>
                  AI Assistant Persona / Tone Prompt
                </label>
                <textarea
                  value={aiPersonaPrompt}
                  onChange={e => setAiPersonaPrompt(e.target.value)}
                  style={{
                    padding: '12px 16px',
                    border: '1px solid var(--b360-border)',
                    borderRadius: 'var(--radius-sm)',
                    fontSize: 13,
                    outline: 'none',
                    fontFamily: 'inherit',
                    background: 'white',
                    color: 'var(--b360-text)',
                    height: 120,
                    resize: 'vertical',
                    lineHeight: 1.5
                  }}
                />
                <span style={{ fontSize: 11, color: 'var(--b360-text-secondary)' }}>
                  💡 Custom prompts direct the LLM. Specify languages, friendly phrases, store policies, or local slang (Sheng).
                </span>
              </div>
            )}
          </div>

          <div style={{ borderTop: '1px solid var(--b360-border)', paddingTop: 20, display: 'flex', justifyContent: 'space-between' }}>
            <Btn variant="secondary" onClick={handleBack} icon={<ArrowLeft size={14} />}>
              Back
            </Btn>
            <Btn onClick={handleSaveAIPersona} disabled={savingChannel}>
              {savingChannel ? 'Activating Channel...' : 'Activate & Finish Setup'}
            </Btn>
          </div>
        </Card>
      )}

      {/* ── Injection Styles for Animation ── */}
      <style>{`
        @keyframes spin {
          0% { transform: rotate(0deg); }
          100% { transform: rotate(360deg); }
        }
        .spin {
          animation: spin 1s linear infinite;
        }
      `}</style>
    </div>
  )
}

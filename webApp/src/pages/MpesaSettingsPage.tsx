import React, { useState, useEffect } from 'react'
import { PageHeader, Card, Btn, Input, Select } from '../components/ui'
import { settingsApi, MpesaConfigResponse } from '../services/api'

export default function MpesaSettingsPage() {
  const [shortCode, setShortCode] = useState('')
  const [passKey, setPassKey] = useState('')
  const [passkeyConfigured, setPasskeyConfigured] = useState(false)
  const [environment, setEnvironment] = useState('sandbox')
  const [accountType, setAccountType] = useState('paybill')
  const [callbackUrl, setCallbackUrl] = useState('')
  const [channels, setChannels] = useState<MpesaConfigResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [saved, setSaved] = useState(false)
  const [saving, setSaving] = useState(false)
  const [errorMsg, setErrorMsg] = useState('')

  useEffect(() => {
    settingsApi.getMpesaChannels()
      .then(res => {
        if (res.success && res.data) {
          setChannels(res.data)
        }
      })
      .catch(() => {
        // If not found, it is fine
      })
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => {
    const config = channels.find(channel => channel.accountType === accountType)
    setShortCode(config?.shortCode || '')
    setCallbackUrl(config?.callbackUrl || '')
    setPasskeyConfigured(config?.passkeyConfigured || false)
    setEnvironment(config?.environment || 'sandbox')
    setPassKey('')
  }, [accountType, channels])

  const handleSave = async () => {
    setSaving(true)
    setErrorMsg('')
    try {
      const res = await settingsApi.updateMpesa({
        shortCode,
        ...(passKey.trim() ? { passKey: passKey.trim() } : {}),
        environment,
        accountType,
        callbackUrl
      })
      if (res.success) {
        if (res.data) {
          setChannels(previous => [
            ...previous.filter(channel => channel.accountType !== accountType),
            res.data as MpesaConfigResponse
          ])
          setPasskeyConfigured(res.data.passkeyConfigured)
          setPassKey('')
        }
        setSaved(true)
        setTimeout(() => setSaved(false), 3000)
      } else {
        setErrorMsg(res.message || 'Failed to save configuration')
      }
    } catch (e: any) {
      setErrorMsg(e.response?.data?.message || 'Network error')
    } finally {
      setSaving(false)
    }
  }

  const Section = ({ title, children }: { title: string; children: React.ReactNode }) => (
    <Card style={{ padding: 24, marginBottom: 16 }}>
      <h3 style={{ fontWeight: 700, marginBottom: 16, fontSize: 15 }}>{title}</h3>
      <div style={{ borderTop: '1px solid var(--b360-border)', paddingTop: 16, display: 'flex', flexDirection: 'column', gap: 14 }}>{children}</div>
    </Card>
  )

  if (loading) {
    return <div style={{ padding: 32, textAlign: 'center', color: 'var(--b360-text-secondary)' }}>Loading settings…</div>
  }

  return (
    <div className="fade-in" style={{ maxWidth: 640 }}>
      <PageHeader
        title="M-Pesa Integration Settings"
        action={
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            {saved && <span style={{ fontSize: 12, color: 'var(--b360-green)', fontWeight: 600 }}>✓ Saved successfully</span>}
            <Btn onClick={handleSave} disabled={saving}>{saving ? 'Saving…' : 'Save Config'}</Btn>
          </div>
        }
      />

      {errorMsg && (
        <div style={{ padding: 12, background: 'var(--b360-red-bg)', color: 'var(--b360-red)', borderRadius: 8, fontSize: 13, fontWeight: 600, marginBottom: 16 }}>
          {errorMsg}
        </div>
      )}

      <Section title="Daraja API Configurations">
        <div style={{ padding: 12, background: 'var(--b360-surface)', borderRadius: 8, fontSize: 12, color: 'var(--b360-text-secondary)' }}>
          Consumer credentials remain backend-managed. This merchant passkey is sent securely to the backend and is never returned to the client.
        </div>
        <Select
          label="Channel to configure"
          value={accountType}
          onChange={setAccountType}
          options={[
            { value: 'paybill', label: channels.some(c => c.accountType === 'paybill') ? 'Paybill — Configured' : 'Paybill — Not configured' },
            { value: 'till', label: channels.some(c => c.accountType === 'till') ? 'Till — Configured' : 'Till — Not configured' }
          ]}
        />
        <Input label={passkeyConfigured ? 'Replace Lipa na M-Pesa Passkey' : 'Lipa na M-Pesa Passkey *'} value={passKey} onChange={setPassKey} type="password" placeholder={passkeyConfigured ? '••••••••••••••••' : 'Enter passkey from Safaricom'} />
        <Input
          label="Business Shortcode (Paybill / Till) *"
          value={shortCode}
          onChange={setShortCode}
          placeholder="e.g. 174379"
        />
        <Input
          label="Callback URL *"
          value={callbackUrl}
          onChange={setCallbackUrl}
          placeholder="https://api.example.com/v1/payments/mpesa/callback"
        />

        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 }}>
          <Select
            label="Environment"
            value={environment}
            onChange={setEnvironment}
            options={[
              { value: 'sandbox', label: 'Sandbox' },
              { value: 'production', label: 'Production' }
            ]}
          />
          <div style={{ padding: 12, borderRadius: 8, background: 'var(--b360-surface)', fontSize: 12 }}>
            Transaction type: <strong>{accountType === 'paybill' ? 'CustomerPayBillOnline' : 'CustomerBuyGoodsOnline'}</strong>
          </div>
        </div>
      </Section>
    </div>
  )
}

import React, { useState, useEffect } from 'react'
import { PageHeader, Card, Btn, Input, Select } from '../components/ui'
import { settingsApi } from '../services/api'

export default function MpesaSettingsPage() {
  const [consumerKey, setConsumerKey] = useState('')
  const [consumerSecret, setConsumerSecret] = useState('')
  const [shortCode, setShortCode] = useState('')
  const [passKey, setPassKey] = useState('')
  const [callbackUrl, setCallbackUrl] = useState('')
  const [environment, setEnvironment] = useState('sandbox')
  const [accountType, setAccountType] = useState('paybill')
  const [loading, setLoading] = useState(true)
  const [saved, setSaved] = useState(false)
  const [saving, setSaving] = useState(false)
  const [errorMsg, setErrorMsg] = useState('')

  useEffect(() => {
    settingsApi.getMpesa()
      .then(res => {
        if (res.success && res.data) {
          setConsumerKey(res.data.consumerKey)
          setConsumerSecret('••••••••••••••••••••••••••••••••')
          setShortCode(res.data.shortCode)
          setPassKey('••••••••••••••••••••••••••••••••')
          setCallbackUrl(res.data.callbackUrl)
          setEnvironment(res.data.environment)
          setAccountType(res.data.accountType)
        }
      })
      .catch(() => {
        // If not found, it is fine
      })
      .finally(() => setLoading(false))
  }, [])

  const handleSave = async () => {
    setSaving(true)
    setErrorMsg('')
    try {
      const res = await settingsApi.updateMpesa({
        consumerKey,
        consumerSecret,
        shortCode,
        passKey,
        callbackUrl,
        environment,
        accountType
      })
      if (res.success) {
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
        <Input
          label="Consumer Key *"
          value={consumerKey}
          onChange={setConsumerKey}
          placeholder="Enter Consumer Key"
        />
        <Input
          label="Consumer Secret *"
          value={consumerSecret}
          onChange={setConsumerSecret}
          type="password"
          placeholder="Enter Consumer Secret"
        />
        <Input
          label="Passkey *"
          value={passKey}
          onChange={setPassKey}
          type="password"
          placeholder="Enter LNM Passkey"
        />
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
          placeholder="https://api.yourdomain.com/v1/payments/mpesa/callback"
        />

        <div style={{ display: 'grid', gridTemplateColumns: '1f 1f', gap: 16 }}>
          <Select
            label="Environment"
            value={environment}
            onChange={setEnvironment}
            options={[
              { value: 'sandbox', label: 'Sandbox' },
              { value: 'production', label: 'Production' }
            ]}
          />
          <Select
            label="Account Type"
            value={accountType}
            onChange={setAccountType}
            options={[
              { value: 'paybill', label: 'Paybill (C2B / LNM)' },
              { value: 'till', label: 'Buy Goods Till' }
            ]}
          />
        </div>
      </Section>
    </div>
  )
}

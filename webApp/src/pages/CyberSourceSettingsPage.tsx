import React, { useState, useEffect } from 'react'
import { PageHeader, Card, Btn, Input } from '../components/ui'
import { settingsApi } from '../services/api'

export default function CyberSourceSettingsPage() {
  const [merchantId, setMerchantId] = useState('')
  const [merchantKeyId, setMerchantKeyId] = useState('')
  const [merchantSecretKey, setMerchantSecretKey] = useState('')
  const [isSandbox, setIsSandbox] = useState(true)
  const [loading, setLoading] = useState(true)
  const [saved, setSaved] = useState(false)
  const [saving, setSaving] = useState(false)
  const [errorMsg, setErrorMsg] = useState('')

  useEffect(() => {
    settingsApi.getCyberSource()
      .then(res => {
        if (res.success && res.data) {
          setMerchantId(res.data.merchantId)
          setMerchantKeyId(res.data.merchantKeyId)
          setMerchantSecretKey('')
          setIsSandbox(res.data.environment === 'sandbox')
        }
      })
      .catch(() => {
        // If not found, it is fine, just leave blank
      })
      .finally(() => setLoading(false))
  }, [])

  const handleSave = async () => {
    setSaving(true)
    setErrorMsg('')
    try {
      const res = await settingsApi.updateCyberSource({
        merchantId,
        merchantKeyId,
        ...(merchantSecretKey.trim() ? { merchantSecretKey: merchantSecretKey.trim() } : {}),
        environment: isSandbox ? 'sandbox' : 'production'
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
        title="CyberSource API Settings"
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

      <Section title="API Configuration">
        <Input
          label="Merchant ID (Organization ID) *"
          value={merchantId}
          onChange={setMerchantId}
          placeholder="e.g. wanfashion_cs_098"
        />
        <Input
          label="Active Key ID (REST API JWT/P12 Key ID) *"
          value={merchantKeyId}
          onChange={setMerchantKeyId}
          placeholder="e.g. 9c7c25eb-xxxx-xxxx-xxxx-xxxxxxx"
        />
        <Input
          label="Replace Shared Secret Key"
          value={merchantSecretKey}
          onChange={setMerchantSecretKey}
          type="password"
          placeholder="Leave blank to keep the current secret"
        />

        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: 8 }}>
          <div>
            <span style={{ fontSize: 13, fontWeight: 600, display: 'block' }}>Active Sandbox Environment</span>
            <span style={{ fontSize: 11, color: 'var(--b360-text-secondary)' }}>Toggle off to deploy credentials on live CyberSource production rails</span>
          </div>
          <div onClick={() => setIsSandbox(!isSandbox)} style={{
            width: 44, height: 24, borderRadius: 12, cursor: 'pointer', transition: 'background 0.2s',
            background: isSandbox ? 'var(--b360-green)' : '#D1D5DB', position: 'relative'
          }}>
            <div style={{ position: 'absolute', top: 2, left: isSandbox ? 22 : 2, width: 20, height: 20, borderRadius: '50%', background: 'white', transition: 'left 0.2s', boxShadow: '0 1px 3px rgba(0,0,0,0.2)' }} />
          </div>
        </div>
      </Section>
    </div>
  )
}

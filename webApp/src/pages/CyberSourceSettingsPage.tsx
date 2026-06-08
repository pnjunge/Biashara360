import React, { useState } from 'react'
import { PageHeader, Card, Btn, Input } from '../components/ui'

export default function CyberSourceSettingsPage() {
  const [merchantId, setMerchantId] = useState('WanFashion_CS_098')
  const [merchantKeyId, setMerchantKeyId] = useState('9c7c25eb-42f8-4a52-b8bb-69d2d0c2e39b')
  const [merchantSecretKey, setMerchantSecretKey] = useState('••••••••••••••••••••••••••••••••')
  const [isSandbox, setIsSandbox] = useState(true)
  const [saved, setSaved] = useState(false)
  const [saving, setSaving] = useState(false)

  const handleSave = () => {
    setSaving(true)
    setTimeout(() => {
      setSaving(false)
      setSaved(true)
      setTimeout(() => setSaved(false), 3000)
    }, 800)
  }

  const Section = ({ title, children }: { title: string; children: React.ReactNode }) => (
    <Card style={{ padding: 24, marginBottom: 16 }}>
      <h3 style={{ fontWeight: 700, marginBottom: 16, fontSize: 15 }}>{title}</h3>
      <div style={{ borderTop: '1px solid var(--b360-border)', paddingTop: 16, display: 'flex', flexDirection: 'column', gap: 14 }}>{children}</div>
    </Card>
  )

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
          label="Shared Secret Key (REST API Shared Secret) *"
          value={merchantSecretKey}
          onChange={setMerchantSecretKey}
          type="password"
          placeholder="Enter secure shared secret key"
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

import React, { useEffect, useState } from 'react'
import { Btn, Card, Input, PageHeader } from '../components/ui'
import { settingsApi } from '../services/api'

const secondsToMinutes = (seconds: number) => String(Math.round(seconds / 60))
const minutesToSeconds = (minutes: string) => Math.round(Number(minutes) * 60)

export default function SessionTimeoutSettingsPage() {
  const [webMinutes, setWebMinutes] = useState('30')
  const [androidMinutes, setAndroidMinutes] = useState('30')
  const [desktopMinutes, setDesktopMinutes] = useState('30')
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [message, setMessage] = useState<{ ok: boolean; text: string } | null>(null)

  useEffect(() => {
    settingsApi.getSessionTimeouts()
      .then(res => {
        if (res.success && res.data) {
          setWebMinutes(secondsToMinutes(res.data.webTimeoutSeconds))
          setAndroidMinutes(secondsToMinutes(res.data.androidTimeoutSeconds))
          setDesktopMinutes(secondsToMinutes(res.data.desktopTimeoutSeconds))
        }
      })
      .catch(() => setMessage({ ok: false, text: 'Unable to load the session policy.' }))
      .finally(() => setLoading(false))
  }, [])

  const save = async () => {
    const values = [webMinutes, androidMinutes, desktopMinutes].map(minutesToSeconds)
    if (values.some(value => !Number.isFinite(value) || value < 60 || value > 86_400)) {
      setMessage({ ok: false, text: 'Each timeout must be between 1 minute and 24 hours.' })
      return
    }
    setSaving(true)
    setMessage(null)
    try {
      const res = await settingsApi.updateSessionTimeouts({
        webTimeoutSeconds: values[0],
        androidTimeoutSeconds: values[1],
        desktopTimeoutSeconds: values[2]
      })
      if (res.success && res.data) {
        window.dispatchEvent(new CustomEvent('session-timeout-updated', { detail: res.data.webTimeoutSeconds }))
      }
      setMessage({ ok: res.success, text: res.message || (res.success ? 'Session policy saved.' : 'Unable to save session policy.') })
    } catch (error: any) {
      setMessage({ ok: false, text: error.response?.data?.message || 'Unable to save session policy.' })
    } finally {
      setSaving(false)
    }
  }

  if (loading) return <div style={{ padding: 32, textAlign: 'center' }}>Loading session policy…</div>

  return (
    <div className="fade-in" style={{ maxWidth: 640 }}>
      <PageHeader title="Session Timeout Policy" action={<Btn onClick={save} disabled={saving}>{saving ? 'Saving…' : 'Save Policy'}</Btn>} />
      {message && <div style={{ marginBottom: 16, padding: 12, borderRadius: 8, color: message.ok ? 'var(--b360-green)' : 'var(--b360-red)', background: message.ok ? 'var(--b360-green-bg)' : 'var(--b360-red-bg)' }}>{message.text}</div>}
      <Card style={{ padding: 24 }}>
        <p style={{ marginTop: 0, color: 'var(--b360-text-secondary)', fontSize: 13 }}>
          Idle users are signed out after the configured period. Android and desktop cache the latest policy after authentication.
        </p>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          <Input label="Web timeout (minutes)" value={webMinutes} onChange={setWebMinutes} type="number" />
          <Input label="Android timeout (minutes)" value={androidMinutes} onChange={setAndroidMinutes} type="number" />
          <Input label="Desktop timeout (minutes)" value={desktopMinutes} onChange={setDesktopMinutes} type="number" />
        </div>
      </Card>
    </div>
  )
}

import React, { useState } from 'react'
import { Modal, Btn, Input, Select } from '../ui'
import { BusinessResponse, superAdminApi } from '../../services/api'
import { Calendar, Clock, CheckCircle, AlertTriangle, ShieldCheck } from 'lucide-react'

interface ExtendSubscriptionModalProps {
  business: BusinessResponse
  onClose: () => void
  onSuccess: (updated: BusinessResponse) => void
}

export function ExtendSubscriptionModal({ business, onClose, onSuccess }: ExtendSubscriptionModalProps) {
  const isCurrentlyTrial = business.isTrial || business.subscriptionTier === 'TRIAL'
  const [extendDays, setExtendDays] = useState<number>(14)
  const [extendTier, setExtendTier] = useState<'FREEMIUM' | 'TRIAL' | 'PREMIUM'>(
    business.subscriptionTier === 'PREMIUM' ? 'PREMIUM' : (isCurrentlyTrial ? 'TRIAL' : 'FREEMIUM')
  )
  const [extendIsTrial, setExtendIsTrial] = useState<boolean>(isCurrentlyTrial)
  const [extendNote, setExtendNote] = useState<string>('')
  const [saving, setSaving] = useState<boolean>(false)
  const [error, setError] = useState<string>('')
  const [success, setSuccess] = useState<string>('')

  // Calculate projected new expiry date
  const now = new Date()
  const currentExpiry = business.subscriptionValidUntil ? new Date(business.subscriptionValidUntil) : null
  const baseDate = currentExpiry && currentExpiry > now ? currentExpiry : now
  const projectedExpiry = new Date(baseDate.getTime() + extendDays * 86400 * 1000)

  const handleSubmit = async () => {
    if (!extendDays || extendDays <= 0) {
      setError('Please specify a positive number of days to extend.')
      return
    }
    setSaving(true)
    setError('')
    try {
      const res = await superAdminApi.extendSubscription(business.id, {
        extendDays,
        isTrial: extendIsTrial,
        tier: extendTier,
        note: extendNote.trim() || undefined,
      })
      if (res.success && res.data) {
        setSuccess(res.message || 'Period extended successfully!')
        onSuccess(res.data)
        setTimeout(() => {
          onClose()
        }, 1200)
      } else {
        setError(res.message || 'Failed to extend subscription.')
      }
    } catch (e: any) {
      setError(e.response?.data?.message || 'Network error. Please try again.')
    } finally {
      setSaving(false)
    }
  }

  const quickOptions = [
    { days: 7, label: '+7 Days' },
    { days: 14, label: '+14 Days (2 Weeks)' },
    { days: 30, label: '+30 Days (1 Month)' },
    { days: 60, label: '+60 Days (2 Months)' },
    { days: 90, label: '+90 Days (1 Quarter)' },
    { days: 365, label: '+365 Days (1 Year)' },
  ]

  return (
    <Modal
      title={`Extend Period · ${business.name}`}
      onClose={onClose}
      footer={
        <>
          <Btn variant="secondary" onClick={onClose}>Cancel</Btn>
          <Btn onClick={handleSubmit} disabled={saving}>
            {saving ? 'Extending...' : `Extend by ${extendDays} Days`}
          </Btn>
        </>
      }
    >
      <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
        {error && (
          <div style={{
            color: 'var(--b360-red)',
            fontSize: 13,
            background: 'var(--b360-red-bg)',
            padding: '10px 14px',
            borderRadius: 8,
            display: 'flex',
            alignItems: 'center',
            gap: 8
          }}>
            <AlertTriangle size={16} />
            <span>{error}</span>
          </div>
        )}
        {success && (
          <div style={{
            color: 'var(--b360-green)',
            fontSize: 13,
            background: 'var(--b360-green-bg)',
            padding: '10px 14px',
            borderRadius: 8,
            display: 'flex',
            alignItems: 'center',
            gap: 8
          }}>
            <CheckCircle size={16} />
            <span>{success}</span>
          </div>
        )}

        {/* Current Status Overview */}
        <div style={{
          background: 'var(--b360-surface)',
          padding: 14,
          borderRadius: 10,
          border: '1px solid var(--b360-border)',
          display: 'grid',
          gridTemplateColumns: 'repeat(2, minmax(0, 1fr))',
          gap: 12,
          fontSize: 13
        }}>
          <div>
            <div style={{ color: 'var(--b360-text-secondary)', fontSize: 11, fontWeight: 600 }}>CURRENT TIER</div>
            <div style={{ fontWeight: 700, marginTop: 2, display: 'flex', alignItems: 'center', gap: 6 }}>
              <span>{business.subscriptionTier}</span>
              {isCurrentlyTrial && (
                <span style={{
                  background: '#fef3c7',
                  color: '#b45309',
                  padding: '2px 6px',
                  borderRadius: 4,
                  fontSize: 10,
                  fontWeight: 800
                }}>
                  TRIAL
                </span>
              )}
            </div>
          </div>
          <div>
            <div style={{ color: 'var(--b360-text-secondary)', fontSize: 11, fontWeight: 600 }}>ACCOUNT STATUS</div>
            <div style={{
              fontWeight: 700,
              marginTop: 2,
              color: business.isExpired ? 'var(--b360-red)' : (business.subscriptionEnabled ? 'var(--b360-green)' : 'var(--b360-red)')
            }}>
              {business.isExpired ? 'Expired' : (business.subscriptionEnabled ? 'Active' : 'Disabled')}
            </div>
          </div>
          <div>
            <div style={{ color: 'var(--b360-text-secondary)', fontSize: 11, fontWeight: 600 }}>VALID UNTIL</div>
            <div style={{ marginTop: 2 }}>
              {business.subscriptionValidUntil
                ? new Date(business.subscriptionValidUntil).toLocaleDateString(undefined, { dateStyle: 'medium' })
                : 'Unlimited (No expiry set)'}
            </div>
          </div>
          <div>
            <div style={{ color: 'var(--b360-text-secondary)', fontSize: 11, fontWeight: 600 }}>DAYS REMAINING</div>
            <div style={{
              marginTop: 2,
              fontWeight: 700,
              color: (business.daysRemaining != null && business.daysRemaining <= 3) ? 'var(--b360-red)' : 'inherit'
            }}>
              {business.isExpired
                ? 'Expired'
                : (business.daysRemaining != null ? `${business.daysRemaining} days left` : 'Perpetual')}
            </div>
          </div>
        </div>

        {/* Quick presets */}
        <div>
          <label style={{ fontSize: 12, fontWeight: 700, display: 'block', marginBottom: 6 }}>
            Quick Extension Presets
          </label>
          <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
            {quickOptions.map(opt => (
              <button
                key={opt.days}
                type="button"
                onClick={() => setExtendDays(opt.days)}
                style={{
                  padding: '6px 12px',
                  borderRadius: 8,
                  fontSize: 12,
                  fontWeight: 600,
                  border: extendDays === opt.days ? '2px solid var(--b360-primary)' : '1px solid var(--b360-border)',
                  background: extendDays === opt.days ? 'rgba(15, 118, 110, 0.1)' : 'var(--b360-surface)',
                  color: extendDays === opt.days ? 'var(--b360-primary)' : 'inherit',
                  cursor: 'pointer',
                  transition: 'all 0.15s ease'
                }}
              >
                {opt.label}
              </button>
            ))}
          </div>
        </div>

        {/* Custom Days Input */}
        <Input
          label="Extension Duration (Days) *"
          type="number"
          value={String(extendDays)}
          onChange={v => setExtendDays(Math.max(1, parseInt(v, 10) || 1))}
          placeholder="e.g. 14"
        />

        {/* Plan Configuration */}
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, minmax(0, 1fr))', gap: 12 }}>
          <Select
            label="Plan Tier"
            value={extendTier}
            onChange={v => {
              const t = v as 'FREEMIUM' | 'TRIAL' | 'PREMIUM'
              setExtendTier(t)
              if (t === 'TRIAL') setExtendIsTrial(true)
              if (t === 'PREMIUM' || t === 'FREEMIUM') setExtendIsTrial(false)
            }}
            options={[
              { value: 'TRIAL', label: 'Trial Mode' },
              { value: 'PREMIUM', label: 'Premium Plan' },
              { value: 'FREEMIUM', label: 'Freemium Plan' },
            ]}
          />
          <Select
            label="Period Classification"
            value={extendIsTrial ? 'TRIAL' : 'PAID'}
            onChange={v => setExtendIsTrial(v === 'TRIAL')}
            options={[
              { value: 'TRIAL', label: 'Free Trial' },
              { value: 'PAID', label: 'Paid / Standard Subscription' },
            ]}
          />
        </div>

        {/* Projected Expiration Banner */}
        <div style={{
          padding: '10px 14px',
          background: 'rgba(15, 118, 110, 0.08)',
          border: '1px solid rgba(15, 118, 110, 0.25)',
          borderRadius: 8,
          fontSize: 12,
          display: 'flex',
          alignItems: 'center',
          gap: 10,
          color: '#0F766E'
        }}>
          <Calendar size={18} />
          <div>
            <div><strong>New Expiry Date:</strong> {projectedExpiry.toLocaleDateString(undefined, { dateStyle: 'full' })}</div>
            <div style={{ opacity: 0.85, fontSize: 11, marginTop: 2 }}>
              Extends from {currentExpiry && currentExpiry > now ? 'current expiry' : 'today'} by +{extendDays} days.
            </div>
          </div>
        </div>

        {/* Optional Audit Note */}
        <Input
          label="SuperAdmin Reason / Audit Note (Optional)"
          value={extendNote}
          onChange={setExtendNote}
          placeholder="e.g. Complimentary trial extension for merchant onboarding"
        />
      </div>
    </Modal>
  )
}

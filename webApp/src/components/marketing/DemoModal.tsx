import React, { useState } from 'react'
import { X, CheckCircle2, MessageCircle, Phone, ArrowRight, Building, Sparkles } from 'lucide-react'

interface DemoModalProps {
  isOpen: boolean
  onClose: () => void
}

export default function DemoModal({ isOpen, onClose }: DemoModalProps) {
  const [name, setName] = useState('')
  const [businessName, setBusinessName] = useState('')
  const [phone, setPhone] = useState('')
  const [businessType, setBusinessType] = useState('RETAIL')
  const [submitted, setSubmitted] = useState(false)

  if (!isOpen) return null

  const handleWhatsAppDirect = () => {
    const text = encodeURIComponent(
      `Hello Biashara360! My name is ${name || 'Trader'} from ${businessName || 'my business'}. I would like to schedule a quick demo of Biashara360 POS & KRA eTIMS software.`
    )
    window.open(`https://wa.me/254700360360?text=${text}`, '_blank')
    onClose()
  }

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    setSubmitted(true)
  }

  return (
    <div className="m-modal-backdrop" onClick={onClose} role="dialog" aria-modal="true">
      <div className="m-modal-card" onClick={e => e.stopPropagation()}>
        {/* Close Button */}
        <button
          type="button"
          onClick={onClose}
          style={{
            position: 'absolute',
            top: 20,
            right: 20,
            width: 32,
            height: 32,
            borderRadius: '50%',
            background: '#f1f5f9',
            border: 'none',
            display: 'grid',
            placeItems: 'center',
            cursor: 'pointer',
            color: '#64748b'
          }}
          aria-label="Close modal"
        >
          <X size={18} />
        </button>

        {!submitted ? (
          <>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 8 }}>
              <div className="m-logo-icon" style={{ width: 28, height: 28, borderRadius: 8 }}>
                <Sparkles size={16} />
              </div>
              <span style={{ fontSize: 12, fontWeight: 800, color: '#059669', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                Biashara360 Consultation
              </span>
            </div>

            <h3 style={{ fontSize: 22, fontWeight: 800, color: '#0f172a', marginBottom: 6 }}>
              Book a 1-on-1 Personalized Demo
            </h3>
            <p style={{ fontSize: 13, color: '#64748b', marginBottom: 20 }}>
              See how Biashara360 transforms checkout, KRA eTIMS, and M-Pesa tracking for your specific shop.
            </p>

            {/* Quick WhatsApp option */}
            <button
              type="button"
              onClick={handleWhatsAppDirect}
              style={{
                width: '100%',
                padding: '12px',
                borderRadius: 10,
                background: '#ecfdf5',
                border: '1.5px solid #a7f3d0',
                color: '#047857',
                fontWeight: 700,
                fontSize: 14,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: 8,
                cursor: 'pointer',
                marginBottom: 16
              }}
            >
              <MessageCircle size={18} color="#059669" />
              <span>Chat Instantly with Sales Specialist on WhatsApp</span>
            </button>

            <div style={{ display: 'flex', alignItems: 'center', gap: 10, margin: '14px 0', color: '#94a3b8', fontSize: 12 }}>
              <div style={{ flex: 1, height: 1, background: '#e2e8f0' }} />
              <span>or request a call back</span>
              <div style={{ flex: 1, height: 1, background: '#e2e8f0' }} />
            </div>

            <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
              <div>
                <label style={{ display: 'block', fontSize: 12, fontWeight: 600, color: '#334155', marginBottom: 4 }}>
                  Your Full Name
                </label>
                <input
                  type="text"
                  required
                  placeholder="e.g. Dennis Kiprop"
                  value={name}
                  onChange={e => setName(e.target.value)}
                  style={{ width: '100%', padding: '10px 12px', borderRadius: 8, border: '1px solid #cbd5e1', fontSize: 14 }}
                />
              </div>

              <div>
                <label style={{ display: 'block', fontSize: 12, fontWeight: 600, color: '#334155', marginBottom: 4 }}>
                  Business Name
                </label>
                <input
                  type="text"
                  required
                  placeholder="e.g. Westlands Gourmet Mart"
                  value={businessName}
                  onChange={e => setBusinessName(e.target.value)}
                  style={{ width: '100%', padding: '10px 12px', borderRadius: 8, border: '1px solid #cbd5e1', fontSize: 14 }}
                />
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                <div>
                  <label style={{ display: 'block', fontSize: 12, fontWeight: 600, color: '#334155', marginBottom: 4 }}>
                    Phone (M-Pesa / WhatsApp)
                  </label>
                  <input
                    type="tel"
                    required
                    placeholder="0712 345 678"
                    value={phone}
                    onChange={e => setPhone(e.target.value)}
                    style={{ width: '100%', padding: '10px 12px', borderRadius: 8, border: '1px solid #cbd5e1', fontSize: 14 }}
                  />
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: 12, fontWeight: 600, color: '#334155', marginBottom: 4 }}>
                    Business Type
                  </label>
                  <select
                    value={businessType}
                    onChange={e => setBusinessType(e.target.value)}
                    style={{ width: '100%', padding: '10px 12px', borderRadius: 8, border: '1px solid #cbd5e1', fontSize: 14, background: '#ffffff' }}
                  >
                    <option value="RETAIL">Retail / Supermarket</option>
                    <option value="RESTAURANT">Bar, Café or Restaurant</option>
                    <option value="WHOLESALE">Wholesale & Distributor</option>
                    <option value="HARDWARE">Hardware / Agrovet</option>
                    <option value="PHARMACY">Pharmacy / Chemist</option>
                    <option value="SERVICES">Salon, Spa or Service</option>
                  </select>
                </div>
              </div>

              <button
                type="submit"
                className="m-btn m-btn-primary"
                style={{ width: '100%', padding: '12px', fontSize: 15, marginTop: 6 }}
              >
                <span>Request Demo Call</span>
                <ArrowRight size={16} />
              </button>
            </form>
          </>
        ) : (
          <div style={{ textAlign: 'center', padding: '20px 0' }}>
            <div style={{ width: 56, height: 56, borderRadius: '50%', background: '#ecfdf5', display: 'grid', placeItems: 'center', margin: '0 auto 16px' }}>
              <CheckCircle2 size={32} color="#059669" />
            </div>
            <h3 style={{ fontSize: 20, fontWeight: 800, color: '#0f172a', marginBottom: 8 }}>
              Demo Request Received!
            </h3>
            <p style={{ fontSize: 14, color: '#475569', lineHeight: 1.5, marginBottom: 20 }}>
              Asante sana {name}! A Biashara360 specialist will call or message you on <strong>{phone}</strong> within 30 minutes during business hours.
            </p>
            <button
              type="button"
              onClick={onClose}
              className="m-btn m-btn-secondary"
              style={{ width: '100%' }}
            >
              Done
            </button>
          </div>
        )}
      </div>
    </div>
  )
}

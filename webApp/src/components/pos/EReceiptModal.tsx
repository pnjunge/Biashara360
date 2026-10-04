import React, { useState, useEffect } from 'react'
import QRCode from 'qrcode'
import {
  X,
  QrCode,
  Smartphone,
  Mail,
  Share2,
  Copy,
  Check,
  ExternalLink,
  Receipt,
  CheckCircle2,
  AlertCircle,
  Loader2
} from 'lucide-react'
import { orderApi, OrderResponse } from '../../services/api'
import './EReceiptModal.css'

interface EReceiptModalProps {
  isOpen: boolean
  onClose: () => void
  order: OrderResponse | null
  businessName?: string
}

export default function EReceiptModal({
  isOpen,
  onClose,
  order,
  businessName = 'Biashara360'
}: EReceiptModalProps) {
  const [activeTab, setActiveTab] = useState<'qr' | 'sms' | 'whatsapp' | 'email'>('qr')
  const [phone, setPhone] = useState('')
  const [email, setEmail] = useState('')
  const [qrCodeUrl, setQrCodeUrl] = useState('')
  const [sending, setSending] = useState(false)
  const [statusMessage, setStatusMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null)
  const [copied, setCopied] = useState(false)

  const receiptUrl = order
    ? `${window.location.origin}/receipt/${order.id}`
    : ''

  useEffect(() => {
    if (order) {
      setPhone(order.customerPhone && order.customerPhone !== 'Walk-In Customer' ? order.customerPhone : '')
      setEmail('')
      setStatusMessage(null)
      setCopied(false)

      const url = `${window.location.origin}/receipt/${order.id}`
      QRCode.toDataURL(url, {
        width: 320,
        margin: 2,
        color: {
          dark: '#0f172a',
          light: '#ffffff'
        }
      })
        .then(setQrCodeUrl)
        .catch(() => {})
    }
  }, [order])

  if (!isOpen || !order) return null

  const handleCopy = () => {
    if (!receiptUrl) return
    navigator.clipboard.writeText(receiptUrl).then(() => {
      setCopied(true)
      setTimeout(() => setCopied(false), 2000)
    })
  }

  const handleSendSms = async () => {
    if (!phone.trim()) {
      setStatusMessage({ type: 'error', text: 'Please enter a recipient phone number.' })
      return
    }

    setSending(true)
    setStatusMessage(null)

    try {
      const res = await orderApi.sendEReceipt(order.id, {
        channel: 'SMS',
        recipient: phone.trim()
      })

      if (res.success) {
        setStatusMessage({ type: 'success', text: `Receipt SMS sent successfully to ${phone.trim()}` })
      } else {
        setStatusMessage({ type: 'error', text: res.message || 'Failed to send SMS.' })
      }
    } catch (err: any) {
      setStatusMessage({
        type: 'error',
        text: err.response?.data?.message || 'Network error while sending SMS.'
      })
    } finally {
      setSending(false)
    }
  }

  const handleSendEmail = async () => {
    if (!email.trim() || !email.includes('@')) {
      setStatusMessage({ type: 'error', text: 'Please enter a valid email address.' })
      return
    }

    setSending(true)
    setStatusMessage(null)

    try {
      const res = await orderApi.sendEReceipt(order.id, {
        channel: 'EMAIL',
        recipient: email.trim()
      })

      if (res.success) {
        setStatusMessage({ type: 'success', text: `e-Receipt email sent to ${email.trim()}` })
      } else {
        setStatusMessage({ type: 'error', text: res.message || 'Failed to send email.' })
      }
    } catch (err: any) {
      setStatusMessage({
        type: 'error',
        text: err.response?.data?.message || 'Network error while sending email.'
      })
    } finally {
      setSending(false)
    }
  }

  const handleOpenWhatsApp = () => {
    const rawPhone = phone.trim().replace(/[\s\-()]/g, '')
    const digits = rawPhone.startsWith('07') ? `254${rawPhone.slice(1)}`
      : rawPhone.startsWith('01') ? `254${rawPhone.slice(1)}`
      : rawPhone.startsWith('+254') ? rawPhone.slice(1)
      : rawPhone

    const text = `🧾 *Electronic Receipt from ${businessName}*\nOrder: #${order.orderNumber}\nTotal: KES ${Number(order.subtotal).toLocaleString('en-KE', { minimumFractionDigits: 2 })}\nPayment: ${order.paymentMethod}\n\nView official receipt:\n${receiptUrl}`
    
    const waUrl = digits
      ? `https://wa.me/${digits}?text=${encodeURIComponent(text)}`
      : `https://api.whatsapp.com/send?text=${encodeURIComponent(text)}`

    window.open(waUrl, '_blank')
  }

  return (
    <div className="ereceipt-modal-backdrop">
      <div className="ereceipt-modal-dialog">
        {/* Header */}
        <div className="ereceipt-modal-header">
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <div className="ereceipt-modal-icon">
              <Receipt size={22} />
            </div>
            <div>
              <div style={{ fontSize: 16, fontWeight: 700, color: '#0f172a' }}>Digital e-Receipt</div>
              <div style={{ fontSize: 12, color: '#64748b', fontFamily: 'monospace' }}>Order #{order.orderNumber}</div>
            </div>
          </div>
          <button type="button" onClick={onClose} className="ereceipt-modal-close">
            <X size={18} />
          </button>
        </div>

        {/* Channels Navigation Tabs */}
        <div className="ereceipt-tabs">
          <button
            type="button"
            onClick={() => { setActiveTab('qr'); setStatusMessage(null) }}
            className={`ereceipt-tab-btn ${activeTab === 'qr' ? 'is-active' : ''}`}
          >
            <QrCode size={14} />
            QR Code
          </button>
          <button
            type="button"
            onClick={() => { setActiveTab('whatsapp'); setStatusMessage(null) }}
            className={`ereceipt-tab-btn ${activeTab === 'whatsapp' ? 'is-active is-whatsapp' : ''}`}
          >
            <Share2 size={14} />
            WhatsApp
          </button>
          <button
            type="button"
            onClick={() => { setActiveTab('sms'); setStatusMessage(null) }}
            className={`ereceipt-tab-btn ${activeTab === 'sms' ? 'is-active' : ''}`}
          >
            <Smartphone size={14} />
            SMS
          </button>
          <button
            type="button"
            onClick={() => { setActiveTab('email'); setStatusMessage(null) }}
            className={`ereceipt-tab-btn ${activeTab === 'email' ? 'is-active' : ''}`}
          >
            <Mail size={14} />
            Email
          </button>
        </div>

        {/* Tab Body */}
        <div className="ereceipt-modal-body">
          {/* Status Message */}
          {statusMessage && (
            <div className={`ereceipt-modal-alert ${statusMessage.type === 'success' ? 'is-success' : 'is-error'}`}>
              {statusMessage.type === 'success' ? (
                <CheckCircle2 size={16} />
              ) : (
                <AlertCircle size={16} />
              )}
              <span>{statusMessage.text}</span>
            </div>
          )}

          {/* QR Tab */}
          {activeTab === 'qr' && (
            <div style={{ textAlign: 'center' }}>
              <div style={{
                display: 'inline-block',
                padding: 12,
                background: '#ffffff',
                borderRadius: 16,
                border: '2px dashed #bbf7d0',
                marginBottom: 12
              }}>
                {qrCodeUrl ? (
                  <img src={qrCodeUrl} alt="e-Receipt QR" style={{ width: 180, height: 180, display: 'block' }} />
                ) : (
                  <div style={{ width: 180, height: 180, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                    <Loader2 size={24} className="animate-spin" color="#10b981" />
                  </div>
                )}
              </div>
              <div style={{ fontSize: 13, fontWeight: 700, color: '#0f172a', marginBottom: 2 }}>
                Show this QR Code to customer
              </div>
              <div style={{ fontSize: 11, color: '#64748b', marginBottom: 16 }}>
                Customer scans with any phone camera to view & keep receipt
              </div>

              <div style={{ display: 'flex', justifyContent: 'center', gap: 8 }}>
                <button
                  type="button"
                  onClick={handleCopy}
                  style={{
                    display: 'inline-flex',
                    alignItems: 'center',
                    gap: 6,
                    padding: '8px 14px',
                    borderRadius: 10,
                    background: '#f1f5f9',
                    color: '#334155',
                    fontSize: 12,
                    fontWeight: 600,
                    border: '1px solid #cbd5e1',
                    cursor: 'pointer'
                  }}
                >
                  {copied ? <Check size={14} color="#059669" /> : <Copy size={14} />}
                  {copied ? 'Link Copied!' : 'Copy Link'}
                </button>
                <a
                  href={receiptUrl}
                  target="_blank"
                  rel="noreferrer"
                  style={{
                    display: 'inline-flex',
                    alignItems: 'center',
                    gap: 6,
                    padding: '8px 14px',
                    borderRadius: 10,
                    background: '#ecfdf5',
                    color: '#065f46',
                    fontSize: 12,
                    fontWeight: 600,
                    border: '1px solid #a7f3d0',
                    textDecoration: 'none'
                  }}
                >
                  <ExternalLink size={14} />
                  Open Receipt Page
                </a>
              </div>
            </div>
          )}

          {/* WhatsApp Tab */}
          {activeTab === 'whatsapp' && (
            <div>
              <div className="ereceipt-input-group">
                <label className="ereceipt-input-label">Customer WhatsApp Number</label>
                <input
                  type="tel"
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  className="ereceipt-input-field"
                  style={{ fontFamily: 'monospace' }}
                />
                <div style={{ fontSize: 11, color: '#94a3b8', marginTop: 4 }}>
                  Leave blank to choose customer contact inside WhatsApp.
                </div>
              </div>

              <div style={{ padding: 12, background: '#f0fdf4', borderRadius: 12, border: '1px solid #bbf7d0', fontSize: 12, color: '#166534', marginBottom: 16 }}>
                <strong>Message:</strong>
                <p style={{ margin: '4px 0 0', fontStyle: 'italic', fontSize: 11, color: '#475569' }}>
                  "🧾 Official Electronic Receipt from {businessName} for Order #{order.orderNumber} (KES {Number(order.subtotal).toLocaleString('en-KE')})..."
                </p>
              </div>

              <button
                type="button"
                onClick={handleOpenWhatsApp}
                className="ereceipt-submit-btn btn-whatsapp-theme"
              >
                <Share2 size={16} />
                Open WhatsApp & Send
              </button>
            </div>
          )}

          {/* SMS Tab */}
          {activeTab === 'sms' && (
            <div>
              <div className="ereceipt-input-group">
                <label className="ereceipt-input-label">Recipient Phone Number</label>
                <input
                  type="tel"
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  className="ereceipt-input-field"
                  style={{ fontFamily: 'monospace' }}
                />
                <div style={{ fontSize: 11, color: '#94a3b8', marginTop: 4 }}>
                  Delivered instantly via SMS with the e-receipt link.
                </div>
              </div>

              <button
                type="button"
                disabled={sending || !phone.trim()}
                onClick={handleSendSms}
                className="ereceipt-submit-btn btn-dark"
              >
                {sending ? <Loader2 size={16} className="animate-spin" /> : <Smartphone size={16} />}
                {sending ? 'Sending SMS...' : 'Send SMS e-Receipt'}
              </button>
            </div>
          )}

          {/* Email Tab */}
          {activeTab === 'email' && (
            <div>
              <div className="ereceipt-input-group">
                <label className="ereceipt-input-label">Recipient Email Address</label>
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="ereceipt-input-field"
                />
                <div style={{ fontSize: 11, color: '#94a3b8', marginTop: 4 }}>
                  Customer will receive a branded HTML receipt with item breakdown.
                </div>
              </div>

              <button
                type="button"
                disabled={sending || !email.trim()}
                onClick={handleSendEmail}
                className="ereceipt-submit-btn btn-dark"
              >
                {sending ? <Loader2 size={16} className="animate-spin" /> : <Mail size={16} />}
                {sending ? 'Sending Email...' : 'Send Email e-Receipt'}
              </button>
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="ereceipt-modal-footer">
          <span style={{ fontSize: 12, color: '#64748b' }}>
            Total: <strong style={{ color: '#0f172a', fontFamily: 'monospace' }}>KES {Number(order.subtotal).toLocaleString('en-KE', { minimumFractionDigits: 2 })}</strong>
          </span>
          <button
            type="button"
            onClick={onClose}
            style={{
              padding: '6px 14px',
              borderRadius: 8,
              border: '1px solid #cbd5e1',
              background: '#ffffff',
              fontSize: 12,
              fontWeight: 600,
              color: '#334155',
              cursor: 'pointer'
            }}
          >
            Done
          </button>
        </div>
      </div>
    </div>
  )
}

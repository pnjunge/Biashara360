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
  Printer,
  Receipt,
  CheckCircle2,
  AlertCircle,
  Loader2
} from 'lucide-react'
import { orderApi, OrderResponse } from '../../services/api'

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
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm animate-fadeIn">
      <div className="bg-white rounded-3xl max-w-md w-full shadow-2xl border border-slate-100 overflow-hidden flex flex-col max-h-[90vh]">
        {/* Header */}
        <div className="p-5 border-b border-slate-100 flex items-center justify-between bg-slate-50/50">
          <div className="flex items-center gap-2.5">
            <div className="w-10 h-10 rounded-xl bg-emerald-100 text-emerald-700 flex items-center justify-center">
              <Receipt className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-base font-bold text-slate-900 leading-tight">Digital e-Receipt</h2>
              <p className="text-xs text-slate-500 font-mono">Order #{order.orderNumber}</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="w-8 h-8 rounded-full hover:bg-slate-200/80 flex items-center justify-center text-slate-400 hover:text-slate-600 transition"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Channels Navigation Tabs */}
        <div className="grid grid-cols-4 p-1.5 mx-5 mt-4 bg-slate-100 rounded-xl text-xs font-semibold text-slate-600">
          <button
            type="button"
            onClick={() => { setActiveTab('qr'); setStatusMessage(null) }}
            className={`py-2 rounded-lg flex items-center justify-center gap-1.5 transition ${
              activeTab === 'qr' ? 'bg-white text-slate-900 shadow-sm' : 'hover:text-slate-900'
            }`}
          >
            <QrCode className="w-3.5 h-3.5" />
            QR Code
          </button>
          <button
            type="button"
            onClick={() => { setActiveTab('whatsapp'); setStatusMessage(null) }}
            className={`py-2 rounded-lg flex items-center justify-center gap-1.5 transition ${
              activeTab === 'whatsapp' ? 'bg-white text-emerald-700 shadow-sm' : 'hover:text-slate-900'
            }`}
          >
            <Share2 className="w-3.5 h-3.5" />
            WhatsApp
          </button>
          <button
            type="button"
            onClick={() => { setActiveTab('sms'); setStatusMessage(null) }}
            className={`py-2 rounded-lg flex items-center justify-center gap-1.5 transition ${
              activeTab === 'sms' ? 'bg-white text-slate-900 shadow-sm' : 'hover:text-slate-900'
            }`}
          >
            <Smartphone className="w-3.5 h-3.5" />
            SMS
          </button>
          <button
            type="button"
            onClick={() => { setActiveTab('email'); setStatusMessage(null) }}
            className={`py-2 rounded-lg flex items-center justify-center gap-1.5 transition ${
              activeTab === 'email' ? 'bg-white text-slate-900 shadow-sm' : 'hover:text-slate-900'
            }`}
          >
            <Mail className="w-3.5 h-3.5" />
            Email
          </button>
        </div>

        {/* Tab Body */}
        <div className="p-6 overflow-y-auto flex-1">
          {/* Status Message */}
          {statusMessage && (
            <div
              className={`mb-4 p-3 rounded-xl text-xs font-medium flex items-center gap-2 ${
                statusMessage.type === 'success'
                  ? 'bg-emerald-50 text-emerald-800 border border-emerald-200'
                  : 'bg-rose-50 text-rose-800 border border-rose-200'
              }`}
            >
              {statusMessage.type === 'success' ? (
                <CheckCircle2 className="w-4 h-4 text-emerald-600 flex-shrink-0" />
              ) : (
                <AlertCircle className="w-4 h-4 text-rose-600 flex-shrink-0" />
              )}
              <span>{statusMessage.text}</span>
            </div>
          )}

          {/* QR Tab */}
          {activeTab === 'qr' && (
            <div className="text-center">
              <div className="inline-block p-3 bg-white rounded-2xl border-2 border-dashed border-emerald-200 shadow-sm mb-3">
                {qrCodeUrl ? (
                  <img src={qrCodeUrl} alt="e-Receipt QR" className="w-48 h-48 mx-auto" />
                ) : (
                  <div className="w-48 h-48 flex items-center justify-center text-slate-400">
                    <Loader2 className="w-6 h-6 animate-spin" />
                  </div>
                )}
              </div>
              <p className="text-xs font-bold text-slate-800 mb-0.5">Show this to the customer</p>
              <p className="text-[11px] text-slate-400 mb-4">
                Customer scans with any phone camera to view & save receipt
              </p>

              <div className="flex items-center justify-center gap-2">
                <button
                  type="button"
                  onClick={handleCopy}
                  className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-semibold rounded-xl transition"
                >
                  {copied ? <Check className="w-3.5 h-3.5 text-emerald-600" /> : <Copy className="w-3.5 h-3.5" />}
                  {copied ? 'Link Copied!' : 'Copy Link'}
                </button>
                <a
                  href={receiptUrl}
                  target="_blank"
                  rel="noreferrer"
                  className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-emerald-50 hover:bg-emerald-100 text-emerald-700 text-xs font-semibold rounded-xl transition"
                >
                  <ExternalLink className="w-3.5 h-3.5" />
                  Open Digital Page
                </a>
              </div>
            </div>
          )}

          {/* WhatsApp Tab */}
          {activeTab === 'whatsapp' && (
            <div className="space-y-4">
              <div>
                <label className="block text-xs font-bold uppercase tracking-wider text-slate-500 mb-1.5">
                  Customer WhatsApp Number
                </label>
                <input
                  type="tel"
                  placeholder="e.g. 0712345678"
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500 font-mono"
                />
                <p className="text-[11px] text-slate-400 mt-1">
                  Leave blank to choose contact manually inside WhatsApp.
                </p>
              </div>

              <div className="p-3 bg-emerald-50/60 rounded-xl border border-emerald-100 text-xs text-emerald-900 space-y-1">
                <p className="font-semibold">Message Preview:</p>
                <p className="text-slate-600 font-sans italic text-[11px]">
                  "🧾 Official Electronic Receipt from {businessName} for Order #{order.orderNumber} (KES {Number(order.subtotal).toLocaleString('en-KE')})..."
                </p>
              </div>

              <button
                type="button"
                onClick={handleOpenWhatsApp}
                className="w-full py-2.5 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl text-sm font-bold flex items-center justify-center gap-2 shadow-sm transition active:scale-[0.98]"
              >
                <Share2 className="w-4 h-4" />
                Open WhatsApp & Send
              </button>
            </div>
          )}

          {/* SMS Tab */}
          {activeTab === 'sms' && (
            <div className="space-y-4">
              <div>
                <label className="block text-xs font-bold uppercase tracking-wider text-slate-500 mb-1.5">
                  Recipient Phone Number
                </label>
                <input
                  type="tel"
                  placeholder="e.g. 0712345678 or +254..."
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500 font-mono"
                />
                <p className="text-[11px] text-slate-400 mt-1">
                  Delivered instantly via SMS with the e-receipt link.
                </p>
              </div>

              <button
                type="button"
                disabled={sending || !phone.trim()}
                onClick={handleSendSms}
                className="w-full py-2.5 bg-slate-900 hover:bg-slate-800 disabled:opacity-50 text-white rounded-xl text-sm font-bold flex items-center justify-center gap-2 shadow-sm transition active:scale-[0.98]"
              >
                {sending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Smartphone className="w-4 h-4" />}
                {sending ? 'Sending SMS...' : 'Send SMS e-Receipt'}
              </button>
            </div>
          )}

          {/* Email Tab */}
          {activeTab === 'email' && (
            <div className="space-y-4">
              <div>
                <label className="block text-xs font-bold uppercase tracking-wider text-slate-500 mb-1.5">
                  Recipient Email Address
                </label>
                <input
                  type="email"
                  placeholder="customer@example.com"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500"
                />
                <p className="text-[11px] text-slate-400 mt-1">
                  Customer will receive a branded HTML receipt with item breakdown.
                </p>
              </div>

              <button
                type="button"
                disabled={sending || !email.trim()}
                onClick={handleSendEmail}
                className="w-full py-2.5 bg-slate-900 hover:bg-slate-800 disabled:opacity-50 text-white rounded-xl text-sm font-bold flex items-center justify-center gap-2 shadow-sm transition active:scale-[0.98]"
              >
                {sending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Mail className="w-4 h-4" />}
                {sending ? 'Sending Email...' : 'Send Email e-Receipt'}
              </button>
            </div>
          )}
        </div>

        {/* Modal Footer */}
        <div className="p-4 bg-slate-50/80 border-t border-slate-100 flex items-center justify-between">
          <span className="text-xs font-medium text-slate-500">
            Total: <span className="font-bold text-slate-800 font-mono">KES {Number(order.subtotal).toLocaleString('en-KE', { minimumFractionDigits: 2 })}</span>
          </span>
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-1.5 bg-white hover:bg-slate-100 border border-slate-200 rounded-xl text-xs font-semibold text-slate-700 transition"
          >
            Done
          </button>
        </div>
      </div>
    </div>
  )
}

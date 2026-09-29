import React, { useEffect, useState, useRef } from 'react'
import { useParams, Link } from 'react-router-dom'
import QRCode from 'qrcode'
import {
  Printer,
  Share2,
  Copy,
  Check,
  Building2,
  MapPin,
  Phone,
  Mail,
  Receipt,
  FileCheck,
  ShieldCheck,
  ChevronLeft,
  AlertCircle
} from 'lucide-react'
import { receiptApi, PublicReceiptResponse } from '../services/api'
import './EReceiptPage.css'

export default function EReceiptPage() {
  const { orderId } = useParams<{ orderId: string }>()
  const [receiptData, setReceiptData] = useState<PublicReceiptResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [qrCodeDataUrl, setQrCodeDataUrl] = useState<string>('')
  const [copied, setCopied] = useState(false)
  const receiptCardRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!orderId) {
      setError('No receipt identifier provided.')
      setLoading(false)
      return
    }

    let active = true
    setLoading(true)
    setError(null)

    receiptApi.getPublicReceipt(orderId)
      .then((res) => {
        if (!active) return
        if (res.success && res.data) {
          setReceiptData(res.data)
          // Generate QR code for verification URL
          const currentUrl = res.data.receiptUrl || window.location.href
          QRCode.toDataURL(currentUrl, {
            width: 280,
            margin: 1,
            color: {
              dark: '#0f172a',
              light: '#ffffff'
            }
          }).then((dataUrl) => {
            if (active) setQrCodeDataUrl(dataUrl)
          }).catch(() => {})
        } else {
          setError(res.message || 'Receipt could not be loaded.')
        }
      })
      .catch((err) => {
        if (!active) return
        setError(err.response?.data?.message || 'Digital receipt not found or expired.')
      })
      .finally(() => {
        if (active) setLoading(false)
      })

    return () => {
      active = false
    }
  }, [orderId])

  const handleCopyLink = () => {
    const url = receiptData?.receiptUrl || window.location.href
    navigator.clipboard.writeText(url).then(() => {
      setCopied(true)
      setTimeout(() => setCopied(false), 2500)
    }).catch(() => {})
  }

  const handlePrint = () => {
    window.print()
  }

  const handleWhatsAppShare = () => {
    if (!receiptData) return
    const { order, business, receiptUrl } = receiptData
    const text = `🧾 *Electronic Receipt from ${business.name}*\nOrder: #${order.orderNumber}\nTotal: KES ${Number(order.subtotal).toLocaleString('en-KE', { minimumFractionDigits: 2 })}\nStatus: ${order.paymentStatus}\n\nView official receipt:\n${receiptUrl}`
    const waUrl = `https://api.whatsapp.com/send?text=${encodeURIComponent(text)}`
    window.open(waUrl, '_blank')
  }

  if (loading) {
    return (
      <div className="ereceipt-page-wrapper">
        <div className="ereceipt-slip" style={{ padding: '60px 30px', textAlign: 'center' }}>
          <div className="ereceipt-brand-icon" style={{ opacity: 0.5 }}>...</div>
          <h2 style={{ fontSize: 18, color: '#64748b', fontWeight: 600 }}>Loading electronic receipt...</h2>
        </div>
      </div>
    )
  }

  if (error || !receiptData) {
    return (
      <div className="ereceipt-page-wrapper">
        <div className="ereceipt-slip" style={{ padding: '40px 28px', textAlign: 'center' }}>
          <div style={{
            width: 56,
            height: 56,
            borderRadius: '50%',
            background: '#fee2e2',
            color: '#ef4444',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            margin: '0 auto 16px'
          }}>
            <AlertCircle size={28} />
          </div>
          <h2 style={{ fontSize: 20, fontWeight: 700, color: '#0f172a', marginBottom: 8 }}>Receipt Not Found</h2>
          <p style={{ fontSize: 13, color: '#64748b', marginBottom: 24, lineHeight: 1.6 }}>
            {error || 'We could not locate this electronic receipt. Please check the link or contact the merchant.'}
          </p>
          <Link
            to="/"
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: 8,
              padding: '10px 20px',
              background: '#0f172a',
              color: '#ffffff',
              fontSize: 13,
              fontWeight: 600,
              borderRadius: 12,
              textDecoration: 'none'
            }}
          >
            <ChevronLeft size={16} />
            Return to Biashara360
          </Link>
        </div>
      </div>
    )
  }

  const { order, business, branch, kraFiscal } = receiptData
  const isPaid = order.paymentStatus === 'PAID'
  const isFailed = order.paymentStatus === 'FAILED'
  const formattedDate = new Date(order.createdAt).toLocaleDateString('en-GB', {
    weekday: 'short',
    day: 'numeric',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })

  return (
    <div className="ereceipt-page-wrapper">
      {/* Top Floating Action Bar */}
      <div className="ereceipt-toolbar">
        <div className="ereceipt-verified-pill">
          <ShieldCheck size={16} color="#059669" />
          Verified e-Receipt
        </div>

        <div className="ereceipt-toolbar-actions">
          <button
            type="button"
            onClick={handleCopyLink}
            className="ereceipt-action-btn"
            title="Copy digital receipt link"
          >
            {copied ? <Check size={14} color="#059669" /> : <Copy size={14} color="#64748b" />}
            {copied ? 'Copied' : 'Copy'}
          </button>

          <button
            type="button"
            onClick={handleWhatsAppShare}
            className="ereceipt-action-btn btn-whatsapp"
            title="Share on WhatsApp"
          >
            <Share2 size={14} />
            WhatsApp
          </button>

          <button
            type="button"
            onClick={handlePrint}
            className="ereceipt-action-btn btn-print"
            title="Print or Save as PDF"
          >
            <Printer size={14} />
            Print
          </button>
        </div>
      </div>

      {/* Main Electronic Receipt Slip */}
      <div ref={receiptCardRef} className="ereceipt-slip" id="receipt-print-area">
        {/* Status Banner */}
        <div className={`ereceipt-status-bar ${
          isPaid ? 'status-paid' : isFailed ? 'status-failed' : 'status-pending'
        }`}>
          {isPaid ? '✓ Transaction Completed · Official Receipt' : `Payment Status: ${order.paymentStatus}`}
        </div>

        {/* Business Header */}
        <div className="ereceipt-header">
          {business.receiptLogo ? (
            <img
              src={business.receiptLogo}
              alt={business.name}
              style={{
                display: 'block',
                margin: '0 auto 12px',
                maxWidth: `${business.receiptLogoWidthMm || 42}mm`,
                maxHeight: `${business.receiptLogoHeightMm || 24}mm`,
                objectFit: 'contain'
              }}
            />
          ) : (
            <div className="ereceipt-brand-icon">
              {business.name.charAt(0).toUpperCase()}
            </div>
          )}

          <h1 className="ereceipt-brand-title">{business.name}</h1>

          {branch && (
            <div className="ereceipt-branch-badge">
              <Building2 size={13} color="#64748b" />
              {branch.name} {branch.code ? `(${branch.code})` : ''}
            </div>
          )}

          <div className="ereceipt-contact-list">
            {(branch?.address || business.address) && (
              <div className="ereceipt-contact-item">
                <MapPin size={12} color="#94a3b8" />
                <span>{branch?.address || business.address} {business.county ? `· ${business.county}` : ''}</span>
              </div>
            )}
            {(branch?.phone || business.phone) && (
              <div className="ereceipt-contact-item">
                <Phone size={12} color="#94a3b8" />
                <span>{branch?.phone || business.phone}</span>
              </div>
            )}
            {business.email && (
              <div className="ereceipt-contact-item">
                <Mail size={12} color="#94a3b8" />
                <span>{business.email}</span>
              </div>
            )}
            {business.kraPin && (
              <div className="ereceipt-contact-item" style={{ marginTop: 2, fontWeight: 600, color: '#334155' }}>
                KRA PIN: <span style={{ fontFamily: 'monospace' }}>{business.kraPin}</span>
              </div>
            )}
          </div>

          {(branch?.receiptHeader || business.receiptHeader) && (
            <div className="ereceipt-header-note">
              {branch?.receiptHeader || business.receiptHeader}
            </div>
          )}
        </div>

        {/* Perforated Divider */}
        <div className="ereceipt-perforation">
          <div className="ereceipt-cutout-left" />
          <div className="ereceipt-dashed-line" />
          <div className="ereceipt-cutout-right" />
        </div>

        {/* Order Meta */}
        <div className="ereceipt-meta">
          <div className="ereceipt-meta-grid">
            <div className="ereceipt-meta-item">
              <span className="ereceipt-meta-label">Order / Receipt</span>
              <span className="ereceipt-meta-val font-mono">#{order.orderNumber}</span>
            </div>
            <div className="ereceipt-meta-item align-right">
              <span className="ereceipt-meta-label">Date & Time</span>
              <span className="ereceipt-meta-val">{formattedDate}</span>
            </div>
            <div className="ereceipt-meta-item">
              <span className="ereceipt-meta-label">Payment Method</span>
              <span className="ereceipt-meta-val" style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                <span style={{ width: 8, height: 8, borderRadius: '50%', background: '#10b981', display: 'inline-block' }} />
                {order.paymentMethod}
              </span>
            </div>
            <div className="ereceipt-meta-item align-right">
              <span className="ereceipt-meta-label">Channel</span>
              <span className="ereceipt-meta-val" style={{ textTransform: 'capitalize' }}>
                {order.salesChannel?.toLowerCase()}
              </span>
            </div>

            {order.mpesaTransactionCode && (
              <div className="ereceipt-meta-item full-width">
                <span style={{ color: '#64748b', fontSize: 12, fontWeight: 500 }}>M-PESA Reference:</span>
                <span className="ereceipt-meta-val font-mono" style={{ background: '#ffffff', padding: '2px 8px', borderRadius: 6, border: '1px solid #e2e8f0' }}>
                  {order.mpesaTransactionCode}
                </span>
              </div>
            )}

            {business.receiptShowCustomer && order.customerName && order.customerName !== 'Walk-in Customer' && (
              <div className="ereceipt-meta-item full-width">
                <span style={{ color: '#64748b', fontSize: 12, fontWeight: 500 }}>Customer:</span>
                <span className="ereceipt-meta-val">
                  {order.customerName} {order.customerPhone ? `(${order.customerPhone})` : ''}
                </span>
              </div>
            )}
          </div>
        </div>

        {/* Itemized Line Items */}
        <div className="ereceipt-items-section">
          <div className="ereceipt-section-title">Items Summary</div>
          {order.items.map((item) => (
            <div key={item.id} className="ereceipt-item-row">
              <div style={{ paddingRight: 16 }}>
                <div className="ereceipt-item-name">{item.productName}</div>
                <div className="ereceipt-item-sub">
                  <span>{item.quantity} × KES {Number(item.unitPrice).toLocaleString('en-KE', { minimumFractionDigits: 2 })}</span>
                  {(item.discountAmount ?? 0) > 0 && (
                    <span style={{ color: '#047857', background: '#ecfdf5', padding: '1px 6px', borderRadius: 4, fontSize: 10, fontWeight: 700 }}>
                      -KES {item.discountAmount}
                    </span>
                  )}
                  {item.complimentary && (
                    <span style={{ color: '#4338ca', background: '#e0e7ff', padding: '1px 6px', borderRadius: 4, fontSize: 10, fontWeight: 700 }}>
                      Free
                    </span>
                  )}
                </div>
                {item.modifiers && item.modifiers.length > 0 && (
                  <div style={{ fontSize: 11, color: '#94a3b8', marginTop: 2 }}>
                    + {item.modifiers.map(m => `${m.name} (${m.priceDelta >= 0 ? '+' : ''}${m.priceDelta})`).join(', ')}
                  </div>
                )}
                {item.itemNote && (
                  <div style={{ fontSize: 11, color: '#94a3b8', fontStyle: 'italic', marginTop: 2 }}>
                    Note: {item.itemNote}
                  </div>
                )}
              </div>
              <div className="ereceipt-item-total">
                KES {Number(item.lineTotal).toLocaleString('en-KE', { minimumFractionDigits: 2 })}
              </div>
            </div>
          ))}

          {/* Breakdown & Totals */}
          <div className="ereceipt-totals-block">
            {business.receiptShowTax && (order.taxAmount ?? 0) > 0 && (
              <>
                <div className="ereceipt-subtotal-row">
                  <span>Net Subtotal:</span>
                  <span style={{ fontFamily: 'monospace' }}>
                    KES {Number(order.baseAmount || (order.subtotal - (order.taxAmount || 0))).toLocaleString('en-KE', { minimumFractionDigits: 2 })}
                  </span>
                </div>
                <div className="ereceipt-subtotal-row">
                  <span>VAT ({((order.taxRate || 0.16) * 100).toFixed(0)}%):</span>
                  <span style={{ fontFamily: 'monospace' }}>
                    KES {Number(order.taxAmount || 0).toLocaleString('en-KE', { minimumFractionDigits: 2 })}
                  </span>
                </div>
              </>
            )}

            <div className="ereceipt-grand-total">
              <span className="ereceipt-grand-total-label">Total Paid</span>
              <span className="ereceipt-grand-total-amount">
                KES {Number(order.subtotal).toLocaleString('en-KE', { minimumFractionDigits: 2 })}
              </span>
            </div>
          </div>
        </div>

        {/* KRA eTIMS Fiscal Box (if verified with KRA) */}
        {kraFiscal && (kraFiscal.invoiceNumber || kraFiscal.rcptSign) && (
          <div className="ereceipt-kra-card">
            <div className="ereceipt-kra-title">
              <FileCheck size={16} color="#059669" />
              KRA eTIMS Fiscal Information
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 4, fontSize: 11, fontFamily: 'monospace', color: '#475569' }}>
              {kraFiscal.invoiceNumber && (
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <span style={{ color: '#94a3b8' }}>Invoice No:</span>
                  <span style={{ fontWeight: 700, color: '#0f172a' }}>{kraFiscal.invoiceNumber}</span>
                </div>
              )}
              {kraFiscal.sdcId && (
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <span style={{ color: '#94a3b8' }}>SDC ID:</span>
                  <span>{kraFiscal.sdcId}</span>
                </div>
              )}
              {kraFiscal.rcptSign && (
                <div style={{ marginTop: 2 }}>
                  <span style={{ color: '#94a3b8', display: 'block', fontSize: 10 }}>Receipt Signature:</span>
                  <span style={{ wordBreak: 'break-all', fontSize: 10, color: '#64748b' }}>{kraFiscal.rcptSign}</span>
                </div>
              )}
            </div>

            {kraFiscal.qrCodeBase64 && (
              <div style={{ textAlign: 'center', marginTop: 12, paddingTop: 10, borderTop: '1px solid #e2e8f0' }}>
                <img
                  src={`data:image/png;base64,${kraFiscal.qrCodeBase64}`}
                  alt="KRA Fiscal QR"
                  style={{ width: 90, height: 90, margin: '0 auto', display: 'block', background: '#fff', padding: 4, borderRadius: 8, border: '1px solid #cbd5e1' }}
                />
                <span style={{ fontSize: 10, color: '#94a3b8', display: 'block', marginTop: 4 }}>Scan for KRA fiscal verification</span>
              </div>
            )}
          </div>
        )}

        {/* Verification QR Code */}
        <div className="ereceipt-qr-box">
          {qrCodeDataUrl ? (
            <div className="ereceipt-qr-img-wrapper">
              <img src={qrCodeDataUrl} alt="e-Receipt QR" />
            </div>
          ) : (
            <div className="ereceipt-qr-img-wrapper" style={{ width: 140, height: 140, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <Receipt size={32} color="#94a3b8" />
            </div>
          )}

          <p className="ereceipt-qr-title">Scan to Verify or Save Receipt</p>
          <p className="ereceipt-qr-desc">Works with any smartphone camera or QR reader</p>

          {(branch?.receiptFooter || business.receiptFooter) && (
            <div className="ereceipt-footer-message">
              "{branch?.receiptFooter || business.receiptFooter}"
            </div>
          )}

          <div className="ereceipt-power-tag">
            <span>Powered by</span>
            <strong>Biashara360</strong>
            <span>· Electronic Receipts</span>
          </div>
        </div>
      </div>

      {/* Outside Footer */}
      <footer className="ereceipt-page-footer">
        <p>This is a tamper-evident digital receipt issued by {business.name}.</p>
        <p>&copy; {new Date().getFullYear()} Biashara360 Kenya. All rights reserved.</p>
      </footer>
    </div>
  )
}

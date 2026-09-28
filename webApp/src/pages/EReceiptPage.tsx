import React, { useEffect, useState, useRef } from 'react'
import { useParams, Link } from 'react-router-dom'
import QRCode from 'qrcode'
import {
  CheckCircle2,
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
  ExternalLink,
  ChevronLeft,
  AlertCircle
} from 'lucide-react'
import { receiptApi, PublicReceiptResponse } from '../services/api'

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
            width: 256,
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
      <div className="min-h-screen bg-slate-50 flex flex-col items-center justify-center p-4">
        <div className="w-full max-w-md bg-white rounded-3xl p-8 border border-slate-200/80 shadow-sm text-center animate-pulse">
          <div className="w-16 h-16 bg-slate-200 rounded-2xl mx-auto mb-4" />
          <div className="h-6 bg-slate-200 rounded-lg w-3/4 mx-auto mb-3" />
          <div className="h-4 bg-slate-100 rounded w-1/2 mx-auto mb-8" />
          <div className="space-y-3 mb-8">
            <div className="h-4 bg-slate-100 rounded" />
            <div className="h-4 bg-slate-100 rounded" />
            <div className="h-4 bg-slate-100 rounded" />
          </div>
          <div className="h-10 bg-slate-200 rounded-xl w-full" />
        </div>
      </div>
    )
  }

  if (error || !receiptData) {
    return (
      <div className="min-h-screen bg-slate-50 flex flex-col items-center justify-center p-4">
        <div className="w-full max-w-md bg-white rounded-3xl p-8 border border-slate-200 shadow-sm text-center">
          <div className="w-14 h-14 bg-rose-50 text-rose-600 rounded-2xl flex items-center justify-center mx-auto mb-4">
            <AlertCircle className="w-7 h-7" />
          </div>
          <h2 className="text-xl font-bold text-slate-800 mb-2">Receipt Not Found</h2>
          <p className="text-slate-600 text-sm mb-6 leading-relaxed">
            {error || 'We could not locate this electronic receipt. Please check the link or contact the merchant.'}
          </p>
          <Link
            to="/"
            className="inline-flex items-center gap-2 px-5 py-2.5 bg-slate-900 hover:bg-slate-800 text-white text-sm font-medium rounded-xl transition"
          >
            <ChevronLeft className="w-4 h-4" />
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
    <div className="min-h-screen bg-gradient-to-b from-slate-100 via-slate-50 to-slate-100 py-8 px-4 sm:px-6 print:p-0 print:bg-white text-slate-800 flex flex-col items-center">
      {/* Top Floating Action Bar (Hidden in Print) */}
      <div className="w-full max-w-lg mb-6 flex items-center justify-between gap-3 print:hidden">
        <div className="flex items-center gap-2 text-xs font-semibold text-emerald-800 bg-emerald-50 px-3 py-1.5 rounded-full border border-emerald-200/80">
          <ShieldCheck className="w-4 h-4 text-emerald-600" />
          Verified Official e-Receipt
        </div>

        <div className="flex items-center gap-2">
          <button
            onClick={handleCopyLink}
            title="Copy digital receipt link"
            className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-white hover:bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium text-slate-700 shadow-sm transition active:scale-95"
          >
            {copied ? <Check className="w-3.5 h-3.5 text-emerald-600" /> : <Copy className="w-3.5 h-3.5 text-slate-500" />}
            {copied ? 'Copied' : 'Copy'}
          </button>

          <button
            onClick={handleWhatsAppShare}
            title="Share on WhatsApp"
            className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl text-xs font-semibold shadow-sm transition active:scale-95"
          >
            <Share2 className="w-3.5 h-3.5" />
            WhatsApp
          </button>

          <button
            onClick={handlePrint}
            title="Print or Save as PDF"
            className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-slate-900 hover:bg-slate-800 text-white rounded-xl text-xs font-semibold shadow-sm transition active:scale-95"
          >
            <Printer className="w-3.5 h-3.5" />
            Print
          </button>
        </div>
      </div>

      {/* Main Electronic Receipt Slip */}
      <div
        ref={receiptCardRef}
        id="receipt-print-area"
        className="w-full max-w-lg bg-white rounded-3xl shadow-xl shadow-slate-200/60 border border-slate-200/90 overflow-hidden print:shadow-none print:border-none print:max-w-full print:rounded-none"
      >
        {/* Top Status Banner */}
        <div className={`py-2.5 px-6 text-center text-xs font-bold uppercase tracking-wider ${
          isPaid ? 'bg-emerald-600 text-white' : isFailed ? 'bg-rose-600 text-white' : 'bg-amber-500 text-slate-900'
        }`}>
          {isPaid ? '✓ Transaction Completed · Official Receipt' : `Payment Status: ${order.paymentStatus}`}
        </div>

        {/* Business Header Section */}
        <div className="p-6 sm:p-8 text-center border-b border-slate-100">
          {business.receiptLogo ? (
            <img
              src={business.receiptLogo}
              alt={business.name}
              className="mx-auto mb-3 object-contain"
              style={{
                maxWidth: `${business.receiptLogoWidthMm || 42}mm`,
                maxHeight: `${business.receiptLogoHeightMm || 24}mm`
              }}
            />
          ) : (
            <div className="w-14 h-14 bg-gradient-to-tr from-emerald-600 to-teal-500 text-white font-extrabold text-2xl rounded-2xl flex items-center justify-center mx-auto mb-3 shadow-md shadow-emerald-500/20">
              {business.name.charAt(0).toUpperCase()}
            </div>
          )}

          <h1 className="text-xl sm:text-2xl font-black text-slate-900 tracking-tight mb-1">
            {business.name}
          </h1>

          {branch && (
            <div className="inline-flex items-center gap-1.5 px-2.5 py-0.5 bg-slate-100 text-slate-700 text-xs font-medium rounded-md mb-2">
              <Building2 className="w-3 h-3 text-slate-500" />
              {branch.name} {branch.code ? `(${branch.code})` : ''}
            </div>
          )}

          <div className="text-xs text-slate-500 space-y-0.5 mt-1 font-normal">
            {(branch?.address || business.address) && (
              <p className="flex items-center justify-center gap-1">
                <MapPin className="w-3 h-3 text-slate-400" />
                {branch?.address || business.address} {business.county ? `· ${business.county}` : ''}
              </p>
            )}
            {(branch?.phone || business.phone) && (
              <p className="flex items-center justify-center gap-1">
                <Phone className="w-3 h-3 text-slate-400" />
                {branch?.phone || business.phone}
              </p>
            )}
            {business.email && (
              <p className="flex items-center justify-center gap-1">
                <Mail className="w-3 h-3 text-slate-400" />
                {business.email}
              </p>
            )}
            {business.kraPin && (
              <p className="font-mono text-slate-700 font-medium pt-1">
                KRA PIN: <span className="font-bold">{business.kraPin}</span>
              </p>
            )}
          </div>

          {(branch?.receiptHeader || business.receiptHeader) && (
            <p className="text-xs font-medium text-emerald-700 bg-emerald-50/80 rounded-lg py-1.5 px-3 mt-3.5 border border-emerald-100">
              {branch?.receiptHeader || business.receiptHeader}
            </p>
          )}
        </div>

        {/* Perforated Divider Effect */}
        <div className="relative flex items-center justify-center my-0 bg-white">
          <div className="absolute -left-3.5 w-7 h-7 bg-slate-100 rounded-full border-r border-slate-200/90 print:hidden" />
          <div className="w-full border-t-2 border-dashed border-slate-200" />
          <div className="absolute -right-3.5 w-7 h-7 bg-slate-100 rounded-full border-l border-slate-200/90 print:hidden" />
        </div>

        {/* Order Meta Info */}
        <div className="p-6 sm:p-8 bg-slate-50/60 text-xs text-slate-600 border-b border-slate-100">
          <div className="grid grid-cols-2 gap-y-2.5">
            <div>
              <span className="text-slate-400 block uppercase text-[10px] tracking-wider font-semibold">Order / Receipt</span>
              <span className="font-mono font-bold text-slate-900 text-sm">#{order.orderNumber}</span>
            </div>
            <div className="text-right">
              <span className="text-slate-400 block uppercase text-[10px] tracking-wider font-semibold">Date & Time</span>
              <span className="font-medium text-slate-800">{formattedDate}</span>
            </div>
            <div>
              <span className="text-slate-400 block uppercase text-[10px] tracking-wider font-semibold">Payment Method</span>
              <span className="font-semibold text-slate-800 flex items-center gap-1.5 mt-0.5">
                <span className="w-2 h-2 rounded-full bg-emerald-500 inline-block" />
                {order.paymentMethod}
              </span>
            </div>
            <div className="text-right">
              <span className="text-slate-400 block uppercase text-[10px] tracking-wider font-semibold">Channel / Type</span>
              <span className="font-medium text-slate-800 capitalize">
                {order.salesChannel?.toLowerCase()} · {order.serviceType?.toLowerCase() || 'in-store'}
              </span>
            </div>

            {order.mpesaTransactionCode && (
              <div className="col-span-2 pt-1 border-t border-slate-200/60 mt-1 flex justify-between items-center">
                <span className="text-slate-500 font-medium">M-PESA Code:</span>
                <span className="font-mono font-bold text-slate-900 bg-white px-2 py-0.5 rounded border border-slate-200">
                  {order.mpesaTransactionCode}
                </span>
              </div>
            )}

            {business.receiptShowCustomer && order.customerName && order.customerName !== 'Walk-in Customer' && (
              <div className="col-span-2 pt-1 border-t border-slate-200/60 mt-1 flex justify-between items-center">
                <span className="text-slate-500 font-medium">Customer:</span>
                <span className="font-medium text-slate-800">
                  {order.customerName} {order.customerPhone ? `(${order.customerPhone})` : ''}
                </span>
              </div>
            )}
          </div>
        </div>

        {/* Itemized Line Items */}
        <div className="p-6 sm:p-8">
          <h2 className="text-[11px] font-bold uppercase tracking-wider text-slate-400 mb-3">Items Summary</h2>
          <div className="space-y-3">
            {order.items.map((item) => (
              <div key={item.id} className="flex justify-between items-start text-sm pb-2.5 border-b border-slate-100 last:border-b-0 last:pb-0">
                <div className="pr-4">
                  <div className="font-semibold text-slate-900 leading-snug">
                    {item.productName}
                  </div>
                  <div className="text-xs text-slate-500 mt-0.5 flex items-center gap-1.5">
                    <span className="font-mono font-medium">{item.quantity} × KES {Number(item.unitPrice).toLocaleString('en-KE', { minimumFractionDigits: 2 })}</span>
                    {(item.discountAmount ?? 0) > 0 && (
                      <span className="text-emerald-700 bg-emerald-50 px-1.5 py-0.2 rounded text-[10px] font-semibold">
                        -KES {item.discountAmount} off
                      </span>
                    )}
                    {item.complimentary && (
                      <span className="text-indigo-700 bg-indigo-50 px-1.5 py-0.2 rounded text-[10px] font-semibold">
                        Free
                      </span>
                    )}
                  </div>
                  {item.modifiers && item.modifiers.length > 0 && (
                    <div className="text-[11px] text-slate-400 mt-0.5">
                      + {item.modifiers.map(m => `${m.name} (${m.priceDelta >= 0 ? '+' : ''}${m.priceDelta})`).join(', ')}
                    </div>
                  )}
                  {item.itemNote && (
                    <div className="text-[11px] text-slate-400 italic mt-0.5">
                      Note: {item.itemNote}
                    </div>
                  )}
                </div>
                <div className="text-right font-mono font-bold text-slate-900 whitespace-nowrap text-sm">
                  KES {Number(item.lineTotal).toLocaleString('en-KE', { minimumFractionDigits: 2 })}
                </div>
              </div>
            ))}
          </div>

          {/* Breakdown & Totals */}
          <div className="mt-6 pt-4 border-t-2 border-slate-900/10 space-y-2 text-xs">
            {business.receiptShowTax && (order.taxAmount ?? 0) > 0 && (
              <>
                <div className="flex justify-between text-slate-500">
                  <span>Net Subtotal:</span>
                  <span className="font-mono">KES {Number(order.baseAmount || (order.subtotal - (order.taxAmount || 0))).toLocaleString('en-KE', { minimumFractionDigits: 2 })}</span>
                </div>
                <div className="flex justify-between text-slate-500">
                  <span>VAT ({((order.taxRate || 0.16) * 100).toFixed(0)}%):</span>
                  <span className="font-mono">KES {Number(order.taxAmount || 0).toLocaleString('en-KE', { minimumFractionDigits: 2 })}</span>
                </div>
              </>
            )}

            <div className="flex justify-between items-baseline pt-2 border-t border-slate-200">
              <span className="text-base font-bold text-slate-900">Total Paid</span>
              <span className="text-2xl font-black font-mono text-emerald-700">
                KES {Number(order.subtotal).toLocaleString('en-KE', { minimumFractionDigits: 2 })}
              </span>
            </div>
          </div>
        </div>

        {/* KRA eTIMS Fiscal Box (if verified with KRA) */}
        {kraFiscal && (kraFiscal.invoiceNumber || kraFiscal.rcptSign) && (
          <div className="mx-6 sm:mx-8 mb-6 p-4 bg-slate-50 rounded-2xl border border-slate-200 text-xs">
            <div className="flex items-center gap-2 mb-2 font-bold text-slate-800">
              <FileCheck className="w-4 h-4 text-emerald-600" />
              KRA eTIMS Fiscal Information
            </div>
            <div className="space-y-1 font-mono text-[11px] text-slate-600">
              {kraFiscal.invoiceNumber && (
                <div className="flex justify-between">
                  <span className="text-slate-400">Invoice No:</span>
                  <span className="font-bold text-slate-800">{kraFiscal.invoiceNumber}</span>
                </div>
              )}
              {kraFiscal.sdcId && (
                <div className="flex justify-between">
                  <span className="text-slate-400">SDC ID:</span>
                  <span>{kraFiscal.sdcId}</span>
                </div>
              )}
              {kraFiscal.rcptSign && (
                <div className="pt-1">
                  <span className="text-slate-400 block text-[10px]">Receipt Signature:</span>
                  <span className="break-all text-[10px] text-slate-500">{kraFiscal.rcptSign}</span>
                </div>
              )}
            </div>

            {kraFiscal.qrCodeBase64 && (
              <div className="text-center mt-3 pt-3 border-t border-slate-200/80">
                <img
                  src={`data:image/png;base64,${kraFiscal.qrCodeBase64}`}
                  alt="KRA Fiscal QR"
                  className="w-24 h-24 mx-auto rounded border border-slate-200 p-1 bg-white"
                />
                <span className="text-[10px] text-slate-400 block mt-1">Scan for KRA fiscal verification</span>
              </div>
            )}
          </div>
        )}

        {/* Verification QR Code & Digital Seal */}
        <div className="p-6 bg-slate-50/80 border-t border-slate-100 text-center">
          {qrCodeDataUrl ? (
            <div className="inline-block bg-white p-2.5 rounded-2xl border border-slate-200/90 shadow-sm mb-2.5">
              <img
                src={qrCodeDataUrl}
                alt="e-Receipt QR"
                className="w-32 h-32 mx-auto"
              />
            </div>
          ) : (
            <div className="w-32 h-32 mx-auto bg-slate-100 rounded-2xl flex items-center justify-center text-slate-400 mb-2">
              <Receipt className="w-8 h-8" />
            </div>
          )}

          <p className="text-xs font-semibold text-slate-700">Scan to Verify or Keep Receipt</p>
          <p className="text-[11px] text-slate-400 mt-0.5">Compatible with any smartphone camera or QR reader</p>

          {(branch?.receiptFooter || business.receiptFooter) && (
            <div className="text-xs font-medium text-slate-600 italic mt-4 pt-3 border-t border-slate-200/60">
              "{branch?.receiptFooter || business.receiptFooter}"
            </div>
          )}

          <div className="mt-4 pt-3 border-t border-slate-200/60 flex items-center justify-center gap-1.5 text-[11px] text-slate-400 font-medium">
            <span>Powered by</span>
            <span className="font-bold text-slate-700">Biashara360</span>
            <span>· Electronic Receipts</span>
          </div>
        </div>
      </div>

      {/* Footer Info & Customer Support */}
      <footer className="mt-8 text-center text-xs text-slate-400 print:hidden space-y-1">
        <p>This is a tamper-evident digital receipt issued by {business.name}.</p>
        <p>&copy; {new Date().getFullYear()} Biashara360 Kenya. All rights reserved.</p>
      </footer>

      {/* Print Specific CSS */}
      <style>{`
        @media print {
          body {
            background: white !important;
            color: black !important;
            margin: 0 !important;
            padding: 0 !important;
          }
          #receipt-print-area {
            box-shadow: none !important;
            border: none !important;
            max-width: 100% !important;
            width: 100% !important;
            margin: 0 !important;
            padding: 0 !important;
          }
          .print\\:hidden {
            display: none !important;
          }
        }
      `}</style>
    </div>
  )
}

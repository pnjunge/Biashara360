import React, { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Receipt, Plus, Search, Package, AlertTriangle, FileText,
  CheckCircle2, Clock, Trash2, Eye, X, Check, Store, Phone, CreditCard, Hash
} from 'lucide-react'
import {
  KpiCard, StatusBadge, PageHeader, Card, Btn, DataTable, AlertBanner, Modal, Input, Select
} from '../components/ui'
import {
  purchaseApi, productApi, PurchaseInvoice, ProductResponse, CreatePurchaseInvoiceRequest
} from '../services/api'
import { useAuth } from '../App'

interface LineItemDraft {
  productId: string
  productName: string
  sku: string
  quantity: number
  unitCost: number
}

export default function PurchasesPage() {
  const navigate = useNavigate()
  const { user } = useAuth()

  const [purchases, setPurchases] = useState<PurchaseInvoice[]>([])
  const [products, setProducts] = useState<ProductResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [error, setError] = useState('')
  const [successMsg, setSuccessMsg] = useState('')

  // Dialog states
  const [showRecordModal, setShowRecordModal] = useState(false)
  const [viewingInvoice, setViewingInvoice] = useState<PurchaseInvoice | null>(null)
  const [saving, setSaving] = useState(false)

  // Form states for new purchase invoice
  const [invoiceNumber, setInvoiceNumber] = useState('')
  const [supplierName, setSupplierName] = useState('')
  const [supplierPhone, setSupplierPhone] = useState('')
  const [paymentMethod, setPaymentMethod] = useState('CASH')
  const [paymentStatus, setPaymentStatus] = useState('PAID')
  const [invoiceDate, setInvoiceDate] = useState(() => new Date().toISOString().split('T')[0])
  const [notes, setNotes] = useState('')
  const [draftItems, setDraftItems] = useState<LineItemDraft[]>([])

  const loadData = async () => {
    setLoading(true)
    setError('')
    try {
      const [purchasesRes, productsRes] = await Promise.all([
        purchaseApi.list(),
        productApi.list()
      ])
      if (purchasesRes.success && purchasesRes.data) {
        setPurchases(purchasesRes.data)
      }
      if (productsRes.success && productsRes.data) {
        setProducts(productsRes.data)
      }
    } catch (e: any) {
      setError(e.response?.data?.message || 'Failed to load purchase records.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadData()
  }, [])

  const resetForm = () => {
    const randomSuffix = Math.floor(1000 + Math.random() * 9000)
    setInvoiceNumber(`INV-${new Date().getFullYear()}-${randomSuffix}`)
    setSupplierName('')
    setSupplierPhone('')
    setPaymentMethod('CASH')
    setPaymentStatus('PAID')
    setInvoiceDate(new Date().toISOString().split('T')[0])
    setNotes('')
    const firstProd = products[0]
    if (firstProd) {
      setDraftItems([{
        productId: firstProd.id,
        productName: firstProd.name,
        sku: firstProd.sku,
        quantity: 1,
        unitCost: firstProd.buyingPrice || 1000
      }])
    } else {
      setDraftItems([])
    }
    setError('')
  }

  const openRecordModal = () => {
    resetForm()
    setShowRecordModal(true)
  }

  const handleAddLineItem = () => {
    const firstProd = products[0]
    setDraftItems(prev => [
      ...prev,
      {
        productId: firstProd?.id || '',
        productName: firstProd?.name || '',
        sku: firstProd?.sku || '',
        quantity: 1,
        unitCost: firstProd?.buyingPrice || 0
      }
    ])
    setError('')
  }

  const handleUpdateItem = (index: number, updates: Partial<LineItemDraft>) => {
    setDraftItems(prev => prev.map((item, i) => {
      if (i !== index) return item
      const updated = { ...item, ...updates }
      if (updates.productId) {
        const found = products.find(p => p.id === updates.productId)
        if (found) {
          updated.productName = found.name
          updated.sku = found.sku
          if (found.buyingPrice > 0) {
            updated.unitCost = found.buyingPrice
          }
        }
      }
      return updated
    }))
  }

  const handleRemoveLineItem = (index: number) => {
    setDraftItems(prev => prev.filter((_, i) => i !== index))
  }

  const computedTotal = draftItems.reduce((acc, item) => acc + (item.quantity * item.unitCost), 0)

  const handleSavePurchase = async () => {
    if (!invoiceNumber.trim()) {
      setError('Invoice number is required.')
      return
    }
    if (!supplierName.trim()) {
      setError('Supplier name is required.')
      return
    }
    if (draftItems.length === 0) {
      setError('Please add at least one line item to the invoice.')
      return
    }
    if (draftItems.some(it => !it.productId || it.quantity <= 0)) {
      setError('Please ensure every line item has a selected product and valid quantity.')
      return
    }

    setSaving(true)
    setError('')
    try {
      const payload: CreatePurchaseInvoiceRequest = {
        invoiceNumber: invoiceNumber.trim().toUpperCase(),
        supplierName: supplierName.trim(),
        supplierPhone: supplierPhone.trim() || null,
        totalAmount: computedTotal,
        paymentStatus,
        paymentMethod,
        notes: notes.trim() || null,
        invoiceDate,
        items: draftItems.map(item => ({
          productId: item.productId,
          productName: item.productName,
          sku: item.sku,
          quantity: item.quantity,
          unitCost: item.unitCost,
          totalCost: item.quantity * item.unitCost
        }))
      }

      const res = await purchaseApi.create(payload)
      if (res.success && res.data) {
        setShowRecordModal(false)
        setSuccessMsg(`Invoice #${res.data.invoiceNumber} recorded! Stock augmented automatically.`)
        await loadData()
        setTimeout(() => setSuccessMsg(''), 5000)
      } else {
        setError(res.message || 'Failed to save purchase invoice.')
      }
    } catch (e: any) {
      setError(e.response?.data?.message || 'Network error while recording invoice.')
    } finally {
      setSaving(false)
    }
  }

  // Filtered Purchases
  const filteredPurchases = purchases.filter(p => {
    const q = search.trim().toLowerCase()
    const matchesSearch = !q ||
      p.invoiceNumber.toLowerCase().includes(q) ||
      p.supplierName.toLowerCase().includes(q) ||
      (p.notes && p.notes.toLowerCase().includes(q)) ||
      p.items.some(it => it.productName.toLowerCase().includes(q) || it.sku.toLowerCase().includes(q))

    const matchesStatus = statusFilter === 'ALL' || p.paymentStatus === statusFilter
    return matchesSearch && matchesStatus
  })

  // KPI calculations
  const totalAmount = purchases.reduce((acc, p) => acc + p.totalAmount, 0)
  const totalUnits = purchases.reduce((acc, p) => acc + p.items.reduce((s, it) => s + it.quantity, 0), 0)
  const pendingCount = purchases.filter(p => p.paymentStatus === 'PENDING').length

  const paymentOptions = [
    { value: 'CASH', label: 'CASH' },
    { value: 'MPESA', label: 'MPESA' },
    { value: 'BANK_TRANSFER', label: 'BANK TRANSFER' },
    { value: 'CREDIT', label: 'CREDIT' }
  ]

  const paymentStatusOptions = [
    { value: 'PAID', label: 'PAID' },
    { value: 'PENDING', label: 'PENDING' },
    { value: 'PARTIAL', label: 'PARTIAL' }
  ]

  return (
    <div className="fade-in" style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
      {/* Page Header */}
      <PageHeader
        title="Purchases & Supplier Invoices"
        action={
          <div style={{ display: 'flex', gap: 8 }}>
            <Btn variant="secondary" icon={<Package size={14} />} onClick={() => navigate('/inventory')}>
              Go to Inventory
            </Btn>
            <Btn icon={<Plus size={14} />} onClick={openRecordModal}>
              Record Purchase Invoice
            </Btn>
          </div>
        }
      />

      {/* Sub-nav Tab Switcher augmenting Inventory */}
      <div style={{
        display: 'flex',
        alignItems: 'center',
        gap: 12,
        borderBottom: '1px solid var(--b360-border)',
        paddingBottom: 8
      }}>
        <button
          onClick={() => navigate('/inventory')}
          style={{
            background: 'none',
            border: 'none',
            padding: '8px 16px',
            fontSize: 14,
            fontWeight: 500,
            color: 'var(--b360-text-secondary)',
            cursor: 'pointer',
            borderRadius: 6
          }}
        >
          Products & Stock
        </button>
        <button
          style={{
            background: 'var(--b360-green-subtle)',
            border: '1px solid var(--b360-green)',
            padding: '8px 16px',
            fontSize: 14,
            fontWeight: 600,
            color: 'var(--b360-green)',
            cursor: 'pointer',
            borderRadius: 6
          }}
        >
          Purchase Invoices (Stock In)
        </button>
      </div>

      {successMsg && (
        <AlertBanner
          message={successMsg}
          icon={<CheckCircle2 size={16} />}
          color="var(--b360-green)"
        />
      )}

      {/* KPIs */}
      <div className="responsive-grid responsive-grid-4" style={{ gap: 12 }}>
        <KpiCard
          title="Total Purchases"
          value={`KES ${totalAmount.toLocaleString('en-KE', { maximumFractionDigits: 0 })}`}
          change="Supplier expenditures"
          icon={<Receipt size={18} />}
          color="var(--b360-green)"
        />
        <KpiCard
          title="Invoices Recorded"
          value={`${purchases.length}`}
          change="Vendor delivery bills"
          icon={<FileText size={18} />}
          color="var(--b360-blue)"
        />
        <KpiCard
          title="Stock Influx"
          value={`${totalUnits} units`}
          change="Augmented to products"
          icon={<Package size={18} />}
          color="#7C3AED"
        />
        <KpiCard
          title="Pending Due"
          value={`${pendingCount} invoices`}
          change="Outstanding supplier credit"
          icon={<Clock size={18} />}
          color={pendingCount > 0 ? 'var(--b360-amber)' : 'var(--b360-green)'}
        />
      </div>

      {/* Main Table Card */}
      <Card>
        {/* Toolbar */}
        <div style={{
          padding: '16px 20px',
          borderBottom: '1px solid var(--b360-border)',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          flexWrap: 'wrap',
          gap: 12
        }}>
          <div style={{ position: 'relative', flex: 1, maxWidth: 360 }}>
            <Search size={14} style={{ position: 'absolute', left: 10, top: '50%', transform: 'translateY(-50%)', color: 'var(--b360-text-secondary)' }} />
            <input
              value={search}
              onChange={e => setSearch(e.target.value)}
              placeholder="Search by Invoice #, supplier, or SKU..."
              style={{
                width: '100%',
                padding: '8px 12px 8px 32px',
                borderRadius: 8,
                border: '1px solid var(--b360-border)',
                fontSize: 13,
                outline: 'none',
                background: 'var(--b360-surface)'
              }}
            />
          </div>

          <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
            <span style={{ fontSize: 13, color: 'var(--b360-text-secondary)' }}>Status:</span>
            {['ALL', 'PAID', 'PENDING', 'PARTIAL'].map(st => (
              <button
                key={st}
                onClick={() => setStatusFilter(st)}
                style={{
                  padding: '4px 10px',
                  borderRadius: 6,
                  border: statusFilter === st ? '1px solid var(--b360-green)' : '1px solid var(--b360-border)',
                  background: statusFilter === st ? 'var(--b360-green-subtle)' : 'transparent',
                  color: statusFilter === st ? 'var(--b360-green)' : 'var(--b360-text-secondary)',
                  fontSize: 12,
                  fontWeight: statusFilter === st ? 600 : 400,
                  cursor: 'pointer'
                }}
              >
                {st}
              </button>
            ))}
          </div>
        </div>

        {/* Invoices List */}
        {loading ? (
          <div style={{ padding: 40, textAlign: 'center', color: 'var(--b360-text-secondary)' }}>
            Loading purchase invoices...
          </div>
        ) : filteredPurchases.length === 0 ? (
          <div style={{ padding: 40, textAlign: 'center', color: 'var(--b360-text-secondary)' }}>
            No purchase invoices recorded yet. Click 'Record Purchase Invoice' to add stock.
          </div>
        ) : (
          <DataTable
            headers={['Invoice #', 'Supplier', 'Invoice Date', 'Items Stocked', 'Total Amount', 'Payment', 'Status', 'Action']}
            rows={filteredPurchases.map(row => {
              const totalQty = row.items.reduce((s, it) => s + it.quantity, 0)
              return [
                <div key="inv" style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                  <FileText size={15} style={{ color: 'var(--b360-green)' }} />
                  <strong style={{ color: 'var(--b360-green)', fontSize: 13 }}>
                    #{row.invoiceNumber}
                  </strong>
                </div>,
                <div key="sup">
                  <div style={{ fontWeight: 600, fontSize: 13 }}>{row.supplierName}</div>
                  {row.supplierPhone && (
                    <div style={{ fontSize: 11, color: 'var(--b360-text-secondary)' }}>
                      {row.supplierPhone}
                    </div>
                  )}
                </div>,
                <div key="date" style={{ fontSize: 12, color: 'var(--b360-text-secondary)' }}>
                  {row.invoiceDate ? row.invoiceDate.split('T')[0] : row.createdAt.split('T')[0]}
                </div>,
                <span key="items" style={{ fontSize: 12, fontWeight: 500 }}>
                  {row.items.length} line{row.items.length === 1 ? '' : 's'} · {totalQty} units
                </span>,
                <strong key="total" style={{ fontSize: 13 }}>
                  KES {row.totalAmount.toLocaleString('en-KE', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                </strong>,
                <span key="method" style={{ fontSize: 12, color: 'var(--b360-text-secondary)' }}>
                  {row.paymentMethod.replace('_', ' ')}
                </span>,
                <StatusBadge key="status" status={row.paymentStatus} />,
                <Btn
                  key="action"
                  small
                  variant="secondary"
                  icon={<Eye size={13} />}
                  onClick={() => setViewingInvoice(row)}
                >
                  View
                </Btn>
              ]
            })}
          />
        )}
      </Card>

      {/* Record Purchase Modal (Matching User Mockup) */}
      {showRecordModal && (
        <Modal
          wide
          title=""
          onClose={() => setShowRecordModal(false)}
        >
          <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            {/* Custom Header matching mockup */}
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                <div style={{
                  width: 44,
                  height: 44,
                  borderRadius: 10,
                  background: '#E6F7F0',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  color: '#059669'
                }}>
                  <Receipt size={24} />
                </div>
                <div>
                  <h3 style={{ margin: 0, fontSize: 18, fontWeight: 700, color: '#0F1F3A' }}>
                    Record Purchase Invoice
                  </h3>
                  <p style={{ margin: 0, fontSize: 12, color: '#64748B' }}>
                    Receive supplier stock and augment inventory counts
                  </p>
                </div>
              </div>
            </div>

            {error && (
              <p style={{ color: 'var(--b360-red)', fontSize: 12, margin: 0, fontWeight: 600 }}>
                {error}
              </p>
            )}

            {/* Step 1: Invoice & Supplier Details Card */}
            <div style={{
              border: '1px solid #E2E8F0',
              borderRadius: 12,
              padding: 16,
              display: 'flex',
              flexDirection: 'column',
              gap: 14
            }}>
              {/* Step 1 Header */}
              <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <div style={{
                  width: 24,
                  height: 24,
                  borderRadius: '50%',
                  background: '#2563EB',
                  color: 'white',
                  fontSize: 12,
                  fontWeight: 700,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center'
                }}>
                  1
                </div>
                <span style={{ fontSize: 14, fontWeight: 700, color: '#0F1F3A' }}>
                  Invoice & Supplier Details
                </span>
              </div>

              {/* Row 1 */}
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                <div style={{ display: 'flex', flexDirection: 'column', gap: 5 }}>
                  <label style={{ fontSize: 12, fontWeight: 600, color: '#475569' }}>
                    Invoice Number <span style={{ color: 'red' }}>*</span>
                  </label>
                  <div style={{
                    display: 'flex',
                    alignItems: 'center',
                    border: '1.5px solid #3B82F6',
                    borderRadius: 8,
                    padding: '8px 12px',
                    background: 'white',
                    gap: 8
                  }}>
                    <Hash size={16} style={{ color: '#059669', flexShrink: 0 }} />
                    <input
                      value={invoiceNumber}
                      onChange={e => setInvoiceNumber(e.target.value)}
                      placeholder="Enter invoice number"
                      style={{
                        border: 'none',
                        outline: 'none',
                        fontSize: 13,
                        width: '100%',
                        fontFamily: 'inherit'
                      }}
                    />
                  </div>
                </div>

                <div style={{ display: 'flex', flexDirection: 'column', gap: 5 }}>
                  <label style={{ fontSize: 12, fontWeight: 600, color: '#475569' }}>
                    Supplier Name <span style={{ color: 'red' }}>*</span>
                  </label>
                  <div style={{
                    display: 'flex',
                    alignItems: 'center',
                    border: '1px solid #CBD5E1',
                    borderRadius: 8,
                    padding: '8px 12px',
                    background: 'white',
                    gap: 8
                  }}>
                    <Store size={16} style={{ color: '#64748B', flexShrink: 0 }} />
                    <input
                      value={supplierName}
                      onChange={e => setSupplierName(e.target.value)}
                      placeholder="Search or select supplier"
                      style={{
                        border: 'none',
                        outline: 'none',
                        fontSize: 13,
                        width: '100%',
                        fontFamily: 'inherit'
                      }}
                    />
                  </div>
                </div>
              </div>

              {/* Row 2 */}
              <div style={{ display: 'grid', gridTemplateColumns: '1.2fr 1fr 1fr', gap: 12 }}>
                <div style={{ display: 'flex', flexDirection: 'column', gap: 5 }}>
                  <label style={{ fontSize: 12, fontWeight: 600, color: '#475569' }}>
                    Supplier Phone (Optional)
                  </label>
                  <div style={{
                    display: 'flex',
                    alignItems: 'center',
                    border: '1px solid #CBD5E1',
                    borderRadius: 8,
                    padding: '8px 12px',
                    background: 'white',
                    gap: 8
                  }}>
                    <Phone size={16} style={{ color: '#64748B', flexShrink: 0 }} />
                    <input
                      value={supplierPhone}
                      onChange={e => setSupplierPhone(e.target.value)}
                      placeholder="e.g. 0712 345 678"
                      style={{
                        border: 'none',
                        outline: 'none',
                        fontSize: 13,
                        width: '100%',
                        fontFamily: 'inherit'
                      }}
                    />
                  </div>
                </div>

                <div style={{ display: 'flex', flexDirection: 'column', gap: 5 }}>
                  <label style={{ fontSize: 12, fontWeight: 600, color: '#475569' }}>
                    Payment Method
                  </label>
                  <div style={{
                    display: 'flex',
                    alignItems: 'center',
                    border: '1px solid #CBD5E1',
                    borderRadius: 8,
                    padding: '8px 12px',
                    background: 'white',
                    gap: 8
                  }}>
                    <CreditCard size={16} style={{ color: '#64748B', flexShrink: 0 }} />
                    <select
                      value={paymentMethod}
                      onChange={e => setPaymentMethod(e.target.value)}
                      style={{
                        border: 'none',
                        outline: 'none',
                        fontSize: 13,
                        width: '100%',
                        background: 'transparent',
                        fontFamily: 'inherit',
                        fontWeight: 600
                      }}
                    >
                      {paymentOptions.map(o => (
                        <option key={o.value} value={o.value}>{o.label}</option>
                      ))}
                    </select>
                  </div>
                </div>

                <div style={{ display: 'flex', flexDirection: 'column', gap: 5 }}>
                  <label style={{ fontSize: 12, fontWeight: 600, color: '#475569' }}>
                    Payment Status
                  </label>
                  <div style={{
                    display: 'flex',
                    alignItems: 'center',
                    border: '1px solid #CBD5E1',
                    borderRadius: 8,
                    padding: '8px 12px',
                    background: 'white',
                    gap: 8
                  }}>
                    <CheckCircle2 size={16} style={{ color: '#059669', flexShrink: 0 }} />
                    <select
                      value={paymentStatus}
                      onChange={e => setPaymentStatus(e.target.value)}
                      style={{
                        border: 'none',
                        outline: 'none',
                        fontSize: 13,
                        width: '100%',
                        background: 'transparent',
                        fontFamily: 'inherit',
                        fontWeight: 600
                      }}
                    >
                      {paymentStatusOptions.map(o => (
                        <option key={o.value} value={o.value}>{o.label}</option>
                      ))}
                    </select>
                  </div>
                </div>
              </div>
            </div>

            {/* Step 2: Line Items Card */}
            <div style={{
              border: '1px solid #E2E8F0',
              borderRadius: 12,
              padding: 16,
              display: 'flex',
              flexDirection: 'column',
              gap: 14
            }}>
              {/* Step 2 Header with "+ Add Item" Button */}
              <div style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between'
              }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                  <div style={{
                    width: 24,
                    height: 24,
                    borderRadius: '50%',
                    background: '#2563EB',
                    color: 'white',
                    fontSize: 12,
                    fontWeight: 700,
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center'
                  }}>
                    2
                  </div>
                  <span style={{ fontSize: 14, fontWeight: 700, color: '#0F1F3A' }}>
                    Line Items (Augments Inventory Stock)
                  </span>
                </div>

                <button
                  type="button"
                  onClick={handleAddLineItem}
                  style={{
                    display: 'inline-flex',
                    alignItems: 'center',
                    gap: 6,
                    background: '#2563EB',
                    color: 'white',
                    border: 'none',
                    borderRadius: 8,
                    padding: '8px 14px',
                    fontSize: 13,
                    fontWeight: 600,
                    cursor: 'pointer'
                  }}
                >
                  <Plus size={15} /> Add Item
                </button>
              </div>

              {/* Items Table */}
              <div style={{
                border: '1px solid #E2E8F0',
                borderRadius: 8,
                overflow: 'hidden'
              }}>
                <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
                  <thead>
                    <tr style={{ background: '#F8FAFC', borderBottom: '1px solid #E2E8F0', textAlign: 'left', color: '#475569' }}>
                      <th style={{ padding: '10px 12px', width: 36, textAlign: 'center' }}>#</th>
                      <th style={{ padding: '10px 12px' }}>Product *</th>
                      <th style={{ padding: '10px 12px', width: 110, textAlign: 'center' }}>Qty Received *</th>
                      <th style={{ padding: '10px 12px', width: 140 }}>Unit Cost (KES) *</th>
                      <th style={{ padding: '10px 12px', width: 120, textAlign: 'right' }}>Total (KES)</th>
                      <th style={{ padding: '10px 12px', width: 60, textAlign: 'center' }}>Action</th>
                    </tr>
                  </thead>
                  <tbody>
                    {draftItems.length === 0 ? (
                      <tr>
                        <td colSpan={6} style={{ padding: '36px 16px', textAlign: 'center' }}>
                          <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 6 }}>
                            <Package size={36} style={{ color: '#94A3B8' }} />
                            <strong style={{ fontSize: 14, color: '#1E293B' }}>No items added yet.</strong>
                            <span style={{ fontSize: 12, color: '#64748B' }}>
                              Select a product above to add to this purchase invoice.
                            </span>
                          </div>
                        </td>
                      </tr>
                    ) : (
                      draftItems.map((item, idx) => (
                        <tr key={idx} style={{ borderBottom: '1px solid #E2E8F0' }}>
                          <td style={{ padding: '10px 12px', textAlign: 'center', color: '#64748B', fontWeight: 600 }}>
                            {idx + 1}
                          </td>
                          <td style={{ padding: '8px 12px' }}>
                            <select
                              value={item.productId}
                              onChange={e => handleUpdateItem(idx, { productId: e.target.value })}
                              style={{
                                width: '100%',
                                padding: '8px 10px',
                                borderRadius: 6,
                                border: '1px solid #CBD5E1',
                                fontSize: 13,
                                outline: 'none',
                                background: 'white'
                              }}
                            >
                              <option value="">Search or select product</option>
                              {products.map(p => (
                                <option key={p.id} value={p.id}>
                                  {p.name} ({p.sku}) - Stock: {p.currentStock}
                                </option>
                              ))}
                            </select>
                          </td>
                          <td style={{ padding: '8px 12px', textAlign: 'center' }}>
                            <input
                              type="number"
                              min="1"
                              value={item.quantity}
                              onChange={e => handleUpdateItem(idx, { quantity: Math.max(1, Number(e.target.value) || 1) })}
                              style={{
                                width: 80,
                                padding: '8px 8px',
                                borderRadius: 6,
                                border: '1px solid #CBD5E1',
                                fontSize: 13,
                                textAlign: 'center',
                                outline: 'none'
                              }}
                            />
                          </td>
                          <td style={{ padding: '8px 12px' }}>
                            <input
                              type="number"
                              min="0"
                              step="0.01"
                              value={item.unitCost}
                              onChange={e => handleUpdateItem(idx, { unitCost: Math.max(0, Number(e.target.value) || 0) })}
                              style={{
                                width: '100%',
                                padding: '8px 10px',
                                borderRadius: 6,
                                border: '1px solid #CBD5E1',
                                fontSize: 13,
                                outline: 'none'
                              }}
                            />
                          </td>
                          <td style={{ padding: '8px 12px', textAlign: 'right', fontWeight: 700, color: '#059669', fontSize: 14 }}>
                            {(item.quantity * item.unitCost).toLocaleString('en-KE', { minimumFractionDigits: 2 })}
                          </td>
                          <td style={{ padding: '8px 12px', textAlign: 'center' }}>
                            <button
                              type="button"
                              onClick={() => handleRemoveLineItem(idx)}
                              style={{
                                width: 34,
                                height: 34,
                                borderRadius: 6,
                                background: '#FEE2E2',
                                border: 'none',
                                color: '#EF4444',
                                display: 'inline-flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                                cursor: 'pointer'
                              }}
                            >
                              <Trash2 size={16} />
                            </button>
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>

              {/* Total Invoice Amount Banner */}
              <div style={{
                background: '#E8FDF3',
                borderRadius: 10,
                padding: '14px 20px',
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center'
              }}>
                <span style={{ fontSize: 15, fontWeight: 700, color: '#0F1F3A' }}>
                  Total Invoice Amount:
                </span>
                <span style={{ fontSize: 18, fontWeight: 800, color: '#059669' }}>
                  KES {computedTotal.toLocaleString('en-KE', { minimumFractionDigits: 2 })}
                </span>
              </div>

              {/* Invoice Notes */}
              <div style={{ display: 'flex', flexDirection: 'column', gap: 5 }}>
                <label style={{ fontSize: 12, fontWeight: 600, color: '#475569' }}>
                  Invoice Notes / Delivery Remarks (Optional)
                </label>
                <div style={{
                  display: 'flex',
                  alignItems: 'flex-start',
                  border: '1px solid #CBD5E1',
                  borderRadius: 8,
                  padding: '10px 12px',
                  background: 'white',
                  gap: 8
                }}>
                  <FileText size={16} style={{ color: '#94A3B8', marginTop: 2, flexShrink: 0 }} />
                  <textarea
                    rows={2}
                    value={notes}
                    onChange={e => setNotes(e.target.value)}
                    placeholder="Enter any notes or delivery remarks..."
                    style={{
                      border: 'none',
                      outline: 'none',
                      fontSize: 13,
                      width: '100%',
                      fontFamily: 'inherit',
                      resize: 'vertical'
                    }}
                  />
                </div>
              </div>
            </div>

            {/* Footer Buttons */}
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10, marginTop: 4 }}>
              <button
                type="button"
                onClick={() => setShowRecordModal(false)}
                style={{
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: 6,
                  background: 'white',
                  border: '1px solid #CBD5E1',
                  borderRadius: 8,
                  padding: '10px 20px',
                  fontSize: 13,
                  fontWeight: 600,
                  color: '#334155',
                  cursor: 'pointer'
                }}
              >
                <X size={16} /> Cancel
              </button>
              <button
                type="button"
                onClick={handleSavePurchase}
                disabled={saving}
                style={{
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: 6,
                  background: '#059669',
                  border: 'none',
                  borderRadius: 8,
                  padding: '10px 24px',
                  fontSize: 13,
                  fontWeight: 700,
                  color: 'white',
                  cursor: saving ? 'not-allowed' : 'pointer',
                  opacity: saving ? 0.7 : 1
                }}
              >
                <Check size={16} /> {saving ? 'Augmenting...' : 'Confirm & Augment Stock'}
              </button>
            </div>
          </div>
        </Modal>
      )}

      {/* View Invoice Modal */}
      {viewingInvoice && (
        <Modal
          title={`Purchase Invoice #${viewingInvoice.invoiceNumber}`}
          onClose={() => setViewingInvoice(null)}
          footer={
            <Btn variant="secondary" onClick={() => setViewingInvoice(null)}>
              Close
            </Btn>
          }
        >
          <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            {/* Summary card */}
            <div style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))',
              gap: 12,
              padding: 14,
              background: 'var(--b360-bg)',
              borderRadius: 8
            }}>
              <div>
                <div style={{ fontSize: 11, color: 'var(--b360-text-secondary)' }}>INVOICE NUMBER</div>
                <div style={{ fontSize: 15, fontWeight: 700, color: 'var(--b360-green)' }}>
                  #{viewingInvoice.invoiceNumber}
                </div>
              </div>
              <div>
                <div style={{ fontSize: 11, color: 'var(--b360-text-secondary)' }}>SUPPLIER</div>
                <div style={{ fontSize: 14, fontWeight: 600 }}>{viewingInvoice.supplierName}</div>
                {viewingInvoice.supplierPhone && (
                  <div style={{ fontSize: 11, color: 'var(--b360-text-secondary)' }}>
                    {viewingInvoice.supplierPhone}
                  </div>
                )}
              </div>
              <div>
                <div style={{ fontSize: 11, color: 'var(--b360-text-secondary)' }}>INVOICE DATE</div>
                <div style={{ fontSize: 13, fontWeight: 500 }}>
                  {viewingInvoice.invoiceDate ? viewingInvoice.invoiceDate.split('T')[0] : viewingInvoice.createdAt.split('T')[0]}
                </div>
              </div>
              <div>
                <div style={{ fontSize: 11, color: 'var(--b360-text-secondary)' }}>PAYMENT STATUS</div>
                <StatusBadge status={viewingInvoice.paymentStatus} />
              </div>
              <div>
                <div style={{ fontSize: 11, color: 'var(--b360-text-secondary)' }}>PAYMENT METHOD</div>
                <div style={{ fontSize: 13, fontWeight: 500 }}>
                  {viewingInvoice.paymentMethod.replace('_', ' ')}
                </div>
              </div>
              <div>
                <div style={{ fontSize: 11, color: 'var(--b360-text-secondary)' }}>TOTAL AMOUNT</div>
                <div style={{ fontSize: 15, fontWeight: 700, color: 'var(--b360-green)' }}>
                  KES {viewingInvoice.totalAmount.toLocaleString('en-KE', { minimumFractionDigits: 2 })}
                </div>
              </div>
            </div>

            {viewingInvoice.notes && (
              <div style={{ fontSize: 12, color: 'var(--b360-text-secondary)' }}>
                <strong>Notes:</strong> {viewingInvoice.notes}
              </div>
            )}

            {/* Items Table */}
            <div>
              <h4 style={{ margin: '0 0 8px 0', fontSize: 13 }}>Items Augmented into Inventory</h4>
              <div style={{ border: '1px solid var(--b360-border)', borderRadius: 8, overflow: 'hidden' }}>
                <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 12 }}>
                  <thead>
                    <tr style={{ background: 'var(--b360-bg)', borderBottom: '1px solid var(--b360-border)', textAlign: 'left' }}>
                      <th style={{ padding: '8px 12px' }}>Product</th>
                      <th style={{ padding: '8px 12px' }}>SKU</th>
                      <th style={{ padding: '8px 12px', textAlign: 'right' }}>Qty Received</th>
                      <th style={{ padding: '8px 12px', textAlign: 'right' }}>Unit Cost</th>
                      <th style={{ padding: '8px 12px', textAlign: 'right' }}>Line Total</th>
                    </tr>
                  </thead>
                  <tbody>
                    {viewingInvoice.items.map((item, idx) => (
                      <tr key={idx} style={{ borderBottom: '1px solid var(--b360-border)' }}>
                        <td style={{ padding: '8px 12px', fontWeight: 500 }}>{item.productName}</td>
                        <td style={{ padding: '8px 12px', color: 'var(--b360-text-secondary)' }}>{item.sku}</td>
                        <td style={{ padding: '8px 12px', textAlign: 'right', fontWeight: 600 }}>{item.quantity}</td>
                        <td style={{ padding: '8px 12px', textAlign: 'right' }}>
                          KES {item.unitCost.toLocaleString('en-KE', { minimumFractionDigits: 2 })}
                        </td>
                        <td style={{ padding: '8px 12px', textAlign: 'right', fontWeight: 600 }}>
                          KES {item.totalCost.toLocaleString('en-KE', { minimumFractionDigits: 2 })}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                  <tfoot>
                    <tr style={{ background: 'var(--b360-bg)', fontWeight: 'bold' }}>
                      <td colSpan={4} style={{ padding: '10px 12px', textAlign: 'right' }}>Grand Total:</td>
                      <td style={{ padding: '10px 12px', textAlign: 'right', color: 'var(--b360-green)' }}>
                        KES {viewingInvoice.totalAmount.toLocaleString('en-KE', { minimumFractionDigits: 2 })}
                      </td>
                    </tr>
                  </tfoot>
                </table>
              </div>
            </div>
          </div>
        </Modal>
      )}
    </div>
  )
}

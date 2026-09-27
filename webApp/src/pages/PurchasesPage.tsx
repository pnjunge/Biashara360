import React, { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Receipt, Plus, Search, Package, AlertTriangle, FileText,
  DollarSign, CheckCircle2, Clock, Trash2, Eye
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

  // Selector for adding line item
  const [selectedProductId, setSelectedProductId] = useState('')
  const [itemQty, setItemQty] = useState('1')
  const [itemCost, setItemCost] = useState('')

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
    setDraftItems([])
    setSelectedProductId('')
    setItemQty('1')
    setItemCost('')
    setError('')
  }

  const openRecordModal = () => {
    resetForm()
    setShowRecordModal(true)
  }

  const handleProductSelect = (productId: string) => {
    setSelectedProductId(productId)
    const found = products.find(p => p.id === productId)
    if (found) {
      setItemCost(String(found.buyingPrice || 0))
    }
  }

  const handleAddLineItem = () => {
    if (!selectedProductId) {
      setError('Please choose a product to add.')
      return
    }
    const qty = Number(itemQty)
    const cost = Number(itemCost)
    if (isNaN(qty) || qty <= 0) {
      setError('Quantity must be greater than zero.')
      return
    }
    if (isNaN(cost) || cost < 0) {
      setError('Unit cost must be a valid amount.')
      return
    }
    const found = products.find(p => p.id === selectedProductId)
    if (!found) return

    setDraftItems(prev => [
      ...prev,
      {
        productId: found.id,
        productName: found.name,
        sku: found.sku,
        quantity: qty,
        unitCost: cost
      }
    ])
    setSelectedProductId('')
    setItemQty('1')
    setItemCost('')
    setError('')
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
    { value: 'CASH', label: 'Cash' },
    { value: 'MPESA', label: 'M-Pesa' },
    { value: 'BANK_TRANSFER', label: 'Bank Transfer' },
    { value: 'CREDIT', label: 'Supplier Credit' }
  ]

  const paymentStatusOptions = [
    { value: 'PAID', label: 'Fully Paid' },
    { value: 'PENDING', label: 'Pending Due' },
    { value: 'PARTIAL', label: 'Partially Paid' }
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
          <div style={{ padding: 32, textAlign: 'center', color: 'var(--b360-text-secondary)' }}>
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

      {/* Record Purchase Modal */}
      {showRecordModal && (
        <Modal
          title="Record Purchase Invoice (Augment Inventory)"
          onClose={() => setShowRecordModal(false)}
          footer={
            <>
              <Btn variant="secondary" onClick={() => setShowRecordModal(false)}>
                Cancel
              </Btn>
              <Btn onClick={handleSavePurchase} disabled={saving}>
                {saving ? 'Recording & Stocking...' : 'Save & Augment Stock'}
              </Btn>
            </>
          }
        >
          <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
            {error && (
              <p style={{ color: 'var(--b360-red)', fontSize: 12, margin: 0, fontWeight: 600 }}>
                {error}
              </p>
            )}

            {/* Note banner on inventory augmentation */}
            <div style={{
              background: 'var(--b360-green-subtle)',
              border: '1px solid var(--b360-green)',
              borderRadius: 8,
              padding: '10px 14px',
              fontSize: 12,
              color: 'var(--b360-text)',
              display: 'flex',
              gap: 8,
              alignItems: 'center'
            }}>
              <CheckCircle2 size={16} style={{ color: 'var(--b360-green)', flexShrink: 0 }} />
              <div>
                <strong>Automatic Stock In:</strong> Quantities entered on this invoice will immediately augment the current stock of corresponding products and create audit records.
              </div>
            </div>

            {/* Invoice Details Header */}
            <div className="responsive-grid responsive-grid-2" style={{ gap: 12 }}>
              <Input
                label="Invoice Number *"
                value={invoiceNumber}
                onChange={setInvoiceNumber}
                placeholder="e.g. INV-2024-001 or Bill #"
              />
              <Input
                label="Supplier Name *"
                value={supplierName}
                onChange={setSupplierName}
                placeholder="e.g. Kenya Wine Agencies Ltd"
              />
            </div>

            <div className="responsive-grid responsive-grid-3" style={{ gap: 12 }}>
              <Input
                label="Supplier Phone"
                value={supplierPhone}
                onChange={setSupplierPhone}
                placeholder="e.g. 0712345678"
              />
              <Input
                label="Invoice Date"
                type="date"
                value={invoiceDate}
                onChange={setInvoiceDate}
              />
              <Select
                label="Payment Method"
                value={paymentMethod}
                onChange={setPaymentMethod}
                options={paymentOptions}
              />
            </div>

            <div className="responsive-grid responsive-grid-2" style={{ gap: 12 }}>
              <Select
                label="Payment Status"
                value={paymentStatus}
                onChange={setPaymentStatus}
                options={paymentStatusOptions}
              />
              <Input
                label="Notes / Delivery Ref"
                value={notes}
                onChange={setNotes}
                placeholder="Optional supplier notes"
              />
            </div>

            {/* Line items section */}
            <div style={{
              border: '1px solid var(--b360-border)',
              borderRadius: 10,
              padding: 14,
              display: 'flex',
              flexDirection: 'column',
              gap: 10
            }}>
              <h4 style={{ margin: 0, fontSize: 13, fontWeight: 600 }}>Invoice Line Items</h4>

              {/* Add line item inputs */}
              <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr 1fr auto', gap: 8, alignItems: 'flex-end' }}>
                <Select
                  label="Select Product to Stock"
                  value={selectedProductId}
                  onChange={handleProductSelect}
                  options={[
                    { value: '', label: '-- Choose Product --' },
                    ...products.map(p => ({
                      value: p.id,
                      label: `${p.name} (Stock: ${p.currentStock}) - ${p.sku}`
                    }))
                  ]}
                />
                <Input
                  label="Qty"
                  type="number"
                  value={itemQty}
                  onChange={setItemQty}
                  placeholder="1"
                />
                <Input
                  label="Unit Cost (KES)"
                  type="number"
                  value={itemCost}
                  onChange={setItemCost}
                  placeholder="0.00"
                />
                <div style={{ paddingBottom: 2 }}>
                  <Btn
                    variant="secondary"
                    icon={<Plus size={14} />}
                    onClick={handleAddLineItem}
                  >
                    Add
                  </Btn>
                </div>
              </div>

              {/* Draft Items List */}
              {draftItems.length === 0 ? (
                <div style={{
                  padding: 16,
                  textAlign: 'center',
                  color: 'var(--b360-text-secondary)',
                  fontSize: 12,
                  background: 'var(--b360-bg)',
                  borderRadius: 6
                }}>
                  No items added yet. Select a product above and click Add.
                </div>
              ) : (
                <div style={{
                  border: '1px solid var(--b360-border)',
                  borderRadius: 8,
                  overflow: 'hidden'
                }}>
                  <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 12 }}>
                    <thead>
                      <tr style={{ background: 'var(--b360-bg)', borderBottom: '1px solid var(--b360-border)', textAlign: 'left' }}>
                        <th style={{ padding: '8px 10px' }}>Product</th>
                        <th style={{ padding: '8px 10px' }}>SKU</th>
                        <th style={{ padding: '8px 10px', textAlign: 'right' }}>Qty</th>
                        <th style={{ padding: '8px 10px', textAlign: 'right' }}>Unit Cost</th>
                        <th style={{ padding: '8px 10px', textAlign: 'right' }}>Line Total</th>
                        <th style={{ padding: '8px 10px', textAlign: 'center' }}>Remove</th>
                      </tr>
                    </thead>
                    <tbody>
                      {draftItems.map((item, idx) => (
                        <tr key={idx} style={{ borderBottom: '1px solid var(--b360-border)' }}>
                          <td style={{ padding: '8px 10px', fontWeight: 500 }}>{item.productName}</td>
                          <td style={{ padding: '8px 10px', color: 'var(--b360-text-secondary)' }}>{item.sku}</td>
                          <td style={{ padding: '8px 10px', textAlign: 'right', fontWeight: 600 }}>{item.quantity}</td>
                          <td style={{ padding: '8px 10px', textAlign: 'right' }}>
                            KES {item.unitCost.toLocaleString('en-KE', { minimumFractionDigits: 2 })}
                          </td>
                          <td style={{ padding: '8px 10px', textAlign: 'right', fontWeight: 600 }}>
                            KES {(item.quantity * item.unitCost).toLocaleString('en-KE', { minimumFractionDigits: 2 })}
                          </td>
                          <td style={{ padding: '8px 10px', textAlign: 'center' }}>
                            <button
                              onClick={() => handleRemoveLineItem(idx)}
                              style={{
                                background: 'none',
                                border: 'none',
                                color: 'var(--b360-red)',
                                cursor: 'pointer',
                                padding: 2
                              }}
                            >
                              <Trash2 size={14} />
                            </button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                    <tfoot>
                      <tr style={{ background: 'var(--b360-bg)', fontWeight: 'bold' }}>
                        <td colSpan={4} style={{ padding: '10px 10px', textAlign: 'right' }}>
                          Total Invoice Amount:
                        </td>
                        <td style={{ padding: '10px 10px', textAlign: 'right', color: 'var(--b360-green)' }}>
                          KES {computedTotal.toLocaleString('en-KE', { minimumFractionDigits: 2 })}
                        </td>
                        <td></td>
                      </tr>
                    </tfoot>
                  </table>
                </div>
              )}
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

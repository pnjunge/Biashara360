import { useMenuAccess } from '../components/access/MenuAccess'
import React, { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Receipt, Plus, Search, Package, AlertTriangle, FileText,
  CheckCircle2, Clock, Trash2, Eye, X, Check, Store, Phone, CreditCard, Hash,
  Building2, Mail, MapPin, Edit2, Users
} from 'lucide-react'
import {
  KpiCard, StatusBadge, PageHeader, Card, Btn, DataTable, AlertBanner, Modal, Input, Select
} from '../components/ui'
import {
  purchaseApi, productApi, supplierApi, PurchaseInvoice, ProductResponse, CreatePurchaseInvoiceRequest, Supplier
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
  const access=useMenuAccess()
  const navigate = useNavigate()
  const { user } = useAuth()

  const [activeTab, setActiveTab] = useState<'INVOICES' | 'SUPPLIERS'>('INVOICES')
  const [purchases, setPurchases] = useState<PurchaseInvoice[]>([])
  const [products, setProducts] = useState<ProductResponse[]>([])
  const [suppliers, setSuppliers] = useState<Supplier[]>([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')
  const [supplierSearch, setSupplierSearch] = useState('')
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [error, setError] = useState('')
  const [successMsg, setSuccessMsg] = useState('')

  // Dialog states
  const [showRecordModal, setShowRecordModal] = useState(false)
  const [viewingInvoice, setViewingInvoice] = useState<PurchaseInvoice | null>(null)
  const [saving, setSaving] = useState(false)

  // Supplier modal states
  const [showSupplierModal, setShowSupplierModal] = useState(false)
  const [savingSupplier, setSavingSupplier] = useState(false)
  const [supplierError, setSupplierError] = useState('')
  const [supplierForm, setSupplierForm] = useState({
    id: '',
    name: '',
    phone: '',
    email: '',
    address: ''
  })

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
      const [purchasesRes, productsRes, suppliersRes] = await Promise.all([
        purchaseApi.list(),
        productApi.list(),
        supplierApi.list().catch(() => ({ success: false, data: [] as Supplier[] }))
      ])
      if (purchasesRes.success && purchasesRes.data) {
        setPurchases(purchasesRes.data)
      }
      if (productsRes.success && productsRes.data) {
        setProducts(productsRes.data.filter(product => product.stockMode !== 'INGREDIENTS'))
      }
      if (suppliersRes.success && suppliersRes.data) {
        setSuppliers(suppliersRes.data)
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

  const resetForm = (initialSupplierName = '', initialSupplierPhone = '') => {
    const randomSuffix = Math.floor(1000 + Math.random() * 9000)
    setInvoiceNumber(`INV-${new Date().getFullYear()}-${randomSuffix}`)
    setSupplierName(initialSupplierName)
    setSupplierPhone(initialSupplierPhone)
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

  const openRecordModal = (initialSupplierName = '', initialSupplierPhone = '') => {
    if(!access.hasPermission('purchases.create'))return
    resetForm(initialSupplierName, initialSupplierPhone)
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

  // Supplier Management Handlers
  const handleOpenAddSupplier = () => {
    if(!access.hasPermission("inventory.suppliers"))return
    setSupplierForm({ id: '', name: '', phone: '', email: '', address: '' })
    setSupplierError('')
    setShowSupplierModal(true)
  }

  const handleOpenEditSupplier = (s: Supplier) => {
    if(!access.hasPermission("inventory.suppliers"))return
    setSupplierForm({
      id: s.id,
      name: s.name,
      phone: s.phone || '',
      email: s.email || '',
      address: s.address || ''
    })
    setSupplierError('')
    setShowSupplierModal(true)
  }

  const handleSaveSupplier = async (e?: React.FormEvent) => {
    if (e) e.preventDefault()
    if (!supplierForm.name.trim()) {
      setSupplierError('Supplier name is required.')
      return
    }
    setSavingSupplier(true)
    setSupplierError('')
    try {
      if (supplierForm.id) {
        const res = await supplierApi.update(supplierForm.id, {
          name: supplierForm.name.trim(),
          phone: supplierForm.phone.trim() || undefined,
          email: supplierForm.email.trim() || undefined,
          address: supplierForm.address.trim() || undefined
        })
        if (res.success) {
          setShowSupplierModal(false)
          setSuccessMsg(`Supplier "${supplierForm.name}" updated successfully.`)
          await loadData()
          setTimeout(() => setSuccessMsg(''), 4000)
        } else {
          setSupplierError(res.message || 'Failed to update supplier.')
        }
      } else {
        const res = await supplierApi.create({
          name: supplierForm.name.trim(),
          phone: supplierForm.phone.trim() || undefined,
          email: supplierForm.email.trim() || undefined,
          address: supplierForm.address.trim() || undefined
        })
        if (res.success && res.data) {
          setShowSupplierModal(false)
          setSuccessMsg(`Supplier "${supplierForm.name}" added successfully.`)
          // If recording a purchase modal is open, link it directly
          setSupplierName(res.data.name)
          if (res.data.phone) setSupplierPhone(res.data.phone)
          await loadData()
          setTimeout(() => setSuccessMsg(''), 4000)
        } else {
          setSupplierError(res.message || 'Failed to add supplier.')
        }
      }
    } catch (err: any) {
      setSupplierError(err.response?.data?.message || err.message || 'Error saving supplier.')
    } finally {
      setSavingSupplier(false)
    }
  }

  const handleDeleteSupplier = async (id: string, name: string) => {
    if (!window.confirm(`Are you sure you want to delete supplier "${name}"?`)) return
    try {
      const res = await supplierApi.delete(id)
      if (res.success) {
        setSuccessMsg(`Supplier "${name}" deleted.`)
        await loadData()
        setTimeout(() => setSuccessMsg(''), 4000)
      } else {
        setError(res.message || 'Failed to delete supplier.')
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'Error deleting supplier.')
    }
  }

  const handleSavePurchase = async () => {
    if (!invoiceNumber.trim()) {
      setError('Invoice number is required.')
      return
    }
    // Note: Supplier name is optional!
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
        supplierName: supplierName.trim() || 'Unspecified',
        supplierPhone: supplierPhone.trim() || undefined,
        totalAmount: computedTotal,
        paymentStatus,
        paymentMethod,
        notes: notes.trim(),
        invoiceDate,
        items: draftItems.map(item => {
          const qty = Math.max(1, Number(item.quantity) || 1)
          const cost = Math.max(0, Number(item.unitCost) || 0)
          return {
            productId: item.productId,
            productName: item.productName,
            sku: item.sku || '',
            quantity: qty,
            unitCost: cost,
            lineTotal: qty * cost,
            totalCost: qty * cost
          }
        })
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
      const serverMsg = e.response?.data?.error?.message || e.response?.data?.message || e.message
      setError(serverMsg || 'Failed to record purchase invoice. Please check server connectivity.')
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

  // Filtered Suppliers
  const filteredSuppliers = suppliers.filter(s => {
    const q = supplierSearch.trim().toLowerCase()
    if (!q) return true
    return s.name.toLowerCase().includes(q) ||
      (s.phone && s.phone.toLowerCase().includes(q)) ||
      (s.email && s.email.toLowerCase().includes(q)) ||
      (s.address && s.address.toLowerCase().includes(q))
  })

  const activeSupplierCount = suppliers.filter(s => s.isActive !== false).length
  const supplierPurchases = purchases.filter(p => p.supplierName && p.supplierName !== 'Unspecified')
  const totalSupplierSpend = supplierPurchases.reduce((acc, p) => acc + p.totalAmount, 0)

  return (
    <div className="fade-in" style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
      {/* Page Header */}
      <PageHeader
        title="Product invoices & suppliers"
        action={
          <div style={{ display: 'flex', gap: 8 }}>
            <Btn variant="secondary" icon={<Package size={14} />} onClick={() => navigate('/inventory')}>
              Go to Inventory
            </Btn>
            {activeTab === 'SUPPLIERS' ? (
              <>
                <Btn disabled={!access.hasPermission("purchases.create")} variant="secondary" icon={<Receipt size={14} />} onClick={() => openRecordModal()}>
                  Record Purchase
                </Btn>
                <Btn disabled={!access.hasPermission("inventory.suppliers")} icon={<Plus size={14} />} onClick={handleOpenAddSupplier}>
                  Add Supplier
                </Btn>
              </>
            ) : (
              <>
                <Btn variant="secondary" icon={<Building2 size={14} />} onClick={() => setActiveTab('SUPPLIERS')}>
                  Suppliers ({suppliers.length})
                </Btn>
                <Btn disabled={!access.hasPermission("purchases.create")} icon={<Plus size={14} />} onClick={() => openRecordModal()}>
                  Record Purchase Invoice
                </Btn>
              </>
            )}
          </div>
        }
      />

      {access.hasMenu('HOSPITALITY_OPS') && access.hasPermission('hospitality.view') && access.hasPermission('hospitality.purchasing') && <p style={{margin:0}}>Recipe portions use bulk ingredient stock. <button type="button" onClick={() => navigate('/purchases?type=ingredients')} style={{border:0,background:'none',color:'var(--b360-green)',cursor:'pointer',fontWeight:700}}>Purchase ingredients</button> for items such as goat portions.</p>}

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
          onClick={() => setActiveTab('INVOICES')}
          style={{
            background: activeTab === 'INVOICES' ? 'var(--b360-green-subtle)' : 'none',
            border: activeTab === 'INVOICES' ? '1px solid var(--b360-green)' : '1px solid transparent',
            padding: '8px 16px',
            fontSize: 14,
            fontWeight: activeTab === 'INVOICES' ? 600 : 500,
            color: activeTab === 'INVOICES' ? 'var(--b360-green)' : 'var(--b360-text-secondary)',
            cursor: 'pointer',
            borderRadius: 6
          }}
        >
          Purchase Invoices (Stock In)
        </button>
        <button
          onClick={() => setActiveTab('SUPPLIERS')}
          style={{
            background: activeTab === 'SUPPLIERS' ? 'var(--b360-green-subtle)' : 'none',
            border: activeTab === 'SUPPLIERS' ? '1px solid var(--b360-green)' : '1px solid transparent',
            padding: '8px 16px',
            fontSize: 14,
            fontWeight: activeTab === 'SUPPLIERS' ? 600 : 500,
            color: activeTab === 'SUPPLIERS' ? 'var(--b360-green)' : 'var(--b360-text-secondary)',
            cursor: 'pointer',
            borderRadius: 6
          }}
        >
          Suppliers ({suppliers.length})
        </button>
      </div>

      {successMsg && (
        <AlertBanner
          message={successMsg}
          icon={<CheckCircle2 size={16} />}
          color="var(--b360-green)"
        />
      )}

      {activeTab === 'INVOICES' ? (
        <>
          {/* Invoices KPIs */}
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
                  aria-label="Search by invoice, supplier, or SKU"
                  value={search}
                  onChange={e => setSearch(e.target.value)}
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
                      <div style={{ fontWeight: 600, fontSize: 13 }}>
                        {row.supplierName && row.supplierName !== 'Unspecified' ? row.supplierName : <span style={{ color: 'var(--b360-text-secondary)', fontStyle: 'italic' }}>Unspecified</span>}
                      </div>
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
        </>
      ) : (
        <>
          {/* Suppliers View */}
          <div className="responsive-grid responsive-grid-4" style={{ gap: 12 }}>
            <KpiCard
              title="Total Suppliers"
              value={`${suppliers.length}`}
              change="Registered vendors"
              icon={<Building2 size={18} />}
              color="var(--b360-blue)"
            />
            <KpiCard
              title="Active Suppliers"
              value={`${activeSupplierCount}`}
              change="Available for purchasing"
              icon={<CheckCircle2 size={18} />}
              color="var(--b360-green)"
            />
            <KpiCard
              title="Linked Purchases"
              value={`${supplierPurchases.length} invoices`}
              change="Attributed to vendors"
              icon={<Receipt size={18} />}
              color="#7C3AED"
            />
            <KpiCard
              title="Supplier Spend"
              value={`KES ${totalSupplierSpend.toLocaleString('en-KE', { maximumFractionDigits: 0 })}`}
              change="Purchased from vendors"
              icon={<Clock size={18} />}
              color="var(--b360-green)"
            />
          </div>

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
                  aria-label="Search suppliers by name, phone, email, address"
                  value={supplierSearch}
                  onChange={e => setSupplierSearch(e.target.value)}
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

              <Btn disabled={!access.hasPermission("inventory.suppliers")} icon={<Plus size={14} />} onClick={handleOpenAddSupplier}>
                Add Supplier
              </Btn>
            </div>

            {loading ? (
              <div style={{ padding: 40, textAlign: 'center', color: 'var(--b360-text-secondary)' }}>
                Loading suppliers...
              </div>
            ) : filteredSuppliers.length === 0 ? (
              <div style={{ padding: 40, textAlign: 'center', color: 'var(--b360-text-secondary)' }}>
                {supplierSearch ? 'No suppliers match your search.' : 'No suppliers registered yet. Click "Add Supplier" to create one.'}
              </div>
            ) : (
              <DataTable
                headers={['Supplier / Vendor', 'Phone', 'Email', 'Physical Address', 'Status', 'Invoices', 'Action']}
                rows={filteredSuppliers.map(s => {
                  const sInvoices = purchases.filter(p => p.supplierName.toLowerCase() === s.name.toLowerCase())
                  const sSpend = sInvoices.reduce((acc, p) => acc + p.totalAmount, 0)
                  return [
                    <div key="name" style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                      <div style={{
                        width: 32,
                        height: 32,
                        borderRadius: 6,
                        background: 'var(--b360-green-subtle)',
                        color: 'var(--b360-green)',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center'
                      }}>
                        <Store size={16} />
                      </div>
                      <div>
                        <strong style={{ fontSize: 13, color: 'var(--b360-text)' }}>{s.name}</strong>
                      </div>
                    </div>,
                    <div key="phone" style={{ fontSize: 13, color: s.phone ? 'inherit' : 'var(--b360-text-secondary)' }}>
                      {s.phone || '—'}
                    </div>,
                    <div key="email" style={{ fontSize: 13, color: s.email ? 'inherit' : 'var(--b360-text-secondary)' }}>
                      {s.email || '—'}
                    </div>,
                    <div key="addr" style={{ fontSize: 12, color: s.address ? 'inherit' : 'var(--b360-text-secondary)', maxWidth: 220, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                      {s.address || '—'}
                    </div>,
                    <StatusBadge key="status" status={s.isActive !== false ? 'ACTIVE' : 'INACTIVE'} />,
                    <span key="invoices" style={{ fontSize: 12, fontWeight: 500 }}>
                      {sInvoices.length} invoice{sInvoices.length === 1 ? '' : 's'}
                      {sSpend > 0 ? ` (KES ${sSpend.toLocaleString('en-KE', { maximumFractionDigits: 0 })})` : ''}
                    </span>,
                    <div key="act" style={{ display: 'flex', gap: 6 }}>
                      <Btn
                        small
                        variant="secondary"
                        icon={<Receipt size={12} />}
                        onClick={() => openRecordModal(s.name, s.phone || '')}
                      >
                        Purchase
                      </Btn>
                      <Btn
                        small
                        variant="secondary"
                        icon={<Edit2 size={12} />}
                        onClick={() => handleOpenEditSupplier(s)}
                      >
                        Edit
                      </Btn>
                      <Btn
                        small
                        variant="secondary"
                        icon={<Trash2 size={12} />}
                        onClick={() => handleDeleteSupplier(s.id, s.name)}
                      >
                        Delete
                      </Btn>
                    </div>
                  ]
                })}
              />
            )}
          </Card>
        </>
      )}

      {/* Record Purchase Modal (Matching User Mockup) */}
      {showRecordModal && (
        <Modal
          extraWide
          title=""
          onClose={() => setShowRecordModal(false)}
        >
          <div style={{ display: 'flex', flexDirection: 'column', gap: 18, width: '100%', maxWidth: 960, margin: '0 auto' }}>
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
                      aria-label="Invoice number"
                      value={invoiceNumber}
                      onChange={e => setInvoiceNumber(e.target.value)}
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
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <label style={{ fontSize: 12, fontWeight: 600, color: '#475569' }}>
                      Supplier Name <span style={{ fontSize: 11, fontWeight: 400, color: '#64748B' }}>(Optional)</span>
                    </label>
                    <button
                      type="button"
                      onClick={handleOpenAddSupplier}
                      style={{
                        background: 'transparent',
                        border: 'none',
                        color: 'var(--b360-green)',
                        fontSize: 11,
                        fontWeight: 600,
                        cursor: 'pointer',
                        padding: 0
                      }}
                    >
                      + Add New Supplier
                    </button>
                  </div>
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
                      aria-label="Supplier name"
                      list="supplier-options"
                      value={supplierName}
                      onChange={e => {
                        const val = e.target.value
                        setSupplierName(val)
                        const matched = suppliers.find(s => s.name.toLowerCase() === val.toLowerCase())
                        if (matched && matched.phone) {
                          setSupplierPhone(matched.phone)
                        }
                      }}
                      style={{
                        border: 'none',
                        outline: 'none',
                        fontSize: 13,
                        width: '100%',
                        fontFamily: 'inherit'
                      }}
                    />
                    <datalist id="supplier-options">
                      {suppliers.map(s => (
                        <option key={s.id} value={s.name}>
                          {s.phone ? `${s.name} (${s.phone})` : s.name}
                        </option>
                      ))}
                    </datalist>
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
                      aria-label="Supplier phone"
                      value={supplierPhone}
                      onChange={e => setSupplierPhone(e.target.value)}
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
                      <th style={{ padding: '10px 14px', width: 40, textAlign: 'center' }}>#</th>
                      <th style={{ padding: '10px 14px' }}>Product *</th>
                      <th style={{ padding: '10px 14px', width: 120, textAlign: 'center' }}>Qty Received *</th>
                      <th style={{ padding: '10px 14px', width: 170 }}>Unit Cost (KES) *</th>
                      <th style={{ padding: '10px 14px', width: 160, textAlign: 'right' }}>Total (KES)</th>
                      <th style={{ padding: '10px 14px', width: 60, textAlign: 'center' }}>Action</th>
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
                          <td style={{ padding: '10px 14px', textAlign: 'center', color: '#64748B', fontWeight: 600 }}>
                            {idx + 1}
                          </td>
                          <td style={{ padding: '8px 14px' }}>
                            <select
                              value={item.productId}
                              onChange={e => handleUpdateItem(idx, { productId: e.target.value })}
                              style={{
                                width: '100%',
                                padding: '9px 12px',
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
                          <td style={{ padding: '8px 14px', textAlign: 'center' }}>
                            <input
                              type="number"
                              min="1"
                              value={item.quantity}
                              onChange={e => handleUpdateItem(idx, { quantity: Math.max(1, Number(e.target.value) || 1) })}
                              style={{
                                width: 85,
                                padding: '8px 10px',
                                borderRadius: 6,
                                border: '1px solid #CBD5E1',
                                fontSize: 13,
                                textAlign: 'center',
                                outline: 'none'
                              }}
                            />
                          </td>
                          <td style={{ padding: '8px 14px' }}>
                            <div style={{
                              display: 'flex',
                              alignItems: 'center',
                              border: '1px solid #CBD5E1',
                              borderRadius: 6,
                              padding: '0 8px',
                              background: 'white',
                              gap: 4
                            }}>
                              <span style={{ fontSize: 11, fontWeight: 700, color: '#64748B' }}>KES</span>
                              <input
                                type="number"
                                min="0"
                                step="0.01"
                                value={item.unitCost}
                                onChange={e => handleUpdateItem(idx, { unitCost: Math.max(0, Number(e.target.value) || 0) })}
                                style={{
                                  width: '100%',
                                  padding: '8px 4px',
                                  borderRadius: 6,
                                  border: 'none',
                                  fontSize: 13,
                                  fontWeight: 600,
                                  outline: 'none'
                                }}
                              />
                            </div>
                          </td>
                          <td style={{ padding: '8px 14px', textAlign: 'right', fontWeight: 800, color: '#059669', fontSize: 14 }}>
                            KES {(item.quantity * item.unitCost).toLocaleString('en-KE', { minimumFractionDigits: 2 })}
                          </td>
                          <td style={{ padding: '8px 14px', textAlign: 'center' }}>
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
                    aria-label="Purchase notes or remarks"
                    rows={2}
                    value={notes}
                    onChange={e => setNotes(e.target.value)}
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

      {/* Add / Edit Supplier Modal */}
      {showSupplierModal && (
        <Modal
          title={supplierForm.id ? "Edit Supplier" : "Add New Supplier"}
          onClose={() => setShowSupplierModal(false)}
        >
          <form onSubmit={handleSaveSupplier} style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
            {supplierError && (
              <p style={{ color: 'var(--b360-red)', fontSize: 12, margin: 0, fontWeight: 600 }}>
                {supplierError}
              </p>
            )}
            <Input
              label="Supplier / Vendor Name *"
              value={supplierForm.name}
              onChange={v => setSupplierForm({ ...supplierForm, name: v })}
            />
            <Input
              label="Phone Number"
              value={supplierForm.phone}
              onChange={v => setSupplierForm({ ...supplierForm, phone: v })}
            />
            <Input
              label="Email Address"
              type="email"
              value={supplierForm.email}
              onChange={v => setSupplierForm({ ...supplierForm, email: v })}
            />
            <Input
              label="Physical Address / Location"
              value={supplierForm.address}
              onChange={v => setSupplierForm({ ...supplierForm, address: v })}
            />
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 8, marginTop: 10 }}>
              <Btn variant="secondary" onClick={() => setShowSupplierModal(false)}>
                Cancel
              </Btn>
              <Btn type="submit" disabled={savingSupplier}>
                {savingSupplier ? 'Saving...' : supplierForm.id ? 'Update Supplier' : 'Save Supplier'}
              </Btn>
            </div>
          </form>
        </Modal>
      )}
    </div>
  )
}

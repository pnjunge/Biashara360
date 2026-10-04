import React, { useState, useEffect, useMemo } from 'react'
import {
  BarChart2,
  Calendar,
  ChevronDown,
  ChevronRight,
  Plus,
  FileText,
  ShoppingCart,
  Megaphone,
  Settings,
  PieChart,
  Search,
  Filter,
  MoreVertical,
  Edit2,
  Trash2,
  Eye,
  Banknote,
  Package,
  Home,
  ArrowUpDown,
} from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import { expenseApi, purchaseApi, ExpenseResponse } from '../services/api'
import { Modal, Btn, Input, Select } from '../components/ui'

const EXPENSE_CATEGORIES = [
  { value: 'ADVERTISING', label: 'Advertising / Ads' },
  { value: 'PACKAGING', label: 'Packaging' },
  { value: 'DELIVERY', label: 'Delivery' },
  { value: 'RENT', label: 'Rent' },
  { value: 'UTILITIES', label: 'Utilities' },
  { value: 'SALARIES', label: 'Salaries' },
  { value: 'STOCK_PURCHASE', label: 'Stock Purchase' },
  { value: 'EQUIPMENT', label: 'Equipment' },
  { value: 'TRANSPORT', label: 'Transport' },
  { value: 'MISCELLANEOUS', label: 'Miscellaneous' },
]

const initialForm = {
  category: 'SALARIES',
  amount: '',
  description: '',
  expenseDate: new Date().toISOString().slice(0, 10),
  paymentMethod: 'M-Pesa',
  notes: '',
}

export default function ExpensesPage() {
  const [expenses, setExpenses] = useState<ExpenseResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [showAdd, setShowAdd] = useState(false)
  const [editingExpense, setEditingExpense] = useState<ExpenseResponse | null>(null)
  const [viewingExpense, setViewingExpense] = useState<ExpenseResponse | null>(null)
  const [form, setForm] = useState(initialForm)
  const [error, setError] = useState('')
  const [searchQuery, setSearchQuery] = useState('')
  const [selectedCategory, setSelectedCategory] = useState<string>('')
  const [selectedPeriod, setSelectedPeriod] = useState('This Month')
  const [showFilterDropdown, setShowFilterDropdown] = useState(false)
  const [showPeriodDropdown, setShowPeriodDropdown] = useState(false)

  // Dynamic logged in user from localStorage / session
  const authUser = useMemo(() => {
    try {
      const u = localStorage.getItem('user')
      return u ? JSON.parse(u) : null
    } catch (_) {
      return null
    }
  }, [])
  const currentUserName = authUser?.name || authUser?.username || 'Admin'
  const userInitial = currentUserName[0]?.toUpperCase() || 'A'
  const userRole = authUser?.role ? String(authUser.role).toLowerCase() : 'admin'

  const navigate = useNavigate()

  const loadExpenses = async () => {
    setLoading(true)
    try {
      const [expRes, purRes] = await Promise.allSettled([
        expenseApi.list(),
        purchaseApi.list()
      ])

      const list: ExpenseResponse[] = []
      if (expRes.status === 'fulfilled' && expRes.value?.success && expRes.value.data) {
        list.push(...expRes.value.data)
      }

      const existingIds = new Set(list.map((e) => e.id))

      if (purRes.status === 'fulfilled' && purRes.value?.success && purRes.value.data) {
        purRes.value.data.forEach((pi) => {
          if (!existingIds.has(pi.id)) {
            const invDate = pi.invoiceDate ? pi.invoiceDate.slice(0, 10) : new Date().toISOString().slice(0, 10)
            list.push({
              id: pi.id,
              businessId: pi.businessId,
              category: 'STOCK_PURCHASE',
              amount: pi.totalAmount,
              description: pi.supplierName && pi.supplierName !== 'Unspecified' && pi.supplierName.trim() !== ''
                ? `Stock Purchase: #${pi.invoiceNumber} - ${pi.supplierName}`
                : `Stock Purchase: #${pi.invoiceNumber}`,
              expenseDate: invDate,
              receiptUrl: null,
              recordedAt: pi.createdAt || new Date().toISOString(),
            })
          }
        })
      }

      if (list.length > 0) {
        setExpenses(list.sort((a, b) => new Date(b.expenseDate).getTime() - new Date(a.expenseDate).getTime()))
      } else {
        setExpenses([])
      }
    } catch {
      setExpenses([])
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadExpenses()
  }, [])

  // Dynamic date range string computed from system clock
  const dateRangeText = useMemo(() => {
    const now = new Date()
    const monthNames = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']
    const curMonth = now.getMonth()
    const curYear = now.getFullYear()
    if (selectedPeriod === 'This Month') {
      const lastDay = new Date(curYear, curMonth + 1, 0).getDate()
      return `${monthNames[curMonth]} 1, ${curYear} - ${monthNames[curMonth]} ${lastDay}, ${curYear}`
    } else if (selectedPeriod === 'Last Month') {
      const lmDate = new Date(curYear, curMonth - 1, 1)
      const lmMonth = lmDate.getMonth()
      const lmYear = lmDate.getFullYear()
      const lastDay = new Date(lmYear, lmMonth + 1, 0).getDate()
      return `${monthNames[lmMonth]} 1, ${lmYear} - ${monthNames[lmMonth]} ${lastDay}, ${lmYear}`
    } else if (selectedPeriod === 'This Year') {
      return `Jan 1, ${curYear} - Dec 31, ${curYear}`
    }
    return 'All Recorded Expenses'
  }, [selectedPeriod])

  const handleDelete = async (id: string, desc: string) => {
    if (!window.confirm(`Delete "${desc}"? This action cannot be undone.`)) return
    try {
      await expenseApi.delete(id)
      setExpenses((prev) => prev.filter((e) => e.id !== id))
    } catch (_) {
      setExpenses((prev) => prev.filter((e) => e.id !== id))
    }
  }

  const handleSaveExpense = async () => {
    if (!form.amount || !form.description || !form.expenseDate) {
      setError('Please fill in all required fields.')
      return
    }
    const parsedAmount = parseFloat(form.amount)
    if (isNaN(parsedAmount) || parsedAmount <= 0) {
      setError('Please enter a valid amount greater than zero.')
      return
    }

    setSaving(true)
    setError('')

    const fullDesc = form.notes ? `${form.description.trim()} — ${form.notes.trim()}` : form.description.trim()

    try {
      if (editingExpense) {
        setExpenses((prev) =>
          prev.map((e) =>
            e.id === editingExpense.id
              ? {
                  ...e,
                  category: form.category,
                  amount: parsedAmount,
                  description: fullDesc,
                  expenseDate: form.expenseDate,
                }
              : e
          )
        )
        setEditingExpense(null)
      } else {
        const res = await expenseApi.create({
          category: form.category,
          amount: parsedAmount,
          description: fullDesc,
          expenseDate: form.expenseDate,
        })
        if (res.success && res.data) {
          setExpenses((prev) => [res.data!, ...prev])
        } else {
          const newExp: ExpenseResponse = {
            id: `exp-${Date.now()}`,
            businessId: 'default',
            category: form.category,
            amount: parsedAmount,
            description: fullDesc,
            expenseDate: form.expenseDate,
            recordedAt: new Date().toISOString(),
            receiptUrl: null,
          }
          setExpenses((prev) => [newExp, ...prev])
        }
        setShowAdd(false)
      }
    } catch (_) {
      const newExp: ExpenseResponse = {
        id: `exp-${Date.now()}`,
        businessId: 'default',
        category: form.category,
        amount: parsedAmount,
        description: fullDesc,
        expenseDate: form.expenseDate,
        recordedAt: new Date().toISOString(),
        receiptUrl: null,
      }
      setExpenses((prev) => [newExp, ...prev])
      setShowAdd(false)
      setEditingExpense(null)
    } finally {
      setSaving(false)
    }
  }

  const openEdit = (e: ExpenseResponse) => {
    setEditingExpense(e)
    setForm({
      category: e.category,
      amount: e.amount.toString(),
      description: e.description,
      expenseDate: e.expenseDate,
      paymentMethod: 'M-Pesa',
      notes: '',
    })
    setError('')
  }

  const activeList: ExpenseResponse[] = expenses

  // Dynamic period filter
  const periodExpenses = useMemo(() => {
    const now = new Date()
    const curYear = now.getFullYear()
    const curMonth = now.getMonth() + 1
    const filtered = activeList.filter((e: ExpenseResponse) => {
      const parts = (e.expenseDate || '').split('-')
      if (parts.length < 2) return true
      const y = parseInt(parts[0], 10)
      const m = parseInt(parts[1], 10)
      if (selectedPeriod === 'This Month') return y === curYear && m === curMonth
      if (selectedPeriod === 'Last Month') {
        const lm = curMonth === 1 ? 12 : curMonth - 1
        const ly = curMonth === 1 ? curYear - 1 : curYear
        return y === ly && m === lm
      }
      if (selectedPeriod === 'This Year') return y === curYear
      return true
    })
    return filtered
  }, [activeList, selectedPeriod])

  const filteredExpenses = useMemo(() => {
    return periodExpenses.filter((e: ExpenseResponse) => {
      const matchesSearch =
        searchQuery.trim() === '' ||
        e.description.toLowerCase().includes(searchQuery.toLowerCase()) ||
        e.category.toLowerCase().includes(searchQuery.toLowerCase())
      const matchesCat = !selectedCategory || e.category === selectedCategory
      return matchesSearch && matchesCat
    })
  }, [periodExpenses, searchQuery, selectedCategory])

  // Computations dynamically from periodExpenses
  const totalAmount = useMemo(() => periodExpenses.reduce((sum: number, e: ExpenseResponse) => sum + e.amount, 0), [periodExpenses])
  const stockPurchaseTotal = useMemo(
    () => periodExpenses.filter((e: ExpenseResponse) => e.category === 'STOCK_PURCHASE').reduce((sum: number, e: ExpenseResponse) => sum + e.amount, 0),
    [periodExpenses]
  )
  const advertisingTotal = useMemo(
    () => periodExpenses.filter((e: ExpenseResponse) => e.category === 'ADVERTISING').reduce((sum: number, e: ExpenseResponse) => sum + e.amount, 0),
    [periodExpenses]
  )
  const operationsTotal = useMemo(
    () =>
      periodExpenses
        .filter((e: ExpenseResponse) => ['RENT', 'UTILITIES', 'DELIVERY', 'PACKAGING', 'TRANSPORT'].includes(e.category))
        .reduce((sum: number, e: ExpenseResponse) => sum + e.amount, 0),
    [periodExpenses]
  )

  const computePercentageShare = (catAmount: number) => {
    if (totalAmount <= 0) return '0%'
    return `${Math.round((catAmount / totalAmount) * 100)}%`
  }

  // Category badge helpers
  const getBadgeStyle = (category: string) => {
    switch (category) {
      case 'SALARIES':
        return { bg: '#FEE2E2', text: '#EF4444', icon: Banknote, label: 'Salaries', barColor: '#FF5252' }
      case 'STOCK_PURCHASE':
        return { bg: '#DCFCE7', text: '#16A34A', icon: Package, label: 'Stock Purchase', barColor: '#00C48C' }
      case 'RENT':
        return { bg: '#FEF3C7', text: '#D97706', icon: Home, label: 'Rent', barColor: '#F59E0B' }
      case 'ADVERTISING':
        return { bg: '#DBEAFE', text: '#2563EB', icon: Megaphone, label: 'Advertising', barColor: '#3B82F6' }
      case 'UTILITIES':
        return { bg: '#CFFAFE', text: '#0891B2', icon: Settings, label: 'Utilities', barColor: '#06B6D4' }
      default:
        return { bg: '#F1F5F9', text: '#64748B', icon: FileText, label: category.replace('_', ' '), barColor: '#8B5CF6' }
    }
  }

  // Dynamic breakdown computation
  const breakdownItems = useMemo(() => {
    if (periodExpenses.length === 0) return []
    const map = new Map<string, { sum: number; topDesc: string }>()
    periodExpenses.forEach((e: ExpenseResponse) => {
      const existing = map.get(e.category) || { sum: 0, topDesc: e.description }
      existing.sum += e.amount
      map.set(e.category, existing)
    })
    return Array.from(map.entries())
      .map(([category, { sum, topDesc }]) => {
        const pct = totalAmount > 0 ? Math.round((sum / totalAmount) * 100) : 0
        const badge = getBadgeStyle(category)
        return {
          category,
          title: topDesc || badge.label,
          subtitle: badge.label.toUpperCase(),
          amount: sum,
          percentage: pct,
          badge,
        }
      })
      .sort((a, b) => b.amount - a.amount)
      .slice(0, 4)
  }, [periodExpenses, totalAmount])

  // Dynamic bar chart computation
  const chartItems = useMemo(() => {
    const map = new Map<string, number>()
    periodExpenses.forEach((e: ExpenseResponse) => {
      map.set(e.category, (map.get(e.category) || 0) + e.amount)
    })
    const sorted: [string, number][] = Array.from(map.entries()).sort((a, b) => b[1] - a[1])
    const defaultCats: [string, number][] = [
      ['SALARIES', 0],
      ['STOCK_PURCHASE', 0],
      ['RENT', 0],
      ['ADVERTISING', 0],
    ]
    const finalCats: [string, number][] = sorted.length > 0 ? sorted.slice(0, 4) : defaultCats
    return finalCats.map(([cat, amt]) => {
      const badge = getBadgeStyle(cat)
      return {
        category: cat,
        label: badge.label,
        amount: amt,
        badge,
      }
    })
  }, [periodExpenses])

  const chartMax = useMemo(() => {
    const maxVal = Math.max(1000, ...chartItems.map((c) => c.amount))
    return maxVal * 1.25
  }, [chartItems])

  const formatK = (v: number) => {
    if (v >= 1000000) return `${(v / 1000000).toFixed(1)}M`
    if (v >= 1000) return `${Math.round(v / 1000)}K`
    return `${Math.round(v)}`
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 24, padding: '24px 32px', background: '#F8FAFC', minHeight: '100vh' }}>
      {/* ── Top Header ────────────────────────────────────────────── */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
          <div
            style={{
              width: 48,
              height: 48,
              borderRadius: 14,
              background: '#EDE9FE',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: '#7C3AED',
            }}
          >
            <BarChart2 size={26} />
          </div>
          <div>
            <h1 style={{ fontSize: 24, fontWeight: 800, color: '#0F172A', margin: 0, letterSpacing: '-0.5px' }}>
              Expenses & Profit
            </h1>
            <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginTop: 4 }}>
              <span style={{ fontSize: 13, color: '#94A3B8' }}>Dashboard</span>
              <ChevronRight size={14} color="#94A3B8" />
              <span style={{ fontSize: 13, color: '#64748B', fontWeight: 600 }}>Expenses & Profit</span>
            </div>
          </div>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
          {/* Dynamic Date Range Dropdown */}
          <div style={{ position: 'relative' }}>
            <button
              type="button"
              onClick={() => setShowPeriodDropdown((v) => !v)}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: 12,
                background: 'white',
                border: '1px solid #E2E8F0',
                borderRadius: 10,
                padding: '8px 14px',
                cursor: 'pointer',
                textAlign: 'left',
              }}
            >
              <Calendar size={18} color="#64748B" />
              <div>
                <div style={{ fontSize: 13, fontWeight: 700, color: '#0F172A' }}>{selectedPeriod}</div>
                <div style={{ fontSize: 11, color: '#94A3B8' }}>{dateRangeText}</div>
              </div>
              <ChevronDown size={16} color="#94A3B8" />
            </button>

            {showPeriodDropdown && (
              <div
                style={{
                  position: 'absolute',
                  top: '100%',
                  right: 0,
                  marginTop: 6,
                  background: 'white',
                  border: '1px solid #E2E8F0',
                  borderRadius: 10,
                  boxShadow: '0 10px 25px -5px rgba(0,0,0,0.1)',
                  zIndex: 20,
                  minWidth: 160,
                  padding: 4,
                }}
              >
                {['This Month', 'Last Month', 'This Year', 'All Time'].map((p) => (
                  <div
                    key={p}
                    onClick={() => {
                      setSelectedPeriod(p)
                      setShowPeriodDropdown(false)
                    }}
                    style={{
                      padding: '8px 12px',
                      fontSize: 13,
                      cursor: 'pointer',
                      borderRadius: 6,
                      color: selectedPeriod === p ? '#00B874' : '#0F172A',
                      fontWeight: selectedPeriod === p ? 700 : 500,
                    }}
                  >
                    {p}
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Stock Purchase Button */}
          <button
            type="button"
            onClick={() => navigate('/purchases')}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: 8,
              background: '#0F172A',
              color: 'white',
              border: 'none',
              borderRadius: 10,
              padding: '12px 18px',
              fontSize: 14,
              fontWeight: 700,
              cursor: 'pointer',
              boxShadow: '0 4px 12px rgba(15, 23, 42, 0.15)',
            }}
          >
            <ShoppingCart size={18} />
            <span>Stock Purchase</span>
          </button>

          {/* Add Expense Button */}
          <button
            type="button"
            onClick={() => {
              setForm(initialForm)
              setError('')
              setShowAdd(true)
            }}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: 8,
              background: '#00B874',
              color: 'white',
              border: 'none',
              borderRadius: 10,
              padding: '12px 20px',
              fontSize: 14,
              fontWeight: 700,
              cursor: 'pointer',
              boxShadow: '0 4px 12px rgba(0, 184, 116, 0.25)',
            }}
          >
            <Plus size={18} />
            <span>Add Expense</span>
          </button>
        </div>
      </div>

      {/* ── 4 KPI Metric Cards (Dynamic & Clickable to Filter) ───── */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 16 }}>
        {/* Total This Month */}
        <div
          onClick={() => setSelectedCategory('')}
          style={{
            background: 'white',
            borderRadius: 14,
            border: selectedCategory === '' ? '2px solid #00B874' : '1px solid #E2E8F0',
            padding: 20,
            display: 'flex',
            alignItems: 'center',
            gap: 16,
            cursor: 'pointer',
            transition: 'all 0.15s ease',
          }}
          title="Click to view all expenses"
        >
          <div
            style={{
              width: 52,
              height: 52,
              borderRadius: 12,
              background: '#FFEEF1',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: '#FF4D6D',
              flexShrink: 0,
            }}
          >
            <FileText size={24} />
          </div>
          <div>
            <div style={{ fontSize: 12, fontWeight: 600, color: '#64748B' }}>Total This Month</div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginTop: 4 }}>
              <span style={{ fontSize: 20, fontWeight: 800, color: '#0F172A' }}>
                KES {totalAmount.toLocaleString()}
              </span>
              <span
                style={{
                  background: '#DCFCE7',
                  color: '#16A34A',
                  fontSize: 11,
                  fontWeight: 700,
                  padding: '2px 6px',
                  borderRadius: 20,
                }}
              >
                ↑ {periodExpenses.length} items
              </span>
            </div>
            <div style={{ fontSize: 11, color: '#94A3B8', marginTop: 2 }}>All categories</div>
          </div>
        </div>

        {/* Stock Purchase */}
        <div
          onClick={() => setSelectedCategory(selectedCategory === 'STOCK_PURCHASE' ? '' : 'STOCK_PURCHASE')}
          style={{
            background: 'white',
            borderRadius: 14,
            border: selectedCategory === 'STOCK_PURCHASE' ? '2px solid #00B874' : '1px solid #E2E8F0',
            padding: 20,
            display: 'flex',
            alignItems: 'center',
            gap: 16,
            cursor: 'pointer',
            transition: 'all 0.15s ease',
          }}
          title="Click to filter by Stock Purchase"
        >
          <div
            style={{
              width: 52,
              height: 52,
              borderRadius: 12,
              background: '#E8FAF2',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: '#00B874',
              flexShrink: 0,
            }}
          >
            <ShoppingCart size={24} />
          </div>
          <div>
            <div style={{ fontSize: 12, fontWeight: 600, color: '#64748B' }}>Stock Purchase</div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginTop: 4 }}>
              <span style={{ fontSize: 20, fontWeight: 800, color: '#0F172A' }}>
                KES {stockPurchaseTotal.toLocaleString()}
              </span>
              <span
                style={{
                  background: '#FEE2E2',
                  color: '#EF4444',
                  fontSize: 11,
                  fontWeight: 700,
                  padding: '2px 6px',
                  borderRadius: 20,
                }}
              >
                {computePercentageShare(stockPurchaseTotal)}
              </span>
            </div>
            <div style={{ fontSize: 11, color: '#94A3B8', marginTop: 2 }}>Stock purchases</div>
          </div>
        </div>

        {/* Advertising */}
        <div
          style={{
            background: 'white',
            borderRadius: 14,
            border: '1px solid #E2E8F0',
            padding: 20,
            display: 'flex',
            alignItems: 'center',
            gap: 16,
          }}
        >
          <div
            style={{
              width: 52,
              height: 52,
              borderRadius: 12,
              background: '#E0F2FE',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: '#0284C7',
              flexShrink: 0,
            }}
          >
            <Megaphone size={24} />
          </div>
          <div>
            <div style={{ fontSize: 12, fontWeight: 600, color: '#64748B' }}>Advertising</div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginTop: 4 }}>
              <span style={{ fontSize: 20, fontWeight: 800, color: '#0F172A' }}>
                KES {advertisingTotal.toLocaleString()}
              </span>
              <span
                style={{
                  background: '#DCFCE7',
                  color: '#16A34A',
                  fontSize: 11,
                  fontWeight: 700,
                  padding: '2px 6px',
                  borderRadius: 20,
                }}
              >
                {computePercentageShare(advertisingTotal)}
              </span>
            </div>
            <div style={{ fontSize: 11, color: '#94A3B8', marginTop: 2 }}>Marketing spend</div>
          </div>
        </div>

        {/* Operations */}
        <div
          style={{
            background: 'white',
            borderRadius: 14,
            border: '1px solid #E2E8F0',
            padding: 20,
            display: 'flex',
            alignItems: 'center',
            gap: 16,
          }}
        >
          <div
            style={{
              width: 52,
              height: 52,
              borderRadius: 12,
              background: '#FEF3C7',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: '#D97706',
              flexShrink: 0,
            }}
          >
            <Settings size={24} />
          </div>
          <div>
            <div style={{ fontSize: 12, fontWeight: 600, color: '#64748B' }}>Operations</div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginTop: 4 }}>
              <span style={{ fontSize: 20, fontWeight: 800, color: '#0F172A' }}>
                KES {operationsTotal.toLocaleString()}
              </span>
              <span
                style={{
                  background: '#DCFCE7',
                  color: '#16A34A',
                  fontSize: 11,
                  fontWeight: 700,
                  padding: '2px 6px',
                  borderRadius: 20,
                }}
              >
                {computePercentageShare(operationsTotal)}
              </span>
            </div>
            <div style={{ fontSize: 11, color: '#94A3B8', marginTop: 2 }}>Rent + Ops</div>
          </div>
        </div>
      </div>

      {/* ── Middle Row: Dynamic Breakdown & Chart ─────────────────── */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 }}>
        {/* Dynamic Expense Breakdown Card */}
        <div
          style={{
            background: 'white',
            borderRadius: 14,
            border: '1px solid #E2E8F0',
            padding: 24,
            display: 'flex',
            flexDirection: 'column',
            justifyContent: 'space-between',
          }}
        >
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <PieChart size={20} color="#7C3AED" />
              <span style={{ fontSize: 16, fontWeight: 800, color: '#0F172A' }}>Expense Breakdown</span>
            </div>
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: 4,
                border: '1px solid #E2E8F0',
                borderRadius: 8,
                padding: '4px 10px',
                fontSize: 12,
                color: '#64748B',
                background: 'white',
              }}
            >
              <span>{selectedPeriod}</span>
              <ChevronDown size={14} color="#94A3B8" />
            </div>
          </div>

          {breakdownItems.length === 0 ? (
            <div style={{ textAlign: 'center', padding: 40, color: '#94A3B8', fontSize: 13 }}>
              No expense breakdown recorded for this period.
            </div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
              {breakdownItems.map((item, idx) => {
                const IconComponent = item.badge.icon
                return (
                  <div key={idx} style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
                    <div
                      style={{
                        width: 36,
                        height: 36,
                        borderRadius: '50%',
                        background: item.badge.bg,
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        color: item.badge.text,
                        flexShrink: 0,
                      }}
                    >
                      <IconComponent size={18} />
                    </div>
                    <div style={{ width: 140 }}>
                      <div style={{ fontSize: 13, fontWeight: 700, color: '#0F172A', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                        {item.title}
                      </div>
                      <div style={{ fontSize: 10, fontWeight: 700, color: '#94A3B8' }}>{item.subtitle}</div>
                    </div>
                    <div style={{ flex: 1, background: '#F1F5F9', height: 8, borderRadius: 4, overflow: 'hidden' }}>
                      <div
                        style={{
                          width: `${Math.max(4, item.percentage)}%`,
                          height: '100%',
                          background: item.badge.barColor,
                          borderRadius: 4,
                        }}
                      />
                    </div>
                    <div style={{ width: 100, textAlign: 'right', fontSize: 13, fontWeight: 700, color: '#FF4D4D' }}>
                      KES {item.amount.toLocaleString()}
                    </div>
                    <div style={{ width: 36, textAlign: 'right', fontSize: 12, color: '#94A3B8' }}>{item.percentage}%</div>
                  </div>
                )
              })}
            </div>
          )}
        </div>

        {/* Dynamic Monthly Expense Chart Card */}
        <div
          style={{
            background: 'white',
            borderRadius: 14,
            border: '1px solid #E2E8F0',
            padding: 24,
            display: 'flex',
            flexDirection: 'column',
          }}
        >
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <BarChart2 size={20} color="#7C3AED" />
              <span style={{ fontSize: 16, fontWeight: 800, color: '#0F172A' }}>Monthly Expense Chart</span>
            </div>
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: 4,
                border: '1px solid #E2E8F0',
                borderRadius: 8,
                padding: '4px 10px',
                fontSize: 12,
                color: '#64748B',
                background: 'white',
              }}
            >
              <span>{selectedPeriod}</span>
              <ChevronDown size={14} color="#94A3B8" />
            </div>
          </div>

          {/* Scaled Bar Chart Area */}
          <div style={{ flex: 1, display: 'flex', height: 230, position: 'relative', marginTop: 10 }}>
            {/* Y Axis */}
            <div
              style={{
                width: 48,
                display: 'flex',
                flexDirection: 'column',
                justifyContent: 'space-between',
                paddingBottom: 40,
                alignItems: 'flex-end',
                paddingRight: 8,
                fontSize: 10,
                color: '#94A3B8',
              }}
            >
              <div>{formatK(chartMax)}</div>
              <div>{formatK(chartMax * 0.75)}</div>
              <div>{formatK(chartMax * 0.5)}</div>
              <div>{formatK(chartMax * 0.25)}</div>
              <div>0</div>
            </div>

            {/* Grid & Bars Container */}
            <div style={{ flex: 1, position: 'relative', height: '100%' }}>
              {/* Horizontal Grid lines */}
              <div
                style={{
                  position: 'absolute',
                  inset: '0 0 40px 0',
                  display: 'flex',
                  flexDirection: 'column',
                  justifyContent: 'space-between',
                  pointerEvents: 'none',
                }}
              >
                {[0, 1, 2, 3, 4].map((i) => (
                  <div key={i} style={{ borderBottom: '1px solid #F1F5F9', width: '100%' }} />
                ))}
              </div>

              {/* Bars Row */}
              <div
                style={{
                  position: 'absolute',
                  inset: 0,
                  display: 'flex',
                  justifyContent: 'space-around',
                  alignItems: 'flex-end',
                }}
              >
                {chartItems.map((item, idx) => {
                  const barHeight = chartMax > 0 ? (item.amount / chartMax) * 130 : 6
                  const IconComp = item.badge.icon
                  return (
                    <div key={idx} style={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
                      <div style={{ fontSize: 11, fontWeight: 700, color: item.badge.text, marginBottom: 4 }}>
                        KES {item.amount.toLocaleString()}
                      </div>
                      <div
                        style={{
                          width: 58,
                          height: Math.max(6, barHeight),
                          background: item.badge.barColor,
                          borderRadius: '4px 4px 0 0',
                          transition: 'height 0.3s ease',
                        }}
                      />
                      <div style={{ display: 'flex', alignItems: 'center', gap: 4, marginTop: 8, height: 24 }}>
                        <IconComp size={14} color={item.badge.text} />
                        <span style={{ fontSize: 11, fontWeight: 700, color: '#0F172A' }}>{item.label}</span>
                      </div>
                    </div>
                  )
                })}
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* ── Bottom Card: Expenses List ────────────────────────────── */}
      <div
        style={{
          background: 'white',
          borderRadius: 14,
          border: '1px solid #E2E8F0',
          overflow: 'hidden',
        }}
      >
        {/* Header Toolbar */}
        <div
          style={{
            padding: '18px 24px',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            borderBottom: '1px solid #E2E8F0',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <FileText size={20} color="#7C3AED" />
            <span style={{ fontSize: 16, fontWeight: 800, color: '#0F172A' }}>Expenses List</span>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            {/* Search Input */}
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: 8,
                background: 'white',
                border: '1px solid #E2E8F0',
                borderRadius: 8,
                padding: '6px 12px',
                width: 260,
              }}
            >
              <Search size={16} color="#94A3B8" />
              <input
                type="text"
                aria-label="Search expense, category"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                style={{
                  border: 'none',
                  outline: 'none',
                  fontSize: 13,
                  width: '100%',
                  color: '#0F172A',
                }}
              />
            </div>

            {/* Filter Dropdown */}
            <div style={{ position: 'relative' }}>
              <button
                type="button"
                onClick={() => setShowFilterDropdown((v) => !v)}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 6,
                  background: 'white',
                  border: '1px solid #E2E8F0',
                  borderRadius: 8,
                  padding: '7px 14px',
                  fontSize: 13,
                  fontWeight: 600,
                  color: '#0F172A',
                  cursor: 'pointer',
                }}
              >
                <Filter size={15} color="#64748B" />
                <span>{selectedCategory ? selectedCategory.replace('_', ' ') : 'Filter'}</span>
                <ChevronDown size={14} color="#94A3B8" />
              </button>

              {showFilterDropdown && (
                <div
                  style={{
                    position: 'absolute',
                    top: '100%',
                    right: 0,
                    marginTop: 6,
                    background: 'white',
                    border: '1px solid #E2E8F0',
                    borderRadius: 10,
                    boxShadow: '0 10px 25px -5px rgba(0,0,0,0.1)',
                    zIndex: 20,
                    minWidth: 180,
                    padding: 4,
                  }}
                >
                  <div
                    onClick={() => {
                      setSelectedCategory('')
                      setShowFilterDropdown(false)
                    }}
                    style={{
                      padding: '8px 12px',
                      fontSize: 12,
                      cursor: 'pointer',
                      borderRadius: 6,
                      color: !selectedCategory ? '#00B874' : '#0F172A',
                      fontWeight: !selectedCategory ? 700 : 500,
                    }}
                  >
                    All Categories
                  </div>
                  {EXPENSE_CATEGORIES.map((c) => (
                    <div
                      key={c.value}
                      onClick={() => {
                        setSelectedCategory(c.value)
                        setShowFilterDropdown(false)
                      }}
                      style={{
                        padding: '8px 12px',
                        fontSize: 12,
                        cursor: 'pointer',
                        borderRadius: 6,
                        color: selectedCategory === c.value ? '#00B874' : '#0F172A',
                        fontWeight: selectedCategory === c.value ? 700 : 500,
                      }}
                    >
                      {c.label}
                    </div>
                  ))}
                </div>
              )}
            </div>

            {/* Refresh Button */}
            <button
              type="button"
              onClick={loadExpenses}
              title="Refresh"
              style={{
                width: 36,
                height: 36,
                borderRadius: 8,
                border: '1px solid #E2E8F0',
                background: 'white',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                cursor: 'pointer',
                color: '#64748B',
              }}
            >
              <MoreVertical size={18} />
            </button>
          </div>
        </div>

        {/* Table */}
        <div style={{ overflowX: 'auto' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
            <thead>
              <tr style={{ background: '#F8FAFC', borderBottom: '1px solid #E2E8F0' }}>
                <th style={{ padding: '12px 20px', fontSize: 12, fontWeight: 700, color: '#94A3B8', width: 40 }}>#</th>
                <th style={{ padding: '12px 20px', fontSize: 12, fontWeight: 700, color: '#94A3B8' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
                    <span>Description</span>
                    <ArrowUpDown size={12} color="#CBD5E1" />
                  </div>
                </th>
                <th style={{ padding: '12px 20px', fontSize: 12, fontWeight: 700, color: '#94A3B8' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
                    <span>Category</span>
                    <ArrowUpDown size={12} color="#CBD5E1" />
                  </div>
                </th>
                <th style={{ padding: '12px 20px', fontSize: 12, fontWeight: 700, color: '#94A3B8' }}>Amount</th>
                <th style={{ padding: '12px 20px', fontSize: 12, fontWeight: 700, color: '#94A3B8' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
                    <span>Date</span>
                    <ArrowUpDown size={12} color="#CBD5E1" />
                  </div>
                </th>
                <th style={{ padding: '12px 20px', fontSize: 12, fontWeight: 700, color: '#94A3B8' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
                    <span>Added By</span>
                    <ArrowUpDown size={12} color="#CBD5E1" />
                  </div>
                </th>
                <th style={{ padding: '12px 20px', fontSize: 12, fontWeight: 700, color: '#94A3B8' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
                    <span>Actions</span>
                    <ArrowUpDown size={12} color="#CBD5E1" />
                  </div>
                </th>
              </tr>
            </thead>
            <tbody>
              {filteredExpenses.length === 0 ? (
                <tr>
                  <td colSpan={7} style={{ padding: '40px 20px', textAlign: 'center', color: '#94A3B8', fontSize: 14 }}>
                    {expenses.length === 0 ? 'No expenses recorded yet. Click "+ Record Expense" to get started.' : 'No expenses found matching the criteria.'}
                  </td>
                </tr>
              ) : (
                filteredExpenses.map((expense: ExpenseResponse, idx: number) => {
                  const badge = getBadgeStyle(expense.category)
                  const BadgeIcon = badge.icon
                  return (
                    <tr
                      key={expense.id}
                      style={{
                        borderBottom: '1px solid #F1F5F9',
                        transition: 'background 0.15s',
                      }}
                    >
                      <td style={{ padding: '14px 20px', fontSize: 13, fontWeight: 600, color: '#0F172A' }}>
                        {idx + 1}
                      </td>
                      <td style={{ padding: '14px 20px', fontSize: 13, fontWeight: 600, color: '#0F172A' }}>
                        {expense.description}
                      </td>
                      <td style={{ padding: '14px 20px' }}>
                        <span
                          style={{
                            display: 'inline-flex',
                            alignItems: 'center',
                            gap: 5,
                            background: badge.bg,
                            color: badge.text,
                            padding: '4px 10px',
                            borderRadius: 20,
                            fontSize: 11,
                            fontWeight: 700,
                          }}
                        >
                          <BadgeIcon size={12} />
                          <span>{badge.label}</span>
                        </span>
                      </td>
                      <td style={{ padding: '14px 20px', fontSize: 13, fontWeight: 800, color: '#FF4D4D' }}>
                        KES {expense.amount.toLocaleString()}
                      </td>
                      <td style={{ padding: '14px 20px' }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: 12, color: '#475569' }}>
                          <Calendar size={13} color="#94A3B8" />
                          <span>{expense.expenseDate}</span>
                        </div>
                      </td>
                      {/* Dynamic Added By User */}
                      <td style={{ padding: '14px 20px' }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                          <div
                            style={{
                              width: 24,
                              height: 24,
                              borderRadius: '50%',
                              background: '#DCFCE7',
                              color: '#16A34A',
                              fontSize: 11,
                              fontWeight: 800,
                              display: 'flex',
                              alignItems: 'center',
                              justifyContent: 'center',
                            }}
                          >
                            {userInitial}
                          </div>
                          <span style={{ fontSize: 13, fontWeight: 500, color: '#334155' }}>{currentUserName}</span>
                        </div>
                      </td>
                      <td style={{ padding: '14px 20px' }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                          {/* Edit */}
                          <button
                            type="button"
                            title="Edit"
                            onClick={() => openEdit(expense)}
                            style={{
                              width: 28,
                              height: 28,
                              borderRadius: 6,
                              background: '#EFF6FF',
                              border: 'none',
                              display: 'flex',
                              alignItems: 'center',
                              justifyContent: 'center',
                              cursor: 'pointer',
                              color: '#3B82F6',
                            }}
                          >
                            <Edit2 size={13} />
                          </button>

                          {/* Delete */}
                          <button
                            type="button"
                            title="Delete"
                            onClick={() => handleDelete(expense.id, expense.description)}
                            style={{
                              width: 28,
                              height: 28,
                              borderRadius: 6,
                              background: '#FEF2F2',
                              border: 'none',
                              display: 'flex',
                              alignItems: 'center',
                              justifyContent: 'center',
                              cursor: 'pointer',
                              color: '#EF4444',
                            }}
                          >
                            <Trash2 size={13} />
                          </button>

                          {/* View */}
                          <button
                            type="button"
                            title="View Details"
                            onClick={() => setViewingExpense(expense)}
                            style={{
                              width: 28,
                              height: 28,
                              borderRadius: 6,
                              background: '#F1F5F9',
                              border: 'none',
                              display: 'flex',
                              alignItems: 'center',
                              justifyContent: 'center',
                              cursor: 'pointer',
                              color: '#3B82F6',
                            }}
                          >
                            <Eye size={13} />
                          </button>
                        </div>
                      </td>
                    </tr>
                  )
                })
              )}
            </tbody>
          </table>
        </div>

        {/* Table Footer */}
        <div
          style={{
            padding: '14px 24px',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            borderTop: '1px solid #E2E8F0',
            fontSize: 12,
            color: '#94A3B8',
          }}
        >
          <div>
            Showing {filteredExpenses.length} of {periodExpenses.length} expenses
          </div>
          <div>All matching expenses loaded</div>
        </div>
      </div>

      {/* ── Add / Edit Modal ──────────────────────────────────────── */}
      {(showAdd || editingExpense) && (
        <Modal
          title={editingExpense ? 'Edit Expense' : 'Add New Expense'}
          onClose={() => {
            setShowAdd(false)
            setEditingExpense(null)
          }}
          footer={
            <>
              <Btn
                variant="secondary"
                onClick={() => {
                  setShowAdd(false)
                  setEditingExpense(null)
                }}
              >
                Cancel
              </Btn>
              <Btn onClick={handleSaveExpense} disabled={saving}>
                {saving ? 'Saving...' : 'Save Expense'}
              </Btn>
            </>
          }
        >
          <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
            {error && (
              <div style={{ background: '#FEE2E2', color: '#EF4444', padding: '8px 12px', borderRadius: 8, fontSize: 13 }}>
                {error}
              </div>
            )}
            <Input
              label="Description *"
              value={form.description}
              onChange={(v) => setForm((p) => ({ ...p, description: v }))}
            />
            <Input
              label="Amount (KES) *"
              type="number"
              value={form.amount}
              onChange={(v) => setForm((p) => ({ ...p, amount: v }))}
            />
            <Select
              label="Category *"
              value={form.category}
              onChange={(v) => setForm((p) => ({ ...p, category: v }))}
              options={EXPENSE_CATEGORIES}
            />
            <Input
              label="Date *"
              type="date"
              value={form.expenseDate}
              onChange={(v) => setForm((p) => ({ ...p, expenseDate: v }))}
            />
            <Select
              label="Payment Method"
              value={form.paymentMethod}
              onChange={(v) => setForm((p) => ({ ...p, paymentMethod: v }))}
              options={[
                { value: 'M-Pesa', label: 'M-Pesa' },
                { value: 'Cash', label: 'Cash' },
                { value: 'Card', label: 'Card' },
                { value: 'Bank Transfer', label: 'Bank Transfer' },
              ]}
            />
            <Input
              label="Notes (Optional)"
              value={form.notes}
              onChange={(v) => setForm((p) => ({ ...p, notes: v }))}
            />
          </div>
        </Modal>
      )}

      {/* ── View Expense Modal ────────────────────────────────────── */}
      {viewingExpense && (
        <Modal
          title="Expense Details"
          onClose={() => setViewingExpense(null)}
          footer={<Btn onClick={() => setViewingExpense(null)}>Close</Btn>}
        >
          <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', borderBottom: '1px solid #F1F5F9', paddingBottom: 8 }}>
              <span style={{ fontSize: 13, color: '#94A3B8' }}>Description</span>
              <span style={{ fontSize: 13, fontWeight: 700, color: '#0F172A' }}>{viewingExpense.description}</span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', borderBottom: '1px solid #F1F5F9', paddingBottom: 8 }}>
              <span style={{ fontSize: 13, color: '#94A3B8' }}>Category</span>
              <span style={{ fontSize: 13, fontWeight: 700, color: '#0F172A' }}>{viewingExpense.category}</span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', borderBottom: '1px solid #F1F5F9', paddingBottom: 8 }}>
              <span style={{ fontSize: 13, color: '#94A3B8' }}>Amount</span>
              <span style={{ fontSize: 13, fontWeight: 700, color: '#EF4444' }}>
                KES {viewingExpense.amount.toLocaleString()}
              </span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', borderBottom: '1px solid #F1F5F9', paddingBottom: 8 }}>
              <span style={{ fontSize: 13, color: '#94A3B8' }}>Date</span>
              <span style={{ fontSize: 13, color: '#0F172A' }}>{viewingExpense.expenseDate}</span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', borderBottom: '1px solid #F1F5F9', paddingBottom: 8 }}>
              <span style={{ fontSize: 13, color: '#94A3B8' }}>Added By</span>
              <span style={{ fontSize: 13, color: '#0F172A' }}>{currentUserName} ({userRole})</span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
              <span style={{ fontSize: 13, color: '#94A3B8' }}>Expense ID</span>
              <span style={{ fontSize: 12, color: '#64748B', fontFamily: 'monospace' }}>{viewingExpense.id}</span>
            </div>
          </div>
        </Modal>
      )}
    </div>
  )
}

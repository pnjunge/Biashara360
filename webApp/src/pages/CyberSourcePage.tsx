import React, { useCallback, useEffect, useMemo, useState } from 'react'
import { CheckCircle, Clock, CreditCard, RefreshCw, XCircle } from 'lucide-react'
import { Btn, Card, DataTable, KpiCard, PageHeader } from '../components/ui'
import { CsTransactionRecord, cyberSourceApi } from '../services/api'

function CardBrand({ type }: { type: string }) {
  const colors: Record<string, string> = { VISA: '#1A1F71', MASTERCARD: '#EB001B', AMEX: '#2E77BC' }
  const color = colors[type] ?? '#64748B'
  return (
    <span style={{ fontSize: 10, fontWeight: 900, letterSpacing: 1, color, background: `${color}15`, padding: '2px 6px', borderRadius: 4 }}>
      {type || 'CARD'}
    </span>
  )
}

function TransactionStatus({ status }: { status: string }) {
  const styles: Record<string, [string, string, React.ReactNode]> = {
    CAPTURED: ['var(--b360-green)', 'var(--b360-green-bg)', <CheckCircle size={12} />],
    AUTHORIZED: ['var(--b360-blue)', 'var(--b360-blue-bg)', <Clock size={12} />],
    REFUNDED: ['var(--b360-amber)', 'var(--b360-amber-bg)', <RefreshCw size={12} />],
    DECLINED: ['var(--b360-red)', 'var(--b360-red-bg)', <XCircle size={12} />],
    VOIDED: ['#64748B', '#F1F5F9', <XCircle size={12} />],
    ERROR: ['var(--b360-red)', 'var(--b360-red-bg)', <XCircle size={12} />],
  }
  const [color, background, icon] = styles[status] ?? ['#64748B', '#F1F5F9', null]
  return (
    <span style={{ display: 'inline-flex', alignItems: 'center', gap: 4, color, background, borderRadius: 20, padding: '3px 8px', fontSize: 11, fontWeight: 700 }}>
      {icon}{status}
    </span>
  )
}

export default function CyberSourcePage() {
  const [transactions, setTransactions] = useState<CsTransactionRecord[]>([])
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const loadReport = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const response = await cyberSourceApi.getTransactions()
      if (!response.success) throw new Error(response.message || 'Unable to load card payments')
      setTransactions(response.data ?? [])
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Unable to load card payments')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { void loadReport() }, [loadReport])

  const visibleTransactions = useMemo(
    () => statusFilter === 'ALL' ? transactions : transactions.filter(transaction => transaction.status === statusFilter),
    [statusFilter, transactions],
  )
  const captured = transactions.filter(transaction => transaction.status === 'CAPTURED').reduce((sum, transaction) => sum + transaction.amount, 0)
  const authorized = transactions.filter(transaction => transaction.status === 'AUTHORIZED').reduce((sum, transaction) => sum + transaction.amount, 0)
  const refunded = transactions.filter(transaction => transaction.status === 'REFUNDED').reduce((sum, transaction) => sum + transaction.amount, 0)
  const declined = transactions.filter(transaction => transaction.status === 'DECLINED' || transaction.status === 'ERROR').length
  const statuses = ['ALL', ...Array.from(new Set(transactions.map(transaction => transaction.status)))]

  return (
    <div className="fade-in" style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
      <PageHeader
        title="Card Payment Report"
        action={<Btn variant="secondary" icon={<RefreshCw size={14} />} onClick={() => void loadReport()} disabled={loading}>{loading ? 'Refreshing…' : 'Refresh'}</Btn>}
      />

      <div style={{ color: 'var(--b360-text-secondary)', fontSize: 13, marginTop: -24 }}>
        CyberSource card transactions recorded for your business. Card collection is initiated from an order or POS checkout.
      </div>

      <div className="responsive-kpi-grid" style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(210px, 1fr))', gap: 14 }}>
        <KpiCard title="Captured" value={`KES ${captured.toLocaleString()}`} change="Successfully settled" icon={<CheckCircle size={18} />} color="var(--b360-green)" />
        <KpiCard title="Authorized" value={`KES ${authorized.toLocaleString()}`} change="Awaiting capture" icon={<Clock size={18} />} color="var(--b360-blue)" />
        <KpiCard title="Refunded" value={`KES ${refunded.toLocaleString()}`} change="Returned to customers" icon={<RefreshCw size={18} />} color="var(--b360-amber)" />
        <KpiCard title="Declined / Errors" value={declined.toLocaleString()} change="Unsuccessful transactions" icon={<XCircle size={18} />} color="var(--b360-red)" />
      </div>

      <Card style={{ padding: 16 }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 12, marginBottom: 16 }}>
          <div>
            <h3 style={{ margin: 0, fontSize: 16 }}>CyberSource transactions</h3>
            <span style={{ color: 'var(--b360-text-secondary)', fontSize: 12 }}>{visibleTransactions.length} transaction{visibleTransactions.length === 1 ? '' : 's'}</span>
          </div>
          <label style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 12, color: 'var(--b360-text-secondary)' }}>
            Status
            <select value={statusFilter} onChange={event => setStatusFilter(event.target.value)} style={{ padding: '8px 10px', border: '1px solid var(--b360-border)', borderRadius: 8, background: 'white' }}>
              {statuses.map(status => <option key={status} value={status}>{status === 'ALL' ? 'All statuses' : status}</option>)}
            </select>
          </label>
        </div>

        {error ? (
          <div style={{ padding: 24, textAlign: 'center', color: 'var(--b360-red)', background: 'var(--b360-red-bg)', borderRadius: 8 }}>{error}</div>
        ) : loading ? (
          <div style={{ padding: 40, textAlign: 'center', color: 'var(--b360-text-secondary)' }}>Loading card payment report…</div>
        ) : visibleTransactions.length === 0 ? (
          <div style={{ padding: 40, textAlign: 'center', color: 'var(--b360-text-secondary)' }}>
            <CreditCard size={28} style={{ marginBottom: 8 }} />
            <div>No card transactions found.</div>
          </div>
        ) : (
          <DataTable
            headers={['CyberSource reference', 'Order', 'Type', 'Card', 'Amount', 'Status', 'Approval', 'Reconciliation', 'Date']}
            rows={visibleTransactions.map(transaction => [
              <span style={{ fontFamily: 'monospace', fontSize: 11 }}>{transaction.csTransactionId || '—'}</span>,
              <span style={{ fontWeight: 700, color: 'var(--b360-green)' }}>{transaction.orderId || '—'}</span>,
              transaction.type || '—',
              <span style={{ display: 'inline-flex', alignItems: 'center', gap: 6 }}><CardBrand type={transaction.cardType} /> ••{transaction.cardLast4 || '—'}</span>,
              <span style={{ fontWeight: 700 }}>{transaction.currency || 'KES'} {transaction.amount.toLocaleString()}</span>,
              <TransactionStatus status={transaction.status} />,
              <span style={{ fontFamily: 'monospace' }}>{transaction.approvalCode || '—'}</span>,
              <span style={{ fontFamily: 'monospace', fontSize: 11 }}>{transaction.reconciliationId || '—'}</span>,
              new Date(transaction.createdAt).toLocaleString('en-KE'),
            ])}
          />
        )}
      </Card>
    </div>
  )
}

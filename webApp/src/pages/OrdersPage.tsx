import React, { useState, useEffect } from 'react'
import { PageHeader, Card, Btn, DataTable, StatusBadge, KpiCard } from '../components/ui'
import { ShoppingCart, Plus } from 'lucide-react'
import { orderApi, OrderResponse } from '../services/api'

export function OrdersPage() {
  const [orders, setOrders] = useState<OrderResponse[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    orderApi.list().then(res => {
      if (res.success && res.data) setOrders(res.data.data)
    }).finally(() => setLoading(false))
  }, [])

  return (
    <div className="fade-in" style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
      <PageHeader title="Orders"
        action={<Btn icon={<Plus size={14} />}>New Order</Btn>} />

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4,1fr)', gap: 12 }}>
        <KpiCard title="Total Orders"   value={String(orders.length)}                                                      change="All time"      icon={<ShoppingCart size={18} />} color="var(--b360-blue)" />
        <KpiCard title="Delivered"      value={String(orders.filter(o => o.deliveryStatus === 'DELIVERED').length)}        change="Completed"     icon={<ShoppingCart size={18} />} color="var(--b360-green)" />
        <KpiCard title="In Progress"    value={String(orders.filter(o => o.deliveryStatus === 'PROCESSING' || o.deliveryStatus === 'SHIPPED').length)} change="Active" icon={<ShoppingCart size={18} />} color="var(--b360-amber)" />
        <KpiCard title="Pending"        value={String(orders.filter(o => o.deliveryStatus === 'PENDING').length)}          change="Awaiting action" icon={<ShoppingCart size={18} />} color="var(--b360-red)" />
      </div>

      <Card>
        {loading ? (
          <div style={{ padding: 40, textAlign: 'center', color: 'var(--b360-text-secondary)' }}>Loading...</div>
        ) : orders.length === 0 ? (
          <div style={{ padding: 40, textAlign: 'center', color: 'var(--b360-text-secondary)' }}>No orders yet</div>
        ) : (
          <DataTable
            headers={['Order #', 'Customer', 'Items', 'Total', 'Payment', 'Delivery', 'Date', 'Actions']}
            rows={orders.map(o => [
              <span style={{ fontFamily: 'monospace', fontWeight: 700 }}>{o.orderNumber}</span>,
              <span style={{ fontWeight: 600 }}>{o.customerName}</span>,
              o.items.length,
              <span style={{ fontWeight: 700 }}>KES {o.subtotal.toLocaleString()}</span>,
              <StatusBadge status={o.paymentStatus} />,
              <StatusBadge status={o.deliveryStatus} />,
              <span style={{ fontSize: 12, color: 'var(--b360-text-secondary)' }}>{new Date(o.createdAt).toLocaleDateString('en-KE')}</span>,
              <Btn small>View</Btn>,
            ])}
          />
        )}
      </Card>
    </div>
  )
}

export default OrdersPage

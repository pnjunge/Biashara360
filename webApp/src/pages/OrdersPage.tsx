import React from 'react'
import { PageHeader, Card, Btn, DataTable, StatusBadge, KpiCard } from '../components/ui'
import { ShoppingCart, Plus } from 'lucide-react'

const orders = [
  { id: 1001, customer: 'Amina Hassan',  items: 3, total: 4500,  status: 'DELIVERED', date: 'Today 2:30PM' },
  { id: 1002, customer: 'David Kamau',   items: 1, total: 6800,  status: 'PROCESSING', date: 'Yesterday' },
  { id: 1003, customer: 'Sarah Wangui',  items: 5, total: 1200,  status: 'PENDING',    date: 'Yesterday' },
  { id: 1004, customer: 'Tom Mutua',     items: 2, total: 2300,  status: 'SHIPPED',    date: 'Mon' },
]

export function OrdersPage() {
  return (
    <div className="fade-in" style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
      <PageHeader title="Orders"
        action={<Btn icon={<Plus size={14} />}>New Order</Btn>} />

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4,1fr)', gap: 12 }}>
        <KpiCard title="Total Orders"   value={String(orders.length)}                                          change="All time"      icon={<ShoppingCart size={18} />} color="var(--b360-blue)" />
        <KpiCard title="Delivered"      value={String(orders.filter(o => o.status === 'DELIVERED').length)}    change="Completed"     icon={<ShoppingCart size={18} />} color="var(--b360-green)" />
        <KpiCard title="In Progress"    value={String(orders.filter(o => o.status === 'PROCESSING' || o.status === 'SHIPPED').length)} change="Active" icon={<ShoppingCart size={18} />} color="var(--b360-amber)" />
        <KpiCard title="Pending"        value={String(orders.filter(o => o.status === 'PENDING').length)}      change="Awaiting action" icon={<ShoppingCart size={18} />} color="var(--b360-red)" />
      </div>

      <Card>
        <DataTable
          headers={['Order #', 'Customer', 'Items', 'Total', 'Status', 'Date', 'Actions']}
          rows={orders.map(o => [
            <span style={{ fontFamily: 'monospace', fontWeight: 700 }}>#{o.id}</span>,
            <span style={{ fontWeight: 600 }}>{o.customer}</span>,
            o.items,
            <span style={{ fontWeight: 700 }}>KES {o.total.toLocaleString()}</span>,
            <StatusBadge status={o.status} />,
            o.date,
            <Btn small>View</Btn>,
          ])}
        />
      </Card>
    </div>
  )
}

export default OrdersPage

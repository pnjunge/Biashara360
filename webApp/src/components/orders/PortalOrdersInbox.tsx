import React, { useEffect, useRef, useState } from 'react'
import { client, ApiResponse } from '../../services/api'
import { Btn, Modal } from '../ui'
import { CheckCircle2, ChevronRight, Clock3, CreditCard, List, PackageOpen, ShoppingBag } from 'lucide-react'

type PortalOrder = {
  id: string; orderNumber: string; customerName: string; location: string;
  amount: number; paymentStatus: string; createdAt: string; claimedBy: string | null;
  items: Array<{ name: string; quantity: number }>
}
type Queue = { waiting: PortalOrder[]; mine: PortalOrder[] }

const ageLabel = (createdAt: string) => {
  const minutes = Math.max(0, Math.floor((Date.now() - new Date(createdAt).getTime()) / 60000))
  return minutes < 60 ? `${minutes} min ago` : `${Math.floor(minutes / 60)}h ${minutes % 60}m ago`
}

export default function PortalOrdersInbox() {
  const [queue, setQueue] = useState<Queue>({ waiting: [], mine: [] })
  const [open, setOpen] = useState(false)
  const [mine, setMine] = useState(false)
  const [busy, setBusy] = useState('')
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const known = useRef(new Set<string>())
  const revision = useRef(0)
  const mounted = useRef(true)

  const refresh = async () => {
    const version = revision.current
    try {
      const { data: result } = await client.get<ApiResponse<Queue>>('/portal-orders')
      if (!mounted.current || version !== revision.current) return
      if (!result.success || !result.data) throw new Error(result.message || 'Could not load portal orders.')
      const incoming = result.data.waiting.some(order => !known.current.has(order.id))
      known.current = new Set(result.data.waiting.map(order => order.id))
      setQueue(result.data)
      if (incoming) { setMine(false); setOpen(true); setMessage('New portal orders are waiting to be claimed.') }
      setError('')
    } catch (e: any) {
      if (mounted.current && version === revision.current) setError(e.response?.data?.message || e.message || 'Portal orders could not refresh.')
    }
  }

  useEffect(() => {
    mounted.current = true
    let active = true
    let timer: ReturnType<typeof setTimeout>
    const poll = async () => { await refresh(); if (active) timer = setTimeout(poll, 5000) }
    poll()
    return () => { active = false; mounted.current = false; clearTimeout(timer); revision.current++ }
  }, [])

  const claim = async (order: PortalOrder) => {
    if (busy) return
    setBusy(order.id); setError(''); setMessage(''); revision.current++
    try {
      const { data: result } = await client.post<ApiResponse<PortalOrder>>(`/portal-orders/${encodeURIComponent(order.id)}/claim`)
      if (!result.success || !result.data) throw new Error(result.message || 'Could not claim order.')
      revision.current++
      setQueue(current => ({ waiting: current.waiting.filter(item => item.id !== order.id), mine: [result.data!, ...current.mine.filter(item => item.id !== order.id)] }))
      setMessage(`You claimed ${order.orderNumber}. Payment status is ${result.data.paymentStatus}.`)
      setMine(true)
      await refresh()
    } catch (e: any) {
      revision.current++
      await refresh()
      setError(e.response?.data?.message || e.message || 'Could not claim order.')
    } finally { setBusy('') }
  }

  const orders = mine ? queue.mine : queue.waiting
  return <>
    <Btn small variant={queue.waiting.length ? 'primary' : 'secondary'} onClick={() => { setOpen(true); refresh() }}>
      Portal orders ({queue.waiting.length}){error ? ' · !' : ''}
    </Btn>
    {open && <Modal
      title={<span className="portal-modal-title"><span className="portal-modal-title-icon"><ShoppingBag size={20} /></span><span>Portal orders</span></span>}
      onClose={() => setOpen(false)}
      wide
    >
      <div className="portal-orders-modal-body">
        <p className="portal-orders-subtitle">Any staff member in this business can claim a waiting order.</p>
        <div className="portal-order-tabs" role="tablist" aria-label="Portal order queues">
          <button type="button" role="tab" aria-selected={!mine} className={!mine ? 'active' : ''} onClick={() => setMine(false)}>Waiting ({queue.waiting.length})</button>
          <button type="button" role="tab" aria-selected={mine} className={mine ? 'active' : ''} onClick={() => setMine(true)}>My orders ({queue.mine.length})</button>
        </div>
        {message && <div className="portal-order-message" role="status"><CheckCircle2 size={16} /> {message}</div>}
        {error && <div className="portal-order-error" role="alert">{error}</div>}
        {!orders.length && <div className="portal-order-empty">{mine ? 'You have no active claimed portal orders.' : 'No portal orders are waiting.'}</div>}
        <div className="portal-order-list">
          {orders.map(order => <article key={order.id} className="portal-order-card">
            <div className="portal-order-main">
              <div className="portal-order-thumb"><PackageOpen size={28} /></div>
              <div className="portal-order-details">
                <div className="portal-order-line portal-order-heading-line"><strong>{order.orderNumber}</strong><span className="portal-order-separator">•</span><span className="portal-order-channel">{(order.location || 'PICKUP').toUpperCase()}</span></div>
                <div className="portal-order-customer">{order.customerName}</div>
                <div className="portal-order-line"><ShoppingBag size={14} /> <span>KES {order.amount.toLocaleString(undefined, { minimumFractionDigits: 2 })}</span></div>
                <div className="portal-order-line"><CreditCard size={14} /> <span>Payment: {order.paymentStatus}</span></div>
                <div className="portal-order-items"><List size={15} /> <div>{order.items.map((item, index) => <span key={index}>{item.quantity} × {item.name}</span>)}</div></div>
              </div>
              <div className="portal-order-age"><span><Clock3 size={15} /> {ageLabel(order.createdAt)}</span><ChevronRight size={18} /></div>
            </div>
            <div className="portal-order-card-footer">
              {mine ? <span className="portal-order-claimed"><CheckCircle2 size={15} /> Claimed by you</span> : <Btn disabled={!!busy} onClick={() => claim(order)}>{busy === order.id ? 'Claiming…' : 'Claim order'}</Btn>}
            </div>
          </article>)}
        </div>
      </div>
    </Modal>}
  </>
}

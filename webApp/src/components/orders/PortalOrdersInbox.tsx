import React, { useEffect, useRef, useState } from 'react'
import { client, ApiResponse } from '../../services/api'
import { Btn, Modal } from '../ui'

type PortalOrder = {
  id: string; orderNumber: string; customerName: string; location: string;
  amount: number; paymentStatus: string; createdAt: string; claimedBy: string | null;
  items: Array<{ name: string; quantity: number }>
}
type Queue = { waiting: PortalOrder[]; mine: PortalOrder[] }

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
    {open && <Modal title="Portal orders" onClose={() => setOpen(false)} wide>
      <p>Any staff member in this business can claim a waiting order.</p>
      <div style={{ display: 'flex', gap: 8, marginBottom: 14 }}>
        <Btn small variant={!mine ? 'primary' : 'secondary'} onClick={() => setMine(false)}>Waiting ({queue.waiting.length})</Btn>
        <Btn small variant={mine ? 'primary' : 'secondary'} onClick={() => setMine(true)}>My orders ({queue.mine.length})</Btn>
      </div>
      {message && <p role="status" style={{ color: '#047857' }}>{message}</p>}
      {error && <p role="alert" style={{ color: '#b91c1c' }}>{error}</p>}
      {!orders.length && <p>{mine ? 'You have no active claimed portal orders.' : 'No portal orders are waiting.'}</p>}
      <div style={{ display: 'grid', gap: 12 }}>
        {orders.map(order => <article key={order.id} style={{ padding: 16, border: '1px solid #dce4e2', borderRadius: 10 }}>
          <strong>{order.orderNumber} · {order.location || 'Pickup'}</strong>
          <p>{order.customerName} · KES {order.amount.toLocaleString()} · Payment: {order.paymentStatus}</p>
          <ul>{order.items.map((item, index) => <li key={index}>{item.quantity} × {item.name}</li>)}</ul>
          {mine ? <span>Claimed by you</span> : <Btn disabled={!!busy} onClick={() => claim(order)}>{busy === order.id ? 'Claiming…' : 'Claim order'}</Btn>}
        </article>)}
      </div>
    </Modal>}
  </>
}

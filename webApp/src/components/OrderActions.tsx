import React, { useState } from 'react'
import { createPortal } from 'react-dom'
import { Btn, Input, Modal } from './ui'
import { OrderResponse, hospitalityOpsApi, orderApi } from '../services/api'

export function OrderActions({ order, permissions, onChanged }: {
  order: OrderResponse; permissions: string[]; onChanged: (order?: OrderResponse) => void
}) {
  const [action, setAction] = useState<'CANCEL' | 'VOID' | 'REQUEST_CANCEL' | null>(null)
  const [reason, setReason] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const hospitality = ['DINE_IN', 'TAKEAWAY', 'DELIVERY'].includes(order.serviceType || '')
  const eligible = (hospitality || order.serviceType === 'RETAIL') &&
    !['CANCELLED', 'REFUNDED'].includes(order.paymentStatus) && order.deliveryStatus !== 'CANCELLED'
  if (!eligible) return null
  const canCancel = permissions.includes('orders.cancel') && order.paymentStatus !== 'PAID'
  const canVoid = permissions.includes('orders.refund')
  const canRequest = hospitality && permissions.includes('hospitality.view') &&
    order.tabStatus === 'OPEN' && order.paymentStatus === 'PENDING' && !canCancel
  const open = (value: NonNullable<typeof action>) => { setAction(value); setReason(''); setError('') }
  const submit = async () => {
    setBusy(true); setError('')
    try {
      const result = action === 'REQUEST_CANCEL'
        ? await hospitalityOpsApi.approval({ actionType: 'CANCEL_ORDER', entityType: 'ORDER', entityId: order.id, reason: reason.trim() })
        : action === 'VOID' ? await orderApi.void(order.id) : await orderApi.cancel(order.id)
      if (!result.success) throw new Error(result.message || 'Could not update the order')
      onChanged(action === 'REQUEST_CANCEL' ? undefined : result.data || undefined)
      setAction(null)
    } catch (e: any) { setError(e.response?.data?.message || e.message || 'Could not update the order') }
    finally { setBusy(false) }
  }
  return <>
    {canCancel && <Btn small variant="danger" onClick={() => open('CANCEL')}>Cancel order</Btn>}
    {canVoid && <Btn small variant="danger" onClick={() => open('VOID')}>Void order</Btn>}
    {canRequest && <Btn small variant="secondary" onClick={() => open('REQUEST_CANCEL')}>Request cancellation</Btn>}
    {action && createPortal(<Modal title={`${action === 'VOID' ? 'Void' : action === 'REQUEST_CANCEL' ? 'Request cancellation of' : 'Cancel'} ${order.orderNumber}`}
      onClose={() => { if (!busy) setAction(null) }} footer={<>
        <Btn variant="secondary" disabled={busy} onClick={() => setAction(null)}>Keep order</Btn>
        <Btn variant="danger" disabled={busy || (action === 'REQUEST_CANCEL' && !reason.trim())} onClick={submit}>
          {busy ? 'Saving…' : action === 'REQUEST_CANCEL' ? 'Send cancellation request' : action === 'VOID' ? 'Confirm void' : 'Confirm cancellation'}
        </Btn></>}>
      <p>{action === 'REQUEST_CANCEL' ? 'The order stays open until another authorized user approves and applies the cancellation.' :
        action === 'VOID' ? 'Void this order and restore its product stock. This records an order reversal; any money owed to the customer must be refunded separately.' :
        'Cancel this unpaid order and restore its product stock.'}</p>
      {hospitality && action !== 'REQUEST_CANCEL' && <p>The tab will close and its preparation tickets will be cancelled.</p>}
      {action === 'REQUEST_CANCEL' && <Input label="Cancellation reason" value={reason} onChange={setReason} />}
      {error && <p role="alert" style={{ color: 'var(--b360-red)' }}>{error}</p>}
    </Modal>, document.body)}
  </>
}

import React, { useMemo, useState, useEffect } from 'react'
import { Btn, Input, Modal, Select } from '../ui'
import { BusinessProfileResponse, OrderResponse, businessApi, hospitalityApi, hospitalityOpsApi, orderApi, paymentApi } from '../../services/api'
import { printOrderReceipt } from '../../utils/receipt'

type SplitLine = { method: string; amount: string; phone: string }

export function SettlementModal({ order, onClose, onComplete }: { order: OrderResponse; onClose: () => void; onComplete: () => Promise<void> | void }) {
  const [mode, setMode] = useState<'single'|'split'>('single')
  const [method, setMethod] = useState('CASH')
  const [phone, setPhone] = useState(order.customerPhone || '')
  const [profile, setProfile] = useState<BusinessProfileResponse | null>(null)
  const half = (order.subtotal / 2).toFixed(2)
  const [lines, setLines] = useState<SplitLine[]>([{method:'CASH',amount:half,phone:''},{method:'CASH',amount:(order.subtotal-Number(half)).toFixed(2),phone:''}])
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    businessApi.getProfile().then(res => { if (res.success && res.data) setProfile(res.data) }).catch(() => undefined)
  }, [])
  const splitTotal = useMemo(() => lines.reduce((sum,line)=>sum+(Number(line.amount)||0),0),[lines])
  const methods = [{value:'CASH',label:'Cash'},{value:'MPESA',label:'M-Pesa'},{value:'CARD',label:'Card'}]
  const splitMethods = methods.filter(item=>item.value==='CASH')
  const waitForMpesa = async () => {
    for (let attempt = 0; attempt < 30; attempt += 1) {
      await new Promise(resolve => window.setTimeout(resolve, 2000))
      const latest = await orderApi.get(order.id)
      if (latest.success && latest.data?.paymentStatus === 'PAID') return latest.data
    }
    return null
  }
  const submit = async () => {
    setError('')
    setSaving(true)
    try {
      let settledOrder: OrderResponse | null = mode === 'split'
        ? { ...order, paymentStatus: 'PAID', paymentMethod: 'SPLIT' }
        : method === 'MPESA' ? null : { ...order, paymentStatus: 'PAID', paymentMethod: method }
      if (mode === 'split') {
        if (lines.length < 2 || lines.some(line => !(Number(line.amount)>0))) throw new Error('Enter at least two valid payment amounts.')
        if (Math.abs(splitTotal-order.subtotal)>0.01) throw new Error(`Split total must equal KES ${order.subtotal.toLocaleString()}.`)
        if (lines.some(line=>line.method==='MPESA'&&!line.phone.trim())) throw new Error('Enter a phone number for every M-Pesa payment.')
        const result = await hospitalityOpsApi.splitBill(order.id,lines.map(line=>({method:line.method,amount:Number(line.amount),phone:line.phone.trim()||null})))
        if (result.success === false) throw new Error(result.message || 'Could not split this bill.')
      } else {
        if (method==='MPESA'&&!phone.trim()) throw new Error('Enter the customer M-Pesa phone number.')
        const result=await hospitalityApi.closeTab(order.id,method)
        if(!result.success) throw new Error(result.message||'Could not settle this tab.')
        if(method==='MPESA') {
          const push=await paymentApi.initiate({orderId:order.id,phoneNumber:phone.trim()})
          if(!push.success) throw new Error(push.message||'Could not send the M-Pesa prompt.')
          const confirmed = await waitForMpesa()
          if (confirmed) settledOrder = confirmed
          else throw new Error('M-Pesa prompt sent. Payment confirmation is still pending; this tab will close automatically when it arrives.')
        }
        if(method==='CARD') {
          window.location.assign(`/pay/card?orderId=${encodeURIComponent(order.id)}&businessId=${encodeURIComponent(order.businessId)}`)
          return
        }
      }
      try {
        if (settledOrder?.paymentStatus === 'PAID') printOrderReceipt(settledOrder, profile, false)
      } catch (err) {
        console.warn('Auto print receipt error:', err)
      }
      await onComplete()
      onClose()
    } catch (e:any) { setError(e.response?.data?.message||e.message||'Payment could not be processed.') }
    finally { setSaving(false) }
  }
  const update=(index:number,patch:Partial<SplitLine>)=>setLines(current=>current.map((line,i)=>i===index?{...line,...patch}:line))
  return <Modal title={`Settle ${order.orderNumber}`} onClose={onClose} footer={<><Btn variant="secondary" onClick={onClose}>Cancel</Btn><Btn disabled={saving} onClick={submit}>{saving?'Processing…':mode==='split'?'Confirm split':method==='MPESA'?'Send M-Pesa prompt':method==='CARD'?'Continue to card payment':`Confirm ${method}`}</Btn></>}>
    <div style={{display:'grid',gap:14}}>
      <div style={{padding:16,background:'var(--b360-bg)',borderRadius:10}}><div style={{fontSize:12,color:'var(--b360-text-secondary)'}}>Amount due</div><div style={{fontSize:25,fontWeight:800}}>KES {order.subtotal.toLocaleString()}</div></div>
      {error&&<div role="alert" style={{padding:10,borderRadius:8,background:'var(--b360-red-bg)',color:'var(--b360-red)',fontSize:12}}>{error}</div>}
      <div style={{display:'grid',gridTemplateColumns:'1fr 1fr',gap:8}}><Btn variant={mode==='single'?'primary':'secondary'} onClick={()=>setMode('single')}>Single payment</Btn><Btn variant={mode==='split'?'primary':'secondary'} onClick={()=>setMode('split')}>Split bill</Btn></div>
      {mode==='single'?<><Select label="Payment method" value={method} onChange={setMethod} options={methods}/>{method==='MPESA'&&<Input label="M-Pesa phone" value={phone} onChange={setPhone} placeholder="07… or 254…"/>}<p style={{fontSize:12,color:'var(--b360-text-secondary)',margin:0}}>{method==='MPESA'?'The tab remains open until Safaricom confirms payment.':method==='CARD'?'You will continue to the secure hosted card checkout.':'Cash closes the receipt immediately.'}</p></>:<>
        {lines.map((line,index)=><div key={index} style={{display:'grid',gap:8,padding:10,border:'1px solid var(--b360-border)',borderRadius:9}}><Select label={`Payment ${index+1}`} value={line.method} onChange={value=>update(index,{method:value})} options={splitMethods}/><Input label="Amount (KES)" type="number" value={line.amount} onChange={value=>update(index,{amount:value})}/>{line.method==='MPESA'&&<Input label="M-Pesa phone" value={line.phone} onChange={value=>update(index,{phone:value})}/>}</div>)}
        <div style={{display:'flex',gap:8}}><Btn variant="secondary" onClick={()=>setLines(current=>[...current,{method:'CASH',amount:'',phone:''}])}>Add payment</Btn>{lines.length>2&&<Btn variant="danger" onClick={()=>setLines(current=>current.slice(0,-1))}>Remove last</Btn>}</div>
        <div style={{fontSize:12,fontWeight:700,color:Math.abs(splitTotal-order.subtotal)<=.01?'var(--b360-green)':'var(--b360-red)'}}>Allocated KES {splitTotal.toLocaleString()} of KES {order.subtotal.toLocaleString()}</div><small style={{color:'var(--b360-text-secondary)'}}>Split cash collection is available here. Use separate receipts for mixed M-Pesa or card payments so each gateway payment remains independently traceable.</small>
      </>}
    </div>
  </Modal>
}

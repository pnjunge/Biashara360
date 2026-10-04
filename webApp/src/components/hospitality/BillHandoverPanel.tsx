import { StaffShiftTally } from './StaffShiftTally'
import React, { useEffect, useState } from 'react'
import { useAuth } from '../../App'
import { Btn, Card, Input, Select, StatusBadge, Modal } from '../ui'
import { BillHandover, HospitalityDuty, OrderResponse, hospitalityDutyApi } from '../../services/api'

export default function BillHandoverPanel({orders,permissions,onChanged}:{orders:OrderResponse[];permissions:string[];onChanged:()=>void}) {
  const {user}=useAuth()
  const [data,setData]=useState<HospitalityDuty|null>(null),[error,setError]=useState(''),[message,setMessage]=useState(''),[busy,setBusy]=useState(false)
  const [ending,setEnding]=useState(false)
  const [selected,setSelected]=useState<string[]>([]),[recipient,setRecipient]=useState(''),[notes,setNotes]=useState('')
  const canWork=permissions.includes('hospitality.orders')||permissions.includes('hospitality.billing')
  const manage=permissions.includes('hospitality.shifts')
  useEffect(()=>{
    if(!permissions.includes('hospitality.view')||!canWork) return
    let active=true;let timer:ReturnType<typeof setTimeout>
    const poll=async()=>{try{const r=await hospitalityDutyApi.dashboard();if(active){if(r.success&&r.data)setData(r.data);else setError(r.message||'Could not load staff shifts')}}catch(e:any){if(active)setError(e.response?.data?.message||'Could not load bill handovers')}if(active)timer=setTimeout(poll,5000)}
    void poll();return()=>{active=false;clearTimeout(timer)}
  },[user?.id,canWork,permissions.includes('hospitality.view')])
  const act=async(fn:()=>Promise<any>,success:string)=>{
    if(busy)return;setBusy(true);setError('');setMessage('')
    try{const r=await fn();if(!r.success)throw new Error(r.message||'Request failed');const latest=await hospitalityDutyApi.dashboard();if(latest.success&&latest.data)setData(latest.data);setMessage(success);onChanged()}
    catch(e:any){setError(e.response?.data?.message||e.message||'Request failed')}finally{setBusy(false)}
  }
  if(!canWork||!permissions.includes('hospitality.view'))return null
  const owned=orders.filter(o=>manage||(o.responsibleUserId||o.serverUserId)===user?.id)
  const pending=data?.handovers.filter(h=>h.status==='PENDING')||[]
  const candidates=(data?.staff||[]).filter(s=>s.id!==user?.id&&s.onDuty)
  const selectedBills=selected.filter(id=>owned.some(o=>o.id===id)&&!pending.some(h=>h.orderId===id))
  const decision=(h:BillHandover,action:'ACCEPT'|'REJECT'|'CANCEL')=>act(()=>hospitalityDutyApi.decide(h.id,action),action==='ACCEPT'?`${h.orderNumber} handed over to you.`:action==='REJECT'?'Handover rejected.':'Handover cancelled.')
  return <Card style={{padding:18}}><h3>My staff shift & bill handovers</h3>
    <p style={{fontSize:12}}>End your own shift after settling your bills or handing them to another on-duty waiter or cashier. The business trading day stays open.</p>
    {error&&<p role="alert" style={{color:'var(--b360-red)'}}>{error}</p>}{message&&<p role="status">{message}</p>}
    {!data?<p>Loading staff shifts…</p>:<>
      <div style={{display:'flex',gap:10,alignItems:'center',flexWrap:'wrap'}}><StatusBadge status={data.shift?'OPEN':'CLOSED'}/>{data.shift?<><span>Started {new Date(data.shift.openedAt).toLocaleTimeString('en-KE',{timeZone:'Africa/Nairobi'})}</span><Btn variant="secondary" disabled={busy||data.ownedOpenOrderIds.length>0} onClick={()=>setEnding(true)}>End my shift</Btn>{data.ownedOpenOrderIds.length>0&&<span style={{fontSize:12}}>{data.ownedOpenOrderIds.length} bill(s) still your responsibility</span>}</>:<Btn disabled={busy} onClick={()=>act(()=>hospitalityDutyApi.start(),'Your staff shift has started.')}>Start my shift</Btn>}</div>
      {orders.some(o=>!o.responsibleUserId&&!o.serverUserId)&&<div style={{marginTop:12}}><b>Unassigned bills</b>{orders.filter(o=>!o.responsibleUserId&&!o.serverUserId).map(o=><div key={o.id} style={{display:'flex',gap:10,marginTop:6,alignItems:'center'}}>{o.orderNumber}<Btn small disabled={busy||!data.shift} onClick={()=>act(()=>hospitalityDutyApi.claim(o.id),`${o.orderNumber} assigned to you.`)}>Take responsibility</Btn></div>)}</div>}
      {!!owned.length&&<div style={{marginTop:16}}><b>Hand over open bills</b><div style={{display:'flex',gap:14,flexWrap:'wrap',margin:'10px 0'}}>{owned.map(o=>{const waiting=pending.some(h=>h.orderId===o.id);return <label key={o.id}><input type="checkbox" aria-label={`Hand over ${o.orderNumber}`} disabled={busy||waiting} checked={selected.includes(o.id)&&!waiting} onChange={e=>setSelected(current=>e.target.checked?[...current,o.id]:current.filter(id=>id!==o.id))}/> {o.orderNumber} · {o.responsibleUserName||'Unassigned'} {waiting?'(awaiting acceptance)':''}</label>})}</div>
        <div className="responsive-grid responsive-grid-2"><Select label="Incoming waiter / cashier" value={recipient} onChange={setRecipient} options={[{value:'',label:'Select on-duty staff'},...candidates.map(s=>({value:s.id,label:s.name}))]}/><Input label="Handover notes" value={notes} onChange={setNotes}/></div>
        {!candidates.length&&<p style={{fontSize:12}}>The incoming staff member must start their staff shift before you can select them.</p>}
        <Btn disabled={busy||!selectedBills.length||!candidates.some(s=>s.id===recipient)} onClick={()=>act(async()=>{const r=await hospitalityDutyApi.handover(selectedBills,recipient,notes);if(r.success)setSelected([]);return r},'Handover requested. Responsibility stays with the sender until accepted.')}>Request bill handover</Btn>
      </div>}
      {!!pending.length&&<div style={{marginTop:18}}><b>Pending handovers</b>{pending.map(h=><div key={h.id} style={{padding:'10px 0',borderBottom:'1px solid var(--b360-border)'}}><strong>{h.orderNumber}</strong> · {h.fromUserName} → {h.toUserName} · Outstanding at handover: KES {h.balanceAtRequest.toLocaleString('en-KE')}{h.notes&&<p>{h.notes}</p>}<div style={{display:'flex',gap:8,marginTop:6}}>{h.toUserId===user?.id&&<><Btn small disabled={busy||!data.shift} onClick={()=>decision(h,'ACCEPT')}>Accept bill</Btn><Btn small variant="secondary" disabled={busy} onClick={()=>decision(h,'REJECT')}>Reject handover</Btn></>}{(h.fromUserId===user?.id||h.requestedBy===user?.id)&&<Btn small variant="secondary" disabled={busy} onClick={()=>decision(h,'CANCEL')}>Cancel handover</Btn>}</div></div>)}</div>}
      {data.shift&&<details style={{marginTop:14}}><summary>Current shift tally</summary><StaffShiftTally shift={data.shift}/></details>}
      {!!data.recentShifts?.length&&<details style={{marginTop:14}}><summary>Completed shift tallies</summary>{data.recentShifts.map(shift=><details key={shift.id} style={{marginTop:10}}><summary>{new Date(shift.openedAt).toLocaleString('en-KE',{timeZone:'Africa/Nairobi'})} · {shift.summary.completedOrderCount} completed · KES {shift.summary.collectedTotal.toLocaleString('en-KE')} collected</summary><StaffShiftTally shift={shift}/></details>)}</details>}
      {ending&&data.shift&&<Modal title="End my staff shift · review tally" onClose={()=>setEnding(false)} footer={<><Btn variant="secondary" onClick={()=>setEnding(false)}>Keep shift open</Btn><Btn disabled={busy||data.ownedOpenOrderIds.length>0} onClick={()=>act(async()=>{const r=await hospitalityDutyApi.end(notes);if(r.success)setEnding(false);return r},'Your staff shift has ended and its tally has been saved.')}>Confirm end shift</Btn></>}><StaffShiftTally shift={data.shift}/><Input label="Shift closing notes" value={notes} onChange={setNotes}/><p>Open bills handed over and accepted are excluded from completed orders. Your tally is saved when you confirm; the business trading day remains open.</p></Modal>}
      {!!data.handovers.length&&<details style={{marginTop:14}}><summary>Handover history</summary>{data.handovers.filter(h=>h.status!=='PENDING').map(h=><div key={h.id} style={{padding:'7px 0'}}>{h.orderNumber} · {h.fromUserName} → {h.toUserName} · {h.status} · {new Date(h.decidedAt||h.requestedAt).toLocaleString('en-KE',{timeZone:'Africa/Nairobi'})}</div>)}</details>}
    </>}
  </Card>
}

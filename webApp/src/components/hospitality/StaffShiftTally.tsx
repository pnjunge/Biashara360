import React, { useEffect, useState } from 'react'
import { Btn, Card, DataTable } from '../ui'
import { StaffShift, hospitalityDutyApi } from '../../services/api'

export function StaffShiftTally({shift}:{shift:StaffShift}) {
  const s=shift.summary
  const download=()=>{
    const rows=[['Staff',shift.userName],['Started',new Date(shift.openedAt).toLocaleString('en-KE',{timeZone:'Africa/Nairobi'})],['Ended',shift.closedAt?new Date(shift.closedAt).toLocaleString('en-KE',{timeZone:'Africa/Nairobi'}):'Open'],['Completed orders',s.completedOrderCount],['Completed order value',s.completedOrderTotal],['Cash collected',s.cashTotal],['M-Pesa collected',s.mpesaTotal],['Card collected',s.cardTotal],['Other collected',s.otherTotal],['Total collected',s.collectedTotal],['Handed over',s.handedOverCount],['Received',s.receivedBillCount],[],['Order','Value','Completed','Involvement'],...s.completedOrders.map(o=>[o.orderNumber,o.amount,new Date(o.completedAt).toLocaleString('en-KE',{timeZone:'Africa/Nairobi'}),o.involvement])]
    const escape=(value:any)=>{const text=String(value??'');return '"'+(/^[=+\-@]/.test(text)?"'"+text:text).replace(/"/g,'""')+'"'}
    const url=URL.createObjectURL(new Blob(['\uFEFF'+rows.map(row=>row.map(escape).join(',')).join('\r\n')],{type:'text/csv;charset=utf-8'}));const link=document.createElement('a');link.href=url;link.download=`staff-shift-${shift.id}.csv`;link.click();setTimeout(()=>URL.revokeObjectURL(url),1000)
  }
  return <div style={{display:'grid',gap:10}}><strong>{shift.userName} · {shift.status}</strong><span style={{fontSize:12}}>{new Date(shift.openedAt).toLocaleString('en-KE',{timeZone:'Africa/Nairobi'})} → {shift.closedAt?new Date(shift.closedAt).toLocaleString('en-KE',{timeZone:'Africa/Nairobi'}):'Now'}</span>
    <div className="responsive-grid responsive-grid-3">{[['Completed orders',s.completedOrderCount],['Completed order value',`KES ${s.completedOrderTotal.toLocaleString('en-KE')}`],['Payments collected',`KES ${s.collectedTotal.toLocaleString('en-KE')}`],['Cash',`KES ${s.cashTotal.toLocaleString('en-KE')}`],['M-Pesa',`KES ${s.mpesaTotal.toLocaleString('en-KE')}`],['Card',`KES ${s.cardTotal.toLocaleString('en-KE')}`]].map(([label,value])=><div key={label} style={{padding:10,background:'var(--b360-bg)',borderRadius:6}}><small>{label}</small><strong style={{display:'block'}}>{value}</strong></div>)}</div>
    <p style={{fontSize:12,margin:0}}>Served: {s.servedOrderCount} · Settled: {s.settledOrderCount} · Handed over: {s.handedOverCount} · Received: {s.receivedBillCount}. Completed orders include bills you served, held or settled during this shift. Payment totals count only money collected by you.</p>
    {s.otherTotal>0&&<p>Other payment methods: KES {s.otherTotal.toLocaleString('en-KE')}</p>}
    {!!s.completedOrders.length&&<DataTable headers={['Completed order','Value','Your involvement']} rows={s.completedOrders.map(o=>[o.orderNumber,`KES ${o.amount.toLocaleString('en-KE')}`,o.involvement])}/>}
    <Btn small variant="secondary" onClick={download}>Download shift tally</Btn>
  </div>
}

export function StaffShiftReports() {
  const [shifts,setShifts]=useState<StaffShift[]>([]),[error,setError]=useState('')
  useEffect(()=>{let active=true;hospitalityDutyApi.reports().then(r=>{if(active){if(r.success&&r.data)setShifts(r.data);else setError(r.message||'Could not load staff shift tallies')}}).catch(()=>{if(active)setError('Could not load staff shift tallies')});return()=>{active=false}},[])
  return <Card style={{padding:18}}><h3>Staff shift tallies</h3>{error&&<p role="alert">{error}</p>}{!shifts.length&&!error&&<p>No personal staff shifts recorded yet.</p>}{shifts.map(shift=><details key={shift.id} style={{marginTop:10}}><summary>{shift.userName} · {new Date(shift.openedAt).toLocaleString('en-KE',{timeZone:'Africa/Nairobi'})} · {shift.summary.completedOrderCount} completed · KES {shift.summary.collectedTotal.toLocaleString('en-KE')} collected · {shift.status}</summary><StaffShiftTally shift={shift}/></details>)}</Card>
}

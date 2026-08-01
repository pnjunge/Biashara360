import React, { useEffect, useMemo, useState } from 'react'
import { ChefHat, Plus, RefreshCw, Users, UtensilsCrossed } from 'lucide-react'
import { Btn, Card, Input, KpiCard, Modal, PageHeader, Select, StatusBadge } from '../components/ui'
import { HospitalityDashboard, HospitalityTable, ProductResponse, hospitalityApi, paymentApi, productApi } from '../services/api'
import { useAuth } from '../App'

type Cart = Record<string, number>

export default function HospitalityPage() {
  const { user } = useAuth()
  const isAdmin = user?.role === 'ADMIN'
  const [data,setData]=useState<HospitalityDashboard|null>(null)
  const [products,setProducts]=useState<ProductResponse[]>([])
  const [loading,setLoading]=useState(true)
  const [error,setError]=useState('')
  const [showTable,setShowTable]=useState(false)
  const [tableDraft,setTableDraft]=useState({name:'',area:'Main Floor',capacity:'4'})
  const [orderTable,setOrderTable]=useState<HospitalityTable|null>(null)
  const [serviceType,setServiceType]=useState('DINE_IN')
  const [guests,setGuests]=useState('2')
  const [customerName,setCustomerName]=useState('Walk-in Guest')
  const [customerPhone,setCustomerPhone]=useState('')
  const [notes,setNotes]=useState('')
  const [cart,setCart]=useState<Cart>({})
  const [saving,setSaving]=useState(false)

  const load=async()=>{setLoading(true);try{const [dashboard,catalog]=await Promise.all([hospitalityApi.dashboard(),productApi.list()]);if(dashboard.success&&dashboard.data)setData(dashboard.data);if(catalog.success&&catalog.data)setProducts(catalog.data.filter(p=>p.currentStock>0))}catch(e:any){setError(e.response?.data?.message||'Could not load hospitality operations.')}finally{setLoading(false)}}
  useEffect(()=>{load()},[])
  const cartProducts=useMemo(()=>products.filter(p=>cart[p.id]),[products,cart])
  const total=cartProducts.reduce((sum,p)=>sum+p.sellingPrice*cart[p.id],0)
  const change=(id:string,delta:number)=>setCart(current=>{const next=Math.max(0,(current[id]||0)+delta);const copy={...current};if(next)copy[id]=next;else delete copy[id];return copy})
  const openOrder=(table?:HospitalityTable)=>{setOrderTable(table||null);setServiceType(table?'DINE_IN':'TAKEAWAY');setCart({});setNotes('')}
  const createTable=async()=>{setSaving(true);try{const res=await hospitalityApi.createTable({name:tableDraft.name,area:tableDraft.area,capacity:Number(tableDraft.capacity)});if(!res.success)throw new Error(res.message);setShowTable(false);setTableDraft({name:'',area:'Main Floor',capacity:'4'});load()}catch(e:any){setError(e.response?.data?.message||e.message)}finally{setSaving(false)}}
  const submitOrder=async()=>{if(!cartProducts.length)return setError('Add at least one menu item.');setSaving(true);setError('');try{const res=await hospitalityApi.createOrder({tableId:orderTable?.id,serviceType,guestCount:Number(guests),customerName,customerPhone,notes,items:cartProducts.map(p=>({productId:p.id,quantity:cart[p.id],unitPrice:p.sellingPrice}))});if(!res.success)throw new Error(res.message);setOrderTable(null);setCart({});await load()}catch(e:any){setError(e.response?.data?.message||e.message)}finally{setSaving(false)}}
  const advanceTicket=async(id:string,status:string)=>{await hospitalityApi.updateTicket(id,status);load()}
  const closeTab=async(orderId:string)=>{const method=(window.prompt('Payment method: CASH, CARD, or MPESA','CASH')||'').toUpperCase();if(!['CASH','CARD','MPESA'].includes(method))return;try{const result=await hospitalityApi.closeTab(orderId,method);if(!result.success)throw new Error(result.message);if(method==='MPESA'){const phone=window.prompt('Customer M-Pesa phone number')||'';if(phone){const push=await paymentApi.initiate({orderId,phoneNumber:phone});if(!push.success)throw new Error(push.message||'Could not send M-Pesa prompt')}}load()}catch(e:any){setError(e.response?.data?.message||e.message)}}

  if(loading)return <div style={{padding:30}}>Loading bar and restaurant operations…</div>
  if(!data)return <div style={{color:'var(--b360-red)'}}>{error||'Hospitality is unavailable.'}</div>
  if(!data.enabled)return <Card style={{padding:32,maxWidth:620}}><PageHeader title="Bar & Restaurant"/><p style={{color:'var(--b360-text-secondary)',marginBottom:18}}>Enable hospitality mode to manage tables, tabs, kitchen tickets, and bar orders.</p>{isAdmin?<Btn onClick={async()=>{await hospitalityApi.setEnabled(true);load()}}>Enable hospitality mode</Btn>:<p>An administrator must enable hospitality mode.</p>}</Card>

  return <div className="fade-in" style={{display:'flex',flexDirection:'column',gap:18}}>
    <PageHeader title="Bar & Restaurant" action={<div style={{display:'flex',gap:8,flexWrap:'wrap'}}><Btn variant="secondary" icon={<RefreshCw size={14}/>} onClick={load}>Refresh</Btn>{isAdmin&&<Btn icon={<Plus size={14}/>} onClick={()=>setShowTable(true)}>Add table</Btn>}<Btn icon={<UtensilsCrossed size={14}/>} onClick={()=>openOrder()}>Takeaway order</Btn></div>}/>
    {error&&<div style={{padding:10,background:'var(--b360-red-bg)',color:'var(--b360-red)',borderRadius:8}}>{error}</div>}
    <div className="responsive-grid responsive-grid-3"><KpiCard title="Tables" value={String(data.tables.length)} change={`${data.tables.filter(t=>t.status==='OCCUPIED').length} occupied`} icon={<Users size={18}/>} color="var(--b360-blue)"/><KpiCard title="Open tabs" value={String(data.openTabs.length)} change={`KES ${data.openTabs.reduce((s,o)=>s+o.subtotal,0).toLocaleString()}`} icon={<UtensilsCrossed size={18}/>} color="var(--b360-amber)"/><KpiCard title="Active tickets" value={String(data.tickets.filter(t=>!['SERVED','CANCELLED'].includes(t.status)).length)} change="Kitchen and bar" icon={<ChefHat size={18}/>} color="var(--b360-green)"/></div>

    <h2 style={{fontSize:17}}>Floor & tables</h2><div style={{display:'grid',gridTemplateColumns:'repeat(auto-fill,minmax(170px,1fr))',gap:12}}>{data.tables.map(table=><Card key={table.id} style={{padding:16,borderTop:`4px solid ${table.status==='OCCUPIED'?'var(--b360-amber)':'var(--b360-green)'}`}}><div style={{display:'flex',justifyContent:'space-between'}}><b>{table.name}</b><StatusBadge status={table.status}/></div><div style={{fontSize:12,color:'var(--b360-text-secondary)',margin:'7px 0'}}>{table.area} · {table.capacity} seats</div>{table.openOrderId?<><strong>KES {table.openAmount.toLocaleString()}</strong><Btn small onClick={()=>closeTab(table.openOrderId!)}>Settle tab</Btn></>:<Btn small variant="secondary" onClick={()=>openOrder(table)}>Open table</Btn>}</Card>)}</div>

    <h2 style={{fontSize:17}}>Kitchen & bar tickets</h2><div className="responsive-grid responsive-grid-3">{data.tickets.filter(t=>!['SERVED','CANCELLED'].includes(t.status)).map(ticket=><Card key={ticket.id} style={{padding:16}}><div style={{display:'flex',justifyContent:'space-between',gap:8}}><b>{ticket.station} · {ticket.tableName||'Takeaway'}</b><StatusBadge status={ticket.status}/></div><div style={{fontSize:11,color:'var(--b360-text-secondary)',margin:'4px 0 10px'}}>{ticket.orderNumber}</div>{ticket.items.map(item=><div key={item.id} style={{fontSize:13,marginBottom:4}}><b>{item.quantity}×</b> {item.productName}</div>)}{ticket.notes&&<div style={{fontSize:12,marginTop:8,color:'var(--b360-amber)'}}>{ticket.notes}</div>}<div style={{display:'flex',gap:6,marginTop:12}}>{ticket.status==='NEW'&&<Btn small onClick={()=>advanceTicket(ticket.id,'PREPARING')}>Start</Btn>}{ticket.status==='PREPARING'&&<Btn small onClick={()=>advanceTicket(ticket.id,'READY')}>Ready</Btn>}{ticket.status==='READY'&&<Btn small onClick={()=>advanceTicket(ticket.id,'SERVED')}>Served</Btn>}</div></Card>)}</div>

    {showTable&&<Modal title="Add restaurant table" onClose={()=>setShowTable(false)} footer={<><Btn variant="secondary" onClick={()=>setShowTable(false)}>Cancel</Btn><Btn disabled={saving} onClick={createTable}>Save table</Btn></>}><div style={{display:'flex',flexDirection:'column',gap:12}}><Input label="Table name" value={tableDraft.name} onChange={v=>setTableDraft({...tableDraft,name:v})} placeholder="e.g. Terrace 4"/><Input label="Area" value={tableDraft.area} onChange={v=>setTableDraft({...tableDraft,area:v})}/><Input label="Seats" type="number" value={tableDraft.capacity} onChange={v=>setTableDraft({...tableDraft,capacity:v})}/></div></Modal>}
    {(orderTable||serviceType==='TAKEAWAY')&&<Modal wide title={orderTable?`Open ${orderTable.name}`:'New takeaway order'} onClose={()=>{setOrderTable(null);setServiceType('')}} footer={<><Btn variant="secondary" onClick={()=>{setOrderTable(null);setServiceType('')}}>Cancel</Btn><Btn disabled={saving||!cartProducts.length} onClick={submitOrder}>Open tab · KES {total.toLocaleString()}</Btn></>}><div className="responsive-grid responsive-grid-2"><div><Select label="Service" value={serviceType} onChange={setServiceType} options={[{value:'DINE_IN',label:'Dine in'},{value:'TAKEAWAY',label:'Takeaway'},{value:'DELIVERY',label:'Delivery'}]}/><Input label="Guests" type="number" value={guests} onChange={setGuests}/><Input label="Customer" value={customerName} onChange={setCustomerName}/><Input label="Phone (optional)" value={customerPhone} onChange={setCustomerPhone}/><Input label="Kitchen/bar notes" value={notes} onChange={setNotes}/></div><div style={{maxHeight:430,overflowY:'auto'}}>{products.map(product=><div key={product.id} style={{display:'flex',justifyContent:'space-between',alignItems:'center',gap:8,padding:'8px 0',borderBottom:'1px solid var(--b360-border)'}}><div><b style={{fontSize:12}}>{product.name}</b><div style={{fontSize:11,color:'var(--b360-text-secondary)'}}>KES {product.sellingPrice.toLocaleString()}</div></div><div style={{display:'flex',alignItems:'center',gap:7}}><button onClick={()=>change(product.id,-1)}>−</button><b>{cart[product.id]||0}</b><button onClick={()=>change(product.id,1)}>+</button></div></div>)}</div></div></Modal>}
  </div>
}

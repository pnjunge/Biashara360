import React, { useEffect, useState } from 'react'
import { Btn, Card, DataTable, Input, Select, StatusBadge, Modal } from '../ui'
import { HospitalityOperations, hospitalityOpsApi } from '../../services/api'
import { purchaseFactor, purchaseUnits } from '../../utils/purchaseUnits'

type Props = {data:HospitalityOperations;act:(fn:()=>Promise<any>)=>void;canPay?:boolean}
type Line = {ingredientId:string;quantity:number;unitCost:number;purchaseUnit:string}
const emptyLine=():Line=>({ingredientId:'',quantity:1,unitCost:0,purchaseUnit:''})

export function IngredientPurchaseUnits({data,act}:Props) {
  const [id,setId]=useState(''),[unit,setUnit]=useState('BOTTLE'),[size,setSize]=useState('750')
  const ingredient=data.ingredients.find(i=>i.id===id)
  useEffect(()=>{setUnit(ingredient?.purchaseUnit||'BOTTLE');setSize(String(ingredient?.purchaseUnitSize||750))},[id])
  return <Card style={{padding:18}}><h3>Purchase pack sizes</h3><p>Stock stays in grams, millilitres or your chosen base unit. Kilograms and litres convert automatically. Define bottles, packs or cases here.</p>
    <div className="responsive-grid responsive-grid-3"><Select label="Pack ingredient" value={id} onChange={setId} options={[{value:'',label:'Select ingredient'},...data.ingredients.map(i=>({value:i.id,label:`${i.name} (${i.unit})`}))]}/>
    <Select label="Pack unit" value={unit} onChange={setUnit} options={['BOTTLE','PACK','CASE'].map(value=>({value,label:value}))}/>
    <Input label={`Contents per ${unit.toLowerCase()} (${ingredient?.unit||'base units'})`} type="number" value={size} onChange={setSize}/></div>
    <Btn disabled={!id||!Number.isFinite(Number(size))||Number(size)<=0} onClick={()=>act(()=>hospitalityOpsApi.purchaseUnit(id,{purchaseUnit:unit,purchaseUnitSize:Number(size)}))}>Save pack size</Btn>
  </Card>
}

export default function IngredientPurchasing({data,act,canPay=false}:Props) {
  const [paymentTarget,setPaymentTarget]=useState<{po:HospitalityOperations['purchaseOrders'][number];receive:boolean}|null>(null)
  const [supplier,setSupplier]=useState(''),[name,setName]=useState(''),[phone,setPhone]=useState(''),[lines,setLines]=useState<Line[]>([emptyLine()])
  const update=(index:number,patch:Partial<Line>)=>setLines(current=>current.map((line,i)=>i===index?{...line,...patch}:line))
  const valid=lines.every(line=>{const ingredient=data.ingredients.find(i=>i.id===line.ingredientId);return ingredient && purchaseFactor(ingredient,line.purchaseUnit||ingredient.unit)!==null && Number.isFinite(line.quantity)&&line.quantity>0&&Number.isFinite(line.unitCost)&&line.unitCost>=0}) && new Set(lines.map(l=>l.ingredientId)).size===lines.length
  return <>
    <Card style={{padding:18}}><h3>Ingredient suppliers</h3><Input label="Supplier name" value={name} onChange={setName}/><Input label="Supplier phone" value={phone} onChange={setPhone}/><Btn disabled={!name.trim()} onClick={()=>act(()=>hospitalityOpsApi.supplier({name,phone}))}>Add supplier</Btn></Card>
    <Card style={{padding:18}}><h3>New ingredient purchase order</h3><Select label="Purchase supplier" value={supplier} onChange={setSupplier} options={[{value:'',label:'Select supplier'},...data.suppliers.filter(s=>s.isActive).map(s=>({value:s.id,label:s.name}))]}/>
      {lines.map((line,index)=>{const ingredient=data.ingredients.find(i=>i.id===line.ingredientId),unit=line.purchaseUnit||ingredient?.unit||'',factor=ingredient?purchaseFactor(ingredient,unit):null;return <div key={index} style={{padding:'12px 0',borderBottom:'1px solid var(--b360-border)'}}>
        <div className="responsive-grid responsive-grid-4"><Select label="Purchase ingredient" value={line.ingredientId} onChange={ingredientId=>{const i=data.ingredients.find(i=>i.id===ingredientId);update(index,{ingredientId,purchaseUnit:i?.purchaseUnit||i?.unit||''})}} options={[{value:'',label:'Select ingredient'},...data.ingredients.map(i=>({value:i.id,label:`${i.name} (${i.unit})`}))]}/>
        <Select label="Purchase unit" value={unit} onChange={purchaseUnit=>update(index,{purchaseUnit})} options={ingredient?purchaseUnits(ingredient).map(value=>({value,label:value})):[{value:'',label:'Select ingredient first'}]}/>
        <Input label="Purchase quantity" type="number" value={String(line.quantity)} onChange={v=>update(index,{quantity:Number(v)})}/><Input label={`Cost per ${unit||'purchase unit'} (KES)`} type="number" value={String(line.unitCost)} onChange={v=>update(index,{unitCost:Number(v)})}/></div>
        {ingredient&&factor!==null&&<p style={{fontSize:12}}>Receive {line.quantity*factor} {ingredient.unit} · Cost per {ingredient.unit}: KES {(line.unitCost/factor).toLocaleString(undefined,{maximumFractionDigits:4})} · Total KES {(line.quantity*line.unitCost).toLocaleString()}</p>}
        <Btn small variant="secondary" disabled={lines.length===1} onClick={()=>setLines(lines.filter((_,i)=>i!==index))}>Remove</Btn>
      </div>})}
      <div style={{display:'flex',gap:8,marginTop:12}}><Btn variant="secondary" onClick={()=>setLines([...lines,emptyLine()])}>Add purchase line</Btn><Btn disabled={!supplier||!valid} onClick={()=>act(async()=>{const r=await hospitalityOpsApi.purchaseOrder({supplierId:supplier,items:lines});if(r.success)setLines([emptyLine()]);return r})}>Create purchase order</Btn></div>
    </Card>
    <p>Receiving goods records a linked stock-purchase entry in Expenses. Record supplier payments here; food costs are counted when portions are sold. Cash drawer payments also reduce the business day's expected cash.</p>
    <DataTable headers={['PO','Supplier','Goods / stock conversion','Total','Goods / payment','Action']} rows={data.purchaseOrders.map(po=>[po.orderNumber,data.suppliers.find(s=>s.id===po.supplierId)?.name||po.supplierId,<div>{(po.items||[]).map(line=><div key={line.ingredientId}>{line.ingredientName}: {line.purchaseQuantity} {line.purchaseUnit} → {line.stockQuantity} {line.stockUnit}</div>)}{po.expenseId&&<small>Linked stock-purchase expenditure recorded</small>}</div>,`KES ${po.totalCost.toLocaleString()}`,<div><StatusBadge status={po.status}/><div>{po.paymentStatus||'UNPAID'} · Paid KES {(po.paidAmount||0).toLocaleString()} · Balance KES {(po.outstandingAmount??po.totalCost).toLocaleString()}</div>{!!po.payments?.length&&<details><summary>Payment history</summary>{po.payments.map(p=><div key={p.id}>{new Date(p.paidAt).toLocaleString('en-KE',{timeZone:'Africa/Nairobi'})}: KES {p.amount.toLocaleString()} · {p.method} · {p.reference||'Cash'}{p.paidFromTill?' · Cash drawer':''}</div>)}</details>}</div>,po.status==='ORDERED'?<Btn small onClick={()=>setPaymentTarget({po,receive:true})}>Receive goods</Btn>:canPay&&po.status==='RECEIVED'&&(po.outstandingAmount??po.totalCost)>0?<Btn small onClick={()=>setPaymentTarget({po,receive:false})}>Record supplier payment</Btn>:'—'])}/>
    {paymentTarget&&<PurchasePaymentDialog target={paymentTarget} canPay={canPay} act={act} close={()=>setPaymentTarget(null)}/>}
  </>
}

function PurchasePaymentDialog({target,act,close,canPay}:{target:{po:HospitalityOperations['purchaseOrders'][number];receive:boolean};act:Props['act'];close:()=>void;canPay:boolean}) {
  const {po,receive}=target
  const [mode,setMode]=useState(receive?'UNPAID':'PAY'),[method,setMethod]=useState('CASH'),[amount,setAmount]=useState(String(po.outstandingAmount??po.totalCost)),[reference,setReference]=useState(''),[fromTill,setFromTill]=useState(false),[confirmed,setConfirmed]=useState(false),[busy,setBusy]=useState(false)
  const [key]=useState(()=>crypto.randomUUID())
  const balance=po.outstandingAmount??po.totalCost,valid=Number.isFinite(Number(amount))&&Number(amount)>0&&Number(amount)<=balance&&(method==='CASH'||reference.trim())&&(po.paymentStatus!=='UNRECORDED'||confirmed)
  const save=()=>{if(busy)return;setBusy(true);act(async()=>{try{const payment={amount:Number(amount),method,reference,clientReference:key,paidFromTill:method==='CASH'&&fromTill};const r=receive?await hospitalityOpsApi.receivePurchaseOrder(po.id,mode==='PAY'?{payment}:{}):await hospitalityOpsApi.payPurchaseOrder(po.id,payment);if(r.success!==false)close();return r}finally{setBusy(false)}})}
  return <Modal title={`${receive?'Receive goods':'Supplier payment'} · ${po.orderNumber}`} onClose={()=>{if(!busy)close()}} footer={<><Btn variant="secondary" disabled={busy} onClick={close}>Cancel</Btn><Btn disabled={busy||(mode==='PAY'&&!valid)} onClick={save}>{busy?'Saving…':receive?'Confirm receipt':'Record payment'}</Btn></>}>
    <p>Purchase total: KES {po.totalCost.toLocaleString()} · Recorded paid: KES {(po.paidAmount||0).toLocaleString()} · Balance: KES {balance.toLocaleString()}</p>
    {receive&&<Select label="Supplier payment" value={mode} onChange={setMode} options={[{value:'UNPAID',label:'Receive goods without recording payment'},...(canPay?[{value:'PAY',label:'Receive goods and record payment'}]:[])]}/>}
    {mode==='PAY'&&<div style={{display:'grid',gap:12}}>{po.paymentStatus==='UNRECORDED'&&<label><input type="checkbox" checked={confirmed} onChange={e=>setConfirmed(e.target.checked)}/> Earlier payments were not tracked. I have confirmed this supplier balance.</label>}
    <Input label="Supplier payment amount (KES)" type="number" value={amount} onChange={setAmount}/>
    <Select label="Supplier payment method" value={method} onChange={v=>{setMethod(v);setFromTill(false)}} options={['CASH','MPESA','CARD','BANK_TRANSFER'].map(value=>({value,label:value.replace('_',' ')}))}/>
    <Input label={method==='CASH'?'Receipt / reference (optional)':'Payment transaction reference'} value={reference} onChange={setReference}/>
    {method==='CASH'&&<label><input type="checkbox" checked={fromTill} onChange={e=>setFromTill(e.target.checked)}/> Paid from the business cash drawer (deduct at day closing)</label>}
    <p>Record money already paid to the supplier. This does not send money or increase customer sales.</p></div>}
  </Modal>
}

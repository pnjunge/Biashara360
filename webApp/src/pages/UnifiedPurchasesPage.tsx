import React, { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { useMenuAccess } from '../components/access/MenuAccess'
import { Btn, PageHeader } from '../components/ui'
import IngredientPurchasing from '../components/hospitality/IngredientPurchasing'
import { hospitalityApi, hospitalityOpsApi, HospitalityOperations } from '../services/api'
import PurchasesPage from './PurchasesPage'

export default function UnifiedPurchasesPage() {
  const access = useMenuAccess()
  const [search, setSearch] = useSearchParams()
  const [hospitalityEnabled, setHospitalityEnabled] = useState<boolean | null>(null)
  const productAccess = access.hasMenu('PURCHASES') && access.hasPermission('purchases.view')
  const ingredientAccess = hospitalityEnabled === true && access.hasMenu('HOSPITALITY_OPS') && access.hasPermission('hospitality.view') && access.hasPermission('hospitality.purchasing')
  useEffect(() => { let active = true; hospitalityApi.status().then(r => { if (active) setHospitalityEnabled(r.success && r.data?.enabled === true) }).catch(() => { if (active) setHospitalityEnabled(false) }); return () => { active = false } }, [])
  if (access.loading || hospitalityEnabled === null) return <p>Loading purchases…</p>
  if (!productAccess && !ingredientAccess) return <div role="alert" style={{padding:24}}>You do not have access to purchases.</div>
  const ingredientSelected = ingredientAccess && (search.get('type') === 'ingredients' || !productAccess)
  return <div style={{display:'flex',flexDirection:'column',gap:16}}>
    <PageHeader title="Purchases" />
    <div role="tablist" aria-label="Purchase types" style={{display:'flex',gap:8,flexWrap:'wrap'}}>
      {productAccess && <Btn variant={ingredientSelected?'secondary':'primary'} onClick={() => setSearch({type:'products'})}>Product purchases</Btn>}
      {ingredientAccess && <Btn variant={ingredientSelected?'primary':'secondary'} onClick={() => setSearch({type:'ingredients'})}>Ingredient purchases</Btn>}
    </div>
    {ingredientSelected ? <IngredientPurchases canPay={access.hasPermission('hospitality.purchase_payments')} /> : <PurchasesPage />}
  </div>
}

function IngredientPurchases({canPay}:{canPay:boolean}) {
  const [data,setData] = useState<HospitalityOperations | null>(null)
  const [error,setError] = useState('')
  const [message,setMessage] = useState('')
  const reload = async () => {
    const r = await hospitalityOpsApi.dashboard()
    if (!r.success || !r.data) throw new Error(r.message || 'Could not load ingredient purchases')
    setData(r.data)
  }
  useEffect(() => {
    let active = true
    const load = async () => { try { const r = await hospitalityOpsApi.dashboard(); if (active) { if (!r.success || !r.data) throw new Error(r.message || 'Could not load ingredient purchases'); setData(r.data) } } catch(e:any) { if (active) setError(e.response?.data?.message || e.message || 'Could not load ingredient purchases') } }
    load(); const timer = window.setInterval(load,5000)
    return () => { active = false; window.clearInterval(timer) }
  },[])
  const act = async (fn:()=>Promise<any>) => {
    setError('');setMessage('')
    try { const r = await fn(); if (r.success === false) throw new Error(r.message || 'Purchase could not be saved'); await reload(); setMessage('Saved successfully') }
    catch(e:any) { setError(e.response?.data?.message || e.message || 'Purchase could not be saved') }
  }
  return <>
    {error && <div role="alert" style={{color:'var(--b360-red)'}}>{error}</div>}
    {message && <p role="status">{message}</p>}
    {data ? <IngredientPurchasing data={data} act={act} canPay={canPay} /> : !error && <p>Loading ingredient purchases…</p>}
  </>
}

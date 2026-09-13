import React, { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { storefrontApi, Storefront } from '../services/api'
import OrderingQr, { OrderingTable } from '../components/storefront/OrderingQr'

export default function OrderingQrPage() {
  const { storeSlug = '' } = useParams()
  const [store, setStore] = useState<(Storefront & { tables?: OrderingTable[] }) | null>(null)
  const [error, setError] = useState('')
  useEffect(() => {
    let active = true
    storefrontApi.get(storeSlug).then(result => {
      if (!active) return
      if (result.success && result.data) setStore(result.data)
      else setError(result.message || 'Store unavailable')
    }).catch(() => { if (active) setError('Store unavailable') })
    return () => { active = false }
  }, [storeSlug])
  return <main style={{ minHeight: '100vh', padding: 24, background: '#f1f5f3', fontFamily: 'system-ui, sans-serif' }}>
    {store ? <><h1 style={{ textAlign: 'center' }}>{store.businessName}</h1><OrderingQr slug={store.storefrontSlug} businessName={store.businessName} tables={store.tables} /></> : <p role={error ? 'alert' : 'status'}>{error || 'Loading ordering codes…'}</p>}
  </main>
}

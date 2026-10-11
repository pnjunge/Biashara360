import React, { useEffect, useMemo, useState } from 'react'
import { ApiResponse, client } from '../services/api'
import { Btn } from './ui'

type Item = { id: string; name: string; sku: string; category: string; onlineVisible: boolean; onlineGroup: string; isActive: boolean }

export default function StorefrontCatalogManager() {
  const [items, setItems] = useState<Item[]>([])
  const [drafts, setDrafts] = useState<Record<string, string>>({})
  const [search, setSearch] = useState('')
  const [busy, setBusy] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)
  useEffect(() => {
    let active = true
    client.get<ApiResponse<Item[]>>('/storefront/items').then(({ data }) => {
      if (!data.success || !data.data) throw new Error(data.message || 'Could not load portal items')
      if (active) { setItems(data.data); setDrafts(Object.fromEntries(data.data.map(item => [item.id, item.onlineGroup]))) }
    }).catch(e => { if (active) setError(e.response?.data?.message || e.message) }).finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [])
  const groups = useMemo(() => [...new Set(items.map(item => item.onlineGroup || item.category || 'Other'))].sort(), [items])
  const save = async (item: Item, visible = item.onlineVisible) => {
    setBusy(item.id); setError('')
    try {
      const { data } = await client.put<ApiResponse<Item>>(`/storefront/items/${encodeURIComponent(item.id)}`, { onlineVisible: visible, onlineGroup: drafts[item.id] || '' })
      if (!data.success || !data.data) throw new Error(data.message || 'Could not save portal item')
      const updated = data.data
      setItems(current => current.map(value => value.id === item.id ? updated : value))
      setDrafts(current => ({ ...current, [item.id]: updated.onlineGroup }))
    } catch (e: any) { setError(e.response?.data?.message || e.message) }
    finally { setBusy('') }
  }
  return <div>
    <p>Remove items from the customer portal without deleting inventory. Assign a group, or leave it blank to use the inventory category. Inactive inventory items remain unavailable online.</p>
    <input aria-label="Search portal items" placeholder="Search name, SKU or group" value={search} onChange={event => setSearch(event.target.value)} style={{ width: '100%', padding: 10, margin: '12px 0', border: '1px solid var(--b360-border)', borderRadius: 8 }} />
    {error && <p role="alert" style={{ color: '#b91c1c' }}>{error}</p>}
    {loading ? <p>Loading portal items…</p> : <>
      <datalist id="portal-item-groups">{groups.map(group => <option key={group} value={group} />)}</datalist>
      <div style={{ maxHeight: 480, overflow: 'auto' }}>{items.filter(item => `${item.name} ${item.sku} ${item.onlineGroup} ${item.category}`.toLowerCase().includes(search.toLowerCase())).map(item => <div key={item.id} style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: 10, padding: '12px 0', borderBottom: '1px solid var(--b360-border)' }}>
        <div style={{ flex: '1 1 160px' }}><strong>{item.name}</strong><div style={{ fontSize: 12 }}>{item.sku} · {item.isActive ? (item.onlineVisible ? 'Shown online' : 'Removed from portal') : 'Inactive'}</div></div>
        <input aria-label={`Portal group for ${item.name}`} list="portal-item-groups" maxLength={100} placeholder={item.category || 'Other'} value={drafts[item.id] || ''} disabled={!!busy} onChange={event => setDrafts(current => ({ ...current, [item.id]: event.target.value }))} style={{ flex: '1 1 150px', minWidth: 0, padding: 9, border: '1px solid var(--b360-border)', borderRadius: 8 }} />
        <Btn small variant="secondary" disabled={!!busy || (drafts[item.id] || '') === item.onlineGroup} onClick={() => save(item)}>Save group</Btn>
        <Btn small variant={item.onlineVisible ? 'secondary' : 'primary'} disabled={!!busy} onClick={() => save(item, !item.onlineVisible)}>{busy === item.id ? 'Saving…' : item.onlineVisible ? 'Remove from portal' : 'Show on portal'}</Btn>
      </div>)}{!items.length && <p>No inventory items yet.</p>}</div>
    </>}
  </div>
}

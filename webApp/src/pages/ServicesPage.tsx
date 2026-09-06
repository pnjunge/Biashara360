import React, { useEffect, useMemo, useState } from 'react'
import { CalendarClock, Plus, RefreshCw, Sparkles, Users, Wrench } from 'lucide-react'
import { Btn, Card, DataTable, Input, KpiCard, PageHeader, Select, StatusBadge } from '../components/ui'
import { customerApi, CustomerResponse, ServiceAppointment, ServiceCatalogItem, ServiceResource, servicesApi, UserResponse, userApi } from '../services/api'

const emptyService = { name: '', description: '', category: '', durationMinutes: '60', price: '0' }
const emptyResource = { name: '', type: 'STATION' }
const emptyAppointment = { serviceId: '', resourceId: '', customerId: '', customerName: '', customerPhone: '', staffUserId: '', startsAt: '', durationMinutes: '', notes: '' }

export default function ServicesPage() {
  const [services, setServices] = useState<ServiceCatalogItem[]>([])
  const [resources, setResources] = useState<ServiceResource[]>([])
  const [appointments, setAppointments] = useState<ServiceAppointment[]>([])
  const [customers, setCustomers] = useState<CustomerResponse[]>([])
  const [users, setUsers] = useState<UserResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [message, setMessage] = useState('')
  const [tab, setTab] = useState<'APPOINTMENTS' | 'CATALOG' | 'RESOURCES'>('APPOINTMENTS')
  const [service, setService] = useState(emptyService)
  const [resource, setResource] = useState(emptyResource)
  const [appointment, setAppointment] = useState(emptyAppointment)

  const load = async () => {
    setLoading(true); setMessage('')
    try {
      const [schedule, customerResult, userResult] = await Promise.all([
        servicesApi.schedule(), customerApi.list(), userApi.list().catch(() => null),
      ])
      if (schedule.success && schedule.data) {
        const loaded = schedule.data
        setServices(loaded.services); setResources(loaded.resources); setAppointments(loaded.appointments)
        if (!appointment.serviceId && loaded.services[0]) setAppointment(prev => ({ ...prev, serviceId: loaded.services[0].id }))
      } else setMessage(schedule.message || 'Could not load services.')
      if (customerResult.success) setCustomers(customerResult.data || [])
      if (userResult?.success) setUsers((userResult.data || []).filter(user => user.isActive !== false))
    } catch (error: any) { setMessage(error.response?.data?.message || 'Could not load appointments and services.') }
    finally { setLoading(false) }
  }
  useEffect(() => { load() }, [])

  const act = async (request: () => Promise<any>, successMessage = 'Saved successfully') => {
    setSaving(true); setMessage('')
    try { const result = await request(); if (!result.success) throw new Error(result.message || 'Request failed'); setMessage(successMessage); await load(); return true }
    catch (error: any) { setMessage(error.response?.data?.message || error.message || 'Request failed'); return false }
    finally { setSaving(false) }
  }

  const createService = () => {
    if (!service.name.trim()) { setMessage('Service name is required.'); return }
    act(() => servicesApi.createCatalog({ ...service, durationMinutes: Number(service.durationMinutes), price: Number(service.price) }), 'Service added.').then(ok => { if (ok) setService(emptyService) })
  }
  const createResource = () => {
    if (!resource.name.trim()) { setMessage('Resource name is required.'); return }
    act(() => servicesApi.createResource(resource), 'Resource added.').then(ok => { if (ok) setResource(emptyResource) })
  }
  const createAppointment = () => {
    if (!appointment.serviceId || !appointment.customerName.trim() || !appointment.startsAt) { setMessage('Choose a service, customer name, and start time.'); return }
    const startsAt = new Date(appointment.startsAt)
    if (Number.isNaN(startsAt.getTime())) { setMessage('Choose a valid date and time.'); return }
    act(() => servicesApi.createAppointment({ ...appointment, resourceId: appointment.resourceId || null, customerId: appointment.customerId || null, staffUserId: appointment.staffUserId || null, durationMinutes: appointment.durationMinutes ? Number(appointment.durationMinutes) : undefined, startsAt: startsAt.toISOString() }), 'Appointment booked.').then(ok => { if (ok) setAppointment(prev => ({ ...emptyAppointment, serviceId: prev.serviceId })) })
  }
  const seedTemplates = () => act(() => servicesApi.seedTemplates(), 'Starter services added for this business type.')
  const updateStatus = (id: string, status: string) => act(() => servicesApi.updateAppointmentStatus(id, status), `Appointment marked ${status.toLowerCase().replace('_', ' ')}.`)

  const todayAppointments = useMemo(() => appointments.filter(item => new Date(item.startsAt).toDateString() === new Date().toDateString()), [appointments])
  const activeServices = services.filter(item => item.isActive)

  return <div className="fade-in" style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
    <PageHeader title="Appointments & Services" action={<div style={{ display: 'flex', gap: 8 }}><Btn variant="secondary" icon={<RefreshCw size={14} />} onClick={load}>Refresh</Btn><Btn icon={<Sparkles size={14} />} onClick={seedTemplates} disabled={saving}>Add starter services</Btn></div>} />
    <p style={{ margin: '-8px 0 0', color: 'var(--b360-text-secondary)', fontSize: 13 }}>A shared service workspace for salons, spas, car washes, gyms, clinics, repair shops, hotels and other appointment-led businesses.</p>
    {message && <div style={{ padding: 11, borderRadius: 8, background: 'var(--b360-green-bg)', color: 'var(--b360-text)', fontSize: 13 }}>{message}</div>}

    <div className="responsive-grid responsive-grid-4">
      <KpiCard title="Today" value={String(todayAppointments.length)} change="Scheduled" color="var(--b360-green)" icon={<CalendarClock size={18} />} />
      <KpiCard title="Booked" value={String(appointments.filter(item => ['BOOKED', 'CONFIRMED'].includes(item.status)).length)} change="Upcoming" color="var(--b360-blue)" icon={<CalendarClock size={18} />} />
      <KpiCard title="Active services" value={String(activeServices.length)} change="Catalog" color="var(--b360-green)" icon={<Wrench size={18} />} />
      <KpiCard title="Resources" value={String(resources.filter(item => item.isActive).length)} change="Bookable" color="var(--b360-amber)" icon={<Users size={18} />} />
    </div>

    <div style={{ display: 'flex', gap: 8, borderBottom: '1px solid var(--b360-border)' }}>
      {([['APPOINTMENTS', 'Appointments'], ['CATALOG', 'Service catalog'], ['RESOURCES', 'Resources']] as const).map(([key, label]) => <button key={key} onClick={() => setTab(key)} style={{ border: 0, borderBottom: tab === key ? '3px solid var(--b360-green)' : '3px solid transparent', background: 'transparent', padding: '10px 14px', color: tab === key ? 'var(--b360-green-dark)' : 'var(--b360-text-secondary)', fontWeight: 700, cursor: 'pointer' }}>{label}</button>)}
    </div>

    {tab === 'APPOINTMENTS' && <>
      <Card style={{ padding: 18 }}>
        <h3 style={{ margin: 0, fontSize: 15 }}>Book an appointment</h3>
        <div className="responsive-grid responsive-grid-4" style={{ marginTop: 14 }}>
          <Select label="Service *" value={appointment.serviceId} onChange={value => setAppointment(prev => ({ ...prev, serviceId: value }))} options={activeServices.map(item => ({ value: item.id, label: `${item.name} · KES ${item.price.toLocaleString()}` }))} placeholder="Choose service" />
          <Input label="Start *" type="datetime-local" value={appointment.startsAt} onChange={value => setAppointment(prev => ({ ...prev, startsAt: value }))} />
          <Select label="Resource" value={appointment.resourceId} onChange={value => setAppointment(prev => ({ ...prev, resourceId: value }))} options={[{ value: '', label: 'Assign later' }, ...resources.filter(item => item.isActive).map(item => ({ value: item.id, label: `${item.name} · ${item.type}` }))]} />
          <Select label="Staff member" value={appointment.staffUserId} onChange={value => setAppointment(prev => ({ ...prev, staffUserId: value }))} options={[{ value: '', label: 'Assign later' }, ...users.map(item => ({ value: item.id, label: item.name }))]} />
          <Select label="Existing customer" value={appointment.customerId} onChange={value => { const customer = customers.find(item => item.id === value); setAppointment(prev => ({ ...prev, customerId: value, customerName: customer?.name || prev.customerName, customerPhone: customer?.phone || prev.customerPhone })) }} options={[{ value: '', label: 'Walk-in / new customer' }, ...customers.map(item => ({ value: item.id, label: `${item.name} · ${item.phone}` }))]} />
          <Input label="Customer name *" value={appointment.customerName} onChange={value => setAppointment(prev => ({ ...prev, customerName: value }))} placeholder="Customer name" />
          <Input label="Phone" value={appointment.customerPhone} onChange={value => setAppointment(prev => ({ ...prev, customerPhone: value }))} placeholder="07xx xxx xxx" />
          <Input label="Duration (minutes)" type="number" value={appointment.durationMinutes} onChange={value => setAppointment(prev => ({ ...prev, durationMinutes: value }))} placeholder="Use service duration" />
        </div>
        <div style={{ display: 'flex', gap: 10, alignItems: 'end', marginTop: 12 }}><div style={{ flex: 1 }}><Input label="Notes" value={appointment.notes} onChange={value => setAppointment(prev => ({ ...prev, notes: value }))} placeholder="Instructions, room number, vehicle plate, or other context" /></div><Btn icon={<Plus size={14} />} onClick={createAppointment} disabled={saving}>Book appointment</Btn></div>
      </Card>
      <Card style={{ padding: 0 }}>
        {loading ? <div style={{ padding: 24, textAlign: 'center' }}>Loading appointments…</div> : appointments.length === 0 ? <div style={{ padding: 24, textAlign: 'center', color: 'var(--b360-text-secondary)' }}>No appointments yet. Add a service or book the first appointment above.</div> : <DataTable headers={['When', 'Service', 'Customer', 'Staff / resource', 'Status', 'Actions']} rows={appointments.map(item => [new Date(item.startsAt).toLocaleString('en-KE', { dateStyle: 'medium', timeStyle: 'short' }), <div><strong>{item.serviceName}</strong><div style={{ fontSize: 11, color: 'var(--b360-text-secondary)' }}>{item.durationMinutes} minutes</div></div>, <div><strong>{item.customerName}</strong><div style={{ fontSize: 11, color: 'var(--b360-text-secondary)' }}>{item.customerPhone || 'No phone'}</div></div>, <div>{users.find(user => user.id === item.staffUserId)?.name || 'Unassigned'}<div style={{ fontSize: 11, color: 'var(--b360-text-secondary)' }}>{item.resourceName || 'No resource'}</div></div>, <StatusBadge status={item.status} />, item.status === 'BOOKED' || item.status === 'CONFIRMED' ? <div style={{ display: 'flex', gap: 5 }}><Btn small onClick={() => updateStatus(item.id, 'CHECKED_IN')}>Check in</Btn><Btn small variant="secondary" onClick={() => updateStatus(item.id, 'CANCELLED')}>Cancel</Btn></div> : item.status === 'CHECKED_IN' ? <Btn small onClick={() => updateStatus(item.id, 'COMPLETED')}>Complete</Btn> : '—'])} />}
      </Card>
    </>}

    {tab === 'CATALOG' && <div className="responsive-grid responsive-grid-2">
      <Card style={{ padding: 18 }}><h3 style={{ margin: 0, fontSize: 15 }}>Add service</h3><div style={{ display: 'grid', gap: 12, marginTop: 14 }}><Input label="Name *" value={service.name} onChange={value => setService(prev => ({ ...prev, name: value }))} placeholder="e.g. Haircut, Basic wash, Personal training" /><Input label="Category" value={service.category} onChange={value => setService(prev => ({ ...prev, category: value }))} placeholder="Beauty, detailing, training…" /><div className="responsive-grid responsive-grid-2"><Input label="Duration (minutes)" type="number" value={service.durationMinutes} onChange={value => setService(prev => ({ ...prev, durationMinutes: value }))} /><Input label="Price (KES)" type="number" value={service.price} onChange={value => setService(prev => ({ ...prev, price: value }))} /></div><Input label="Description" value={service.description} onChange={value => setService(prev => ({ ...prev, description: value }))} /><Btn icon={<Plus size={14} />} onClick={createService} disabled={saving}>Add service</Btn></div></Card>
      <Card style={{ padding: 0 }}>{services.length ? <DataTable headers={['Service', 'Category', 'Duration', 'Price', 'Status']} rows={services.map(item => [<div><strong>{item.name}</strong><div style={{ fontSize: 11, color: 'var(--b360-text-secondary)' }}>{item.description || 'No description'}</div></div>, item.category || 'General', `${item.durationMinutes} min`, `KES ${item.price.toLocaleString()}`, <StatusBadge status={item.isActive ? 'ACTIVE' : 'INACTIVE'} />])} /> : <div style={{ padding: 24, textAlign: 'center', color: 'var(--b360-text-secondary)' }}>No services configured.</div>}</Card>
    </div>}

    {tab === 'RESOURCES' && <div className="responsive-grid responsive-grid-2">
      <Card style={{ padding: 18 }}><h3 style={{ margin: 0, fontSize: 15 }}>Add resource</h3><p style={{ fontSize: 12, color: 'var(--b360-text-secondary)' }}>Resources prevent double-booking chairs, bays, rooms, trainers, classes, or stations.</p><div style={{ display: 'grid', gap: 12 }}><Input label="Name *" value={resource.name} onChange={value => setResource(prev => ({ ...prev, name: value }))} placeholder="e.g. Chair 1, Bay A, Room 204" /><Input label="Type" value={resource.type} onChange={value => setResource(prev => ({ ...prev, type: value }))} placeholder="CHAIR, BAY, ROOM, TRAINER" /><Btn icon={<Plus size={14} />} onClick={createResource} disabled={saving}>Add resource</Btn></div></Card>
      <Card style={{ padding: 0 }}>{resources.length ? <DataTable headers={['Name', 'Type', 'Status']} rows={resources.map(item => [item.name, item.type, <StatusBadge status={item.isActive ? 'ACTIVE' : 'INACTIVE'} />])} /> : <div style={{ padding: 24, textAlign: 'center', color: 'var(--b360-text-secondary)' }}>No resources configured.</div>}</Card>
    </div>}
  </div>
}

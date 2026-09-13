import React, { useEffect, useId, useRef } from 'react'

// ── KPI Card ──────────────────────────────────────────────────────────────────
interface KpiCardProps { title: string; value: string; change: string; icon: React.ReactNode; color: string; bgColor?: string }
export function KpiCard({ title, value, change, icon, color, bgColor }: KpiCardProps) {
  const bg = bgColor || `${color}12`
  return (
    <div className="kpi-card ui-kpi-card" style={{ background:'white', borderRadius:'var(--radius-md)', padding:20, boxShadow:'var(--shadow-sm)', border:'1px solid var(--b360-border)', display:'flex', alignItems:'center', gap:16 }}>
      <div style={{ background:bg, borderRadius:'50%', width:48, height:48, minWidth:48, color, display:'flex', alignItems:'center', justifyContent:'center' }}>
        {icon}
      </div>
      <div style={{ display:'flex', flexDirection:'column', gap:2 }}>
        <span style={{ fontSize:12, color:'var(--b360-text-secondary)', fontWeight:600 }}>{title}</span>
        <div style={{ fontSize:22, fontWeight:800, color:'var(--b360-text)', letterSpacing:'-0.5px' }}>{value}</div>
        <div style={{ fontSize:11, color:'var(--b360-green)', fontWeight:600 }}>{change}</div>
      </div>
    </div>
  )
}

// ── Status Badge ──────────────────────────────────────────────────────────────
export function StatusBadge({ status }: { status: string }) {
  const map: Record<string, [string, string]> = {
    PAID:       ['var(--b360-green)',  'var(--b360-green-bg)'],
    PENDING:    ['var(--b360-amber)',  'var(--b360-amber-bg)'],
    COD:        ['var(--b360-blue)',   'var(--b360-blue-bg)'],
    FAILED:     ['var(--b360-red)',    'var(--b360-red-bg)'],
    DELIVERED:  ['var(--b360-green)',  'var(--b360-green-bg)'],
    SHIPPED:    ['var(--b360-blue)',   'var(--b360-blue-bg)'],
    LOW:        ['var(--b360-amber)',  'var(--b360-amber-bg)'],
    OUT:        ['var(--b360-red)',    'var(--b360-red-bg)'],
    RECONCILED: ['var(--b360-green)',  'var(--b360-green-bg)'],
    MATCHED:    ['var(--b360-green)',  'var(--b360-green-bg)'],
    ADMIN:      ['var(--b360-green)',  'var(--b360-green-bg)'],
    MANAGER:    ['var(--b360-blue)',   'var(--b360-blue-bg)'],
    STAFF:      ['var(--b360-text-secondary)', '#f1f5f9'],
    ACTIVE:     ['var(--b360-green)',  'var(--b360-green-bg)'],
    INACTIVE:   ['var(--b360-red)',    'var(--b360-red-bg)'],
    FREEMIUM:   ['var(--b360-blue)',   'var(--b360-blue-bg)'],
    PREMIUM:    ['var(--b360-green)',  'var(--b360-green-bg)'],
    OPEN:       ['var(--b360-blue)',   'var(--b360-blue-bg)'],
    AWAITING_PAYMENT: ['var(--b360-amber)', 'var(--b360-amber-bg)'],
    NEW:        ['var(--b360-blue)',   'var(--b360-blue-bg)'],
    PREPARING:  ['var(--b360-amber)',  'var(--b360-amber-bg)'],
    READY:      ['var(--b360-green)',  'var(--b360-green-bg)'],
    SERVED:     ['var(--b360-green)',  'var(--b360-green-bg)'],
    DELAYED:    ['var(--b360-red)',    'var(--b360-red-bg)'],
    CANCELLED:  ['var(--b360-red)',    'var(--b360-red-bg)'],
  }
  const [color, bg] = map[status.toUpperCase()] ?? ['var(--b360-text-secondary)', '#f1f5f9']
  return (
    <span style={{ color, background:bg, borderRadius:20, padding:'4px 10px', fontSize:10, fontWeight:700, letterSpacing:'0.25px', display:'inline-block' }}>
      {status}
    </span>
  )
}

// ── Page Header ───────────────────────────────────────────────────────────────
export function PageHeader({ title, action }: { title: string; action?: React.ReactNode }) {
  return (
    <div className="ui-page-header" style={{ display:'flex', justifyContent:'space-between', alignItems:'center', marginBottom:20 }}>
      <h1 style={{ fontSize:26, fontWeight:800, letterSpacing:'-0.5px', color:'var(--b360-text)' }}>{title}</h1>
      {action}
    </div>
  )
}

// ── Card ──────────────────────────────────────────────────────────────────────
export function Card({ children, style }: { children: React.ReactNode; style?: React.CSSProperties }) {
  return (
    <div className="ui-card" style={{ background:'white', borderRadius:'var(--radius-md)', border:'1px solid var(--b360-border)', boxShadow:'var(--shadow-sm)', ...style }}>
      {children}
    </div>
  )
}

// ── Skeleton ──────────────────────────────────────────────────────────────────
export function Skeleton({ height = 14, width = '100%', radius = 8 }: { height?: number; width?: number | string; radius?: number }) {
  return <div className="skeleton" style={{ height, width, borderRadius: radius }} />
}

// ── Button ────────────────────────────────────────────────────────────────────
export function Btn({ children, variant='primary', onClick, icon, small, disabled, type='button' }:
  { children: React.ReactNode; variant?: 'primary'|'secondary'|'danger'; onClick?: () => void; icon?: React.ReactNode; small?: boolean; disabled?: boolean; type?: 'button'|'submit'|'reset' }) {
  const styles: Record<string, React.CSSProperties> = {
    primary:   { background:'var(--b360-green)',  color:'white', border:'none' },
    secondary: { background:'white', color:'var(--b360-text)', border:'1px solid var(--b360-border)' },
    danger:    { background:'var(--b360-red-bg)', color:'var(--b360-red)', border:'1px solid var(--b360-red)' },
  }
  return (
    <button className="btn" type={type} onClick={onClick} disabled={disabled} style={{
      display:'inline-flex', alignItems:'center', justifyContent:'center', gap:6, padding: small ? '6px 14px' : '10px 18px',
      borderRadius:'var(--radius-sm)', fontSize: small ? 12 : 13, fontWeight:600, cursor: disabled ? 'not-allowed' : 'pointer',
      opacity: disabled ? 0.6 : 1,
      boxShadow: variant === 'primary' ? '0 2px 4px rgba(16, 185, 129, 0.1)' : 'none',
      ...styles[variant]
    }}>
      {icon}{children}
    </button>
  )
}

// ── Table ─────────────────────────────────────────────────────────────────────
export function DataTable({ headers, rows }: { headers: string[]; rows: React.ReactNode[][] }) {
  return (
    <div className="ui-table-wrap" style={{ overflowX:'auto', borderRadius:'var(--radius-md)', border:'1px solid var(--b360-border)' }}>
      <table style={{ width:'100%', minWidth: Math.max(640, headers.length * 120), borderCollapse:'collapse' }}>
        <thead>
          <tr style={{ background:'var(--b360-surface)', borderBottom:'2px solid var(--b360-border)' }}>
            {headers.map((h, i) => (
              <th key={i} style={{ position:'sticky', top:0, zIndex:1, background:'var(--b360-surface)', padding:'12px 18px', textAlign:'left', fontSize:11, fontWeight:700, color:'var(--b360-text-secondary)', textTransform:'uppercase', letterSpacing:0.5 }}>
                {h}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row, i) => (
            <tr key={i} style={{ borderBottom:'1px solid var(--b360-border)', transition:'background 0.1s' }}
              onMouseEnter={e => (e.currentTarget.style.background = 'var(--b360-surface)')}
              onMouseLeave={e => (e.currentTarget.style.background = 'transparent')}
            >
              {row.map((cell, j) => (
                <td key={j} style={{ padding:'14px 18px', fontSize:13, color:'var(--b360-text)' }}>{cell}</td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

// ── Alert Banner ──────────────────────────────────────────────────────────────
export function AlertBanner({ message, icon, color }: { message: string; icon: React.ReactNode; color: string }) {
  return (
    <div style={{ display:'flex', alignItems:'center', gap:10, padding:'10px 14px', background:`${color}12`, borderRadius:'var(--radius-sm)', color }}>
      {icon}<span style={{ fontSize:13, fontWeight:600 }}>{message}</span>
    </div>
  )
}

// ── Progress Bar ──────────────────────────────────────────────────────────────
export function ProgressBar({ value, color = 'var(--b360-green)' }: { value: number; color?: string }) {
  return (
    <div style={{ background:'#F1F5F9', borderRadius:4, height:6, overflow:'hidden' }}>
      <div style={{ width:`${Math.min(100, value * 100)}%`, background:color, height:'100%', borderRadius:4, transition:'width 0.3s' }} />
    </div>
  )
}

// ── Input ─────────────────────────────────────────────────────────────────────
export function Input({ label, placeholder, value, onChange, type = 'text' }:
  { label?: string; placeholder?: string; value: string; onChange: (v: string) => void; type?: string }) {
  return (
    <div style={{ display:'flex', flexDirection:'column', gap:5 }}>
      {label && <label style={{ fontSize:12, fontWeight:600, color:'var(--b360-text-secondary)' }}>{label}</label>}
      <input
        type={type} placeholder={placeholder} value={value}
        onChange={e => onChange(e.target.value)}
        style={{ padding:'10px 14px', border:'1px solid var(--b360-border)', borderRadius:'var(--radius-sm)', fontSize:13, outline:'none', fontFamily:'inherit', background:'white', color:'var(--b360-text)' }}
      />
    </div>
  )
}

// ── Modal ─────────────────────────────────────────────────────────────────────
export function Modal({ title, onClose, children, footer, wide, extraWide }: {
  title: React.ReactNode; onClose: () => void; children: React.ReactNode; footer?: React.ReactNode; wide?: boolean; extraWide?: boolean
}) {
  const titleId = useId()
  const dialogRef = useRef<HTMLDivElement>(null)
  const closeRef = useRef(onClose)
  closeRef.current = onClose
  useEffect(() => {
    const previous = document.activeElement as HTMLElement | null
    const focusable = () => Array.from(dialogRef.current?.querySelectorAll<HTMLElement>('button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])') || [])
    focusable()[0]?.focus()
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') { closeRef.current(); return }
      if (event.key !== 'Tab') return
      const items = focusable()
      if (!items.length) { event.preventDefault(); return }
      const first = items[0], last = items[items.length - 1]
      if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last.focus() }
      else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first.focus() }
    }
    document.addEventListener('keydown', onKeyDown)
    const overflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    return () => { document.removeEventListener('keydown', onKeyDown); document.body.style.overflow = overflow; previous?.focus() }
  }, [])
  return (
    <div className="ui-modal-backdrop" onMouseDown={event => { if (event.target === event.currentTarget) onClose() }} style={{ position:'fixed', inset:0, background:'rgba(15, 23, 42, 0.4)', backdropFilter:'blur(4px)', display:'flex', alignItems:'center', justifyContent:'center', zIndex:1000, padding:16 }}>
      <div ref={dialogRef} role="dialog" aria-modal="true" aria-labelledby={titleId} className="ui-modal" style={{ background:'white', borderRadius:'var(--radius-lg)', width:'100%', maxWidth: extraWide ? 1240 : wide ? 680 : 480, maxHeight: extraWide ? '94vh' : '90vh', overflow:'hidden', boxShadow:'var(--shadow-lg)', display:'flex', flexDirection:'column', border:'1px solid var(--b360-border)' }}>
        <div className="ui-modal-header" style={{ display:'flex', alignItems:'center', justifyContent:'space-between', padding:'18px 24px', borderBottom:'1px solid var(--b360-border)', flexShrink:0 }}>
          <h2 id={titleId} style={{ fontSize:16, fontWeight:800, letterSpacing:'-0.25px', color:'var(--b360-text)' }}>{title}</h2>
          <button aria-label="Close dialog" className="btn" type="button" onClick={onClose} style={{ fontSize:22, lineHeight:1, color:'var(--b360-text-secondary)', cursor:'pointer', border:'none', background:'none', padding:'4px 8px' }}>×</button>
        </div>
        <div className="ui-modal-body" style={{ padding:'20px 24px', flex:1, overflow:'auto' }}>{children}</div>
        {footer && (
          <div className="ui-modal-footer" style={{ padding:'14px 24px', borderTop:'1px solid var(--b360-border)', display:'flex', justifyContent:'flex-end', gap:8, flexShrink:0 }}>
            {footer}
          </div>
        )}
      </div>
    </div>
  )
}

// ── Select ────────────────────────────────────────────────────────────────────
export function Select({ label, value, onChange, options, placeholder }: {
  label?: string; value: string; onChange: (v: string) => void; options: { value: string; label: string }[]; placeholder?: string
}) {
  return (
    <div style={{ display:'flex', flexDirection:'column', gap:5 }}>
      {label && <label style={{ fontSize:12, fontWeight:600, color:'var(--b360-text-secondary)' }}>{label}</label>}
      <select value={value} onChange={e => onChange(e.target.value)}
        style={{ padding:'10px 14px', border:'1px solid var(--b360-border)', borderRadius:'var(--radius-sm)', fontSize:13, outline:'none', fontFamily:'inherit', background:'white', color:'var(--b360-text)' }}>
        {placeholder && <option value="" disabled>{placeholder}</option>}
        {options.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
      </select>
    </div>
  )
}

// ── Avatar ────────────────────────────────────────────────────────────────────
export function Avatar({ name, size = 36 }: { name: string; size?: number }) {
  return (
    <div style={{ width:size, height:size, borderRadius:'50%', background:'var(--b360-green)', color:'white', display:'flex', alignItems:'center', justifyContent:'center', fontWeight:700, fontSize:size*0.4, flexShrink:0 }}>
      {name[0]?.toUpperCase()}
    </div>
  )
}

import React from 'react'

// ── KPI Card ──────────────────────────────────────────────────────────────────
interface KpiCardProps { title: string; value: string; change: string; icon: React.ReactNode; color: string }
export function KpiCard({ title, value, change, icon, color }: KpiCardProps) {
  return (
    <div style={{ background:'white', borderRadius:12, padding:20, boxShadow:'var(--shadow-sm)', border:'1px solid var(--b360-border)' }}>
      <div style={{ display:'flex', justifyContent:'space-between', alignItems:'flex-start', marginBottom:12 }}>
        <span style={{ fontSize:12, color:'var(--b360-text-secondary)', fontWeight:500 }}>{title}</span>
        <div style={{ background:`${color}18`, borderRadius:8, padding:8, color, display:'flex', alignItems:'center' }}>{icon}</div>
      </div>
      <div style={{ fontSize:22, fontWeight:800, color, marginBottom:4 }}>{value}</div>
      <div style={{ fontSize:12, color:'var(--b360-text-secondary)' }}>{change}</div>
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
  }
  const [color, bg] = map[status.toUpperCase()] ?? ['var(--b360-text-secondary)', '#f5f5f5']
  return (
    <span style={{ color, background:bg, borderRadius:20, padding:'3px 10px', fontSize:11, fontWeight:700 }}>
      {status}
    </span>
  )
}

// ── Page Header ───────────────────────────────────────────────────────────────
export function PageHeader({ title, action }: { title: string; action?: React.ReactNode }) {
  return (
    <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center', marginBottom:20 }}>
      <h1 style={{ fontSize:22, fontWeight:800 }}>{title}</h1>
      {action}
    </div>
  )
}

// ── Card ──────────────────────────────────────────────────────────────────────
export function Card({ children, style }: { children: React.ReactNode; style?: React.CSSProperties }) {
  return (
    <div style={{ background:'white', borderRadius:12, border:'1px solid var(--b360-border)', boxShadow:'var(--shadow-sm)', ...style }}>
      {children}
    </div>
  )
}

// ── Button ────────────────────────────────────────────────────────────────────
export function Btn({ children, variant='primary', onClick, icon, small }:
  { children: React.ReactNode; variant?: 'primary'|'secondary'|'danger'; onClick?: () => void; icon?: React.ReactNode; small?: boolean }) {
  const styles: Record<string, React.CSSProperties> = {
    primary:   { background:'var(--b360-green)',  color:'white', border:'none' },
    secondary: { background:'white', color:'var(--b360-text)', border:'1px solid var(--b360-border)' },
    danger:    { background:'var(--b360-red-bg)', color:'var(--b360-red)', border:'1px solid var(--b360-red)' },
  }
  return (
    <button onClick={onClick} style={{
      display:'flex', alignItems:'center', gap:6, padding: small ? '6px 12px' : '9px 16px',
      borderRadius:8, fontSize: small ? 12 : 13, fontWeight:600, cursor:'pointer', transition:'opacity 0.15s',
      ...styles[variant]
    }}>
      {icon}{children}
    </button>
  )
}

// ── Table ─────────────────────────────────────────────────────────────────────
export function DataTable({ headers, rows }: { headers: string[]; rows: React.ReactNode[][] }) {
  return (
    <div style={{ overflowX:'auto' }}>
      <table style={{ width:'100%', borderCollapse:'collapse' }}>
        <thead>
          <tr style={{ background:'#F9FAFB', borderBottom:'2px solid var(--b360-border)' }}>
            {headers.map((h, i) => (
              <th key={i} style={{ padding:'10px 16px', textAlign:'left', fontSize:11, fontWeight:700, color:'var(--b360-text-secondary)', textTransform:'uppercase', letterSpacing:0.5 }}>
                {h}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row, i) => (
            <tr key={i} style={{ borderBottom:'1px solid var(--b360-border)', transition:'background 0.1s' }}
              onMouseEnter={e => (e.currentTarget.style.background = '#F9FAFB')}
              onMouseLeave={e => (e.currentTarget.style.background = 'transparent')}
            >
              {row.map((cell, j) => (
                <td key={j} style={{ padding:'12px 16px', fontSize:13 }}>{cell}</td>
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
    <div style={{ display:'flex', alignItems:'center', gap:10, padding:'10px 14px', background:`${color}12`, borderRadius:8, color }}>
      {icon}<span style={{ fontSize:13, fontWeight:500 }}>{message}</span>
    </div>
  )
}

// ── Progress Bar ──────────────────────────────────────────────────────────────
export function ProgressBar({ value, color = 'var(--b360-green)' }: { value: number; color?: string }) {
  return (
    <div style={{ background:'#F0F0F0', borderRadius:4, height:6, overflow:'hidden' }}>
      <div style={{ width:`${Math.min(100, value * 100)}%`, background:color, height:'100%', borderRadius:4, transition:'width 0.3s' }} />
    </div>
  )
}

// ── Input ─────────────────────────────────────────────────────────────────────
export function Input({ label, placeholder, value, onChange, type = 'text' }:
  { label?: string; placeholder?: string; value: string; onChange: (v: string) => void; type?: string }) {
  return (
    <div style={{ display:'flex', flexDirection:'column', gap:5 }}>
      {label && <label style={{ fontSize:12, fontWeight:500, color:'var(--b360-text-secondary)' }}>{label}</label>}
      <input
        type={type} placeholder={placeholder} value={value}
        onChange={e => onChange(e.target.value)}
        style={{ padding:'9px 12px', border:'1px solid var(--b360-border)', borderRadius:8, fontSize:13, outline:'none', fontFamily:'inherit' }}
      />
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

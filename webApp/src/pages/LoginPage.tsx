import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../App'

export default function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [otp, setOtp] = useState('')
  const [step, setStep] = useState<'login' | 'otp'>('login')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault(); setLoading(true); setError('')
    await new Promise(r => setTimeout(r, 800))
    setLoading(false); setStep('otp')
  }

  const handleOtp = async (e: React.FormEvent) => {
    e.preventDefault(); setLoading(true); setError('')
    await new Promise(r => setTimeout(r, 600))
    setLoading(false)
    if (otp.length === 6) { login(); navigate('/dashboard') }
    else setError('Invalid OTP. Enter any 6 digits to continue.')
  }

  return (
    <div style={{ minHeight:'100vh', background:'var(--b360-surface)', display:'flex', alignItems:'center', justifyContent:'center', padding:24 }}>
      <div style={{ width:'100%', maxWidth:400 }}>
        {/* Logo */}
        <div style={{ textAlign:'center', marginBottom:32 }}>
          <div style={{ display:'inline-flex', alignItems:'center', justifyContent:'center', width:64, height:64, background:'var(--b360-green)', borderRadius:16, marginBottom:12 }}>
            <span style={{ color:'white', fontWeight:900, fontSize:18 }}>B360</span>
          </div>
          <h1 style={{ fontSize:24, fontWeight:800, marginBottom:4 }}>Biashara360</h1>
          <p style={{ color:'var(--b360-text-secondary)', fontSize:13 }}>Business management for Kenyan traders</p>
        </div>

        <div style={{ background:'white', borderRadius:16, padding:32, boxShadow:'var(--shadow-md)', border:'1px solid var(--b360-border)' }}>
          {step === 'login' ? (
            <>
              <h2 style={{ fontSize:18, fontWeight:700, marginBottom:20 }}>Sign in to your account</h2>
              <form onSubmit={handleLogin} style={{ display:'flex', flexDirection:'column', gap:16 }}>
                <div>
                  <label style={{ fontSize:12, fontWeight:500, color:'var(--b360-text-secondary)', display:'block', marginBottom:5 }}>Email</label>
                  <input type="email" placeholder="wanjiru@example.com" value={email} onChange={e => setEmail(e.target.value)}
                    required style={{ width:'100%', padding:'10px 12px', border:'1px solid var(--b360-border)', borderRadius:8, fontSize:13, fontFamily:'inherit', outline:'none' }} />
                </div>
                <div>
                  <label style={{ fontSize:12, fontWeight:500, color:'var(--b360-text-secondary)', display:'block', marginBottom:5 }}>Password</label>
                  <input type="password" placeholder="••••••••" value={password} onChange={e => setPassword(e.target.value)}
                    required style={{ width:'100%', padding:'10px 12px', border:'1px solid var(--b360-border)', borderRadius:8, fontSize:13, fontFamily:'inherit', outline:'none' }} />
                </div>
                {error && <p style={{ color:'var(--b360-red)', fontSize:12 }}>{error}</p>}
                <button type="submit" disabled={loading} style={{
                  background:'var(--b360-green)', color:'white', padding:'11px', borderRadius:8,
                  fontWeight:700, fontSize:14, opacity: loading ? 0.7 : 1, cursor:'pointer', border:'none', fontFamily:'inherit'
                }}>
                  {loading ? 'Signing in...' : 'Sign In'}
                </button>
                <p style={{ textAlign:'center', fontSize:12, color:'var(--b360-text-secondary)' }}>
                  2FA required for all accounts
                </p>
              </form>
            </>
          ) : (
            <>
              <div style={{ textAlign:'center', marginBottom:20 }}>
                <div style={{ fontSize:40, marginBottom:8 }}>📱</div>
                <h2 style={{ fontSize:18, fontWeight:700, marginBottom:4 }}>Verify your identity</h2>
                <p style={{ fontSize:13, color:'var(--b360-text-secondary)' }}>We sent a 6-digit code to your phone via SMS</p>
              </div>
              <form onSubmit={handleOtp} style={{ display:'flex', flexDirection:'column', gap:16 }}>
                <input type="text" placeholder="Enter 6-digit code" value={otp} onChange={e => setOtp(e.target.value.replace(/\D/,'').slice(0,6))}
                  maxLength={6} style={{ textAlign:'center', letterSpacing:12, fontSize:28, fontWeight:700, padding:'14px', border:'2px solid var(--b360-green)', borderRadius:10, fontFamily:'inherit', outline:'none' }} />
                {error && <p style={{ color:'var(--b360-red)', fontSize:12, textAlign:'center' }}>{error}</p>}
                <button type="submit" disabled={loading || otp.length < 6} style={{
                  background:'var(--b360-green)', color:'white', padding:11, borderRadius:8,
                  fontWeight:700, fontSize:14, opacity:(loading || otp.length<6) ? 0.6 : 1, cursor:'pointer', border:'none', fontFamily:'inherit'
                }}>
                  {loading ? 'Verifying...' : 'Verify & Sign In'}
                </button>
                <button type="button" onClick={() => setStep('login')} style={{ fontSize:12, color:'var(--b360-text-secondary)', background:'none', border:'none', cursor:'pointer' }}>
                  ← Back to login
                </button>
              </form>
            </>
          )}
        </div>
        <p style={{ textAlign:'center', fontSize:11, color:'var(--b360-text-secondary)', marginTop:16 }}>
          © 2025 Biashara360ERP · Kenya Data Protection Act compliant
        </p>
      </div>
    </div>
  )
}

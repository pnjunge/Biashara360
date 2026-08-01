import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../App'
import { authApi } from '../services/api'

function CustomLoginTextField({
  value,
  onChange,
  placeholder,
  type = 'text',
  icon,
  disabled
}: {
  value: string
  onChange: (v: string) => void
  placeholder: string
  type?: string
  icon: React.ReactNode
  disabled?: boolean
}) {
  const [focused, setFocused] = useState(false)
  const [showPassword, setShowPassword] = useState(false)

  const actualType = type === 'password' ? (showPassword ? 'text' : 'password') : type

  return (
    <div
      style={{
        display: 'flex',
        alignItems: 'center',
        height: 56,
        border: `1.5px solid ${focused ? 'var(--b360-green)' : '#E2E8F0'}`,
        borderRadius: 12,
        overflow: 'hidden',
        background: 'white',
        transition: 'border-color 0.15s ease-in-out',
        width: '100%'
      }}
    >
      {/* Icon Box */}
      <div
        style={{
          width: 56,
          height: '100%',
          background: '#F0FDF4',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          color: 'var(--b360-green)'
        }}
      >
        {icon}
      </div>

      {/* Divider */}
      <div style={{ width: 1, height: '100%', background: '#E2E8F0' }} />

      {/* Input */}
      <input
        type={actualType}
        placeholder={placeholder}
        value={value}
        onChange={e => onChange(e.target.value)}
        disabled={disabled}
        onFocus={() => setFocused(true)}
        onBlur={() => setFocused(false)}
        style={{
          flex: 1,
          height: '100%',
          border: 'none',
          outline: 'none',
          padding: '0 16px',
          fontSize: 15,
          color: '#0F172A',
          background: 'transparent',
          fontFamily: 'inherit'
        }}
      />

      {type === 'password' && (
        <button
          type="button"
          onClick={() => setShowPassword(!showPassword)}
          style={{
            background: 'none',
            border: 'none',
            padding: '0 16px',
            color: '#64748B',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            height: '100%'
          }}
        >
          {showPassword ? (
            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"></path><line x1="1" y1="1" x2="23" y2="23"></line></svg>
          ) : (
            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path><circle cx="12" cy="12" r="3"></circle></svg>
          )}
        </button>
      )}
    </div>
  )
}

export default function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [pin, setPin] = useState('')
  const [loginMode, setLoginMode] = useState<'PASSWORD'|'PIN'>('PASSWORD')
  const [otp, setOtp] = useState('')
  const [userId, setUserId] = useState('')
  const [step, setStep] = useState<'login' | 'otp'>('login')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [rememberMe, setRememberMe] = useState(true)

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!email.trim() || (loginMode === 'PASSWORD' ? !password : pin.length !== 6)) { setError(loginMode === 'PASSWORD' ? 'Email and password are required' : 'Email and a 6-digit PIN are required'); return }
    setLoading(true)
    setError('')
    try {
      const result = loginMode === 'PASSWORD' ? await authApi.login({ email, password }) : await authApi.loginWithPin({ email, pin })
      if (result.success && result.data) {
        setUserId(result.data.userId)
        if (result.data.requiresOtp) {
          setStep('otp')
        } else {
          if (result.data.accessToken && result.data.refreshToken && result.data.user) {
            localStorage.setItem('accessToken', result.data.accessToken)
            localStorage.setItem('refreshToken', result.data.refreshToken)
            localStorage.setItem('user', JSON.stringify(result.data.user))
            login()
            navigate('/dashboard')
          } else {
            setError('Login did not return a valid session. Please try again.')
          }
        }
      } else {
        setError(result.message || 'Login failed')
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'Network error. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  const handleOtp = async (e: React.FormEvent) => {
    e.preventDefault()
    setLoading(true)
    setError('')
    try {
      if (otp.length !== 6) {
        setError('Enter 6-digit OTP')
        setLoading(false)
        return
      }
      const result = await authApi.verifyOtp({ userId, otp, channel: 'SMS' })
      if (result.success) {
        login()
        navigate('/dashboard')
      } else {
        setError(result.message || 'OTP verification failed')
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'Network error. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-page"
      style={{
        minHeight: '100vh',
        background: '#F4FBF7',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: 24,
        position: 'relative',
        overflow: 'hidden'
      }}
    >
      {/* Background brand curves and dot grids */}
      <svg style={{ position: 'absolute', top: 0, left: 0, width: '100%', height: '100%', pointerEvents: 'none', zIndex: 0 }}>
        <circle cx="-5%" cy="10%" r="35%" fill="var(--b360-green)" fillOpacity="0.06" />
        <circle cx="-5%" cy="10%" r="42%" fill="none" stroke="var(--b360-green)" strokeWidth="2" strokeOpacity="0.04" />
        
        <circle cx="105%" cy="85%" r="30%" fill="var(--b360-green)" fillOpacity="0.05" />
        <circle cx="105%" cy="85%" r="38%" fill="none" stroke="var(--b360-green)" strokeWidth="2" strokeOpacity="0.03" />

        <pattern id="dot-grid" x="0" y="0" width="24" height="24" patternUnits="userSpaceOnUse">
          <circle cx="2" cy="2" r="2" fill="var(--b360-green)" fillOpacity="0.12" />
        </pattern>
        <rect x="80%" y="10%" width="150" height="250" fill="url(#dot-grid)" />
        <rect x="5%" y="60%" width="150" height="250" fill="url(#dot-grid)" />
      </svg>

      <div style={{ width: '100%', maxWidth: 440, position: 'relative', zIndex: 1 }}>
        <div className="auth-card"
          style={{
            background: 'white',
            borderRadius: 16,
            padding: 40,
            boxShadow: '0 10px 25px -5px rgba(0, 0, 0, 0.05), 0 8px 10px -6px rgba(0, 0, 0, 0.05)',
            border: '1px solid var(--b360-border)',
            display: 'flex',
            flexDirection: 'column',
            gap: 20
          }}
        >
          {step === 'login' ? (
            <>
              {/* Header Logo */}
              <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 12, marginBottom: 8 }}>
                <div
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    width: 48,
                    height: 48,
                    background: 'var(--b360-green)',
                    borderRadius: 12
                  }}
                >
                  <span style={{ color: 'white', fontWeight: 900, fontSize: 14 }}>B360</span>
                </div>

                <div style={{ textAlign: 'center' }}>
                  <h1 style={{ fontSize: 22, fontWeight: 'bold', color: '#0F172A', margin: 0 }}>Welcome to Biashara360</h1>
                  <p style={{ color: 'gray', fontSize: 12, margin: '4px 0 0 0' }}>Enterprise Management Platform</p>
                </div>
              </div>

              {error && (
                <div
                  style={{
                    background: '#FEE2E2',
                    border: '1px solid #FCA5A5',
                    borderRadius: 8,
                    padding: '12px 16px',
                    display: 'flex',
                    alignItems: 'center',
                    gap: 8,
                    color: '#991B1B',
                    fontSize: 13
                  }}
                >
                  <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="8" x2="12" y2="12"></line><line x1="12" y1="16" x2="12.01" y2="16"></line></svg>
                  <span>{error}</span>
                </div>
              )}

              <form onSubmit={handleLogin} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
                <CustomLoginTextField
                  value={email}
                  onChange={v => { setEmail(v); setError('') }}
                  placeholder="Email / Username"
                  disabled={loading}
                  icon={
                    <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2"></path><circle cx="12" cy="7" r="4"></circle></svg>
                  }
                />

                <div style={{display:'grid',gridTemplateColumns:'1fr 1fr',padding:3,borderRadius:9,background:'#F1F5F9'}}>
                  {(['PASSWORD','PIN'] as const).map(mode=><button key={mode} type="button" onClick={()=>{setLoginMode(mode);setError('')}} style={{border:0,borderRadius:7,padding:9,background:loginMode===mode ? 'white' : 'transparent',color:loginMode===mode ? 'var(--b360-green)' : '#64748B',fontWeight:700,cursor:'pointer',boxShadow:loginMode===mode ? '0 1px 3px #0001' : 'none'}}>{mode === 'PASSWORD' ? 'Password' : '6-digit PIN'}</button>)}
                </div>

                {loginMode === 'PASSWORD' ? <CustomLoginTextField
                  value={password}
                  onChange={v => { setPassword(v); setError('') }}
                  placeholder="Password"
                  type="password"
                  disabled={loading}
                  icon={
                    <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect><path d="M7 11V7a5 5 0 0 1 10 0v4"></path></svg>
                  }
                /> : <CustomLoginTextField value={pin} onChange={v=>{setPin(v.replace(/\D/g,'').slice(0,6));setError('')}} placeholder="6-digit PIN" type="password" disabled={loading} icon={<span style={{fontWeight:900,color:'var(--b360-green)'}}>••</span>} />}

                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', width: '100%' }}>
                  <label style={{ display: 'flex', alignItems: 'center', gap: 8, cursor: 'pointer', fontSize: 14, color: '#475569' }}>
                    <input
                      type="checkbox"
                      checked={rememberMe}
                      onChange={e => setRememberMe(e.target.checked)}
                      style={{
                        accentColor: 'var(--b360-green)',
                        width: 16,
                        height: 16,
                        cursor: 'pointer'
                      }}
                    />
                    Remember me
                  </label>
                  <button
                    type="button"
                    style={{
                      border: 'none',
                      background: 'none',
                      color: 'var(--b360-green)',
                      fontWeight: 600,
                      fontSize: 14,
                      cursor: 'pointer',
                      padding: 0
                    }}
                  >
                    Forgot password?
                  </button>
                </div>

                <button
                  type="submit"
                  disabled={loading || !email.trim() || (loginMode === 'PASSWORD' ? !password : pin.length !== 6)}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    gap: 8,
                    height: 48,
                    background: 'var(--b360-green)',
                    color: 'white',
                    border: 'none',
                    borderRadius: 8,
                    fontSize: 14,
                    fontWeight: 'bold',
                    cursor: (loading || !email.trim() || (loginMode === 'PASSWORD' ? !password : pin.length !== 6)) ? 'not-allowed' : 'pointer',
                    opacity: (loading || !email.trim() || (loginMode === 'PASSWORD' ? !password : pin.length !== 6)) ? 0.6 : 1,
                    width: '100%',
                    transition: 'opacity 0.15s ease-in-out'
                  }}
                >
                  {loading ? (
                    'Signing in...'
                  ) : (
                    <>
                      <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4"></path><polyline points="10 17 15 12 10 7"></polyline><line x1="15" y1="12" x2="3" y2="12"></line></svg>
                      <span>Sign In</span>
                    </>
                  )}
                </button>

                <div style={{ display: 'flex', alignItems: 'center', width: '100%', margin: '4px 0' }}>
                  <div style={{ flex: 1, height: 1, background: '#E2E8F0' }} />
                  <span style={{ color: '#94A3B8', fontSize: 12, fontWeight: 'bold', padding: '0 16px' }}>OR</span>
                  <div style={{ flex: 1, height: 1, background: '#E2E8F0' }} />
                </div>

                <button
                  type="button"
                  onClick={() => setError('Fingerprint sign-in is not configured for this account.')}
                  disabled={loading}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    gap: 8,
                    height: 48,
                    background: 'white',
                    color: '#475569',
                    border: '1px solid #E2E8F0',
                    borderRadius: 8,
                    fontSize: 14,
                    fontWeight: 600,
                    cursor: 'pointer',
                    width: '100%'
                  }}
                >
                  <svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="var(--b360-green)" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M12 22v-3M12 17v-1.5M12 12c-2.21 0-4 1.79-4 4v3.5M12 8c-4.42 0-8 3.58-8 8v3.5M16 16c0-2.21-1.79-4-4-4M20 16c0-4.42-3.58-8-8-8M8 12c0-2.21 1.79-4 4-4M12 4c-6.63 0-12 5.37-12 12M12 2c7.73 0 14 6.27 14 14"/></svg>
                  <span>Fingerprint sign-in (unavailable)</span>
                </button>
              </form>
            </>
          ) : (
            <>
              <div style={{ textAlign: 'center', marginBottom: 20 }}>
                <div style={{ fontSize: 40, marginBottom: 8 }}>📱</div>
                <h2 style={{ fontSize: 18, fontWeight: 700, marginBottom: 4 }}>Verify your identity</h2>
                <p style={{ fontSize: 13, color: 'var(--b360-text-secondary)' }}>We sent a 6-digit code to your phone via SMS</p>
              </div>
              <form onSubmit={handleOtp} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
                <input
                  type="text"
                  placeholder="Enter 6-digit code"
                  value={otp}
                  onChange={e => setOtp(e.target.value.replace(/\D/g, '').slice(0, 6))}
                  style={{
                    height: 56,
                    border: '1.5px solid #E2E8F0',
                    borderRadius: 12,
                    padding: '0 16px',
                    fontSize: 16,
                    textAlign: 'center',
                    letterSpacing: 4,
                    outline: 'none',
                    fontFamily: 'inherit',
                    width: '100%',
                    boxSizing: 'border-box'
                  }}
                />
                {error && <p style={{ color: 'var(--b360-red)', fontSize: 12, textAlign: 'center', margin: 0 }}>{error}</p>}
                <button
                  type="submit"
                  disabled={loading || otp.length < 6}
                  style={{
                    height: 48,
                    background: 'var(--b360-green)',
                    color: 'white',
                    border: 'none',
                    borderRadius: 8,
                    fontSize: 14,
                    fontWeight: 'bold',
                    cursor: (loading || otp.length < 6) ? 'not-allowed' : 'pointer',
                    opacity: (loading || otp.length < 6) ? 0.6 : 1,
                    width: '100%'
                  }}
                >
                  {loading ? 'Verifying...' : 'Verify & Sign In'}
                </button>
                <button
                  type="button"
                  onClick={() => setStep('login')}
                  style={{
                    height: 48,
                    background: 'white',
                    color: '#475569',
                    border: '1px solid #E2E8F0',
                    borderRadius: 8,
                    fontSize: 14,
                    fontWeight: 600,
                    cursor: 'pointer',
                    width: '100%'
                  }}
                >
                  ← Back to login
                </button>
              </form>
            </>
          )}
        </div>
        <p style={{ textAlign: 'center', fontSize: 11, color: 'var(--b360-text-secondary)', marginTop: 16 }}>
          © 2025 Biashara360ERP · Kenya Data Protection Act compliant
        </p>
      </div>
    </div>
  )
}

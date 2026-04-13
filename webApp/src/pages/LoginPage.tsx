import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../App'
import { authApi } from '../services/api'
import { Btn, Input } from '../components/ui'

export default function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [otp, setOtp] = useState('')
  const [userId, setUserId] = useState('')
  const [step, setStep] = useState<'login' | 'otp'>('login')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!email.trim() || !password) { setError('Email and password are required'); return }
    setLoading(true)
    setError('')
    try {
      const result = await authApi.login({ email, password })
      if (result.success && result.data) {
        setUserId(result.data.userId)
        if (result.data.requiresOtp) {
          setStep('otp')
        } else {
          if (result.data.accessToken) {
            localStorage.setItem('accessToken', result.data.accessToken)
            localStorage.setItem('refreshToken', result.data.refreshToken!)
            localStorage.setItem('user', JSON.stringify(result.data.user))
          }
          login()
          navigate('/dashboard')
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
                <Input
                  label="Email"
                  placeholder="wanjiru@example.com"
                  value={email}
                  onChange={setEmail}
                  type="email"
                />
                <Input
                  label="Password"
                  placeholder="••••••••"
                  value={password}
                  onChange={setPassword}
                  type="password"
                />
                {error && <p style={{ color:'var(--b360-red)', fontSize:12 }}>{error}</p>}
                <Btn
                  type="submit"
                  disabled={loading || !email.trim() || !password}
                >
                  {loading ? 'Signing in...' : 'Sign In'}
                </Btn>
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
                <Input
                  placeholder="Enter 6-digit code"
                  value={otp}
                  onChange={v => setOtp(v.replace(/\D/g, '').slice(0, 6))}
                  type="text"
                />
                {error && <p style={{ color:'var(--b360-red)', fontSize:12, textAlign:'center' }}>{error}</p>}
                <Btn type="submit" disabled={loading || otp.length < 6}>
                  {loading ? 'Verifying...' : 'Verify & Sign In'}
                </Btn>
                <Btn variant="secondary" onClick={() => setStep('login')}>
                  ← Back to login
                </Btn>
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

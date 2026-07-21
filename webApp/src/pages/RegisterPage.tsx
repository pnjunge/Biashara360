import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../App'
import { authApi } from '../services/api'
import { Btn, Input, Select } from '../components/ui'

const BUSINESS_TYPES = [
  { value: 'RETAIL', label: 'Retail Seller / Mchuuzi' },
  { value: 'SERVICE', label: 'Service Provider / Mhudumu' },
  { value: 'HYBRID', label: 'Hybrid Business / Biashara Mseto' },
  { value: 'ONLINE_SELLER', label: 'Online Seller / Muuzaji Mtandaoni' }
]

export default function RegisterPage() {
  const { login } = useAuth()
  const navigate = useNavigate()

  // Form fields
  const [name, setName] = useState('')
  const [phone, setPhone] = useState('')
  const [email, setEmail] = useState('')
  const [businessName, setBusinessName] = useState('')
  const [businessType, setBusinessType] = useState('RETAIL')
  const [password, setPassword] = useState('')

  // Flow control
  const [step, setStep] = useState<'register' | 'otp'>('register')
  const [userId, setUserId] = useState('')
  const [otp, setOtp] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!name.trim() || !phone.trim() || !email.trim() || !businessName.trim() || !password) {
      setError('All fields are required')
      return
    }
    if (password.length < 6) {
      setError('Password must be at least 6 characters')
      return
    }

    setLoading(true)
    setError('')

    try {
      // 1. Call Register
      const registerRes = await authApi.register({
        name,
        phone,
        email,
        password,
        businessName,
        businessType
      })

      if (registerRes.success) {
        // 2. Automatically trigger login to initiate OTP delivery
        const loginRes = await authApi.login({ email, password })
        if (loginRes.success && loginRes.data) {
          setUserId(loginRes.data.userId)
          if (loginRes.data.requiresOtp) {
            setStep('otp')
          } else {
            // Backup in case 2FA is somehow disabled
            if (loginRes.data.accessToken && loginRes.data.refreshToken && loginRes.data.user) {
              localStorage.setItem('accessToken', loginRes.data.accessToken)
              localStorage.setItem('refreshToken', loginRes.data.refreshToken)
              localStorage.setItem('user', JSON.stringify(loginRes.data.user))
              login()
              navigate('/dashboard')
            } else {
              setError('Registration login did not return a valid session. Please sign in manually.')
            }
          }
        } else {
          setError(loginRes.message || 'Registration succeeded, but login initialization failed. Please sign in manually.')
        }
      } else {
        setError(registerRes.message || 'Registration failed')
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'Network error. Please check your connection.')
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
      setError(err.response?.data?.message || 'Network error. Please check your code.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div style={{ minHeight: '100vh', background: 'var(--b360-surface)', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 24 }}>
      <div style={{ width: '100%', maxWidth: 450 }}>
        {/* Logo */}
        <div style={{ textAlign: 'center', marginBottom: 24 }}>
          <div style={{ display: 'inline-flex', alignItems: 'center', justifyContent: 'center', width: 64, height: 64, background: 'var(--b360-green)', borderRadius: 16, marginBottom: 12 }}>
            <span style={{ color: 'white', fontWeight: 900, fontSize: 18 }}>B360</span>
          </div>
          <h1 style={{ fontSize: 24, fontWeight: 800, marginBottom: 4 }}>Biashara360</h1>
          <p style={{ color: 'var(--b360-text-secondary)', fontSize: 13 }}>Biashara yako, nguvu yako · Create Merchant Account</p>
        </div>

        <div style={{ background: 'white', borderRadius: 16, padding: 32, boxShadow: 'var(--shadow-md)', border: '1px solid var(--b360-border)' }}>
          {step === 'register' ? (
            <>
              <h2 style={{ fontSize: 18, fontWeight: 700, marginBottom: 20 }}>Self Onboarding / Jisajili</h2>
              <form onSubmit={handleRegister} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
                <Input
                  label="Full Name / Jina Kamili *"
                  placeholder="e.g. Jane Mwangi"
                  value={name}
                  onChange={setName}
                />
                
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                  <Input
                    label="Phone / Simu (07XX) *"
                    placeholder="e.g. 0712345678"
                    value={phone}
                    onChange={setPhone}
                  />
                  <Input
                    label="Email / Barua Pepe *"
                    placeholder="e.g. jane@example.com"
                    value={email}
                    onChange={setEmail}
                    type="email"
                  />
                </div>

                <Input
                  label="Business Name / Jina la Biashara *"
                  placeholder="e.g. Kamau Wholesalers"
                  value={businessName}
                  onChange={setBusinessName}
                />

                <Select
                  label="Business Type / Aina ya Biashara *"
                  value={businessType}
                  onChange={setBusinessType}
                  options={BUSINESS_TYPES}
                />

                <Input
                  label="Password / Nenosiri *"
                  placeholder="•••••••• (Min 6 characters)"
                  value={password}
                  onChange={setPassword}
                  type="password"
                />

                {error && <p style={{ color: 'var(--b360-red)', fontSize: 12 }}>{error}</p>}

                <Btn
                  type="submit"
                  disabled={loading || !name.trim() || !phone.trim() || !email.trim() || !businessName.trim() || !password}
                >
                  {loading ? 'Creating Account...' : 'Register / Jiunge sasa'}
                </Btn>

                <div style={{ textAlign: 'center', marginTop: 8 }}>
                  <span style={{ fontSize: 12, color: 'var(--b360-text-secondary)' }}>
                    Already have an account?{' '}
                    <button
                      type="button"
                      onClick={() => navigate('/login')}
                      style={{ color: 'var(--b360-green)', fontWeight: 600, border: 'none', background: 'none', padding: 0 }}
                    >
                      Sign In / Ingia
                    </button>
                  </span>
                </div>
              </form>
            </>
          ) : (
            <>
              <div style={{ textAlign: 'center', marginBottom: 20 }}>
                <div style={{ fontSize: 40, marginBottom: 8 }}>🔐</div>
                <h2 style={{ fontSize: 18, fontWeight: 700, marginBottom: 4 }}>Verify Registration</h2>
                <p style={{ fontSize: 13, color: 'var(--b360-text-secondary)' }}>
                  Enter the 6-digit security code sent to your phone/email to activate your business.
                </p>
              </div>

              <form onSubmit={handleOtp} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
                <Input
                  placeholder="Enter 6-digit code"
                  value={otp}
                  onChange={v => setOtp(v.replace(/\D/g, '').slice(0, 6))}
                  type="text"
                />

                {error && <p style={{ color: 'var(--b360-red)', fontSize: 12, textAlign: 'center' }}>{error}</p>}

                <Btn type="submit" disabled={loading || otp.length < 6}>
                  {loading ? 'Activating...' : 'Verify & Complete Setup'}
                </Btn>

                <Btn variant="secondary" onClick={() => setStep('register')}>
                  ← Back / Rudi nyuma
                </Btn>
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

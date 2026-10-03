import React, { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { ShieldCheck, MessageCircle, ArrowRight, Menu, X, LayoutDashboard, Sparkles } from 'lucide-react'
import { useAuth } from '../../App'

interface MarketingNavProps {
  onOpenDemoModal: () => void
}

export default function MarketingNav({ onOpenDemoModal }: MarketingNavProps) {
  const { isAuthenticated, user } = useAuth()
  const [scrolled, setScrolled] = useState(false)
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)

  useEffect(() => {
    const handleScroll = () => {
      setScrolled(window.scrollY > 20)
    }
    window.addEventListener('scroll', handleScroll, { passive: true })
    return () => window.removeEventListener('scroll', handleScroll)
  }, [])

  const handleLogoClick = (e: React.MouseEvent) => {
    if (window.location.pathname === '/' || window.location.pathname === '/marketing') {
      e.preventDefault()
      window.scrollTo({ top: 0, behavior: 'smooth' })
    }
  }

  return (
    <header className={`m-navbar ${scrolled ? 'scrolled' : ''}`}>
      <div className="m-container m-nav-container">
        {/* Brand Logo */}
        <Link to="/" onClick={handleLogoClick} className="m-brand-logo" aria-label="Biashara360 Home">
          <div className="m-logo-icon">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <circle cx="12" cy="12" r="9" stroke="white" strokeWidth="2.2" strokeLinecap="round" strokeDasharray="38 12" />
              <circle cx="12" cy="12" r="5" stroke="#FDE047" strokeWidth="1.8" strokeLinecap="round" strokeDasharray="22 8" />
              <circle cx="12" cy="12" r="2" fill="white" />
            </svg>
          </div>
          <div style={{ display: 'flex', flexDirection: 'column' }}>
            <span style={{ letterSpacing: '-0.03em', lineHeight: 1.1 }}>
              Biashara<span style={{ color: 'var(--m-primary)' }}>360</span>
            </span>
            <span style={{ fontSize: '10px', color: '#64748b', fontWeight: 600, letterSpacing: '0.04em' }}>
              KENYA COMMERCE OS
            </span>
          </div>
        </Link>

        {/* Desktop Navigation Links */}
        <nav aria-label="Primary Navigation" className="m-nav-links">
          <a href="/#features">Features</a>
          <a href="/#simulator">Live Simulator</a>
          <a href="/#solutions">Solutions</a>
          <a href="/#calculator">ROI Calculator</a>
          <a href="/#pricing">Pricing</a>
          <a href="/#faq">FAQ</a>
          <Link to="/downloads" style={{ color: '#059669', fontWeight: 700 }}>Apps</Link>
        </nav>

        {/* Header Right Actions */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          {/* Quick Demo Booking / WhatsApp */}
          <button
            type="button"
            onClick={onOpenDemoModal}
            className="m-btn m-btn-secondary"
            style={{ padding: '8px 16px', fontSize: '13px', display: 'none', minHeight: 40 }}
            id="nav-book-demo-btn"
          >
            <Sparkles size={15} color="var(--m-primary)" />
            <span>Book Demo</span>
          </button>

          {isAuthenticated ? (
            <Link
              to="/dashboard"
              className="m-btn m-btn-primary"
              style={{ padding: '9px 18px', fontSize: '14px', borderRadius: '10px' }}
              id="nav-go-dashboard-btn"
            >
              <LayoutDashboard size={16} />
              <span>Go to Dashboard</span>
            </Link>
          ) : (
            <>
              <Link
                to="/login"
                style={{
                  fontSize: '14px',
                  fontWeight: 600,
                  color: '#1e293b',
                  padding: '8px 14px',
                  borderRadius: '8px',
                  transition: 'background 0.15s ease'
                }}
                id="nav-login-btn"
              >
                Sign In
              </Link>

              <Link
                to="/register"
                className="m-btn m-btn-primary"
                style={{ padding: '9px 18px', fontSize: '14px', borderRadius: '10px' }}
                id="nav-free-trial-btn"
              >
                <span>Start Free Trial</span>
                <ArrowRight size={15} />
              </Link>
            </>
          )}

          {/* Mobile Menu Toggle */}
          <button
            type="button"
            onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              width: 40,
              height: 40,
              borderRadius: 8,
              border: '1px solid #e2e8f0',
              background: '#ffffff',
              color: '#0f172a',
              cursor: 'pointer'
            }}
            className="md:hidden"
            aria-label="Toggle Navigation Menu"
          >
            {mobileMenuOpen ? <X size={20} /> : <Menu size={20} />}
          </button>
        </div>
      </div>

      {/* Mobile Drawer */}
      {mobileMenuOpen && (
        <div
          style={{
            background: '#ffffff',
            borderBottom: '1px solid #e2e8f0',
            padding: '20px 24px',
            display: 'flex',
            flexDirection: 'column',
            gap: 16,
            boxShadow: '0 12px 24px rgba(0,0,0,0.06)'
          }}
        >
          <a
            href="/#features"
            onClick={() => setMobileMenuOpen(false)}
            style={{ fontSize: 16, fontWeight: 600, color: '#1e293b', padding: '6px 0' }}
          >
            Features & Capabilities
          </a>
          <a
            href="/#simulator"
            onClick={() => setMobileMenuOpen(false)}
            style={{ fontSize: 16, fontWeight: 600, color: '#1e293b', padding: '6px 0' }}
          >
            Interactive Live Simulator
          </a>
          <a
            href="/#solutions"
            onClick={() => setMobileMenuOpen(false)}
            style={{ fontSize: 16, fontWeight: 600, color: '#1e293b', padding: '6px 0' }}
          >
            Solutions by Industry
          </a>
          <a
            href="/#calculator"
            onClick={() => setMobileMenuOpen(false)}
            style={{ fontSize: 16, fontWeight: 600, color: '#1e293b', padding: '6px 0' }}
          >
            ROI & Time Saved Calculator
          </a>
          <a
            href="/#pricing"
            onClick={() => setMobileMenuOpen(false)}
            style={{ fontSize: 16, fontWeight: 600, color: '#1e293b', padding: '6px 0' }}
          >
            Kenyan Pricing Plans
          </a>
          <a
            href="/#faq"
            onClick={() => setMobileMenuOpen(false)}
            style={{ fontSize: 16, fontWeight: 600, color: '#1e293b', padding: '6px 0' }}
          >
            Frequently Asked Questions
          </a>
          <Link
            to="/downloads"
            onClick={() => setMobileMenuOpen(false)}
            style={{ fontSize: 16, fontWeight: 700, color: '#059669', padding: '6px 0' }}
          >
            Download Merchant Apps (Android, Desktop)
          </Link>

          <div style={{ height: 1, background: '#e2e8f0', margin: '8px 0' }} />

          <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
            {isAuthenticated ? (
              <Link
                to="/dashboard"
                className="m-btn m-btn-primary"
                style={{ width: '100%', justifyContent: 'center' }}
              >
                Go to Dashboard
              </Link>
            ) : (
              <>
                <Link
                  to="/register"
                  className="m-btn m-btn-primary"
                  style={{ width: '100%', justifyContent: 'center' }}
                >
                  Start 14-Day Free Trial
                </Link>
                <Link
                  to="/login"
                  className="m-btn m-btn-secondary"
                  style={{ width: '100%', justifyContent: 'center' }}
                >
                  Sign In to Merchant Account
                </Link>
              </>
            )}
          </div>
        </div>
      )}
    </header>
  )
}

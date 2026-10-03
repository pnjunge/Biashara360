import React from 'react'
import { Link } from 'react-router-dom'
import { ShieldCheck, Phone, Mail, MapPin, Lock, CheckCircle2 } from 'lucide-react'

export default function MarketingFooter() {
  return (
    <footer className="m-footer">
      <div className="m-container">
        <div className="m-footer-grid">
          {/* Col 1: Brand & Badges */}
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 14 }}>
              <div className="m-logo-icon" style={{ width: 34, height: 34, borderRadius: 10 }}>
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <circle cx="12" cy="12" r="9" stroke="white" strokeWidth="2.2" strokeLinecap="round" strokeDasharray="38 12" />
                  <circle cx="12" cy="12" r="5" stroke="#FDE047" strokeWidth="1.8" strokeLinecap="round" strokeDasharray="22 8" />
                  <circle cx="12" cy="12" r="2" fill="white" />
                </svg>
              </div>
              <span style={{ fontSize: 20, fontWeight: 800, color: '#ffffff', fontFamily: 'var(--m-font-heading)' }}>
                Biashara<span style={{ color: '#10b981' }}>360</span>
              </span>
            </div>

            <p style={{ fontSize: 14, color: '#94a3b8', lineHeight: 1.6, maxWidth: 360, marginBottom: 20 }}>
              The all-in-one cloud & offline operating system built for Kenyan retail, wholesale, and hospitality. Streamline checkout, automate M-Pesa, and stay 100% KRA eTIMS compliant.
            </p>

            <div style={{ display: 'flex', flexDirection: 'column', gap: 8, fontSize: 13, color: '#cbd5e1' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <MapPin size={15} color="#10b981" />
                <span>Nairobi, Kenya (Westlands / CBD)</span>
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <Phone size={15} color="#10b981" />
                <span>+254 700 360 360</span>
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <Mail size={15} color="#10b981" />
                <span>hello@biashara360.co.ke</span>
              </div>
            </div>
          </div>

          {/* Col 2: Product */}
          <div>
            <h4>Platform</h4>
            <ul className="m-footer-links">
              <li><a href="#features">Smart POS System</a></li>
              <li><a href="#simulator">KRA eTIMS Fiscalizer</a></li>
              <li><a href="#features">Automated M-Pesa STK</a></li>
              <li><a href="#features">Multi-Store Inventory</a></li>
              <li><a href="#features">Restaurant QR & KDS</a></li>
              <li><a href="#features">Debtors & CRM Ledger</a></li>
              <li><Link to="/downloads">Download Merchant Apps</Link></li>
            </ul>
          </div>

          {/* Col 3: Industry Solutions */}
          <div>
            <h4>Industries</h4>
            <ul className="m-footer-links">
              <li><a href="#solutions">Supermarkets & Groceries</a></li>
              <li><a href="#solutions">Bars, Lounges & Cafes</a></li>
              <li><a href="#solutions">Wholesalers & Distributors</a></li>
              <li><a href="#solutions">Hardware & Agrovets</a></li>
              <li><a href="#solutions">Pharmacies & Chemists</a></li>
              <li><a href="#solutions">Salons & Boutiques</a></li>
            </ul>
          </div>

          {/* Col 4: Trust & Compliance */}
          <div>
            <h4>Trust & Security</h4>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 12, marginTop: 4 }}>
              <div style={{ background: 'rgba(255,255,255,0.04)', padding: '10px 14px', borderRadius: 8, border: '1px solid rgba(255,255,255,0.08)' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 6, color: '#34d399', fontSize: 13, fontWeight: 700 }}>
                  <ShieldCheck size={16} />
                  <span>KRA eTIMS Ready</span>
                </div>
                <div style={{ fontSize: 11, color: '#94a3b8', marginTop: 2 }}>
                  Direct API fiscal integration
                </div>
              </div>

              <div style={{ background: 'rgba(255,255,255,0.04)', padding: '10px 14px', borderRadius: 8, border: '1px solid rgba(255,255,255,0.08)' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 6, color: '#34d399', fontSize: 13, fontWeight: 700 }}>
                  <Lock size={16} />
                  <span>Safaricom Daraja API</span>
                </div>
                <div style={{ fontSize: 11, color: '#94a3b8', marginTop: 2 }}>
                  Official STK & Till callbacks
                </div>
              </div>

              <div style={{ background: 'rgba(255,255,255,0.04)', padding: '10px 14px', borderRadius: 8, border: '1px solid rgba(255,255,255,0.08)' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 6, color: '#34d399', fontSize: 13, fontWeight: 700 }}>
                  <CheckCircle2 size={16} />
                  <span>CyberSource Visa/MC</span>
                </div>
                <div style={{ fontSize: 11, color: '#94a3b8', marginTop: 2 }}>
                  PCI-DSS Level 1 compliant card processing
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Bottom Bar */}
        <div style={{
          borderTop: '1px solid rgba(255,255,255,0.08)',
          paddingTop: 24,
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          flexWrap: 'wrap',
          gap: 14,
          fontSize: 13,
          color: '#64748b'
        }}>
          <div>
            &copy; {new Date().getFullYear()} Biashara360 Inc. All rights reserved. Crafted with ❤️ for Kenyan traders.
          </div>
          <div style={{ display: 'flex', gap: 20 }}>
            <Link to="/login" style={{ color: '#94a3b8' }}>Merchant Portal</Link>
            <Link to="/register" style={{ color: '#94a3b8' }}>Create Account</Link>
            <Link to="/downloads" style={{ color: '#94a3b8' }}>App Downloads</Link>
          </div>
        </div>
      </div>
    </footer>
  )
}

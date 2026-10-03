import React, { useState } from 'react'
import { Check, X, ArrowRight, Sparkles, HelpCircle, ShieldCheck } from 'lucide-react'
import { Link } from 'react-router-dom'

interface PricingProps {
  onOpenDemoModal: () => void
}

export default function PricingSection({ onOpenDemoModal }: PricingProps) {
  const [annualBilling, setAnnualBilling] = useState(true)
  const [showMatrix, setShowMatrix] = useState(false)

  return (
    <section className="m-pricing-section" id="pricing">
      <div className="m-container">
        {/* Header */}
        <div style={{ textAlign: 'center', maxWidth: 780, margin: '0 auto 20px' }}>
          <div className="m-badge m-badge-primary" style={{ marginBottom: 14 }}>
            <Sparkles size={14} />
            <span>Fair & Transparent Kenyan Pricing</span>
          </div>
          <h2 className="m-heading" style={{ fontSize: 'clamp(28px, 4vw, 42px)', color: '#0f172a', marginBottom: 16 }}>
            Simple Plans with Unbeatable Local Value
          </h2>
          <p style={{ color: '#64748b', fontSize: '17px' }}>
            No hidden setup fees. No surprise per-transaction percentages. Every plan includes a 14-day free trial.
          </p>

          {/* Billing Toggle */}
          <div className="m-billing-toggle">
            <button
              type="button"
              className={`m-toggle-btn ${!annualBilling ? 'active' : ''}`}
              onClick={() => setAnnualBilling(false)}
            >
              Monthly Billing
            </button>
            <button
              type="button"
              className={`m-toggle-btn ${annualBilling ? 'active' : ''}`}
              onClick={() => setAnnualBilling(true)}
            >
              Annual Billing <span style={{ color: '#059669', fontWeight: 800, fontSize: 11, marginLeft: 4 }}>Save 20%</span>
            </button>
          </div>
        </div>

        {/* Pricing Cards Grid */}
        <div className="m-pricing-grid">
          {/* Card 1: Starter */}
          <div className="m-pricing-card">
            <div style={{ marginBottom: 16 }}>
              <h3 style={{ fontSize: 20, fontWeight: 800, color: '#0f172a' }}>Starter</h3>
              <p style={{ fontSize: 13, color: '#64748b', marginTop: 4 }}>
                For solo traders, kiosks, and small boutiques getting started.
              </p>
            </div>

            <div style={{ margin: '14px 0 24px' }}>
              <div style={{ display: 'flex', alignItems: 'baseline', gap: 4 }}>
                <span style={{ fontSize: 16, fontWeight: 700, color: '#64748b' }}>KES</span>
                <span style={{ fontSize: 38, fontWeight: 800, color: '#0f172a', fontFamily: 'var(--m-font-heading)' }}>
                  {annualBilling ? '415' : '500'}
                </span>
                <span style={{ fontSize: 13, color: '#64748b' }}>/ month</span>
              </div>
              <div style={{ fontSize: 11, color: '#059669', fontWeight: 600, marginTop: 4 }}>
                {annualBilling ? 'Billed KES 5,000 annually (2 months free)' : 'Billed monthly'}
              </div>
            </div>

            <div style={{ fontSize: 13, fontWeight: 700, color: '#1e293b', marginBottom: 12 }}>
              What&apos;s included:
            </div>
            <ul style={{ listStyle: 'none', display: 'flex', flexDirection: 'column', gap: 10, marginBottom: 28, flex: 1 }}>
              <li style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 13, color: '#334155' }}>
                <Check size={16} color="#059669" />
                <span><strong>1 - 2 Cashiers</strong> / Users</span>
              </li>
              <li style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 13, color: '#334155' }}>
                <Check size={16} color="#059669" />
                <span>High-Speed POS & Barcode Checkout</span>
              </li>
              <li style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 13, color: '#334155' }}>
                <Check size={16} color="#059669" />
                <span>Lipa na M-Pesa STK Push</span>
              </li>
              <li style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 13, color: '#334155' }}>
                <Check size={16} color="#059669" />
                <span>Inventory Counts & Stock Alerts</span>
              </li>
              <li style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 13, color: '#334155' }}>
                <Check size={16} color="#059669" />
                <span>Digital e-Receipts (SMS / WhatsApp)</span>
              </li>
              <li style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 13, color: '#94a3b8' }}>
                <X size={16} color="#cbd5e1" />
                <span>KRA eTIMS Fiscalizer (Team plan)</span>
              </li>
            </ul>

            <Link
              to="/register?plan=STARTER"
              className="m-btn m-btn-secondary"
              style={{ width: '100%', justifyContent: 'center' }}
            >
              Start 14-Day Free Trial
            </Link>
          </div>

          {/* Card 2: Team (Featured) */}
          <div className="m-pricing-card featured">
            <div className="m-featured-ribbon">
              ★ Most Popular in Kenya
            </div>

            <div style={{ marginBottom: 16 }}>
              <h3 style={{ fontSize: 20, fontWeight: 800, color: '#0f172a' }}>Team</h3>
              <p style={{ fontSize: 13, color: '#64748b', marginTop: 4 }}>
                For busy retail shops, supermarkets, and growing stores.
              </p>
            </div>

            <div style={{ margin: '14px 0 24px' }}>
              <div style={{ display: 'flex', alignItems: 'baseline', gap: 4 }}>
                <span style={{ fontSize: 16, fontWeight: 700, color: '#64748b' }}>KES</span>
                <span style={{ fontSize: 38, fontWeight: 800, color: '#059669', fontFamily: 'var(--m-font-heading)' }}>
                  {annualBilling ? '830' : '1,000'}
                </span>
                <span style={{ fontSize: 13, color: '#64748b' }}>/ month</span>
              </div>
              <div style={{ fontSize: 11, color: '#059669', fontWeight: 600, marginTop: 4 }}>
                {annualBilling ? 'Billed KES 10,000 annually (2 months free)' : 'Billed monthly'}
              </div>
            </div>

            <div style={{ fontSize: 13, fontWeight: 700, color: '#1e293b', marginBottom: 12 }}>
              Everything in Starter, plus:
            </div>
            <ul style={{ listStyle: 'none', display: 'flex', flexDirection: 'column', gap: 10, marginBottom: 28, flex: 1 }}>
              <li style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 13, color: '#334155' }}>
                <Check size={16} color="#059669" />
                <span><strong>3 - 5 Cashiers</strong> / Staff</span>
              </li>
              <li style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 13, color: '#334155' }}>
                <Check size={16} color="#059669" />
                <span><strong>KRA eTIMS Tax Fiscalization</strong> & QR Codes</span>
              </li>
              <li style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 13, color: '#334155' }}>
                <Check size={16} color="#059669" />
                <span>Customer Debtors Ledger (&ldquo;Lipa Pole Pole&rdquo;)</span>
              </li>
              <li style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 13, color: '#334155' }}>
                <Check size={16} color="#059669" />
                <span>Bluetooth & USB Thermal Receipt Printing</span>
              </li>
              <li style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 13, color: '#334155' }}>
                <Check size={16} color="#059669" />
                <span>Cashier Shift Tracking & Blind Cash Drops</span>
              </li>
              <li style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 13, color: '#334155' }}>
                <Check size={16} color="#059669" />
                <span>Expense & Petty Cash Management</span>
              </li>
            </ul>

            <Link
              to="/register?plan=TEAM"
              className="m-btn m-btn-primary"
              style={{ width: '100%', justifyContent: 'center' }}
            >
              Start 14-Day Free Trial
            </Link>
          </div>

          {/* Card 3: Growth */}
          <div className="m-pricing-card">
            <div style={{ marginBottom: 16 }}>
              <h3 style={{ fontSize: 20, fontWeight: 800, color: '#0f172a' }}>Growth</h3>
              <p style={{ fontSize: 13, color: '#64748b', marginTop: 4 }}>
                For restaurants, bars, multi-branch operations, and wholesalers.
              </p>
            </div>

            <div style={{ margin: '14px 0 24px' }}>
              <div style={{ display: 'flex', alignItems: 'baseline', gap: 4 }}>
                <span style={{ fontSize: 16, fontWeight: 700, color: '#64748b' }}>KES</span>
                <span style={{ fontSize: 38, fontWeight: 800, color: '#0f172a', fontFamily: 'var(--m-font-heading)' }}>
                  {annualBilling ? '1,660' : '2,000'}
                </span>
                <span style={{ fontSize: 13, color: '#64748b' }}>/ month</span>
              </div>
              <div style={{ fontSize: 11, color: '#059669', fontWeight: 600, marginTop: 4 }}>
                {annualBilling ? 'Billed KES 20,000 annually (2 months free)' : 'Billed monthly'}
              </div>
            </div>

            <div style={{ fontSize: 13, fontWeight: 700, color: '#1e293b', marginBottom: 12 }}>
              Everything in Team, plus:
            </div>
            <ul style={{ listStyle: 'none', display: 'flex', flexDirection: 'column', gap: 10, marginBottom: 28, flex: 1 }}>
              <li style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 13, color: '#334155' }}>
                <Check size={16} color="#059669" />
                <span><strong>6 - 10 Cashiers</strong> / Waiters / Staff</span>
              </li>
              <li style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 13, color: '#334155' }}>
                <Check size={16} color="#059669" />
                <span><strong>Restaurant Table QR Ordering & KDS</strong></span>
              </li>
              <li style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 13, color: '#334155' }}>
                <Check size={16} color="#059669" />
                <span>Multi-Store Stock Transfers & Warehouse Control</span>
              </li>
              <li style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 13, color: '#334155' }}>
                <Check size={16} color="#059669" />
                <span>Purchase Orders & Supplier Invoicing (GRN)</span>
              </li>
              <li style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 13, color: '#334155' }}>
                <Check size={16} color="#059669" />
                <span>Security Audit Logs & Role Permissions</span>
              </li>
              <li style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 13, color: '#334155' }}>
                <Check size={16} color="#059669" />
                <span>Meta Social Selling (Instagram / Facebook DM)</span>
              </li>
            </ul>

            <Link
              to="/register?plan=GROWTH"
              className="m-btn m-btn-secondary"
              style={{ width: '100%', justifyContent: 'center' }}
            >
              Start 14-Day Free Trial
            </Link>
          </div>
        </div>

        {/* Enterprise Banner */}
        <div style={{
          marginTop: 40,
          background: 'linear-gradient(135deg, #0f172a 0%, #1e293b 100%)',
          borderRadius: 20,
          padding: '30px 36px',
          color: '#ffffff',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          flexWrap: 'wrap',
          gap: 20
        }}>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 6 }}>
              <ShieldCheck size={20} color="#10b981" />
              <span style={{ fontWeight: 800, fontSize: 18 }}>Need Multi-Branch Enterprise or Custom ERP Integration?</span>
            </div>
            <p style={{ color: '#94a3b8', fontSize: 14, maxWidth: 640 }}>
              Supporting 10+ locations, custom hardware bundles (Sunmi, Epson, Star), dedicated Safaricom Daraja API setups, and on-site cashier training.
            </p>
          </div>

          <button
            type="button"
            onClick={onOpenDemoModal}
            className="m-btn m-btn-primary"
            style={{ padding: '12px 24px', fontSize: 15 }}
          >
            <span>Talk to Enterprise Sales</span>
            <ArrowRight size={16} />
          </button>
        </div>
      </div>
    </section>
  )
}

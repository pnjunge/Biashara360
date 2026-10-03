import React, { useState } from 'react'
import { Calculator, TrendingUp, Clock, ShieldAlert, ArrowRight, CheckCircle2, DollarSign } from 'lucide-react'
import { Link } from 'react-router-dom'

export default function RoiCalculator() {
  const [monthlyRevenue, setMonthlyRevenue] = useState<number>(750000)
  const [dailyTxCount, setDailyTxCount] = useState<number>(80)
  const [staffCount, setStaffCount] = useState<number>(3)

  // Calculations
  // 1. Time saved: approx 1.2 minutes per transaction in manual SMS verification, receipt writing, and closing reconciliation
  const hoursSavedPerMonth = Math.round((dailyTxCount * 1.2 * 30) / 60)

  // 2. Shrinkage / stock theft prevented: estimated at conservative 1.8% of monthly turnover
  const shrinkageSavedPerMonth = Math.round(monthlyRevenue * 0.018)

  // 3. Fake M-Pesa SMS fraud eliminated: approx 0.4% of turnover saved
  const fraudPreventedPerMonth = Math.round(monthlyRevenue * 0.004)

  // Total financial value saved
  const totalFinancialBenefit = shrinkageSavedPerMonth + fraudPreventedPerMonth

  // Recommended plan cost
  const subscriptionCost = staffCount <= 2 ? 500 : staffCount <= 5 ? 1000 : 2000

  // Net monthly profit gain
  const netMonthlyGain = totalFinancialBenefit - subscriptionCost

  // ROI Percentage
  const roiPercentage = Math.round((netMonthlyGain / subscriptionCost) * 100)

  return (
    <section className="m-roi-section" id="calculator">
      <div className="m-container">
        {/* Section Header */}
        <div style={{ textAlign: 'center', maxWidth: 760, margin: '0 auto 48px' }}>
          <div className="m-badge m-badge-primary" style={{ marginBottom: 14 }}>
            <Calculator size={14} />
            <span>Interactive ROI & Value Calculator</span>
          </div>
          <h2 className="m-heading" style={{ fontSize: 'clamp(28px, 4vw, 42px)', color: '#0f172a', marginBottom: 14 }}>
            Calculate Your Return on Investment
          </h2>
          <p style={{ color: '#64748b', fontSize: '17px' }}>
            See how much time, shrinkage loss, and manual bookkeeping Biashara360 eliminates for your Kenyan business every single month.
          </p>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: 40, alignItems: 'center' }}>
          {/* Sliders on Left */}
          <div style={{ background: '#f8fafc', padding: '34px 28px', borderRadius: 24, border: '1px solid #e2e8f0' }}>
            {/* Slider 1: Monthly Revenue */}
            <div className="m-slider-container">
              <div className="m-slider-label">
                <span>Monthly Business Revenue</span>
                <span className="m-slider-value">KES {monthlyRevenue.toLocaleString()}</span>
              </div>
              <input
                type="range"
                min={100000}
                max={5000000}
                step={50000}
                value={monthlyRevenue}
                onChange={e => setMonthlyRevenue(Number(e.target.value))}
                className="m-range-input"
                aria-label="Monthly Business Revenue"
              />
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 11, color: '#94a3b8', marginTop: 4 }}>
                <span>KES 100K</span>
                <span>KES 2.5M</span>
                <span>KES 5M+</span>
              </div>
            </div>

            {/* Slider 2: Daily Transactions */}
            <div className="m-slider-container">
              <div className="m-slider-label">
                <span>Transactions per Day</span>
                <span className="m-slider-value">{dailyTxCount} sales / day</span>
              </div>
              <input
                type="range"
                min={15}
                max={500}
                step={5}
                value={dailyTxCount}
                onChange={e => setDailyTxCount(Number(e.target.value))}
                className="m-range-input"
                aria-label="Transactions per Day"
              />
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 11, color: '#94a3b8', marginTop: 4 }}>
                <span>15 / day</span>
                <span>250 / day</span>
                <span>500+ / day</span>
              </div>
            </div>

            {/* Slider 3: Cashiers / Staff */}
            <div className="m-slider-container">
              <div className="m-slider-label">
                <span>Cashiers & Staff Members</span>
                <span className="m-slider-value">{staffCount} Users</span>
              </div>
              <input
                type="range"
                min={1}
                max={15}
                step={1}
                value={staffCount}
                onChange={e => setStaffCount(Number(e.target.value))}
                className="m-range-input"
                aria-label="Staff Members"
              />
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 11, color: '#94a3b8', marginTop: 4 }}>
                <span>1 (Solo)</span>
                <span>5 (Team)</span>
                <span>15 (Growth)</span>
              </div>
            </div>

            <div style={{ background: '#ffffff', borderRadius: 12, padding: 14, border: '1px solid #e2e8f0', marginTop: 24, fontSize: 13, color: '#475569', display: 'flex', gap: 10, alignItems: 'center' }}>
              <CheckCircle2 size={18} color="#059669" style={{ flexShrink: 0 }} />
              <span>
                Recommended Plan: <strong>{staffCount <= 2 ? 'Starter (KES 500/mo)' : staffCount <= 5 ? 'Team (KES 1,000/mo)' : 'Growth (KES 2,000/mo)'}</strong>
              </span>
            </div>
          </div>

          {/* Results Card on Right */}
          <div className="m-roi-result-card">
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 18 }}>
              <TrendingUp size={22} color="#fde047" />
              <span style={{ fontSize: 13, fontWeight: 800, textTransform: 'uppercase', letterSpacing: '0.05em', color: '#fde047' }}>
                Estimated Monthly Value Unlocked
              </span>
            </div>

            <div style={{ marginBottom: 28 }}>
              <div style={{ fontSize: 14, color: '#a7f3d0' }}>Net Monthly Financial Benefit:</div>
              <div style={{ fontSize: 'clamp(36px, 5vw, 48px)', fontWeight: 800, fontFamily: 'var(--m-font-heading)', color: '#ffffff', lineHeight: 1.1 }}>
                +KES {netMonthlyGain.toLocaleString()}
                <span style={{ fontSize: 16, fontWeight: 500, color: '#a7f3d0' }}> / month</span>
              </div>
              <div style={{ display: 'inline-flex', alignItems: 'center', gap: 6, background: 'rgba(255,255,255,0.15)', padding: '4px 12px', borderRadius: 9999, fontSize: 13, fontWeight: 700, marginTop: 8 }}>
                🚀 {roiPercentage}% Return on Software Investment
              </div>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16, borderTop: '1px solid rgba(255,255,255,0.18)', paddingTop: 20 }}>
              <div>
                <div style={{ display: 'flex', alignItems: 'center', gap: 6, color: '#d1fae5', fontSize: 12 }}>
                  <Clock size={14} />
                  <span>Time Saved:</span>
                </div>
                <div style={{ fontSize: 20, fontWeight: 800, color: '#ffffff', marginTop: 2 }}>
                  ~{hoursSavedPerMonth} hours
                </div>
                <div style={{ fontSize: 11, color: '#a7f3d0', marginTop: 2 }}>No more manual ledger entries</div>
              </div>

              <div>
                <div style={{ display: 'flex', alignItems: 'center', gap: 6, color: '#d1fae5', fontSize: 12 }}>
                  <ShieldAlert size={14} />
                  <span>Theft & Leakage Cut:</span>
                </div>
                <div style={{ fontSize: 20, fontWeight: 800, color: '#ffffff', marginTop: 2 }}>
                  KES {shrinkageSavedPerMonth.toLocaleString()}
                </div>
                <div style={{ fontSize: 11, color: '#a7f3d0', marginTop: 2 }}>Real-time stock audit trails</div>
              </div>
            </div>

            <div style={{ marginTop: 28 }}>
              <Link
                to="/register"
                className="m-btn"
                style={{
                  width: '100%',
                  background: '#ffffff',
                  color: '#064e3b',
                  fontWeight: 800,
                  fontSize: 16,
                  boxShadow: '0 8px 24px rgba(0,0,0,0.18)'
                }}
              >
                <span>Claim Your 14-Day Free Trial</span>
                <ArrowRight size={16} />
              </Link>
              <div style={{ textAlign: 'center', fontSize: 12, color: '#d1fae5', marginTop: 10 }}>
                Instant activation • No credit card required • Cancel anytime
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  )
}

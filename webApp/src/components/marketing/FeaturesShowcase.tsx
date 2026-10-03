import React, { useState } from 'react'
import {
  Store,
  ShieldCheck,
  CreditCard,
  Package,
  UtensilsCrossed,
  Users,
  CheckCircle2,
  ArrowRight,
  Printer,
  WifiOff,
  QrCode,
  Smartphone,
  Layers,
  Sparkles
} from 'lucide-react'
import { Link } from 'react-router-dom'

interface FeaturePillar {
  id: string
  title: string
  badge: string
  shortDesc: string
  heading: string
  description: string
  bullets: string[]
  metric: { value: string; label: string }
  image: string
  tag: string
}

const PILLARS: FeaturePillar[] = [
  {
    id: 'pos',
    title: 'Smart POS Terminal',
    badge: 'High Speed Point of Sale',
    shortDesc: 'Instant touch checkout, barcode scanning, offline sync',
    heading: 'Designed for Lightning-Fast Checkout Lines',
    description: 'Whether you are running a bustling supermarket in downtown Nairobi or a high-turnover boutique, Biashara360 speeds up queue times with intuitive touch categories, instant barcode lookup, and one-tap tenders.',
    bullets: [
      'Works offline with zero downtime; auto-syncs the second internet reconnects',
      'Supports Bluetooth, USB, and Ethernet 58mm/80mm thermal receipt printers',
      'Flexible split tenders: M-Pesa + Cash + Visa in a single sale',
      'Cashier shift reconciliation with blind cash drops and tamper detection',
    ],
    metric: { value: '< 2.5s', label: 'Average checkout speed' },
    image: '/images/screen_pos.png',
    tag: 'RETAIL READY'
  },
  {
    id: 'etims',
    title: 'KRA eTIMS Fiscalizer',
    badge: '100% Tax Compliant',
    shortDesc: 'Cryptographic KRA QR codes without buying ESD boxes',
    heading: 'Instant KRA eTIMS Invoicing Without Expensive Hardware',
    description: 'Avoid the high cost of physical KRA ESD machines (KES 45,000+ each). Biashara360 integrates directly with Kenya Revenue Authority eTIMS cloud servers. Every receipt generates a valid KRA control number and scannable verification QR code.',
    bullets: [
      'Automatic tax classification (16% VAT, 0% Zero-rated, 8% Fuel, Exempt)',
      'Customer KRA PIN validation for legitimate B2B tax deductions',
      'Real-time cryptographic signing in under 300ms per sale',
      'Zero risk of non-compliance penalties or stressful audit discrepancies',
    ],
    metric: { value: '100%', label: 'KRA eTIMS Certification' },
    image: '/images/app_screenshot.png',
    tag: 'GOVERNMENT COMPLIANT'
  },
  {
    id: 'mpesa',
    title: 'M-Pesa & Card Settlements',
    badge: 'Direct Daraja API & CyberSource',
    shortDesc: 'STK push, Buy Goods Till reconciliation, Visa/Mastercard',
    heading: 'End M-Pesa Reconciliation Nightmares Forever',
    description: 'No more asking customers to show their phone screen or guessing fake SMS messages. Trigger a direct M-Pesa STK push to the buyer’s handset, or link your existing Buy Goods Till or Paybill for instant automated matching.',
    bullets: [
      'Instant Lipa na M-Pesa STK Push prompted right on customer’s phone',
      'Automated reconciliation against your Safaricom Till or Paybill',
      'Global card acceptance: Visa & Mastercard powered by CyberSource',
      'End-of-day financial reconciliation reports matching your bank deposits',
    ],
    metric: { value: '0 Error', label: 'In payment matching' },
    image: '/images/screen_dashboard.png',
    tag: 'PAYMENTS'
  },
  {
    id: 'inventory',
    title: 'Multi-Branch Inventory',
    badge: 'Stock & Supplier Control',
    shortDesc: 'Live counts, expiry alerts, low-stock reorders, COGS',
    heading: 'Real-Time Stock Accuracy Across All Branches & Warehouses',
    description: 'Gain total visibility over what’s selling and what’s leaking. Track purchase orders, supplier debts, cost of goods sold (COGS), batch expiry dates, and transfer inventory between multiple stores with full audit accountability.',
    bullets: [
      'Live stock count synchronization across multiple physical stores',
      'Automated low-stock SMS and email notifications before running out',
      'Batch tracking, manufacturing lot numbers, and perishable expiry alarms',
      'Purchase Orders (PO), Goods Received Notes (GRN), and supplier ledger',
    ],
    metric: { value: '99.8%', label: 'Stock tracking accuracy' },
    image: '/images/screen_stock.png',
    tag: 'INVENTORY'
  },
  {
    id: 'hospitality',
    title: 'Restaurant & Bar Suite',
    badge: 'Tables, KDS & QR Ordering',
    shortDesc: 'Visual floor plan, table QR menu, Kitchen Display System',
    heading: 'Run Your Restaurant, Lounge or Café with Precision',
    description: 'From self-service QR table ordering to real-time Kitchen Display Systems (KDS), Biashara360 eliminates lost paper tickets and delays. Waiters enter orders on mobile tablets, and food and drink tickets route to their stations instantly.',
    bullets: [
      'Contactless table QR ordering: guests scan, view menu, and order or pay',
      'Live Kitchen Display System (KDS) replacing noisy thermal kitchen tickets',
      'Open bar tabs, split bills, customer modifiers, and table floor layout',
      'Recipe costing and ingredient deduction per dish served',
    ],
    metric: { value: '45%', label: 'Faster food prep turnaround' },
    image: '/images/screen_orders.png',
    tag: 'HOSPITALITY'
  },
  {
    id: 'crm',
    title: 'Debtor Book & CRM',
    badge: 'Customer Loyalty & Debtors',
    shortDesc: 'Credit ledger, SMS payment reminders, online storefront',
    heading: 'Collect Customer Debts Faster & Build Brand Loyalty',
    description: 'Stop using tattered hardcopy credit notebooks. Track "Lipa Pole Pole" accounts with automated SMS payment reminders and direct M-Pesa pay links. Reward loyal shoppers with points and open your online shop in 60 seconds.',
    bullets: [
      'Digital debtors ledger with credit limits and outstanding balance alerts',
      'Automated SMS debt collection reminders with one-click M-Pesa payment links',
      'Custom customer loyalty points program and purchase histories',
      'Instant digital storefront (biashara360.co.ke/shop/your-brand) with Meta chat',
    ],
    metric: { value: '+35%', label: 'Faster debtor cash recovery' },
    image: '/images/screen_customers.png',
    tag: 'CRM & DEBTORS'
  },
]

export default function FeaturesShowcase() {
  const [activeId, setActiveId] = useState('pos')
  const currentPillar = PILLARS.find(p => p.id === activeId) || PILLARS[0]

  return (
    <section className="m-features-section" id="features">
      <div className="m-container">
        {/* Header */}
        <div style={{ textAlign: 'center', maxWidth: 820, margin: '0 auto 44px' }}>
          <div className="m-badge m-badge-primary" style={{ marginBottom: 14 }}>
            <Layers size={14} />
            <span>Comprehensive Business Architecture</span>
          </div>
          <h2 className="m-heading" style={{ fontSize: 'clamp(28px, 4vw, 42px)', color: '#0f172a', marginBottom: 16 }}>
            Everything You Need to Run & Scale in Kenya
          </h2>
          <p style={{ color: '#64748b', fontSize: '17px' }}>
            Built from the ground up for Kenyan business realities—slow internet, mobile money dominance, tax compliance mandates, and multi-tier retail.
          </p>
        </div>

        {/* Feature Tabs Bar */}
        <div className="m-feature-tabs">
          {PILLARS.map(pillar => (
            <button
              key={pillar.id}
              type="button"
              className={`m-feature-tab-pill ${activeId === pillar.id ? 'active' : ''}`}
              onClick={() => setActiveId(pillar.id)}
            >
              <span>{pillar.title}</span>
            </button>
          ))}
        </div>

        {/* Active Feature Card */}
        <div className="m-feature-content-card">
          {/* Details on Left */}
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 16 }}>
              <span className="m-badge m-badge-primary" style={{ fontSize: 12 }}>
                {currentPillar.badge}
              </span>
              <span style={{ fontSize: 12, fontWeight: 700, color: '#94a3b8' }}>
                {currentPillar.tag}
              </span>
            </div>

            <h3 className="m-heading" style={{ fontSize: 'clamp(24px, 3vw, 32px)', color: '#0f172a', marginBottom: 14 }}>
              {currentPillar.heading}
            </h3>

            <p style={{ color: '#475569', fontSize: 16, lineHeight: 1.6, marginBottom: 24 }}>
              {currentPillar.description}
            </p>

            {/* Bullets */}
            <div style={{ display: 'flex', flexDirection: 'column', gap: 12, marginBottom: 28 }}>
              {currentPillar.bullets.map((bullet, i) => (
                <div key={i} style={{ display: 'flex', alignItems: 'flex-start', gap: 10 }}>
                  <div style={{ width: 20, height: 20, borderRadius: '50%', background: '#ecfdf5', display: 'grid', placeItems: 'center', flexShrink: 0, marginTop: 2 }}>
                    <CheckCircle2 size={15} color="#059669" />
                  </div>
                  <span style={{ fontSize: 14, color: '#334155', fontWeight: 500 }}>
                    {bullet}
                  </span>
                </div>
              ))}
            </div>

            {/* Metric Banner & CTA */}
            <div style={{ display: 'flex', alignItems: 'center', gap: 20, flexWrap: 'wrap', paddingTop: 20, borderTop: '1px solid #e2e8f0' }}>
              <div>
                <div style={{ fontSize: 26, fontWeight: 800, color: '#059669', fontFamily: 'var(--m-font-heading)' }}>
                  {currentPillar.metric.value}
                </div>
                <div style={{ fontSize: 12, color: '#64748b' }}>
                  {currentPillar.metric.label}
                </div>
              </div>

              <Link
                to="/register"
                className="m-btn m-btn-primary"
                style={{ marginLeft: 'auto', padding: '10px 20px', fontSize: 14 }}
              >
                <span>Try this feature free</span>
                <ArrowRight size={15} />
              </Link>
            </div>
          </div>

          {/* Screenshot Preview on Right */}
          <div style={{ position: 'relative' }}>
            <div style={{
              background: '#0f172a',
              borderRadius: 20,
              padding: 12,
              boxShadow: '0 20px 40px rgba(15, 23, 42, 0.15)',
              border: '1px solid #e2e8f0',
              overflow: 'hidden'
            }}>
              <img
                src={currentPillar.image}
                alt={currentPillar.title}
                loading="lazy"
                decoding="async"
                style={{
                  width: '100%',
                  maxHeight: 460,
                  objectFit: 'contain',
                  borderRadius: 12,
                  display: 'block',
                  background: '#090e17'
                }}
              />
            </div>
          </div>
        </div>
      </div>
    </section>
  )
}

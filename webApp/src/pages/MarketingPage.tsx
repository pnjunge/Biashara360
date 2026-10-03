import React, { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import {
  Sparkles,
  ArrowRight,
  ShieldCheck,
  CheckCircle2,
  Play,
  Download,
  Smartphone,
  Monitor,
  Printer,
  ChevronDown,
  ChevronUp,
  Store,
  Utensils,
  Truck,
  Wrench,
  Pill,
  Scissors,
  MessageCircle,
  TrendingUp,
  CreditCard,
  Building2,
  FileCheck2,
  Check
} from 'lucide-react'
import '../styles/marketing.css'
import MarketingNav from '../components/marketing/MarketingNav'
import PosSimulator from '../components/marketing/PosSimulator'
import RoiCalculator from '../components/marketing/RoiCalculator'
import FeaturesShowcase from '../components/marketing/FeaturesShowcase'
import PricingSection from '../components/marketing/PricingSection'
import DemoModal from '../components/marketing/DemoModal'
import MarketingFooter from '../components/marketing/MarketingFooter'
import { useAuth } from '../App'
import { usePageSeo } from '../utils/usePageSeo'

interface FaqItem {
  question: string
  answer: string
}

const FAQS: FaqItem[] = [
  {
    question: 'Do I need a separate physical ESD machine for KRA eTIMS compliance?',
    answer: 'No! You do not need to buy an expensive physical Electronic Signature Device (ESD). Biashara360 integrates directly with the Kenya Revenue Authority (KRA) eTIMS cloud API. Every single sale generated through your Biashara360 POS is signed with an official cryptographic Control Unit Number and verifiable QR code.'
  },
  {
    question: 'What happens if our internet connection drops in the shop?',
    answer: 'Biashara360 is built with offline resilience. You can continue ringing up sales, scanning barcodes, taking cash or recording tenders, and printing customer receipts offline. As soon as your internet reconnects, all sales and inventory changes automatically synchronize with the cloud.'
  },
  {
    question: 'Can I connect my existing Safaricom Lipa na M-Pesa Till or Paybill?',
    answer: 'Yes! Biashara360 connects directly with your existing Buy Goods Till or Paybill number via official Safaricom Daraja APIs. When customers checkout, you can send an automated STK prompt directly to their phone, or have your till automatically match payments in real time without cashier guesswork.'
  },
  {
    question: 'What receipt printers and hardware devices work with Biashara360?',
    answer: 'Biashara360 is hardware-agnostic. It works seamlessly with any standard 58mm or 80mm ESC/POS thermal receipt printer (Bluetooth, USB, or Network/LAN), handheld Android POS terminals (Sunmi, iMin, Telpo), standard desktop barcode scanners, and cash drawers.'
  },
  {
    question: 'Can I monitor my shop’s sales from my phone while travelling?',
    answer: 'Absolutely. As the business owner, you can sign in to your Biashara360 dashboard from any smartphone, tablet, or laptop worldwide to view live sales figures, inventory alerts, cashier shifts, and profit reports in real time.'
  },
  {
    question: 'How do I migrate my existing products and stock into Biashara360?',
    answer: 'You can upload your entire product catalog in minutes using our Excel/CSV import template. Our customer success team in Nairobi is also available on WhatsApp to assist you with free catalog onboarding and setup.'
  },
  {
    question: 'How does the 14-day free trial work?',
    answer: 'You can register in less than 60 seconds with no credit card required. You get full access to all features (POS, Inventory, eTIMS, Reports). At the end of 14 days, you can choose any affordable monthly plan starting from just KES 500/month.'
  }
]

export default function MarketingPage() {
  const { isAuthenticated } = useAuth()
  const [demoModalOpen, setDemoModalOpen] = useState(false)
  const [expandedFaq, setExpandedFaq] = useState<number | null>(0)

  usePageSeo({
    title: 'Biashara360 — POS, KRA eTIMS & M-Pesa Platform for Kenyan Business',
    description: "Biashara360 is Kenya's leading all-in-one business software. Supercharge your retail store, supermarket, or restaurant with fast POS, real-time KRA eTIMS fiscalization, automated Safaricom M-Pesa STK, and multi-branch inventory control.",
    canonicalUrl: 'https://biashara360.co.ke/',
    keywords: 'Biashara360, POS Kenya, KRA eTIMS, eTIMS software, Lipa na M-Pesa STK, Safaricom Daraja, Point of Sale Nairobi, restaurant POS Kenya, inventory management Kenya'
  })

  const toggleFaq = (index: number) => {
    setExpandedFaq(expandedFaq === index ? null : index)
  }

  return (
    <div className="m-page" itemScope itemType="https://schema.org/WebPage">
      {/* ── 1. NAVIGATION BAR ── */}
      <MarketingNav onOpenDemoModal={() => setDemoModalOpen(true)} />

      <main id="main-content">
        {/* ── 2. HERO SECTION ── */}
        <section className="m-hero">
          <div className="m-hero-mesh" />
          <div className="m-hero-mesh-left" />

          <div className="m-container">
            <div className="m-hero-content">
              {/* Kenya Flag & Compliance Badge */}
              <div className="m-badge m-badge-primary">
                <span style={{ fontSize: 15 }}>🇰🇪</span>
                <span className="m-pulse-dot" />
                <span>Built for Kenya • KRA eTIMS Certified • Safaricom M-Pesa Integrated</span>
              </div>

              {/* Headline */}
              <h1 className="m-heading m-hero-title">
                The All-in-One Operating System for Modern{' '}
                <span className="m-text-gradient-emerald">Kenyan Commerce</span>
              </h1>

              {/* Subtitle */}
              <p className="m-hero-subtitle">
                Replace manual ledgers, slow tills, and tax audit headaches. Biashara360 unites high-speed POS, real-time KRA eTIMS fiscalization, automated M-Pesa reconciliation, and multi-branch inventory into one seamless platform.
              </p>

              {/* Action Buttons */}
              <div className="m-hero-actions">
                <Link
                  to="/register"
                  className="m-btn m-btn-primary"
                  style={{ padding: '15px 32px', fontSize: 16 }}
                  id="hero-free-trial-cta"
                >
                  <span>Start 14-Day Free Trial</span>
                  <ArrowRight size={18} />
                </Link>

                <a
                  href="#simulator"
                  className="m-btn m-btn-secondary"
                  style={{ padding: '15px 28px', fontSize: 16 }}
                  id="hero-live-demo-cta"
                >
                  <Play size={16} color="var(--m-primary)" fill="var(--m-primary)" />
                  <span>Test Live POS Simulator</span>
                </a>

                <button
                  type="button"
                  onClick={() => setDemoModalOpen(true)}
                  className="m-btn m-btn-outline-green"
                  style={{ padding: '15px 24px', fontSize: 15 }}
                  id="hero-book-demo-cta"
                >
                  <MessageCircle size={16} />
                  <span>Chat with Specialist</span>
                </button>
              </div>

              {/* Sub-CTA reassurance */}
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 20, flexWrap: 'wrap', fontSize: 13, color: '#64748b' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                  <CheckCircle2 size={16} color="#059669" />
                  <span>No Credit Card Required</span>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                  <CheckCircle2 size={16} color="#059669" />
                  <span>Works Online & Offline</span>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                  <CheckCircle2 size={16} color="#059669" />
                  <span>Zero ESD Box Needed</span>
                </div>
              </div>

              {/* ── Hero App Showcase Window ── */}
              <div className="m-hero-preview">
                <div className="m-preview-window">
                  <div className="m-preview-header">
                    <span className="m-mac-dot red" />
                    <span className="m-mac-dot yellow" />
                    <span className="m-mac-dot green" />
                    <div className="m-preview-url">https://biashara360.co.ke/dashboard</div>
                  </div>

                  <div style={{ position: 'relative' }}>
                    <img
                      src="/images/app_screenshot.png"
                      alt="Biashara360 Merchant POS and Inventory Management Dashboard"
                      className="m-preview-image"
                      width={1920}
                      height={1080}
                      loading="eager"
                      decoding="async"
                    />

                  {/* Floating Telemetry Badge 1: M-Pesa STK Confirmed */}
                  <div className="m-float-card m-float-card-1">
                    <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                      <div style={{ width: 36, height: 36, borderRadius: '50%', background: '#ecfdf5', display: 'grid', placeItems: 'center' }}>
                        <Smartphone size={20} color="#059669" />
                      </div>
                      <div style={{ textAlign: 'left' }}>
                        <div style={{ fontSize: 11, fontWeight: 700, color: '#047857' }}>
                          M-PESA STK CONFIRMED
                        </div>
                        <div style={{ fontSize: 14, fontWeight: 800, color: '#0f172a' }}>
                          KES 3,850.00
                        </div>
                        <div style={{ fontSize: 10, color: '#64748b' }}>
                          Till 882194 • 0s manual matching
                        </div>
                      </div>
                    </div>
                  </div>

                  {/* Floating Telemetry Badge 2: KRA eTIMS Validated */}
                  <div className="m-float-card m-float-card-2">
                    <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                      <div style={{ width: 36, height: 36, borderRadius: '50%', background: '#fef3c7', display: 'grid', placeItems: 'center' }}>
                        <ShieldCheck size={20} color="#b45309" />
                      </div>
                      <div style={{ textAlign: 'left' }}>
                        <div style={{ fontSize: 11, fontWeight: 700, color: '#b45309' }}>
                          KRA eTIMS FISCALIZED
                        </div>
                        <div style={{ fontSize: 13, fontWeight: 800, color: '#0f172a', fontFamily: 'monospace' }}>
                          CU-KRA-202610-8849
                        </div>
                        <div style={{ fontSize: 10, color: '#047857', fontWeight: 600 }}>
                          ✓ Cryptographic QR Validated
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            {/* Trust Metrics Bar */}
            <div className="m-trust-bar">
              <div className="m-trust-stat">
                <div className="m-trust-number">KES 500M+</div>
                <div className="m-trust-label">Transactions Processed</div>
              </div>
              <div className="m-trust-stat">
                <div className="m-trust-number">99.9%</div>
                <div className="m-trust-label">M-Pesa Reconciliation Accuracy</div>
              </div>
              <div className="m-trust-stat">
                <div className="m-trust-number">100%</div>
                <div className="m-trust-label">KRA eTIMS Compliant</div>
              </div>
              <div className="m-trust-stat">
                <div className="m-trust-number">1,200+</div>
                <div className="m-trust-label">Kenyan Merchants Powered</div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* ── 3. INTERACTIVE SIMULATOR (TEST DRIVE) ── */}
      <PosSimulator />

      {/* ── 4. 6 CORE PILLARS FEATURES SHOWCASE ── */}
      <FeaturesShowcase />

      {/* ── 5. INDUSTRY SOLUTIONS ── */}
      <section className="m-features-section" style={{ background: '#ffffff' }} id="solutions">
        <div className="m-container">
          <div style={{ textAlign: 'center', maxWidth: 780, margin: '0 auto 48px' }}>
            <div className="m-badge m-badge-primary" style={{ marginBottom: 14 }}>
              <Building2 size={14} />
              <span>Tailored for Every Kenyan Sector</span>
            </div>
            <h2 className="m-heading" style={{ fontSize: 'clamp(28px, 4vw, 42px)', color: '#0f172a', marginBottom: 14 }}>
              Designed for How Your Industry Works
            </h2>
            <p style={{ color: '#64748b', fontSize: '17px' }}>
              From fast-paced high-volume checkouts to table-side dining and credit distribution, Biashara360 adapts to your trade.
            </p>
          </div>

          <div style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(340px, 1fr))',
            gap: 24
          }}>
            {/* Sector 1: Supermarkets & Retail */}
            <div style={{ background: '#f8fafc', borderRadius: 20, padding: 30, border: '1px solid #e2e8f0', transition: 'transform 0.2s ease' }}>
              <div style={{ width: 44, height: 44, borderRadius: 12, background: '#ecfdf5', display: 'grid', placeItems: 'center', color: '#059669', marginBottom: 16 }}>
                <Store size={22} />
              </div>
              <h3 style={{ fontSize: 19, fontWeight: 800, color: '#0f172a', marginBottom: 10 }}>
                Supermarkets & Retail Stores
              </h3>
              <p style={{ fontSize: 14, color: '#475569', lineHeight: 1.6, marginBottom: 16 }}>
                High-speed barcode checkout, multiple cashier lanes, blind cash drawers, automated supplier purchase orders, and daily shrinkage reports.
              </p>
              <div style={{ fontSize: 13, color: '#059669', fontWeight: 700 }}>
                ✓ Barcode scanning • Blind cash drops • Fast M-Pesa STK
              </div>
            </div>

            {/* Sector 2: Hospitality & Dining */}
            <div style={{ background: '#f8fafc', borderRadius: 20, padding: 30, border: '1px solid #e2e8f0' }}>
              <div style={{ width: 44, height: 44, borderRadius: 12, background: '#fef3c7', display: 'grid', placeItems: 'center', color: '#b45309', marginBottom: 16 }}>
                <Utensils size={22} />
              </div>
              <h3 style={{ fontSize: 19, fontWeight: 800, color: '#0f172a', marginBottom: 10 }}>
                Restaurants, Bars & Lounges
              </h3>
              <p style={{ fontSize: 14, color: '#475569', lineHeight: 1.6, marginBottom: 16 }}>
                Table QR self-ordering, Kitchen Display System (KDS), split billing, open drink tabs, recipe costing, and real-time station ticket routing.
              </p>
              <div style={{ fontSize: 13, color: '#b45309', fontWeight: 700 }}>
                ✓ Table QR codes • Kitchen KDS • Open bar tabs
              </div>
            </div>

            {/* Sector 3: Wholesale & Distributors */}
            <div style={{ background: '#f8fafc', borderRadius: 20, padding: 30, border: '1px solid #e2e8f0' }}>
              <div style={{ width: 44, height: 44, borderRadius: 12, background: '#eff6ff', display: 'grid', placeItems: 'center', color: '#2563eb', marginBottom: 16 }}>
                <Truck size={22} />
              </div>
              <h3 style={{ fontSize: 19, fontWeight: 800, color: '#0f172a', marginBottom: 10 }}>
                Wholesalers & Distributors
              </h3>
              <p style={{ fontSize: 14, color: '#475569', lineHeight: 1.6, marginBottom: 16 }}>
                Bulk tier pricing, commercial B2B eTIMS invoices with customer KRA PINs, credit limit tracking, and multi-warehouse distribution.
              </p>
              <div style={{ fontSize: 13, color: '#2563eb', fontWeight: 700 }}>
                ✓ Tiered pricing • B2B eTIMS tax invoices • Credit limits
              </div>
            </div>

            {/* Sector 4: Hardware & Agrovets */}
            <div style={{ background: '#f8fafc', borderRadius: 20, padding: 30, border: '1px solid #e2e8f0' }}>
              <div style={{ width: 44, height: 44, borderRadius: 12, background: '#fdf2f8', display: 'grid', placeItems: 'center', color: '#db2777', marginBottom: 16 }}>
                <Wrench size={22} />
              </div>
              <h3 style={{ fontSize: 19, fontWeight: 800, color: '#0f172a', marginBottom: 10 }}>
                Hardware & Agrovets
              </h3>
              <p style={{ fontSize: 14, color: '#475569', lineHeight: 1.6, marginBottom: 16 }}>
                Track thousands of SKUs, unit conversions (meters, bags, pieces), serial numbers, supplier warranties, and customer contractor accounts.
              </p>
              <div style={{ fontSize: 13, color: '#db2777', fontWeight: 700 }}>
                ✓ Fractional units • Contractor accounts • Serial tracking
              </div>
            </div>

            {/* Sector 5: Pharmacies & Chemists */}
            <div style={{ background: '#f8fafc', borderRadius: 20, padding: 30, border: '1px solid #e2e8f0' }}>
              <div style={{ width: 44, height: 44, borderRadius: 12, background: '#f0fdf4', display: 'grid', placeItems: 'center', color: '#16a34a', marginBottom: 16 }}>
                <Pill size={22} />
              </div>
              <h3 style={{ fontSize: 19, fontWeight: 800, color: '#0f172a', marginBottom: 10 }}>
                Pharmacies & Chemists
              </h3>
              <p style={{ fontSize: 14, color: '#475569', lineHeight: 1.6, marginBottom: 16 }}>
                Expiry date monitoring, batch number tracking, prescription records, doctor notes, and zero-rated medical tax handling.
              </p>
              <div style={{ fontSize: 13, color: '#16a34a', fontWeight: 700 }}>
                ✓ Expiry date alerts • Batch control • 0% VAT medical
              </div>
            </div>

            {/* Sector 6: Salons, Spas & Services */}
            <div style={{ background: '#f8fafc', borderRadius: 20, padding: 30, border: '1px solid #e2e8f0' }}>
              <div style={{ width: 44, height: 44, borderRadius: 12, background: '#faf5ff', display: 'grid', placeItems: 'center', color: '#9333ea', marginBottom: 16 }}>
                <Scissors size={22} />
              </div>
              <h3 style={{ fontSize: 19, fontWeight: 800, color: '#0f172a', marginBottom: 10 }}>
                Salons, Spas & Boutiques
              </h3>
              <p style={{ fontSize: 14, color: '#475569', lineHeight: 1.6, marginBottom: 16 }}>
                Appointment booking, stylist commission calculation, customer loyalty profiles, and retail product sales in one combined till.
              </p>
              <div style={{ fontSize: 13, color: '#9333ea', fontWeight: 700 }}>
                ✓ Staff commissions • Appointments • Loyalty points
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* ── 6. HARDWARE COMPATIBILITY SECTION ── */}
      <section style={{ padding: '80px 0', background: '#090e17', color: '#ffffff' }}>
        <div className="m-container">
          <div style={{ textAlign: 'center', maxWidth: 760, margin: '0 auto 44px' }}>
            <div className="m-badge m-badge-dark" style={{ marginBottom: 14 }}>
              <Monitor size={14} color="#10B981" />
              <span>Zero Proprietary Hardware Lock-In</span>
            </div>
            <h2 className="m-heading" style={{ fontSize: 'clamp(28px, 4vw, 40px)', color: '#ffffff', marginBottom: 14 }}>
              Works Seamlessly on What You Already Own
            </h2>
            <p style={{ color: '#94a3b8', fontSize: '16px' }}>
              No need to buy overpriced locked-in registers. Biashara360 runs smoothly on standard Android phones, tablets, laptops, Windows PCs, and thermal printers.
            </p>
          </div>

          <div style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
            gap: 20
          }}>
            <div style={{ background: 'rgba(255,255,255,0.04)', borderRadius: 16, padding: 24, border: '1px solid rgba(255,255,255,0.08)', textAlign: 'center' }}>
              <Smartphone size={36} color="#34d399" style={{ margin: '0 auto 14px' }} />
              <h4 style={{ fontSize: 16, fontWeight: 700, color: '#ffffff', marginBottom: 6 }}>Android Phones & Tablets</h4>
              <p style={{ fontSize: 13, color: '#94a3b8' }}>
                Use any phone running Android 7.0+. Perfect for mobile cashiering and delivery riders.
              </p>
            </div>

            <div style={{ background: 'rgba(255,255,255,0.04)', borderRadius: 16, padding: 24, border: '1px solid rgba(255,255,255,0.08)', textAlign: 'center' }}>
              <Monitor size={36} color="#34d399" style={{ margin: '0 auto 14px' }} />
              <h4 style={{ fontSize: 16, fontWeight: 700, color: '#ffffff', marginBottom: 6 }}>Laptops & Windows PCs</h4>
              <p style={{ fontSize: 13, color: '#94a3b8' }}>
                Windows 10/11, macOS, and Linux desktop apps for high-traffic cashier checkout lanes.
              </p>
            </div>

            <div style={{ background: 'rgba(255,255,255,0.04)', borderRadius: 16, padding: 24, border: '1px solid rgba(255,255,255,0.08)', textAlign: 'center' }}>
              <Printer size={36} color="#34d399" style={{ margin: '0 auto 14px' }} />
              <h4 style={{ fontSize: 16, fontWeight: 700, color: '#ffffff', marginBottom: 6 }}>58mm & 80mm Printers</h4>
              <p style={{ fontSize: 13, color: '#94a3b8' }}>
                ESC/POS Bluetooth, USB, and LAN receipt printers (Epson, Xprinter, Sunmi, Star).
              </p>
            </div>

            <div style={{ background: 'rgba(255,255,255,0.04)', borderRadius: 16, padding: 24, border: '1px solid rgba(255,255,255,0.08)', textAlign: 'center' }}>
              <ShieldCheck size={36} color="#34d399" style={{ margin: '0 auto 14px' }} />
              <h4 style={{ fontSize: 16, fontWeight: 700, color: '#ffffff', marginBottom: 6 }}>POS Terminals & Scanners</h4>
              <p style={{ fontSize: 13, color: '#94a3b8' }}>
                Sunmi V2, iMin, Telpo smart Android POS terminals, USB laser barcode readers & cash drawers.
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* ── 7. ROI & TIME-SAVED CALCULATOR ── */}
      <RoiCalculator />

      {/* ── 8. PRICING PLANS ── */}
      <PricingSection onOpenDemoModal={() => setDemoModalOpen(true)} />



      {/* ── 10. INTERACTIVE FAQ ACCORDION ── */}
      <section className="m-faq-section" id="faq">
        <div className="m-container" style={{ maxWidth: 840 }}>
          <div style={{ textAlign: 'center', marginBottom: 44 }}>
            <div className="m-badge m-badge-primary" style={{ marginBottom: 14 }}>
              <span>Got Questions?</span>
            </div>
            <h2 className="m-heading" style={{ fontSize: 'clamp(28px, 4vw, 40px)', color: '#0f172a', marginBottom: 12 }}>
              Frequently Asked Questions
            </h2>
            <p style={{ color: '#64748b', fontSize: '16px' }}>
              Everything you need to know about getting started, hardware, KRA eTIMS, and Lipa na M-Pesa.
            </p>
          </div>

          <div>
            {FAQS.map((faq, idx) => {
              const isOpen = expandedFaq === idx
              return (
                <div key={idx} className="m-faq-item">
                  <button
                    type="button"
                    className="m-faq-question"
                    onClick={() => toggleFaq(idx)}
                    aria-expanded={isOpen}
                  >
                    <span>{faq.question}</span>
                    {isOpen ? <ChevronUp size={18} color="#059669" /> : <ChevronDown size={18} color="#94a3b8" />}
                  </button>
                  {isOpen && (
                    <div className="m-faq-answer">
                      {faq.answer}
                    </div>
                  )}
                </div>
              )
            })}
          </div>

          <div style={{ textAlign: 'center', marginTop: 32, fontSize: 14, color: '#64748b' }}>
            Have a question that is not answered here?{' '}
            <button
              type="button"
              onClick={() => setDemoModalOpen(true)}
              style={{ color: '#059669', fontWeight: 700, textDecoration: 'underline', background: 'none' }}
            >
              Speak directly with our support team in Nairobi
            </button>
          </div>
        </div>
      </section>

      {/* ── 11. MULTI-PLATFORM DOWNLOADS & CTA BANNER ── */}
      <section style={{
        background: 'linear-gradient(135deg, #064e3b 0%, #047857 60%, #059669 100%)',
        color: '#ffffff',
        padding: '80px 0',
        position: 'relative',
        overflow: 'hidden'
      }}>
        <div className="m-container" style={{ textAlign: 'center', position: 'relative', zIndex: 1, maxWidth: 840 }}>
          <div className="m-badge" style={{ background: 'rgba(255,255,255,0.18)', color: '#ffffff', border: '1px solid rgba(255,255,255,0.3)', marginBottom: 18 }}>
            <Download size={14} />
            <span>Available on Android, Desktop & Web</span>
          </div>

          <h2 className="m-heading" style={{ fontSize: 'clamp(30px, 4.5vw, 46px)', color: '#ffffff', marginBottom: 18 }}>
            Ready to Supercharge Your Kenyan Business?
          </h2>

          <p style={{ fontSize: 18, color: '#d1fae5', maxWidth: 650, margin: '0 auto 34px', lineHeight: 1.6 }}>
            Join over 1,200 forward-thinking retailers and restaurateurs who eliminated stock leakages, fast-tracked M-Pesa checkouts, and secured full KRA eTIMS peace of mind.
          </p>

          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 16, flexWrap: 'wrap', marginBottom: 26 }}>
            <Link
              to="/register"
              className="m-btn"
              style={{
                background: '#ffffff',
                color: '#064e3b',
                padding: '16px 36px',
                fontSize: 16,
                fontWeight: 800,
                boxShadow: '0 8px 24px rgba(0,0,0,0.2)'
              }}
              id="cta-bottom-register"
            >
              <span>Start 14-Day Free Trial</span>
              <ArrowRight size={18} />
            </Link>

            <Link
              to="/downloads"
              className="m-btn m-btn-dark"
              style={{ padding: '16px 28px', fontSize: 16 }}
              id="cta-bottom-downloads"
            >
              <Smartphone size={18} />
              <span>Download Android APK</span>
            </Link>
          </div>

          <div style={{ fontSize: 13, color: '#a7f3d0' }}>
            Instant setup in under 2 minutes • Cancel anytime • Free WhatsApp support
          </div>
        </div>
      </section>
      </main>

      {/* ── 12. FOOTER ── */}
      <MarketingFooter />

      {/* ── 13. FLOATING WHATSAPP BUTTON ── */}
      <a
        href="https://wa.me/254700360360?text=Hello%20Biashara360!%20I%20would%20like%20to%20learn%20more%20about%20your%20POS%20and%20KRA%20eTIMS%20system."
        target="_blank"
        rel="noopener noreferrer"
        className="m-whatsapp-float"
        aria-label="Chat with Biashara360 Sales on WhatsApp"
        title="Chat with us on WhatsApp"
      >
        <MessageCircle size={28} />
      </a>

      {/* ── 14. LEAD CAPTURE & DEMO MODAL ── */}
      <DemoModal isOpen={demoModalOpen} onClose={() => setDemoModalOpen(false)} />
    </div>
  )
}

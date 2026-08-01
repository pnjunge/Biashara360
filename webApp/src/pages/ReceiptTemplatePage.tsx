import React, { useState, useEffect } from 'react'
import { PageHeader, Card, Btn, Input } from '../components/ui'
import { businessApi } from '../services/api'
import { ImagePlus, Printer, Trash2 } from 'lucide-react'
import { printOrderReceipt } from '../utils/receipt'

export default function ReceiptTemplatePage() {
  const [header, setHeader] = useState('Welcome to our store!')
  const [footer, setFooter] = useState('Thank you for shopping with us!')
  const [showTax, setShowTax] = useState(true)
  const [showCustomer, setShowCustomer] = useState(true)
  const [logo, setLogo] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [saved, setSaved] = useState(false)
  const [saving, setSaving] = useState(false)
  const [errorMsg, setErrorMsg] = useState('')

  // Store whole profile so we can send it back on update
  const [profile, setProfile] = useState<any>(null)

  useEffect(() => {
    businessApi.getProfile()
      .then(res => {
        if (res.success && res.data) {
          setProfile(res.data)
          setHeader(res.data.receiptHeader || 'Welcome to our store!')
          setFooter(res.data.receiptFooter || 'Thank you for shopping with us!')
          setShowTax(res.data.receiptShowTax !== false)
          setShowCustomer(res.data.receiptShowCustomer !== false)
          setLogo(res.data.receiptLogo || null)
        }
      })
      .catch(() => {
        setErrorMsg('Failed to load business profile')
      })
      .finally(() => setLoading(false))
  }, [])

  const handleSave = async () => {
    if (!profile) return
    setSaving(true)
    setErrorMsg('')
    try {
      const res = await businessApi.updateProfile({
        ...profile,
        receiptHeader: header,
        receiptFooter: footer,
        receiptLogo: logo,
        receiptShowTax: showTax,
        receiptShowCustomer: showCustomer
      })
      if (res.success) {
        setSaved(true)
        setTimeout(() => setSaved(false), 3000)
      } else {
        setErrorMsg(res.message || 'Failed to save template')
      }
    } catch (e: any) {
      setErrorMsg(e.response?.data?.message || 'Network error')
    } finally {
      setSaving(false)
    }
  }

  const handleLogo = (file?: File) => {
    if (!file) return
    if (!['image/png', 'image/jpeg', 'image/webp'].includes(file.type)) {
      setErrorMsg('Logo must be a PNG, JPEG, or WebP image.')
      return
    }
    if (file.size > 500 * 1024) {
      setErrorMsg('Logo must be smaller than 500 KB.')
      return
    }
    const reader = new FileReader()
    reader.onload = () => { setLogo(String(reader.result)); setErrorMsg('') }
    reader.readAsDataURL(file)
  }

  const testPrint = () => {
    if (!profile) return
    printOrderReceipt({
      id: 'preview', orderNumber: 'PREVIEW-001', businessId: profile.id, customerId: null,
      customerName: 'John Doe', customerPhone: '+254 711 222 333', deliveryLocation: 'In-Store POS',
      items: [
        { id: '1', productId: '1', productName: "Men's Slim Fit Jeans", quantity: 1, unitPrice: 2500, buyingPrice: 0, lineTotal: 2500, lineProfit: 0 },
        { id: '2', productId: '2', productName: 'Casual Cotton Shirt', quantity: 2, unitPrice: 1200, buyingPrice: 0, lineTotal: 2400, lineProfit: 0 },
      ], paymentStatus: 'PAID', deliveryStatus: 'DELIVERED', paymentMethod: 'CASH', mpesaTransactionCode: null,
      baseAmount: 4900, taxIncluded: true, taxRate: 0.16, taxAmount: 784, subtotal: 5684,
      salesChannel: 'WEB', notes: '', createdAt: new Date().toISOString(), updatedAt: new Date().toISOString(),
    }, { ...profile, receiptHeader: header, receiptFooter: footer, receiptLogo: logo, receiptShowTax: showTax, receiptShowCustomer: showCustomer })
  }

  if (loading) {
    return <div style={{ padding: 32, textAlign: 'center', color: 'var(--b360-text-secondary)' }}>Loading settings…</div>
  }

  return (
    <div className="fade-in">
      <PageHeader
        title="Receipt Template Customization"
        action={
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            {saved && <span style={{ fontSize: 12, color: 'var(--b360-green)', fontWeight: 600 }}>✓ Saved successfully</span>}
            <Btn variant="secondary" icon={<Printer size={14} />} onClick={testPrint} disabled={!profile}>Test Print</Btn>
            <Btn onClick={handleSave} disabled={saving}>{saving ? 'Saving…' : 'Save Template'}</Btn>
          </div>
        }
      />

      {errorMsg && (
        <div style={{ padding: 12, background: 'var(--b360-red-bg)', color: 'var(--b360-red)', borderRadius: 8, fontSize: 13, fontWeight: 600, marginBottom: 16 }}>
          {errorMsg}
        </div>
      )}

      <div style={{ display: 'grid', gridTemplateColumns: '1.2fr 1fr', gap: 24, alignItems: 'start' }}>
        {/* Editor Form */}
        <Card style={{ padding: 24 }}>
          <h3 style={{ fontWeight: 700, marginBottom: 18, fontSize: 15 }}>Receipt Parameters</h3>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            <div>
              <label style={{ fontSize: 12, fontWeight: 600, color: 'var(--b360-text-secondary)', display: 'block', marginBottom: 6 }}>Business Logo</label>
              <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                <div style={{ width: 88, height: 64, border: '1px dashed var(--b360-border)', borderRadius: 8, display: 'flex', alignItems: 'center', justifyContent: 'center', overflow: 'hidden', background: 'var(--b360-surface)' }}>
                  {logo ? <img src={logo} alt="Receipt logo" style={{ maxWidth: '100%', maxHeight: '100%', objectFit: 'contain' }} /> : <ImagePlus size={22} color="var(--b360-text-secondary)" />}
                </div>
                <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
                  <label className="btn" style={{ cursor: 'pointer', padding: '8px 12px', border: '1px solid var(--b360-border)', borderRadius: 8, fontSize: 12, fontWeight: 600 }}>
                    Choose image<input type="file" accept="image/png,image/jpeg,image/webp" hidden onChange={event => handleLogo(event.target.files?.[0])} />
                  </label>
                  {logo && <button type="button" onClick={() => setLogo(null)} style={{ border: 0, background: 'transparent', color: 'var(--b360-red)', cursor: 'pointer', fontSize: 12, display: 'flex', alignItems: 'center', gap: 4 }}><Trash2 size={12} /> Remove</button>}
                </div>
              </div>
              <div style={{ fontSize: 11, color: 'var(--b360-text-secondary)', marginTop: 5 }}>PNG, JPEG, or WebP; maximum 500 KB.</div>
            </div>
            <Input
              label="Header Message"
              value={header}
              onChange={setHeader}
              placeholder="e.g. Welcome to our store!"
            />
            
            <Input
              label="Footer Note"
              value={footer}
              onChange={setFooter}
              placeholder="e.g. Thank you for shopping with us!"
            />

            <div style={{ borderTop: '1px solid var(--b360-border)', paddingTop: 16, display: 'flex', flexDirection: 'column', gap: 14 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div>
                  <span style={{ fontSize: 13, fontWeight: 600, display: 'block' }}>Show KRA Tax Breakdown</span>
                  <span style={{ fontSize: 11, color: 'var(--b360-text-secondary)' }}>Include VAT (16%) details on thermal receipts</span>
                </div>
                <div onClick={() => setShowTax(!showTax)} style={{
                  width: 44, height: 24, borderRadius: 12, cursor: 'pointer', transition: 'background 0.2s',
                  background: showTax ? 'var(--b360-green)' : '#D1D5DB', position: 'relative'
                }}>
                  <div style={{ position: 'absolute', top: 2, left: showTax ? 22 : 2, width: 20, height: 20, borderRadius: '50%', background: 'white', transition: 'left 0.2s', boxShadow: '0 1px 3px rgba(0,0,0,0.2)' }} />
                </div>
              </div>

              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderTop: '1px solid var(--b360-border)', paddingTop: 14 }}>
                <div>
                  <span style={{ fontSize: 13, fontWeight: 600, display: 'block' }}>Show Customer Details</span>
                  <span style={{ fontSize: 11, color: 'var(--b360-text-secondary)' }}>Include buyer name and phone on receipt header</span>
                </div>
                <div onClick={() => setShowCustomer(!showCustomer)} style={{
                  width: 44, height: 24, borderRadius: 12, cursor: 'pointer', transition: 'background 0.2s',
                  background: showCustomer ? 'var(--b360-green)' : '#D1D5DB', position: 'relative'
                }}>
                  <div style={{ position: 'absolute', top: 2, left: showCustomer ? 22 : 2, width: 20, height: 20, borderRadius: '50%', background: 'white', transition: 'left 0.2s', boxShadow: '0 1px 3px rgba(0,0,0,0.2)' }} />
                </div>
              </div>
            </div>
          </div>
        </Card>

        {/* Live Thermal Receipt Preview */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
          <h4 style={{ fontSize: 13, fontWeight: 700, color: 'var(--b360-text-secondary)', textTransform: 'uppercase', letterSpacing: 0.5 }}>Live Receipt Preview</h4>
          
          <div style={{
            background: '#FFFFF0',
            border: '1px solid #E2E8F0',
            boxShadow: '0 4px 6px -1px rgba(0,0,0,0.05), 0 2px 4px -1px rgba(0,0,0,0.03)',
            fontFamily: 'monospace',
            fontSize: 12,
            color: '#1A202C',
            padding: '24px 20px',
            borderRadius: 4,
            backgroundImage: 'radial-gradient(#E2E8F0 1px, transparent 0)',
            backgroundSize: '8px 8px'
          }}>
            <div style={{ textAlign: 'center', marginBottom: 12 }}>
              {logo && <img src={logo} alt="Business logo" style={{ display: 'block', maxWidth: 150, maxHeight: 72, objectFit: 'contain', margin: '0 auto 8px' }} />}
              <span style={{ fontWeight: 'bold', fontSize: 14, textTransform: 'uppercase' }}>{profile?.name || 'BIASHARA STORE'}</span>
              <div style={{ fontSize: 10, color: '#4A5568', marginTop: 2 }}>
                <div>{profile?.address || '123 Tom Mboya St'}</div>
                <div>{profile?.county || 'Nairobi'}, Kenya</div>
                <div>Tel: {profile?.phone || '+254 700 000 000'}</div>
                {profile?.kraPin && <div>PIN: {profile?.kraPin}</div>}
              </div>
            </div>

            <div style={{ borderTop: '1px dashed #A0AEC0', margin: '8px 0' }} />

            {showCustomer && (
              <div style={{ fontSize: 10, color: '#4A5568', marginBottom: 6 }}>
                <div>CUSTOMER: John Doe</div>
                <div>PHONE: +254 711 222 333</div>
                <div style={{ borderTop: '1px dashed #A0AEC0', marginTop: 6 }} />
              </div>
            )}

            <div style={{ display: 'flex', justifyContent: 'space-between', fontWeight: 'bold', fontSize: 11 }}>
              <span>ITEM</span>
              <span>QTY * PRICE = TOTAL</span>
            </div>
            
            <div style={{ borderTop: '1px dashed #A0AEC0', margin: '4px 0' }} />

            <div style={{ display: 'flex', flexDirection: 'column', gap: 4, fontSize: 11 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span>Men's Slim Fit Jeans</span>
                <span>1 * 2,500.00 = 2,500.00</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span>Casual Cotton Shirt</span>
                <span>2 * 1,200.00 = 2,400.00</span>
              </div>
            </div>

            <div style={{ borderTop: '1px dashed #A0AEC0', margin: '8px 0' }} />

            <div style={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span>SUBTOTAL:</span>
                <span>KES 4,900.00</span>
              </div>
              
              {showTax && (
                <>
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 10, color: '#4A5568' }}>
                    <span>VAT (16% INCL):</span>
                    <span>KES 675.86</span>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 10, color: '#4A5568' }}>
                    <span>NET EXCL TAX:</span>
                    <span>KES 4,224.14</span>
                  </div>
                </>
              )}

              <div style={{ display: 'flex', justifyContent: 'space-between', fontWeight: 'bold', fontSize: 13, marginTop: 4 }}>
                <span>TOTAL:</span>
                <span>KES 4,900.00</span>
              </div>
            </div>

            <div style={{ borderTop: '1px dashed #A0AEC0', margin: '8px 0 12px 0' }} />

            <div style={{ textAlign: 'center', fontSize: 11, fontWeight: 'bold', fontStyle: 'italic' }}>
              <div>{header}</div>
              <div style={{ marginTop: 4 }}>{footer}</div>
            </div>

            <div style={{ textAlign: 'center', fontSize: 9, color: '#718096', marginTop: 12 }}>
              <div>eTIMS Invoice: #INV-2026-0091</div>
              <div>Powered by Biashara360</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}

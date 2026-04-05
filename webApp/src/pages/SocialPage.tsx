import React, { useState, useRef, useEffect } from 'react'
import {
  MessageCircle, Send, Zap, ShoppingCart, CreditCard, X, Check,
  RefreshCw, ChevronDown, Search, Settings, Circle, ArrowRight,
  Phone, MapPin, Package, AlertCircle, Wifi, Link, Copy
} from 'lucide-react'

// ── Brand Colors ──────────────────────────────────────────────────────────────
const PLATFORM = {
  WHATSAPP:  { color: '#25D366', bg: '#E8FBF0', label: 'WhatsApp',  icon: '💬' },
  INSTAGRAM: { color: '#E1306C', bg: '#FDE8F0', label: 'Instagram', icon: '📸' },
  FACEBOOK:  { color: '#1877F2', bg: '#E7F0FE', label: 'Facebook',  icon: '👥' },
  TIKTOK:    { color: '#000000', bg: '#F0F0F0', label: 'TikTok',    icon: '🎵' },
}
const G = '#1B8B34'

// ── Sample Data ───────────────────────────────────────────────────────────────
const SAMPLE_CONVS = [
  { id:'c1', platform:'WHATSAPP',  channelName:'Biashara360 WA',    customerName:'Amina Wanjiru',   customerPhone:'+254712345678', status:'OPEN',            unreadCount:3, lastMessage:'Hii unga inauzwa bei gani?', lastMessageAt:'2026-03-08T14:30:00Z', isAiHandled:false, assignedOrderId:null },
  { id:'c2', platform:'INSTAGRAM', channelName:'@biashara360ke',    customerName:'Kevin Omondi',    customerPhone:null,            status:'PENDING_PAYMENT', unreadCount:0, lastMessage:'Sawa, nitapeleka M-Pesa sasa', lastMessageAt:'2026-03-08T13:55:00Z', isAiHandled:true,  assignedOrderId:'ord123' },
  { id:'c3', platform:'FACEBOOK',  channelName:'Biashara360 Page',  customerName:'Grace Muthoni',   customerPhone:'+254798765432', status:'OPEN',            unreadCount:1, lastMessage:'Do you do deliveries to Nakuru?', lastMessageAt:'2026-03-08T12:20:00Z', isAiHandled:false, assignedOrderId:null },
  { id:'c4', platform:'TIKTOK',    channelName:'@biashara360',      customerName:'TikTok User 902', customerPhone:null,            status:'COMPLETED',       unreadCount:0, lastMessage:'Asante sana! Order imefika 🙏', lastMessageAt:'2026-03-08T10:00:00Z', isAiHandled:true,  assignedOrderId:'ord119' },
  { id:'c5', platform:'WHATSAPP',  channelName:'Biashara360 WA',    customerName:'Peter Kamau',     customerPhone:'+254711223344', status:'OPEN',            unreadCount:2, lastMessage:'Mnafungua saa ngapi?', lastMessageAt:'2026-03-08T09:45:00Z', isAiHandled:false, assignedOrderId:null },
  { id:'c6', platform:'INSTAGRAM', channelName:'@biashara360ke',    customerName:'Sharon Achieng',  customerPhone:null,            status:'OPEN',            unreadCount:4, lastMessage:'I saw your post, how much for 2 bags?', lastMessageAt:'2026-03-08T09:10:00Z', isAiHandled:false, assignedOrderId:null },
]

const MESSAGES: Record<string, any[]> = {
  c1: [
    { id:'m1', direction:'INBOUND',  senderType:'CUSTOMER', content:'Habari! Mnauza unga wa dhahabu?', messageType:'TEXT', createdAt:'2026-03-08T14:20:00Z', isAiGenerated:false },
    { id:'m2', direction:'OUTBOUND', senderType:'AI',       content:'Habari yako! Ndiyo, tunazo unga wa dhahabu. Bei ni KES 180 kwa kilo 2, KES 320 kwa kilo 5. Ungependa kuagiza kiasi gani? 😊', messageType:'TEXT', createdAt:'2026-03-08T14:21:00Z', isAiGenerated:true },
    { id:'m3', direction:'INBOUND',  senderType:'CUSTOMER', content:'Hii unga inauzwa bei gani kwa debe?', messageType:'TEXT', createdAt:'2026-03-08T14:30:00Z', isAiGenerated:false },
  ],
  c2: [
    { id:'m4', direction:'INBOUND',  senderType:'CUSTOMER', content:'Ninaomba order ya 3 bottles za cooking oil', messageType:'TEXT', createdAt:'2026-03-08T13:30:00Z', isAiGenerated:false },
    { id:'m5', direction:'OUTBOUND', senderType:'AI',       content:'Asante Kevin! Cooking oil ya KES 195 × 3 = KES 585 total. Nitakutumia maelekezo ya kulipa sasa hivi! 🛍️', messageType:'TEXT', createdAt:'2026-03-08T13:31:00Z', isAiGenerated:true },
    { id:'m6', direction:'OUTBOUND', senderType:'AGENT',    content:'Hujambo Kevin! 🛍️\n\n*Order ya Cooking Oil × 3*\n💰 Jumla: *KES 585*\n\n💳 *Lipa kwa Mpesa:*\nPaybill: 174379\nAccount: ORD-2026-0122\nKiasi: KES 585\n\nAsante kwa kununua! 🙏', messageType:'PAYMENT_REQUEST', createdAt:'2026-03-08T13:32:00Z', isAiGenerated:false },
    { id:'m7', direction:'INBOUND',  senderType:'CUSTOMER', content:'Sawa, nitapeleka M-Pesa sasa', messageType:'TEXT', createdAt:'2026-03-08T13:55:00Z', isAiGenerated:false },
  ],
  c3: [
    { id:'m8', direction:'INBOUND',  senderType:'CUSTOMER', content:'Do you do deliveries to Nakuru?', messageType:'TEXT', createdAt:'2026-03-08T12:20:00Z', isAiGenerated:false },
  ],
  c4: [
    { id:'m9',  direction:'INBOUND',  senderType:'CUSTOMER', content:'Naomba order ya sugar 2kg na rice 5kg', messageType:'TEXT', createdAt:'2026-03-08T08:00:00Z', isAiGenerated:false },
    { id:'m10', direction:'OUTBOUND', senderType:'AI',       content:'Sawa! Sugar 2kg (KES 280) + Rice 5kg (KES 620) = KES 900. Nikusaidie kumaliza order? 😊', messageType:'TEXT', createdAt:'2026-03-08T08:01:00Z', isAiGenerated:true },
    { id:'m11', direction:'INBOUND',  senderType:'CUSTOMER', content:'Asante sana! Order imefika 🙏', messageType:'TEXT', createdAt:'2026-03-08T10:00:00Z', isAiGenerated:false },
  ],
  c5: [
    { id:'m12', direction:'INBOUND', senderType:'CUSTOMER', content:'Mnafungua saa ngapi?', messageType:'TEXT', createdAt:'2026-03-08T09:45:00Z', isAiGenerated:false },
    { id:'m13', direction:'INBOUND', senderType:'CUSTOMER', content:'Na mnafunga saa ngapi jioni?', messageType:'TEXT', createdAt:'2026-03-08T09:46:00Z', isAiGenerated:false },
  ],
  c6: [
    { id:'m14', direction:'INBOUND', senderType:'CUSTOMER', content:'I saw your post, how much for 2 bags?', messageType:'TEXT', createdAt:'2026-03-08T09:10:00Z', isAiGenerated:false },
    { id:'m15', direction:'INBOUND', senderType:'CUSTOMER', content:'Can you deliver to Westlands?', messageType:'TEXT', createdAt:'2026-03-08T09:11:00Z', isAiGenerated:false },
    { id:'m16', direction:'INBOUND', senderType:'CUSTOMER', content:'What are your payment options?', messageType:'TEXT', createdAt:'2026-03-08T09:12:00Z', isAiGenerated:false },
    { id:'m17', direction:'INBOUND', senderType:'CUSTOMER', content:'I saw your post, how much for 2 bags?', messageType:'TEXT', createdAt:'2026-03-08T09:13:00Z', isAiGenerated:false },
  ],
}

const CHANNELS_DATA = [
  { id:'ch1', platform:'WHATSAPP',  channelName:'Biashara360 WA',   externalId:'254700000001', phoneNumber:'+254700000001', isActive:true,  autoReplyEnabled:true,  webhookVerifyToken:'abc123tok', webhookUrl:'https://api.biashara360.co.ke/v1/social/webhook/whatsapp', unreadCount:5 },
  { id:'ch2', platform:'INSTAGRAM', channelName:'@biashara360ke',   externalId:'17841400000001', phoneNumber:null,          isActive:true,  autoReplyEnabled:true,  webhookVerifyToken:'def456tok', webhookUrl:'https://api.biashara360.co.ke/v1/social/webhook/instagram', unreadCount:4 },
  { id:'ch3', platform:'FACEBOOK',  channelName:'Biashara360 Page', externalId:'100000000001',  phoneNumber:null,          isActive:true,  autoReplyEnabled:false, webhookVerifyToken:'ghi789tok', webhookUrl:'https://api.biashara360.co.ke/v1/social/webhook/facebook',  unreadCount:1 },
  { id:'ch4', platform:'TIKTOK',    channelName:'@biashara360',     externalId:'tt_openid_001', phoneNumber:null,          isActive:false, autoReplyEnabled:false, webhookVerifyToken:'jkl012tok', webhookUrl:'https://api.biashara360.co.ke/v1/social/webhook/tiktok/ch4', unreadCount:0 },
]

// ── Helpers ───────────────────────────────────────────────────────────────────
function timeAgo(iso: string) {
  const diff = Date.now() - new Date(iso).getTime()
  if (diff < 60000)   return 'Just now'
  if (diff < 3600000) return `${Math.floor(diff/60000)}m ago`
  if (diff < 86400000) return `${Math.floor(diff/3600000)}h ago`
  return `${Math.floor(diff/86400000)}d ago`
}
function fmtTime(iso: string) {
  return new Date(iso).toLocaleTimeString('en-KE', { hour:'2-digit', minute:'2-digit' })
}

// ── Platform Badge ────────────────────────────────────────────────────────────
function PlatformBadge({ platform, size = 'sm' }: { platform: string; size?: 'sm'|'md' }) {
  const p = PLATFORM[platform as keyof typeof PLATFORM]
  if (!p) return null
  const pad = size === 'md' ? '4px 12px' : '2px 8px'
  const fs  = size === 'md' ? 12 : 10
  return (
    <span style={{ background:p.bg, color:p.color, borderRadius:20, padding:pad, fontSize:fs, fontWeight:700, display:'inline-flex', alignItems:'center', gap:4 }}>
      {p.icon} {p.label}
    </span>
  )
}

// ── Status Dot ────────────────────────────────────────────────────────────────
function StatusDot({ status }: { status: string }) {
  const colors: Record<string, string> = { OPEN:'#1B8B34', PENDING_PAYMENT:'#FF8F00', COMPLETED:'#1565C0', CLOSED:'#999' }
  return <Circle size={8} fill={colors[status]||'#999'} color={colors[status]||'#999'} />
}

// ── Tab: Inbox ────────────────────────────────────────────────────────────────
function InboxTab() {
  const [convs, setConvs]         = useState(SAMPLE_CONVS)
  const [active, setActive]       = useState<string | null>('c1')
  const [msgs, setMsgs]           = useState(MESSAGES)
  const [draft, setDraft]         = useState('')
  const [filterPlatform, setFP]   = useState('ALL')
  const [filterStatus, setFS]     = useState('ALL')
  const [search, setSearch]       = useState('')
  const [aiLoading, setAiLoading] = useState(false)
  const [aiSuggestion, setAiSug]  = useState<string | null>(null)
  const [showPayModal, setPayMod] = useState(false)
  const [showOrderModal, setOrdMod] = useState(false)
  const [payAmt, setPayAmt]       = useState('')
  const [payDesc, setPayDesc]     = useState('')
  const [copied, setCopied]       = useState(false)
  const msgEnd = useRef<HTMLDivElement>(null)

  useEffect(() => { msgEnd.current?.scrollIntoView({ behavior:'smooth' }) }, [active, msgs])

  const activeConv = convs.find(c => c.id === active)
  const activeMsgs = active ? (msgs[active] || []) : []

  const filtered = convs.filter(c => {
    if (filterPlatform !== 'ALL' && c.platform !== filterPlatform) return false
    if (filterStatus   !== 'ALL' && c.status   !== filterStatus)   return false
    if (search && !c.customerName.toLowerCase().includes(search.toLowerCase()) &&
        !c.lastMessage.toLowerCase().includes(search.toLowerCase())) return false
    return true
  })

  function send() {
    if (!draft.trim() || !active) return
    const msg = { id: `m${Date.now()}`, direction:'OUTBOUND', senderType:'AGENT', content:draft, messageType:'TEXT', createdAt:new Date().toISOString(), isAiGenerated:false }
    setMsgs(m => ({ ...m, [active]: [...(m[active]||[]), msg] }))
    setConvs(cs => cs.map(c => c.id === active ? {...c, lastMessage:draft, lastMessageAt:new Date().toISOString()} : c))
    setDraft('')
    setAiSug(null)
  }

  function getAiReply() {
    if (!activeConv) return
    setAiLoading(true)
    setAiSug(null)
    setTimeout(() => {
      const lastCustomerMsg = [...activeMsgs].reverse().find(m => m.direction === 'INBOUND')?.content || ''
      const suggestions: Record<string, string> = {
        'bei gani': `Habari! ${activeConv.customerName.split(' ')[0]} 😊 Bei yetu ni:\n• Unga 2kg - KES 180\n• Unga 5kg - KES 320\n• Unga debe - KES 1,200\n\nUngependa kuagiza?`,
        'delivery': `Ndiyo, tunafanya delivery Nairobi yote na Kenya! Delivery ya Nairobi ni KES 150. Unataka tuwasilishe wapi? 📦`,
        'how much': `Hi ${activeConv.customerName.split(' ')[0]}! 😊 Our prices:\n• 2kg bag - KES 180\n• 5kg bag - KES 320\n\nShall I prepare an order for you?`,
        'payment': `Tunakubali M-Pesa (Paybill 174379), Cash, na Card. Unataka kulipa kwa njia gani? 💳`,
        'saa ngapi': `Tunafungua 7:00 asubuhi hadi 9:00 usiku kila siku! 🕖 Je, ungependa kuagiza kitu?`,
      }
      const key = Object.keys(suggestions).find(k => lastCustomerMsg.toLowerCase().includes(k))
      const reply = key ? suggestions[key] : `Habari ${activeConv.customerName.split(' ')[0]}! Asante kwa kuwasiliana nasi. Ninawezaje kukusaidia leo? 😊`
      setAiSug(reply)
      setAiLoading(false)
    }, 1200)
  }

  function sendAiReply() {
    if (!aiSuggestion || !active) return
    const msg = { id: `m${Date.now()}`, direction:'OUTBOUND', senderType:'AI', content:aiSuggestion, messageType:'TEXT', createdAt:new Date().toISOString(), isAiGenerated:true }
    setMsgs(m => ({ ...m, [active]: [...(m[active]||[]), msg] }))
    setConvs(cs => cs.map(c => c.id === active ? {...c, lastMessage:aiSuggestion, lastMessageAt:new Date().toISOString(), isAiHandled:true} : c))
    setAiSug(null)
  }

  function sendPayment() {
    if (!active || !payAmt) return
    const amt = parseFloat(payAmt)
    const payMsg = `Hujambo ${activeConv?.customerName.split(' ')[0]}! 🛍️\n\n*${payDesc || 'Order yako'}*\n💰 Jumla: *KES ${amt.toLocaleString()}*\n\n💳 *Lipa kwa Mpesa:*\nPaybill: 174379\nAccount: ORD-${Date.now().toString().slice(-4)}\nKiasi: KES ${amt.toLocaleString()}\n\n📱 Au piga simu: ${activeConv?.customerPhone || '0700000000'}\n\nAsante kwa kununua! 🙏`
    const msg = { id:`m${Date.now()}`, direction:'OUTBOUND', senderType:'AGENT', content:payMsg, messageType:'PAYMENT_REQUEST', createdAt:new Date().toISOString(), isAiGenerated:false }
    setMsgs(m => ({ ...m, [active]: [...(m[active]||[]), msg] }))
    setConvs(cs => cs.map(c => c.id === active ? {...c, status:'PENDING_PAYMENT', lastMessage:payMsg.slice(0,60), lastMessageAt:new Date().toISOString()} : c))
    setPayAmt(''); setPayDesc(''); setPayMod(false)
  }

  const totalUnread = convs.reduce((s, c) => s + c.unreadCount, 0)

  return (
    <div style={{ display:'flex', height:'calc(100vh - 120px)', background:'#F4F7F5', borderRadius:16, overflow:'hidden', border:'1px solid #E5E9E7' }}>

      {/* ── Left Panel: Conversation List ──────────────────────────────── */}
      <div style={{ width:340, flexShrink:0, display:'flex', flexDirection:'column', background:'white', borderRight:'1px solid #E8EDE9' }}>

        {/* Search + Filter */}
        <div style={{ padding:'14px 14px 10px' }}>
          <div style={{ position:'relative', marginBottom:10 }}>
            <Search size={14} color='#999' style={{ position:'absolute', left:10, top:'50%', transform:'translateY(-50%)' }} />
            <input value={search} onChange={e => setSearch(e.target.value)} placeholder="Search conversations…"
              style={{ width:'100%', paddingLeft:32, paddingRight:10, paddingTop:8, paddingBottom:8, borderRadius:10, border:'1px solid #E8EDE9', fontSize:12, outline:'none', boxSizing:'border-box' }} />
          </div>
          <div style={{ display:'flex', gap:4, overflowX:'auto', paddingBottom:4 }}>
            {['ALL','WHATSAPP','INSTAGRAM','FACEBOOK','TIKTOK'].map(p => {
              const meta = PLATFORM[p as keyof typeof PLATFORM]
              return (
                <button key={p} onClick={() => setFP(p)} style={{
                  flexShrink:0, padding:'4px 10px', borderRadius:20, border:'none', cursor:'pointer', fontSize:11, fontWeight:600,
                  background: filterPlatform === p ? (meta?.color || G) : '#F0F3F0',
                  color:       filterPlatform === p ? 'white' : '#666'
                }}>
                  {meta ? `${meta.icon} ${meta.label}` : 'All'}
                </button>
              )
            })}
          </div>
        </div>

        <div style={{ flex:1, overflowY:'auto' }}>
          {filtered.length === 0 && (
            <div style={{ padding:30, textAlign:'center', color:'#999', fontSize:13 }}>No conversations found</div>
          )}
          {filtered.map(c => {
            const p    = PLATFORM[c.platform as keyof typeof PLATFORM]
            const isAct = c.id === active
            return (
              <div key={c.id} onClick={() => setActive(c.id)} style={{
                padding:'12px 14px', cursor:'pointer', position:'relative',
                background: isAct ? '#F0F8F1' : 'white',
                borderLeft: isAct ? `3px solid ${G}` : '3px solid transparent',
                borderBottom: '1px solid #F5F5F5',
              }}>
                {/* Avatar */}
                <div style={{ display:'flex', gap:10, alignItems:'flex-start' }}>
                  <div style={{ width:40, height:40, borderRadius:'50%', background:p?.bg||'#EEE', display:'flex', alignItems:'center', justifyContent:'center', fontSize:18, flexShrink:0, position:'relative' }}>
                    {c.customerName.charAt(0)}
                    <span style={{ position:'absolute', bottom:-2, right:-2, fontSize:12 }}>{p?.icon}</span>
                  </div>
                  <div style={{ flex:1, minWidth:0 }}>
                    <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center', marginBottom:2 }}>
                      <span style={{ fontWeight:700, fontSize:13, color:'#1A1A1A', overflow:'hidden', textOverflow:'ellipsis', whiteSpace:'nowrap', maxWidth:140 }}>{c.customerName}</span>
                      <span style={{ fontSize:10, color:'#AAA', flexShrink:0 }}>{timeAgo(c.lastMessageAt)}</span>
                    </div>
                    <div style={{ fontSize:11, color:'#777', overflow:'hidden', textOverflow:'ellipsis', whiteSpace:'nowrap', marginBottom:4 }}>
                      {c.lastMessage}
                    </div>
                    <div style={{ display:'flex', alignItems:'center', gap:6 }}>
                      <StatusDot status={c.status} />
                      <span style={{ fontSize:10, color:'#AAA' }}>{c.status.replace('_',' ')}</span>
                      {c.isAiHandled && <span style={{ fontSize:10, background:'#E8F5E9', color:G, padding:'1px 6px', borderRadius:10 }}>AI</span>}
                    </div>
                  </div>
                  {c.unreadCount > 0 && (
                    <div style={{ background:p?.color||G, color:'white', borderRadius:'50%', width:18, height:18, fontSize:10, fontWeight:800, display:'flex', alignItems:'center', justifyContent:'center', flexShrink:0 }}>
                      {c.unreadCount}
                    </div>
                  )}
                </div>
              </div>
            )
          })}
        </div>
      </div>

      {/* ── Right Panel: Chat ─────────────────────────────────────────────── */}
      {activeConv ? (
        <div style={{ flex:1, display:'flex', flexDirection:'column', minWidth:0 }}>

          {/* Chat header */}
          <div style={{ padding:'14px 20px', background:'white', borderBottom:'1px solid #E8EDE9', display:'flex', alignItems:'center', justifyContent:'space-between' }}>
            <div style={{ display:'flex', alignItems:'center', gap:12 }}>
              <div style={{ width:42, height:42, borderRadius:'50%', background:PLATFORM[activeConv.platform as keyof typeof PLATFORM]?.bg, display:'flex', alignItems:'center', justifyContent:'center', fontSize:20 }}>
                {activeConv.customerName.charAt(0)}
              </div>
              <div>
                <div style={{ fontWeight:800, fontSize:15 }}>{activeConv.customerName}</div>
                <div style={{ display:'flex', alignItems:'center', gap:8, marginTop:2 }}>
                  <PlatformBadge platform={activeConv.platform} />
                  {activeConv.customerPhone && <span style={{ fontSize:11, color:'#888' }}>{activeConv.customerPhone}</span>}
                  <StatusDot status={activeConv.status} />
                  <span style={{ fontSize:11, color:'#888' }}>{activeConv.status.replace('_',' ')}</span>
                </div>
              </div>
            </div>
            <div style={{ display:'flex', gap:8 }}>
              <button onClick={() => setOrdMod(true)} style={{ padding:'7px 14px', borderRadius:8, border:`1.5px solid ${G}`, background:'white', color:G, fontSize:12, fontWeight:700, cursor:'pointer', display:'flex', alignItems:'center', gap:5 }}>
                <ShoppingCart size={13} /> Order
              </button>
              <button onClick={() => setPayMod(true)} style={{ padding:'7px 14px', borderRadius:8, border:'none', background:G, color:'white', fontSize:12, fontWeight:700, cursor:'pointer', display:'flex', alignItems:'center', gap:5 }}>
                <CreditCard size={13} /> Request Payment
              </button>
            </div>
          </div>

          {/* Messages */}
          <div style={{ flex:1, overflowY:'auto', padding:'16px 20px', display:'flex', flexDirection:'column', gap:8 }}>
            {activeMsgs.map(msg => {
              const isOut = msg.direction === 'OUTBOUND'
              const isPayment = msg.messageType === 'PAYMENT_REQUEST'
              return (
                <div key={msg.id} style={{ display:'flex', justifyContent: isOut ? 'flex-end' : 'flex-start' }}>
                  <div style={{
                    maxWidth:'72%', padding:'10px 14px', borderRadius: isOut ? '16px 4px 16px 16px' : '4px 16px 16px 16px',
                    background: isPayment ? '#E8F5E9' : isOut ? G : 'white',
                    color: isOut ? 'white' : '#1A1A1A',
                    border: isPayment ? `1.5px solid ${G}` : isOut ? 'none' : '1px solid #F0F0F0',
                    boxShadow: '0 1px 4px rgba(0,0,0,0.08)',
                  }}>
                    {isPayment && <div style={{ fontSize:11, fontWeight:700, color:G, marginBottom:6, display:'flex', alignItems:'center', gap:4 }}><CreditCard size={12}/> Payment Request</div>}
                    <div style={{ fontSize:13, lineHeight:1.5, whiteSpace:'pre-wrap' }}>{msg.content}</div>
                    <div style={{ display:'flex', alignItems:'center', gap:6, marginTop:5, justifyContent:'flex-end' }}>
                      {msg.isAiGenerated && <span style={{ fontSize:9, opacity:0.7, background:'rgba(255,255,255,0.3)', padding:'1px 5px', borderRadius:8 }}>AI</span>}
                      <span style={{ fontSize:10, opacity:0.7 }}>{fmtTime(msg.createdAt)}</span>
                    </div>
                  </div>
                </div>
              )
            })}
            <div ref={msgEnd} />
          </div>

          {/* AI Suggestion strip */}
          {aiSuggestion && (
            <div style={{ margin:'0 16px', padding:'12px 16px', background:'#F0F8F1', border:`1px solid ${G}`, borderRadius:12, display:'flex', gap:10, alignItems:'flex-start' }}>
              <Zap size={14} color={G} style={{ flexShrink:0, marginTop:2 }} />
              <div style={{ flex:1 }}>
                <div style={{ fontSize:11, fontWeight:700, color:G, marginBottom:4 }}>AI Suggested Reply</div>
                <div style={{ fontSize:12, color:'#333', whiteSpace:'pre-wrap', lineHeight:1.5 }}>{aiSuggestion}</div>
              </div>
              <div style={{ display:'flex', gap:6, flexShrink:0 }}>
                <button onClick={() => { setDraft(aiSuggestion); setAiSug(null) }} style={{ padding:'5px 10px', borderRadius:7, border:`1px solid ${G}`, background:'white', color:G, fontSize:11, fontWeight:600, cursor:'pointer' }}>Edit</button>
                <button onClick={sendAiReply} style={{ padding:'5px 10px', borderRadius:7, border:'none', background:G, color:'white', fontSize:11, fontWeight:700, cursor:'pointer' }}>Send</button>
                <button onClick={() => setAiSug(null)} style={{ padding:'5px 8px', borderRadius:7, border:'1px solid #DDD', background:'white', color:'#999', fontSize:11, cursor:'pointer' }}><X size={11}/></button>
              </div>
            </div>
          )}

          {/* Compose Bar */}
          <div style={{ padding:'12px 16px 16px', background:'white', borderTop:'1px solid #E8EDE9' }}>
            <div style={{ display:'flex', gap:8, alignItems:'flex-end' }}>
              <button onClick={getAiReply} disabled={aiLoading} title="Get AI reply suggestion" style={{ padding:'10px 12px', borderRadius:10, border:`1.5px solid ${G}`, background: aiLoading ? '#F0F8F1' : 'white', color:G, cursor:'pointer', flexShrink:0, display:'flex', alignItems:'center', gap:5 }}>
                {aiLoading ? <RefreshCw size={14} className="spin" /> : <Zap size={14} />}
                <span style={{ fontSize:11, fontWeight:700 }}>{aiLoading ? 'Thinking…' : 'AI Reply'}</span>
              </button>
              <textarea
                value={draft}
                onChange={e => setDraft(e.target.value)}
                onKeyDown={e => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); send() } }}
                placeholder={`Reply to ${activeConv.customerName}…`}
                rows={2}
                style={{ flex:1, padding:'10px 14px', borderRadius:10, border:'1.5px solid #E8EDE9', fontSize:13, resize:'none', outline:'none', fontFamily:'inherit', lineHeight:1.5 }}
              />
              <button onClick={send} disabled={!draft.trim()} style={{ padding:'10px 16px', borderRadius:10, border:'none', background: draft.trim() ? G : '#E8EDE9', color: draft.trim() ? 'white' : '#AAA', cursor: draft.trim() ? 'pointer' : 'default', flexShrink:0, display:'flex', alignItems:'center', gap:5, fontWeight:700, fontSize:13 }}>
                <Send size={14} /> Send
              </button>
            </div>
          </div>
        </div>
      ) : (
        <div style={{ flex:1, display:'flex', alignItems:'center', justifyContent:'center', flexDirection:'column', gap:12, color:'#999' }}>
          <MessageCircle size={48} strokeWidth={1} />
          <div style={{ fontSize:15, fontWeight:600 }}>Select a conversation</div>
          <div style={{ fontSize:12 }}>Choose a conversation from the left to start replying</div>
        </div>
      )}

      {/* ── Payment Modal ──────────────────────────────────────────────── */}
      {showPayModal && (
        <div style={{ position:'fixed', inset:0, background:'rgba(0,0,0,0.5)', display:'flex', alignItems:'center', justifyContent:'center', zIndex:1000 }}>
          <div style={{ background:'white', borderRadius:16, padding:28, width:420, boxShadow:'0 20px 60px rgba(0,0,0,0.3)' }}>
            <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center', marginBottom:20 }}>
              <div style={{ fontWeight:800, fontSize:17 }}>Request Payment</div>
              <button onClick={() => setPayMod(false)} style={{ background:'none', border:'none', cursor:'pointer' }}><X size={18}/></button>
            </div>
            <div style={{ marginBottom:14 }}>
              <label style={{ fontSize:12, fontWeight:600, display:'block', marginBottom:5, color:'#666' }}>Customer</label>
              <div style={{ padding:'10px 12px', background:'#F8F8F8', borderRadius:8, fontSize:13 }}>{activeConv?.customerName} · <PlatformBadge platform={activeConv?.platform||''}/></div>
            </div>
            <div style={{ marginBottom:14 }}>
              <label style={{ fontSize:12, fontWeight:600, display:'block', marginBottom:5, color:'#666' }}>Amount (KES) *</label>
              <input value={payAmt} onChange={e => setPayAmt(e.target.value)} placeholder="e.g. 850" type="number"
                style={{ width:'100%', padding:'10px 12px', borderRadius:8, border:'1.5px solid #E0E0E0', fontSize:15, fontWeight:700, outline:'none', boxSizing:'border-box' }} />
            </div>
            <div style={{ marginBottom:20 }}>
              <label style={{ fontSize:12, fontWeight:600, display:'block', marginBottom:5, color:'#666' }}>Description</label>
              <input value={payDesc} onChange={e => setPayDesc(e.target.value)} placeholder="e.g. Unga 2kg × 3"
                style={{ width:'100%', padding:'10px 12px', borderRadius:8, border:'1.5px solid #E0E0E0', fontSize:13, outline:'none', boxSizing:'border-box' }} />
            </div>
            {payAmt && (
              <div style={{ marginBottom:20, padding:14, background:'#F0F8F1', borderRadius:10, border:`1px solid ${G}`, fontSize:12 }}>
                <div style={{ fontWeight:700, color:G, marginBottom:6 }}>Preview message:</div>
                <div style={{ color:'#333', lineHeight:1.6 }}>Hujambo {activeConv?.customerName.split(' ')[0]}! 🛍️<br/><b>{payDesc||'Order yako'}</b><br/>💰 KES {parseFloat(payAmt||'0').toLocaleString()}<br/>💳 Lipa Mpesa Paybill 174379…</div>
              </div>
            )}
            <div style={{ display:'flex', gap:10 }}>
              <button onClick={() => setPayMod(false)} style={{ flex:1, padding:'11px', borderRadius:10, border:'1.5px solid #E0E0E0', background:'white', fontSize:13, fontWeight:600, cursor:'pointer' }}>Cancel</button>
              <button onClick={sendPayment} disabled={!payAmt} style={{ flex:2, padding:'11px', borderRadius:10, border:'none', background:payAmt ? G : '#E0E0E0', color:'white', fontSize:13, fontWeight:800, cursor:payAmt?'pointer':'default' }}>
                <CreditCard size={14} style={{ verticalAlign:'middle', marginRight:6 }}/>Send Payment Request + M-Pesa STK
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ── Create Order Modal ─────────────────────────────────────────── */}
      {showOrderModal && (
        <div style={{ position:'fixed', inset:0, background:'rgba(0,0,0,0.5)', display:'flex', alignItems:'center', justifyContent:'center', zIndex:1000 }}>
          <div style={{ background:'white', borderRadius:16, padding:28, width:440, boxShadow:'0 20px 60px rgba(0,0,0,0.3)' }}>
            <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center', marginBottom:20 }}>
              <div style={{ fontWeight:800, fontSize:17 }}>Create Order from Chat</div>
              <button onClick={() => setOrdMod(false)} style={{ background:'none', border:'none', cursor:'pointer' }}><X size={18}/></button>
            </div>
            <div style={{ marginBottom:14, padding:12, background:'#F0F8F1', borderRadius:10, fontSize:13, border:`1px solid ${G}` }}>
              <b>Customer:</b> {activeConv?.customerName} · {activeConv?.customerPhone || 'No phone'}<br/>
              <b>Channel:</b> <PlatformBadge platform={activeConv?.platform||''} size="sm"/>
            </div>
            <div style={{ marginBottom:14 }}>
              <label style={{ fontSize:12, fontWeight:600, display:'block', marginBottom:5, color:'#666' }}>Customer Phone (for M-Pesa STK)</label>
              <input placeholder="+254712345678" defaultValue={activeConv?.customerPhone||''}
                style={{ width:'100%', padding:'10px 12px', borderRadius:8, border:'1.5px solid #E0E0E0', fontSize:13, outline:'none', boxSizing:'border-box' }} />
            </div>
            <div style={{ marginBottom:14 }}>
              <label style={{ fontSize:12, fontWeight:600, display:'block', marginBottom:5, color:'#666' }}>Delivery Location</label>
              <input placeholder="e.g. Westlands, Nairobi"
                style={{ width:'100%', padding:'10px 12px', borderRadius:8, border:'1.5px solid #E0E0E0', fontSize:13, outline:'none', boxSizing:'border-box' }} />
            </div>
            <div style={{ marginBottom:20, padding:12, background:'#FFF8E1', borderRadius:10, fontSize:12, border:'1px solid #FFB300' }}>
              <b>💡 Tip:</b> After creating, use the product search in the Orders page to add items, then come back to send the payment request.
            </div>
            <div style={{ display:'flex', gap:10 }}>
              <button onClick={() => setOrdMod(false)} style={{ flex:1, padding:'11px', borderRadius:10, border:'1.5px solid #E0E0E0', background:'white', fontSize:13, fontWeight:600, cursor:'pointer' }}>Cancel</button>
              <button onClick={() => {
                setOrdMod(false)
                const msg = { id:`m${Date.now()}`, direction:'OUTBOUND', senderType:'AGENT', content:`✅ Order created for ${activeConv?.customerName}! Nitakutumia maelekezo ya kulipa hivi karibuni.`, messageType:'TEXT', createdAt:new Date().toISOString(), isAiGenerated:false }
                if (active) setMsgs(m => ({ ...m, [active]: [...(m[active]||[]), msg] }))
              }} style={{ flex:2, padding:'11px', borderRadius:10, border:'none', background:G, color:'white', fontSize:13, fontWeight:800, cursor:'pointer' }}>
                <ShoppingCart size={14} style={{ verticalAlign:'middle', marginRight:6 }}/>Create Order + Notify Customer
              </button>
            </div>
          </div>
        </div>
      )}

      <style>{`
        @keyframes spin { to { transform: rotate(360deg); } }
        .spin { animation: spin 1s linear infinite; }
      `}</style>
    </div>
  )
}

// ── Tab: Channels ─────────────────────────────────────────────────────────────
function ChannelsTab() {
  const [channels, setChannels] = useState(CHANNELS_DATA)
  const [showAdd, setShowAdd]   = useState(false)
  const [newPlatform, setNP]    = useState('WHATSAPP')
  const [copied, setCopied]     = useState<string|null>(null)

  function copy(text: string, key: string) {
    navigator.clipboard.writeText(text).catch(()=>{})
    setCopied(key)
    setTimeout(() => setCopied(null), 1800)
  }

  function toggle(id: string) {
    setChannels(cs => cs.map(c => c.id === id ? {...c, autoReplyEnabled: !c.autoReplyEnabled} : c))
  }

  const connected = channels.filter(c => c.isActive).length

  const SETUP_STEPS: Record<string, { title: string; steps: string[] }> = {
    WHATSAPP: {
      title: 'Meta Business Suite → WhatsApp',
      steps: [
        'Go to developers.facebook.com → Your App → WhatsApp → API Setup',
        'Add a phone number and complete business verification',
        'Copy the "Phone number ID" as External ID and generate a permanent access token',
        'Paste your Webhook URL and Verify Token on the Webhooks configuration page',
        'Subscribe to "messages" webhook fields',
      ]
    },
    INSTAGRAM: {
      title: 'Meta Business Suite → Instagram',
      steps: [
        'Connect your Instagram Professional account to a Facebook Page',
        'Go to developers.facebook.com → Your App → Messenger → Instagram Settings',
        'Copy the Instagram Account ID as External ID and generate a Page Access Token',
        'Subscribe to instagram_messages and comments webhook fields',
        'Paste your Webhook URL and Verify Token',
      ]
    },
    FACEBOOK: {
      title: 'Meta for Developers → Messenger',
      steps: [
        'Go to developers.facebook.com → Your App → Messenger → Settings',
        'Connect your Facebook Page and generate a Page Access Token',
        'Copy the Page ID as External ID',
        'Subscribe to messages and feed (for comments) webhook fields',
        'Paste your Webhook URL and Verify Token',
      ]
    },
    TIKTOK: {
      title: 'TikTok for Business → Developer Portal',
      steps: [
        'Go to developers.tiktok.com and create a Business app',
        'Enable the "Direct Message" and "Comment" products',
        'Complete Business Verification (required for DM API)',
        'Copy your Client Key / App ID as External ID',
        'Webhook URL for TikTok includes the channel ID (generated after saving)',
      ]
    },
  }

  return (
    <div style={{ display:'flex', flexDirection:'column', gap:16, padding:'4px 0' }}>

      {/* Summary KPIs */}
      <div style={{ display:'grid', gridTemplateColumns:'repeat(4,1fr)', gap:12 }}>
        {Object.entries(PLATFORM).map(([key, p]) => {
          const ch = channels.find(c => c.platform === key)
          return (
            <div key={key} style={{ background:'white', borderRadius:14, padding:16, border:`2px solid ${ch?.isActive ? p.color : '#E8EDE9'}`, position:'relative' }}>
              <div style={{ fontSize:24, marginBottom:6 }}>{p.icon}</div>
              <div style={{ fontWeight:800, fontSize:14, color:'#1A1A1A' }}>{p.label}</div>
              <div style={{ fontSize:11, marginTop:4 }}>
                {ch?.isActive
                  ? <span style={{ color:p.color, fontWeight:600 }}>● Connected — {ch.channelName}</span>
                  : <span style={{ color:'#CCC' }}>○ Not connected</span>
                }
              </div>
              {ch?.isActive && ch.unreadCount > 0 && (
                <div style={{ position:'absolute', top:12, right:12, background:p.color, color:'white', borderRadius:'50%', width:20, height:20, fontSize:11, fontWeight:800, display:'flex', alignItems:'center', justifyContent:'center' }}>
                  {ch.unreadCount}
                </div>
              )}
            </div>
          )
        })}
      </div>

      {/* Connected channels */}
      <div style={{ display:'flex', flexDirection:'column', gap:10 }}>
        {channels.map(ch => {
          const p = PLATFORM[ch.platform as keyof typeof PLATFORM]
          return (
            <div key={ch.id} style={{ background:'white', borderRadius:14, padding:20, border:`1px solid ${ch.isActive ? '#E8EDE9' : '#F5F5F5'}`, opacity: ch.isActive ? 1 : 0.7 }}>
              <div style={{ display:'flex', justifyContent:'space-between', alignItems:'flex-start', marginBottom:16 }}>
                <div style={{ display:'flex', gap:12, alignItems:'center' }}>
                  <div style={{ width:44, height:44, borderRadius:12, background:p.bg, display:'flex', alignItems:'center', justifyContent:'center', fontSize:24 }}>{p.icon}</div>
                  <div>
                    <div style={{ fontWeight:800, fontSize:15 }}>{ch.channelName}</div>
                    <div style={{ fontSize:12, color:'#888', marginTop:2 }}>{p.label} · ID: {ch.externalId}</div>
                    {ch.phoneNumber && <div style={{ fontSize:12, color:'#888' }}>{ch.phoneNumber}</div>}
                  </div>
                </div>
                <div style={{ display:'flex', gap:8, alignItems:'center' }}>
                  {/* Auto-reply toggle */}
                  <div style={{ display:'flex', alignItems:'center', gap:6 }}>
                    <span style={{ fontSize:11, color:'#666' }}>AI Auto-reply</span>
                    <div onClick={() => toggle(ch.id)} style={{
                      width:36, height:20, borderRadius:10, cursor:'pointer',
                      background: ch.autoReplyEnabled ? G : '#DDD',
                      position:'relative', transition:'background .2s'
                    }}>
                      <div style={{
                        width:16, height:16, borderRadius:'50%', background:'white',
                        position:'absolute', top:2, transition:'left .2s',
                        left: ch.autoReplyEnabled ? 18 : 2, boxShadow:'0 1px 3px rgba(0,0,0,0.2)'
                      }}/>
                    </div>
                  </div>
                  <span style={{ padding:'4px 10px', borderRadius:20, fontSize:11, fontWeight:700, background: ch.isActive ? '#E8F5E9' : '#F5F5F5', color: ch.isActive ? G : '#999' }}>
                    {ch.isActive ? 'Active' : 'Inactive'}
                  </span>
                </div>
              </div>

              {/* Webhook info */}
              <div style={{ background:'#F8FAF8', borderRadius:10, padding:12, display:'flex', flexDirection:'column', gap:8 }}>
                <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center' }}>
                  <div>
                    <div style={{ fontSize:11, fontWeight:600, color:'#666', marginBottom:3 }}>Webhook URL</div>
                    <code style={{ fontSize:11, color:'#333', fontFamily:'monospace' }}>{ch.webhookUrl}</code>
                  </div>
                  <button onClick={() => copy(ch.webhookUrl, `url-${ch.id}`)} style={{ padding:'5px 10px', borderRadius:7, border:'1px solid #DDD', background:'white', cursor:'pointer', fontSize:11, display:'flex', alignItems:'center', gap:4 }}>
                    {copied === `url-${ch.id}` ? <><Check size={11} color={G}/> Copied</> : <><Copy size={11}/> Copy</>}
                  </button>
                </div>
                <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center' }}>
                  <div>
                    <div style={{ fontSize:11, fontWeight:600, color:'#666', marginBottom:3 }}>Verify Token</div>
                    <code style={{ fontSize:11, color:'#333', fontFamily:'monospace' }}>{ch.webhookVerifyToken}</code>
                  </div>
                  <button onClick={() => copy(ch.webhookVerifyToken, `tok-${ch.id}`)} style={{ padding:'5px 10px', borderRadius:7, border:'1px solid #DDD', background:'white', cursor:'pointer', fontSize:11, display:'flex', alignItems:'center', gap:4 }}>
                    {copied === `tok-${ch.id}` ? <><Check size={11} color={G}/> Copied</> : <><Copy size={11}/> Copy</>}
                  </button>
                </div>
              </div>
            </div>
          )
        })}
      </div>

      {/* Add channel button */}
      <button onClick={() => setShowAdd(true)} style={{ padding:'14px', borderRadius:12, border:`2px dashed ${G}`, background:'white', color:G, fontSize:14, fontWeight:700, cursor:'pointer', display:'flex', alignItems:'center', justifyContent:'center', gap:8 }}>
        + Connect New Channel
      </button>

      {/* Add channel modal */}
      {showAdd && (
        <div style={{ position:'fixed', inset:0, background:'rgba(0,0,0,0.5)', display:'flex', alignItems:'center', justifyContent:'center', zIndex:1000 }}>
          <div style={{ background:'white', borderRadius:16, padding:28, width:520, maxHeight:'80vh', overflowY:'auto', boxShadow:'0 20px 60px rgba(0,0,0,0.3)' }}>
            <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center', marginBottom:20 }}>
              <div style={{ fontWeight:800, fontSize:18 }}>Connect Social Channel</div>
              <button onClick={() => setShowAdd(false)} style={{ background:'none', border:'none', cursor:'pointer' }}><X size={18}/></button>
            </div>

            {/* Platform selector */}
            <div style={{ display:'grid', gridTemplateColumns:'repeat(4,1fr)', gap:8, marginBottom:20 }}>
              {Object.entries(PLATFORM).map(([key, p]) => (
                <button key={key} onClick={() => setNP(key)} style={{
                  padding:'12px 6px', borderRadius:10, border:`2px solid ${newPlatform===key ? p.color : '#E0E0E0'}`,
                  background: newPlatform===key ? p.bg : 'white', cursor:'pointer',
                  display:'flex', flexDirection:'column', alignItems:'center', gap:4
                }}>
                  <span style={{ fontSize:22 }}>{p.icon}</span>
                  <span style={{ fontSize:11, fontWeight:700, color: newPlatform===key ? p.color : '#666' }}>{p.label}</span>
                </button>
              ))}
            </div>

            {/* Setup guide */}
            <div style={{ background:'#F8F9FF', borderRadius:10, padding:16, marginBottom:16, border:'1px solid #C5CAE9' }}>
              <div style={{ fontWeight:700, fontSize:13, color:'#1565C0', marginBottom:10 }}>📋 {SETUP_STEPS[newPlatform].title}</div>
              {SETUP_STEPS[newPlatform].steps.map((s, i) => (
                <div key={i} style={{ display:'flex', gap:8, marginBottom:7, fontSize:12 }}>
                  <span style={{ fontWeight:800, color:'#1565C0', minWidth:18 }}>{i+1}.</span>
                  <span style={{ color:'#333' }}>{s}</span>
                </div>
              ))}
            </div>

            {/* Form fields */}
            {[
              { label:'Channel Name', placeholder:'e.g. My WhatsApp Business', key:'channelName' },
              { label: newPlatform === 'WHATSAPP' ? 'Phone Number ID (from Meta)' : newPlatform === 'TIKTOK' ? 'TikTok Open ID / Client Key' : 'Page ID / Account ID', placeholder:'Numeric ID from developer console', key:'externalId' },
              ...(newPlatform === 'WHATSAPP' ? [{ label:'Phone Number', placeholder:'+254700000001', key:'phone' }] : []),
              { label:'Access Token', placeholder:'Long-lived access token', key:'token' },
            ].map(f => (
              <div key={f.key} style={{ marginBottom:12 }}>
                <label style={{ fontSize:12, fontWeight:600, display:'block', marginBottom:5, color:'#666' }}>{f.label}</label>
                <input placeholder={f.placeholder}
                  style={{ width:'100%', padding:'10px 12px', borderRadius:8, border:'1.5px solid #E0E0E0', fontSize:13, outline:'none', boxSizing:'border-box' }} />
              </div>
            ))}

            <div style={{ marginBottom:20 }}>
              <label style={{ fontSize:12, fontWeight:600, display:'block', marginBottom:5, color:'#666' }}>AI Auto-reply Persona (optional)</label>
              <textarea placeholder="e.g. You are a friendly sales agent for Biashara360. Reply in Swahili/English. Keep replies short and helpful."
                rows={3} style={{ width:'100%', padding:'10px 12px', borderRadius:8, border:'1.5px solid #E0E0E0', fontSize:13, outline:'none', resize:'vertical', boxSizing:'border-box', fontFamily:'inherit' }} />
            </div>

            <div style={{ display:'flex', gap:10 }}>
              <button onClick={() => setShowAdd(false)} style={{ flex:1, padding:'12px', borderRadius:10, border:'1.5px solid #E0E0E0', background:'white', fontSize:13, fontWeight:600, cursor:'pointer' }}>Cancel</button>
              <button onClick={() => {
                setChannels(cs => [...cs, { id:`ch${Date.now()}`, platform:newPlatform, channelName:`New ${PLATFORM[newPlatform as keyof typeof PLATFORM].label} Channel`, externalId:'pending', phoneNumber:null, isActive:true, autoReplyEnabled:true, webhookVerifyToken:`tok${Date.now()}`, webhookUrl:`https://api.biashara360.co.ke/v1/social/webhook/${newPlatform.toLowerCase()}`, unreadCount:0 }])
                setShowAdd(false)
              }} style={{ flex:2, padding:'12px', borderRadius:10, border:'none', background:PLATFORM[newPlatform as keyof typeof PLATFORM].color, color:'white', fontSize:13, fontWeight:800, cursor:'pointer' }}>
                Connect {PLATFORM[newPlatform as keyof typeof PLATFORM].icon} {PLATFORM[newPlatform as keyof typeof PLATFORM].label}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

// ── Tab: Analytics ────────────────────────────────────────────────────────────
function AnalyticsTab() {
  const stats = [
    { platform:'WHATSAPP',  conversations:48, orders:12, revenue:87600, responseTime:'1m 22s' },
    { platform:'INSTAGRAM', conversations:31, orders:8,  revenue:52400, responseTime:'2m 45s' },
    { platform:'FACEBOOK',  conversations:19, orders:4,  revenue:28800, responseTime:'4m 10s' },
    { platform:'TIKTOK',    conversations:14, orders:3,  revenue:18900, responseTime:'3m 30s' },
  ]
  const total = stats.reduce((a, s) => ({ conversations:a.conversations+s.conversations, orders:a.orders+s.orders, revenue:a.revenue+s.revenue }), { conversations:0, orders:0, revenue:0 })

  return (
    <div style={{ display:'flex', flexDirection:'column', gap:16 }}>
      {/* Top KPIs */}
      <div style={{ display:'grid', gridTemplateColumns:'repeat(4,1fr)', gap:12 }}>
        {[
          { label:'Total Conversations', value:total.conversations, color:G },
          { label:'Orders from Social',  value:total.orders,        color:'#1565C0' },
          { label:'Revenue from Social', value:`KES ${(total.revenue/1000).toFixed(0)}K`, color:'#6A1B9A' },
          { label:'AI Handled',          value:'74%',               color:'#E65100' },
        ].map(k => (
          <div key={k.label} style={{ background:'white', borderRadius:14, padding:18, border:'1px solid #E8EDE9' }}>
            <div style={{ fontSize:11, color:'#888', marginBottom:6 }}>{k.label}</div>
            <div style={{ fontSize:26, fontWeight:900, color:k.color }}>{k.value}</div>
            <div style={{ fontSize:11, color:'#AAA', marginTop:4 }}>This month</div>
          </div>
        ))}
      </div>

      {/* Per-platform breakdown */}
      <div style={{ background:'white', borderRadius:14, padding:20, border:'1px solid #E8EDE9' }}>
        <div style={{ fontWeight:800, fontSize:15, marginBottom:16 }}>Platform Performance</div>
        <table style={{ width:'100%', borderCollapse:'collapse' }}>
          <thead>
            <tr>
              {['Platform','Conversations','Orders','Revenue','Avg Response','Conversion'].map(h => (
                <th key={h} style={{ textAlign:'left', fontSize:11, fontWeight:700, color:'#888', padding:'0 0 10px', borderBottom:'2px solid #F0F0F0' }}>{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {stats.map((s, i) => {
              const p   = PLATFORM[s.platform as keyof typeof PLATFORM]
              const cvr = ((s.orders / s.conversations)*100).toFixed(0)
              return (
                <tr key={s.platform} style={{ borderBottom: i < stats.length-1 ? '1px solid #F5F5F5' : 'none' }}>
                  <td style={{ padding:'12px 0', display:'flex', alignItems:'center', gap:8 }}>
                    <span style={{ fontSize:18 }}>{p.icon}</span>
                    <span style={{ fontWeight:700, fontSize:13 }}>{p.label}</span>
                  </td>
                  <td style={{ padding:'12px 0', fontSize:13, fontWeight:600 }}>{s.conversations}</td>
                  <td style={{ padding:'12px 0', fontSize:13, fontWeight:600 }}>{s.orders}</td>
                  <td style={{ padding:'12px 0', fontSize:13, fontWeight:700, color:G }}>KES {s.revenue.toLocaleString()}</td>
                  <td style={{ padding:'12px 0', fontSize:12, color:'#666' }}>{s.responseTime}</td>
                  <td style={{ padding:'12px 0' }}>
                    <div style={{ display:'flex', alignItems:'center', gap:8 }}>
                      <div style={{ flex:1, height:6, background:'#F0F0F0', borderRadius:3 }}>
                        <div style={{ width:`${cvr}%`, height:'100%', background:p.color, borderRadius:3 }}/>
                      </div>
                      <span style={{ fontSize:12, fontWeight:700, color:p.color, minWidth:28 }}>{cvr}%</span>
                    </div>
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>

      {/* Order source chart */}
      <div style={{ background:'white', borderRadius:14, padding:20, border:'1px solid #E8EDE9' }}>
        <div style={{ fontWeight:800, fontSize:15, marginBottom:16 }}>Orders by Channel</div>
        <div style={{ display:'flex', gap:8, alignItems:'flex-end', height:120 }}>
          {stats.map(s => {
            const p   = PLATFORM[s.platform as keyof typeof PLATFORM]
            const h   = (s.orders / 12) * 100
            return (
              <div key={s.platform} style={{ flex:1, display:'flex', flexDirection:'column', alignItems:'center', gap:6 }}>
                <div style={{ fontSize:12, fontWeight:700, color:p.color }}>{s.orders}</div>
                <div style={{ width:'100%', height:`${h}%`, minHeight:8, background:p.color, borderRadius:'6px 6px 0 0', transition:'height .3s' }}/>
                <div style={{ fontSize:11, color:'#888' }}>{p.icon}</div>
              </div>
            )
          })}
        </div>
      </div>
    </div>
  )
}

// ── Main Social Page ──────────────────────────────────────────────────────────
export default function SocialPage() {
  const [tab, setTab] = useState<'inbox'|'channels'|'analytics'>('inbox')
  const totalUnread   = SAMPLE_CONVS.reduce((s, c) => s + c.unreadCount, 0)

  const tabs = [
    { key:'inbox',     label:'Unified Inbox',    badge: totalUnread > 0 ? totalUnread : null },
    { key:'channels',  label:'Connected Channels', badge: null },
    { key:'analytics', label:'Analytics',          badge: null },
  ]

  return (
    <div style={{ display:'flex', flexDirection:'column', gap:16 }}>
      {/* Header */}
      <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center' }}>
        <div>
          <h1 style={{ margin:0, fontSize:22, fontWeight:900 }}>Social Commerce Inbox</h1>
          <p style={{ margin:'4px 0 0', fontSize:13, color:'#888' }}>WhatsApp · Instagram · Facebook · TikTok — all in one place</p>
        </div>
        <div style={{ display:'flex', gap:8, alignItems:'center' }}>
          {Object.entries(PLATFORM).map(([key, p]) => (
            <span key={key} title={p.label} style={{ fontSize:22, cursor:'default' }}>{p.icon}</span>
          ))}
          <div style={{ width:1, height:28, background:'#E0E0E0', margin:'0 4px' }} />
          <div style={{ background:'#E8F5E9', color:G, padding:'6px 14px', borderRadius:20, fontSize:12, fontWeight:700, display:'flex', alignItems:'center', gap:5 }}>
            <Wifi size={12}/> AI Auto-reply On
          </div>
        </div>
      </div>

      {/* Tabs */}
      <div style={{ display:'flex', gap:2, borderBottom:'2px solid #E0E0E0' }}>
        {tabs.map(t => (
          <button key={t.key} onClick={() => setTab(t.key as any)} style={{
            display:'flex', alignItems:'center', gap:6, padding:'10px 20px',
            border:'none', background:'none', cursor:'pointer',
            fontWeight: tab===t.key ? 800 : 500, fontSize:13,
            color: tab===t.key ? G : '#666',
            borderBottom: tab===t.key ? `2.5px solid ${G}` : '2.5px solid transparent',
            marginBottom:'-2px', position:'relative'
          }}>
            {t.label}
            {t.badge != null && (
              <span style={{ background:G, color:'white', borderRadius:'50%', width:18, height:18, fontSize:10, fontWeight:800, display:'flex', alignItems:'center', justifyContent:'center' }}>
                {t.badge > 9 ? '9+' : t.badge}
              </span>
            )}
          </button>
        ))}
      </div>

      {tab === 'inbox'     && <InboxTab />}
      {tab === 'channels'  && <ChannelsTab />}
      {tab === 'analytics' && <AnalyticsTab />}
    </div>
  )
}

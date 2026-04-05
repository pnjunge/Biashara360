import SwiftUI

// ── Platform Meta ─────────────────────────────────────────────────────────────
struct SocialPlatform {
    let label: String; let emoji: String; let color: Color; let bg: Color
}
let socialPlatforms: [String: SocialPlatform] = [
    "WHATSAPP":  SocialPlatform(label:"WhatsApp",  emoji:"💬", color:Color(red:0.15,green:0.83,blue:0.40), bg:Color(red:0.91,green:0.98,blue:0.94)),
    "INSTAGRAM": SocialPlatform(label:"Instagram", emoji:"📸", color:Color(red:0.88,green:0.19,blue:0.42), bg:Color(red:0.99,green:0.91,blue:0.94)),
    "FACEBOOK":  SocialPlatform(label:"Facebook",  emoji:"👥", color:Color(red:0.09,green:0.46,blue:0.95), bg:Color(red:0.91,green:0.94,blue:1.00)),
    "TIKTOK":    SocialPlatform(label:"TikTok",    emoji:"🎵", color:Color.black,                           bg:Color(red:0.94,green:0.94,blue:0.94)),
]

// ── Data ──────────────────────────────────────────────────────────────────────
struct IOSSocialConv: Identifiable {
    let id: String; let platform: String; let customerName: String
    let customerPhone: String?; var status: String; var unreadCount: Int
    let lastMessage: String; let lastMessageAt: String; var isAiHandled: Bool
}
struct IOSSocialMsg: Identifiable {
    let id: String; let direction: String; let senderType: String
    let content: String; let messageType: String; let time: String; let isAiGenerated: Bool
}

private let iosConvs: [IOSSocialConv] = [
    IOSSocialConv(id:"c1", platform:"WHATSAPP",  customerName:"Amina Wanjiru",   customerPhone:"+254712345678", status:"OPEN",            unreadCount:3, lastMessage:"Hii unga inauzwa bei gani?",     lastMessageAt:"14:30", isAiHandled:false),
    IOSSocialConv(id:"c2", platform:"INSTAGRAM", customerName:"Kevin Omondi",    customerPhone:nil,             status:"PENDING_PAYMENT", unreadCount:0, lastMessage:"Sawa, nitapeleka M-Pesa sasa", lastMessageAt:"13:55", isAiHandled:true),
    IOSSocialConv(id:"c3", platform:"FACEBOOK",  customerName:"Grace Muthoni",   customerPhone:"+254798765432", status:"OPEN",            unreadCount:1, lastMessage:"Do you do deliveries to Nakuru?", lastMessageAt:"12:20", isAiHandled:false),
    IOSSocialConv(id:"c4", platform:"TIKTOK",    customerName:"TikTok User 902", customerPhone:nil,             status:"COMPLETED",       unreadCount:0, lastMessage:"Asante sana! Order imefika 🙏", lastMessageAt:"10:00", isAiHandled:true),
    IOSSocialConv(id:"c5", platform:"WHATSAPP",  customerName:"Peter Kamau",     customerPhone:"+254711223344", status:"OPEN",            unreadCount:2, lastMessage:"Mnafungua saa ngapi?",           lastMessageAt:"09:45", isAiHandled:false),
]

private let iosMsgs: [String: [IOSSocialMsg]] = [
    "c1": [
        IOSSocialMsg(id:"m1", direction:"INBOUND",  senderType:"CUSTOMER", content:"Habari! Mnauza unga wa dhahabu?",                                                                   messageType:"TEXT", time:"14:20", isAiGenerated:false),
        IOSSocialMsg(id:"m2", direction:"OUTBOUND", senderType:"AI",       content:"Habari yako! Ndiyo, tunazo unga. Bei ni KES 180 kwa 2kg, KES 320 kwa 5kg. Ungependa kuagiza? 😊", messageType:"TEXT", time:"14:21", isAiGenerated:true),
        IOSSocialMsg(id:"m3", direction:"INBOUND",  senderType:"CUSTOMER", content:"Hii unga inauzwa bei gani kwa debe?",                                                               messageType:"TEXT", time:"14:30", isAiGenerated:false),
    ],
    "c2": [
        IOSSocialMsg(id:"m4", direction:"INBOUND",  senderType:"CUSTOMER", content:"Ninaomba order ya 3 bottles za cooking oil",                       messageType:"TEXT",            time:"13:30", isAiGenerated:false),
        IOSSocialMsg(id:"m5", direction:"OUTBOUND", senderType:"AI",       content:"Asante Kevin! Cooking oil × 3 = KES 585. Nitakutumia maelekezo ya kulipa! 🛍️", messageType:"TEXT", time:"13:31", isAiGenerated:true),
        IOSSocialMsg(id:"m6", direction:"OUTBOUND", senderType:"AGENT",    content:"Hujambo Kevin! 🛍️\n\nCooking Oil × 3\n💰 KES 585\n\n💳 Lipa Mpesa:\nPaybill: 174379\nAccount: ORD-0122\nKiasi: KES 585\n\nAsante! 🙏", messageType:"PAYMENT_REQUEST", time:"13:32", isAiGenerated:false),
        IOSSocialMsg(id:"m7", direction:"INBOUND",  senderType:"CUSTOMER", content:"Sawa, nitapeleka M-Pesa sasa",                                     messageType:"TEXT",            time:"13:55", isAiGenerated:false),
    ],
    "c3": [
        IOSSocialMsg(id:"m8", direction:"INBOUND", senderType:"CUSTOMER", content:"Do you do deliveries to Nakuru?", messageType:"TEXT", time:"12:20", isAiGenerated:false),
    ],
    "c5": [
        IOSSocialMsg(id:"m12", direction:"INBOUND", senderType:"CUSTOMER", content:"Mnafungua saa ngapi?",          messageType:"TEXT", time:"09:45", isAiGenerated:false),
        IOSSocialMsg(id:"m13", direction:"INBOUND", senderType:"CUSTOMER", content:"Na mnafunga saa ngapi jioni?",  messageType:"TEXT", time:"09:46", isAiGenerated:false),
    ],
]

// ── Main View ─────────────────────────────────────────────────────────────────
struct SocialView: View {
    @State private var selectedTab = 0

    var body: some View {
        NavigationView {
            VStack(spacing: 0) {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 4) {
                        ForEach(["Inbox", "Channels", "Analytics"], id: \.self) { tab in
                            let i = ["Inbox","Channels","Analytics"].firstIndex(of: tab)!
                            Button(action: { selectedTab = i }) {
                                HStack(spacing: 5) {
                                    Text(tab).font(.system(size:13, weight: selectedTab==i ? .bold : .medium))
                                    if tab == "Inbox" {
                                        let total = iosConvs.reduce(0) { $0 + $1.unreadCount }
                                        if total > 0 {
                                            Text("\(total)").font(.system(size:9, weight:.black)).foregroundColor(.white)
                                                .frame(width:17, height:17).background(b360Green).clipShape(Circle())
                                        }
                                    }
                                }
                                .foregroundColor(selectedTab==i ? b360Green : .secondary)
                                .padding(.horizontal, 16).padding(.vertical, 10)
                                .background(selectedTab==i ? b360Green.opacity(0.1) : Color.clear)
                                .cornerRadius(20)
                            }
                        }
                    }.padding(.horizontal, 12).padding(.vertical, 8)
                }.background(Color.white)
                Divider()
                switch selectedTab {
                case 0: SocialInboxView()
                case 1: SocialChannelsView()
                default: SocialAnalyticsView()
                }
            }
            .navigationTitle("Social Inbox")
            .navigationBarTitleDisplayMode(.large)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    HStack(spacing: 6) {
                        Text("💬📸👥🎵").font(.system(size:14))
                        Text("AI On").font(.system(size:11, weight:.bold)).foregroundColor(b360Green)
                            .padding(.horizontal, 10).padding(.vertical, 4)
                            .background(b360Green.opacity(0.12)).cornerRadius(20)
                    }
                }
            }
        }
    }
}

// ── Inbox View ────────────────────────────────────────────────────────────────
struct SocialInboxView: View {
    @State private var convs           = iosConvs
    @State private var filterPlatform  = "ALL"
    @State private var selectedConv: IOSSocialConv? = nil

    let platforms = ["ALL","WHATSAPP","INSTAGRAM","FACEBOOK","TIKTOK"]

    var filtered: [IOSSocialConv] {
        filterPlatform == "ALL" ? convs : convs.filter { $0.platform == filterPlatform }
    }

    var body: some View {
        VStack(spacing: 0) {
            // Platform filter
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 6) {
                    ForEach(platforms, id: \.self) { p in
                        let meta = socialPlatforms[p]
                        let active = filterPlatform == p
                        Button(action: { filterPlatform = p }) {
                            Text(meta != nil ? "\(meta!.emoji) \(meta!.label)" : "All")
                                .font(.system(size:11, weight:.semibold))
                                .foregroundColor(active ? .white : .secondary)
                                .padding(.horizontal, 12).padding(.vertical, 6)
                                .background(active ? (meta?.color ?? b360Green) : Color(UIColor.systemGray6))
                                .cornerRadius(20)
                        }
                    }
                }.padding(.horizontal, 12).padding(.vertical, 8)
            }.background(Color.white)
            Divider()

            List(filtered) { conv in
                Button(action: { selectedConv = conv }) {
                    ConvRowView(conv: conv)
                }
                .listRowInsets(EdgeInsets(top:0,leading:0,bottom:0,trailing:0))
                .listRowSeparator(.hidden)
            }
            .listStyle(.plain)
        }
        .background(Color(UIColor.systemGroupedBackground))
        .sheet(item: $selectedConv) { conv in
            IOSChatView(conv: conv)
        }
    }
}

struct ConvRowView: View {
    let conv: IOSSocialConv
    var body: some View {
        let p = socialPlatforms[conv.platform]!
        let statusColor: Color = conv.status == "OPEN" ? b360Green : conv.status == "PENDING_PAYMENT" ? Color(red:1,green:0.56,blue:0) : conv.status == "COMPLETED" ? Color(red:0.08,green:0.4,blue:0.75) : .gray
        HStack(spacing: 12) {
            ZStack(alignment: .bottomTrailing) {
                ZStack {
                    Circle().fill(p.bg).frame(width:46, height:46)
                    Text(String(conv.customerName.prefix(1))).font(.headline).fontWeight(.bold)
                }
                Text(p.emoji).font(.system(size:13))
            }
            VStack(alignment: .leading, spacing: 3) {
                HStack {
                    Text(conv.customerName).font(.system(size:14, weight:.bold)).lineLimit(1)
                    Spacer()
                    Text(conv.lastMessageAt).font(.system(size:10)).foregroundColor(.secondary)
                }
                Text(conv.lastMessage).font(.system(size:12)).foregroundColor(.secondary).lineLimit(1)
                HStack(spacing: 6) {
                    Circle().fill(statusColor).frame(width:7, height:7)
                    Text(conv.status.replacingOccurrences(of:"_", with:" ")).font(.system(size:10)).foregroundColor(.secondary)
                    if conv.isAiHandled {
                        Text("AI").font(.system(size:9, weight:.bold)).foregroundColor(b360Green)
                            .padding(.horizontal,5).padding(.vertical,1).background(b360Green.opacity(0.12)).cornerRadius(10)
                    }
                }
            }
            if conv.unreadCount > 0 {
                Text("\(conv.unreadCount)").font(.system(size:10, weight:.black)).foregroundColor(.white)
                    .frame(width:20, height:20).background(p.color).clipShape(Circle())
            }
        }
        .padding(.horizontal,14).padding(.vertical,12)
        .background(Color.white)
    }
}

// ── Chat View ─────────────────────────────────────────────────────────────────
struct IOSChatView: View {
    let conv: IOSSocialConv
    @Environment(\.dismiss) var dismiss
    @State private var messages: [IOSSocialMsg] = []
    @State private var draft        = ""
    @State private var aiSuggestion: String? = nil
    @State private var aiLoading    = false
    @State private var showPaySheet = false
    @State private var payAmt       = ""
    @State private var payDesc      = ""

    var body: some View {
        let p = socialPlatforms[conv.platform]!
        NavigationView {
            VStack(spacing: 0) {
                ScrollViewReader { proxy in
                    ScrollView {
                        LazyVStack(spacing: 8) {
                            ForEach(messages) { msg in
                                IOSChatBubble(msg: msg).id(msg.id)
                            }
                        }.padding(12)
                    }
                    .onChange(of: messages.count) { _ in
                        if let last = messages.last { proxy.scrollTo(last.id, anchor:.bottom) }
                    }
                }

                // AI Suggestion
                if let sug = aiSuggestion {
                    VStack(alignment: .leading, spacing: 8) {
                        HStack(spacing: 5) {
                            Image(systemName:"bolt.fill").foregroundColor(b360Green).font(.system(size:11))
                            Text("AI Suggested Reply").font(.system(size:11, weight:.bold)).foregroundColor(b360Green)
                        }
                        Text(sug).font(.system(size:12)).lineSpacing(3)
                        HStack(spacing: 8) {
                            Button("Edit") { draft = sug; aiSuggestion = nil }
                                .font(.system(size:12, weight:.semibold)).foregroundColor(b360Green)
                                .padding(.horizontal,12).padding(.vertical,6)
                                .overlay(RoundedRectangle(cornerRadius:8).stroke(b360Green, lineWidth:1.5))
                            Button("Send") { sendMsg(sug, isAI:true); aiSuggestion = nil }
                                .font(.system(size:12, weight:.bold)).foregroundColor(.white)
                                .padding(.horizontal,16).padding(.vertical,6)
                                .background(b360Green).cornerRadius(8)
                            Spacer()
                            Button(action: { aiSuggestion = nil }) {
                                Image(systemName:"xmark").font(.system(size:11)).foregroundColor(.secondary)
                            }
                        }
                    }
                    .padding(12).background(Color(red:0.94,green:0.99,blue:0.95))
                }

                // Compose bar
                HStack(spacing: 8) {
                    Button(action: getAiReply) {
                        if aiLoading {
                            ProgressView().frame(width:18, height:18)
                        } else {
                            Image(systemName:"bolt.fill").foregroundColor(b360Green)
                        }
                    }
                    .frame(width:42, height:42).background(b360Green.opacity(0.1)).cornerRadius(10)

                    TextField("Reply to \(conv.customerName.components(separatedBy:" ").first ?? "")…", text:$draft, axis:.vertical)
                        .font(.system(size:13)).padding(10)
                        .background(Color(UIColor.systemGray6)).cornerRadius(12)
                        .lineLimit(1...4)

                    Button(action: { if !draft.isEmpty { sendMsg(draft, isAI:false); draft="" } }) {
                        Image(systemName:"arrow.up").font(.system(size:15, weight:.bold)).foregroundColor(.white)
                            .frame(width:40, height:40)
                            .background(draft.isEmpty ? Color(UIColor.systemGray4) : b360Green)
                            .clipShape(Circle())
                    }.disabled(draft.isEmpty)
                }
                .padding(10).background(Color.white)
            }
            .navigationTitle(conv.customerName)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement:.navigationBarLeading) {
                    Button("Close") { dismiss() }
                }
                ToolbarItem(placement:.navigationBarTrailing) {
                    Button(action: { showPaySheet = true }) {
                        Label("Pay", systemImage:"creditcard.fill").font(.system(size:12, weight:.bold)).foregroundColor(b360Green)
                    }
                }
            }
        }
        .onAppear { messages = iosMsgs[conv.id] ?? [] }
        .sheet(isPresented: $showPaySheet) {
            PaymentRequestSheet(conv:conv, onSend:{ amt, desc in
                let text = "Hujambo \(conv.customerName.components(separatedBy:" ").first!)! 🛍️\n\n\(desc)\n💰 KES \(amt)\n\n💳 Lipa Mpesa Paybill 174379\nKiasi: KES \(amt)\n\nAsante! 🙏"
                sendMsg(text, isAI:false, type:"PAYMENT_REQUEST")
                showPaySheet = false
            })
        }
    }

    func sendMsg(_ text: String, isAI: Bool, type: String = "TEXT") {
        messages.append(IOSSocialMsg(id:"m\(Int.random(in:1000...9999))", direction:"OUTBOUND", senderType:isAI ? "AI":"AGENT", content:text, messageType:type, time:"Now", isAiGenerated:isAI))
    }

    func getAiReply() {
        aiLoading = true
        let last = messages.last(where:{ $0.direction == "INBOUND" })?.content ?? ""
        DispatchQueue.main.asyncAfter(deadline:.now()+1.2) {
            let reply: String
            if last.lowercased().contains("bei") || last.lowercased().contains("price") || last.lowercased().contains("how much") {
                reply = "Habari! Bei yetu:\n• Unga 2kg - KES 180\n• Unga 5kg - KES 320\n• Debe - KES 1,200\n\nUngependa kuagiza? 😊"
            } else if last.lowercased().contains("deliver") {
                reply = "Ndiyo! Tunafanya delivery Nairobi yote. Delivery fee KES 150. Unataka tuwasilishe wapi? 📦"
            } else if last.lowercased().contains("saa ngapi") || last.lowercased().contains("open") {
                reply = "Tunafungua 7:00am hadi 9:00pm kila siku! 🕖 Je, ungependa kuagiza kitu?"
            } else {
                reply = "Habari \(conv.customerName.components(separatedBy:" ").first!)! Asante kwa kuwasiliana. Ninawezaje kukusaidia? 😊"
            }
            aiSuggestion = reply
            aiLoading    = false
        }
    }
}

struct IOSChatBubble: View {
    let msg: IOSSocialMsg
    var body: some View {
        let isOut  = msg.direction == "OUTBOUND"
        let isPay  = msg.messageType == "PAYMENT_REQUEST"
        HStack {
            if isOut { Spacer(minLength: 50) }
            VStack(alignment: isOut ? .trailing : .leading, spacing: 4) {
                if isPay {
                    HStack(spacing: 4) {
                        if isOut { Spacer() }
                        Image(systemName:"creditcard.fill").foregroundColor(b360Green).font(.system(size:10))
                        Text("Payment Request").font(.system(size:10, weight:.bold)).foregroundColor(b360Green)
                    }
                }
                Text(msg.content)
                    .font(.system(size:13)).lineSpacing(3)
                    .padding(.horizontal,14).padding(.vertical,10)
                    .foregroundColor(isOut && !isPay ? .white : .primary)
                    .background(
                        isPay ? Color(red:0.91,green:0.98,blue:0.94) :
                        isOut ? b360Green : Color.white
                    )
                    .cornerRadius(16)
                    .overlay(
                        isPay ? RoundedRectangle(cornerRadius:16).stroke(b360Green, lineWidth:1.5) : nil
                    )
                HStack(spacing: 5) {
                    if msg.isAiGenerated {
                        Text("AI").font(.system(size:8, weight:.bold)).foregroundColor(b360Green)
                            .padding(.horizontal,5).padding(.vertical,1).background(b360Green.opacity(0.12)).cornerRadius(8)
                    }
                    Text(msg.time).font(.system(size:10)).foregroundColor(.secondary)
                }
            }
            if !isOut { Spacer(minLength: 50) }
        }
    }
}

// ── Payment Sheet ─────────────────────────────────────────────────────────────
struct PaymentRequestSheet: View {
    let conv: IOSSocialConv
    let onSend: (String, String) -> Void
    @State private var amt  = ""
    @State private var desc = ""
    @Environment(\.dismiss) var dismiss

    var body: some View {
        NavigationView {
            ScrollView {
                VStack(alignment:.leading, spacing:16) {
                    let p = socialPlatforms[conv.platform]!
                    HStack(spacing:10) {
                        Text(p.emoji).font(.title2)
                        VStack(alignment:.leading, spacing:2) {
                            Text(conv.customerName).font(.headline)
                            Text(conv.customerPhone ?? "No phone").font(.caption).foregroundColor(.secondary)
                        }
                    }
                    .padding(14).frame(maxWidth:.infinity, alignment:.leading)
                    .background(Color(UIColor.systemGray6)).cornerRadius(10)

                    Group {
                        Text("Amount (KES)").font(.system(size:13, weight:.semibold)).foregroundColor(.secondary)
                        TextField("e.g. 850", text:$amt).keyboardType(.numberPad)
                            .font(.system(size:22, weight:.bold))
                            .padding(12).background(Color(UIColor.systemGray6)).cornerRadius(10)
                    }
                    Group {
                        Text("Description").font(.system(size:13, weight:.semibold)).foregroundColor(.secondary)
                        TextField("e.g. Unga 2kg × 3", text:$desc)
                            .padding(12).background(Color(UIColor.systemGray6)).cornerRadius(10)
                    }

                    if !amt.isEmpty {
                        VStack(alignment:.leading, spacing:6) {
                            Text("Preview:").font(.system(size:11, weight:.bold)).foregroundColor(b360Green)
                            Text("Hujambo \(conv.customerName.components(separatedBy:" ").first!)! 🛍️\n\(desc.isEmpty ? "Order yako" : desc)\n💰 KES \(amt)\n💳 Lipa Mpesa Paybill 174379")
                                .font(.system(size:12)).lineSpacing(3)
                        }
                        .padding(12).background(Color(red:0.94,green:0.99,blue:0.95))
                        .overlay(RoundedRectangle(cornerRadius:10).stroke(b360Green, lineWidth:1))
                        .cornerRadius(10)
                    }

                    Button(action: { onSend(amt, desc.isEmpty ? "Order yako" : desc) }) {
                        Label("Send Payment Request + M-Pesa STK", systemImage:"creditcard.fill")
                            .frame(maxWidth:.infinity).padding(14)
                            .background(amt.isEmpty ? Color(UIColor.systemGray4) : b360Green)
                            .foregroundColor(.white).cornerRadius(12)
                            .font(.system(size:14, weight:.bold))
                    }.disabled(amt.isEmpty)
                }.padding(20)
            }
            .navigationTitle("Request Payment").navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement:.navigationBarLeading) { Button("Cancel") { dismiss() } }
            }
        }
    }
}

// ── Channels View ─────────────────────────────────────────────────────────────
struct SocialChannelsView: View {
    var body: some View {
        ScrollView {
            VStack(spacing:12) {
                ForEach(Array(socialPlatforms.keys.sorted()), id:\.self) { key in
                    let p = socialPlatforms[key]!
                    let connected = key != "TIKTOK"
                    HStack(spacing:14) {
                        ZStack {
                            RoundedRectangle(cornerRadius:12).fill(p.bg).frame(width:52, height:52)
                            Text(p.emoji).font(.system(size:28))
                        }
                        VStack(alignment:.leading, spacing:4) {
                            Text(p.label).font(.system(size:16, weight:.bold))
                            Text(connected ? "● Connected" : "○ Not connected")
                                .font(.system(size:12, weight:.semibold))
                                .foregroundColor(connected ? p.color : Color(UIColor.systemGray3))
                            if connected {
                                HStack(spacing:8) {
                                    Text("AI On").font(.system(size:10, weight:.bold)).foregroundColor(b360Green)
                                        .padding(.horizontal,7).padding(.vertical,2).background(b360Green.opacity(0.12)).cornerRadius(10)
                                    Text("Auto-reply active").font(.caption).foregroundColor(.secondary)
                                }
                            }
                        }
                        Spacer()
                        if !connected {
                            Button("Connect") {}
                                .font(.system(size:12, weight:.bold)).foregroundColor(.white)
                                .padding(.horizontal,14).padding(.vertical,8).background(p.color).cornerRadius(8)
                        } else {
                            Image(systemName:"checkmark.circle.fill").foregroundColor(p.color).font(.title2)
                        }
                    }
                    .padding(16).background(Color.white)
                    .cornerRadius(14)
                    .overlay(RoundedRectangle(cornerRadius:14).stroke(connected ? p.color : Color(UIColor.systemGray5), lineWidth: connected ? 1.5 : 1))
                }
            }.padding(16)
        }.background(Color(UIColor.systemGroupedBackground))
    }
}

// ── Analytics View ────────────────────────────────────────────────────────────
struct SocialAnalyticsView: View {
    let stats: [(String, Int, Int, Int)] = [
        ("WHATSAPP",  48, 12, 87600),
        ("INSTAGRAM", 31,  8, 52400),
        ("FACEBOOK",  19,  4, 28800),
        ("TIKTOK",    14,  3, 18900),
    ]
    var body: some View {
        ScrollView {
            VStack(spacing:12) {
                LazyVGrid(columns:[GridItem(.flexible()),GridItem(.flexible())], spacing:10) {
                    KraKpiCard(title:"Conversations", value:"112", sub:"All channels", color:b360Green)
                    KraKpiCard(title:"Social Orders",  value:"27",  sub:"From social", color:Color(red:0.08,green:0.4,blue:0.75))
                    KraKpiCard(title:"Revenue",        value:"KES 188K", sub:"Social orders", color:Color(red:0.42,green:0.11,blue:0.60))
                    KraKpiCard(title:"AI Handled",     value:"74%", sub:"Auto-replied", color:Color(red:0.9,green:0.32,blue:0))
                }
                Text("Platform Performance").font(.headline).frame(maxWidth:.infinity, alignment:.leading)
                ForEach(stats, id:\.0) { (platform, convs, orders, revenue) in
                    let p   = socialPlatforms[platform]!
                    let cvr = Float(orders) / Float(convs)
                    HStack(spacing:14) {
                        Text(p.emoji).font(.system(size:30))
                        VStack(alignment:.leading, spacing:6) {
                            Text(p.label).font(.system(size:14, weight:.bold))
                            HStack(spacing:12) {
                                Text("\(convs) convs").font(.caption).foregroundColor(.secondary)
                                Text("\(orders) orders").font(.caption).foregroundColor(p.color)
                                Text("KES \(revenue/1000)K").font(.caption).foregroundColor(b360Green)
                            }
                            GeometryReader { geo in
                                ZStack(alignment:.leading) {
                                    Capsule().fill(Color(UIColor.systemGray5)).frame(height:6)
                                    Capsule().fill(p.color).frame(width:geo.size.width*CGFloat(cvr), height:6)
                                }
                            }.frame(height:6)
                            Text("\(Int(cvr*100))% conversion").font(.system(size:10, weight:.bold)).foregroundColor(p.color)
                        }
                    }
                    .padding(16).background(Color.white).cornerRadius(12)
                    .shadow(color:.black.opacity(0.04), radius:4, y:2)
                }
            }.padding(16)
        }.background(Color(UIColor.systemGroupedBackground))
    }
}

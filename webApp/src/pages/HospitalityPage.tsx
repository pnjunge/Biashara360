import React, { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  ChefHat,
  CheckCircle2,
  Clock,
  Home,
  MoreHorizontal,
  Minus,
  Package,
  Pencil,
  Plus,
  Printer,
  RefreshCw,
  Search,
  ShoppingBag,
  Trash2,
  Users,
  UtensilsCrossed,
} from "lucide-react";
import {
  Btn,
  Card,
  DataTable,
  Input,
  KpiCard,
  Modal,
  PageHeader,
  Select,
  StatusBadge,
} from "../components/ui";
import {
  BusinessProfileResponse,
  HospitalityDashboard,
  HospitalityOperations,
  HospitalityTable,
  OrderResponse,
  ProductResponse,
  businessApi,
  hospitalityApi,
  hospitalityOpsApi,
  productApi,
} from "../services/api";
import { useAuth } from "../App";
import { printOrderReceipt } from "../utils/receipt";
import { SettlementModal } from "../components/hospitality/SettlementModal";

type Cart = Record<string, number>;

export default function HospitalityPage() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const isAdmin = user?.role === "ADMIN";
  const [data, setData] = useState<HospitalityDashboard | null>(null);
  const [products, setProducts] = useState<ProductResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [successMessage, setSuccessMessage] = useState("");
  const [hubTab, setHubTab] = useState<"FLOOR" | "TABS" | "KDS" | "OPS">("KDS");
  const [modalError, setModalError] = useState("");
  const [showTable, setShowTable] = useState(false);
  const [tableDraft, setTableDraft] = useState({
    name: "",
    area: "Main Floor",
    capacity: "4",
  });
  const [editingTable, setEditingTable] = useState<HospitalityTable | null>(
    null,
  );
  const [orderTable, setOrderTable] = useState<HospitalityTable | null>(null);
  const [showOrder, setShowOrder] = useState(false);
  const [serviceType, setServiceType] = useState("DINE_IN");
  const [guests, setGuests] = useState("2");
  const [customerName, setCustomerName] = useState("Walk-in Guest");
  const [customerPhone, setCustomerPhone] = useState("");
  const [notes, setNotes] = useState("");
  const [cart, setCart] = useState<Cart>({});
  const [itemOptions, setItemOptions] = useState<Record<string,{modifiers:Array<{name:string;priceDelta:number}>;note:string;discount:string;complimentary:boolean}>>({});
  const [menuProfiles, setMenuProfiles] = useState<HospitalityOperations["menuProfiles"]>([]);
  const [ageVerified, setAgeVerified] = useState(false);
  const [menuSearch, setMenuSearch] = useState("");
  const [menuCategory, setMenuCategory] = useState("All");
  const [saving, setSaving] = useState(false);
  const [settleOrder, setSettleOrder] = useState<OrderResponse | null>(null);
  const [receiptProfile, setReceiptProfile] =
    useState<BusinessProfileResponse | null>(null);

  const load = async () => {
    setLoading(true);
    setError("");
    try {
      const [dashboard, catalog, operations] = await Promise.all([
        hospitalityApi.dashboard(),
        productApi.list(),
        hospitalityOpsApi.dashboard().catch(()=>null),
      ]);
      if (dashboard.success && dashboard.data) setData(dashboard.data);
      if (catalog.success && catalog.data)
        setProducts(catalog.data.filter((p) => p.currentStock > 0));
      if (operations?.success && operations.data) setMenuProfiles(operations.data.menuProfiles);
    } catch (e: any) {
      setError(
        e.response?.data?.message || "Could not load hospitality operations.",
      );
    } finally {
      setLoading(false);
    }
  };
  useEffect(() => {
    load();
    const timer = window.setInterval(() => {
      hospitalityApi.dashboard().then((result) => {
        if (result.success && result.data) setData(result.data)
      }).catch(() => undefined)
    }, 5000)
    return () => window.clearInterval(timer)
  }, []);
  useEffect(() => {
    businessApi
      .getProfile()
      .then((response) => {
        if (response.success && response.data) setReceiptProfile(response.data);
      })
      .catch(() => undefined);
  }, []);
  const cartProducts = useMemo(
    () => products.filter((p) => cart[p.id]),
    [products, cart],
  );
  const menuCategories = useMemo(
    () => [
      "All",
      ...Array.from(
        new Set(products.map((product) => product.category).filter(Boolean)),
      ),
    ],
    [products],
  );
  const visibleProducts = useMemo(
    () =>
      products.filter(
        (product) =>
          (menuCategory === "All" || product.category === menuCategory) &&
          (!menuSearch.trim() ||
            `${product.name} ${product.category}`
              .toLowerCase()
              .includes(menuSearch.trim().toLowerCase())),
      ),
    [products, menuCategory, menuSearch],
  );
  const total = cartProducts.reduce(
    (sum, p) => sum + (p.sellingPrice+(itemOptions[p.id]?.modifiers.reduce((s,m)=>s+m.priceDelta,0)||0)) * cart[p.id]-(Number(itemOptions[p.id]?.discount)||0),
    0,
  );
  const change = (id: string, delta: number) =>
    setCart((current) => {
      const next = Math.max(0, (current[id] || 0) + delta);
      const copy = { ...current };
      if (next) copy[id] = next;
      else delete copy[id];
      return copy;
    });
  const optionState = (id:string) => itemOptions[id] || {modifiers:[],note:"",discount:"",complimentary:false};
  const updateOption = (id:string, patch:Partial<ReturnType<typeof optionState>>) =>
    setItemOptions(current=>({...current,[id]:{...optionState(id),...patch}}));
  const toggleModifier=(id:string,modifier:{name:string;priceDelta:number})=>
    updateOption(id,{modifiers:optionState(id).modifiers.some(item=>item.name===modifier.name)
      ? optionState(id).modifiers.filter(item=>item.name!==modifier.name)
      : [...optionState(id).modifiers,modifier]});
  const selectOneModifier=(id:string,group:Array<{name:string;priceDelta:number}>,modifier:{name:string;priceDelta:number})=>
    updateOption(id,{modifiers:[...optionState(id).modifiers.filter(item=>!group.some(candidate=>candidate.name===item.name)),modifier]});
  const openOrder = (table?: HospitalityTable) => {
    setShowOrder(true);
    setOrderTable(table || null);
    setServiceType(table ? "DINE_IN" : "TAKEAWAY");
    setGuests("1");
    setCustomerName("Walk-in Guest");
    setCustomerPhone("");
    setCart({});
    setItemOptions({});
    setAgeVerified(false);
    setModalError("");
    setNotes("");
    setMenuSearch("");
    setMenuCategory("All");
  };
  const openTableEditor = (table?: HospitalityTable) => {
    setModalError("");
    setEditingTable(table || null);
    setTableDraft(
      table
        ? {
            name: table.name,
            area: table.area,
            capacity: String(table.capacity),
          }
        : { name: "", area: "Main Floor", capacity: "4" },
    );
    setShowTable(true);
  };
  const saveTable = async () => {
    setSaving(true);
    setModalError("");
    try {
      const payload = {
        name: tableDraft.name,
        area: tableDraft.area,
        capacity: Number(tableDraft.capacity),
      };
      const res = editingTable
        ? await hospitalityApi.updateTable(editingTable.id, payload)
        : await hospitalityApi.createTable(payload);
      if (!res.success) throw new Error(res.message);
      setShowTable(false);
      setEditingTable(null);
      await load();
    } catch (e: any) {
      setModalError(e.response?.data?.message || e.message);
    } finally {
      setSaving(false);
    }
  };
  const submitOrder = async () => {
    if (!cartProducts.length) return setError("Add at least one menu item.");
    setSaving(true);
    setModalError("");
    if(serviceType==="DINE_IN"&&!orderTable) return setModalError("Select a table for dine-in service.");
    const restricted=cartProducts.some(product=>menuProfiles.find(profile=>profile.productId===product.id)?.ageRestricted);
    if(restricted&&!ageVerified) return setModalError("Confirm age verification before adding age-restricted items.");
    try {
      const res = await hospitalityApi.createOrder({
        tableId: orderTable?.id,
        serviceType,
        guestCount: Number(guests),
        customerName,
        customerPhone,
        notes,
        ageVerified,
        items: cartProducts.map((p) => ({
          productId: p.id,
          quantity: cart[p.id],
          unitPrice: p.sellingPrice,
          modifiers:itemOptions[p.id]?.modifiers||[],
          itemNote:itemOptions[p.id]?.note||"",
          discountAmount:Number(itemOptions[p.id]?.discount)||0,
          complimentary:itemOptions[p.id]?.complimentary||false,
        })),
      });
      if (!res.success) throw new Error(res.message);
      setOrderTable(null);
      setShowOrder(false);
      setCart({});
      await load();
    } catch (e: any) {
      setModalError(e.response?.data?.message || e.message);
    } finally {
      setSaving(false);
    }
  };
  const advanceTicket = async (id: string, status: string) => {
    try {
      await hospitalityApi.updateTicket(id, status);
      setSuccessMessage(`Ticket updated to ${status}`);
      await load();
    } catch (e: any) {
      setError(e.response?.data?.message || "Could not update ticket.");
    }
  };
  const openSettlement = (order: OrderResponse) => {
    setSettleOrder(order);
    setModalError("");
  };
  const transferTab = async (orderId: string, tableId: string) => {
    if (!tableId) return;
    try {
      const result = await hospitalityApi.transferTab(orderId, tableId);
      if (!result.success) throw new Error(result.message);
      await load();
    } catch (e: any) {
      setError(e.response?.data?.message || e.message);
    }
  };
  const age = (createdAt: string) => {
    const minutes = Math.max(
      0,
      Math.floor((Date.now() - new Date(createdAt).getTime()) / 60000),
    );
    return minutes < 60
      ? `${minutes} min`
      : `${Math.floor(minutes / 60)}h ${minutes % 60}m`;
  };
  const toggleHospitality = async (enabled: boolean) => {
    setSaving(true);
    setError("");
    try {
      const result = await hospitalityApi.setEnabled(enabled);
      if (!result.success) throw new Error(result.message);
      window.location.assign(enabled ? "/hospitality" : "/dashboard");
    } catch (e: any) {
      setError(e.response?.data?.message || e.message || "Could not update hospitality mode.");
    } finally {
      setSaving(false);
    }
  };

  if (loading)
    return (
      <div style={{ padding: 30 }}>Loading bar and restaurant operations…</div>
    );
  if (!data)
    return (
      <div style={{ color: "var(--b360-red)" }}>
        {error || "Hospitality is unavailable."}
      </div>
    );
  if (!data.enabled)
    return (
      <Card style={{ padding: 32, maxWidth: 620 }}>
        <PageHeader title="Bar & Restaurant" />
        <p style={{ color: "var(--b360-text-secondary)", marginBottom: 18 }}>
          Enable hospitality mode to manage tables, tabs, kitchen tickets, and bar orders.
        </p>
        {error && <p style={{ color: "var(--b360-red)" }}>{error}</p>}
        {isAdmin ? <Btn disabled={saving} onClick={() => toggleHospitality(true)}>{saving ? "Enabling…" : "Enable hospitality mode"}</Btn> : <p>An administrator must enable hospitality mode.</p>}
      </Card>
    );

  return (
    <div className="hospitality-hub fade-in">
      <div className="hub-header">
        <div>
          <div className="hub-breadcrumb"><Home size={16} /> <span>Bar &amp; Restaurant</span></div>
          <h1>Hospitality &amp; Restaurant Hub</h1>
          <p>Live floor plan, kitchen display system (KDS), open tabs and settlement controls</p>
        </div>
        <div className="hub-header-actions">
          <div className="hub-date"><Clock size={17} /><span>{new Date().toLocaleDateString('en-KE', { weekday:'long', day:'numeric', month:'long', year:'numeric' })}<strong>{new Date().toLocaleTimeString('en-KE', { hour:'2-digit', minute:'2-digit' })}</strong></span></div>
          {isAdmin && <Btn icon={<Plus size={15} />} onClick={() => openTableEditor()}>Add Table</Btn>}
          <Btn variant="secondary" icon={<RefreshCw size={14} />} onClick={load}>Refresh</Btn>
        </div>
      </div>

      {error && <div className="hub-alert hub-alert-error">{error}<button onClick={() => setError("")} aria-label="Dismiss error">×</button></div>}
      {successMessage && <div className="hub-alert hub-alert-success"><CheckCircle2 size={20} /><div><strong>{successMessage}</strong><span>The update has been applied in the Kitchen Display System.</span></div><button onClick={() => setSuccessMessage("")} aria-label="Dismiss success">×</button></div>}

      <div className="hub-tabs" role="tablist" aria-label="Hospitality areas">
        <button className={hubTab === "FLOOR" ? "active" : ""} onClick={() => setHubTab("FLOOR")}><Users size={18} /><span>Floor Plan &amp; Tables <b>({data.tables.length})</b></span></button>
        <button className={hubTab === "TABS" ? "active" : ""} onClick={() => setHubTab("TABS")}><ShoppingBag size={18} /><span>Open Tabs <b>({data.openTabs.length})</b></span></button>
        <button className={hubTab === "KDS" ? "active" : ""} onClick={() => setHubTab("KDS")}><ChefHat size={18} /><span>Kitchen KDS <b>({data.tickets.filter(t => t.station === "KITCHEN" && !["SERVED", "CANCELLED"].includes(t.status)).length})</b></span></button>
        <button className={hubTab === "OPS" ? "active" : ""} onClick={() => setHubTab("OPS")}><Package size={18} /><span>Operations &amp; Stock</span></button>
      </div>

      {hubTab === "FLOOR" && <section className="hub-panel">
        <div className="hub-panel-heading"><div><h2>Live floor plan</h2><p>Select a table to start a tab, open POS, or manage seating.</p></div><div className="hub-heading-actions"><Btn icon={<UtensilsCrossed size={14} />} onClick={() => openOrder()}>Takeaway order</Btn>{receiptProfile?.storefrontSlug && <a className="hub-link-button" href={`/shop/${encodeURIComponent(receiptProfile.storefrontSlug)}/qr`} target="_blank" rel="noreferrer">Print customer QR</a>}</div></div>
        {data.tables.length === 0 ? <div className="hub-empty">No tables configured. Add a table to begin dine-in service.</div> : <div className="hub-table-grid">{data.tables.map(table => {
          const tableTabs = data.openTabs.filter(tab => tab.hospitalityTableId === table.id);
          return <Card key={table.id} style={{ padding: 16, borderTop: `4px solid ${table.status === "OCCUPIED" ? "var(--b360-amber)" : "var(--b360-green)"}` }}>
            <div className="hub-card-row"><strong>{table.name}</strong><StatusBadge status={table.status} /></div>
            <div className="hub-muted">{table.area} · {table.capacity} seats</div>
            <div className="hub-table-amount">KES {table.openAmount.toLocaleString()}</div>
            <div className="hub-muted">{tableTabs.length ? `${tableTabs.length} open tab${tableTabs.length === 1 ? "" : "s"}` : "Available for service"}</div>
            <div className="hub-card-actions"><Btn small onClick={() => navigate(`/pos?tableId=${table.id}`)}>POS Order</Btn><Btn small variant="secondary" onClick={() => openOrder(table)}>+ Tab</Btn>{isAdmin && <button className="hub-icon-button" onClick={() => openTableEditor(table)} aria-label={`Edit ${table.name}`}><MoreHorizontal size={17} /></button>}</div>
          </Card>
        })}</div>}
      </section>}

      {hubTab === "TABS" && <section className="hub-panel"><div className="hub-panel-heading"><div><h2>Open customer tabs</h2><p>Manage active tables, receipts and settlement.</p></div><Btn icon={<UtensilsCrossed size={14} />} onClick={() => openOrder()}>New tab</Btn></div>{data.openTabs.length === 0 ? <div className="hub-empty">No open customer tabs currently awaiting settlement.</div> : <div className="hub-tabs-grid">{data.openTabs.map(order => { const table = data.tables.find(item => item.id === order.hospitalityTableId); return <Card key={order.id} style={{ padding: 16 }}><div className="hub-card-row"><strong>#{order.orderNumber}</strong><StatusBadge status={order.tabStatus || "OPEN"} /></div><div className="hub-muted">{table?.name || order.serviceType?.replace("_", " ") || "Takeaway"} · {order.customerName || "Walk-in Guest"}</div><div className="hub-tab-total">KES {order.subtotal.toLocaleString()}</div><div className="hub-muted">{order.items.length} item(s) · {age(order.createdAt)}</div><div className="hub-card-actions"><Btn small onClick={() => openSettlement(order)}>Settle</Btn><Btn small variant="secondary" icon={<Printer size={12} />} onClick={() => printOrderReceipt(order, receiptProfile)}>Receipt</Btn></div></Card> })}</div>}</section>}

      {hubTab === "KDS" && <section className="hub-panel"><div className="hub-panel-heading"><div><h2>Kitchen Display System</h2><p>Track food preparation tickets in real time.</p></div><span className="hub-live-dot">● Live</span></div>{data.tickets.filter(ticket => ticket.station === "KITCHEN" && !["SERVED", "CANCELLED"].includes(ticket.status)).length === 0 ? <div className="hub-empty">No active kitchen tickets.</div> : <div className="hub-kds-grid">{data.tickets.filter(ticket => ticket.station === "KITCHEN" && !["SERVED", "CANCELLED"].includes(ticket.status)).map(ticket => <Card key={ticket.id} style={{ padding: 16, border: ticket.status === "PREPARING" ? "2px solid var(--b360-amber)" : "1px solid var(--b360-border)", background: ticket.status === "PREPARING" ? "#FFF9E8" : "white" }}><div className="hub-kds-order"><strong>Order #{ticket.orderNumber}</strong><span>KITCHEN</span></div><div className="hub-muted">{ticket.tableName || "Takeaway"} <span>·</span> {new Date(ticket.createdAt).toLocaleTimeString('en-KE', { hour:'2-digit', minute:'2-digit' })}</div><div className="hub-divider" />{ticket.items.map(item => <div className="hub-kds-item" key={item.id}><b>{item.quantity}x</b> {item.productName}</div>)}{ticket.notes && <div className="hub-kds-note">Note: {ticket.notes}</div>}<div className="hub-kds-footer"><StatusBadge status={ticket.status} />{ticket.status === "NEW" && <Btn small onClick={() => advanceTicket(ticket.id, "PREPARING")}>Start preparing</Btn>}{ticket.status === "PREPARING" && <Btn small onClick={() => advanceTicket(ticket.id, "READY")}>Mark Ready</Btn>}{ticket.status === "READY" && <Btn small onClick={() => advanceTicket(ticket.id, "SERVED")}>Mark Served</Btn>}</div></Card>)}</div>}</section>}

      {hubTab === "OPS" && <section className="hub-panel"><div className="hub-panel-heading"><div><h2>Operations &amp; stock</h2><p>Reservations, shifts, menu profiles, purchasing, approvals and reports.</p></div><Btn onClick={() => navigate('/hospitality-operations')}>Open operations</Btn></div><div className="hub-ops-summary"><div><strong>{data.tables.filter(t => t.status === "OCCUPIED").length}</strong><span>Occupied tables</span></div><div><strong>{data.openTabs.length}</strong><span>Open tabs</span></div><div><strong>{data.tickets.filter(t => !["SERVED", "CANCELLED"].includes(t.status)).length}</strong><span>Active tickets</span></div><div><strong>Live</strong><span>Stock monitoring</span></div></div></section>}

      <div className="hub-capability-grid">
        <button onClick={() => setHubTab("FLOOR")}><span className="hub-capability-icon"><Users size={21} /></span><span><strong>Live Floor Plan</strong><small>View table status and manage seating in real time.</small></span></button>
        <button onClick={() => setHubTab("KDS")}><span className="hub-capability-icon"><ChefHat size={21} /></span><span><strong>Kitchen Display System</strong><small>Track and manage orders across kitchen stations.</small></span></button>
        <button onClick={() => setHubTab("TABS")}><span className="hub-capability-icon"><ShoppingBag size={21} /></span><span><strong>Open Tabs</strong><small>Manage customer tabs and payments effortlessly.</small></span></button>
        <button onClick={() => setHubTab("OPS")}><span className="hub-capability-icon"><Package size={21} /></span><span><strong>Operations &amp; Stock</strong><small>Monitor inventory, ingredients and stock levels.</small></span></button>
      </div>

      {showTable && (

        <Modal
          title={
            editingTable ? "Edit restaurant table" : "Add restaurant table"
          }
          onClose={() => {
            setShowTable(false);
            setEditingTable(null);
          }}
          footer={
            <>
              <Btn
                variant="secondary"
                onClick={() => {
                  setShowTable(false);
                  setEditingTable(null);
                }}
              >
                Cancel
              </Btn>
              <Btn
                disabled={saving || !tableDraft.name.trim()}
                onClick={saveTable}
              >
                {saving ? "Saving…" : "Save table"}
              </Btn>
            </>
          }
        >
          <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
            {modalError&&<div role="alert" style={{padding:10,borderRadius:8,background:'var(--b360-red-bg)',color:'var(--b360-red)',fontSize:12}}>{modalError}</div>}
            <Input
              label="Table name"
              value={tableDraft.name}
              onChange={(v) => setTableDraft({ ...tableDraft, name: v })}
              placeholder="e.g. Terrace 4"
            />
            <Input
              label="Area"
              value={tableDraft.area}
              onChange={(v) => setTableDraft({ ...tableDraft, area: v })}
            />
            <Input
              label="Seats"
              type="number"
              value={tableDraft.capacity}
              onChange={(v) => setTableDraft({ ...tableDraft, capacity: v })}
            />
          </div>
        </Modal>
      )}
      {settleOrder && (
        <SettlementModal order={settleOrder} onClose={() => setSettleOrder(null)} onComplete={load}/>
      )}
      {showOrder && (
        <Modal
          extraWide
          title={
            orderTable ? `New sale · ${orderTable.name}` : "New takeaway sale"
          }
          onClose={() => {
            setShowOrder(false);
            setOrderTable(null);
            setServiceType("");
          }}
          footer={
            <>
              <div style={{ marginRight: "auto", fontWeight: 800 }}>
                {cartProducts.reduce(
                  (sum, product) => sum + cart[product.id],
                  0,
                )}{" "}
                items · KES {total.toLocaleString()}
              </div>
              <Btn
                variant="secondary"
                onClick={() => {
                  setShowOrder(false);
                  const tableParam = orderTable ? `?tableId=${orderTable.id}` : "";
                  navigate(`/pos${tableParam}`);
                }}
              >
                🛒 Open Full POS
              </Btn>
              <Btn
                variant="secondary"
                onClick={() => {
                  setShowOrder(false);
                  setOrderTable(null);
                  setServiceType("");
                }}
              >
                Cancel
              </Btn>
              <Btn
                disabled={saving || !cartProducts.length}
                onClick={submitOrder}
              >
                {saving ? "Opening…" : "Open tab"}
              </Btn>
            </>
          }
        >
          <div className="hospitality-sale-grid">
            <div className="hospitality-sale-details" style={{ display: "flex", flexDirection: "column", gap: 14, paddingRight:22 }}>
              {modalError&&<div role="alert" style={{padding:10,borderRadius:8,background:'var(--b360-red-bg)',color:'var(--b360-red)',fontSize:12}}>{modalError}</div>}
              <div>
                <div style={{fontSize:12,fontWeight:700,marginBottom:7}}>Service</div>
                <div style={{display:'grid',gridTemplateColumns:'repeat(3,1fr)',border:'1px solid var(--b360-border)',borderRadius:9,overflow:'hidden'}}>
                  {[["DINE_IN","Dine in"],["TAKEAWAY","Take away"],["DELIVERY","Delivery"]].map(([value,label],index)=><button key={value} type="button" onClick={()=>{setServiceType(value);if(value!=="DINE_IN")setOrderTable(null)}} style={{padding:'11px 6px',border:0,borderLeft:index ? '1px solid var(--b360-border)' : 0,background:serviceType===value ? 'var(--b360-green)' : 'white',color:serviceType===value ? 'white' : 'var(--b360-text)',fontWeight:700,cursor:'pointer'}}>{label}</button>)}
                </div>
              </div>
              {serviceType==="DINE_IN"&&<Select label="Table" value={orderTable?.id||""} onChange={id=>setOrderTable(data.tables.find(table=>table.id===id)||null)} placeholder="Select a table" options={data.tables.filter(table=>!table.mergedIntoTableId).map(table=>({value:table.id,label:`${table.name} · ${table.area}`}))}/>}
              <div>
                <div style={{fontSize:12,fontWeight:700,marginBottom:7}}>Guests</div>
                <div style={{display:'grid',gridTemplateColumns:'64px 1fr 64px',border:'1px solid var(--b360-border)',borderRadius:9,overflow:'hidden'}}>
                  <button type="button" aria-label="Remove guest" onClick={()=>setGuests(String(Math.max(1,(Number(guests)||1)-1)))} style={{border:0,background:'white',padding:10,cursor:'pointer'}}><Minus size={15}/></button>
                  <strong style={{display:'grid',placeItems:'center',borderLeft:'1px solid var(--b360-border)',borderRight:'1px solid var(--b360-border)'}}>{Math.max(1,Number(guests)||1)}</strong>
                  <button type="button" aria-label="Add guest" onClick={()=>setGuests(String(Math.max(1,(Number(guests)||1)+1)))} style={{border:0,background:'white',padding:10,cursor:'pointer'}}><Plus size={15}/></button>
                </div>
              </div>
              <Input
                label="Customer"
                value={customerName}
                onChange={setCustomerName}
              />
              <Input
                label="Phone (optional)"
                value={customerPhone}
                onChange={setCustomerPhone}
              />
              <label style={{display:'grid',gap:5,fontSize:12,fontWeight:600,color:'var(--b360-text-secondary)'}}>Kitchen / bar notes
                <textarea value={notes} maxLength={200} onChange={event=>setNotes(event.target.value)} placeholder="Add any special instructions…" rows={3} style={{resize:'vertical',padding:'11px 13px',border:'1px solid var(--b360-border)',borderRadius:9,fontFamily:'inherit'}} />
                <span style={{fontSize:10,textAlign:'right'}}>{notes.length} / 200</span>
              </label>
              {cartProducts.length > 0 && (
                <div
                  style={{
                    marginTop: 5,
                    padding: 14,
                    border: "1px solid var(--b360-border)",
                    borderRadius: 10,
                  }}
                >
                  <div style={{display:'flex',justifyContent:'space-between',alignItems:'center'}}><b style={{ fontSize: 13,display:'flex',gap:7,alignItems:'center' }}><ShoppingBag size={15} color="var(--b360-green)"/>Current order</b><span style={{fontSize:11,fontWeight:700,color:'var(--b360-green)',background:'var(--b360-green-bg)',padding:'3px 8px',borderRadius:12}}>{cartProducts.reduce((sum,p)=>sum+cart[p.id],0)} items</span></div>
                  {cartProducts.map((product) => {
                    const profile=menuProfiles.find(item=>item.productId===product.id);
                    const state=optionState(product.id);
                    return (
                    <div
                      key={product.id}
                      style={{
                        display: "flex",
                        alignItems:'center',gap:9,
                        fontSize: 12,marginTop: 10,
                      }}
                    >
                      {product.imageUrl ? <img src={product.imageUrl} alt="" style={{width:42,height:42,objectFit:'cover',borderRadius:7}}/> : <div style={{width:42,height:42,borderRadius:7,background:'var(--b360-bg)',display:'grid',placeItems:'center'}}><ShoppingBag size={15}/></div>}
                      <span style={{flex:1}}><b style={{display:'block'}}>{product.name}</b>{cart[product.id]} × KES {product.sellingPrice.toLocaleString()}
                        {(profile?.sizes.length||0)>0&&<span style={{display:'block',marginTop:5}}><small>Size: </small>{profile!.sizes.map(option=><label key={option.name} style={{display:'inline-flex',alignItems:'center',gap:3,marginRight:8,fontSize:10}}><input type="radio" name={`size-${product.id}`} checked={state.modifiers.some(item=>item.name===option.name)} onChange={()=>selectOneModifier(product.id,profile!.sizes,option)}/>{option.name}{option.priceDelta?` (+${option.priceDelta})`:''}</label>)}</span>}
                        {(profile?.variants.length||0)>0&&<span style={{display:'block',marginTop:5}}><small>Variant: </small>{profile!.variants.map(option=><label key={option.name} style={{display:'inline-flex',alignItems:'center',gap:3,marginRight:8,fontSize:10}}><input type="radio" name={`variant-${product.id}`} checked={state.modifiers.some(item=>item.name===option.name)} onChange={()=>selectOneModifier(product.id,profile!.variants,option)}/>{option.name}{option.priceDelta?` (+${option.priceDelta})`:''}</label>)}</span>}
                        {(profile?.extras.length||0)>0&&<span style={{display:'block',marginTop:5}}><small>Extras: </small>{profile!.extras.map(option=><label key={option.name} style={{display:'inline-flex',alignItems:'center',gap:3,marginRight:8,fontSize:10}}><input type="checkbox" checked={state.modifiers.some(item=>item.name===option.name)} onChange={()=>toggleModifier(product.id,option)}/>{option.name}{option.priceDelta?` (+${option.priceDelta})`:''}</label>)}</span>}
                        <input aria-label={`Note for ${product.name}`} value={state.note} onChange={event=>updateOption(product.id,{note:event.target.value})} placeholder="Item note" style={{display:'block',width:'100%',marginTop:6,padding:6,border:'1px solid var(--b360-border)',borderRadius:6}}/>
                        {isAdmin&&<span style={{display:'flex',gap:7,marginTop:5,alignItems:'center'}}><input aria-label={`Discount for ${product.name}`} type="number" value={state.discount} onChange={event=>updateOption(product.id,{discount:event.target.value})} placeholder="Discount" style={{width:85,padding:5,border:'1px solid var(--b360-border)',borderRadius:6}}/><label><input type="checkbox" checked={state.complimentary} onChange={event=>updateOption(product.id,{complimentary:event.target.checked})}/> Complimentary</label></span>}
                      </span>
                      <b style={{whiteSpace:'nowrap'}}>
                        KES{" "}
                        {(
                          cart[product.id] * (product.sellingPrice+state.modifiers.reduce((sum,item)=>sum+item.priceDelta,0))-(Number(state.discount)||0)
                        ).toLocaleString()}
                      </b>
                      <button type="button" aria-label={`Remove ${product.name}`} onClick={()=>change(product.id,-cart[product.id])} style={{border:'1px solid var(--b360-border)',background:'white',borderRadius:7,padding:7,cursor:'pointer',color:'var(--b360-text-secondary)'}}><Trash2 size={14}/></button>
                    </div>
                  )})}
                  {cartProducts.some(product=>menuProfiles.find(profile=>profile.productId===product.id)?.ageRestricted)&&<label style={{display:'flex',gap:8,alignItems:'center',marginTop:12,padding:10,background:'var(--b360-amber-bg)',borderRadius:8,fontSize:12,fontWeight:700}}><input type="checkbox" checked={ageVerified} onChange={event=>setAgeVerified(event.target.checked)}/> I verified the customer meets the required minimum age</label>}
                  <div style={{borderTop:'1px solid var(--b360-border)',marginTop:12,paddingTop:11,display:'flex',justifyContent:'space-between'}}><b>Total</b><strong style={{fontSize:18,color:'var(--b360-green)'}}>KES {total.toLocaleString()}</strong></div>
                </div>
              )}
            </div>
            <div className="hospitality-sale-catalog" style={{borderLeft:'1px solid var(--b360-border)',paddingLeft:22}}>
              <div style={{ position: "relative", marginBottom: 9 }}>
                <Search
                  size={15}
                  style={{
                    position: "absolute",
                    left: 11,
                    top: 11,
                    color: "var(--b360-text-secondary)",
                  }}
                />
                <input
                  value={menuSearch}
                  onChange={(event) => setMenuSearch(event.target.value)}
                  placeholder="Search food or drinks…"
                  style={{
                    width: "100%",
                    padding: "10px 12px 10px 34px",
                    border: "1px solid var(--b360-border)",
                    borderRadius: 9,
                  }}
                />
              </div>
              <div
                style={{
                  display: "flex",
                  gap: 6,
                  overflowX: "auto",
                  paddingBottom: 9,
                }}
              >
                {menuCategories.map((category) => (
                  <button
                    key={category}
                    onClick={() => setMenuCategory(category)}
                    style={{
                      whiteSpace: "nowrap",
                      padding: "6px 10px",
                      borderRadius: 16,
                      border: "1px solid var(--b360-border)",
                      background:
                        menuCategory === category
                          ? "var(--b360-green)"
                          : "white",
                      color: menuCategory === category ? "white" : "inherit",
                      cursor: "pointer",
                    }}
                  >
                    {category}
                  </button>
                ))}
              </div>
              <div
                style={{
                  maxHeight: 390,
                  overflowY: "auto",
                  display: "flex",
                  flexDirection:'column',
                  gap: 8,
                }}
              >
                {visibleProducts.map((product) => (
                  <button
                    key={product.id}
                    onClick={() => change(product.id, 1)}
                    style={{
                      textAlign: "left",
                      padding: 9,
                      border: `1px solid ${cart[product.id] ? "var(--b360-green)" : "var(--b360-border)"}`,
                      borderRadius: 10,
                      background: cart[product.id]
                        ? "var(--b360-green-bg)"
                        : "white",
                      cursor: "pointer",display:'flex',alignItems:'center',gap:12,width:'100%'
                    }}
                  >
                    {product.imageUrl ? <img src={product.imageUrl} alt="" style={{width:68,height:68,objectFit:'cover',borderRadius:8,background:'var(--b360-bg)'}}/> : <div style={{width:68,height:68,flexShrink:0,borderRadius:8,background:'var(--b360-bg)',display:'grid',placeItems:'center'}}><ShoppingBag size={22} color="var(--b360-text-secondary)"/></div>}
                    <div style={{flex:1,minWidth:0}}><b style={{fontSize:14,display:'block'}}>{product.name}</b><span style={{fontSize:11,color:'var(--b360-text-secondary)'}}>{product.category || 'Menu'} · Stock {product.currentStock}</span>{(menuProfiles.find(profile=>profile.productId===product.id)?.comboProductIds.length||0)>0&&<span style={{fontSize:10,color:'var(--b360-blue)',display:'block'}}>Combo includes {menuProfiles.find(profile=>profile.productId===product.id)!.comboProductIds.map(id=>products.find(item=>item.id===id)?.name||id).join(', ')}</span>}<strong style={{display:'block',marginTop:7}}>KES {product.sellingPrice.toLocaleString()}</strong></div>
                    {cart[product.id] ? <div onClick={event=>event.stopPropagation()} style={{display:'flex',alignItems:'center',border:'1px solid var(--b360-border)',borderRadius:8,overflow:'hidden',background:'white'}}><button type="button" onClick={()=>change(product.id,-1)} style={{border:0,background:'white',padding:'8px 10px',color:'var(--b360-green)',cursor:'pointer'}}>−</button><b style={{minWidth:24,textAlign:'center'}}>{cart[product.id]}</b><button type="button" onClick={()=>change(product.id,1)} style={{border:0,background:'white',padding:'8px 10px',color:'var(--b360-green)',cursor:'pointer'}}>+</button></div> : <span style={{border:'1px solid var(--b360-green)',color:'var(--b360-green)',borderRadius:8,padding:'7px 12px',fontWeight:700,whiteSpace:'nowrap'}}>+ Add</span>}
                  </button>
                ))}
              </div>
              {visibleProducts.length === 0 && (
                <div
                  style={{
                    padding: 30,
                    textAlign: "center",
                    color: "var(--b360-text-secondary)",
                  }}
                >
                  No menu items match this search.
                </div>
              )}
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
}

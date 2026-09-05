import React, { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  ChefHat,
  Clock,
  Minus,
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
          Enable hospitality mode to manage tables, tabs, kitchen tickets, and
          bar orders.
        </p>
        {error && <p style={{ color: "var(--b360-red)" }}>{error}</p>}
        {isAdmin ? (
          <Btn disabled={saving} onClick={() => toggleHospitality(true)}>
            {saving ? "Enabling…" : "Enable hospitality mode"}
          </Btn>
        ) : (
          <p>An administrator must enable hospitality mode.</p>
        )}
      </Card>
    );

  return (
    <div
      className="fade-in"
      style={{ display: "flex", flexDirection: "column", gap: 18 }}
    >
      <PageHeader
        title="Bar & Restaurant"
        action={
          <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
            <Btn
              variant="secondary"
              icon={<RefreshCw size={14} />}
              onClick={load}
            >
              Refresh
            </Btn>
            {isAdmin && (
              <Btn
                variant="secondary"
                onClick={async () => {
                  if (
                    window.confirm(
                      "Disable hospitality mode? All tabs and kitchen/bar tickets must be completed first.",
                    )
                  ) {
                    await toggleHospitality(false);
                  }
                }}
              >
                Disable mode
              </Btn>
            )}
            {isAdmin && (
              <Btn icon={<Plus size={14} />} onClick={() => openTableEditor()}>
                Add table
              </Btn>
            )}
            <Btn
              icon={<UtensilsCrossed size={14} />}
              onClick={() => openOrder()}
            >
              Takeaway order
            </Btn>
            <Btn variant="secondary" icon={<ChefHat size={14}/>} onClick={()=>window.location.assign('/kitchen-display')}>Kitchen display</Btn>
          </div>
        }
      />
      {error && (
        <div
          style={{
            padding: 10,
            background: "var(--b360-red-bg)",
            color: "var(--b360-red)",
            borderRadius: 8,
          }}
        >
          {error}
        </div>
      )}
      <div className="responsive-grid responsive-grid-3">
        <KpiCard
          title="Tables"
          value={String(data.tables.length)}
          change={`${data.tables.filter((t) => t.status === "OCCUPIED").length} occupied`}
          icon={<Users size={18} />}
          color="var(--b360-blue)"
        />
        <KpiCard
          title="Open tabs"
          value={String(data.openTabs.length)}
          change={`KES ${data.openTabs.reduce((s, o) => s + o.subtotal, 0).toLocaleString()}`}
          icon={<UtensilsCrossed size={18} />}
          color="var(--b360-amber)"
        />
        <KpiCard
          title="Active tickets"
          value={String(
            data.tickets.filter(
              (t) => t.station === "KITCHEN" && !["SERVED", "CANCELLED"].includes(t.status),
            ).length,
          )}
          change="Kitchen"
          icon={<ChefHat size={18} />}
          color="var(--b360-green)"
        />
      </div>

      <h2 style={{ fontSize: 17 }}>Floor & tables</h2>
      {data.tables.length===0&&<Card style={{padding:20,color:'var(--b360-text-secondary)'}}>No tables configured. Add a table to begin dine-in service, or use Takeaway order.</Card>}
      <div
        style={{
          display: "grid",
          gridTemplateColumns: "repeat(auto-fill,minmax(190px,1fr))",
          gap: 12,
        }}
      >
        {data.tables.map((table) => {
          const tableTabs = data.openTabs.filter(
            (tab) => tab.hospitalityTableId === table.id,
          );
          return (
            <Card
              key={table.id}
              style={{
                padding: 16,
                borderTop: `4px solid ${table.status === "OCCUPIED" ? "var(--b360-amber)" : "var(--b360-green)"}`,
              }}
            >
              <div
                style={{
                  display: "flex",
                  justifyContent: "space-between",
                  gap: 8,
                }}
              >
                <b>{table.name}</b>
                <div style={{ display: "flex", gap: 5, alignItems: "center" }}>
                  {isAdmin && (
                    <button
                      aria-label={`Edit ${table.name}`}
                      onClick={() => openTableEditor(table)}
                      style={{
                        border: 0,
                        background: "transparent",
                        cursor: "pointer",
                        color: "var(--b360-text-secondary)",
                      }}
                    >
                      <Pencil size={14} />
                    </button>
                  )}
                  <StatusBadge status={table.status} />
                </div>
              </div>
              <div
                style={{
                  fontSize: 12,
                  color: "var(--b360-text-secondary)",
                  margin: "7px 0",
                }}
              >
                {table.area} · {table.capacity} seats
              </div>
              {tableTabs.length > 0 ? (
                <>
                  <div style={{ fontSize: 12, marginBottom: 4 }}>
                    {tableTabs.length} customer tab{tableTabs.length === 1 ? "" : "s"} · {tableTabs.reduce((sum, tab) => sum + (tab.guestCount || 1), 0)} guest(s)
                  </div>
                  <strong style={{ display: "block", marginBottom: 9 }}>
                    KES {table.openAmount.toLocaleString()}
                  </strong>
                  <div style={{ fontSize: 11, color: "var(--b360-text-secondary)", marginBottom: 9 }}>
                    Receipts: {tableTabs.map((tab) => tab.orderNumber).join(", ")}
                  </div>
                  <div style={{ display: "flex", gap: 6 }}>
                    <Btn small onClick={() => navigate(`/pos?tableId=${table.id}`)}>
                      🛒 POS Order
                    </Btn>
                    <Btn small variant="secondary" onClick={() => openOrder(table)}>
                      + Tab
                    </Btn>
                  </div>
                </>
              ) : (
                <div style={{ display: "flex", gap: 6 }}>
                  <Btn small onClick={() => navigate(`/pos?tableId=${table.id}`)}>
                    🛒 Order in POS
                  </Btn>
                  <Btn small variant="secondary" onClick={() => openOrder(table)}>
                    Quick Tab
                  </Btn>
                </div>
              )}
            </Card>
          );
        })}
      </div>

      <h2 style={{ fontSize: 17 }}>Open tabs</h2>
      {data.openTabs.length === 0 ? (
        <Card style={{ padding: 20, color: "var(--b360-text-secondary)" }}>
          No open tabs. Select an available table or create a takeaway order.
        </Card>
      ) : (
        <Card>
          <DataTable
            headers={["Table", "Receipt / Tab", "Customer", "Guests / Items", "Open", "Amount", "Status", "Actions"]}
            rows={data.openTabs.map((order) => {
            const table = data.tables.find(
              (item) => item.id === order.hospitalityTableId,
            );
            const available = data.tables.filter(
              (item) => item.id !== order.hospitalityTableId,
            );
            return [
              <div><b>{table?.name || order.serviceType?.replace("_", " ") || "Takeaway"}</b><div style={{fontSize:10,color:"var(--b360-text-secondary)"}}>{table?.area || "Off premises"}</div></div>,
              <span style={{fontFamily:"monospace",fontWeight:800,color:"var(--b360-green)"}}>{order.orderNumber}</span>,
              <div><b>{order.customerName || "Walk-in Guest"}</b><div style={{fontSize:10,color:"var(--b360-text-secondary)"}}>{order.customerPhone || "No phone"}</div></div>,
              `${order.guestCount || 1} guest(s) · ${order.items.length} item(s)`,
              <span><Clock size={12} style={{verticalAlign:"-2px",marginRight:4}}/>{age(order.createdAt)}</span>,
              <strong>KES {order.subtotal.toLocaleString()}</strong>,
              <StatusBadge status={order.tabStatus || "OPEN"} />,
              <div style={{ display: "flex", gap: 7, flexWrap: "wrap", minWidth: 250 }}>
                  <Btn small onClick={() => openSettlement(order)}>
                    Settle
                  </Btn>
                  <Btn
                    small
                    variant="secondary"
                    icon={<Printer size={12} />}
                    onClick={() => printOrderReceipt(order, receiptProfile)}
                  >
                    Receipt
                  </Btn>
                  {table && available.length > 0 && (
                    <select
                      aria-label={`Transfer ${order.orderNumber}`}
                      defaultValue=""
                      onChange={(event) =>
                        transferTab(order.id, event.target.value)
                      }
                      style={{
                        padding: "6px 8px",
                        border: "1px solid var(--b360-border)",
                        borderRadius: 7,
                        fontSize: 12,
                      }}
                    >
                      <option value="">Transfer table…</option>
                      {available.map((item) => (
                        <option key={item.id} value={item.id}>
                          {item.name} · {item.area}
                        </option>
                      ))}
                    </select>
                  )}
                </div>,
            ];
          })}
          />
        </Card>
      )}

      <h2 style={{ fontSize: 17 }}>Kitchen tickets</h2>
      {data.tickets.every((t) => t.station !== "KITCHEN" || ["SERVED", "CANCELLED"].includes(t.status)) ? (
        <Card style={{ padding: 20, color: "var(--b360-text-secondary)" }}>
          No active kitchen tickets.
        </Card>
      ) : (
        <div className="responsive-grid responsive-grid-3">
          {data.tickets.filter(ticket => ticket.station === "KITCHEN")
            .filter((t) => t.station === "KITCHEN" && !["SERVED", "CANCELLED"].includes(t.status))
            .map((ticket) => {
              const ticketStatusLabel: Record<string, string> = {
                NEW: "Waiting to start",
                PREPARING: "Being prepared",
                READY: "Ready for service",
              };
              return (
              <Card
                key={ticket.id}
                style={{
                  padding: 16,
                  borderLeft: `4px solid ${ticket.status === "READY" ? "var(--b360-green)" : "var(--b360-amber)"}`,
                }}
              >
                <div style={{ display: "flex", justifyContent: "space-between", gap: 8, alignItems: "start" }}>
                  <div>
                    <div style={{ fontSize: 10, textTransform: "uppercase", color: "var(--b360-text-secondary)", fontWeight: 700 }}>Preparation station</div>
                    <b style={{ fontSize: 16 }}>Kitchen</b>
                  </div>
                  <div style={{ textAlign: "right" }}>
                    <StatusBadge status={ticket.status} />
                    <div style={{ fontSize: 10, color: "var(--b360-text-secondary)", marginTop: 3 }}>{ticketStatusLabel[ticket.status] || ticket.status}</div>
                  </div>
                </div>
                <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 8, margin: "12px 0", padding: 10, background: "var(--b360-bg)", borderRadius: 8 }}>
                  <div><div style={{ fontSize: 10, color: "var(--b360-text-secondary)" }}>Table / service</div><b style={{ fontSize: 12 }}>{ticket.tableName || "Takeaway"}</b></div>
                  <div><div style={{ fontSize: 10, color: "var(--b360-text-secondary)" }}>Receipt / tab</div><b style={{ fontSize: 12, fontFamily: "monospace", color: "var(--b360-green)" }}>{ticket.orderNumber}</b></div>
                  <div><div style={{ fontSize: 10, color: "var(--b360-text-secondary)" }}>Opened</div><span style={{ fontSize: 12 }}><Clock size={12} style={{ verticalAlign: "-2px", marginRight: 4 }} />{age(ticket.createdAt)} ago</span></div>
                  <div><div style={{ fontSize: 10, color: "var(--b360-text-secondary)" }}>Ticket ID</div><span title={ticket.id} style={{ fontSize: 12, fontFamily: "monospace" }}>{ticket.id.slice(0, 8)}</span></div>
                </div>
                {ticket.items.map((item) => (
                  <div key={item.id} style={{ fontSize: 13, marginBottom: 4 }}>
                    <b>{item.quantity}×</b> {item.productName}
                  </div>
                ))}
                {ticket.notes && (
                  <div
                    style={{
                      fontSize: 12,
                      marginTop: 8,
                      color: "var(--b360-amber)",
                    }}
                  >
                    {ticket.notes}
                  </div>
                )}
                <div style={{ display: "flex", gap: 6, marginTop: 12 }}>
                  {ticket.status === "NEW" && (
                    <Btn
                      small
                      onClick={() => advanceTicket(ticket.id, "PREPARING")}
                    >
                      Start
                    </Btn>
                  )}
                  {ticket.status === "PREPARING" && (
                    <Btn
                      small
                      onClick={() => advanceTicket(ticket.id, "READY")}
                    >
                      Ready
                    </Btn>
                  )}
                  {ticket.status === "READY" && (
                    <Btn
                      small
                      onClick={() => advanceTicket(ticket.id, "SERVED")}
                    >
                      Served
                    </Btn>
                  )}
                  {ticket.status !== "DELAYED" && <Btn small variant="secondary" onClick={() => advanceTicket(ticket.id,"DELAYED")}>Delay</Btn>}
                  <Btn
                    small
                    variant="danger"
                    onClick={() => window.confirm(`Cancel ticket ${ticket.orderNumber}? This cannot be undone.`) && advanceTicket(ticket.id, "CANCELLED")}
                  >
                    Cancel
                  </Btn>
                </div>
              </Card>
              );
            })}
        </div>
      )}

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

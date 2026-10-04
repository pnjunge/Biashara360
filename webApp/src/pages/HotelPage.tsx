import React, { useEffect, useRef, useState } from "react";
import { Building2, Plus, RefreshCw, Printer } from "lucide-react";
import { useAuth } from "../App";
import { accessApi, paymentApi } from "../services/api";
import {
  hotelApi,
  HotelDashboard,
  RoomType,
  HotelRoom,
  Reservation,
  Folio,
  HotelPayment,
  HotelReport,
} from "../services/hotelApi";
import {
  Card,
  Btn,
  PageHeader,
  Modal,
  DataTable,
  KpiCard,
  StatusBadge,
} from "../components/ui";
import {
  printReport,
  downloadReportCsv,
  ShareableReport,
} from "../utils/reportShare";
import "./HotelPage.css";

const today = () =>
  new Intl.DateTimeFormat("en-CA", {
    timeZone: "Africa/Nairobi",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).format(new Date());
function addDays(date: string, days: number) {
  const d = new Date(`${date}T12:00:00Z`);
  if (!date || Number.isNaN(d.getTime())) return "";
  d.setUTCDate(d.getUTCDate() + days);
  return d.toISOString().slice(0, 10);
}
const money = (cents: number) =>
  `KES ${(cents / 100).toLocaleString("en-KE", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
function amount(value: string) {
  const n = Number(value);
  if (
    !Number.isFinite(n) ||
    n <= 0 ||
    !Number.isSafeInteger(Math.round(n * 100))
  )
    throw new Error("Enter a positive amount.");
  return Math.round(n * 100);
}
const emptyStay = () => ({
  roomId: "",
  guestName: "",
  guestPhone: "",
  guestEmail: "",
  guests: 1,
  arrival: today(),
  departure: addDays(today(), 1),
  source: "DIRECT",
  reference: "",
  notes: "",
});
export default function HotelPage() {
  const { user } = useAuth();
  const admin = ["ADMIN", "BUSINESS_ADMIN", "SUPERADMIN"].includes(
    user?.role || "",
  );
  const [permissions, setPermissions] = useState<string[]>([]);
  const can = (code: string) => admin || permissions.includes(`hotel.${code}`);
  const [enabled, setEnabled] = useState<boolean | null>(null);
  const [data, setData] = useState<HotelDashboard | null>(null);
  const [tab, setTab] = useState("RESERVATIONS");
  const [arrival, setArrival] = useState(today());
  const [departure, setDeparture] = useState(addDays(today(), 30));
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [busy, setBusy] = useState(false);
  const [loading, setLoading] = useState(true);
  const loadVersion = useRef(0);
  const [modal, setModal] = useState("");
  const [editingId, setEditingId] = useState<string | null>(null);
  const [stay, setStay] = useState(emptyStay);
  const [type, setType] = useState({
    name: "",
    description: "",
    capacity: 2,
    price: "",
    isActive: true,
  });
  const [room, setRoom] = useState({
    number: "",
    floor: "",
    typeId: "",
    isActive: true,
  });
  const [clean, setClean] = useState({ status: "CLEAN", notes: "" });
  const [block, setBlock] = useState({
    roomId: "",
    arrival: today(),
    departure: addDays(today(), 1),
    reason: "",
  });
  const [folio, setFolio] = useState<Folio | null>(null);
  const [payments, setPayments] = useState<HotelPayment[]>([]);
  const [entry, setEntry] = useState({
    kind: "CHARGE",
    description: "",
    amount: "",
  });
  const [payment, setPayment] = useState({ method: "CASH", amount: "" });
  const [requestId, setRequestId] = useState(() => crypto.randomUUID());
  const [report, setReport] = useState<HotelReport | null>(null);
  const [channel, setChannel] = useState({
    roomId: "",
    source: "",
    calendar: "",
  });
  const [confirmation, setConfirmation] = useState<{
    id: string;
    action: string;
    name: string;
  } | null>(null);
  const getError = (e: any) =>
    e.response?.data?.message || e.message || "Hotel request failed.";
  async function load() {
    const version = ++loadVersion.current;
    setLoading(true);
    setError("");
    setData(null);
    try {
      const response = await hotelApi.dashboard(arrival, departure);
      if (version === loadVersion.current) setData(response);
    } catch (e) {
      if (version === loadVersion.current) setError(getError(e));
    } finally {
      if (version === loadVersion.current) setLoading(false);
    }
  }
  useEffect(() => {
    let active = true;
    Promise.all([hotelApi.status(), accessApi.me()])
      .then(([status, access]) => {
        if (active) {
          setEnabled(status.enabled);
          setPermissions(access.data?.permissions || []);
        }
      })
      .catch((e) => {
        if (active) setError(getError(e));
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, []);
  useEffect(() => {
    if (enabled) {
      load();
      setReport(null);
    }
  }, [enabled, arrival, departure]);
  async function run(work: () => Promise<void>) {
    if (busy) return;
    setBusy(true);
    setError("");
    setMessage("");
    try {
      await work();
    } catch (e) {
      setError(getError(e));
    } finally {
      setBusy(false);
    }
  }
  const close = () => {
    if (!busy) {
      setModal("");
      setFolio(null);
      setConfirmation(null);
    }
  };
  const label = (name: string, input: React.ReactNode) => (
    <label className="hotel-field">
      <span>{name}</span>
      {input}
    </label>
  );
  const roomName = (id: string) =>
    data?.rooms.find((r) => r.id === id)?.number || id;
  const roomOptions = (
    <>
      <option value="">Choose room</option>
      {data?.rooms
        .filter((r) => r.isActive)
        .map((r) => (
          <option key={r.id} value={r.id}>
            {r.number} · {data.roomTypes.find((t) => t.id === r.typeId)?.name}
          </option>
        ))}
    </>
  );
  function editStay(r?: Reservation) {
    setEditingId(r?.id || null);
    setStay(
      r
        ? {
            roomId: r.roomId,
            guestName: r.guestName,
            guestPhone: r.guestPhone,
            guestEmail: r.guestEmail,
            guests: r.guests,
            arrival: r.arrival,
            departure: r.departure,
            source: r.source,
            reference: r.reference,
            notes: r.notes,
          }
        : emptyStay(),
    );
    setModal("RESERVE");
    setError("");
  }
  function editType(r?: RoomType) {
    setEditingId(r?.id || null);
    setType(
      r
        ? {
            name: r.name,
            description: r.description,
            capacity: r.capacity,
            price: String(r.nightlyRateCents / 100),
            isActive: r.isActive,
          }
        : { name: "", description: "", capacity: 2, price: "", isActive: true },
    );
    setModal("TYPE");
  }
  function editRoom(r?: HotelRoom) {
    setEditingId(r?.id || null);
    setRoom(
      r
        ? {
            number: r.number,
            floor: r.floor,
            typeId: r.typeId,
            isActive: r.isActive,
          }
        : { number: "", floor: "", typeId: "", isActive: true },
    );
    setModal("ROOM");
  }
  async function refreshFolio(id: string) {
    const [f, p] = await Promise.all([
      hotelApi.folio(id),
      hotelApi.payments(id),
    ]);
    setFolio(f);
    setPayments(p);
    return f;
  }
  function openFolio(r: Reservation, checkout = false) {
    run(async () => {
      const f = await refreshFolio(r.id);
      setEntry(checkout && f.balanceCents < 0 && can("refunds")
        ? { kind: "CASH_REFUND", description: "Guest credit refunded before checkout", amount: String(-f.balanceCents / 100) }
        : { kind: "CHARGE", description: "", amount: "" });
      setPayment({ method: "CASH", amount: checkout && f.balanceCents > 0 ? String(f.balanceCents / 100) : "" });
      setRequestId(crypto.randomUUID());
      setModal("FOLIO");
    });
  }
  function folioDocument(): ShareableReport {
    const f = folio!;
    return {
      title: `Guest Folio · Room ${roomName(f.reservation.roomId)}`,
      businessName: user?.businessName || "Biashara360",
      period: `${f.reservation.guestName} · ${f.reservation.arrival} to ${f.reservation.departure}`,
      summary: [
        ["Charges", money(f.totalChargeCents)],
        ["Paid", money(f.totalPaidCents)],
        ["Balance", money(f.balanceCents)],
      ],
      columns: ["Date", "Description", "Type", "Amount (KES)"],
      rows: [
        [
          f.reservation.arrival,
          `Accommodation · ${f.reservation.arrival} to ${f.reservation.departure}`,
          "ROOM",
          f.roomChargeCents / 100,
        ],
        ...f.entries.map((e) => [
          e.createdAt,
          e.description,
          e.kind,
          e.amountCents / 100,
        ]),
      ],
    };
  }
  function reportDocument(): ShareableReport {
    const r = report!;
    return {
      title: "Hotel Occupancy & Accommodation",
      businessName: user?.businessName || "Biashara360",
      period: `${arrival} to ${departure} (end excluded)`,
      summary: [
        ["Occupancy", `${r.occupancyPercent.toFixed(1)}%`],
        ["Booked accommodation", money(r.accommodationRevenueCents)],
      ],
      columns: ["Metric", "Value"],
      rows: [
        ["Room nights available", r.roomNightsAvailable],
        ["Room nights booked", r.roomNightsBooked],
        ["ADR (KES)", r.averageDailyRateCents / 100],
        ["RevPAR (KES)", r.revParCents / 100],
        ["Outstanding folios (KES)", r.outstandingBalanceCents / 100],
        ["Collected, net of refunds (KES)", r.collectedCents / 100],
        ["Guest credits to refund (KES)", r.refundableCreditCents / 100],
        ["Arrivals", r.arrivals],
        ["Departures", r.departures],
      ],
    };
  }
  const save = (work: () => Promise<unknown>) =>
    run(async () => {
      await work();
      setModal("");
      await load();
      setMessage("Saved.");
    });
  const days: string[] = [];
  for (let d = arrival; d < departure && days.length < 62; d = addDays(d, 1))
    days.push(d);
  return (
    <div className="hotel-page">
      <PageHeader
        title="Hotel & Accommodation"
        action={
          <Btn
            variant="secondary"
            disabled={busy || loading}
            icon={<RefreshCw size={14} />}
            onClick={() => load()}
          >
            Refresh
          </Btn>
        }
      />
      {error && (
        <div role="alert" className="hotel-error">
          {error}
        </div>
      )}
      {message && (
        <div role="status" className="hotel-success">
          {message}
        </div>
      )}
      {enabled === false ? (
        <Card style={{ padding: 24 }}>
          <h3>Enable accommodation management</h3>
          <p>
            Manage overnight stays independently of restaurant and bar
            operations.
          </p>
          {admin ? (
            <Btn
              disabled={busy}
              onClick={() =>
                run(async () => {
                  await hotelApi.enable(true);
                  setEnabled(true);
                  window.dispatchEvent(new Event("access-updated"));
                })
              }
            >
              Enable Hotels
            </Btn>
          ) : (
            <p>
              Ask your administrator to enable Hotels and assign hotel
              permissions.
            </p>
          )}
        </Card>
      ) : enabled === null ? (
        <p>Loading hotel settings…</p>
      ) : (
        <>
          <div className="hotel-toolbar">
            {label(
              "From",
              <input
                type="date"
                value={arrival}
                onChange={(e) => { if (e.target.value) setArrival(e.target.value) }}
              />,
            )}
            {label(
              "To (departure / exclusive)",
              <input
                type="date"
                min={addDays(arrival, 1)}
                value={departure}
                onChange={(e) => { if (e.target.value) setDeparture(e.target.value) }}
              />,
            )}
            {can("reservations") && (
              <Btn
                disabled={busy}
                icon={<Plus size={14} />}
                onClick={() => editStay()}
              >
                New Reservation
              </Btn>
            )}
          </div>
          <div className="hotel-tabs">
            {[
              "RESERVATIONS",
              "CALENDAR",
              "ROOMS",
              "HOUSEKEEPING",
              ...(can("reports") ? ["REPORTS"] : []),
              ...(can("channels") ? ["CHANNELS"] : []),
            ].map((t) => (
              <button
                key={t}
                className={tab === t ? "active" : ""}
                onClick={() => setTab(t)}
              >
                {t.toLowerCase().replace(/^./, (c) => c.toUpperCase())}
              </button>
            ))}
          </div>
          {loading ? (
            <p>Loading rooms and stays…</p>
          ) : (
            data && (
              <>
                <div className="responsive-grid responsive-grid-4">
                  <KpiCard
                    change="Selected date range"
                    color="var(--b360-green)"
                    icon={<Building2 size={18} />}
                    title="Rooms"
                    value={String(data.rooms.filter((r) => r.isActive).length)}
                  />
                  <KpiCard
                    change="Selected date range"
                    color="var(--b360-green)"
                    icon={<Building2 size={18} />}
                    title="Available for range"
                    value={String(data.rooms.filter((r) => r.available).length)}
                  />
                  <KpiCard
                    change="Selected date range"
                    color="var(--b360-green)"
                    icon={<Building2 size={18} />}
                    title="Checked in"
                    value={String(
                      data.reservations.filter((r) => r.status === "CHECKED_IN")
                        .length,
                    )}
                  />
                  <KpiCard
                    change="Selected date range"
                    color="var(--b360-green)"
                    icon={<Building2 size={18} />}
                    title="Needs housekeeping"
                    value={String(
                      data.rooms.filter((r) =>
                        ["DIRTY", "IN_PROGRESS"].includes(r.housekeepingStatus),
                      ).length,
                    )}
                  />
                </div>
                {tab === "RESERVATIONS" && (
                  <Card>
                    <DataTable
                      headers={[
                        "Guest",
                        "Room",
                        "Stay",
                        "Status",
                        "Balance",
                        "Actions",
                      ]}
                      rows={data.reservations.map((r) => [
                        <div>
                          <strong>{r.guestName}</strong>
                          <div>{r.guestPhone}</div>
                          <small>
                            {r.source}
                            {r.reference && ` · ${r.reference}`}
                          </small>
                        </div>,
                        roomName(r.roomId),
                        <div>
                          {r.arrival} → {r.departure}
                          <br />
                          <small>
                            {r.guests} guest(s) · {money(r.nightlyRateCents)}
                            /night
                          </small>
                        </div>,
                        <StatusBadge status={r.status} />,
                        money(r.balanceCents),
                        <div className="hotel-actions">
                          {can("billing") && (
                            <Btn
                              small
                              variant="secondary"
                              disabled={busy}
                              onClick={() => openFolio(r)}
                            >
                              Folio
                            </Btn>
                          )}
                          {can("reservations") &&
                            ["CONFIRMED", "CHECKED_IN"].includes(r.status) && (
                              <Btn
                                small
                                variant="secondary"
                                onClick={() => editStay(r)}
                              >
                                Edit / Move / Extend
                              </Btn>
                            )}
                          {can("frontdesk") && r.status === "CONFIRMED" && (
                            <Btn
                              small
                              disabled={busy}
                              onClick={() =>
                                setConfirmation({
                                  id: r.id,
                                  action: "check-in",
                                  name: r.guestName,
                                })
                              }
                            >
                              Check In
                            </Btn>
                          )}
                          {can("frontdesk") && r.status === "CHECKED_IN" && (
                            <Btn
                              small
                              disabled={busy}
                              onClick={() => {
                                if (can("billing")) openFolio(r, true);
                                else setConfirmation({ id: r.id, action: "check-out", name: r.guestName });
                              }}
                            >
                              Check Out
                            </Btn>
                          )}
                          {can("reservations") && r.status === "CONFIRMED" && (
                            <>
                              <Btn
                                small
                                variant="secondary"
                                onClick={() =>
                                  setConfirmation({
                                    id: r.id,
                                    action: "cancel",
                                    name: r.guestName,
                                  })
                                }
                              >
                                Cancel
                              </Btn>
                              <Btn
                                small
                                variant="secondary"
                                onClick={() =>
                                  setConfirmation({
                                    id: r.id,
                                    action: "no-show",
                                    name: r.guestName,
                                  })
                                }
                              >
                                No-show
                              </Btn>
                            </>
                          )}
                        </div>,
                      ])}
                    />
                    {data.reservations.length === 0 && (
                      <p className="hotel-empty">
                        No stays in this date range.
                      </p>
                    )}
                  </Card>
                )}
                {tab === "CALENDAR" && (
                  <Card style={{ padding: 16 }}>
                    <p>
                      Nightly occupancy; departure days are available for the
                      next guest. Showing up to 62 nights.
                    </p>
                    <div className="hotel-calendar">
                      <table>
                        <thead>
                          <tr>
                            <th>Room</th>
                            {days.map((d) => (
                              <th key={d}>{d.slice(5)}</th>
                            ))}
                          </tr>
                        </thead>
                        <tbody>
                          {data.rooms
                            .filter((r) => r.isActive)
                            .map((r) => (
                              <tr key={r.id}>
                                <th>{r.number}</th>
                                {days.map((d) => {
                                  const stay = data.reservations.find(
                                    (s) =>
                                      s.roomId === r.id &&
                                      ["CONFIRMED", "CHECKED_IN"].includes(
                                        s.status,
                                      ) &&
                                      s.arrival <= d &&
                                      s.departure > d,
                                  );
                                  const blocked =
                                    data.blocks.some(
                                      (b) =>
                                        b.roomId === r.id &&
                                        b.arrival <= d &&
                                        b.departure > d,
                                    ) ||
                                    r.housekeepingStatus === "OUT_OF_SERVICE";
                                  return (
                                    <td
                                      key={d}
                                      className={
                                        stay
                                          ? "booked"
                                          : blocked
                                            ? "blocked"
                                            : "free"
                                      }
                                      title={
                                        stay
                                          ? `${stay.guestName} · ${stay.status}`
                                          : blocked
                                            ? "Unavailable"
                                            : "Available"
                                      }
                                    >
                                      {stay ? "●" : blocked ? "×" : "·"}
                                    </td>
                                  );
                                })}
                              </tr>
                            ))}
                        </tbody>
                      </table>
                    </div>
                    <p>● Reserved / in house · × Blocked · · Available</p>
                  </Card>
                )}
                {tab === "ROOMS" && (
                  <>
                    <Card style={{ padding: 20 }}>
                      <div className="hotel-toolbar">
                        <h3>Room Types & Nightly Rates</h3>
                        {can("manage") && (
                          <Btn small onClick={() => editType()}>
                            Add Room Type
                          </Btn>
                        )}
                      </div>
                      <DataTable
                        headers={[
                          "Type",
                          "Capacity",
                          "Nightly rate (all-inclusive)",
                          "Status",
                          "Actions",
                        ]}
                        rows={data.roomTypes.map((t) => [
                          <div>
                            <strong>{t.name}</strong>
                            <p>{t.description}</p>
                          </div>,
                          String(t.capacity),
                          money(t.nightlyRateCents),
                          t.isActive ? "Active" : "Disabled",
                          can("manage") ? (
                            <Btn
                              small
                              variant="secondary"
                              onClick={() => editType(t)}
                            >
                              Edit
                            </Btn>
                          ) : null,
                        ])}
                      />
                    </Card>
                    <Card style={{ padding: 20 }}>
                      <div className="hotel-toolbar">
                        <h3>Rooms</h3>
                        {can("manage") && (
                          <>
                            <Btn
                              small
                              disabled={!data.roomTypes.length}
                              onClick={() => editRoom()}
                            >
                              Add Room
                            </Btn>
                            <Btn
                              small
                              variant="secondary"
                              onClick={() => {
                                setBlock({
                                  roomId: "",
                                  arrival,
                                  departure,
                                  reason: "",
                                });
                                setModal("BLOCK");
                              }}
                            >
                              Block Dates
                            </Btn>
                          </>
                        )}
                      </div>
                      <DataTable
                        headers={[
                          "Room",
                          "Type",
                          "Floor",
                          "Housekeeping",
                          "Availability",
                          "Actions",
                        ]}
                        rows={data.rooms.map((r) => [
                          r.number,
                          data.roomTypes.find((t) => t.id === r.typeId)?.name ||
                            "",
                          r.floor,
                          r.housekeepingStatus,
                          !r.isActive
                            ? "Disabled"
                            : r.available
                              ? "Available"
                              : "Unavailable",
                          can("manage") ? (
                            <Btn
                              small
                              variant="secondary"
                              onClick={() => editRoom(r)}
                            >
                              Edit
                            </Btn>
                          ) : null,
                        ])}
                      />
                      <h4>Date Blocks</h4>
                      {data.blocks.map((b) => (
                        <div className="hotel-toolbar" key={b.id}>
                          <span>
                            Room {roomName(b.roomId)} · {b.arrival} →{" "}
                            {b.departure} · {b.reason} ({b.source})
                          </span>
                          {can("manage") && (
                            <Btn
                              small
                              variant="secondary"
                              disabled={busy}
                              onClick={() =>
                                save(() => hotelApi.removeBlock(b.id))
                              }
                            >
                              Remove
                            </Btn>
                          )}
                        </div>
                      ))}
                    </Card>
                  </>
                )}
                {tab === "HOUSEKEEPING" && (
                  <div className="responsive-grid responsive-grid-3">
                    {data.rooms
                      .filter((r) => r.isActive)
                      .map((r) => (
                        <Card key={r.id} style={{ padding: 20 }}>
                          <h3>Room {r.number}</h3>
                          <StatusBadge status={r.housekeepingStatus} />
                          <p>{r.notes || "No housekeeping notes."}</p>
                          {can("housekeeping") && (
                            <Btn
                              small
                              disabled={busy}
                              onClick={() => {
                                setEditingId(r.id);
                                setClean({
                                  status: r.housekeepingStatus,
                                  notes: r.notes,
                                });
                                setModal("CLEAN");
                              }}
                            >
                              Update Status
                            </Btn>
                          )}
                        </Card>
                      ))}
                  </div>
                )}
                {tab === "REPORTS" && can("reports") && (
                  <Card style={{ padding: 20 }}>
                    <div className="hotel-toolbar">
                      <h3>Occupancy & Accommodation</h3>
                      <Btn
                        disabled={busy}
                        onClick={() =>
                          run(async () =>
                            setReport(
                              await hotelApi.report(arrival, departure),
                            ),
                          )
                        }
                      >
                        Generate Report
                      </Btn>
                      {report && (
                        <>
                          <Btn
                            variant="secondary"
                            onClick={() => printReport(reportDocument())}
                          >
                            Print
                          </Btn>
                          <Btn
                            variant="secondary"
                            onClick={() => downloadReportCsv(reportDocument())}
                          >
                            CSV
                          </Btn>
                        </>
                      )}
                    </div>
                    <p>
                      Booked room revenue, ADR and RevPAR use contracted nightly
                      rates. They are forecasts for future reservations, not
                      collected payments. Folios show actual collections.
                    </p>
                    {report && (
                      <DataTable
                        headers={["Metric", "Value"]}
                        rows={reportDocument().rows.map((r) => r.map(String))}
                      />
                    )}
                  </Card>
                )}
                {tab === "CHANNELS" && can("channels") && (
                  <Card style={{ padding: 20 }}>
                    <h3>Booking Channel Calendars</h3>
                    <p>
                      Import an all-day .ics calendar to block external bookings
                      for a room. Reimporting replaces that channel’s blocks
                      atomically. Resolve conflicts before importing. Export
                      contains availability only, without guest details. This is
                      a manual exchange; it does not send rates or create guest
                      reservations.
                    </p>
                    <div className="hotel-form">
                      {label(
                        "Room",
                        <select
                          value={channel.roomId}
                          onChange={(e) =>
                            setChannel({ ...channel, roomId: e.target.value })
                          }
                        >
                          {roomOptions}
                        </select>,
                      )}
                      {label(
                        "Channel name",
                        <input
                          maxLength={80}
                          value={channel.source}
                          onChange={(e) =>
                            setChannel({ ...channel, source: e.target.value })
                          }
                        />,
                      )}
                      {label(
                        "Calendar (.ics)",
                        <input
                          type="file"
                          accept=".ics,text/calendar"
                          onChange={async (e) => {
                            const file = e.currentTarget.files?.[0];
                            if (file) {
                              if (file.size > 1_000_000) {
                                setError(
                                  "Choose a calendar smaller than 1 MB.",
                                );
                                return;
                              }
                              setChannel((prev) => ({ ...prev, calendar: "" }));
                              try {
                                const calendar = await file.text();
                                setChannel((prev) => ({ ...prev, calendar }));
                              } catch {
                                setError("Could not read calendar.");
                              }
                            }
                          }}
                        />,
                      )}
                      <div className="hotel-actions">
                        <Btn
                          disabled={
                            busy ||
                            !channel.roomId ||
                            !channel.source ||
                            !channel.calendar
                          }
                          onClick={() =>
                            run(async () => {
                              const count = await hotelApi.importCalendar(
                                channel.roomId,
                                channel.source,
                                channel.calendar,
                              );
                              await load();
                              setMessage(
                                `Imported ${count} external booking blocks.`,
                              );
                            })
                          }
                        >
                          Import Calendar
                        </Btn>
                        <Btn
                          variant="secondary"
                          disabled={busy || !channel.roomId}
                          onClick={() =>
                            run(async () => {
                              const calendar = await hotelApi.exportCalendar(
                                channel.roomId,
                              );
                              const url = URL.createObjectURL(
                                new Blob([calendar], { type: "text/calendar" }),
                              );
                              const a = document.createElement("a");
                              a.href = url;
                              a.download = `room-${roomName(channel.roomId).replace(/[^a-z0-9]/gi, "-")}.ics`;
                              a.click();
                              URL.revokeObjectURL(url);
                            })
                          }
                        >
                          Export Calendar
                        </Btn>
                      </div>
                    </div>
                  </Card>
                )}
              </>
            )
          )}
        </>
      )}
      {confirmation && (
        <Modal
          title={`${confirmation.action.replace("-", " ")} · ${confirmation.name}`}
          onClose={close}
          footer={
            <>
              <Btn variant="secondary" disabled={busy} onClick={close}>
                Back
              </Btn>
              <Btn
                disabled={busy}
                onClick={() =>
                  run(async () => {
                    await hotelApi.transition(
                      confirmation.id,
                      confirmation.action,
                    );
                    setConfirmation(null);
                    await load();
                    setMessage("Reservation updated.");
                  })
                }
              >
                Confirm
              </Btn>
            </>
          }
        >
          <p>
            {confirmation.action === "check-out"
              ? "Checkout requires a fully settled folio. The room will be marked dirty."
              : confirmation.action === "cancel" ||
                  confirmation.action === "no-show"
                ? "Accommodation charges will be removed; any deposit becomes a refundable guest credit."
                : "Confirm arrival and check the guest into their room."}
          </p>
          {error && <p role="alert">{error}</p>}
        </Modal>
      )}
      {modal && (
        <Modal
          title={
            modal === "RESERVE"
              ? editingId
                ? "Edit Reservation"
                : "New Reservation"
              : modal === "TYPE"
                ? "Room Type"
                : modal === "ROOM"
                  ? "Room"
                  : modal === "CLEAN"
                    ? "Housekeeping"
                    : modal === "BLOCK"
                      ? "Block Room Dates"
                      : "Guest Folio"
          }
          onClose={close}
          footer={
            <Btn variant="secondary" disabled={busy} onClick={close}>
              Close
            </Btn>
          }
        >
          {error && (
            <p role="alert" className="hotel-error">
              {error}
            </p>
          )}
          <fieldset disabled={busy} className="hotel-form">
            {modal === "RESERVE" && (
              <>
                {label(
                  "Room",
                  <select
                    required
                    value={stay.roomId}
                    onChange={(e) =>
                      setStay({ ...stay, roomId: e.target.value })
                    }
                  >
                    {roomOptions}
                  </select>,
                )}
                {label(
                  "Guest name",
                  <input
                    maxLength={160}
                    value={stay.guestName}
                    onChange={(e) =>
                      setStay({ ...stay, guestName: e.target.value })
                    }
                  />,
                )}
                {label(
                  "Phone (M-Pesa)",
                  <input
                    maxLength={30}
                    value={stay.guestPhone}
                    onChange={(e) =>
                      setStay({ ...stay, guestPhone: e.target.value })
                    }
                  />,
                )}
                {label(
                  "Email",
                  <input
                    type="email"
                    maxLength={160}
                    value={stay.guestEmail}
                    onChange={(e) =>
                      setStay({ ...stay, guestEmail: e.target.value })
                    }
                  />,
                )}
                {label(
                  "Guests",
                  <input
                    type="number"
                    min={1}
                    max={30}
                    value={stay.guests}
                    onChange={(e) =>
                      setStay({ ...stay, guests: Number(e.target.value) })
                    }
                  />,
                )}
                {label(
                  "Arrival",
                  <input
                    type="date"
                    value={stay.arrival}
                    onChange={(e) =>
                      setStay({ ...stay, arrival: e.target.value })
                    }
                  />,
                )}
                {label(
                  "Departure",
                  <input
                    type="date"
                    min={addDays(stay.arrival, 1)}
                    value={stay.departure}
                    onChange={(e) =>
                      setStay({ ...stay, departure: e.target.value })
                    }
                  />,
                )}
                {label(
                  "Booking source",
                  <input
                    maxLength={80}
                    value={stay.source}
                    onChange={(e) =>
                      setStay({ ...stay, source: e.target.value })
                    }
                  />,
                )}
                {label(
                  "External reference",
                  <input
                    maxLength={120}
                    value={stay.reference}
                    onChange={(e) =>
                      setStay({ ...stay, reference: e.target.value })
                    }
                  />,
                )}
                {label(
                  "Notes",
                  <textarea
                    maxLength={4000}
                    value={stay.notes}
                    onChange={(e) =>
                      setStay({ ...stay, notes: e.target.value })
                    }
                  />,
                )}
                <p>
                  Reservations retain their original nightly rate when rates
                  change. Dates and guest count are validated against room
                  availability and capacity.
                </p>
                <Btn
                  disabled={
                    busy || !stay.roomId || stay.guestName.trim().length < 2
                  }
                  onClick={() => save(() => hotelApi.reserve(editingId, stay))}
                >
                  Save Reservation
                </Btn>
              </>
            )}
            {modal === "TYPE" && (
              <>
                {label(
                  "Name",
                  <input
                    maxLength={100}
                    value={type.name}
                    onChange={(e) => setType({ ...type, name: e.target.value })}
                  />,
                )}
                {label(
                  "Description",
                  <textarea
                    maxLength={2000}
                    value={type.description}
                    onChange={(e) =>
                      setType({ ...type, description: e.target.value })
                    }
                  />,
                )}
                {label(
                  "Capacity",
                  <input
                    type="number"
                    min={1}
                    max={30}
                    value={type.capacity}
                    onChange={(e) =>
                      setType({ ...type, capacity: Number(e.target.value) })
                    }
                  />,
                )}
                {label(
                  "Nightly rate (KES, all-inclusive)",
                  <input
                    type="number"
                    min="0.01"
                    step="0.01"
                    value={type.price}
                    onChange={(e) =>
                      setType({ ...type, price: e.target.value })
                    }
                  />,
                )}
                <label>
                  <input
                    type="checkbox"
                    checked={type.isActive}
                    onChange={(e) =>
                      setType({ ...type, isActive: e.target.checked })
                    }
                  />{" "}
                  Active
                </label>
                <Btn
                  disabled={busy}
                  onClick={() =>
                    save(() =>
                      hotelApi.saveType(editingId, {
                        name: type.name,
                        description: type.description,
                        capacity: type.capacity,
                        isActive: type.isActive,
                        nightlyRateCents: amount(type.price),
                      }),
                    )
                  }
                >
                  Save Type
                </Btn>
              </>
            )}
            {modal === "ROOM" && (
              <>
                {label(
                  "Room number",
                  <input
                    maxLength={50}
                    value={room.number}
                    onChange={(e) =>
                      setRoom({ ...room, number: e.target.value })
                    }
                  />,
                )}
                {label(
                  "Floor / building",
                  <input
                    maxLength={50}
                    value={room.floor}
                    onChange={(e) =>
                      setRoom({ ...room, floor: e.target.value })
                    }
                  />,
                )}
                {label(
                  "Room type",
                  <select
                    value={room.typeId}
                    onChange={(e) =>
                      setRoom({ ...room, typeId: e.target.value })
                    }
                  >
                    <option value="">Choose type</option>
                    {data?.roomTypes.map((t) => (
                      <option value={t.id} key={t.id}>
                        {t.name}
                      </option>
                    ))}
                  </select>,
                )}
                <label>
                  <input
                    type="checkbox"
                    checked={room.isActive}
                    onChange={(e) =>
                      setRoom({ ...room, isActive: e.target.checked })
                    }
                  />{" "}
                  Active
                </label>
                <Btn
                  disabled={busy || !room.typeId || !room.number.trim()}
                  onClick={() => save(() => hotelApi.saveRoom(editingId, room))}
                >
                  Save Room
                </Btn>
              </>
            )}
            {modal === "CLEAN" && (
              <>
                {label(
                  "Status",
                  <select
                    value={clean.status}
                    onChange={(e) =>
                      setClean({ ...clean, status: e.target.value })
                    }
                  >
                    {[
                      "CLEAN",
                      "DIRTY",
                      "IN_PROGRESS",
                      "INSPECTED",
                      "OUT_OF_SERVICE",
                    ].map((s) => (
                      <option key={s}>{s}</option>
                    ))}
                  </select>,
                )}
                {label(
                  "Notes / maintenance details",
                  <textarea
                    maxLength={2000}
                    value={clean.notes}
                    onChange={(e) =>
                      setClean({ ...clean, notes: e.target.value })
                    }
                  />,
                )}
                <Btn
                  disabled={busy}
                  onClick={() =>
                    save(() =>
                      hotelApi.housekeeping(
                        editingId!,
                        clean.status,
                        clean.notes,
                      ),
                    )
                  }
                >
                  Update Room
                </Btn>
              </>
            )}
            {modal === "BLOCK" && (
              <>
                {label(
                  "Room",
                  <select
                    value={block.roomId}
                    onChange={(e) =>
                      setBlock({ ...block, roomId: e.target.value })
                    }
                  >
                    {roomOptions}
                  </select>,
                )}
                {label(
                  "From",
                  <input
                    type="date"
                    value={block.arrival}
                    onChange={(e) =>
                      setBlock({ ...block, arrival: e.target.value })
                    }
                  />,
                )}
                {label(
                  "To (exclusive)",
                  <input
                    type="date"
                    value={block.departure}
                    onChange={(e) =>
                      setBlock({ ...block, departure: e.target.value })
                    }
                  />,
                )}
                {label(
                  "Reason",
                  <input
                    maxLength={255}
                    value={block.reason}
                    onChange={(e) =>
                      setBlock({ ...block, reason: e.target.value })
                    }
                  />,
                )}
                <Btn
                  disabled={
                    busy || !block.roomId || block.reason.trim().length < 2
                  }
                  onClick={() => save(() => hotelApi.block(block))}
                >
                  Block Dates
                </Btn>
              </>
            )}
            {modal === "FOLIO" && folio && (
              <>
                <div>
                  <strong>
                    {folio.reservation.guestName} · Room{" "}
                    {roomName(folio.reservation.roomId)}
                  </strong>
                  <p>
                    {folio.reservation.arrival} → {folio.reservation.departure}
                  </p>
                  <p>
                    Charges: {money(folio.totalChargeCents)} · Paid:{" "}
                    {money(folio.totalPaidCents)} ·{" "}
                    <strong>Balance: {money(folio.balanceCents)}</strong>
                  </p>
                  {folio.reservation.status === "CHECKED_IN" && (
                    <>
                      <p role="status">
                        {folio.balanceCents > 0
                          ? `Collect ${money(folio.balanceCents)} using the payment form below before checkout.`
                          : folio.balanceCents < 0
                            ? `Return the guest credit of ${money(-folio.balanceCents)} before checkout.${can("refunds") ? " Record cash actually returned using Cash refund paid to guest below." : " Ask a user with hotel refund permission to record the refund."}`
                            : payments.some(p => p.paymentStatus === "PENDING")
                              ? "Resolve pending payment requests before checkout."
                              : "The folio is settled and ready for checkout."}
                      </p>
                      {can("frontdesk") && (
                        <Btn disabled={busy || folio.balanceCents !== 0 || payments.some(p => p.paymentStatus === "PENDING")}
                          onClick={() => run(async () => {
                            await hotelApi.transition(folio.reservation.id, "check-out");
                            setModal("");
                            setFolio(null);
                            await load();
                            setMessage("Guest checked out. The room is marked dirty.");
                          })}>
                          Complete Checkout
                        </Btn>
                      )}
                    </>
                  )}
                  <div className="hotel-actions">
                    <Btn
                      small
                      variant="secondary"
                      icon={<Printer size={14} />}
                      onClick={() => printReport(folioDocument())}
                    >
                      Print Folio
                    </Btn>
                    <Btn
                      small
                      variant="secondary"
                      onClick={() => downloadReportCsv(folioDocument())}
                    >
                      CSV
                    </Btn>
                    <Btn
                      small
                      variant="secondary"
                      onClick={() =>
                        run(async () => { await refreshFolio(folio.reservation.id); })
                      }
                    >
                      Refresh Payments
                    </Btn>
                  </div>
                </div>
                <DataTable
                  headers={["Description", "Type", "Amount"]}
                  rows={[
                    [
                      `Accommodation · ${folio.reservation.arrival} to ${folio.reservation.departure}`,
                      "ROOM",
                      money(folio.roomChargeCents),
                    ],
                    ...folio.entries.map((e) => [
                      <div>
                        {e.description}
                        <br />
                        <small>
                          {new Date(e.createdAt).toLocaleString("en-KE")}
                        </small>
                      </div>,
                      e.kind,
                      money(e.amountCents),
                    ]),
                  ]}
                />
                {folio.reservation.status !== "CHECKED_OUT" && (
                  <>
                    <h4>Charges & Adjustments</h4>
                    {label(
                      "Entry type",
                      <select
                        value={entry.kind}
                        onChange={(e) => {
                          setEntry({ ...entry, kind: e.target.value });
                          setRequestId(crypto.randomUUID());
                        }}
                      >
                        <option value="CHARGE">Additional charge</option>
                        {can("refunds") && (
                          <>
                            <option value="CREDIT">Credit / discount</option>
                            <option value="CASH_REFUND">
                              Cash refund paid to guest
                            </option>
                          </>
                        )}
                      </select>,
                    )}
                    {label(
                      "Description",
                      <input
                        maxLength={255}
                        value={entry.description}
                        onChange={(e) => {
                          setEntry({ ...entry, description: e.target.value });
                          setRequestId(crypto.randomUUID());
                        }}
                      />,
                    )}
                    {label(
                      "Amount (KES)",
                      <input
                        type="number"
                        step="0.01"
                        min="0.01"
                        value={entry.amount}
                        onChange={(e) => {
                          setEntry({ ...entry, amount: e.target.value });
                          setRequestId(crypto.randomUUID());
                        }}
                      />,
                    )}
                    <Btn
                      disabled={
                        busy ||
                        !entry.amount ||
                        entry.description.trim().length < 2
                      }
                      onClick={() =>
                        run(async () => {
                          setFolio(
                            await hotelApi.entry(folio.reservation.id, {
                              kind: entry.kind,
                              description: entry.description,
                              amountCents: amount(entry.amount),
                              requestId,
                            }),
                          );
                          setRequestId(crypto.randomUUID());
                          setEntry({ ...entry, amount: "", description: "" });
                          await load();
                        })
                      }
                    >
                      Post Entry
                    </Btn>
                    {entry.kind === "CASH_REFUND" && (
                      <p>
                        Records cash actually returned to the guest. It does not
                        reverse a card or M-Pesa transaction.
                      </p>
                    )}
                    {["CONFIRMED", "CHECKED_IN"].includes(
                      folio.reservation.status,
                    ) && (
                      <>
                        <h4>Collect Payment / Deposit</h4>
                        {label(
                          "Method",
                          <select
                            value={payment.method}
                            onChange={(e) => {
                              setPayment({
                                ...payment,
                                method: e.target.value,
                              });
                              setRequestId(crypto.randomUUID());
                            }}
                          >
                            {["CASH", "MPESA", "CARD"].map((m) => (
                              <option key={m}>{m}</option>
                            ))}
                          </select>,
                        )}
                        {label(
                          "Amount (KES)",
                          <input
                            type="number"
                            step="0.01"
                            min="0.01"
                            value={payment.amount}
                            onChange={(e) => {
                              setPayment({
                                ...payment,
                                amount: e.target.value,
                              });
                              setRequestId(crypto.randomUUID());
                            }}
                          />,
                        )}
                        <Btn
                          disabled={
                            busy || !payment.amount || folio.balanceCents <= 0
                          }
                          onClick={() =>
                            run(async () => {
                              const p = await hotelApi.payment(
                                folio.reservation.id,
                                {
                                  method: payment.method,
                                  amountCents: amount(payment.amount),
                                  requestId,
                                },
                              );
                              setRequestId(crypto.randomUUID());
                              setPayment({ ...payment, amount: "" });
                              await refreshFolio(folio.reservation.id);
                              if (p.paymentMethod === "MPESA") {
                                const response = await paymentApi.initiate({
                                  orderId: p.orderId,
                                  phoneNumber: folio.reservation.guestPhone,
                                });
                                if (!response.success)
                                  throw new Error(
                                    response.message ||
                                      "Payment request saved. M-Pesa initiation failed; retry below.",
                                  );
                              }
                              await load();
                              setMessage(
                                p.paymentStatus === "PAID"
                                  ? "Cash payment recorded."
                                  : "Payment request created. Refresh Payments after the guest pays.",
                              );
                            })
                          }
                        >
                          {payment.method === "CASH"
                            ? "Record Cash Received"
                            : "Request Payment"}
                        </Btn>
                      </>
                    )}
                  </>
                )}
                {payments.length > 0 && (
                  <>
                    <h4>Payment Requests</h4>
                    {payments.map((p) => (
                      <div key={p.orderId} className="hotel-payment">
                        <span>
                          {p.paymentMethod} · {money(p.amountCents)} ·{" "}
                          {p.paymentStatus}
                        </span>
                        {p.paymentStatus === "PENDING" && (
                          <div className="hotel-actions">
                            {p.paymentMethod === "CARD" && (
                              <a
                                target="_blank"
                                rel="noreferrer"
                                href={`/pay/card?orderId=${encodeURIComponent(p.orderId)}&businessId=${encodeURIComponent(user?.businessId || "")}`}
                              >
                                Open card checkout
                              </a>
                            )}
                            {p.paymentMethod === "MPESA" && (
                              <Btn
                                small
                                variant="secondary"
                                onClick={() =>
                                  run(async () => {
                                    const r = await paymentApi.initiate({
                                      orderId: p.orderId,
                                      phoneNumber: folio.reservation.guestPhone,
                                    });
                                    if (!r.success)
                                      throw new Error(
                                        r.message ||
                                          "M-Pesa initiation failed.",
                                      );
                                    setMessage(
                                      "M-Pesa prompt sent. Refresh after payment.",
                                    );
                                  })
                                }
                              >
                                Send M-Pesa Prompt
                              </Btn>
                            )}
                            <Btn
                              small
                              variant="secondary"
                              onClick={() =>
                                run(async () => {
                                  await hotelApi.cancelPayment(
                                    folio.reservation.id,
                                    p.orderId,
                                  );
                                  await refreshFolio(folio.reservation.id);
                                })
                              }
                            >
                              Cancel Uninitiated Request
                            </Btn>
                          </div>
                        )}
                      </div>
                    ))}
                  </>
                )}
              </>
            )}
          </fieldset>
        </Modal>
      )}
    </div>
  );
}

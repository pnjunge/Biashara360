import { client, ApiResponse } from "./api";
export interface RoomType {
  id: string;
  name: string;
  description: string;
  capacity: number;
  nightlyRateCents: number;
  isActive: boolean;
}
export interface HotelRoom {
  id: string;
  typeId: string;
  number: string;
  floor: string;
  housekeepingStatus: string;
  notes: string;
  isActive: boolean;
  available: boolean;
}
export interface Reservation {
  id: string;
  roomId: string;
  guestName: string;
  guestPhone: string;
  guestEmail: string;
  guests: number;
  arrival: string;
  departure: string;
  nightlyRateCents: number;
  status: string;
  source: string;
  reference: string;
  notes: string;
  checkedInAt: string | null;
  checkedOutAt: string | null;
  balanceCents: number;
}
export interface RoomBlock {
  id: string;
  roomId: string;
  arrival: string;
  departure: string;
  reason: string;
  source: string;
}
export interface HotelDashboard {
  roomTypes: RoomType[];
  rooms: HotelRoom[];
  reservations: Reservation[];
  blocks: RoomBlock[];
}
export interface Folio {
  reservation: Reservation;
  entries: Array<{
    id: string;
    kind: string;
    description: string;
    amountCents: number;
    createdAt: string;
    paymentId: string | null;
    orderId: string | null;
  }>;
  roomChargeCents: number;
  totalChargeCents: number;
  totalPaidCents: number;
  balanceCents: number;
}
export interface HotelPayment {
  orderId: string;
  paymentMethod: string;
  paymentStatus: string;
  amountCents: number;
}
export interface HotelReport {
  collectedCents: number;
  refundableCreditCents: number;
  startDate: string;
  endDate: string;
  roomNightsAvailable: number;
  roomNightsBooked: number;
  occupancyPercent: number;
  accommodationRevenueCents: number;
  averageDailyRateCents: number;
  revParCents: number;
  outstandingBalanceCents: number;
  arrivals: number;
  departures: number;
}
async function request<T>(
  method: string,
  path: string,
  data?: unknown,
): Promise<T> {
  const response = await client.request<ApiResponse<T>>({
    method,
    url: `/hotel${path}`,
    data,
  });
  if (!response.data.success || response.data.data == null)
    throw new Error(response.data.message || "Hotel request failed.");
  return response.data.data;
}
export const hotelApi = {
  status: () => request<{ enabled: boolean }>("GET", "/status"),
  enable: (enabled: boolean) =>
    request<boolean>("PUT", "/enabled", { enabled }),
  dashboard: (arrival: string, departure: string) =>
    request<HotelDashboard>(
      "GET",
      `?arrival=${arrival}&departure=${departure}`,
    ),
  report: (arrival: string, departure: string) =>
    request<HotelReport>(
      "GET",
      `/report?arrival=${arrival}&departure=${departure}`,
    ),
  saveType: (id: string | null, data: Omit<RoomType, "id">) =>
    request<RoomType>(
      id ? "PUT" : "POST",
      `/room-types${id ? `/${id}` : ""}`,
      data,
    ),
  saveRoom: (
    id: string | null,
    data: Pick<HotelRoom, "number" | "floor" | "typeId" | "isActive">,
  ) =>
    request<HotelRoom>(
      id ? "PUT" : "POST",
      `/rooms${id ? `/${id}` : ""}`,
      data,
    ),
  housekeeping: (id: string, status: string, notes: string) =>
    request<HotelRoom>("PUT", `/rooms/${id}/housekeeping`, { status, notes }),
  reserve: (
    id: string | null,
    data: Pick<
      Reservation,
      | "roomId"
      | "guestName"
      | "guestPhone"
      | "guestEmail"
      | "guests"
      | "arrival"
      | "departure"
      | "source"
      | "reference"
      | "notes"
    >,
  ) =>
    request<Reservation>(
      id ? "PUT" : "POST",
      `/reservations${id ? `/${id}` : ""}`,
      data,
    ),
  transition: (id: string, action: string) =>
    request<Folio>("POST", `/reservations/${id}/${action}`),
  folio: (id: string) => request<Folio>("GET", `/reservations/${id}/folio`),
  entry: (
    id: string,
    data: {
      kind: string;
      description: string;
      amountCents: number;
      requestId: string;
    },
  ) => request<Folio>("POST", `/reservations/${id}/folio`, data),
  payments: (id: string) =>
    request<HotelPayment[]>("GET", `/reservations/${id}/payments`),
  payment: (
    id: string,
    data: { amountCents: number; method: string; requestId: string },
  ) => request<HotelPayment>("POST", `/reservations/${id}/payments`, data),
  cancelPayment: (id: string, orderId: string) =>
    request<boolean>("DELETE", `/reservations/${id}/payments/${orderId}`),
  block: (data: {
    roomId: string;
    arrival: string;
    departure: string;
    reason: string;
  }) => request<string>("POST", "/blocks", data),
  removeBlock: (id: string) => request<boolean>("DELETE", `/blocks/${id}`),
  importCalendar: (roomId: string, source: string, calendar: string) =>
    request<number>("POST", `/rooms/${roomId}/calendar`, { source, calendar }),
  exportCalendar: async (roomId: string) =>
    (
      await client.get<string>(`/hotel/rooms/${roomId}/calendar`, {
        responseType: "text",
      })
    ).data,
};

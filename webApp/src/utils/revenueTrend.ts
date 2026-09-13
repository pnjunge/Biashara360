export interface RevenuePoint { date: string; revenue: number }
interface RevenueOrder { createdAt: string; subtotal: number; paymentStatus: string }
export const nairobiDate = (date: Date) => {
  const parts = new Intl.DateTimeFormat('en-CA', { timeZone: 'Africa/Nairobi', year: 'numeric', month: '2-digit', day: '2-digit' }).formatToParts(date)
  return ['year', 'month', 'day'].map(type => parts.find(part => part.type === type)?.value).join('-')
}

export function dailyRevenueFromOrders(start: string, end: string, orders: RevenueOrder[]): RevenuePoint[] {
  const totals = new Map<string, number>()
  for (const order of orders) {
    if (order.paymentStatus !== 'PAID') continue
    if (!Number.isFinite(order.subtotal) || !Number.isFinite(Date.parse(order.createdAt))) throw new Error('An order has invalid revenue data.')
    const date = nairobiDate(new Date(order.createdAt))
    if (date >= start && date <= end) totals.set(date, (totals.get(date) || 0) + order.subtotal)
  }
  const points: RevenuePoint[] = []
  for (let date = new Date(`${start}T12:00:00Z`); date <= new Date(`${end}T12:00:00Z`); date.setUTCDate(date.getUTCDate() + 1)) {
    const key = date.toISOString().slice(0, 10)
    points.push({ date: key, revenue: Math.round((totals.get(key) || 0) * 100) / 100 })
  }
  return points
}

export async function loadPaidRevenue(start: string, end: string, list: (page: number) => Promise<{ success: boolean; message?: string; data?: { data: RevenueOrder[]; page: number; hasMore: boolean } | null }>) {
  const orders: RevenueOrder[] = []
  for (let page = 1; ; page++) {
    const response = await list(page)
    const data = response.data
    if (!response.success || !data || !Array.isArray(data.data) || data.page !== page || typeof data.hasMore !== 'boolean' || (data.hasMore && data.data.length === 0)) {
      throw new Error(response.message || 'Unable to load the complete revenue trend.')
    }
    orders.push(...data.data)
    if (!data.hasMore) break
  }
  return dailyRevenueFromOrders(start, end, orders)
}

export type OrderAlert = 'placed' | 'ready' | 'online'

// Keep seen events across polling responses, including tickets removed from the queue.
export class OrderAlertTracker {
  private initialized = false
  private seen = new Set<string>()
  private ready = new Set<string>()
  observe(items: Array<{ id: string; status?: string }>): OrderAlert[] {
    const alerts = new Set<OrderAlert>()
    for (const item of items) {
      if (this.initialized && !this.seen.has(item.id) && !['SERVED', 'CANCELLED'].includes(item.status || '')) alerts.add('placed')
      if (this.initialized && item.status === 'READY' && !this.ready.has(item.id)) alerts.add('ready')
      this.seen.add(item.id)
      if (item.status === 'READY') this.ready.add(item.id)
    }
    this.initialized = true
    return [...alerts]
  }
}
export function announceOrderAlert(type: OrderAlert) {
  window.dispatchEvent(new CustomEvent('order-sound-alert', { detail: type }))
}

export function announceOnlineOrders(items: Array<{ id: string }>) {
  window.dispatchEvent(new CustomEvent('online-orders-snapshot', { detail: items }))
}

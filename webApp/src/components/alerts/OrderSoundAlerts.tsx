import React, { useEffect, useRef, useState } from 'react'
import { Bell, BellOff } from 'lucide-react'
import { accessApi, hospitalityApi } from '../../services/api'
import { announceOrderAlert, OrderAlert, OrderAlertTracker } from '../../utils/orderAlerts'
import { Btn } from '../ui'

const melodies: Record<OrderAlert, number[]> = {
  placed: [660, 880], ready: [523, 659, 784], online: [880, 660, 880],
}

export default function OrderSoundAlerts({ hospitalityEnabled }: { hospitalityEnabled: boolean }) {
  const [enabled, setEnabled] = useState(false)
  const [message, setMessage] = useState('')
  const context = useRef<AudioContext | null>(null)
  const enabledRef = useRef(false)

  const play = (type: OrderAlert) => {
    const audio = context.current
    if (!enabledRef.current || !audio || audio.state !== 'running') return
    const now = audio.currentTime
    melodies[type].forEach((frequency, index) => {
      const oscillator = audio.createOscillator()
      const gain = audio.createGain()
      const start = now + index * .22
      oscillator.frequency.value = frequency
      gain.gain.setValueAtTime(0, start)
      gain.gain.linearRampToValueAtTime(.15, start + .015)
      gain.gain.exponentialRampToValueAtTime(.001, start + .18)
      oscillator.connect(gain); gain.connect(audio.destination)
      oscillator.onended = () => { oscillator.disconnect(); gain.disconnect() }
      oscillator.start(start); oscillator.stop(start + .2)
    })
  }

  useEffect(() => {
    const onlineTracker = new OrderAlertTracker()
    const onlineListener = (event: Event) => {
      const items = (event as CustomEvent<Array<{ id: string }>>).detail
      if (onlineTracker.observe(items).includes('placed')) announceOrderAlert('online')
    }
    const listener = (event: Event) => {
      const type = (event as CustomEvent<OrderAlert>).detail
      if (!(type in melodies)) return
      setMessage(type === 'placed' ? 'New kitchen or bar order' : type === 'ready' ? 'Kitchen or bar order ready' : 'New online order')
      play(type)
    }
    window.addEventListener('order-sound-alert', listener)
    window.addEventListener('online-orders-snapshot', onlineListener)
    return () => { window.removeEventListener('order-sound-alert', listener); window.removeEventListener('online-orders-snapshot', onlineListener); enabledRef.current = false; void context.current?.close() }
  }, [])

  useEffect(() => {
    if (!hospitalityEnabled) return
    let active = true
    let timer: ReturnType<typeof setTimeout>
    const tracker = new OrderAlertTracker()
    const poll = async () => {
      try {
        const result = await hospitalityApi.dashboard()
        if (active && result.success && result.data) tracker.observe(result.data.tickets).forEach(announceOrderAlert)
      } catch { /* Preserve the last successful snapshot during connection failures. */ }
      if (active) timer = setTimeout(poll, 5000)
    }
    accessApi.me().then(result => {
      if (active && result.success && result.data?.permissions?.includes('hospitality.view')) void poll()
    }).catch(() => {})
    return () => { active = false; clearTimeout(timer) }
  }, [hospitalityEnabled])

  const toggle = async () => {
    if (enabledRef.current) { enabledRef.current = false; setEnabled(false); return }
    try {
      if (!context.current || context.current.state === 'closed') context.current = new AudioContext()
      await context.current.resume()
      if (context.current.state !== 'running') throw new Error('Sound is blocked')
      enabledRef.current = true; setEnabled(true); setMessage('Sound alerts enabled'); play('placed')
    } catch { setMessage('Sound could not start. Try enabling alerts again.'); enabledRef.current = false; setEnabled(false) }
  }

  return <div style={{display:'flex',alignItems:'center',gap:8}}>
    <Btn small variant="secondary" icon={enabled ? <Bell size={14}/> : <BellOff size={14}/>} onClick={toggle} title="Kitchen orders, ready orders and online orders">{enabled ? 'Mute alerts' : 'Enable sound'}</Btn>
    <span role="status" aria-live="polite" style={{fontSize:12,maxWidth:160}}>{message}</span>
  </div>
}

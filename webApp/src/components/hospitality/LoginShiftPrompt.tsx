import React, { useEffect, useState } from 'react'
import { useAuth } from '../../App'
import { Btn, Input, Modal } from '../ui'
import { accessApi, hospitalityApi, hospitalityDutyApi, hospitalityOpsApi } from '../../services/api'

const promptKey = 'hospitalityShiftPromptUser'
type ShiftState = { dayOpen:boolean; personalOpen:boolean; canOpenDay:boolean; canStartPersonal:boolean }

export default function LoginShiftPrompt() {
  const { user } = useAuth()
  const [state,setState] = useState<ShiftState | null>(null)
  const [error,setError] = useState('')
  const [busy,setBusy] = useState(false)
  const [float,setFloat] = useState('0')
  const [retry,setRetry] = useState(0)
  const finish = () => {
    sessionStorage.removeItem(promptKey)
    setState(null)
    window.dispatchEvent(new Event('hospitality-shift-updated'))
  }
  useEffect(() => {
    if (!user?.businessId || sessionStorage.getItem(promptKey)!==user.id) return
    let active = true
    let timer:ReturnType<typeof setTimeout>
    const check = async () => {
      if(sessionStorage.getItem(promptKey)!==user.id)return
      try {
        const [status,access] = await Promise.all([hospitalityApi.status(),accessApi.me()])
        if (!active || sessionStorage.getItem(promptKey)!==user.id) return
        if (!status.success || !access.success || !access.data) throw new Error('Could not check shift access. Please retry.')
        const permissions = access.data.permissions || []
        const eligible = status.data?.enabled && access.data.enabledMenus.some(m=>['HOSPITALITY','HOSPITALITY_OPS','OPEN_TABS'].includes(m)) && permissions.includes('hospitality.view')
        if (!eligible) { sessionStorage.removeItem(promptKey); setState(null); return }
        const canStartPersonal = permissions.includes('hospitality.orders') || permissions.includes('hospitality.billing')
        const next:ShiftState = { dayOpen:status.data?.shiftOpen===true, personalOpen:!canStartPersonal, canOpenDay:permissions.includes('hospitality.shifts'), canStartPersonal }
        if (canStartPersonal) {
          const duty = await hospitalityDutyApi.dashboard()
          if (!active || sessionStorage.getItem(promptKey)!==user.id) return
          if (!duty.success || !duty.data) { setState(next); throw new Error(duty.message || 'Could not check your personal shift. Please retry.') }
          next.personalOpen = !!duty.data.shift
        }
        if (!active || sessionStorage.getItem(promptKey)!==user.id) return
        if (next.dayOpen && next.personalOpen) { sessionStorage.removeItem(promptKey); setState(null); window.dispatchEvent(new Event('hospitality-shift-updated')); return }
        setState(next)
      } catch(e:any) { if(active) setError(e.response?.data?.message || e.message || 'Could not check shifts. Please retry.') }
      if (active) timer=setTimeout(check,5000)
    }
    void check()
    return () => { active=false; clearTimeout(timer) }
  },[user?.id,retry])
  const act = async (openDay:boolean) => {
    if(busy)return
    setBusy(true);setError('')
    try {
      const r = openDay
        ? await hospitalityOpsApi.openShift({openingFloat:Number(float),notes:'Opened after signing in'})
        : await hospitalityDutyApi.start('Started after signing in')
      if(r.success===false)throw new Error(r.message||'Could not open shift')
      if(openDay){setState(current=>current?{...current,dayOpen:true}:current);setRetry(n=>n+1);window.dispatchEvent(new Event('hospitality-shift-updated'))}
      else finish()
    } catch(e:any) {setError(e.response?.data?.message || e.message || 'Could not open shift. Please retry.')}
    finally {setBusy(false)}
  }
  if(!state)return null
  const validFloat=Number.isFinite(Number(float))&&Number(float)>=0&&float.trim()!==''
  return <Modal title="Open your hospitality shift" onClose={()=>{if(!busy)finish()}} footer={<>
    <Btn variant="secondary" disabled={busy} onClick={finish}>Later</Btn>
    {!state.dayOpen&&state.canOpenDay&&<Btn variant="secondary" disabled={busy||!validFloat} onClick={()=>act(true)}>{busy?'Opening…':'Open trading day'}</Btn>}
    {state.canStartPersonal&&!state.personalOpen?<Btn disabled={busy} onClick={()=>act(false)}>{busy?'Starting…':'Start my shift'}</Btn>:<Btn disabled={busy} onClick={()=>{setError('');setRetry(n=>n+1)}}>Check again</Btn>}
  </>}>
    <p>Start your shift before taking orders or settling customer bills.</p>
    {error&&<p role="alert" style={{color:'var(--b360-red)'}}>{error}</p>}
    <p>Business trading day: <strong>{state.dayOpen?'Open':'Closed'}</strong></p>
    {state.canStartPersonal&&<p>Your personal shift: <strong>{state.personalOpen?'Open':'Not started'}</strong></p>}
    {!state.dayOpen&&(state.canOpenDay?<><Input label="Opening cash float (KES)" type="number" value={float} onChange={setFloat}/><p>To record an opening cash float, open the trading day first. Choosing Start my shift directly opens a closed trading day with zero float.</p></>:<p>Start your own shift now. If the trading day is closed, it opens automatically with zero cash float.</p>)}
    {state.canStartPersonal&&!state.personalOpen&&<p>This starts your own waiter or cashier shift. Your completed orders and collected payments will be tallied when you end it.</p>}
  </Modal>
}

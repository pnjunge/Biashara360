import React, { useEffect, useState } from 'react'
import { accessApi } from '../../services/api'
import { useAuth } from '../../App'

export function useMenuAccess() {
  const { user }=useAuth()
  const [access,setAccess]=useState<{enabledMenus:string[];permissions?:string[]}|null>(null)
  const [loading,setLoading]=useState(true)
  useEffect(()=>{let active=true;setLoading(true);setAccess(null)
    accessApi.me().then(r=>{if(active&&r.success&&r.data)setAccess(r.data)}).catch(()=>{}).finally(()=>{if(active)setLoading(false)})
    return()=>{active=false}
  },[user?.id])
  return {loading,hasMenu:(menu:string)=>!!access?.enabledMenus.includes(menu),hasPermission:(permission:string)=>!!access?.permissions?.includes(permission)}
}
export default function MenuAccess({menu,permission,children}:{menu:string;permission:string;children:React.ReactNode}) {
  const access=useMenuAccess()
  if(access.loading)return <p>Checking access…</p>
  if(!access.hasMenu(menu)||!access.hasPermission(permission))return <div role="alert" style={{padding:24}}>You do not have access to this page.</div>
  return <>{children}</>
}

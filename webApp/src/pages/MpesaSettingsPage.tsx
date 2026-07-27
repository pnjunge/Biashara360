import React, { useEffect } from 'react'
import { useSearchParams } from 'react-router-dom'
import { SettingsPage } from './SettingsPage'

export default function MpesaSettingsPage() {
  const [searchParams, setSearchParams] = useSearchParams()

  useEffect(() => {
    if (searchParams.get('tab') !== 'mpesa') {
      setSearchParams({ tab: 'mpesa' })
    }
  }, [searchParams, setSearchParams])

  return <SettingsPage />
}

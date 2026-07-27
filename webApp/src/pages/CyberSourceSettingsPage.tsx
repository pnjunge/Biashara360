import React, { useEffect } from 'react'
import { useSearchParams } from 'react-router-dom'
import { SettingsPage } from './SettingsPage'

export default function CyberSourceSettingsPage() {
  const [searchParams, setSearchParams] = useSearchParams()

  useEffect(() => {
    if (searchParams.get('tab') !== 'cybersource') {
      setSearchParams({ tab: 'cybersource' })
    }
  }, [searchParams, setSearchParams])

  return <SettingsPage />
}

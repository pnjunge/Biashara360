import { useEffect, useRef, useState } from 'react'
import { client } from '../services/api'

export type SocialCredential = { provider: 'google' | 'facebook'; token: string }
const scripts = new Map<string, Promise<void>>()
function loadScript(src: string) {
  if (!scripts.has(src)) scripts.set(src, new Promise<void>((resolve, reject) => {
    const script = document.createElement('script')
    script.src = src
    script.async = true
    script.onload = () => resolve()
    script.onerror = () => { scripts.delete(src); script.remove(); reject(new Error('Sign-in could not load. Check your connection and try again.')) }
    document.head.appendChild(script)
  }))
  return scripts.get(src)!
}

export default function SocialSignIn({ onCredential, disabled }: { onCredential: (credential: SocialCredential) => void; disabled?: boolean }) {
  const googleButton = useRef<HTMLDivElement>(null)
  const callback = useRef(onCredential)
  callback.current = credential => { if (!disabled) onCredential(credential) }
  const [facebookReady, setFacebookReady] = useState(false)
  const [error, setError] = useState('')
  useEffect(() => {
    let cancelled = false
    client.get('/auth/social/providers').then(async response => {
      const config = response.data.data
      const browser = window as any
      const tasks: Promise<void>[] = []
      if (config.googleClientId) tasks.push(loadScript('https://accounts.google.com/gsi/client').then(() => {
        if (cancelled || !googleButton.current) return
        browser.google.accounts.id.initialize({ client_id: config.googleClientId, auto_select: false,
          callback: (result: { credential: string }) => { if (!cancelled) callback.current({ provider: 'google', token: result.credential }) } })
        browser.google.accounts.id.renderButton(googleButton.current, { type: 'standard', theme: 'outline', size: 'large', text: 'continue_with', width: 320 })
      }))
      if (config.facebookAppId) tasks.push((browser.FB ? Promise.resolve() : loadScript('https://connect.facebook.net/en_US/sdk.js')).then(() => {
        if (cancelled) return
        browser.FB.init({ appId: config.facebookAppId, version: config.facebookVersion, cookie: false, xfbml: false })
        setFacebookReady(true)
      }))
      const results = await Promise.allSettled(tasks)
      if (!cancelled && results.some(result => result.status === 'rejected')) setError('Some sign-in options could not load. Try refreshing or use email signup.')
    }).catch(() => { if (!cancelled) setError('Social sign-in is unavailable. You can still use email signup.') })
    return () => { cancelled = true }
  }, [])
  return <div style={{ display: 'grid', gap: 12, marginBottom: 16 }}>
    <div ref={googleButton} aria-hidden={disabled || undefined} style={{ pointerEvents: disabled ? 'none' : undefined, opacity: disabled ? 0.5 : 1 }} />
    {facebookReady && <button type="button" disabled={disabled} style={{ padding: 12, border: 0, borderRadius: 6, background: '#1877F2', color: 'white', fontWeight: 600 }} onClick={() => {
      setError('')
      ;(window as any).FB.login((result: any) => {
        if (result.authResponse?.accessToken) callback.current({ provider: 'facebook', token: result.authResponse.accessToken })
        else setError('Facebook sign-in was cancelled or email access was declined. Try again or use email signup.')
      }, { scope: 'public_profile,email', return_scopes: true })
    }}>Continue with Facebook</button>}
    {error && <p role="alert" style={{ fontSize: 12, color: 'var(--b360-red)' }}>{error}</p>}
  </div>
}

import React, { useEffect, useState } from 'react'
import {
  Apple,
  CheckCircle2,
  Download,
  Laptop,
  Monitor,
  ShieldCheck,
  Smartphone,
} from 'lucide-react'
import { usePageSeo } from '../utils/usePageSeo'
import { appReleaseApi, AppRelease } from '../services/api'

type DownloadCardProps = {
  title: string
  subtitle: string
  icon: typeof Smartphone
  fileLabel: string
  url?: string
  available: boolean
  note: string
  release?: AppRelease
}

const configuredUrl = (value: string | undefined) => {
  const trimmed = value?.trim()
  return trimmed ? trimmed : undefined
}

export const formatBytes = (bytes: number) => {
  if (!bytes) return ''
  const units = ['B', 'KB', 'MB', 'GB']
  const i = Math.min(Math.floor(Math.log(bytes) / Math.log(1024)), units.length - 1)
  return `${(bytes / Math.pow(1024, i)).toFixed(i === 0 ? 0 : 1)} ${units[i]}`
}

function DownloadCard({
  title,
  subtitle,
  icon: Icon,
  fileLabel,
  url,
  available,
  note,
  release,
}: DownloadCardProps) {
  return (
    <article
      style={{
        display: 'flex',
        flexDirection: 'column',
        minHeight: 270,
        padding: 22,
        borderRadius: 16,
        border: '1px solid var(--b360-border)',
        background: 'white',
        boxShadow: 'var(--shadow-sm)',
      }}
    >
      <div style={{ display: 'flex', justifyContent: 'space-between', gap: 16 }}>
        <div
          style={{
            width: 48,
            height: 48,
            borderRadius: 13,
            display: 'grid',
            placeItems: 'center',
            color: available ? 'var(--b360-green-dark)' : 'var(--b360-text-secondary)',
            background: available ? 'var(--b360-green-bg)' : 'var(--b360-surface)',
          }}
        >
          <Icon size={24} aria-hidden="true" />
        </div>
        <span
          style={{
            alignSelf: 'flex-start',
            padding: '5px 9px',
            borderRadius: 999,
            fontSize: 11,
            fontWeight: 750,
            color: available ? '#047857' : '#92400e',
            background: available ? '#d1fae5' : '#fef3c7',
          }}
        >
          {available ? 'Available' : 'Coming soon'}
        </span>
      </div>

      <h2 style={{ marginTop: 18, fontSize: 18, lineHeight: 1.25 }}>{title}</h2>
      <p style={{ marginTop: 4, color: 'var(--b360-text-secondary)', fontSize: 13 }}>
        {subtitle}
      </p>
      {release && (
        <div style={{ marginTop: 12, display: 'flex', flexWrap: 'wrap', gap: 6, fontSize: 11, fontWeight: 700 }}>
          <span style={{ padding: '3px 8px', borderRadius: 999, background: '#ecfdf5', color: '#047857' }}>
            v{release.version} (build {release.buildNumber})
          </span>
          {release.fileSizeBytes > 0 && (
            <span style={{ padding: '3px 8px', borderRadius: 999, background: '#f1f5f9', color: '#475569' }}>
              {formatBytes(release.fileSizeBytes)}
            </span>
          )}
          {release.isSigned && (
            <span style={{ padding: '3px 8px', borderRadius: 999, background: '#eff6ff', color: '#1d4ed8' }}>
              ✓ Signed
            </span>
          )}
        </div>
      )}
      <p style={{ marginTop: 12, color: 'var(--b360-text-secondary)', fontSize: 12, flex: 1 }}>
        {release?.releaseNotes || note}
      </p>
      {release?.sha256 && (
        <p
          title={release.sha256}
          style={{ marginTop: 8, fontSize: 10, color: '#94a3b8', fontFamily: 'monospace', wordBreak: 'break-all' }}
        >
          SHA-256: {release.sha256}
        </p>
      )}

      {available && url ? (
        <a
          href={url}
          download
          style={{
            marginTop: 20,
            minHeight: 44,
            padding: '11px 14px',
            borderRadius: 10,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            gap: 8,
            background: 'var(--b360-green)',
            color: 'white',
            fontWeight: 750,
          }}
        >
          <Download size={17} aria-hidden="true" />
          Download {fileLabel}
        </a>
      ) : (
        <button
          type="button"
          disabled
          style={{
            marginTop: 20,
            minHeight: 44,
            padding: '11px 14px',
            borderRadius: 10,
            color: 'var(--b360-text-secondary)',
            background: 'var(--b360-surface)',
            border: '1px solid var(--b360-border)',
            fontWeight: 700,
          }}
        >
          Not yet available
        </button>
      )}
    </article>
  )
}

export default function DownloadsPage() {
  const [releases, setReleases] = useState<Record<string, AppRelease>>({})

  useEffect(() => {
    appReleaseApi.getReleases()
      .then(res => { if (res.success && res.data) setReleases(res.data) })
      .catch(() => undefined) // fall back to build-time env URLs
  }, [])

  // Uploaded releases take precedence over build-time env URLs
  const pick = (platform: string, envUrl?: string) => {
    const release = releases[platform]
    return {
      release,
      url: release ? appReleaseApi.resolveUrl(release.downloadUrl) : configuredUrl(envUrl),
    }
  }
  const android = pick('ANDROID', import.meta.env.VITE_ANDROID_DOWNLOAD_URL)
  const linux = pick('LINUX', import.meta.env.VITE_LINUX_DOWNLOAD_URL)
  const windows = pick('WINDOWS', import.meta.env.VITE_WINDOWS_DOWNLOAD_URL)
  const mac = pick('MACOS', import.meta.env.VITE_MACOS_DOWNLOAD_URL)
  const ios = pick('IOS', import.meta.env.VITE_IOS_DOWNLOAD_URL)
  const androidUrl = android.url
  const linuxUrl = linux.url
  const windowsUrl = windows.url
  const macUrl = mac.url
  const iosUrl = ios.url

  usePageSeo({
    title: 'Download Biashara360 Apps | Android APK, Windows & Mac POS',
    description: 'Download the official Biashara360 merchant point of sale apps for Android phones & tablets, Windows desktops, and macOS. High-speed offline checkout, eTIMS fiscalization, and receipt printing.',
    canonicalUrl: 'https://biashara360.co.ke/downloads',
    keywords: 'Download Biashara360, POS APK download, Android POS Kenya, Windows POS software Kenya'
  })

  return (
    <div className="fade-in" style={{ display: 'flex', flexDirection: 'column', gap: 22 }}>
      <header>
        <h1 style={{ fontSize: 26, fontWeight: 800, letterSpacing: '-0.5px' }}>
          Download Biashara360
        </h1>
        <p style={{ marginTop: 6, color: 'var(--b360-text-secondary)' }}>
          Install the merchant app on your phone or business computer.
        </p>
      </header>

      <section
        style={{
          display: 'flex',
          alignItems: 'flex-start',
          gap: 12,
          padding: 16,
          borderRadius: 13,
          color: '#065f46',
          background: 'var(--b360-green-bg)',
          border: '1px solid #a7f3d0',
        }}
      >
        <ShieldCheck size={22} style={{ flex: '0 0 auto', marginTop: 1 }} aria-hidden="true" />
        <div>
          <strong>Official merchant downloads</strong>
          <p style={{ marginTop: 3, fontSize: 12 }}>
            These installers are published by Biashara360. Android users may need to allow
            installation from this browser when installing the APK.
          </p>
        </div>
      </section>

      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(245px, 1fr))',
          gap: 16,
        }}
      >
        <DownloadCard
          title="Android"
          subtitle="Android 7.0 or newer"
          icon={Smartphone}
          fileLabel="APK"
          url={androidUrl}
          release={android.release}
          available={Boolean(androidUrl)}
          note="Signed production APK for Android phones and tablets."
        />
        <DownloadCard
          title="iPhone & iPad"
          subtitle="Install through the Apple App Store"
          icon={Apple}
          fileLabel="on the App Store"
          url={iosUrl}
          release={ios.release}
          available={Boolean(iosUrl)}
          note="The App Store link will appear after Apple review and production release."
        />
        <DownloadCard
          title="Windows desktop"
          subtitle="Windows 10 or newer"
          icon={Monitor}
          fileLabel="installer"
          url={windowsUrl}
          release={windows.release}
          available={Boolean(windowsUrl)}
          note="The signed Windows installer will be published after Windows code signing."
        />
        <DownloadCard
          title="Linux desktop"
          subtitle="64-bit Debian and Ubuntu"
          icon={Laptop}
          fileLabel="DEB"
          url={linuxUrl}
          release={linux.release}
          available={Boolean(linuxUrl)}
          note="Production package for supported 64-bit Debian-based computers."
        />
        <DownloadCard
          title="macOS desktop"
          subtitle="Install a notarized macOS application"
          icon={CheckCircle2}
          fileLabel="DMG"
          url={macUrl}
          release={mac.release}
          available={Boolean(macUrl)}
          note="The download will appear after Apple Developer signing and notarization."
        />
      </div>
    </div>
  )
}

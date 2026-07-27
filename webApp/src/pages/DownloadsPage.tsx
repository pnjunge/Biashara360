import {
  Apple,
  CheckCircle2,
  Download,
  Laptop,
  Monitor,
  ShieldCheck,
  Smartphone,
} from 'lucide-react'

type DownloadCardProps = {
  title: string
  subtitle: string
  icon: typeof Smartphone
  fileLabel: string
  url?: string
  available: boolean
  note: string
}

const configuredUrl = (value: string | undefined) => {
  const trimmed = value?.trim()
  return trimmed ? trimmed : undefined
}

function DownloadCard({
  title,
  subtitle,
  icon: Icon,
  fileLabel,
  url,
  available,
  note,
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
      <p style={{ marginTop: 16, color: 'var(--b360-text-secondary)', fontSize: 12, flex: 1 }}>
        {note}
      </p>

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
  const androidUrl = configuredUrl(import.meta.env.VITE_ANDROID_DOWNLOAD_URL)
  const linuxUrl = configuredUrl(import.meta.env.VITE_LINUX_DOWNLOAD_URL)
  const windowsUrl = configuredUrl(import.meta.env.VITE_WINDOWS_DOWNLOAD_URL)
  const macUrl = configuredUrl(import.meta.env.VITE_MACOS_DOWNLOAD_URL)
  const iosUrl = configuredUrl(import.meta.env.VITE_IOS_DOWNLOAD_URL)

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
          available={Boolean(androidUrl)}
          note="Signed production APK for Android phones and tablets."
        />
        <DownloadCard
          title="iPhone & iPad"
          subtitle="Install through the Apple App Store"
          icon={Apple}
          fileLabel="on the App Store"
          url={iosUrl}
          available={Boolean(iosUrl)}
          note="The App Store link will appear after Apple review and production release."
        />
        <DownloadCard
          title="Windows desktop"
          subtitle="Windows 10 or newer"
          icon={Monitor}
          fileLabel="installer"
          url={windowsUrl}
          available={Boolean(windowsUrl)}
          note="The signed Windows installer will be published after Windows code signing."
        />
        <DownloadCard
          title="Linux desktop"
          subtitle="64-bit Debian and Ubuntu"
          icon={Laptop}
          fileLabel="DEB"
          url={linuxUrl}
          available={Boolean(linuxUrl)}
          note="Production package for supported 64-bit Debian-based computers."
        />
        <DownloadCard
          title="macOS desktop"
          subtitle="Install a notarized macOS application"
          icon={CheckCircle2}
          fileLabel="DMG"
          url={macUrl}
          available={Boolean(macUrl)}
          note="The download will appear after Apple Developer signing and notarization."
        />
      </div>
    </div>
  )
}

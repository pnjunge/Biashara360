import React, { useCallback, useEffect, useRef, useState } from 'react'
import {
  Apple,
  CheckCircle2,
  Copy,
  ExternalLink,
  Laptop,
  Link2,
  Monitor,
  Power,
  ShieldCheck,
  Smartphone,
  Trash2,
  UploadCloud,
} from 'lucide-react'
import { appReleaseApi, AppRelease } from '../services/api'
import { formatBytes } from './DownloadsPage'

type Platform = AppRelease['platform']

const PLATFORMS: { value: Platform; label: string; icon: typeof Smartphone; accept: string; hint: string }[] = [
  { value: 'ANDROID', label: 'Android', icon: Smartphone, accept: '.apk,.aab', hint: 'Signed release APK (.apk)' },
  { value: 'WINDOWS', label: 'Windows', icon: Monitor, accept: '.msi,.exe,.zip', hint: 'Code-signed installer (.msi / .exe)' },
  { value: 'LINUX', label: 'Linux', icon: Laptop, accept: '.deb,.appimage,.tar.gz,.zip', hint: 'Debian package (.deb) or AppImage' },
  { value: 'MACOS', label: 'macOS', icon: CheckCircle2, accept: '.dmg,.pkg,.zip', hint: 'Notarized disk image (.dmg / .pkg)' },
  { value: 'IOS', label: 'iOS', icon: Apple, accept: '', hint: 'App Store link (external only)' },
]

const platformMeta = (p: string) => PLATFORMS.find(x => x.value === p) || PLATFORMS[0]

const card: React.CSSProperties = {
  background: 'white',
  border: '1px solid var(--b360-border)',
  borderRadius: 16,
  padding: 22,
  boxShadow: 'var(--shadow-sm)',
}

const label: React.CSSProperties = { display: 'block', fontSize: 12, fontWeight: 700, marginBottom: 6, color: '#334155' }
const input: React.CSSProperties = {
  width: '100%', padding: '10px 12px', borderRadius: 10, border: '1px solid #e2e8f0', fontSize: 14, background: 'white',
}

export default function AppReleasesPage() {
  const [releases, setReleases] = useState<AppRelease[]>([])
  const [loading, setLoading] = useState(true)
  const [mode, setMode] = useState<'upload' | 'external'>('upload')

  const [platform, setPlatform] = useState<Platform>('ANDROID')
  const [version, setVersion] = useState('1.0.0')
  const [buildNumber, setBuildNumber] = useState(1)
  const [minOsVersion, setMinOsVersion] = useState('')
  const [releaseNotes, setReleaseNotes] = useState('')
  const [isSigned, setIsSigned] = useState(true)
  const [makeActive, setMakeActive] = useState(true)
  const [file, setFile] = useState<File | null>(null)
  const [externalUrl, setExternalUrl] = useState('')
  const [dragOver, setDragOver] = useState(false)

  const [progress, setProgress] = useState<number | null>(null)
  const [message, setMessage] = useState<{ type: 'ok' | 'err'; text: string } | null>(null)
  const fileInputRef = useRef<HTMLInputElement>(null)

  const load = useCallback(() => {
    setLoading(true)
    appReleaseApi.list()
      .then(res => { if (res.success) setReleases(res.data || []) })
      .catch(err => setMessage({ type: 'err', text: err?.response?.data?.message || 'Failed to load releases' }))
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => { load() }, [load])
  useEffect(() => { if (platform === 'IOS') setMode('external') }, [platform])

  const resetForm = () => {
    setFile(null)
    setReleaseNotes('')
    setExternalUrl('')
    setProgress(null)
    if (fileInputRef.current) fileInputRef.current.value = ''
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setMessage(null)
    if (!version.trim()) return setMessage({ type: 'err', text: 'Version is required' })

    try {
      if (mode === 'upload') {
        if (!file) return setMessage({ type: 'err', text: 'Select a signed build file to upload' })
        const fd = new FormData()
        // Form fields must precede the file so the server reads metadata first
        fd.append('platform', platform)
        fd.append('version', version.trim())
        fd.append('buildNumber', String(buildNumber))
        fd.append('minOsVersion', minOsVersion)
        fd.append('releaseNotes', releaseNotes)
        fd.append('isSigned', String(isSigned))
        fd.append('makeActive', String(makeActive))
        fd.append('file', file)
        setProgress(0)
        const res = await appReleaseApi.upload(fd, setProgress)
        if (!res.success) throw new Error(res.message)
        setMessage({ type: 'ok', text: `${platformMeta(platform).label} v${version} published successfully` })
      } else {
        if (!/^https:\/\//i.test(externalUrl.trim())) {
          return setMessage({ type: 'err', text: 'External link must start with https://' })
        }
        const res = await appReleaseApi.createExternal({
          platform, version: version.trim(), buildNumber, downloadUrl: externalUrl.trim(),
          releaseNotes: releaseNotes || undefined, minOsVersion: minOsVersion || undefined,
          isSigned, isActive: makeActive,
        })
        if (!res.success) throw new Error(res.message)
        setMessage({ type: 'ok', text: `${platformMeta(platform).label} external release registered` })
      }
      resetForm()
      load()
    } catch (err: any) {
      setProgress(null)
      setMessage({ type: 'err', text: err?.response?.data?.message || err?.message || 'Upload failed' })
    }
  }

  const toggle = async (r: AppRelease) => {
    const res = await appReleaseApi.toggleStatus(r.id, !r.isActive).catch(() => null)
    if (res?.success) load()
    else setMessage({ type: 'err', text: res?.message || 'Failed to update status' })
  }

  const remove = async (r: AppRelease) => {
    if (!window.confirm(`Delete ${platformMeta(r.platform).label} v${r.version}? This removes the file permanently.`)) return
    const res = await appReleaseApi.delete(r.id).catch(() => null)
    if (res?.success) load()
    else setMessage({ type: 'err', text: res?.message || 'Failed to delete release' })
  }

  const meta = platformMeta(platform)
  const uploading = progress !== null

  return (
    <div className="fade-in" style={{ display: 'flex', flexDirection: 'column', gap: 22 }}>
      <header>
        <h1 style={{ fontSize: 26, fontWeight: 800, letterSpacing: '-0.5px' }}>App Releases</h1>
        <p style={{ marginTop: 6, color: 'var(--b360-text-secondary)' }}>
          Publish signed Android and desktop builds. The latest active release per platform appears on the public{' '}
          <a href="/downloads" target="_blank" rel="noreferrer" style={{ color: 'var(--b360-green)', fontWeight: 700 }}>Downloads page</a>.
        </p>
      </header>

      {message && (
        <div style={{
          padding: '12px 16px', borderRadius: 12, fontSize: 14, fontWeight: 600,
          background: message.type === 'ok' ? '#ecfdf5' : '#fef2f2',
          color: message.type === 'ok' ? '#047857' : '#b91c1c',
          border: `1px solid ${message.type === 'ok' ? '#a7f3d0' : '#fecaca'}`,
        }}>
          {message.text}
        </div>
      )}

      {/* ── Publish form ── */}
      <form onSubmit={handleSubmit} style={card}>
        <div style={{ display: 'flex', gap: 8, marginBottom: 18 }}>
          {(['upload', 'external'] as const).map(m => (
            <button
              key={m}
              type="button"
              id={`release-mode-${m}`}
              disabled={m === 'upload' && platform === 'IOS'}
              onClick={() => setMode(m)}
              style={{
                display: 'flex', alignItems: 'center', gap: 6, padding: '8px 14px', borderRadius: 999, fontSize: 13, fontWeight: 700,
                border: '1px solid', borderColor: mode === m ? 'var(--b360-green)' : '#e2e8f0',
                background: mode === m ? 'var(--b360-green-bg)' : 'white',
                color: mode === m ? 'var(--b360-green-dark)' : '#475569',
                opacity: m === 'upload' && platform === 'IOS' ? 0.5 : 1,
              }}
            >
              {m === 'upload' ? <UploadCloud size={15} /> : <Link2 size={15} />}
              {m === 'upload' ? 'Upload signed file' : 'External link (Play Store, S3, App Store)'}
            </button>
          ))}
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: 14 }}>
          <div>
            <label style={label} htmlFor="release-platform">Platform</label>
            <select id="release-platform" style={input} value={platform} onChange={e => setPlatform(e.target.value as Platform)}>
              {PLATFORMS.map(p => <option key={p.value} value={p.value}>{p.label}</option>)}
            </select>
          </div>
          <div>
            <label style={label} htmlFor="release-version">Version</label>
            <input id="release-version" style={input} value={version} onChange={e => setVersion(e.target.value)} />
          </div>
          <div>
            <label style={label} htmlFor="release-build">Build number / versionCode</label>
            <input id="release-build" type="number" min={1} style={input} value={buildNumber} onChange={e => setBuildNumber(Number(e.target.value) || 1)} />
          </div>
          <div>
            <label style={label} htmlFor="release-minos">Minimum OS</label>
            <input id="release-minos" style={input} value={minOsVersion} onChange={e => setMinOsVersion(e.target.value)} />
          </div>
        </div>

        {mode === 'upload' ? (
          <div
            id="release-dropzone"
            onClick={() => fileInputRef.current?.click()}
            onDragOver={e => { e.preventDefault(); setDragOver(true) }}
            onDragLeave={() => setDragOver(false)}
            onDrop={e => { e.preventDefault(); setDragOver(false); if (e.dataTransfer.files[0]) setFile(e.dataTransfer.files[0]) }}
            style={{
              marginTop: 16, padding: 28, borderRadius: 14, cursor: 'pointer', textAlign: 'center',
              border: `2px dashed ${dragOver ? 'var(--b360-green)' : '#cbd5e1'}`,
              background: dragOver ? 'var(--b360-green-bg)' : '#f8fafc', transition: 'all .15s ease',
            }}
          >
            <input
              ref={fileInputRef}
              type="file"
              accept={meta.accept}
              style={{ display: 'none' }}
              onChange={e => setFile(e.target.files?.[0] || null)}
            />
            <UploadCloud size={32} color="var(--b360-green)" style={{ margin: '0 auto 8px' }} />
            {file ? (
              <>
                <div style={{ fontWeight: 800 }}>{file.name}</div>
                <div style={{ fontSize: 12, color: '#64748b', marginTop: 4 }}>{formatBytes(file.size)} • click to change</div>
              </>
            ) : (
              <>
                <div style={{ fontWeight: 700 }}>Drop the {meta.label} build here or click to browse</div>
                <div style={{ fontSize: 12, color: '#64748b', marginTop: 4 }}>{meta.hint}</div>
              </>
            )}
          </div>
        ) : (
          <div style={{ marginTop: 16 }}>
            <label style={label} htmlFor="release-url">Download / store URL</label>
            <input
              id="release-url"
              style={input}
              value={externalUrl}
              onChange={e => setExternalUrl(e.target.value)}
            />
          </div>
        )}

        <div style={{ marginTop: 16 }}>
          <label style={label} htmlFor="release-notes">Release notes</label>
          <textarea
            id="release-notes"
            rows={3}
            style={{ ...input, resize: 'vertical' }}
            value={releaseNotes}
            onChange={e => setReleaseNotes(e.target.value)}
          />
        </div>

        <div style={{ marginTop: 14, display: 'flex', flexWrap: 'wrap', gap: 20, fontSize: 13, fontWeight: 600 }}>
          <label style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <input id="release-signed" type="checkbox" checked={isSigned} onChange={e => setIsSigned(e.target.checked)} />
            Build is signed (release keystore / code-signing certificate)
          </label>
          <label style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <input id="release-active" type="checkbox" checked={makeActive} onChange={e => setMakeActive(e.target.checked)} />
            Publish immediately (replaces current active {meta.label} release)
          </label>
        </div>

        {uploading && (
          <div style={{ marginTop: 16 }}>
            <div style={{ height: 8, borderRadius: 999, background: '#e2e8f0', overflow: 'hidden' }}>
              <div style={{ width: `${progress}%`, height: '100%', background: 'var(--b360-green)', transition: 'width .2s ease' }} />
            </div>
            <div style={{ fontSize: 12, color: '#64748b', marginTop: 6 }}>
              {progress! < 100 ? `Uploading… ${progress}%` : 'Verifying checksum and publishing…'}
            </div>
          </div>
        )}

        <button
          type="submit"
          id="release-submit"
          disabled={uploading}
          style={{
            marginTop: 18, padding: '12px 22px', borderRadius: 10, border: 'none', fontWeight: 800, fontSize: 14,
            display: 'inline-flex', alignItems: 'center', gap: 8,
            background: 'var(--b360-green)', color: 'white', opacity: uploading ? 0.6 : 1, cursor: uploading ? 'wait' : 'pointer',
          }}
        >
          <ShieldCheck size={17} />
          {mode === 'upload' ? 'Upload & publish' : 'Register release'}
        </button>
      </form>

      {/* ── Release history ── */}
      <section style={card}>
        <h2 style={{ fontSize: 17, fontWeight: 800, marginBottom: 14 }}>Release history</h2>
        {loading ? (
          <p style={{ color: '#64748b' }}>Loading releases…</p>
        ) : releases.length === 0 ? (
          <p style={{ color: '#64748b' }}>No releases published yet.</p>
        ) : (
          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
              <thead>
                <tr style={{ textAlign: 'left', color: '#64748b', fontSize: 11, textTransform: 'uppercase', letterSpacing: '.04em' }}>
                  <th style={{ padding: '8px 10px' }}>Platform</th>
                  <th style={{ padding: '8px 10px' }}>Version</th>
                  <th style={{ padding: '8px 10px' }}>File</th>
                  <th style={{ padding: '8px 10px' }}>SHA-256</th>
                  <th style={{ padding: '8px 10px' }}>Downloads</th>
                  <th style={{ padding: '8px 10px' }}>Status</th>
                  <th style={{ padding: '8px 10px' }}>Published</th>
                  <th style={{ padding: '8px 10px' }} />
                </tr>
              </thead>
              <tbody>
                {releases.map(r => {
                  const Icon = platformMeta(r.platform).icon
                  const url = appReleaseApi.resolveUrl(r.downloadUrl)
                  return (
                    <tr key={r.id} style={{ borderTop: '1px solid #f1f5f9' }}>
                      <td style={{ padding: '10px' }}>
                        <span style={{ display: 'inline-flex', alignItems: 'center', gap: 6, fontWeight: 700 }}>
                          <Icon size={15} /> {platformMeta(r.platform).label}
                        </span>
                      </td>
                      <td style={{ padding: '10px', fontWeight: 700 }}>
                        v{r.version} <span style={{ color: '#94a3b8', fontWeight: 500 }}>({r.buildNumber})</span>
                        {r.isSigned && <span title="Signed" style={{ marginLeft: 6, color: '#1d4ed8' }}>✓</span>}
                      </td>
                      <td style={{ padding: '10px' }}>
                        <div style={{ maxWidth: 220, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }} title={r.fileName}>{r.fileName}</div>
                        <div style={{ color: '#94a3b8', fontSize: 11 }}>{r.fileSizeBytes > 0 ? formatBytes(r.fileSizeBytes) : 'External link'}</div>
                      </td>
                      <td style={{ padding: '10px' }}>
                        {r.sha256 ? (
                          <button
                            type="button"
                            title={`Copy ${r.sha256}`}
                            onClick={() => navigator.clipboard?.writeText(r.sha256!)}
                            style={{ display: 'inline-flex', alignItems: 'center', gap: 4, fontFamily: 'monospace', fontSize: 11, background: 'none', color: '#475569' }}
                          >
                            {r.sha256.slice(0, 12)}… <Copy size={12} />
                          </button>
                        ) : <span style={{ color: '#cbd5e1' }}>—</span>}
                      </td>
                      <td style={{ padding: '10px' }}>{r.downloadCount.toLocaleString()}</td>
                      <td style={{ padding: '10px' }}>
                        <span style={{
                          padding: '3px 9px', borderRadius: 999, fontSize: 11, fontWeight: 800,
                          background: r.isActive ? '#d1fae5' : '#f1f5f9', color: r.isActive ? '#047857' : '#64748b',
                        }}>
                          {r.isActive ? 'LIVE' : 'Inactive'}
                        </span>
                      </td>
                      <td style={{ padding: '10px', color: '#64748b', whiteSpace: 'nowrap' }}>{new Date(r.createdAt).toLocaleDateString()}</td>
                      <td style={{ padding: '10px' }}>
                        <div style={{ display: 'flex', gap: 6, justifyContent: 'flex-end' }}>
                          <a href={url} target="_blank" rel="noreferrer" title="Open download" style={iconBtn}><ExternalLink size={15} /></a>
                          <button type="button" title={r.isActive ? 'Deactivate' : 'Make live'} onClick={() => toggle(r)} style={{ ...iconBtn, color: r.isActive ? '#b45309' : '#047857' }}>
                            <Power size={15} />
                          </button>
                          <button type="button" title="Delete" onClick={() => remove(r)} style={{ ...iconBtn, color: '#b91c1c' }}>
                            <Trash2 size={15} />
                          </button>
                        </div>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  )
}

const iconBtn: React.CSSProperties = {
  width: 32, height: 32, borderRadius: 8, display: 'grid', placeItems: 'center',
  border: '1px solid #e2e8f0', background: 'white', color: '#475569',
}

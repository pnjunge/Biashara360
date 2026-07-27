/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_ANDROID_DOWNLOAD_URL?: string
  readonly VITE_IOS_DOWNLOAD_URL?: string
  readonly VITE_WINDOWS_DOWNLOAD_URL?: string
  readonly VITE_LINUX_DOWNLOAD_URL?: string
  readonly VITE_MACOS_DOWNLOAD_URL?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}

declare module '*.module.css' {
  const classes: Record<string, string>
  export default classes
}

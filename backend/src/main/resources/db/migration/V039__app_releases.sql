-- V039: App Releases for signed Android APK and Desktop Installers (Windows, Linux, macOS)
CREATE TABLE IF NOT EXISTS app_releases (
    id VARCHAR(36) PRIMARY KEY,
    platform VARCHAR(20) NOT NULL,
    version VARCHAR(50) NOT NULL,
    build_number INTEGER NOT NULL DEFAULT 1,
    file_name VARCHAR(255) NOT NULL,
    file_size_bytes BIGINT NOT NULL DEFAULT 0,
    sha256 VARCHAR(64),
    download_url TEXT NOT NULL,
    release_notes TEXT,
    min_os_version VARCHAR(100),
    is_signed BOOLEAN NOT NULL DEFAULT TRUE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    download_count BIGINT NOT NULL DEFAULT 0,
    uploaded_by VARCHAR(36) REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_app_releases_platform_active ON app_releases(platform, is_active);
CREATE INDEX IF NOT EXISTS idx_app_releases_created_at ON app_releases(created_at DESC);

package com.app.biashara.services

import java.io.ByteArrayInputStream
import java.net.URI
import java.util.Base64
import javax.imageio.ImageIO

/** Bounded thumbnails are stored with the category; no ephemeral upload directory is needed. */
internal object CategoryImage {
    private const val MAX_BYTES = 256 * 1024
    const val ERROR = "Use an HTTP(S) image URL or a JPEG/PNG thumbnail up to 512 pixels and 256 KB"

    fun validate(value: String?): String? {
        val normalized = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        if (normalized.startsWith("https://") || normalized.startsWith("http://")) {
            return normalized.takeIf { it.length <= 500 && runCatching { !URI(it).host.isNullOrBlank() }.getOrDefault(false) }
        }
        val prefix = when {
            normalized.startsWith("data:image/jpeg;base64,") -> "data:image/jpeg;base64,"
            normalized.startsWith("data:image/png;base64,") -> "data:image/png;base64,"
            else -> return null
        }
        if (normalized.length > prefix.length + ((MAX_BYTES + 2) / 3) * 4) return null
        return runCatching {
            val bytes = Base64.getDecoder().decode(normalized.substring(prefix.length))
            if (bytes.size > MAX_BYTES) return null
            ImageIO.createImageInputStream(ByteArrayInputStream(bytes)).use { stream ->
                val readers = ImageIO.getImageReaders(stream)
                if (!readers.hasNext()) return null
                val reader = readers.next()
                try {
                    reader.input = stream
                    val expectedFormat = if (prefix.contains("jpeg")) "JPEG" else "PNG"
                    if (!reader.formatName.equals(expectedFormat, ignoreCase = true)) return null
                    if (reader.getWidth(0) !in 1..512 || reader.getHeight(0) !in 1..512) return null
                    reader.read(0) ?: return null
                } finally { reader.dispose() }
            }
            normalized
        }.getOrNull()
    }
}

package com.app.biashara.routes

import com.app.biashara.models.ApiResponse
import com.app.biashara.models.CreateAppReleaseRequest
import com.app.biashara.models.UpdateAppReleaseStatusRequest
import com.app.biashara.services.AppReleaseService
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory
import java.io.File
import java.security.DigestInputStream
import java.security.MessageDigest
import java.util.UUID

private val logger = LoggerFactory.getLogger("AppReleaseRoutes")

private val ALLOWED_EXTENSIONS = setOf(
    "apk", "aab", "msi", "exe", "deb", "appimage", "dmg", "pkg", "zip", "tar.gz", "tar", "gz"
)

private fun resolveMimeType(fileName: String): ContentType {
    val ext = fileName.substringAfterLast('.', "").lowercase()
    return when (ext) {
        "apk" -> ContentType.parse("application/vnd.android.package-archive")
        "msi" -> ContentType.parse("application/x-msi")
        "exe" -> ContentType.parse("application/x-msdownload")
        "deb" -> ContentType.parse("application/vnd.debian.binary-package")
        "dmg" -> ContentType.parse("application/x-apple-diskimage")
        "zip" -> ContentType.Application.Zip
        "gz", "tar.gz" -> ContentType.Application.GZip
        else -> ContentType.Application.OctetStream
    }
}

// ─── Public Download Routes ──────────────────────────────────────────────────
fun Route.publicAppReleaseRoutes() {
    val appReleaseService: AppReleaseService by inject()

    route("/downloads") {
        // GET /v1/downloads/releases - latest active release per platform
        get("/releases") {
            val releases = appReleaseService.getActiveReleasesByPlatform()
            call.respond(ApiResponse(true, data = releases))
        }

        // GET /v1/downloads/list - list all active releases
        get("/list") {
            val releases = appReleaseService.listReleases(activeOnly = true)
            call.respond(ApiResponse(true, data = releases))
        }

        // GET /v1/downloads/files/{id} - stream binary file
        get("/files/{id}") {
            val id = call.parameters["id"]
            if (id.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, message = "Release id is required"))
                return@get
            }

            val pair = appReleaseService.getReleaseFile(id)
            if (pair == null) {
                call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, message = "Download file not found on server"))
                return@get
            }

            val (file, release) = pair
            appReleaseService.incrementDownloadCount(release.id)

            val mimeType = resolveMimeType(release.fileName)
            call.response.header(
                HttpHeaders.ContentDisposition,
                ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, release.fileName).toString()
            )
            call.response.header(HttpHeaders.ContentType, mimeType.toString())
            call.response.header(HttpHeaders.ContentLength, file.length().toString())
            call.respondFile(file)
        }

        // GET /v1/downloads/latest/{platform} - direct shortcut to latest release (e.g. /v1/downloads/latest/android)
        get("/latest/{platform}") {
            val platform = call.parameters["platform"]
            if (platform.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, message = "Platform is required"))
                return@get
            }

            val release = appReleaseService.getLatestActiveRelease(platform)
            if (release == null) {
                call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, message = "No active release found for platform $platform"))
                return@get
            }

            if (release.downloadUrl.startsWith("/v1/downloads/files/")) {
                val pair = appReleaseService.getReleaseFile(release.id)
                if (pair != null) {
                    val (file, _) = pair
                    appReleaseService.incrementDownloadCount(release.id)
                    val mimeType = resolveMimeType(release.fileName)
                    call.response.header(
                        HttpHeaders.ContentDisposition,
                        ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, release.fileName).toString()
                    )
                    call.response.header(HttpHeaders.ContentType, mimeType.toString())
                    call.response.header(HttpHeaders.ContentLength, file.length().toString())
                    call.respondFile(file)
                    return@get
                }
            }

            // Redirect to downloadUrl (or external link)
            call.respondRedirect(release.downloadUrl)
        }
    }
}

// ─── Admin / SuperAdmin Upload & Management Routes ───────────────────────────
fun Route.adminAppReleaseRoutes() {
    val appReleaseService: AppReleaseService by inject()

    route("/admin/app-releases") {
        // GET /v1/admin/app-releases - list all uploaded releases
        get {
            if (!call.hasRole("SUPERADMIN")) {
                call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, message = "Superadmin access required"))
                return@get
            }
            val platform = call.request.queryParameters["platform"]
            val releases = appReleaseService.listReleases(platform = platform)
            call.respond(ApiResponse(true, data = releases))
        }

        // POST /v1/admin/app-releases/upload - multipart upload for signed .apk, .msi, .deb, .dmg, etc.
        post("/upload") {
            if (!call.hasRole("SUPERADMIN")) {
                call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, message = "Superadmin access required to publish releases"))
                return@post
            }

            val callerUserId = call.callerUserId()
            val storageDir = appReleaseService.getStorageDirectory()

            var platform = "ANDROID"
            var version = "1.0.0"
            var buildNumber = 1
            var minOsVersion: String? = null
            var releaseNotes: String? = null
            var isSigned = true
            var makeActive = true

            var originalFileName = ""
            var targetStoredFileName = ""
            var fileSizeBytes = 0L
            var sha256Hex = ""
            var releaseId = UUID.randomUUID().toString()
            var fileUploaded = false

            try {
                val multipart = call.receiveMultipart()
                multipart.forEachPart { part ->
                    when (part) {
                        is PartData.FormItem -> {
                            when (part.name) {
                                "platform" -> platform = part.value.uppercase().trim()
                                "version" -> version = part.value.trim()
                                "buildNumber" -> buildNumber = part.value.toIntOrNull() ?: 1
                                "minOsVersion" -> minOsVersion = part.value.trim().takeIf { it.isNotBlank() }
                                "releaseNotes" -> releaseNotes = part.value.trim().takeIf { it.isNotBlank() }
                                "isSigned" -> isSigned = part.value.toBooleanStrictOrNull() ?: true
                                "makeActive" -> makeActive = part.value.toBooleanStrictOrNull() ?: true
                            }
                        }
                        is PartData.FileItem -> {
                            originalFileName = part.originalFileName ?: "app-release.bin"
                            val extension = originalFileName.substringAfterLast('.', "").lowercase()

                            if (extension !in ALLOWED_EXTENSIONS) {
                                part.dispose()
                                throw IllegalArgumentException("Unsupported file extension .$extension. Allowed extensions: ${ALLOWED_EXTENSIONS.joinToString(", ")}")
                            }

                            val sanitized = originalFileName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
                            targetStoredFileName = "${releaseId}_$sanitized"
                            val targetFile = File(storageDir, targetStoredFileName)

                            val digest = MessageDigest.getInstance("SHA-256")
                            part.streamProvider().use { input ->
                                DigestInputStream(input, digest).use { dis ->
                                    targetFile.outputStream().use { fos ->
                                        fileSizeBytes = dis.copyTo(fos)
                                    }
                                }
                            }
                            sha256Hex = digest.digest().joinToString("") { "%02x".format(it) }
                            fileUploaded = true
                        }
                        else -> {}
                    }
                    part.dispose()
                }

                if (!fileUploaded || fileSizeBytes <= 0) {
                    call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, message = "No valid file received or file is empty"))
                    return@post
                }

                if (version.isBlank()) {
                    version = "1.0.0"
                }

                val savedRelease = appReleaseService.saveUploadedRelease(
                    releaseId = releaseId,
                    platform = platform,
                    version = version,
                    buildNumber = buildNumber,
                    originalFileName = originalFileName,
                    fileSizeBytes = fileSizeBytes,
                    sha256Hex = sha256Hex,
                    minOsVersion = minOsVersion,
                    releaseNotes = releaseNotes,
                    isSigned = isSigned,
                    makeActive = makeActive,
                    uploadedBy = callerUserId
                )

                call.respond(HttpStatusCode.Created, ApiResponse(true, data = savedRelease, message = "Signed app uploaded and published successfully"))
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, message = e.message ?: "Invalid upload parameter"))
            } catch (e: Exception) {
                logger.error("Failed to process app release upload: ${e.message}", e)
                call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(false, message = "Failed to upload file: ${e.message}"))
            }
        }

        // POST /v1/admin/app-releases/external - register an external release (e.g. Play Store or S3)
        post("/external") {
            if (!call.hasRole("SUPERADMIN")) {
                call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, message = "Superadmin access required"))
                return@post
            }
            val req = call.receive<CreateAppReleaseRequest>()
            if (req.downloadUrl.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, message = "Download URL is required"))
                return@post
            }
            val result = appReleaseService.saveExternalRelease(req, call.callerUserId())
            call.respond(if (result.success) HttpStatusCode.Created else HttpStatusCode.BadRequest, result)
        }

        // PATCH /v1/admin/app-releases/{id}/status - toggle active status
        patch("/{id}/status") {
            if (!call.hasRole("SUPERADMIN")) {
                call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, message = "Superadmin access required"))
                return@patch
            }
            val id = call.parameters["id"]
            if (id.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, message = "Release id is required"))
                return@patch
            }
            val req = call.receive<UpdateAppReleaseStatusRequest>()
            val result = appReleaseService.updateStatus(id, req.isActive)
            call.respond(if (result.success) HttpStatusCode.OK else HttpStatusCode.NotFound, result)
        }

        // DELETE /v1/admin/app-releases/{id} - delete release & physical file
        delete("/{id}") {
            if (!call.hasRole("SUPERADMIN")) {
                call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, message = "Superadmin access required"))
                return@delete
            }
            val id = call.parameters["id"]
            if (id.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, message = "Release id is required"))
                return@delete
            }
            val result = appReleaseService.deleteRelease(id)
            call.respond(if (result.success) HttpStatusCode.OK else HttpStatusCode.NotFound, result)
        }
    }
}

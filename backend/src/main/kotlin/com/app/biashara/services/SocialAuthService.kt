package com.app.biashara.services

import com.app.biashara.models.*
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.config.ApplicationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class SocialAuthService(config: ApplicationConfig, private val client: HttpClient) {
    private fun setting(config: ApplicationConfig, key: String, env: String) =
        config.propertyOrNull(key)?.getString()?.trim()?.takeIf { it.isNotEmpty() }
            ?: System.getenv(env)?.trim().orEmpty()
    private val googleId = setting(config, "socialAuth.googleClientId", "GOOGLE_CLIENT_ID")
    private val facebookId = setting(config, "socialAuth.facebookAppId", "FACEBOOK_LOGIN_APP_ID")
    private val facebookSecret = setting(config, "socialAuth.facebookAppSecret", "FACEBOOK_LOGIN_APP_SECRET")
    private val version = setting(config, "socialAuth.facebookVersion", "FACEBOOK_LOGIN_GRAPH_VERSION").ifEmpty { "v23.0" }
    private val googleVerifier = GoogleIdTokenVerifier.Builder(NetHttpTransport(), GsonFactory.getDefaultInstance())
        .setAudience(listOf(googleId)).build()

    fun configuration() = SocialProviderConfig(googleId, if (facebookSecret.isNotEmpty()) facebookId else "", version)

    suspend fun verify(credential: SocialCredential): SocialProfile {
        require(credential.token.length in 1..16384) { "Invalid provider credential" }
        return when (credential.provider) {
            "google" -> {
                require(googleId.isNotBlank()) { "Google sign-in is not configured" }
                val payload = withContext(Dispatchers.IO) { googleVerifier.verify(credential.token)?.payload }
                    ?: throw IllegalArgumentException("Google sign-in expired. Please try again.")
                require(payload.emailVerified == true && !payload.email.isNullOrBlank()) { "Google must share a verified email address" }
                require(!payload.subject.isNullOrBlank()) { "Missing Google account identity" }
                SocialProfile("google", payload.subject, payload["name"]?.toString().orEmpty(), payload.email.lowercase())
            }
            "facebook" -> {
                require(facebookId.isNotBlank() && facebookSecret.isNotBlank()) { "Facebook sign-in is not configured" }
                val debug = client.get("https://graph.facebook.com/$version/debug_token") {
                    bearerAuth("$facebookId|$facebookSecret")
                    parameter("input_token", credential.token)
                }
                require(debug.status.isSuccess()) { "Could not verify Facebook sign-in" }
                val data = debug.body<JsonObject>()["data"]?.jsonObject
                    ?: throw IllegalArgumentException("Invalid Facebook credential")
                val now = System.currentTimeMillis() / 1000
                require(data["is_valid"]?.jsonPrimitive?.booleanOrNull == true &&
                    data["app_id"]?.jsonPrimitive?.content == facebookId &&
                    (data["expires_at"]?.jsonPrimitive?.longOrNull ?: 0) > now &&
                    (data["data_access_expires_at"]?.jsonPrimitive?.longOrNull ?: 0) > now) { "Facebook sign-in expired. Please try again." }
                val mac = Mac.getInstance("HmacSHA256")
                mac.init(SecretKeySpec(facebookSecret.toByteArray(), "HmacSHA256"))
                val proof = mac.doFinal(credential.token.toByteArray()).joinToString("") { "%02x".format(it) }
                val response = client.get("https://graph.facebook.com/$version/me") {
                    bearerAuth(credential.token)
                    parameter("fields", "id,name,email")
                    parameter("appsecret_proof", proof)
                }
                require(response.status.isSuccess()) { "Could not read Facebook profile" }
                val profile = response.body<JsonObject>()
                val subject = profile["id"]?.jsonPrimitive?.content.orEmpty()
                require(subject.isNotBlank() && subject == data["user_id"]?.jsonPrimitive?.content) { "Invalid Facebook identity" }
                val email = profile["email"]?.jsonPrimitive?.content.orEmpty()
                require(email.isNotBlank()) { "Allow Facebook to share your email, or sign up with email instead." }
                SocialProfile("facebook", subject, profile["name"]?.jsonPrimitive?.content.orEmpty(), email.lowercase())
            }
            else -> throw IllegalArgumentException("Unsupported sign-in provider")
        }
    }
}

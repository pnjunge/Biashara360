package com.app.biashara.ui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Toolkit
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import java.util.prefs.Preferences

internal object WorkstationAlertPreferences {
    @Volatile var connectionActive = false
    private val prefs = Preferences.userRoot().node("com/app/biashara/workstation-alerts")
    var sound: Boolean get() = prefs.getBoolean("sound", true); set(value) { prefs.putBoolean("sound", value); prefs.flush() }
    var speech: String get() = prefs.get("speech", "New {type}"); set(value) { prefs.put("speech",value.take(200)); prefs.flush() }
    var token: String get() = prefs.get("key", ""); set(value) { prefs.put("key",value); prefs.flush() }
    var endpoint: String get() = prefs.get("endpoint", ""); set(value) { prefs.put("endpoint",value); prefs.flush() }
    var merchant: String get() = prefs.get("merchant", ""); set(value) { prefs.put("merchant",value); prefs.flush() }
    var seen: Set<String> get() = prefs.get("seen", "").split(',').filter { it.isNotBlank() }.toSet(); set(value) { prefs.put("seen",value.takeLastBounded().joinToString(",")); prefs.flush() }
    private fun Set<String>.takeLastBounded() = toList().takeLast(100)
    fun unlink() { connectionActive=false; token=""; merchant=""; endpoint=""; seen=emptySet() }
}
internal object DesktopAlertSound {
    private fun executable(name: String): String? = System.getenv("PATH").orEmpty().split(java.io.File.pathSeparator).map { Path.of(it, name) }.firstOrNull { Files.isExecutable(it) }?.toString()
    suspend fun play(count: Int = 1, order: String = "", type: String = "online order") = withContext(Dispatchers.IO) {
        if (!WorkstationAlertPreferences.sound) return@withContext null
        runCatching { Toolkit.getDefaultToolkit().beep() }
        val text = WorkstationAlertPreferences.speech.replace("{type}",type).replace("{count}",count.toString()).replace("{order}",order).take(240).trim()
        if (text.isEmpty()) return@withContext null
        val os = System.getProperty("os.name").lowercase()
        val command = when {
            os.contains("win") -> listOf("powershell.exe","-NoProfile","-NonInteractive","-Command","Add-Type -AssemblyName System.Speech; (New-Object System.Speech.Synthesis.SpeechSynthesizer).Speak(\$env:B360_ALERT_TEXT)")
            os.contains("mac") -> listOf("/usr/bin/say", "--", text)
            executable("espeak-ng") != null -> listOf(executable("espeak-ng")!!,"--stdin")
            executable("espeak") != null -> listOf(executable("espeak")!!,"--stdin")
            executable("spd-say") != null -> listOf(executable("spd-say")!!,"--wait","--",text)
            else -> return@withContext "Spoken alerts need a system speech provider (espeak-ng or Speech Dispatcher on Linux). The alert tone is still enabled."
        }
        runCatching {
            val builder = ProcessBuilder(command).redirectOutput(ProcessBuilder.Redirect.DISCARD).redirectError(ProcessBuilder.Redirect.DISCARD)
            builder.environment()["B360_ALERT_TEXT"] = text
            val process = builder.start()
            process.outputStream.use { if (command.last() == "--stdin") it.write(text.toByteArray()) }
            if (!process.waitFor(15, TimeUnit.SECONDS)) { process.destroyForcibly(); error("Speech provider timed out") }
            check(process.exitValue() == 0) { "System speech provider could not play the alert text" }
            null
        }.getOrElse { it.message ?: "Could not play spoken alert" }
    }
}

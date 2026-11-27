package com.nexus.ai

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import java.net.URLEncoder

object AgentCommand {
    fun run(context: Context, raw: String): String {
        val command = raw.trim()
        val lower = command.lowercase()

        if (lower.startsWith("open ")) {
            val target = command.substringAfter(" ", "").trim()
            val pkg = appPackage(target)
            if (target.equals("settings", true)) {
                context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return "Opening Android Settings."
            }
            if (pkg != null) {
                val launch = context.packageManager.getLaunchIntentForPackage(pkg)
                if (launch != null) {
                    launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launch)
                    return "Opening $target."
                }
            }
            return "I couldn't find $target as an installed app. Try its exact app name."
        }

        if (lower.startsWith("search ")) {
            val query = command.substringAfter("search ").trim()
            if (query.isBlank()) return "Tell me what to search for."
            val url = "https://www.google.com/search?q=" + URLEncoder.encode(query, "UTF-8")
            context.startActivity(Intent(context, BrowserActivity::class.java).putExtra("url", url))
            return "Searching the public web for: $query"
        }

        if (lower.startsWith("browse ")) {
            var url = command.substringAfter("browse ").trim()
            if (!url.startsWith("https://") && !url.startsWith("http://")) url = "https://$url"
            context.startActivity(Intent(context, BrowserActivity::class.java).putExtra("url", url))
            return "Opening website: $url"
        }

        if (lower.startsWith("sms ")) {
            val rest = command.substringAfter("sms ").trim()
            val number = rest.substringBefore(" ").trim()
            val body = rest.substringAfter(" ", "").trim()
            if (number.isBlank() || body.isBlank()) return "Use: sms +911234567890 Your message"
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${Uri.encode(number)}"))
                .putExtra("sms_body", body)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            return "SMS draft opened. Review the recipient and message, then press Send yourself."
        }

        if (lower.startsWith("type ")) {
            val text = command.substringAfter("type ").trim()
            if (text.isBlank()) return "Tell me what text to insert."
            NexusAccessibilityService.pendingText = text
            return "Text queued. Focus the target text field in the other app. ULTRON will insert it when Android reports the focused screen. It will not press Send."
        }

        return "I can run local commands such as: open WhatsApp, open YouTube, search Android news, browse example.com, sms +911234567890 Hello, or type Hello. Other requests are sent only to your local AI server."
    }

    private fun appPackage(name: String): String? = when (name.lowercase()) {
        "whatsapp" -> "com.whatsapp"
        "whatsapp business" -> "com.whatsapp.w4b"
        "youtube" -> "com.google.android.youtube"
        "chrome" -> "com.android.chrome"
        "gmail" -> "com.google.android.gm"
        "telegram" -> "org.telegram.messenger"
        "instagram" -> "com.instagram.android"
        "facebook" -> "com.facebook.katana"
        "camera" -> "com.android.camera"
        "phone", "dialer" -> "com.google.android.dialer"
        "messages", "sms" -> "com.google.android.apps.messaging"
        else -> null
    }
}

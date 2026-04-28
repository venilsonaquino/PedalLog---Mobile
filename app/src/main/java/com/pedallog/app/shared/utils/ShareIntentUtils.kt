package com.pedallog.app.shared.utils

import android.content.Context
import android.content.Intent
import android.net.Uri

object ShareIntentUtils {

    fun shareImage(context: Context, uri: Uri, packageName: String? = null) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            packageName?.let { setPackage(it) }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartilhar com"))
    }

    fun shareToWhatsApp(context: Context, uri: Uri) = shareImage(context, uri, "com.whatsapp")
    
    fun shareToInstagram(context: Context, uri: Uri) = shareImage(context, uri, "com.instagram.android")
}

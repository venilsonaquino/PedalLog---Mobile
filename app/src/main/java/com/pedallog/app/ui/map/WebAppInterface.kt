package com.pedallog.app.ui.map

import android.content.Context
import android.util.Log
import android.webkit.JavascriptInterface
import android.widget.Toast

class WebAppInterface(private val context: Context) {

    @JavascriptInterface
    fun onMapReady() {
        Log.d("WebAppInterface", "Map is ready and track is drawn.")
    }

    @JavascriptInterface
    fun onError(error: String) {
        Log.e("WebAppInterface", "Error from map: $error")
        Toast.makeText(context, "Map Error: $error", Toast.LENGTH_SHORT).show()
    }
}

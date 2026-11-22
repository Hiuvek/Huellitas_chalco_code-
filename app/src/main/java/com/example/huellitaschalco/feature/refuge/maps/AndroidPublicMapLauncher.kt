package com.example.huellitaschalco.feature.refuge.maps

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.huellitaschalco.feature.refuge.domain.PublicMapLauncher

class AndroidPublicMapLauncher(private val context: Context) : PublicMapLauncher {
    override fun open(url: String) {
        val uri = Uri.parse(url)
        require(
            uri.scheme == "https" &&
                uri.host == "www.google.com" &&
                uri.path == "/maps/search/" &&
                uri.getQueryParameter("api") == "1"
        )
        context.startActivity(
            Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

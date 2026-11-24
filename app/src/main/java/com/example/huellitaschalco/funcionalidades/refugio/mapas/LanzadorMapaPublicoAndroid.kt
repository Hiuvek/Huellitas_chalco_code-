package com.example.huellitaschalco.funcionalidades.refugio.mapas

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.huellitaschalco.funcionalidades.refugio.dominio.LanzadorMapaPublico

class LanzadorMapaPublicoAndroid(private val context: Context) : LanzadorMapaPublico {
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

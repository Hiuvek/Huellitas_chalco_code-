package com.example.huellitaschalco.aplicacion

import android.app.Application

class InicioHuellitas : Application() {
    val container by lazy { ContenedorAplicacion(this) }
}

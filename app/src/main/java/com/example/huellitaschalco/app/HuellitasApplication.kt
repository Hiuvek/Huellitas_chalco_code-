package com.example.huellitaschalco.app

import android.app.Application

class HuellitasApplication : Application() {
    val container by lazy { AppContainer(this) }
}

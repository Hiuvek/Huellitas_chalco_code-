package com.example.huellitaschalco

import android.content.pm.ApplicationInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.huellitaschalco.aplicacion.AplicacionHuellitas
import com.example.huellitaschalco.aplicacion.InicioHuellitas
import com.example.huellitaschalco.ui.theme.HuellitasChalcoTheme
import com.example.huellitaschalco.nucleo.navegacion.DestinoInterfaz
import com.example.huellitaschalco.nucleo.navegacion.PantallaInterfaz
import com.example.huellitaschalco.nucleo.navegacion.VarianteInterfaz

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle =
                SystemBarStyle.light(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.TRANSPARENT,
                )
        )
        val preview =
            if (
                BuildConfig.SHOW_PREVIEW_GALLERY &&
                    applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
            ) {
                intent.getStringExtra("preview_screen")?.let { name ->
                    runCatching {
                            DestinoInterfaz(
                                screen = PantallaInterfaz.valueOf(name),
                                dogId =
                                    intent.getIntExtra("preview_dog_id", 6).takeIf { it in 1..8 }
                                        ?: 6,
                                requestId =
                                    intent.getIntExtra("preview_request_id", 1).takeIf {
                                        it in 1..8
                                    } ?: 1,
                                variant =
                                    intent
                                        .getStringExtra("preview_variant")
                                        ?.let(VarianteInterfaz::valueOf) ?: VarianteInterfaz.Normal,
                            )
                        }
                        .getOrNull()
                }
            } else null
        val container = (application as InicioHuellitas).container
        val authRepository = container.authRepository
        setContent {
            HuellitasChalcoTheme {
                AplicacionHuellitas(authRepository, container.shelterAccessRepository, preview)
            }
        }
    }
}

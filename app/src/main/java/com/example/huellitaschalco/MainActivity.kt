package com.example.huellitaschalco

import android.content.pm.ApplicationInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.huellitaschalco.app.HuellitasApp
import com.example.huellitaschalco.app.HuellitasApplication
import com.example.huellitaschalco.core.navigation.UiDestination
import com.example.huellitaschalco.core.navigation.UiScreen
import com.example.huellitaschalco.core.navigation.UiVariant
import com.example.huellitaschalco.ui.theme.HuellitasChalcoTheme

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
                            UiDestination(
                                screen = UiScreen.valueOf(name),
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
                                        ?.let(UiVariant::valueOf) ?: UiVariant.Normal,
                            )
                        }
                        .getOrNull()
                }
            } else null
        val container = (application as HuellitasApplication).container
        val authRepository = container.authRepository
        setContent {
            HuellitasChalcoTheme {
                HuellitasApp(authRepository, container.shelterAccessRepository, preview)
            }
        }
    }
}

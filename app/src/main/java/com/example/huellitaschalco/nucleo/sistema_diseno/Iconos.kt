package com.example.huellitaschalco.nucleo.sistema_diseno

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

enum class SimboloHuellitas(val path: String) {
    Paw(
        "M8 14c-2 2-3 6 0 7 2 1 2-1 4-1s2 2 4 1c3-1 2-5 0-7-2-3-6-3-8 0ZM5 6a2 3 0 1 0 0 6 2 3 0 1 0 0-6ZM10 2a2 3 0 1 0 0 6 2 3 0 1 0 0-6ZM16 3a2 3 0 1 0 0 6 2 3 0 1 0 0-6ZM20 9a2 2 0 1 0 0 4 2 2 0 1 0 0-4Z"
    ),
    Back("M19 12H5m7-7-7 7 7 7"),
    Forward("M5 12h14m-7-7 7 7-7 7"),
    Chevron("m9 5 7 7-7 7"),
    Check("m5 12 4 4L19 6"),
    Close("m6 6 12 12M6 18 18 6"),
    Home("m3 10 9-7 9 7v10H3ZM9 20v-7h6v7"),
    User("M8 7a4 4 0 1 0 8 0 4 4 0 1 0-8 0ZM4 21v-2a8 8 0 0 1 16 0v2"),
    Document("M6 3h9l4 4v14H6ZM14 3v5h5M9 12h7m-7 4h7"),
    Calendar("M4 5h16v16H4ZM8 2v6m8-6v6M4 10h16m-12 4h2m4 0h2m-8 4h2"),
    Search("M4 10a6 6 0 1 0 12 0 6 6 0 1 0-12 0Zm11 5 6 6"),
    Filter("M4 6h16M4 12h16M4 18h16M8 3v6m8 0v6M10 15v6"),
    Pin("M12 22s7-7 7-13A7 7 0 1 0 5 9c0 6 7 13 7 13ZM9 9a3 3 0 1 0 6 0 3 3 0 1 0-6 0Z"),
    Clock("M3 12a9 9 0 1 0 18 0 9 9 0 1 0-18 0Zm9-5v6l4 2"),
    Heart("M12 21 3 12C-3 4 7-1 12 6c5-7 15-2 9 6Z"),
    Info("M3 12a9 9 0 1 0 18 0 9 9 0 1 0-18 0Zm9-1v6m0-10v1"),
    Alert("m12 3 10 18H2Zm0 5v6m0 3v1"),
    Shield("m12 2 8 3v7c0 6-8 10-8 10S4 18 4 12V5Zm-4 9 3 3 5-6"),
    Camera("M3 7h5l2-3h4l2 3h5v14H3Zm5 7a4 4 0 1 0 8 0 4 4 0 1 0-8 0Z"),
    Image("M3 3h18v18H3Zm0 14 6-6 5 5 3-3 4 4M7 7h1"),
    Plus("M12 4v16M4 12h16"),
    Edit("m4 16 12-12 4 4L8 20H4Zm10-10 4 4"),
    Eye("M2 12s4-7 10-7 10 7 10 7-4 7-10 7S2 12 2 12Zm7 0a3 3 0 1 0 6 0 3 3 0 1 0-6 0Z"),
    Inbox("M4 4h16l2 12v5H2v-5Zm-2 12h6l2 3h4l2-3h6"),
    Send("m22 2-7 20-4-9-9-4Zm0 0L11 13"),
    Save("M4 3h13l4 4v14H3V3Zm3 0v6h9V3M7 21v-8h10v8"),
    Logout("M9 3H3v18h6m5-16 7 7-7 7m-5-7h12"),
    Refresh("M20 8a8 8 0 0 0-14-3L3 8m0-5v5h5m-4 8a8 8 0 0 0 14 3l3-3m0 5v-5h-5"),
    WifiOff(
        "m2 2 20 20M2 8a16 16 0 0 1 4-2m4-1a16 16 0 0 1 12 3M5 12a11 11 0 0 1 5-2m5 0a11 11 0 0 1 4 2m-11 4a6 6 0 0 1 7 0m-3 4h1"
    ),
    Sparkles("m12 2 3 7 7 3-7 3-3 7-3-7-7-3 7-3ZM3 2v4M1 4h4m15 14v4m-2-2h4"),
    Phone("M6 2h4l1 6-3 2a14 14 0 0 0 6 6l2-3 6 1v4c0 7-20-1-20-12Z"),
    Compass("M3 12a9 9 0 1 0 18 0 9 9 0 1 0-18 0Zm5 4 3-7 5-1-3 7Z"),
}

@Composable
fun Iconos(
    symbol: SimboloHuellitas,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
    description: String? = null,
) {
    val path = remember(symbol) { PathParser().parsePathString(symbol.path).toPath() }
    Canvas(
        modifier.size(22.dp).semantics { if (description != null) contentDescription = description }
    ) {
        withTransform({
            scale(
                size.width / 24f,
                size.height / 24f,
                pivot = androidx.compose.ui.geometry.Offset.Zero,
            )
        }) {
            drawPath(
                path,
                tint,
                style = Stroke(1.7f, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}

package com.example.huellitaschalco.funcionalidades.administracion_refugio.seguimiento

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.huellitaschalco.nucleo.navegacion.*
import com.example.huellitaschalco.nucleo.sistema_diseno.*
import com.example.huellitaschalco.nucleo.vista_previa.*

@Composable
fun FollowUpsScreen(nav: NavegadorInterfaz, destination: DestinoInterfaz) {
    var tab by rememberSaveable {
        mutableStateOf(if (destination.variant == VarianteInterfaz.History) "Historial" else "Programados")
    }
    val after = destination.variant in listOf(VarianteInterfaz.AfterFollowUp, VarianteInterfaz.History)
    HScaffold(
        nav,
        "Seguimientos",
        "Acompaña la adaptación en casa",
        back = false,
        shelter = true,
        tab = "Seguimientos",
    ) {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            listOf("Programados", "Adopciones", "Historial").forEachIndexed { i, name ->
                SegmentedButton(
                    selected = tab == name,
                    onClick = { tab = name },
                    shape = SegmentedButtonDefaults.itemShape(i, 3),
                    label = { Text(name, style = MaterialTheme.typography.labelSmall) },
                )
            }
        }
        when (tab) {
            "Programados" -> {
                if (destination.variant == VarianteInterfaz.AfterAdoption)
                    HCard {
                        val request = DatosVistaPrevia.request(destination.requestId)
                        HDogRow(
                            DatosVistaPrevia.dog(destination.dogId),
                            "Con ${request.user}",
                            "Programado",
                        )
                        HBanner("30 nov 2026 · Visita a domicilio", SimboloHuellitas.Calendar)
                        HCaption("Primer seguimiento después de la entrega del 16 nov 2026.")
                    }
                HCard {
                    HDogRow(
                        DatosVistaPrevia.dog(7),
                        "Con Usuario 6",
                        if (after) "Programado" else "Esta semana",
                    )
                    HBanner(
                        if (after) "16 dic 2026 · Llamada" else "20 nov 2026 · Visita a domicilio",
                        SimboloHuellitas.Calendar,
                    )
                    HCaption(
                        if (after)
                            "Último seguimiento realizado el 16 nov 2026 (programado para el 20 nov)."
                        else "Último seguimiento realizado el 18 oct 2026."
                    )
                    HButton(
                        "Registrar seguimiento",
                        { nav.open(PantallaInterfaz.FollowUpForm, dogId = 7) },
                        outlined = true,
                        icon = SimboloHuellitas.Plus,
                    )
                }
            }
            "Adopciones" -> {
                HDogRow(
                    DatosVistaPrevia.dog(7),
                    "Con Usuario 6 · Entrega 4 oct 2026",
                    "Adoptado",
                    {
                        nav.open(
                            PantallaInterfaz.FollowUpDetail,
                            dogId = 7,
                            variant = VarianteInterfaz.OldFollowUp,
                        )
                    },
                )
                HButton(
                    "Programar revisión",
                    { nav.open(PantallaInterfaz.FollowUpForm, dogId = 7) },
                    outlined = true,
                    icon = SimboloHuellitas.Calendar,
                )
            }
            else -> {
                if (after)
                    FollowUpHistoryCard("16 nov 2026", "Muy buena", DatosVistaPrevia.followUpNotes) {
                        nav.open(PantallaInterfaz.FollowUpDetail, dogId = 7)
                    }
                FollowUpHistoryCard("18 oct 2026", "Buena", DatosVistaPrevia.oldFollowUpNotes) {
                    nav.open(PantallaInterfaz.FollowUpDetail, dogId = 7, variant = VarianteInterfaz.OldFollowUp)
                }
            }
        }
    }
}

@Composable
private fun FollowUpHistoryCard(
    date: String,
    adaptation: String,
    notes: String,
    onOpen: () -> Unit,
) {
    HCard(onClick = onOpen) {
        HKeyValue("Perrito 7", date)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HTag(adaptation)
            HTag("Realizado")
        }
        HBody(notes)
        HTextAction("Ver detalle", onOpen, icon = SimboloHuellitas.Chevron)
    }
}

@Composable
fun FollowUpFormScreen(nav: NavegadorInterfaz) {
    var schedule by rememberSaveable { mutableStateOf(true) }
    var photo by rememberSaveable { mutableStateOf(false) }
    HScaffold(
        nav,
        "Registrar seguimiento",
        "Perrito 7 · Usuario 6",
        bottom = {
            HButton(
                "Guardar seguimiento",
                { nav.open(PantallaInterfaz.FollowUpSaved, dogId = 7) },
                icon = SimboloHuellitas.Save,
            )
        },
    ) {
        HBanner(
            "Al guardar, la revisión programada del 20 nov 2026 (Visita a domicilio) quedará como realizada.",
            SimboloHuellitas.Calendar,
        )
        HInput("Fecha del seguimiento", "16/11/2026", helper = "Formato DD/MM/AAAA.")
        HChoices("Adaptación al hogar", listOf("Muy buena", "Buena", "Regular", "Con dificultades"))
        HInput(
            "Observaciones",
            DatosVistaPrevia.followUpNotes,
            helper = "Alimentación, convivencia, salud y comportamiento. Mínimo 15 caracteres.",
            lines = 6,
        )
        HHeading("Fotografía (opcional)")
        if (photo) {
            HPhoto(DatosVistaPrevia.dog(7), Modifier.fillMaxWidth().height(180.dp))
            HTextAction("Quitar fotografía", { photo = false }, danger = true)
        } else
            HButton("Agregar fotografía", { photo = true }, outlined = true, icon = SimboloHuellitas.Camera)
        HHeading("Próxima revisión")
        HChoices(
            "¿Programar otra revisión?",
            listOf("Sí, programar", "No, cerrar seguimiento"),
            onChange = { schedule = it == "Sí, programar" },
        )
        if (schedule) {
            HInput("Fecha", "16/12/2026", helper = "Formato DD/MM/AAAA.")
            HChoices("Tipo", listOf("Visita a domicilio", "Llamada", "Videollamada"), "Llamada")
        }
    }
}

@Composable
fun FollowUpSavedScreen(nav: NavegadorInterfaz) {
    HScaffold(
        nav,
        "",
        back = false,
        bottom = {
            HButton("Ver detalle del seguimiento", { nav.open(PantallaInterfaz.FollowUpDetail, dogId = 7) })
            HTextAction(
                "Ir al historial",
                { nav.open(PantallaInterfaz.FollowUps, variant = VarianteInterfaz.History) },
            )
        },
    ) {
        HSuccessHeader("Seguimiento guardado", "Ya aparece en el historial de Perrito 7.")
        HBanner(
            "Revisión programada para el 20 nov 2026 marcada como realizada. Fecha real de realización: 16 nov 2026.",
            SimboloHuellitas.Calendar,
        )
        HBanner(
            "Próxima revisión: 16 dic 2026 · Llamada. Aparece en Programados.",
            SimboloHuellitas.Calendar,
        )
        FollowUpResultCard(false)
    }
}

@Composable
fun FollowUpDetailScreen(nav: NavegadorInterfaz, destination: DestinoInterfaz) {
    val old = destination.variant == VarianteInterfaz.OldFollowUp
    HScaffold(
        nav,
        "Detalle de seguimiento",
        "Perrito 7 · ${if (old) "18 oct 2026" else "16 nov 2026"}",
        bottom = {
            HButton(
                "Ver revisiones programadas",
                { nav.open(PantallaInterfaz.FollowUps, variant = VarianteInterfaz.AfterFollowUp) },
                outlined = true,
                icon = SimboloHuellitas.Calendar,
            )
        },
    ) {
        FollowUpResultCard(old)
        HBanner(
            if (old)
                "Revisión del 18 oct 2026 (Llamada) marcada como realizada en la fecha programada."
            else
                "Revisión programada para el 20 nov 2026 (Visita a domicilio) marcada como realizada. Fecha real de realización: 16 nov 2026.",
            SimboloHuellitas.Calendar,
        )
        HBanner(
            if (old) "La siguiente revisión del 20 nov ya se realizó el 16 nov 2026."
            else "Próxima revisión: 16 dic 2026 · Llamada.",
            SimboloHuellitas.Calendar,
        )
        HHeading("Adopción")
        HCard {
            HKeyValue("Adoptante", "Usuario 6")
            HKeyValue("Fecha de entrega", "4 oct 2026", true)
            HKeyValue("Folio de solicitud", "HC-2026-0128", true)
            HKeyValue("Seguimientos registrados", "2", true)
        }
    }
}

@Composable
private fun FollowUpResultCard(old: Boolean) {
    HCard {
        HDogRow(DatosVistaPrevia.dog(7), "Con Usuario 6", "Realizado")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HMetric("Realizado el", if (old) "18 oct 2026" else "16 nov 2026", Modifier.weight(1f))
            HMetric("Adaptación", if (old) "Buena" else "Muy buena", Modifier.weight(1f))
        }
        HHeading("Observaciones")
        HBody(if (old) DatosVistaPrevia.oldFollowUpNotes else DatosVistaPrevia.followUpNotes)
        HPhoto(DatosVistaPrevia.dog(7), Modifier.fillMaxWidth().height(185.dp))
    }
}

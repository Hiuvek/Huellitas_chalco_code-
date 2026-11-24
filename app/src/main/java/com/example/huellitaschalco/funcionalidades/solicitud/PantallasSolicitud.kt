package com.example.huellitaschalco.funcionalidades.solicitud

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.huellitaschalco.ui.theme.*
import com.example.huellitaschalco.nucleo.navegacion.*
import com.example.huellitaschalco.nucleo.sistema_diseno.*
import com.example.huellitaschalco.nucleo.vista_previa.*

private val steps =
    listOf(
        PantallaInterfaz.ApplicationContact,
        PantallaInterfaz.ApplicationHome,
        PantallaInterfaz.ApplicationPets,
        PantallaInterfaz.ApplicationReasons,
        PantallaInterfaz.ApplicationReview,
    )
private val labels = listOf("Contacto", "Tu hogar", "Otras mascotas", "Motivos", "Revisar")

@Composable
fun ApplicationScreen(nav: NavegadorInterfaz, destination: DestinoInterfaz) {
    val step = steps.indexOf(destination.screen)
    val dog = DatosVistaPrevia.dog(destination.dogId)
    val fieldError = destination.variant == VarianteInterfaz.FieldError
    HScaffold(
        nav,
        "Solicitud para ${dog.name}",
        "Paso ${step + 1} de 5 · ${labels[step]}",
        bottom = {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (step > 0) HButton("Atrás", nav::back, Modifier.weight(.8f), outlined = true)
                HButton(
                    if (step == 4) "Enviar solicitud" else "Continuar",
                    {
                        if (step == 4) nav.open(PantallaInterfaz.ApplicationSent)
                        else nav.open(steps[step + 1])
                    },
                    Modifier.weight(1.3f),
                    icon = if (step == 4) SimboloHuellitas.Send else null,
                )
            }
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            LinearProgressIndicator(
                progress = { (step + 1) / 5f },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = Forest,
                trackColor = Line,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Iconos(SimboloHuellitas.Save, Modifier.size(15.dp))
                Text(
                    "Borrador guardado en este teléfono",
                    style = MaterialTheme.typography.labelMedium,
                    color = Forest,
                )
            }
        }
        when (step) {
            0 -> {
                HBody("El refugio usará estos datos para contactarte.", muted = true)
                HInput("Nombre completo", "Usuario 1")
                HInput(
                    "Teléfono celular",
                    if (fieldError) "" else "5500000001",
                    helper = "10 dígitos, sin espacios.",
                    keyboard = KeyboardType.Phone,
                    error = if (fieldError) "El teléfono debe tener 10 dígitos." else null,
                )
                HInput("Correo electrónico", "ana@correo.invalid", keyboard = KeyboardType.Email)
                if (fieldError)
                    HBanner("Completa los campos obligatorios marcados.", SimboloHuellitas.Alert, true)
            }
            1 -> {
                HChoices("Tipo de vivienda", listOf("Casa", "Departamento", "Otro"))
                HChoices("La vivienda es…", listOf("Propia", "Rentada", "Familiar"))
                HChoices("¿Tienes patio o espacio exterior?", listOf("Sí", "No"))
                HInput("¿Cuántas personas viven en tu hogar?", "3", keyboard = KeyboardType.Number)
                HChoices(
                    "¿Cuántas horas al día pasaría solo?",
                    listOf("Menos de 4 h", "De 4 a 8 h", "Más de 8 h"),
                    "De 4 a 8 h",
                )
            }
            2 -> {
                HChoices("¿Tienes otras mascotas?", listOf("Sí", "No"), "No")
                HInput(
                    "Cuéntanos sobre ellas (opcional)",
                    placeholder = "Especie, edad y cómo conviven…",
                    lines = 3,
                )
                HBanner(
                    "Si tienes otras mascotas, el refugio puede sugerir una presentación antes de la entrega."
                )
            }
            3 ->
                HInput(
                    "¿Por qué quieres adoptar a ${dog.name}?",
                    DatosVistaPrevia.reasons.replace("Perrito 6", dog.name),
                    helper = "Mínimo 30 caracteres.",
                    lines = 6,
                )
            4 -> {
                HBody(
                    "Revisa tus respuestas antes de enviar. Puedes editar cualquier sección.",
                    muted = true,
                )
                HDogRow(dog, "Solicitud para")
                ReviewSection("Contacto", { nav.open(PantallaInterfaz.ApplicationContact) }) {
                    HKeyValue("Nombre", "Usuario 1")
                    HKeyValue("Teléfono", "5500000001", true)
                    HKeyValue("Correo", "ana@correo.invalid", true)
                }
                ReviewSection("Tu hogar", { nav.open(PantallaInterfaz.ApplicationHome) }) {
                    HKeyValue("Vivienda", "Casa")
                    HKeyValue("Es", "Propia", true)
                    HKeyValue("Patio", "Sí", true)
                    HKeyValue("Personas", "3", true)
                    HKeyValue("Horas solo", "De 4 a 8 h", true)
                }
                ReviewSection("Otras mascotas", { nav.open(PantallaInterfaz.ApplicationPets) }) {
                    HKeyValue("¿Tiene?", "No")
                }
                ReviewSection("Motivos", { nav.open(PantallaInterfaz.ApplicationReasons) }) {
                    HBody(DatosVistaPrevia.reasons.replace("Perrito 6", dog.name))
                }
                if (destination.variant == VarianteInterfaz.Error)
                    HBanner(
                        "No pudimos enviar tu solicitud. Hubo un problema de conexión. Tus respuestas siguen guardadas en este teléfono; un reintento no duplicará la solicitud.",
                        SimboloHuellitas.Alert,
                        true,
                    ) {
                        HButton(
                            "Reintentar envío",
                            { nav.open(PantallaInterfaz.ApplicationSent) },
                            icon = SimboloHuellitas.Refresh,
                        )
                    }
            }
        }
    }
}

@Composable
private fun ReviewSection(
    title: String,
    onEdit: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    HCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            TextButton(onClick = onEdit) {
                Iconos(SimboloHuellitas.Edit, Modifier.size(15.dp))
                Spacer(Modifier.width(5.dp))
                Text("Editar")
            }
        }
        content()
    }
}

@Composable
fun ApplicationSentScreen(nav: NavegadorInterfaz) {
    val dog = DatosVistaPrevia.dog(nav.current.dogId)
    HScaffold(
        nav,
        "",
        back = false,
        bottom = {
            HButton("Ver mi solicitud", { nav.open(PantallaInterfaz.RequestDetail) })
            HTextAction("Seguir explorando", { nav.root(PantallaInterfaz.Catalog) })
        },
    ) {
        HSuccessHeader(
            "¡Solicitud enviada!",
            "Tu solicitud para conocer a ${dog.name} llegó a ${dog.refuge}.",
        )
        HDogRow(dog, "Folio HC-2026-0148", "Enviada")
        HBanner("Siguiente paso: el refugio revisará tus respuestas en los próximos días.")
    }
}

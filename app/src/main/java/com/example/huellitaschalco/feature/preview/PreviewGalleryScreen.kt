package com.example.huellitaschalco.feature.preview

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.huellitaschalco.core.designsystem.*
import com.example.huellitaschalco.core.navigation.*

private data class PreviewEntry(val title: String, val destination: UiDestination)

private fun entry(
    title: String,
    screen: UiScreen,
    variant: UiVariant = UiVariant.Normal,
    dog: Int = 6,
    request: Int = 1,
) = PreviewEntry(title, UiDestination(screen, dog, request, variant))

/** Muestrario para revisar cada variante de los mockups sin conectar servicios. */
@Composable
fun PreviewGalleryScreen(nav: UiNavigator) {
    val groups =
        listOf(
            "Acceso" to
                listOf(
                    entry("Bienvenida", UiScreen.Welcome),
                    entry("Crear cuenta", UiScreen.SignUp),
                    entry("Iniciar sesión", UiScreen.Login),
                    entry("Error de acceso", UiScreen.Login, UiVariant.Error),
                    entry("Acceso para refugios", UiScreen.ShelterLogin),
                    entry("Acceso restringido", UiScreen.AccessRestricted),
                ),
            "Adoptante · catálogo" to
                listOf(
                    entry("Catálogo", UiScreen.Catalog),
                    entry("Catálogo sin conexión con datos", UiScreen.Catalog, UiVariant.Offline),
                    entry("Sin conexión ni datos guardados", UiScreen.Catalog, UiVariant.NoCache),
                    entry("Detalle de Perrito 6", UiScreen.DogDetail),
                    entry("Mapa que no carga", UiScreen.DogDetail, UiVariant.MapError),
                    entry("Requisitos de adopción", UiScreen.Requirements),
                    entry("Información del refugio", UiScreen.RefugeInformation),
                    entry(
                        "Catálogo después de una adopción",
                        UiScreen.Catalog,
                        UiVariant.AfterAdoption,
                    ),
                ),
            "Adoptante · solicitud" to
                listOf(
                    entry("1. Contacto", UiScreen.ApplicationContact),
                    entry("2. Tu hogar", UiScreen.ApplicationHome),
                    entry("3. Otras mascotas", UiScreen.ApplicationPets),
                    entry("4. Motivos", UiScreen.ApplicationReasons),
                    entry("5. Revisar", UiScreen.ApplicationReview),
                    entry(
                        "Campo obligatorio vacío",
                        UiScreen.ApplicationContact,
                        UiVariant.FieldError,
                    ),
                    entry("Envío fallido", UiScreen.ApplicationReview, UiVariant.Error),
                    entry("Solicitud enviada", UiScreen.ApplicationSent),
                    entry("Mis solicitudes", UiScreen.MyRequests),
                    entry("Sin solicitudes", UiScreen.MyRequests, UiVariant.Empty),
                    entry("Detalle de solicitud", UiScreen.RequestDetail),
                    entry("Solicitud en revisión", UiScreen.RequestDetail, UiVariant.InReview),
                    entry("Perfil del adoptante", UiScreen.AdopterProfile),
                ),
            "Refugio · perritos" to
                listOf(
                    entry("Mis perritos", UiScreen.ShelterDogs),
                    entry("Registro de Perrito 1", UiScreen.ShelterDogDetail, dog = 1),
                    entry("Registrar perrito", UiScreen.DogForm, dog = 8),
                    entry("Editar perrito", UiScreen.DogForm, UiVariant.Editing, dog = 1),
                    entry("Registro con fotografía", UiScreen.DogForm, UiVariant.Photos, dog = 8),
                    entry(
                        "Fotografía que no cargó",
                        UiScreen.DogForm,
                        UiVariant.PhotoError,
                        dog = 8,
                    ),
                    entry(
                        "Campos obligatorios vacíos",
                        UiScreen.DogForm,
                        UiVariant.FieldError,
                        dog = 8,
                    ),
                    entry(
                        "Ayuda de IA: sugerencia",
                        UiScreen.DogForm,
                        UiVariant.AiSuggestion,
                        dog = 8,
                    ),
                    entry("Ayuda de IA: error", UiScreen.DogForm, UiVariant.AiError, dog = 8),
                    entry("Vista previa antes de publicar", UiScreen.DogPreview, dog = 8),
                    entry("Perrito publicado", UiScreen.DogPublished, dog = 8),
                    entry("Perrito 8 en el listado", UiScreen.ShelterDogs, UiVariant.Photos),
                ),
            "Refugio · solicitudes y adopción" to
                listOf(
                    entry("Solicitudes recibidas", UiScreen.ShelterRequests),
                    entry("Sin solicitudes recibidas", UiScreen.ShelterRequests, UiVariant.Empty),
                    entry("Evaluar solicitud enviada", UiScreen.EvaluateRequest),
                    entry(
                        "Solicitud en revisión y decisión",
                        UiScreen.EvaluateRequest,
                        UiVariant.InReview,
                        dog = 4,
                        request = 7,
                    ),
                    entry(
                        "Solicitud aprobada",
                        UiScreen.EvaluateRequest,
                        UiVariant.Approved,
                        dog = 2,
                        request = 3,
                    ),
                    entry(
                        "Segunda aprobación bloqueada",
                        UiScreen.EvaluateRequest,
                        UiVariant.ApprovalBlocked,
                        dog = 2,
                        request = 8,
                    ),
                    entry("Registrar adopción", UiScreen.AdoptionForm, dog = 2, request = 3),
                    entry("Adopción registrada", UiScreen.AdoptionComplete, dog = 2, request = 3),
                    entry("Otra adopción bloqueada", UiScreen.AdoptionBlocked, dog = 2),
                    entry(
                        "Mis perritos después de adoptar",
                        UiScreen.ShelterDogs,
                        UiVariant.AfterAdoption,
                    ),
                    entry(
                        "Solicitudes después de adoptar",
                        UiScreen.ShelterRequests,
                        UiVariant.AfterAdoption,
                    ),
                    entry(
                        "Solicitud aprobada con entrega",
                        UiScreen.EvaluateRequest,
                        UiVariant.AfterAdoption,
                        dog = 2,
                        request = 3,
                    ),
                    entry(
                        "Solicitud restante no aprobada",
                        UiScreen.EvaluateRequest,
                        UiVariant.AfterAdoption,
                        dog = 2,
                        request = 8,
                    ),
                ),
            "Refugio · seguimientos" to
                listOf(
                    entry("Revisiones programadas", UiScreen.FollowUps, dog = 7),
                    entry("Registrar seguimiento", UiScreen.FollowUpForm, dog = 7),
                    entry("Seguimiento guardado", UiScreen.FollowUpSaved, dog = 7),
                    entry("Historial actualizado", UiScreen.FollowUps, UiVariant.History, dog = 7),
                    entry("Detalle del 16 de noviembre", UiScreen.FollowUpDetail, dog = 7),
                    entry(
                        "Detalle del 18 de octubre",
                        UiScreen.FollowUpDetail,
                        UiVariant.OldFollowUp,
                        dog = 7,
                    ),
                    entry(
                        "Programados actualizados",
                        UiScreen.FollowUps,
                        UiVariant.AfterFollowUp,
                        dog = 7,
                    ),
                    entry("Perfil del refugio", UiScreen.ShelterProfile),
                ),
        )
    HScaffold(
        nav,
        "Vistas de ejemplo",
        "${groups.sumOf { it.second.size }} pantallas y variantes",
    ) {
        HBanner(
            "Muestrario de interfaz. Los formularios, estados e imágenes son ejemplos; no se crean cuentas ni se guardan solicitudes o adopciones.",
            HSymbol.Eye,
        )
        groups.forEach { (name, entries) ->
            HHeading(name)
            entries.forEach { entry ->
                HCard(onClick = { nav.open(entry.destination) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            entry.title,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        HIcon(HSymbol.Chevron, Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

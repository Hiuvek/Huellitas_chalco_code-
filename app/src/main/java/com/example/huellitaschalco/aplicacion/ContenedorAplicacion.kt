package com.example.huellitaschalco.aplicacion

import android.content.Context
import com.example.huellitaschalco.funcionalidades.acceso.datos.RepositorioAccesoRefugioFirestore
import com.example.huellitaschalco.funcionalidades.administracion_refugio.perritos.datos.ImportadorFotoPerritoAndroid
import com.example.huellitaschalco.funcionalidades.administracion_refugio.perritos.datos.RepositorioPerritosFirebase
import com.example.huellitaschalco.funcionalidades.autenticacion.datos.RepositorioAutenticacionFirebase
import com.example.huellitaschalco.funcionalidades.autenticacion.dominio.RepositorioAutenticacion
import com.example.huellitaschalco.nucleo.firebase.ProveedorFirebase
import com.example.huellitaschalco.nucleo.local.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class ContenedorAplicacion(context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val firebase = ProveedorFirebase(context.applicationContext)
    val authRepository: RepositorioAutenticacion =
        RepositorioAutenticacionFirebase(firebase.auth, firebase.usesEmulator)
    val localDatabase = BaseDatosHuellitas.open(context)
    val draftStore: AlmacenBorradores = AlmacenBorradoresRoom(localDatabase.local())
    val publicCache = CachePublicaRoom(localDatabase.local(), context)
    private val remoteCatalog =
        com.example.huellitaschalco.funcionalidades.catalogo.datos.RepositorioCatalogoFirebase(
            firebase.firestore
        )
    val catalogRepository = RepositorioCatalogoConCache(remoteCatalog, publicCache)
    private val remotePublicShelter =
        com.example.huellitaschalco.funcionalidades.refugio.datos.RepositorioRefugioPublicoFirebase(
            firebase.firestore
        )
    val publicShelterRepository = RepositorioRefugioPublicoConCache(remotePublicShelter, publicCache)
    val adoptionApplicationRepository =
        com.example.huellitaschalco.funcionalidades.solicitud.datos.RepositorioSolicitudAdopcionFirebase(
            authRepository,
            firebase.firestore,
        )
    val adoptionRequestsRepository =
        com.example.huellitaschalco.funcionalidades.solicitudes.datos.RepositorioSolicitudesAdopcionFirebase(
            authRepository,
            firebase.firestore,
        )
    val evaluationRepository =
        com.example.huellitaschalco.funcionalidades.administracion_refugio.solicitudes.datos.RepositorioEvaluacionFirebase(
            authRepository,
            firebase.firestore,
        )
    val adoptionRepository =
        com.example.huellitaschalco.funcionalidades.administracion_refugio.adopcion.datos.RepositorioAdopcionFirebase(
            authRepository,
            firebase.firestore,
            firebase.functions,
        )
    val followUpRepository =
        com.example.huellitaschalco.funcionalidades.administracion_refugio.seguimiento.datos.RepositorioSeguimientoFirebase(
            authRepository,
            firebase.firestore,
            firebase.functions,
        )
    val descriptionGenerator by lazy {
        com.example.huellitaschalco.funcionalidades.administracion_refugio.descripcion.datos.GeneradorDescripcionFirebase(
            com.example.huellitaschalco.nucleo.firebase.ProveedorIAGemini(context.applicationContext)
        )
    }
    val dogPhotoImporter = ImportadorFotoPerritoAndroid(context.applicationContext)
    private val remoteDogs =
        RepositorioPerritosFirebase(
            authRepository,
            firebase.firestore,
            firebase.usesEmulator,
        )
    val dogRepository = RepositorioPerritosConCache(remoteDogs, publicCache)
    val shelterAccessRepository =
        RepositorioAccesoRefugioFirestore(authRepository, firebase.firestore, scope)
}

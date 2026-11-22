package com.example.huellitaschalco.core.local

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import com.example.huellitaschalco.app.HuellitasApplication
import com.example.huellitaschalco.feature.application.domain.*
import com.example.huellitaschalco.feature.application.presentation.ApplicationFormViewModel
import com.example.huellitaschalco.feature.auth.domain.AuthUser
import com.example.huellitaschalco.feature.catalog.domain.*
import com.example.huellitaschalco.feature.refuge.domain.*
import com.example.huellitaschalco.feature.shelter.dogs.domain.*
import com.example.huellitaschalco.feature.shelter.dogs.presentation.DogRegistrationViewModel
import java.io.File
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.*
import org.junit.Assert.*

class RoomPersistenceTest {
    private val context
        get() = ApplicationProvider.getApplicationContext<Context>()

    private val name = "hu17-${UUID.randomUUID()}.db"
    private lateinit var db: HuellitasDatabase
    private val stores = mutableListOf<ViewModelStore>()

    private fun keep(vm: androidx.lifecycle.ViewModel) {
        stores.add(ViewModelStore().apply { put("vm", vm) })
    }

    @Before
    fun setup() {
        db = HuellitasDatabase.open(context, name)
    }

    @After
    fun cleanup(): Unit = runBlocking {
        withContext(Dispatchers.Main) { stores.forEach { it.clear() } }
        db.close()
        context.deleteDatabase(name)
    }

    private val shelter =
        PublicShelter(
            "s",
            "Refugio de prueba",
            "Chalco",
            "Estado de México",
            "Zona pública aproximada",
            "10 a 16 h",
            true,
            AdoptionPolicy(listOf("Identificación"), listOf("Enviar solicitud")),
        )
    private val dog =
        RegisteredDog(
            "d",
            "s",
            "Luna",
            12,
            "Mediano",
            "Hembra",
            "Descripción suficientemente larga del perrito de prueba.",
            listOf(DogPhoto("p", "perritos/s/u/d/p.jpg", 0)),
            true,
            DogHealth(HealthStatus.Yes, HealthStatus.No, HealthStatus.Unknown),
        )
    private val catalog
        get() =
            PublicCatalog(false, listOf(CatalogDog(dog, shelter.name)), shelters = listOf(shelter))

    @Test
    fun schemaMatchesOriginalAndForeignKeysRollbackAnIncompleteReplacement(): Unit = runBlocking {
        val cache = RoomPublicCache(db.local(), context, "photos-$name")
        cache.save(catalog)
        val broken =
            CachedDogEntity(
                "other",
                "missing",
                "Otra",
                3,
                "Chico",
                "Macho",
                "Texto",
                "Sí",
                "No",
                "Sin información",
                "Disponible",
                "{}",
                1,
            )
        assertTrue(runCatching { db.local().replaceCatalog(emptyList(), listOf(broken)) }.isFailure)
        assertEquals("d", cache.snapshot()!!.dogs.single().dog.id)
        val tables =
            withContext(Dispatchers.IO) {
                db.openHelper.readableDatabase
                    .query("SELECT name FROM sqlite_master WHERE type='table'")
                    .use { c -> buildSet { while (c.moveToNext()) add(c.getString(0)) } }
            }
        assertTrue(tables.containsAll(listOf("cache_refugios", "cache_perritos", "borradores")))
    }

    @Test
    fun catalogAndPolicySurviveDatabaseReopenAndOfflineFiltersUseRealSavedFields(): Unit =
        runBlocking {
            RoomPublicCache(db.local(), context, "photos-$name").save(catalog)
            db.close()
            db = HuellitasDatabase.open(context, name)
            val cache = RoomPublicCache(db.local(), context, "photos-$name")
            val remote =
                object : CatalogRepository {
                    override fun observe() = flowOf(PublicCatalog(false, error = "Sin conexión"))
                }
            val result = CachedCatalogRepository(remote, cache).observe().toList().last()
            assertTrue(result.stale)
            assertTrue(result.cachedAt!! > 0)
            assertNull(result.error)
            assertEquals(dog, result.dogs.single().dog)
            assertEquals(shelter, result.shelters.single())
            assertTrue(
                CatalogFilters(query = "Luna", size = "Mediano", sex = "Hembra", age = "Adulto")
                    .matches(result.dogs.single())
            )
            assertFalse(CatalogFilters(sex = "Macho").matches(result.dogs.single()))
            cache.save(catalog.copy(dogs = emptyList()))
            assertTrue(cache.snapshot()!!.dogs.isEmpty())
        }

    @Test
    fun absentCacheHasExplicitErrorAndNeverInventsExampleDogs(): Unit = runBlocking {
        val remote =
            object : CatalogRepository {
                override fun observe() = flowOf(PublicCatalog(false, error = "Offline"))
            }
        val state =
            CachedCatalogRepository(remote, RoomPublicCache(db.local(), context, "photos-$name"))
                .observe()
                .toList()
                .last()
        assertFalse(state.loading)
        assertTrue(state.dogs.isEmpty())
        assertTrue(state.error!!.contains("No hay catálogo guardado"))
    }

    @Test
    fun draftCompositeKeySeparatesAccountsTypesAndReferencesAcrossReopen(): Unit = runBlocking {
        val store = RoomDraftStore(db.local())
        store.put("a", "SOLICITUD", "dog1", "a1")
        store.put("b", "SOLICITUD", "dog1", "b1")
        store.put("a", "PERRITO", "refuge", "dog")
        store.put("a", "SOLICITUD", "dog2", "a2")
        db.close()
        db = HuellitasDatabase.open(context, name)
        val reopened = RoomDraftStore(db.local())
        assertEquals("a1", reopened.get("a", "SOLICITUD", "dog1"))
        assertEquals("b1", reopened.get("b", "SOLICITUD", "dog1"))
        assertEquals("a2", reopened.get("a", "SOLICITUD", "dog2"))
        assertNull(reopened.get("guest", "SOLICITUD", "dog1"))
        reopened.remove("a", "SOLICITUD", "dog1")
        assertNull(reopened.get("a", "SOLICITUD", "dog1"))
        assertEquals("dog", reopened.get("a", "PERRITO", "refuge"))
        assertEquals("b1", reopened.get("b", "SOLICITUD", "dog1"))
    }

    @Test
    fun applicationRestoresAmbiguousAttemptAndDeletesOnlyAfterConfirmedSubmission(): Unit =
        runBlocking {
            val store = RoomDraftStore(db.local())
            val user = AuthUser("a", "ana@example.test", "Ana Prueba")
            val draft =
                ApplicationDraft(
                    "Ana Prueba",
                    "ana@example.test",
                    "5512345678",
                    "CASA",
                    "PROPIA",
                    true,
                    "2",
                    "MENOS_DE_4",
                    false,
                    "",
                    "Queremos ofrecer un hogar responsable y acompañar su adaptación.",
                )
            val id = UUID.randomUUID().toString()
            store.put(
                "a",
                "SOLICITUD",
                "d",
                DraftCodecs.application(SavedApplication(draft, 4, id, true, true, true)),
            )
            var fail = true
            val calls = mutableListOf<ApplicationAttempt>()
            val remote =
                object : AdoptionApplicationRepository {
                    override suspend fun submit(attempt: ApplicationAttempt): ApplicationReceipt {
                        calls.add(attempt)
                        if (fail) throw Exception("red")
                        return ApplicationReceipt(attempt.id, attempt.folio, "d", "s", "Enviada")
                    }
                }
            val vm = ApplicationFormViewModel(remote, user, "d", "s", store)
            keep(vm)
            withTimeout(10_000) {
                vm.state.first { !it.restoring && it.draftMessage?.contains("guardado") == true }
            }
            assertEquals(draft, vm.state.value.draft)
            assertTrue(vm.state.value.locked)
            withContext(Dispatchers.Main) { vm.send() }
            withTimeout(10_000) { vm.state.first { !it.sending && it.error != null } }
            assertNotNull(store.get("a", "SOLICITUD", "d"))
            assertEquals(id, calls.single().id)
            withContext(Dispatchers.Main) { stores.last().clear() }
            val restored = ApplicationFormViewModel(remote, user, "d", "s", store)
            keep(restored)
            withTimeout(10_000) { restored.state.first { !it.restoring } }
            fail = false
            withContext(Dispatchers.Main) { restored.send() }
            withTimeout(10_000) {
                restored.state.first {
                    it.receipt != null && it.draftMessage?.contains("eliminado") == true
                }
            }
            assertEquals(id, calls.last().id)
            assertNull(store.get("a", "SOLICITUD", "d"))
        }

    @Test
    fun dogDraftAndPrivatePhotoSurviveViewModelClearAndRestoreForSameAccountOnly(): Unit =
        runBlocking {
            val store = RoomDraftStore(db.local())
            val app = ApplicationProvider.getApplicationContext<HuellitasApplication>()
            val source = File(context.cacheDir, "hu17-source-${UUID.randomUUID()}.png")
            val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
            source.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
            val vm =
                DogRegistrationViewModel(
                    app.container.dogRepository,
                    app.container.dogPhotoImporter,
                    "local-a",
                    "s",
                    store,
                )
            keep(vm)
            withTimeout(10_000) { vm.state.first { !it.restoring } }
            withContext(Dispatchers.Main) {
                vm.edit(DogField.Name, "Borrador privado")
                vm.addPhotos(listOf(android.net.Uri.fromFile(source).toString()))
            }
            withTimeout(15_000) { vm.state.first { !it.importing && it.draft.photos.size == 1 } }
            withTimeout(10_000) {
                while (
                    store.get("local-a", "PERRITO", "s")?.let {
                        DraftCodecs.dog(it).draft.photos.size
                    } != 1
                ) delay(30)
            }
            val photo = vm.state.value.draft.photos.single()
            assertTrue(File(photo.filePath).isFile)
            withContext(Dispatchers.Main) { stores.last().clear() }
            assertTrue(File(photo.filePath).isFile)
            val restored =
                DogRegistrationViewModel(
                    app.container.dogRepository,
                    app.container.dogPhotoImporter,
                    "local-a",
                    "s",
                    store,
                )
            keep(restored)
            withTimeout(10_000) { restored.state.first { !it.restoring } }
            assertEquals("Borrador privado", restored.state.value.draft.name)
            assertEquals(photo, restored.state.value.draft.photos.single())
            assertNull(store.get("local-b", "PERRITO", "s"))
            withContext(Dispatchers.Main) { restored.reset() }
            withTimeout(10_000) { while (store.get("local-a", "PERRITO", "s") != null) delay(30) }
            assertFalse(File(photo.filePath).exists())
            source.delete()
        }

    @Test
    fun photoCacheServesOnlyPathsInPublicCatalogAndExcludesPrivateEvidence(): Unit = runBlocking {
        val cache = RoomPublicCache(db.local(), context, "photos-$name")
        cache.save(catalog)
        val image = byteArrayOf(1, 2, 3)
        val path = dog.photos.single().path
        cache.savePhoto(path, image)
        assertArrayEquals(image, cache.photo(path))
        val privatePath = "refugios/s/seguimientos/private/photo.jpg"
        cache.savePhoto(privatePath, image)
        assertNull(cache.photo(privatePath))
        cache.save(catalog.copy(dogs = emptyList()))
        assertNull(cache.photo(path))
    }
}

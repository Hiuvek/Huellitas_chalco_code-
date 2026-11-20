# Huellitas Chalco

Interfaz Android en Kotlin y Jetpack Compose basada en los mockups aprobados. Incluye las vistas del adoptante y del refugio, con navegación de demostración y datos de ejemplo. HU-01 incorpora registro e inicio de sesión reales con Firebase Authentication.

El estado de las 18 historias y el orden para implementar su funcionalidad están en [CHECKLIST.md](CHECKLIST.md).

## Pantallas

- Bienvenida, registro e inicio de sesión para ambos roles.
- Catálogo, detalle del perrito, requisitos e información del refugio.
- Solicitud de adopción en cinco pasos, confirmación y consulta de solicitudes.
- Perfil del adoptante y del refugio.
- Registro, edición, salud, fotografías y vista previa de perritos.
- Solicitudes recibidas, evaluación, decisiones y registro de adopción.
- Programación, registro e historial de seguimientos.
- Variantes visuales de errores, listas vacías, falta de conexión y confirmaciones.

El icono del ojo abre **Vistas de ejemplo**, donde se pueden consultar directamente todas las pantallas y variantes.

## Organización

Todos los paquetes están dentro de `app/src/main/java/com/example/huellitaschalco/`:

```text
MainActivity.kt               Entrada de Android
app/                          Composición de la aplicación y rutas
core/
  designsystem/               Componentes e iconos reutilizables
  navigation/                 Destinos y navegación de presentación
  firebase/                   Inicialización de Firebase y emulador de pruebas
  preview/                    Datos inmutables de ejemplo
feature/
  auth/                       Dominio, repositorio Firebase, ViewModel y pantallas de acceso
  catalog/                    Catálogo, detalle y requisitos
  application/                Formulario de solicitud
  requests/                   Solicitudes del adoptante
  profile/                    Perfiles
  shelter/
    dogs/                     Pantallas de perritos del refugio
    requests/                 Evaluación de solicitudes
    adoption/                 Pantallas de entrega y adopción
    followup/                 Seguimientos posteriores
  preview/                    Muestrario de pantallas
ui/theme/                     Colores, tipografía y formas
```

Las fotografías extraídas de los mockups están en `app/src/main/res/drawable-nodpi/`. No requieren conexión a internet.

## Alcance

HU-01 permite crear una cuenta con nombre, correo y contraseña, iniciar sesión, restaurar la sesión y cerrarla. Los formularios validan campos, muestran errores de Firebase y conservan los datos cuando falla una operación.

Las demás historias mantienen su interfaz de demostración. Todavía no hay consultas de perritos, persistencia en Room, solicitudes guardadas, cargas de fotografías, roles de refugio, Google Maps ni Gemini. Los mapas son ilustrativos.

La configuración de Firebase y los comandos de pruebas están en [docs/firebase-auth.md](docs/firebase-auth.md). El archivo `app/google-services.json` se conserva localmente y no se publica en Git.

## Ejecutar

Abrir la carpeta raíz en Android Studio, sincronizar Gradle y ejecutar el módulo `app`. Se conserva la configuración del proyecto: Android SDK 37 y Android mínimo 24.

Compilar desde la raíz:

```bash
./gradlew :app:assembleDebug
```

El APK se genera en `app/build/outputs/apk/debug/app-debug.apk`.

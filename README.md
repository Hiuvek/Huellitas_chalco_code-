# Huellitas Chalco

Interfaz Android en Kotlin y Jetpack Compose basada en los mockups aprobados. Incluye las vistas del adoptante y del refugio, con navegación de demostración y datos de ejemplo.

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
  preview/                    Datos inmutables de ejemplo
feature/
  auth/                       Bienvenida y acceso
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

Esta entrega implementa únicamente la interfaz. Se pueden recorrer pantallas, escribir en campos, cambiar selecciones y abrir diálogos. Los botones de envío, guardado, acceso y confirmación llevan a vistas de ejemplo.

No hay autenticación, persistencia, consultas, cargas de fotografías ni llamadas a Firebase, Room, Google Maps o Gemini. Los mapas son ilustrativos. Las selecciones y los textos son estado temporal de presentación; las listas no cambian sus datos al enviar formularios.

## Ejecutar

Abrir la carpeta raíz en Android Studio, sincronizar Gradle y ejecutar el módulo `app`. Se conserva la configuración del proyecto: Android SDK 37 y Android mínimo 24.

Compilar desde la raíz:

```bash
./gradlew :app:assembleDebug
```

El APK se genera en `app/build/outputs/apk/debug/app-debug.apk`.
# Huellitas_chalco_code-

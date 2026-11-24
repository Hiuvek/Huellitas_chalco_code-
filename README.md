# Huellitas Chalco

Aplicación Android para adopción de perritos en Chalco, Estado de México.

## Requisitos y compilación

Abre el proyecto en Android Studio y configura el SDK indicado en `app/build.gradle.kts`. Utiliza el JDK definido por Gradle y agrega localmente `app/google-services.json`, correspondiente a la aplicación `com.example.huellitaschalco` de tu proyecto Firebase. Este archivo y `local.properties` no se versionan.

```sh
./gradlew :app:assembleDebug :app:lintDebug
```

El APK se genera en `app/build/outputs/apk/debug/app-debug.apk`. Para instalarlo en un dispositivo con depuración USB:

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Si actualizas una instalación existente, utiliza su misma clave de firma; Android no permite actualizarla con otra clave.

## Servicios y datos

Utiliza Firebase Authentication, Cloud Firestore, Firebase AI Logic con Gemini y App Check. El catálogo, las fotografías y la información del refugio se consultan en Firestore; no se cargan semillas al iniciar. Se necesita conexión para consultar datos remotos y realizar operaciones privadas. Room conserva caché pública y borradores locales.

Las fotografías comprimidas se guardan en documentos separados de Firestore. No se utilizan Cloud Storage ni Cloud Functions. La ubicación abre Google Maps mediante un enlace externo. Se conserva el alcance actual de Firebase Spark.

`firestore.rules`, `firestore.indexes.json` y `firebase.json` definen la configuración del backend. Los responsables requieren una membresía autorizada de su refugio. Gemini necesita que Firebase AI Logic y App Check estén configurados; el APK de desarrollo requiere registrar su instalación de App Check.

Las credenciales, claves de firma y configuraciones privadas se conservan fuera de Git. No publiques tokens de desarrollo ni archivos de cuentas de servicio.

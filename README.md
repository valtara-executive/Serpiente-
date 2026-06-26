# StreamBrowser

Navegador Android con AdBlock integrado + Cast a Chromecast / Google TV / Fire TV.

## Características

- ✅ **AdBlock** — Bloquea ~200+ redes de anuncios, trackers y popups de sitios de streaming
- ✅ **Chromecast / Google TV** — Detecta URLs de video (.mp4, .m3u8, .mpd) y las envía a tu TV
- ✅ **Navegador completo** — Barra de URL, historial, gestos back/forward
- ✅ **Dark mode** — UI oscura diseñada para uso nocturno
- ✅ **Sin dependencias externas** — No requiere root ni permisos especiales

---

## Requisitos

- Android Studio Iguana (2023.2.1) o superior
- Android SDK 26+ (Android 8.0)
- Google Play Services instalado en el dispositivo (para Cast)
- Dispositivo Chromecast, Google TV, Fire TV con receptor Cast en la misma red WiFi

---

## Instalación

1. Abrir el proyecto en Android Studio
2. Esperar a que Gradle sincronice las dependencias
3. Conectar tu dispositivo Android por USB con depuración habilitada
4. Presionar **Run** (▶) o Build > Generate APK

---

## Cómo usar

### AdBlock
El bloqueador de anuncios está activo automáticamente. Bloquea:
- Redes de publicidad (Google Ads, DoubleClick, AppNexus, etc.)
- Trackers (Google Analytics, Facebook Pixel, Hotjar, etc.)
- Anuncios de popups y redirects comunes en sitios de streaming
- Notificaciones push no deseadas

### Cast a TV
1. Asegúrate de que tu teléfono y el Chromecast estén en la **misma red WiFi**
2. Presiona el ícono 📡 (MediaRoute) en la barra superior para seleccionar tu dispositivo
3. Navega al sitio y **reproduce el video**
4. Presiona el ícono 📺 (Cast) cuando aparezca resaltado — indica que se detectó un stream
5. Confirma el diálogo para enviar el video a tu TV

### Limitaciones de Cast
- Funciona con URLs de video directas (`.mp4`, `.m3u8`, `.webm`, `.mpd`)
- Sitios con DRM (Widevine) **no pueden** enviarse al Cast básico — requieren receptor personalizado
- Si el video usa streams ofuscados o fragmentados sin URL accesible, puede no detectarse

---

## Estructura del proyecto

```
StreamBrowser/
├── app/src/main/
│   ├── java/com/streambrowser/
│   │   ├── ui/MainActivity.java          # Actividad principal, WebView
│   │   ├── cast/CastOptionsProvider.java # Configuración Cast SDK
│   │   ├── cast/CastManager.java         # Envío de video al dispositivo
│   │   └── adblock/AdBlocker.java        # Motor de bloqueo de anuncios
│   ├── assets/adblock_rules.txt          # Reglas EasyList (200+ dominios)
│   └── res/                              # Layouts, iconos, temas
```

---

## Personalizar URL de inicio

En `MainActivity.java`, línea:
```java
private static final String HOME_URL = "https://pelisfliz.bz";
```
Cambia la URL por la que necesites.

---

## Agregar más reglas de adblock

Edita `app/src/main/assets/adblock_rules.txt` y agrega líneas en formato EasyList:
```
||dominio-de-anuncios.com^
```

---

## Notas técnicas

- WebView usa User-Agent de Chrome móvil para máxima compatibilidad
- El AdBlocker intercepta todas las peticiones de red en `shouldInterceptRequest`
- La detección de video ocurre en dos niveles: (1) intercepción de red y (2) inyección JS post-carga
- Cast usa el **Default Media Receiver** (`CC1AD845`) de Google — no requiere cuenta de desarrollador Cast

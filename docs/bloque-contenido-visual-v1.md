# Bloque: contenido visual, perfiles customizables, login UX y modo oscuro V1

> **Doc vivo del bloque — se actualiza en tiempo real.** Acá está
> asentada la especificación de Agustín (2026-08-19), el diagnóstico
> completo de Fase 0, las decisiones tomadas, las que faltan tomar y el
> estado de cada fase. El roadmap general de 15 bloques vive en
> `docs/roadmap-proximos-bloques.md`. Reglas permanentes: CLAUDE.md.

## Objetivo del bloque

Que DondeEntreno deje de sentirse "catálogo con placeholders" y pase a
sentirse una app real: actividades con identidad visual, publicadores
con marca propia, perfiles editables, visibilidad controlada, login
simple y modo oscuro. Es la base que destraba Publicador UX V2, Detalle
premium, Perfil público premium y Home feed (bloques 9–12 del roadmap).

## Reglas del bloque (de la especificación)

- Sin push sin aprobación. Sin tocar Supabase ni migraciones sin
  autorización. Sin tocar secrets/.env.
- No romper: login, roles, favoritos sync, Dondi, Gemini, imágenes
  existentes. No eliminar imágenes existentes. No cambiar contratos
  públicos sin explicarlo.
- Backend/migraciones: primero diagnosticar y proponer. Bloque grande →
  fases, frenando antes de producción.
- Producto: fotos reales, perfiles confiables, galerías cómodas,
  edición simple, visibilidad controlada. Likes más adelante;
  comentarios abiertos NO por ahora (requieren moderación).

## Estado de las fases (actualizar acá)

| Fase | Contenido | Estado |
|---|---|---|
| **0** | Diagnóstico técnico y UX completo | ✅ **HECHA** (2026-08-19, este doc) |
| **1** | Login UX V1 (frontend-only) | ✅ **EN PRODUCCIÓN** (`151960d`, deploy Vercel verificado por marcador; falta el smoke autenticado de Agustín) |
| **2** | Controles del publicador sin migración (backend + plan previo) | **IMPLEMENTADA COMPLETA** (2026-08-19, plan aprobado por Agustín con las 4 recomendaciones; rama `feat/fase2-controles-publicador`): backend — eliminar aprobadas (baja lógica `activa=false` + borrado best-effort del bucket público vía `eliminarPublicoPorUrl`; las aprobadas inactivas = historia y dejan de listarse en el panel), `PUT /imagenes/orden` (set exacto de GALERIA activas → orden 1..n), `PUT /imagenes/{id}/principal` (swap sin re-moderación, la vieja baja a la galería), límites (12 galería + 15 pendientes por actividad vía `MediaProperties` con defaults; 1 LOGO/PORTADA pendiente por tipo), `PATCH /imagenes/{id}` (título/descripción → alt público), fix de `calcularSiguienteOrden` (max+1, antes duplicaba); DTO del panel suma orden/titulo/descripcion (aditivo). **427 unit + 61 ITs verdes** (14 unit nuevos + IT de flujo completo `controlesDelPublicadorSobreImagenesAprobadas`). Frontend — flechas de orden, "Hacer principal", "Eliminar" con confirm (actividad y logo/portada), editor de texto colapsable por foto, preview pública (ya desplegable). **EN PRODUCCIÓN en dos tandas** (backend `3429c11` verificado por marcador `Allow: DELETE,PATCH,PUT` + 401 anónimos + público 200; frontend `c62abda` verificado por marcadores `imagenes/orden` y "Hacer principal" en los chunks, consola limpia). **SMOKE DE AGUSTÍN: OK (2026-08-20) — FASE 2 CERRADA.** Su smoke destapó el **pulido dark 7d** (misma rama): 5 textos ilegibles medidos con detector de contraste (ratio 1.01: `text-[var(--color-border-accent)]` —token de BORDE como tinta— sobre superficies de marca en HomePublishCta, HomeCrearCuentaCta ×2 y el subtítulo de Dondi → pineados al celeste original `#BFDDEA`, invariante); chip "1 seguidor" invisible (`bg-[#F1F8FC]`→`surface-soft`), esqueletos de MisFavoritos (`#F1F5F9`→`bg`), fichas del sheet (`#EAF6FB`→`info-soft`); y **el logo sin pastilla blanca**: en oscuro se re-tiñe con `invert(1) hue-rotate(195deg) saturate(1.4) brightness(1.05)` — verificado por muestreo de píxeles en canvas: navy→celeste claro, verde de marca idéntico (0,200,80); en claro el filtro no aplica. Detector de contraste corrido en home (logueada con datos reales vía backend local read-only), explorar, mi-cuenta (2 solapas), detalle, deportes y perfil público: CERO problemas restantes salvo el botón verde de WhatsApp (blanco/verde ratio 2.0 — IGUAL en claro, preexistente, documentado). El fantasma de los nombres de clubes de las capturas NO reproduce en el DOM (computan claros, ratio alto) — si reaparece tras este deploy, sospechar extensión del navegador tipo Dark Reader |
| **3** | Media Center publicador V1 (frontend sobre F2) | **IMPLEMENTADA** (2026-08-20, commit local en `feat/fase2-controles-publicador`, pendiente de push): página nueva **`/publicador/fotos`** ("Centro de fotos") — identidad visual (logo/portada con preview en su forma real y chip de estado Sin cargar/En revisión/Aprobada), fotos por actividad (miniatura de la principal con fallback por deporte, conteos aprobadas/en revisión, aviso "Sin imagen principal", CTA "Gestionar fotos" → detalle del panel), y **checklist de presencia** (6 pasos medibles con datos reales: logo, portada, principal, galería ≥3, descripción, WhatsApp — "horarios" quedó fuera a propósito: el resumen del panel no trae el dato y no se inventa) con el microcopy de confianza; cada paso pendiente linkea a donde se resuelve. Entradas: ítem "Mis fotos" en `menuCuenta.ts` (sheet + menú desktop automáticos) y "Centro de fotos" en los accesos del dashboard. Datos: 100% endpoints existentes (perfil, imágenes de perfil, actividades con `imagenPrincipalUrl`, fotos por actividad en `Promise.all` — fan-out barato con unidades de actividades). Verificado local: página renderiza bajo luz forzada, guard activo, error de backend limpio, sin overflow, "Mis fotos" en el sheet; **el camino con datos reales queda para el smoke de Agustín** (los endpoints son privados y el bypass no tiene token válido — mismo precedente que F2). Build 26/26 + typecheck + lint. **EN PRODUCCIÓN** (`4289329`, deploy Vercel verificado). **SMOKE DE AGUSTÍN: OK (2026-08-21) — FASE 3 CERRADA** |
| **4** | Vista pública de galerías V1 (frontend + endpoint agregado) | **IMPLEMENTADA** (2026-08-21, frontend-only, rama `feat/fase2-controles-publicador`, pendiente de push): **`LightboxFotos`** nuevo (`components/imagenes/`) — visor a pantalla completa con `<dialog>` nativo showModal (mismo patrón que el sheet: top layer, Escape, foco contenido, sin librerías), navegación por flechas + teclado (←/→) + swipe táctil (umbral 48px: un tap no navega), contador, cierre por X y por velo, scroll del fondo congelado, epígrafe del publicador y link contextual opcional; **oscuro en los dos temas a propósito** (convención de visor de medios — sin tokens de tema). En el **detalle** (`ActividadGaleria`): cada foto del carrusel es un botón que abre el visor en esa foto (el swipe del carrusel no se pierde: el arrastre no dispara click) + chip **"Ver todas las fotos (N)"** arriba a la izquierda (espeja el contador; solo con 2+ fotos). En el **perfil público** (`GaleriaPerfil` nuevo, reemplaza la grilla inline de `publicadores/[id]`): tocar una foto muestra LA FOTO (antes navegaba a la actividad — en una solapa de fotos era un salto inesperado); el camino a la actividad sigue como link "Ver la actividad" dentro del visor. **El fan-out N+1 del perfil quedó AFUERA a propósito**: arreglarlo bien pide endpoint público agregado (backend) — documentado, no entra en una fase frontend-only. **Verificado con datos reales** (backend local read-only + dev server, fotos reales del smoke de F2): detalle karate 8 fotos — visor abre 1/8, flecha derecha → 2/8, X cierra y restaura scroll, chip presente; perfil 8 fotos en grilla — visor con "Ver la actividad" → `/actividades/karate`, velo cierra; 390px: visor cubre 390×844 exacto, sin overflow horizontal; typecheck + lint + build 26/26. **EN PRODUCCIÓN** (`6998a5c`, deploy Vercel verificado: chip "Ver todas las fotos (8)" en el HTML servido y visor probado en vivo — abre 1/8, flecha → 2/8, X cierra; perfil con la grilla nueva y datos reales; consola limpia; el dominio real es `donde-entreno-web.vercel.app`). **SMOKE DE AGUSTÍN: OK (2026-08-21) — FASE 4 CERRADA** |
| **5** | Perfiles editables + seguridad de cuenta (ver sub-fases) | **5a IMPLEMENTADA** (2026-08-21, plan aprobado por Agustín con las 4 recomendaciones — `docs/plan-fase5a-cambio-contrasena.md`; pendiente de push en dos tandas): backend — `POST /api/auth/cambiar-password` (autenticado, explícito en SecurityConfig), verifica la actual con BCrypt, **reutiliza `validarPassword` del registro** (una sola política), rechaza nueva==actual, actualiza el hash y **revoca TODOS los refresh tokens del usuario** (repo nuevo `revocarTodasDe`, la pieza que faltaba del diagnóstico F0) ANTES de emitir la sesión nueva que devuelve (forma de login: el dispositivo del cambio queda adentro). 400 con `CambioPasswordInvalidoException` (401 reservado a sesión inválida), **freno de fuerza bruta** `LimitadorCambioPassword` (5 fallos/15 min por usuario → 429, en memoria, patrón LimitadorConsultas; bloqueado NO consulta la base — sin oráculo) y log `PASSWORD_CAMBIADO usuarioId= tokensRevocados=` (solo metadata). **438 unit (+11) + 65 ITs (+4 de `CambiarPasswordIT`: dos dispositivos → el ajeno muere y el devuelto vive, actual incorrecta no revoca, 6.º intento 429 aunque la password sea correcta, anónimo 401)**. Frontend — "Cambiar contraseña" en MenuAjustes (engranaje de `/mi-cuenta`, los tres roles), diálogo `<dialog>` nativo con 3 campos + ojito por campo (patrón F1), validación de cliente sin red (confirmación y política), éxito → `iniciarSesionDesdeRespuesta` (el camino único de sesiones) + aviso de que las otras sesiones se cerraron; **cerrar remonta el form** (un ojito en "mostrar" reabría el campo con la contraseña visible — cazado en la verificación). Verificado en navegador con bypass (revertido): diálogo, ojitos con aria, errores de cliente con CERO llamadas de red, reapertura limpia, 390px sin overflow, dark con tokens. El submit real lo cubren los ITs + smoke de Agustín. **EN PRODUCCIÓN EN DOS TANDAS** (2026-08-21): backend `e8dcb72` verificado con marcador `OPTIONS .../cambiar-password` → `Allow: POST,OPTIONS` + POST anónimo 401 + health/catálogo/login sanos; frontend `d03bf59` verificado con "Cambiar contraseña" en los chunks servidos de `/mi-cuenta` (cookie-trick; el poll anónimo no sirve porque la ruta redirige a login) + públicas 200 + redirect anónimo intacto. **SMOKE DE AGUSTÍN: OK (2026-08-21) — FASE 5a CERRADA.** 5b/5c bloqueadas por proveedor de email. **5d+5e IMPLEMENTADAS** (2026-08-21, plan aprobado por Agustín con las 4 recomendaciones — `docs/plan-fase5de-perfil-editable.md`; **cierran el gate #3 y el 5d**; pendiente de: script 21 en Supabase Y LOCAL por Agustín → ITs → push en dos tandas): **5e nombre público DIRECTO** — `PATCH /api/publicador/me` acepta `nombre` (obligatorio: vacío = 400, no limpia; log `PERFIL_NOMBRE_CAMBIADO` solo metadata) + campo en MiPerfilEditor con ayuda y salida de la lista de "Datos protegidos". **5d avatar** — script `21_avatar_usuario.sql` (columna `usuario.avatar_url` aditiva pura, con PRE/POST y COMMENT que fija la condición: SIN moderación en V1 porque no hay superficie pública — moderar ANTES de cualquier superficie futura); `PUT/DELETE /api/usuario/avatar` (validación espejo de F2 por firma de bytes/2MB, directo al bucket público vía guardarPendiente+publicar — cero cambios de interfaz del almacén —, reemplazo con borrado best-effort, logs de metadata); `UsuarioActualDTO.avatarUrl` aditivo; frontend — camarita sobre el avatar de la cabecera de `/mi-cuenta` → `DialogoAvatar` (dialog nativo, editor de encuadre F2 preset LOGO 1:1, "Quitar foto", refresco local inmediato + /me en la próxima carga), tipos tolerantes al orden de deploys. **450 unit verdes (+8)**; typecheck+lint+build OK. Verificado en navegador con bypass (revertido): camarita → diálogo → JPEG real generado por canvas → editor 1:1 → confirmar → **bug cazado y arreglado**: el error de subida quedaba invisible con el editor montado — ahora vuelve a la vista inicial y se ve ("No fue posible conectar con el servidor"). **Script 21 aplicado por Agustín en Supabase y local (2026-08-21, POST verificado: varchar 500 nullable) → 450 unit + 68 ITs verdes → EN PRODUCCIÓN EN DOS TANDAS**: backend `20e9f92` verificado (marcador `OPTIONS /api/usuario/avatar` → `Allow: DELETE,PUT,OPTIONS` al primer intento; PUT/DELETE/PATCH anónimos 401; health y catálogo 200 — el arranque con `validate` confirma la columna en Supabase); frontend `a98812f` verificado ("Foto de perfil" en chunks de `/mi-cuenta` y "Nombre público" en chunks de `/publicador/perfil`, vía cookie-trick; públicas 200, redirect anónimo intacto). **SMOKE DE AGUSTÍN: OK (2026-08-21) — FASES 5d Y 5e CERRADAS.** Del bloque queda pendiente únicamente 5b/5c (verificación y reset de email), gateadas por proveedor + DNS |
| **6** | Visibilidad de actividades (pausar/mostrar; backend sin migración) | **IMPLEMENTADA** (2026-08-21, plan aprobado por Agustín con las 4 recomendaciones — `docs/plan-fase6-visibilidad-actividades.md`; **cierra el gate #5**: PAUSADA = pausa voluntaria reversible del publicador, `activa` queda para baja administrativa; pendiente de push en dos tandas): backend — **sin migración** (el CHECK del script 01 ya incluye PAUSADA), `PATCH /api/publicador/actividades/{id}/visibilidad` `{visible}` (solo alterna PUBLICADA↔PAUSADA, idempotente, devuelve el detalle, log `ACTIVIDAD_VISIBILIDAD`), y **el fix que hacía imposible pausar**: las 5 queries del panel pasan a `IN (PUBLICADA, PAUSADA)` — listado, detalle, gestión de fotos, crear solicitud de cambio y **aprobación admin de cambios** (un cambio aprobado aplica sobre la pausada; el auto-rechazo queda solo para bajas reales). En pausa se gestiona todo (decisión #2); las solicitudes abiertas no se auto-rechazan (#3); los favoritos se omiten sin borrarse y vuelven al reanudar (#4, era el comportamiento emergente y el IT lo fija). El público no cambia una línea. Métricas: `actividadesPausadas` aditivo. Frontend — botón **Pausar/Reanudar** en el detalle del panel (confirm al pausar que explica el efecto; aviso persistente mientras está pausada; el texto "ya está publicada" se oculta en pausa), badge "Pausada" con tono warning (el DTO ya traía el estado), métrica "Actividades pausadas" en el dashboard solo si hay (>0), tolerante al orden de deploys (campo opcional). Verificación: unit + IT de flujo completo (pausar saca del público/favoritos sin borrar, el panel la sigue viendo, reanudar devuelve todo, ajeno 404, anónimo 401, usuario común 403); el E2E del botón queda para el smoke (precedente F2: el panel exige token real y el backend local escribe en Supabase). **EN PRODUCCIÓN EN DOS TANDAS** (2026-08-21): backend `c70dff8` verificado con marcador `OPTIONS .../visibilidad` → `Allow: PATCH,OPTIONS` + PATCH anónimo 401 + health/catálogo/detalle público sanos; frontend `228607f` verificado con "Pausar actividad" en los chunks del detalle del panel (cookie-trick) + públicas 200 + redirect anónimo con returnTo intacto. **SMOKE DE AGUSTÍN: OK (2026-08-21) — FASE 6 CERRADA** (y con ella el gate #5 del bloque) |
| **7** | Modo oscuro V1 (7a higiene → 7b tema → 7c barrido) | **7a EN PRODUCCIÓN** (`fa0d15b`, deploy Vercel verificado: tokens nuevos en `:root`, colores computados idénticos, mains de publicador/admin sin gradiente, consola limpia; falta smoke visual de Agustín): borrados los 19 `<main>` opacos (regresión cerrada), 8 tokens nuevos (`surface-soft`, `success-border/surface/wash`, `warning/-surface/-border`, `danger`), ~726 hexes → tokens (907→181 en tsx/ts; quedan modificadores `/NN`, `opengraph-image`/`DondiAvatar`/`IlustracionSinResultados` excluidos a propósito —Satori no soporta var()— y la paleta propia del Footer), primitivos SurfaceCard/StatusMessage/AppButton/AppLinkButton tokenizados (blancos → `--color-surface`; los `red-*` de danger/error se conservan a propósito: unificarlos con `--color-danger` cambiaría el color — decisión pendiente). Verificado en navegador: tokens computan idéntico (4/4 exactos), `var(--color-surface)/70` computa el MISMO oklab que `bg-white/70`, main de `/publicador` transparente con el ambiente del body visible. **7b + 7c EN PRODUCCIÓN** (`846ca3c`, deploy Vercel verificado: 6 selectores dark en el CSS servido, anti-FOUC en el HTML, oscuro navy sin sábanas claras, claro idéntico, consola limpia; falta smoke de Agustín): paleta navy completa en `:root[data-theme="dark"]` (tinta invertida, superficies por luminancia, semánticos oscuros, sombras negras) + separación **`--color-brand`/`--color-brand-strong`** (marca como superficie, invariante-ish: los 18 `bg-primary` de avatares/badges pasaron a `bg-brand` — sin esto, invertir la tinta rompía todo avatar); body nocturno con halos tenues; `.surface-glass` oscuro; `color-scheme` por tema; `--color-borde-vidrio` (header/nav); `--color-divisor`; **anti-FOUC** con script inline bloqueante en layout (+`suppressHydrationWarning`); `lib/preferenciaTema.ts` POR DISPOSITIVO + `SincronizadorTema` (sistema/otras pestañas) + **`SelectorTema`** (Sistema/Claro/Oscuro) montado en MenuAjustes, sheet mobile y menú desktop; **luz forzada** vía `[data-fuerza-claro]` en layouts nuevos de `/admin`, `/publicador` y `/publicar` (re-declara tokens; se borra cuando esas áreas se barran); barrido público: `bg-white`→surface (exactos), `from/via/to-white`→surface, `bg-white/N` clasificados uno a uno (los que van sobre gradientes de marca/imágenes QUEDAN blancos, con tinta pineada a `--color-brand` en chips/flechas/guardar sobre foto), chip claro para el logo en oscuro (`.logo-marca`; falta el asset del wordmark claro), moneda blanca para los 3 iconos PNG de HomeHowItWorks, pie con línea de acento en oscuro. Verificado (390/1280, dev server): detector de "sábanas claras" en CERO en `/`, `/explorar`, `/login`, `/mi-cuenta`; ciclo del selector EN VIVO (dark→Claro→blanco al instante→Sistema→dark, preferencia persistida); luz forzada activa en `/publicar` y `/publicador` con tema global dark; claro = valores originales exactos. **No verificable en el preview** (artefacto: la emulación no despacha el `change` de matchMedia): el cambio de tema del SO en caliente — patrón estándar, entra al smoke real. Restos documentados para después: stories con discos blancos (aceptado V1), `hover:text-[#0B314D]` (4, cosmético), reds de danger |
| **8** | Diseño futuro documentado (likes fotos, secciones custom, videos) | ✅ **HECHA** (2026-08-21) — **`docs/fase8-diseno-futuro.md`**: likes en fotos (migración `me_gusta_imagen`, patrón favoritos, contador dentro de los DTOs existentes), secciones de galería (columna `seccion` con catálogo fijo — la tabla de nombres libres se descarta en V1 por exigir moderación de texto), videos (V1 recomendado: embeds con allowlist, sin storage; subida nativa recién si quedan cortos), "Estoy entrenando acá" (contador agregado y anónimo — privacidad primero), valoraciones en 3 etapas (A social-proof sin migración de opinión → B estrellas con anti-fraude "solo cuentas con uso real" y promedio recién con N≥3 → C reseñas NUNCA sin moderación) y la regla ratificada: **comentarios NO hasta tener circuito de moderación de texto** (clonar el de imágenes). Con orden recomendado y dependencias (avatar de usuario ayuda pero no bloquea; email saliente no hace falta). Nada se implementa sin pedido explícito |

---

## FASE 0 — Diagnóstico (hecho 2026-08-19)

Cuatro relevamientos exhaustivos sobre el código real. Lo esencial:

### A. Sistema de imágenes — mejor de lo esperado

- **Una sola entidad/tabla `imagen`** (script 01 + 15): dueño
  `perfil_publicador_id` XOR `actividad_id` (CHECK), `tipo_imagen` ∈
  {LOGO, PORTADA, PRINCIPAL, GALERIA}, `estado_moderacion` ∈
  {PENDIENTE, APROBADA, RECHAZADA}, `activa` (baja lógica), **`orden`
  ya existe** (todas las queries públicas ya ordenan por él), `titulo`/
  `descripcion` existen pero son huérfanos (nada los escribe → alt text
  siempre null).
- **Storage**: dos buckets Supabase (`imagenes-pendientes` privado con
  URLs firmadas de 10 min, `imagenes-publicas`), subida vía REST con
  service key, validación por firma de bytes (JPEG/PNG/WebP), 2 MB,
  nombres UUID. Al aprobar: copy privado→público.
- **Endpoints completos** de subida (actividad: PRINCIPAL/GALERIA;
  perfil: LOGO/PORTADA), listado, aprobación/rechazo admin. `DELETE`
  solo si PENDIENTE — **nadie puede borrar una imagen aprobada**.
- **Recorte cliente** (`recorteImagen.ts`): PRINCIPAL/GALERIA 4:3
  (1200×900), LOGO 1:1 (400×400 PNG), PORTADA 3:1 (1200×400).
- **Fallback en 3 niveles** ya implementado: imagen real → ilustración
  del deporte (14 + 11 alias) → placeholder general.
- **Gaps**: orden manual = *backend sin migración* (endpoint PUT +
  arreglar `calcularSiguienteOrden`, que hoy cuenta rechazadas y puede
  duplicar). Promover GALERIA aprobada→PRINCIPAL = *sin migración*.
  Límites de cantidad = *sin migración* (hoy no hay NINGUNO). Alt/
  título editable = *sin migración*. **Secciones de galería = migración**
  (no existe el concepto). **Likes en fotos = migración** (`meGusta.ts`
  actual es local-only por actividad, no por foto). **Avatar de usuario
  = migración** (el CHECK XOR de dueño bloquea reusar la tabla tal
  cual; alternativa barata: columna `usuario.avatar_url`).

### B. Publicador — 4 de 5 gaps sin migración

- Perfil (`perfil_publicador`): edita hoy SOLO descripción, Instagram y
  email de contacto (`PATCH /api/publicador/me`). Nombre público, tipo,
  ciudad, WhatsApp/teléfono: **sin ninguna vía de cambio** (el "flujo
  con revisión" que promete el copy NO existe).
- Logo/portada: sube con moderación; **no puede eliminarlos una vez
  aprobados** (la guarda `retirarPendiente` solo permite PENDIENTE);
  reemplazo = subir otro y esperar aprobación.
- Actividades: **solo lectura + solicitudes de cambio moderadas** (9
  campos: título, descripción, precio, mostrarPrecio, 3 contactos,
  nivel, modalidad; una sola abierta por actividad → 409). **Horarios,
  ubicación, deporte, edades y enfoque no son editables por NINGÚN
  camino.** Sin ocultar/pausar/archivar/eliminar: `PAUSADA` existe en
  el CHECK pero es un estado muerto (nada lo escribe).
- ⚠️ **Trampa para la pausa**: el panel del publicador, las solicitudes
  y las imágenes filtran `activa=true AND estado='PUBLICADA'` en 4
  queries — pausar una actividad hoy la haría desaparecer de su propio
  panel sin forma de reactivarla. Cualquier pausa exige queries nuevas.
- Preview pública del perfil desde el panel: **no existe y es
  frontend-only** (`/publicadores/{id}` ya es público y `/me` da el id).

### C. Usuario común — casi nada editable (ya sabido, confirmado)

Avatar = iniciales (sin foto, sin backend), nombre/email solo lectura
(diálogo "Datos de mi cuenta" lo aclara), ciudad vía `/ciudades`,
deportes editables y sincronizados, apariencia: no existe.

### D. Login — corregido en Fase 1 (ver abajo)

Antes: sin placeholders, sin mostrar/ocultar, post-login por rol
(publicador → `/publicador`), error técnico del backend.

### E. Tema visual / dark mode — viable con higiene previa

- **Cero infraestructura de tema hoy** (sin `prefers-color-scheme`, sin
  `data-theme`, sin variante `dark:`, sin next-themes, sin
  `color-scheme`). Tokens en `@theme` de `globals.css` (20).
- **La app tokenizó el TEXTO pero no las SUPERFICIES**: 97% de los 708
  `var(--color-*)` son primer plano; las superficies son `bg-white`
  (142) y hexes crudos (263 `bg-[#...]`). Al revés de lo que necesita
  un dark mode.
- **907 hexes totales, ~700 duplican tokens existentes → 629 reemplazos
  mecánicos** (`#DDEAF3`→border-soft ×120, `#BFDDEA`→border-accent ×98,
  `#F8FAFC`→bg ×96, `#E8F6FB`→info-soft ×78, etc.). `#F8FCFE` (63) no
  tiene token: crear `--color-surface-soft`.
- ⚠️ **REGRESIÓN VIVA (bug en modo claro, hoy)**: **19 `<main>` en 17
  archivos de admin/publicador/guards** siguen pintando
  `bg-gradient-to-br from-[#F8FAFC] via-white to-[#E8F6FB]` opaco
  encima del fondo ambientado del body — el mismo string idéntico. El
  bloque de contraste (`3d3da84`) decía haber limpiado los 25 main;
  estos quedaron. El fix es BORRAR el string, no reemplazarlo.
- Puntos duros del dark: las **sombras desaparecen sobre navy**
  (jerarquía de elevación a reconstruir con luminancia — es rediseño);
  el **Footer se disuelve** (hoy es la única superficie oscura); el
  **wordmark del header necesita asset nuevo** (no hay horizontal
  claro; `logo-darkmode.png` es un tile cuadrado); `SurfaceCard
  variant="brand"` se invierte; falta `color-scheme` (selects/inputs
  nativos); **anti-FOUC** exige script inline en `layout.tsx` antes del
  primer paint; la preferencia va **por dispositivo** (patrón
  `ciudadActiva.ts`, NUNCA `porUsuario: true` — dejaría flash claro en
  cada carga).
- Trampa Lightning CSS confirmada acotada: 0 `backdrop-filter` en CSS
  plano; los overrides de tema del vidrio son solo `background-color` y
  sobreviven.
- **Estimación**: 3–5 días en 3 tandas — A higiene (~1,5d, vale aunque
  el dark se cancele), B infra de tema (~1d), C barrido de superficies
  (~1,5–2,5d). **Recorte de alcance recomendado**: V1 solo superficies
  públicas (~55 archivos); admin/publicador forzados a claro con
  `data-theme="light"` en su layout.

### 4 BIS. Seguridad de cuenta — diagnóstico

- **Emails duplicados: YA RESUELTO.** Normalización `trim+lowercase` en
  registro y login (backend y SQL) + índice único funcional
  `LOWER(BTRIM(email))` (script 07). `Test@Email.com` = `test@email.com`.
- **`email_verificado` ya existe y YA GATEA el login**: alimenta
  `isEnabled()` en `UsuarioPrincipal` — con `false` el login da 401. El
  registro lo fuerza a `true` a mano (`AuthService.crearUsuario`). Ese
  es el punto exacto de inserción de la verificación real. La UI nunca
  usa el campo (solo el type guard).
- **BCrypt** (strength 10) para passwords; fortaleza: ≥8 + letra +
  dígito (lógica de servicio, duplicada en frontend). Sin tope de
  longitud (BCrypt trunca a 72 bytes), sin lista de comunes.
- **No existe NADA de**: cambio de contraseña, reset, envío de emails
  (cero `spring-boot-starter-mail`, cero propiedad `spring.mail.*`,
  cero proveedor).
- **Patrones listos para copiar** del refresh token (script 19): token
  opaco 256 bits → SHA-256 hex, un solo uso, expiración, revocación por
  familia, `noRollbackFor` para no deshacer revocaciones al lanzar 401,
  higiene en cada login, reloj inyectable, mensajes de no-enumeración.
  **Falta `revocarTodasDe(usuarioId)`** en el repo (prerequisito de
  cambio y reset).
- **Rate limiting**: existe `LimitadorConsultas` del asistente (ventana
  deslizante en memoria por IP, thread-safe, con tope de memoria) —
  reusable extrayendo un limitador genérico con properties propias; el
  control fuerte del reset debe ser además por cuenta en base
  (columna `intentos`), porque un código de 6 dígitos no tiene la
  entropía de un token de 256 bits.
- **Diseño propuesto** (NO implementado):
  - *(a) Verificación de email*: script 21 `verificacion_email`
    (usuario_id, codigo_hash SHA-256 —salado o BCrypt, decidir—,
    email_destino, expira_en 10–15 min, usado_en, invalidado_en,
    intentos con tope duro 3–5). Endpoints `POST
    /api/auth/verificacion/enviar` (204 siempre, cooldown) y
    `/confirmar`. Dejar de forzar `emailVerificado=true` en el registro
    — **decisión de producto pendiente**: bloquear login hasta
    verificar vs. entrar con features limitadas (esto último exige
    sacar el campo de `isEnabled()` y gatear por feature).
  - *(b) Cambio de contraseña logueado*: **SIN migración** (columna
    `password_actualizada_en` opcional). Endpoint autenticado, valida
    actual con BCrypt, reusa `validarPassword` (hoy private — extraer a
    validador compartido), revoca TODAS las familias y emite sesión
    nueva para el dispositivo actual. UI en Ajustes → Seguridad.
    **Es lo único entregable completo sin proveedor de email.**
  - *(c) Reset "olvidé mi contraseña"*: script 22
    `password_reset_token` calcado del 19 (32 bytes, hash, un solo uso,
    30–60 min). `POST /api/auth/password/olvidada` (204 SIEMPRE, exista
    o no) y `/restablecer`. Revoca todas las familias. Rate limit por
    IP y por email. Frontend: link en login + `/recuperar-password` +
    `/restablecer-password`.
  - *(d) Camino crítico real*: **proveedor de email** (elección +
    cuenta + dominio verificado con SPF/DKIM/DMARC + env vars por
    nombre: `DONDEENTRENO_EMAIL_*`) — trabajo de infra/DNS fuera del
    código. Patrón de integración: bean siempre presente con fallback
    no-op que loguea (como `AsistenteConfig`/`AlmacenamientoConfig`).
  - *Deudas colaterales detectadas*: `normalizarEmail` triplicada,
    `ultimo_login_at` existe y nunca se escribe, rate limit de
    `/api/auth/*` sigue siendo la deuda anotada en
    `docs/plan-refresh-token.md`.

---

## FASE 1 — Login UX V1 (IMPLEMENTADA, pendiente de push)

**Decisiones de producto aplicadas:**

1. **Loguearse te deja en la app, no en un panel**: usuario común y
   publicador → `/` (Home). Admin → `/admin/solicitudes` (equipo
   interno, entra a trabajar — documentado como decisión). `returnTo`
   sigue ganando siempre (guard → login → vuelta a donde ibas).
2. **Estrenar cuenta ≠ loguearse**: el registro recién completado sí
   aterriza en el primer paso por rol (usuario → `/mi-cuenta` con
   onboarding; publicador → `/publicador` a armar su espacio). Alguien
   YA logueado que pisa `/login` o `/registro` sale al inicio.
3. El fallback del proxy (rol pisando ruta ajena → su espacio) queda
   con `obtenerRutaInicialPorRol`, intacto.

**Implementación** (`feat/login-ux-v1`):
- `lib/authRedirects.ts`: nueva `obtenerRutaPostLogin(rol)`.
- `LoginForm.tsx`: redirecciones nuevas (submit y sesión ya activa);
  placeholder `tu@email.com`; campo contraseña con placeholder y botón
  mostrar/ocultar (ojo/ojo tachado, `aria-label` "Mostrar/Ocultar
  contraseña", `aria-pressed`, botón real dentro del campo, target
  40px); error 401 → "Email o contraseña incorrectos. Revisá los datos
  e intentá de nuevo." (otros errores conservan el mensaje del server).
- `RegisterChoice/RegisterUserForm/RegisterPublisherForm`: el redirect
  de "ya logueado" pasa a `obtenerRutaPostLogin`; el post-registro
  queda por rol.
- Admin login (`/admin/login`): sin cambios, documentado.

**Verificado** (dev server, página pública): placeholders presentes,
toggle alterna `type`/`aria-label`/`aria-pressed` y vuelve, botón
dentro del campo a 320/390px, sin overflow, typecheck/lint/build OK.
**Para el smoke de Agustín**: login real de usuario → Home, de
publicador → Home, de admin → Administración; el mensaje de
credenciales malas; logout intacto; sesión persistente intacta.

**Nota**: el mapeo del 401 y las redirecciones con credenciales reales
no son verificables sin cuentas — quedan para el smoke autenticado.

---

## Fases siguientes — diseño acordado a validar

### Fase 2 — Controles del publicador sin migración (plan previo a código)
Orden manual de galería (endpoint + fix `calcularSiguienteOrden` +
drag&drop), promover GALERIA→PRINCIPAL (sin re-moderación: el archivo
ya fue aprobado), eliminar logo/portada aprobados (baja lógica; decidir
si se borra el objeto del bucket público), límites de cantidad, alt/
título editable, **preview pública del perfil desde el panel**
(frontend-only, entra acá). Todo backend SIN migración.

### Fase 3 — Media Center V1 (frontend)
Panel del publicador: identidad visual (logo/portada/estado), fotos por
actividad (principal, cantidad, pendientes/aprobadas, CTA gestionar),
checklist de presencia con el microcopy de confianza.

### Fase 4 — Vista pública de galerías V1
Detalle: hero + carrusel + "Ver todas las fotos" + lightbox. Perfil
público más visual. Secciones V1 SIN migración: "Principal" +
"Galería" (las secciones reales tipo Instalaciones/Entrenamientos
necesitan migración → quedan diseñadas en F8). Arreglar el fan-out N+1
del perfil público (endpoint agregado, backend sin migración).

### Fase 5 — Perfiles editables + seguridad
- **5a Cambio de contraseña** (sin migración, sin proveedor): entregable
  ya — plan previo por tocar auth.
- **5b Verificación de email / 5c Reset**: bloqueadas por decisión de
  proveedor + DNS + scripts 21/22. FRENADO hasta aprobación.
- **5d Usuario editable** (avatar/nombre): necesita migración + diseño
  (columna `avatar_url` vs tabla con moderación). FRENADO hasta diseño.
- **5e Publicador: nombre público editable**: sin migración si es
  directo; con migración si es moderado. Decisión pendiente.

### Fase 6 — Visibilidad de actividades (plan previo)
`PATCH /api/publicador/actividades/{id}/visibilidad` con semántica
`PAUSADA` (pausa voluntaria) vs `activa` (baja administrativa), fix de
las 4 queries del panel, y efectos colaterales: favoritos, feed,
sitemap, y el auto-rechazo de solicitudes abiertas sobre actividad no
publicada. Sin borrado destructivo (soft/pausa/archivo).

### Fase 7 — Modo oscuro V1
7a **Higiene** (borrar los 19 main, 629 hex→token, tokenizar
SurfaceCard/StatusMessage/AppButton/AppLinkButton, crear
`--color-surface-soft`) → 7b **Infra** (`[data-theme]`, paleta navy sin
negro puro, escala de superficies por luminancia, `color-scheme`,
`preferenciaTema.ts` por dispositivo, script anti-FOUC, toggle
Sistema/Claro/Oscuro en Ajustes) → 7c **Barrido** público-only
(admin/publicador forzados a claro). Pendiente de asset: wordmark
horizontal claro para el header.

### Fase 8 — Diseño futuro documentado
Likes en fotos (tabla `me_gusta_imagen`, patrón favoritos), secciones
de galería reales (migración), videos, "Estoy entrenando acá",
valoraciones. Comentarios: NO hasta tener moderación.

## Decisiones pendientes de Agustín (gates del bloque)

1. **Proveedor de email** (Resend/SendGrid/Postmark/otro) + dominio y
   DNS → desbloquea 5b/5c.
2. **Gate de verificación**: ¿sin verificar no se loguea, o entra con
   features limitadas? (recomendación: estricto para publicadores,
   laxo para usuarios — exige rediseñar el gate actual de `isEnabled`).
3. **Nombre público del publicador**: ¿edición directa o moderada?
4. **Eliminar logo/portada aprobados**: ¿borrar el archivo del bucket
   público o solo baja lógica? (recomendación: baja lógica + borrado
   best-effort).
5. **Pausa de actividad**: confirmar semántica PAUSADA vs `activa`.
6. **Dark mode V1 público-only**: confirmar recorte de alcance.
7. **Avatar de usuario**: `avatar_url` simple sin moderación vs tabla
   con moderación (afecta migración).

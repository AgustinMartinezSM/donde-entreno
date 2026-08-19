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
| **2** | Controles del publicador sin migración (backend + plan previo) | Pendiente — diseño abajo |
| **3** | Media Center publicador V1 (frontend sobre F2) | Pendiente |
| **4** | Vista pública de galerías V1 (frontend + endpoint agregado) | Pendiente |
| **5** | Perfiles editables + seguridad de cuenta (ver sub-fases) | Pendiente — 5a entregable ya; 5b/5c bloqueadas por proveedor de email; 5d necesita migración |
| **6** | Visibilidad de actividades (pausar/mostrar; backend sin migración) | Pendiente — plan previo obligatorio |
| **7** | Modo oscuro V1 (7a higiene → 7b tema → 7c barrido) | **7a HECHA** (2026-08-19, rama `feat/higiene-visual-tokens`): borrados los 19 `<main>` opacos (regresión cerrada), 8 tokens nuevos (`surface-soft`, `success-border/surface/wash`, `warning/-surface/-border`, `danger`), ~726 hexes → tokens (907→181 en tsx/ts; quedan modificadores `/NN`, `opengraph-image`/`DondiAvatar`/`IlustracionSinResultados` excluidos a propósito —Satori no soporta var()— y la paleta propia del Footer), primitivos SurfaceCard/StatusMessage/AppButton/AppLinkButton tokenizados (blancos → `--color-surface`; los `red-*` de danger/error se conservan a propósito: unificarlos con `--color-danger` cambiaría el color — decisión pendiente). Verificado en navegador: tokens computan idéntico (4/4 exactos), `var(--color-surface)/70` computa el MISMO oklab que `bg-white/70`, main de `/publicador` transparente con el ambiente del body visible. 7b y 7c pendientes |
| **8** | Diseño futuro documentado (likes fotos, secciones custom, videos) | Pendiente |

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

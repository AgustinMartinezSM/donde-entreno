# Mi perfil / navegación por rol V2

> Bloque frontend/UX. Plan de partida: `docs/plan-mi-perfil-ux.md`
> (opciones A/B/C). Agustín aprobó la dirección con la especificación
> del bloque: bottom sheet en mobile como opción preferida, menú
> desktop por secciones, `/mi-cuenta` como hub, y la regla de producto
> de abajo. Sin backend, sin contratos nuevos, sin rutas nuevas.

## 1. La regla de producto que ordena todo

**"Mi perfil" lleva SIEMPRE al espacio personal del usuario.**
**El espacio de publicador y la administración son opciones separadas**
para quien corresponde — nunca el reemplazo de su lado persona.

## 2. El problema (verificado en código y en producción)

En desktop el avatar desplegaba un menú con todo. En mobile no existía
ningún menú: el avatar del header (`MobileAccountShortcut`) y "Mi
perfil" de la barra (`MobileNavigation`) navegaban DIRECTO al destino
de `obtenerRutaInicialPorRol` — un publicador caía siempre en
`/publicador` y **no tenía ninguna forma de llegar a `/mi-cuenta`, a
sus guardadas ni a sus deportes**. El admin, igual con
`/admin/solicitudes`. Mismo label, destinos distintos por rol: el
modelo mental no se podía formar.

Además: dentro de `/publicador` el único camino de salida era "Ver el
sitio" (el perfil deportivo no existía como concepto), y dos textos de
la app seguían diciendo que favoritos y deportes "se guardan solo en
este navegador" cuando el sync con la cuenta ya está en producción
(`fcc4fa5`).

## 3. Qué se construyó

### Una sola fuente de verdad: `lib/menuCuenta.ts`

`obtenerSeccionesCuenta(rol)` define las opciones del menú de cuenta
por rol. La consumen el menú desktop y el panel mobile: las dos
superficies ofrecen exactamente lo mismo y un rol nuevo se agrega una
sola vez.

- **Tu espacio** (todos): Mi perfil deportivo (`/mi-cuenta`),
  Actividades guardadas (`/favoritos`), Mis deportes
  (`/mi-cuenta?tab=deportes`), Publicadores que sigo
  (`/mi-cuenta?tab=siguiendo`).
- **Publicador** (solo PUBLICADOR): Mi espacio de publicador
  (`/publicador`), Mis actividades (`/publicador/actividades`), Mis
  solicitudes (`/publicador/solicitudes`).
- **Administración** (solo ADMIN/SUPER_ADMIN): Solicitudes de
  publicación (`/admin/solicitudes`), Imágenes pendientes
  (`/admin/imagenes`).
- Cerrar sesión va aparte, al final, después de un separador.

### Mobile: panel de cuenta como bottom sheet (`MenuCuentaMobile`)

Tocar "Mi perfil" (barra) o el avatar (header) con sesión abre un
`<dialog>` nativo con `showModal()` anclado abajo: manija, cabecera con
avatar/nombre/email y chip de rol ("Publicador" / "Equipo"), las
secciones de arriba con iconos, y Cerrar sesión al pie.

- **`<dialog>` nativo a propósito**: foco contenido, cierre con Escape,
  velo y fondo inerte los da la plataforma; y al vivir en el top layer
  queda por encima de la barra inferior (z-50), del launcher de Dondi
  (z-50) y de su panel (z-60) **sin sumar un z-index más** — la matriz
  de flotantes se resuelve estructuralmente.
- Cierra con: X, Escape, click en el velo, elegir una opción, y cruzar
  el viewport a ≥1024px (los disparadores viven en contenedores
  `lg:hidden`; sin ese guard, rotar una tablet con el panel abierto
  dejaría un modal invisible bloqueando la página).
- Scroll del body congelado mientras está abierto; `max-h-[85dvh]` con
  scroll interno; safe-area en el padding inferior; contenido centrado
  a `max-w-lg` en tablet.
- **El visitante no tiene sheet**: "Ingresar" y el avatar navegan a
  `/login` (con `returnTo=/mi-cuenta` desde la barra). El login, con
  "Iniciar sesión" y "Crear cuenta" a la vista, ya es su menú — un
  panel intermedio solo le sumaría un toque.

### Desktop: `HeaderSessionMenu` por secciones

El mismo menú de siempre (MenuDesplegable: Escape, click afuera, foco
de vuelta al disparador) pero organizado con los títulos de sección y
los iconos de la fuente única. Cerrar sesión queda último y en gris.

### `/mi-cuenta`: solapas enlazables y conmutador de espacio

- **`?tab=` en la URL**: `normalizarTabPerfil` valida el parámetro y
  la solapa activa se deriva de `useSearchParams` (con `Suspense` en la
  página, que el prerender lo exige). Cambiar solapa usa
  `window.history.replaceState` — Next lo sincroniza sin re-navegar,
  sin mover el scroll y sin ensuciar el historial: atrás sale de la
  página, no desanda solapas. Así "Mis deportes" y "Publicadores que
  sigo" son destinos reales desde los menús.
- **`EspacioDeRol`**: card al tope de la columna de apoyo de "Para
  vos". Publicador: "Tu espacio de publicador" con Ir a mi espacio /
  Mis actividades / Mis solicitudes. Admin: "Administración" con Ir a
  administración / Imágenes pendientes. Usuario común: no existe.
- El espejo del conmutador: la barra superior de `PublicadorPageHeader`
  suma **"Mi perfil deportivo"** junto a "Ver el sitio" y "Cerrar
  sesión" — sirve a TODAS las páginas de `/publicador/**` de una vez.

### Copys que mentían tras el sync

- `MisFavoritos`: eyebrow "Guardadas en este dispositivo" → "Guardadas
  en tu cuenta"; la nota "solo en este navegador… más adelante vas a
  poder sincronizarlos" → "sincronizados con tu cuenta: entrá desde
  cualquier dispositivo y vas a ver la misma lista". La vista solo
  existe detrás de `AuthGuard`, así que "tu cuenta" siempre es cierto.
- `MisDeportes`: ídem ("se guardan en este dispositivo… van a viajar
  con tu cuenta" → "se sincronizan con tu cuenta").

## 4. Decisiones finas (y por qué)

- **Login/registro siguen aterrizando por rol** (`obtenerRutaInicialPorRol`
  intacto, igual que el fallback del proxy): la regla es sobre "Mi
  perfil", no sobre dónde te deja el login. Un publicador que entra a
  trabajar aterriza en su espacio y ahora sí tiene el puente de vuelta.
- **"Mis deportes" y no "Deportes y preferencias"**: es el lenguaje que
  la app ya usa en todos lados.
- **"Solicitudes de publicación" / "Imágenes pendientes" y no "Panel
  administrador"**: "Panel" está desterrado del lenguaje y el área se
  llama "Administración" (decisión cerrada previa).
- **El admin también tiene "Tu espacio" completo** (la espec mobile lo
  listaba mínimo): sale del mismo builder que el resto, no pierde nada
  y es equipo interno — la consistencia vale más que ahorrarle dos
  filas.
- **Sin item "Imágenes" para el publicador**: no existe una ruta propia
  (viven en `/publicador/perfil` y por actividad); un menú no inventa
  destinos.
- **La cabecera del perfil no recupera "Explorar" ni un "Editar
  deportes" permanente**: se quitaron a propósito en el bloque anterior
  (viven en la navegación global y en las stats clickeables); el CTA
  contextual de onboarding sigue igual.
- **`/favoritos` y la solapa Guardados siguen coexistiendo** (P5 del
  plan): mismo componente, cero contradicción; fusionarlos es decisión
  aparte.
- **Estados vacíos**: los de `/mi-cuenta` ya eran buenos (bloque
  `846bf12`) y el de actividades del publicador tiene CTAs; "No hay
  solicitudes para mostrar" quedó como está porque también aparece
  filtrando, donde un copy cálido de bienvenida sería mentira.

## 5. Verificación

Contra el dev server local con backend apagado (los fetch fallan a
error states — este bloque es navegación, no datos), sesión simulada
por el bypass documentado de `AuthSessionProvider` (revertido después:
`git status` limpio de ese archivo).

- **Publicador 390px**: "Mi perfil" abre el sheet con las 8 opciones +
  Cerrar sesión; "Mis deportes" navega a `/mi-cuenta?tab=deportes` con
  la solapa activa, el sheet cerrado y el scroll restaurado; el avatar
  del header abre el mismo panel; desde el sheet, "Publicadores que
  sigo" estando YA en `/mi-cuenta` cambia la solapa client-side; card
  "Tu espacio de publicador" con sus tres links; `/publicador` muestra
  "Mi perfil deportivo" en su barra (390 y 1280).
- **Admin**: secciones "Tu espacio" + "Administración", chip "Equipo",
  card "Administración"; `/mi-cuenta` accesible (el proxy ya lo
  permitía a todo rol con sesión).
- **Usuario**: solo "Tu espacio"; sin cards de rol.
- **Visitante**: cero `<dialog>` en el DOM; "Ingresar" →
  `/login?returnTo=/mi-cuenta`; Guardados → login con motivo; al quitar
  la sesión estando en `/publicador`, el guard redirigió a login.
- **Desktop 1280**: menú por secciones (orden: Tu espacio → Publicador
  → Cerrar sesión), panel de 224px sin desbordes ni wrapping, click
  afuera cierra.
- **Anchos 320/390/430/768/1280**: sin overflow horizontal; sheet
  anclado abajo en todos; a 320×568 el contenido scrollea interno
  (`85dvh`).
- **Flotantes**: con el sheet abierto, `elementFromPoint` sobre el
  launcher de Dondi y sobre la barra devuelve el dialog (top layer +
  fondo inerte nativo).
- **Cierres**: X, velo y navegación verificados en vivo (estado React
  sincronizado, `aria-expanded` correcto).

**Artefactos del entorno de preview, no bugs** (documentados para no
re-diagnosticarlos): con el pane del navegador oculto, (1) el evento
nativo `close` de `<dialog>` no se despacha — probado con un dialog
pelado fuera de React, mismo resultado — así que el camino
Escape→`cancel`→`close`→`onClose` no se pudo ejercitar ahí; es el mismo
patrón nativo ya desplegado en `DialogoDatosDeCuenta`. Y (2) las
animaciones CSS quedan congeladas en t=0 (sin compositing), por lo que
el sheet aparece desplazado `translateY(100%)`; con `finish()` queda
clavado abajo — la posición final es correcta.

**Pendiente que solo puede hacer Agustín**: smoke autenticado real en
producción (las tres cuentas, teléfono de verdad — incluye Escape/atrás
de Android sobre el sheet) tras el deploy.

## 6. Archivos

Nuevos: `lib/menuCuenta.ts`, `components/cuenta/IconoMenuCuenta.tsx`,
`components/cuenta/EspacioDeRol.tsx`,
`components/layout/MenuCuentaMobile.tsx`.

Modificados: `MobileNavigation`, `MobileAccountShortcut`,
`HeaderSessionMenu`, `app/mi-cuenta/page.tsx`, `usePerfilDeportivo`
(`normalizarTabPerfil`), `ParaVos` (prop `rol` + card),
`PublicadorPageHeader` (link espejo), `MisFavoritos` y `MisDeportes`
(copys de sync).

## 7. Lo que este bloque NO tocó

Backend, contratos, rutas nuevas, guards/roles (el proxy y los guards
quedaron idénticos), `obtenerRutaInicialPorRol` (login/registro/proxy),
el sync de favoritos/deportes, Dondi, y el área admin más allá de sus
entradas en los menús.

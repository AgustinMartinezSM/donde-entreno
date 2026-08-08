# Experiencia social deportiva V2 — `feature/social-sports-experience-v2`

**Fecha:** 2026-08-07 · **Rama:** `feature/social-sports-experience-v2` (desde `main` = `1f0a264`) · **Alcance:** solo `frontend/donde-entreno-web` · **Sin cambios** en backend, contratos de API, rutas públicas, slugs, dependencias ni variables de entorno.

## 1. Objetivo de la etapa

Consolidar la evolución de DondeEntreno de "catálogo de actividades" a **plataforma social para descubrir dónde entrenar**: mobile-first, con identidad de publicadores visible en todo el recorrido, feed real de seguidos, y corrección de los bugs visuales y contradicciones que dejaron iteraciones previas (bloque Codex incluido).

Se partió de una **auditoría comparativa** de las 10 áreas del frontend entre el repo productivo y el laboratorio (`donde-entreno-cloude-lab`), con verificación de contratos contra los controllers reales del backend.

## 2. Decisiones visuales tomadas

- **Una sola card de actividad** (`SocialActivityCard`, en `components/social/`) con variantes `feed` (home, feed de seguidos) y `compacta` (explorar y landings). Se eliminó la dualidad home-social vs explorar-catálogo.
- **Identidad del publicador en todas las superficies**: avatar de iniciales + tipo formateado + badge de verificado (`PublisherIdentity`, ahora también con variante compacta), incluida la página de detalle donde antes el publicador era invisible.
- **Fondo plano** `--color-bg` en todas las páginas públicas (antes la home era plana y el resto gradiente: el fondo "saltaba" al navegar). Los paneles conservan su gradiente suave como identidad de herramienta.
- **Un solo verde de éxito** `#1D7B4A` (se eliminó la dupla casi idéntica con `#167A4A`) y colores legacy fuera de paleta reemplazados por tokens de marca.
- **Radios de CTA unificados en 18px** (el estándar del kit `ui/`); tokens de radio aplicados donde había valores sueltos.
- **Tokens en `@theme` de Tailwind v4**: mismos valores, pero ahora generan utilities (`bg-primary`, `shadow-card`, etc.). Todo el código existente con `var(--color-*)` sigue intacto; los tintes recurrentes quedaron tokenizados para adopción gradual.

## 3. Cambios implementados (por bloque / commit)

1. **Shell social** — sticky header reparado en la home (`overflow-x-clip`), header full-bleed en mobile, links desktop desde `lg` con estado activo (`HeaderNavLinks`), **bottom nav hasta `lg`** (cierra el hueco 768–1279px sin navegación), `viewport-fit=cover` (sin esto las safe areas iOS valían 0), CitySelector con id por instancia y fetch compartido, ítem "Cuenta" neutro durante la carga de sesión, asistente sin launcher en `/admin`, `/login` y `/registro` y panel bottom-sheet hasta `lg`.
2. **Sistema de cards** — `SocialActivityCard` unificada con imagen y título clickeables, CTA anclado al fondo, precios `Intl es-AR`, etiquetas de catálogo formateadas (`lib/formatoCatalogo`: chau "PRINCIPIANTE"), `sizes` por layout en `ActivityImage` (nitidez en tablet). Eliminados `ActivityCard`, `ActivityList` y `PopularCategories` (muertos o superados).
3. **Mi cuenta como "mi espacio deportivo"** — Header del sitio presente, saludo, feed de novedades renderizado con cards sociales (el backend ya enviaba el `ActividadDTO` completo; solo se amplió el tipo), dejar de seguir con patrón optimista desde la lista de seguidos, skeletons de carga, datos administrativos colapsados al final.
4. **Detalle de actividad** — bloque de identidad del publicador junto a Seguir, secciones "Más de {publicador}" y "Similares en {ciudad}" (best-effort), **barra sticky de contacto en mobile** con precio, 404 real con `notFound()` + `noindex` para slugs inexistentes, `og:image` con la imagen real, WhatsApp saneado para `wa.me` con mensaje prellenado, rango de edades completo, chips de cupos/inscripción.
5. **Explorar** — el filtro `perfilPublicadorId` ya no se pierde al paginar/ordenar/filtrar; chip "Actividades de {nombre}" con quitar; select de ciudad preselecciona la ciudad activa (antes decía "Todas" filtrando por Mar del Plata); `?page` inválido saneado; buscador unificado con el `SearchBar` de la home (sugerencias de deportes).
6. **Panel publicador** — botón "Solicitar cambios" restaurado en el detalle (la ruta y el form existían pero quedaron inaccesibles al excluir el bloque 4) y copy "Próximamente" corregido; dashboard sin CTAs triplicados; "Ver el sitio" + "Cerrar sesión" en el header del panel; tile de imágenes solo si hay pendientes; link a la actividad generada desde la solicitud aprobada; fix del loading infinito con id inválido; eliminado el warning aceptado de lint (`SectionHeader`).
7. **Admin y auth** — eliminados los dos botones "Moderación de imágenes" que apuntaban a `/admin/imagenes` (404 en producción para admins); `/login` y `/registro` con "Volver al inicio" visible en desktop.
8. **Home social + SEO** — nueva sección **"Clubes y profes para seguir"** consumiendo `GET /api/perfiles-publicadores` (existía en el backend y nadie lo usaba) con botón Seguir; chips "Tus deportes" si hay preferencias guardadas; fix de la card "Gimnasio" que apuntaba al slug inexistente `gimnasio` (ahora "Musculación", slug real del seed); deportes populares linkean a las landings indexables `/ciudades/[ciudad]/[deporte]`; canonicals en home, explorar y landings.
9. **Perfil público de publicador V1** (`/publicadores/[id]`, rama `feature/perfil-publico-publicador`) — el nodo visible del grafo social, construido 100% sobre contratos existentes: portada e imagen de logo (`GET /api/perfiles-publicadores/{id}/imagenes`, con fallback a gradiente de marca y monograma), nombre + tipo + verificado, descripción, botón Seguir, contacto (WhatsApp saneado / Instagram / email), sitio web, botón Compartir (share nativo con fallback a portapapeles) y grilla de sus actividades. **`PublisherIdentity` ahora es navegable** (prop `href`): el nombre del publicador linkea a su perfil desde las cards, el detalle de actividad, la home y la lista de seguidos. El sitemap incluye los perfiles (best-effort) y los catálogos de deportes/ciudades se deduplican por request con `cache()` de React (antes las landings hacían hasta 3 fetches idénticos por request).

## 4. Archivos principales tocados

- `src/app/`: `page.tsx`, `layout.tsx`, `globals.css`, `actividades/[slug]/page.tsx`, `explorar/page.tsx`, `mi-cuenta/page.tsx`, `login/page.tsx`, `registro/**`, `admin/solicitudes*/page.tsx`, landings de deportes/ciudades (canonicals + card social + fondo).
- `src/components/social/`: `SocialActivityCard.tsx` (nuevo, unificado), `PublisherIdentity.tsx`.
- `src/components/layout/`: `Header.tsx`, `HeaderNavLinks.tsx` (nuevo), `MobileNavigation.tsx`, `HeaderFavoritosLink.tsx`, `ScrollToTopButton.tsx`.
- `src/components/home/`: `HomePublicadoresSugeridos.tsx` (nuevo), `HomePreferenciasChips.tsx` (nuevo), `HomeDiscoveryFeed.tsx`, `HomePopularSports.tsx`, `HomeHero.tsx`, `AsistenteHomeButton.tsx`.
- `src/components/cuenta/`: `FeedNovedades.tsx`, `PublicadoresSeguidos.tsx`, `PreferenciasDeportivas.tsx`.
- `src/components/publicador/`: `PublicadorDashboard.tsx`, `PublicadorPageHeader.tsx`, `PublicadorActividadDetail.tsx`, `PublicadorMetricasPanel.tsx`, `SolicitarCambiosForm.tsx`, `SolicitudesCambioList.tsx`, `PublicadorSolicitudDetail.tsx`.
- `src/components/`: `actividad/ActivityImage.tsx`, `actividad/ContactButton.tsx`, `explorar/FiltersPanel.tsx`, `explorar/Pagination.tsx`, `explorar/SortSelect.tsx`, `asistente/AsistenteWidget.tsx`, `ciudades/CitySelector.tsx`, `auth/CerrarSesionButton.tsx` (nuevo), `ui/SectionHeader.tsx`.
- `src/lib/formatoCatalogo.ts` (nuevo), `src/services/perfilPublicadorService.ts` (nuevo), `src/services/actividadService.ts`, `src/services/seguimientoService.ts`, `src/types/` (`actividad`, `seguimiento`, `publicadorPublico` nuevo).

## 5. Qué se reutilizó del Lab

- El **botón "Solicitar cambios"** del detalle del publicador (bloque del lab `PublicadorActividadDetail.tsx:305-317`), única pieza funcional del lab que main había perdido al excluir el bloque 4.
- **Criterios** (no archivos): visibilidad temprana de la etiqueta de favoritos en el header, radios tokenizados en las cards de deportes populares.

## 6. Qué se descartó del Lab

- Todo el shell del lab (Header de 3 filas, sin bottom nav, sin safe areas): superado por el shell de Codex ya en main.
- La home landing del lab (`ActivityList`, hero de venta con 3 CTAs): superada por el home de descubrimiento.
- El **bloque 4 de imágenes** completo (subida del publicador, `GestionImagenesActividad`, `admin/imagenes`, services de imágenes): sigue excluido a propósito hasta implementar Supabase Storage en el backend (regla de producción). La UI del lab queda como referencia lista para ese momento.
- El resto del lab es byte-idéntico a main (verificado con diff ignorando CRLF): no había nada más que portar.

## 7. Qué quedó pendiente

- **Perfil público de publicador — deuda de backend para la V2**: la V1 ya está implementada (`/publicadores/[id]`), pero resuelve el perfil filtrando el listado público (no hay `GET /api/perfiles-publicadores/{id}` individual — cuando exista, solo cambia `obtenerPerfilPublicadorPorId`), no hay slug amigable para la URL y no hay contador público de seguidores. Los tres son cambios aditivos de backend.
- **Deuda de contrato para la visión social** (documentada, no implementable solo con frontend): favoritos/preferencias por cuenta (hoy `localStorage` por dispositivo, prometido en la UI), likes con contadores, paginación del feed (top 20 fijo), `imagenPrincipalUrl` en el DTO público de listados (las cards públicas siempre caen al placeholder por deporte), refresh token (la sesión muere por pestaña). ~~`fechaPublicacion` en `ActividadDTO`~~ → **hecho** (rama `feature/fecha-publicacion-dto`): campo aditivo mapeado de `createdAt` (las actividades nacen al aprobarse, así que es su fecha de publicación real; sin migración), con "Publicada hace X" (`Intl.RelativeTimeFormat` es-AR) en las cards del feed y el detalle. Campo opcional en el frontend: tolera cualquier orden de llegada de los deploys.
- **Adopción gradual de utilities de marca**: los tokens ya están en `@theme`; el código existente sigue con valores arbitrarios equivalentes. Migrarlos al escribir/tocar componentes.
- `AppInput`/`AppSelect` compartidos para unificar formularios (hoy inputs 18px vs selects 14px).
- Cache/`revalidate` para catálogos en landings SEO (hoy `no-store` con fetches repetidos por request).
- Menores preexistentes del repo: IT del orden del feed, regla explícita `/api/usuario/**` en `SecurityConfig`, flag `Secure` en la cookie `de_sesion`.

## 8. Riesgos

- **Smoke test productivo autenticado sigue pendiente** (heredado del bloque Codex): usuario/publicador/admin end-to-end en producción. El QA local cubrió visitante + flujos de redirección.
- El 404 de actividad inexistente devuelve **status HTTP 200 con UI 404 + `noindex`** (limitación del streaming de Next con `loading.tsx`: los headers ya viajaron). SEO cubierto por `noindex`; si se quisiera el status real habría que resignar el loading state.
- La bottom nav ahora llega hasta `lg` (1023px): cualquier componente flotante nuevo debe respetar los offsets (`bottom-[calc(...)]` + safe area) como hacen asistente y scroll-to-top.
- La normalización de WhatsApp antepone `54` si falta código de país: correcto para el foco actual (Argentina); revisar si la plataforma se expande.
- El sweep de fondo plano tocó 12 páginas: revisado en navegador, pero ojo al hacer merge con ramas que toquen esas páginas.

## 9. Validaciones ejecutadas

- `npm run typecheck` ✅ · `npm run lint` ✅ (0 errores, **0 warnings** — desapareció el warning aceptado de `SectionHeader`) · `npm run build` ✅ (todas las rutas generan).
- `git diff --check` ✅ limpio.
- **QA en navegador contra build de producción + backend local con datos reales del seed** (320/375/768/1280): sin overflow horizontal en ninguna medida; header sticky funcionando en la home; bottom nav visible hasta 1023px y oculta en desktop; links desktop con estado activo; label "Guardados" visible; cards sociales con avatar/precio formateado/chips legibles en explorar; select de ciudad preseleccionado; chip de publicador con "Quitar" en `/explorar?perfilPublicadorId=N`; detalle con bloque de publicador, barra sticky de contacto mobile y sección "Más de {publicador}" (aparece solo cuando hay hermanas — verificado con Kimberley); slug inexistente → página 404 + `noindex`; `/favoritos` y `/mi-cuenta` redirigen a login; asistente ausente en `/login`; `sitemap.xml` y `robots.txt` 200; canonical presente en home.
- **No probado:** sesiones autenticadas end-to-end (sin credenciales locales) — ver riesgos.

## 10. Próximos pasos recomendados

1. Smoke test productivo autenticado (usuario, publicador, admin, mobile) tras el deploy.
2. Perfil público de publicador (pedir al backend el endpoint de detalle + slug + contador de seguidores; el frontend ya tiene `PublisherIdentity`, `SeguirPublicadorButton` y el service público como base).
3. `fechaPublicacion` en `ActividadDTO` (cambio aditivo) para "publicado hace X" en feed y cards.
4. Favoritos y preferencias por cuenta (los módulos `lib/favoritos` y `lib/preferenciasDeportivas` ya están diseñados como único punto de reemplazo).
5. Bloque 4 de imágenes contra Supabase Storage, reutilizando la UI del lab.

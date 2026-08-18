# Contexto — Shell social + Home de descubrimiento V1 (bloque de Codex)

Reporte entregado por Codex al finalizar el bloque, conservado como referencia. **Nota de estado (2026-08-07):** el procedimiento de merge que este reporte dejaba como "recomendado" **ya se ejecutó**: `main` = `origin/main` = `1f0a2642` (fast-forward de `codex/social-shell-home-v1`, verificado localmente). Queda pendiente el smoke test productivo autenticado (ver "Limitación de QA").

## Alcance

- Rama: `codex/social-shell-home-v1` (commits `fb7d575b` y `1f0a2642`), partiendo de `main` = `ed3fbeac`.
- 20 archivos modificados, +804 / −244 líneas, **todo dentro de `frontend/donde-entreno-web`**.
- **No** se modificaron: backend, base de datos, migraciones, Supabase, variables de entorno, secrets, `package.json`, `package-lock.json`, contratos de API, rutas públicas existentes.

## Cambios funcionales y visuales

1. **Header responsive** — simplificado y reorganizado; comportamiento por rol (visitante/usuario/publicador/admin); mejores accesos a favoritos, cuenta y panel; acceso mobile a la cuenta según rol.
2. **Navegación inferior mobile** — Inicio · Explorar · Guardados · (Ingresar / Mi espacio / Panel según sesión y rol). Estados activos, `aria-label`/`aria-current`, safe area inferior, enlaces protegidos redirigen a login, distinción Cuenta vs Panel.
3. **Home de descubrimiento** — sigue SSR/dinámico con datos reales vía `buscarActividades` (sin mocks permanentes). Nuevo Hero de descubrimiento, búsqueda por texto y ciudad, ciudad activa sincronizada, feed de descubrimiento, deportes populares, CTA de registro y de publicadores. Copy de "comunidad deportiva local". Metadata, H1 y contenido indexable conservados.
4. **Cards sociales** — `SocialActivityCard` (identidad del publicador, imagen, deporte y atributos, ubicación, precio, favoritos, link a la actividad) + `PublisherIdentity`.
5. **Asistente** — acceso explícito desde el Hero de Home (sin launcher flotante duplicado en Home; en otras rutas se conserva); el diálogo queda por encima de la navegación mobile; el foco vuelve al CTA que lo abrió; contempla `safe-area-inset-bottom`.
6. **Responsive y safe areas** — navegación inferior, asistente, scroll-to-top, paddings inferiores, capas/z-index; probado en 320/375/390/768 px y desktop.

## Archivos principales

```
src/app/globals.css · src/app/layout.tsx · src/app/page.tsx
src/components/asistente/AsistenteConversacion.tsx · AsistenteWidget.tsx
src/components/ciudades/CitySelector.tsx
src/components/explorar/SocialActivityCard.tsx
src/components/home/AsistenteHomeButton.tsx · HomeDiscoveryFeed.tsx · HomeHero.tsx
src/components/layout/Header.tsx · MobileAccountShortcut.tsx · MobileNavigation.tsx · ScrollToTopButton.tsx
src/components/social/PublisherIdentity.tsx
```

## Validaciones realizadas por Codex

- `npm run typecheck` OK · `npm run lint` OK (0 errores; persiste el warning preexistente de `SectionHeader` en `SolicitudesCambioList.tsx`) · `npm run build` OK (24 páginas estáticas) · `git diff --check` OK.
- Responsive sin overflow horizontal en 320/375/390/768/desktop.
- `robots.txt` y `sitemap.xml` HTTP 200. Home conserva metadata/H1/contenido indexable. Rutas y slugs sin cambios.

## QA funcional (visitante)

Home nuevo, Explorar, búsqueda, login y registros, favoritos→login con `returnTo`, rutas privadas→login, navegación mobile, asistente y retorno de foco: **OK**. Scroll-to-top y asistente no pisan la bottom navigation. Las rutas que dependen del backend mostraron su fallback (backend local apagado durante el QA).

## ⚠️ Limitación de QA (pendiente)

**No se probaron sesiones autenticadas end-to-end** (sin backend local ni credenciales de usuario/publicador/admin). Se revisó estáticamente: destinos por rol, restricciones de rutas, Header autenticado, Cuenta/Panel, panel publicador, panel admin, favoritos y funcionalidades sociales. No se consideró bloqueador, pero **queda recomendado un smoke test autenticado en producción**: usuario común, publicador, admin/superadmin, mobile, SEO, favoritos, preferencias, seguimiento/feed y asistente.

## Reglas que rigieron el bloque

No tocar backend / base de datos / Supabase / variables / secrets; no instalar dependencias; no agregar funcionalidades nuevas; no cambiar contratos de API ni rutas; no hacer force push.

# Nota — Soft-404 en las landings dinámicas (deportes y ciudades)

## Síntoma

Las landings `/deportes/[slug]`, `/ciudades/[slug]` y `/ciudades/[slug]/[deporte]`
sirven el **contenido** de "no encontrado" correcto para slugs inexistentes,
pero con **status HTTP 200** en vez de 404 (soft-404). Es comportamiento de
Next.js con páginas dinámicas: `notFound()` en un render dinámico streameado
no cambia el status ya iniciado.

## Impacto: bajo

El `sitemap.xml` solo lista slugs válidos del seed (deportes del catálogo y la
ciudad activa). Los buscadores nunca descubren slugs inexistentes, así que no
indexan soft-404. El problema solo se ve tipeando a mano una URL inválida.

## Intento con generateStaticParams (descartado en el laboratorio)

Se probó `generateStaticParams` (allowlist estático desde el catálogo espejo
del seed) + `export const dynamicParams = false`, que da un 404 real para slugs
fuera de la lista. **No es viable en este laboratorio:**

- `generateStaticParams` hace que Next intente **pre-renderizar cada param en
  build**. Estas páginas leen datos del backend (deportes, ciudad,
  actividades), así que el build pasa a **depender del backend disponible en
  tiempo de build**.
- Con `dynamicParams = false`, un pre-render que falla (backend no alcanzable)
  **rompe el build** (exit 255). En el laboratorio el build corre normalmente
  sin backend, y aun con el backend levantado el fetch de build a `localhost`
  falla (probable resolución IPv6 del fetch de Node vs. IPv4 del backend).
- Agregar `export const dynamic = "force-dynamic"` evita el pre-render, pero
  entonces `dynamicParams = false` se ignora y **vuelve el soft-404**.

Conclusión: para el estado actual, `generateStaticParams` cambia un soft-404
inofensivo por un build frágil acoplado al backend. Se mantuvieron las
landings como dinámicas.

## Recomendación

- **Ahora:** convivir con el soft-404 (impacto SEO nulo por el sitemap).
- **Cuando exista deploy real con backend garantizado en build** (Render/Vercel
  con la API arriba y `localhost` no en juego): reconsiderar
  `generateStaticParams` + `dynamicParams = false`, resolviendo la resolución
  de host del fetch de build. Ahí el patrón es el estándar y da 404 reales.

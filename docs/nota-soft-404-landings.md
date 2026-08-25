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

---

## Causa raíz encontrada (2026-08-25)

La explicación de arriba ("`notFound()` en un render streameado no
cambia el status") era correcta pero incompleta: faltaba **por qué** el
render ya venía streameado. La causa concreta es que existe
**`src/app/loading.tsx`, en la RAÍZ de `app/`**. Ese archivo pone un
boundary de Suspense sobre **todas** las rutas que no tengan uno
propio: Next manda el shell enseguida —con status 200 ya escrito— y
`notFound()` llega tarde.

Medido en producción el 2026-08-25, **las seis rutas dinámicas** dan
200 con contenido de "no encontrado":

```
/deportes/no-existe            -> 200
/ciudades/no-existe            -> 200
/ciudades/mar-del-plata/no-x   -> 200
/publicadores/no-existe        -> 200
/actividades/no-existe         -> 200
/eventos/no-existe             -> 200   (heredado, la ruta es de la Fase 9)
```

O sea que no son las tres landings de esta nota: es transversal, y toda
ruta nueva con `notFound()` lo va a heredar.

## Por qué NO se arregla ahora

Las dos salidas posibles tienen un costo mayor que el problema:

1. **Quitar los `loading.tsx`** de las rutas que pueden dar 404 (la
   raíz, `actividades/[slug]`, `publicadores/[id]`). Devuelve el 404
   real, pero **se pierden los skeletons** que la Fase 1 puso
   justamente para que la navegación no se sienta vacía. Se cambia una
   mejora visible en cada carga por una corrección que hoy no ve nadie.
2. **`generateStaticParams` + `dynamicParams = false`**. Ahora sí
   existe deploy real, pero el build de Vercel pasaría a **depender de
   que Render responda**, y Render free tier hace **spin-down por
   inactividad**: un build disparado con la API dormida fallaría y
   dejaría el deploy roto. Cambia un soft-404 invisible por un riesgo
   real de no poder desplegar.

**El impacto sigue siendo nulo**: el `sitemap.xml` lista solo slugs
válidos (y desde la Fase 9, eventos que existen), así que ningún
buscador descubre estas URLs. El problema solo aparece tipeando a mano
una URL inválida.

**Cuándo reconsiderar**: si alguna vez el sitio deja de depender del
free tier para el build, la opción 2 es el patrón estándar. Si en algún
momento se decide que los skeletons no valen lo que cuestan, la opción
1 es de una línea por archivo.

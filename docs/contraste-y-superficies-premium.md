# Contraste y superficies premium

Segunda pasada visual sobre el refresh mobile. Solo frontend: no toca
backend, base, secretos, contratos ni variables de entorno.

Continuación de `refresh-visual-mobile-dondi.md`, que dejó el sistema de
superficies armado y Dondi flotando. Este bloque ataca lo que quedaba:
la app seguía leyéndose plana y blanca.

## 1. El hallazgo que ordena todo el bloque

**El fondo ambientado del bloque anterior no se veía.**

`globals.css` definía dos halos radiales sobre el `body`, pero cada
página abría con:

```jsx
<main className="min-h-screen bg-[var(--color-bg)] ...">
```

Ese `main` pinta el color de fondo **opaco** y encima de los halos.
Medido en la home: el `main` cubría **8508 px de los 9256** del
documento. O sea que los halos existían, estaban bien hechos y no los
veía nadie salvo en la franja del pie.

Eran 25 `main` con el mismo patrón. Ahora ninguno pinta fondo: el color
base lo pone el `body`, que es el único lugar donde el fondo tiene que
decidirse.

Esto también explica por qué "el fondo es plano" seguía siendo cierto
después de un bloque que se había ocupado justamente de eso.

## 2. Qué se implementó

### Fondo de la app

Cuatro capas fijas respecto del viewport: tres halos de marca (celeste
arriba a la derecha, verde abajo a la izquierda, azul al medio para
ligarlos) sobre una base fría con degradado vertical que arranca en
blanco puro detrás de la cabecera y toma cuerpo hacia abajo
(`#FFFFFF → #F2F7FC → #EAF1F8`).

El sentido del degradado no es decorativo: arriba la app tiene que
sentirse liviana, y abajo —donde se apilan las cards— el fondo tiene que
dar contra qué recortarlas. Sigue siendo todo CSS: cero assets nuevos.

### Contraste del texto secundario

`--color-muted` pasa de `#627D98` a `#4E6A85`.

El valor viejo daba **4,28:1 sobre blanco** y **4,09:1 sobre el fondo de
la app**, los dos por debajo del 4,5:1 que pide WCAG AA para texto
normal — y la mayor parte de ese texto es de 14 px. El nuevo da
**5,64:1 y 5,13:1**. Es una mejora de accesibilidad real y además es lo
que deja subirle cuerpo al fondo sin quedar al límite.

Se usa por token en 178 lugares y **cero veces hardcodeado**, así que el
cambio entra parejo en toda la app. Se verificó que no haya texto
`muted` sobre superficies oscuras (donde oscurecer sería empeorar).

### Botones de acción principal

`AppButton` y `AppLinkButton` en variante `primary` pasan de azul plano a
degradado (`.gradient-cta`: azul de marca → azul saturado).

El degradado **no llega al verde**: un botón chico con los tres colores
de la marca adentro se lee sucio, y el verde ya es el color del acento y
de los estados de éxito.

El color de fondo queda debajo del degradado a propósito: si el
degradado no se pintara, el botón queda azul con texto blanco, nunca
transparente. Es la misma precaución que la trampa de `SurfaceCard`
—donde el fondo se elige solo por `variant`—, pero al revés: acá el
`background-image` y el `background-color` conviven en la misma clase, no
compiten.

### Regla de acento en los títulos de sección

`SectionHeader` suma una barrita corta bajo el eyebrow. Las **50**
secciones de la app usan eyebrow, así que todas ganan el mismo anclaje
visual sin tocar el tamaño ni el peso de la tipografía. La misma regla
aparece en el pie y en las pantallas de acceso.

### Cabecera

Pasa a la misma superficie de vidrio que la navegación inferior
(`surface-glass` + `backdrop-blur-xl backdrop-saturate-150`). Era un
`#F8FAFC` al 95% que sobre el fondo nuevo se leía como una franja opaca;
las dos barras de la app ahora se leen iguales.

Sigue valiendo la regla de Lightning CSS: el desenfoque va con utilities
de Tailwind, nunca con un `backdrop-filter` escrito a mano.

### Pie

Era una pila de tres listas de texto. Ahora cada destino es una fila con
tile de ícono y chevron —el lenguaje de fila del resto de la app— y
contacto son tres botones reales con sus íconos. Se suma un hilo de
acento arriba y la fila de marca abajo. Los íconos son SVG inline: ni una
request nueva.

### Pantallas de acceso

Login, crear cuenta de usuario y crear cuenta de publicador tenían el
**mismo bloque de marca copiado tres veces** con distinto copy (y ya
habían divergido en anchos de columna). Ahora vive una sola vez en
`components/auth/AuthHero.tsx`.

**Corrección mobile-first medida**: con los tres puntos de venta, el hero
medía **500 px de los 844** de un teléfono y el formulario —lo único que
la persona vino a hacer— arrancaba al 63% de la pantalla. Los puntos se
ocultan abajo de 640 px: el hero baja a **263 px** y el campo de email
queda a 520 px, dentro de la primera vista. A 320×720 el hero mide 291 px
y el email cae en 580.

Los dos accesos a registro pasan a tener tile de ícono y chevron: se leen
como dos caminos para tocar y no como dos notas al pie.

### Filtros

Cada campo suma su tile de ícono. No es adorno: cinco selects idénticos
apilados en mobile se leen como un formulario largo y sin jerarquía, y el
ícono es lo que deja ver de un vistazo en cuál está parada la persona.

La grilla de escritorio baja de **5 columnas a 3**. Con cinco, cada campo
quedaba en 211 px a 1440 px y sumarle el tile lo dejaba en 159 px útiles;
con tres, el select mide **297 px**. El panel es colapsable, así que el
alto extra de la segunda fila no le saca lugar a los resultados.

## 3. Validaciones

- `npm run typecheck`, `npm run lint`, `npm run build` (25 páginas).
- Navegador, con backend local y datos reales:
  - **320 / 390 / 1440 px**: `scrollWidth == innerWidth` y **cero
    elementos desbordados** fuera de los carruseles con scroll propio.
  - Fondo: `main` en `rgba(0,0,0,0)` y las cuatro capas del `body`
    llegando al navegador.
  - Build de producción: `.gradient-cta`, `.icon-tile`, `.rule-accent` y
    `.surface-glass` sobreviven a Lightning CSS con sus reglas completas,
    y el `backdrop-filter` se emite (viene de las utilities).
  - Cabecera: `rgba(255,255,255,0.82)` con `blur(24px) saturate(1.5)`.
  - Filtros: tiles con delta **0 px** contra el centro de cada select.
  - Rutas en 200: `/`, `/explorar`, `/login`, `/registro`,
    `/registro/usuario`, `/registro/publicador`, `/deportes`,
    `/ciudades`, `/publicar`, `/actividades/karate`, `/publicadores/8`.

**Falta la confirmación visual con capturas**: el panel del navegador no
estuvo disponible en la sesión, así que todo lo de arriba está medido en
el DOM y en el CSS compilado, no mirado. Antes de pushear conviene
recorrer la app a ojo.

## 4. Fuera de alcance

- **Hero con foto en la home** (el de la referencia, "Tu comunidad
  deportiva en Mar del Plata" sobre una foto de la ciudad): es el salto
  visual más grande que queda, pero necesita un asset y aprobación. Se
  mantiene la regla de cero assets nuevos.
- **Datos sobre la imagen de las cards** (el chip de días y horario de la
  referencia): el DTO de listado **no trae horarios**, solo el de
  detalle. Ponerlo pediría backend, y inventarlo está prohibido.
- Badges tipo "Popular" o "+120 miembros" de las referencias: no salen de
  la base.
- `/mi-cuenta` con sesión real sigue sin verificarse; este bloque le entra
  solo por los cambios globales.

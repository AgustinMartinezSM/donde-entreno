# Modo oscuro V2 — las áreas internas y los estados de error

Estado: **✅ CERRADO EN PRODUCCIÓN** (smoke de Agustín OK, 2026-08-26;
`main` = `origin/main` = **`fab5c67`**).

Cierra la deuda que dejó anotada la Fase 7 en el propio CSS: *"cuando
esas áreas se barran (tanda pendiente), este bloque se borra"*.

## El chequeo que originó el bloque

Se corrió un detector que mide **luminancia y área computadas**, no
clases, sobre las rutas públicas en oscuro. Resultado: **cero sábanas
claras**. El problema estaba en otro lado:

1. **23 de 48 rutas forzaban claro**: `/admin` (8), `/publicador` (14) y
   `/publicar` (1), envueltas en `[data-fuerza-claro]`. Casi la mitad de
   la app, y el panel del publicador **lo usan los clubes**.
2. **Los bloques de error quedaban claros incluso en las áreas
   tematizadas**: `StatusMessage` tenía tokens para `info`, `success` y
   `warning`, pero `error` seguía con `red-*` crudo.

## Bloque A — la familia danger (`a5e6f27`)

La causa era concreta: **a `danger` le faltaban dos tokens**. `warning`
tenía el set completo (color, surface, border); `danger` solo la tinta,
así que la variante no tenía a qué apuntar.

- Los valores **claros son exactamente** los de `red-50` / `red-200` /
  `red-700` medidos en el navegador: el modo claro no se mueve.
  Verificado sobre el bloque de error real de `/login`.
- La superficie oscura **no se eligió a ojo**: `#3A1C1C` daba 1.05 de
  contraste contra el fondo de página y el bloque se perdía. `#452323`
  da 1.17 — la misma banda que info (1.23), success (1.29) y warning
  (1.13) — con la tinta encima en 6.75.

## Bloque B — sacar la luz forzada (`fab5c67`)

Barrido hacia tokens que **ya existían**: 71 de 73 hexes (los 2 que
quedan son una parada de gradiente de marca y un orbe decorativo), 38
`bg-white`, 24 gradientes con `white` crudo y 30 `red-*`.

### Los dos bugs reales que aparecieron

**1. `--color-primary` usado como parada de gradiente.** Ese token **se
invierte** (en oscuro es casi blanco), así que el gradiente quedaba
blanco con texto blanco encima. Estaba en `AdminLoginForm` y **también
en `/publicadores/[id]`, que es pública**.

> El detector no lo vio porque solo miraba `background-color`, y un
> gradiente es `background-image`. **Un detector de sábanas tiene que
> mirar las dos propiedades.**

**2. `--color-error` y `--color-error-soft` NO EXISTÍAN.** Se usaban en
tres lugares desde la Fase 9 (el chip de evento cancelado) y
renderizaban **fondo transparente con tinta heredada**: el chip no se
veía como error en ningún tema. Un token inexistente no falla, no
avisa y no se nota leyendo el código.

> Chequeo nuevo, barato y que hay que repetir: **todo `var(--color-*)`
> referenciado tiene que estar declarado**. Se compara la lista de
> referencias contra la de declaraciones con `comm`.

### Por qué borrar el wrapper no podía romper el claro

No se asumió: el bloque `[data-fuerza-claro]` era un **clon exacto** del
`@theme` claro — 27 tokens y las 2 sombras, **cero diferencias**
verificadas con `comm`. Lo único que además hacía era pintar su propia
base clara, así que esas áreas ahora muestran el fondo ambientado del
body: **el mismo cambio deliberado** que la Fase 7a hizo con los 19
`main`.

## Verificado en producción

- La hoja servida trae la familia danger en los dos temas y **cero
  rastros de `fuerza-claro`**.
- `/publicar` y las públicas en oscuro: sin una sola sábana, con el
  detector ya ampliado a gradientes. En claro, body `#f8fafc` con tinta
  `#102a43` y `color-scheme: light`.
- Guards intactos (307 en las cuatro privadas), públicas en 200,
  consola limpia.

## Lo que cubrió el smoke

`/publicador/*` y `/admin/*` **necesitan sesión y no se pudieron ver
desde acá**. Se intentó el bypass de guards documentado, pero el
dashboard redirige por su cuenta y seguir bypasseando implicaba tocar el
componente bajo prueba; se revirtió (verificado: no quedó ningún
bypass). **Esas 22 rutas las cerró el smoke de Agustín**, que es donde
estaba todo el barrido.

## Lo que queda como regla

- **Un detector de sábanas mira `background-color` Y `background-image`.**
- **Todo `var(--color-*)` referenciado tiene que estar declarado**, y se
  compara con `comm`. Un token inexistente no falla ni avisa.
- **Los tokens que se invierten (`--color-primary`, `--color-text`) no
  van como parada de gradiente**; para eso está `--color-brand`.
- **Nunca un token de borde como color de texto** (regla del pulido
  dark, sigue vigente).

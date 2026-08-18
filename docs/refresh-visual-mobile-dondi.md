# Refresh visual mobile + Dondi

Bloque de frontend visual. No toca backend, base, secretos ni contratos:
cambia cómo se ve y cómo se usa lo que ya existía.

## 1. Diagnóstico de partida

La app funcionaba pero se leía como "cards blancas sobre fondo blanco":

1. **Sin profundidad.** El fondo era `#F8FAFC` plano y las cards blancas;
   sobre una pantalla clara los bordes se perdían y varias tarjetas
   seguidas se leían como una sola mancha.
2. **Los filtros se comían la pantalla.** En `/explorar` el panel abierto
   ocupaba **el 82% del viewport a 390px**, así que los resultados —lo
   que la persona vino a ver— arrancaban abajo del pliegue. Peor: en
   desktop el panel estaba **siempre abierto** (`sm:block`) y el botón de
   Mostrar/Ocultar era `sm:hidden`, o sea que no había forma de cerrarlo.
3. **El asistente no existía fuera de dos pantallas.** Se abría desde la
   barra inferior o desde un botón de la home; en el resto de la app no
   había manera de invocarlo, y cerrado no quedaba ninguna señal de que
   estuviera disponible.
4. **La barra inferior no se sentía de app.** Fondo blanco al 95% sin
   saturación: en mobile leía como una franja, no como una barra de
   vidrio.

## 2. Qué se implementó

### Sistema visual (`globals.css`)

Utilities reutilizables en vez de estilos sueltos repetidos:
`surface-glass`, `surface-soft`, `gradient-brand`, `gradient-deep`,
`shadow-soft`, `shadow-lifted`, `decorative-orb`, `decorative-dots`, más
las animaciones `de-sheet` y `de-halo`.

El `body` pasó a tener dos halos radiales muy suaves —celeste arriba a la
derecha, verde abajo a la izquierda— con `background-attachment: fixed`,
así el scroll no los arrastra. Toda la decoración es CSS: cero assets
nuevos, cero peso de red.

### Dondi

El asistente pasa a tener identidad propia:

- **Avatar** (`DondiAvatar.tsx`): SVG propio de unos cientos de bytes, con
  el gradiente de marca, antena y cara mínima. Escala del launcher (56px)
  a la burbuja (28px) sin perder nitidez. Se descartó descargar un asset.
- **Launcher flotante**: circular, presente en toda la app, con halo que
  late (solo el halo, nunca el botón: si escalara, el objetivo táctil se
  movería bajo el dedo). Va **a la izquierda en mobile** porque la
  derecha ya la ocupa el botón de volver arriba; en desktop pasa a la
  derecha, donde no compite.
- **Panel**: bottom sheet con velo y agarradera en mobile, tarjeta anclada
  en desktop, encabezado con el gradiente de marca.
- **Minimizar (—) y cerrar (X) hacen cosas distintas**, que es lo que
  justifica que sean dos botones: minimizar guarda la charla —el caso real
  es leer una recomendación, ir a mirarla y volver— y el launcher queda
  con un punto verde avisando que hay conversación en curso; cerrar la da
  por terminada y la próxima vez arranca del saludo. `Escape` minimiza,
  nunca cierra: nadie espera perder lo que escribió con `Escape`.

El copy de bienvenida ahora lo presenta por nombre. El CTA de la home
quedó compacto (avatar + "Preguntale a Dondi"), porque el acceso principal
es el launcher.

### Filtros colapsados

Arrancan **siempre cerrados**, incluso con filtros activos en la URL, y el
botón Mostrar/Ocultar existe en todos los anchos. Con el panel cerrado, lo
que se está filtrando se muestra como chips leídos **de la URL** (lo
aplicado) y no del estado de los selects (lo tocado sin aplicar): el
resumen tiene que describir los resultados que se están viendo.

Medido a 390px: **de 688px de alto (82% de la pantalla) a 136px (16%)**.

### Superficies

- **Bottom nav**: vidrio real con blur y saturación, tab activo con
  pastilla verde más un borde interno, porque sobre contenido claro el
  fondo suave solo se pierde.
- **Home**: el encabezado pasó de texto suelto a superficie propia con
  orbe y puntos, para que el buscador —lo más importante de la home— se
  distinga de lo que viene abajo.
- **Explorar**: mismo tratamiento en el hero.
- **Cards de actividad**: degradado muy leve hacia el celeste de marca en
  la parte superior, que corta a blanco antes de la imagen para no teñir
  la foto.
- **Cabecera de `/mi-cuenta`**: usa `gradient-brand` compartido con el
  encabezado del chat, así las dos superficies de marca se leen iguales, y
  suma una trama de puntos.

## 3. Decisiones que vale registrar

**El `backdrop-filter` escrito a mano no sobrevive al build.** Lightning
CSS lo elimina de una regla CSS plana —verificado en el navegador: la
regla llegaba con el `background-color` y sin el filtro— pero sí emite el
que generan las utilities de Tailwind. Por eso `.surface-glass` lleva solo
el color y el desenfoque se aplica con `backdrop-blur-*` /
`backdrop-saturate-*`. Si aparece otra superficie de vidrio, seguir ese
patrón.

**La decoración va en pseudo-elementos**, no en divs vacíos: no ensucia el
DOM, no la lee un lector de pantalla y no captura clicks.

**Nada de datos nuevos ni métricas inventadas.** El bloque es visual: no
se agregaron contadores, ni "+120 miembros", ni badges que no salgan de
la base.

## 4. Fuera de alcance

- Backend, Supabase, env vars, auth, roles y contratos de API: intactos.
- Footer oscuro: se evaluó y se dejó como está; con el fondo ambientado
  nuevo, oscurecerlo además desbalanceaba la página.
- Ilustraciones propias por deporte: hoy las cards caen a una imagen de
  fallback por deporte. Sería el siguiente salto visual, pero necesita
  assets y aprobación.

## 5. Validaciones

- `npm run typecheck`, `npm run lint`, `npm run build` (25 páginas).
- Navegador, con backend local y datos reales:
  - **320 / 390 / 430 / 768 / 1440 px**: sin overflow horizontal y cero
    elementos desbordados en todos.
  - El launcher no pisa la barra inferior en ningún ancho, y en desktop no
    choca con el botón de volver arriba.
  - Dondi: abre, responde ("busco karate" → Karate), minimiza conservando
    la charla, reabre con la charla intacta, cierra y reabre limpio.
  - Filtros: colapsados de entrada con dos filtros en la URL, chips
    correctos ("Mar del Plata", "Karate", "Principiante"), expanden y
    vuelven a colapsar.
  - Rutas: `/`, `/explorar`, `/publicadores/8`, `/actividades/karate`,
    `/login`, `/registro`, `/publicar` en 200; `/mi-cuenta`, `/favoritos`,
    `/publicador`, `/admin/imagenes` redirigen a login con `returnTo`.

## 6. Riesgos y pendientes

- **`/mi-cuenta` con sesión real no se vio.** El cambio ahí es CSS (banda
  de gradiente), pero el layout con datos propios sigue sin verificarse
  con una cuenta de verdad, como el resto de lo que está detrás del guard.
- El velo del sheet es solo mobile: en desktop el panel convive con la
  página, que es lo esperado de un chat anclado.
- El halo del launcher usa `motion-safe`, así que se apaga con
  `prefers-reduced-motion`.

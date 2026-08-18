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

## 7. Pulido post-deploy (bloque aparte, mismo espíritu)

Tres correcciones hechas después de ver el refresh desplegado con
usuarios reales:

**El menú de usuario se veía "lavado" debajo del hero (desktop).** No
era el menú: el header es `sm:static` y su `backdrop-filter` le crea un
stacking context aunque sea estático — pero estático, el `z-40` se
ignora, así que el hero (posterior en el DOM y translúcido) pintaba
encima de todo lo que cuelga del header, incluido el panel `z-50` del
menú, que no puede salir del contexto del padre. Fix: `sm:relative`, un
solo cambio de clase. Verificado con una sonda absoluta y
`elementFromPoint` antes y después.

**El botón "Asistente" duplicado se fue de la navegación.** Con Dondi
flotante en toda la app, la barra inferior y el header ofrecían dos
entradas al mismo panel. La barra inferior queda **Inicio · Explorar ·
Guardados · Mi perfil/Ingresar** (Guardados vuelve: para el visitante va
al login con `returnTo`, como el acceso del header); del header desktop
y del encabezado del publicador el botón se eliminó
(`HeaderAsistenteButton.tsx` borrado). Los CTA contextuales —home y
mi-cuenta— siguen disparando `donde-entreno:abrir-asistente`, que el
widget escucha igual que siempre.

**Dondi ahora invita: burbuja "¿Necesitás ayuda? Escribime".** Vive
junto al launcher: **arriba** en mobile (al costado chocaba con "Volver
arriba" a 320px: llegaba a x290 y el botón arranca en x256) y **al
costado, centrada** en desktop. Tocarla abre el panel; la X la descarta.
El descarte vive en `sessionStorage` (`dondi-burbuja-descartada`) y
abrir el asistente por cualquier camino también la descarta para toda la
sesión: la invitación ya cumplió, y sin eso reaparecería en cada
minimizar. La lectura inicial va con `useSyncExternalStore` (el linter
prohíbe `setState` sincrónico en efectos, y el snapshot de servidor
evita el mismatch de hidratación). Entrada con `de-entrada` bajo
`motion-safe`. El `bottom` va por clase y no por `style`: en desktop hay
que overridearlo con `lg:` y un inline le ganaría.

Verificado a 320/375/390/768/1280: cuatro flotantes (barra, burbuja,
launcher, volver arriba) sin ningún solapamiento ni overflow, burbuja
ausente en `/login` y `/registro*` (rinde con el launcher), y el ciclo
completo abrir → descartar → minimizar → recargar deja la burbuja donde
corresponde.

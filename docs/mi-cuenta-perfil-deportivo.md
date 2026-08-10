# /mi-cuenta como espacio deportivo

Bloque de frontend que convierte `/mi-cuenta` de una pantalla de configuración
en el lugar propio de cada persona dentro de la app: sus deportes, lo que
guardó, a quién sigue y qué le recomendamos.

La ruta no cambia. Lo que cambia es qué es esa pantalla.

## 1. Objetivo

Que quien entra sienta "esta es mi base en DondeEntreno" y no "esta es la
pantalla donde configuro mi cuenta". En concreto: que la primera solapa
siempre tenga algo que mirar, que los deportes elegidos dejen de estar
escondidos, y que la configuración no compita con el contenido.

## 2. Problema del que se parte

La versión anterior ya tenía cabecera y solapas, pero arrastraba cuatro
problemas:

1. **La solapa de entrada estaba vacía para casi todos.** "Novedades" mostraba
   solo el feed de publicadores seguidos y hoy ningún perfil tiene seguidores,
   así que la pantalla abría con un cartel gris.
2. **Los deportes estaban dentro de "Ajustes".** Lo único que personaliza la
   experiencia se llegaba por la solapa de configuración; el número "3
   deportes" de la cabecera navegaba literalmente a `ajustes`.
3. **Acciones duplicadas.** La cabecera repetía `Explorar` (que ya está en el
   header desktop y en la barra inferior mobile) y ofrecía `Publicar actividad`
   con peso de acción principal a cualquier usuario común.
4. **Cerrar sesión competía con todo.** En el header desktop convivían "Hola,
   X", el acceso al perfil y "Cerrar sesión", los tres del mismo tamaño: salir
   pesaba tanto como entrar.

## 3. Qué se implementó

### Cabecera de perfil deportivo

Mantiene la banda de color y el avatar de iniciales —el mismo lenguaje que el
perfil público del publicador— y suma:

- **Línea de identidad deportiva**: ciudad activa + deportes elegidos
  (`Mar del Plata · Jiu Jitsu · Yoga · Pádel`). Es lo más parecido a una bio
  que se puede escribir con datos reales.
- **Tres números clickeables**: deportes, guardadas, siguiendo. Cada uno
  navega a su solapa. Mientras el dato no llegó se muestra un guion, nunca un
  cero falso.
- **Una sola acción, y solo si hace falta**: el botón principal es el próximo
  paso pendiente del perfil ("Elegir deportes", "Ver a quién seguir"…). Cuando
  el perfil está completo, desaparece.
- **Menú de ajustes** en la esquina (engranaje): mis deportes, cambiar ciudad,
  datos de mi cuenta, acceso por rol si corresponde, y cerrar sesión al final,
  después de un separador.

Salieron de la cabecera `Explorar` y `Publicar actividad`.

### Solapas de contenido

`Para vos · Guardados · Siguiendo · Deportes`. "Ajustes" ya no es solapa.

- **Para vos** — Progreso del perfil, novedades de quienes seguís, y si todavía
  no seguís a nadie, a quién seguir. Debajo, recomendaciones armadas con la
  ciudad activa y los deportes elegidos, y el acceso al asistente. En pantallas
  `xl` el contenido va a la izquierda y progreso + asistente a una columna
  lateral sticky.
- **Guardados** — El mismo listado que `/favoritos` (una sola colección). El
  estado vacío suma accesos a los deportes elegidos y al asistente. El aviso de
  "esto vive en el navegador" pasó de cartel a nota al pie.
- **Siguiendo** — Con seguidos, el listado de siempre. Sin seguidos, en vez de
  un cartel azul aislado, los publicadores reales de la plataforma con su botón
  de seguir.
- **Deportes** — Los 27 deportes agrupados por categoría (en una sola pared de
  chips no se encontraba nada en mobile), con `+ Nombre` → `✓ Nombre`,
  `aria-pressed`, accesos rápidos a los elegidos y CTA al asistente.

### Progreso del perfil

Cinco pasos calculados en vivo, sin persistencia nueva: nombre (de la cuenta),
ciudad (activa), deportes, seguir a alguien, guardar una actividad. La tarjeta
desaparece sola cuando están los cinco.

Cuando la cantidad de seguidos todavía no llegó, el paso se da por hecho: es
preferible a marcarle un pendiente a alguien que quizás ya sigue a diez
personas.

### Header global

`HeaderSessionMenu` pasó de tres controles sueltos a un solo botón con avatar
que abre un menú: mi perfil deportivo, actividades guardadas, acceso por rol si
corresponde, y cerrar sesión al final. El acceso por rol dejó de ocupar lugar
para el usuario común, que no tiene ninguno.

## 4. Decisiones que vale la pena registrar

**No hay botón "Editar perfil".** No existe endpoint para editar el perfil de
usuario: los únicos `/api/usuario/**` que consume el frontend son seguimientos
y feed. Un botón que abriera un formulario que no guarda sería peor que no
tenerlo. En su lugar, la acción principal es el próximo paso real y los datos
de cuenta se muestran en un diálogo de solo lectura que lo dice.

**Las recomendaciones no son un modelo.** Son la búsqueda pública real
(`GET /api/actividades`) filtrada por la ciudad activa y hasta tres de los
deportes elegidos, intercalando resultados para que la primera fila no sea toda
del mismo deporte, y descartando lo que ya está guardado. El encabezado dice de
dónde salen. Sin deportes elegidos, muestra lo último de la ciudad.

**Los menús no usan `role="menu"`.** Esa semántica promete navegación por
flechas y foco gestionado, que no implementamos. Con links y botones reales el
Tab recorre las opciones como en cualquier página. Es el mismo criterio que ya
usaban las solapas.

**El diálogo de datos usa `<dialog>` nativo con `showModal()`**: contención de
foco, cierre con Escape y fondo inerte sin implementarlos a mano.

**La cabecera no lleva `overflow-hidden`.** El redondeo va en la banda de
color: con `overflow-hidden` en la tarjeta, el panel del menú de ajustes
quedaba cortado por el borde.

## 5. Fuera de alcance (y por qué)

| Pendiente | Motivo |
|---|---|
| Bio editable | Sin endpoint de perfil de usuario |
| Foto de perfil | Sin endpoint ni storage para avatar de usuario |
| Editar nombre / email | Sin endpoint; hoy es lectura y se avisa |
| Colecciones de guardados | Sin backend |
| Sincronizar favoritos con la cuenta | Hoy viven en `localStorage` del dispositivo |
| Actividad reciente ("guardaste X", "seguiste a Y") | Requiere persistencia nueva |
| Objetivos / intensidad / disponibilidad / distancia | Requiere backend de preferencias |
| Notificaciones | Requiere backend |
| Perfil público de usuario | `/mi-cuenta` es privado por ahora |

Nada de esto se simuló ni se dejó a medias en la UI.

## 6. Validaciones

- `npm run typecheck`, `npm run lint`, `npm run build` (34 rutas).
- Verificado en el navegador contra el backend local (solo lecturas, datos
  reales de Supabase): recomendaciones con deportes elegidos y sin ellos,
  guardados vacío y con datos, siguiendo vacío, menú de ajustes, diálogo de
  datos, y el cambio de la acción principal según el paso pendiente.
- Responsive sin overflow horizontal en 320, 375, 390, 768 y 1440 px; solapas
  de 86×48 px y números de 95×70 px a 375 px; dos columnas a partir de `xl`.
- La sesión se simuló con un bypass temporal en código, revertido después. El
  **smoke autenticado real queda pendiente**: feed con datos, listado de
  seguidos con datos, y dejar de seguir desde la solapa.

## 7. Riesgos

- Los estados con datos reales de feed y seguidos no se pudieron probar con una
  sesión de verdad (ver arriba).
- Las recomendaciones hacen hasta tres requests en paralelo (uno por deporte).
  Con el tope actual de tres deportes consultados es acotado; si el tope sube,
  conviene un endpoint que acepte varios deportes.
- `MisFavoritos` ahora se comparte entre `/favoritos` y la solapa Guardados: un
  cambio ahí impacta en las dos.

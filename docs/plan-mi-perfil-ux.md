# Plan — "Mi perfil" intuitivo (desktop y mobile)

> **Para el chat que tome este bloque**: este documento es el punto de
> partida completo. Leer también CLAUDE.md (reglas y decisiones
> cerradas) y `docs/mi-cuenta-perfil-deportivo.md` (el diseño vigente de
> `/mi-cuenta`). El disparador fue un hallazgo real del smoke del
> 2026-08-19, abajo. **Antes de codear: proponer el diseño elegido a
> Agustín** — este doc deja opciones con recomendación, no un diseño
> cerrado.

## 1. El hallazgo que dispara el bloque (Agustín, smoke 2026-08-19)

En **desktop**, el avatar del header despliega un menú con TODO: "Mi
perfil deportivo" (`/mi-cuenta`), "Actividades guardadas"
(`/favoritos`), "Mi espacio de publicador" (`/publicador`) y "Cerrar
sesión".

En **mobile no existe ese menú**: el avatar del header y el ítem "Mi
perfil" de la barra inferior navegan DIRECTO a un único destino por rol.
Un publicador cae siempre en `/publicador` y **no tiene ninguna forma de
llegar a su perfil deportivo, a sus guardadas como usuario, ni de elegir
a dónde ir**. Capturas del hallazgo: el menú desktop completo vs. el
salto directo mobile a `/publicador`.

## 2. Mapa actual de entradas y destinos (verificado en código)

| Entrada | Dónde vive | Visitante | USUARIO | PUBLICADOR | ADMIN |
|---|---|---|---|---|---|
| Menú del avatar (desktop, `HeaderSessionMenu`) | header ≥lg | botón "Iniciar sesión" | menú: mi-cuenta / favoritos / — / logout | menú: mi-cuenta / favoritos / **/publicador** / logout | menú: mi-cuenta / favoritos / **/admin/solicitudes** / logout |
| Avatar del header (mobile, `MobileAccountShortcut`) | header <lg | link a /login | **directo** a /mi-cuenta | **directo** a /publicador | **directo** a /admin/solicitudes |
| "Mi perfil" barra inferior (`MobileNavigation`) | <lg | "Ingresar" → login | /mi-cuenta | **/publicador** | **/admin/solicitudes** |
| "Guardados" barra inferior | <lg | login con returnTo | /favoritos | /favoritos | /favoritos |
| "Guardados" header (`HeaderFavoritosLink`) | ≥lg | login con returnTo | /favoritos | /favoritos | /favoritos |

La función que decide es `lib/authRedirects.ts` →
`obtenerRutaInicialPorRol`: ADMIN → `/admin/solicitudes`, PUBLICADOR →
`/publicador`, USUARIO → `/mi-cuenta`. Mobile la usa como destino único
(`MobileAccountShortcut.tsx:38`, `MobileNavigation.tsx` destinoCuenta);
desktop la usa solo como UNA opción del menú.

## 3. Problemas, priorizados

1. **P1 — Publicador/admin mobile sin acceso a su lado "persona"**: no
   pueden llegar a `/mi-cuenta` (Para vos / Guardados / Siguiendo /
   Deportes) desde ningún lugar de la UI mobile. Un publicador TAMBIÉN
   es usuario: sigue clubes, guarda actividades, tiene deportes — y con
   el sync (`fcc4fa5`) ese lado vale más que antes.
2. **P1 — "Mi perfil" significa cosas distintas según el rol**: mismo
   label, destinos diferentes. El modelo mental no se puede formar.
3. **P2 — Dos "casas" sin puente claro**: `/mi-cuenta` (lado social) y
   `/publicador` (lado gestión) no se enlazan entre sí en mobile; en
   desktop solo vía el menú. Dentro de `/publicador` el único camino de
   vuelta es "Ver el sitio".
4. **P2 — Asimetría desktop/mobile**: desktop tiene menú con opciones;
   mobile tiene saltos directos. Lo aprendido en un dispositivo no sirve
   en el otro.
5. **P3 — Duplicación Guardados**: `/favoritos` (página) y la solapa
   Guardados de `/mi-cuenta` muestran lo mismo por caminos distintos.

## 4. Direcciones posibles (con recomendación)

**Opción A — Menú de cuenta también en mobile (bottom sheet).**
Tocar el avatar o "Mi perfil" abre un sheet con las mismas opciones del
menú desktop (perfil deportivo, guardadas, espacio publicador/admin si
corresponde, cerrar sesión). Paridad total, cambio acotado.
*Contra*: un tap más para llegar a todo; el sheet es otro overlay a
cruzar con Dondi/volver-arriba (ver memoria de flotantes).

**Opción B — `/mi-cuenta` como única casa, el rol como sección.**
"Mi perfil" lleva SIEMPRE a `/mi-cuenta` (todos los roles). Para
publicador/admin, la cabecera de `/mi-cuenta` suma una tarjeta/acceso
prominente "Tu espacio de publicador" / "Administración". El panel
`/publicador` queda como está (con su header propio), pero deja de ser
el destino del avatar.
*Pro*: un solo significado para "Mi perfil"; el lado persona del
publicador deja de ser inaccesible; cero overlays nuevos.
*Contra*: el publicador que entra 10 veces por día a gestionar gana un
tap; puede mitigarse recordando el último espacio visitado o con la
tarjeta bien arriba.

**Opción C — Híbrida (recomendada).**
B como base (una casa, un significado) + un **conmutador de espacio**
persistente para roles con dos mundos: en `/mi-cuenta` una tarjeta
"Ir a tu espacio de publicador" arriba de todo, y en `/publicador` el
espejo ("Tu perfil deportivo") en su header. En desktop el menú del
avatar queda como está (ya funciona). En mobile, el avatar del header
puede seguir siendo salto directo pero AL MISMO lugar que "Mi perfil"
(/mi-cuenta), nunca a un lugar distinto.
*Es la que menos piezas nuevas agrega y cierra P1–P4; P5 se decide
aparte (ver §6).*

## 5. Restricciones y decisiones que NO se re-litigan

- Todo frontend; sin tocar backend, contratos ni auth (los guards y
  roles quedan como están — esto es navegación, no permisos).
- "Panel" no vuelve: el lenguaje es "Mi perfil" / "Mi espacio de
  publicador"; el área admin se llama "Administración" a propósito.
- Dondi, filtros colapsados, barra inferior de 4 ítems (Inicio ·
  Explorar · Guardados · Mi perfil/Ingresar): decisiones cerradas.
- Cruzar TODOS los flotantes si se agrega un sheet (memoria
  `verificar-elementos-flotantes`): elementFromPoint sobre lo tapado,
  mobile y desktop, con scroll.
- El smoke autenticado de producción lo hace Agustín.

## 6. Preguntas abiertas para decidir con Agustín antes de codear

1. ¿Opción A, B o C? (recomendación: C)
2. Para el publicador frecuente: ¿recordar el último espacio y abrir
   ahí, o siempre `/mi-cuenta`? (recomendación: siempre `/mi-cuenta` en
   V1 — predecible gana a rápido hasta medir)
3. ¿`/favoritos` se fusiona con la solapa Guardados de `/mi-cuenta`
   (redirect) o siguen coexistiendo? (la barra inferior hoy apunta a
   `/favoritos`)
4. ¿El admin entra en este bloque o queda con su salto directo? (el
   admin es interno; puede quedar fuera del V1)

## 7. Matriz de verificación del bloque (cuando se implemente)

Por CADA celda: rol (usuario / publicador / admin / visitante) ×
dispositivo (390px / 1280px) × entrada (avatar header / ítem barra /
menú desktop):

- Se llega a: perfil deportivo, guardadas, espacio de rol (si aplica),
  cerrar sesión — sin callejones sin salida.
- "Mi perfil" significa LO MISMO en todas las entradas del mismo
  dispositivo.
- Desde `/publicador` se vuelve al lado social sin usar "Ver el sitio".
- Sin overlays pisándose (matriz de flotantes completa si hay sheet).
- typecheck + lint + build; commit local; push solo con aprobación.

## 8. Fuera de alcance

- Permisos/roles del backend, perfil público de usuario, editar perfil
  (sin endpoint), colecciones — backlog de `docs/mi-cuenta-perfil-deportivo.md` §5.
- Cambios al panel interno de administración más allá del punto 4 de §6.

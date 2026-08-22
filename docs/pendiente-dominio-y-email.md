# Pendiente futuro — Dominio propio e infraestructura de email

**Decisión de producto de Agustín (2026-08-21): EN PAUSA.** No es un
descarte: es un "todavía no".

## El motivo (en sus palabras)

DondeEntreno necesita más madurez visual, funcional y de experiencia
antes de consolidarse como marca final con dominio propio, mails
transaccionales y configuración DNS. Primero producto y UX; la
infraestructura externa se abre después.

## Qué queda en pausa (la lista completa)

- Registrar/configurar el dominio propio.
- Cloudflare/DNS.
- Frontend en dominio propio.
- API en subdominio propio.
- Proveedor de email transaccional.
- Verificación de email (fase 5b del bloque visual).
- Recuperación de contraseña / "me olvidé mi contraseña" (fase 5c).
- Reset por link.
- Emails automáticos.
- DNS SPF/DKIM/DMARC.
- Variables de entorno para email.

## Lo que este bloque desbloquearía cuando se abra

- **5b/5c del bloque visual**: el diagnóstico ya está hecho
  (`docs/bloque-contenido-visual-v1.md` §4 BIS) — `email_verificado`
  existe y YA gatea el login vía `isEnabled()` (el registro lo fuerza a
  true), así que la verificación real arranca por ahí. El gate #2 del
  bloque (verificación estricta vs laxa por rol) se decide recién
  entonces.
- **La nota de localStorage del refresh token**: el plan
  (`docs/plan-refresh-token.md`) dejó escrito que con dominio propio se
  reevalúa cookie HttpOnly/BFF — Vercel y Render dejarían de ser
  dominios de terceros entre sí.
- `NEXT_PUBLIC_SITE_URL`, sitemap/robots/OG y los orígenes de CORS
  cambian de valor al migrar de dominio (checklist para ese día, no
  antes).

## Regla operativa mientras dure la pausa

Ningún trabajo de este listado se arranca sin pedido explícito de
Agustín. Si alguna fase futura pareciera necesitar email saliente,
frenarla y proponer la alternativa sin email (como se hizo con 5a:
cambio de contraseña logueado sin proveedor).

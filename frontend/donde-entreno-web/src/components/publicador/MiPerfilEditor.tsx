"use client";

import { formatearEtiquetaCatalogo as formatearCatalogo } from "../../lib/formatoCatalogo";
import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import type { FormEvent } from "react";

import { useAuthSession } from "../auth/AuthSessionProvider";
import { AppButton } from "../ui/AppButton";
import { AppLinkButton } from "../ui/AppLinkButton";
import { SectionHeader } from "../ui/SectionHeader";
import { StatusMessage } from "../ui/StatusMessage";
import { SurfaceCard } from "../ui/SurfaceCard";
import { GestionImagenesPerfil } from "./GestionImagenesPerfil";
import {
  ChecklistPresencia,
  usePresenciaPublicador,
} from "./ChecklistPresencia";
import { PublicadorPageHeader } from "./PublicadorPageHeader";
import {
  PublicadorApiError,
  actualizarPerfilPublicador,
  obtenerPerfilPublicador,
} from "../../services/publicadorService";
import type { PerfilPublicadorActual } from "../../types/publicador";
import type { AuthErroresPorCampo } from "../../types/auth";

const CLASE_INPUT =
  "mt-2 min-h-12 w-full rounded-[18px] border border-[var(--color-border-accent)] bg-[var(--color-bg)] px-4 text-base text-[var(--color-text)] outline-none transition duration-200 ease-out hover:border-[var(--color-accent)] focus:border-[var(--color-accent)] focus:ring-4 focus:ring-[var(--color-border-soft)] disabled:cursor-not-allowed disabled:opacity-70";

/*
  Edición del perfil publicador (V1 + fase 5e).

  Campos de edición directa de PATCH /api/publicador/me: nombre público
  (5e, directo por decisión de Agustín — la descripción ya lo era y es
  texto más riesgoso), descripción, Instagram y email de contacto. Tipo,
  ciudad, WhatsApp/teléfono y estado siguen en solo lectura.
*/
export function MiPerfilEditor() {
  const router = useRouter();
  const { accessToken, cerrarSesion } = useAuthSession();

  const [perfil, setPerfil] = useState<PerfilPublicadorActual | null>(null);
  const [cargando, setCargando] = useState(true);
  const [errorCarga, setErrorCarga] = useState<string | null>(null);

  /* El checklist compartido con el Centro de fotos (best-effort). */
  const pasosPresencia = usePresenciaPublicador(accessToken);

  const [nombre, setNombre] = useState("");
  const [descripcion, setDescripcion] = useState("");
  const [instagram, setInstagram] = useState("");
  const [emailContacto, setEmailContacto] = useState("");

  const [guardando, setGuardando] = useState(false);
  const [guardadoOk, setGuardadoOk] = useState(false);
  const [errorGuardado, setErrorGuardado] = useState<string | null>(null);
  const [erroresPorCampo, setErroresPorCampo] =
    useState<AuthErroresPorCampo | null>(null);

  useEffect(() => {
    let componenteActivo = true;

    if (!accessToken) {
      return () => {
        componenteActivo = false;
      };
    }

    obtenerPerfilPublicador(accessToken)
      .then((perfilActual) => {
        if (!componenteActivo) {
          return;
        }

        setPerfil(perfilActual);
        setNombre(perfilActual.nombre ?? "");
        setDescripcion(perfilActual.descripcion ?? "");
        setInstagram(perfilActual.instagram ?? "");
        setEmailContacto(perfilActual.emailContacto ?? "");
        setErrorCarga(null);
      })
      .catch((error: unknown) => {
        if (!componenteActivo) {
          return;
        }

        if (error instanceof PublicadorApiError) {
          if (error.status === 401) {
            cerrarSesion();
            router.replace(
              `/login?returnTo=${encodeURIComponent("/publicador/perfil")}`
            );
            return;
          }

          setErrorCarga(error.message);
          return;
        }

        setErrorCarga("Ocurrió un problema inesperado al cargar tu perfil.");
      })
      .finally(() => {
        if (componenteActivo) {
          setCargando(false);
        }
      });

    return () => {
      componenteActivo = false;
    };
  }, [accessToken, cerrarSesion, router]);

  async function manejarEnvio(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault();

    if (guardando || !accessToken) {
      return;
    }

    /* El nombre es obligatorio: mejor frenarlo acá que con el 400. */
    if (!nombre.trim()) {
      setErrorGuardado("El nombre público no puede quedar vacío.");
      return;
    }

    setGuardando(true);
    setGuardadoOk(false);
    setErrorGuardado(null);
    setErroresPorCampo(null);

    try {
      /*
        Mandamos siempre los tres campos: el backend interpreta vacío
        como "limpiar", así el formulario refleja exactamente lo que
        queda guardado.
      */
      const perfilActualizado = await actualizarPerfilPublicador(
        {
          nombre: nombre.trim(),
          descripcion: descripcion.trim(),
          instagram: instagram.trim(),
          emailContacto: emailContacto.trim(),
        },
        accessToken
      );

      setPerfil(perfilActualizado);
      setNombre(perfilActualizado.nombre ?? "");
      setDescripcion(perfilActualizado.descripcion ?? "");
      setInstagram(perfilActualizado.instagram ?? "");
      setEmailContacto(perfilActualizado.emailContacto ?? "");
      setGuardadoOk(true);
    } catch (error: unknown) {
      if (error instanceof PublicadorApiError) {
        if (error.status === 401) {
          cerrarSesion();
          router.replace(
            `/login?returnTo=${encodeURIComponent("/publicador/perfil")}`
          );
          return;
        }

        setErrorGuardado(error.message);
        setErroresPorCampo(error.erroresPorCampo);
        return;
      }

      setErrorGuardado("No pudimos guardar los cambios. Probá nuevamente.");
    } finally {
      setGuardando(false);
    }
  }

  const errorNombre = erroresPorCampo?.nombre ?? null;
  const errorDescripcion = erroresPorCampo?.descripcion ?? null;
  const errorInstagram = erroresPorCampo?.instagram ?? null;
  const errorEmail = erroresPorCampo?.emailContacto ?? null;

  return (
    <main className="min-h-screen px-4 py-8 text-[var(--color-text)] sm:py-12">
      <section className="mx-auto w-full max-w-6xl">
        <PublicadorPageHeader
          title="Mi perfil publicador"
          description="Mantené actualizada la información que ven las personas cuando publicás actividades."
          action={
            <div className="grid gap-2">
              {/*
                Preview del perfil PUBLICADO (lo pendiente de moderación
                todavía no se ve ahí). Solo con perfil activo: la ruta
                pública devuelve 404 para inactivos.
              */}
              {perfil?.activo ? (
                <AppLinkButton
                  href={`/publicadores/${perfil.id}`}
                  variant="outline"
                  fullWidth
                >
                  Ver mi perfil público
                </AppLinkButton>
              ) : null}
              <AppLinkButton href="/publicador" variant="secondary" fullWidth>
                Volver al panel
              </AppLinkButton>
            </div>
          }
        />

        {cargando ? (
          <StatusMessage variant="info" role="status" className="mt-6">
            Cargando tu perfil publicador...
          </StatusMessage>
        ) : null}

        {errorCarga ? (
          <StatusMessage variant="error" role="alert" className="mt-6">
            {errorCarga}
          </StatusMessage>
        ) : null}

        {perfil ? (
          <div className="mt-6 grid gap-5 lg:grid-cols-[1.1fr_0.9fr]">
            <SurfaceCard className="p-6 sm:p-8">
              <SectionHeader
                eyebrow="Edición directa"
                title="Datos que podés actualizar"
                description="Estos cambios se aplican al instante en tu perfil."
              />

              <form className="mt-6 flex flex-col gap-5" onSubmit={manejarEnvio}>
                <div>
                  <label
                    htmlFor="perfil-nombre"
                    className="text-sm font-bold text-[var(--color-primary)]"
                  >
                    Nombre público
                  </label>
                  <input
                    id="perfil-nombre"
                    type="text"
                    maxLength={150}
                    value={nombre}
                    onChange={(evento) => {
                      setNombre(evento.target.value);
                      setGuardadoOk(false);
                      setErrorGuardado(null);
                      setErroresPorCampo(null);
                    }}
                    disabled={guardando}
                    aria-invalid={Boolean(errorNombre)}
                    aria-describedby={
                      errorNombre ? "perfil-nombre-error" : "perfil-nombre-ayuda"
                    }
                    className={CLASE_INPUT}
                  />
                  <p
                    id="perfil-nombre-ayuda"
                    className="mt-2 text-xs leading-5 text-[var(--color-muted)]"
                  >
                    Es el nombre que se ve en tu perfil público y en todas
                    tus actividades.
                  </p>
                  {errorNombre ? (
                    <p
                      id="perfil-nombre-error"
                      className="mt-2 text-sm font-bold text-[var(--color-danger)]"
                    >
                      {errorNombre}
                    </p>
                  ) : null}
                </div>

                <div>
                  <label
                    htmlFor="perfil-descripcion"
                    className="text-sm font-bold text-[var(--color-primary)]"
                  >
                    Descripción
                  </label>
                  <textarea
                    id="perfil-descripcion"
                    rows={4}
                    maxLength={2000}
                    value={descripcion}
                    onChange={(evento) => {
                      setDescripcion(evento.target.value);
                      setGuardadoOk(false);
                      setErroresPorCampo(null);
                    }}
                    disabled={guardando}
                    aria-invalid={Boolean(errorDescripcion)}
                    aria-describedby={
                      errorDescripcion ? "perfil-descripcion-error" : undefined
                    }
                    placeholder="Contá quién sos, qué actividades ofrecés y qué hace especial a tu espacio."
                    className={`${CLASE_INPUT} min-h-28 py-3 leading-6`}
                  />
                  {errorDescripcion ? (
                    <p
                      id="perfil-descripcion-error"
                      className="mt-2 text-sm font-bold text-[var(--color-danger)]"
                    >
                      {errorDescripcion}
                    </p>
                  ) : null}
                </div>

                <div>
                  <label
                    htmlFor="perfil-instagram"
                    className="text-sm font-bold text-[var(--color-primary)]"
                  >
                    Instagram
                  </label>
                  <input
                    id="perfil-instagram"
                    type="text"
                    maxLength={150}
                    value={instagram}
                    onChange={(evento) => {
                      setInstagram(evento.target.value);
                      setGuardadoOk(false);
                      setErroresPorCampo(null);
                    }}
                    disabled={guardando}
                    aria-invalid={Boolean(errorInstagram)}
                    aria-describedby={
                      errorInstagram ? "perfil-instagram-error" : undefined
                    }
                    placeholder="@tuclub"
                    className={CLASE_INPUT}
                  />
                  {errorInstagram ? (
                    <p
                      id="perfil-instagram-error"
                      className="mt-2 text-sm font-bold text-[var(--color-danger)]"
                    >
                      {errorInstagram}
                    </p>
                  ) : null}
                </div>

                <div>
                  <label
                    htmlFor="perfil-email"
                    className="text-sm font-bold text-[var(--color-primary)]"
                  >
                    Email de contacto
                  </label>
                  <input
                    id="perfil-email"
                    type="email"
                    maxLength={150}
                    value={emailContacto}
                    onChange={(evento) => {
                      setEmailContacto(evento.target.value);
                      setGuardadoOk(false);
                      setErroresPorCampo(null);
                    }}
                    disabled={guardando}
                    aria-invalid={Boolean(errorEmail)}
                    aria-describedby={
                      errorEmail ? "perfil-email-error" : undefined
                    }
                    placeholder="contacto@tuclub.com"
                    className={CLASE_INPUT}
                  />
                  {errorEmail ? (
                    <p
                      id="perfil-email-error"
                      className="mt-2 text-sm font-bold text-[var(--color-danger)]"
                    >
                      {errorEmail}
                    </p>
                  ) : null}
                </div>

                {errorGuardado ? (
                  <StatusMessage variant="error" role="alert">
                    {errorGuardado}
                  </StatusMessage>
                ) : null}

                {guardadoOk ? (
                  <StatusMessage variant="success" role="status">
                    Perfil actualizado correctamente.
                  </StatusMessage>
                ) : null}

                <AppButton type="submit" disabled={guardando} fullWidth>
                  {guardando ? "Guardando..." : "Guardar cambios"}
                </AppButton>
              </form>
            </SurfaceCard>

            <SurfaceCard variant="info" className="h-fit p-6 sm:p-8">
              <SectionHeader
                eyebrow="Con revisión"
                title="Datos protegidos"
                description="Estos datos definen tu identidad pública: próximamente vas a poder pedir cambios y el equipo los revisa antes de aplicarlos."
              />

              <dl className="mt-6 grid gap-3">
                <DatoProtegido
                  etiqueta="Tipo de publicador"
                  valor={formatearCatalogo(perfil.tipoPublicador)}
                />
                <DatoProtegido
                  etiqueta="Ciudad principal"
                  valor={perfil.ciudadPrincipalNombre}
                />
                <DatoProtegido etiqueta="WhatsApp" valor={perfil.whatsapp} />
                <DatoProtegido
                  etiqueta="Teléfono"
                  valor={perfil.telefonoContacto}
                />
                <DatoProtegido
                  etiqueta="Estado del perfil"
                  valor={formatearCatalogo(perfil.estado)}
                />
              </dl>
            </SurfaceCard>

            {/*
              Identidad visual del perfil: ocupa el ancho completo porque
              la portada es una banda ancha y no entra bien en una
              columna angosta.
            */}
            <div className="lg:col-span-2">
              <GestionImagenesPerfil />
            </div>

            {/*
              El mismo checklist del Centro de fotos, acá: cuatro de
              sus seis pasos se resuelven EN esta pantalla, así que era
              el lugar donde más falta hacía. Si los datos no cargan
              completos no se dibuja: un "2 de 6" falso sería peor que
              no mostrar nada.
            */}
            {pasosPresencia ? (
              <SurfaceCard
                as="section"
                variant="info"
                className="p-5 sm:p-6 lg:col-span-2"
              >
                <p className="text-xs font-extrabold uppercase tracking-[0.16em] text-[var(--color-info-deep)]">
                  Recomendaciones
                </p>

                <ChecklistPresencia pasos={pasosPresencia} />

                <p className="mt-5 border-t border-[var(--color-border-accent)] pt-4 text-sm leading-6 text-[var(--color-muted)]">
                  Un perfil completo genera más confianza y ayuda a que las
                  personas decidan dónde entrenar.
                </p>
              </SurfaceCard>
            ) : null}
          </div>
        ) : null}
      </section>
    </main>
  );
}

function DatoProtegido({
  etiqueta,
  valor,
}: {
  etiqueta: string;
  valor: string | null;
}) {
  return (
    <div className="rounded-[18px] border border-[var(--color-border-soft)] bg-[var(--color-surface)]/80 p-4">
      <dt className="text-xs font-extrabold uppercase tracking-[0.14em] text-[var(--color-muted)]">
        {etiqueta}
      </dt>
      <dd className="mt-1.5 break-words text-sm font-bold text-[var(--color-primary)]">
        {valor || "No informado"}
      </dd>
    </div>
  );
}

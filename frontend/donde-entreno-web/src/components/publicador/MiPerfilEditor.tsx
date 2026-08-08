"use client";

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
import { PublicadorPageHeader } from "./PublicadorPageHeader";
import {
  PublicadorApiError,
  actualizarPerfilPublicador,
  obtenerPerfilPublicador,
} from "../../services/publicadorService";
import type { PerfilPublicadorActual } from "../../types/publicador";
import type { AuthErroresPorCampo } from "../../types/auth";

const CLASE_INPUT =
  "mt-2 min-h-12 w-full rounded-[18px] border border-[#BFDDEA] bg-[#F8FAFC] px-4 text-base text-[var(--color-text)] outline-none transition duration-200 ease-out hover:border-[var(--color-accent)] focus:border-[var(--color-accent)] focus:ring-4 focus:ring-[#DDEAF3] disabled:cursor-not-allowed disabled:opacity-70";

/*
  Edición del perfil publicador (V1).

  Solo permite editar los campos de edición directa que expone
  PATCH /api/publicador/me: descripción, Instagram y email de contacto.
  Los campos sensibles (nombre público, tipo, ciudad, WhatsApp/teléfono
  y estado) se muestran en solo lectura: van a editarse más adelante
  mediante un flujo con revisión del equipo.
*/
export function MiPerfilEditor() {
  const router = useRouter();
  const { accessToken, cerrarSesion } = useAuthSession();

  const [perfil, setPerfil] = useState<PerfilPublicadorActual | null>(null);
  const [cargando, setCargando] = useState(true);
  const [errorCarga, setErrorCarga] = useState<string | null>(null);

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
          descripcion: descripcion.trim(),
          instagram: instagram.trim(),
          emailContacto: emailContacto.trim(),
        },
        accessToken
      );

      setPerfil(perfilActualizado);
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

  const errorDescripcion = erroresPorCampo?.descripcion ?? null;
  const errorInstagram = erroresPorCampo?.instagram ?? null;
  const errorEmail = erroresPorCampo?.emailContacto ?? null;

  return (
    <main className="min-h-screen bg-gradient-to-br from-[#F8FAFC] via-white to-[#E8F6FB] px-4 py-8 text-[var(--color-text)] sm:py-12">
      <section className="mx-auto w-full max-w-6xl">
        <PublicadorPageHeader
          title="Mi perfil publicador"
          description="Mantené actualizada la información que ven las personas cuando publicás actividades."
          action={
            <AppLinkButton href="/publicador" variant="secondary" fullWidth>
              Volver al panel
            </AppLinkButton>
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
                      className="mt-2 text-sm font-bold text-red-700"
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
                      className="mt-2 text-sm font-bold text-red-700"
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
                      className="mt-2 text-sm font-bold text-red-700"
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
                <DatoProtegido etiqueta="Nombre público" valor={perfil.nombre} />
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
    <div className="rounded-[18px] border border-[#DDEAF3] bg-white/80 p-4">
      <dt className="text-xs font-extrabold uppercase tracking-[0.14em] text-[var(--color-muted)]">
        {etiqueta}
      </dt>
      <dd className="mt-1.5 break-words text-sm font-bold text-[var(--color-primary)]">
        {valor || "No informado"}
      </dd>
    </div>
  );
}

function formatearCatalogo(valor: string): string {
  return valor
    .toLowerCase()
    .split("_")
    .map((parte) => parte.charAt(0).toUpperCase() + parte.slice(1))
    .join(" ");
}

"use client";

import Image from "next/image";
import { useState } from "react";

import type { UsuarioActual } from "../../types/auth";
import { AppButton } from "../ui/AppButton";
import { AppLinkButton } from "../ui/AppLinkButton";
import { SurfaceCard } from "../ui/SurfaceCard";
import { DialogoAvatar } from "./DialogoAvatar";
import { MenuAjustes } from "./MenuAjustes";
import type { PerfilDeportivo, TabPerfil } from "./usePerfilDeportivo";

type CabeceraPerfilProps = {
  perfil: PerfilDeportivo;
  usuario: UsuarioActual | null;
  rol: string | null;
  tabActiva: TabPerfil;
  onIrATab: (tab: TabPerfil) => void;
  onCerrarSesion: () => void;
};

/* Cuántos deportes entran en la línea de identidad antes de resumir. */
const DEPORTES_VISIBLES = 3;

/*
  Cabecera de "mi espacio": identidad deportiva, no configuración.

  Antes acá vivían dos botones —Explorar y Publicar actividad— que ya
  están en la navegación global y que además le proponían publicar a
  alguien que solo entrena. En su lugar quedó lo que sí es de esta
  persona: quién es, dónde entrena, qué le interesa y sus números. La
  única acción que aparece es la del paso que le falta, y desaparece
  cuando el perfil está completo.

  Los ajustes (ciudad, datos, cerrar sesión) viven en el menú de la
  esquina: están a un toque, pero no compiten con el contenido.
*/
export function CabeceraPerfil({
  perfil,
  usuario,
  rol,
  tabActiva,
  onIrATab,
  onCerrarSesion,
}: CabeceraPerfilProps) {
  const deportesVisibles = perfil.deportesNombres.slice(0, DEPORTES_VISIBLES);
  const deportesRestantes =
    perfil.deportesNombres.length - deportesVisibles.length;

  const [avatarAbierto, setAvatarAbierto] = useState(false);
  /*
    La foto recién cambiada se refleja al instante desde acá; en la
    próxima carga viene del backend por /api/auth/me. undefined = todavía
    no hubo cambio en esta vista.
  */
  const [avatarLocal, setAvatarLocal] = useState<string | null | undefined>(
    undefined
  );
  const avatarUrl =
    avatarLocal !== undefined ? avatarLocal : (usuario?.avatarUrl ?? null);

  return (
    <SurfaceCard as="section">
      {/*
        El redondeo va en la banda y no como overflow-hidden en la card:
        con overflow-hidden, el panel del menú de ajustes quedaba cortado
        por el borde de la tarjeta.
      */}
      {/*
        La banda usa el gradiente de marca compartido en vez de repetir
        los tres colores acá: es el mismo que encabeza el chat de Dondi,
        así las dos superficies "de marca" de la app se leen iguales.
      */}
      <div className="relative h-24 sm:h-28">
        <div
          aria-hidden="true"
          className="gradient-brand absolute inset-0 rounded-t-[var(--radius-xl)]"
        />
        <div
          aria-hidden="true"
          className="absolute inset-0 rounded-t-[var(--radius-xl)] opacity-40"
          style={{
            backgroundImage:
              "radial-gradient(rgba(255,255,255,0.5) 1px, transparent 1px)",
            backgroundSize: "0.9rem 0.9rem",
          }}
        />

        <div className="absolute right-4 top-4 sm:right-5">
          <MenuAjustes
            usuario={usuario}
            rol={rol}
            onIrADeportes={() => onIrATab("deportes")}
            onCerrarSesion={onCerrarSesion}
          />
        </div>
      </div>

      <div className="px-5 pb-6 sm:px-8 sm:pb-8">
        <div className="flex flex-col gap-5 sm:flex-row sm:items-end sm:justify-between sm:gap-6">
          <div className="flex min-w-0 items-end gap-4">
            {/*
              relative + z-10: la banda de arriba es un elemento
              posicionado y sin esto le comía la mitad al avatar.
            */}
            <span className="relative z-10 -mt-12 block h-20 w-20 shrink-0 sm:-mt-16 sm:h-28 sm:w-28">
              <span
                aria-hidden="true"
                className="flex h-full w-full items-center justify-center overflow-hidden rounded-full bg-[var(--color-brand)] text-xl font-extrabold tracking-[0.08em] text-white shadow-[0_10px_24px_rgba(15,61,94,0.18)] ring-4 ring-white sm:text-2xl"
              >
                {avatarUrl ? (
                  <Image
                    src={avatarUrl}
                    alt=""
                    fill
                    sizes="112px"
                    className="rounded-full object-cover"
                  />
                ) : (
                  perfil.iniciales
                )}
              </span>

              {/* La camarita abre el diálogo de foto (fase 5d). */}
              <button
                type="button"
                onClick={() => setAvatarAbierto(true)}
                aria-label={
                  avatarUrl ? "Cambiar tu foto de perfil" : "Agregar una foto de perfil"
                }
                aria-haspopup="dialog"
                className="gradient-cta absolute -bottom-0.5 -right-0.5 flex h-9 w-9 items-center justify-center rounded-full bg-[var(--color-brand)] text-white shadow-[0_4px_12px_rgba(15,61,94,0.35)] ring-2 ring-white transition duration-200 ease-out hover:brightness-110 focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-[#4FB3D9]/50 active:scale-95"
              >
                <IconoCamara />
              </button>
            </span>

            <div className="min-w-0 pb-1">
              <p className="text-xs font-bold uppercase tracking-[0.14em] text-[var(--color-secondary)] sm:text-sm">
                Mi espacio deportivo
              </p>
              <h1 className="mt-1 truncate text-2xl font-extrabold leading-tight text-[var(--color-primary)] sm:text-3xl">
                {perfil.nombreCompleto || "Tu perfil"}
              </h1>
            </div>
          </div>

          {/*
            Una sola acción, y solo mientras haya algo por hacer: cuando
            el perfil está completo —o la persona ya pasó por el
            onboarding— la cabecera queda limpia. Sigue el mismo criterio
            que la tarjeta de progreso, para no ofrecer un paso de
            bienvenida a alguien que ya usó la app.
          */}
          {perfil.mostrarOnboarding && perfil.proximoPaso?.accion ? (
            <div className="sm:shrink-0">
              {perfil.proximoPaso.accion.href ? (
                <AppLinkButton
                  href={perfil.proximoPaso.accion.href}
                  size="sm"
                  className="w-full sm:w-auto"
                >
                  {perfil.proximoPaso.accion.texto}
                </AppLinkButton>
              ) : (
                <AppButton
                  size="sm"
                  fullWidth
                  className="sm:w-auto"
                  onClick={() => {
                    const tab = perfil.proximoPaso?.accion?.tab;

                    if (tab) {
                      onIrATab(tab);
                    }
                  }}
                >
                  {perfil.proximoPaso.accion.texto}
                </AppButton>
              )}
            </div>
          ) : null}
        </div>

        {/*
          Línea de identidad deportiva: dónde entrena y qué le interesa.
          Es lo más parecido a una bio que podemos escribir con datos
          reales — una bio editable necesita backend que hoy no existe.
        */}
        <p className="mt-4 flex flex-wrap items-center gap-x-2 gap-y-1 text-sm font-semibold text-[var(--color-muted)]">
          {perfil.ciudadNombre ? (
            <span className="inline-flex items-center gap-1.5">
              <IconoUbicacion />
              <strong className="font-extrabold text-[var(--color-primary)]">
                {perfil.ciudadNombre}
              </strong>
            </span>
          ) : null}

          {perfil.ciudadNombre && deportesVisibles.length > 0 ? (
            <span aria-hidden="true" className="text-[var(--color-border-accent)]">
              ·
            </span>
          ) : null}

          {deportesVisibles.length > 0 ? (
            <span>
              {deportesVisibles.join(" · ")}
              {deportesRestantes > 0 ? ` y ${deportesRestantes} más` : ""}
            </span>
          ) : (
            <span>Todavía no elegiste deportes.</span>
          )}
        </p>

        {/*
          Números reales, nada estimado: deportes y guardados salen de
          este dispositivo y siguiendo del backend.
        */}
        <div className="mt-5 grid grid-cols-3 gap-2 sm:max-w-lg sm:gap-3">
          <EstadisticaPerfil
            valor={perfil.deportesNombres.length}
            etiqueta={
              perfil.deportesNombres.length === 1 ? "deporte" : "deportes"
            }
            activa={tabActiva === "deportes"}
            onClick={() => onIrATab("deportes")}
          />
          <EstadisticaPerfil
            valor={perfil.favoritos.length}
            etiqueta={perfil.favoritos.length === 1 ? "guardada" : "guardadas"}
            activa={tabActiva === "guardados"}
            onClick={() => onIrATab("guardados")}
          />
          <EstadisticaPerfil
            valor={perfil.cantidadSiguiendo}
            etiqueta="siguiendo"
            activa={tabActiva === "siguiendo"}
            onClick={() => onIrATab("siguiendo")}
          />
        </div>
      </div>

      <DialogoAvatar
        abierto={avatarAbierto}
        avatarUrl={avatarUrl}
        iniciales={perfil.iniciales}
        onCerrar={() => setAvatarAbierto(false)}
        onUsuarioActualizado={(usuarioNuevo) =>
          setAvatarLocal(usuarioNuevo.avatarUrl ?? null)
        }
      />
    </SurfaceCard>
  );
}

function IconoCamara() {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      className="h-4 w-4"
      aria-hidden="true"
    >
      <path d="M14.5 4h-5L7.5 7H4a2 2 0 0 0-2 2v9a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V9a2 2 0 0 0-2-2h-3.5l-2-3z" />
      <circle cx="12" cy="13" r="3" />
    </svg>
  );
}

/*
  Un número del perfil. Mientras el dato no llegó mostramos un guion en
  vez de un cero: un "0 siguiendo" que después salta a 3 se lee como un
  dato, no como una carga.
*/
function EstadisticaPerfil({
  valor,
  etiqueta,
  activa,
  onClick,
}: {
  valor: number | null;
  etiqueta: string;
  activa: boolean;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-label={`Ver ${etiqueta}${valor === null ? "" : `: ${valor}`}`}
      className={`min-h-16 rounded-[18px] border px-3 py-3 text-center transition duration-200 ease-out hover:-translate-y-0.5 focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-[#4FB3D9]/30 ${
        activa
          ? "border-[var(--color-success-border)] bg-[var(--color-success-surface)]"
          : "border-[var(--color-border-soft)] bg-[var(--color-surface)] hover:border-[var(--color-border-accent)]"
      }`}
    >
      <span className="block text-2xl font-extrabold leading-none text-[var(--color-primary)]">
        {valor === null ? "—" : valor}
      </span>
      <span className="mt-1 block text-xs font-bold text-[var(--color-muted)]">
        {etiqueta}
      </span>
    </button>
  );
}

function IconoUbicacion() {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      className="h-4 w-4 shrink-0 text-[var(--color-accent)]"
      aria-hidden="true"
    >
      <path d="M20 10c0 5-8 11-8 11S4 15 4 10a8 8 0 1 1 16 0z" />
      <circle cx="12" cy="10" r="2.5" />
    </svg>
  );
}

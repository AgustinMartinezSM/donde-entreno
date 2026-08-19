"use client";

import { Suspense } from "react";
import { useRouter, useSearchParams } from "next/navigation";

import { AuthGuard } from "../../components/auth/AuthGuard";
import { useAuthSession } from "../../components/auth/AuthSessionProvider";
import { Header } from "../../components/layout/Header";
import { CabeceraPerfil } from "../../components/cuenta/CabeceraPerfil";
import { GuardadosPerfil } from "../../components/cuenta/GuardadosPerfil";
import { MisDeportes } from "../../components/cuenta/MisDeportes";
import { ParaVos } from "../../components/cuenta/ParaVos";
import { PublicadoresSeguidos } from "../../components/cuenta/PublicadoresSeguidos";
import { TabsPerfil } from "../../components/cuenta/TabsPerfil";
import { useFeedNovedades } from "../../components/cuenta/useFeedNovedades";
import {
  normalizarTabPerfil,
  usePerfilDeportivo,
} from "../../components/cuenta/usePerfilDeportivo";
import { useSeguimientos } from "../../components/cuenta/useSeguimientos";
import type { TabPerfil } from "../../components/cuenta/usePerfilDeportivo";

export default function MiCuentaPage() {
  return (
    <AuthGuard>
      {/* useSearchParams exige un límite de Suspense en el prerender. */}
      <Suspense fallback={null}>
        <MiCuentaContenido />
      </Suspense>
    </AuthGuard>
  );
}

/*
  Mi espacio deportivo: el lugar propio de la persona dentro de la app.

  Dos etapas atrás esto era una pila de cinco tarjetas de configuración;
  después pasó a tener cabecera y solapas, pero seguía abriendo por un
  feed que estaba vacío para casi todos y escondía los deportes —lo único
  que personaliza la experiencia— dentro de "Ajustes".

  Ahora las cuatro solapas son de contenido (Para vos, Guardados,
  Siguiendo, Deportes), la configuración vive en el menú de la cabecera y
  la primera solapa siempre tiene algo que mostrar: novedades si sigue a
  alguien, a quién seguir si todavía no, y recomendaciones reales de su
  ciudad y sus deportes en los dos casos.

  La solapa activa vive en la URL (?tab=): así los menús de cuenta pueden
  enlazar directo a Deportes o Siguiendo, y recargar conserva dónde
  estabas. Al cambiar de solapa se usa replaceState —no push— para que el
  botón atrás salga de la página en vez de desandar solapas.

  La página solo orquesta: levanta el estado que comparten la cabecera y
  las solapas (seguidos, feed, perfil) para no pedir dos veces lo mismo
  ni dejar contadores desincronizados.
*/
function MiCuentaContenido() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { sesion, usuario, accessToken, cerrarSesion } = useAuthSession();

  const tabActiva: TabPerfil =
    normalizarTabPerfil(searchParams.get("tab")) ?? "para-vos";

  /*
    window.history.replaceState (y no router.replace): Next lo sincroniza
    con useSearchParams sin re-navegar ni tocar la posición de scroll.
  */
  function cambiarTab(tab: TabPerfil) {
    window.history.replaceState(
      null,
      "",
      tab === "para-vos" ? "/mi-cuenta" : `/mi-cuenta?tab=${tab}`
    );
  }

  const usuarioVisible = usuario ?? null;
  const usuarioDeSesion = usuario ?? sesion?.usuario ?? null;
  const rolActual = usuarioDeSesion?.rol ?? null;

  const seguimientos = useSeguimientos(accessToken ?? null);
  const feed = useFeedNovedades(accessToken ?? null);
  const perfil = usePerfilDeportivo({
    nombre: usuarioDeSesion?.nombre ?? "",
    apellido: usuarioDeSesion?.apellido ?? "",
    cantidadSiguiendo: seguimientos.cantidad,
  });

  function manejarCerrarSesion() {
    cerrarSesion();
    router.replace("/");
  }

  return (
    <main className="min-h-screen text-[var(--color-text)]">
      <section className="mx-auto w-full max-w-6xl px-4 py-6">
        <Header />

        <div className="py-7 sm:py-9">
          <CabeceraPerfil
            perfil={perfil}
            usuario={usuarioVisible}
            rol={rolActual}
            tabActiva={tabActiva}
            onIrATab={cambiarTab}
            onCerrarSesion={manejarCerrarSesion}
          />

          <TabsPerfil tabActiva={tabActiva} onCambiar={cambiarTab} />

          <div className="mt-7">
            {tabActiva === "para-vos" ? (
              <ParaVos
                perfil={perfil}
                feed={feed}
                seguimientos={seguimientos}
                rol={rolActual}
                onIrATab={cambiarTab}
              />
            ) : null}

            {tabActiva === "guardados" ? (
              <GuardadosPerfil perfil={perfil} onIrATab={cambiarTab} />
            ) : null}

            {tabActiva === "siguiendo" ? (
              <PublicadoresSeguidos seguimientos={seguimientos} />
            ) : null}

            {tabActiva === "deportes" ? <MisDeportes perfil={perfil} /> : null}
          </div>
        </div>
      </section>
    </main>
  );
}

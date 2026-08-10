"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";

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
import { usePerfilDeportivo } from "../../components/cuenta/usePerfilDeportivo";
import { useSeguimientos } from "../../components/cuenta/useSeguimientos";
import type { TabPerfil } from "../../components/cuenta/usePerfilDeportivo";

export default function MiCuentaPage() {
  return (
    <AuthGuard>
      <MiCuentaContenido />
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

  La página solo orquesta: levanta el estado que comparten la cabecera y
  las solapas (seguidos, feed, perfil) para no pedir dos veces lo mismo
  ni dejar contadores desincronizados.
*/
function MiCuentaContenido() {
  const router = useRouter();
  const { sesion, usuario, accessToken, cerrarSesion } = useAuthSession();
  const [tabActiva, setTabActiva] = useState<TabPerfil>("para-vos");

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
    <main className="min-h-screen bg-[var(--color-bg)] text-[var(--color-text)]">
      <section className="mx-auto w-full max-w-6xl px-4 py-6">
        <Header />

        <div className="py-7 sm:py-9">
          <CabeceraPerfil
            perfil={perfil}
            usuario={usuarioVisible}
            rol={rolActual}
            tabActiva={tabActiva}
            onIrATab={setTabActiva}
            onCerrarSesion={manejarCerrarSesion}
          />

          <TabsPerfil tabActiva={tabActiva} onCambiar={setTabActiva} />

          <div className="mt-7">
            {tabActiva === "para-vos" ? (
              <ParaVos
                perfil={perfil}
                feed={feed}
                seguimientos={seguimientos}
                onIrATab={setTabActiva}
              />
            ) : null}

            {tabActiva === "guardados" ? (
              <GuardadosPerfil perfil={perfil} onIrATab={setTabActiva} />
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

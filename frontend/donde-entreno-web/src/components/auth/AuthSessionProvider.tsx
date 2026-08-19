"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";
import {
  AuthApiError,
  borrarRefreshTokenGuardado,
  cerrarSesionAuth,
  esSesionAuthVigente,
  guardarSesionAuth,
  hayLogoutRecienteAuth,
  obtenerRefreshTokenGuardado,
  obtenerSesionAuth,
  obtenerUsuarioActual,
  obtenerVencimientoRefreshGuardado,
  refrescarSesion,
  REFRESH_TOKEN_STORAGE_KEY,
  revocarRefreshToken,
} from "../../services/authService";
import { sincronizarCookieSesion } from "../../lib/sesionCookie";
import {
  crearScopeDeUsuario,
  establecerScopeAlmacen,
  SCOPE_INVITADO,
} from "../../lib/scopeAlmacen";
import type { LoginResponse, SesionAuth, UsuarioActual } from "../../types/auth";
import type { ReactNode } from "react";

export type AuthSessionStatus = "loading" | "authenticated" | "guest";

type AuthSessionContextValue = {
  status: AuthSessionStatus;
  sesion: SesionAuth | null;
  usuario: UsuarioActual | null;
  accessToken: string | null;
  iniciarSesionDesdeRespuesta: (response: LoginResponse) => Promise<void>;
  refrescarUsuarioActual: () => Promise<void>;
  cerrarSesion: () => void;
};

type AuthSessionProviderProps = {
  children: ReactNode;
};

type ResultadoSesionInicial =
  | {
      tipo: "authenticated";
      sesion: SesionAuth;
      usuario: UsuarioActual | null;
    }
  | {
      tipo: "guest";
    };

/*
  Cuanto antes del vencimiento del access token se renueva la sesion.
  Con access de 60 minutos, el timer dispara a los 50: la pestaña activa
  nunca llega a ver un token vencido.
*/
const MARGEN_RENOVACION_MS = 10 * 60_000;

const AuthSessionContext = createContext<AuthSessionContextValue | null>(null);

/*
  Un solo refresh en vuelo por pestaña: el boot, el timer y la vuelta de
  foco pueden coincidir, y cada uno rotaria el token del anterior. La
  carrera ENTRE pestañas la absorbe la gracia de 30s del backend; la
  carrera dentro de la misma pestaña se corta aca.
*/
let refrescoEnCurso: Promise<SesionAuth | null> | null = null;

/**
 * Intenta rotar el refresh token persistido y dejar la sesion nueva
 * guardada. Devuelve null si no hay token, si el backend lo rechazo
 * (y ahi el token se descarta: esta muerto) o si no hubo red (y ahi NO
 * se descarta: un corte de wifi no invalida una sesion de 30 dias).
 */
async function intentarRefrescarSesion(): Promise<SesionAuth | null> {
  if (refrescoEnCurso) {
    return refrescoEnCurso;
  }

  const token = obtenerRefreshTokenGuardado();

  if (!token) {
    return null;
  }

  refrescoEnCurso = (async () => {
    try {
      const respuesta = await refrescarSesion(token);
      /* Guarda la sesion nueva Y el refresh rotado (localStorage). */
      return guardarSesionAuth(respuesta);
    } catch (error: unknown) {
      if (
        error instanceof AuthApiError &&
        (error.status === 400 || error.status === 401)
      ) {
        borrarRefreshTokenGuardado();
      }

      return null;
    } finally {
      refrescoEnCurso = null;
    }
  })();

  return refrescoEnCurso;
}

export function AuthSessionProvider({ children }: AuthSessionProviderProps) {
  const [status, setStatus] = useState<AuthSessionStatus>("loading");
  const [sesion, setSesion] = useState<SesionAuth | null>(null);
  const [usuario, setUsuario] = useState<UsuarioActual | null>(null);
  const versionSesionRef = useRef(0);

  /*
    La cookie liviana del proxy vence con el REFRESH (30 dias), no con
    el access (60 min): si venciera con el access, volver mañana a una
    ruta privada redirigiria a login antes de que este provider pudiera
    refrescar. Sigue sin contener ningun token: el backend valida JWT.
  */
  const sincronizarCookiePersistente = useCallback((sesionActual: SesionAuth) => {
    const horizonteRefresh = obtenerVencimientoRefreshGuardado();

    sincronizarCookieSesion({
      expiresAt:
        horizonteRefresh && horizonteRefresh > sesionActual.expiresAt
          ? horizonteRefresh
          : sesionActual.expiresAt,
      usuario: { rol: sesionActual.usuario.rol },
    });
  }, []);

  /*
    Limpieza local de la sesion. `revocarRemoto` distingue el logout que
    la persona pidio (revoca la familia en el servidor) de los cierres
    automaticos (refresh rechazado, evento de otra pestaña), donde no
    hay nada que revocar o ya se revoco en otro lado.
  */
  const limpiarSesion = useCallback((opciones: { revocarRemoto: boolean }) => {
    versionSesionRef.current += 1;

    if (opciones.revocarRemoto) {
      const refreshActual = obtenerRefreshTokenGuardado();

      if (refreshActual) {
        revocarRefreshToken(refreshActual);
      }
    }

    borrarRefreshTokenGuardado();
    cerrarSesionAuth();
    sincronizarCookieSesion(null);
    /*
      Lo guardado en este dispositivo vuelve al scope del visitante: si no,
      la próxima persona que use esta computadora vería la lista de quien
      acaba de salir.
    */
    establecerScopeAlmacen(SCOPE_INVITADO);
    setSesion(null);
    setUsuario(null);
    setStatus("guest");
  }, []);

  const cerrarSesion = useCallback(() => {
    limpiarSesion({ revocarRemoto: true });
  }, [limpiarSesion]);

  const aplicarSesionAutenticada = useCallback(
    (sesionActual: SesionAuth, usuarioActual: UsuarioActual | null) => {
      sincronizarCookiePersistente(sesionActual);
      /*
        A partir de acá, lo guardado en el dispositivo es de esta cuenta y
        no de la anterior ni del visitante.
      */
      establecerScopeAlmacen(
        crearScopeDeUsuario(usuarioActual ?? sesionActual.usuario)
      );
      setSesion(sesionActual);
      setUsuario(usuarioActual);
      setStatus("authenticated");
    },
    [sincronizarCookiePersistente]
  );

  /*
    Renovacion silenciosa: rota el refresh y actualiza la sesion sin
    tocar al usuario en pantalla. Si el refresh esta muerto no cambia
    nada: la sesion actual sigue hasta su vencimiento natural, igual que
    antes de este bloque.
  */
  const renovarSesionSilenciosamente = useCallback(async () => {
    const versionActual = versionSesionRef.current;
    const sesionNueva = await intentarRefrescarSesion();

    if (!sesionNueva || versionActual !== versionSesionRef.current) {
      return;
    }

    setSesion(sesionNueva);
    sincronizarCookiePersistente(sesionNueva);
    setStatus("authenticated");
  }, [sincronizarCookiePersistente]);

  const refrescarUsuarioActual = useCallback(async () => {
    const versionActual = versionSesionRef.current;

    if (hayLogoutRecienteAuth()) {
      limpiarSesion({ revocarRemoto: false });
      return;
    }

    let sesionActual = obtenerSesionAuth();

    /* Sin sesion viva en la pestaña, el refresh persistido decide. */
    if (!sesionActual || !esSesionAuthVigente(sesionActual)) {
      sesionActual = await intentarRefrescarSesion();

      if (versionActual !== versionSesionRef.current) {
        return;
      }

      if (!sesionActual) {
        limpiarSesion({ revocarRemoto: false });
        return;
      }
    }

    try {
      const usuarioActual = await obtenerUsuarioActual(sesionActual.accessToken);

      if (versionActual !== versionSesionRef.current) {
        return;
      }

      aplicarSesionAutenticada(sesionActual, usuarioActual);
    } catch (error: unknown) {
      if (versionActual !== versionSesionRef.current) {
        return;
      }

      if (error instanceof AuthApiError && error.status === 401) {
        /* El access murio antes de tiempo: una rotacion todavia puede salvarla. */
        const sesionRecuperada = await intentarRefrescarSesion();

        if (versionActual !== versionSesionRef.current) {
          return;
        }

        if (!sesionRecuperada) {
          limpiarSesion({ revocarRemoto: false });
          return;
        }

        try {
          const usuarioRecuperado = await obtenerUsuarioActual(
            sesionRecuperada.accessToken
          );

          if (versionActual !== versionSesionRef.current) {
            return;
          }

          aplicarSesionAutenticada(sesionRecuperada, usuarioRecuperado);
        } catch {
          if (versionActual !== versionSesionRef.current) {
            return;
          }

          aplicarSesionAutenticada(sesionRecuperada, null);
        }

        return;
      }

      aplicarSesionAutenticada(sesionActual, null);
    }
  }, [aplicarSesionAutenticada, limpiarSesion]);

  const iniciarSesionDesdeRespuesta = useCallback(
    async (response: LoginResponse) => {
      const versionActual = versionSesionRef.current + 1;
      versionSesionRef.current = versionActual;
      const nuevaSesion = guardarSesionAuth(response);

      try {
        const usuarioActual = await obtenerUsuarioActual(nuevaSesion.accessToken);

        if (versionActual !== versionSesionRef.current) {
          return;
        }

        aplicarSesionAutenticada(nuevaSesion, usuarioActual);
      } catch (error: unknown) {
        if (versionActual !== versionSesionRef.current) {
          return;
        }

        if (error instanceof AuthApiError && error.status === 401) {
          limpiarSesion({ revocarRemoto: false });
          return;
        }

        aplicarSesionAutenticada(nuevaSesion, null);
      }
    },
    [aplicarSesionAutenticada, limpiarSesion]
  );

  useEffect(() => {
    let componenteActivo = true;
    const versionActual = versionSesionRef.current;

    void resolverSesionInicial().then((resultado) => {
      if (!componenteActivo || versionActual !== versionSesionRef.current) {
        return;
      }

      if (resultado.tipo === "guest") {
        sincronizarCookieSesion(null);
        establecerScopeAlmacen(SCOPE_INVITADO);
        setSesion(null);
        setUsuario(null);
        setStatus("guest");
        return;
      }

      sincronizarCookiePersistente(resultado.sesion);
      establecerScopeAlmacen(
        crearScopeDeUsuario(resultado.usuario ?? resultado.sesion.usuario)
      );
      setSesion(resultado.sesion);
      setUsuario(resultado.usuario);
      setStatus("authenticated");
    });

    return () => {
      componenteActivo = false;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  /*
    Renovacion proactiva: el timer dispara MARGEN antes del vencimiento
    del access (a los 50 min de los 60). La vuelta de foco cubre a la
    computadora que durmio con la pestaña abierta: el timer de un tab
    suspendido puede no correr, el focus siempre llega.
  */
  useEffect(() => {
    if (status !== "authenticated" || !sesion) {
      return;
    }

    const demora = Math.max(
      sesion.expiresAt - Date.now() - MARGEN_RENOVACION_MS,
      5_000
    );

    const timer = window.setTimeout(() => {
      void renovarSesionSilenciosamente();
    }, demora);

    function alRecuperarFoco() {
      const cercaDelVencimiento =
        sesion !== null &&
        sesion.expiresAt - Date.now() < MARGEN_RENOVACION_MS;

      if (cercaDelVencimiento) {
        void renovarSesionSilenciosamente();
      }
    }

    window.addEventListener("focus", alRecuperarFoco);
    document.addEventListener("visibilitychange", alRecuperarFoco);

    return () => {
      window.clearTimeout(timer);
      window.removeEventListener("focus", alRecuperarFoco);
      document.removeEventListener("visibilitychange", alRecuperarFoco);
    };
  }, [status, sesion, renovarSesionSilenciosamente]);

  /*
    Logout cross-tab: cuando otra pestaña borra el refresh de
    localStorage (cierre de sesion real), esta tambien sale. El evento
    `storage` solo llega desde OTRAS pestañas, asi que no hay eco del
    propio logout. La revocacion remota ya la hizo quien inicio el
    logout.
  */
  useEffect(() => {
    function alCambiarStorage(evento: StorageEvent) {
      if (
        evento.key === REFRESH_TOKEN_STORAGE_KEY &&
        evento.newValue === null &&
        status === "authenticated"
      ) {
        limpiarSesion({ revocarRemoto: false });
      }
    }

    window.addEventListener("storage", alCambiarStorage);

    return () => {
      window.removeEventListener("storage", alCambiarStorage);
    };
  }, [status, limpiarSesion]);

  const value = useMemo<AuthSessionContextValue>(
    () => ({
      status,
      sesion,
      usuario,
      accessToken: sesion?.accessToken ?? null,
      iniciarSesionDesdeRespuesta,
      refrescarUsuarioActual,
      cerrarSesion,
    }),
    [
      status,
      sesion,
      usuario,
      iniciarSesionDesdeRespuesta,
      refrescarUsuarioActual,
      cerrarSesion,
    ]
  );

  return (
    <AuthSessionContext.Provider value={value}>
      {children}
    </AuthSessionContext.Provider>
  );
}

export function useAuthSession() {
  const context = useContext(AuthSessionContext);

  if (!context) {
    throw new Error(
      "useAuthSession debe usarse dentro de AuthSessionProvider."
    );
  }

  return context;
}

async function resolverSesionInicial(): Promise<ResultadoSesionInicial> {
  if (hayLogoutRecienteAuth()) {
    return {
      tipo: "guest",
    };
  }

  let sesionActual = obtenerSesionAuth();

  /*
    El corazon de la sesion persistente: una pestaña nueva no tiene nada
    en sessionStorage, pero el refresh de localStorage la loguea solo.
    Si el refresh esta muerto o no hay red, se queda como visitante sin
    molestar (y sin descartar un token que quizas siga siendo bueno).
  */
  if (!sesionActual || !esSesionAuthVigente(sesionActual)) {
    sesionActual = await intentarRefrescarSesion();

    if (!sesionActual) {
      return {
        tipo: "guest",
      };
    }
  }

  try {
    const usuarioActual = await obtenerUsuarioActual(sesionActual.accessToken);

    return {
      tipo: "authenticated",
      sesion: sesionActual,
      usuario: usuarioActual,
    };
  } catch (error: unknown) {
    if (error instanceof AuthApiError && error.status === 401) {
      /*
        El access de la pestaña murio pero el refresh puede seguir vivo:
        una rotacion la rescata sin pedir contraseña. Recien si tambien
        falla se cierra de verdad.
      */
      const sesionRecuperada = await intentarRefrescarSesion();

      if (!sesionRecuperada) {
        cerrarSesionAuth();

        return {
          tipo: "guest",
        };
      }

      try {
        const usuarioRecuperado = await obtenerUsuarioActual(
          sesionRecuperada.accessToken
        );

        return {
          tipo: "authenticated",
          sesion: sesionRecuperada,
          usuario: usuarioRecuperado,
        };
      } catch {
        return {
          tipo: "authenticated",
          sesion: sesionRecuperada,
          usuario: null,
        };
      }
    }

    return {
      tipo: "authenticated",
      sesion: sesionActual,
      usuario: null,
    };
  }
}

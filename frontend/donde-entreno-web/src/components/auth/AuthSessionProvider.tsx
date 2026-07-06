"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from "react";
import {
  AuthApiError,
  cerrarSesionAuth,
  esSesionAuthVigente,
  guardarSesionAuth,
  obtenerSesionAuth,
  obtenerUsuarioActual,
} from "../../services/authService";
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

const AuthSessionContext = createContext<AuthSessionContextValue | null>(null);

export function AuthSessionProvider({ children }: AuthSessionProviderProps) {
  const [status, setStatus] = useState<AuthSessionStatus>("loading");
  const [sesion, setSesion] = useState<SesionAuth | null>(null);
  const [usuario, setUsuario] = useState<UsuarioActual | null>(null);

  const cerrarSesion = useCallback(() => {
    cerrarSesionAuth();
    setSesion(null);
    setUsuario(null);
    setStatus("guest");
  }, []);

  const aplicarSesionAutenticada = useCallback(
    (sesionActual: SesionAuth, usuarioActual: UsuarioActual | null) => {
      setSesion(sesionActual);
      setUsuario(usuarioActual);
      setStatus("authenticated");
    },
    []
  );

  const refrescarUsuarioActual = useCallback(async () => {
    const sesionActual = obtenerSesionAuth();

    if (!sesionActual || !esSesionAuthVigente(sesionActual)) {
      cerrarSesion();
      return;
    }

    try {
      const usuarioActual = await obtenerUsuarioActual(sesionActual.accessToken);
      aplicarSesionAutenticada(sesionActual, usuarioActual);
    } catch (error: unknown) {
      if (error instanceof AuthApiError && error.status === 401) {
        cerrarSesion();
        return;
      }

      aplicarSesionAutenticada(sesionActual, null);
    }
  }, [aplicarSesionAutenticada, cerrarSesion]);

  const iniciarSesionDesdeRespuesta = useCallback(
    async (response: LoginResponse) => {
      const nuevaSesion = guardarSesionAuth(response);

      try {
        const usuarioActual = await obtenerUsuarioActual(nuevaSesion.accessToken);
        aplicarSesionAutenticada(nuevaSesion, usuarioActual);
      } catch (error: unknown) {
        if (error instanceof AuthApiError && error.status === 401) {
          cerrarSesion();
          return;
        }

        aplicarSesionAutenticada(nuevaSesion, null);
      }
    },
    [aplicarSesionAutenticada, cerrarSesion]
  );

  useEffect(() => {
    let componenteActivo = true;

    void resolverSesionInicial().then((resultado) => {
      if (!componenteActivo) {
        return;
      }

      if (resultado.tipo === "guest") {
        setSesion(null);
        setUsuario(null);
        setStatus("guest");
        return;
      }

      setSesion(resultado.sesion);
      setUsuario(resultado.usuario);
      setStatus("authenticated");
    });

    return () => {
      componenteActivo = false;
    };
  }, []);

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
  const sesionActual = obtenerSesionAuth();

  if (!sesionActual || !esSesionAuthVigente(sesionActual)) {
    return {
      tipo: "guest",
    };
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
      cerrarSesionAuth();

      return {
        tipo: "guest",
      };
    }

    return {
      tipo: "authenticated",
      sesion: sesionActual,
      usuario: null,
    };
  }
}

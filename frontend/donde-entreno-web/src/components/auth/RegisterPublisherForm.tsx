"use client";

import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { AuthApiError, registrarPublicador } from "../../services/authService";
import { obtenerCiudades } from "../../services/ciudadService";
import { obtenerRutaInicialPorRol } from "../../lib/authRedirects";
import { useAuthSession } from "./AuthSessionProvider";
import { AppButton } from "../ui/AppButton";
import { AppLinkButton } from "../ui/AppLinkButton";
import { StatusMessage } from "../ui/StatusMessage";
import type {
  AuthErroresPorCampo,
  RegistroPublicadorRequest,
} from "../../types/auth";
import type { Ciudad } from "../../types/ciudad";
import type { ChangeEvent, FormEvent, ReactNode } from "react";

type RegistroPublicadorFormState = {
  nombre: string;
  apellido: string;
  email: string;
  password: string;
  confirmarPassword: string;
  whatsapp: string;
  tipoPublicador: string;
  nombrePublico: string;
  ciudadPrincipalId: string;
  descripcion: string;
  instagram: string;
  emailContacto: string;
  telefonoContacto: string;
};

type OpcionTipoPublicador = {
  valor: string;
  etiqueta: string;
  descripcion: string;
};

const ESTADO_INICIAL: RegistroPublicadorFormState = {
  nombre: "",
  apellido: "",
  email: "",
  password: "",
  confirmarPassword: "",
  whatsapp: "",
  tipoPublicador: "",
  nombrePublico: "",
  ciudadPrincipalId: "",
  descripcion: "",
  instagram: "",
  emailContacto: "",
  telefonoContacto: "",
};

const TIPOS_PUBLICADOR: OpcionTipoPublicador[] = [
  {
    valor: "PROFESOR_INDEPENDIENTE",
    etiqueta: "Profesor/a",
    descripcion: "Para profes que dictan clases o entrenamientos.",
  },
  {
    valor: "CLUB",
    etiqueta: "Club",
    descripcion: "Para clubes deportivos o sociales.",
  },
  {
    valor: "GIMNASIO",
    etiqueta: "Gimnasio",
    descripcion: "Para espacios de entrenamiento y fitness.",
  },
  {
    valor: "ESCUELA_DEPORTIVA",
    etiqueta: "Escuela deportiva",
    descripcion: "Para escuelas, academias o formaciones deportivas.",
  },
  {
    valor: "INSTITUCION",
    etiqueta: "Institución",
    descripcion: "Para instituciones que organizan actividades.",
  },
  {
    valor: "ESPACIO_ENTRENAMIENTO",
    etiqueta: "Espacio de entrenamiento",
    descripcion: "Para espacios donde se realizan clases o prácticas.",
  },
];

const inputClassName =
  "mt-2 min-h-12 w-full rounded-[18px] border border-[#BFDDEA] bg-[#F8FAFC] px-4 text-base text-[var(--color-text)] outline-none transition duration-200 ease-out hover:border-[var(--color-accent)] focus:border-[var(--color-accent)] focus:ring-4 focus:ring-[#DDEAF3] disabled:cursor-not-allowed disabled:opacity-70";

const TEXTO_AYUDA_PASSWORD =
  "Mínimo 8 caracteres, con al menos una letra y un número.";

export function RegisterPublisherForm() {
  const router = useRouter();
  const { status, sesion, usuario, iniciarSesionDesdeRespuesta } =
    useAuthSession();
  const cargaCiudadesActualRef = useRef(0);
  const [formulario, setFormulario] =
    useState<RegistroPublicadorFormState>(ESTADO_INICIAL);
  const [ciudades, setCiudades] = useState<Ciudad[]>([]);
  const [cargandoCiudades, setCargandoCiudades] = useState(true);
  const [errorCiudades, setErrorCiudades] = useState<string | null>(null);
  const [cargando, setCargando] = useState(false);
  const [errorGeneral, setErrorGeneral] = useState<string | null>(null);
  const [erroresPorCampo, setErroresPorCampo] =
    useState<AuthErroresPorCampo | null>(null);

  useEffect(() => {
    if (status !== "authenticated") {
      return;
    }

    const rolActual = usuario?.rol ?? sesion?.usuario.rol;

    if (!rolActual) {
      return;
    }

    router.replace(obtenerRutaInicialPorRol(rolActual));
  }, [router, sesion, status, usuario]);

  useEffect(() => {
    let componenteActivo = true;
    const cargaActual = cargaCiudadesActualRef.current + 1;
    cargaCiudadesActualRef.current = cargaActual;

    obtenerCiudades()
      .then((ciudadesObtenidas) => {
        if (
          !componenteActivo ||
          cargaCiudadesActualRef.current !== cargaActual
        ) {
          return;
        }

        setCiudades(ordenarCiudadesRegistro(ciudadesObtenidas));
        setErrorCiudades(null);
      })
      .catch(() => {
        if (
          !componenteActivo ||
          cargaCiudadesActualRef.current !== cargaActual
        ) {
          return;
        }

        setErrorCiudades("No pudimos cargar las ciudades.");
      })
      .finally(() => {
        if (
          !componenteActivo ||
          cargaCiudadesActualRef.current !== cargaActual
        ) {
          return;
        }

        setCargandoCiudades(false);
      });

    return () => {
      componenteActivo = false;
      cargaCiudadesActualRef.current += 1;
    };
  }, []);

  function manejarCambio(
    campo: keyof RegistroPublicadorFormState,
    evento: ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>
  ) {
    setFormulario((estadoActual) => ({
      ...estadoActual,
      [campo]: evento.target.value,
    }));
    setErroresPorCampo(null);
    setErrorGeneral(null);
  }

  async function manejarEnvio(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault();

    if (cargando) {
      return;
    }

    const erroresLocales = validarFormularioPublicador(formulario);

    if (Object.keys(erroresLocales).length > 0) {
      setErroresPorCampo(erroresLocales);
      setErrorGeneral("Revisá los campos marcados para crear tu perfil.");
      return;
    }

    const ciudadPrincipalId = Number(formulario.ciudadPrincipalId);

    setCargando(true);
    setErrorGeneral(null);
    setErroresPorCampo(null);

    try {
      const request: RegistroPublicadorRequest = {
        nombre: formulario.nombre.trim(),
        apellido: formulario.apellido.trim(),
        email: formulario.email.trim().toLowerCase(),
        password: formulario.password,
        confirmarPassword: formulario.confirmarPassword,
        whatsapp: formulario.whatsapp.trim(),
        tipoPublicador: formulario.tipoPublicador,
        nombrePublico: formulario.nombrePublico.trim(),
        ciudadPrincipalId,
        descripcion: normalizarTextoOpcional(formulario.descripcion),
        instagram: normalizarTextoOpcional(formulario.instagram),
        emailContacto: normalizarEmailOpcional(formulario.emailContacto),
        telefonoContacto: normalizarTextoOpcional(formulario.telefonoContacto),
      };
      const respuesta = await registrarPublicador(request);

      await iniciarSesionDesdeRespuesta(respuesta);
      router.replace(obtenerRutaInicialPorRol(respuesta.usuario.rol));
    } catch (errorRegistro: unknown) {
      if (errorRegistro instanceof AuthApiError) {
        setErrorGeneral(errorRegistro.message);
        setErroresPorCampo(errorRegistro.erroresPorCampo);
        return;
      }

      setErrorGeneral("No pudimos crear tu perfil. Intentá nuevamente.");
      setErroresPorCampo(null);
    } finally {
      setCargando(false);
    }
  }

  const ciudadError = erroresPorCampo?.ciudadPrincipalId;

  return (
    <form className="mt-8 flex flex-col gap-6" onSubmit={manejarEnvio}>
      <StatusMessage variant="info" className="text-sm leading-6">
        Creá una cuenta para publicar actividades desde tu panel. El perfil
        publicador es la información que ayuda al equipo a revisar tus envíos.
      </StatusMessage>

      <fieldset className="grid gap-5 sm:grid-cols-2">
        <legend className="sr-only">Datos de usuario</legend>
        <CampoTexto
          id="registro-publicador-nombre"
          label="Nombre"
          value={formulario.nombre}
          onChange={(evento) => manejarCambio("nombre", evento)}
          error={erroresPorCampo?.nombre}
          disabled={cargando}
          autoComplete="given-name"
          placeholder="Ej: Agustín"
          helpText="Datos de la persona que va a administrar la cuenta."
        />
        <CampoTexto
          id="registro-publicador-apellido"
          label="Apellido"
          value={formulario.apellido}
          onChange={(evento) => manejarCambio("apellido", evento)}
          error={erroresPorCampo?.apellido}
          disabled={cargando}
          autoComplete="family-name"
          placeholder="Ej: Pérez"
          helpText="Completá tu apellido para identificar al responsable."
        />
      </fieldset>

      <CampoTexto
        id="registro-publicador-email"
        label="Email"
        type="email"
        value={formulario.email}
        onChange={(evento) => manejarCambio("email", evento)}
        error={erroresPorCampo?.email}
        disabled={cargando}
        autoComplete="email"
        placeholder="Ej: responsable@email.com"
        helpText="Este email se usa para iniciar sesión en el panel publicador."
      />

      <fieldset className="grid gap-5 sm:grid-cols-2">
        <legend className="sr-only">Contraseña</legend>
        <CampoTexto
          id="registro-publicador-password"
          label="Contraseña"
          type="password"
          value={formulario.password}
          onChange={(evento) => manejarCambio("password", evento)}
          error={erroresPorCampo?.password}
          disabled={cargando}
          autoComplete="new-password"
          placeholder="Ej: Entreno2026"
          helpText={TEXTO_AYUDA_PASSWORD}
        />
        <CampoTexto
          id="registro-publicador-confirmar-password"
          label="Confirmar contraseña"
          type="password"
          value={formulario.confirmarPassword}
          onChange={(evento) => manejarCambio("confirmarPassword", evento)}
          error={erroresPorCampo?.confirmarPassword}
          disabled={cargando}
          autoComplete="new-password"
          placeholder="Repetí la contraseña"
          helpText="Escribí la misma contraseña para confirmar que está bien cargada."
        />
      </fieldset>

      <div className="rounded-[24px] border border-[#DDEAF3] bg-white/70 p-4 sm:p-5">
        <p className="text-xs font-extrabold uppercase tracking-[0.16em] text-[var(--color-secondary)]">
          Perfil publicador
        </p>

        <div className="mt-5 grid gap-5 sm:grid-cols-2">
          <CampoTexto
            id="registro-publicador-nombre-publico"
            label="Nombre público"
            value={formulario.nombrePublico}
            onChange={(evento) => manejarCambio("nombrePublico", evento)}
            error={erroresPorCampo?.nombrePublico}
            disabled={cargando}
            placeholder="Ej: Club Atlético Norte"
            helpText="Es el nombre que la gente verá públicamente en tus actividades."
          />

          <CampoTexto
            id="registro-publicador-whatsapp"
            label="WhatsApp"
            type="tel"
            value={formulario.whatsapp}
            onChange={(evento) => manejarCambio("whatsapp", evento)}
            error={erroresPorCampo?.whatsapp}
            disabled={cargando}
            autoComplete="tel"
            placeholder="Ej: +54 9 223 555 1234"
            helpText="Debe ser un número donde podamos contactarte por la publicación."
          />
        </div>

        <div className="mt-5 grid gap-5 sm:grid-cols-2">
          <div>
            <label
              htmlFor="registro-publicador-tipo"
              className="text-sm font-bold text-[var(--color-primary)]"
            >
              Tipo de publicador
            </label>
            <select
              id="registro-publicador-tipo"
              value={formulario.tipoPublicador}
              onChange={(evento) => manejarCambio("tipoPublicador", evento)}
              disabled={cargando}
              aria-invalid={Boolean(erroresPorCampo?.tipoPublicador)}
              aria-describedby={
                [
                  "registro-publicador-tipo-help",
                  formulario.tipoPublicador
                    ? "registro-publicador-tipo-descripcion"
                    : undefined,
                  erroresPorCampo?.tipoPublicador
                    ? "registro-publicador-tipo-error"
                    : undefined,
                ]
                  .filter(Boolean)
                  .join(" ") || undefined
              }
              className={inputClassName}
            >
              <option value="">Seleccioná una opción</option>
              {TIPOS_PUBLICADOR.map((tipo) => (
                <option key={tipo.valor} value={tipo.valor}>
                  {tipo.etiqueta}
                </option>
              ))}
            </select>
            {erroresPorCampo?.tipoPublicador ? (
              <p
                id="registro-publicador-tipo-error"
                className="mt-2 text-sm font-bold text-red-700"
              >
                {erroresPorCampo.tipoPublicador}
              </p>
            ) : null}
            <p
              id="registro-publicador-tipo-help"
              className="mt-2 text-xs leading-5 text-[var(--color-muted)]"
            >
              Elegí la opción que mejor describe desde dónde vas a publicar.
            </p>
            {formulario.tipoPublicador ? (
              <p
                id="registro-publicador-tipo-descripcion"
                className="mt-2 text-xs font-bold text-[var(--color-muted)]"
              >
                {
                  TIPOS_PUBLICADOR.find(
                    (tipo) => tipo.valor === formulario.tipoPublicador
                  )?.descripcion
                }
              </p>
            ) : null}
          </div>

          <div>
            <label
              htmlFor="registro-publicador-ciudad"
              className="text-sm font-bold text-[var(--color-primary)]"
            >
              Ciudad principal
            </label>
            <select
              id="registro-publicador-ciudad"
              value={formulario.ciudadPrincipalId}
              onChange={(evento) => manejarCambio("ciudadPrincipalId", evento)}
              disabled={cargando || cargandoCiudades || Boolean(errorCiudades)}
              aria-invalid={Boolean(ciudadError)}
              aria-describedby={
                [
                  "registro-publicador-ciudad-help",
                  ciudadError ? "registro-publicador-ciudad-error" : undefined,
                  errorCiudades
                    ? "registro-publicador-ciudad-carga-error"
                    : undefined,
                ]
                  .filter(Boolean)
                  .join(" ") || undefined
              }
              className={inputClassName}
            >
              <option value="">
                {cargandoCiudades ? "Cargando ciudades..." : "Seleccioná ciudad"}
              </option>
              {ciudades.map((ciudad) => (
                <option key={ciudad.id} value={ciudad.id}>
                  {ciudad.nombre}
                </option>
              ))}
            </select>
            {ciudadError ? (
              <p
                id="registro-publicador-ciudad-error"
                className="mt-2 text-sm font-bold text-red-700"
              >
                {ciudadError}
              </p>
            ) : null}
            <p
              id="registro-publicador-ciudad-help"
              className="mt-2 text-xs leading-5 text-[var(--color-muted)]"
            >
              Usala como ciudad base para tus actividades y tu perfil.
            </p>
            {errorCiudades ? (
              <p
                id="registro-publicador-ciudad-carga-error"
                className="mt-2 text-sm font-bold text-red-700"
              >
                {errorCiudades}
              </p>
            ) : null}
          </div>
        </div>

        <div className="mt-5">
          <label
            htmlFor="registro-publicador-descripcion"
            className="text-sm font-bold text-[var(--color-primary)]"
          >
            Descripción opcional
          </label>
          <textarea
            id="registro-publicador-descripcion"
            value={formulario.descripcion}
            onChange={(evento) => manejarCambio("descripcion", evento)}
            disabled={cargando}
            rows={4}
            placeholder="Ej: Entrenamientos personalizados, clases grupales y propuestas para principiantes."
            aria-describedby="registro-publicador-descripcion-help"
            className={`${inputClassName} resize-y py-3`}
          />
          <p
            id="registro-publicador-descripcion-help"
            className="mt-2 text-xs leading-5 text-[var(--color-muted)]"
          >
            Opcional. Contá brevemente qué ofrecés o qué tipo de actividades
            publicás.
          </p>
        </div>

        <div className="mt-5 grid gap-5 sm:grid-cols-3">
          <CampoTexto
            id="registro-publicador-instagram"
            label="Instagram opcional"
            value={formulario.instagram}
            onChange={(evento) => manejarCambio("instagram", evento)}
            error={erroresPorCampo?.instagram}
            disabled={cargando}
            placeholder="Ej: @tuactividad"
            helpText="Opcional. Agregalo si querés que puedan encontrarte en Instagram."
          />
          <CampoTexto
            id="registro-publicador-email-contacto"
            label="Email de contacto opcional"
            type="email"
            value={formulario.emailContacto}
            onChange={(evento) => manejarCambio("emailContacto", evento)}
            error={erroresPorCampo?.emailContacto}
            disabled={cargando}
            placeholder="Ej: contacto@tuactividad.com"
            helpText="Opcional. Puede ser distinto al email con el que iniciás sesión."
          />
          <CampoTexto
            id="registro-publicador-telefono-contacto"
            label="Teléfono de contacto opcional"
            type="tel"
            value={formulario.telefonoContacto}
            onChange={(evento) => manejarCambio("telefonoContacto", evento)}
            error={erroresPorCampo?.telefonoContacto}
            disabled={cargando}
            placeholder="Ej: 223 555 1234"
            helpText="Opcional. Sumá otro teléfono si preferís recibir consultas ahí."
          />
        </div>
      </div>

      {errorGeneral ? (
        <StatusMessage variant="error" role="alert" className="font-bold">
          {errorGeneral}
        </StatusMessage>
      ) : null}

      <AppButton
        type="submit"
        disabled={cargando || cargandoCiudades || Boolean(errorCiudades)}
        fullWidth
      >
        {cargando ? "Creando perfil..." : "Crear cuenta de publicador"}
      </AppButton>

      <div className="grid gap-3 border-t border-[#DDEAF3] pt-5 text-sm sm:grid-cols-2">
        <AppLinkButton href="/login" variant="secondary" fullWidth>
          Ya tengo cuenta
        </AppLinkButton>
        <AppLinkButton href="/registro/usuario" variant="outline" fullWidth>
          Solo quiero buscar actividades
        </AppLinkButton>
      </div>
    </form>
  );
}

function CampoTexto({
  id,
  label,
  value,
  onChange,
  error,
  disabled,
  type = "text",
  autoComplete,
  placeholder,
  helpText,
}: {
  id: string;
  label: string;
  value: string;
  onChange: (evento: ChangeEvent<HTMLInputElement>) => void;
  error?: string;
  disabled: boolean;
  type?: string;
  autoComplete?: string;
  placeholder?: string;
  helpText?: ReactNode;
}) {
  const helpId = helpText ? `${id}-help` : undefined;
  const errorId = error ? `${id}-error` : undefined;
  const descripcionIds = [helpId, errorId].filter(Boolean).join(" ");

  return (
    <div>
      <label htmlFor={id} className="text-sm font-bold text-[var(--color-primary)]">
        {label}
      </label>
      <input
        id={id}
        type={type}
        value={value}
        onChange={onChange}
        disabled={disabled}
        autoComplete={autoComplete}
        placeholder={placeholder}
        aria-invalid={Boolean(error)}
        aria-describedby={descripcionIds || undefined}
        className={inputClassName}
      />
      {helpText ? (
        <p id={helpId} className="mt-2 text-xs leading-5 text-[var(--color-muted)]">
          {helpText}
        </p>
      ) : null}
      {error ? (
        <p id={errorId} className="mt-2 text-sm font-bold text-red-700">
          {error}
        </p>
      ) : null}
    </div>
  );
}

function validarFormularioPublicador(
  formulario: RegistroPublicadorFormState
): AuthErroresPorCampo {
  const errores: AuthErroresPorCampo = {};
  const email = formulario.email.trim();
  const emailContacto = formulario.emailContacto.trim();
  const ciudadPrincipalId = Number(formulario.ciudadPrincipalId);

  if (!formulario.nombre.trim()) {
    errores.nombre = "Ingresá tu nombre.";
  }

  if (!formulario.apellido.trim()) {
    errores.apellido = "Ingresá tu apellido.";
  }

  if (!email) {
    errores.email = "Ingresá tu email.";
  } else if (!esEmailValido(email)) {
    errores.email = "Ingresá un email válido.";
  }

  if (!formulario.password) {
    errores.password = "Ingresá una contraseña.";
  } else if (!cumpleRequisitosPassword(formulario.password)) {
    errores.password = `La contraseña debe tener ${TEXTO_AYUDA_PASSWORD.toLowerCase()}`;
  }

  if (!formulario.confirmarPassword) {
    errores.confirmarPassword = "Confirmá tu contraseña.";
  } else if (formulario.password !== formulario.confirmarPassword) {
    errores.confirmarPassword = "Las contraseñas no coinciden.";
  }

  if (!formulario.whatsapp.trim()) {
    errores.whatsapp = "Ingresá un WhatsApp de contacto.";
  }

  if (!formulario.tipoPublicador) {
    errores.tipoPublicador = "Seleccioná el tipo de publicador.";
  }

  if (!formulario.nombrePublico.trim()) {
    errores.nombrePublico = "Ingresá el nombre público.";
  }

  if (!Number.isFinite(ciudadPrincipalId) || ciudadPrincipalId <= 0) {
    errores.ciudadPrincipalId = "Seleccioná la ciudad principal.";
  }

  if (emailContacto && !esEmailValido(emailContacto)) {
    errores.emailContacto = "Ingresá un email de contacto válido.";
  }

  return errores;
}

function ordenarCiudadesRegistro(ciudades: Ciudad[]): Ciudad[] {
  return [...ciudades]
    .filter((ciudad) => ciudad.activa)
    .sort((a, b) => {
      const ordenA = a.orden ?? Number.MAX_SAFE_INTEGER;
      const ordenB = b.orden ?? Number.MAX_SAFE_INTEGER;

      if (ordenA !== ordenB) {
        return ordenA - ordenB;
      }

      return a.nombre.localeCompare(b.nombre, "es");
    });
}

function esEmailValido(email: string): boolean {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}

function cumpleRequisitosPassword(password: string): boolean {
  const tieneLongitudMinima = password.length >= 8;
  const tieneLetra = /\p{L}/u.test(password);
  const tieneNumero = /\d/.test(password);

  return tieneLongitudMinima && tieneLetra && tieneNumero;
}

function normalizarTextoOpcional(valor: string): string | null {
  const textoLimpio = valor.trim();

  return textoLimpio ? textoLimpio : null;
}

function normalizarEmailOpcional(valor: string): string | null {
  const textoLimpio = valor.trim().toLowerCase();

  return textoLimpio ? textoLimpio : null;
}

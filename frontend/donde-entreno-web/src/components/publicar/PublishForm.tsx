"use client";

import {
  useCallback,
  useEffect,
  useRef,
  useState,
  type ChangeEvent,
  type FormEvent,
} from "react";
import {
  CatalogosPublicacionApiError,
  obtenerBarriosPublicacion,
  obtenerCiudadesPublicacion,
  obtenerDeportesPublicacion,
} from "../../services/catalogosPublicacionService";
import {
  enviarSolicitudPublicacion,
  SolicitudPublicacionApiError,
} from "../../services/solicitudPublicacionService";
import {
  crearSolicitudPublicador,
  PublicadorApiError,
} from "../../services/publicadorService";
import { BrandName } from "../brand/BrandName";
import type { CrearSolicitudPublicadorRequest } from "../../types/publicador";
import type {
  BarrioPublicacionOpcion,
  CiudadPublicacionOpcion,
  DeportePublicacionOpcion,
} from "../../types/catalogosPublicacion";
import {
  DIAS_SEMANA_SOLICITUD,
  ENFOQUES_SOLICITUD,
  MODALIDADES_SOLICITUD,
  NIVELES_SOLICITUD,
  TIPOS_PUBLICADOR_SOLICITUD,
  type DiaSemanaSolicitudPublicacion,
  type EnfoqueSolicitudPublicacion,
  type ModalidadSolicitudPublicacion,
  type NivelSolicitudPublicacion,
  type SolicitudPublicacionErroresPorCampo,
  type SolicitudPublicacionHorarioRequest,
  type SolicitudPublicacionRequest,
  type SolicitudPublicacionResponse,
  type TipoPublicadorSolicitud,
} from "../../types/solicitudPublicacion";
import { AppButton } from "../ui/AppButton";
import { SurfaceCard } from "../ui/SurfaceCard";

type SolicitudPublicacionFormState = {
  tipoPublicador: "" | TipoPublicadorSolicitud;
  nombrePublicador: string;
  nombreActividad: string;
  deporteId: string;
  deporteOtro: string;
  descripcion: string;
  nivel: "" | NivelSolicitudPublicacion;
  enfoque: "" | EnfoqueSolicitudPublicacion;
  modalidad: "" | ModalidadSolicitudPublicacion;
  edadMinima: string;
  edadMaxima: string;
  precioReferencia: string;
  mostrarPrecio: boolean;
  ciudadId: string;
  ciudadOtra: string;
  barrioId: string;
  barrioOtro: string;
  nombreLugar: string;
  direccion: string;
  referenciaUbicacion: string;
  whatsapp: string;
  instagram: string;
  email: string;
  observacionesSolicitante: string;
  aceptaCondiciones: boolean;
};

type PublishFormModo = "publico" | "publicador";

type PublishFormProps = {
  modo?: PublishFormModo;
  accessToken?: string | null;
  tituloExitoPersonalizado?: string;
};

type HorarioPublicacionFormState = {
  idInterno: number;
  diaSemana: "" | DiaSemanaSolicitudPublicacion;
  horaInicio: string;
  horaFin: string;
  observacion: string;
};

type ErroresHorarioFormulario = {
  diaSemana?: string;
  horaInicio?: string;
  horaFin?: string;
  duplicado?: string;
};

type ErroresSolicitudPublicacionForm = {
  deporte?: string;
  ciudad?: string;
  barrio?: string;
  ubicacion?: string;
  contacto?: string;
  edades?: string;
  precioReferencia?: string;
  aceptaCondiciones?: string;
  horarios?: string;
  horariosDuplicados?: string;
  horariosPorId: Record<number, ErroresHorarioFormulario>;
};

const ESTADO_INICIAL_FORMULARIO: SolicitudPublicacionFormState = {
  tipoPublicador: "",
  nombrePublicador: "",
  nombreActividad: "",
  deporteId: "",
  deporteOtro: "",
  descripcion: "",
  nivel: "",
  enfoque: "",
  modalidad: "",
  edadMinima: "",
  edadMaxima: "",
  precioReferencia: "",
  mostrarPrecio: false,
  ciudadId: "",
  ciudadOtra: "",
  barrioId: "",
  barrioOtro: "",
  nombreLugar: "",
  direccion: "",
  referenciaUbicacion: "",
  whatsapp: "",
  instagram: "",
  email: "",
  observacionesSolicitante: "",
  aceptaCondiciones: false,
};

function crearHorarioFormulario(
  idInterno: number
): HorarioPublicacionFormState {
  return {
    idInterno,
    diaSemana: "",
    horaInicio: "",
    horaFin: "",
    observacion: "",
  };
}

function crearEstadoInicialErroresFormulario(): ErroresSolicitudPublicacionForm {
  return {
    horariosPorId: {},
  };
}

function tieneTexto(valor: string) {
  return valor.trim().length > 0;
}

function convertirNumeroOpcional(valor: string) {
  if (!tieneTexto(valor)) {
    return null;
  }

  const numero = Number(valor);
  return Number.isFinite(numero) ? numero : null;
}

function convertirHoraAMinutos(hora: string) {
  const partesHora = /^(\d{2}):(\d{2})$/.exec(hora);

  if (!partesHora) {
    return null;
  }

  const horas = Number(partesHora[1]);
  const minutos = Number(partesHora[2]);

  if (horas < 0 || horas > 23 || minutos < 0 || minutos > 59) {
    return null;
  }

  return horas * 60 + minutos;
}

function precioTieneFormatoValido(valor: string) {
  return /^\d{1,8}(\.\d{1,2})?$/.test(valor.trim());
}

function agregarErrorHorario(
  errores: ErroresSolicitudPublicacionForm,
  idInterno: number,
  errorHorario: ErroresHorarioFormulario
) {
  errores.horariosPorId[idInterno] = {
    ...errores.horariosPorId[idInterno],
    ...errorHorario,
  };
}

function validarSolicitudFormulario(
  formulario: SolicitudPublicacionFormState,
  horarios: HorarioPublicacionFormState[],
  seleccionDeporte: string,
  seleccionCiudad: string,
  seleccionBarrio: string
): ErroresSolicitudPublicacionForm {
  const errores = crearEstadoInicialErroresFormulario();
  const tieneDeporteId = tieneTexto(formulario.deporteId);
  const tieneDeporteOtro = tieneTexto(formulario.deporteOtro);
  const tieneCiudadId = tieneTexto(formulario.ciudadId);
  const tieneCiudadOtra = tieneTexto(formulario.ciudadOtra);
  const tieneBarrioId = tieneTexto(formulario.barrioId);
  const tieneBarrioOtro = tieneTexto(formulario.barrioOtro);

  if (
    tieneDeporteId === tieneDeporteOtro ||
    (!tieneDeporteId && !tieneTexto(seleccionDeporte))
  ) {
    errores.deporte = "Seleccioná un deporte o ingresá otro.";
  }

  if (
    tieneCiudadId === tieneCiudadOtra ||
    (!tieneCiudadId && !tieneTexto(seleccionCiudad))
  ) {
    errores.ciudad = "Seleccioná una ciudad o ingresá otra.";
  }

  if (
    tieneBarrioId === tieneBarrioOtro ||
    (tieneBarrioId && !tieneCiudadId) ||
    (tieneCiudadOtra && tieneBarrioId) ||
    (!tieneBarrioId && !tieneTexto(seleccionBarrio))
  ) {
    errores.barrio = "Seleccioná un barrio o ingresá otro.";
  }

  if (!tieneTexto(formulario.nombreLugar) && !tieneTexto(formulario.direccion)) {
    errores.ubicacion = "Completá el nombre del lugar o la dirección.";
  }

  if (!tieneTexto(formulario.whatsapp) && !tieneTexto(formulario.email)) {
    errores.contacto = "Completá al menos WhatsApp o email.";
  }

  const edadMinima = convertirNumeroOpcional(formulario.edadMinima);
  const edadMaxima = convertirNumeroOpcional(formulario.edadMaxima);

  if (
    (tieneTexto(formulario.edadMinima) && edadMinima === null) ||
    (tieneTexto(formulario.edadMaxima) && edadMaxima === null)
  ) {
    errores.edades = "Ingresá edades válidas.";
  } else if (edadMinima !== null && edadMaxima !== null && edadMinima > edadMaxima) {
    errores.edades = "La edad mínima no puede ser mayor que la edad máxima.";
  }

  if (formulario.mostrarPrecio) {
    const precioReferencia = convertirNumeroOpcional(formulario.precioReferencia);

    if (!tieneTexto(formulario.precioReferencia)) {
      errores.precioReferencia = "Ingresá un precio de referencia.";
    } else if (
      precioReferencia === null ||
      precioReferencia < 0 ||
      !precioTieneFormatoValido(formulario.precioReferencia)
    ) {
      errores.precioReferencia =
        "Ingresá un precio válido, con hasta 8 enteros y 2 decimales.";
    }
  }

  if (horarios.length === 0) {
    errores.horarios = "Agregá al menos un horario.";
  }

  const horariosPorClave = new Map<string, number[]>();

  horarios.forEach((horario) => {
    const errorHorario: ErroresHorarioFormulario = {};
    const tieneDiaSemana = tieneTexto(horario.diaSemana);
    const tieneHoraInicio = tieneTexto(horario.horaInicio);
    const tieneHoraFin = tieneTexto(horario.horaFin);

    if (!tieneDiaSemana) {
      errorHorario.diaSemana = "Seleccioná un día.";
    }

    if (!tieneHoraInicio) {
      errorHorario.horaInicio = "Ingresá la hora de inicio.";
    }

    if (!tieneHoraFin) {
      errorHorario.horaFin = "Ingresá la hora de finalización.";
    }

    if (tieneHoraInicio && tieneHoraFin) {
      const horaInicio = convertirHoraAMinutos(horario.horaInicio);
      const horaFin = convertirHoraAMinutos(horario.horaFin);

      if (horaInicio === null || horaFin === null) {
        errorHorario.horaFin = "Ingresá un rango horario válido.";
      } else if (horaInicio >= horaFin) {
        errorHorario.horaFin =
          "La hora de finalización debe ser posterior a la hora de inicio.";
      }
    }

    if (Object.keys(errorHorario).length > 0) {
      agregarErrorHorario(errores, horario.idInterno, errorHorario);
    }

    if (tieneDiaSemana && tieneHoraInicio && tieneHoraFin) {
      const claveHorario = `${horario.diaSemana}-${horario.horaInicio}-${horario.horaFin}`;
      const idsHorario = horariosPorClave.get(claveHorario) ?? [];
      horariosPorClave.set(claveHorario, [...idsHorario, horario.idInterno]);
    }
  });

  horariosPorClave.forEach((idsHorario) => {
    if (idsHorario.length > 1) {
      errores.horariosDuplicados = "Hay horarios repetidos.";

      idsHorario.forEach((idInterno) => {
        agregarErrorHorario(errores, idInterno, {
          duplicado: "Este horario está repetido.",
        });
      });
    }
  });

  if (formulario.aceptaCondiciones !== true) {
    errores.aceptaCondiciones = "Debés aceptar las condiciones para continuar.";
  }

  return errores;
}

function tieneErroresHorario(errores: ErroresHorarioFormulario) {
  return Object.values(errores).some(Boolean);
}

function tieneErroresFormulario(errores: ErroresSolicitudPublicacionForm) {
  return (
    Boolean(
      errores.deporte ||
        errores.ciudad ||
        errores.barrio ||
        errores.ubicacion ||
        errores.contacto ||
        errores.edades ||
        errores.precioReferencia ||
        errores.aceptaCondiciones ||
        errores.horarios ||
        errores.horariosDuplicados
    ) || Object.values(errores.horariosPorId).some(tieneErroresHorario)
  );
}

function obtenerMensajesErrores(errores: ErroresSolicitudPublicacionForm) {
  const mensajes = new Set<string>();
  const agregarMensaje = (mensaje: string | undefined) => {
    if (mensaje) {
      mensajes.add(mensaje);
    }
  };

  agregarMensaje(errores.deporte);
  agregarMensaje(errores.ciudad);
  agregarMensaje(errores.barrio);
  agregarMensaje(errores.ubicacion);
  agregarMensaje(errores.contacto);
  agregarMensaje(errores.edades);
  agregarMensaje(errores.precioReferencia);
  agregarMensaje(errores.horarios);
  agregarMensaje(errores.horariosDuplicados);
  agregarMensaje(errores.aceptaCondiciones);

  Object.values(errores.horariosPorId).forEach((erroresHorario) => {
    agregarMensaje(erroresHorario.diaSemana);
    agregarMensaje(erroresHorario.horaInicio);
    agregarMensaje(erroresHorario.horaFin);
    agregarMensaje(erroresHorario.duplicado);
  });

  return Array.from(mensajes);
}

const OPCION_OTRO = "__OTRO__";

const inputClassName =
  "min-h-12 rounded-[var(--radius-md)] border border-[#BFDDEA] bg-[#F8FAFC] px-4 text-sm outline-none transition duration-200 ease-out hover:border-[var(--color-accent)] focus:border-[var(--color-accent)] focus-visible:ring-4 focus-visible:ring-[#4FB3D9]/30 disabled:cursor-not-allowed disabled:opacity-70";

const textareaClassName =
  "rounded-[var(--radius-md)] border border-[#BFDDEA] bg-[#F8FAFC] px-4 py-3 text-sm outline-none transition duration-200 ease-out hover:border-[var(--color-accent)] focus:border-[var(--color-accent)] focus-visible:ring-4 focus-visible:ring-[#4FB3D9]/30";

const labelClassName = "text-sm font-bold text-[var(--color-primary)]";
const fieldsetClassName =
  "rounded-[var(--radius-xl)] border border-[#DDEAF3] bg-white p-4 shadow-[0_16px_40px_rgba(12,52,80,0.08)] sm:p-6";

function formatearEtiquetaCatalogo(valor: string) {
  return valor
    .toLowerCase()
    .split("_")
    .map((parte, indice) =>
      indice === 0 ? parte.charAt(0).toUpperCase() + parte.slice(1) : parte
    )
    .join(" ");
}

function obtenerMensajeErrorCatalogo(
  error: unknown,
  mensajePorDefecto: string
) {
  if (error instanceof CatalogosPublicacionApiError) {
    return error.message;
  }

  if (error instanceof Error && error.message) {
    return error.message;
  }

  return mensajePorDefecto;
}

async function obtenerCatalogosInicialesPublicacion(): Promise<{
  deportes: DeportePublicacionOpcion[];
  ciudades: CiudadPublicacionOpcion[];
}> {
  const [deportes, ciudades] = await Promise.all([
    obtenerDeportesPublicacion(),
    obtenerCiudadesPublicacion(),
  ]);

  return {
    deportes,
    ciudades,
  };
}

function normalizarTextoOpcional(valor: string): string | null {
  const texto = valor.trim();
  return texto.length > 0 ? texto : null;
}

function normalizarIdOpcional(valor: string): number | null {
  const texto = valor.trim();

  if (texto.length === 0) {
    return null;
  }

  const numero = Number(texto);
  return Number.isInteger(numero) && numero > 0 ? numero : null;
}

function normalizarNumeroOpcional(valor: string): number | null {
  const texto = valor.trim();

  if (texto.length === 0) {
    return null;
  }

  const numero = Number(texto);
  return Number.isFinite(numero) ? numero : null;
}

function construirSolicitudPublicadorRequest(
  formulario: SolicitudPublicacionFormState,
  horarios: HorarioPublicacionFormState[]
): CrearSolicitudPublicadorRequest | null {
  const nivel = formulario.nivel;
  const enfoque = formulario.enfoque;
  const modalidad = formulario.modalidad;
  const nombreActividad = formulario.nombreActividad.trim();
  const descripcion = formulario.descripcion.trim();

  if (
    !nivel ||
    !enfoque ||
    !modalidad ||
    nombreActividad.length === 0 ||
    descripcion.length === 0 ||
    horarios.length === 0 ||
    formulario.aceptaCondiciones !== true
  ) {
    return null;
  }

  const deporteId = normalizarIdOpcional(formulario.deporteId);
  const deporteOtro = normalizarTextoOpcional(formulario.deporteOtro);

  if ((deporteId === null) === (deporteOtro === null)) {
    return null;
  }

  const ciudadId = normalizarIdOpcional(formulario.ciudadId);
  const ciudadOtra = normalizarTextoOpcional(formulario.ciudadOtra);

  if ((ciudadId === null) === (ciudadOtra === null)) {
    return null;
  }

  const barrioId = normalizarIdOpcional(formulario.barrioId);
  const barrioOtro = normalizarTextoOpcional(formulario.barrioOtro);

  if (
    (barrioId === null) === (barrioOtro === null) ||
    (barrioId !== null && ciudadId === null) ||
    (ciudadOtra !== null && barrioId !== null)
  ) {
    return null;
  }

  const edadMinima = normalizarNumeroOpcional(formulario.edadMinima);
  const edadMaxima = normalizarNumeroOpcional(formulario.edadMaxima);
  const precioReferencia = formulario.mostrarPrecio
    ? normalizarNumeroOpcional(formulario.precioReferencia)
    : null;

  if (formulario.mostrarPrecio && precioReferencia === null) {
    return null;
  }

  const nombreLugar = normalizarTextoOpcional(formulario.nombreLugar);
  const direccion = normalizarTextoOpcional(formulario.direccion);

  if (nombreLugar === null && direccion === null) {
    return null;
  }

  const whatsapp = normalizarTextoOpcional(formulario.whatsapp);
  const email = normalizarTextoOpcional(formulario.email)?.toLowerCase() ?? null;

  if (whatsapp === null && email === null) {
    return null;
  }

  const horariosSolicitud: SolicitudPublicacionHorarioRequest[] = [];

  for (const horario of horarios) {
    const diaSemana = horario.diaSemana;
    const horaInicio = horario.horaInicio.trim();
    const horaFin = horario.horaFin.trim();

    if (!diaSemana || horaInicio.length === 0 || horaFin.length === 0) {
      return null;
    }

    horariosSolicitud.push({
      diaSemana,
      horaInicio,
      horaFin,
      observacion: normalizarTextoOpcional(horario.observacion),
    });
  }

  return {
    nombreActividad,
    deporteId,
    deporteOtro,
    descripcion,
    nivel,
    enfoque,
    modalidad,
    edadMinima,
    edadMaxima,
    precioReferencia,
    mostrarPrecio: formulario.mostrarPrecio,
    ciudadId,
    ciudadOtra,
    barrioId,
    barrioOtro,
    nombreLugar,
    direccion,
    referenciaUbicacion: normalizarTextoOpcional(formulario.referenciaUbicacion),
    whatsapp,
    instagram: normalizarTextoOpcional(formulario.instagram),
    email,
    observacionesSolicitante: normalizarTextoOpcional(
      formulario.observacionesSolicitante
    ),
    aceptaCondiciones: formulario.aceptaCondiciones,
    horarios: horariosSolicitud,
  };
}

function construirSolicitudPublicacionRequest(
  formulario: SolicitudPublicacionFormState,
  horarios: HorarioPublicacionFormState[]
): SolicitudPublicacionRequest | null {
  const tipoPublicador = formulario.tipoPublicador;
  const nombrePublicador = formulario.nombrePublicador.trim();

  if (!tipoPublicador || nombrePublicador.length === 0) {
    return null;
  }

  const datosSolicitud = construirSolicitudPublicadorRequest(
    formulario,
    horarios
  );

  if (datosSolicitud === null) {
    return null;
  }

  return {
    tipoPublicador,
    nombrePublicador,
    ...datosSolicitud,
  };
}

function CampoObligatorio() {
  return <span className="text-[var(--color-secondary)]">*</span>;
}

type PublishSectionHeaderProps = {
  paso: number;
  titulo: string;
  descripcion: string;
};

function PublishSectionHeader({
  paso,
  titulo,
  descripcion,
}: PublishSectionHeaderProps) {
  return (
    <legend className="mb-5 flex w-full flex-col gap-3 sm:flex-row sm:items-start">
      <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-[#E6F7EF] text-sm font-extrabold text-[#167A4A]">
        {paso}
      </span>
      <span>
        <span className="block text-xl font-extrabold text-[var(--color-primary)]">
          {titulo}
        </span>
        <span className="mt-2 block text-sm leading-6 text-[var(--color-muted)]">
          {descripcion}
        </span>
      </span>
    </legend>
  );
}

export function PublishForm({
  modo = "publico",
  accessToken = null,
  tituloExitoPersonalizado,
}: PublishFormProps = {}) {
  const [formulario, setFormulario] = useState<SolicitudPublicacionFormState>(
    ESTADO_INICIAL_FORMULARIO
  );
  const [procesandoFormulario, setProcesandoFormulario] = useState(false);
  const [respuestaEnvio, setRespuestaEnvio] =
    useState<SolicitudPublicacionResponse | null>(null);
  const [errorEnvio, setErrorEnvio] = useState<string | null>(null);
  const [erroresBackend, setErroresBackend] =
    useState<SolicitudPublicacionErroresPorCampo | null>(null);
  const [deportes, setDeportes] = useState<DeportePublicacionOpcion[]>([]);
  const [ciudades, setCiudades] = useState<CiudadPublicacionOpcion[]>([]);
  const [barrios, setBarrios] = useState<BarrioPublicacionOpcion[]>([]);
  const [cargandoCatalogosIniciales, setCargandoCatalogosIniciales] =
    useState(true);
  const [errorCatalogosIniciales, setErrorCatalogosIniciales] = useState<
    string | null
  >(null);
  const [cargandoBarrios, setCargandoBarrios] = useState(false);
  const [errorBarrios, setErrorBarrios] = useState<string | null>(null);
  const [seleccionDeporte, setSeleccionDeporte] = useState("");
  const [seleccionCiudad, setSeleccionCiudad] = useState("");
  const [seleccionBarrio, setSeleccionBarrio] = useState("");
  const [horarios, setHorarios] = useState<HorarioPublicacionFormState[]>([
    crearHorarioFormulario(1),
  ]);
  const [erroresFormulario, setErroresFormulario] =
    useState<ErroresSolicitudPublicacionForm>(
      crearEstadoInicialErroresFormulario
    );
  const siguienteHorarioIdRef = useRef(2);
  const resumenErroresRef = useRef<HTMLDivElement | null>(null);
  const envioEnCursoRef = useRef(false);
  const resultadoEnvioRef = useRef<HTMLDivElement | null>(null);
  const cargaBarriosActualRef = useRef(0);
  const cargaCatalogosActualRef = useRef(0);
  const formularioMontadoRef = useRef(true);
  const esModoPublicador = modo === "publicador";
  const obtenerNumeroPaso = (pasoPublico: number) =>
    esModoPublicador ? pasoPublico - 1 : pasoPublico;

  const cargarCatalogosIniciales = useCallback(async () => {
    const cargaActual = cargaCatalogosActualRef.current + 1;
    cargaCatalogosActualRef.current = cargaActual;
    setCargandoCatalogosIniciales(true);
    setErrorCatalogosIniciales(null);

    try {
      const { deportes: deportesObtenidos, ciudades: ciudadesObtenidas } =
        await obtenerCatalogosInicialesPublicacion();

      if (
        !formularioMontadoRef.current ||
        cargaCatalogosActualRef.current !== cargaActual
      ) {
        return;
      }

      setDeportes(deportesObtenidos);
      setCiudades(ciudadesObtenidas);
    } catch (error: unknown) {
      if (
        !formularioMontadoRef.current ||
        cargaCatalogosActualRef.current !== cargaActual
      ) {
        return;
      }

      setErrorCatalogosIniciales(
        obtenerMensajeErrorCatalogo(
          error,
          "No se pudieron cargar los deportes y ciudades."
        )
      );
    } finally {
      if (
        formularioMontadoRef.current &&
        cargaCatalogosActualRef.current === cargaActual
      ) {
        setCargandoCatalogosIniciales(false);
      }
    }
  }, []);
  const cargarBarriosParaCiudad = useCallback(async (ciudadId: string) => {
    const cargaActual = cargaBarriosActualRef.current + 1;
    cargaBarriosActualRef.current = cargaActual;
    setCargandoBarrios(true);
    setErrorBarrios(null);

    try {
      const barriosObtenidos = await obtenerBarriosPublicacion(
        Number(ciudadId)
      );

      if (
        !formularioMontadoRef.current ||
        cargaBarriosActualRef.current !== cargaActual
      ) {
        return;
      }

      setBarrios(barriosObtenidos);
    } catch (error: unknown) {
      if (
        !formularioMontadoRef.current ||
        cargaBarriosActualRef.current !== cargaActual
      ) {
        return;
      }

      setBarrios([]);
      setErrorBarrios(
        obtenerMensajeErrorCatalogo(
          error,
          "No se pudieron cargar los barrios."
        )
      );
    } finally {
      if (
        formularioMontadoRef.current &&
        cargaBarriosActualRef.current === cargaActual
      ) {
        setCargandoBarrios(false);
      }
    }
  }, []);

  useEffect(() => {
    formularioMontadoRef.current = true;
    const cargaActual = cargaCatalogosActualRef.current + 1;
    cargaCatalogosActualRef.current = cargaActual;

    obtenerCatalogosInicialesPublicacion()
      .then(({ deportes: deportesObtenidos, ciudades: ciudadesObtenidas }) => {
        if (
          !formularioMontadoRef.current ||
          cargaCatalogosActualRef.current !== cargaActual
        ) {
          return;
        }

        setDeportes(deportesObtenidos);
        setCiudades(ciudadesObtenidas);
      })
      .catch((error: unknown) => {
        if (
          !formularioMontadoRef.current ||
          cargaCatalogosActualRef.current !== cargaActual
        ) {
          return;
        }

        setErrorCatalogosIniciales(
          obtenerMensajeErrorCatalogo(
            error,
            "No se pudieron cargar los deportes y ciudades."
          )
        );
      })
      .finally(() => {
        if (
          formularioMontadoRef.current &&
          cargaCatalogosActualRef.current === cargaActual
        ) {
          setCargandoCatalogosIniciales(false);
        }
      });

    return () => {
      formularioMontadoRef.current = false;
      cargaCatalogosActualRef.current += 1;
      cargaBarriosActualRef.current += 1;
    };
  }, []);

  function limpiarValidacionLocal() {
    setErroresFormulario(crearEstadoInicialErroresFormulario());
    setErrorEnvio(null);
    setErroresBackend(null);
  }

  function actualizarCampo<K extends keyof SolicitudPublicacionFormState>(
    campo: K,
    valor: SolicitudPublicacionFormState[K]
  ) {
    setFormulario((estadoActual) => ({
      ...estadoActual,
      [campo]: valor,
    }));
    limpiarValidacionLocal();
  }

  function actualizarFormulario(
    actualizador: (
      estadoActual: SolicitudPublicacionFormState
    ) => SolicitudPublicacionFormState
  ) {
    setFormulario(actualizador);
    limpiarValidacionLocal();
  }

  function manejarCambioTexto(
    campo: keyof SolicitudPublicacionFormState,
    evento: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>
  ) {
    actualizarCampo(campo, evento.target.value);
  }

  function manejarCambioMostrarPrecio(evento: ChangeEvent<HTMLInputElement>) {
    const mostrarPrecio = evento.target.checked;

    actualizarFormulario((estadoActual) => ({
      ...estadoActual,
      mostrarPrecio,
      precioReferencia: mostrarPrecio ? estadoActual.precioReferencia : "",
    }));
  }

  function manejarSeleccionDeporte(evento: ChangeEvent<HTMLSelectElement>) {
    const valorSeleccionado = evento.target.value;
    setSeleccionDeporte(valorSeleccionado);

    actualizarFormulario((estadoActual) => ({
      ...estadoActual,
      deporteId: valorSeleccionado !== OPCION_OTRO ? valorSeleccionado : "",
      deporteOtro: "",
    }));
  }

  function limpiarSeleccionBarrio() {
    setSeleccionBarrio("");
    setBarrios([]);
    setErrorBarrios(null);
    setCargandoBarrios(false);
    cargaBarriosActualRef.current += 1;
  }

  function manejarSeleccionCiudad(evento: ChangeEvent<HTMLSelectElement>) {
    const valorSeleccionado = evento.target.value;
    setSeleccionCiudad(valorSeleccionado);

    if (valorSeleccionado === OPCION_OTRO) {
      limpiarSeleccionBarrio();
      setSeleccionBarrio(OPCION_OTRO);
      actualizarFormulario((estadoActual) => ({
        ...estadoActual,
        ciudadId: "",
        ciudadOtra: "",
        barrioId: "",
        barrioOtro: "",
      }));
      return;
    }

    if (valorSeleccionado === "") {
      limpiarSeleccionBarrio();
      actualizarFormulario((estadoActual) => ({
        ...estadoActual,
        ciudadId: "",
        ciudadOtra: "",
        barrioId: "",
        barrioOtro: "",
      }));
      return;
    }

    setSeleccionBarrio("");
    setBarrios([]);
    setErrorBarrios(null);
    actualizarFormulario((estadoActual) => ({
      ...estadoActual,
      ciudadId: valorSeleccionado,
      ciudadOtra: "",
      barrioId: "",
      barrioOtro: "",
    }));
    void cargarBarriosParaCiudad(valorSeleccionado);
  }
  function manejarSeleccionBarrio(evento: ChangeEvent<HTMLSelectElement>) {
    const valorSeleccionado = evento.target.value;
    setSeleccionBarrio(valorSeleccionado);

    actualizarFormulario((estadoActual) => ({
      ...estadoActual,
      barrioId: valorSeleccionado !== OPCION_OTRO ? valorSeleccionado : "",
      barrioOtro: "",
    }));
  }

  function manejarReintentarBarrios() {
    if (seleccionCiudad !== "" && seleccionCiudad !== OPCION_OTRO) {
      void cargarBarriosParaCiudad(seleccionCiudad);
    }
  }

  function agregarHorario() {
    const nuevoHorario = crearHorarioFormulario(siguienteHorarioIdRef.current);
    siguienteHorarioIdRef.current += 1;

    setHorarios((estadoActual) => [...estadoActual, nuevoHorario]);
    limpiarValidacionLocal();
  }

  function quitarHorario(idInterno: number) {
    setHorarios((estadoActual) => {
      if (estadoActual.length <= 1) {
        return estadoActual;
      }

      return estadoActual.filter((horario) => horario.idInterno !== idInterno);
    });
    limpiarValidacionLocal();
  }

  function actualizarHorario<
    K extends "diaSemana" | "horaInicio" | "horaFin" | "observacion",
  >(idInterno: number, campo: K, valor: HorarioPublicacionFormState[K]) {
    setHorarios((estadoActual) =>
      estadoActual.map((horario) =>
        horario.idInterno === idInterno
          ? {
              ...horario,
              [campo]: valor,
            }
          : horario
      )
    );
    limpiarValidacionLocal();
  }

  async function manejarEnvio(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault();

    if (envioEnCursoRef.current || respuestaEnvio !== null) {
      return;
    }

    setErrorEnvio(null);
    setErroresBackend(null);
    setRespuestaEnvio(null);

    const errores = validarSolicitudFormulario(
      formulario,
      horarios,
      seleccionDeporte,
      seleccionCiudad,
      seleccionBarrio
    );
    setErroresFormulario(errores);

    if (tieneErroresFormulario(errores)) {
      setTimeout(() => {
        resumenErroresRef.current?.scrollIntoView({
          behavior: "smooth",
          block: "center",
        });
        resumenErroresRef.current?.focus();
      }, 100);
      return;
    }

    envioEnCursoRef.current = true;
    setProcesandoFormulario(true);

    try {
      if (esModoPublicador) {
        const solicitud = construirSolicitudPublicadorRequest(
          formulario,
          horarios
        );

        if (solicitud === null) {
          throw new Error(
            "No pudimos preparar la solicitud. Revisá los datos ingresados."
          );
        }

        if (!accessToken) {
          throw new PublicadorApiError(
            "Necesitás iniciar sesión como publicador para enviar la solicitud."
          );
        }

        const respuesta = await crearSolicitudPublicador(
          solicitud,
          accessToken
        );
        setRespuestaEnvio(respuesta);
      } else {
        const solicitud = construirSolicitudPublicacionRequest(
          formulario,
          horarios
        );

        if (solicitud === null) {
          throw new Error(
            "No pudimos preparar la solicitud. Revisá los datos ingresados."
          );
        }

        const respuesta = await enviarSolicitudPublicacion(solicitud);
        setRespuestaEnvio(respuesta);
      }

      setErrorEnvio(null);
      setErroresBackend(null);
      setTimeout(() => {
        resultadoEnvioRef.current?.scrollIntoView({
          behavior: "smooth",
          block: "center",
        });
        resultadoEnvioRef.current?.focus();
      }, 100);
    } catch (error: unknown) {
      if (
        error instanceof SolicitudPublicacionApiError ||
        error instanceof PublicadorApiError
      ) {
        setErrorEnvio(error.message);
        setErroresBackend(error.erroresPorCampo);
      } else if (error instanceof Error && error.message) {
        setErrorEnvio(error.message);
        setErroresBackend(null);
      } else {
        setErrorEnvio(
          "Ocurrió un problema inesperado al enviar la solicitud."
        );
        setErroresBackend(null);
      }

      setTimeout(() => {
        resultadoEnvioRef.current?.scrollIntoView({
          behavior: "smooth",
          block: "center",
        });
        resultadoEnvioRef.current?.focus();
      }, 100);
    } finally {
      setProcesandoFormulario(false);
      envioEnCursoRef.current = false;
    }
  }

  const ciudadExistenteSeleccionada =
    seleccionCiudad !== "" && seleccionCiudad !== OPCION_OTRO;
  const ciudadPersonalizadaSeleccionada = seleccionCiudad === OPCION_OTRO;
  const deportePersonalizadoSeleccionado = seleccionDeporte === OPCION_OTRO;
  const barrioPersonalizadoSeleccionado = seleccionBarrio === OPCION_OTRO;
  const mensajesErroresFormulario = obtenerMensajesErrores(erroresFormulario);
  const formularioTieneErrores = tieneErroresFormulario(erroresFormulario);

  return (
    <div className="mt-8 grid gap-6 lg:grid-cols-[0.78fr_1.22fr] lg:items-start">
      <aside className="lg:sticky lg:top-8">
        <SurfaceCard className="bg-white/90 p-4 lg:hidden">
          <details className="group">
            <summary className="flex cursor-pointer list-none items-start justify-between gap-3 rounded-[var(--radius-lg)] outline-none transition duration-200 ease-out focus-visible:ring-4 focus-visible:ring-[#4FB3D9]/30 focus-visible:ring-offset-2 [&::-webkit-details-marker]:hidden">
              <span>
                <span className="block text-xs font-extrabold uppercase tracking-[0.18em] text-[var(--color-secondary)]">
                  Guía rápida
                </span>
                <span className="mt-1 block text-lg font-extrabold leading-tight text-[var(--color-primary)]">
                  Publicá tu actividad en 5 pasos
                </span>
                <span className="mt-1 block text-sm leading-5 text-[var(--color-muted)]">
                  Revisá la guía cuando necesites orientarte.
                </span>
              </span>

              <span className="shrink-0 rounded-full border border-[#BFDDEA] bg-[#F8FCFE] px-3 py-2 text-xs font-extrabold text-[var(--color-primary)]">
                <span className="group-open:hidden">Ver pasos</span>
                <span className="hidden group-open:inline">Ocultar</span>
              </span>
            </summary>

            <ol className="mt-4 space-y-3">
              <li className="flex gap-3 rounded-[var(--radius-lg)] border border-[#DDEAF3] bg-[#F8FAFC] p-3">
                <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-[#E6F7EF] text-sm font-extrabold text-[#167A4A]">
                  1
                </span>
                <span>
                  <span className="block text-sm font-extrabold text-[var(--color-primary)]">
                    Contanos quién publica
                  </span>
                  <span className="mt-1 block text-sm leading-5 text-[var(--color-muted)]">
                    Club, gimnasio, profe, escuela o espacio deportivo.
                  </span>
                </span>
              </li>
              <li className="flex gap-3 rounded-[var(--radius-lg)] border border-[#DDEAF3] bg-[#F8FAFC] p-3">
                <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-[#E8F6FB] text-sm font-extrabold text-[#0F6F8F]">
                  2
                </span>
                <span>
                  <span className="block text-sm font-extrabold text-[var(--color-primary)]">
                    Describí la actividad
                  </span>
                  <span className="mt-1 block text-sm leading-5 text-[var(--color-muted)]">
                    Nombre, deporte, nivel, modalidad y descripción.
                  </span>
                </span>
              </li>
              <li className="flex gap-3 rounded-[var(--radius-lg)] border border-[#DDEAF3] bg-[#F8FAFC] p-3">
                <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-[#E6F7EF] text-sm font-extrabold text-[#167A4A]">
                  3
                </span>
                <span>
                  <span className="block text-sm font-extrabold text-[var(--color-primary)]">
                    Indicá dónde se realiza
                  </span>
                  <span className="mt-1 block text-sm leading-5 text-[var(--color-muted)]">
                    Ciudad, barrio, lugar o dirección de referencia.
                  </span>
                </span>
              </li>
              <li className="flex gap-3 rounded-[var(--radius-lg)] border border-[#DDEAF3] bg-[#F8FAFC] p-3">
                <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-[#E8F6FB] text-sm font-extrabold text-[#0F6F8F]">
                  4
                </span>
                <span>
                  <span className="block text-sm font-extrabold text-[var(--color-primary)]">
                    Sumá contacto y horarios
                  </span>
                  <span className="mt-1 block text-sm leading-5 text-[var(--color-muted)]">
                    Agregá cómo pueden contactarte y cuándo se dicta.
                  </span>
                </span>
              </li>
              <li className="flex gap-3 rounded-[var(--radius-lg)] border border-[#BDE8D0] bg-[#E6F7EF] p-3">
                <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-white text-sm font-extrabold text-[#167A4A]">
                  5
                </span>
                <span>
                  <span className="block text-sm font-extrabold text-[var(--color-primary)]">
                    Enviá la solicitud a revisión
                  </span>
                  <span className="mt-1 block text-sm leading-5 text-[#167A4A]">
                    El equipo de <BrandName className="inline font-bold" /> la
                    revisa antes de publicarla.
                  </span>
                </span>
              </li>
            </ol>
          </details>
        </SurfaceCard>

        <SurfaceCard className="hidden bg-white/90 p-5 lg:block">
          <p className="text-xs font-extrabold uppercase tracking-[0.18em] text-[var(--color-secondary)]">
            Antes de enviar
          </p>
          <h2 className="mt-2 text-2xl font-extrabold text-[var(--color-primary)]">
            Publicá tu actividad deportiva en 5 pasos simples
          </h2>
          <p className="mt-3 text-sm leading-6 text-[var(--color-muted)]">
            Te guiamos para cargar la información principal y enviar tu solicitud
            a revisión.
          </p>

          <ol className="mt-5 space-y-3">
            <li className="flex gap-3 rounded-[var(--radius-lg)] border border-[#DDEAF3] bg-[#F8FAFC] p-3">
              <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-[#E6F7EF] text-sm font-extrabold text-[#167A4A]">
                1
              </span>
              <span>
                <span className="block text-sm font-extrabold text-[var(--color-primary)]">
                  Contanos quién publica
                </span>
                <span className="mt-1 block text-sm leading-5 text-[var(--color-muted)]">
                  Club, gimnasio, profe, escuela o espacio deportivo.
                </span>
              </span>
            </li>
            <li className="flex gap-3 rounded-[var(--radius-lg)] border border-[#DDEAF3] bg-[#F8FAFC] p-3">
              <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-[#E8F6FB] text-sm font-extrabold text-[#0F6F8F]">
                2
              </span>
              <span>
                <span className="block text-sm font-extrabold text-[var(--color-primary)]">
                  Describí la actividad
                </span>
                <span className="mt-1 block text-sm leading-5 text-[var(--color-muted)]">
                  Nombre, deporte, nivel, modalidad y descripción.
                </span>
              </span>
            </li>
            <li className="flex gap-3 rounded-[var(--radius-lg)] border border-[#DDEAF3] bg-[#F8FAFC] p-3">
              <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-[#E6F7EF] text-sm font-extrabold text-[#167A4A]">
                3
              </span>
              <span>
                <span className="block text-sm font-extrabold text-[var(--color-primary)]">
                  Indicá dónde se realiza
                </span>
                <span className="mt-1 block text-sm leading-5 text-[var(--color-muted)]">
                  Ciudad, barrio, lugar o dirección de referencia.
                </span>
              </span>
            </li>
            <li className="flex gap-3 rounded-[var(--radius-lg)] border border-[#DDEAF3] bg-[#F8FAFC] p-3">
              <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-[#E8F6FB] text-sm font-extrabold text-[#0F6F8F]">
                4
              </span>
              <span>
                <span className="block text-sm font-extrabold text-[var(--color-primary)]">
                  Sumá contacto y horarios
                </span>
                <span className="mt-1 block text-sm leading-5 text-[var(--color-muted)]">
                  Agregá cómo pueden contactarte y cuándo se dicta.
                </span>
              </span>
            </li>
            <li className="flex gap-3 rounded-[var(--radius-lg)] border border-[#BDE8D0] bg-[#E6F7EF] p-3">
              <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-white text-sm font-extrabold text-[#167A4A]">
                5
              </span>
              <span>
                <span className="block text-sm font-extrabold text-[var(--color-primary)]">
                  Enviá la solicitud a revisión
                </span>
                <span className="mt-1 block text-sm leading-5 text-[#167A4A]">
                  El equipo de <BrandName className="inline font-bold" /> la
                  revisa antes de publicarla.
                </span>
              </span>
            </li>
          </ol>
        </SurfaceCard>
      </aside>

      <form onSubmit={manejarEnvio} className="space-y-5">
        <p className="rounded-[var(--radius-md)] border border-[#DDEAF3] bg-white px-4 py-3 text-sm font-bold text-[var(--color-muted)]">
          Los campos marcados con{" "}
          <span className="font-bold text-[var(--color-secondary)]">*</span> son
          obligatorios.
        </p>

      {cargandoCatalogosIniciales && (
        <div
          role="status"
          aria-live="polite"
          className="rounded-[var(--radius-lg)] border border-[#BFDDEA] bg-[#E8F6FB] p-4 text-sm font-bold leading-6 text-[#0F6F8F]"
        >
          Cargando deportes y ciudades...
        </div>
      )}

      {errorCatalogosIniciales && (
        <div
          role="alert"
          aria-live="assertive"
          className="rounded-[var(--radius-lg)] border border-[#F6C56D] bg-[#FFF4E5] p-4 text-sm leading-6 text-[#8A4B00]"
        >
          <p className="font-bold">{errorCatalogosIniciales}</p>
          <AppButton
            type="button"
            onClick={() => void cargarCatalogosIniciales()}
            variant="outline"
            size="sm"
            className="mt-3 border-[#8A4B00] text-[#8A4B00]"
          >
            Reintentar
          </AppButton>
        </div>
      )}

      {formularioTieneErrores && (
        <div
          ref={resumenErroresRef}
          tabIndex={-1}
          role="alert"
          aria-live="assertive"
          className="rounded-[var(--radius-lg)] border border-[#F6C56D] bg-[#FFF4E5] p-4 text-sm leading-6 text-[#8A4B00] outline-none focus:ring-2 focus:ring-[#8A4B00]"
        >
          <h2 className="font-extrabold text-[var(--color-primary)]">
            Revisá los siguientes datos
          </h2>
          <ul className="mt-2 list-disc space-y-1 pl-5">
            {mensajesErroresFormulario.map((mensaje) => (
              <li key={mensaje}>{mensaje}</li>
            ))}
          </ul>
        </div>
      )}
      {!esModoPublicador && (
        <fieldset className={fieldsetClassName}>
          <PublishSectionHeader
            paso={1}
            titulo="Datos del publicador"
            descripcion="Contanos quién ofrece la actividad para poder identificar la solicitud durante la revisión."
          />

          <div className="mt-4 grid gap-4 sm:grid-cols-2">
            <div className="flex flex-col gap-2">
              <label htmlFor="tipoPublicador" className={labelClassName}>
                Tipo de publicador <CampoObligatorio />
              </label>

              <select
                id="tipoPublicador"
                name="tipoPublicador"
                required
                value={formulario.tipoPublicador}
                onChange={(evento) =>
                  actualizarCampo(
                    "tipoPublicador",
                    evento.target
                      .value as SolicitudPublicacionFormState["tipoPublicador"]
                  )
                }
                className={inputClassName}
              >
                <option value="">Seleccionar tipo</option>
                {TIPOS_PUBLICADOR_SOLICITUD.map((tipoPublicador) => (
                  <option key={tipoPublicador} value={tipoPublicador}>
                    {formatearEtiquetaCatalogo(tipoPublicador)}
                  </option>
                ))}
              </select>
            </div>

            <div className="flex flex-col gap-2">
              <label htmlFor="nombrePublicador" className={labelClassName}>
                Nombre del club, gimnasio, institución o profesor{" "}
                <CampoObligatorio />
              </label>

              <input
                id="nombrePublicador"
                name="nombrePublicador"
                type="text"
                required
                maxLength={150}
                autoComplete="organization"
                value={formulario.nombrePublicador}
                onChange={(evento) =>
                  manejarCambioTexto("nombrePublicador", evento)
                }
                className={inputClassName}
              />
            </div>
          </div>
        </fieldset>
      )}
      <fieldset className={fieldsetClassName}>
        <PublishSectionHeader
          paso={obtenerNumeroPaso(2)}
          titulo="Datos de la actividad"
          descripcion="Sumá la información principal para que podamos revisar la publicación con contexto claro."
        />

        <div className="mt-4 grid gap-4 sm:grid-cols-2">
          <div className="flex flex-col gap-2 sm:col-span-2">
            <label htmlFor="nombreActividad" className={labelClassName}>
              Nombre de la actividad <CampoObligatorio />
            </label>

            <input
              id="nombreActividad"
              name="nombreActividad"
              type="text"
              required
              maxLength={150}
              value={formulario.nombreActividad}
              onChange={(evento) =>
                manejarCambioTexto("nombreActividad", evento)
              }
              className={inputClassName}
            />
          </div>

          <div className="flex flex-col gap-2">
            <label htmlFor="deporteSeleccion" className={labelClassName}>
              Deporte <CampoObligatorio />
            </label>

            <select
              id="deporteSeleccion"
              name="deporteId"
              required
              value={seleccionDeporte}
              aria-invalid={Boolean(erroresFormulario.deporte)}
              aria-describedby={
                erroresFormulario.deporte ? "error-deporte" : undefined
              }
              disabled={cargandoCatalogosIniciales}
              onChange={manejarSeleccionDeporte}
              className={inputClassName}
            >
              <option value="">Seleccionar deporte</option>
              {deportes.map((deporte) => (
                <option key={deporte.id} value={String(deporte.id)}>
                  {deporte.nombre}
                </option>
              ))}
              <option value={OPCION_OTRO}>Otro deporte</option>
            </select>
          </div>

          {deportePersonalizadoSeleccionado && (
            <div className="flex flex-col gap-2">
              <label htmlFor="deporteOtro" className={labelClassName}>
                Otro deporte <CampoObligatorio />
              </label>

              <input
                id="deporteOtro"
                name="deporteOtro"
                type="text"
                required
                maxLength={100}
                value={formulario.deporteOtro}
                aria-invalid={Boolean(erroresFormulario.deporte)}
                aria-describedby={
                  erroresFormulario.deporte ? "error-deporte" : undefined
                }
                onChange={(evento) =>
                  manejarCambioTexto("deporteOtro", evento)
                }
                className={inputClassName}
              />
            </div>
          )}

          {erroresFormulario.deporte && (
            <p
              id="error-deporte"
              role="alert"
              className="text-sm font-bold text-[#9A3D3D] sm:col-span-2"
            >
              {erroresFormulario.deporte}
            </p>
          )}

          <div className="flex flex-col gap-2">
            <label htmlFor="nivel" className={labelClassName}>
              Nivel <CampoObligatorio />
            </label>

            <select
              id="nivel"
              name="nivel"
              required
              value={formulario.nivel}
              onChange={(evento) =>
                actualizarCampo(
                  "nivel",
                  evento.target.value as SolicitudPublicacionFormState["nivel"]
                )
              }
              className={inputClassName}
            >
              <option value="">Seleccionar nivel</option>
              {NIVELES_SOLICITUD.map((nivel) => (
                <option key={nivel} value={nivel}>
                  {formatearEtiquetaCatalogo(nivel)}
                </option>
              ))}
            </select>
          </div>

          <div className="flex flex-col gap-2">
            <label htmlFor="enfoque" className={labelClassName}>
              Enfoque <CampoObligatorio />
            </label>

            <select
              id="enfoque"
              name="enfoque"
              required
              value={formulario.enfoque}
              onChange={(evento) =>
                actualizarCampo(
                  "enfoque",
                  evento.target
                    .value as SolicitudPublicacionFormState["enfoque"]
                )
              }
              className={inputClassName}
            >
              <option value="">Seleccionar enfoque</option>
              {ENFOQUES_SOLICITUD.map((enfoque) => (
                <option key={enfoque} value={enfoque}>
                  {formatearEtiquetaCatalogo(enfoque)}
                </option>
              ))}
            </select>
          </div>

          <div className="flex flex-col gap-2">
            <label htmlFor="modalidad" className={labelClassName}>
              Modalidad <CampoObligatorio />
            </label>

            <select
              id="modalidad"
              name="modalidad"
              required
              value={formulario.modalidad}
              onChange={(evento) =>
                actualizarCampo(
                  "modalidad",
                  evento.target
                    .value as SolicitudPublicacionFormState["modalidad"]
                )
              }
              className={inputClassName}
            >
              <option value="">Seleccionar modalidad</option>
              {MODALIDADES_SOLICITUD.map((modalidad) => (
                <option key={modalidad} value={modalidad}>
                  {formatearEtiquetaCatalogo(modalidad)}
                </option>
              ))}
            </select>
          </div>

          <div className="flex flex-col gap-2 sm:col-span-2">
            <label htmlFor="descripcion" className={labelClassName}>
              Descripción <CampoObligatorio />
            </label>

            <textarea
              id="descripcion"
              name="descripcion"
              rows={5}
              required
              value={formulario.descripcion}
              onChange={(evento) => manejarCambioTexto("descripcion", evento)}
              className={textareaClassName}
            />
          </div>

          <div className="flex flex-col gap-2">
            <label htmlFor="edadMinima" className={labelClassName}>
              Edad mínima
            </label>

            <input
              id="edadMinima"
              name="edadMinima"
              type="number"
              min="0"
              value={formulario.edadMinima}
              aria-invalid={Boolean(erroresFormulario.edades)}
              aria-describedby={
                erroresFormulario.edades ? "error-edades" : undefined
              }
              onChange={(evento) => manejarCambioTexto("edadMinima", evento)}
              className={inputClassName}
            />
          </div>

          <div className="flex flex-col gap-2">
            <label htmlFor="edadMaxima" className={labelClassName}>
              Edad máxima
            </label>

            <input
              id="edadMaxima"
              name="edadMaxima"
              type="number"
              min="0"
              value={formulario.edadMaxima}
              aria-invalid={Boolean(erroresFormulario.edades)}
              aria-describedby={
                erroresFormulario.edades ? "error-edades" : undefined
              }
              onChange={(evento) => manejarCambioTexto("edadMaxima", evento)}
              className={inputClassName}
            />
          </div>

          {erroresFormulario.edades && (
            <p
              id="error-edades"
              role="alert"
              className="text-sm font-bold text-[#9A3D3D] sm:col-span-2"
            >
              {erroresFormulario.edades}
            </p>
          )}

          <div className="flex items-start gap-3 rounded-[var(--radius-md)] bg-[#E8F6FB] p-4 sm:col-span-2">
            <input
              id="mostrarPrecio"
              name="mostrarPrecio"
              type="checkbox"
              checked={formulario.mostrarPrecio}
              onChange={manejarCambioMostrarPrecio}
              className="mt-1 h-5 w-5 accent-[var(--color-secondary)]"
            />

            <label
              htmlFor="mostrarPrecio"
              className="text-sm font-bold leading-6 text-[var(--color-primary)]"
            >
              Mostrar precio de referencia
            </label>
          </div>

          {formulario.mostrarPrecio && (
            <div className="flex flex-col gap-2 sm:col-span-2">
              <label htmlFor="precioReferencia" className={labelClassName}>
                Precio de referencia
              </label>

              <input
                id="precioReferencia"
                name="precioReferencia"
                type="number"
                min="0"
                step="0.01"
                value={formulario.precioReferencia}
                aria-invalid={Boolean(erroresFormulario.precioReferencia)}
                aria-describedby={
                  erroresFormulario.precioReferencia
                    ? "error-precioReferencia"
                    : undefined
                }
                onChange={(evento) =>
                  manejarCambioTexto("precioReferencia", evento)
                }
                className={inputClassName}
              />
              {erroresFormulario.precioReferencia && (
                <p
                  id="error-precioReferencia"
                  role="alert"
                  className="text-sm font-bold text-[#9A3D3D]"
                >
                  {erroresFormulario.precioReferencia}
                </p>
              )}
            </div>
          )}
        </div>
      </fieldset>
      <fieldset className={fieldsetClassName}>
        <PublishSectionHeader
          paso={obtenerNumeroPaso(3)}
          titulo="Ubicación"
          descripcion="Indicá dónde se realiza la actividad. Completá al menos el nombre del lugar o la dirección."
        />

        <div className="mt-4 grid gap-4 sm:grid-cols-2">
          <div className="flex flex-col gap-2">
            <label htmlFor="ciudadSeleccion" className={labelClassName}>
              Ciudad <CampoObligatorio />
            </label>

            <select
              id="ciudadSeleccion"
              name="ciudadId"
              required
              value={seleccionCiudad}
              aria-invalid={Boolean(erroresFormulario.ciudad)}
              aria-describedby={
                erroresFormulario.ciudad ? "error-ciudad" : undefined
              }
              disabled={cargandoCatalogosIniciales}
              onChange={manejarSeleccionCiudad}
              className={inputClassName}
            >
              <option value="">Seleccionar ciudad</option>
              {ciudades.map((ciudad) => (
                <option key={ciudad.id} value={String(ciudad.id)}>
                  {ciudad.nombre}
                </option>
              ))}
              <option value={OPCION_OTRO}>Otra ciudad</option>
            </select>
          </div>

          {ciudadPersonalizadaSeleccionada && (
            <div className="flex flex-col gap-2">
              <label htmlFor="ciudadOtra" className={labelClassName}>
                Otra ciudad <CampoObligatorio />
              </label>

              <input
                id="ciudadOtra"
                name="ciudadOtra"
                type="text"
                required
                maxLength={100}
                value={formulario.ciudadOtra}
                aria-invalid={Boolean(erroresFormulario.ciudad)}
                aria-describedby={
                  erroresFormulario.ciudad ? "error-ciudad" : undefined
                }
                onChange={(evento) => manejarCambioTexto("ciudadOtra", evento)}
                className={inputClassName}
              />
            </div>
          )}

          {erroresFormulario.ciudad && (
            <p
              id="error-ciudad"
              role="alert"
              className="text-sm font-bold text-[#9A3D3D] sm:col-span-2"
            >
              {erroresFormulario.ciudad}
            </p>
          )}

          {seleccionCiudad === "" && (
            <div className="flex flex-col gap-2">
              <label htmlFor="barrioSeleccion" className={labelClassName}>
                Barrio <CampoObligatorio />
              </label>

              <select
                id="barrioSeleccion"
                name="barrioId"
                value=""
                disabled
                className={inputClassName}
              >
                <option value="">Primero seleccioná una ciudad</option>
              </select>
            </div>
          )}

          {ciudadExistenteSeleccionada && (
            <div className="flex flex-col gap-2">
              <label htmlFor="barrioSeleccion" className={labelClassName}>
                Barrio <CampoObligatorio />
              </label>

              <select
                id="barrioSeleccion"
                name="barrioId"
                required
                value={seleccionBarrio}
                aria-invalid={Boolean(erroresFormulario.barrio)}
                aria-describedby={
                  erroresFormulario.barrio ? "error-barrio" : undefined
                }
                disabled={cargandoBarrios}
                onChange={manejarSeleccionBarrio}
                className={inputClassName}
              >
                <option value="">Seleccionar barrio</option>
                {barrios.map((barrio) => (
                  <option key={barrio.id} value={String(barrio.id)}>
                    {barrio.nombre}
                  </option>
                ))}
                <option value={OPCION_OTRO}>Otro barrio</option>
              </select>

              {cargandoBarrios && (
                <p
                  role="status"
                  aria-live="polite"
                  className="text-sm font-bold text-[#0F6F8F]"
                >
                  Cargando barrios...
                </p>
              )}

              {errorBarrios && (
                <div
                  role="alert"
                  aria-live="assertive"
                  className="rounded-[var(--radius-md)] bg-[#FFF4E5] p-3 text-sm leading-6 text-[#8A4B00]"
                >
                  <p className="font-bold">{errorBarrios}</p>
                  <AppButton
                    type="button"
                    onClick={manejarReintentarBarrios}
                    variant="outline"
                    size="sm"
                    className="mt-2 border-[#8A4B00] text-[#8A4B00]"
                  >
                    Reintentar barrios
                  </AppButton>
                </div>
              )}
            </div>
          )}

          {ciudadPersonalizadaSeleccionada && (
            <div className="flex flex-col gap-2">
              <label htmlFor="barrioOtro" className={labelClassName}>
                Barrio <CampoObligatorio />
              </label>

              <p className="text-sm leading-6 text-[var(--color-muted)]">
                Al cargar una ciudad nueva, indicá también el barrio en texto.
              </p>

              <input
                id="barrioOtro"
                name="barrioOtro"
                type="text"
                required
                maxLength={100}
                value={formulario.barrioOtro}
                aria-invalid={Boolean(erroresFormulario.barrio)}
                aria-describedby={
                  erroresFormulario.barrio ? "error-barrio" : undefined
                }
                onChange={(evento) => manejarCambioTexto("barrioOtro", evento)}
                className={inputClassName}
              />
            </div>
          )}

          {ciudadExistenteSeleccionada && barrioPersonalizadoSeleccionado && (
            <div className="flex flex-col gap-2 sm:col-span-2">
              <label htmlFor="barrioOtro" className={labelClassName}>
                Otro barrio <CampoObligatorio />
              </label>

              <input
                id="barrioOtro"
                name="barrioOtro"
                type="text"
                required
                maxLength={100}
                value={formulario.barrioOtro}
                aria-invalid={Boolean(erroresFormulario.barrio)}
                aria-describedby={
                  erroresFormulario.barrio ? "error-barrio" : undefined
                }
                onChange={(evento) => manejarCambioTexto("barrioOtro", evento)}
                className={inputClassName}
              />
            </div>
          )}

          {erroresFormulario.barrio && (
            <p
              id="error-barrio"
              role="alert"
              className="text-sm font-bold text-[#9A3D3D] sm:col-span-2"
            >
              {erroresFormulario.barrio}
            </p>
          )}

          <div className="flex flex-col gap-2">
            <label htmlFor="nombreLugar" className={labelClassName}>
              Nombre del lugar
            </label>

            <input
              id="nombreLugar"
              name="nombreLugar"
              type="text"
              maxLength={150}
              value={formulario.nombreLugar}
              aria-invalid={Boolean(erroresFormulario.ubicacion)}
              aria-describedby={
                erroresFormulario.ubicacion ? "error-ubicacion" : undefined
              }
              onChange={(evento) => manejarCambioTexto("nombreLugar", evento)}
              className={inputClassName}
            />
          </div>

          <div className="flex flex-col gap-2">
            <label htmlFor="direccion" className={labelClassName}>
              Dirección
            </label>

            <input
              id="direccion"
              name="direccion"
              type="text"
              maxLength={255}
              autoComplete="street-address"
              value={formulario.direccion}
              aria-invalid={Boolean(erroresFormulario.ubicacion)}
              aria-describedby={
                erroresFormulario.ubicacion ? "error-ubicacion" : undefined
              }
              onChange={(evento) => manejarCambioTexto("direccion", evento)}
              className={inputClassName}
            />
          </div>

          {erroresFormulario.ubicacion && (
            <p
              id="error-ubicacion"
              role="alert"
              className="text-sm font-bold text-[#9A3D3D] sm:col-span-2"
            >
              {erroresFormulario.ubicacion}
            </p>
          )}

          <div className="flex flex-col gap-2 sm:col-span-2">
            <label htmlFor="referenciaUbicacion" className={labelClassName}>
              Referencia de ubicación
            </label>

            <input
              id="referenciaUbicacion"
              name="referenciaUbicacion"
              type="text"
              maxLength={255}
              value={formulario.referenciaUbicacion}
              onChange={(evento) =>
                manejarCambioTexto("referenciaUbicacion", evento)
              }
              className={inputClassName}
            />
          </div>
        </div>
      </fieldset>
      <fieldset className={fieldsetClassName}>
        <PublishSectionHeader
          paso={obtenerNumeroPaso(4)}
          titulo="Contacto"
          descripcion="Completá al menos WhatsApp o email para que podamos contactarte si necesitamos confirmar datos."
        />

        <div className="mt-4 grid gap-4 sm:grid-cols-2">
          <div className="flex flex-col gap-2">
            <label htmlFor="whatsapp" className={labelClassName}>
              WhatsApp
            </label>

            <input
              id="whatsapp"
              name="whatsapp"
              type="text"
              inputMode="tel"
              autoComplete="tel"
              maxLength={40}
              value={formulario.whatsapp}
              aria-invalid={Boolean(erroresFormulario.contacto)}
              aria-describedby={
                erroresFormulario.contacto ? "error-contacto" : undefined
              }
              onChange={(evento) => manejarCambioTexto("whatsapp", evento)}
              className={inputClassName}
            />
          </div>

          <div className="flex flex-col gap-2">
            <label htmlFor="instagram" className={labelClassName}>
              Instagram
            </label>

            <input
              id="instagram"
              name="instagram"
              type="text"
              maxLength={150}
              value={formulario.instagram}
              onChange={(evento) => manejarCambioTexto("instagram", evento)}
              className={inputClassName}
            />
          </div>

          <div className="flex flex-col gap-2 sm:col-span-2">
            <label htmlFor="email" className={labelClassName}>
              Email
            </label>

            <input
              id="email"
              name="email"
              type="email"
              autoComplete="email"
              maxLength={150}
              value={formulario.email}
              aria-invalid={Boolean(erroresFormulario.contacto)}
              aria-describedby={
                erroresFormulario.contacto ? "error-contacto" : undefined
              }
              onChange={(evento) => manejarCambioTexto("email", evento)}
              className={inputClassName}
            />
          </div>
          {erroresFormulario.contacto && (
            <p
              id="error-contacto"
              role="alert"
              className="text-sm font-bold text-[#9A3D3D] sm:col-span-2"
            >
              {erroresFormulario.contacto}
            </p>
          )}
        </div>
      </fieldset>

      <fieldset className={fieldsetClassName}>
        <PublishSectionHeader
          paso={obtenerNumeroPaso(5)}
          titulo="Horarios"
          descripcion="Cargá los días y rangos horarios en los que se realiza la actividad."
        />

        <div className="mt-2 space-y-4">
          <div className="rounded-[var(--radius-lg)] border border-[#BFDDEA] bg-[#E8F6FB] p-4 text-sm leading-6 text-[#0F6F8F]">
            <h3 className="font-extrabold text-[var(--color-primary)]">
              Horarios de la actividad
            </h3>
            <p className="mt-2">
              Agregá al menos un día y un rango horario. Podés agregar tantos
              horarios como necesites.
            </p>
            <p role="status" aria-live="polite" className="mt-2 font-bold">
              {horarios.length === 1
                ? "1 horario cargado"
                : `${horarios.length} horarios cargados`}
            </p>
          </div>

          {(erroresFormulario.horarios || erroresFormulario.horariosDuplicados) && (
            <p
              id="error-horarios"
              role="alert"
              className="text-sm font-bold text-[#9A3D3D]"
            >
              {erroresFormulario.horarios ?? erroresFormulario.horariosDuplicados}
            </p>
          )}

          <div className="space-y-4">
            {horarios.map((horario, indice) => {
              const diaId = `horario-dia-${horario.idInterno}`;
              const horaInicioId = `horario-hora-inicio-${horario.idInterno}`;
              const horaFinId = `horario-hora-fin-${horario.idInterno}`;
              const observacionId = `horario-observacion-${horario.idInterno}`;
              const erroresHorario =
                erroresFormulario.horariosPorId[horario.idInterno] ?? {};
              const diaErrorId = `error-horario-dia-${horario.idInterno}`;
              const horaInicioErrorId = `error-horario-hora-inicio-${horario.idInterno}`;
              const horaFinErrorId = `error-horario-hora-fin-${horario.idInterno}`;
              const duplicadoErrorId = `error-horario-duplicado-${horario.idInterno}`;

              return (
                <div
                  key={horario.idInterno}
                  className="rounded-[var(--radius-lg)] border border-[#DDEAF3] bg-[#F8FAFC] p-4 shadow-[0_10px_26px_rgba(12,52,80,0.06)]"
                >
                  <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                    <h3 className="inline-flex w-fit rounded-full bg-[#E6F7EF] px-3 py-1 text-sm font-extrabold text-[#167A4A]">
                      Horario {indice + 1}
                    </h3>

                    {horarios.length > 1 && (
                      <button
                        type="button"
                        aria-label={`Quitar horario ${indice + 1}`}
                        onClick={() => quitarHorario(horario.idInterno)}
                        className="min-h-10 rounded-[var(--radius-md)] border border-[#C96B6B] bg-white px-4 text-sm font-bold text-[#9A3D3D] transition duration-200 ease-out hover:bg-[#FFF4F4] active:scale-[0.98]"
                      >
                        Quitar
                      </button>
                    )}
                  </div>

                  {erroresHorario.duplicado && (
                    <p
                      id={duplicadoErrorId}
                      role="alert"
                      className="mt-3 text-sm font-bold text-[#9A3D3D]"
                    >
                      {erroresHorario.duplicado}
                    </p>
                  )}

                  <div className="mt-4 grid gap-4 md:grid-cols-3">
                    <div className="flex flex-col gap-2">
                      <label htmlFor={diaId} className={labelClassName}>
                        Día <CampoObligatorio />
                      </label>

                      <select
                        id={diaId}
                        name={`horarios.${horario.idInterno}.diaSemana`}
                        required
                        value={horario.diaSemana}
                        aria-invalid={Boolean(erroresHorario.diaSemana)}
                        aria-describedby={
                          erroresHorario.diaSemana ? diaErrorId : undefined
                        }
                        onChange={(evento) =>
                          actualizarHorario(
                            horario.idInterno,
                            "diaSemana",
                            evento.target
                              .value as HorarioPublicacionFormState["diaSemana"]
                          )
                        }
                        className={inputClassName}
                      >
                        <option value="">Seleccionar día</option>
                        {DIAS_SEMANA_SOLICITUD.map((diaSemana) => (
                          <option key={diaSemana} value={diaSemana}>
                            {formatearEtiquetaCatalogo(diaSemana)}
                          </option>
                        ))}
                      </select>
                      {erroresHorario.diaSemana && (
                        <p
                          id={diaErrorId}
                          role="alert"
                          className="text-sm font-bold text-[#9A3D3D]"
                        >
                          {erroresHorario.diaSemana}
                        </p>
                      )}
                    </div>

                    <div className="flex flex-col gap-2">
                      <label htmlFor={horaInicioId} className={labelClassName}>
                        Hora de inicio <CampoObligatorio />
                      </label>

                      <input
                        id={horaInicioId}
                        name={`horarios.${horario.idInterno}.horaInicio`}
                        type="time"
                        required
                        value={horario.horaInicio}
                        aria-invalid={Boolean(erroresHorario.horaInicio)}
                        aria-describedby={
                          erroresHorario.horaInicio
                            ? horaInicioErrorId
                            : undefined
                        }
                        onChange={(evento) =>
                          actualizarHorario(
                            horario.idInterno,
                            "horaInicio",
                            evento.target.value
                          )
                        }
                        className={inputClassName}
                      />
                      {erroresHorario.horaInicio && (
                        <p
                          id={horaInicioErrorId}
                          role="alert"
                          className="text-sm font-bold text-[#9A3D3D]"
                        >
                          {erroresHorario.horaInicio}
                        </p>
                      )}
                    </div>

                    <div className="flex flex-col gap-2">
                      <label htmlFor={horaFinId} className={labelClassName}>
                        Hora de finalización <CampoObligatorio />
                      </label>

                      <input
                        id={horaFinId}
                        name={`horarios.${horario.idInterno}.horaFin`}
                        type="time"
                        required
                        value={horario.horaFin}
                        aria-invalid={Boolean(erroresHorario.horaFin)}
                        aria-describedby={
                          erroresHorario.horaFin ? horaFinErrorId : undefined
                        }
                        onChange={(evento) =>
                          actualizarHorario(
                            horario.idInterno,
                            "horaFin",
                            evento.target.value
                          )
                        }
                        className={inputClassName}
                      />
                      {erroresHorario.horaFin && (
                        <p
                          id={horaFinErrorId}
                          role="alert"
                          className="text-sm font-bold text-[#9A3D3D]"
                        >
                          {erroresHorario.horaFin}
                        </p>
                      )}
                    </div>

                    <div className="flex flex-col gap-2 md:col-span-3">
                      <label htmlFor={observacionId} className={labelClassName}>
                        Observación
                      </label>

                      <textarea
                        id={observacionId}
                        name={`horarios.${horario.idInterno}.observacion`}
                        rows={2}
                        maxLength={255}
                        value={horario.observacion}
                        onChange={(evento) =>
                          actualizarHorario(
                            horario.idInterno,
                            "observacion",
                            evento.target.value
                          )
                        }
                        className={textareaClassName}
                      />
                    </div>
                  </div>
                </div>
              );
            })}
          </div>

          <AppButton
            type="button"
            onClick={agregarHorario}
            variant="secondary"
            size="lg"
          >
            Agregar horario
          </AppButton>
        </div>
      </fieldset>
      <fieldset className={fieldsetClassName}>
        <PublishSectionHeader
          paso={obtenerNumeroPaso(6)}
          titulo="Confirmación"
          descripcion="Revisá los datos cargados y agregá cualquier aclaración útil para el equipo."
        />

        <div className="mt-4 grid gap-4">
          <div className="flex flex-col gap-2">
            <label
              htmlFor="observacionesSolicitante"
              className={labelClassName}
            >
              Observaciones
            </label>

            <textarea
              id="observacionesSolicitante"
              name="observacionesSolicitante"
              rows={4}
              value={formulario.observacionesSolicitante}
              onChange={(evento) =>
                manejarCambioTexto("observacionesSolicitante", evento)
              }
              className={textareaClassName}
            />
          </div>

          <div className="flex items-start gap-3 rounded-[var(--radius-lg)] border border-[#BDE8D0] bg-[#E6F7EF] p-4">
            <input
              id="aceptaCondiciones"
              name="aceptaCondiciones"
              type="checkbox"
              required
              checked={formulario.aceptaCondiciones}
              aria-invalid={Boolean(erroresFormulario.aceptaCondiciones)}
              aria-describedby={
                erroresFormulario.aceptaCondiciones
                  ? "error-aceptaCondiciones"
                  : undefined
              }
              onChange={(evento) =>
                actualizarCampo("aceptaCondiciones", evento.target.checked)
              }
              className="mt-1 h-5 w-5 accent-[var(--color-secondary)]"
            />

            <label
              htmlFor="aceptaCondiciones"
              className="text-sm font-bold leading-6 text-[var(--color-primary)]"
            >
              Confirmo que los datos ingresados son correctos y acepto que la
              solicitud sea revisada antes de publicarse. <CampoObligatorio />
            </label>
          </div>
          {erroresFormulario.aceptaCondiciones && (
            <p
              id="error-aceptaCondiciones"
              role="alert"
              className="text-sm font-bold text-[#9A3D3D]"
            >
              {erroresFormulario.aceptaCondiciones}
            </p>
          )}
        </div>
      </fieldset>

      <div className="rounded-[var(--radius-lg)] border border-[#BFDDEA] bg-[#E8F6FB] p-4 text-sm leading-6 text-[#0F6F8F]">
        <p className="font-extrabold text-[var(--color-primary)]">
          La solicitud quedará pendiente de revisión
        </p>
        <p className="mt-2">
          No publica automáticamente la actividad. El equipo podrá contactar al
          solicitante para confirmar información.
        </p>
      </div>

      {errorEnvio && (
        <div
          ref={resultadoEnvioRef}
          tabIndex={-1}
          role="alert"
          aria-live="assertive"
          className="rounded-[var(--radius-lg)] border border-[#F6C56D] bg-[#FFF4E5] p-4 text-sm leading-6 text-[#8A4B00] outline-none focus:ring-2 focus:ring-[#8A4B00]"
        >
          <h2 className="font-extrabold text-[var(--color-primary)]">
            No pudimos enviar la solicitud
          </h2>
          <p className="mt-2 font-bold">{errorEnvio}</p>
          {erroresBackend !== null && (
            <ul className="mt-2 list-disc space-y-1 pl-5">
              {Array.from(new Set(Object.values(erroresBackend))).map(
                (mensaje) => (
                  <li key={mensaje}>{mensaje}</li>
                )
              )}
            </ul>
          )}
        </div>
      )}

      {respuestaEnvio && (
        <div
          ref={resultadoEnvioRef}
          tabIndex={-1}
          role="status"
          aria-live="polite"
          className="rounded-[var(--radius-lg)] border border-[#BDE8D0] bg-[#E6F7EF] p-4 text-sm leading-6 text-[#167A4A] outline-none focus:ring-2 focus:ring-[#167A4A]"
        >
          <h2 className="font-extrabold text-[var(--color-primary)]">
            {tituloExitoPersonalizado ?? "Tu solicitud fue enviada correctamente"}
          </h2>
          <p className="mt-2">La vamos a revisar antes de publicarla.</p>
          <p className="mt-2 font-bold">{respuestaEnvio.mensaje}</p>
          <div className="mt-3 rounded-[var(--radius-md)] border border-[#BDE8D0] bg-white p-3">
            <p className="text-xs font-bold uppercase tracking-wide text-[var(--color-muted)]">
              Código de seguimiento
            </p>
            <p className="mt-1 text-lg font-extrabold text-[var(--color-primary)]">
              {respuestaEnvio.codigoSeguimiento}
            </p>
          </div>
          <p className="mt-3">
            Estado:{" "}
            <span className="font-bold">
              {formatearEtiquetaCatalogo(respuestaEnvio.estado)}
            </span>
          </p>
          <p className="mt-2 font-bold">
            Guardá este código para identificar tu solicitud.
          </p>
        </div>
      )}

      <div className="flex flex-col gap-3 sm:flex-row sm:justify-end">
        <AppButton
          type="submit"
          disabled={procesandoFormulario || respuestaEnvio !== null}
          size="lg"
          fullWidth
          className="sm:w-auto"
        >
          {procesandoFormulario
            ? "Enviando..."
            : respuestaEnvio
              ? "Solicitud enviada"
              : "Enviar solicitud"}
        </AppButton>
      </div>
      </form>
    </div>
  );
}

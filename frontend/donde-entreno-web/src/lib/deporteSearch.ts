import type { Deporte } from "../types/deporte";

export type CategoriaBusquedaDeporte = {
  valor: string;
  nombre: string;
};

export type SugerenciaBusquedaDeporte = {
  id: string;
  tipo: "deporte" | "categoria";
  label: string;
  valor: string;
  textoAyuda: string;
};

export type DestinoBusquedaDeporte = {
  tipo: "deporte" | "categoria";
  label: string;
  valor: string;
};

type BusquedaParaMatch = {
  textos: string[];
  compactos: string[];
  tokens: string[];
};

type VariantesBusquedaPuntaje = {
  textos: string[];
  compactos: string[];
};

const PUNTAJE_NOMBRE_DEPORTE_EXACTO = 1000;
const PUNTAJE_SLUG_DEPORTE_EXACTO = 980;
const PUNTAJE_ALIAS_DEPORTE_EXACTO = 950;
const PUNTAJE_CATEGORIA_EXACTA = 900;
const PUNTAJE_NOMBRE_DEPORTE_EMPIEZA = 850;
const PUNTAJE_ALIAS_DEPORTE_EMPIEZA = 820;
const PUNTAJE_NOMBRE_ALIAS_CONTIENE = 750;
const PUNTAJE_ALIAS_CATEGORIA_EXACTA = 700;
const PUNTAJE_DEPORTE_RELACIONADO_CATEGORIA = 450;
const PUNTAJE_DESCRIPCION_CONTIENE = 250;

const aliasesPorDeporteSlug: Record<string, string[]> = {
  boxeo: [
    "boxeo",
    "boxing",
    "box",
    "clases de boxeo",
    "entrenar boxeo",
    "deporte de combate",
    "pelea",
  ],
  "jiu-jitsu": [
    "bjj",
    "b j j",
    "bi jei jei",
    "jiujitsu",
    "jiu jitsu",
    "jiu-jitsu",
    "jiu jitzu",
    "jiu jutsu",
    "ju jitsu",
    "jujitsu",
    "brazilian jiu jitsu",
    "brazilian jiujitsu",
    "jiu jitsu brasileño",
    "jiu jitsu brasilero",
    "grappling",
    "submission grappling",
    "arte suave",
  ],
  futbol: [
    "futbol",
    "fútbol",
    "football",
    "soccer",
    "fulbo",
    "cancha",
    "pelota",
  ],
  gimnasio: [
    "gimnasio",
    "gym",
    "fitness",
    "entrenamiento",
    "entrenar",
    "sala de musculacion",
  ],
  natacion: [
    "natacion",
    "natación",
    "pileta",
    "piscina",
    "nadar",
    "swimming",
    "agua",
  ],
  musculacion: [
    "musculacion",
    "musculación",
    "pesas",
    "fuerza",
    "hipertrofia",
    "entrenamiento de fuerza",
  ],
  "cross-training": [
    "cross training",
    "crossfit",
    "funcional",
    "entrenamiento funcional",
    "wod",
  ],
  yoga: [
    "yoga",
    "yog",
    "relajacion",
    "relajación",
    "flexibilidad",
    "movilidad",
  ],
  pilates: ["pilates", "reformer", "pilate", "postura", "core"],
  running: ["running", "correr", "trotar", "atletismo", "runner"],
  tenis: ["tenis", "tennis", "raqueta"],
  padel: ["padel", "pádel", "paddle", "paleta"],
  basquet: ["basquet", "básquet", "basket", "basketball"],
  voley: ["voley", "vóley", "volleyball", "volley"],
  karate: ["karate", "artes marciales", "defensa personal"],
  taekwondo: ["taekwondo", "tae kwon do", "artes marciales", "defensa personal"],
  kickboxing: ["kickboxing", "kick boxing", "k1", "combate"],
  "muay-thai": [
    "muay thai",
    "muay-thai",
    "thai boxing",
    "boxeo tailandes",
    "boxeo tailandés",
  ],
  judo: ["judo", "arte marcial", "lucha"],
  hockey: ["hockey", "palo", "bocha"],
  stretching: [
    "stretching",
    "elongacion",
    "elongación",
    "movilidad",
    "flexibilidad",
  ],
  calistenia: ["calistenia", "calisthenics", "barras", "peso corporal"],
  cycling: ["cycling", "ciclismo", "bici", "bicicleta", "indoor bike", "spinning"],
};

const aliasesPorCategoria: Record<string, string[]> = {
  "deportes-de-combate": [
    "deportes de combate",
    "deportes de contacto",
    "artes marciales",
    "pelea",
    "lucha",
    "defensa personal",
    "mma",
    "grappling",
    "striking",
    "combate",
  ],
  "actividades-acuaticas": [
    "actividades acuaticas",
    "actividades acuáticas",
    "agua",
    "pileta",
    "piscina",
    "nadar",
    "natacion",
    "natación",
  ],
  "fitness-y-entrenamiento": [
    "fitness",
    "entrenamiento",
    "gym",
    "gimnasio",
    "fuerza",
    "pesas",
    "funcional",
    "crossfit",
    "musculacion",
    "musculación",
  ],
  "bienestar-y-salud": [
    "bienestar",
    "salud",
    "yoga",
    "pilates",
    "movilidad",
    "elongacion",
    "elongación",
    "relajacion",
    "relajación",
  ],
  "deportes-de-equipo": [
    "deportes de equipo",
    "equipo",
    "pelota",
    "futbol",
    "voley",
    "basquet",
    "hockey",
  ],
  "deportes-con-raqueta": ["raqueta", "tenis", "padel", "pádel"],
  "actividades-al-aire-libre": [
    "aire libre",
    "outdoor",
    "plaza",
    "running",
    "correr",
    "ciclismo",
    "bici",
  ],
};

const aliasesAmpliosDeCategoria = [
  "actividades acuaticas",
  "actividades acuáticas",
  "aire libre",
  "artes marciales",
  "bienestar",
  "deportes con raqueta",
  "deportes de combate",
  "deportes de contacto",
  "deportes de equipo",
  "fitness",
  "outdoor",
];

const palabrasIntencionBusqueda = [
  "clases",
  "clase",
  "aprender",
  "practicar",
  "empezar",
  "principiante",
  "principiantes",
  "inicial",
  "desde cero",
  "adultos",
  "niños",
  "ninos",
  "infantil",
  "infantiles",
  "juvenil",
  "mujeres",
  "cerca",
  "cerca mio",
  "cerca mío",
  "cerca de mi",
  "cerca de mí",
  "en mi ciudad",
  "en mi barrio",
  "mar del plata",
  "mdp",
  "centro",
  "horarios",
  "hoy",
  "lunes",
  "martes",
  "noche",
  "mañana",
  "tarde",
  "profesor",
  "profe",
  "instructor",
  "academia",
  "escuela",
  "club",
  "team",
  "dojo",
];

const conectoresBusqueda = [
  "de",
  "del",
  "para",
  "por",
  "con",
  "sin",
  "una",
  "un",
  "el",
  "la",
  "los",
  "las",
  "y",
];

export function normalizarTexto(valor: string): string {
  return valor
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/[-_]+/g, " ")
    .replace(/[^a-zA-Z0-9\s]/g, " ")
    .toLowerCase()
    .replace(/\s+/g, " ")
    .trim();
}

export function normalizarCompacto(valor: string): string {
  return normalizarTexto(valor).replace(/\s+/g, "");
}

function obtenerValoresUnicos(valores: string[]): string[] {
  return Array.from(new Set(valores.filter((valor) => valor.length > 0)));
}

function escaparRegExp(valor: string): string {
  return valor.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

function quitarPalabrasIntencion(textoNormalizado: string): string {
  const frases = [...palabrasIntencionBusqueda, ...conectoresBusqueda]
    .map(normalizarTexto)
    .filter((frase) => frase.length > 0)
    .sort((a, b) => b.length - a.length);
  let textoDepurado = ` ${textoNormalizado} `;

  for (const frase of frases) {
    const patron = new RegExp(`\\b${escaparRegExp(frase)}\\b`, "g");
    textoDepurado = textoDepurado.replace(patron, " ");
  }

  return textoDepurado.replace(/\s+/g, " ").trim();
}

function normalizarBusquedaParaMatch(query: string): BusquedaParaMatch {
  const busquedaNormalizada = normalizarTexto(query);
  const busquedaDepurada = quitarPalabrasIntencion(busquedaNormalizada);
  const textos = obtenerValoresUnicos([
    busquedaNormalizada,
    busquedaDepurada,
  ]);
  const compactos = obtenerValoresUnicos(textos.map(normalizarCompacto));
  const textoParaTokens = busquedaDepurada || busquedaNormalizada;
  const tokens = obtenerValoresUnicos(
    textoParaTokens.split(" ").filter((token) => token.length > 1)
  );

  return {
    textos,
    compactos,
    tokens,
  };
}

function obtenerVariantesBusquedaPuntaje(
  busqueda: string
): VariantesBusquedaPuntaje {
  const busquedaNormalizada = normalizarTexto(busqueda);
  const busquedaDepurada = quitarPalabrasIntencion(busquedaNormalizada);
  const textos = obtenerValoresUnicos([
    busquedaNormalizada,
    busquedaDepurada,
  ]);

  return {
    textos,
    compactos: obtenerValoresUnicos(textos.map(normalizarCompacto)),
  };
}

function obtenerClaveAliasCategoria(valor: string): string {
  return normalizarTexto(valor).replace(/\s+/g, "-");
}

export function obtenerAliasesDeporte(slug: string): string[] {
  return obtenerValoresUnicos(aliasesPorDeporteSlug[slug] || []);
}

export function obtenerAliasesCategoria(
  valores: Array<string | null | undefined>
): string[] {
  const aliases: string[] = [];

  for (const valor of valores) {
    if (!valor) {
      continue;
    }

    const clavesPosibles = obtenerValoresUnicos([
      valor,
      normalizarTexto(valor),
      obtenerClaveAliasCategoria(valor),
      normalizarCompacto(valor),
    ]);

    for (const clave of clavesPosibles) {
      aliases.push(...(aliasesPorCategoria[clave] || []));
    }
  }

  return obtenerValoresUnicos(aliases);
}

export function obtenerValorCategoria(deporte: Deporte): string {
  if (deporte.categoriaSlug) {
    return deporte.categoriaSlug;
  }

  return deporte.categoriaNombre ? normalizarTexto(deporte.categoriaNombre) : "";
}

export function obtenerCategoriasBusquedaDeportes(
  deportes: Deporte[]
): CategoriaBusquedaDeporte[] {
  const categorias = new Map<string, CategoriaBusquedaDeporte>();

  for (const deporte of deportes) {
    if (!deporte.categoriaNombre) {
      continue;
    }

    const valor = obtenerValorCategoria(deporte);

    if (!valor || categorias.has(valor)) {
      continue;
    }

    categorias.set(valor, {
      valor,
      nombre: deporte.categoriaNombre,
    });
  }

  return Array.from(categorias.values());
}

function textoCoincideConBusqueda(
  textosComparables: string[],
  busqueda: BusquedaParaMatch
): boolean {
  const textoAgrupado = textosComparables.join(" ");
  const textos = [...textosComparables, textoAgrupado];

  return textos.some((texto) => {
    const textoNormalizado = normalizarTexto(texto);
    const textoCompacto = normalizarCompacto(texto);

    if (!textoNormalizado) {
      return false;
    }

    const coincideTexto = busqueda.textos.some(
      (query) =>
        textoNormalizado.includes(query) || query.includes(textoNormalizado)
    );
    const coincideCompacto = busqueda.compactos.some(
      (query) => textoCompacto.includes(query) || query.includes(textoCompacto)
    );
    const coincideTokens =
      busqueda.tokens.length > 0 &&
      busqueda.tokens.every(
        (token) => textoNormalizado.includes(token) || textoCompacto.includes(token)
      );

    return coincideTexto || coincideCompacto || coincideTokens;
  });
}

function obtenerTextosComparablesDeporte(deporte: Deporte): string[] {
  const aliasesDeporte = obtenerAliasesDeporte(deporte.slug);
  const aliasesCategoria = obtenerAliasesCategoria([
    deporte.categoriaSlug,
    deporte.categoriaNombre,
  ]);

  return [
    deporte.nombre,
    deporte.slug,
    deporte.descripcion || "",
    deporte.categoriaNombre || "",
    deporte.categoriaSlug || "",
    ...aliasesDeporte,
    ...aliasesCategoria,
  ];
}

function obtenerTextosComparablesCategoria(
  categoria: CategoriaBusquedaDeporte
): string[] {
  return [
    categoria.nombre,
    categoria.valor,
    ...obtenerAliasesCategoria([categoria.valor, categoria.nombre]),
  ];
}

function textoCoincideExacto(
  textosComparables: string[],
  busqueda: VariantesBusquedaPuntaje
): boolean {
  return textosComparables.some((texto) => {
    const textoNormalizado = normalizarTexto(texto);
    const textoCompacto = normalizarCompacto(texto);

    return (
      busqueda.textos.includes(textoNormalizado) ||
      busqueda.compactos.includes(textoCompacto)
    );
  });
}

function textoEmpiezaConBusqueda(
  textosComparables: string[],
  busqueda: VariantesBusquedaPuntaje
): boolean {
  return textosComparables.some((texto) => {
    const textoNormalizado = normalizarTexto(texto);
    const textoCompacto = normalizarCompacto(texto);

    return (
      busqueda.textos.some(
        (query) => query.length > 0 && textoNormalizado.startsWith(query)
      ) ||
      busqueda.compactos.some(
        (query) => query.length > 0 && textoCompacto.startsWith(query)
      )
    );
  });
}

function textoContieneBusqueda(
  textosComparables: string[],
  busqueda: VariantesBusquedaPuntaje
): boolean {
  return textosComparables.some((texto) => {
    const textoNormalizado = normalizarTexto(texto);
    const textoCompacto = normalizarCompacto(texto);

    return (
      busqueda.textos.some(
        (query) => query.length > 0 && textoNormalizado.includes(query)
      ) ||
      busqueda.compactos.some(
        (query) => query.length > 0 && textoCompacto.includes(query)
      )
    );
  });
}

function textoCoincideConAliasCategoriaAmplia(
  textosComparables: string[],
  busqueda: VariantesBusquedaPuntaje
) {
  const aliasesAmpliosNormalizados = aliasesAmpliosDeCategoria.map(normalizarTexto);

  return textosComparables.some((texto) => {
    const textoNormalizado = normalizarTexto(texto);
    const textoCompacto = normalizarCompacto(texto);

    return (
      aliasesAmpliosNormalizados.includes(textoNormalizado) &&
      (busqueda.textos.includes(textoNormalizado) ||
        busqueda.compactos.includes(textoCompacto))
    );
  });
}

function obtenerPuntajeCategoriaParaDeporte(
  deporte: Deporte,
  busquedaPuntaje: VariantesBusquedaPuntaje
): number {
  const categoria = deporte.categoriaNombre
    ? {
        valor: obtenerValorCategoria(deporte),
        nombre: deporte.categoriaNombre,
      }
    : null;

  if (!categoria) {
    return 0;
  }

  const textosCategoria = obtenerTextosComparablesCategoria(categoria);

  return textoCoincideConBusqueda(
    textosCategoria,
    normalizarBusquedaParaMatch(busquedaPuntaje.textos[0] || "")
  )
    ? PUNTAJE_DEPORTE_RELACIONADO_CATEGORIA
    : 0;
}

export function obtenerPuntajeBusquedaDeporte(
  deporte: Deporte,
  busqueda: string
): number {
  const busquedaPuntaje = obtenerVariantesBusquedaPuntaje(busqueda);

  if (busquedaPuntaje.textos.length === 0) {
    return 0;
  }

  const textosNombre = [deporte.nombre];
  const textosSlug = [deporte.slug];
  const aliasesDeporte = obtenerAliasesDeporte(deporte.slug);
  const aliasesDeporteEspecificos = aliasesDeporte.filter(
    (alias) =>
      !aliasesAmpliosDeCategoria
        .map(normalizarTexto)
        .includes(normalizarTexto(alias))
  );
  const textosNombreAlias = [
    deporte.nombre,
    deporte.slug,
    ...aliasesDeporteEspecificos,
  ];

  if (textoCoincideExacto(textosNombre, busquedaPuntaje)) {
    return PUNTAJE_NOMBRE_DEPORTE_EXACTO;
  }

  if (textoCoincideExacto(textosSlug, busquedaPuntaje)) {
    return PUNTAJE_SLUG_DEPORTE_EXACTO;
  }

  if (textoCoincideExacto(aliasesDeporteEspecificos, busquedaPuntaje)) {
    return PUNTAJE_ALIAS_DEPORTE_EXACTO;
  }

  if (textoEmpiezaConBusqueda(textosNombre, busquedaPuntaje)) {
    return PUNTAJE_NOMBRE_DEPORTE_EMPIEZA;
  }

  if (textoEmpiezaConBusqueda(aliasesDeporteEspecificos, busquedaPuntaje)) {
    return PUNTAJE_ALIAS_DEPORTE_EMPIEZA;
  }

  if (textoContieneBusqueda(textosNombreAlias, busquedaPuntaje)) {
    return PUNTAJE_NOMBRE_ALIAS_CONTIENE;
  }

  const puntajeCategoria = obtenerPuntajeCategoriaParaDeporte(
    deporte,
    busquedaPuntaje
  );

  if (puntajeCategoria > 0) {
    return puntajeCategoria;
  }

  if (
    deporte.descripcion &&
    textoContieneBusqueda([deporte.descripcion], busquedaPuntaje)
  ) {
    return PUNTAJE_DESCRIPCION_CONTIENE;
  }

  return 0;
}

export function obtenerPuntajeBusquedaCategoria(
  categoria: CategoriaBusquedaDeporte,
  busqueda: string
): number {
  const busquedaPuntaje = obtenerVariantesBusquedaPuntaje(busqueda);

  if (busquedaPuntaje.textos.length === 0) {
    return 0;
  }

  const textosCategoria = [categoria.nombre, categoria.valor];
  const aliasesCategoria = obtenerAliasesCategoria([
    categoria.valor,
    categoria.nombre,
  ]);

  if (textoCoincideExacto(textosCategoria, busquedaPuntaje)) {
    return PUNTAJE_CATEGORIA_EXACTA;
  }

  if (textoCoincideConAliasCategoriaAmplia(aliasesCategoria, busquedaPuntaje)) {
    return PUNTAJE_CATEGORIA_EXACTA;
  }

  if (textoCoincideExacto(aliasesCategoria, busquedaPuntaje)) {
    return PUNTAJE_ALIAS_CATEGORIA_EXACTA;
  }

  if (
    textoEmpiezaConBusqueda(textosCategoria, busquedaPuntaje) ||
    textoEmpiezaConBusqueda(aliasesCategoria, busquedaPuntaje)
  ) {
    return PUNTAJE_DEPORTE_RELACIONADO_CATEGORIA;
  }

  if (
    textoContieneBusqueda(textosCategoria, busquedaPuntaje) ||
    textoContieneBusqueda(aliasesCategoria, busquedaPuntaje)
  ) {
    return PUNTAJE_DEPORTE_RELACIONADO_CATEGORIA;
  }

  return 0;
}

export function coincideDeporteConBusqueda(
  deporte: Deporte,
  busqueda: string
): boolean {
  const busquedaNormalizada = normalizarBusquedaParaMatch(busqueda);

  if (busquedaNormalizada.textos.length === 0) {
    return true;
  }

  return textoCoincideConBusqueda(
    obtenerTextosComparablesDeporte(deporte),
    busquedaNormalizada
  );
}

export function coincideCategoriaConBusqueda(
  categoria: CategoriaBusquedaDeporte,
  busqueda: string
): boolean {
  const busquedaNormalizada = normalizarBusquedaParaMatch(busqueda);

  if (busquedaNormalizada.textos.length === 0) {
    return true;
  }

  return textoCoincideConBusqueda(
    obtenerTextosComparablesCategoria(categoria),
    busquedaNormalizada
  );
}

function coincideExactamente(textosComparables: string[], busqueda: string) {
  const busquedaNormalizada = normalizarTexto(busqueda);
  const busquedaCompacta = normalizarCompacto(busqueda);
  const busquedaDepurada = quitarPalabrasIntencion(busquedaNormalizada);
  const busquedaDepuradaCompacta = normalizarCompacto(busquedaDepurada);

  return textosComparables.some((texto) => {
    const textoNormalizado = normalizarTexto(texto);
    const textoCompacto = normalizarCompacto(texto);

    return (
      textoNormalizado === busquedaNormalizada ||
      textoCompacto === busquedaCompacta ||
      (busquedaDepurada.length > 0 && textoNormalizado === busquedaDepurada) ||
      (busquedaDepuradaCompacta.length > 0 &&
        textoCompacto === busquedaDepuradaCompacta)
    );
  });
}

function coincideAliasCategoriaAmplia(
  categoria: CategoriaBusquedaDeporte,
  busqueda: string
) {
  const aliasesCategoria = obtenerAliasesCategoria([
    categoria.valor,
    categoria.nombre,
  ]);
  const aliasAmpliosNormalizados = aliasesAmpliosDeCategoria.map(normalizarTexto);
  const busquedaNormalizada = normalizarTexto(busqueda);

  return aliasesCategoria.some(
    (alias) =>
      normalizarTexto(alias) === busquedaNormalizada &&
      aliasAmpliosNormalizados.includes(normalizarTexto(alias))
  );
}

export function obtenerSugerenciasBusquedaDeportes({
  deportes,
  categorias,
  busqueda,
  limite = 6,
}: {
  deportes: Deporte[];
  categorias?: CategoriaBusquedaDeporte[];
  busqueda: string;
  limite?: number;
}): SugerenciaBusquedaDeporte[] {
  if (normalizarCompacto(busqueda).length < 2) {
    return [];
  }

  const categoriasDisponibles =
    categorias || obtenerCategoriasBusquedaDeportes(deportes);
  const sugerenciasDeporte = deportes
    .map((deporte, indice) => ({
      indice,
      puntaje: obtenerPuntajeBusquedaDeporte(deporte, busqueda),
      sugerencia: {
        id: `deporte-${deporte.slug}`,
        tipo: "deporte" as const,
        label: deporte.nombre,
        valor: deporte.slug,
        textoAyuda: deporte.categoriaNombre || "Ver actividades de este deporte",
      },
    }))
    .filter((item) => item.puntaje > 0);
  const sugerenciasCategoria = categoriasDisponibles
    .map((categoria, indice) => ({
      indice,
      puntaje: obtenerPuntajeBusquedaCategoria(categoria, busqueda),
      sugerencia: {
        id: `categoria-${categoria.valor}`,
        tipo: "categoria" as const,
        label: categoria.nombre,
        valor: categoria.valor,
        textoAyuda: "Explorar deportes de este estilo",
      },
    }))
    .filter((item) => item.puntaje > 0);

  return [...sugerenciasDeporte, ...sugerenciasCategoria]
    .sort((itemA, itemB) => {
      if (itemB.puntaje !== itemA.puntaje) {
        return itemB.puntaje - itemA.puntaje;
      }

      if (itemA.sugerencia.tipo !== itemB.sugerencia.tipo) {
        return itemA.sugerencia.tipo === "categoria" ? -1 : 1;
      }

      return itemA.indice - itemB.indice;
    })
    .slice(0, limite)
    .map((item) => item.sugerencia);
}

export function obtenerDestinoBusquedaDeportes({
  deportes,
  categorias,
  busqueda,
}: {
  deportes: Deporte[];
  categorias?: CategoriaBusquedaDeporte[];
  busqueda: string;
}): DestinoBusquedaDeporte | null {
  const textoLimpio = busqueda.trim();

  if (!textoLimpio) {
    return null;
  }

  const categoriasDisponibles =
    categorias || obtenerCategoriasBusquedaDeportes(deportes);
  const categoriaExacta = categoriasDisponibles.find((categoria) =>
    coincideExactamente(obtenerTextosComparablesCategoria(categoria), textoLimpio)
  );
  const deporteExacto = deportes.find((deporte) =>
    coincideExactamente(obtenerTextosComparablesDeporte(deporte), textoLimpio)
  );

  if (
    categoriaExacta &&
    coincideAliasCategoriaAmplia(categoriaExacta, textoLimpio)
  ) {
    return {
      tipo: "categoria",
      label: categoriaExacta.nombre,
      valor: categoriaExacta.valor,
    };
  }

  if (deporteExacto) {
    return {
      tipo: "deporte",
      label: deporteExacto.nombre,
      valor: deporteExacto.slug,
    };
  }

  if (categoriaExacta) {
    return {
      tipo: "categoria",
      label: categoriaExacta.nombre,
      valor: categoriaExacta.valor,
    };
  }

  return null;
}

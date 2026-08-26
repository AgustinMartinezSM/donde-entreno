import { ImageResponse } from "next/og";

import { obtenerEventoPorSlug } from "../../../services/eventosService";

/*
  Imagen Open Graph por evento (pendiente que quedó de la Fase 9).

  Un evento se comparte por WhatsApp más que ninguna otra cosa del
  sitio: tiene fecha, así que se manda a alguien "mirá esto". Hasta
  ahora todos compartían la MISMA imagen genérica del sitio, y en un
  chat eso se lee como un link cualquiera.

  Los colores van literales y no por token: Satori no resuelve var().
*/

export const runtime = "nodejs";

export const alt = "Evento deportivo en DondeEntreno";

export const size = { width: 1200, height: 630 };

export const contentType = "image/png";

const MESES = [
  "enero", "febrero", "marzo", "abril", "mayo", "junio",
  "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre",
];

/*
  La fecha se lee del PROPIO string ISO y no con `new Date(...)`.

  Esta imagen la renderiza el SERVIDOR, y en Vercel el servidor corre en
  UTC: `getHours()` mostraría 21:30 para un evento de las 18:30 en
  Argentina. El resto de la app puede usar la hora local porque son
  componentes de CLIENTE, y ahí la zona del visitante es la correcta.

  El backend manda `iniciaAt` con offset (2026-09-12T18:30:00-03:00),
  así que la hora escrita en el string ES la que tipeó el publicador.
*/
function formatearFecha(iso: string) {
  const partes = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})/.exec(iso);

  if (!partes) {
    return null;
  }

  const [, , mes, dia, hora, minutos] = partes;
  const nombreMes = MESES[Number(mes) - 1];

  if (!nombreMes) {
    return null;
  }

  return `${Number(dia)} de ${nombreMes} · ${hora}:${minutos}`;
}

export default async function OpenGraphImageEvento({
  params,
}: {
  params: Promise<{ slug: string }>;
}) {
  const { slug } = await params;

  let titulo = "Evento deportivo";
  let fecha: string | null = null;
  let lugar: string | null = null;
  let deporte: string | null = null;
  let cancelado = false;

  try {
    const evento = await obtenerEventoPorSlug(slug);

    titulo = evento.titulo;
    fecha = formatearFecha(evento.iniciaAt);
    lugar = [evento.sedeNombre, evento.barrioNombre, evento.ciudadNombre]
      .filter(Boolean)
      .join(" · ");
    deporte = evento.deporteNombre;
    cancelado = evento.estado === "CANCELADO";
  } catch {
    /*
      Si el evento no se puede leer, la imagen sale igual con la
      identidad del sitio: una OG rota es peor que una genérica.
    */
  }

  return new ImageResponse(
    (
      <div
        style={{
          width: "100%",
          height: "100%",
          display: "flex",
          flexDirection: "column",
          justifyContent: "space-between",
          background:
            "linear-gradient(135deg, #0F3D5E 0%, #0B314D 60%, #082539 100%)",
          fontFamily: "sans-serif",
          padding: 72,
          position: "relative",
        }}
      >
        <div
          style={{
            position: "absolute",
            bottom: -160,
            right: -160,
            width: 460,
            height: 460,
            borderRadius: 9999,
            background: "rgba(46, 184, 114, 0.16)",
          }}
        />

        <div style={{ display: "flex", alignItems: "center", gap: 16 }}>
          <div
            style={{
              display: "flex",
              width: 20,
              height: 20,
              borderRadius: 9999,
              background: "#2EB872",
            }}
          />
          <div
            style={{
              display: "flex",
              fontSize: 26,
              fontWeight: 800,
              letterSpacing: 6,
              color: "#BFDDEA",
            }}
          >
            {cancelado ? "EVENTO CANCELADO" : "EVENTO DEPORTIVO"}
          </div>
        </div>

        <div style={{ display: "flex", flexDirection: "column" }}>
          {deporte ? (
            <div
              style={{
                display: "flex",
                fontSize: 30,
                fontWeight: 700,
                color: "#7FDCA8",
                marginBottom: 14,
              }}
            >
              {deporte}
            </div>
          ) : null}

          <div
            style={{
              display: "flex",
              fontSize: titulo.length > 46 ? 62 : 78,
              fontWeight: 800,
              color: "#FFFFFF",
              lineHeight: 1.1,
            }}
          >
            {titulo}
          </div>

          {fecha ? (
            <div
              style={{
                display: "flex",
                fontSize: 38,
                fontWeight: 700,
                color: "#BFDDEA",
                marginTop: 26,
              }}
            >
              {fecha}
            </div>
          ) : null}

          {/* El lugar solo si existe: un renglón vacío se ve como un bug. */}
          {lugar ? (
            <div
              style={{
                display: "flex",
                fontSize: 30,
                color: "#9CCFE4",
                marginTop: 12,
              }}
            >
              {lugar}
            </div>
          ) : null}
        </div>

        <div
          style={{
            display: "flex",
            fontSize: 28,
            fontWeight: 800,
            color: "#FFFFFF",
          }}
        >
          DondeEntreno
        </div>
      </div>
    ),
    size
  );
}

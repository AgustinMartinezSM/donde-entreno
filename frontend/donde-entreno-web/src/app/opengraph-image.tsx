import { ImageResponse } from "next/og";

/*
  Imagen Open Graph por defecto del sitio (1200x630).

  Next la genera localmente con next/og (sin servicios externos) y la
  registra como og:image / twitter:image para todas las páginas que no
  definan una propia. Mantiene la identidad DondeEntreno: azul profundo,
  verde activo, celeste y el concepto del pin de ubicación.
*/

export const runtime = "nodejs";

export const alt =
  "DondeEntreno - Encontrá deportes, clubes, profes y gimnasios en tu ciudad";

export const size = {
  width: 1200,
  height: 630,
};

export const contentType = "image/png";

export default function OpenGraphImage() {
  return new ImageResponse(
    (
      <div
        style={{
          width: "100%",
          height: "100%",
          display: "flex",
          flexDirection: "column",
          alignItems: "center",
          justifyContent: "center",
          background: "linear-gradient(135deg, #0F3D5E 0%, #0B314D 60%, #082539 100%)",
          fontFamily: "sans-serif",
          position: "relative",
        }}
      >
        {/* Detalle decorativo celeste arriba a la izquierda */}
        <div
          style={{
            position: "absolute",
            top: -120,
            left: -120,
            width: 360,
            height: 360,
            borderRadius: 9999,
            background: "rgba(79, 179, 217, 0.18)",
          }}
        />

        {/* Detalle decorativo verde abajo a la derecha */}
        <div
          style={{
            position: "absolute",
            bottom: -140,
            right: -140,
            width: 420,
            height: 420,
            borderRadius: 9999,
            background: "rgba(46, 184, 114, 0.16)",
          }}
        />

        {/* Pin de ubicación simplificado (esencia del logo) */}
        <div
          style={{
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            width: 132,
            height: 132,
            borderRadius: 9999,
            background: "#2EB872",
            boxShadow: "0 24px 60px rgba(0, 0, 0, 0.35)",
            marginBottom: 40,
          }}
        >
          <div
            style={{
              display: "flex",
              width: 52,
              height: 52,
              borderRadius: 9999,
              background: "#F8FAFC",
            }}
          />
        </div>

        {/* Wordmark: Donde en blanco, Entreno en verde */}
        <div
          style={{
            display: "flex",
            fontSize: 96,
            fontWeight: 800,
            letterSpacing: -2,
          }}
        >
          <span style={{ color: "#F8FAFC" }}>Donde</span>
          <span style={{ color: "#2EB872" }}>Entreno</span>
        </div>

        <div
          style={{
            display: "flex",
            marginTop: 28,
            fontSize: 34,
            fontWeight: 500,
            color: "#BFDDEA",
            textAlign: "center",
          }}
        >
          Encontrá deportes, clubes, profes y gimnasios en tu ciudad
        </div>

        {/* Chips de ejemplo, estilo de la app */}
        <div
          style={{
            display: "flex",
            gap: 16,
            marginTop: 44,
          }}
        >
          {["Fútbol", "Yoga", "Boxeo", "Natación", "Funcional"].map(
            (deporte) => (
              <div
                key={deporte}
                style={{
                  display: "flex",
                  padding: "12px 28px",
                  borderRadius: 9999,
                  background: "rgba(248, 250, 252, 0.12)",
                  border: "1px solid rgba(191, 221, 234, 0.35)",
                  color: "#F8FAFC",
                  fontSize: 26,
                  fontWeight: 700,
                }}
              >
                {deporte}
              </div>
            )
          )}
        </div>
      </div>
    ),
    size
  );
}

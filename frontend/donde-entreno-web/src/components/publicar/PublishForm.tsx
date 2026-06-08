"use client";

import { useRef, useState } from "react";

export function PublishForm() {
  /*
    Por ahora usamos estados simples para simular el envío del formulario.
    Todavía no conectamos este formulario al backend.
  */
  const [formularioEnviado, setFormularioEnviado] = useState(false);
  const [enviandoFormulario, setEnviandoFormulario] = useState(false);
  const mensajeExitoRef = useRef<HTMLDivElement | null>(null);

  function manejarEnvio(evento: React.FormEvent<HTMLFormElement>) {
    /*
      Evitamos que el navegador recargue la página.
    */
    evento.preventDefault();

    /*
      Simulamos un envío corto para que el usuario vea una respuesta más real.
      Más adelante acá vamos a llamar al endpoint del backend.
    */
    setEnviandoFormulario(true);
    setFormularioEnviado(false);

    setTimeout(() => {
      setEnviandoFormulario(false);
      setFormularioEnviado(true);

      /*
        Cuando aparece el mensaje verde, bajamos suavemente hasta esa zona.
        Esto ayuda sobre todo en mobile.
      */
      setTimeout(() => {
        mensajeExitoRef.current?.scrollIntoView({
          behavior: "smooth",
          block: "center",
        });
      }, 100);
    }, 700);
  }

  return (
    <form
      onSubmit={manejarEnvio}
      className="mt-8 rounded-[var(--radius-xl)] border border-[var(--color-border)] bg-[var(--color-surface)] p-4 shadow-[var(--shadow-card)] sm:p-6"
    >
      <p className="mb-5 text-sm text-[var(--color-muted)]">
        Los campos marcados con{" "}
        <span className="font-bold text-[var(--color-secondary)]">*</span> son
        obligatorios.
      </p>

      <div className="grid gap-4 sm:grid-cols-2">
        {/* Nombre de la actividad */}
        <div className="flex flex-col gap-2 sm:col-span-2">
          <label
            htmlFor="nombreActividad"
            className="text-sm font-bold text-[var(--color-primary)]"
          >
            Nombre de la actividad{" "}
            <span className="text-[var(--color-secondary)]">*</span>
          </label>

          <input
            id="nombreActividad"
            name="nombreActividad"
            type="text"
            required
            placeholder="Ej: Boxeo recreativo para principiantes"
            className="min-h-12 rounded-[var(--radius-md)] border border-[var(--color-border)] bg-white px-4 text-sm outline-none focus:border-[var(--color-accent)]"
          />
        </div>

        {/* Deporte */}
        <div className="flex flex-col gap-2">
          <label
            htmlFor="deporte"
            className="text-sm font-bold text-[var(--color-primary)]"
          >
            Deporte <span className="text-[var(--color-secondary)]">*</span>
          </label>

          <input
            id="deporte"
            name="deporte"
            type="text"
            required
            placeholder="Ej: Boxeo, Yoga, Jiu Jitsu"
            className="min-h-12 rounded-[var(--radius-md)] border border-[var(--color-border)] bg-white px-4 text-sm outline-none focus:border-[var(--color-accent)]"
          />
        </div>

        {/* Modalidad */}
        <div className="flex flex-col gap-2">
          <label
            htmlFor="modalidad"
            className="text-sm font-bold text-[var(--color-primary)]"
          >
            Modalidad <span className="text-[var(--color-secondary)]">*</span>
          </label>

          <select
            id="modalidad"
            name="modalidad"
            required
            className="min-h-12 rounded-[var(--radius-md)] border border-[var(--color-border)] bg-white px-4 text-sm outline-none focus:border-[var(--color-accent)]"
          >
            <option value="">Seleccionar modalidad</option>
            <option value="PRESENCIAL">Presencial</option>
            <option value="ONLINE">Online</option>
            <option value="MIXTA">Mixta</option>
          </select>
        </div>

        {/* Ciudad */}
        <div className="flex flex-col gap-2">
          <label
            htmlFor="ciudad"
            className="text-sm font-bold text-[var(--color-primary)]"
          >
            Ciudad <span className="text-[var(--color-secondary)]">*</span>
          </label>

          <input
            id="ciudad"
            name="ciudad"
            type="text"
            required
            placeholder="Ej: Mar del Plata"
            className="min-h-12 rounded-[var(--radius-md)] border border-[var(--color-border)] bg-white px-4 text-sm outline-none focus:border-[var(--color-accent)]"
          />
        </div>

        {/* Barrio opcional */}
        <div className="flex flex-col gap-2">
          <label
            htmlFor="barrio"
            className="text-sm font-bold text-[var(--color-primary)]"
          >
            Barrio
          </label>

          <input
            id="barrio"
            name="barrio"
            type="text"
            placeholder="Ej: Centro, Güemes, Constitución"
            className="min-h-12 rounded-[var(--radius-md)] border border-[var(--color-border)] bg-white px-4 text-sm outline-none focus:border-[var(--color-accent)]"
          />
        </div>

        {/* Publicador */}
        <div className="flex flex-col gap-2 sm:col-span-2">
          <label
            htmlFor="publicador"
            className="text-sm font-bold text-[var(--color-primary)]"
          >
            Nombre del club, profesor o gimnasio{" "}
            <span className="text-[var(--color-secondary)]">*</span>
          </label>

          <input
            id="publicador"
            name="publicador"
            type="text"
            required
            placeholder="Ej: Escuela de Boxeo Norte"
            className="min-h-12 rounded-[var(--radius-md)] border border-[var(--color-border)] bg-white px-4 text-sm outline-none focus:border-[var(--color-accent)]"
          />
        </div>

        {/* Nivel opcional */}
        <div className="flex flex-col gap-2">
          <label
            htmlFor="nivel"
            className="text-sm font-bold text-[var(--color-primary)]"
          >
            Nivel
          </label>

          <select
            id="nivel"
            name="nivel"
            className="min-h-12 rounded-[var(--radius-md)] border border-[var(--color-border)] bg-white px-4 text-sm outline-none focus:border-[var(--color-accent)]"
          >
            <option value="">Seleccionar nivel</option>
            <option value="PRINCIPIANTE">Principiante</option>
            <option value="INTERMEDIO">Intermedio</option>
            <option value="AVANZADO">Avanzado</option>
            <option value="TODOS">Todos los niveles</option>
          </select>
        </div>

        {/* WhatsApp */}
        <div className="flex flex-col gap-2">
          <label
            htmlFor="whatsapp"
            className="text-sm font-bold text-[var(--color-primary)]"
          >
            WhatsApp de contacto{" "}
            <span className="text-[var(--color-secondary)]">*</span>
          </label>

          <input
            id="whatsapp"
            name="whatsapp"
            type="text"
            inputMode="numeric"
            required
            placeholder="Ej: 5492231234567"
            className="min-h-12 rounded-[var(--radius-md)] border border-[var(--color-border)] bg-white px-4 text-sm outline-none focus:border-[var(--color-accent)]"
          />
        </div>

        {/* Descripción */}
        <div className="flex flex-col gap-2 sm:col-span-2">
          <label
            htmlFor="descripcion"
            className="text-sm font-bold text-[var(--color-primary)]"
          >
            Descripción{" "}
            <span className="text-[var(--color-secondary)]">*</span>
          </label>

          <textarea
            id="descripcion"
            name="descripcion"
            rows={5}
            required
            placeholder="Contanos brevemente de qué se trata la actividad, para quién es y qué necesita saber una persona antes de consultar."
            className="rounded-[var(--radius-md)] border border-[var(--color-border)] bg-white px-4 py-3 text-sm outline-none focus:border-[var(--color-accent)]"
          />
        </div>
      </div>

      {/* Aviso temporal */}
      <div className="mt-5 rounded-[var(--radius-md)] bg-[#E8F6FB] p-4 text-sm leading-6 text-[#0F6F8F]">
        Este formulario todavía no guarda datos en el backend. Por ahora lo
        usamos para preparar la interfaz de publicación del MVP.
      </div>

      {/* Mensaje simulado de envío */}
      {formularioEnviado && (
        <div
          ref={mensajeExitoRef}
          className="mt-5 rounded-[var(--radius-md)] bg-[#E6F7EF] p-4 text-sm font-bold leading-6 text-[#167A4A]"
        >
          Solicitud preparada correctamente. Más adelante vamos a conectarla con
          el backend.
        </div>
      )}

      {/* Botón de envío */}
      <div className="mt-6 flex flex-col gap-3 sm:flex-row sm:justify-end">
        <button
          type="submit"
          disabled={enviandoFormulario}
          className="min-h-12 rounded-[var(--radius-md)] bg-[var(--color-primary)] px-6 text-sm font-bold text-white shadow-[var(--shadow-button)] transition hover:-translate-y-0.5 disabled:cursor-not-allowed disabled:opacity-70"
        >
          {enviandoFormulario ? "Enviando..." : "Enviar solicitud"}
        </button>
      </div>
    </form>
  );
}
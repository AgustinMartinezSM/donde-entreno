/*
  Guías deportivas (Fase 10).

  El contenido vive acá, en código, y no en una tabla: son textos
  editoriales que cambian poco y que se revisan antes de publicar. Una
  tabla traería un editor, moderación y estados para algo que hoy
  escribe una sola persona.

  REGLA DE CONTENIDO, y es la que ordena todo lo demás: acá NO se
  afirma nada que la plataforma no pueda sostener. Nada de precios, de
  duraciones de clase, de beneficios de salud ni de cuánto se tarda en
  progresar. Lo que se cuenta es qué es el deporte, cómo es empezar y
  qué preguntar antes de anotarse — y los datos concretos (qué hay,
  dónde, a qué precio) los pone el catálogo real, no el texto.
*/

export type SeccionGuia = {
  titulo: string;
  parrafos: string[];
};

export type Guia = {
  slug: string;
  /** El deporte del catálogo con el que se enlaza. */
  deporteSlug: string;
  titulo: string;
  /** Bajada corta: se usa en la tarjeta del índice y en la metadata. */
  resumen: string;
  /** Lo que alguien quiere saber antes de leer: se muestra arriba. */
  deUnVistazo: string[];
  secciones: SeccionGuia[];
  /** Preguntas para hacerle al club o profe antes de anotarse. */
  quePreguntar: string[];
  /** Fecha de la última revisión editorial, en ISO. */
  actualizada: string;
};

const GUIA_KARATE: Guia = {
  slug: "karate",
  deporteSlug: "karate",
  titulo: "Empezar karate",
  resumen:
    "Qué es el karate, cómo es la primera clase, qué necesitás para arrancar y qué conviene preguntar antes de anotarte.",
  deUnVistazo: [
    "Se practica descalzo, en un piso de tatami o parqué.",
    "Se entrena en grupo, pero cada quien avanza a su ritmo.",
    "Para probar alcanza con ropa cómoda: el karategi viene después.",
    "Hay clases para chicos, adultos y adultos mayores.",
  ],
  secciones: [
    {
      titulo: "Qué es",
      parrafos: [
        "El karate es un arte marcial de origen japonés —desarrollado en Okinawa— que se practica sin armas y se basa en técnicas de puño, pierna y desplazamiento. Se entrena en un dojo, descalzo, y se ordena en tres tipos de trabajo: kihon (la técnica básica, repetida), kata (formas, una secuencia fija que se practica sola) y kumite (el trabajo con un compañero).",
        "Esa división importa para decidir, porque no todas las escuelas reparten igual el tiempo entre las tres. Hay dojos más orientados a la competencia y otros al trabajo tradicional, y la diferencia se nota desde la primera clase.",
      ],
    },
    {
      titulo: "Cómo es empezar",
      parrafos: [
        "Una clase para principiantes arranca con entrada en calor y sigue con kihon: pocas técnicas, muchas repeticiones. Es normal pasar las primeras semanas puliendo dos o tres movimientos, y es normal también que al principio cueste coordinar postura, brazo y pierna al mismo tiempo.",
        "No hace falta llegar con estado físico ni con experiencia previa. Las clases de principiantes están pensadas para gente que empieza de cero, y en la mayoría de los grupos hay personas que arrancaron hace poco.",
        "El contacto varía mucho según la escuela y el grupo. En principiantes suele trabajarse sin golpear al compañero, o con contacto controlado; si el contacto es algo que te preocupa, es de las primeras cosas que conviene preguntar.",
      ],
    },
    {
      titulo: "Qué necesitás",
      parrafos: [
        "Para las primeras clases alcanza con ropa deportiva cómoda que te deje mover las piernas, y agua. Se entrena descalzo, así que no hace falta calzado.",
        "El karategi —el uniforme blanco— y el cinturón se suman cuando ya decidiste seguir; muchas escuelas te dicen cuándo y cuál conseguir, porque hay diferencias de tela y de corte. Si más adelante entrás a trabajar kumite con contacto, pueden pedirte protecciones: protector bucal, guantillas, canilleras.",
        "Nada de eso se compra antes de la primera clase. Preguntá qué te va a hacer falta y en qué momento.",
      ],
    },
    {
      titulo: "Los cinturones",
      parrafos: [
        "El progreso se ordena en grados. Antes del cinturón negro están los kyu, que se cuentan de mayor a menor; del cinturón negro en adelante están los dan, que se cuentan al revés. Cada grado se rinde en un examen, y los colores de los kyu no son universales: cambian según el estilo y la federación.",
        "Vale saberlo para no comparar de más: dos personas con el mismo color de cinturón en escuelas distintas no necesariamente pasaron por lo mismo. El cinturón ordena el aprendizaje adentro de una escuela; no es una medida que se traslade sola de un lado a otro.",
      ],
    },
    {
      titulo: "Cómo elegir dónde",
      parrafos: [
        "Lo primero es práctico: que te quede cerca y en un horario al que puedas ir de verdad. La constancia en un arte marcial pesa más que cualquier otra cosa, y un dojo excelente al que llegás tarde y cansado se abandona rápido.",
        "Después, mirá el grupo. Fijate si hay clase específica de principiantes o si entrás directamente a un grupo general, y si hay gente de tu edad y de tu nivel. Si podés, pedí ir a mirar una clase antes de anotarte: en veinte minutos se ve el trato del profe con el grupo, que es lo que más va a definir tu experiencia.",
        "Y preguntá por el estilo y la orientación de la escuela. No hay uno mejor que otro, pero sí uno que se parece más a lo que estás buscando.",
      ],
    },
  ],
  quePreguntar: [
    "¿Hay clase específica para principiantes o se entra al grupo general?",
    "¿Puedo ir a mirar una clase antes de anotarme?",
    "¿Cómo se trabaja el contacto en el grupo de principiantes?",
    "¿Qué estilo se practica y qué lugar ocupan kata y kumite?",
    "¿Qué necesito llevar a las primeras clases?",
    "¿Con qué frecuencia conviene entrenar al empezar?",
  ],
  actualizada: "2026-08-25",
};

export const GUIAS: Guia[] = [GUIA_KARATE];

export function obtenerGuia(slug: string): Guia | undefined {
  return GUIAS.find((guia) => guia.slug === slug);
}

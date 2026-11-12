// Unidades de medida de los lotes
export const MEASURE_UNITS = [
  { value: 'UNITS', label: 'Unidades (Fichas)' },
  { value: 'KILOS', label: 'Kilogramos (Materia prima)' },
  { value: 'BOXES', label: 'Cajas de empaquetado' },
];

const SHORT_LABELS = { UNITS: 'unidades', KILOS: 'kg', BOXES: 'cajas' };

// Unidad tal como se muestra junto a una cantidad; una desconocida se enseña tal cual
export const unitLabel = (unit) => SHORT_LABELS[unit] ?? unit ?? '';

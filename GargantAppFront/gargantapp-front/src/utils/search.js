// Texto en minúsculas y sin tildes, para que "Gonzalez" encuentre "González".
export function normalizeText(value) {
  return String(value ?? '')
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .toLowerCase()
    .trim();
}

// Cierto si alguno de los campos contiene todas las palabras buscadas.
export function matchesSearch(query, fields) {
  const words = normalizeText(query).split(/\s+/).filter(Boolean);
  if (words.length === 0) return true;
  const haystack = fields.map(normalizeText).join(' ');
  return words.every((word) => haystack.includes(word));
}

// Opciones de un desplegable ({ value, label }) cuyo texto contiene lo buscado.
export function filterOptions(query, options) {
  return options.filter((option) => matchesSearch(query, [option.label]));
}

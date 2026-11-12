import { describe, expect, it } from 'vitest';
import { filterOptions, matchesSearch, normalizeText } from '../utils/search';

describe('búsqueda de texto', () => {
  it('ignora mayúsculas y tildes', () => {
    expect(normalizeText('  Sociedad GASTRONÓMICA ')).toBe('sociedad gastronomica');
    expect(matchesSearch('gastronomica', ['Sociedad Gastronómica Lagunak'])).toBe(true);
  });

  it('exige todas las palabras, aunque estén en campos distintos', () => {
    const fields = ['PED-DEMO-004', 'Restaurante Arrantzale', null];
    expect(matchesSearch('demo arrantzale', fields)).toBe(true);
    expect(matchesSearch('demo lagunak', fields)).toBe(false);
  });

  it('sin texto lo muestra todo', () => {
    expect(matchesSearch('   ', ['x'])).toBe(true);
  });
});

describe('desplegable con búsqueda', () => {
  const options = [
    { value: 1, label: 'Huevo campero (GAR-HUE-001)' },
    { value: 2, label: 'Patata agria (GAR-PAT-002)' },
    { value: 3, label: 'Cebolla dulce (GAR-CEB-003)' },
  ];

  it('filtra por cualquier parte del texto, sin tildes ni mayúsculas', () => {
    expect(filterOptions('pat', options).map((o) => o.value)).toEqual([2]);
    expect(filterOptions('gar-ceb', options).map((o) => o.value)).toEqual([3]);
  });

  it('sin texto deja todas las opciones', () => {
    expect(filterOptions('', options)).toHaveLength(3);
  });
});

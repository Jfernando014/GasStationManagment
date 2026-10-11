/** Connectors that stay in lower case inside a name, except as the first word ("María del Pilar"). */
const NAME_CONNECTORS = new Set(['de', 'del', 'la', 'las', 'los', 'y', 'e']);

/**
 * Normalizes a person's name: single spaces, first letter of each word in upper case and the rest in
 * lower case. Parts joined by a hyphen are capitalized too ("Ana-maría" -> "Ana-María").
 * Example: "  cARLOS   andrés DE LA torre " -> "Carlos Andrés de la Torre".
 */
export function toNameCase(text: string): string {
  return text
    .trim()
    .toLocaleLowerCase('es')
    .split(/\s+/)
    .filter(word => word.length > 0)
    .map((word, index) =>
      index > 0 && NAME_CONNECTORS.has(word)
        ? word
        : word
            .split('-')
            .map(part => part.charAt(0).toLocaleUpperCase('es') + part.slice(1))
            .join('-')
    )
    .join(' ');
}

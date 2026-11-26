import { SalesParameterKey } from '../models/dtos/sales-parameter.dto';
import { formatGallonMoney, parseGallonValue } from './fuel-price.helpers';
import { formatPercentage, formatPercentageInput, parsePercentage } from './percentage.helpers';

/** How a parameter is named, written and read. Values are the ones the backend stores. */
export interface SalesParameterMeta {
  title: string;
  /** The parameter inside a sentence. */
  name: string;
  fieldLabel: string;
  placeholder: string;
  hint: string;
  format(value: number): string;
  /** A stored value as it is typed in the field. */
  toInput(value: number): string;
  parse(text: string): { value: number; error: null } | { value: null; error: string };
}

const perGallon = {
  fieldLabel: 'Valor por galón ($)',
  placeholder: 'Ej.: 250',
  hint: 'En pesos, hasta 2 decimales. Puede ser 0.',
  format: (value: number) => formatGallonMoney(value),
  toInput: (value: number) => formatGallonMoney(value).replace('$', ''),
  parse: (text: string) => parseGallonValue(text, true)
};

export const SALES_PARAMETERS: Record<SalesParameterKey, SalesParameterMeta> = {
  'freight-per-gallon': { ...perGallon, title: 'Flete por galón', name: 'flete por galón' },
  'financial-cost-per-gallon': {
    ...perGallon, title: 'Costo financiero por galón', name: 'costo financiero por galón', placeholder: 'Ej.: 100'
  },
  'credibanco-commission-rate': {
    title: 'Comisión Credibanco',
    name: 'comisión de Credibanco',
    fieldLabel: 'Porcentaje de comisión (%)',
    placeholder: 'Ej.: 2,65',
    hint: 'Sobre las ventas con tarjeta, hasta 4 decimales.',
    format: value => formatPercentage(value * 100, 4),
    toInput: value => formatPercentageInput(value * 100),
    parse: text => {
      const result = parsePercentage(text, false);
      // The backend keeps the fraction with 6 decimals: the rounding removes the noise of the division.
      return result.value === null ? result : { value: Number((result.value / 100).toFixed(6)), error: null };
    }
  }
};

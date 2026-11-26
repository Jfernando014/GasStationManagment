/** Parameters of Ventas the frontend reads or changes, named as in their address. */
export type SalesParameterKey = 'freight-per-gallon' | 'financial-cost-per-gallon' | 'credibanco-commission-rate';

/**
 * A value that rules from `validFrom` ("YYYY-MM-DD"). Money values are pesos per gallon; the commission is a
 * fraction (2,65 % = 0.0265).
 */
export interface SalesParameterDto {
  key: string;
  value: number;
  validFrom: string;
}

export type SalesParameterRequestDto = Omit<SalesParameterDto, 'key'>;

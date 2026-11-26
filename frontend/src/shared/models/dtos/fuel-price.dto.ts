import { FuelType } from './tank.dto';

/** Which of the two values per gallon: the sale price or the purchase cost. */
export type FuelValueKind = 'sale' | 'purchase';

/**
 * A value per gallon that rules from `validFrom` ("YYYY-MM-DD") until a later one of the same fuel starts.
 * The sale price and the purchase cost share this shape.
 */
export interface FuelValueDto {
  id: number;
  fuelType: FuelType;
  pricePerGallon: number;
  validFrom: string;
}

export type FuelValueRequestDto = Omit<FuelValueDto, 'id'>;

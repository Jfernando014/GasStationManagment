import { FuelType } from './tank.dto';
import { FuelAutonomyResponse } from './fuel-autonomy.dto';
import { FuelVariationResponse } from './fuel-variation.dto';
import { ProductCategory } from './product.dto';
import { StockStatus } from './product-stock.dto';

/** Variation and autonomy of a fuel on a journal date. */
export interface FuelControlResponse {
  fuelType: FuelType;
  variation: FuelVariationResponse;
  autonomy: FuelAutonomyResponse;
  /** Journal date plus the whole days of autonomy. Null unless the autonomy was calculated. */
  estimatedDepletionDate: string | null;
}

/** Stock of a product at the end of a journal date. */
export interface ProductControlResponse {
  productId: number;
  productCode: string;
  productName: string;
  category: ProductCategory;
  sold: number;
  officeStock: number;
  counterStock: number;
  status: StockStatus;
  depleted: boolean;
}

export interface DailyControlResponse {
  journalDate: string;
  fuels: FuelControlResponse[];
  products: ProductControlResponse[];
}

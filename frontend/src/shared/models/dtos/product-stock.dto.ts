import { ProductCategory } from './product.dto';

export type StockLocation = 'OFFICE' | 'COUNTER';
export type StockStatus = 'OK' | 'NEGATIVE_STOCK';

/** Units of a product in one location on a journal date. */
export interface LocationStockResponse {
  opening: number;
  initial: number;
  entries: number;
  /** Net units: negative in the office, positive in the counter. */
  transfers: number;
  adjustments: number;
  sold: number;
  closing: number;
}

export interface ProductStockResponse {
  productId: number;
  productCode: string;
  productName: string;
  category: ProductCategory;
  active: boolean;
  office: LocationStockResponse;
  counter: LocationStockResponse;
  /** NEGATIVE_STOCK when the closing stock of a location is below zero. */
  status: StockStatus;
}

export interface InitialStockRequest {
  productId: number;
  officeQuantity: number;
  counterQuantity: number;
  journalDate: string;
}

export interface StockEntryRequest {
  productId: number;
  quantity: number;
  journalDate: string;
  note?: string;
}

export interface StockTransferRequest {
  productId: number;
  quantity: number;
  journalDate: string;
  note?: string;
}

export interface StockAdjustmentRequest {
  productId: number;
  location: StockLocation;
  /** Signed units: positive adds, negative removes. Cannot be zero. */
  quantity: number;
  journalDate: string;
  reason: string;
}

export interface StockMovementResponse {
  id: number;
  productId: number;
  journalDate: string;
  location: StockLocation;
  movementType: string;
  quantity: number;
  note: string | null;
  createdAt: string;
}

export interface SalesSyncResponse {
  journalDate: string;
  syncedProducts: number;
  /** Sold codes that do not match any product of the catalog. */
  unmatchedCodes: string[];
}

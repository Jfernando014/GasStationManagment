import { ProductStockResponse } from '../../../shared/models/dtos/product-stock.dto';

/** Tabs of step 5: tank measurements and lubricants/aditives stock. */
export type StepTab = 'tanques' | 'lubricantes';

/** Filters of the lubricants tab toolbar. */
export type ProductFilter = 'movimiento' | 'todos' | 'LUBRICANTE' | 'ADITIVO' | 'OTRO';

/** Stock movement being registered for a product. */
export type DialogMode = 'initial' | 'entry';

export interface DialogContext {
  mode: DialogMode;
  product: ProductStockResponse;
}

export interface TankHeightChange {
  tankId: number;
  height: number | null;
}

export interface TransferChange {
  productId: number;
  quantity: number | null;
}

export interface MovementValues {
  officeQuantity: number | null;
  counterQuantity: number | null;
  quantity: number | null;
}

export interface StockMovementRequest {
  context: DialogContext;
  values: MovementValues;
}

export interface MovementTarget {
  product: ProductStockResponse;
  mode: DialogMode;
}

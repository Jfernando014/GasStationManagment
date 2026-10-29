import { ProductStockResponse } from '../models/dtos/product-stock.dto';
import { TankResponse } from '../models/dtos/tank.dto';

/** A reading is valid when it is positive and does not exceed the maximum height of the tank. */
export function isValidTankHeight(tank: TankResponse, height: number | null | undefined): boolean {
  return height !== null && height !== undefined && height > 0 && height <= tank.maxHeightCm;
}

/** A counter transfer must be a whole number that does not exceed the office stock. */
export function isTransferInvalid(product: ProductStockResponse, quantity: number | null): boolean {
  return quantity !== null && (!Number.isInteger(quantity) || quantity < 0 || quantity > product.office.closing);
}

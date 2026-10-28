export type ProductCategory = 'LUBRICANTE' | 'ADITIVO' | 'REFRIGERANTE' | 'OTRO';

export const PRODUCT_CATEGORIES: ProductCategory[] = ['LUBRICANTE', 'ADITIVO', 'REFRIGERANTE', 'OTRO'];

export interface ProductResponse {
  id: number;
  code: string;
  name: string;
  category: ProductCategory;
  price: number;
  cost: number | null;
  active: boolean;
  /** Computed by the backend from price and cost; it is never sent back. */
  marginPercent: number | null;
}

export interface ProductRequest {
  code: string;
  name: string;
  category: ProductCategory;
  price: number;
  cost: number | null;
  active: boolean;
}

export interface ProductFilters {
  category?: ProductCategory | null;
  active?: boolean | null;
}

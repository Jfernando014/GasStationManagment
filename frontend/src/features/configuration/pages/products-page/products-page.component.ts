import { CommonModule } from '@angular/common';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { ProductStockService } from '../../../../core/services/product-stock.service';
import { ProductsService } from '../../../../core/services/products.service';
import {
  PRODUCT_CATEGORY_FILTER_LABELS,
  PRODUCT_CATEGORY_LABELS,
  formatCop,
  formatPercent
} from '../../../../shared/helpers/inventory-display.helpers';
import { getErrorMessage, getFieldErrors, isApiError } from '../../../../shared/helpers/problem-details.helpers';
import {
  PRODUCT_CATEGORIES,
  ProductCategory,
  ProductRequest,
  ProductResponse
} from '../../../../shared/models/dtos/product.dto';
import { ProductStockResponse, LocationStockResponse } from '../../../../shared/models/dtos/product-stock.dto';

const TOAST_DURATION_MS = 3000;

type ProductTab = 'catalogo' | 'margenes';
type ActiveFilter = 'all' | 'active' | 'inactive';

interface ProductDraft {
  code: string;
  name: string;
  category: ProductCategory | '';
  price: number | null;
  cost: number | null;
  active: boolean;
}

const EMPTY_DRAFT: ProductDraft = {
  code: '',
  name: '',
  category: '',
  price: null,
  cost: null,
  active: true
};

/** Configuración › Productos: catalog of lubricants, additives and other products. */
@Component({
  selector: 'app-products-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './products-page.component.html',
  styleUrl: './products-page.component.css'
})
export class ProductsPageComponent {
  private readonly productsService = inject(ProductsService);
  private readonly stockService = inject(ProductStockService);
  private readonly destroyRef = inject(DestroyRef);

  readonly categories = PRODUCT_CATEGORIES;
  readonly categoryLabels = PRODUCT_CATEGORY_LABELS;
  readonly categoryFilterLabels = PRODUCT_CATEGORY_FILTER_LABELS;

  readonly products = signal<ProductResponse[]>([]);
  readonly stock = signal<Record<number, ProductStockResponse>>({});
  readonly stockError = signal(false);
  readonly loading = signal(true);
  readonly loadError = signal<string | null>(null);

  readonly tab = signal<ProductTab>('catalogo');
  readonly search = signal('');
  readonly category = signal<ProductCategory | ''>('');
  readonly activeFilter = signal<ActiveFilter>('all');

  readonly draft = signal<ProductDraft>({ ...EMPTY_DRAFT });
  readonly formOpen = signal(false);
  readonly editingId = signal<number | null>(null);
  readonly submitted = signal(false);
  readonly saving = signal(false);
  readonly fieldErrors = signal<Record<string, string>>({});
  readonly formError = signal<string | null>(null);

  readonly toastText = signal('');
  readonly toastVisible = signal(false);
  private toastTimer?: ReturnType<typeof setTimeout>;

  readonly visibleProducts = computed(() => {
    const query = this.search().trim().toLowerCase();
    const category = this.category();
    const activeFilter = this.activeFilter();
    return this.products().filter(product => {
      if (category && product.category !== category) return false;
      if (activeFilter !== 'all' && product.active !== (activeFilter === 'active')) return false;
      if (!query) return true;
      return product.name.toLowerCase().includes(query) || product.code.toLowerCase().includes(query);
    });
  });

  readonly margins = computed(() =>
    this.products()
      .filter(product => product.marginPercent !== null)
      .sort((a, b) => (b.marginPercent ?? 0) - (a.marginPercent ?? 0))
  );

  readonly averageMargin = computed(() => {
    const margins = this.margins();
    if (margins.length === 0) return null;
    return margins.reduce((total, product) => total + (product.marginPercent ?? 0), 0) / margins.length;
  });

  readonly bestMargin = computed(() => this.margins()[0] ?? null);
  readonly withoutCost = computed(() => this.products().filter(product => product.cost === null).length);

  readonly canSubmit = computed(() => !this.saving());

  constructor() {
    this.destroyRef.onDestroy(() => clearTimeout(this.toastTimer));
    this.loadProducts();
    this.loadStock();
  }

  retry(): void {
    this.loadProducts();
    if (this.stockError()) this.loadStock();
  }

  private loadProducts(): void {
    this.loading.set(true);
    this.loadError.set(null);
    this.productsService
      .list()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: products => {
          this.products.set(products);
          this.loading.set(false);
        },
        error: (error: unknown) => {
          this.loading.set(false);
          this.loadError.set(getErrorMessage(error));
        }
      });
  }

  /** Stock of today, only to show the units of each product; the catalog works without it. */
  private loadStock(): void {
    this.stockError.set(false);
    this.stockService
      .listByJournalDate(this.today())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: rows => {
          const byProduct = rows.reduce<Record<number, ProductStockResponse>>((result, row) => {
            result[row.productId] = row;
            return result;
          }, {});
          this.stock.set(byProduct);
        },
        error: () => this.stockError.set(true)
      });
  }

  private today(): string {
    const now = new Date();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const day = String(now.getDate()).padStart(2, '0');
    return `${now.getFullYear()}-${month}-${day}`;
  }

  /** Category and status are filtered in the browser, so the table reacts without another request. */
  onCategoryChange(value: string): void {
    this.category.set(value as ProductCategory | '');
  }

  onActiveChange(value: string): void {
    this.activeFilter.set(value as ActiveFilter);
  }

  openCreate(): void {
    this.editingId.set(null);
    this.draft.set({ ...EMPTY_DRAFT });
    this.submitted.set(false);
    this.clearErrors();
    this.formOpen.set(true);
  }

  openEdit(product: ProductResponse): void {
    this.editingId.set(product.id);
    this.draft.set({
      code: product.code,
      name: product.name,
      category: product.category,
      price: product.price,
      cost: product.cost,
      active: product.active
    });
    this.submitted.set(false);
    this.clearErrors();
    this.formOpen.set(true);
  }

  closeForm(): void {
    this.formOpen.set(false);
    this.submitted.set(false);
    this.clearErrors();
  }

  updateDraft(field: keyof ProductDraft, value: string | number | boolean | null): void {
    this.draft.update(draft => {
      if (field === 'active') return { ...draft, active: Boolean(value) };
      if (field === 'price' || field === 'cost') {
        const number = value === null || value === '' ? null : Number(value);
        return { ...draft, [field]: number !== null && Number.isFinite(number) ? number : null };
      }
      return { ...draft, [field]: String(value) };
    });
  }

  fieldError(field: string): string {
    return this.fieldErrors()[field] ?? '';
  }

  isInvalid(field: keyof ProductDraft): boolean {
    if (!this.submitted() && !this.fieldError(field)) return false;
    const draft = this.draft();
    switch (field) {
      case 'code':
        return !draft.code.trim() || draft.code.trim().length > 20;
      case 'name':
        return !draft.name.trim() || draft.name.trim().length > 100;
      case 'category':
        return !draft.category;
      case 'price':
        return draft.price === null || draft.price <= 0;
      case 'cost':
        return draft.cost !== null && draft.cost < 0;
      default:
        return false;
    }
  }

  save(): void {
    if (this.saving()) return;
    this.submitted.set(true);
    this.clearErrors();
    if (this.isInvalid('code') || this.isInvalid('name') || this.isInvalid('category') ||
        this.isInvalid('price') || this.isInvalid('cost')) {
      this.formError.set('Revisa los datos marcados para guardar el producto.');
      return;
    }
    const draft = this.draft();
    const request: ProductRequest = {
      code: draft.code.trim(),
      name: draft.name.trim(),
      category: draft.category as ProductCategory,
      price: Number(draft.price),
      cost: draft.cost === null ? null : Number(draft.cost),
      active: draft.active
    };
    const editingId = this.editingId();
    const operation = editingId === null
      ? this.productsService.create(request)
      : this.productsService.update(editingId, request);

    this.saving.set(true);
    operation.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: saved => {
        this.saving.set(false);
        this.products.update(products => editingId === null
          ? [...products, saved].sort((a, b) => a.name.localeCompare(b.name))
          : products.map(product => product.id === saved.id ? saved : product));
        if (editingId === null) {
          this.seedStockRow(saved);
        } else {
          // The active flag decides which rows the stock endpoint returns, so it is read again.
          this.loadStock();
        }
        this.formOpen.set(false);
        this.submitted.set(false);
        this.showToast(editingId === null ? 'Producto registrado correctamente.' : 'Producto actualizado correctamente.');
      },
      error: (error: unknown) => {
        this.saving.set(false);
        this.showServerError(error);
      }
    });
  }

  private showServerError(error: unknown): void {
    const mapped = getFieldErrors(error).reduce<Record<string, string>>((result, fieldError) => {
      result[fieldError.field] = fieldError.message;
      return result;
    }, {});
    if (Object.keys(mapped).length > 0) {
      this.fieldErrors.set(mapped);
      return;
    }
    if (isApiError(error) && error.status === 409) {
      this.fieldErrors.set({ code: 'El código ya se encuentra registrado.' });
      return;
    }
    this.formError.set(getErrorMessage(error));
  }

  private clearErrors(): void {
    this.fieldErrors.set({});
    this.formError.set(null);
  }

  stockOf(product: ProductResponse): ProductStockResponse | undefined {
    return this.stock()[product.id];
  }

  /**
   * A product created in this session has no movements yet, so the stock endpoint will report zeros
   * for it. The row is added here to avoid showing "—" until the page is reloaded.
   */
  private seedStockRow(product: ProductResponse): void {
    if (!product.active || this.stockError() || this.stock()[product.id]) return;
    const empty: LocationStockResponse = {
      opening: 0,
      initial: 0,
      entries: 0,
      transfers: 0,
      adjustments: 0,
      sold: 0,
      closing: 0
    };
    this.stock.update(stock => ({
      ...stock,
      [product.id]: {
        productId: product.id,
        productCode: product.code,
        productName: product.name,
        category: product.category,
        active: product.active,
        office: { ...empty },
        counter: { ...empty },
        status: 'OK'
      }
    }));
  }

  private showToast(text: string): void {
    clearTimeout(this.toastTimer);
    this.toastText.set(text);
    this.toastVisible.set(true);
    this.toastTimer = setTimeout(() => this.toastVisible.set(false), TOAST_DURATION_MS);
  }

  readonly formatCop = formatCop;
  readonly formatPercent = formatPercent;
}

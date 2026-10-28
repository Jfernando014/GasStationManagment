import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { isTransferInvalid } from '../../../../shared/helpers/inventory-rules.helpers';
import { ProductStockResponse } from '../../../../shared/models/dtos/product-stock.dto';
import {
  MovementTarget,
  ProductFilter,
  TransferChange
} from '../../models/step5.model';

/** Step 5b: lubricants and additives stock, counter transfers and stock movements. */
@Component({
  selector: 'app-lubricant-stock-table',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './lubricant-stock-table.component.html',
  styleUrl: './lubricant-stock-table.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class LubricantStockTableComponent {
  readonly products = input.required<ProductStockResponse[]>();
  readonly totalProducts = input.required<number>();
  readonly filter = input.required<ProductFilter>();
  readonly search = input.required<string>();
  readonly pendingTransfers = input.required<Record<number, number | null>>();
  readonly editable = input(false);
  readonly savingTransfers = input(false);
  readonly syncing = input(false);
  readonly hasPendingTransfers = input(false);
  readonly unmatchedCodes = input<string[]>([]);

  readonly filterChanged = output<ProductFilter>();
  readonly searchChanged = output<string>();
  readonly transferChanged = output<TransferChange>();
  readonly syncRequested = output<void>();
  readonly saveRequested = output<void>();
  readonly openMovement = output<MovementTarget>();

  transferOf(productId: number): number | null {
    return this.pendingTransfers()[productId] ?? null;
  }

  transferInvalid(product: ProductStockResponse): boolean {
    return isTransferInvalid(product, this.transferOf(product.productId));
  }

  existingTotal(product: ProductStockResponse): number {
    return product.office.closing + product.counter.closing;
  }

  counterAfterTransfer(product: ProductStockResponse): number {
    return product.counter.closing + (this.transferOf(product.productId) ?? 0);
  }

  onTransferInput(product: ProductStockResponse, event: Event): void {
    const raw = (event.target as HTMLInputElement).value.trim();
    const quantity = raw === '' ? null : Number(raw);
    this.transferChanged.emit({
      productId: product.productId,
      quantity: quantity !== null && Number.isFinite(quantity) ? quantity : null
    });
  }
}

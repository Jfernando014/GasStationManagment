import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  ElementRef,
  computed,
  inject,
  input,
  output,
  signal,
  viewChild
} from '@angular/core';
import { FormsModule } from '@angular/forms';

import { DialogContext, StockMovementRequest } from '../../models/step5.model';

/** Same as --duration-normal: the dialog is closed once its fade-out has finished. */
const DIALOG_CLOSE_DELAY_MS = 320;

/** Step 5b dialog: registers the initial stock of a product or an entry to the office. */
@Component({
  selector: 'app-stock-movement-dialog',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './stock-movement-dialog.component.html',
  styleUrl: './stock-movement-dialog.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class StockMovementDialogComponent {
  private readonly destroyRef = inject(DestroyRef);
  private readonly dialogRef = viewChild<ElementRef<HTMLDialogElement>>('dialog');

  readonly saving = input(false);
  readonly errorMessage = input<string | null>(null);
  readonly fieldErrors = input<Record<string, string>>({});

  readonly confirmed = output<StockMovementRequest>();

  readonly context = signal<DialogContext | null>(null);
  readonly visible = signal(false);
  readonly validationError = signal<string | null>(null);
  readonly initialOffice = signal<number | null>(null);
  readonly initialCounter = signal<number | null>(null);
  readonly entryQuantity = signal<number | null>(null);
  readonly error = computed(() => this.validationError() ?? this.errorMessage());
  private dialogTimer?: ReturnType<typeof setTimeout>;

  constructor() {
    this.destroyRef.onDestroy(() => clearTimeout(this.dialogTimer));
  }

  show(context: DialogContext): void {
    this.context.set(context);
    this.initialOffice.set(null);
    this.initialCounter.set(null);
    this.entryQuantity.set(null);
    this.validationError.set(null);
    const dialog = this.dialogRef()?.nativeElement;
    if (dialog && !dialog.open) dialog.showModal();
    clearTimeout(this.dialogTimer);
    requestAnimationFrame(() => this.visible.set(true));
  }

  close(): void {
    this.visible.set(false);
    clearTimeout(this.dialogTimer);
    this.dialogTimer = setTimeout(() => this.dialogRef()?.nativeElement.close(), DIALOG_CLOSE_DELAY_MS);
  }

  onCancel(event: Event): void {
    event.preventDefault();
    this.close();
  }

  fieldError(field: string): string {
    return this.validationError() ? '' : this.fieldErrors()[field] ?? '';
  }

  confirm(): void {
    const context = this.context();
    if (!context || this.saving()) return;

    this.validationError.set(null);

    if (context.mode === 'initial') {
      const office = this.initialOffice();
      const counter = this.initialCounter();
      if (office === null || counter === null || !Number.isInteger(office) || !Number.isInteger(counter) ||
          office < 0 || counter < 0) {
        this.validationError.set('Las existencias deben ser números enteros mayores o iguales a cero.');
        return;
      }
    } else {
      const quantity = this.entryQuantity();
      if (quantity === null || !Number.isInteger(quantity) || quantity <= 0) {
        this.validationError.set('La cantidad debe ser un número entero mayor que cero.');
        return;
      }
    }

    this.confirmed.emit({
      context,
      values: {
        officeQuantity: this.initialOffice(),
        counterQuantity: this.initialCounter(),
        quantity: this.entryQuantity()
      }
    });
  }
}

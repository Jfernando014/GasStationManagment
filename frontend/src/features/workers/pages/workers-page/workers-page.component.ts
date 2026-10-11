import { Component, DestroyRef, ElementRef, computed, inject, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl } from '@angular/forms';
import { Subject, catchError, of, switchMap, tap } from 'rxjs';

import { AvatarComponent } from '../../../../atomic-design/atoms/avatar/avatar.component';
import { SearchFieldComponent } from '../../../../atomic-design/atoms/search-field/search-field.component';
import { StatusBadgeComponent } from '../../../../atomic-design/atoms/status-badge/status-badge.component';
import { EmptyStateComponent } from '../../../../atomic-design/molecules/empty-state/empty-state.component';
import { SelectFieldComponent, SelectOption } from '../../../../atomic-design/molecules/select-field/select-field.component';
import { WorkersService } from '../../../../core/services/workers.service';
import { getErrorMessage } from '../../../../shared/helpers/problem-details.helpers';
import { RoleOption } from '../../../../shared/models/dtos/shift-catalog.dto';
import { Worker } from '../../../../shared/models/dtos/worker.dto';
import { WorkerFilters } from '../../../../shared/models/worker.model';
import { WorkerFormComponent } from '../../components/worker-form/worker-form.component';

const TOAST_DURATION_MS = 3000;
/** Same as --duration-normal: the modal is closed once its fade-out has finished. */
const DIALOG_CLOSE_DELAY_MS = 320;

/** Personal › Vendedores: filtered list on the left, edit form on the right and a modal to register. */
@Component({
  selector: 'app-workers-page',
  standalone: true,
  imports: [
    AvatarComponent,
    SearchFieldComponent,
    StatusBadgeComponent,
    EmptyStateComponent,
    SelectFieldComponent,
    WorkerFormComponent
  ],
  templateUrl: './workers-page.component.html',
  styleUrl: './workers-page.component.css'
})
export class WorkersPageComponent {
  private readonly workersService = inject(WorkersService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');

  readonly workers = signal<Worker[]>([]);
  readonly roles = signal<RoleOption[]>([]);
  readonly filters = signal<WorkerFilters>({});
  readonly loading = signal(false);
  readonly loadError = signal<string | null>(null);
  readonly selected = signal<Worker | null>(null);
  /** Drives the open / close animation of the modal. */
  readonly dialogOpen = signal(false);
  /** Grows each time the modal opens, so it starts with a fresh form. */
  readonly dialogSession = signal(0);
  readonly toastText = signal('');
  readonly toastVisible = signal(false);

  readonly statusFilter = new FormControl<boolean | null>(null);
  readonly roleFilter = new FormControl<string | null>(null);
  readonly statusOptions: SelectOption<boolean>[] = [
    { value: true, label: 'Activos' },
    { value: false, label: 'Inactivos' }
  ];
  readonly roleOptions = computed<SelectOption<string>[]>(() =>
    this.roles().map(role => ({ value: role.id, label: role.name }))
  );
  readonly hasFilters = computed(() => {
    const { active, roleId, search } = this.filters();
    return (active !== undefined && active !== null) || !!roleId || !!search?.trim();
  });

  private readonly reload$ = new Subject<void>();
  private toastTimer?: ReturnType<typeof setTimeout>;
  private dialogCloseTimer?: ReturnType<typeof setTimeout>;

  constructor() {
    this.reload$
      .pipe(
        tap(() => {
          this.loading.set(true);
          this.loadError.set(null);
        }),
        switchMap(() =>
          this.workersService.list(this.filters()).pipe(
            catchError((error: unknown) => {
              this.loadError.set(getErrorMessage(error));
              return of(null);
            })
          )
        ),
        takeUntilDestroyed()
      )
      .subscribe(workers => {
        this.loading.set(false);
        if (workers) {
          this.workers.set(workers);
        }
      });

    this.statusFilter.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe(active => this.applyFilters({ active }));
    this.roleFilter.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe(roleId => this.applyFilters({ roleId }));

    this.destroyRef.onDestroy(() => {
      clearTimeout(this.toastTimer);
      clearTimeout(this.dialogCloseTimer);
    });

    this.loadRoles();
    this.reload$.next();
  }

  onSearch(search: string): void {
    this.applyFilters({ search });
  }

  retry(): void {
    if (this.roles().length === 0) {
      this.loadRoles();
    }
    this.reload$.next();
  }

  /**
   * Opens the register modal as a native modal dialog: the rest of the page becomes inert and the focus stays
   * inside. The `visible` class is added on the next frame so the card animates in.
   */
  openDialog(): void {
    clearTimeout(this.dialogCloseTimer);
    this.dialogSession.update(session => session + 1);
    const dialog = this.dialog().nativeElement;
    if (!dialog.open) {
      dialog.showModal();
    }
    requestAnimationFrame(() => this.dialogOpen.set(true));
  }

  closeDialog(): void {
    this.dialogOpen.set(false);
    clearTimeout(this.dialogCloseTimer);
    this.dialogCloseTimer = setTimeout(() => this.dialog().nativeElement.close(), DIALOG_CLOSE_DELAY_MS);
  }

  /** Escape: close with the animation instead of the instant native close. */
  onDialogCancel(event: Event): void {
    event.preventDefault();
    this.closeDialog();
  }

  /** Closes the modal when the click lands on the dark backdrop, not on the card. */
  onBackdropClick(event: MouseEvent): void {
    if (event.target === event.currentTarget) {
      this.closeDialog();
    }
  }

  select(worker: Worker): void {
    this.selected.set(worker);
  }

  onCreated(worker: Worker): void {
    this.closeDialog();
    this.showToast('Vendedor registrado correctamente');
    this.selected.set(worker);
    this.reload$.next();
  }

  onUpdated(worker: Worker): void {
    this.showToast('Vendedor actualizado correctamente');
    this.selected.set(worker);
    this.reload$.next();
  }

  onEditCancelled(): void {
    this.selected.set(null);
  }

  private applyFilters(changes: WorkerFilters): void {
    this.filters.update(filters => ({ ...filters, ...changes }));
    this.reload$.next();
  }

  private loadRoles(): void {
    this.workersService
      .listRoles()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: roles => this.roles.set(roles),
        error: (error: unknown) => this.loadError.set(getErrorMessage(error))
      });
  }

  private showToast(text: string): void {
    clearTimeout(this.toastTimer);
    this.toastText.set(text);
    this.toastVisible.set(true);
    this.toastTimer = setTimeout(() => this.toastVisible.set(false), TOAST_DURATION_MS);
  }
}

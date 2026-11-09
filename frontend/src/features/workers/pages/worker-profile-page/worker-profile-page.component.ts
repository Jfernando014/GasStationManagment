import { ChangeDetectionStrategy, Component, DestroyRef, ElementRef, computed, inject, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { catchError, map, of, switchMap, tap } from 'rxjs';

import { EmptyStateComponent } from '../../../../atomic-design/molecules/empty-state/empty-state.component';
import { TabItem, TabsComponent } from '../../../../atomic-design/molecules/tabs/tabs.component';
import { ToastComponent } from '../../../../atomic-design/molecules/toast/toast.component';
import { ShiftsScheduleService } from '../../../../core/services/shifts-schedule.service';
import { ToastService } from '../../../../core/services/toast.service';
import { WorkersService } from '../../../../core/services/workers.service';
import { getErrorMessage, isApiError } from '../../../../shared/helpers/problem-details.helpers';
import { monthOf, operationalToday } from '../../../../shared/helpers/shift-display.helpers';
import { RoleOption, ShiftCode } from '../../../../shared/models/dtos/shift-catalog.dto';
import { Worker } from '../../../../shared/models/dtos/worker.dto';
import { WorkerFormComponent } from '../../components/worker-form/worker-form.component';
import { WorkerProfileHeaderComponent } from '../../components/worker-profile-header/worker-profile-header.component';
import { WorkerProfilePerformanceTabComponent } from '../../components/worker-profile-performance-tab/worker-profile-performance-tab.component';
import { WorkerProfileShiftsTabComponent } from '../../components/worker-profile-shifts-tab/worker-profile-shifts-tab.component';
import { isValidMonth } from '../../helpers/worker-profile.helpers';

type ProfileTab = 'turnos' | 'desempeno';
type LoadStatus = 'loading' | 'ready' | 'missing' | 'error';

const TABS: TabItem[] = [
  { id: 'turnos', label: 'Turnos del mes' },
  { id: 'desempeno', label: 'Desempeño histórico' }
];

/** Same as --duration-normal: the dialog is closed once its fade-out has finished. */
const DIALOG_CLOSE_DELAY_MS = 320;
const SAVED_MESSAGE = 'Datos del vendedor actualizados';

/**
 * Personal › Vendedores › one worker: their data, the schedule of the month and their historical performance.
 * The tab and the month live in the URL (?tab=turnos|desempeno&mes=yyyy-MM), so reloading or sharing keeps the view.
 */
@Component({
  selector: 'app-worker-profile-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink,
    EmptyStateComponent,
    TabsComponent,
    ToastComponent,
    WorkerFormComponent,
    WorkerProfileHeaderComponent,
    WorkerProfileShiftsTabComponent,
    WorkerProfilePerformanceTabComponent
  ],
  templateUrl: './worker-profile-page.component.html',
  styleUrl: './worker-profile-page.component.css'
})
export class WorkerProfilePageComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly workersService = inject(WorkersService);
  private readonly shiftsService = inject(ShiftsScheduleService);
  private readonly toast = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');
  private readonly header = viewChild(WorkerProfileHeaderComponent);

  protected readonly tabs = TABS;
  private readonly currentMonth = monthOf(operationalToday());

  private readonly params = toSignal(this.route.paramMap, { initialValue: this.route.snapshot.paramMap });
  private readonly query = toSignal(this.route.queryParamMap, { initialValue: this.route.snapshot.queryParamMap });

  protected readonly workerId = computed(() => this.params().get('workerId') ?? '');
  protected readonly tab = computed<ProfileTab>(() => (this.query().get('tab') === 'desempeno' ? 'desempeno' : 'turnos'));
  protected readonly month = computed(() => {
    const month = this.query().get('mes');
    return isValidMonth(month) ? month : this.currentMonth;
  });

  protected readonly status = signal<LoadStatus>('loading');
  protected readonly worker = signal<Worker | null>(null);
  protected readonly errorMessage = signal('');
  /** Codes of the catalog by name; they decide the color of every chip. Without them the name of the code is used. */
  protected readonly catalog = signal<Record<string, ShiftCode>>({});
  protected readonly roles = signal<RoleOption[]>([]);

  /** Drives the open / close animation of the edit dialog. */
  protected readonly dialogOpen = signal(false);
  /** Grows each time the dialog opens, so it starts with a fresh form. */
  protected readonly dialogSession = signal(0);

  private readonly reloads = signal(0);
  private dialogCloseTimer?: ReturnType<typeof setTimeout>;

  constructor() {
    toObservable(computed(() => ({ id: this.workerId(), reload: this.reloads() })))
      .pipe(
        tap(() => this.status.set('loading')),
        switchMap(({ id }) =>
          this.workersService.getById(id).pipe(
            map(worker => ({ worker, status: 'ready' as LoadStatus, message: '' })),
            catchError((error: unknown) =>
              of({
                worker: null,
                // A malformed id (400) and an unknown one (404) both mean there is no such worker.
                status: (isApiError(error) && (error.status === 400 || error.status === 404) ? 'missing' : 'error') as LoadStatus,
                message: getErrorMessage(error)
              })
            )
          )
        ),
        takeUntilDestroyed()
      )
      .subscribe(result => {
        this.worker.set(result.worker);
        this.errorMessage.set(result.message);
        this.status.set(result.status);
      });

    this.shiftsService
      .getCatalog()
      .pipe(takeUntilDestroyed())
      .subscribe({
        next: codes => this.catalog.set(Object.fromEntries(codes.map(code => [code.code, code]))),
        error: () => this.catalog.set({})
      });

    this.workersService
      .listRoles()
      .pipe(takeUntilDestroyed())
      .subscribe({ next: roles => this.roles.set(roles), error: () => this.roles.set([]) });

    this.destroyRef.onDestroy(() => clearTimeout(this.dialogCloseTimer));
  }

  protected selectTab(tab: string): void {
    this.updateUrl({ tab, mes: this.month() });
  }

  protected selectMonth(month: string): void {
    this.updateUrl({ tab: this.tab(), mes: month });
  }

  protected retry(): void {
    this.reloads.update(count => count + 1);
  }

  /** Opens the edit form as a native modal dialog: the rest of the page becomes inert and the focus stays inside. */
  protected openEdit(): void {
    clearTimeout(this.dialogCloseTimer);
    this.dialogSession.update(session => session + 1);
    const dialog = this.dialog().nativeElement;
    if (!dialog.open) {
      dialog.showModal();
    }
    requestAnimationFrame(() => this.dialogOpen.set(true));
  }

  protected closeEdit(): void {
    this.dialogOpen.set(false);
    clearTimeout(this.dialogCloseTimer);
    this.dialogCloseTimer = setTimeout(() => {
      this.dialog().nativeElement.close();
      this.header()?.focusEdit();
    }, DIALOG_CLOSE_DELAY_MS);
  }

  /** Escape: close with the animation instead of the instant native close. */
  protected onDialogCancel(event: Event): void {
    event.preventDefault();
    this.closeEdit();
  }

  /** Closes the dialog when the click lands on the dark backdrop, not on the card. */
  protected onBackdropClick(event: MouseEvent): void {
    if (event.target === event.currentTarget) {
      this.closeEdit();
    }
  }

  protected onSaved(worker: Worker): void {
    this.worker.set(worker);
    this.closeEdit();
    this.toast.show(SAVED_MESSAGE);
  }

  /** Replaces the URL instead of adding history entries, so "back" returns to the list. */
  private updateUrl(queryParams: { tab: string; mes: string }): void {
    void this.router.navigate([], { relativeTo: this.route, queryParams, queryParamsHandling: 'merge', replaceUrl: true });
  }
}

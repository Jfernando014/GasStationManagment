import {
  Component,
  DestroyRef,
  ElementRef,
  afterNextRender,
  computed,
  effect,
  inject,
  input,
  output,
  signal,
  untracked
} from '@angular/core';
import { NgTemplateOutlet } from '@angular/common';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { AbstractControl, NonNullableFormBuilder, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { Observable } from 'rxjs';

import { AvatarComponent } from '../../../../atomic-design/atoms/avatar/avatar.component';
import { StatusBadgeComponent } from '../../../../atomic-design/atoms/status-badge/status-badge.component';
import { FormFieldComponent } from '../../../../atomic-design/molecules/form-field/form-field.component';
import { SelectFieldComponent, SelectOption } from '../../../../atomic-design/molecules/select-field/select-field.component';
import { WorkersService } from '../../../../core/services/workers.service';
import { SERVER_ERROR_KEY } from '../../../../shared/helpers/form-control.helpers';
import { applyFieldErrors, getErrorMessage, isApiError } from '../../../../shared/helpers/problem-details.helpers';
import { toNameCase } from '../../../../shared/helpers/text.helpers';
import { RoleOption } from '../../../../shared/models/dtos/shift-catalog.dto';
import { Worker, WorkerRequest } from '../../../../shared/models/dtos/worker.dto';

// Same rules as the backend (CreateWorkerRequest / UpdateWorkerRequest)
const DOCUMENT_MIN_LENGTH = 6;
const DOCUMENT_MAX_LENGTH = 12;
const FULL_NAME_MAX_LENGTH = 150;
const DOCUMENT_PATTERN = /^\d*$/;
const FULL_NAME_PATTERN = /^[\p{L}\s'-]*$/u;

const DUPLICATE_DOCUMENT_CODE = 'DUPLICATE_DOCUMENT';
const DUPLICATE_DOCUMENT_MESSAGE = 'El documento ya se encuentra registrado para otro trabajador';

/** Validator that checks a pattern and returns its own message (shown by the form field). */
function formatValidator(pattern: RegExp, message: string): ValidatorFn {
  return (control: AbstractControl<string>): ValidationErrors | null =>
    pattern.test(control.value) ? null : { format: message };
}

/**
 * Form to create a worker (no worker given) or edit one, and to activate or deactivate it.
 * `variant` only changes the frame: `dialog` for the create modal, `panel` for the side panel.
 * Emits `saved` with the worker returned by the backend.
 */
@Component({
  selector: 'app-worker-form',
  standalone: true,
  imports: [
    NgTemplateOutlet,
    ReactiveFormsModule,
    AvatarComponent,
    StatusBadgeComponent,
    FormFieldComponent,
    SelectFieldComponent
  ],
  templateUrl: './worker-form.component.html',
  styleUrl: './worker-form.component.css'
})
export class WorkerFormComponent {
  private readonly workersService = inject(WorkersService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly formBuilder = inject(NonNullableFormBuilder);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  readonly worker = input<Worker | null>(null);
  readonly roles = input<RoleOption[]>([]);
  readonly variant = input<'panel' | 'dialog'>('panel');
  readonly saved = output<Worker>();
  readonly cancelled = output<void>();

  readonly documentMaxLength = DOCUMENT_MAX_LENGTH;
  readonly fullNameMaxLength = FULL_NAME_MAX_LENGTH;
  readonly documentHint = `Solo números, entre ${DOCUMENT_MIN_LENGTH} y ${DOCUMENT_MAX_LENGTH} dígitos, sin puntos ni guiones`;

  readonly saving = signal(false);
  readonly formError = signal<string | null>(null);

  readonly form = this.formBuilder.group({
    document: [
      '',
      [
        Validators.required,
        Validators.minLength(DOCUMENT_MIN_LENGTH),
        Validators.maxLength(DOCUMENT_MAX_LENGTH),
        formatValidator(DOCUMENT_PATTERN, 'Solo se permiten números, sin puntos, guiones ni espacios')
      ]
    ],
    fullName: [
      '',
      [
        Validators.required,
        Validators.maxLength(FULL_NAME_MAX_LENGTH),
        formatValidator(FULL_NAME_PATTERN, 'El nombre solo puede tener letras, espacios, guiones o apóstrofos')
      ]
    ],
    roleId: this.formBuilder.control<string | null>(null, Validators.required)
  });

  private readonly roleId = toSignal(this.form.controls.roleId.valueChanges, { initialValue: null });

  /** Prefix of the field ids, so the modal and the panel can be on the page at the same time. */
  readonly idPrefix = computed(() => `worker-${this.variant()}`);
  readonly roleOptions = computed<SelectOption<string>[]>(() =>
    this.roles().map(role => ({ value: role.id, label: `${toNameCase(role.name)} (surtidor ${role.dispenser})` }))
  );
  readonly dispenserHint = computed(() => {
    const role = this.roles().find(option => option.id === this.roleId());
    return role ? `Surtidor asignado: ${role.dispenser}` : '';
  });

  constructor() {
    effect(() => {
      const worker = this.worker();
      untracked(() => this.resetForm(worker));
    });
    afterNextRender(() => {
      if (this.variant() === 'dialog') {
        this.host.nativeElement.querySelector<HTMLInputElement>('input')?.focus();
      }
    });
  }

  submit(): void {
    if (this.saving()) {
      return;
    }
    const { fullName, document } = this.form.getRawValue();
    this.form.patchValue({ fullName: toNameCase(fullName), document: document.trim() });
    this.form.markAllAsTouched();
    Object.values(this.form.controls).forEach(control => control.markAsDirty());
    const value = this.form.getRawValue();
    if (this.form.invalid || !value.roleId) {
      return;
    }
    const request: WorkerRequest = { fullName: value.fullName, document: value.document, roleId: value.roleId };
    const worker = this.worker();
    this.send(worker ? this.workersService.update(worker.id, request) : this.workersService.create(request));
  }

  /** Shows the name already normalized when the user leaves the field. */
  normalizeFullName(): void {
    const control = this.form.controls.fullName;
    const normalized = toNameCase(control.value);
    if (normalized !== control.value) {
      control.setValue(normalized);
    }
  }

  toggleStatus(): void {
    const worker = this.worker();
    if (!worker || this.saving()) {
      return;
    }
    this.send(this.workersService.changeStatus(worker.id, !worker.active));
  }

  private send(request: Observable<Worker>): void {
    this.saving.set(true);
    this.formError.set(null);
    request.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: worker => {
        this.saving.set(false);
        this.saved.emit(worker);
      },
      error: (error: unknown) => {
        this.saving.set(false);
        this.showError(error);
      }
    });
  }

  private showError(error: unknown): void {
    if (isApiError(error) && error.code === DUPLICATE_DOCUMENT_CODE) {
      const document = this.form.controls.document;
      document.setErrors({ [SERVER_ERROR_KEY]: DUPLICATE_DOCUMENT_MESSAGE });
      document.markAsTouched();
      return;
    }
    if (!applyFieldErrors(this.form, error)) {
      this.formError.set(getErrorMessage(error));
    }
  }

  private resetForm(worker: Worker | null): void {
    this.form.reset({
      document: worker?.document ?? '',
      fullName: worker?.fullName ?? '',
      roleId: worker?.roleId ?? null
    });
    this.formError.set(null);
  }
}

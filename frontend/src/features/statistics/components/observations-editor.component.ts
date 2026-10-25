import { Component, inject, output } from '@angular/core';
import { FormArray, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { targetDecimalValidator, decimalText } from '../../../shared/helpers/sales-target.helpers';
import { serializeObservations } from '../../../shared/helpers/sales-statistics.helpers';
import { SalesJourney } from '../../../shared/models/dtos/sales-statistics.dto';
import { SalesTargetCategory } from '../../../shared/models/dtos/sales-target.dto';
import { StatisticsWorkspace } from '../statistics-workspace';

@Component({ selector: 'app-observations-editor', standalone: true, imports: [ReactiveFormsModule],
  templateUrl: './observations-editor.component.html', styleUrls: ['../statistics.css', './observations-editor.component.css'] })
export class ObservationsEditorComponent {
  readonly workspace = inject(StatisticsWorkspace);
  readonly saved = output<void>();
  readonly category = new FormControl<SalesTargetCategory>(this.workspace.category(), { nonNullable: true });
  readonly rows = new FormArray(this.workspace.observations().map(row => this.row(row.date, row.journey, row.value)));
  readonly form = new FormGroup({ category: this.category, observations: this.rows });
  error = '';
  private row(date = this.workspace.from(), journey: SalesJourney = 'DAY', value = '') {
    return new FormGroup({ date: new FormControl(date, { nonNullable: true, validators: Validators.required }),
      journey: new FormControl<SalesJourney>(journey, { nonNullable: true }),
      value: new FormControl(value, { nonNullable: true, validators: [Validators.required, targetDecimalValidator] }) });
  }
  add(): void { if (this.rows.length < 1000) this.rows.push(this.row()); }
  save(): void {
    this.form.markAllAsTouched(); this.error = '';
    if (this.form.invalid) { this.error = 'Completa cada fecha y un importe no negativo, sin separadores de miles y con hasta 12 decimales.'; return; }
    const rows = this.rows.getRawValue().map(row => ({ ...row, value: decimalText(row.value) ?? '' }));
    try {
      serializeObservations({ from: this.workspace.from(), to: this.workspace.to(), category: this.category.value, outputScale: 2, observations: rows });
      this.workspace.replaceObservations(this.category.value, rows); this.saved.emit();
    } catch (error: unknown) { this.error = error instanceof Error ? error.message : 'Observaciones inválidas'; }
  }
}

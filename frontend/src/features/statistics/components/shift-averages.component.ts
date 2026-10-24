import { Component, computed, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { EmptyStateComponent } from '../../../atomic-design/molecules/empty-state/empty-state.component';
import { difference, formatStatistics, participation } from '../../../shared/helpers/sales-statistics.helpers';
import { SalesObservation } from '../../../shared/models/dtos/sales-statistics.dto';
import { StatisticsWorkspace } from '../statistics-workspace';

@Component({
  selector: 'app-shift-averages', standalone: true, imports: [FormsModule, EmptyStateComponent],
  templateUrl: './shift-averages.component.html', styleUrl: '../statistics.css'
})
export class ShiftAveragesComponent {
  readonly workspace = inject(StatisticsWorkspace);
  readonly format = formatStatistics;
  readonly rows = computed(() => this.workspace.averages() ? [...this.workspace.selected()].sort((a, b) => a.date.localeCompare(b.date) || a.journey.localeCompare(b.journey)) : []);
  delta(row: SalesObservation): string | null {
    const average = this.workspace.averages()?.series.find(s => s.journey === row.journey)?.average ?? null;
    return average === null ? null : difference(row.value.replace(',', '.'), average);
  }
  percent(row: SalesObservation): string | null {
    const average = this.workspace.averages()?.series.find(s => s.journey === row.journey)?.average ?? null;
    const delta = this.delta(row);
    return average === null || delta === null ? null : participation(delta, average);
  }
}

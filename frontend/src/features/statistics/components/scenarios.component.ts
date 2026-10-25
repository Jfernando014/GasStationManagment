import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { formatStatistics, WEEKDAYS } from '../../../shared/helpers/sales-statistics.helpers';
import { SalesScenario } from '../../../shared/models/dtos/sales-statistics.dto';
import { StatisticsWorkspace } from '../statistics-workspace';

@Component({ selector: 'app-statistics-scenarios', standalone: true, imports: [FormsModule],
  templateUrl: './scenarios.component.html', styleUrls: ['../statistics.css', './scenarios.component.css'] })
export class ScenariosComponent {
  readonly workspace = inject(StatisticsWorkspace);
  readonly days = WEEKDAYS;
  readonly format = formatStatistics;
  first: SalesScenario = { dayOfWeek: 'FRIDAY', journey: 'DAY' };
  second: SalesScenario = { dayOfWeek: 'SUNDAY', journey: 'NIGHT' };
  readonly scenarios = [this.first, this.second];
}

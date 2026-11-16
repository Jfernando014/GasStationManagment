import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import {
  ChangeDetectionStrategy, Component, DestroyRef, computed, effect, inject, input, signal, untracked
} from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Subscription } from 'rxjs';

import { ActionButtonDirective } from '../../../../atomic-design/atoms/action-button/action-button.directive';
import { SkeletonComponent } from '../../../../atomic-design/atoms/skeleton/skeleton.component';
import { TagComponent } from '../../../../atomic-design/atoms/tag/tag.component';
import { EmptyStateComponent } from '../../../../atomic-design/molecules/empty-state/empty-state.component';
import { formatShortDate } from '../../../../shared/helpers/format.helpers';
import { capitalize, stationToday } from '../../../../shared/helpers/shift-display.helpers';
import { ParameterVersionDrawerComponent } from '../../components/parameter-version-drawer/parameter-version-drawer.component';
import { hourOfDay, hoursText, ParameterChange, parameterChanges } from '../../helpers/overtime-display.helpers';
import { LoadResult, ParameterVersion } from '../../models/overtime.model';
import { OvertimeParametersService } from '../../services/overtime-parameters.service';

type VersionTag = { label: string; variant: 'green' | 'blue' | 'gray' };

interface VersionView {
  id: number;
  title: string;
  tag: VersionTag;
  dates: string;
  reason: string | null;
  selected: boolean;
}

/** Configuración › Parámetros › Horas extra: the versions of the parameters and what each one sets. */
@Component({
  selector: 'app-overtime-parameters-tab',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ActionButtonDirective, SkeletonComponent, TagComponent, EmptyStateComponent, ParameterVersionDrawerComponent
  ],
  templateUrl: './overtime-parameters-tab.component.html',
  styleUrl: './overtime-parameters-tab.component.css'
})
export class OvertimeParametersTabComponent {
  private readonly service = inject(OvertimeParametersService);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);

  /** Only the administrator exists today; with more roles this hides "Nueva versión". */
  readonly canManage = input(true);

  private readonly query = toSignal(this.route.queryParamMap, { initialValue: this.route.snapshot.queryParamMap });

  /** Answer for the history; null while it loads. */
  private readonly result = signal<LoadResult<ParameterVersion[]> | null>(null);
  private readonly selectedId = signal<number | null>(null);
  protected readonly drawerOpen = signal(false);
  protected readonly skeletonItems = [0, 1, 2];
  private loadSubscription: Subscription | null = null;
  /** The version asked for in the address is shown only the first time the history arrives. */
  private addressApplied = false;

  protected readonly versions = computed(() => {
    const result = this.result();
    return result?.outcome === 'OK' ? result.data : [];
  });
  protected readonly state = computed(() => {
    const result = this.result();
    return !result ? 'loading' : result.outcome === 'OK' ? 'ready' : 'error';
  });
  protected readonly errorMessage = computed(() => {
    const result = this.result();
    return result && result.outcome !== 'OK' ? result.message : '';
  });

  protected readonly selected = computed(() =>
    this.versions().find(version => version.id === this.selectedId()) ?? null);

  protected readonly views = computed<VersionView[]>(() => this.versions().map(version => ({
    id: version.id,
    title: `Versión ${version.number}`,
    tag: tagOf(version),
    dates: version.validTo
      ? `${formatShortDate(version.validFrom)} – ${formatShortDate(version.validTo)}`
      : `Desde el ${formatShortDate(version.validFrom)}`,
    reason: version.reason,
    selected: version.id === this.selectedId()
  })));

  /** Values of the version on screen, as the sentences of the list. */
  protected readonly details = computed(() => {
    const version = this.selected();
    if (!version) return [];
    return [
      { label: 'La noche empieza a las', value: `${hourOfDay(version.nightStartHour)} (termina a las 06:00)` },
      { label: 'Descanso no pagado', value: hoursText(version.restHours) },
      { label: 'Mínimo de horas del turno para descontar el descanso', value: hoursText(version.restMinShiftHours) },
      { label: 'HEDO fijas por quincena', value: hoursText(version.fixedHedoHours) }
    ];
  });
  protected readonly roles = computed(() =>
    (this.selected()?.fixedHedoRoles ?? []).map(role => capitalize(role.toLowerCase())));

  protected readonly selectedTag = computed(() => {
    const version = this.selected();
    return version ? tagOf(version) : null;
  });
  protected readonly selectedPeriod = computed(() => {
    const view = this.views().find(item => item.selected);
    return view?.dates ?? '';
  });
  protected readonly selectedWho = computed(() => {
    const version = this.selected();
    if (!version) return '';
    const day = stationToday(new Date(version.createdAt));
    return `Creada por ${version.createdBy ?? 'un usuario'} · ${formatShortDate(day)}`;
  });

  /** What the version on screen changed with respect to the one before it; null for the first version. */
  protected readonly changes = computed<ParameterChange[] | null>(() => {
    const versions = this.versions();
    const index = versions.findIndex(version => version.id === this.selectedId());
    if (index < 0 || index === versions.length - 1) return null;
    return parameterChanges(versions[index + 1], versions[index]);
  });

  constructor() {
    this.load(null);
    // Once the history is here, the version asked for in the address (or else the one in force) is on screen.
    effect(() => {
      const versions = this.versions();
      const asked = this.query().get('desde');
      untracked(() => {
        if (versions.length === 0) return;
        const fromAddress = this.addressApplied ? undefined : versions.find(version => version.validFrom === asked);
        this.addressApplied = true;
        const wanted = fromAddress
          ?? versions.find(version => version.id === this.selectedId())
          ?? versions.find(version => version.current) ?? versions[0];
        this.selectedId.set(wanted.id);
      });
    });
  }

  protected select(id: number): void {
    this.selectedId.set(id);
  }

  protected retry(): void {
    this.load(null);
  }

  protected openDrawer(): void {
    this.drawerOpen.set(true);
  }

  protected closeDrawer(): void {
    this.drawerOpen.set(false);
  }

  /** A version was created: the history is asked again and the new version is shown. */
  protected reload(): void {
    this.load('newest');
  }

  private load(select: 'newest' | null): void {
    this.loadSubscription?.unsubscribe();
    if (select === null) this.result.set(null);
    this.loadSubscription = this.service.getHistory().pipe(takeUntilDestroyed(this.destroyRef)).subscribe(result => {
      if (result.outcome === 'OK' && select === 'newest' && result.data.length > 0) this.selectedId.set(result.data[0].id);
      this.result.set(result);
    });
  }
}

function tagOf(version: ParameterVersion): VersionTag {
  if (version.current) return { label: 'Vigente', variant: 'green' };
  return version.scheduled ? { label: 'Programada', variant: 'blue' } : { label: 'Anterior', variant: 'gray' };
}

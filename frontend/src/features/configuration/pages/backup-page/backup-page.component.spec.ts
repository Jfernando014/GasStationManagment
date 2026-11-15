import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { AuthService } from '../../../../core/services/auth.service';
import { BackupAuditEntry, BackupService } from '../../../../core/services/backup.service';
import { ApiError } from '../../../../shared/models/api-error.model';
import { BackupPageComponent } from './backup-page.component';

const RAW_ERROR = 'pg_restore: error: could not execute query: DROP SEQUENCE backup_audit_id_seq';

const FAILED_ENTRY: BackupAuditEntry = {
  id: 3,
  username: 'admin',
  action: 'IMPORT',
  fileName: 'gas-station.dump',
  sizeBytes: 1024,
  status: 'FAILED',
  message: RAW_ERROR,
  clientIp: '127.0.0.1',
  startedAt: '2026-11-14T11:22:43',
  finishedAt: null
};

describe('BackupPageComponent', () => {
  let backupService: {
    getAudit: ReturnType<typeof vi.fn>;
    importBackup: ReturnType<typeof vi.fn>;
    exportBackup: ReturnType<typeof vi.fn>;
  };
  let authService: { logout: ReturnType<typeof vi.fn> };
  let router: { navigate: ReturnType<typeof vi.fn> };

  function create() {
    const fixture = TestBed.createComponent(BackupPageComponent);
    fixture.detectChanges();
    return fixture;
  }

  beforeEach(async () => {
    backupService = {
      getAudit: vi.fn(() => of([FAILED_ENTRY])),
      importBackup: vi.fn(),
      exportBackup: vi.fn()
    };
    authService = { logout: vi.fn() };
    router = { navigate: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [BackupPageComponent],
      providers: [
        { provide: BackupService, useValue: backupService },
        { provide: AuthService, useValue: authService },
        { provide: Router, useValue: router }
      ]
    }).compileComponents();
  });

  it('does not render the raw message of a failed audit entry', () => {
    const fixture = create();
    const detail = fixture.nativeElement.querySelector('.backup-status-detail') as HTMLElement;

    expect(fixture.nativeElement.textContent).toContain('La operación falló. Revisa los logs del servidor.');
    expect(fixture.nativeElement.textContent).not.toContain(RAW_ERROR);
    expect(detail.getAttribute('title')).toBeNull();
  });

  it('does not render the raw restore error', () => {
    const fixture = create();
    const component = fixture.componentInstance;
    backupService.importBackup.mockReturnValue(throwError(() => new ApiError(500, {
      code: 'RESTORE_FAILED',
      detail: RAW_ERROR
    })));

    component.selectedFile.set(new File(['dump'], 'backup.dump'));
    component.confirmation.set('RESTAURAR');
    component.restore();
    fixture.detectChanges();

    expect(component.restoreError()).toBe(
      'No se pudo restaurar la base de datos. Revisa los logs del servidor o intenta de nuevo.'
    );
    expect(fixture.nativeElement.textContent).not.toContain(RAW_ERROR);
  });
});

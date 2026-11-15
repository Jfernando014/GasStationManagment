import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { BackupAuditEntry, BackupService, RestoreResponse } from './backup.service';

const URL = 'http://localhost:8080/api/v1/backups';

describe('BackupService', () => {
  let service: BackupService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(BackupService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('downloads the dump and reads its file name from the header', () => {
    const blob = new Blob(['PGDMP'], { type: 'application/octet-stream' });
    let fileName = '';

    service.exportBackup().subscribe(file => {
      fileName = file.fileName;
      expect(file.blob).toBe(blob);
    });

    const call = http.expectOne(`${URL}/export`);
    expect(call.request.method).toBe('GET');
    expect(call.request.responseType).toBe('blob');
    call.flush(blob, {
      headers: { 'Content-Disposition': 'attachment; filename="gas-station-20261113.dump"' }
    });
    expect(fileName).toBe('gas-station-20261113.dump');
  });

  it('uploads the file with the confirmation header', () => {
    const file = new File(['PGDMP'], 'backup.dump', { type: 'application/octet-stream' });
    const response: RestoreResponse = {
      fileName: 'backup.dump',
      sizeBytes: 5,
      restartScheduled: true,
      message: 'Restore completed.'
    };

    service.importBackup(file, 'RESTAURAR').subscribe();

    const call = http.expectOne(`${URL}/import`);
    expect(call.request.method).toBe('POST');
    expect(call.request.headers.get('X-Confirm-Restore')).toBe('RESTAURAR');
    const form = call.request.body as FormData;
    expect((form.get('file') as File).name).toBe('backup.dump');
    call.flush(response);
  });

  it('lists the backup audit', () => {
    const audit: BackupAuditEntry[] = [{
      id: 1,
      username: 'admin',
      action: 'EXPORT',
      fileName: 'gas-station-20261113.dump',
      sizeBytes: 1024,
      status: 'SUCCEEDED',
      message: 'Backup generated',
      clientIp: '127.0.0.1',
      startedAt: '2026-11-13T15:00:00Z',
      finishedAt: '2026-11-13T15:00:05Z'
    }];

    service.getAudit().subscribe(entries => expect(entries).toEqual(audit));

    http.expectOne(`${URL}/audit`).flush(audit);
  });
});

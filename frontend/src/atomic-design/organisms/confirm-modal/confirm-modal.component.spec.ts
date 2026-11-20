import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { ConfirmModalComponent } from './confirm-modal.component';

@Component({
  standalone: true,
  imports: [ConfirmModalComponent],
  template: `
    <app-confirm-modal
      [open]="open()"
      [title]="'Eliminar tanque'"
      [message]="'¿Deseas eliminar T1C1?'"
      [busy]="busy()"
      (confirmed)="confirmCount = confirmCount + 1"
      (cancelled)="cancelCount = cancelCount + 1" />
  `
})
class HostComponent {
  readonly open = signal(true);
  readonly busy = signal(false);
  confirmCount = 0;
  cancelCount = 0;
}

describe('ConfirmModalComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [HostComponent] }).compileComponents();
  });

  it('emits confirmed and cancelled actions from modal controls and backdrop', () => {
    const fixture = TestBed.createComponent(HostComponent);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;

    expect(el.textContent).toContain('Eliminar tanque');
    expect(el.textContent).toContain('¿Deseas eliminar T1C1?');

    (el.querySelector('.btn-confirm-danger') as HTMLButtonElement).click();
    expect(fixture.componentInstance.confirmCount).toBe(1);

    (el.querySelector('.confirm-modal-backdrop') as HTMLElement).click();
    expect(fixture.componentInstance.cancelCount).toBe(1);

    (el.querySelector('.confirm-modal-close') as HTMLButtonElement).click();
    expect(fixture.componentInstance.cancelCount).toBe(2);
  });

  it('ignores backdrop dismiss when busy or closed', () => {
    const fixture = TestBed.createComponent(HostComponent);
    fixture.componentInstance.busy.set(true);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;

    (el.querySelector('.confirm-modal-backdrop') as HTMLElement).click();
    expect(fixture.componentInstance.cancelCount).toBe(0);

    fixture.componentInstance.open.set(false);
    fixture.detectChanges();
    expect(el.querySelector('.confirm-modal-backdrop')).toBeNull();
  });
});

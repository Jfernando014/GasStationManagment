import { Component, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TagComponent } from './tag.component';

@Component({
  standalone: true,
  imports: [TagComponent],
  template: `<app-tag [variant]="variant()" [dot]="dot()">Texto</app-tag>`
})
class HostComponent {
  readonly variant = signal<'green' | 'blue' | 'gray' | 'amber' | 'red' | 'orange'>('gray');
  readonly dot = signal(false);
}

describe('TagComponent', () => {
  let fixture: ComponentFixture<HostComponent>;
  let host: HostComponent;
  const tag = () => fixture.nativeElement.querySelector('app-tag') as HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [HostComponent] }).compileComponents();
    fixture = TestBed.createComponent(HostComponent);
    host = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('has no dot by default', () => {
    expect(tag().querySelector('.dot')).toBeNull();
    expect(tag().textContent?.trim()).toBe('Texto');
  });

  it('shows a decorative dot when dot is set', () => {
    host.dot.set(true);
    fixture.detectChanges();

    const dot = tag().querySelector('.dot');
    expect(dot).not.toBeNull();
    expect(dot?.getAttribute('aria-hidden')).toBe('true');
    expect(tag().textContent?.trim()).toBe('Texto');
  });

  it('keeps the shift-support blue variant', () => {
    host.variant.set('blue');
    fixture.detectChanges();

    expect(tag().classList.contains('tag--blue')).toBe(true);
    expect(tag().className).toContain('tag--blue');
  });
});

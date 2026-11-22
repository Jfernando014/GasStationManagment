import { Component, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TabItem, TabsComponent } from './tabs.component';

@Component({
  standalone: true,
  imports: [TabsComponent],
  template: `<app-tabs idPrefix="demo" label="Demo" [tabs]="tabs()" [active]="active()" (activeChange)="active.set($event)" />`
})
class HostComponent {
  readonly tabs = signal<TabItem[]>([{ id: 'a', label: 'Primera' }, { id: 'b', label: 'Segunda', tone: 'amber' }]);
  readonly active = signal('a');
}

describe('TabsComponent', () => {
  let fixture: ComponentFixture<HostComponent>;
  let host: HostComponent;
  const buttons = () => Array.from(fixture.nativeElement.querySelectorAll('[role=tab]')) as HTMLButtonElement[];

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [HostComponent] }).compileComponents();
    fixture = TestBed.createComponent(HostComponent);
    host = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('links each tab with its panel through the prefix', () => {
    expect(buttons().map(b => b.id)).toEqual(['demo-tab-a', 'demo-tab-b']);
    expect(buttons().map(b => b.getAttribute('aria-controls'))).toEqual(['demo-panel-a', 'demo-panel-b']);
    expect(buttons().map(b => b.getAttribute('aria-selected'))).toEqual(['true', 'false']);
  });

  it('shows a status dot only on the tabs that have a tone', () => {
    const [first, second] = buttons();
    expect(first.querySelector('.dot')).toBeNull();
    expect(second.querySelector('.dot')?.getAttribute('data-tone')).toBe('amber');
  });

  it('selects the next tab with the arrow keys', () => {
    buttons()[0].dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowRight', bubbles: true }));
    fixture.detectChanges();

    expect(host.active()).toBe('b');
  });
});

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { JournalStepperComponent } from './journal-stepper.component';
import { JournalStepResponse, StepStatus } from '../../../shared/models/journal.model';

describe('JournalStepperComponent (C-02, S1-15)', () => {
  let component: JournalStepperComponent;
  let fixture: ComponentFixture<JournalStepperComponent>;

  const mockSteps: JournalStepResponse[] = [
    { id: 1, stepNumber: 1, name: 'Personal', status: StepStatus.COMPLETED, completedAt: '2026-08-05T08:00:00Z' },
    { id: 2, stepNumber: 2, name: 'Vendedores', status: StepStatus.COMPLETED, completedAt: '2026-08-05T08:30:00Z' },
    { id: 3, stepNumber: 3, name: 'Electrónicos', status: StepStatus.PENDING, completedAt: null },
    { id: 4, stepNumber: 4, name: 'Caja', status: StepStatus.PENDING, completedAt: null },
    { id: 5, stepNumber: 5, name: 'Inventario', status: StepStatus.PENDING, completedAt: null },
    { id: 6, stepNumber: 6, name: 'Cierre', status: StepStatus.PENDING, completedAt: null }
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [JournalStepperComponent],
      providers: [provideRouter([])]
    }).compileComponents();

    fixture = TestBed.createComponent(JournalStepperComponent);
    component = fixture.componentInstance;
    component.steps = mockSteps;
    fixture.detectChanges();
  });

  it('should create the stepper component and receive 6 steps', () => {
    expect(component).toBeTruthy();
    expect(component.steps.length).toBe(6);
  });

  it('should return correct CSS classes depending on step status', () => {
    expect(component.getStepClass(StepStatus.COMPLETED, 1)).toBe('stepper-completed');
    expect(component.getStepClass(StepStatus.PENDING, 3)).toBe('stepper-pending');
  });

  it('should return correct display names for each step number', () => {
    expect(component.getStepName(1)).toBe('Personal');
    expect(component.getStepName(2)).toBe('Vendedores');
    expect(component.getStepName(3)).toBe('Electrónicos');
    expect(component.getStepName(4)).toBe('Caja');
    expect(component.getStepName(5)).toBe('Inventario');
    expect(component.getStepName(6)).toBe('Cierre');
    expect(component.getStepName(7)).toBe('Paso 7');
  });

  it('should return correct relative route for navigation between child step views', () => {
    expect(component.getRouteForStep(1)).toBe('personal');
    expect(component.getRouteForStep(2)).toBe('vendedores');
    expect(component.getRouteForStep(3)).toBe('electronicos');
    expect(component.getRouteForStep(4)).toBe('caja');
    expect(component.getRouteForStep(5)).toBe('inventario');
    expect(component.getRouteForStep(6)).toBe('cierre');
    expect(component.getRouteForStep(99)).toBe('');
  });

  it('should render 6 navigation step links in the template', () => {
    const stepLinks = fixture.nativeElement.querySelectorAll('.stepper li');
    expect(stepLinks.length).toBe(6);
  });
});

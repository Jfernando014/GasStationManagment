import { Component, ElementRef, computed, inject, input, signal, viewChild } from '@angular/core';
import { FormControl } from '@angular/forms';

import { getControlErrorMessage } from '../../../shared/helpers/form-control.helpers';

/** Option of a select field. */
export interface SelectOption<T> {
  value: T;
  label: string;
}

/**
 * Label + dropdown + error text (or hint) bound to a reactive form control.
 * The options open in a floating menu with the app style (the native select list cannot be styled)
 * and can be used with the keyboard: arrows, Home / End, Enter or Space to choose, Escape or Tab to close.
 * The placeholder is shown when the value is null; it is also an option (e.g. "Todos") unless the field is required.
 */
@Component({
  selector: 'app-select-field',
  standalone: true,
  templateUrl: './select-field.component.html',
  styleUrl: './select-field.component.css',
  host: {
    class: 'form-item',
    '(document:pointerdown)': 'onDocumentPointerDown($event)'
  }
})
export class SelectFieldComponent<T> {
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly trigger = viewChild.required<ElementRef<HTMLButtonElement>>('trigger');

  readonly fieldId = input.required<string>();
  readonly label = input.required<string>();
  readonly control = input.required<FormControl<T | null>>();
  readonly options = input.required<SelectOption<T>[]>();
  readonly placeholder = input('');
  readonly required = input(false);
  readonly labelHidden = input(false);
  readonly hint = input('');

  readonly open = signal(false);
  readonly activeIndex = signal(-1);

  /** Options shown in the menu: the placeholder first when it can be chosen. */
  readonly items = computed<SelectOption<T | null>[]>(() =>
    this.placeholder() && !this.required()
      ? [{ value: null, label: this.placeholder() }, ...this.options()]
      : this.options()
  );

  errorMessage(): string | null {
    return getControlErrorMessage(this.control());
  }

  selectedLabel(): string {
    const value = this.control().value;
    return this.options().find(option => option.value === value)?.label ?? this.placeholder();
  }

  /** True when nothing is chosen in a required field (the placeholder is only a prompt). */
  showsPrompt(): boolean {
    return this.required() && this.control().value === null;
  }

  isSelected(option: SelectOption<T | null>): boolean {
    return option.value === this.control().value;
  }

  toggle(): void {
    if (this.open()) {
      this.close();
    } else {
      this.openMenu();
    }
  }

  choose(option: SelectOption<T | null>): void {
    const control = this.control();
    if (control.value !== option.value) {
      control.setValue(option.value);
      control.markAsDirty();
    }
    this.close();
    this.trigger().nativeElement.focus();
  }

  onKeydown(event: KeyboardEvent): void {
    const count = this.items().length;
    switch (event.key) {
      case 'ArrowDown':
      case 'ArrowUp':
        event.preventDefault();
        if (!this.open()) {
          this.openMenu();
        } else if (count > 0) {
          const step = event.key === 'ArrowDown' ? 1 : -1;
          this.setActive((this.activeIndex() + step + count) % count);
        }
        break;
      case 'Home':
      case 'End':
        if (this.open() && count > 0) {
          event.preventDefault();
          this.setActive(event.key === 'Home' ? 0 : count - 1);
        }
        break;
      case 'Enter':
      case ' ':
        event.preventDefault();
        if (!this.open()) {
          this.openMenu();
        } else {
          const option = this.items()[this.activeIndex()];
          if (option) {
            this.choose(option);
          }
        }
        break;
      case 'Escape':
        if (this.open()) {
          // Only the menu closes, not a modal that contains the field
          event.preventDefault();
          event.stopPropagation();
          this.close();
        }
        break;
      case 'Tab':
        this.close();
        break;
    }
  }

  onDocumentPointerDown(event: PointerEvent): void {
    if (this.open() && !this.host.nativeElement.contains(event.target as Node)) {
      this.close();
    }
  }

  private openMenu(): void {
    if (this.control().disabled) {
      return;
    }
    const selected = this.items().findIndex(option => this.isSelected(option));
    this.activeIndex.set(Math.max(selected, 0));
    this.open.set(true);
  }

  /** Moves the keyboard highlight and scrolls the option into view when the menu is long. */
  private setActive(index: number): void {
    this.activeIndex.set(index);
    this.host.nativeElement
      .querySelector(`#${CSS.escape(this.fieldId())}-option-${index}`)
      ?.scrollIntoView({ block: 'nearest' });
  }

  private close(): void {
    if (!this.open()) {
      return;
    }
    this.open.set(false);
    this.control().markAsTouched();
  }
}

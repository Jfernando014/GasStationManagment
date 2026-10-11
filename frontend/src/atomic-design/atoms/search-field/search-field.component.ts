import { Component, input, output } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subject, debounceTime, distinctUntilChanged } from 'rxjs';

const SEARCH_DELAY_MS = 300;

/** Search box that emits the typed text after the user stops typing for a moment. */
@Component({
  selector: 'app-search-field',
  standalone: true,
  template: `
    <label class="jr-sr" [for]="fieldId()">{{ label() }}</label>
    <input
      type="search"
      class="flat-input"
      [id]="fieldId()"
      [placeholder]="placeholder()"
      (input)="onInput($event)">
  `,
  styles: `
    input::-webkit-search-cancel-button {
      cursor: pointer;
    }
  `
})
export class SearchFieldComponent {
  readonly fieldId = input.required<string>();
  readonly label = input.required<string>();
  readonly placeholder = input('');
  readonly searchChange = output<string>();

  private readonly typed = new Subject<string>();

  constructor() {
    this.typed
      .pipe(
        debounceTime(SEARCH_DELAY_MS),
        distinctUntilChanged(),
        takeUntilDestroyed()
      )
      .subscribe(text => this.searchChange.emit(text));
  }

  onInput(event: Event): void {
    this.typed.next((event.target as HTMLInputElement).value);
  }
}

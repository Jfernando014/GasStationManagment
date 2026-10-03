import { Component, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

/**
 * Example Atomic Design - Molecule: Search Bar Component
 * Combines input and button atoms.
 */
@Component({
  selector: 'app-search-bar-molecule-example',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: 
    <div class="search-bar">
      <input type="text" [(ngModel)]="searchTerm" placeholder="Search..." />
      <button (click)="submitSearch()">Search</button>
    </div>
  
})
export class SearchBarMoleculeExampleComponent {
  searchTerm = '';
  @Output() searchSubmitted = new EventEmitter<string>();

  submitSearch(): void {
    this.searchSubmitted.emit(this.searchTerm);
  }
}

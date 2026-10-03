import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

/**
 * Example Feature Component: Inventory
 */
@Component({
  selector: 'app-inventory-example',
  standalone: true,
  imports: [CommonModule],
  template: 
    <section class="inventory-container">
      <h3>Inventory Feature</h3>
      <p>Placeholder component for inventory feature module.</p>
    </section>
  
})
export class InventoryComponentExample {
  featureName = 'inventory';
}

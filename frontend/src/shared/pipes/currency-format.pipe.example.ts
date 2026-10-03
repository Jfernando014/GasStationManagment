import { Pipe, PipeTransform } from '@angular/core';

/**
 * Example Shared Pipe: Formats numeric amount to currency string.
 */
@Pipe({
  name: 'currencyFormatExample',
  standalone: true
})
export class CurrencyFormatPipeExample implements PipeTransform {
  transform(amount: number, currencyCode = 'USD'): string {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: currencyCode
    }).format(amount);
  }
}

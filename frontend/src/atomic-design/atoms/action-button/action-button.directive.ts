import { Directive, input } from '@angular/core';

/**
 * Botón de acción de la malla y del personal del día. Se aplica sobre un `<button>` o `<a>` nativo para no perder
 * su accesibilidad: `<button appButton variant="primary">`.
 * primary: la acción principal de la pantalla · secondary: acciones de apoyo · ghost: cancelar o cerrar ·
 * danger: deshacer algo guardado.
 */
@Directive({
  selector: 'button[appButton], a[appButton]',
  standalone: true,
  host: {
    '[class]': "'ab ab--' + variant() + ' ab--' + size()",
    '[class.ab--block]': 'block()'
  }
})
export class ActionButtonDirective {
  readonly variant = input<'primary' | 'secondary' | 'ghost' | 'danger'>('secondary');
  readonly size = input<'md' | 'sm'>('md');
  /** Ocupa todo el ancho disponible (pie de página en celular). */
  readonly block = input(false);
}

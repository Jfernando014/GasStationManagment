import { ApiError, DEFAULT_ERROR_MESSAGE } from '../../../shared/models/api-error.model';
import { toActionError, toLoadError } from './overtime-errors';

describe('overtime-errors', () => {
  it('maps read errors into NOT_FOUND or ERROR with session handling', () => {
    expect(toLoadError(new ApiError(404, { detail: 'No existe el trabajador' }))).toEqual({
      outcome: 'NOT_FOUND',
      message: 'No existe el trabajador'
    });

    expect(toLoadError(new ApiError(400, { detail: 'Identificador inválido' }), true)).toEqual({
      outcome: 'NOT_FOUND',
      message: 'Identificador inválido'
    });

    expect(toLoadError(new ApiError(400, { detail: 'Identificador inválido' }), false)).toEqual({
      outcome: 'ERROR',
      message: 'Identificador inválido'
    });

    expect(toLoadError(new ApiError(401, { detail: 'Unauthorized' }))).toEqual({
      outcome: 'ERROR',
      message: 'Tu sesión ya no es válida. Inicia sesión de nuevo'
    });
    expect(toLoadError(new ApiError(403, { detail: 'Forbidden' }))).toEqual({
      outcome: 'ERROR',
      message: 'Tu sesión ya no es válida. Inicia sesión de nuevo'
    });

    expect(toLoadError(new Error('boom'))).toEqual({
      outcome: 'ERROR',
      message: DEFAULT_ERROR_MESSAGE
    });
  });

  it('maps action errors by 403, field validation errors and backend code', () => {
    const fieldByCode = { INVALID_PARAMETER_REASON: 'reason', PARAMETER_VERSION_EXISTS: 'validFrom' };

    expect(toActionError(new ApiError(403, {}), fieldByCode, 'Sin permisos')).toEqual({
      outcome: 'ERROR',
      message: 'Sin permisos',
      field: null
    });

    expect(toActionError(new ApiError(400, {
      detail: 'Validación',
      errors: [{ field: 'hours', message: 'Debe ser positivo' }]
    }), fieldByCode, 'Sin permisos')).toEqual({
      outcome: 'ERROR',
      message: 'Debe ser positivo',
      field: 'hours'
    });

    expect(toActionError(new ApiError(409, {
      detail: 'Ya existe versión para esa fecha',
      code: 'PARAMETER_VERSION_EXISTS'
    }), fieldByCode, 'Sin permisos')).toEqual({
      outcome: 'ERROR',
      message: 'Ya existe versión para esa fecha',
      field: 'validFrom'
    });

    expect(toActionError(new ApiError(500, { detail: 'Fallo interno', code: 'UNMAPPED' }), fieldByCode, 'Sin permisos'))
      .toEqual({ outcome: 'ERROR', message: 'Fallo interno', field: null });
  });
});

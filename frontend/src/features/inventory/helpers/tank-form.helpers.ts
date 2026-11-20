import { getErrorMessage, getFieldErrors, isApiError } from '../../../shared/helpers/problem-details.helpers';
import { FUEL_LABELS } from '../../../shared/helpers/inventory-display.helpers';
import { FuelType, TankRequest, TankResponse } from '../../../shared/models/dtos/tank.dto';

export const FUEL_OPTIONS: readonly string[] = Object.values(FUEL_LABELS);

export type TankField = 'code' | 'name' | 'fuel' | 'capacity' | 'maxHeight' | 'tolerance' | 'gaugeTableId';

export interface TankFormModel {
  id: string;
  backendId?: number;
  name: string;
  code: string;
  fuel: string;
  fuelType: FuelType;
  capacity: number;
  maxHeight: number;
  tolerance: number;
  gaugeTableId: number | null;
}

export function createEmptyTankForm(): TankFormModel {
  return {
    id: '',
    code: '',
    name: '',
    fuel: '',
    fuelType: 'MOTOR',
    capacity: 0,
    maxHeight: 0,
    tolerance: 0,
    gaugeTableId: null
  };
}

export function fromTankResponse(tank: TankResponse): TankFormModel {
  return {
    id: String(tank.id),
    backendId: tank.id,
    code: tank.code,
    name: tank.name,
    fuel: FUEL_LABELS[tank.fuelType] ?? 'Gasolina Corriente',
    fuelType: tank.fuelType,
    capacity: tank.maxCapacityGallons,
    maxHeight: tank.maxHeightCm,
    tolerance: tank.toleranceCm,
    gaugeTableId: tank.gaugeTableId ?? null
  };
}

export function toFuelType(fuel: string): FuelType {
  const entry = (Object.entries(FUEL_LABELS) as [FuelType, string][]).find(([, label]) => label === fuel);
  return entry ? entry[0] : 'MOTOR';
}

export function toTankRequest(tank: TankFormModel): TankRequest {
  return {
    code: tank.code.trim(),
    name: tank.name.trim(),
    fuelType: tank.fuelType,
    maxCapacityGallons: tank.capacity,
    maxHeightCm: tank.maxHeight,
    toleranceCm: tank.tolerance,
    gaugeTableId: tank.gaugeTableId && tank.gaugeTableId > 0 ? tank.gaugeTableId : null
  };
}

export function isTankFieldInvalid(field: TankField, tank: TankFormModel): boolean {
  if (field === 'gaugeTableId') return false;
  if (field === 'code' || field === 'name' || field === 'fuel') return !String(tank[field]).trim();
  if (field === 'capacity' || field === 'maxHeight') return Number(tank[field]) <= 0;
  return Number(tank[field]) < 0;
}

export function isTankFormValid(tank: TankFormModel): boolean {
  return Boolean(tank.code.trim() && tank.name.trim() && tank.fuel) &&
    tank.capacity > 0 &&
    tank.maxHeight > 0 &&
    tank.tolerance >= 0;
}

export function mapServerFieldErrors(error: unknown, fallbackField: string): Record<string, string> {
  const mapped = getFieldErrors(error).reduce<Record<string, string>>((result, fieldError) => {
    result[fieldError.field] = fieldError.message;
    return result;
  }, {});
  if (Object.keys(mapped).length === 0) {
    const message = isApiError(error) && error.status === 409
      ? (fallbackField === 'code'
        ? 'El código ya se encuentra registrado.'
        : 'El nombre ya se encuentra registrado.')
      : getErrorMessage(error);
    mapped[fallbackField] = message;
  }
  return mapped;
}

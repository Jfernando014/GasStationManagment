import { FuelType } from './tank.dto';

export type ColorCategory = 'FUEL' | 'SHIFT' | 'WORKER';

export type ShiftColorKey = 'DIA' | 'DIA6' | 'NUEVE_SEIS' | 'NOCHE' | 'APOYO' | 'DESCANSO';

export interface ResolvedColorEntryDto {
  key: string;
  label: string;
  colorHex: string;
  defaultHex: string;
  customized: boolean;
}

export interface ColorPresetDto {
  name: string;
  hex: string;
}

export interface ColorThemeDto {
  fuels: ResolvedColorEntryDto[];
  shifts: ResolvedColorEntryDto[];
  workers: Record<string, string>;
  palette: ColorPresetDto[];
}

export interface ColorUpdateItemRequestDto {
  category: ColorCategory;
  entityKey: string;
  colorHex: string | null;
}

export interface UpdateColorThemeRequestDto {
  items: ColorUpdateItemRequestDto[];
}

export type FuelColorKey = FuelType;

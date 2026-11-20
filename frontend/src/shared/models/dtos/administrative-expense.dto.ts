export interface AdministrativeExpenseDto {
  id: number;
  date: string;
  amount: number;
  name: string | null;
  documentNumber: string | null;
  description: string;
  createdAt: string;
}

export interface AdministrativeExpenseListDto {
  expenses: AdministrativeExpenseDto[];
  total: number;
}

export interface AdministrativeExpenseRequestDto {
  date: string;
  amount: number;
  name?: string | null;
  documentNumber?: string | null;
  description: string;
}

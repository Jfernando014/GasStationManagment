export enum JournalStatus {
    OPEN = 'OPEN',
    CLOSED = 'CLOSED'
}

export enum StepStatus {
    PENDING = 'PENDING',
    IN_PROGRESS = 'IN_PROGRESS',
    COMPLETED = 'COMPLETED'
}

export interface JournalStepResponse {
    id: number;
    stepNumber: number;
    name: string;
    status: StepStatus;
    completedAt: string | null;
}

export interface JournalResponse {
    id: number;
    date: string;
    consecutive: number;
    status: JournalStatus;
    steps: JournalStepResponse[];
}

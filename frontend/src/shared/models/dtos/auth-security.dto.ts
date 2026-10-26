export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
  confirmPassword: string;
}

export interface ChangePasswordResponse {
  message: string;
}

export interface AccountLockedResponse {
  error: string;
  message: string;
  minutesRemaining: number;
  lockedUntil: string;
}

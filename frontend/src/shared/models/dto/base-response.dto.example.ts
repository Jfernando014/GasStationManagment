/**
 * Example Shared DTO: Standard API response wrapper
 */
export interface BaseResponseDtoExample<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

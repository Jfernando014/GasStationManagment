/** Optional filters for the worker list. Empty values are not sent. */
export interface WorkerFilters {
  active?: boolean | null;
  roleId?: string | null;
  /** Text contained in the name or in the document. */
  search?: string | null;
}

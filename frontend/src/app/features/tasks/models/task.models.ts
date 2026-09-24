// ── Task Models ───────────────────────────────────────────────────────────────
// Canonical interfaces for the /api/v1/tasks endpoints.
// The backend returns `completed` (boolean); the service normalises it to
// `isCompleted` so the rest of the UI only ever reads one field.

export interface TaskData {
  id: number;
  title: string;
  /** Canonical completion flag – always populated after service normalisation. */
  isCompleted: boolean;
  createdAt?: string;
  updatedAt?: string;
}

// ── Request DTOs ──────────────────────────────────────────────────────────────

export interface CreateTaskRequest {
  title: string;
  isCompleted: boolean;
}

export interface UpdateTaskRequest {
  title?: string;
  isCompleted?: boolean;
}

// ── Pageable response ─────────────────────────────────────────────────────────

export interface PageableTaskResponse {
  content: TaskData[];
  totalPages?: number;
  totalElements?: number;
  numberOfElements?: number;
  size?: number;
  number?: number;
  first?: boolean;
  last?: boolean;
}

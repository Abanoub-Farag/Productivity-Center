// ── Enums ─────────────────────────────────────────────────────────────────────

export type RoomVisibility = 'PUBLIC' | 'PRIVATE';

// ── Core Room model ───────────────────────────────────────────────────────────

/** Full room entity returned by the API. */
export interface RoomData {
  id: number;
  title: string;
  description: string;
  ownerId?: number;
  visibility?: RoomVisibility;
  tags?: string[];
  status?: string;
  count?: number;
  countType?: string;
  actionType?: string;
}

/** Pageable wrapper for room lists. */
export interface PageableResponse {
  content: RoomData[];
  totalPages?: number;
  totalElements?: number;
  numberOfElements?: number;
  first?: boolean;
  last?: boolean;
  size?: number;
  number?: number;
}

// ── Favorites ─────────────────────────────────────────────────────────────────

export interface FavoriteRoomItem {
  roomId: number;
  title: string;
  description: string;
  addedAt: string;
  visibility?: RoomVisibility;
}

export interface FavoritePageResponse {
  content: FavoriteRoomItem[];
  numberOfElements: number;
  first: boolean;
  last: boolean;
  size: number;
  number?: number;
  totalPages?: number;
  totalElements?: number;
}

// ── DTOs ──────────────────────────────────────────────────────────────────────

export interface CreateRoomDto {
  title: string;
  description: string;
  visibility: RoomVisibility;
}

export interface UpdateRoomDto {
  title: string;
  description: string;
  visibility: RoomVisibility;
}

// ── View model (RoomCardComponent) ────────────────────────────────────────────

/** Flattened presentational model consumed by RoomCardComponent. */
export interface Room {
  id: string;
  title: string;
  description: string;
  tags: string[];
  actionType: 'join' | 'view';
  visibility?: RoomVisibility;
  isFavorite?: boolean;
  addedAt?: string;
  isPendingFavorite?: boolean;
  ownerId?: number;
}

// ── Tasks ─────────────────────────────────────────────────────────────────────

export interface TaskData {
  id: number;
  title: string;
  isCompleted: boolean;
  completed?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface UpdateTaskRequest {
  title?: string;
  isCompleted?: boolean;
}

export interface PageableTaskResponse {
  content: TaskData[];
  totalPages?: number;
  totalElements?: number;
  first?: boolean;
  last?: boolean;
}

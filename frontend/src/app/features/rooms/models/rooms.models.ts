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
// Favorite types are owned by favorite-room.models.ts; re-exported here so
// that existing imports within the rooms module continue to resolve.

export type {
  FavoriteRoomItem,
  FavoritePageResponse,
  AddFavoriteResponse,
} from './favorite-room.models';

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
// Task types are owned by the tasks feature module; re-exported here so that
// existing imports within the rooms module continue to resolve without changes.

export type { TaskData, UpdateTaskRequest, PageableTaskResponse } from '../../tasks/models/task.models';

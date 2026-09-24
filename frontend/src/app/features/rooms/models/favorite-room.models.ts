// ── Favorite Room Models ──────────────────────────────────────────────────────
// Canonical interfaces for the /api/v1/rooms/favorites endpoints.

/** Single item returned by GET /api/v1/rooms/favorites (content array). */
export interface FavoriteRoomItem {
  roomId: number;
  title: string;
  description: string;
  addedAt: string;
}

/** Full item returned by POST /api/v1/rooms/favorites/{roomId}. */
export type AddFavoriteResponse = FavoriteRoomItem;

/** Pageable wrapper for the favorites list. */
export interface FavoritePageResponse {
  content: FavoriteRoomItem[];
  numberOfElements?: number;
  size?: number;
  number?: number;
  totalPages?: number;
  totalElements?: number;
  first?: boolean;
  last?: boolean;
}

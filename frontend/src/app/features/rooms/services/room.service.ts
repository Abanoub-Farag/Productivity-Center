/**
 * @deprecated This file is retained as a compatibility shim.
 * Import from `rooms-data.service.ts`, `favorite-room.service.ts`,
 * or `../models/rooms.models.ts` directly.
 */
export { RoomsDataService as RoomService } from './rooms-data.service';
export { FavoriteRoomService } from './favorite-room.service';
export type { ApiResponse } from '../../../core/models/auth.models';
export type {
  RoomData,
  RoomVisibility,
  CreateRoomDto,
  UpdateRoomDto,
  PageableResponse,
  FavoriteRoomItem,
  FavoritePageResponse,
  AddFavoriteResponse,
} from '../models/rooms.models';

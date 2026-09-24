import { Injectable, inject, computed } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { AuthService } from '../../../core/services/auth.service';
import { ApiResponse, getUserRoomId } from '../../../core/models/auth.models';
import {
  RoomData,
  PageableResponse,
  CreateRoomDto,
  UpdateRoomDto,
} from '../models/rooms.models';

/**
 * HTTP service for the /api/v1/rooms resource (CRUD + membership).
 *
 * Auth headers are injected globally by `authInterceptor`.
 * The `ngrok-skip-browser-warning` header is injected globally by `ngrokInterceptor`.
 *
 * AuthService is injected only to derive the `userRoomId` computed signal
 * (pure read of auth state — no HTTP coupling).
 *
 * For favorites operations use `FavoriteRoomService`.
 */
@Injectable({ providedIn: 'root' })
export class RoomsDataService {
  private readonly http = inject(HttpClient);
  private readonly authService = inject(AuthService);
  private readonly baseUrl = `${environment.apiUrl}/api/v1/rooms`;

  /** Derived from auth state — which room the current user owns/is in. */
  readonly userRoomId = computed<number | null>(() =>
    getUserRoomId(this.authService.currentUser()),
  );

  // ── Private helpers ────────────────────────────────────────────────────────

  private toNumericId(id: number | string): number | null {
    const n = typeof id === 'number' ? id : parseInt(String(id), 10);
    return Number.isInteger(n) && n > 0 ? n : null;
  }

  private invalidIdError(): Observable<never> {
    return throwError(() => new Error('Invalid room ID: must be a positive integer.'));
  }

  // ── Rooms ──────────────────────────────────────────────────────────────────

  getRooms(page = 0, size = 20): Observable<ApiResponse<PageableResponse>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<ApiResponse<PageableResponse>>(this.baseUrl, { params });
  }

  getRoomById(id: number | string): Observable<ApiResponse<RoomData>> {
    const numericId = this.toNumericId(id);
    if (!numericId) return this.invalidIdError();

    return this.http.get<ApiResponse<RoomData>>(`${this.baseUrl}/${numericId}`);
  }

  createRoom(dto: CreateRoomDto): Observable<ApiResponse<RoomData>> {
    return this.http.post<ApiResponse<RoomData>>(this.baseUrl, dto);
  }

  updateRoom(roomId: number | string, dto: UpdateRoomDto): Observable<ApiResponse<RoomData>> {
    const numericId = this.toNumericId(roomId);
    if (!numericId) return this.invalidIdError();

    return this.http.put<ApiResponse<RoomData>>(`${this.baseUrl}/${numericId}`, dto);
  }

  deleteRoom(roomId: number | string): Observable<ApiResponse<null>> {
    const numericId = this.toNumericId(roomId);
    if (!numericId) return this.invalidIdError();

    return this.http.delete<ApiResponse<null>>(`${this.baseUrl}/${numericId}`);
  }

  // ── Membership ─────────────────────────────────────────────────────────────

  joinRoom(roomId: number | string): Observable<ApiResponse<null>> {
    const numericId = this.toNumericId(roomId);
    if (!numericId) return this.invalidIdError();

    return this.http.post<ApiResponse<null>>(`${this.baseUrl}/${numericId}/join`, {});
  }

  sendHeartbeat(roomId: number | string): Observable<ApiResponse<null>> {
    const numericId = this.toNumericId(roomId);
    if (!numericId) return this.invalidIdError();

    return this.http.post<ApiResponse<null>>(`${this.baseUrl}/${numericId}/heartbeat`, {});
  }
}

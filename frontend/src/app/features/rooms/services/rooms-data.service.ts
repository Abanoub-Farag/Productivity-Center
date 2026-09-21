import { Injectable, inject, computed } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { AuthService } from '../../../core/services/auth.service';
import { ApiResponse, getUserRoomId } from '../../../core/models/auth.models';
import {
  RoomData,
  PageableResponse,
  FavoritePageResponse,
  CreateRoomDto,
  UpdateRoomDto,
} from '../models/rooms.models';

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

  private buildHeaders(includeContentType = false): HttpHeaders {
    let headers = new HttpHeaders({ 'ngrok-skip-browser-warning': 'true' });
    const token = this.authService.getToken();
    if (token) {
      headers = headers.set('Authorization', `Bearer ${token}`);
    }
    if (includeContentType) {
      headers = headers.set('Content-Type', 'application/json');
    }
    return headers;
  }

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

    return this.http.get<ApiResponse<PageableResponse>>(this.baseUrl, {
      headers: this.buildHeaders(),
      params,
    });
  }

  getRoomById(id: number | string): Observable<ApiResponse<RoomData>> {
    const numericId = this.toNumericId(id);
    if (!numericId) return this.invalidIdError();

    return this.http.get<ApiResponse<RoomData>>(`${this.baseUrl}/${numericId}`, {
      headers: this.buildHeaders(),
    });
  }

  createRoom(dto: CreateRoomDto): Observable<ApiResponse<RoomData>> {
    return this.http.post<ApiResponse<RoomData>>(this.baseUrl, dto, {
      headers: this.buildHeaders(true),
    });
  }

  updateRoom(roomId: number | string, dto: UpdateRoomDto): Observable<ApiResponse<RoomData>> {
    const numericId = this.toNumericId(roomId);
    if (!numericId) return this.invalidIdError();

    return this.http.put<ApiResponse<RoomData>>(`${this.baseUrl}/${numericId}`, dto, {
      headers: this.buildHeaders(true),
    });
  }

  deleteRoom(roomId: number | string): Observable<ApiResponse<null>> {
    const numericId = this.toNumericId(roomId);
    if (!numericId) return this.invalidIdError();

    return this.http.delete<ApiResponse<null>>(`${this.baseUrl}/${numericId}`, {
      headers: this.buildHeaders(),
    });
  }

  // ── Membership ─────────────────────────────────────────────────────────────

  joinRoom(roomId: number | string): Observable<ApiResponse<null>> {
    const numericId = this.toNumericId(roomId);
    if (!numericId) return this.invalidIdError();

    return this.http.post<ApiResponse<null>>(
      `${this.baseUrl}/${numericId}/join`,
      {},
      { headers: this.buildHeaders(true) },
    );
  }

  sendHeartbeat(roomId: number | string): Observable<ApiResponse<null>> {
    const numericId = this.toNumericId(roomId);
    if (!numericId) return this.invalidIdError();

    return this.http.post<ApiResponse<null>>(
      `${this.baseUrl}/${numericId}/heartbeat`,
      {},
      { headers: this.buildHeaders(true) },
    );
  }

  // ── Favorites ──────────────────────────────────────────────────────────────

  getFavorites(page = 0, size = 20, sort?: string): Observable<ApiResponse<FavoritePageResponse>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    if (sort) params = params.set('sort', sort);

    return this.http.get<ApiResponse<FavoritePageResponse>>(`${this.baseUrl}/favorites`, {
      headers: this.buildHeaders(),
      params,
    });
  }

  addToFavorites(roomId: number | string): Observable<ApiResponse<null>> {
    const numericId = this.toNumericId(roomId);
    if (!numericId) return this.invalidIdError();

    return this.http.post<ApiResponse<null>>(
      `${this.baseUrl}/favorites/${numericId}`,
      {},
      { headers: this.buildHeaders(true) },
    );
  }

  removeFromFavorites(roomId: number | string): Observable<ApiResponse<null>> {
    const numericId = this.toNumericId(roomId);
    if (!numericId) return this.invalidIdError();

    return this.http.delete<ApiResponse<null>>(`${this.baseUrl}/favorites/${numericId}`, {
      headers: this.buildHeaders(),
    });
  }
}

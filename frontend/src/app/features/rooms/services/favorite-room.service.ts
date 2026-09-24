import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { ApiResponse } from '../../../core/models/auth.models';
import {
  AddFavoriteResponse,
  FavoritePageResponse,
} from '../models/favorite-room.models';

/**
 * HTTP service for the /api/v1/rooms/favorites resource.
 *
 * Auth headers are injected globally by `authInterceptor`.
 * The `ngrok-skip-browser-warning` header is injected globally by `ngrokInterceptor`.
 * This service carries no infrastructure dependencies.
 */
@Injectable({ providedIn: 'root' })
export class FavoriteRoomService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/v1/rooms/favorites`;

  /**
   * GET /api/v1/rooms/favorites
   * Returns a paginated list of the current user's favorite rooms.
   */
  getFavorites(page = 0, size = 20, sort?: string): Observable<ApiResponse<FavoritePageResponse>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    if (sort) params = params.set('sort', sort);

    return this.http.get<ApiResponse<FavoritePageResponse>>(this.baseUrl, { params });
  }

  /**
   * POST /api/v1/rooms/favorites/{roomId}
   * Adds a room to the current user's favorites.
   * Returns the created favorite item.
   */
  addFavorite(roomId: number): Observable<ApiResponse<AddFavoriteResponse>> {
    return this.http.post<ApiResponse<AddFavoriteResponse>>(
      `${this.baseUrl}/${roomId}`,
      {},
    );
  }

  /**
   * DELETE /api/v1/rooms/favorites/{roomId}
   * Removes a room from the current user's favorites.
   */
  removeFavorite(roomId: number): Observable<ApiResponse<null>> {
    return this.http.delete<ApiResponse<null>>(`${this.baseUrl}/${roomId}`);
  }
}

import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map, catchError, throwError } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { ApiResponse, RoomMember } from '../../../core/models/room-member.model';

/**
 * HTTP service for /api/v1/rooms/{roomId}/members.
 *
 * Auth headers are injected globally by `authInterceptor`.
 * The `ngrok-skip-browser-warning` header is injected globally by `ngrokInterceptor`.
 */
@Injectable({ providedIn: 'root' })
export class RoomMemberService {
  private readonly http = inject(HttpClient);

  getRoomMembers(roomId: number | string): Observable<RoomMember[]> {
    const url = `${environment.apiUrl}/api/v1/rooms/${roomId}/members`;

    return this.http
      .get<ApiResponse<RoomMember[]>>(url)
      .pipe(
        map((response) => response.data ?? []),
        catchError((error) => {
          const message =
            error?.error?.message ||
            error?.message ||
            'Failed to load room members. Please try again.';
          return throwError(() => new Error(message));
        }),
      );
  }
}

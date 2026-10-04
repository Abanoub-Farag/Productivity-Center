import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { ApiResponse } from '../../../core/models/auth.models';
import { TimerSessionDto } from '../models/timer.models';

/**
 * Thin HTTP service for the timer-sessions resource.
 *
 * Auth headers are injected globally by `authInterceptor`.
 * The `ngrok-skip-browser-warning` header is injected globally by `ngrokInterceptor`.
 *
 * This service is stateless — all state lives in RoomDetailFacade.
 */
@Injectable({ providedIn: 'root' })
export class TimerDataService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/v1`;

  // ── Start ──────────────────────────────────────────────────────────────────

  /** POST /api/v1/rooms/{roomId}/timer-sessions */
  startTimer(roomId: number): Observable<ApiResponse<TimerSessionDto>> {
    return this.http.post<ApiResponse<TimerSessionDto>>(
      `${this.baseUrl}/rooms/${roomId}/timer-sessions`,
      {},
    );
  }

  // ── Complete ───────────────────────────────────────────────────────────────

  /** PATCH /api/v1/timer-sessions/{sessionId} */
  completeTimer(sessionId: number): Observable<ApiResponse<TimerSessionDto>> {
    return this.http.patch<ApiResponse<TimerSessionDto>>(
      `${this.baseUrl}/timer-sessions/${sessionId}`,
      {},
    );
  }

  // ── Get one ────────────────────────────────────────────────────────────────

  /** GET /api/v1/timer-sessions/{sessionId} — used for page-reload recovery. */
  getSession(sessionId: number): Observable<ApiResponse<TimerSessionDto>> {
    return this.http.get<ApiResponse<TimerSessionDto>>(
      `${this.baseUrl}/timer-sessions/${sessionId}`,
    );
  }
}

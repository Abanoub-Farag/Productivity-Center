import { Component, input, inject, signal, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Subject, of } from 'rxjs';
import { exhaustMap, catchError, tap, filter, map } from 'rxjs/operators';
import { RoomService, ApiResponse } from '../../services/room.service';

export type RequestState = 'idle' | 'loading' | 'success' | 'error';

const STATUS_MESSAGES: Record<number, string> = {
  400: 'Bad Request: The provided room ID or data is invalid.',
  401: 'Unauthorized: Please log in to join this room.',
  403: 'Forbidden: You do not have permission to join this room.',
  404: 'Room Not Found: The requested room does not exist.',
  409: 'Conflict: You are already a member of this room.',
  500: 'Server Error: An internal server error occurred while joining.',
  503: 'Service Unavailable: Room server is currently unavailable.',
  504: 'Gateway Timeout: The server timed out processing your request.',
};

@Component({
  selector: 'app-room-join',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './room-join.component.html',
  styles: [`
    .join-container {
      display: flex;
      flex-direction: column;
      gap: 12px;
      max-width: 480px;
      margin: 16px 0;
      font-family: inherit;
    }
    .input-group {
      display: flex;
      gap: 8px;
      align-items: center;
    }
    .room-input {
      flex: 1;
      padding: 10px 14px;
      border-radius: 8px;
      border: 1px solid #2D2B52;
      background: #121124;
      color: #E2E8F0;
      font-size: 0.95rem;
      outline: none;
      transition: border-color 0.2s;
    }
    .room-input:focus {
      border-color: #6366F1;
    }
    .join-btn {
      padding: 10px 20px;
      cursor: pointer;
      border-radius: 8px;
      border: 1px solid #4F46E5;
      background: linear-gradient(135deg, #4F46E5 0%, #3730A3 100%);
      color: #FFFFFF;
      font-weight: 600;
      font-size: 0.95rem;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
      transition: all 0.2s ease;
    }
    .join-btn:hover:not(:disabled) {
      background: linear-gradient(135deg, #6366F1 0%, #4338CA 100%);
      box-shadow: 0 4px 12px rgba(79, 70, 229, 0.3);
    }
    .join-btn:disabled {
      opacity: 0.6;
      cursor: not-allowed;
      box-shadow: none;
    }
    .spinner {
      width: 16px;
      height: 16px;
      border: 2px solid rgba(255, 255, 255, 0.3);
      border-top-color: #ffffff;
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
    }
    @keyframes spin {
      to { transform: rotate(360deg); }
    }
    .alert {
      padding: 12px 16px;
      border-radius: 8px;
      font-size: 0.875rem;
      line-height: 1.4;
      display: flex;
      align-items: flex-start;
      gap: 8px;
    }
    .alert-success {
      background: rgba(16, 185, 129, 0.12);
      color: #34D399;
      border: 1px solid rgba(16, 185, 129, 0.25);
    }
    .alert-error {
      background: rgba(239, 68, 68, 0.12);
      color: #F87171;
      border: 1px solid rgba(239, 68, 68, 0.25);
    }
  `]
})
export class RoomJoinComponent {
  private readonly roomService = inject(RoomService);
  private readonly destroyRef = inject(DestroyRef);

  /** Optional: pre-fill the room ID to join. If provided, hides the manual input. */
  readonly roomId = input<number | string | null | undefined>();

  readonly joinState = signal<RequestState>('idle');
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);
  readonly manualRoomId = signal<string>('');

  private readonly joinRequest$ = new Subject<number | string | null | undefined>();

  constructor() {
    this.joinRequest$.pipe(
      map(explicitId => explicitId ?? this.roomId() ?? this.manualRoomId()),
      map(targetId => this.validateRoomId(targetId)),
      tap(validation => {
        if (!validation.valid) {
          this.joinState.set('error');
          this.errorMessage.set(validation.error ?? 'Invalid Room ID.');
          this.successMessage.set(null);
        } else {
          this.joinState.set('loading');
          this.errorMessage.set(null);
          this.successMessage.set(null);
        }
      }),
      filter(validation => validation.valid && validation.parsedId !== undefined),
      exhaustMap(validation =>
        this.roomService.joinRoom(validation.parsedId!).pipe(
          tap((response: ApiResponse<unknown>) => {
            this.joinState.set('success');
            this.successMessage.set(response.message || `Successfully joined room #${validation.parsedId}.`);
          }),
          catchError((err: HttpErrorResponse | Error) => {
            this.joinState.set('error');
            this.errorMessage.set(this.extractErrorMessage(err));
            return of(null);
          }),
        ),
      ),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe();
  }

  joinRoom(explicitId?: number | string | null): void {
    if (this.joinState() === 'loading') return;
    this.joinRequest$.next(explicitId);
  }

  private validateRoomId(id: number | string | null | undefined): { valid: boolean; parsedId?: number; error?: string } {
    if (id == null || String(id).trim() === '') {
      return { valid: false, error: 'Room ID is required.' };
    }

    const trimmedStr = String(id).trim();
    if (!/^\d+$/.test(trimmedStr)) {
      return { valid: false, error: 'Room ID must be a valid positive integer.' };
    }

    const parsedId = Number(trimmedStr);
    if (!Number.isInteger(parsedId) || parsedId <= 0) {
      return { valid: false, error: 'Room ID must be a positive integer greater than 0.' };
    }

    if (parsedId > Number.MAX_SAFE_INTEGER) {
      return { valid: false, error: 'Room ID exceeds maximum allowable numerical limit.' };
    }

    return { valid: true, parsedId };
  }

  private extractErrorMessage(err: HttpErrorResponse | Error | unknown): string {
    if (err instanceof Error && !(err instanceof HttpErrorResponse)) {
      return err.message;
    }

    if (!(err instanceof HttpErrorResponse)) {
      return 'An unexpected error occurred while attempting to join the room.';
    }

    if (err.status === 0 || err.error instanceof ErrorEvent) {
      return 'Network Error: Unable to reach the server. Please check your internet connection.';
    }

    const payload = err.error as { errors?: unknown; message?: string } | null;
    const parsedErrors = this.formatApiErrors(payload?.errors);
    if (parsedErrors) return parsedErrors;

    return payload?.message ?? STATUS_MESSAGES[err.status] ?? `Error (${err.status}): Failed to join room.`;
  }

  private formatApiErrors(apiErrors: unknown): string | null {
    if (!apiErrors) return null;
    if (typeof apiErrors === 'string') return apiErrors;
    if (Array.isArray(apiErrors) && apiErrors.length > 0) return (apiErrors as string[]).join(', ');

    if (typeof apiErrors === 'object') {
      const keys = Object.keys(apiErrors as object);
      if (keys.length === 0) return null;
      return Object.entries(apiErrors as Record<string, unknown>)
        .map(([field, msg]) => Array.isArray(msg) ? `${field}: ${(msg as string[]).join(', ')}` : `${field}: ${msg}`)
        .join('; ');
    }

    return null;
  }
}

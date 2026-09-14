import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError, switchMap, BehaviorSubject, filter, take } from 'rxjs';
import { AuthService } from '../services/auth.service';

let isRefreshing = false;
let refreshTokenSubject = new BehaviorSubject<string | null>(null);

/**
 * Unwraps standard backend ErrorResponse/ApiResponse into a clean JavaScript Error
 * so components downstream can read `error.message` and `error.errors` reliably.
 */
function unwrapError(err: unknown): Error | unknown {
  if (err instanceof HttpErrorResponse && err.error && typeof err.error === 'object') {
    const errorBody = err.error;
    const errorMessage = errorBody.message || err.message || 'An unexpected error occurred';
    const unwrappedErr = new Error(errorMessage);
    (unwrappedErr as any).status = err.status;
    (unwrappedErr as any).errors = errorBody.errors;
    (unwrappedErr as any).originalError = err;
    return unwrappedErr;
  }
  return err;
}

export const authErrorInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  return next(req).pipe(
    catchError((err: unknown) => {
      const isUnauthenticated = err instanceof HttpErrorResponse &&
        err.status === 401 &&
        req.headers.has('Authorization');

      if (!isUnauthenticated) {
        return throwError(() => unwrapError(err));
      }

      if (!isRefreshing) {
        isRefreshing = true;
        refreshTokenSubject.next(null); // Reset subject to block subsequent requests

        return authService.refreshToken().pipe(
          switchMap((token) => {
            isRefreshing = false;
            refreshTokenSubject.next(token);
            
            const authReq = req.clone({
              headers: req.headers.set('Authorization', `Bearer ${token}`)
            });
            return next(authReq);
          }),
          catchError((refreshErr) => {
            isRefreshing = false;
            // Emit an empty string to unblock queued requests and let them fail gracefully
            refreshTokenSubject.next(''); 
            
            authService.clearToken();
            const currentPath = router.url;
            const loginUrl = currentPath && currentPath !== '/login'
              ? `/login?redirect=${encodeURIComponent(currentPath)}`
              : '/login';
            router.navigateByUrl(loginUrl, { replaceUrl: true });
            return throwError(() => unwrapError(refreshErr));
          })
        );
      } else {
        // Wait for the token to be refreshed
        return refreshTokenSubject.pipe(
          filter(token => token !== null), // Wait until it's not null (either success or empty string)
          take(1),
          switchMap(token => {
            if (!token) {
              // The refresh failed, so we abort this queued request
              return throwError(() => new Error('Authentication session expired.'));
            }
            const authReq = req.clone({
              headers: req.headers.set('Authorization', `Bearer ${token}`)
            });
            return next(authReq);
          })
        );
      }
    })
  );
};

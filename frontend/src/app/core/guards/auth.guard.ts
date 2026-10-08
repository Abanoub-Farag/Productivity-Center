import { inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

/**
 * Protects routes that require authentication.
 *
 * Checks both token presence AND local expiry (decoded from the JWT `exp` claim)
 * so that an already-expired token causes an immediate redirect without waiting
 * for a network round-trip to produce a 401 (which the authErrorInterceptor
 * would catch anyway — this just makes it faster and prevents any flicker).
 */
export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const platformId = inject(PLATFORM_ID);

  if (!isPlatformBrowser(platformId)) {
    return true; // Allow SSR to render the page shell
  }

  if (authService.isAuthenticated() && !authService.isTokenExpired()) {
    return true;
  }

  // Clear any stale/expired token before redirecting
  authService.clearToken();

  return router.createUrlTree(['/login'], {
    queryParams: { redirect: state.url },
  });
};

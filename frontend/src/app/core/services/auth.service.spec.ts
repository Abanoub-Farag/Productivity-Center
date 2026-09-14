import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { PLATFORM_ID } from '@angular/core';
import { AuthService, AuthError } from './auth.service';
import { environment } from '../../../environments/environment';
import { ApiResponse, AuthData, LoginRequest } from '../models/auth.models';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  const baseUrl = `${environment.apiUrl}/api/v1/auth`;

  // A mock JWT token (header.payload.signature)
  // Payload: {"sub":"test@test.com","exp":9999999999}
  const mockJwt = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXN0QHRlc3QuY29tIiwiZXhwIjo5OTk5OTk5OTk5fQ.signature';
  const mockRefreshToken = 'mock-refresh-token';

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        AuthService,
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: PLATFORM_ID, useValue: 'browser' }
      ]
    });

    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);

    // Clear local storage for isolation
    localStorage.clear();
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  describe('Login Flow and JWT Storage', () => {
    it('should login successfully, persist tokens, and load user data', () => {
      const loginPayload: LoginRequest = { email: 'test@test.com', password: 'password123' };
      const mockResponse: ApiResponse<AuthData> = {
        status: 200,
        message: 'Success',
        localDateTime: new Date().toISOString(),
        data: {
          jwtToken: mockJwt,
          refreshToken: mockRefreshToken,
          user: { id: 1, email: 'test@test.com', firstName: 'Test', lastName: 'User' } as any
        }
      };

      service.login(loginPayload).subscribe((res) => {
        expect(res.data?.jwtToken).toBe(mockJwt);
      });

      const req = httpMock.expectOne(`${baseUrl}/login`);
      expect(req.request.method).toBe('POST');
      req.flush(mockResponse);

      // Verify localStorage was updated
      expect(localStorage.getItem('pcenter_jwt_token')).toBe(mockJwt);
      expect(localStorage.getItem('pcenter_refresh_token')).toBe(mockRefreshToken);
      
      // The login method also triggers a call to loadCurrentUser which hits the /me endpoint
      const meReq = httpMock.expectOne(`${baseUrl}/me`);
      expect(meReq.request.method).toBe('GET');
      meReq.flush({ status: 200, data: { id: 1, email: 'test@test.com' } });
    });
  });

  describe('Refresh Token Rotation', () => {
    it('should successfully rotate refresh token and store the new JWT', () => {
      localStorage.setItem('pcenter_refresh_token', mockRefreshToken);

      service.refreshToken().subscribe((newToken) => {
        expect(newToken).toBe('new-jwt-token');
      });

      const req = httpMock.expectOne(`${baseUrl}/refresh`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ refreshToken: mockRefreshToken });
      
      req.flush({
        status: 200,
        data: { token: 'new-jwt-token' }
      });

      expect(localStorage.getItem('pcenter_jwt_token')).toBe('new-jwt-token');
    });

    it('should throw an error and clear tokens if no refresh token is present', () => {
      localStorage.removeItem('pcenter_refresh_token');
      localStorage.setItem('pcenter_jwt_token', mockJwt);

      service.refreshToken().subscribe({
        next: () => fail('Should have failed'),
        error: (err) => {
          expect(err.message).toBe('No refresh token available');
        }
      });

      expect(localStorage.getItem('pcenter_jwt_token')).toBeNull();
      httpMock.expectNone(`${baseUrl}/refresh`);
    });
  });

  describe('Logout Flow', () => {
    it('should call logout endpoint, clear storage, and reset user signal', () => {
      localStorage.setItem('pcenter_refresh_token', mockRefreshToken);
      localStorage.setItem('pcenter_jwt_token', mockJwt);

      // Mock setting a user
      service.currentUser.set({ id: 1 } as any);

      service.logout().subscribe((res) => {
        expect(res.status).toBe(200);
      });

      const req = httpMock.expectOne(`${baseUrl}/logout`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ refreshToken: mockRefreshToken });
      
      req.flush({ status: 200, message: 'Logged out' });

      expect(localStorage.getItem('pcenter_jwt_token')).toBeNull();
      expect(localStorage.getItem('pcenter_refresh_token')).toBeNull();
      expect(service.currentUser()).toBeNull();
    });

    it('should handle local logout gracefully if server logout fails', () => {
      localStorage.setItem('pcenter_refresh_token', mockRefreshToken);
      localStorage.setItem('pcenter_jwt_token', mockJwt);
      service.currentUser.set({ id: 1 } as any);

      service.logout().subscribe((res) => {
        expect(res.status).toBe(500);
        expect(res.message).toBe('Server logout failed');
      });

      const req = httpMock.expectOne(`${baseUrl}/logout`);
      req.flush('Server Error', { status: 500, statusText: 'Internal Server Error' });

      // Ensure tokens are still cleared locally
      expect(localStorage.getItem('pcenter_jwt_token')).toBeNull();
      expect(localStorage.getItem('pcenter_refresh_token')).toBeNull();
      expect(service.currentUser()).toBeNull();
    });
  });

  describe('Error Handling (403 / 500 Responses)', () => {
    it('should format a 500 Server Error response correctly during login', () => {
      const loginPayload: LoginRequest = { email: 'test@test.com', password: 'password123' };
      
      service.login(loginPayload).subscribe({
        next: () => fail('Should have failed'),
        error: (err: AuthError) => {
          expect(err.status).toBe(500);
          expect(err.message).toBe('Oops! Something went wrong on our end. Please try again in a few minutes.');
        }
      });

      const req = httpMock.expectOne(`${baseUrl}/login`);
      req.flush(
        { message: 'Internal Server Error' },
        { status: 500, statusText: 'Server Error' }
      );
    });

    it('should parse specific backend validation errors (e.g. 403/400) containing fieldErrors', () => {
      const loginPayload: LoginRequest = { email: 'test@test.com', password: 'password123' };
      
      service.login(loginPayload).subscribe({
        next: () => fail('Should have failed'),
        error: (err: AuthError) => {
          expect(err.status).toBe(403);
          expect(err.message).toBe('Access Denied');
          expect(err.fieldErrors['email']).toBe('Account is locked');
        }
      });

      const req = httpMock.expectOne(`${baseUrl}/login`);
      req.flush(
        {
          message: 'Access Denied',
          errors: {
            errors: [
              { field: 'email', message: 'Account is locked' }
            ]
          }
        },
        { status: 403, statusText: 'Forbidden' }
      );
    });
  });
});

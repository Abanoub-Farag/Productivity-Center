import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { AuthService } from '../../../core/services/auth.service';
import { getUserRoomId } from '../../../core/models/auth.models';

export interface ApiErrorDetail {
  [key: string]: string | string[] | any;
}

export interface ApiResponse<T = any> {
  localDateTime: string;
  status: number;
  message: string;
  data: T;
  errors: ApiErrorDetail | string[] | string | null;
}

export type RoomVisibility = 'PUBLIC' | 'PRIVATE';

export interface RoomData {
  id: number;
  title: string;
  description: string;
  ownerId?: number;
  visibility?: RoomVisibility;
  // To satisfy our components which might look for these:
  tags?: string[];
  status?: string;
  count?: number;
  countType?: string;
  actionType?: string;
  [key: string]: any;
}

export interface PageableResponse {
  content: RoomData[];
  [key: string]: any; // To cover pageable info
}

export interface FavoriteRoomItem {
  roomId: number;
  title: string;
  description: string;
  addedAt: string;
  [key: string]: any;
}

export interface FavoritePageResponse {
  content: FavoriteRoomItem[];
  numberOfElements: number;
  first: boolean;
  last: boolean;
  size: number;
  number?: number;
  totalPages?: number;
  totalElements?: number;
  pageable?: any;
  [key: string]: any;
}

export interface CreateRoomDto {
  title: string;
  description: string;
  visibility: RoomVisibility;
}

export interface UpdateRoomDto {
  title: string;
  description: string;
  visibility: RoomVisibility;
}

@Injectable({
  providedIn: 'root'
})
export class RoomService {
  private readonly http = inject(HttpClient);
  private readonly authService = inject(AuthService);
  private readonly baseUrl = `${environment.apiUrl}/api/v1/rooms`;

  // State to track if the current user owns an active room derived from global user state
  userRoomId = computed<number | null>(() => {
    return getUserRoomId(this.authService.currentUser());
  });



  getRooms(page: number = 0, size: number = 20): Observable<ApiResponse<PageableResponse>> {
    const token = this.authService.getToken();
    let headers = new HttpHeaders({
      'ngrok-skip-browser-warning': 'true'
    });
    if (token) {
      headers = headers.set('Authorization', `Bearer ${token}`);
    }

    return this.http.get<ApiResponse<PageableResponse>>(this.baseUrl, {
      headers,
      params: {
        page: page.toString(),
        size: size.toString()
      }
    });
  }

  getFavorites(page: number = 0, size: number = 20, sort?: string): Observable<ApiResponse<FavoritePageResponse>> {
    const token = this.authService.getToken();
    let headers = new HttpHeaders({
      'ngrok-skip-browser-warning': 'true'
    });
    if (token) {
      headers = headers.set('Authorization', `Bearer ${token}`);
    }

    const params: any = {
      page: page.toString(),
      size: size.toString()
    };
    if (sort) {
      params.sort = sort;
    }

    return this.http.get<ApiResponse<FavoritePageResponse>>(`${this.baseUrl}/favorites`, { headers, params });
  }

  addToFavorites(roomId: number | string): Observable<ApiResponse<any>> {
    const numericId = typeof roomId === 'number' ? roomId : parseInt(String(roomId), 10);
    if (!numericId || isNaN(numericId) || !Number.isInteger(numericId) || numericId <= 0) {
      return throwError(() => new Error('Invalid room ID: must be a positive 64-bit integer.'));
    }
    const token = this.authService.getToken();
    let headers = new HttpHeaders({
      'Content-Type': 'application/json',
      'ngrok-skip-browser-warning': 'true'
    });
    if (token) {
      headers = headers.set('Authorization', `Bearer ${token}`);
    }

    return this.http.post<ApiResponse<any>>(`${this.baseUrl}/favorites/${numericId}`, {}, { headers });
  }

  removeFromFavorites(roomId: number | string): Observable<ApiResponse<any>> {
    const numericId = typeof roomId === 'number' ? roomId : parseInt(String(roomId), 10);
    if (!numericId || isNaN(numericId) || !Number.isInteger(numericId) || numericId <= 0) {
      return throwError(() => new Error('Invalid room ID: must be a positive 64-bit integer.'));
    }
    const token = this.authService.getToken();
    let headers = new HttpHeaders({
      'ngrok-skip-browser-warning': 'true'
    });
    if (token) {
      headers = headers.set('Authorization', `Bearer ${token}`);
    }

    return this.http.delete<ApiResponse<any>>(`${this.baseUrl}/favorites/${numericId}`, { headers });
  }

  createRoom(dto: CreateRoomDto): Observable<ApiResponse<RoomData>> {
    const token = this.authService.getToken();
    let headers = new HttpHeaders({
      'Content-Type': 'application/json',
      'ngrok-skip-browser-warning': 'true'
    });
    if (token) {
      headers = headers.set('Authorization', `Bearer ${token}`);
    }

    return this.http.post<ApiResponse<RoomData>>(this.baseUrl, dto, { headers });
  }

  getRoomById(id: number | string): Observable<ApiResponse<RoomData>> {
    const numericId = typeof id === 'number' ? id : parseInt(String(id), 10);
    const token = this.authService.getToken();
    let headers = new HttpHeaders({
      'ngrok-skip-browser-warning': 'true'
    });
    if (token) {
      headers = headers.set('Authorization', `Bearer ${token}`);
    }

    return this.http.get<ApiResponse<RoomData>>(`${this.baseUrl}/${numericId}`, { headers });
  }

  updateRoom(roomId: number | string, dto: UpdateRoomDto): Observable<ApiResponse<RoomData>> {
    const numericId = typeof roomId === 'number' ? roomId : parseInt(String(roomId), 10);
    if (!numericId || isNaN(numericId) || !Number.isInteger(numericId) || numericId <= 0) {
      return throwError(() => new Error('Invalid room ID: must be a positive 64-bit integer.'));
    }
    const token = this.authService.getToken();
    let headers = new HttpHeaders({
      'Content-Type': 'application/json',
      'ngrok-skip-browser-warning': 'true'
    });
    if (token) {
      headers = headers.set('Authorization', `Bearer ${token}`);
    }

    return this.http.put<ApiResponse<RoomData>>(`${this.baseUrl}/${numericId}`, dto, { headers });
  }

  deleteRoom(roomId: number | string): Observable<ApiResponse<any>> {
    const numericId = typeof roomId === 'number' ? roomId : parseInt(String(roomId), 10);
    if (!numericId || isNaN(numericId) || !Number.isInteger(numericId) || numericId <= 0) {
      return throwError(() => new Error('Invalid room ID: must be a positive 64-bit integer.'));
    }
    const token = this.authService.getToken();
    let headers = new HttpHeaders({
      'ngrok-skip-browser-warning': 'true'
    });
    if (token) {
      headers = headers.set('Authorization', `Bearer ${token}`);
    }

    return this.http.delete<ApiResponse<any>>(`${this.baseUrl}/${numericId}`, { headers });
  }

  joinRoom(roomId: number | string): Observable<ApiResponse<any>> {
    const numericId = typeof roomId === 'number' ? roomId : parseInt(String(roomId), 10);
    if (!numericId || isNaN(numericId) || !Number.isInteger(numericId) || numericId <= 0) {
      return throwError(() => new Error('Invalid room ID: must be a positive 64-bit integer.'));
    }
    const token = this.authService.getToken();
    let headers = new HttpHeaders({
      'Content-Type': 'application/json',
      'ngrok-skip-browser-warning': 'true'
    });
    if (token) {
      headers = headers.set('Authorization', `Bearer ${token}`);
    }
    return this.http.post<ApiResponse<any>>(`${this.baseUrl}/${numericId}/join`, {}, { headers });
  }

  sendHeartbeat(roomId: number | string): Observable<ApiResponse<any>> {
    const numericId = typeof roomId === 'number' ? roomId : parseInt(String(roomId), 10);
    if (!numericId || isNaN(numericId) || !Number.isInteger(numericId) || numericId <= 0) {
      return throwError(() => new Error('Invalid room ID: must be a positive 64-bit integer.'));
    }
    const token = this.authService.getToken();
    let headers = new HttpHeaders({
      'Content-Type': 'application/json',
      'ngrok-skip-browser-warning': 'true'
    });
    if (token) {
      headers = headers.set('Authorization', `Bearer ${token}`);
    }
    return this.http.post<ApiResponse<any>>(`${this.baseUrl}/${numericId}/heartbeat`, {}, { headers });
  }
}

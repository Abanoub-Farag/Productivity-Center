import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { ApiResponse } from '../../../core/models/auth.models';
import { UserProfileData, UpdateProfileRequest } from '../models/profile.models';

export type { UserProfileData, UpdateProfileRequest };

@Injectable({ providedIn: 'root' })
export class ProfileDataService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/v1/profile`;

  getProfile(userId: number | string): Observable<ApiResponse<UserProfileData>> {
    return this.http.get<ApiResponse<UserProfileData>>(`${this.baseUrl}/${userId}`);
  }

  updateProfile(data: UpdateProfileRequest): Observable<ApiResponse<UserProfileData>> {
    return this.http.put<ApiResponse<UserProfileData>>(this.baseUrl, data);
  }
}

// Backwards-compat shim — keeps any existing consumers (room-join, top-nav, etc.) compiling.
export { ProfileDataService as ProfileService };

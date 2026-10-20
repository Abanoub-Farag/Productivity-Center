import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../../environments/environment';
import { ApiResponse } from '../../../core/models/auth.models';
import { DashboardResponseDto } from '../models/analytics.models';

@Injectable({ providedIn: 'root' })
export class AnalyticsDataService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/v1/dashboard`;

  getDashboard(startDate: string, endDate: string): Observable<DashboardResponseDto> {
    const params = new HttpParams()
      .set('startDate', startDate)
      .set('endDate', endDate);

    return this.http
      .get<ApiResponse<DashboardResponseDto>>(this.baseUrl, { params })
      .pipe(map((r) => r.data!));
  }
}

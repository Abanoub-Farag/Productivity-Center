import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { AuthService } from '../../../core/services/auth.service';
import { ApiResponse } from '../../../core/models/auth.models';
import { TaskData, UpdateTaskRequest, PageableTaskResponse } from '../models/rooms.models';

export type { TaskData, UpdateTaskRequest };

@Injectable({ providedIn: 'root' })
export class TaskService {
  private readonly http = inject(HttpClient);
  private readonly authService = inject(AuthService);
  private readonly baseUrl = `${environment.apiUrl}/api/v1/tasks`;

  private get authHeaders(): Record<string, string> {
    return { Authorization: `Bearer ${this.authService.getToken()}` };
  }

  getTasks(page = 0, size = 50, sort?: string): Observable<ApiResponse<PageableTaskResponse>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    if (sort) params = params.set('sort', sort);

    return this.http.get<ApiResponse<PageableTaskResponse>>(this.baseUrl, {
      headers: this.authHeaders,
      params,
    });
  }

  createTask(data: { title: string; isCompleted: boolean }): Observable<ApiResponse<TaskData>> {
    return this.http.post<ApiResponse<TaskData>>(this.baseUrl, data, {
      headers: this.authHeaders,
    });
  }

  updateTask(taskId: number, data: UpdateTaskRequest): Observable<ApiResponse<TaskData>> {
    return this.http.put<ApiResponse<TaskData>>(`${this.baseUrl}/${taskId}`, data, {
      headers: this.authHeaders,
    });
  }

  deleteTask(taskId: number): Observable<ApiResponse<null>> {
    return this.http.delete<ApiResponse<null>>(`${this.baseUrl}/${taskId}`, {
      headers: this.authHeaders,
    });
  }
}

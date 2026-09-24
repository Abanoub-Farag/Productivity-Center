import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../../environments/environment';
import { ApiResponse } from '../../../core/models/auth.models';
import {
  TaskData,
  CreateTaskRequest,
  UpdateTaskRequest,
  PageableTaskResponse,
} from '../models/task.models';

/**
 * HTTP service for the /api/v1/tasks resource.
 *
 * Auth headers are injected globally by `authInterceptor` – this service
 * carries no AuthService dependency.
 *
 * The backend response uses the field name `completed`; this service
 * normalises every task to `isCompleted` before emitting, so callers
 * never need to handle both fields.
 */
@Injectable({ providedIn: 'root' })
export class TaskService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/v1/tasks`;

  getTasks(page = 0, size = 50, sort?: string): Observable<ApiResponse<PageableTaskResponse>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    if (sort) params = params.set('sort', sort);

    return this.http
      .get<ApiResponse<PageableTaskResponse>>(this.baseUrl, { params })
      .pipe(map((res) => this.normalisePageResponse(res)));
  }

  createTask(data: CreateTaskRequest): Observable<ApiResponse<TaskData>> {
    return this.http
      .post<ApiResponse<TaskData>>(this.baseUrl, data)
      .pipe(map((res) => this.normaliseTaskResponse(res)));
  }

  updateTask(taskId: number, data: UpdateTaskRequest): Observable<ApiResponse<TaskData>> {
    return this.http
      .put<ApiResponse<TaskData>>(`${this.baseUrl}/${taskId}`, data)
      .pipe(map((res) => this.normaliseTaskResponse(res)));
  }

  deleteTask(taskId: number): Observable<ApiResponse<null>> {
    return this.http.delete<ApiResponse<null>>(`${this.baseUrl}/${taskId}`);
  }

  // ── Private helpers ─────────────────────────────────────────────────────────

  /** Normalise the `completed` field the API returns into `isCompleted`. */
  private normaliseTask(raw: TaskData & { completed?: boolean }): TaskData {
    return {
      ...raw,
      isCompleted: raw.isCompleted ?? raw.completed ?? false,
    };
  }

  private normaliseTaskResponse(
    res: ApiResponse<TaskData & { completed?: boolean }>,
  ): ApiResponse<TaskData> {
    if (res.data) {
      (res as ApiResponse<TaskData>).data = this.normaliseTask(res.data);
    }
    return res as ApiResponse<TaskData>;
  }

  private normalisePageResponse(
    res: ApiResponse<PageableTaskResponse & { content: (TaskData & { completed?: boolean })[] }>,
  ): ApiResponse<PageableTaskResponse> {
    if (res.data?.content) {
      res.data.content = res.data.content.map((t) => this.normaliseTask(t));
    }
    return res as ApiResponse<PageableTaskResponse>;
  }
}

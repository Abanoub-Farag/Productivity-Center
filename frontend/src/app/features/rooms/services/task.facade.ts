import { Injectable, inject, signal, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import { TaskService } from '../../../features/tasks/services/task.service';
import { TaskData, UpdateTaskRequest } from '../../../features/tasks/models/task.models';

/**
 * Scoped per room-detail route; provided in RoomDetailComponent's providers array.
 *
 * Owns the full task CRUD lifecycle for the room session panel.
 * Performs optimistic updates with automatic rollback on error.
 * All HTTP I/O is delegated to TaskService.
 */
@Injectable()
export class TaskFacade {
  private readonly taskService = inject(TaskService);
  private readonly destroyRef = inject(DestroyRef);

  // ── Task state ─────────────────────────────────────────────────────────────

  readonly tasks = signal<TaskData[]>([]);
  readonly isTasksLoading = signal<boolean>(true);
  readonly tasksError = signal<string | null>(null);
  readonly newTaskText = signal<string>('');
  readonly editingTaskId = signal<number | null>(null);
  readonly editingTaskTitle = signal<string>('');
  readonly updatingTaskId = signal<number | null>(null);
  readonly taskUpdateError = signal<string | null>(null);

  // ── Task actions ───────────────────────────────────────────────────────────

  fetchTasks(): void {
    this.isTasksLoading.set(true);
    this.tasksError.set(null);

    this.taskService.getTasks()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => {
          // TaskService already normalises `completed` → `isCompleted`.
          this.tasks.set(response.data?.content ?? []);
          this.isTasksLoading.set(false);
        },
        error: (err: HttpErrorResponse) => {
          console.error(err);
          this.tasksError.set('Failed to load tasks.');
          this.isTasksLoading.set(false);
        },
      });
  }

  toggleTask(task: TaskData): void {
    const updatedStatus = !task.isCompleted;
    this.updatingTaskId.set(task.id);
    this.taskUpdateError.set(null);
    this.tasks.update((list) =>
      list.map((t) =>
        t.id === task.id ? { ...t, isCompleted: updatedStatus } : t,
      ),
    );

    const payload: UpdateTaskRequest = { title: task.title, isCompleted: updatedStatus };
    this.taskService.updateTask(task.id, payload)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => this.updatingTaskId.set(null),
        error: (err: HttpErrorResponse) => {
          console.error('Failed to update task completion', err);
          this.taskUpdateError.set('Failed to update task status.');
          this.updatingTaskId.set(null);
          // Roll back optimistic update.
          this.tasks.update((list) =>
            list.map((t) =>
              t.id === task.id ? { ...t, isCompleted: task.isCompleted } : t,
            ),
          );
        },
      });
  }

  startEditTask(task: TaskData): void {
    this.editingTaskId.set(task.id);
    this.editingTaskTitle.set(task.title);
    this.taskUpdateError.set(null);
  }

  cancelEditTask(): void {
    this.editingTaskId.set(null);
    this.editingTaskTitle.set('');
  }

  setEditingTaskTitle(title: string): void { this.editingTaskTitle.set(title); }

  saveTaskTitle(task: TaskData): void {
    const newTitle = this.editingTaskTitle().trim();
    if (!newTitle) return;
    if (newTitle === task.title) { this.cancelEditTask(); return; }

    const previousTitle = task.title;
    this.updatingTaskId.set(task.id);
    this.taskUpdateError.set(null);
    this.tasks.update((list) =>
      list.map((t) => (t.id === task.id ? { ...t, title: newTitle } : t)),
    );
    this.editingTaskId.set(null);

    const payload: UpdateTaskRequest = {
      title: newTitle,
      isCompleted: task.isCompleted,
    };
    this.taskService.updateTask(task.id, payload)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => this.updatingTaskId.set(null),
        error: (err: HttpErrorResponse) => {
          console.error('Failed to update task title', err);
          this.taskUpdateError.set('Failed to update task title.');
          this.updatingTaskId.set(null);
          this.tasks.update((list) =>
            list.map((t) => (t.id === task.id ? { ...t, title: previousTitle } : t)),
          );
        },
      });
  }

  addTask(): void {
    const text = this.newTaskText().trim();
    if (!text) return;
    this.newTaskText.set('');
    this.taskService.createTask({ title: text, isCompleted: false })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => this.fetchTasks(),
        error: (err: HttpErrorResponse) =>
          console.error('Failed to create task', err),
      });
  }

  setNewTaskText(text: string): void { this.newTaskText.set(text); }

  deleteTask(taskId: number): void {
    this.updatingTaskId.set(taskId);
    this.taskUpdateError.set(null);
    this.taskService.deleteTask(taskId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.tasks.update((list) => list.filter((t) => t.id !== taskId));
          this.updatingTaskId.set(null);
        },
        error: (err: HttpErrorResponse) => {
          console.error('Failed to delete task', err);
          this.taskUpdateError.set('Failed to delete task.');
          this.updatingTaskId.set(null);
        },
      });
  }
}

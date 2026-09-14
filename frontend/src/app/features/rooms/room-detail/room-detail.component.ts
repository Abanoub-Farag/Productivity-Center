import {
  Component,
  OnInit,
  inject,
  signal,
  computed,
  ChangeDetectionStrategy,
  DestroyRef
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { timer, switchMap, retry, catchError, of } from 'rxjs';
import {
  LucideAngularModule,
  ArrowLeft,
  Bell,
  Clock,
  RotateCcw,
  Play,
  Pause,
  Settings,
  Plus,
  CheckCircle2,
  Circle,
  HelpCircle,
  ClipboardList,
  Trash2,
  Heart,
  Pencil,
  Check,
  X,
  Shield,
  Lock,
} from 'lucide-angular';
import { SidebarComponent } from '../components/sidebar/sidebar.component';
import { RoomTimerComponent } from '../components/room-timer/room-timer.component';
import { RoomMembersListComponent } from '../components/room-members-list/room-members-list.component';
import { RoomService, RoomData, UpdateRoomDto } from '../services/room.service';
import { TaskService, TaskData, UpdateTaskRequest } from '../services/task.service';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-room-detail',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    FormsModule,
    LucideAngularModule,
    SidebarComponent,
    RoomTimerComponent,
    RoomMembersListComponent
  ],
  templateUrl: './room-detail.component.html',
  styleUrls: ['./room-detail.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class RoomDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly roomService = inject(RoomService);
  private readonly taskService = inject(TaskService);
  private readonly authService = inject(AuthService);
  private readonly destroyRef = inject(DestroyRef);

  // ── Lucide Icons ──────────────────────────────────────────────────────────
  readonly ArrowLeftIcon = ArrowLeft;
  readonly BellIcon = Bell;
  readonly ClockIcon = Clock;
  readonly RotateCcwIcon = RotateCcw;
  readonly PlayIcon = Play;
  readonly PauseIcon = Pause;
  readonly SettingsIcon = Settings;
  readonly PlusIcon = Plus;
  readonly CheckCircle2Icon = CheckCircle2;
  readonly CircleIcon = Circle;
  readonly HelpCircleIcon = HelpCircle;
  readonly CheckSquareIcon = ClipboardList;
  readonly Trash2Icon = Trash2;
  readonly HeartIcon = Heart;
  readonly PencilIcon = Pencil;
  readonly CheckIcon = Check;
  readonly XIcon = X;
  readonly ShieldIcon = Shield;
  readonly LockIcon = Lock;

  // ── Room data ─────────────────────────────────────────────────────────────
  roomId = signal<number | null>(null);
  room = signal<RoomData | null>(null);
  isLoading = signal<boolean>(true);
  error = signal<string | null>(null);

  isOwner = computed(() => {
    const r = this.room();
    const user = this.authService.currentUser();
    return !!r && !!user && r.ownerId === user.id;
  });

  isFavorite = signal<boolean>(false);
  isPendingFavorite = signal<boolean>(false);

  // ── Room Edit & Delete States ──────────────────────────────────────────────
  isEditRoomModalOpen = signal<boolean>(false);
  editRoomTitle = signal<string>('');
  editRoomDescription = signal<string>('');
  editRoomVisibility = signal<'PUBLIC' | 'PRIVATE'>('PUBLIC');
  isUpdatingRoom = signal<boolean>(false);
  roomUpdateError = signal<string | null>(null);

  isDeleteRoomModalOpen = signal<boolean>(false);
  isDeletingRoom = signal<boolean>(false);
  roomDeleteError = signal<string | null>(null);

  // ── Heartbeat State ───────────────────────────────────────────────────────
  heartbeatStatus = signal<'active' | 'retrying' | 'failed'>('active');
  heartbeatErrorMessage = signal<string | null>(null);

  // ── Tasks ─────────────────────────────────────────────────────────────────
  tasks = signal<TaskData[]>([]);
  isTasksLoading = signal<boolean>(true);
  tasksError = signal<string | null>(null);
  newTaskText = signal<string>('');
  editingTaskId = signal<number | null>(null);
  editingTaskTitle = signal<string>('');
  updatingTaskId = signal<number | null>(null);
  taskUpdateError = signal<string | null>(null);

  // ── Participants ──────────────────────────────────────────────────────────
  participants = signal<any[]>([]);

  ngOnInit() {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      const id = parseInt(idParam, 10);
      if (!isNaN(id) && id > 0) {
        this.roomId.set(id);
        this.joinRoom(id);
        this.fetchRoom(id);
        this.fetchTasks();
        this.startHeartbeat(id);
        this.checkIfFavorite(id);
      } else {
        this.router.navigate(['/404'], { replaceUrl: true });
      }
    } else {
      this.router.navigate(['/404'], { replaceUrl: true });
    }
  }

  checkIfFavorite(roomId: number) {
    this.roomService.getFavorites(0, 100).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (res) => {
        const items = res.data?.content || [];
        const isFav = items.some(item => item.roomId === roomId);
        this.isFavorite.set(isFav);
      },
      error: (err) => console.error('Failed checking favorite state:', err)
    });
  }

  toggleFavorite() {
    const currentRoom = this.room();
    if (!currentRoom || this.isPendingFavorite()) return;

    const roomId = currentRoom.id;
    const isCurrentlyFav = this.isFavorite();
    const targetState = !isCurrentlyFav;

    this.isFavorite.set(targetState);
    this.isPendingFavorite.set(true);

    const request$ = targetState 
      ? this.roomService.addToFavorites(roomId) 
      : this.roomService.removeFromFavorites(roomId);

    request$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.isPendingFavorite.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.isFavorite.set(isCurrentlyFav);
        this.isPendingFavorite.set(false);
        const msg = err.error?.message || 'Failed to update favorite status.';
        alert(msg);
      }
    });
  }

  joinRoom(id: number) {
    this.roomService.joinRoom(id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (res) => {
        console.log(`Successfully joined room ${id}:`, res);
      },
      error: (err) => {
        console.error(`Error joining room ${id}:`, err);
      }
    });
  }

  private startHeartbeat(roomId: number) {
    const INTERVAL_MS = 25000; // 25s keep-alive interval
    let consecutiveFailures = 0;
    const MAX_CONSECUTIVE_FAILURES = 3;

    timer(0, INTERVAL_MS)
      .pipe(
        switchMap(() =>
          this.roomService.sendHeartbeat(roomId).pipe(
            retry({
              count: 2,
              delay: (error: HttpErrorResponse, retryCount: number) => {
                // Critical security/membership errors should fail immediately without retry
                if ([401, 403, 404].includes(error.status)) {
                  throw error;
                }
                return timer(retryCount * 2000);
              }
            }),
            catchError((err: HttpErrorResponse) => {
              return of({ isError: true, error: err });
            })
          )
        ),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe((res: any) => {
        if (res?.isError) {
          const err: HttpErrorResponse = res.error;
          const isCritical = [401, 403, 404].includes(err.status);
          consecutiveFailures++;

          if (isCritical || consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
            this.heartbeatStatus.set('failed');
            const parsedMsg = this.extractHeartbeatError(err);
            this.heartbeatErrorMessage.set(parsedMsg);
            if (err.status === 404) {
              this.error.set('Room has been closed or no longer exists.');
            } else if (err.status === 401 || err.status === 403) {
              this.error.set('Session expired or access to this room was revoked.');
            }
          } else {
            this.heartbeatStatus.set('retrying');
          }
        } else {
          consecutiveFailures = 0;
          this.heartbeatStatus.set('active');
          this.heartbeatErrorMessage.set(null);
        }
      });
  }

  private extractHeartbeatError(err: HttpErrorResponse | any): string {
    if (err.status === 0 || err.error instanceof ErrorEvent) {
      return 'Network connection lost. Retrying heartbeat connection...';
    }

    const payloadMsg = err.error?.message;
    const payloadErrors = err.error?.errors;

    if (payloadErrors && typeof payloadErrors === 'object') {
      const formatted = Object.entries(payloadErrors)
        .map(([k, v]) => `${k}: ${v}`)
        .join(', ');
      if (formatted) return formatted;
    }

    switch (err.status) {
      case 401: return payloadMsg || 'Unauthorized room session.';
      case 403: return payloadMsg || 'Access denied to this room.';
      case 404: return payloadMsg || 'Room not found or session ended.';
      case 500: return payloadMsg || 'Internal server error while sending keep-alive heartbeat.';
      default: return payloadMsg || `Heartbeat failed (Status ${err.status}).`;
    }
  }

  fetchRoom(id: number) {
    this.roomService.getRoomById(id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (response) => {
        if (response.data) {
          this.room.set(response.data);
          this.isLoading.set(false);
        } else {
          this.router.navigate(['/404'], { replaceUrl: true });
        }
      },
      error: (err: HttpErrorResponse) => {
        console.error(err);
        if (err?.status === 404) {
          this.router.navigate(['/404'], { replaceUrl: true });
        } else {
          this.error.set('Failed to load room details.');
          this.isLoading.set(false);
        }
      }
    });
  }

  // ── Room Edit & Delete Controls ───────────────────────────────────────────
  openEditRoomModal() {
    const currentRoom = this.room();
    if (!currentRoom) return;
    this.editRoomTitle.set(currentRoom.title || '');
    this.editRoomDescription.set(currentRoom.description || '');
    this.editRoomVisibility.set(currentRoom.visibility || 'PUBLIC');
    this.roomUpdateError.set(null);
    this.isEditRoomModalOpen.set(true);
  }

  closeEditRoomModal() {
    this.isEditRoomModalOpen.set(false);
    this.roomUpdateError.set(null);
  }

  onEditRoomTitleInput(event: Event) {
    this.editRoomTitle.set((event.target as HTMLInputElement).value);
  }

  onEditRoomDescriptionInput(event: Event) {
    this.editRoomDescription.set((event.target as HTMLTextAreaElement).value);
  }

  submitUpdateRoom() {
    const currentRoom = this.room();
    if (!currentRoom) return;
    const title = this.editRoomTitle().trim();
    const description = this.editRoomDescription().trim();
    const visibility = this.editRoomVisibility();

    if (!title) {
      this.roomUpdateError.set('Room title is required.');
      return;
    }

    this.isUpdatingRoom.set(true);
    this.roomUpdateError.set(null);

    const dto: UpdateRoomDto = { title, description, visibility };

    this.roomService.updateRoom(currentRoom.id, dto)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => {
          this.isUpdatingRoom.set(false);
          this.isEditRoomModalOpen.set(false);
          const updatedData = res.data || { ...currentRoom, title, description, visibility };
          this.room.update(r => r ? { ...r, ...updatedData } : null);
        },
        error: (err: HttpErrorResponse) => {
          console.error('Failed to update room:', err);
          this.isUpdatingRoom.set(false);
          const msg = err.status === 403 
            ? 'You do not have permission to perform this action' 
            : (err.error?.message || 'Failed to update room. Please try again.');
          this.roomUpdateError.set(msg);
        }
      });
  }

  openDeleteRoomModal() {
    this.roomDeleteError.set(null);
    this.isDeleteRoomModalOpen.set(true);
  }

  closeDeleteRoomModal() {
    this.isDeleteRoomModalOpen.set(false);
    this.roomDeleteError.set(null);
  }

  confirmDeleteRoom() {
    const currentRoom = this.room();
    if (!currentRoom) return;

    this.isDeletingRoom.set(true);
    this.roomDeleteError.set(null);

    this.roomService.deleteRoom(currentRoom.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.isDeletingRoom.set(false);
          this.isDeleteRoomModalOpen.set(false);
          this.router.navigate(['/rooms']);
        },
        error: (err: HttpErrorResponse) => {
          console.error('Failed to delete room:', err);
          this.isDeletingRoom.set(false);
          const msg = err.status === 403 
            ? 'You do not have permission to perform this action' 
            : (err.error?.message || 'Failed to delete room. Please try again.');
          this.roomDeleteError.set(msg);
        }
      });
  }

  fetchTasks() {
    this.isTasksLoading.set(true);
    this.tasksError.set(null);
    this.taskService.getTasks().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (response) => {
        this.tasks.set(response.data?.content || []);
        this.isTasksLoading.set(false);
      },
      error: (err) => {
        console.error(err);
        this.tasksError.set('Failed to load tasks.');
        this.isTasksLoading.set(false);
      }
    });
  }

  // ── Task controls ─────────────────────────────────────────────────────────
  toggleTask(task: TaskData) {
    const updatedStatus = !task.isCompleted;
    this.updatingTaskId.set(task.id);
    this.taskUpdateError.set(null);

    // Optimistic update
    this.tasks.update((tasks) =>
      tasks.map((t) => (t.id === task.id ? { ...t, isCompleted: updatedStatus } : t)),
    );

    const payload: UpdateTaskRequest = {
      title: task.title,
      completed: updatedStatus
    };

    this.taskService.updateTask(task.id, payload)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.updatingTaskId.set(null);
        },
        error: (err) => {
          console.error('Failed to update task completion', err);
          this.taskUpdateError.set('Failed to update task status.');
          this.updatingTaskId.set(null);
          // Revert optimistic update on error
          this.tasks.update((tasks) =>
            tasks.map((t) => (t.id === task.id ? { ...t, isCompleted: task.isCompleted } : t)),
          );
        }
      });
  }

  startEditTask(task: TaskData) {
    this.editingTaskId.set(task.id);
    this.editingTaskTitle.set(task.title);
    this.taskUpdateError.set(null);
  }

  cancelEditTask() {
    this.editingTaskId.set(null);
    this.editingTaskTitle.set('');
  }

  onEditTaskInput(event: Event) {
    this.editingTaskTitle.set((event.target as HTMLInputElement).value);
  }

  saveTaskTitle(task: TaskData) {
    const newTitle = this.editingTaskTitle().trim();
    if (!newTitle) return;
    if (newTitle === task.title) {
      this.cancelEditTask();
      return;
    }

    const previousTitle = task.title;
    this.updatingTaskId.set(task.id);
    this.taskUpdateError.set(null);

    // Optimistic update
    this.tasks.update((tasks) =>
      tasks.map((t) => (t.id === task.id ? { ...t, title: newTitle } : t))
    );
    this.editingTaskId.set(null);

    const payload: UpdateTaskRequest = {
      title: newTitle,
      completed: task.isCompleted
    };

    this.taskService.updateTask(task.id, payload)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.updatingTaskId.set(null);
        },
        error: (err) => {
          console.error('Failed to update task title', err);
          this.taskUpdateError.set('Failed to update task title.');
          this.updatingTaskId.set(null);
          // Revert optimistic update on error
          this.tasks.update((tasks) =>
            tasks.map((t) => (t.id === task.id ? { ...t, title: previousTitle } : t))
          );
        }
      });
  }

  addTask() {
    const text = this.newTaskText().trim();
    if (!text) return;
    this.newTaskText.set('');
    
    this.taskService.createTask({ title: text, isCompleted: false })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.fetchTasks(); // Refetch to get the ID and correct state
        },
        error: (err) => {
          console.error('Failed to create task', err);
        }
      });
  }

  deleteTask(taskId: number) {
    this.taskService.deleteTask(taskId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.tasks.update((tasks) => tasks.filter(t => t.id !== taskId));
        },
        error: (err) => {
          console.error('Failed to delete task', err);
        }
      });
  }

  onNewTaskKeydown(event: KeyboardEvent) {
    if (event.key === 'Enter') {
      this.addTask();
    }
  }

  onNewTaskInput(event: Event) {
    this.newTaskText.set((event.target as HTMLInputElement).value);
  }
}

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
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
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
import { RoomHeaderComponent } from '../components/room-header/room-header.component';
import { RoomTaskPanelComponent } from '../components/room-task-panel/room-task-panel.component';
import { RoomStateService } from '../services/room-state.service';
import { TaskService, TaskData, UpdateTaskRequest } from '../services/task.service';

@Component({
  selector: 'app-room-detail',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    LucideAngularModule,
    SidebarComponent,
    RoomTimerComponent,
    RoomMembersListComponent,
    RoomHeaderComponent,
    RoomTaskPanelComponent
  ],
  providers: [RoomStateService],
  templateUrl: './room-detail.component.html',
  styleUrls: ['./room-detail.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class RoomDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  public readonly roomState = inject(RoomStateService);
  private readonly taskService = inject(TaskService);
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
  roomId = this.roomState.roomId;
  room = this.roomState.room;
  isLoading = this.roomState.isLoading;
  error = this.roomState.error;
  isOwner = this.roomState.isOwner;
  isFavorite = this.roomState.isFavorite;
  isPendingFavorite = this.roomState.isPendingFavorite;

  // ── Room Edit & Delete States ──────────────────────────────────────────────
  isEditRoomModalOpen = this.roomState.isEditRoomModalOpen;
  editRoomTitle = this.roomState.editRoomTitle;
  editRoomDescription = this.roomState.editRoomDescription;
  editRoomVisibility = this.roomState.editRoomVisibility;
  isUpdatingRoom = this.roomState.isUpdatingRoom;
  roomUpdateError = this.roomState.roomUpdateError;

  isDeleteRoomModalOpen = this.roomState.isDeleteRoomModalOpen;
  isDeletingRoom = this.roomState.isDeletingRoom;
  roomDeleteError = this.roomState.roomDeleteError;

  // ── Heartbeat State ───────────────────────────────────────────────────────
  heartbeatStatus = this.roomState.heartbeatStatus;
  heartbeatErrorMessage = this.roomState.heartbeatErrorMessage;

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
        this.roomState.initialize(id);
        this.fetchTasks();
      } else {
        this.router.navigate(['/404'], { replaceUrl: true });
      }
    } else {
      this.router.navigate(['/404'], { replaceUrl: true });
    }
  }

  checkIfFavorite(roomId: number) {
    this.roomState.checkIfFavorite(roomId);
  }

  toggleFavorite() {
    this.roomState.toggleFavorite();
  }

  joinRoom(id: number) {
    this.roomState.joinRoom(id);
  }

  fetchRoom(id: number) {
    this.roomState.fetchRoom(id);
  }

  // ── Room Edit & Delete Controls ───────────────────────────────────────────
  openEditRoomModal() {
    this.roomState.openEditRoomModal();
  }

  closeEditRoomModal() {
    this.roomState.closeEditRoomModal();
  }

  onEditRoomTitleInput(event: Event) {
    this.roomState.setEditRoomTitle((event.target as HTMLInputElement).value);
  }

  onEditRoomDescriptionInput(event: Event) {
    this.roomState.setEditRoomDescription((event.target as HTMLTextAreaElement).value);
  }

  submitUpdateRoom() {
    this.roomState.submitUpdateRoom();
  }

  openDeleteRoomModal() {
    this.roomState.openDeleteRoomModal();
  }

  closeDeleteRoomModal() {
    this.roomState.closeDeleteRoomModal();
  }

  confirmDeleteRoom() {
    this.roomState.confirmDeleteRoom();
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

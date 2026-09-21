import {
  Component,
  OnInit,
  inject,
  ChangeDetectionStrategy,
} from '@angular/core';
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
import { RoomDetailFacade } from '../services/room-state.service';
import { TaskData } from '../models/rooms.models';

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
    RoomTaskPanelComponent,
  ],
  providers: [RoomDetailFacade],
  templateUrl: './room-detail.component.html',
  styleUrls: ['./room-detail.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RoomDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  /** Expose facade publicly so template can access signals directly. */
  readonly roomState = inject(RoomDetailFacade);

  // ── Lucide icons ───────────────────────────────────────────────────────────
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

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      const id = parseInt(idParam, 10);
      if (!isNaN(id) && id > 0) {
        this.roomState.initialize(id);
      } else {
        this.router.navigate(['/404'], { replaceUrl: true });
      }
    } else {
      this.router.navigate(['/404'], { replaceUrl: true });
    }
  }

  // ── Thin template event handlers (delegate to facade) ─────────────────────

  toggleFavorite(): void { this.roomState.toggleFavorite(); }

  openEditRoomModal(): void { this.roomState.openEditRoomModal(); }
  closeEditRoomModal(): void { this.roomState.closeEditRoomModal(); }
  onEditRoomTitleInput(event: Event): void {
    this.roomState.setEditRoomTitle((event.target as HTMLInputElement).value);
  }
  onEditRoomDescriptionInput(event: Event): void {
    this.roomState.setEditRoomDescription((event.target as HTMLTextAreaElement).value);
  }
  submitUpdateRoom(): void { this.roomState.submitUpdateRoom(); }

  openDeleteRoomModal(): void { this.roomState.openDeleteRoomModal(); }
  closeDeleteRoomModal(): void { this.roomState.closeDeleteRoomModal(); }
  confirmDeleteRoom(): void { this.roomState.confirmDeleteRoom(); }

  onToggleTask(task: TaskData): void { this.roomState.toggleTask(task); }
  onStartEditTask(task: TaskData): void { this.roomState.startEditTask(task); }
  onSaveTaskTitle(task: TaskData): void { this.roomState.saveTaskTitle(task); }
  onCancelEditTask(): void { this.roomState.cancelEditTask(); }
  onDeleteTask(taskId: number): void { this.roomState.deleteTask(taskId); }
  onAddTask(): void { this.roomState.addTask(); }
  onNewTaskInput(event: Event): void {
    this.roomState.setNewTaskText((event.target as HTMLInputElement).value);
  }
  onNewTaskKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter') this.roomState.addTask();
  }
  onEditTaskInput(event: Event): void {
    this.roomState.setEditingTaskTitle((event.target as HTMLInputElement).value);
  }
}

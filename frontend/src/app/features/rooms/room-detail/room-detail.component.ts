import {
  Component,
  OnInit,
  inject,
  signal,
  ChangeDetectionStrategy,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule, X } from 'lucide-angular';
import { SidebarComponent } from '../components/sidebar/sidebar.component';
import { RoomTimerComponent } from '../components/room-timer/room-timer.component';
import { RoomMembersListComponent } from '../components/room-members-list/room-members-list.component';
import { RoomHeaderComponent } from '../components/room-header/room-header.component';
import { RoomTaskPanelComponent } from '../components/room-task-panel/room-task-panel.component';
import { EditRoomModalComponent } from '../components/edit-room-modal/edit-room-modal.component';
import { RoomDetailFacade } from '../services/room-state.service';
import { TimerFacade } from '../services/timer.facade';
import { TaskFacade } from '../services/task.facade';
import { Room, UpdateRoomDto } from '../models/rooms.models';

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
    EditRoomModalComponent,
  ],
  providers: [RoomDetailFacade, TimerFacade, TaskFacade],
  templateUrl: './room-detail.component.html',
  styleUrls: ['./room-detail.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RoomDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  /** Expose facades publicly so templates can bind to signals and methods directly. */
  readonly roomState = inject(RoomDetailFacade);
  readonly timer = inject(TimerFacade);
  readonly tasks = inject(TaskFacade);

  readonly XIcon = X;

  /** Room currently being edited — opens the shared EditRoomModalComponent when non-null. */
  readonly editingRoom = signal<Room | null>(null);

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      const id = parseInt(idParam, 10);
      if (!isNaN(id) && id > 0) {
        this.roomState.initialize(id);
        this.timer.initialize(id);
        this.tasks.fetchTasks();
      } else {
        this.router.navigate(['/404'], { replaceUrl: true });
      }
    } else {
      this.router.navigate(['/404'], { replaceUrl: true });
    }
  }

  // ── Edit Room (delegates through shared modal + facade) ────────────────────

  openEditRoomModal(): void {
    const r = this.roomState.room();
    if (!r) return;
    this.editingRoom.set({
      id: r.id.toString(),
      title: r.title,
      description: r.description,
      visibility: r.visibility,
      tags: [],
    });
  }

  onSaveRoom(event: { roomId: string; dto: UpdateRoomDto }): void {
    this.roomState.updateRoom(parseInt(event.roomId, 10), event.dto);
    this.editingRoom.set(null);
  }

  onCloseModal(): void {
    this.editingRoom.set(null);
  }

  // ── Task event handlers ────────────────────────────────────────────────────

  onNewTaskInput(event: Event): void {
    this.tasks.setNewTaskText((event.target as HTMLInputElement).value);
  }

  onNewTaskKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter') this.tasks.addTask();
  }

  onEditTaskInput(event: Event): void {
    this.tasks.setEditingTaskTitle((event.target as HTMLInputElement).value);
  }
}

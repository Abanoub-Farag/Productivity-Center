import { Component, ChangeDetectionStrategy, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  LucideAngularModule,
  Plus,
  CheckCircle2,
  Circle,
  ClipboardList,
  Trash2,
  Pencil,
  Check,
  X,
} from 'lucide-angular';
import { TaskData } from '../../models/rooms.models';

@Component({
  selector: 'app-room-task-panel',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule],
  templateUrl: './room-task-panel.component.html',
  styleUrls: ['./room-task-panel.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class RoomTaskPanelComponent {
  readonly PlusIcon = Plus;
  readonly CheckCircle2Icon = CheckCircle2;
  readonly CircleIcon = Circle;
  readonly CheckSquareIcon = ClipboardList;
  readonly Trash2Icon = Trash2;
  readonly PencilIcon = Pencil;
  readonly CheckIcon = Check;
  readonly XIcon = X;

  tasks = input<TaskData[]>([]);
  isTasksLoading = input<boolean>(false);
  tasksError = input<string | null>(null);
  taskUpdateError = input<string | null>(null);
  updatingTaskId = input<number | null>(null);
  
  newTaskText = input<string>('');
  editingTaskId = input<number | null>(null);
  editingTaskTitle = input<string>('');

  onAddTask = output<void>();
  onNewTaskInput = output<Event>();
  onNewTaskKeydown = output<KeyboardEvent>();
  
  onFetchTasks = output<void>();
  onToggleTask = output<TaskData>();
  onStartEditTask = output<TaskData>();
  onDeleteTask = output<number>();
  
  onEditTaskInput = output<Event>();
  onSaveTaskTitle = output<TaskData>();
  onCancelEditTask = output<void>();
}

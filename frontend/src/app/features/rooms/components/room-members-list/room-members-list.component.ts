import { Component, Input, OnInit, OnChanges, SimpleChanges, inject, signal, computed, effect, input, untracked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RoomMemberService } from '../../services/room-member.service';
import { RoomMember, MemberStatus, MemberRole } from '../../../../core/models/room-member.model';

@Component({
  selector: 'app-room-members-list',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './room-members-list.component.html',
  styleUrls: ['./room-members-list.component.scss'],
})
export class RoomMembersListComponent implements OnInit, OnChanges {
  @Input({ required: true }) roomId!: number | string;
  /**
   * Incremented by RoomDetailFacade on each successful heartbeat.
   * An `effect` reacts to changes and triggers a member re-fetch.
   * This replaces the former `@ViewChild`-based imperative call pattern.
   */
  readonly refreshTrigger = input<number>(0);

  private roomMemberService = inject(RoomMemberService);

  // State Signals
  members = signal<RoomMember[]>([]);
  isLoading = signal<boolean>(true);
  errorMessage = signal<string | null>(null);
  onlineCount = computed(() => this.members().filter((m) => m.status === MemberStatus.ONLINE).length);

  constructor() {
    effect(() => {
      // React to heartbeat ticks after the initial load (tick > 0).
      const tick = this.refreshTrigger();
      if (tick > 0) {
        untracked(() => this.fetchMembers());
      }
    }, { allowSignalWrites: true });
  }

  ngOnInit(): void {
    if (this.roomId != null) {
      this.fetchMembers();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['roomId'] && !changes['roomId'].firstChange) {
      this.fetchMembers();
    }
  }

  fetchMembers(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.roomMemberService.getRoomMembers(this.roomId).subscribe({
      next: (data) => {
        this.members.set(data);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.errorMessage.set(err.message || 'An unexpected error occurred while fetching members.');
        this.isLoading.set(false);
      },
    });
  }

  retry(): void {
    this.fetchMembers();
  }

  getInitials(firstName: string, lastName: string): string {
    const f = firstName ? firstName.charAt(0).toUpperCase() : '';
    const l = lastName ? lastName.charAt(0).toUpperCase() : '';
    return `${f}${l}` || 'U';
  }

  getStatusBadgeClass(status: MemberStatus | string): { dotClass: string; textClass: string; label: string } {
    switch (status) {
      case MemberStatus.ONLINE:
        return {
          dotClass: 'bg-emerald-500 ring-emerald-500/20',
          textClass: 'text-emerald-700 dark:text-emerald-400 bg-emerald-50 dark:bg-emerald-950/40 border-emerald-200 dark:border-emerald-800',
          label: 'Online',
        };
      case MemberStatus.AWAY:
        return {
          dotClass: 'bg-amber-500 ring-amber-500/20',
          textClass: 'text-amber-700 dark:text-amber-400 bg-amber-50 dark:bg-amber-950/40 border-amber-200 dark:border-amber-800',
          label: 'Away',
        };
      case MemberStatus.BUSY:
        return {
          dotClass: 'bg-rose-500 ring-rose-500/20',
          textClass: 'text-rose-700 dark:text-rose-400 bg-rose-50 dark:bg-rose-950/40 border-rose-200 dark:border-rose-800',
          label: 'Busy',
        };
      case MemberStatus.OFFLINE:
      default:
        return {
          dotClass: 'bg-slate-400 ring-slate-400/20',
          textClass: 'text-slate-600 dark:text-slate-400 bg-slate-100 dark:bg-slate-800 border-slate-200 dark:border-slate-700',
          label: status || 'Offline',
        };
    }
  }

  getRoleLabel(role?: MemberRole | string): string {
    if (!role) return 'Member';
    if (role === MemberRole.HOST || role === 'HOST') return 'Host';
    return 'Member';
  }
}

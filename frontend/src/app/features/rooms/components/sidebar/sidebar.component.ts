import { Component, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { 
  LucideAngularModule, 
  LayoutDashboard, 
  MonitorPlay, 
  Trophy, 
  Users, 
  Settings, 
  User as UserIcon,
  ChevronLeft,
  ChevronRight,
  LogOut
} from 'lucide-angular';
import { AuthService } from '../../../../core/services/auth.service';
import { RouterModule, Router } from '@angular/router';
import { formatUserName, extractInitials, loadSidebarCollapseState, saveSidebarCollapseState } from './sidebar.utils';

interface NavItem {
  label: string;
  icon: any;
  active?: boolean;
  route?: string;
}

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, LucideAngularModule, RouterModule],
  templateUrl: './sidebar.component.html',
  styleUrls: ['./sidebar.component.scss']
})
export class SidebarComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly ChevronLeftIcon = ChevronLeft;
  readonly ChevronRightIcon = ChevronRight;
  readonly LogOutIcon = LogOut;

  readonly isCollapsed = signal<boolean>(loadSidebarCollapseState());

  readonly userName = computed(() => formatUserName(this.authService.currentUser()));
  readonly userInitials = computed(() => extractInitials(this.userName()));

  readonly navItems: NavItem[] = [
    { label: 'Profile', icon: UserIcon, route: '/profile' },
    { label: 'Dashboard', icon: LayoutDashboard },
    { label: 'Rooms', icon: MonitorPlay, route: '/rooms' },
    { label: 'Leaderboard', icon: Trophy },
    { label: 'Communities', icon: Users },
    { label: 'Settings', icon: Settings },
  ];

  toggleCollapse(): void {
    this.isCollapsed.update(v => !v);
    saveSidebarCollapseState(this.isCollapsed());
  }

  logout(): void {
    this.authService.logout().subscribe(() => {
      this.router.navigate(['/login']);
    });
  }
}

import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { SidebarComponent } from '../../rooms/components/sidebar/sidebar.component';
import { TopNavComponent } from '../../rooms/components/top-nav/top-nav.component';
import { AnalyticsFacade } from '../services/analytics.facade';
import { DateRangeKey } from '../models/analytics.models';

@Component({
  selector: 'app-analytics-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    LucideAngularModule,
    SidebarComponent,
    TopNavComponent,
  ],
  templateUrl: './analytics-dashboard.component.html',
  styleUrls: ['./analytics-dashboard.component.scss'],
})
export class AnalyticsDashboardComponent implements OnInit {
  readonly f = inject(AnalyticsFacade);

  readonly ranges: DateRangeKey[] = ['7D', '30D', '90D'];

  ngOnInit(): void {
    this.f.load();
  }

  setRange(r: DateRangeKey): void {
    this.f.setRange(r);
  }

  trackByRoomId(_: number, item: { roomId: number }): number {
    return item.roomId;
  }

  trackByTaskId(_: number, item: { id: number }): number {
    return item.id;
  }
}

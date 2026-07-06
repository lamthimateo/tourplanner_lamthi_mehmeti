import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Stats } from '../services/api.service';
import { StatCardComponent } from './stat-card.component';
import { IconComponent, IconName } from './icon.component';

// Stats overview from GET /api/stats.
@Component({
  selector: 'app-stats-dashboard',
  standalone: true,
  imports: [CommonModule, StatCardComponent, IconComponent],
  template: `
    <section class="card card-hero">
      <div class="eyebrow">Statistics Dashboard</div>
      <h2 style="margin:6px 0 4px; font-size:28px;">Your expedition, at a glance.</h2>
      <p class="small" style="margin:0 0 18px;">Live aggregates across your tours and logs.</p>

      <div class="stats-grid">
        <app-stat-card [value]="stats.totalTours"                  label="Tours"     tone="primary"></app-stat-card>
        <app-stat-card [value]="stats.totalLogs"                   label="Logs"      tone="navy"></app-stat-card>
        <app-stat-card [value]="stats.totalDistanceKm + ' km'"     label="Distance"  tone="forest" sublabel="total covered"></app-stat-card>
        <app-stat-card [value]="stats.totalTimeHours + ' h'"       label="Time"      tone="gold"   sublabel="total elapsed"></app-stat-card>
        <app-stat-card [value]="stats.avgRating + '/10'"           label="Avg rating" tone="primary"></app-stat-card>
      </div>

      <div *ngIf="stats.byTransportType.length">
        <div class="eyebrow" style="margin-bottom:10px;">By transport type</div>
        <div class="transport-bars">
          <div class="bar-row" *ngFor="let s of stats.byTransportType">
            <div class="type">
              <app-icon [name]="transportIcon(s.transportType)" [size]="14"></app-icon>
              {{ s.transportType }}
            </div>
            <div class="bar-track" [attr.aria-label]="s.tourCount + ' tours'">
              <div class="bar-fill" [style.width.%]="barWidth(s.tourCount)"></div>
            </div>
            <div class="bar-value">
              {{ s.tourCount }} tour{{ s.tourCount === 1 ? '' : 's' }} ·
              avg {{ s.avgDistanceKm }} km ·
              ★ {{ s.avgRating }}
            </div>
          </div>
        </div>
      </div>

      <div *ngIf="!stats.totalTours" class="small" style="margin-top:10px; opacity:0.7;">
        Create tours and logs to populate the dashboard.
      </div>
    </section>
  `
})
export class StatsDashboardComponent {
  @Input() stats!: Stats;

  // Scale bar width relative to the busiest transport type.
  barWidth(count: number): number {
    if (!this.stats?.byTransportType?.length) return 0;
    const max = Math.max(...this.stats.byTransportType.map(s => s.tourCount), 1);
    return Math.round((count / max) * 100);
  }

  transportIcon(type: string | null | undefined): IconName {
    switch ((type || '').toLowerCase()) {
      case 'car':      return 'car';
      case 'bicycle':  return 'bicycle';
      case 'walking':  return 'walking';
      case 'running':  return 'running';
      case 'hiking':   return 'hiking';
      default:         return 'pin';
    }
  }
}

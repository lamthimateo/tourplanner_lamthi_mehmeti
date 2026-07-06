// Single metric box on the stats dashboard.
import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

export type StatTone = 'primary' | 'navy' | 'forest' | 'gold';

@Component({
  selector: 'app-stat-card',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="stat-box" [class.tone-primary]="tone === 'primary'"
                          [class.tone-navy]="tone === 'navy'"
                          [class.tone-forest]="tone === 'forest'"
                          [class.tone-gold]="tone === 'gold'">
      <div class="label">{{ label }}</div>
      <div class="stat-num">{{ value }}</div>
      <div class="stat-sub" *ngIf="sublabel">{{ sublabel }}</div>
    </div>
  `
})
export class StatCardComponent {
  @Input() value: string | number = '';
  @Input() label: string = '';
  @Input() sublabel?: string;
  @Input() tone: StatTone = 'primary';
}

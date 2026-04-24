/**
 * StatCardComponent — a reusable display card for a single statistic.
 *
 * Used in the Statistics Dashboard to show one metric (e.g. "42 Tours").
 * Demonstrates the "reusable UI component" pattern required by the project spec.
 *
 * Supports four accent tones (primary / navy / forest / gold) so the dashboard
 * can read as a varied, editorial grid rather than a flat list of numbers.
 *
 * @example
 * <app-stat-card [value]="stats.totalTours" label="Tours" tone="primary"
 *                sublabel="across 3 months"></app-stat-card>
 */
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
  /** The numeric or string value to display prominently. */
  @Input() value: string | number = '';
  /** Short uppercase label shown above the value (e.g. "Tours"). */
  @Input() label: string = '';
  /** Optional smaller caption shown below the value. */
  @Input() sublabel?: string;
  /** Visual accent tone. */
  @Input() tone: StatTone = 'primary';
}

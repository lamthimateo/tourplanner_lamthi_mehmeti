/**
 * IconComponent — a single inline-SVG icon set used throughout the UI.
 *
 * Icons inherit `currentColor`, so wrappers style them via regular CSS
 * (e.g. `.stars svg { color: var(--accent-4); }`). The icons are drawn
 * from a small Lucide-style set to keep the visual language consistent.
 *
 * @example
 * <app-icon name="compass" [size]="24"></app-icon>
 * <app-icon name="bicycle"></app-icon>
 */
import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

export type IconName =
  | 'compass' | 'compass-large'
  | 'search' | 'plus' | 'close' | 'check'
  | 'chart' | 'upload' | 'download' | 'trash' | 'edit' | 'camera'
  | 'pin' | 'arrow-right' | 'route'
  | 'sun' | 'moon'
  | 'star' | 'alert'
  | 'car' | 'bicycle' | 'walking' | 'running' | 'hiking';

@Component({
  selector: 'app-icon',
  standalone: true,
  imports: [CommonModule],
  template: `
    <svg xmlns="http://www.w3.org/2000/svg"
         [attr.width]="size" [attr.height]="size"
         viewBox="0 0 24 24" fill="none" stroke="currentColor"
         [attr.stroke-width]="strokeWidth"
         stroke-linecap="round" stroke-linejoin="round"
         aria-hidden="true" focusable="false">
      <ng-container [ngSwitch]="name">

        <g *ngSwitchCase="'compass'">
          <circle cx="12" cy="12" r="10"/>
          <polygon points="16.24 7.76 14.12 14.12 7.76 16.24 9.88 9.88 16.24 7.76" fill="currentColor" fill-opacity="0.18"/>
        </g>

        <g *ngSwitchCase="'compass-large'">
          <circle cx="12" cy="12" r="10"/>
          <circle cx="12" cy="12" r="6"/>
          <polygon points="12 4 13.5 11 12 20 10.5 11 12 4" fill="currentColor" fill-opacity="0.25"/>
          <polygon points="4 12 11 10.5 20 12 11 13.5 4 12" fill="currentColor" fill-opacity="0.12"/>
          <circle cx="12" cy="12" r="1.2" fill="currentColor"/>
        </g>

        <g *ngSwitchCase="'search'">
          <circle cx="11" cy="11" r="7"/>
          <path d="m20 20-3.5-3.5"/>
        </g>

        <g *ngSwitchCase="'plus'">
          <path d="M12 5v14M5 12h14"/>
        </g>

        <g *ngSwitchCase="'close'">
          <path d="M18 6 6 18M6 6l12 12"/>
        </g>

        <g *ngSwitchCase="'check'">
          <path d="M20 6 9 17l-5-5"/>
        </g>

        <g *ngSwitchCase="'chart'">
          <path d="M3 3v18h18"/>
          <path d="M7 15v-4M11 15V8M15 15v-6M19 15v-2"/>
        </g>

        <g *ngSwitchCase="'upload'">
          <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
          <polyline points="17 8 12 3 7 8"/>
          <line x1="12" y1="3" x2="12" y2="15"/>
        </g>

        <g *ngSwitchCase="'download'">
          <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
          <polyline points="7 10 12 15 17 10"/>
          <line x1="12" y1="15" x2="12" y2="3"/>
        </g>

        <g *ngSwitchCase="'trash'">
          <path d="M3 6h18"/>
          <path d="M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
          <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6"/>
        </g>

        <g *ngSwitchCase="'edit'">
          <path d="M17 3a2.828 2.828 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5Z"/>
        </g>

        <g *ngSwitchCase="'camera'">
          <path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"/>
          <circle cx="12" cy="13" r="4"/>
        </g>

        <g *ngSwitchCase="'pin'">
          <path d="M20 10c0 6-8 12-8 12s-8-6-8-12a8 8 0 0 1 16 0Z"/>
          <circle cx="12" cy="10" r="3"/>
        </g>

        <g *ngSwitchCase="'arrow-right'">
          <line x1="5" y1="12" x2="19" y2="12"/>
          <polyline points="12 5 19 12 12 19"/>
        </g>

        <g *ngSwitchCase="'route'">
          <circle cx="6" cy="19" r="3"/>
          <circle cx="18" cy="5" r="3"/>
          <path d="M6 16V10a4 4 0 0 1 4-4h4a4 4 0 0 1 4 4v4"/>
        </g>

        <g *ngSwitchCase="'sun'">
          <circle cx="12" cy="12" r="4"/>
          <path d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M6.34 17.66l-1.41 1.41M19.07 4.93l-1.41 1.41"/>
        </g>

        <g *ngSwitchCase="'moon'">
          <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79Z"/>
        </g>

        <g *ngSwitchCase="'star'">
          <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2" fill="currentColor"/>
        </g>

        <g *ngSwitchCase="'alert'">
          <circle cx="12" cy="12" r="10"/>
          <line x1="12" y1="8" x2="12" y2="12"/>
          <line x1="12" y1="16" x2="12.01" y2="16"/>
        </g>

        <g *ngSwitchCase="'car'">
          <path d="M5 17h14a1 1 0 0 0 1-1v-4l-2-5a2 2 0 0 0-2-1H8a2 2 0 0 0-2 1l-2 5v4a1 1 0 0 0 1 1Z"/>
          <circle cx="7.5" cy="17.5" r="1.5"/>
          <circle cx="16.5" cy="17.5" r="1.5"/>
          <path d="M6 12h12"/>
        </g>

        <g *ngSwitchCase="'bicycle'">
          <circle cx="5.5" cy="17.5" r="3.5"/>
          <circle cx="18.5" cy="17.5" r="3.5"/>
          <path d="m15 6-3 6-6-1 6 5h4l-1-10"/>
          <circle cx="15" cy="5" r="1" fill="currentColor"/>
        </g>

        <g *ngSwitchCase="'walking'">
          <circle cx="13" cy="4" r="2" fill="currentColor"/>
          <path d="m9 20 2-5 3-1 2 3 3 1"/>
          <path d="M7 10h3l2-2 3 3-2 4"/>
        </g>

        <g *ngSwitchCase="'running'">
          <circle cx="17" cy="4" r="2" fill="currentColor"/>
          <path d="m5 20 3-6 4-2 3 4 3 1"/>
          <path d="M10 9h3l3-3 2 5-3 3"/>
        </g>

        <g *ngSwitchCase="'hiking'">
          <circle cx="10" cy="4" r="2" fill="currentColor"/>
          <path d="M6 21v-5l2-4 2 3 2-1 1 3"/>
          <path d="m15 9 2 2v6M19 21l-2-4"/>
          <path d="M13 15V9"/>
        </g>

      </ng-container>
    </svg>
  `
})
export class IconComponent {
  @Input() name!: IconName;
  @Input() size: number | string = 16;
  @Input() strokeWidth: number | string = 1.8;
}

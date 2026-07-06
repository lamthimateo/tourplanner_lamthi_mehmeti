// Left sidebar: tour list, search, import, theme toggle.
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Tour } from '../models/tour';
import { IconComponent, IconName } from './icon.component';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, FormsModule, IconComponent],
  template: `
    <aside class="sidebar">

      <div class="brand">
        <div class="brand-left">
          <div class="brand-title">
            <b>Tourplanner</b>
            <span>routes · logs · reports</span>
          </div>
        </div>
      </div>

      <div class="topbar">
        <div class="user">
          <span class="avatar" aria-hidden="true">{{ initial(username) }}</span>
          <span>
            <b>{{ username }}</b><br/>
            <span class="small">expedition member</span>
          </span>
        </div>
        <div class="topbar-actions">
          <button class="btn-icon btn-ghost" type="button" (click)="toggleTheme.emit()"
                  [attr.aria-label]="isDark ? 'Switch to light theme' : 'Switch to dark theme'"
                  [title]="isDark ? 'Light mode' : 'Dark mode'">
            <app-icon [name]="isDark ? 'sun' : 'moon'" [size]="16"></app-icon>
          </button>
          <button class="small-btn btn-ghost" (click)="logout.emit()" title="Sign out">Logout</button>
        </div>
      </div>

      <div class="search-wrap">
        <app-icon name="search" [size]="16"></app-icon>
        <input [ngModel]="search" (ngModelChange)="searchChange.emit($event)"
               placeholder="Search tours, logs, places…" />
      </div>

      <div class="actions">
        <button class="btn-primary" (click)="newTour.emit()" title="New tour">
          <app-icon name="plus" [size]="15"></app-icon> New
        </button>
        <button class="btn-ghost" (click)="toggleStats.emit()" title="Statistics dashboard">
          <app-icon name="chart" [size]="15"></app-icon> {{ showStats ? 'Hide' : 'Stats' }}
        </button>
        <label style="display:block;">
          <input type="file" accept=".json" style="display:none" #importInput (change)="onImportChange($event)" />
          <button class="btn-ghost" type="button" (click)="importInput.click()" [disabled]="busy"
                  title="Import tour from JSON" style="width:100%;">
            <app-icon name="upload" [size]="15"></app-icon> Import
          </button>
        </label>
      </div>

      <div class="eyebrow" style="padding: 4px 4px 0;">
        Expedition Ledger · {{ filteredTours.length }}{{ search.trim() ? ' of ' + totalCount : '' }}
      </div>

      <div class="tour-list">
        <div class="tour-entry"
             [class.active]="t.id === selectedTourId"
             *ngFor="let t of filteredTours"
             (click)="tourSelected.emit(t)">
          <div class="entry-title">
            <app-icon name="pin" [size]="14"></app-icon>
            {{ t.name }}
          </div>
          <div class="entry-route">
            <span>{{ t.origin }}</span>
            <span class="arrow">→</span>
            <span>{{ t.destination }}</span>
          </div>
          <div class="entry-meta">
            <span class="chip accent">
              <app-icon [name]="transportIcon(t.transportType)" [size]="11"></app-icon>
              {{ t.transportType }}
            </span>
            <span class="chip mono">{{ t.distance }} km</span>
            <span class="chip mono">{{ t.estimatedTime }} min</span>
            <span class="chip accent-3" *ngIf="t.popularity != null" title="Popularity = # logs">
              <app-icon name="star" [size]="10"></app-icon> {{ t.popularity }}
            </span>
          </div>
        </div>
        <div *ngIf="!filteredTours.length" class="small" style="padding: 8px; text-align:center; opacity:0.7;">
          {{ search.trim() ? 'No tours match your search.' : 'No tours yet. Create your first one.' }}
        </div>
      </div>

    </aside>
  `
})
export class SidebarComponent {
  @Input() filteredTours: Tour[] = [];
  @Input() totalCount = 0;
  @Input() search = '';
  @Input() busy = false;
  @Input() showStats = false;
  @Input() username: string | null = null;
  @Input() isDark = false;
  @Input() selectedTourId: number | null | undefined = null;

  @Output() searchChange = new EventEmitter<string>();
  @Output() tourSelected = new EventEmitter<Tour>();
  @Output() newTour = new EventEmitter<void>();
  @Output() toggleStats = new EventEmitter<void>();
  @Output() importFile = new EventEmitter<File>();
  @Output() toggleTheme = new EventEmitter<void>();
  @Output() logout = new EventEmitter<void>();

  onImportChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files?.length) return;
    this.importFile.emit(input.files[0]);
    input.value = '';
  }

  initial(name: string | null | undefined): string {
    if (!name) return '?';
    return name.trim().charAt(0).toUpperCase() || '?';
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

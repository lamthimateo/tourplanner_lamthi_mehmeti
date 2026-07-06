// Root shell: login gate, sidebar, stats, tour workspace.
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService, Stats } from './services/api.service';
import { AuthService } from './services/auth.service';
import { Tour } from './models/tour';
import { TourViewModel } from './viewmodel/tour.viewmodel';
import { LoginComponent } from './components/login.component';
import { SidebarComponent } from './components/sidebar.component';
import { StatsDashboardComponent } from './components/stats-dashboard.component';
import { TourWorkspaceComponent } from './components/tour-workspace.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, LoginComponent, SidebarComponent, StatsDashboardComponent, TourWorkspaceComponent],
  template: `
    <app-login *ngIf="!auth.isLoggedIn()" (loggedIn)="onLoggedIn()"></app-login>

    <div *ngIf="auth.isLoggedIn()" class="container">

      <app-sidebar
        [filteredTours]="filteredTours"
        [totalCount]="tours.length"
        [search]="search"
        [busy]="busy"
        [showStats]="showStats"
        [username]="auth.getUsername()"
        [isDark]="isDark"
        [selectedTourId]="selectedTour?.id"
        (searchChange)="onSearch($event)"
        (tourSelected)="selectTour($event)"
        (newTour)="newTour()"
        (toggleStats)="toggleStats()"
        (importFile)="importTour($event)"
        (toggleTheme)="toggleTheme()"
        (logout)="logout()">
      </app-sidebar>

      <main class="main">

        <div class="error-banner" *ngIf="importError">
          <span>{{ importError }}</span>
          <button class="small-btn btn-ghost" (click)="importError = null" style="margin-left:auto;">Dismiss</button>
        </div>

        <app-stats-dashboard *ngIf="showStats && stats" [stats]="stats!"></app-stats-dashboard>

        <div *ngIf="!selectedTour && !showStats" class="empty-state">
          <div>
            <div class="empty-title">Chart your first tour.</div>
            <div class="empty-body">
              Pick a tour from the ledger on the left — or create a new one to plan a route,
              attach field logs, and generate an illustrated PDF report.
            </div>
          </div>
          <button class="btn-primary" (click)="newTour()">New tour</button>
        </div>

        <app-tour-workspace
          *ngIf="selectedTour"
          [tour]="selectedTour!"
          (tourSaved)="onTourSaved($event)"
          (tourDeleted)="onTourDeleted()"
          (tourListChanged)="reloadTours(true)">
        </app-tour-workspace>

      </main>
    </div>
  `
})
export class AppComponent implements OnInit {

  tours: Tour[] = [];
  filteredTours: Tour[] = [];
  selectedTour: TourViewModel | null = null;
  stats: Stats | null = null;
  showStats = false;
  search = '';
  busy = false;
  isDark = false;
  importError: string | null = null;

  constructor(private api: ApiService, public auth: AuthService) {}

  ngOnInit(): void {
    this.initTheme();
    if (this.auth.isLoggedIn()) this.reloadTours();
  }

  onLoggedIn(): void { this.reloadTours(); }

  logout(): void {
    this.auth.logout();
    this.tours = []; this.filteredTours = []; this.selectedTour = null; this.stats = null;
  }

  reloadTours(refreshSelected = false): void {
    const selectedId = refreshSelected ? this.selectedTour?.id : undefined;
    this.api.getTours().subscribe({
      next: (tours) => {
        this.tours = tours;
        this.applyFilter();
        if (selectedId != null) {
          const updated = tours.find(t => t.id === selectedId);
          if (updated) this.selectedTour = TourViewModel.from(updated);
        }
        if (this.showStats) {
          this.api.getStats().subscribe({ next: (s) => this.stats = s });
        }
      },
      error: (err) => { if (err.status === 401 || err.status === 403) this.auth.logout(); }
    });
  }

  onSearch(q: string): void {
    this.search = q;
    if (!q.trim()) { this.filteredTours = this.tours; return; }
    // Try backend search first; fall back to client filter on error.
    this.api.searchTours(q.trim()).subscribe({
      next: (ids) => {
        const idSet = new Set(ids);
        this.filteredTours = this.tours.filter(t => t.id != null && idSet.has(t.id));
      },
      error: () => this.applyClientFilter(q)
    });
  }

  private applyClientFilter(q: string): void {
    const lower = q.toLowerCase();
    this.filteredTours = this.tours.filter(t =>
      (t.name ?? '').toLowerCase().includes(lower) ||
      (t.description ?? '').toLowerCase().includes(lower) ||
      (t.origin ?? '').toLowerCase().includes(lower) ||
      (t.destination ?? '').toLowerCase().includes(lower)
    );
  }

  private applyFilter(): void {
    if (!this.search.trim()) { this.filteredTours = this.tours; return; }
    this.applyClientFilter(this.search);
  }

  selectTour(tour: Tour): void {
    this.selectedTour = TourViewModel.from(tour);
    this.showStats = false;
  }

  newTour(): void {
    this.selectedTour = new TourViewModel();
    this.showStats = false;
  }

  onTourSaved(saved: Tour): void {
    this.selectedTour = TourViewModel.from(saved);
    this.reloadTours();
  }

  onTourDeleted(): void {
    this.selectedTour = null;
    this.reloadTours();
  }

  toggleStats(): void {
    this.showStats = !this.showStats;
    if (this.showStats) this.api.getStats().subscribe({ next: (s) => this.stats = s });
  }

  importTour(file: File): void {
    this.importError = null;
    const reader = new FileReader();
    reader.onload = () => {
      // Parse JSON locally, then send to backend for import.
      let data: unknown;
      try {
        data = JSON.parse(reader.result as string);
      } catch {
        this.importError = `"${file.name}" is not valid JSON — export a tour first and import that file.`;
        return;
      }
      this.busy = true;
      this.api.importTour(data).subscribe({
        next: (imported) => { this.busy = false; this.reloadTours(); this.selectTour(imported); },
        error: (err) => {
          this.busy = false;
          this.importError = err?.error?.message || 'Import failed — the file does not look like a tour export.';
        }
      });
    };
    reader.readAsText(file);
  }

  private initTheme(): void {
    try {
      const stored = localStorage.getItem('tourplanner.theme');
      if (stored === 'dark' || stored === 'light') {
        this.isDark = stored === 'dark';
      } else if (window.matchMedia) {
        this.isDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
      }
    } catch {}
    this.applyTheme();
  }

  toggleTheme(): void {
    this.isDark = !this.isDark;
    this.applyTheme();
    try { localStorage.setItem('tourplanner.theme', this.isDark ? 'dark' : 'light'); } catch {}
  }

  private applyTheme(): void {
    if (this.isDark) document.documentElement.setAttribute('data-theme', 'dark');
    else document.documentElement.removeAttribute('data-theme');
  }
}

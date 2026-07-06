// Edit one tour: form, map, logs, reports.
import {
  AfterViewInit, Component, ElementRef, EventEmitter,
  Input, OnChanges, OnDestroy, Output, SimpleChanges, ViewChild
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import * as L from 'leaflet';
import { ApiService } from '../services/api.service';
import { Tour } from '../models/tour';
import { TourLog } from '../models/tour-log';
import { TourViewModel } from '../viewmodel/tour.viewmodel';
import { TourLogViewModel } from '../viewmodel/tour-log.viewmodel';
import { IconComponent, IconName } from './icon.component';

@Component({
  selector: 'app-tour-workspace',
  standalone: true,
  imports: [CommonModule, FormsModule, IconComponent],
  template: `
    <!-- Hero header -->
    <section class="card card-hero tour-header">
      <div class="eyebrow">{{ tour.id ? 'Tour #' + tour.id : 'New tour' }}</div>
      <h1>{{ tour.name || 'Untitled expedition' }}</h1>
      <div class="route">
        <app-icon name="pin" [size]="13"></app-icon>
        <span>{{ tour.origin || '—' }}</span>
        <span class="arrow"><app-icon name="arrow-right" [size]="14"></app-icon></span>
        <span>{{ tour.destination || '—' }}</span>
      </div>
      <div class="meta-row">
        <span class="chip accent" *ngIf="tour.transportType">
          <app-icon [name]="transportIcon(tour.transportType)" [size]="12"></app-icon>
          {{ tour.transportType }}
        </span>
        <span class="chip mono"><b>{{ tour.distance || 0 }}</b>&nbsp;km</span>
        <span class="chip mono"><b>{{ tour.estimatedTime || 0 }}</b>&nbsp;min</span>
        <span class="chip accent-3" *ngIf="tour.popularity != null" title="Popularity = # logs">
          <app-icon name="star" [size]="11"></app-icon> pop {{ tour.popularity }}
        </span>
        <span class="chip accent-2" *ngIf="tour.childFriendliness != null">
          child-friendly {{ tour.childFriendliness }}/10
        </span>
      </div>
    </section>

    <div class="error-banner" *ngIf="error">
      <app-icon name="alert" [size]="16"></app-icon>
      <span>{{ error }}</span>
    </div>

    <!-- Tour details form -->
    <section class="card">
      <div class="eyebrow">Details</div>
      <div class="row" style="margin-top:10px;">

        <div class="row two">
          <div>
            <label>Name <span style="color:var(--accent);">*</span></label>
            <input [(ngModel)]="tour.name"
                   placeholder="e.g. Vienna ring tour"
                   [class.invalid]="tour.submitted && !tour.name.trim()" />
            <div class="validation-msg" *ngIf="tour.submitted && !tour.name.trim()">Name is required</div>
          </div>
          <div>
            <label>Transport <span style="color:var(--accent);">*</span></label>
            <select [(ngModel)]="tour.transportType"
                    [class.invalid]="tour.submitted && !tour.transportType.trim()">
              <option value="car">Car</option>
              <option value="bicycle">Bicycle</option>
              <option value="walking">Walking</option>
              <option value="running">Running</option>
              <option value="hiking">Hiking</option>
            </select>
            <div class="validation-msg" *ngIf="tour.submitted && !tour.transportType.trim()">Transport type is required</div>
          </div>
        </div>

        <div>
          <label>Description</label>
          <textarea rows="3" [(ngModel)]="tour.description" maxlength="2000"
                    placeholder="What makes this tour memorable?"></textarea>
        </div>

        <div class="row two">
          <div style="position:relative;">
            <label>From <span style="color:var(--accent);">*</span></label>
            <input [(ngModel)]="tour.origin"
                   (ngModelChange)="onOriginChange()"
                   (blur)="showOriginSug = false"
                   autocomplete="off"
                   placeholder="Start city"
                   [class.invalid]="tour.submitted && !tour.origin.trim()" />
            <div class="validation-msg" *ngIf="tour.submitted && !tour.origin.trim()">Origin is required</div>
            <div class="suggestions" *ngIf="showOriginSug && originSuggestions.length">
              <div class="suggestion-item" *ngFor="let c of originSuggestions"
                   (mousedown)="pickOrigin(c)">
                <app-icon name="pin" [size]="13"></app-icon> {{ c }}
              </div>
            </div>
          </div>
          <div style="position:relative;">
            <label>To <span style="color:var(--accent);">*</span></label>
            <input [(ngModel)]="tour.destination"
                   (ngModelChange)="onDestChange()"
                   (blur)="showDestSug = false"
                   autocomplete="off"
                   placeholder="Destination city"
                   [class.invalid]="tour.submitted && !tour.destination.trim()" />
            <div class="validation-msg" *ngIf="tour.submitted && !tour.destination.trim()">Destination is required</div>
            <div class="suggestions" *ngIf="showDestSug && destSuggestions.length">
              <div class="suggestion-item" *ngFor="let c of destSuggestions"
                   (mousedown)="pickDest(c)">
                <app-icon name="pin" [size]="13"></app-icon> {{ c }}
              </div>
            </div>
          </div>
        </div>

        <div class="row two">
          <div>
            <label>Distance (km)</label>
            <input type="number" [(ngModel)]="tour.distance" min="0" />
          </div>
          <div>
            <label>Estimated time (min)</label>
            <input type="number" [(ngModel)]="tour.estimatedTime" min="0" />
          </div>
        </div>

        <div class="btn-row" style="margin-top:4px;">
          <button class="btn-primary" (click)="saveTour()" [disabled]="busy">
            <app-icon name="check" [size]="15"></app-icon> Save
          </button>
          <button class="btn-ghost" (click)="calculateRoute()" [disabled]="busy">
            <app-icon name="route" [size]="15"></app-icon> Calculate route
          </button>
          <button class="btn-ghost" (click)="exportTour()" [disabled]="busy || !tour.id" title="Export tour + logs as JSON">
            <app-icon name="download" [size]="15"></app-icon> Export JSON
          </button>
          <button class="btn-ghost" (click)="downloadTourReport()" [disabled]="busy || !tour.id">
            <app-icon name="download" [size]="15"></app-icon> Tour PDF
          </button>
          <button class="btn-ghost" (click)="downloadSummaryReport()" [disabled]="busy">
            <app-icon name="download" [size]="15"></app-icon> Summary PDF
          </button>
          <button class="btn-danger" (click)="deleteTour()" [disabled]="busy || !tour.id">
            <app-icon name="trash" [size]="15"></app-icon> Delete
          </button>
        </div>

        <div class="small" *ngIf="!tour.id" style="opacity:0.65;">
          Save the tour first to enable export, reports and image upload.
        </div>

        <!-- Tour image upload and display -->
        <div *ngIf="tour.id" style="margin-top:4px;">
          <label>Tour image</label>
          <input type="file" accept="image/*" style="display:none" #imageInput (change)="uploadImage($event)" />
          <button (click)="imageInput.click()" [disabled]="busy" class="btn-ghost">
            <app-icon name="camera" [size]="15"></app-icon>
            {{ tourImageSafeUrl ? 'Replace image' : 'Upload image' }}
          </button>
          <div *ngIf="tourImageSafeUrl" class="image-preview">
            <img [src]="tourImageSafeUrl" alt="Tour image" />
          </div>
        </div>
      </div>
    </section>

    <!-- Map -->
    <section class="card map-card">
      <div style="display:flex;justify-content:space-between;align-items:center;gap:10px;">
        <div>
          <div class="eyebrow">Route</div>
          <h3 style="margin:2px 0 0;">Interactive map</h3>
        </div>
        <button class="btn-ghost" (click)="calculateRoute()" [disabled]="busy">
          <app-icon name="route" [size]="15"></app-icon> Recalculate
        </button>
      </div>
      <div #mapEl class="map-el" style="margin-top:12px;"></div>
      <div class="map-foot">
        <span>© OpenStreetMap · routing by OpenRouteService</span>
        <span class="mono" *ngIf="tour.distance">
          {{ tour.distance }} km · {{ tour.estimatedTime }} min
        </span>
      </div>
    </section>

    <!-- Logs header -->
    <section class="card">
      <div style="display:flex;justify-content:space-between;align-items:center;gap:10px;">
        <div>
          <div class="eyebrow">Field logs</div>
          <h3 style="margin:2px 0 0;">
            {{ logs.length }} entr{{ logs.length === 1 ? 'y' : 'ies' }}
          </h3>
        </div>
        <button class="btn-primary" (click)="newLog()">
          <app-icon name="plus" [size]="15"></app-icon> New log
        </button>
      </div>
    </section>

    <!-- Log form (edit/create) -->
    <section class="card" *ngIf="editingLog">
      <div class="eyebrow">{{ editingLog.id ? 'Edit log #' + editingLog.id : 'New log entry' }}</div>
      <div class="row" style="margin-top:10px;">
        <div class="row two">
          <div>
            <label>Date <span style="color:var(--accent);">*</span></label>
            <input type="date" [(ngModel)]="editingLog.logDate"
                   [class.invalid]="editingLog.submitted && !editingLog.logDate" />
            <div class="validation-msg" *ngIf="editingLog.submitted && !editingLog.logDate">Date is required</div>
          </div>
          <div>
            <label>Rating (0–10) <span style="color:var(--accent);">*</span></label>
            <input type="number" [(ngModel)]="editingLog.rating" min="0" max="10"
                   [class.invalid]="editingLog.submitted && (editingLog.rating < 0 || editingLog.rating > 10)" />
            <div class="validation-msg" *ngIf="editingLog.submitted && (editingLog.rating < 0 || editingLog.rating > 10)">Rating must be 0–10</div>
          </div>
        </div>
        <div>
          <label>Comment <span style="color:var(--accent);">*</span></label>
          <textarea rows="3" [(ngModel)]="editingLog.comment" maxlength="2000"
                    placeholder="How did it go? Weather, highlights, hiccups…"
                    [class.invalid]="editingLog.submitted && !editingLog.comment.trim()"></textarea>
          <div class="validation-msg" *ngIf="editingLog.submitted && !editingLog.comment.trim()">Comment is required</div>
        </div>
        <div>
          <label>Details (optional)</label>
          <textarea rows="2" [(ngModel)]="editingLog.logDetails" maxlength="4000"
                    placeholder="Extended notes: gear, conditions, anything worth remembering…"></textarea>
        </div>
        <div class="row three">
          <div>
            <label>Difficulty (1–5) <span style="color:var(--accent);">*</span></label>
            <input type="number" [(ngModel)]="editingLog.difficulty" min="1" max="5"
                   [class.invalid]="editingLog.submitted && (editingLog.difficulty < 1 || editingLog.difficulty > 5)" />
            <div class="validation-msg" *ngIf="editingLog.submitted && (editingLog.difficulty < 1 || editingLog.difficulty > 5)">1–5</div>
          </div>
          <div>
            <label>Total time (min)</label>
            <input type="number" [(ngModel)]="editingLog.totalTimeMinutes" min="0" />
          </div>
          <div>
            <label>Total distance (km)</label>
            <input type="number" [(ngModel)]="editingLog.totalDistance" min="0" />
          </div>
        </div>
        <div class="btn-row">
          <button class="btn-primary" (click)="saveLog()" [disabled]="busy">
            <app-icon name="check" [size]="15"></app-icon> Save log
          </button>
          <button class="btn-ghost" (click)="editingLog = null" [disabled]="busy">
            <app-icon name="close" [size]="15"></app-icon> Cancel
          </button>
        </div>
      </div>
    </section>

    <!-- Log list -->
    <div class="logs-list">
      <article class="log-card" *ngFor="let l of logs" (click)="editLog(l)">
        <div class="log-head">
          <div style="min-width:0;">
            <div class="log-date">{{ l.logDate }}</div>
            <div class="log-comment">"{{ l.comment }}"</div>
            <div class="small" *ngIf="l.logDetails" style="opacity:0.7; margin-top:2px;">{{ l.logDetails }}</div>
          </div>
          <div class="stars" [attr.title]="'Rating ' + l.rating + '/10'">
            <app-icon name="star" [size]="14" *ngFor="let filled of starStates(l.rating)"
                      [class.empty]="!filled"></app-icon>
          </div>
        </div>
        <div class="log-meta">
          <span class="chip mono">{{ l.totalDistance }} km</span>
          <span class="chip mono">{{ l.totalTimeMinutes }} min</span>
          <span class="chip">
            <span class="difficulty-dots" [attr.aria-label]="'Difficulty ' + l.difficulty + '/5'">
              <i *ngFor="let n of [1,2,3,4,5]" [class.on]="n <= l.difficulty"></i>
            </span>
            difficulty
          </span>
        </div>
        <div class="log-actions">
          <button class="small-btn btn-danger" (click)="deleteLog(l); $event.stopPropagation();" [disabled]="busy">
            <app-icon name="trash" [size]="13"></app-icon> Delete
          </button>
        </div>
      </article>
      <div *ngIf="!logs.length && !editingLog" class="small" style="text-align:center; padding: 20px; opacity:0.7;">
        No logs yet. Attach one to remember how this tour actually went.
      </div>
    </div>
  `
})
export class TourWorkspaceComponent implements AfterViewInit, OnChanges, OnDestroy {

  @Input() tour!: TourViewModel;

  @Output() tourSaved = new EventEmitter<Tour>();
  @Output() tourDeleted = new EventEmitter<void>();
  @Output() tourListChanged = new EventEmitter<void>();

  logs: TourLog[] = [];
  editingLog: TourLogViewModel | null = null;
  tourImageSafeUrl: SafeUrl | null = null;
  private tourImageObjectUrl: string | null = null;

  originSuggestions: string[] = [];
  destSuggestions: string[] = [];
  showOriginSug = false;
  showDestSug = false;
  private suggestDebounce: ReturnType<typeof setTimeout> | null = null;

  @ViewChild('mapEl') mapEl?: ElementRef<HTMLDivElement>;
  private map: L.Map | null = null;
  private routeLayer: L.Polyline | null = null;
  private markerFrom: L.CircleMarker | null = null;
  private markerTo: L.CircleMarker | null = null;

  busy = false;
  error: string | null = null;

  constructor(private api: ApiService, private sanitizer: DomSanitizer) {}

  private readonly onWindowResize = (): void => {
    this.map?.invalidateSize();
  };

  ngAfterViewInit(): void {
    window.addEventListener('resize', this.onWindowResize);
    setTimeout(() => this.ensureMap(), 0);
    this.loadLogs();
    if (this.tour?.imagePath) this.loadTourImage(this.tour.id!);
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (!changes['tour'] || changes['tour'].firstChange) return;
    const prev = changes['tour'].previousValue as TourViewModel | undefined;
    const curr = changes['tour'].currentValue as TourViewModel;
    if (prev?.id !== curr?.id) {
      this.editingLog = null;
      this.error = null;
      this.showOriginSug = false;
      this.showDestSug = false;
      this.clearRoute();
      this.loadLogs();
      if (curr?.imagePath) {
        this.loadTourImage(curr.id!);
      } else {
        this.revokeImageUrl();
      }
      setTimeout(() => this.ensureMap(), 0);
    }
  }

  ngOnDestroy(): void {
    window.removeEventListener('resize', this.onWindowResize);
    if (this.map) { this.map.remove(); this.map = null; }
    this.revokeImageUrl();
  }

  // --- Tour ---

  saveTour(): void {
    this.tour.submitted = true;
    this.error = null;
    if (!this.tour.isValid()) { this.error = 'Please fill in all required fields.'; return; }
    this.busy = true;
    const payload = this.tour.toTour();
    const req = payload.id
      ? this.api.updateTour(payload.id, payload)
      : this.api.createTour(payload);
    req.subscribe({
      next: (saved) => {
        this.busy = false;
        this.tourSaved.emit(saved);
        if (saved.imagePath) this.loadTourImage(saved.id!);
        this.loadLogs();
      },
      error: (err) => { this.busy = false; this.error = this.fmt(err); }
    });
  }

  deleteTour(): void {
    if (!this.tour.id) return;
    this.busy = true;
    this.api.deleteTour(this.tour.id).subscribe({
      next: () => { this.busy = false; this.tourDeleted.emit(); },
      error: (err) => { this.busy = false; this.error = this.fmt(err); }
    });
  }

  exportTour(): void {
    if (!this.tour.id) return;
    this.busy = true;
    this.api.exportTour(this.tour.id).subscribe({
      next: (data) => {
        this.busy = false;
        this.downloadBlob(
          new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' }),
          `tour_${this.tour.id}.json`
        );
      },
      error: (err) => { this.busy = false; this.error = this.fmt(err); }
    });
  }

  downloadTourReport(): void {
    if (!this.tour.id) return;
    this.busy = true;
    this.api.downloadTourReport(this.tour.id).subscribe({
      next: (blob) => { this.busy = false; this.downloadBlob(blob, `TourReport_${this.tour.id}.pdf`); },
      error: (err) => { this.busy = false; this.error = this.fmt(err); }
    });
  }

  downloadSummaryReport(): void {
    this.busy = true;
    this.api.downloadSummaryReport().subscribe({
      next: (blob) => { this.busy = false; this.downloadBlob(blob, 'SummaryReport.pdf'); },
      error: (err) => { this.busy = false; this.error = this.fmt(err); }
    });
  }

  uploadImage(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!this.tour.id || !input.files?.length) return;
    this.busy = true;
    this.api.uploadImage(this.tour.id, input.files[0]).subscribe({
      next: (saved) => {
        this.busy = false;
        this.tourSaved.emit(saved);
        this.loadTourImage(saved.id!);
      },
      error: (err) => { this.busy = false; this.error = this.fmt(err); }
    });
    input.value = '';
  }

  // --- Route / map ---

  calculateRoute(): void {
    const from = this.tour.origin?.trim();
    const to = this.tour.destination?.trim();
    if (!from || !to) { this.error = 'Please fill in From and To first.'; return; }
    this.busy = true;
    this.error = null;
    // Geocode start/end, then fetch route from backend.
    this.api.getCoordinates(from).subscribe({
      next: ([fromLat, fromLng]) => {
        this.api.getCoordinates(to).subscribe({
          next: ([toLat, toLng]) => {
            this.api.getRoute(fromLat, fromLng, toLat, toLng, this.tour.transportType).subscribe({
              next: (routeJson) => {
                this.busy = false;
                this.ensureMap();
                const coords = routeJson?.features?.[0]?.geometry?.coordinates as [number, number][] | undefined;
                const summary = routeJson?.features?.[0]?.properties?.summary;
                // ORS returns metres/seconds; we store km/minutes.
                if (summary?.distance != null) this.tour.distance = Math.round((summary.distance / 1000) * 100) / 100;
                if (summary?.duration != null) this.tour.estimatedTime = Math.round(summary.duration / 60);
                if (Array.isArray(coords) && coords.length > 1) {
                  // GeoJSON is [lng, lat]; Leaflet wants [lat, lng].
                  const latLngs = coords.map(([lng, lat]) => L.latLng(lat, lng));
                  this.drawRoute(latLngs, L.latLng(fromLat, fromLng), L.latLng(toLat, toLng));
                } else {
                  this.error = 'Route response did not contain geometry.';
                }
              },
              error: (err) => { this.busy = false; this.error = this.fmt(err); }
            });
          },
          error: (err) => { this.busy = false; this.error = this.fmt(err); }
        });
      },
      error: (err) => { this.busy = false; this.error = this.fmt(err); }
    });
  }

  private ensureMap(): void {
    if (!this.mapEl?.nativeElement) return;
    if (this.map) { this.map.invalidateSize(); return; }
    // Default centre: Vienna.
    this.map = L.map(this.mapEl.nativeElement, { zoomControl: true }).setView([48.2082, 16.3738], 12);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', { maxZoom: 19 }).addTo(this.map);
  }

  private drawRoute(latLngs: L.LatLng[], from: L.LatLng, to: L.LatLng): void {
    if (!this.map) return;
    this.clearRoute();
    this.markerFrom = L.circleMarker(from, { radius: 7 }).addTo(this.map);
    this.markerTo = L.circleMarker(to, { radius: 7 }).addTo(this.map);
    this.routeLayer = L.polyline(latLngs).addTo(this.map);
    this.map.fitBounds(this.routeLayer.getBounds(), { padding: [20, 20] });
  }

  private clearRoute(): void {
    if (this.routeLayer) { this.routeLayer.remove(); this.routeLayer = null; }
    if (this.markerFrom) { this.markerFrom.remove(); this.markerFrom = null; }
    if (this.markerTo) { this.markerTo.remove(); this.markerTo = null; }
  }

  // --- Logs ---

  loadLogs(): void {
    if (!this.tour?.id) { this.logs = []; return; }
    this.api.getLogs(this.tour.id).subscribe({
      next: (logs) => this.logs = logs,
      error: (err) => this.error = this.fmt(err)
    });
  }

  newLog(): void { this.editingLog = new TourLogViewModel(); }

  editLog(log: TourLog): void { this.editingLog = TourLogViewModel.from(log); }

  saveLog(): void {
    if (!this.tour.id || !this.editingLog) return;
    this.editingLog.submitted = true;
    this.error = null;
    if (!this.editingLog.isValid()) { this.error = 'Please fix validation errors.'; return; }
    this.busy = true;
    const log = this.editingLog.toLog();
    const req = log.id
      ? this.api.updateLog(this.tour.id, log.id!, log)
      : this.api.createLog(this.tour.id, log);
    req.subscribe({
      next: () => {
        this.busy = false;
        this.editingLog = null;
        this.loadLogs();
        this.tourListChanged.emit();
      },
      error: (err) => { this.busy = false; this.error = this.fmt(err); }
    });
  }

  deleteLog(log: TourLog): void {
    if (!this.tour.id || !log.id) return;
    this.busy = true;
    this.api.deleteLog(this.tour.id, log.id).subscribe({
      next: () => {
        this.busy = false;
        this.loadLogs();
        this.tourListChanged.emit();
      },
      error: (err) => { this.busy = false; this.error = this.fmt(err); }
    });
  }

  // --- Image ---

  loadTourImage(tourId: number): void {
    this.api.getTourImage(tourId).subscribe({
      next: (blob) => {
        this.revokeImageUrl();
        // Blob from backend → temporary URL for img preview.
        this.tourImageObjectUrl = URL.createObjectURL(blob);
        this.tourImageSafeUrl = this.sanitizer.bypassSecurityTrustUrl(this.tourImageObjectUrl);
      },
      error: () => { this.tourImageSafeUrl = null; this.tourImageObjectUrl = null; }
    });
  }

  private revokeImageUrl(): void {
    if (this.tourImageObjectUrl) URL.revokeObjectURL(this.tourImageObjectUrl);
    this.tourImageObjectUrl = null;
    this.tourImageSafeUrl = null;
  }

  // --- City autocomplete ---

  onOriginChange(): void { this.fetchSuggestions(this.tour.origin, 'origin'); }
  onDestChange(): void { this.fetchSuggestions(this.tour.destination, 'dest'); }

  private fetchSuggestions(query: string, field: 'origin' | 'dest'): void {
    const clear = () => {
      if (field === 'origin') { this.originSuggestions = []; this.showOriginSug = false; }
      else { this.destSuggestions = []; this.showDestSug = false; }
    };
    if (!query || query.trim().length < 2) { clear(); return; }
    if (this.suggestDebounce) clearTimeout(this.suggestDebounce);
    this.suggestDebounce = setTimeout(() => {
      this.api.searchCities(query.trim()).subscribe({
        next: (suggestions) => {
          if (field === 'origin') { this.originSuggestions = suggestions; this.showOriginSug = suggestions.length > 0; }
          else { this.destSuggestions = suggestions; this.showDestSug = suggestions.length > 0; }
        },
        error: () => {}
      });
    }, 300);
  }

  pickOrigin(city: string): void { this.tour.origin = city; this.showOriginSug = false; }
  pickDest(city: string): void { this.tour.destination = city; this.showDestSug = false; }

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

  starStates(rating: number | null | undefined): boolean[] {
    const r = Math.max(0, Math.min(10, Number(rating) || 0));
    const filled = Math.round(r / 2);
    return [0, 1, 2, 3, 4].map(i => i < filled);
  }

  private downloadBlob(blob: Blob, filename: string): void {
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url; a.download = filename; a.click();
    window.URL.revokeObjectURL(url);
  }

  private fmt(err: any): string {
    const message = err?.error?.message || err?.message || 'Unknown error';
    return typeof message === 'string' ? message : JSON.stringify(message);
  }
}

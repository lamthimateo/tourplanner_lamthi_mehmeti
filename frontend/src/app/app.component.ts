/**
 * AppComponent — the root (and only) View in this application.
 *
 * MVVM pattern breakdown:
 *   - View       : This component's inline template (HTML + Angular bindings).
 *   - ViewModel  : TourViewModel and TourLogViewModel hold mutable form state,
 *                  validation logic, and model-conversion helpers.  The component
 *                  class itself is the "controller glue" that wires ViewModel and
 *                  Model together and owns the Leaflet map (a DOM concern).
 *   - Model      : ApiService (REST calls), AuthService (JWT), Tour / TourLog
 *                  interfaces (data shapes).
 *
 * Two-way data binding via [(ngModel)] connects View fields directly to
 * TourViewModel / TourLogViewModel properties, satisfying the MVVM contract.
 */
import { AfterViewInit, Component, ElementRef, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import { ApiService, Stats } from './services/api.service';
import { AuthService } from './services/auth.service';
import { Tour } from './models/tour';
import { TourLog } from './models/tour-log';
import { TourViewModel } from './viewmodel/tour.viewmodel';
import { TourLogViewModel } from './viewmodel/tour-log.viewmodel';
import { StatCardComponent } from './components/stat-card.component';
import { IconComponent, IconName } from './components/icon.component';
import * as L from 'leaflet';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormsModule, StatCardComponent, IconComponent],
  template: `
    <!-- ══════════════════════ LOGIN / REGISTER ═══════════════════════ -->
    <div *ngIf="!auth.isLoggedIn()" class="auth-shell">

      <!-- Hero side — an editorial "expedition" splash -->
      <section class="auth-hero">
        <div class="contours"></div>
        <svg class="bg-compass" viewBox="0 0 200 200" fill="none" stroke="currentColor" stroke-width="0.6">
          <circle cx="100" cy="100" r="90"/>
          <circle cx="100" cy="100" r="70"/>
          <circle cx="100" cy="100" r="50"/>
          <line x1="100" y1="10" x2="100" y2="190"/>
          <line x1="10" y1="100" x2="190" y2="100"/>
          <line x1="36" y1="36" x2="164" y2="164"/>
          <line x1="164" y1="36" x2="36" y2="164"/>
          <polygon points="100,10 106,100 100,190 94,100" fill="currentColor" fill-opacity="0.35"/>
        </svg>

        <div class="auth-hero-inner">
          <div class="brand">
            <div class="brand-left">
              <div class="brand-mark" aria-hidden="true">
                <app-icon name="compass" [size]="22"></app-icon>
              </div>
              <div class="brand-title">
                <b>Tour Journal</b>
                <span>expedition atlas</span>
              </div>
            </div>
          </div>

          <div>
            <div class="eyebrow">SWEN 2 · Spring 2026</div>
            <h1 class="hero-title">An <em>atlas</em> for<br/>every journey.</h1>
            <p class="hero-lede">Plan tours, trace routes on an interactive map, capture field logs, and export illustrated reports — all inside a single, carefully-crafted journal.</p>
          </div>
        </div>

        <div class="hero-features">
          <div class="hero-feature">
            <app-icon name="route" [size]="18"></app-icon>
            <div><b>Turn-by-turn routing</b><br/><span class="small">OpenRouteService over OpenStreetMap</span></div>
          </div>
          <div class="hero-feature">
            <app-icon name="edit" [size]="18"></app-icon>
            <div><b>Field logs</b><br/><span class="small">Dates, ratings, difficulty, notes</span></div>
          </div>
          <div class="hero-feature">
            <app-icon name="download" [size]="18"></app-icon>
            <div><b>PDF reports</b><br/><span class="small">Per-tour & summary, ready to share</span></div>
          </div>
          <div class="hero-feature">
            <app-icon name="chart" [size]="18"></app-icon>
            <div><b>Statistics</b><br/><span class="small">Aggregates per transport type</span></div>
          </div>
        </div>
      </section>

      <!-- Form side -->
      <section class="auth-form-wrap">
        <div class="auth-card">
          <div class="brand">
            <div class="brand-left">
              <div class="brand-mark" aria-hidden="true">
                <app-icon name="compass" [size]="22"></app-icon>
              </div>
              <div class="brand-title">
                <b>Tour Journal</b>
                <span>since 2026</span>
              </div>
            </div>
            <button class="btn-icon btn-ghost" type="button" (click)="toggleTheme()"
                    [attr.aria-label]="isDark ? 'Switch to light theme' : 'Switch to dark theme'"
                    [title]="isDark ? 'Light mode' : 'Dark mode'">
              <app-icon [name]="isDark ? 'sun' : 'moon'" [size]="16"></app-icon>
            </button>
          </div>

          <h2>{{ isRegistering ? 'Create account' : 'Welcome back' }}</h2>
          <div class="sub">{{ isRegistering ? 'A fresh expedition notebook, just for you.' : 'Sign in to your tour journal.' }}</div>

          <div class="row" style="gap:14px;">
            <div>
              <label>Username</label>
              <input [(ngModel)]="authUsername" placeholder="e.g. explorer"
                     autocomplete="username"
                     [class.invalid]="authSubmitted && authUsername.trim().length < 3" />
              <div class="validation-msg" *ngIf="authSubmitted && authUsername.trim().length < 3">Username must be at least 3 characters</div>
            </div>
            <div>
              <label>Password</label>
              <input type="password" [(ngModel)]="authPassword" placeholder="At least 4 characters"
                     [autocomplete]="isRegistering ? 'new-password' : 'current-password'"
                     [class.invalid]="authSubmitted && authPassword.length < 4"
                     (keyup.enter)="submitAuth()" />
              <div class="validation-msg" *ngIf="authSubmitted && authPassword.length < 4">Password must be at least 4 characters</div>
            </div>

            <button class="btn-primary" style="margin-top:4px;" (click)="submitAuth()" [disabled]="busy">
              <app-icon [name]="isRegistering ? 'plus' : 'arrow-right'" [size]="15"></app-icon>
              {{ isRegistering ? 'Register' : 'Sign in' }}
            </button>

            <button (click)="isRegistering = !isRegistering; authSubmitted = false; error = null;" class="link-btn">
              {{ isRegistering ? 'Already have an account? Sign in' : 'No account yet? Create one' }}
            </button>

            <div class="error-banner" *ngIf="error">
              <app-icon name="alert" [size]="16"></app-icon>
              <span>{{ error }}</span>
            </div>
          </div>
        </div>
      </section>
    </div>

    <!-- ══════════════════════ MAIN APP ═══════════════════════════════ -->
    <div *ngIf="auth.isLoggedIn()" class="container">

      <!-- ── Sidebar ─────────────────────────────────────────────────── -->
      <aside class="sidebar">

        <div class="brand">
          <div class="brand-left">
            <div class="brand-mark" aria-hidden="true">
              <app-icon name="compass" [size]="22"></app-icon>
            </div>
            <div class="brand-title">
              <b>Tour Journal</b>
              <span>routes · logs · reports</span>
            </div>
          </div>
        </div>

        <div class="topbar">
          <div class="user">
            <span class="avatar" aria-hidden="true">{{ initial(auth.getUsername()) }}</span>
            <span>
              <b>{{ auth.getUsername() }}</b><br/>
              <span class="small">expedition member</span>
            </span>
          </div>
          <div class="topbar-actions">
            <button class="btn-icon btn-ghost" type="button" (click)="toggleTheme()"
                    [attr.aria-label]="isDark ? 'Switch to light theme' : 'Switch to dark theme'"
                    [title]="isDark ? 'Light mode' : 'Dark mode'">
              <app-icon [name]="isDark ? 'sun' : 'moon'" [size]="16"></app-icon>
            </button>
            <button class="small-btn btn-ghost" (click)="logout()" title="Sign out">Logout</button>
          </div>
        </div>

        <div class="search-wrap">
          <app-icon name="search" [size]="16"></app-icon>
          <input [(ngModel)]="search" (ngModelChange)="onSearch()" placeholder="Search tours, logs, places…" />
        </div>

        <div class="actions">
          <button class="btn-primary" (click)="newTour()" title="New tour">
            <app-icon name="plus" [size]="15"></app-icon> New
          </button>
          <button class="btn-ghost" (click)="toggleStats()" title="Statistics dashboard">
            <app-icon name="chart" [size]="15"></app-icon> {{ showStats ? 'Hide' : 'Stats' }}
          </button>
          <label style="display:block;">
            <input type="file" accept=".json" style="display:none" #importInput (change)="importTour($event)" />
            <button class="btn-ghost" type="button" (click)="importInput.click()" [disabled]="busy"
                    title="Import tour from JSON" style="width:100%;">
              <app-icon name="upload" [size]="15"></app-icon> Import
            </button>
          </label>
        </div>

        <div class="eyebrow" style="padding: 4px 4px 0;">
          Expedition Ledger · {{ filteredTours.length }}{{ search.trim() ? ' of ' + tours.length : '' }}
        </div>

        <div class="tour-list">
          <div class="tour-entry"
               [class.active]="t.id === selectedTour?.id"
               *ngFor="let t of filteredTours"
               (click)="selectTour(t)">
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

      <!-- ── Main content ────────────────────────────────────────────── -->
      <main class="main">

        <!-- Statistics Dashboard (unique feature) -->
        <section *ngIf="showStats && stats" class="card card-hero">
          <div class="eyebrow">Statistics Dashboard</div>
          <h2 style="margin:6px 0 4px; font-size:28px;">Your expedition, at a glance.</h2>
          <p class="small" style="margin:0 0 18px;">Live aggregates across your tours and logs.</p>

          <div class="stats-grid">
            <app-stat-card [value]="stats.totalTours" label="Tours" tone="primary"></app-stat-card>
            <app-stat-card [value]="stats.totalLogs"  label="Logs"  tone="navy"></app-stat-card>
            <app-stat-card [value]="stats.totalDistanceKm + ' km'" label="Distance" tone="forest"
                           sublabel="total covered"></app-stat-card>
            <app-stat-card [value]="stats.totalTimeHours + ' h'" label="Time"    tone="gold"
                           sublabel="total elapsed"></app-stat-card>
            <app-stat-card [value]="stats.avgRating + '/10'" label="Avg rating" tone="primary"></app-stat-card>
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

        <!-- Empty state -->
        <div *ngIf="!selectedTour && !showStats" class="empty-state">
          <app-icon class="empty-compass" name="compass-large" [size]="120" [strokeWidth]="1.3"></app-icon>
          <div>
            <div class="empty-title">Chart your first tour.</div>
            <div class="empty-body">
              Pick a tour from the ledger on the left — or create a new one to plan a route,
              attach field logs, and generate an illustrated PDF report.
            </div>
          </div>
          <button class="btn-primary" (click)="newTour()">
            <app-icon name="plus" [size]="15"></app-icon> New tour
          </button>
        </div>

        <!-- ─── Selected tour ───────────────────────────────────────── -->
        <ng-container *ngIf="selectedTour">

          <!-- Hero header -->
          <section class="card card-hero tour-header">
            <div class="eyebrow">{{ selectedTour.id ? 'Tour #' + selectedTour.id : 'New tour' }}</div>
            <h1>{{ selectedTour.name || 'Untitled expedition' }}</h1>
            <div class="route">
              <app-icon name="pin" [size]="13"></app-icon>
              <span>{{ selectedTour.origin || '—' }}</span>
              <span class="arrow"><app-icon name="arrow-right" [size]="14"></app-icon></span>
              <span>{{ selectedTour.destination || '—' }}</span>
            </div>
            <div class="meta-row">
              <span class="chip accent" *ngIf="selectedTour.transportType">
                <app-icon [name]="transportIcon(selectedTour.transportType)" [size]="12"></app-icon>
                {{ selectedTour.transportType }}
              </span>
              <span class="chip mono"><b>{{ selectedTour.distance || 0 }}</b>&nbsp;km</span>
              <span class="chip mono"><b>{{ selectedTour.estimatedTime || 0 }}</b>&nbsp;min</span>
              <span class="chip accent-3" *ngIf="selectedTour.popularity != null" title="Popularity = # logs">
                <app-icon name="star" [size]="11"></app-icon> pop {{ selectedTour.popularity }}
              </span>
              <span class="chip accent-2" *ngIf="selectedTour.childFriendliness != null">
                child-friendly {{ selectedTour.childFriendliness }}/10
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
                  <input [(ngModel)]="selectedTour.name"
                         placeholder="e.g. Vienna ring tour"
                         [class.invalid]="selectedTour.submitted && !selectedTour.name.trim()" />
                  <div class="validation-msg" *ngIf="selectedTour.submitted && !selectedTour.name.trim()">Name is required</div>
                </div>
                <div>
                  <label>Transport <span style="color:var(--accent);">*</span></label>
                  <select [(ngModel)]="selectedTour.transportType"
                          [class.invalid]="selectedTour.submitted && !selectedTour.transportType.trim()">
                    <option value="car">Car</option>
                    <option value="bicycle">Bicycle</option>
                    <option value="walking">Walking</option>
                    <option value="running">Running</option>
                    <option value="hiking">Hiking</option>
                  </select>
                  <div class="validation-msg" *ngIf="selectedTour.submitted && !selectedTour.transportType.trim()">Transport type is required</div>
                </div>
              </div>

              <div>
                <label>Description</label>
                <textarea rows="3" [(ngModel)]="selectedTour.description" maxlength="2000"
                          placeholder="What makes this tour memorable?"></textarea>
              </div>

              <div class="row two">
                <div style="position:relative;">
                  <label>From <span style="color:var(--accent);">*</span></label>
                  <input [(ngModel)]="selectedTour.origin"
                         (ngModelChange)="onOriginChange()"
                         (blur)="showOriginSug = false"
                         autocomplete="off"
                         placeholder="Start city"
                         [class.invalid]="selectedTour.submitted && !selectedTour.origin.trim()" />
                  <div class="validation-msg" *ngIf="selectedTour.submitted && !selectedTour.origin.trim()">Origin is required</div>
                  <div class="suggestions" *ngIf="showOriginSug && originSuggestions.length">
                    <div class="suggestion-item" *ngFor="let c of originSuggestions"
                         (mousedown)="pickOrigin(c)">
                      <app-icon name="pin" [size]="13"></app-icon> {{ c }}
                    </div>
                  </div>
                </div>
                <div style="position:relative;">
                  <label>To <span style="color:var(--accent);">*</span></label>
                  <input [(ngModel)]="selectedTour.destination"
                         (ngModelChange)="onDestChange()"
                         (blur)="showDestSug = false"
                         autocomplete="off"
                         placeholder="Destination city"
                         [class.invalid]="selectedTour.submitted && !selectedTour.destination.trim()" />
                  <div class="validation-msg" *ngIf="selectedTour.submitted && !selectedTour.destination.trim()">Destination is required</div>
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
                  <input type="number" [(ngModel)]="selectedTour.distance" min="0" />
                </div>
                <div>
                  <label>Estimated time (min)</label>
                  <input type="number" [(ngModel)]="selectedTour.estimatedTime" min="0" />
                </div>
              </div>

              <div class="btn-row" style="margin-top:4px;">
                <button class="btn-primary" (click)="saveTour()" [disabled]="busy">
                  <app-icon name="check" [size]="15"></app-icon> Save
                </button>
                <button class="btn-ghost" (click)="calculateRoute()" [disabled]="busy">
                  <app-icon name="route" [size]="15"></app-icon> Calculate route
                </button>
                <button class="btn-ghost" (click)="exportTour()" [disabled]="busy || !selectedTour.id" title="Export tour + logs as JSON">
                  <app-icon name="download" [size]="15"></app-icon> Export JSON
                </button>
                <button class="btn-ghost" (click)="downloadTourReport()" [disabled]="busy || !selectedTour.id">
                  <app-icon name="download" [size]="15"></app-icon> Tour PDF
                </button>
                <button class="btn-ghost" (click)="downloadSummaryReport()" [disabled]="busy">
                  <app-icon name="download" [size]="15"></app-icon> Summary PDF
                </button>
                <button class="btn-danger" (click)="deleteSelectedTour()" [disabled]="busy || !selectedTour.id">
                  <app-icon name="trash" [size]="15"></app-icon> Delete
                </button>
              </div>

              <div class="small" *ngIf="!selectedTour.id" style="opacity:0.65;">
                Save the tour first to enable export, reports and image upload.
              </div>

              <!-- Tour image upload and display -->
              <div *ngIf="selectedTour.id" style="margin-top:4px;">
                <label>Tour image</label>
                <input type="file" accept="image/*" style="display:none" #imageInput (change)="uploadTourImage($event)" />
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
              <span class="mono" *ngIf="selectedTour.distance">
                {{ selectedTour.distance }} km · {{ selectedTour.estimatedTime }} min
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
                <button class="btn-ghost" (click)="cancelLog()" [disabled]="busy">
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
                  <div class="log-comment">“{{ l.comment }}”</div>
                </div>
                <div class="stars" [attr.title]="'Rating ' + l.rating + '/10'">
                  <app-icon name="star" [size]="14" *ngFor="let filled of starStates(l.rating); let i = index"
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
        </ng-container>
      </main>
    </div>
  `
})
export class AppComponent implements OnInit, AfterViewInit, OnDestroy {

  // ── Tour list state (Model objects from the API) ──────────────────────────
  tours: Tour[] = [];
  filteredTours: Tour[] = [];
  logs: TourLog[] = [];
  stats: Stats | null = null;
  showStats = false;

  /**
   * The currently selected tour, represented as a TourViewModel so the template
   * can bind to it with [(ngModel)] and invoke ViewModel validation logic.
   * Null when no tour is selected.
   */
  selectedTour: TourViewModel | null = null;

  /**
   * The tour log currently being created or edited, represented as a
   * TourLogViewModel.  Null when the log form is closed.
   */
  editingLog: TourLogViewModel | null = null;

  /** Sanitized blob URL for the currently displayed tour image. Null if no image. */
  tourImageSafeUrl: SafeUrl | null = null;
  /** Raw object URL kept separately so it can be properly revoked. */
  private tourImageObjectUrl: string | null = null;

  // ── City autocomplete state ───────────────────────────────────────────────
  originSuggestions: string[] = [];
  destSuggestions: string[]   = [];
  showOriginSug = false;
  showDestSug   = false;
  private suggestDebounce: ReturnType<typeof setTimeout> | null = null;

  // ── Map (DOM concern — stays in the View layer) ───────────────────────────
  @ViewChild('mapEl') mapEl?: ElementRef<HTMLDivElement>;
  private map: L.Map | null = null;
  private routeLayer: L.Polyline | null = null;
  private markerFrom: L.CircleMarker | null = null;
  private markerTo: L.CircleMarker | null = null;

  // ── Search / UI state ─────────────────────────────────────────────────────
  search = '';
  busy = false;
  error: string | null = null;

  // ── Auth form state ───────────────────────────────────────────────────────
  authUsername = '';
  authPassword = '';
  isRegistering = false;
  authSubmitted = false;

  // ── Theme (light / dark) — persisted in localStorage ─────────────────────
  /** When true, `<html>` gets `data-theme="dark"`. */
  isDark = false;

  constructor(private api: ApiService, public auth: AuthService, private sanitizer: DomSanitizer) {}

  ngOnInit(): void {
    this.initTheme();
    if (this.auth.isLoggedIn()) this.reloadTours();
  }

  ngAfterViewInit(): void {}

  ngOnDestroy(): void {
    if (this.map) { this.map.remove(); this.map = null; }
  }

  // ── Auth ──────────────────────────────────────────────────────────────────

  submitAuth(): void {
    this.authSubmitted = true;
    this.error = null;
    if (this.authUsername.trim().length < 3 || this.authPassword.length < 4) return;
    this.busy = true;
    const req = { username: this.authUsername.trim(), password: this.authPassword };
    const call = this.isRegistering ? this.auth.register(req) : this.auth.login(req);
    call.subscribe({
      next: () => {
        this.busy = false; this.authSubmitted = false;
        this.authUsername = ''; this.authPassword = '';
        this.reloadTours();
      },
      error: (err) => { this.busy = false; this.error = this.formatError(err); }
    });
  }

  logout(): void {
    this.auth.logout();
    this.tours = []; this.filteredTours = []; this.selectedTour = null;
    this.logs = []; this.stats = null; this.error = null;
  }

  // ── Tours ─────────────────────────────────────────────────────────────────

  reloadTours(): void {
    this.error = null;
    this.api.getTours().subscribe({
      next: (tours) => { this.tours = tours; this.applyFilter(); },
      error: (err) => {
        if (err.status === 401 || err.status === 403) this.auth.logout();
        this.error = this.formatError(err);
      }
    });
  }

  /**
   * Triggered on every keystroke in the search box.
   *
   * For non-empty queries the backend full-text search endpoint is called so that
   * tour LOG comments and computed attributes (popularity, childFriendliness) are
   * also included in the results — something a client-side filter cannot do.
   * Falls back to client-side filtering if the backend call fails.
   * For an empty query all tours are shown immediately without a network round-trip.
   */
  onSearch(): void {
    const q = this.search.trim();
    if (!q) {
      // Empty query — show everything without hitting the server
      this.filteredTours = this.tours;
      return;
    }
    // Backend search: searches tour fields, log comments AND computed attributes
    this.api.searchTours(q).subscribe({
      next: (ids) => {
        const idSet = new Set(ids);
        this.filteredTours = this.tours.filter(t => t.id != null && idSet.has(t.id));
      },
      error: () => {
        // If the backend call fails (e.g. network error) fall back to client-side filter
        this.applyClientFilter(q);
      }
    });
  }

  /** Client-side fallback: filters the in-memory tour list by visible text fields. */
  private applyClientFilter(q: string): void {
    const lower = q.toLowerCase();
    this.filteredTours = this.tours.filter(t =>
      (t.name ?? '').toLowerCase().includes(lower) ||
      (t.description ?? '').toLowerCase().includes(lower) ||
      (t.origin ?? '').toLowerCase().includes(lower) ||
      (t.destination ?? '').toLowerCase().includes(lower)
    );
  }

  /** Re-applies the current search after the tour list is reloaded. */
  private applyFilter(): void {
    const q = this.search.trim();
    if (!q) { this.filteredTours = this.tours; return; }
    this.applyClientFilter(q);
  }

  /**
   * Selects an existing tour: wraps the raw Tour in a TourViewModel so the
   * form can bind to it, then reloads its logs and initialises the map.
   */
  selectTour(tour: Tour): void {
    this.selectedTour = TourViewModel.from(tour); // ← ViewModel created here
    this.editingLog = null;
    this.showStats = false;
    this.showOriginSug = false; this.showDestSug = false;
    this.loadLogs();
    // Load the tour image if one has been uploaded
    if (tour.imagePath) {
      this.loadTourImage(tour.id!);
    } else {
      this.tourImageSafeUrl = null;
    }
    setTimeout(() => this.ensureMap(), 0);
  }

  /** Creates an empty TourViewModel for the "new tour" form. */
  newTour(): void {
    this.selectedTour = new TourViewModel(); // ← fresh ViewModel
    this.logs = []; this.editingLog = null; this.showStats = false; this.tourImageSafeUrl = null;
    setTimeout(() => this.ensureMap(), 0);
  }

  saveTour(): void {
    if (!this.selectedTour) return;
    // Ask the ViewModel to mark itself as submitted (triggers validation UI)
    this.selectedTour.submitted = true;
    this.error = null;
    // Delegate validation to the ViewModel
    if (!this.selectedTour.isValid()) {
      this.error = 'Please fill in all required fields.'; return;
    }
    this.busy = true;
    // Convert ViewModel → plain Tour model for the API call
    const payload = this.selectedTour.toTour();
    const req = payload.id
      ? this.api.updateTour(payload.id, payload)
      : this.api.createTour(payload);
    req.subscribe({
      next: (saved) => {
        this.busy = false;
        // Wrap the saved Tour back in a ViewModel
        this.selectedTour = TourViewModel.from(saved);
        // Restore the image after save (it may have been uploaded before saving)
        if (saved.imagePath) {
          this.loadTourImage(this.selectedTour.id!);
        }
        this.reloadTours(); this.loadLogs();
      },
      error: (err) => { this.busy = false; this.error = this.formatError(err); }
    });
  }

  deleteSelectedTour(): void {
    if (!this.selectedTour?.id) return;
    this.busy = true;
    this.api.deleteTour(this.selectedTour.id).subscribe({
      next: () => { this.busy = false; this.selectedTour = null; this.logs = []; this.reloadTours(); },
      error: (err) => { this.busy = false; this.error = this.formatError(err); }
    });
  }

  // ── Stats ─────────────────────────────────────────────────────────────────

  toggleStats(): void {
    this.showStats = !this.showStats;
    if (this.showStats) this.loadStats();
  }

  loadStats(): void {
    this.api.getStats().subscribe({
      next: (s) => this.stats = s,
      error: (err) => this.error = this.formatError(err)
    });
  }

  /** Loads and sanitizes the tour image as a blob URL for display. */
  loadTourImage(tourId: number): void {
    this.api.getTourImage(tourId).subscribe({
      next: (blob) => {
        // Revoke the previous object URL to free browser memory
        if (this.tourImageObjectUrl) {
          URL.revokeObjectURL(this.tourImageObjectUrl);
        }
        this.tourImageObjectUrl = URL.createObjectURL(blob);
        this.tourImageSafeUrl = this.sanitizer.bypassSecurityTrustUrl(this.tourImageObjectUrl);
      },
      error: () => { this.tourImageSafeUrl = null; this.tourImageObjectUrl = null; }
    });
  }

  /** Handles the image file input change event, uploads the file to the backend. */
  uploadTourImage(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!this.selectedTour?.id || !input.files?.length) return;
    const file = input.files[0];
    this.busy = true;
    this.api.uploadImage(this.selectedTour.id, file).subscribe({
      next: (saved) => {
        this.busy = false;
        this.selectedTour = TourViewModel.from(saved);
        this.loadTourImage(this.selectedTour.id!);
      },
      error: (err) => { this.busy = false; this.error = this.formatError(err); }
    });
    input.value = '';
  }

  // ── Import / Export ───────────────────────────────────────────────────────

  exportTour(): void {
    if (!this.selectedTour?.id) return;
    this.busy = true;
    this.api.exportTour(this.selectedTour.id).subscribe({
      next: (data) => {
        this.busy = false;
        const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
        this.downloadBlob(blob, `tour_${this.selectedTour!.id}.json`);
      },
      error: (err) => { this.busy = false; this.error = this.formatError(err); }
    });
  }

  importTour(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files?.length) return;
    const file = input.files[0];
    const reader = new FileReader();
    reader.onload = () => {
      try {
        const data = JSON.parse(reader.result as string);
        this.busy = true;
        this.api.importTour(data).subscribe({
          next: (imported) => { this.busy = false; this.reloadTours(); this.selectTour(imported); },
          error: (err) => { this.busy = false; this.error = this.formatError(err); }
        });
      } catch {
        this.error = 'Invalid JSON file.';
      }
    };
    reader.readAsText(file);
    input.value = '';
  }

  // ── Route / Map ───────────────────────────────────────────────────────────

  calculateRoute(): void {
    if (!this.selectedTour) return;
    const from = this.selectedTour.origin?.trim();
    const to = this.selectedTour.destination?.trim();
    if (!from || !to) { this.error = 'Please fill in From and To first.'; return; }
    this.busy = true; this.error = null;
    this.api.getCoordinates(from).subscribe({
      next: ([fromLat, fromLng]) => {
        this.api.getCoordinates(to).subscribe({
          next: ([toLat, toLng]) => {
            this.api.getRoute(fromLat, fromLng, toLat, toLng).subscribe({
              next: (routeJson) => {
                this.busy = false;
                this.ensureMap();
                const coords = routeJson?.features?.[0]?.geometry?.coordinates as [number, number][] | undefined;
                const summary = routeJson?.features?.[0]?.properties?.summary;
                // Write route distance and time back into the ViewModel
                if (summary?.distance != null) this.selectedTour!.distance = Math.round((summary.distance / 1000) * 100) / 100;
                if (summary?.duration != null) this.selectedTour!.estimatedTime = Math.round(summary.duration / 60);
                if (Array.isArray(coords) && coords.length > 1) {
                  const latLngs = coords.map(([lng, lat]) => L.latLng(lat, lng));
                  this.drawRoute(latLngs, L.latLng(fromLat, fromLng), L.latLng(toLat, toLng));
                } else { this.error = 'Route response did not contain geometry.'; }
              },
              error: (err) => { this.busy = false; this.error = this.formatError(err); }
            });
          },
          error: (err) => { this.busy = false; this.error = this.formatError(err); }
        });
      },
      error: (err) => { this.busy = false; this.error = this.formatError(err); }
    });
  }

  private ensureMap(): void {
    if (!this.mapEl?.nativeElement) return;
    if (this.map) { this.map.invalidateSize(); return; }
    this.map = L.map(this.mapEl.nativeElement, { zoomControl: true }).setView([48.2082, 16.3738], 12);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', { maxZoom: 19 }).addTo(this.map);
  }

  private drawRoute(latLngs: L.LatLng[], from: L.LatLng, to: L.LatLng): void {
    if (!this.map) return;
    if (this.routeLayer) { this.routeLayer.remove(); this.routeLayer = null; }
    if (this.markerFrom) { this.markerFrom.remove(); this.markerFrom = null; }
    if (this.markerTo) { this.markerTo.remove(); this.markerTo = null; }
    this.markerFrom = L.circleMarker(from, { radius: 7 }).addTo(this.map);
    this.markerTo = L.circleMarker(to, { radius: 7 }).addTo(this.map);
    this.routeLayer = L.polyline(latLngs).addTo(this.map);
    this.map.fitBounds(this.routeLayer.getBounds(), { padding: [20, 20] });
  }

  // ── Logs ──────────────────────────────────────────────────────────────────

  loadLogs(): void {
    if (!this.selectedTour?.id) { this.logs = []; return; }
    this.api.getLogs(this.selectedTour.id).subscribe({
      next: (logs) => this.logs = logs,
      error: (err) => this.error = this.formatError(err)
    });
  }

  /** Creates an empty TourLogViewModel for a new log entry. */
  newLog(): void {
    this.editingLog = new TourLogViewModel(); // ← fresh ViewModel
  }

  /** Wraps an existing TourLog in a TourLogViewModel for editing. */
  editLog(log: TourLog): void {
    this.editingLog = TourLogViewModel.from(log); // ← ViewModel created here
  }

  cancelLog(): void { this.editingLog = null; }

  saveLog(): void {
    if (!this.selectedTour?.id || !this.editingLog) return;
    // Mark log ViewModel as submitted to show validation messages
    this.editingLog.submitted = true;
    this.error = null;
    // Delegate validation to the ViewModel
    if (!this.editingLog.isValid()) {
      this.error = 'Please fix validation errors.'; return;
    }
    this.busy = true;
    // Convert ViewModel → plain TourLog model for the API call
    const log = this.editingLog.toLog();
    const req = log.id
      ? this.api.updateLog(this.selectedTour.id, log.id!, log)
      : this.api.createLog(this.selectedTour.id, log);
    req.subscribe({
      next: () => { this.busy = false; this.editingLog = null; this.loadLogs(); this.reloadTours(); },
      error: (err) => { this.busy = false; this.error = this.formatError(err); }
    });
  }

  deleteLog(log: TourLog): void {
    if (!this.selectedTour?.id || !log.id) return;
    this.busy = true;
    this.api.deleteLog(this.selectedTour.id, log.id).subscribe({
      next: () => { this.busy = false; this.loadLogs(); this.reloadTours(); },
      error: (err) => { this.busy = false; this.error = this.formatError(err); }
    });
  }

  // ── Reports ───────────────────────────────────────────────────────────────

  downloadTourReport(): void {
    if (!this.selectedTour?.id) return;
    this.busy = true;
    this.api.downloadTourReport(this.selectedTour.id).subscribe({
      next: (blob) => { this.busy = false; this.downloadBlob(blob, `TourReport_${this.selectedTour!.id}.pdf`); },
      error: (err) => { this.busy = false; this.error = this.formatError(err); }
    });
  }

  downloadSummaryReport(): void {
    this.busy = true;
    this.api.downloadSummaryReport().subscribe({
      next: (blob) => { this.busy = false; this.downloadBlob(blob, 'SummaryReport.pdf'); },
      error: (err) => { this.busy = false; this.error = this.formatError(err); }
    });
  }

  private downloadBlob(blob: Blob, filename: string): void {
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url; a.download = filename; a.click();
    window.URL.revokeObjectURL(url);
  }

  // ── City autocomplete ─────────────────────────────────────────────────────

  /** Called on every keystroke in the From input. */
  onOriginChange(): void { this.fetchSuggestions(this.selectedTour!.origin, 'origin'); }

  /** Called on every keystroke in the To input. */
  onDestChange(): void { this.fetchSuggestions(this.selectedTour!.destination, 'dest'); }

  /**
   * Debounced geocoding lookup: waits 300 ms after the user stops typing,
   * then queries Nominatim for matching city names.
   */
  private fetchSuggestions(query: string, field: 'origin' | 'dest'): void {
    const clear = () => {
      if (field === 'origin') { this.originSuggestions = []; this.showOriginSug = false; }
      else                    { this.destSuggestions = [];   this.showDestSug   = false; }
    };
    if (!query || query.trim().length < 2) { clear(); return; }
    if (this.suggestDebounce) clearTimeout(this.suggestDebounce);
    this.suggestDebounce = setTimeout(() => {
      this.api.searchCities(query.trim()).subscribe({
        next: (suggestions) => {
          if (field === 'origin') { this.originSuggestions = suggestions; this.showOriginSug = suggestions.length > 0; }
          else                    { this.destSuggestions = suggestions;   this.showDestSug   = suggestions.length > 0; }
        },
        error: () => {} // fail silently — user can still type manually
      });
    }, 300);
  }

  /** Fills the From field with the chosen suggestion and closes the dropdown. */
  pickOrigin(city: string): void {
    this.selectedTour!.origin = city;
    this.showOriginSug = false;
  }

  /** Fills the To field with the chosen suggestion and closes the dropdown. */
  pickDest(city: string): void {
    this.selectedTour!.destination = city;
    this.showDestSug = false;
  }

  private formatError(err: any): string {
    const message = err?.error?.message || err?.message || 'Unknown error';
    return typeof message === 'string' ? message : JSON.stringify(message);
  }

  // ─────────────────────────────────────────────────────────────────────────
  // View helpers — used only by the template
  // ─────────────────────────────────────────────────────────────────────────

  /** Reads the user's stored preference (or OS preference) on first load. */
  private initTheme(): void {
    try {
      const stored = localStorage.getItem('tourjournal.theme');
      if (stored === 'dark' || stored === 'light') {
        this.isDark = stored === 'dark';
      } else if (typeof window !== 'undefined' && window.matchMedia) {
        this.isDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
      }
    } catch { /* localStorage may be unavailable (SSR, privacy mode) */ }
    this.applyTheme();
  }

  /** Flips the theme and persists the choice. */
  toggleTheme(): void {
    this.isDark = !this.isDark;
    this.applyTheme();
    try { localStorage.setItem('tourjournal.theme', this.isDark ? 'dark' : 'light'); } catch {}
  }

  private applyTheme(): void {
    const root = document.documentElement;
    if (this.isDark) root.setAttribute('data-theme', 'dark');
    else root.removeAttribute('data-theme');
  }

  /** First character of a username, for the small avatar bubble. */
  initial(name: string | null | undefined): string {
    if (!name) return '?';
    return name.trim().charAt(0).toUpperCase() || '?';
  }

  /**
   * Maps a backend transport-type string to an icon name so the template can
   * say <app-icon [name]="transportIcon(t)" />. Unknown values fall back to
   * the generic pin.
   */
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

  /**
   * Converts a 0–10 rating into 5 star-slots (each true/false = filled/empty).
   * A rating of 7 → [✓,✓,✓,○,○] (rounded to the nearest even number of half-stars).
   */
  starStates(rating: number | null | undefined): boolean[] {
    const r = Math.max(0, Math.min(10, Number(rating) || 0));
    const filled = Math.round(r / 2);
    return [0, 1, 2, 3, 4].map(i => i < filled);
  }

  /**
   * Returns the bar-fill percentage for the transport breakdown chart,
   * normalised against the largest bucket so the biggest bar is always full.
   */
  barWidth(count: number): number {
    if (!this.stats?.byTransportType?.length) return 0;
    const max = Math.max(...this.stats.byTransportType.map(s => s.tourCount), 1);
    return Math.round((count / max) * 100);
  }
}

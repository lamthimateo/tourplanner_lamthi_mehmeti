import { Component, EventEmitter, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="auth-shell">
      <section class="auth-card" aria-label="Authentication">
        <h2>{{ isRegistering ? 'Create account' : 'Sign in' }}</h2>

        <div class="row" style="gap:14px;">
          <div>
            <label>Username</label>
            <input [(ngModel)]="username" autocomplete="username"
                   [class.invalid]="submitted && username.trim().length < 3" />
            <div class="validation-msg" *ngIf="submitted && username.trim().length < 3">Username must be at least 3 characters</div>
          </div>
          <div>
            <label>Password</label>
            <input type="password" [(ngModel)]="password"
                   [autocomplete]="isRegistering ? 'new-password' : 'current-password'"
                   [class.invalid]="submitted && password.length < 4"
                   (keyup.enter)="submit()" />
            <div class="validation-msg" *ngIf="submitted && password.length < 4">Password must be at least 4 characters</div>
          </div>

          <button class="btn-primary" style="margin-top:4px;" (click)="submit()" [disabled]="busy">
            {{ isRegistering ? 'Register' : 'Sign in' }}
          </button>

          <button (click)="isRegistering = !isRegistering; submitted = false; error = null;" class="link-btn">
            {{ isRegistering ? 'Sign in instead' : 'Create an account' }}
          </button>

          <div class="error-banner" *ngIf="error">
            <span>{{ error }}</span>
          </div>
        </div>
      </section>
    </div>
  `
})
export class LoginComponent {
  @Output() loggedIn = new EventEmitter<void>();

  username = '';
  password = '';
  isRegistering = false;
  submitted = false;
  error: string | null = null;
  busy = false;

  constructor(private auth: AuthService) {}

  submit(): void {
    this.submitted = true;
    this.error = null;
    if (this.username.trim().length < 3 || this.password.length < 4) return;
    this.busy = true;
    const req = { username: this.username.trim(), password: this.password };
    const call = this.isRegistering ? this.auth.register(req) : this.auth.login(req);
    call.subscribe({
      next: () => {
        this.busy = false;
        this.submitted = false;
        this.username = '';
        this.password = '';
        this.loggedIn.emit();
      },
      error: (err) => {
        this.busy = false;
        const msg = err?.error?.message || err?.message || 'Unknown error';
        this.error = typeof msg === 'string' ? msg : JSON.stringify(msg);
      }
    });
  }
}

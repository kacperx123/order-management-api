import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  standalone: true,
  selector: 'app-login',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule
  ],
  template: `
    <div class="login-page">
      <div class="login-card">
        <div class="login-brand">
          <div class="brand-icon">O</div>
          <div>
            <div class="brand-title">Order Ops</div>
            <div class="brand-subtitle">operations console</div>
          </div>
        </div>

        <form [formGroup]="form" (ngSubmit)="submit()">
          <mat-form-field appearance="outline" class="full">
            <mat-label>Email</mat-label>
            <input matInput formControlName="email" autocomplete="username" />
          </mat-form-field>

          <mat-form-field appearance="outline" class="full">
            <mat-label>Password</mat-label>
            <input matInput type="password" formControlName="password" autocomplete="current-password" />
          </mat-form-field>

          <div class="login-error" *ngIf="error()">{{ error() }}</div>

          <button
            mat-flat-button
            color="primary"
            class="full submit"
            type="submit"
            [disabled]="loading()"
          >
            <mat-spinner diameter="18" *ngIf="loading()"></mat-spinner>
            <span *ngIf="!loading()">Sign in</span>
          </button>
        </form>

        <div class="dev-hint">
          <span class="mono">admin&#64;example.com / admin123</span>
          <span class="hint-label">dev credentials</span>
        </div>
      </div>
    </div>
  `,
  styles: [
    `
      .login-page {
        min-height: 100vh;
        display: flex;
        align-items: center;
        justify-content: center;
        background: var(--ops-bg-deep);
      }
      .login-card {
        width: 360px;
        background: var(--ops-panel);
        border: 1px solid var(--ops-border);
        border-radius: 10px;
        padding: 28px;
      }
      .login-brand {
        display: flex;
        align-items: center;
        gap: 10px;
        margin-bottom: 24px;
      }
      .brand-icon {
        width: 34px;
        height: 34px;
        border-radius: 8px;
        background: var(--ops-accent-strong);
        color: #04120c;
        display: flex;
        align-items: center;
        justify-content: center;
        font-weight: 600;
        font-size: 17px;
      }
      .brand-title {
        font-size: 1.05rem;
        font-weight: 600;
        color: var(--ops-text);
      }
      .brand-subtitle {
        font-family: var(--ops-mono);
        font-size: 0.72rem;
        color: var(--ops-muted-deep);
      }
      .full { width: 100%; }
      .submit {
        margin-top: 4px;
        height: 42px;
        display: flex;
        align-items: center;
        justify-content: center;
      }
      .login-error {
        color: var(--ops-danger);
        background: var(--ops-danger-bg);
        border-radius: 6px;
        padding: 8px 12px;
        font-size: 12.5px;
        margin-bottom: 12px;
      }
      .dev-hint {
        margin-top: 18px;
        padding-top: 14px;
        border-top: 1px solid var(--ops-border-soft);
        display: flex;
        justify-content: space-between;
        align-items: center;
        font-size: 11.5px;
        color: var(--ops-muted);
      }
      .hint-label {
        color: var(--ops-muted-deep);
      }
    `
  ]
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required]
  });

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  submit(): void {
    if (this.form.invalid || this.loading()) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    const { email, password } = this.form.getRawValue();

    this.auth.login(email, password).subscribe({
      next: (profile) => {
        void this.router.navigate([profile.role === 'ADMIN' ? '/dashboard' : '/my-orders']);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.status === 401 ? 'Invalid email or password' : 'Sign-in failed. Try again.');
      }
    });
  }
}

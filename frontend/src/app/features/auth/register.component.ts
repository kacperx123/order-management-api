import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../core/auth/auth.service';

function passwordsMatch(group: AbstractControl): ValidationErrors | null {
  const password = group.get('password')?.value;
  const confirm = group.get('confirm')?.value;
  return password && confirm && password !== confirm ? { mismatch: true } : null;
}

@Component({
  standalone: true,
  selector: 'app-register',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
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
            <div class="brand-subtitle">create your account</div>
          </div>
        </div>

        <form [formGroup]="form" (ngSubmit)="submit()">
          <mat-form-field appearance="outline" class="full">
            <mat-label>Email</mat-label>
            <input matInput formControlName="email" autocomplete="username" />
            <mat-error *ngIf="form.controls.email.hasError('email')">Enter a valid email</mat-error>
          </mat-form-field>

          <mat-form-field appearance="outline" class="full">
            <mat-label>Password</mat-label>
            <input matInput type="password" formControlName="password" autocomplete="new-password" />
            <mat-error *ngIf="form.controls.password.hasError('minlength')">
              At least 8 characters
            </mat-error>
          </mat-form-field>

          <mat-form-field appearance="outline" class="full">
            <mat-label>Confirm password</mat-label>
            <input matInput type="password" formControlName="confirm" autocomplete="new-password" />
          </mat-form-field>

          <div class="login-error" *ngIf="form.hasError('mismatch') && form.controls.confirm.touched">
            Passwords do not match
          </div>
          <div class="login-error" *ngIf="error()">{{ error() }}</div>

          <button
            mat-flat-button
            color="primary"
            class="full submit"
            type="submit"
            [disabled]="loading()"
          >
            <mat-spinner diameter="18" *ngIf="loading()"></mat-spinner>
            <span *ngIf="!loading()">Create account</span>
          </button>
        </form>

        <div class="alt-action">
          <span>Already have an account?</span>
          <a routerLink="/login">Sign in</a>
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
      .alt-action {
        margin-top: 18px;
        padding-top: 14px;
        border-top: 1px solid var(--ops-border-soft);
        display: flex;
        gap: 6px;
        justify-content: center;
        font-size: 12.5px;
        color: var(--ops-muted);
      }
      .alt-action a { color: var(--ops-accent); text-decoration: none; }
      .alt-action a:hover { text-decoration: underline; }
    `
  ]
})
export class RegisterComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  form = this.fb.nonNullable.group(
    {
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8)]],
      confirm: ['', Validators.required]
    },
    { validators: passwordsMatch }
  );

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

    this.auth.register(email, password).subscribe({
      next: (profile) => {
        void this.router.navigate([profile.role === 'ADMIN' ? '/dashboard' : '/home']);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(
          err?.status === 409
            ? 'This email is already registered'
            : 'Registration failed. Try again.'
        );
      }
    });
  }
}

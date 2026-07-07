import { HttpClient } from '@angular/common/http';
import { Injectable, computed, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, switchMap, tap } from 'rxjs';

export type Role = 'ADMIN' | 'USER';

export interface UserProfile {
  id: string;
  email: string;
  role: Role;
  createdAt: string;
}

export interface TokenResponse {
  token: string;
  expiresAt: string;
}

const TOKEN_KEY = 'ops.token';
const PROFILE_KEY = 'ops.profile';

function readStoredProfile(): UserProfile | null {
  try {
    const raw = localStorage.getItem(PROFILE_KEY);
    return raw ? (JSON.parse(raw) as UserProfile) : null;
  } catch {
    return null;
  }
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly profileSignal = signal<UserProfile | null>(readStoredProfile());

  readonly currentUser = this.profileSignal.asReadonly();
  readonly role = computed(() => this.profileSignal()?.role ?? null);
  readonly isAdmin = computed(() => this.role() === 'ADMIN');

  constructor(
    private readonly http: HttpClient,
    private readonly router: Router
  ) {}

  get token(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  isAuthenticated(): boolean {
    return this.token !== null;
  }

  /** Registers a new USER account, then logs in with the same credentials. */
  register(email: string, password: string): Observable<UserProfile> {
    return this.http
      .post<UserProfile>('/auth/register', { email, password })
      .pipe(switchMap(() => this.login(email, password)));
  }

  /** Logs in, stores the JWT, then resolves the user profile (role comes from /users/me). */
  login(email: string, password: string): Observable<UserProfile> {
    return this.http.post<TokenResponse>('/auth/login', { email, password }).pipe(
      tap((res) => localStorage.setItem(TOKEN_KEY, res.token)),
      switchMap(() => this.http.get<UserProfile>('/users/me')),
      tap((profile) => {
        localStorage.setItem(PROFILE_KEY, JSON.stringify(profile));
        this.profileSignal.set(profile);
      })
    );
  }

  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(PROFILE_KEY);
    this.profileSignal.set(null);
    void this.router.navigate(['/login']);
  }
}

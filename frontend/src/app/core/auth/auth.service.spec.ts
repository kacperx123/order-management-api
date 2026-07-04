import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { AuthService, UserProfile } from './auth.service';

const PROFILE: UserProfile = {
  id: 'a3f1c9e2-0000-0000-0000-000000000001',
  email: 'user@test.com',
  role: 'USER',
  createdAt: '2026-01-01T00:00:00Z'
};

describe('AuthService', () => {
  let service: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([{ path: 'login', children: [] }])]
    });
    service = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    localStorage.clear();
  });

  it('starts unauthenticated with no stored token', () => {
    expect(service.isAuthenticated()).toBe(false);
    expect(service.currentUser()).toBeNull();
    expect(service.role()).toBeNull();
  });

  it('login stores the token, then resolves and stores the profile', () => {
    let received: UserProfile | undefined;
    service.login('user@test.com', 'password123').subscribe((p) => (received = p));

    const loginReq = http.expectOne('/auth/login');
    expect(loginReq.request.method).toBe('POST');
    expect(loginReq.request.body).toEqual({ email: 'user@test.com', password: 'password123' });
    loginReq.flush({ token: 'jwt-abc', expiresAt: '2026-01-01T01:00:00Z' });

    const meReq = http.expectOne('/users/me');
    expect(meReq.request.method).toBe('GET');
    meReq.flush(PROFILE);

    expect(received).toEqual(PROFILE);
    expect(service.token).toBe('jwt-abc');
    expect(service.isAuthenticated()).toBe(true);
    expect(service.currentUser()).toEqual(PROFILE);
    expect(service.role()).toBe('USER');
    expect(service.isAdmin()).toBe(false);
  });

  it('logout clears the session', () => {
    service.login('user@test.com', 'password123').subscribe();
    http.expectOne('/auth/login').flush({ token: 'jwt-abc', expiresAt: '' });
    http.expectOne('/users/me').flush(PROFILE);

    service.logout();

    expect(service.token).toBeNull();
    expect(service.isAuthenticated()).toBe(false);
    expect(service.currentUser()).toBeNull();
    expect(localStorage.getItem('ops.token')).toBeNull();
    expect(localStorage.getItem('ops.profile')).toBeNull();
  });

  it('restores the profile from localStorage on construction', () => {
    localStorage.setItem('ops.token', 'jwt-restored');
    localStorage.setItem('ops.profile', JSON.stringify({ ...PROFILE, role: 'ADMIN' }));

    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([{ path: 'login', children: [] }])]
    });
    const restored = TestBed.inject(AuthService);

    expect(restored.isAuthenticated()).toBe(true);
    expect(restored.isAdmin()).toBe(true);
    expect(restored.currentUser()?.email).toBe(PROFILE.email);
  });
});

import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { authInterceptor } from './auth.interceptor';
import { AuthService } from './auth.service';

describe('authInterceptor', () => {
  let httpClient: HttpClient;
  let http: HttpTestingController;
  let auth: AuthService;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        provideRouter([{ path: 'login', children: [] }])
      ]
    });
    httpClient = TestBed.inject(HttpClient);
    http = TestBed.inject(HttpTestingController);
    auth = TestBed.inject(AuthService);
  });

  afterEach(() => {
    http.verify();
    localStorage.clear();
  });

  it('adds the Bearer header when a token is stored', () => {
    localStorage.setItem('ops.token', 'jwt-abc');

    httpClient.get('/products').subscribe();

    const req = http.expectOne('/products');
    expect(req.request.headers.get('Authorization')).toBe('Bearer jwt-abc');
    req.flush([]);
  });

  it('sends no Authorization header without a token', () => {
    httpClient.get('/products').subscribe();

    const req = http.expectOne('/products');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush([]);
  });

  it('clears the session on 401 from an API call', () => {
    localStorage.setItem('ops.token', 'jwt-expired');
    const logoutSpy = vi.spyOn(auth, 'logout');

    httpClient.get('/orders/my').subscribe({ error: () => {} });
    http.expectOne('/orders/my').flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(logoutSpy).toHaveBeenCalled();
  });

  it('does not log out on 401 from the login endpoint itself', () => {
    const logoutSpy = vi.spyOn(auth, 'logout');

    httpClient.post('/auth/login', {}).subscribe({ error: () => {} });
    http.expectOne('/auth/login').flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(logoutSpy).not.toHaveBeenCalled();
  });
});

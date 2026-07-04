import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService, Role } from './auth.service';

/** Requires the given role; non-matching authenticated users land on their home view. */
export function roleGuard(required: Role): CanActivateFn {
  return () => {
    const auth = inject(AuthService);
    const router = inject(Router);

    if (!auth.isAuthenticated()) {
      return router.createUrlTree(['/login']);
    }
    if (auth.role() === required) {
      return true;
    }
    return router.createUrlTree([auth.isAdmin() ? '/dashboard' : '/my-orders']);
  };
}

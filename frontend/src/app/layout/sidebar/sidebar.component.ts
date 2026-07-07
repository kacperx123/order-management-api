import { Component, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../core/auth/auth.service';

interface NavLink {
  path: string;
  label: string;
  icon: string;
}

const ADMIN_LINKS: NavLink[] = [
  { path: '/dashboard', label: 'Dashboard', icon: 'space_dashboard' },
  { path: '/products', label: 'Products', icon: 'inventory_2' },
  { path: '/inventory', label: 'Inventory', icon: 'warehouse' },
  { path: '/orders', label: 'Orders', icon: 'shopping_cart' },
  { path: '/outbox-events', label: 'Events', icon: 'sensors' },
  { path: '/my-orders', label: 'My orders', icon: 'receipt_long' }
];

const USER_LINKS: NavLink[] = [
  { path: '/home', label: 'Dashboard', icon: 'space_dashboard' },
  { path: '/catalog', label: 'Catalog', icon: 'inventory_2' },
  { path: '/my-orders', label: 'My orders', icon: 'receipt_long' }
];

@Component({
  standalone: true,
  selector: 'app-sidebar',
  imports: [CommonModule, MatIconModule, RouterLink, RouterLinkActive],
  templateUrl: './sidebar.component.html',
  styleUrls: ['./sidebar.component.scss']
})
export class SidebarComponent {
  private readonly auth = inject(AuthService);

  protected readonly navLinks = computed<NavLink[]>(() =>
    this.auth.isAdmin() ? ADMIN_LINKS : USER_LINKS
  );
}

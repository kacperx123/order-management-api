import { Routes } from '@angular/router';
import { AppLayoutComponent } from './layout/app-layout/app-layout.component';
import { DashboardComponent } from './features/dashboard/dashboard.component';
import { ProductsComponent } from './features/products/products.component';
import { InventoryComponent } from './features/inventory/inventory.component';
import { OrdersComponent } from './features/orders/orders.component';
import { MyOrdersComponent } from './features/orders/my-orders.component';
import { OutboxEventsComponent } from './features/outbox-events/outbox-events.component';
import { LoginComponent } from './features/auth/login.component';
import { RegisterComponent } from './features/auth/register.component';
import { UserHomeComponent } from './features/home/user-home.component';
import { CatalogComponent } from './features/catalog/catalog.component';
import { authGuard } from './core/auth/auth.guard';
import { roleGuard } from './core/auth/role.guard';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  {
    path: '',
    component: AppLayoutComponent,
    canActivate: [authGuard],
    children: [
      { path: 'dashboard', component: DashboardComponent, canActivate: [roleGuard('ADMIN')] },
      { path: 'products', component: ProductsComponent, canActivate: [roleGuard('ADMIN')] },
      { path: 'inventory', component: InventoryComponent, canActivate: [roleGuard('ADMIN')] },
      { path: 'orders', component: OrdersComponent, canActivate: [roleGuard('ADMIN')] },
      { path: 'outbox-events', component: OutboxEventsComponent, canActivate: [roleGuard('ADMIN')] },
      { path: 'home', component: UserHomeComponent },
      { path: 'catalog', component: CatalogComponent },
      { path: 'my-orders', component: MyOrdersComponent },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
    ]
  },
  { path: '**', redirectTo: 'dashboard' }
];

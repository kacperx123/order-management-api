import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { RouterLink } from '@angular/router';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';
import { ShortIdPipe } from '../../shared/pipes/short-id.pipe';
import { AuthService } from '../../core/auth/auth.service';
import { OrderApiResponse, OrderApiService } from '../../shared/services/api.service';
import { NewOrderDialogComponent } from '../orders/new-order-dialog.component';

type OrderRow = OrderApiResponse & { total: number };

@Component({
  standalone: true,
  selector: 'app-user-home',
  imports: [
    CommonModule,
    MatButtonModule,
    MatDialogModule,
    MatProgressSpinnerModule,
    RouterLink,
    PageHeaderComponent,
    ShortIdPipe
  ],
  template: `
    <section class="page-section page-home">
      <app-page-header [title]="greeting()" subtitle="Your ordering overview">
        <button mat-flat-button color="primary" (click)="openNewOrder()">New order</button>
      </app-page-header>

      <div class="loading" *ngIf="loading()">
        <mat-spinner diameter="28"></mat-spinner>
      </div>

      <div class="error-box" *ngIf="error()">{{ error() }}</div>

      <ng-container *ngIf="!loading() && !error()">
        <div class="stat-cards">
          <div class="stat-card">
            <div class="stat-label">Total orders</div>
            <div class="stat-value">{{ orders().length }}</div>
          </div>
          <div class="stat-card">
            <div class="stat-label">Awaiting payment</div>
            <div class="stat-value warn">{{ pendingCount() }}</div>
          </div>
          <div class="stat-card">
            <div class="stat-label">Paid</div>
            <div class="stat-value ok">{{ paidCount() }}</div>
          </div>
          <div class="stat-card">
            <div class="stat-label">Total spent</div>
            <div class="stat-value">{{ totalSpent() | number: '1.2-2' }}</div>
          </div>
        </div>

        <div class="ops-panel panel">
          <div class="panel-header">
            <span class="panel-title">Recent orders</span>
            <a mat-button routerLink="/my-orders">View all</a>
          </div>
          <table class="panel-table">
            <thead>
              <tr>
                <th>Order</th>
                <th>Items</th>
                <th>Status</th>
                <th class="num">Total</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let order of recentOrders()">
                <td class="mono id">{{ order.id | shortId: 'ORD' }}</td>
                <td class="ellipsis">{{ itemsSummary(order) }}</td>
                <td><span class="status-chip" [ngClass]="order.status.toLowerCase()">{{ order.status }}</span></td>
                <td class="mono num">{{ order.total | number: '1.2-2' }}</td>
              </tr>
              <tr *ngIf="recentOrders().length === 0">
                <td colspan="4" class="empty">
                  No orders yet — <a routerLink="/catalog">browse the catalog</a> to place your first one.
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </ng-container>
    </section>
  `,
  styles: [
    `
      .page-home { display: grid; gap: 4px; }
      .loading { display: flex; justify-content: center; padding: 32px; }
      .error-box {
        color: var(--ops-danger);
        background: var(--ops-danger-bg);
        border-radius: 8px;
        padding: 12px 16px;
        font-size: 13px;
      }
      .panel { padding: 4px 0 8px; }
      .panel-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 8px 16px;
        border-bottom: 1px solid var(--ops-border-soft);
      }
      .panel-title { font-size: 13px; font-weight: 600; color: var(--ops-text); }
      .panel-table { width: 100%; border-collapse: collapse; font-size: 12.5px; }
      .panel-table th {
        text-align: left;
        color: var(--ops-muted);
        font-weight: 400;
        font-size: 11.5px;
        padding: 8px 16px 6px;
      }
      .panel-table td { padding: 8px 16px; border-top: 1px solid var(--ops-border-soft); color: var(--ops-text-dim); }
      .panel-table .id { color: var(--ops-id); font-size: 12px; }
      .panel-table .num { text-align: right; }
      .panel-table .ellipsis { max-width: 280px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
      .panel-table .empty { color: var(--ops-muted); text-align: center; padding: 20px; }
      .panel-table .empty a { color: var(--ops-accent); }
    `
  ]
})
export class UserHomeComponent implements OnInit {
  private readonly orderApi = inject(OrderApiService);
  private readonly auth = inject(AuthService);
  private readonly dialog = inject(MatDialog);

  readonly orders = signal<OrderRow[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  readonly greeting = computed(() => {
    const email = this.auth.currentUser()?.email ?? '';
    const name = email.split('@')[0];
    return name ? `Welcome, ${name}` : 'Welcome';
  });

  readonly pendingCount = computed(() => this.orders().filter((o) => o.status === 'CREATED').length);
  readonly paidCount = computed(() => this.orders().filter((o) => o.status === 'PAID').length);
  readonly totalSpent = computed(() =>
    this.orders()
      .filter((o) => o.status === 'PAID')
      .reduce((sum, o) => sum + o.total, 0)
  );
  readonly recentOrders = computed(() => this.orders().slice(0, 5));

  ngOnInit(): void {
    this.refresh();
  }

  refresh(): void {
    this.loading.set(true);
    this.error.set(null);
    this.orderApi.listMyOrders().subscribe({
      next: (orders) => {
        this.orders.set(
          [...orders]
            .sort((a, b) => b.createdAt.localeCompare(a.createdAt))
            .map((order) => ({
              ...order,
              total: order.items.reduce((sum, item) => sum + item.unitPriceAtPurchase * item.quantity, 0)
            }))
        );
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Could not load your overview');
        this.loading.set(false);
      }
    });
  }

  openNewOrder(): void {
    this.dialog
      .open(NewOrderDialogComponent, { width: '520px' })
      .afterClosed()
      .subscribe((created) => {
        if (created) {
          this.refresh();
        }
      });
  }

  itemsSummary(order: OrderApiResponse): string {
    return order.items.map((i) => `${i.quantity}× ${i.productName}`).join(', ');
  }
}

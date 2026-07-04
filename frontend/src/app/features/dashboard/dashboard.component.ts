import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';
import {
  InventoryApiService,
  OrderApiResponse,
  OrderApiService,
  OutboxApiService,
  OutboxEventApiResponse,
  ProductApiService
} from '../../shared/services/api.service';

interface DashboardStats {
  pendingOrders: number;
  paidOrders: number;
  activeProducts: number;
  totalProducts: number;
  lowStock: number;
  outOfStock: number;
  unpublishedEvents: number;
}

@Component({
  standalone: true,
  selector: 'app-dashboard',
  imports: [
    CommonModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    RouterLink,
    PageHeaderComponent
  ],
  template: `
    <section class="page-section page-dashboard">
      <app-page-header title="Dashboard" subtitle="Live system overview">
        <button mat-stroked-button (click)="refresh()">Refresh</button>
      </app-page-header>

      <div class="loading" *ngIf="loading()">
        <mat-spinner diameter="28"></mat-spinner>
      </div>

      <div class="error-box" *ngIf="error()">{{ error() }}</div>

      <ng-container *ngIf="!loading() && !error() && stats() as s">
        <div class="stat-cards">
          <div class="stat-card">
            <div class="stat-label">Pending orders</div>
            <div class="stat-value warn">{{ s.pendingOrders }}</div>
          </div>
          <div class="stat-card">
            <div class="stat-label">Paid orders</div>
            <div class="stat-value ok">{{ s.paidOrders }}</div>
          </div>
          <div class="stat-card">
            <div class="stat-label">Active products</div>
            <div class="stat-value">{{ s.activeProducts }}<span class="stat-sub">/{{ s.totalProducts }}</span></div>
          </div>
          <div class="stat-card">
            <div class="stat-label">Low / out of stock</div>
            <div class="stat-value" [class.danger]="s.outOfStock > 0" [class.warn]="s.outOfStock === 0 && s.lowStock > 0">
              {{ s.lowStock + s.outOfStock }}
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-label">Events in queue</div>
            <div class="stat-value" [ngClass]="s.unpublishedEvents === 0 ? 'ok' : 'warn'">
              {{ s.unpublishedEvents }}
            </div>
          </div>
        </div>

        <div class="dashboard-panels">
          <div class="ops-panel panel">
            <div class="panel-header">
              <span class="panel-title">Recent orders</span>
              <a mat-button routerLink="/orders">View all</a>
            </div>
            <table class="panel-table">
              <thead>
                <tr>
                  <th>Order</th>
                  <th>Customer</th>
                  <th>Status</th>
                  <th class="num">Total</th>
                </tr>
              </thead>
              <tbody>
                <tr *ngFor="let order of recentOrders()">
                  <td class="mono id">{{ order.id.slice(0, 8) }}…</td>
                  <td class="ellipsis">{{ order.customerEmail }}</td>
                  <td><span class="status-chip" [ngClass]="order.status.toLowerCase()">{{ order.status }}</span></td>
                  <td class="mono num">{{ order.total | number: '1.2-2' }}</td>
                </tr>
                <tr *ngIf="recentOrders().length === 0">
                  <td colspan="4" class="empty">No orders yet</td>
                </tr>
              </tbody>
            </table>
          </div>

          <div class="ops-panel panel stream">
            <div class="panel-header">
              <span class="panel-title mono stream-title">event stream (outbox → kafka)</span>
              <a mat-button routerLink="/outbox-events">View all</a>
            </div>
            <div class="stream-lines">
              <div class="stream-line" *ngFor="let e of recentEvents()">
                <span [ngClass]="e.publishedAt ? 'mark-ok' : 'mark-pending'">{{ e.publishedAt ? '✓' : '◌' }}</span>
                <span class="time">{{ e.occurredAt | date: 'HH:mm:ss' }}</span>
                <span class="type">{{ e.type }}</span>
                <span class="topic">{{ e.publishedAt ? topicOf(e) : 'pending' }}</span>
              </div>
              <div class="stream-line" *ngIf="recentEvents().length === 0">
                <span class="topic">no events recorded</span>
              </div>
            </div>
          </div>
        </div>
      </ng-container>
    </section>
  `,
  styles: [
    `
      .page-dashboard { display: grid; gap: 4px; }
      .loading { display: flex; justify-content: center; padding: 32px; }
      .error-box {
        color: var(--ops-danger);
        background: var(--ops-danger-bg);
        border-radius: 8px;
        padding: 12px 16px;
        font-size: 13px;
      }
      .stat-sub { font-size: 14px; color: var(--ops-muted); }
      .dashboard-panels {
        display: grid;
        grid-template-columns: 1.5fr 1fr;
        gap: 12px;
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
      .stream-title { color: var(--ops-muted-deep); font-weight: 400; font-size: 12px; }
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
      .panel-table .ellipsis { max-width: 160px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
      .panel-table .empty { color: var(--ops-muted); text-align: center; padding: 20px; }
      .stream-lines { padding: 10px 16px; font-family: var(--ops-mono); font-size: 12px; line-height: 2; }
      .stream-line { display: flex; gap: 10px; align-items: baseline; }
      .mark-ok { color: var(--ops-accent); }
      .mark-pending { color: var(--ops-warn); }
      .time { color: var(--ops-muted); }
      .type { color: var(--ops-text-dim); }
      .topic { color: var(--ops-muted-deep); margin-left: auto; }
      @media (max-width: 1100px) { .dashboard-panels { grid-template-columns: 1fr; } }
    `
  ]
})
export class DashboardComponent implements OnInit {
  private readonly orderApi = inject(OrderApiService);
  private readonly productApi = inject(ProductApiService);
  private readonly inventoryApi = inject(InventoryApiService);
  private readonly outboxApi = inject(OutboxApiService);

  readonly stats = signal<DashboardStats | null>(null);
  readonly recentOrders = signal<Array<OrderApiResponse & { total: number }>>([]);
  readonly recentEvents = signal<OutboxEventApiResponse[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.refresh();
  }

  refresh(): void {
    this.loading.set(true);
    this.error.set(null);

    forkJoin({
      orders: this.orderApi.listOrders(),
      products: this.productApi.listProducts(),
      inventory: this.inventoryApi.listInventory(),
      events: this.outboxApi.listEvents()
    }).subscribe({
      next: ({ orders, products, inventory, events }) => {
        this.stats.set({
          pendingOrders: orders.filter((o) => o.status === 'CREATED').length,
          paidOrders: orders.filter((o) => o.status === 'PAID').length,
          activeProducts: products.filter((p) => p.active).length,
          totalProducts: products.length,
          lowStock: inventory.filter((i) => i.available > 0 && i.available <= 10).length,
          outOfStock: inventory.filter((i) => i.available === 0).length,
          unpublishedEvents: events.filter((e) => !e.publishedAt).length
        });

        this.recentOrders.set(
          [...orders]
            .sort((a, b) => b.createdAt.localeCompare(a.createdAt))
            .slice(0, 5)
            .map((order) => ({
              ...order,
              total: order.items.reduce((sum, item) => sum + item.unitPriceAtPurchase * item.quantity, 0)
            }))
        );

        this.recentEvents.set(
          [...events].sort((a, b) => b.occurredAt.localeCompare(a.occurredAt)).slice(0, 6)
        );

        this.loading.set(false);
      },
      error: () => {
        this.error.set('Could not load dashboard data');
        this.loading.set(false);
      }
    });
  }

  topicOf(event: OutboxEventApiResponse): string {
    return event.aggregateType === 'ORDER' ? 'order-events' : 'product-events';
  }
}

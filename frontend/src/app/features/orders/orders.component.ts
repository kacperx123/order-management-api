import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatTableModule } from '@angular/material/table';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';
import { OrderDetailDialogComponent } from './order-detail-dialog.component';
import { ConfirmActionDialogComponent } from './confirm-action-dialog.component';
import { OrderApiResponse, OrderApiService } from '../../shared/services/api.service';

type OrderRow = OrderApiResponse & { total: number };

@Component({
  standalone: true,
  selector: 'app-orders',
  imports: [
    CommonModule,
    MatTableModule,
    MatIconModule,
    MatButtonModule,
    MatDialogModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    PageHeaderComponent
  ],
  template: `
    <section class="page-section">
      <app-page-header title="Orders" subtitle="All customer orders across the system">
        <button mat-stroked-button (click)="refresh()">Refresh</button>
      </app-page-header>

      <div class="stat-cards">
        <div class="stat-card">
          <div class="stat-label">Total orders</div>
          <div class="stat-value">{{ orders().length }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">Pending payment</div>
          <div class="stat-value warn">{{ pendingCount() }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">Paid</div>
          <div class="stat-value ok">{{ paidCount() }}</div>
        </div>
      </div>

      <div class="loading" *ngIf="loading()">
        <mat-spinner diameter="28"></mat-spinner>
      </div>

      <div class="error-box" *ngIf="error()">{{ error() }}</div>

      <mat-table [dataSource]="orders()" *ngIf="!loading() && !error()">
        <ng-container matColumnDef="id">
          <mat-header-cell *matHeaderCellDef>Order</mat-header-cell>
          <mat-cell *matCellDef="let o" class="cell-id">{{ o.id.slice(0, 8) }}…</mat-cell>
        </ng-container>

        <ng-container matColumnDef="customer">
          <mat-header-cell *matHeaderCellDef>Customer</mat-header-cell>
          <mat-cell *matCellDef="let o">{{ o.customerEmail }}</mat-cell>
        </ng-container>

        <ng-container matColumnDef="status">
          <mat-header-cell *matHeaderCellDef>Status</mat-header-cell>
          <mat-cell *matCellDef="let o">
            <span class="status-chip" [ngClass]="o.status.toLowerCase()">{{ o.status }}</span>
          </mat-cell>
        </ng-container>

        <ng-container matColumnDef="createdAt">
          <mat-header-cell *matHeaderCellDef>Created</mat-header-cell>
          <mat-cell *matCellDef="let o">{{ o.createdAt | date: 'short' }}</mat-cell>
        </ng-container>

        <ng-container matColumnDef="total">
          <mat-header-cell *matHeaderCellDef>Total</mat-header-cell>
          <mat-cell *matCellDef="let o" class="cell-number">{{ o.total | number: '1.2-2' }}</mat-cell>
        </ng-container>

        <ng-container matColumnDef="actions">
          <mat-header-cell *matHeaderCellDef></mat-header-cell>
          <mat-cell *matCellDef="let o">
            <div class="table-actions">
              <button mat-icon-button matTooltip="Details" (click)="openDetails(o)">
                <mat-icon>visibility</mat-icon>
              </button>
              <button mat-stroked-button *ngIf="o.status === 'CREATED'" (click)="confirmAction(o, 'PAY')">
                Pay
              </button>
              <button mat-button color="warn" *ngIf="o.status === 'CREATED'" (click)="confirmAction(o, 'CANCEL')">
                Cancel
              </button>
            </div>
          </mat-cell>
        </ng-container>

        <mat-header-row *matHeaderRowDef="columns"></mat-header-row>
        <mat-row *matRowDef="let row; columns: columns"></mat-row>
      </mat-table>
    </section>
  `,
  styles: [
    `
      .loading { display: flex; justify-content: center; padding: 32px; }
      .error-box {
        color: var(--ops-danger);
        background: var(--ops-danger-bg);
        border-radius: 8px;
        padding: 12px 16px;
        font-size: 13px;
      }
    `
  ]
})
export class OrdersComponent implements OnInit {
  private readonly dialog = inject(MatDialog);
  private readonly orderApi = inject(OrderApiService);

  columns = ['id', 'customer', 'status', 'createdAt', 'total', 'actions'];
  readonly orders = signal<OrderRow[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  readonly pendingCount = computed(() => this.orders().filter((o) => o.status === 'CREATED').length);
  readonly paidCount = computed(() => this.orders().filter((o) => o.status === 'PAID').length);

  ngOnInit(): void {
    this.refresh();
  }

  openDetails(order: OrderRow): void {
    this.dialog.open(OrderDetailDialogComponent, { data: order, width: '640px' });
  }

  confirmAction(order: OrderRow, action: 'PAY' | 'CANCEL'): void {
    const ref = this.dialog.open(ConfirmActionDialogComponent, {
      data: { order, action },
      width: '420px'
    });
    ref.afterClosed().subscribe((res) => {
      if (res === true) {
        const request$ =
          action === 'PAY' ? this.orderApi.payOrder(order.id) : this.orderApi.cancelOrder(order.id);
        request$.subscribe({
          next: () => this.refresh(),
          error: () => this.error.set('Action failed. Refresh and try again.')
        });
      }
    });
  }

  refresh(): void {
    this.loading.set(true);
    this.error.set(null);
    this.orderApi.listOrders().subscribe({
      next: (orders) => {
        this.orders.set(
          orders.map((order) => ({
            ...order,
            total: order.items.reduce((sum, item) => sum + item.unitPriceAtPurchase * item.quantity, 0)
          }))
        );
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Could not load orders');
        this.loading.set(false);
      }
    });
  }
}

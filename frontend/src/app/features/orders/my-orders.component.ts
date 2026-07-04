import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';
import { OrderApiResponse, OrderApiService } from '../../shared/services/api.service';
import { NewOrderDialogComponent } from './new-order-dialog.component';

@Component({
  standalone: true,
  selector: 'app-my-orders',
  imports: [
    CommonModule,
    MatTableModule,
    MatButtonModule,
    MatDialogModule,
    MatProgressSpinnerModule,
    PageHeaderComponent
  ],
  template: `
    <section class="page-section">
      <app-page-header title="My orders" subtitle="Orders placed with your account">
        <button mat-flat-button color="primary" (click)="openNewOrder()">New order</button>
      </app-page-header>

      <div class="loading" *ngIf="loading()">
        <mat-spinner diameter="28"></mat-spinner>
      </div>

      <div class="error-box" *ngIf="error()">{{ error() }}</div>

      <div class="empty-box" *ngIf="!loading() && !error() && data().length === 0">
        <p>No orders yet</p>
        <button mat-stroked-button (click)="openNewOrder()">Place your first order</button>
      </div>

      <mat-table [dataSource]="data()" *ngIf="!loading() && data().length > 0">
        <ng-container matColumnDef="id">
          <mat-header-cell *matHeaderCellDef>Order</mat-header-cell>
          <mat-cell *matCellDef="let el" class="cell-id">{{ el.id.slice(0, 8) }}…</mat-cell>
        </ng-container>

        <ng-container matColumnDef="items">
          <mat-header-cell *matHeaderCellDef>Items</mat-header-cell>
          <mat-cell *matCellDef="let el">{{ itemsSummary(el) }}</mat-cell>
        </ng-container>

        <ng-container matColumnDef="total">
          <mat-header-cell *matHeaderCellDef>Total</mat-header-cell>
          <mat-cell *matCellDef="let el" class="cell-number">{{ total(el) | number: '1.2-2' }}</mat-cell>
        </ng-container>

        <ng-container matColumnDef="status">
          <mat-header-cell *matHeaderCellDef>Status</mat-header-cell>
          <mat-cell *matCellDef="let el">
            <span class="status-chip" [ngClass]="el.status.toLowerCase()">{{ el.status }}</span>
          </mat-cell>
        </ng-container>

        <ng-container matColumnDef="created">
          <mat-header-cell *matHeaderCellDef>Created</mat-header-cell>
          <mat-cell *matCellDef="let el">{{ el.createdAt | date: 'short' }}</mat-cell>
        </ng-container>

        <ng-container matColumnDef="actions">
          <mat-header-cell *matHeaderCellDef></mat-header-cell>
          <mat-cell *matCellDef="let el">
            <div class="table-actions" *ngIf="el.status === 'CREATED'">
              <button mat-stroked-button (click)="pay(el)">Pay</button>
              <button mat-button color="warn" (click)="cancel(el)">Cancel</button>
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
      .empty-box {
        text-align: center;
        padding: 40px;
        color: var(--ops-muted);
        background: var(--ops-panel);
        border: 1px dashed var(--ops-border);
        border-radius: 8px;
      }
    `
  ]
})
export class MyOrdersComponent implements OnInit {
  private readonly orderApi = inject(OrderApiService);
  private readonly dialog = inject(MatDialog);

  columns = ['id', 'items', 'total', 'status', 'created', 'actions'];
  readonly data = signal<OrderApiResponse[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.refresh();
  }

  refresh(): void {
    this.loading.set(true);
    this.error.set(null);
    this.orderApi.listMyOrders().subscribe({
      next: (orders) => {
        this.data.set(orders);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Could not load your orders');
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

  pay(order: OrderApiResponse): void {
    this.orderApi.payOrder(order.id).subscribe({
      next: () => this.refresh(),
      error: () => this.error.set('Payment failed. Refresh and try again.')
    });
  }

  cancel(order: OrderApiResponse): void {
    this.orderApi.cancelOrder(order.id).subscribe({
      next: () => this.refresh(),
      error: () => this.error.set('Cancel failed. Refresh and try again.')
    });
  }

  itemsSummary(order: OrderApiResponse): string {
    return order.items.map((i) => `${i.quantity}× ${i.productName}`).join(', ');
  }

  total(order: OrderApiResponse): number {
    return order.items.reduce((sum, i) => sum + i.quantity * i.unitPriceAtPurchase, 0);
  }
}

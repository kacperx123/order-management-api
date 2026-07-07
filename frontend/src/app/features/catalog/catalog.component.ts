import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';
import { ShortIdPipe } from '../../shared/pipes/short-id.pipe';
import { ProductApiResponse, ProductApiService } from '../../shared/services/api.service';
import { NewOrderDialogComponent } from '../orders/new-order-dialog.component';

type CatalogRow = ProductApiResponse & { stockStatus: 'IN STOCK' | 'LOW STOCK' | 'OUT OF STOCK' };

@Component({
  standalone: true,
  selector: 'app-catalog',
  imports: [
    CommonModule,
    MatTableModule,
    MatButtonModule,
    MatDialogModule,
    MatProgressSpinnerModule,
    PageHeaderComponent,
    ShortIdPipe
  ],
  template: `
    <section class="page-section">
      <app-page-header title="Catalog" subtitle="Products available to order, with live stock">
        <button mat-flat-button color="primary" (click)="openNewOrder()">New order</button>
      </app-page-header>

      <div class="loading" *ngIf="loading()">
        <mat-spinner diameter="28"></mat-spinner>
      </div>

      <div class="error-box" *ngIf="error()">{{ error() }}</div>

      <div class="empty-box" *ngIf="!loading() && !error() && products().length === 0">
        <p>No products available right now</p>
      </div>

      <mat-table [dataSource]="products()" *ngIf="!loading() && !error() && products().length > 0">
        <ng-container matColumnDef="id">
          <mat-header-cell *matHeaderCellDef>Ref</mat-header-cell>
          <mat-cell *matCellDef="let p" class="cell-id">{{ p.id | shortId: 'PRD' }}</mat-cell>
        </ng-container>

        <ng-container matColumnDef="name">
          <mat-header-cell *matHeaderCellDef>Product</mat-header-cell>
          <mat-cell *matCellDef="let p">{{ p.name }}</mat-cell>
        </ng-container>

        <ng-container matColumnDef="price">
          <mat-header-cell *matHeaderCellDef>Price</mat-header-cell>
          <mat-cell *matCellDef="let p" class="cell-number">{{ p.price | number: '1.2-2' }}</mat-cell>
        </ng-container>

        <ng-container matColumnDef="available">
          <mat-header-cell *matHeaderCellDef>Available</mat-header-cell>
          <mat-cell *matCellDef="let p" class="cell-number">{{ p.available }}</mat-cell>
        </ng-container>

        <ng-container matColumnDef="status">
          <mat-header-cell *matHeaderCellDef>Stock</mat-header-cell>
          <mat-cell *matCellDef="let p">
            <span class="status-chip" [ngClass]="statusClass(p.stockStatus)">{{ p.stockStatus }}</span>
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
export class CatalogComponent implements OnInit {
  private readonly productApi = inject(ProductApiService);
  private readonly dialog = inject(MatDialog);

  columns = ['id', 'name', 'price', 'available', 'status'];
  readonly products = signal<CatalogRow[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.refresh();
  }

  statusClass(status: CatalogRow['stockStatus']): string {
    return status === 'IN STOCK' ? 'ok' : status === 'LOW STOCK' ? 'low' : 'out';
  }

  refresh(): void {
    this.loading.set(true);
    this.error.set(null);
    this.productApi.listProducts(true).subscribe({
      next: (products) => {
        this.products.set(
          products.map((product) => ({
            ...product,
            stockStatus:
              product.available === 0
                ? 'OUT OF STOCK'
                : product.available <= 10
                  ? 'LOW STOCK'
                  : 'IN STOCK'
          }))
        );
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Could not load the catalog');
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
}

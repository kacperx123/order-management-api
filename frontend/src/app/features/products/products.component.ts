import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatTableModule } from '@angular/material/table';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';
import { ConfirmDialogComponent } from '../../shared/components/confirm-dialog/confirm-dialog.component';
import { ProductApiResponse, ProductApiService } from '../../shared/services/api.service';
import { AddProductDialogComponent } from './add-product-dialog.component';
import { EditProductDialogComponent } from './edit-product-dialog.component';
import { AdjustStockDialogComponent } from './adjust-stock-dialog.component';

@Component({
  standalone: true,
  selector: 'app-products',
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
      <app-page-header title="Products" subtitle="Catalog and stock management">
        <button mat-flat-button color="primary" (click)="openAdd()">Add product</button>
      </app-page-header>

      <div class="loading" *ngIf="loading()">
        <mat-spinner diameter="28"></mat-spinner>
      </div>

      <div class="error-box" *ngIf="error()">{{ error() }}</div>

      <mat-table [dataSource]="data()" *ngIf="!loading() && !error()">
        <ng-container matColumnDef="name">
          <mat-header-cell *matHeaderCellDef>Name</mat-header-cell>
          <mat-cell *matCellDef="let el">{{ el.name }}</mat-cell>
        </ng-container>

        <ng-container matColumnDef="price">
          <mat-header-cell *matHeaderCellDef>Price</mat-header-cell>
          <mat-cell *matCellDef="let el" class="cell-number">{{ el.price | number: '1.2-2' }}</mat-cell>
        </ng-container>

        <ng-container matColumnDef="stock">
          <mat-header-cell *matHeaderCellDef>Stock</mat-header-cell>
          <mat-cell *matCellDef="let el" class="cell-number">{{ el.available }}</mat-cell>
        </ng-container>

        <ng-container matColumnDef="active">
          <mat-header-cell *matHeaderCellDef>Status</mat-header-cell>
          <mat-cell *matCellDef="let el">
            <span class="status-chip" [ngClass]="el.active ? 'active' : 'inactive'">
              {{ el.active ? 'ACTIVE' : 'INACTIVE' }}
            </span>
          </mat-cell>
        </ng-container>

        <ng-container matColumnDef="created">
          <mat-header-cell *matHeaderCellDef>Created</mat-header-cell>
          <mat-cell *matCellDef="let el">{{ el.createdAt | date: 'short' }}</mat-cell>
        </ng-container>

        <ng-container matColumnDef="actions">
          <mat-header-cell *matHeaderCellDef></mat-header-cell>
          <mat-cell *matCellDef="let el">
            <div class="table-actions">
              <button mat-icon-button matTooltip="Adjust stock" (click)="openAdjustStock(el)">
                <mat-icon>tune</mat-icon>
              </button>
              <button mat-icon-button matTooltip="Edit" (click)="openEdit(el)">
                <mat-icon>edit</mat-icon>
              </button>
              <button
                mat-icon-button
                color="warn"
                matTooltip="Deactivate"
                (click)="deactivate(el)"
                [disabled]="!el.active"
              >
                <mat-icon>block</mat-icon>
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
export class ProductsComponent implements OnInit {
  private readonly dialog = inject(MatDialog);
  private readonly productApi = inject(ProductApiService);

  columns = ['name', 'price', 'stock', 'active', 'created', 'actions'];
  readonly data = signal<ProductApiResponse[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.refresh();
  }

  refresh(): void {
    this.loading.set(true);
    this.error.set(null);
    this.productApi.listProducts().subscribe({
      next: (products) => {
        this.data.set(products);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Could not load products');
        this.loading.set(false);
      }
    });
  }

  openAdd(): void {
    this.dialog
      .open(AddProductDialogComponent, { width: '480px' })
      .afterClosed()
      .subscribe((created) => {
        if (created) {
          this.refresh();
        }
      });
  }

  openEdit(product: ProductApiResponse): void {
    this.dialog
      .open(EditProductDialogComponent, { width: '480px', data: product })
      .afterClosed()
      .subscribe((updated) => {
        if (updated) {
          this.refresh();
        }
      });
  }

  openAdjustStock(product: ProductApiResponse): void {
    this.dialog
      .open(AdjustStockDialogComponent, {
        width: '440px',
        data: { productId: product.id, productName: product.name, available: product.available }
      })
      .afterClosed()
      .subscribe((updated) => {
        if (updated) {
          this.refresh();
        }
      });
  }

  deactivate(product: ProductApiResponse): void {
    this.dialog
      .open(ConfirmDialogComponent, {
        width: '420px',
        data: {
          title: 'Deactivate product',
          message: `"${product.name}" will no longer be orderable. Existing orders and stock are kept.`,
          confirmLabel: 'Deactivate'
        }
      })
      .afterClosed()
      .subscribe((confirmed) => {
        if (!confirmed) {
          return;
        }
        this.productApi.updateProduct(product.id, { active: false }).subscribe({
          next: () => this.refresh(),
          error: () => this.error.set('Could not deactivate the product')
        });
      });
  }
}

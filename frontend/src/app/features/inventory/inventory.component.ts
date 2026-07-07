import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatTableModule } from '@angular/material/table';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';
import { ShortIdPipe } from '../../shared/pipes/short-id.pipe';
import { InventoryApiResponse, InventoryApiService } from '../../shared/services/api.service';
import { AdjustStockDialogComponent } from '../products/adjust-stock-dialog.component';

type InventoryRow = InventoryApiResponse & { stockStatus: 'IN STOCK' | 'LOW STOCK' | 'OUT OF STOCK' };

@Component({
  standalone: true,
  selector: 'app-inventory',
  imports: [
    CommonModule,
    MatTableModule,
    MatIconModule,
    MatButtonModule,
    MatDialogModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    PageHeaderComponent,
    ShortIdPipe
  ],
  template: `
    <section class="page-section">
      <app-page-header
        title="Inventory"
        subtitle="Stock levels with optimistic-locking version per row"
      >
        <button mat-stroked-button (click)="refresh()">Reload</button>
      </app-page-header>

      <div class="loading" *ngIf="loading()">
        <mat-spinner diameter="28"></mat-spinner>
      </div>

      <div class="error-box" *ngIf="error()">{{ error() }}</div>

      <mat-table [dataSource]="inventory()" *ngIf="!loading() && !error()">
        <ng-container matColumnDef="name">
          <mat-header-cell *matHeaderCellDef>Product</mat-header-cell>
          <mat-cell *matCellDef="let el">{{ el.productName }}</mat-cell>
        </ng-container>

        <ng-container matColumnDef="productId">
          <mat-header-cell *matHeaderCellDef>Product id</mat-header-cell>
          <mat-cell *matCellDef="let el" class="cell-id">{{ el.productId | shortId: 'PRD' }}</mat-cell>
        </ng-container>

        <ng-container matColumnDef="available">
          <mat-header-cell *matHeaderCellDef>Available</mat-header-cell>
          <mat-cell *matCellDef="let el" class="cell-number">{{ el.available }}</mat-cell>
        </ng-container>

        <ng-container matColumnDef="reserved">
          <mat-header-cell *matHeaderCellDef>Reserved</mat-header-cell>
          <mat-cell *matCellDef="let el" class="cell-number">{{ el.reserved }}</mat-cell>
        </ng-container>

        <ng-container matColumnDef="version">
          <mat-header-cell *matHeaderCellDef>Version</mat-header-cell>
          <mat-cell *matCellDef="let el" class="cell-number">v{{ el.version }}</mat-cell>
        </ng-container>

        <ng-container matColumnDef="status">
          <mat-header-cell *matHeaderCellDef>Status</mat-header-cell>
          <mat-cell *matCellDef="let el">
            <span class="status-chip" [ngClass]="statusClass(el.stockStatus)">{{ el.stockStatus }}</span>
          </mat-cell>
        </ng-container>

        <ng-container matColumnDef="actions">
          <mat-header-cell *matHeaderCellDef></mat-header-cell>
          <mat-cell *matCellDef="let el">
            <button mat-icon-button matTooltip="Adjust stock" (click)="openAdjust(el)">
              <mat-icon>tune</mat-icon>
            </button>
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
export class InventoryComponent implements OnInit {
  private readonly inventoryApi = inject(InventoryApiService);
  private readonly dialog = inject(MatDialog);

  columns = ['name', 'productId', 'available', 'reserved', 'version', 'status', 'actions'];
  readonly inventory = signal<InventoryRow[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.refresh();
  }

  statusClass(status: InventoryRow['stockStatus']): string {
    return status === 'IN STOCK' ? 'ok' : status === 'LOW STOCK' ? 'low' : 'out';
  }

  refresh(): void {
    this.loading.set(true);
    this.error.set(null);
    this.inventoryApi.listInventory().subscribe({
      next: (inventory) => {
        this.inventory.set(
          inventory.map((item) => ({
            ...item,
            stockStatus:
              item.available === 0 ? 'OUT OF STOCK' : item.available <= 10 ? 'LOW STOCK' : 'IN STOCK'
          }))
        );
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Could not load inventory');
        this.loading.set(false);
      }
    });
  }

  openAdjust(row: InventoryRow): void {
    this.dialog
      .open(AdjustStockDialogComponent, {
        width: '440px',
        data: { productId: row.productId, productName: row.productName, available: row.available }
      })
      .afterClosed()
      .subscribe((updated) => {
        if (updated) {
          this.refresh();
        }
      });
  }
}

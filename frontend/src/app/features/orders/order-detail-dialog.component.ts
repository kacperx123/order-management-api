import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatListModule } from '@angular/material/list';
import { ShortIdPipe } from '../../shared/pipes/short-id.pipe';

@Component({
  standalone: true,
  selector: 'app-order-detail-dialog',
  imports: [CommonModule, MatDialogModule, MatButtonModule, MatListModule, ShortIdPipe],
  template: `
    <h2 mat-dialog-title>Order Details</h2>
    <div mat-dialog-content>
      <p><strong>Ref:</strong> <span class="mono">{{ data.id | shortId: 'ORD' }}</span></p>
      <p><strong>Customer:</strong> {{data.customerEmail}}</p>
      <p><strong>Status:</strong> {{data.status}}</p>

      <mat-list>
        <mat-list-item *ngFor="let item of (data.items || [])">
          {{item.productName}} — {{item.quantity}} x {{item.unitPriceAtPurchase | currency}}
        </mat-list-item>
      </mat-list>
    </div>
    <div mat-dialog-actions style="display:flex;justify-content:flex-end;gap:8px">
      <button mat-button mat-dialog-close>Close</button>
    </div>
  `
})
export class OrderDetailDialogComponent {
  constructor(@Inject(MAT_DIALOG_DATA) public data: any) {}
}

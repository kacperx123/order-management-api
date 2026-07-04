import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatIconModule } from '@angular/material/icon';
import {
  OrderApiService,
  ProductApiResponse,
  ProductApiService
} from '../../shared/services/api.service';

@Component({
  standalone: true,
  selector: 'app-new-order-dialog',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatIconModule
  ],
  template: `
    <h2 mat-dialog-title>New order</h2>
    <div mat-dialog-content>
      <form [formGroup]="form">
        <div formArrayName="items">
          <div class="item-row" *ngFor="let item of items.controls; let i = index" [formGroupName]="i">
            <mat-form-field appearance="outline" class="product-field">
              <mat-label>Product</mat-label>
              <mat-select formControlName="productId">
                <mat-option *ngFor="let p of products()" [value]="p.id" [disabled]="p.available === 0">
                  {{ p.name }} — {{ p.price | number: '1.2-2' }} ({{ p.available }} in stock)
                </mat-option>
              </mat-select>
            </mat-form-field>

            <mat-form-field appearance="outline" class="qty-field">
              <mat-label>Qty</mat-label>
              <input matInput type="number" min="1" formControlName="quantity" />
            </mat-form-field>

            <button
              mat-icon-button
              type="button"
              aria-label="Remove item"
              (click)="removeItem(i)"
              [disabled]="items.length === 1"
            >
              <mat-icon>close</mat-icon>
            </button>
          </div>
        </div>

        <button mat-stroked-button type="button" (click)="addItem()">
          <mat-icon>add</mat-icon>
          Add item
        </button>

        <div class="dialog-error" *ngIf="error()">{{ error() }}</div>
      </form>
    </div>
    <div mat-dialog-actions align="end">
      <button mat-button mat-dialog-close [disabled]="saving()">Cancel</button>
      <button mat-flat-button color="primary" (click)="save()" [disabled]="saving() || form.invalid">
        Place order
      </button>
    </div>
  `,
  styles: [
    `
      .item-row { display: flex; gap: 10px; align-items: baseline; }
      .product-field { flex: 1; }
      .qty-field { width: 90px; }
      .dialog-error {
        color: var(--ops-danger);
        background: var(--ops-danger-bg);
        border-radius: 6px;
        padding: 8px 12px;
        font-size: 12.5px;
        margin-top: 12px;
      }
    `
  ]
})
export class NewOrderDialogComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly productApi = inject(ProductApiService);
  private readonly orderApi = inject(OrderApiService);
  private readonly dialogRef = inject(MatDialogRef<NewOrderDialogComponent>);

  readonly products = signal<ProductApiResponse[]>([]);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);

  form = this.fb.group({
    items: this.fb.array([this.createItemGroup()])
  });

  ngOnInit(): void {
    this.productApi.listProducts(true).subscribe({
      next: (products) => this.products.set(products),
      error: () => this.error.set('Could not load products')
    });
  }

  get items(): FormArray<FormGroup> {
    return this.form.get('items') as FormArray<FormGroup>;
  }

  addItem(): void {
    this.items.push(this.createItemGroup());
  }

  removeItem(index: number): void {
    this.items.removeAt(index);
  }

  save(): void {
    if (this.form.invalid || this.saving()) {
      return;
    }
    this.saving.set(true);
    this.error.set(null);

    const items = this.items.controls.map((group) => ({
      productId: group.get('productId')!.value as string,
      quantity: Number(group.get('quantity')!.value)
    }));

    this.orderApi.createOrder({ items }).subscribe({
      next: (order) => this.dialogRef.close(order),
      error: (err) => {
        this.saving.set(false);
        this.error.set(err?.error?.message ?? 'Could not place the order');
      }
    });
  }

  private createItemGroup(): FormGroup {
    return this.fb.group({
      productId: ['', Validators.required],
      quantity: [1, [Validators.required, Validators.min(1)]]
    });
  }
}

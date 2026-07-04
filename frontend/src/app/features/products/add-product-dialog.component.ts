import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { ProductApiService } from '../../shared/services/api.service';

@Component({
  standalone: true,
  selector: 'app-add-product-dialog',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatCheckboxModule
  ],
  template: `
    <h2 mat-dialog-title>Add product</h2>
    <div mat-dialog-content>
      <form [formGroup]="form" (ngSubmit)="save()">
        <mat-form-field appearance="outline" class="full">
          <mat-label>Name</mat-label>
          <input matInput formControlName="name" />
          <mat-error *ngIf="form.controls.name.hasError('required')">Enter a product name</mat-error>
        </mat-form-field>

        <mat-form-field appearance="outline" class="full">
          <mat-label>Price</mat-label>
          <input matInput type="number" min="0.01" step="0.01" formControlName="price" />
          <mat-error *ngIf="form.controls.price.errors">Enter a price of at least 0.01</mat-error>
        </mat-form-field>

        <mat-form-field appearance="outline" class="full">
          <mat-label>Initial stock</mat-label>
          <input matInput type="number" min="0" step="1" formControlName="initialStock" />
          <mat-error *ngIf="form.controls.initialStock.errors">Enter 0 or more</mat-error>
        </mat-form-field>

        <mat-checkbox formControlName="active">Active (available for ordering)</mat-checkbox>

        <div class="dialog-error" *ngIf="error()">{{ error() }}</div>
      </form>
    </div>
    <div mat-dialog-actions align="end">
      <button mat-button mat-dialog-close [disabled]="saving()">Cancel</button>
      <button mat-flat-button color="primary" (click)="save()" [disabled]="saving() || form.invalid">
        Save
      </button>
    </div>
  `,
  styles: [
    `
      .full { width: 100%; }
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
export class AddProductDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly productApi = inject(ProductApiService);
  private readonly dialogRef = inject(MatDialogRef<AddProductDialogComponent>);

  readonly saving = signal(false);
  readonly error = signal<string | null>(null);

  form = this.fb.nonNullable.group({
    name: ['', Validators.required],
    price: [null as number | null, [Validators.required, Validators.min(0.01)]],
    initialStock: [0, [Validators.required, Validators.min(0)]],
    active: [true]
  });

  save(): void {
    if (this.form.invalid || this.saving()) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    this.error.set(null);

    const value = this.form.getRawValue();

    this.productApi
      .createProduct({
        name: value.name,
        price: Number(value.price),
        initialStock: Number(value.initialStock),
        active: value.active
      })
      .subscribe({
        next: (created) => this.dialogRef.close(created),
        error: (err) => {
          this.saving.set(false);
          this.error.set(err?.error?.message ?? 'Could not create the product');
        }
      });
  }
}

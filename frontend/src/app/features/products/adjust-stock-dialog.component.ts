import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { ProductApiService, UpdateStockRequest } from '../../shared/services/api.service';

export interface AdjustStockDialogData {
  productId: string;
  productName: string;
  available: number;
}

@Component({
  standalone: true,
  selector: 'app-adjust-stock-dialog',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonToggleModule
  ],
  template: `
    <h2 mat-dialog-title>Adjust stock</h2>
    <div mat-dialog-content>
      <p class="context">
        {{ data.productName }} — current stock:
        <span class="mono">{{ data.available }}</span>
      </p>

      <form [formGroup]="form" (ngSubmit)="save()">
        <mat-button-toggle-group formControlName="mode" class="mode-toggle">
          <mat-button-toggle value="delta">Change by (±)</mat-button-toggle>
          <mat-button-toggle value="setTo">Set to</mat-button-toggle>
        </mat-button-toggle-group>

        <mat-form-field appearance="outline" class="full">
          <mat-label>{{ form.controls.mode.value === 'delta' ? 'Delta (e.g. -5 or 20)' : 'New stock level' }}</mat-label>
          <input matInput type="number" step="1" formControlName="value" />
          <mat-error *ngIf="form.controls.value.errors">Enter a value</mat-error>
        </mat-form-field>

        <p class="preview" *ngIf="form.controls.value.value !== null">
          Result: <span class="mono">{{ resultPreview() }}</span>
        </p>

        <div class="dialog-error" *ngIf="error()">{{ error() }}</div>
      </form>
    </div>
    <div mat-dialog-actions align="end">
      <button mat-button mat-dialog-close [disabled]="saving()">Cancel</button>
      <button mat-flat-button color="primary" (click)="save()" [disabled]="saving() || form.invalid">
        Apply
      </button>
    </div>
  `,
  styles: [
    `
      .full { width: 100%; }
      .context { color: var(--ops-muted); font-size: 13px; margin-top: 0; }
      .mode-toggle { margin-bottom: 16px; }
      .preview { color: var(--ops-muted); font-size: 12.5px; margin: 0 0 4px; }
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
export class AdjustStockDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly productApi = inject(ProductApiService);
  private readonly dialogRef = inject(MatDialogRef<AdjustStockDialogComponent>);
  readonly data = inject<AdjustStockDialogData>(MAT_DIALOG_DATA);

  readonly saving = signal(false);
  readonly error = signal<string | null>(null);

  form = this.fb.nonNullable.group({
    mode: ['delta' as 'delta' | 'setTo'],
    value: [null as number | null, Validators.required]
  });

  resultPreview(): number {
    const value = Number(this.form.controls.value.value ?? 0);
    return this.form.controls.mode.value === 'delta' ? this.data.available + value : value;
  }

  save(): void {
    if (this.form.invalid || this.saving()) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    this.error.set(null);

    const { mode, value } = this.form.getRawValue();
    const request: UpdateStockRequest =
      mode === 'delta' ? { delta: Number(value) } : { setTo: Number(value) };

    this.productApi.adjustStock(this.data.productId, request).subscribe({
      next: (updated) => this.dialogRef.close(updated),
      error: (err) => {
        this.saving.set(false);
        this.error.set(err?.error?.message ?? 'Could not adjust stock');
      }
    });
  }
}

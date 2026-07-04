import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';

@Component({
  standalone: true,
  selector: 'app-confirm-action-dialog',
  imports: [MatDialogModule, MatButtonModule],
  template: `
    <h2 mat-dialog-title>Confirm {{data.action}}</h2>
    <div mat-dialog-content>
      <p>Are you sure you want to {{data.action.toLowerCase()}} order <strong>{{data.order.id}}</strong>?</p>
    </div>
    <div mat-dialog-actions style="display:flex;justify-content:flex-end;gap:8px">
      <button mat-button mat-dialog-close="false">No</button>
      <button mat-flat-button color="primary" [mat-dialog-close]="true">Yes</button>
    </div>
  `
})
export class ConfirmActionDialogComponent {
  constructor(@Inject(MAT_DIALOG_DATA) public data: any) {}
}

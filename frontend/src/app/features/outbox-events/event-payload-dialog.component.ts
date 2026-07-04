import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';

@Component({
  standalone: true,
  selector: 'app-event-payload-dialog',
  imports: [MatDialogModule, MatButtonModule],
  template: `
    <h2 mat-dialog-title>Event Payload</h2>
    <div mat-dialog-content>
      <pre style="white-space:pre-wrap;">{{pretty(data)}}</pre>
    </div>
    <div mat-dialog-actions style="display:flex;justify-content:flex-end;gap:8px">
      <button mat-button mat-dialog-close>Close</button>
    </div>
  `
})
export class EventPayloadDialogComponent {
  constructor(@Inject(MAT_DIALOG_DATA) public data: any) {}
  pretty(d:any){ return JSON.stringify(d, null, 2) }
}

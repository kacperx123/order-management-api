import { Component, Input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

@Component({
  standalone: true,
  selector: 'app-error-alert',
  imports: [MatIconModule],
  template: `
    <div style="display:flex;gap:8px;align-items:flex-start;background:rgba(220,38,38,0.06);padding:12px;border-radius:8px;border:1px solid rgba(220,38,38,0.12)">
      <mat-icon color="warn">error</mat-icon>
      <div>
        <div style="font-weight:600">{{ title }}</div>
        <div class="small-muted">{{ message }}</div>
      </div>
    </div>
  `
})
export class ErrorAlertComponent {
  @Input() title = 'Error';
  @Input() message = '';
}

import { Component, Input } from '@angular/core';
import { MatCardModule } from '@angular/material/card';

@Component({
  standalone: true,
  selector: 'app-empty-state',
  imports: [MatCardModule],
  template: `
    <mat-card>
      <div style="padding:24px;text-align:center">
        <h3>{{ title }}</h3>
        <div class="small-muted">{{ subtitle }}</div>
      </div>
    </mat-card>
  `
})
export class EmptyStateComponent {
  @Input() title = 'No data';
  @Input() subtitle = '';
}

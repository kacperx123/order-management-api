import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';

@Component({
  standalone: true,
  selector: 'app-page-header',
  imports: [CommonModule, MatButtonModule],
  template: `
    <div class="page-header">
      <div>
        <h2>{{ title }}</h2>
        <div class="small-muted">{{ subtitle }}</div>
      </div>
      <div class="actions"><ng-content></ng-content></div>
    </div>
  `,
  styles: [
    `
    h2 { margin:0; font-size:1.25rem }
    .actions { display:flex; gap:8px; }
    `
  ]
})
export class PageHeaderComponent {
  @Input() title = '';
  @Input() subtitle = '';
}

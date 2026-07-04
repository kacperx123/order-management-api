import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  standalone: true,
  selector: 'app-status-badge',
  imports: [CommonModule],
  template: `
    <span [ngClass]="cssClass" class="status-badge">{{ label }}</span>
  `,
  styles: [
    `
    .status-badge { font-weight:600; font-size:0.85rem; padding:4px 8px; border-radius:12px; display:inline-block }
    .status-badge.info { color: var(--primary); background: rgba(37,99,235,0.08); }
    .status-badge.success { color: var(--success); background: rgba(22,163,74,0.08); }
    .status-badge.warn { color: var(--warning); background: rgba(217,119,6,0.08); }
    .status-badge.danger { color: var(--danger); background: rgba(220,38,38,0.06); }
    .status-badge.muted { color: var(--text-muted); background: rgba(107,114,128,0.06); }
    `
  ]
})
export class StatusBadgeComponent {
  @Input() label = '';
  @Input() variant: 'info'|'success'|'warn'|'danger'|'muted' = 'muted';

  get cssClass() {
    return this.variant;
  }
}

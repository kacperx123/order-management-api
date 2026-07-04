import { Component } from '@angular/core';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

@Component({
  standalone: true,
  selector: 'app-loading-spinner',
  imports: [MatProgressSpinnerModule],
  template: `
    <div style="display:flex;align-items:center;justify-content:center;padding:16px">
      <mat-progress-spinner diameter="40" mode="indeterminate"></mat-progress-spinner>
    </div>
  `
})
export class LoadingSpinnerComponent {}

import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';
import { EventPayloadDialogComponent } from './event-payload-dialog.component';
import { OutboxApiService, OutboxEventApiResponse } from '../../shared/services/api.service';

type EventRow = OutboxEventApiResponse & { payload: unknown; status: 'PUBLISHED' | 'PENDING' };

@Component({
  standalone: true,
  selector: 'app-outbox-events',
  imports: [
    CommonModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatDialogModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    PageHeaderComponent
  ],
  template: `
    <section class="page-section">
      <app-page-header title="Events" subtitle="Transactional outbox stream published to Kafka">
        <button mat-stroked-button (click)="refresh()">Refresh</button>
      </app-page-header>

      <div class="stat-cards">
        <div class="stat-card">
          <div class="stat-label">Events total</div>
          <div class="stat-value">{{ events().length }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">Published</div>
          <div class="stat-value ok">{{ publishedCount() }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">Pending</div>
          <div class="stat-value" [ngClass]="pendingCount() === 0 ? 'ok' : 'warn'">{{ pendingCount() }}</div>
        </div>
      </div>

      <div class="loading" *ngIf="loading()">
        <mat-spinner diameter="28"></mat-spinner>
      </div>

      <div class="error-box" *ngIf="error()">{{ error() }}</div>

      <mat-table [dataSource]="events()" *ngIf="!loading() && !error()">
        <ng-container matColumnDef="occurredAt">
          <mat-header-cell *matHeaderCellDef>Occurred</mat-header-cell>
          <mat-cell *matCellDef="let e" class="cell-number">{{ e.occurredAt | date: 'HH:mm:ss dd.MM' }}</mat-cell>
        </ng-container>

        <ng-container matColumnDef="type">
          <mat-header-cell *matHeaderCellDef>Event</mat-header-cell>
          <mat-cell *matCellDef="let e">{{ e.type }}</mat-cell>
        </ng-container>

        <ng-container matColumnDef="aggregate">
          <mat-header-cell *matHeaderCellDef>Aggregate</mat-header-cell>
          <mat-cell *matCellDef="let e">
            <span>{{ e.aggregateType }}</span>
            <span class="cell-id agg-id">{{ e.aggregateId.slice(0, 8) }}…</span>
          </mat-cell>
        </ng-container>

        <ng-container matColumnDef="status">
          <mat-header-cell *matHeaderCellDef>Status</mat-header-cell>
          <mat-cell *matCellDef="let e">
            <span class="status-chip" [ngClass]="e.status === 'PUBLISHED' ? 'published' : 'pending'">
              {{ e.status }}
            </span>
          </mat-cell>
        </ng-container>

        <ng-container matColumnDef="actions">
          <mat-header-cell *matHeaderCellDef></mat-header-cell>
          <mat-cell *matCellDef="let e">
            <button mat-icon-button matTooltip="View payload" aria-label="View payload" (click)="showPayload(e)">
              <mat-icon>data_object</mat-icon>
            </button>
          </mat-cell>
        </ng-container>

        <mat-header-row *matHeaderRowDef="cols"></mat-header-row>
        <mat-row *matRowDef="let row; columns: cols"></mat-row>
      </mat-table>
    </section>
  `,
  styles: [
    `
      .loading { display: flex; justify-content: center; padding: 32px; }
      .error-box {
        color: var(--ops-danger);
        background: var(--ops-danger-bg);
        border-radius: 8px;
        padding: 12px 16px;
        font-size: 13px;
      }
      .agg-id { margin-left: 8px; }
    `
  ]
})
export class OutboxEventsComponent implements OnInit {
  private readonly dialog = inject(MatDialog);
  private readonly outboxApi = inject(OutboxApiService);

  cols = ['occurredAt', 'type', 'aggregate', 'status', 'actions'];
  readonly events = signal<EventRow[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  readonly publishedCount = computed(() => this.events().filter((e) => e.status === 'PUBLISHED').length);
  readonly pendingCount = computed(() => this.events().filter((e) => e.status === 'PENDING').length);

  ngOnInit(): void {
    this.refresh();
  }

  showPayload(e: EventRow): void {
    this.dialog.open(EventPayloadDialogComponent, { data: e.payload, width: '700px' });
  }

  refresh(): void {
    this.loading.set(true);
    this.error.set(null);
    this.outboxApi.listEvents().subscribe({
      next: (events) => {
        this.events.set(
          [...events]
            .sort((a, b) => b.occurredAt.localeCompare(a.occurredAt))
            .map((event) => ({
              ...event,
              payload: this.parsePayload(event.payloadJson),
              status: event.publishedAt ? 'PUBLISHED' : 'PENDING'
            }))
        );
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Could not load outbox events');
        this.loading.set(false);
      }
    });
  }

  private parsePayload(payloadJson: string): unknown {
    try {
      return JSON.parse(payloadJson);
    } catch {
      return payloadJson;
    }
  }
}

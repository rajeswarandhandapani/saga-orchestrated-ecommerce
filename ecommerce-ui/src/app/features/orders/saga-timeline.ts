import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Saga } from './orders.api';

/** Renders the orchestrator's audit trail: events received, commands sent, compensations highlighted. */
@Component({
  selector: 'app-saga-timeline',
  imports: [DatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="saga-head">
      <span class="state state-{{ saga().state }}">{{ saga().state }}</span>
      @if (saga().abortRequested) {
        <span class="muted small">abort requested: {{ saga().abortReason }}</span>
      }
    </div>
    <ol class="timeline">
      @for (s of saga().steps; track $index) {
        <li class="step" [class.sent]="s.kind === 'SENT'" [class.note]="s.kind === 'NOTE'" [class.comp]="s.compensation">
          <span class="time">{{ s.at | date: 'HH:mm:ss.SSS' }}</span>
          <span class="arrow" aria-hidden="true">{{ s.kind === 'SENT' ? '→' : s.kind === 'RECEIVED' ? '←' : '•' }}</span>
          <span class="kind">{{ s.kind === 'SENT' ? 'command' : s.kind === 'RECEIVED' ? 'event' : 'note' }}</span>
          <strong>{{ s.message }}</strong>
          @if (s.compensation) {
            <span class="badge bad">compensation</span>
          }
          @if (s.detail) {
            <span class="muted small">{{ s.detail }}</span>
          }
        </li>
      }
    </ol>
  `,
})
export class SagaTimeline {
  readonly saga = input.required<Saga>();
}

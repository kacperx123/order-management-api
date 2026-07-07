import { Pipe, PipeTransform } from '@angular/core';

/**
 * Turns a raw UUID into a compact, human-friendly reference.
 * `shortId('3f2a9c1b-...', 'ORD')` → `ORD-3F2A9C`.
 */
@Pipe({ name: 'shortId', standalone: true })
export class ShortIdPipe implements PipeTransform {
  transform(id: string | null | undefined, prefix = ''): string {
    if (!id) {
      return '—';
    }
    const short = id.replace(/-/g, '').slice(0, 6).toUpperCase();
    return prefix ? `${prefix}-${short}` : short;
  }
}

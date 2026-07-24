import { provideHttpClient } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { describe, expect, it } from 'vitest';

import { EffectiveAssignmentService } from '../effective-assignment/effective-assignment.service';
import { InstitutionalShellComponent } from './institutional-shell.component';

describe('InstitutionalShellComponent', () => {
  it('ofrece salto al contenido y navegación pública por teclado', () => {
    TestBed.configureTestingModule({
      imports: [InstitutionalShellComponent],
      providers: [
        provideHttpClient(),
        provideRouter([]),
        {
          provide: EffectiveAssignmentService,
          useValue: { selectedId: () => undefined, options: () => [], load: () => of([]), select: () => undefined }
        }
      ]
    });
    const fixture: ComponentFixture<InstitutionalShellComponent> = TestBed.createComponent(InstitutionalShellComponent);
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;

    expect(element.querySelector('a.skip-link')?.getAttribute('href')).toBe('#institutional-content');
    expect(element.querySelector('main')?.getAttribute('tabindex')).toBe('-1');
    expect(element.querySelector('a[href="/consulta-publica"]')).not.toBeNull();
  });
});

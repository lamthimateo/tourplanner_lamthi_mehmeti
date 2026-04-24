/**
 * Smoke test for {@link AppComponent}.
 *
 * <p>Kept intentionally minimal — the app is architected so that meaningful
 * behaviour lives in pure TypeScript classes (`TourViewModel`,
 * `TourLogViewModel`, `ApiService`, `AuthService`) that are unit-tested at
 * the service level. This component spec just verifies the root component
 * can be instantiated under Angular's test bed, which would catch a broken
 * @Component decorator, missing imports or an invalid template.
 */
import { TestBed } from '@angular/core/testing';
import { AppComponent } from './app.component';

describe('AppComponent', () => {
  // Fresh module setup before each test so state doesn't leak across specs.
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AppComponent]
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });
});

/**
 * TourViewModel — MVVM ViewModel for the tour form.
 *
 * In the MVVM pattern used by this application:
 *   - Model      : Tour interface (raw data shape) + ApiService (data access)
 *   - ViewModel  : TourViewModel (holds mutable UI state, validation, conversion)
 *   - View       : AppComponent template (binds to ViewModel via [(ngModel)])
 *
 * The ViewModel is the bridge between the raw Tour model and the UI.  It owns
 * form field values, tracks whether the form has been submitted (to trigger
 * validation messages), validates the fields, and converts back to a plain
 * Tour object for API calls.
 */
import { Tour } from '../models/tour';

export class TourViewModel {
  /** Database ID — undefined when this is a new (unsaved) tour. */
  id?: number;

  /** Display name of the tour. Required, non-blank. */
  name: string = '';

  /** Free-text description of the tour. */
  description: string = '';

  /** Starting location (geocoded by OpenRouteService). Required. */
  origin: string = '';

  /** Ending location (geocoded by OpenRouteService). Required. */
  destination: string = '';

  /** Mode of transport: car | bicycle | walking | running | hiking. Required. */
  transportType: string = 'car';

  /** Route distance in kilometres (filled by "Calculate route"). */
  distance: number = 0;

  /** Estimated travel time in minutes (filled by "Calculate route"). */
  estimatedTime: number = 0;

  /** Absolute path to the stored tour image on the server, or null. */
  imagePath: string | null = null;

  /** Computed: number of tour logs attached to this tour (read-only from server). */
  popularity?: number;

  /** Computed: child-friendliness score 1–10 derived from avg difficulty / distance / time. */
  childFriendliness?: number;

  /**
   * Whether the user has attempted to submit the tour form at least once.
   * Used by the View to decide when to show inline validation error messages.
   */
  submitted: boolean = false;

  // ---------------------------------------------------------------------------
  // Factory helpers
  // ---------------------------------------------------------------------------

  /**
   * Creates a TourViewModel populated from a plain Tour model object.
   * Use this when selecting an existing tour to edit.
   *
   * @param tour  The raw Tour data returned by the API.
   * @returns     A new TourViewModel with all fields copied.
   */
  static from(tour: Tour): TourViewModel {
    const vm = new TourViewModel();
    vm.id = tour.id;
    vm.name = tour.name;
    vm.description = tour.description ?? '';
    vm.origin = tour.origin;
    vm.destination = tour.destination;
    vm.transportType = tour.transportType;
    vm.distance = tour.distance ?? 0;
    vm.estimatedTime = tour.estimatedTime ?? 0;
    vm.imagePath = tour.imagePath ?? null;
    vm.popularity = tour.popularity;
    vm.childFriendliness = tour.childFriendliness;
    return vm;
  }

  // ---------------------------------------------------------------------------
  // Validation
  // ---------------------------------------------------------------------------

  /**
   * Returns true when all required fields pass validation.
   * The View calls this before sending the tour to the API.
   *
   * Required fields: name, origin, destination, transportType (all non-blank).
   */
  isValid(): boolean {
    return (
      this.name.trim().length > 0 &&
      this.origin.trim().length > 0 &&
      this.destination.trim().length > 0 &&
      this.transportType.trim().length > 0
    );
  }

  // ---------------------------------------------------------------------------
  // Model conversion
  // ---------------------------------------------------------------------------

  /**
   * Converts this ViewModel back into a plain Tour object suitable for API calls.
   * The `submitted`, `popularity`, and `childFriendliness` fields are intentionally
   * excluded — they are UI state or server-computed values, not stored fields.
   *
   * @returns A Tour object ready to pass to ApiService.createTour / updateTour.
   */
  toTour(): Tour {
    return {
      id: this.id,
      name: this.name,
      description: this.description,
      origin: this.origin,
      destination: this.destination,
      transportType: this.transportType,
      distance: this.distance,
      estimatedTime: this.estimatedTime,
      imagePath: this.imagePath,
    };
  }
}

/**
 * TourLogViewModel — MVVM ViewModel for the tour-log form.
 *
 * Mirrors the role of TourViewModel but for individual log entries.
 * Holds mutable form state, owns validation logic, and converts to/from
 * the plain TourLog model object used by ApiService.
 *
 * MVVM roles in this application:
 *   - Model      : TourLog interface + ApiService (data access)
 *   - ViewModel  : TourLogViewModel (UI state, validation, conversion)
 *   - View       : AppComponent template (two-way binds via [(ngModel)])
 */
import { TourLog } from '../models/tour-log';

export class TourLogViewModel {
  /** Database ID — undefined for new (unsaved) logs. */
  id?: number;

  /** ISO date string (YYYY-MM-DD) of when the tour was completed. Required. */
  logDate: string = new Date().toISOString().slice(0, 10);

  /** Free-text diary entry for this log. Required, non-blank. */
  comment: string = '';

  /** Subjective difficulty rating from 1 (easy) to 5 (very hard). Required. */
  difficulty: number = 1;

  /** Actual distance covered in kilometres. */
  totalDistance: number = 0;

  /** Actual time spent on the tour in minutes. */
  totalTimeMinutes: number = 0;

  /** Overall satisfaction rating from 0 (terrible) to 10 (excellent). Required. */
  rating: number = 5;

  /**
   * Whether the log form has been submitted at least once.
   * Controls when the View shows inline validation error messages.
   */
  submitted: boolean = false;

  // ---------------------------------------------------------------------------
  // Factory helpers
  // ---------------------------------------------------------------------------

  /**
   * Creates a TourLogViewModel from an existing TourLog model object.
   * Use this when the user clicks a log entry to edit it.
   *
   * @param log  The raw TourLog data returned by the API.
   * @returns    A new TourLogViewModel with all fields copied.
   */
  static from(log: TourLog): TourLogViewModel {
    const vm = new TourLogViewModel();
    vm.id = log.id;
    vm.logDate = log.logDate;
    vm.comment = log.comment;
    vm.difficulty = log.difficulty;
    vm.totalDistance = log.totalDistance;
    vm.totalTimeMinutes = log.totalTimeMinutes;
    vm.rating = log.rating;
    return vm;
  }

  // ---------------------------------------------------------------------------
  // Validation
  // ---------------------------------------------------------------------------

  /**
   * Returns true when all required fields are within acceptable ranges.
   *
   * Rules:
   *   - logDate must be non-empty
   *   - comment must be non-blank
   *   - difficulty must be 1–5
   *   - rating must be 0–10
   */
  isValid(): boolean {
    return (
      this.logDate.length > 0 &&
      this.comment.trim().length > 0 &&
      this.difficulty >= 1 && this.difficulty <= 5 &&
      this.rating >= 0 && this.rating <= 10
    );
  }

  // ---------------------------------------------------------------------------
  // Model conversion
  // ---------------------------------------------------------------------------

  /**
   * Converts this ViewModel to a plain TourLog object for API calls.
   * Excludes the `submitted` flag — that is UI state only.
   *
   * @returns A TourLog object ready to pass to ApiService.createLog / updateLog.
   */
  toLog(): TourLog {
    return {
      id: this.id,
      logDate: this.logDate,
      comment: this.comment,
      difficulty: this.difficulty,
      totalDistance: this.totalDistance,
      totalTimeMinutes: this.totalTimeMinutes,
      rating: this.rating,
    };
  }
}

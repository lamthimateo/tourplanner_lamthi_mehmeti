/**
 * @file tour.ts
 * @description Defines the `Tour` data model used throughout the Tour Planner application.
 * A Tour represents a planned journey between two locations, including metadata such as
 * transport type, distance, estimated duration, and computed attributes (popularity,
 * child-friendliness) that are derived server-side from associated tour logs.
 */

/**
 * Represents a single tour entry in the Tour Planner application.
 *
 * Tours are the primary entity of the application. Each tour captures a planned
 * route from an origin to a destination, along with transport details and optional
 * computed statistics. Computed fields (`popularity`, `childFriendliness`) are
 * read-only values returned by the backend and should not be sent on create/update
 * unless explicitly required.
 */
export interface Tour {
    /**
     * The server-assigned unique identifier for the tour.
     * Optional because it is absent before the tour is persisted (i.e. during creation).
     */
    id?: number;

    /**
     * A short, human-readable name for the tour (e.g. "Vienna City Walk").
     * Required; used as the primary label in the tour list sidebar.
     */
    name: string;

    /**
     * A longer free-text description of the tour, providing context, highlights,
     * or notes about the route. Maximum 2000 characters as enforced by the UI.
     */
    description: string;

    /**
     * The starting location of the tour (e.g. a city name, address, or landmark).
     * Used both as a display label and as the geocoding input for the map route.
     */
    origin: string;

    /**
     * The ending location of the tour (e.g. a city name, address, or landmark).
     * Used both as a display label and as the geocoding input for the map route.
     */
    destination: string;

    /**
     * The mode of transport for the tour. Accepted values are:
     * `"car"`, `"bicycle"`, `"walking"`, `"running"`, `"hiking"`.
     * Passed to OpenRouteService to compute an appropriate route.
     */
    transportType: string;

    /**
     * The total distance of the tour in kilometres.
     * May be entered manually or auto-populated after a successful route calculation
     * via OpenRouteService (where it is derived from `summary.distance / 1000`).
     */
    distance: number;

    /**
     * The estimated duration of the tour in minutes.
     * May be entered manually or auto-populated after a successful route calculation
     * via OpenRouteService (where it is derived from `summary.duration / 60`).
     */
    estimatedTime: number;

    /**
     * An optional server-side path or URL to a map image or route thumbnail for the tour.
     * May be `null` when no image has been generated or associated.
     */
    imagePath?: string | null;

    /**
     * A computed read-only score representing how popular this tour is,
     * calculated server-side as the total number of tour logs associated with the tour.
     * `undefined` when the tour has not yet been saved or when no logs exist.
     */
    popularity?: number;

    /**
     * A computed read-only score (0–10) indicating how child-friendly the tour is.
     * Derived server-side from the average rating and inverse of average difficulty
     * across all associated tour logs. Higher values indicate greater suitability
     * for children. `undefined` when there are no logs to compute from.
     */
    childFriendliness?: number;
}

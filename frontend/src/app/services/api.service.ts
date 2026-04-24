/**
 * @file api.service.ts
 * @description Central HTTP client service for the Tour Planner application.
 *
 * `ApiService` wraps every backend REST endpoint in a typed `Observable`-returning
 * method so that the rest of the application never constructs raw HTTP calls directly.
 * All requests are automatically enriched with the JWT Bearer token by `authInterceptor`.
 *
 * The service covers five functional areas:
 *  - **Tours**: CRUD operations and full-text search.
 *  - **Logs**: CRUD operations scoped to a parent tour.
 *  - **Import / Export**: JSON-based tour data interchange.
 *  - **Reports**: PDF generation (individual tour report and global summary).
 *  - **OpenRouteService (ORS)**: Geocoding and route geometry via the backend proxy.
 *  - **Statistics**: Aggregate metrics across all tours and logs (unique feature).
 */

import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {map} from 'rxjs/operators';
import {Tour} from '../models/tour';
import {TourLog} from '../models/tour-log';

/**
 * Aggregate statistics for the current user's tour portfolio.
 *
 * Returned by `GET /api/stats` and displayed in the Statistics Dashboard panel.
 * All numeric fields are rounded server-side (distances to 2 decimal places,
 * hours to 2 decimal places, ratings to 1 decimal place).
 */
export interface Stats {
    /** Total number of tours owned by the logged-in user. */
    totalTours: number;

    /** Total number of tour log entries across all tours. */
    totalLogs: number;

    /** Cumulative distance covered across all logs, in kilometres. */
    totalDistanceKm: number;

    /** Cumulative time spent across all logs, in hours. */
    totalTimeHours: number;

    /** Overall average rating across all log entries (scale 0–10). */
    avgRating: number;

    /**
     * Per-transport-type breakdown of key metrics.
     * Allows the user to compare how their different travel modes perform.
     */
    byTransportType: {
        /** The transport type label (e.g. `"car"`, `"hiking"`). */
        transportType: string;

        /** Number of tours using this transport type. */
        tourCount: number;

        /** Number of log entries across tours of this transport type. */
        logCount: number;

        /** Average distance per log entry for this transport type, in kilometres. */
        avgDistanceKm: number;

        /** Average log rating for this transport type (scale 0–10). */
        avgRating: number;
    }[];
}

/**
 * Angular service that provides typed wrappers around every Tour Planner REST endpoint.
 *
 * Provided at the root injector level so a single shared instance is used across the
 * entire application. All methods return cold `Observable`s — the HTTP request is only
 * sent when the caller subscribes.
 *
 * Authentication is handled transparently by `authInterceptor`, which attaches the
 * JWT Bearer header before the request leaves the browser.
 *
 * @example
 * constructor(private api: ApiService) {}
 *
 * this.api.getTours().subscribe(tours => this.tours = tours);
 */
@Injectable({providedIn: 'root'})
export class ApiService {
    /**
     * Root URL of the backend REST API.
     * All endpoint paths are constructed relative to this base.
     */
    private readonly baseUrl = 'http://localhost:8081/api';

    /**
     * @param http Angular's `HttpClient` used to issue all HTTP requests.
     */
    constructor(private http: HttpClient) {
    }

    // ---------------------------------------------------------------------------
    // Tours
    // ---------------------------------------------------------------------------

    /**
     * Fetches all tours belonging to the currently authenticated user.
     *
     * @returns `Observable<Tour[]>` — emits the full list of the user's tours,
     *          each enriched with server-computed `popularity` and `childFriendliness`.
     */
    getTours(): Observable<Tour[]> {
        return this.http.get<Tour[]>(`${this.baseUrl}/tours`);
    }

    /**
     * Fetches a single tour by its unique identifier.
     *
     * @param id The numeric primary key of the tour to retrieve.
     * @returns `Observable<Tour>` — emits the matching tour, or errors with 404 if not found.
     */
    getTour(id: number): Observable<Tour> {
        return this.http.get<Tour>(`${this.baseUrl}/tours/${id}`);
    }

    /**
     * Creates a new tour in the backend database.
     *
     * @param tour The tour data to persist. The `id` field must be absent (or `undefined`)
     *             because the backend assigns the primary key on creation.
     * @returns `Observable<Tour>` — emits the newly created tour including the
     *          server-assigned `id`.
     */
    createTour(tour: Tour): Observable<Tour> {
        return this.http.post<Tour>(`${this.baseUrl}/tours`, tour);
    }

    /**
     * Updates an existing tour identified by `id` with the provided data.
     *
     * @param id   The numeric primary key of the tour to update.
     * @param tour The full tour payload with updated field values. The backend
     *             replaces all mutable fields on the persisted entity.
     * @returns `Observable<Tour>` — emits the updated tour as stored by the backend
     *          (including re-computed `popularity` and `childFriendliness`).
     */
    updateTour(id: number, tour: Tour): Observable<Tour> {
        return this.http.put<Tour>(`${this.baseUrl}/tours/${id}`, tour);
    }

    /**
     * Permanently deletes a tour and all of its associated logs from the backend.
     *
     * @param id The numeric primary key of the tour to delete.
     * @returns `Observable<void>` — emits once on success and completes with no value.
     */
    deleteTour(id: number): Observable<void> {
        return this.http.delete<void>(`${this.baseUrl}/tours/${id}`);
    }

    /**
     * Performs a full-text search across tour name, description, origin, destination,
     * and associated log comments on the backend, returning matching tour IDs.
     *
     * The returned IDs are used by the component to highlight or filter the tour list.
     * Note: the component also performs a lightweight client-side filter via `applyFilter()`
     * for immediate responsiveness; this server call enables deeper log-level searching.
     *
     * @param query The search string to match against tour and log text fields.
     * @returns `Observable<number[]>` — emits an array of tour IDs whose content
     *          matches the query.
     */
    searchTours(query: string): Observable<number[]> {
        return this.http.get<number[]>(`${this.baseUrl}/tours/search`, {params: {q: query}});
    }

    // ---------------------------------------------------------------------------
    // Logs
    // ---------------------------------------------------------------------------

    /**
     * Fetches all log entries associated with a specific tour.
     *
     * @param tourId The numeric primary key of the parent tour.
     * @returns `Observable<TourLog[]>` — emits all logs for the tour, ordered by
     *          `logDate` descending (most recent first) as determined by the backend.
     */
    getLogs(tourId: number): Observable<TourLog[]> {
        return this.http.get<TourLog[]>(`${this.baseUrl}/tours/${tourId}/logs`);
    }

    /**
     * Creates a new log entry under the specified tour.
     *
     * Adding a log also causes the backend to re-compute and persist updated
     * `popularity` and `childFriendliness` values for the parent tour.
     *
     * @param tourId The numeric primary key of the parent tour.
     * @param log    The log data to persist. The `id` field must be absent because the
     *               backend assigns the primary key.
     * @returns `Observable<TourLog>` — emits the newly created log including the
     *          server-assigned `id`.
     */
    createLog(tourId: number, log: TourLog): Observable<TourLog> {
        return this.http.post<TourLog>(`${this.baseUrl}/tours/${tourId}/logs`, log);
    }

    /**
     * Updates an existing log entry within the specified tour.
     *
     * @param tourId The numeric primary key of the parent tour.
     * @param logId  The numeric primary key of the log entry to update.
     * @param log    The full log payload with updated field values.
     * @returns `Observable<TourLog>` — emits the updated log as stored by the backend.
     */
    updateLog(tourId: number, logId: number, log: TourLog): Observable<TourLog> {
        return this.http.put<TourLog>(`${this.baseUrl}/tours/${tourId}/logs/${logId}`, log);
    }

    /**
     * Permanently deletes a specific log entry from the specified tour.
     *
     * Deletion also triggers a server-side re-computation of the parent tour's
     * `popularity` and `childFriendliness` scores.
     *
     * @param tourId The numeric primary key of the parent tour.
     * @param logId  The numeric primary key of the log entry to delete.
     * @returns `Observable<void>` — emits once on success and completes with no value.
     */
    deleteLog(tourId: number, logId: number): Observable<void> {
        return this.http.delete<void>(`${this.baseUrl}/tours/${tourId}/logs/${logId}`);
    }

    // ---------------------------------------------------------------------------
    // Import / Export
    // ---------------------------------------------------------------------------

    /**
     * Exports a tour and all of its logs as a structured JSON payload from the backend.
     *
     * The component serialises the emitted value to a `.json` file and triggers a
     * browser download so the user can archive or share their tour data.
     *
     * @param tourId The numeric primary key of the tour to export.
     * @returns `Observable<any>` — emits the JSON export payload (tour + logs combined).
     */
    exportTour(tourId: number): Observable<any> {
        return this.http.get<any>(`${this.baseUrl}/tours/${tourId}/export`);
    }

    /**
     * Imports a previously exported tour JSON payload into the backend, creating
     * a new tour (and its logs) from the provided data.
     *
     * The backend assigns new primary keys, so the imported tour is treated as a
     * brand-new entity even if it originated from this or another user's account.
     *
     * @param data The raw export payload (typically parsed from a `.json` file the user
     *             selects via the file input in the sidebar).
     * @returns `Observable<Tour>` — emits the newly created `Tour` with the
     *          server-assigned `id`.
     */
    importTour(data: any): Observable<Tour> {
        return this.http.post<Tour>(`${this.baseUrl}/tours/import`, data);
    }

    // ---------------------------------------------------------------------------
    // Reports
    // ---------------------------------------------------------------------------

    /**
     * Requests a PDF report for a specific tour from the backend.
     *
     * The report includes tour metadata, the map image, and a table of all associated
     * log entries. The response is a binary blob that the component saves as a file.
     *
     * @param tourId The numeric primary key of the tour for which to generate the report.
     * @returns `Observable<Blob>` — emits a PDF `Blob` ready for browser download.
     *          The `responseType: 'blob'` option prevents `HttpClient` from trying to
     *          parse the binary response as JSON.
     */
    downloadTourReport(tourId: number): Observable<Blob> {
        return this.http.get(`${this.baseUrl}/tours/${tourId}/report`, {responseType: 'blob'});
    }

    /**
     * Requests a global summary PDF report from the backend covering all of the
     * authenticated user's tours and aggregate statistics.
     *
     * @returns `Observable<Blob>` — emits a PDF `Blob` ready for browser download.
     */
    downloadSummaryReport(): Observable<Blob> {
        return this.http.get(`${this.baseUrl}/reports/summary`, {responseType: 'blob'});
    }

    // ---------------------------------------------------------------------------
    // OpenRouteService (ORS) — accessed via backend proxy
    // ---------------------------------------------------------------------------

    /**
     * Resolves a location name (e.g. a city or address string) to geographic coordinates
     * via the backend's OpenRouteService geocoding proxy.
     *
     * The backend forwards the request to ORS and returns a `[latitude, longitude]` pair.
     * Called twice during route calculation — once for origin, once for destination.
     *
     * @param location A human-readable location string (e.g. `"Vienna"`, `"Stephansplatz"`).
     * @returns `Observable<[number, number]>` — emits a `[latitude, longitude]` tuple.
     */
    getCoordinates(location: string): Observable<[number, number]> {
        return this.http.get<[number, number]>(`${this.baseUrl}/route/coordinates`, {params: {location}});
    }

    /**
     * Calculates the road/path route between two geographic coordinate pairs via the
     * backend's OpenRouteService routing proxy.
     *
     * The returned GeoJSON `FeatureCollection` contains:
     *  - `features[0].geometry.coordinates`: array of `[longitude, latitude]` pairs
     *    that define the polyline drawn on the Leaflet map.
     *  - `features[0].properties.summary.distance`: total route distance in metres.
     *  - `features[0].properties.summary.duration`: total route duration in seconds.
     *
     * Note: ORS returns coordinates in `[longitude, latitude]` order (GeoJSON standard),
     * which the component reverses to `[latitude, longitude]` before passing to Leaflet.
     *
     * @param fromLat Latitude of the start point.
     * @param fromLng Longitude of the start point.
     * @param toLat   Latitude of the end point.
     * @param toLng   Longitude of the end point.
     * @returns `Observable<any>` — emits a GeoJSON FeatureCollection with route geometry
     *          and summary properties.
     */
    getRoute(fromLat: number, fromLng: number, toLat: number, toLng: number): Observable<any> {
        return this.http.get<any>(`${this.baseUrl}/route`, {
            params: {
                fromLat: String(fromLat),
                fromLng: String(fromLng),
                toLat: String(toLat),
                toLng: String(toLng)
            }
        });
    }

    // ---------------------------------------------------------------------------
    // Tours — Image upload / download
    // ---------------------------------------------------------------------------

    /**
     * Uploads an image file for the specified tour.
     * Sends a multipart/form-data POST request. The backend stores the file and
     * updates the tour's imagePath field.
     *
     * @param tourId  The tour to attach the image to.
     * @param file    The image File selected by the user.
     * @returns Observable<Tour> — emits the updated tour with the new imagePath.
     */
    uploadImage(tourId: number, file: File): Observable<Tour> {
        const formData = new FormData();
        formData.append('file', file);
        return this.http.post<Tour>(`${this.baseUrl}/tours/${tourId}/image`, formData);
    }

    /**
     * Downloads the stored image for a tour as a Blob.
     * Uses HttpClient (which adds the JWT header via the interceptor) rather than
     * a plain img src, because the endpoint is authentication-protected.
     *
     * @param tourId  The tour whose image to fetch.
     * @returns Observable<Blob> — emits the raw image data.
     */
    getTourImage(tourId: number): Observable<Blob> {
        return this.http.get(`${this.baseUrl}/tours/${tourId}/image`, {responseType: 'blob'});
    }

    // ---------------------------------------------------------------------------
    // City autocomplete — Nominatim (OpenStreetMap), no API key required
    // ---------------------------------------------------------------------------

    /**
     * Returns a list of location display strings matching the given query,
     * sourced from the Nominatim geocoding API.
     * Called by the From/To inputs to show city suggestions as the user types.
     *
     * @param query  A partial city name or address (at least 2 characters).
     * @returns Observable<string[]> — up to 6 location label strings.
     */
    searchCities(query: string): Observable<string[]> {
        return this.http.get<any[]>('https://nominatim.openstreetmap.org/search', {
            params: { q: query, format: 'json', limit: '6', addressdetails: '1' }
        }).pipe(
            map(results => results.map(r => {
                // Show "<place name>, <country>" for a clean, compact label
                const parts = (r.display_name as string).split(',');
                return parts.slice(0, 2).map((s: string) => s.trim()).join(', ');
            }))
        );
    }

    // ---------------------------------------------------------------------------
    // Statistics (unique feature)
    // ---------------------------------------------------------------------------

    /**
     * Fetches aggregate statistics for all of the authenticated user's tours and logs.
     *
     * This is a unique feature of the Tour Planner that provides an at-a-glance dashboard
     * of the user's touring activity, including totals and per-transport-type breakdowns.
     *
     * @returns `Observable<Stats>` — emits a `Stats` object with totals and breakdowns.
     *          Displayed in the Statistics Dashboard panel when the user clicks "Statistics".
     */
    getStats(): Observable<Stats> {
        return this.http.get<Stats>(`${this.baseUrl}/stats`);
    }
}

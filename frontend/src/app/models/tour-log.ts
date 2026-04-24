/**
 * @file tour-log.ts
 * @description Defines the `TourLog` data model used throughout the Tour Planner application.
 * A TourLog records the details of a single completed outing of a particular tour,
 * capturing objective metrics (distance, time) and subjective assessments (difficulty,
 * rating, comment). Multiple logs can be associated with a single tour and are used
 * server-side to compute the tour's `popularity` and `childFriendliness` scores.
 */

/**
 * Represents a single log entry for a completed tour outing.
 *
 * Tour logs are nested under a specific `Tour` on the backend (route:
 * `GET /api/tours/{tourId}/logs`). They capture the real-world outcome of
 * completing the tour on a given date, allowing users to track progress over time
 * and compare multiple attempts at the same route.
 */
export interface TourLog {
    /**
     * The server-assigned unique identifier for this log entry.
     * Optional because it is absent before the log is first persisted (i.e. during creation).
     */
    id?: number;

    /**
     * The calendar date on which the tour was completed, formatted as an ISO 8601 date
     * string (`yyyy-MM-dd`, e.g. `"2024-06-15"`).
     * Required; used as the primary label in the log list.
     */
    logDate: string; // ISO date (yyyy-MM-dd)

    /**
     * A free-text comment or journal entry describing the outing — conditions,
     * highlights, or anything noteworthy. Required; maximum 2000 characters as
     * enforced by the UI textarea's `maxlength` attribute.
     */
    comment: string;

    /**
     * A numeric difficulty rating for this specific outing on a scale of 1 to 5,
     * where 1 is very easy and 5 is extremely challenging.
     * Required; validated to be in the range [1, 5] before saving.
     */
    difficulty: number;

    /**
     * The actual total distance covered during this outing, in kilometres.
     * May differ from the tour's planned `distance` if the route was altered.
     */
    totalDistance: number;

    /**
     * The actual total time taken to complete this outing, in minutes.
     * May differ from the tour's planned `estimatedTime`.
     */
    totalTimeMinutes: number;

    /**
     * A subjective overall rating of the outing on a scale of 0 to 10,
     * where 0 is very poor and 10 is excellent.
     * Required; validated to be in the range [0, 10] before saving.
     * Contributes to the parent tour's server-computed `childFriendliness` score.
     */
    rating: number;
}

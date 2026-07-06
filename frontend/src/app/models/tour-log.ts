// One log entry for a completed tour outing.
export interface TourLog {
    id?: number;
    logDate: string; // yyyy-MM-dd
    comment: string;
    difficulty: number;
    totalDistance: number;
    totalTimeMinutes: number;
    rating: number;
    logDetails?: string;
}

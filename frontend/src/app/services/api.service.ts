// HTTP wrapper for all backend REST calls.
import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {Tour} from '../models/tour';
import {TourLog} from '../models/tour-log';
import {environment} from '../../environments/environment';

export interface Stats {
    totalTours: number;
    totalLogs: number;
    totalDistanceKm: number;
    totalTimeHours: number;
    avgRating: number;
    byTransportType: {
        transportType: string;
        tourCount: number;
        logCount: number;
        avgDistanceKm: number;
        avgRating: number;
    }[];
}

@Injectable({providedIn: 'root'})
export class ApiService {
    private readonly baseUrl = environment.apiBaseUrl;

    constructor(private http: HttpClient) {
    }

    // --- Tours ---

    getTours(): Observable<Tour[]> {
        return this.http.get<Tour[]>(`${this.baseUrl}/tours`);
    }

    getTour(id: number): Observable<Tour> {
        return this.http.get<Tour>(`${this.baseUrl}/tours/${id}`);
    }

    createTour(tour: Tour): Observable<Tour> {
        return this.http.post<Tour>(`${this.baseUrl}/tours`, tour);
    }

    updateTour(id: number, tour: Tour): Observable<Tour> {
        return this.http.put<Tour>(`${this.baseUrl}/tours/${id}`, tour);
    }

    deleteTour(id: number): Observable<void> {
        return this.http.delete<void>(`${this.baseUrl}/tours/${id}`);
    }

    searchTours(query: string): Observable<number[]> {
        return this.http.get<number[]>(`${this.baseUrl}/tours/search`, {params: {q: query}});
    }

    // --- Logs ---

    getLogs(tourId: number): Observable<TourLog[]> {
        return this.http.get<TourLog[]>(`${this.baseUrl}/tours/${tourId}/logs`);
    }

    createLog(tourId: number, log: TourLog): Observable<TourLog> {
        return this.http.post<TourLog>(`${this.baseUrl}/tours/${tourId}/logs`, log);
    }

    updateLog(tourId: number, logId: number, log: TourLog): Observable<TourLog> {
        return this.http.put<TourLog>(`${this.baseUrl}/tours/${tourId}/logs/${logId}`, log);
    }

    deleteLog(tourId: number, logId: number): Observable<void> {
        return this.http.delete<void>(`${this.baseUrl}/tours/${tourId}/logs/${logId}`);
    }

    // --- Import / export / reports ---

    exportTour(tourId: number): Observable<any> {
        return this.http.get<any>(`${this.baseUrl}/tours/${tourId}/export`);
    }

    importTour(data: any): Observable<Tour> {
        return this.http.post<Tour>(`${this.baseUrl}/tours/import`, data);
    }

    downloadTourReport(tourId: number): Observable<Blob> {
        return this.http.get(`${this.baseUrl}/tours/${tourId}/report`, {responseType: 'blob'});
    }

    downloadSummaryReport(): Observable<Blob> {
        return this.http.get(`${this.baseUrl}/reports/summary`, {responseType: 'blob'});
    }

    // --- Routing / geocoding ---

    getCoordinates(location: string): Observable<[number, number]> {
        return this.http.get<[number, number]>(`${this.baseUrl}/route/coordinates`, {params: {location}});
    }

    getRoute(fromLat: number, fromLng: number, toLat: number, toLng: number, transport?: string): Observable<any> {
        const params: Record<string, string> = {
            fromLat: String(fromLat),
            fromLng: String(fromLng),
            toLat: String(toLat),
            toLng: String(toLng)
        };
        if (transport) params['transport'] = transport;
        return this.http.get<any>(`${this.baseUrl}/route`, {params});
    }

    searchCities(query: string): Observable<string[]> {
        return this.http.get<string[]>(`${this.baseUrl}/route/suggest`, {params: {q: query}});
    }

    // --- Images / stats ---

    uploadImage(tourId: number, file: File): Observable<Tour> {
        const formData = new FormData();
        formData.append('file', file);
        return this.http.post<Tour>(`${this.baseUrl}/tours/${tourId}/image`, formData);
    }

    getTourImage(tourId: number): Observable<Blob> {
        return this.http.get(`${this.baseUrl}/tours/${tourId}/image`, {responseType: 'blob'});
    }

    getStats(): Observable<Stats> {
        return this.http.get<Stats>(`${this.baseUrl}/stats`);
    }
}

// Login/register and JWT storage in localStorage.
import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable, tap} from 'rxjs';
import {environment} from '../../environments/environment';

export interface AuthRequest {
    username: string;
    password: string;
}

export interface AuthResponse {
    token: string;
    username: string;
}

@Injectable({providedIn: 'root'})
export class AuthService {
    private readonly baseUrl = `${environment.apiBaseUrl}/auth`;
    private readonly TOKEN_KEY = 'tp_token';
    private readonly USER_KEY = 'tp_username';

    constructor(private http: HttpClient) {
    }

    register(req: AuthRequest): Observable<AuthResponse> {
        return this.http.post<AuthResponse>(`${this.baseUrl}/register`, req).pipe(
            tap(res => this.storeAuth(res))
        );
    }

    login(req: AuthRequest): Observable<AuthResponse> {
        return this.http.post<AuthResponse>(`${this.baseUrl}/login`, req).pipe(
            tap(res => this.storeAuth(res))
        );
    }

    logout(): void {
        localStorage.removeItem(this.TOKEN_KEY);
        localStorage.removeItem(this.USER_KEY);
    }

    getToken(): string | null {
        return localStorage.getItem(this.TOKEN_KEY);
    }

    getUsername(): string | null {
        return localStorage.getItem(this.USER_KEY);
    }

    isLoggedIn(): boolean {
        return !!this.getToken();
    }

    private storeAuth(res: AuthResponse): void {
        localStorage.setItem(this.TOKEN_KEY, res.token);
        localStorage.setItem(this.USER_KEY, res.username);
    }
}

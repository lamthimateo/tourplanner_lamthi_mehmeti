/**
 * @file auth.service.ts
 * @description Provides JWT-based authentication for the Tour Planner application.
 *
 * This service handles user registration and login by communicating with the backend
 * `/api/auth` endpoints. On a successful response it persists the JWT token and
 * username in `localStorage` so they survive page refreshes. It also exposes helpers
 * that other parts of the application (e.g. `AppComponent`, `authInterceptor`) use
 * to check login state and retrieve the stored credentials.
 */

import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable, tap} from 'rxjs';

/**
 * Payload sent to the backend for both registration and login requests.
 */
export interface AuthRequest {
    /** The chosen or existing username. Minimum 3 characters as enforced by the UI. */
    username: string;

    /** The chosen or existing plaintext password. Minimum 4 characters as enforced by the UI. */
    password: string;
}

/**
 * Shape of the successful authentication response returned by the backend.
 * On registration the backend creates the user; on login it validates credentials.
 * Either way the same response shape is used.
 */
export interface AuthResponse {
    /** The signed JWT that must be included as a Bearer token on all subsequent API calls. */
    token: string;

    /** The authenticated user's username, stored locally for display in the UI. */
    username: string;
}

/**
 * Angular service responsible for all authentication operations in the Tour Planner app.
 *
 * Provided at the root injector level so a single shared instance is used across the
 * entire application. The service wraps HTTP calls to the backend auth API and manages
 * the resulting JWT via `localStorage`.
 *
 * @example
 * // Injecting and using the service
 * constructor(public auth: AuthService) {}
 *
 * login() {
 *   this.auth.login({ username: 'alice', password: 'secret' }).subscribe(() => {
 *     // token is now stored; auth.isLoggedIn() returns true
 *   });
 * }
 */
@Injectable({providedIn: 'root'})
export class AuthService {
    /**
     * Base URL for all authentication API endpoints.
     * Points to the Spring Boot backend running on port 8081.
     */
    private readonly baseUrl = 'http://localhost:8081/api/auth';

    /**
     * `localStorage` key under which the JWT is stored.
     * Prefixed with `tp_` (Tour Planner) to avoid collisions with other apps
     * sharing the same origin.
     */
    private readonly TOKEN_KEY = 'tp_token';

    /**
     * `localStorage` key under which the authenticated username is stored.
     * Used to display the current user's name in the UI without re-querying the backend.
     */
    private readonly USER_KEY = 'tp_username';

    /**
     * @param http Angular's `HttpClient` used to make POST requests to the auth endpoints.
     */
    constructor(private http: HttpClient) {
    }

    /**
     * Registers a new user account with the backend and, on success, automatically
     * stores the returned JWT and username in `localStorage`.
     *
     * @param req The username and password for the new account.
     * @returns An `Observable` that emits the `AuthResponse` (token + username) once
     *          the registration succeeds. The side effect of storing credentials fires
     *          via `tap` before the caller's subscriber receives the value.
     */
    register(req: AuthRequest): Observable<AuthResponse> {
        return this.http.post<AuthResponse>(`${this.baseUrl}/register`, req).pipe(
            // Store the token and username immediately after a successful response
            tap(res => this.storeAuth(res))
        );
    }

    /**
     * Authenticates an existing user against the backend and, on success, stores
     * the returned JWT and username in `localStorage`.
     *
     * @param req The username and password of the existing account.
     * @returns An `Observable` that emits the `AuthResponse` (token + username) once
     *          the login succeeds. The side effect of storing credentials fires
     *          via `tap` before the caller's subscriber receives the value.
     */
    login(req: AuthRequest): Observable<AuthResponse> {
        return this.http.post<AuthResponse>(`${this.baseUrl}/login`, req).pipe(
            // Store the token and username immediately after a successful response
            tap(res => this.storeAuth(res))
        );
    }

    /**
     * Logs the current user out by removing both the JWT and the username from
     * `localStorage`. After this call, `isLoggedIn()` returns `false` and the
     * app returns to the login/register screen.
     *
     * @returns `void` — this method has no return value and does not make an HTTP call.
     */
    logout(): void {
        localStorage.removeItem(this.TOKEN_KEY);
        localStorage.removeItem(this.USER_KEY);
    }

    /**
     * Retrieves the stored JWT from `localStorage`.
     *
     * Used by `authInterceptor` to attach the `Authorization: Bearer <token>` header
     * to outgoing HTTP requests.
     *
     * @returns The JWT string if the user is logged in, or `null` if not authenticated.
     */
    getToken(): string | null {
        return localStorage.getItem(this.TOKEN_KEY);
    }

    /**
     * Retrieves the stored username from `localStorage`.
     *
     * Used by the UI to display the currently logged-in user's name.
     *
     * @returns The username string if the user is logged in, or `null` if not authenticated.
     */
    getUsername(): string | null {
        return localStorage.getItem(this.USER_KEY);
    }

    /**
     * Checks whether a user is currently authenticated by verifying that a JWT
     * is present in `localStorage`.
     *
     * Note: this is a client-side check only — it does not validate the token's
     * signature or expiry. An expired token will still cause `isLoggedIn()` to
     * return `true` until the next API call returns a 401/403.
     *
     * @returns `true` if a token is stored, `false` otherwise.
     */
    isLoggedIn(): boolean {
        return !!this.getToken();
    }

    /**
     * Persists the authentication credentials returned by the backend into `localStorage`.
     *
     * This private helper is called via `tap` after both successful login and registration
     * responses, keeping the two public methods DRY.
     *
     * @param res The `AuthResponse` containing the JWT token and username to store.
     */
    private storeAuth(res: AuthResponse): void {
        localStorage.setItem(this.TOKEN_KEY, res.token);
        localStorage.setItem(this.USER_KEY, res.username);
    }
}

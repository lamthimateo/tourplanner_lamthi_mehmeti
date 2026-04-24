/**
 * @file app.config.ts
 * @description Root application configuration for the Tour Planner Angular standalone app.
 *
 * This file defines the `appConfig` object that is passed to `bootstrapApplication()`
 * in `main.ts`. It replaces the traditional `AppModule` in Angular 17's standalone
 * component model. All application-wide providers — including `HttpClient` and the
 * JWT auth interceptor — are registered here rather than in a module.
 *
 * The `authInterceptor` is registered via `withInterceptors()` so that it is applied
 * to every `HttpClient` request made anywhere in the application, automatically
 * attaching the Bearer token for authenticated API calls.
 */

import {ApplicationConfig} from '@angular/core';
import {provideHttpClient, withInterceptors} from '@angular/common/http';
import {authInterceptor} from './services/auth.interceptor';

/**
 * Root `ApplicationConfig` object consumed by `bootstrapApplication()` in `main.ts`.
 *
 * Registers the following application-wide providers:
 * - `HttpClient`: Angular's built-in HTTP client, configured with the functional
 *   interceptor API (`withInterceptors`) introduced in Angular 15.
 * - `authInterceptor`: The JWT Bearer token interceptor that attaches the stored
 *   token to all outgoing HTTP requests that are not authentication endpoints.
 *   See `auth.interceptor.ts` for full details.
 */
export const appConfig: ApplicationConfig = {
    providers: [
        // Provide HttpClient with the auth interceptor applied globally.
        // withInterceptors() accepts an array of HttpInterceptorFn functions,
        // executed in order for every outgoing request.
        provideHttpClient(withInterceptors([authInterceptor]))
    ]
};

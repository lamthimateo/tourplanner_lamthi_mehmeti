/**
 * @file auth.interceptor.ts
 * @description Functional HTTP interceptor that attaches a JWT Bearer token to all
 * outgoing HTTP requests that are NOT directed at the authentication endpoints.
 *
 * This interceptor is registered in `app.config.ts` via `withInterceptors([authInterceptor])`
 * so it runs automatically for every `HttpClient` request in the application. It keeps
 * authentication logic out of individual service methods by centralising header injection
 * in a single place.
 *
 * Auth endpoints (`/api/auth/`) are explicitly excluded because they are the very calls
 * that obtain the token — sending a (potentially absent) token on those requests would
 * be meaningless and could cause backend errors.
 */

import {HttpInterceptorFn} from '@angular/common/http';
import {inject} from '@angular/core';
import {AuthService} from './auth.service';

/**
 * Angular functional HTTP interceptor that adds a `Authorization: Bearer <token>` header
 * to every outgoing request, provided:
 *  1. A valid JWT is present in `localStorage` (i.e. the user is logged in), AND
 *  2. The request URL does not target an authentication endpoint (`/api/auth/`).
 *
 * The interceptor uses Angular's `inject()` to obtain the `AuthService` at runtime,
 * which is the correct pattern for functional (non-class-based) interceptors in
 * Angular 15+ standalone APIs.
 *
 * @param req  The outgoing `HttpRequest` that Angular is about to send.
 * @param next The `HttpHandlerFn` representing the next step in the interceptor chain
 *             (ultimately the actual HTTP transport).
 * @returns An `Observable<HttpEvent<unknown>>` — the response stream from the next handler,
 *          with the request potentially cloned to include the Authorization header.
 *
 * @example
 * // Registration in app.config.ts:
 * provideHttpClient(withInterceptors([authInterceptor]))
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
    // Inject AuthService using the standalone inject() API, valid inside injection context
    const auth = inject(AuthService);

    // Retrieve the JWT from localStorage (null if not logged in)
    const token = auth.getToken();

    // Only attach the header if we have a token AND the request is not an auth call.
    // Skipping auth endpoints prevents circular dependency issues (login/register
    // must succeed before we ever have a token to attach).
    if (token && !req.url.includes('/api/auth/')) {
        // Clone the request (HttpRequest objects are immutable) and add the Authorization header
        req = req.clone({
            setHeaders: {Authorization: `Bearer ${token}`}
        });
    }

    // Pass the (possibly modified) request to the next handler in the chain
    return next(req);
};

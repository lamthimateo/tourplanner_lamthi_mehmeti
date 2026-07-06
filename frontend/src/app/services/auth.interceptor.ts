import {HttpInterceptorFn} from '@angular/common/http';
import {inject} from '@angular/core';
import {AuthService} from './auth.service';
import {environment} from '../../environments/environment';

// Attach JWT to our API requests (skip auth endpoints).
export const authInterceptor: HttpInterceptorFn = (req, next) => {
    const auth = inject(AuthService);
    const token = auth.getToken();

    const isOwnApi = req.url.startsWith(environment.apiBaseUrl);
    if (token && isOwnApi && !req.url.includes('/api/auth/')) {
        req = req.clone({
            setHeaders: {Authorization: `Bearer ${token}`}
        });
    }

    return next(req);
};

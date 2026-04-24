/**
 * Angular application entry point.
 *
 * Bootstraps the root {@link AppComponent} using the application configuration
 * defined in {@link appConfig} (which registers HttpClient with the JWT interceptor
 * and any other application-level providers).
 *
 * Any fatal bootstrap error (e.g. missing provider, template compilation failure)
 * is caught here and logged to the browser console.
 */
import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { AppComponent } from './app/app.component';

bootstrapApplication(AppComponent, appConfig)
    .catch(err => console.error(err));

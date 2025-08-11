import { APP_INITIALIZER, ApplicationConfig, importProvidersFrom, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { httpTokenInterceptor } from './services/interceptor/http-token.interceptor';
import { ApiModule } from './services/api.module';
import { KeycloakService } from './services/keycloak/keycloak.service';
import { ToastrModule } from 'ngx-toastr';

export function kcFactory(kcService: KeycloakService) {
  return () => kcService.init();
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
     provideRouter(routes),
    provideHttpClient(
      withInterceptors([httpTokenInterceptor])
    ),
    /*
    importProvidersFrom(
      ApiModule.forRoot({
        rootUrl: 'http://192.168.18.197:8088/api/v1'
      })
    ),*/
     {
      provide: APP_INITIALIZER,
      deps: [KeycloakService],
      useFactory: kcFactory,
      multi: true
    },
     importProvidersFrom(
      ToastrModule.forRoot({
        progressBar: true,
        closeButton: true,
        newestOnTop: true,
        tapToDismiss: true,
        positionClass: 'toast-bottom-right',
        timeOut: 8000
      })
    ),
    ]

};

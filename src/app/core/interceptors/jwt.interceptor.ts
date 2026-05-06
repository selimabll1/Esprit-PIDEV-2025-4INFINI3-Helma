import { Injectable } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { environment } from '../../../environments/environment';

@Injectable()
export class JwtInterceptor implements HttpInterceptor {
  constructor(private readonly authService: AuthService) {}

  intercept(req: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    const token = this.authService.getToken();
    const url = req.url ?? '';

    const isPublic = url.includes('/auth/login') || url.includes('/auth/register');
    const isProtectedApi = url.startsWith(environment.loanApiUrl)
                        || url.startsWith(environment.apiBaseUrl);

    if (token && !isPublic && isProtectedApi) {
      return next.handle(req.clone({
        setHeaders: { Authorization: `Bearer ${token}` }
      }));
    }

    return next.handle(req);
  }
}

import { HttpInterceptorFn } from '@angular/common/http';

/**
 * Example functional HTTP interceptor for JWT authentication.
 */
export const authInterceptorExample: HttpInterceptorFn = (req, next) => {
  const authToken = localStorage.getItem('auth_token');
  if (authToken) {
    const authorizedRequest = req.clone({
      headers: req.headers.set('Authorization', Bearer ${authToken})
    });
    return next(authorizedRequest);
  }
  return next(req);
};

export const environment = {
  production: false,
  /**
   * User-Backend base URL (port 8085).
   * Used for: POST /api/auth/login, POST /api/auth/register
   */
  apiBaseUrl: 'http://localhost:8085/api',

  /**
   * loan-service base URL (port 8081, context-path /h).
   * Used for: /api/loans/**, /api/users/**, etc.
   */
  loanApiUrl: 'http://localhost:8081/h'
};

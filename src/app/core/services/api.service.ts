import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  CreateLoanRequest,
  EarlyWarning,
  FinancialHealth,
  PagedLoansResponse,
  CreatePaymentRequest,
  Loan,
  LoanPayment,
  LoanStatistics,
  LoanSummary,
  MultiAgentDecision,
  RepaymentSchedule,
  SimulationRequest,
  SimulationResult
} from '../models';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly BASE_URL = environment.loanApiUrl;

  constructor(private readonly http: HttpClient) {}

  createLoan(data: CreateLoanRequest): Observable<Loan> {
    return this.http.post<Loan>(`${this.BASE_URL}/api/loans`, data);
  }

  getLoanById(id: number): Observable<Loan> {
    return this.http.get<Loan>(`${this.BASE_URL}/api/loans/${id}`);
  }

  getLoansByUser(userId: number): Observable<Loan[]> {
    return this.http.get<Loan[]>(`${this.BASE_URL}/api/loans/user/${userId}`);
  }

  getAllLoans(page: number, size: number): Observable<PagedLoansResponse> {
    return this.http.get<PagedLoansResponse>(`${this.BASE_URL}/api/loans`, {
      params: { page, size }
    });
  }

  approveLoan(id: number): Observable<Loan> {
    return this.http.put<Loan>(`${this.BASE_URL}/api/loans/${id}/approve`, {});
  }

  rejectLoan(id: number): Observable<Loan> {
    return this.http.put<Loan>(`${this.BASE_URL}/api/loans/${id}/reject`, {});
  }

  getLoanSchedule(loanId: number): Observable<RepaymentSchedule[]> {
    return this.http.get<RepaymentSchedule[]>(`${this.BASE_URL}/api/loans/${loanId}/schedule`);
  }

  getLoanSummary(loanId: number): Observable<LoanSummary> {
    return this.http.get<LoanSummary>(`${this.BASE_URL}/api/loans/${loanId}/summary`);
  }

  getAIDecision(loanId: number): Observable<any> {
    return this.http.post<any>(`${this.BASE_URL}/api/loans/${loanId}/ai-decision`, {});
  }

  getMLPrediction(loanId: number): Observable<any> {
    return this.http.post<any>(`${this.BASE_URL}/api/loans/${loanId}/ml-predict`, {});
  }

  getMarkovPrediction(loanId: number): Observable<any> {
    return this.http.post<any>(`${this.BASE_URL}/api/loans/${loanId}/markov-predict`, {});
  }

  getMultiAgentDecision(loanId: number): Observable<MultiAgentDecision> {
    return this.http.post<MultiAgentDecision>(
      `${this.BASE_URL}/api/loans/${loanId}/multi-agent-decision`,
      {}
    );
  }

  getFinancialHealth(userId: number): Observable<FinancialHealth> {
    return this.http.get<FinancialHealth>(`${this.BASE_URL}/api/users/${userId}/financial-health`);
  }

  downloadContract(loanId: number): Observable<Blob> {
    return this.http.get(`${this.BASE_URL}/api/loans/${loanId}/generate-contract`, {
      responseType: 'blob'
    });
  }

  getEarlyWarnings(): Observable<EarlyWarning[]> {
    return this.http.get<EarlyWarning[]>(`${this.BASE_URL}/api/loans/early-warnings`);
  }

  createPayment(data: CreatePaymentRequest): Observable<LoanPayment> {
    return this.http.post<LoanPayment>(`${this.BASE_URL}/api/loans/payments`, data);
  }

  getPaymentsByLoan(loanId: number): Observable<LoanPayment[]> {
    return this.http.get<LoanPayment[]>(`${this.BASE_URL}/api/loans/payments/${loanId}`);
  }

  simulateLoan(data: SimulationRequest): Observable<SimulationResult> {
    return this.http.post<SimulationResult>(`${this.BASE_URL}/api/loans/simulate`, data);
  }

  getStatistics(): Observable<LoanStatistics> {
    return this.http.get<LoanStatistics>(`${this.BASE_URL}/api/loans/statistics`);
  }
}


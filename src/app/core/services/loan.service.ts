import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import {
  CreateLoanRequest,
  EarlyWarning,
  FinancialHealth,
  Loan,
  LoanSummary,
  MultiAgentDecision,
  PagedLoansResponse,
  RepaymentSchedule
} from '../models';

@Injectable({ providedIn: 'root' })
export class LoanService {
  constructor(private readonly api: ApiService) {}

  createLoan(data: CreateLoanRequest): Observable<Loan> {
    return this.api.createLoan(data);
  }

  getLoanById(id: number): Observable<Loan> {
    return this.api.getLoanById(id);
  }

  getLoansByUser(userId: number): Observable<Loan[]> {
    return this.api.getLoansByUser(userId);
  }

  getAllLoans(page: number, size: number): Observable<PagedLoansResponse> {
    return this.api.getAllLoans(page, size);
  }

  approveLoan(id: number): Observable<Loan> {
    return this.api.approveLoan(id);
  }

  rejectLoan(id: number): Observable<Loan> {
    return this.api.rejectLoan(id);
  }

  getLoanSummary(loanId: number): Observable<LoanSummary> {
    return this.api.getLoanSummary(loanId);
  }

  getLoanSchedule(loanId: number): Observable<RepaymentSchedule[]> {
    return this.api.getLoanSchedule(loanId);
  }

  getMultiAgentDecision(loanId: number): Observable<MultiAgentDecision> {
    return this.api.getMultiAgentDecision(loanId);
  }

  getFinancialHealth(userId: number): Observable<FinancialHealth> {
    return this.api.getFinancialHealth(userId);
  }

  downloadContract(loanId: number): Observable<Blob> {
    return this.api.downloadContract(loanId);
  }

  getEarlyWarnings(): Observable<EarlyWarning[]> {
    return this.api.getEarlyWarnings();
  }
}


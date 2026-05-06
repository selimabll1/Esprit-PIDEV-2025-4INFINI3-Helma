import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import { Loan, LoanStatistics } from '../models';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  constructor(private readonly api: ApiService) {}

  getStatistics(): Observable<LoanStatistics> {
    return this.api.getStatistics();
  }

  getUserLoans(userId: number): Observable<Loan[]> {
    return this.api.getLoansByUser(userId);
  }
}


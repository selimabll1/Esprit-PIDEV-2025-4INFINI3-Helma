import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import { CreatePaymentRequest, LoanPayment } from '../models';

@Injectable({ providedIn: 'root' })
export class PaymentService {
  constructor(private readonly api: ApiService) {}

  createPayment(data: CreatePaymentRequest): Observable<LoanPayment> {
    return this.api.createPayment(data);
  }

  getPaymentsByLoan(loanId: number): Observable<LoanPayment[]> {
    return this.api.getPaymentsByLoan(loanId);
  }
}


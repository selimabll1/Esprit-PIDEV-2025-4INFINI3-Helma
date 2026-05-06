import { PaymentStatus } from './enums.model';

export interface RepaymentSchedule {
  id: number;
  loanId: number;
  installmentNumber: number;
  dueDate: string;
  expectedAmount: number;
  paidAmount: number;
  status: PaymentStatus;
}

export interface LoanPayment {
  id: number;
  loanId: number;
  scheduleId: number;
  amount: number;
  paidAt: string;
  paymentMethod: string;
  reference: string;
}

export interface CreatePaymentRequest {
  loanId: number;
  scheduleId: number;
  amount: number;
  paymentMethod: string;
}

export interface LoanStatistics {
  totalLoans: number;
  activeLoans: number;
  defaultedLoans: number;
  par30: number;
  par60: number;
  par90: number;
}

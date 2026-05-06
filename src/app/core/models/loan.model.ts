import { LoanStatus, LoanType } from './enums.model';
import { RepaymentSchedule } from './payment.model';

export interface Loan {
  id: number;
  userId: number;
  principalAmount: number;
  interestRate: number;
  durationMonths: number;
  monthlyPayment?: number;
  startDate?: string;
  loanType: LoanType;
  riskScore?: number;
  status: LoanStatus;
}

export interface CreateLoanRequest {
  userId: number;
  loanType: LoanType;
  principalAmount: number;
  durationMonths: number;
}

export interface LoanSummary {
  loan: Loan;
  totalPaid: number;
  totalRemaining: number;
  nextPayment: RepaymentSchedule | null;
}

export interface MultiAgentDecision {
  loanId: number;
  originalRiskScore: number;
  creditScoringAnalysis: string;
  adjustedScore: number;
  criticalFlag: boolean;
  fraudDetectionAnalysis: string;
  fraudRisk: 'LOW' | 'MEDIUM' | 'HIGH';
  marketContextAnalysis: string;
  marketContext: string;
  finalDecisionAnalysis: string;
  finalDecision: 'APPROVE' | 'REJECT';
  explanation: string;
}

export interface FinancialHealth {
  userId: number;
  helmaScore: number;
  level: 'EXCELLENT' | 'GOOD' | 'FAIR' | 'POOR';
  maxLoanAmount: number;
  preferentialRate: boolean;
  details: {
    repaymentHistory: number;
    regularity: number;
    debtRatio: number;
    seniority: number;
    loanType: number;
  };
}

export interface EarlyWarning {
  id: number;
  loanId: number;
  userId: number;
  combinedScore: number;
  mlScore: number;
  markovScore: number;
  mcScore: number;
  aiExplanation: string;
  createdAt: string;
  status: 'OPEN' | 'RESOLVED';
}

export interface PagedLoansResponse {
  content: Loan[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface SimulationRequest {
  principalAmount: number;
  interestRate: number;
  durationMonths: number;
}

export interface SimulationInstallment {
  installmentNumber: number;
  dueDate: string;
  expectedAmount: number;
}

export interface SimulationResult {
  principalAmount: number;
  interestRate: number;
  durationMonths: number;
  monthlyPayment: number;
  totalCost: number;
  totalInterest: number;
  schedule: SimulationInstallment[];
}

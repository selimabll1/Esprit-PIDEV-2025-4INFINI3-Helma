import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  Transaction, TransactionCreate, TransactionUpdate,
  Budget, BudgetCreate, BudgetUpdate, TrustBudgetResponse,
  SavingsGoal, SavingsGoalCreate,
  Dashboard, CashFlow, BurnRate, Forecast,
  HealthScore, IncomeStatement, RiskCase,
  CoachMessage, CoachSend, CoachResponse
} from '../models/budget.models';

@Injectable({ providedIn: 'root' })
export class BudgetApiService {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiBaseUrl;

  /* ── Transactions ──────────────────────────────── */
  getTransactions(userId: number): Observable<Transaction[]> {
    return this.http.get<Transaction[]>(`${this.base}/transactions`, { params: new HttpParams().set('userId', userId) });
  }
  createTransaction(req: TransactionCreate): Observable<Transaction> {
    return this.http.post<Transaction>(`${this.base}/transactions`, req);
  }
  updateTransaction(id: number, req: TransactionUpdate): Observable<Transaction> {
    return this.http.put<Transaction>(`${this.base}/transactions/${id}`, req);
  }
  deleteTransaction(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/transactions/${id}`);
  }

  /* ── Budgets ───────────────────────────────────── */
  getBudgets(userId: number): Observable<Budget[]> {
    return this.http.get<Budget[]>(`${this.base}/budgets`, { params: new HttpParams().set('userId', userId) });
  }
  createBudget(req: BudgetCreate): Observable<Budget> {
    return this.http.post<Budget>(`${this.base}/budgets`, req);
  }
  updateBudget(id: number, req: BudgetUpdate): Observable<Budget> {
    return this.http.put<Budget>(`${this.base}/budgets/${id}`, req);
  }
  deleteBudget(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/budgets/${id}`);
  }
  getTrustBudget(userId: number): Observable<TrustBudgetResponse> {
    return this.http.get<TrustBudgetResponse>(`${this.base}/budgets/trust-budget/${userId}`);
  }

  /* ── Savings Goals ─────────────────────────────── */
  getSavingsGoals(userId: number): Observable<SavingsGoal[]> {
    return this.http.get<SavingsGoal[]>(`${this.base}/savings-goals/${userId}`);
  }
  createSavingsGoal(req: SavingsGoalCreate): Observable<SavingsGoal> {
    return this.http.post<SavingsGoal>(`${this.base}/savings-goals`, req);
  }
  addGoalProgress(goalId: number, amount: number): Observable<SavingsGoal> {
    return this.http.patch<SavingsGoal>(`${this.base}/savings-goals/${goalId}/progress`, { amount });
  }
  deleteSavingsGoal(goalId: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/savings-goals/${goalId}`);
  }

  /* ── Dashboard ─────────────────────────────────── */
  getDashboard(userId: number): Observable<Dashboard> {
    return this.http.get<Dashboard>(`${this.base}/dashboard/${userId}`);
  }

  /* ── Cash Flow ─────────────────────────────────── */
  getCashFlow(userId: number): Observable<CashFlow> {
    return this.http.get<CashFlow>(`${this.base}/cashflow/${userId}`);
  }
  getCashFlowHistory(userId: number): Observable<CashFlow[]> {
    return this.http.get<CashFlow[]>(`${this.base}/cashflow/${userId}/history`);
  }

  /* ── Burn Rate ─────────────────────────────────── */
  getBurnRate(userId: number): Observable<BurnRate> {
    return this.http.get<BurnRate>(`${this.base}/burn-rate/${userId}`);
  }

  /* ── Forecast ──────────────────────────────────── */
  getForecast(userId: number): Observable<Forecast> {
    return this.http.get<Forecast>(`${this.base}/forecast/${userId}`);
  }

  /* ── Health Score ──────────────────────────────── */
  getHealthScore(userId: number): Observable<HealthScore> {
    return this.http.get<HealthScore>(`${this.base}/health-score/${userId}`);
  }

  /* ── Income Statement ──────────────────────────── */
  getIncomeStatement(userId: number): Observable<IncomeStatement> {
    return this.http.get<IncomeStatement>(`${this.base}/income-statement/${userId}`);
  }
  getIncomeStatementHistory(userId: number): Observable<IncomeStatement[]> {
    return this.http.get<IncomeStatement[]>(`${this.base}/income-statement/${userId}/history`);
  }

  /* ── Risk Cases ────────────────────────────────── */
  getRiskCases(userId: number): Observable<RiskCase[]> {
    return this.http.get<RiskCase[]>(`${this.base}/risk-cases`, { params: new HttpParams().set('userId', userId) });
  }

  /* ── Coach (AI) ────────────────────────────────── */
  sendCoachMessage(req: CoachSend): Observable<CoachResponse> {
    return this.http.post<CoachResponse>(`${this.base}/coach/message`, req);
  }
  getCoachHistory(userId: number): Observable<CoachMessage[]> {
    return this.http.get<CoachMessage[]>(`${this.base}/coach/history/${userId}`);
  }

  /* ── Analytics (NEW — charts data) ─────────────── */
  getSpendingByCategory(userId: number, month: string): Observable<{category: string, amount: number}[]> {
    return this.http.get<any[]>(`${this.base}/analytics/by-category`, {
      params: new HttpParams().set('userId', userId).set('month', month)
    });
  }
  getDailySpending(userId: number, month: string): Observable<{date: string, amount: number}[]> {
    return this.http.get<any[]>(`${this.base}/analytics/daily`, {
      params: new HttpParams().set('userId', userId).set('month', month)
    });
  }
  getBudgetVsActual(userId: number, month: string): Observable<{category: string, budgetLimit: number, actualSpent: number, remaining: number, usagePercent: number}[]> {
    return this.http.get<any[]>(`${this.base}/analytics/budget-vs-actual`, {
      params: new HttpParams().set('userId', userId).set('month', month)
    });
  }
  getDailyAllowance(userId: number): Observable<{totalBudget: number, totalSpent: number, remaining: number, daysLeft: number, dailyAllowance: number}> {
    return this.http.get<any>(`${this.base}/analytics/daily-allowance`, {
      params: new HttpParams().set('userId', userId)
    });
  }

  /* ── Reports (PDF) ─────────────────────────────── */
  downloadCashFlowReport(userId: number): Observable<Blob> {
    return this.http.get(`${this.base}/reports/cash-flow/${userId}`, { responseType: 'blob' });
  }
  downloadMonthlyReport(userId: number, month: string): Observable<Blob> {
    return this.http.get(`${this.base}/reports/monthly/${userId}`, {
      responseType: 'blob', params: new HttpParams().set('month', month)
    });
  }
  downloadYearlyReport(userId: number, year: number): Observable<Blob> {
    return this.http.get(`${this.base}/reports/yearly/${userId}`, {
      responseType: 'blob', params: new HttpParams().set('year', year)
    });
  }
  getAvailableMonths(userId: number): Observable<string[]> {
    return this.http.get<string[]>(`${this.base}/reports/available-months/${userId}`);
  }
}

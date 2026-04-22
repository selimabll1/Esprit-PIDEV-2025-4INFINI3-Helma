/* ── Transaction ──────────────────────────────────── */
export type TransactionType = 'INCOME' | 'EXPENSE';

export interface TransactionCreate {
  userId: number;
  amount: number;
  category: string;
  type: TransactionType;
  receiptUrl?: string | null;
}

export interface TransactionUpdate {
  amount: number;
  category: string;
  type: TransactionType;
  receiptUrl?: string | null;
}

export interface Transaction {
  id: number;
  userId: number;
  amount: number;
  category: string;
  type: TransactionType;
  txnDate: string;
  receiptUrl?: string | null;
}

/* ── Budget ───────────────────────────────────────── */
export interface BudgetCreate {
  userId: number;
  limitAmount: number;
  monthStart: string;   // yyyy-MM-dd
  category: string;
}

export interface BudgetUpdate {
  limitAmount: number;
  monthStart: string;
  category: string;
}

export interface Budget {
  id: number;
  userId: number;
  limitAmount: number;
  monthStart: string;
  category: string;
}

export interface TrustBudgetResponse {
  userId: number;
  entrepreneur: boolean;
  baseMethod: string;
  base: number;
  riskScore: number;
  budgetUsage: number;
  maxOpenRisk: number;
  trustBudget: number;
  badgeLevel: string;
  badgeReasons: string[];
  reasons: string[];
}

/* ── Savings Goal ─────────────────────────────────── */
export interface SavingsGoalCreate {
  userId: number;
  name: string;
  targetAmount: number;
  deadline: string;
}

export interface SavingsGoal {
  id: number;
  userId: number;
  name: string;
  targetAmount: number;
  currentAmount: number;
  deadline: string;
  weeklyTarget: number;
  completed: boolean;
  createdAt: string;
}

/* ── Cash Flow ────────────────────────────────────── */
export interface CashFlow {
  id: number;
  userId: number;
  monthStart: string;
  totalIncome: number;
  totalExpense: number;
  netFlow: number;
  cumulativeBalance: number;
  updatedAt: string;
}

/* ── Burn Rate ────────────────────────────────────── */
export type RunwayStatus = 'HEALTHY' | 'WARNING' | 'CRITICAL';

export interface MonthExpense {
  monthStart: string;
  totalExpense: number;
}

export interface BurnRate {
  userId: number;
  burnRate: number;
  currentBalance: number;
  runwayMonths: number;
  status: RunwayStatus;
  riskCaseTriggered: boolean;
  burnRateMethod: string;
  monthsUsed: number;
  breakdown: MonthExpense[];
  finCoachTips: string[];
  computedAt: string;
  projectedZeroDate: string | null;
}

/* ── Forecast ─────────────────────────────────────── */
export interface ForecastMonth {
  monthStart: string;
  predictedIncome: number;
  predictedExpense: number;
  predictedNetFlow: number;
  predictedBalance: number;
}

export interface Forecast {
  userId: number;
  monthsUsed: number;
  forecastMethod: string;
  confidenceLevel: string;
  historyQuality: string;
  incomeTrend: string;
  expenseTrend: string;
  incomeSlope: number;
  expenseSlope: number;
  avgPredictedIncome: number;
  avgPredictedExpense: number;
  avgPredictedNetFlow: number;
  predictedRunwayMonths: number;
  projectedCashoutDate: string | null;
  months: ForecastMonth[];
  alerts: string[];
  explanations: string[];
  computedAt: string;
}

/* ── Health Score ──────────────────────────────────── */
export interface HealthScore {
  userId: number;
  score: number;
  label: string;
  runwayScore: number;
  budgetScore: number;
  savingsScore: number;
  stabilityScore: number;
  riskScore: number;
  highlights: string[];
  computedAt: string;
}

/* ── Income Statement ─────────────────────────────── */
export interface IncomeStatement {
  id: number;
  userId: number;
  monthStart: string;
  totalRevenue: number;
  totalExpenses: number;
  netResult: number;
  marginRate: number;
  updatedAt: string;
}

/* ── Risk Case ────────────────────────────────────── */
export interface RiskCase {
  id: number;
  userId: number;
  riskLevel: number;
  assignedAdminId: number | null;
  status: string;
  detectedAt: string;
}

/* ── Trust Badge ──────────────────────────────────── */
export type BadgeLevel = 'BRONZE' | 'SILVER' | 'GOLD' | 'PLATINUM';

export interface TrustBadge {
  userId: number;
  level: BadgeLevel;
  creditCapacity: number;
  reasons: string;
  computedAt: string;
}

/* ── Dashboard (aggregated) ───────────────────────── */
export interface Dashboard {
  userId: number;
  currentCashFlow: CashFlow;
  burnRate: BurnRate;
  forecast: Forecast;
  healthScore: HealthScore;
  trustBadge: TrustBadge;
  incomeStatement: IncomeStatement;
  currentBudgets: Budget[];
  savingsGoals: SavingsGoal[];
  openRiskCases: RiskCase[];
  mainAlert: string;
  mainPositive: string;
  nextBestAction: string;
}

/* ── Coach ─────────────────────────────────────────── */
export interface CoachMessage {
  messageId: number;
  role: string;
  content: string;
  createdAt: string;
}

export interface CoachSend {
  userId: number;
  message: string;
}

export interface CoachResponse {
  messageId: number;
  sessionId: number;
  content: string;
  createdAt: string;
}

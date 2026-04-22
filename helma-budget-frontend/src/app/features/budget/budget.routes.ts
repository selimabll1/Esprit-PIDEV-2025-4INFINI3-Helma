import { Routes } from '@angular/router';
import { BudgetShellComponent } from './shell/budget-shell.component';

export const BUDGET_ROUTES: Routes = [
  {
    path: '',
    component: BudgetShellComponent,
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard', loadComponent: () => import('./dashboard/budget-dashboard.page').then(m => m.BudgetDashboardPage) },
      { path: 'transactions', loadComponent: () => import('./transactions/transactions.page').then(m => m.TransactionsPage) },
      { path: 'budgets', loadComponent: () => import('./budgets/budgets.page').then(m => m.BudgetsPage) },
      { path: 'savings', loadComponent: () => import('./savings/savings.page').then(m => m.SavingsPage) },
      { path: 'coach', loadComponent: () => import('./coach/coach.page').then(m => m.CoachPage) },
      { path: 'reports', loadComponent: () => import('./reports/reports.page').then(m => m.ReportsPage) }
    ]
  }
];

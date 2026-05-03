import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

/* =====================================================
   MODELS
===================================================== */

export interface AdminTopGoal {
  title: string;
  saved: number;
  target: number;
  percent: number;
}

export interface AdminDashboardStats {

  /* KPI */
  totalSaved: number;
  totalTarget: number;
  progress: number;

  goalCount: number;
  depositCount: number;
  userCount: number;

  totalVouchers: number;

  /* CHARTS */
  depositsByMonth: {
    [key: string]: number;
  };

  depositsByDay: {
    [key: string]: number;
  };

  goalStatus: {
    [key: string]: number;
  };

  /* TABLE */
  topGoals: AdminTopGoal[];

  /* QUICK AI */
  recommendation: string;
}

/* =====================================================
   SERVICE
===================================================== */

@Injectable({
  providedIn: 'root'
})
export class StatsadminService {

  private api =
    'http://localhost:8084/api/stats';

  constructor(
    private http: HttpClient
  ) {}

  /* =====================================================
     ADMIN DASHBOARD DATA
  ===================================================== */

  getDashboard():
    Observable<AdminDashboardStats> {

    return this.http.get<AdminDashboardStats>(
      `${this.api}/dashboard/admin`
    );

  }

  /* =====================================================
     AI SUMMARY
     short | medium | long
  ===================================================== */

  getAiSummary(
    size: 'short' | 'medium' | 'long' = 'medium'
  ): Observable<string> {

    return this.http.get(
      `${this.api}/dashboard/admin/summary?size=${size}`,
      {
        responseType: 'text'
      }
    );

  }

}
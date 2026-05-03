// stats.service.ts

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface DashboardStats {
  totalSaved: number;
  totalTarget: number;
  progress: number;

  goalCount: number;
  depositCount: number;

  totalVouchers: number;
  claimedVouchers: number;

  depositsByMonth: { [key: string]: number };
  depositsByDay: { [key: string]: number };

  goalStatus: { [key: string]: number };

  topGoals: {
    title: string;
    saved: number;
    target: number;
    percent: number;
  }[];

  recommendation: string;
}

@Injectable({
  providedIn: 'root'
})
export class StatsService {

  private api =
    'http://localhost:8084/api/stats';

  constructor(
    private http: HttpClient
  ) {}

  getUserDashboard(
    userId: number
  ): Observable<DashboardStats> {

    return this.http.get<DashboardStats>(
      `${this.api}/dashboard/user/${userId}`
    );

  }

}
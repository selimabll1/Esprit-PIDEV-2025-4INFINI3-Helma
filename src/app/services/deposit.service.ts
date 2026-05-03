import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Deposit } from '../models/deposit';

@Injectable({
  providedIn: 'root'
})
export class DepositService {

  private api = 'http://localhost:8084/api/deposits';

  constructor(private http: HttpClient) {}

  /* =====================================================
     GET ALL DEPOSITS (ADMIN)
  ===================================================== */

  getDeposits(): Observable<Deposit[]> {
    return this.http.get<Deposit[]>(this.api);
  }

  /* =====================================================
     ADD DEPOSIT
  ===================================================== */

  addDeposit(goalId: number, amount: number): Observable<Deposit> {
    return this.http.post<Deposit>(
      `${this.api}/${goalId}?amount=${amount}`,
      {}
    );
  }

  /* =====================================================
     HISTORY BY USER
  ===================================================== */

  getDepositsByUser(userId:number): Observable<Deposit[]> {
    return this.http.get<Deposit[]>(
      `${this.api}/user/${userId}`
    );
  }

  /* =====================================================
     HISTORY BY GOAL
  ===================================================== */

  getDepositsByGoal(goalId:number): Observable<Deposit[]> {
    return this.http.get<Deposit[]>(
      `${this.api}/goal/${goalId}`
    );
  }

  /* =====================================================
     GET ONE DEPOSIT
  ===================================================== */

  getDeposit(id:number): Observable<Deposit> {
    return this.http.get<Deposit>(
      `${this.api}/${id}`
    );
  }

  /* =====================================================
     EDIT DEPOSIT
  ===================================================== */

  updateDeposit(
    depositId:number,
    amount:number
  ): Observable<Deposit> {

    return this.http.put<Deposit>(
      `${this.api}/${depositId}?amount=${amount}`,
      {}
    );
  }

  /* =====================================================
     DELETE DEPOSIT
  ===================================================== */

  deleteDeposit(depositId:number): Observable<any> {
    return this.http.delete(
      `${this.api}/${depositId}`
    );
  }
  getStatementPdf(userId: number) {
    return this.http.get(
      `http://localhost:8084/api/deposits/statement/${userId}`,
      {
        responseType: 'blob' // 🔥 VERY IMPORTANT (PDF)
      }
    );
  }
}
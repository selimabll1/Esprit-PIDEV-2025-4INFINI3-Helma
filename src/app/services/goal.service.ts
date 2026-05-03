import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { Goal } from '../models/goal';
import { CreateGoal } from '../models/create-goal';

@Injectable({
  providedIn: 'root'
})
export class GoalService {

  private apiUrl = 'http://localhost:8084/api/goals';

  constructor(private http: HttpClient) {}

  // GET ALL
  getGoals(): Observable<Goal[]> {
    return this.http.get<Goal[]>(this.apiUrl);
  }

  // GET ONE
  getGoal(id:number): Observable<Goal>{
    return this.http.get<Goal>(`${this.apiUrl}/${id}`);
  }

  // CREATE
  addGoal(goal: CreateGoal): Observable<Goal> {
    return this.http.post<Goal>(this.apiUrl, goal);
  }

  // UPDATE
  updateGoal(id:number, goal: CreateGoal): Observable<Goal>{
    return this.http.put<Goal>(`${this.apiUrl}/${id}`, goal);
  }

  // DELETE
  deleteGoal(id: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${id}`);
  }
  getGoalsAdmin(): Observable<Goal[]> {
    return this.http.get<Goal[]>(`${this.apiUrl}/admin`);
  }


  
}

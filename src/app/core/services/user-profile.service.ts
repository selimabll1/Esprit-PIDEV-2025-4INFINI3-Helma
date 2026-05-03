import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface UserProfileResponse {
  firstName: string | null;
  lastName: string | null;
  phone: string | null;
  dateOfBirth: string | null;
  gender: string | null;
  country: string | null;
  city: string | null;
  address: string | null;
  occupation: string | null;
  nationalId: string | null;
  bio: string | null;
}

@Injectable({
  providedIn: 'root'
})
export class UserProfileService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/users/me/profile`;

  getMyProfile(): Observable<UserProfileResponse> {
    return this.http.get<UserProfileResponse>(this.baseUrl);
  }

  updateMyProfile(payload: Partial<UserProfileResponse>): Observable<UserProfileResponse> {
    return this.http.put<UserProfileResponse>(this.baseUrl, payload);
  }
}
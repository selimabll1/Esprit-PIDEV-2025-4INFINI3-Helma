import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Voucher } from '../models/voucher';

@Injectable({
  providedIn: 'root'
})
export class VoucherService {

  private api = "http://localhost:8084/api/vouchers";

  constructor(private http: HttpClient) {}

  // all vouchers of user
  getUserVouchers(userId:number):Observable<Voucher[]>{
    return this.http.get<Voucher[]>(`${this.api}/user/${userId}`);
  }
  getVoucher(goalId:number){
    return this.http.get<Voucher>(this.api + "/" + goalId);
  }
  
  getAllVouchers(): Observable<Voucher[]> {
    return this.http.get<Voucher[]>(this.api);
  }

  getVoucherQrUrl(voucherId: number): string {
    return `${this.api}/qr/${voucherId}`;
  }
  
}

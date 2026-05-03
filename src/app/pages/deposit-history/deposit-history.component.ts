import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { DepositService } from '../../services/deposit.service';
import { Deposit } from '../../models/deposit';

@Component({
  selector: 'app-deposit-history',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './deposit-history.component.html',
  styleUrls: ['./deposit-history.component.css']
})
export class DepositHistoryComponent implements OnInit {

  deposits: Deposit[] = [];
  total: number = 0;

  // ⭐ required by template
  goalPercent: number = 0;
  goal: any = null;

  constructor(private depositService: DepositService) {}

  ngOnInit(): void {
    this.loadHistory();
  }

  loadHistory(){

    const userId = 1; // later JWT

    this.depositService.getDepositsByUser(userId)
      .subscribe(data => {

        // newest first
        this.deposits = data.sort(
          (a,b)=> new Date(b.dateDeposit).getTime() - new Date(a.dateDeposit).getTime()
        );

        // total amount
        this.total = this.deposits.reduce((s,d)=> s + d.amount, 0);

        // fake progress (for UI circle)
        this.goalPercent = Math.min((this.total / 5000) * 100, 100);
      });
  }
}

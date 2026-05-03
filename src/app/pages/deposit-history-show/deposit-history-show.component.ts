// deposit-history-show.component.ts

import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';

import { DepositService } from '../../services/deposit.service';
import { Deposit } from '../../models/deposit';

@Component({
  selector: 'app-deposit-history-show',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './deposit-history-show.component.html',
  styleUrls: ['./deposit-history-show.component.css']
})
export class DepositHistoryShowComponent implements OnInit {

  deposits: Deposit[] = [];
  total: number = 0;
  goalId!: number;

  goalTarget: number = 1000;

  constructor(
    private route: ActivatedRoute,
    private depositService: DepositService
  ) {}

  ngOnInit(): void {

    this.goalId = Number(this.route.snapshot.paramMap.get('id'));

    this.depositService.getDepositsByGoal(this.goalId).subscribe({
      next: (data) => {

        this.deposits = data.sort(
          (a, b) =>
            new Date(b.dateDeposit).getTime() -
            new Date(a.dateDeposit).getTime()
        );

        this.total = this.deposits.reduce(
          (sum, d) => sum + d.amount,
          0
        );
      },

      error: (err) => {
        console.error('Error loading deposits:', err);
      }
    });
  }
}
import {
  Component,
  OnInit,
  computed,
  signal
} from '@angular/core';

import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClientModule } from '@angular/common/http';

import { DepositService } from '../../../services/deposit.service';
import { Deposit } from '../../../models/deposit';

@Component({
  selector: 'app-deposit-history-admin-component',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    HttpClientModule
  ],
  templateUrl: './deposit-history-admin-component.component.html',
  styleUrls: ['./deposit-history-admin-component.component.css']
})
export class DepositHistoryAdminComponentComponent implements OnInit {

  constructor(
    private depositService: DepositService
  ) {}

  /* =====================================================
     STATE
  ===================================================== */

  loading = signal(true);

  deposits = signal<Deposit[]>([]);
  filteredDeposits = signal<Deposit[]>([]);

  search = signal('');
  sortBy = signal('latest');

  currentPage = signal(1);
  pageSize = 8;

  /* VIEW MODAL */
  showViewModal = signal(false);
  selectedDeposit = signal<Deposit | null>(null);

  /* =====================================================
     INIT
  ===================================================== */

  ngOnInit(): void {
    this.loadDeposits();
  }

  /* =====================================================
     LOAD
  ===================================================== */

  loadDeposits(): void {

    this.loading.set(true);

    this.depositService.getDeposits().subscribe({

      next: (data: Deposit[]) => {

        this.deposits.set(data || []);
        this.applyFilters();
        this.loading.set(false);

      },

      error: (err: any) => {

        console.error(err);
        this.loading.set(false);

      }

    });

  }

  /* =====================================================
     FILTER
  ===================================================== */

  applyFilters(): void {

    let data = [...this.deposits()];

    const q = this.search().toLowerCase().trim();

    if (q) {

      data = data.filter(d =>

        d.id.toString().includes(q) ||

        this.getUserName(d).toLowerCase().includes(q) ||

        this.getGoalTitle(d).toLowerCase().includes(q) ||

        d.amount.toString().includes(q) ||

        d.dateDeposit?.includes(q)

      );

    }

    switch (this.sortBy()) {

      case 'latest':

        data.sort((a, b) =>
          new Date(b.dateDeposit).getTime() -
          new Date(a.dateDeposit).getTime()
        );

        break;

      case 'oldest':

        data.sort((a, b) =>
          new Date(a.dateDeposit).getTime() -
          new Date(b.dateDeposit).getTime()
        );

        break;

      case 'highest':

        data.sort((a, b) => b.amount - a.amount);

        break;

      case 'lowest':

        data.sort((a, b) => a.amount - b.amount);

        break;
    }

    this.filteredDeposits.set(data);
    this.currentPage.set(1);

  }

  /* =====================================================
     PAGINATION
  ===================================================== */

  paginatedDeposits = computed(() => {

    const start =
      (this.currentPage() - 1) * this.pageSize;

    return this.filteredDeposits().slice(
      start,
      start + this.pageSize
    );

  });

  totalPages = computed(() =>
    Math.ceil(
      this.filteredDeposits().length / this.pageSize
    )
  );

  pageNumbers(): number[] {

    return Array.from(
      { length: this.totalPages() },
      (_, i) => i + 1
    );

  }

  nextPage(): void {

    if (this.currentPage() < this.totalPages()) {
      this.currentPage.update(v => v + 1);
    }

  }

  prevPage(): void {

    if (this.currentPage() > 1) {
      this.currentPage.update(v => v - 1);
    }

  }

  goToPage(page: number): void {
    this.currentPage.set(page);
  }

  /* =====================================================
     STATS
  ===================================================== */

  totalDeposits = computed(() =>
    this.deposits().length
  );

  totalAmount = computed(() =>
    this.deposits().reduce(
      (sum, d) => sum + Number(d.amount || 0),
      0
    )
  );

  averageDeposit = computed(() => {

    if (!this.totalDeposits()) return 0;

    return Math.round(
      this.totalAmount() / this.totalDeposits()
    );

  });

  todayDeposits = computed(() => {

    const today =
      new Date().toISOString().substring(0, 10);

    return this.deposits().filter(d =>
      d.dateDeposit?.substring(0, 10) === today
    ).length;

  });

  /* =====================================================
     HELPERS
  ===================================================== */

  getUserName(d: Deposit): string {
    return d?.savingsGoal?.user?.name || 'Unknown User';
  }

  getGoalTitle(d: Deposit): string {
    return d?.savingsGoal?.title || 'No Goal';
  }

  getGoalStatus(d: Deposit): string {
    return d?.savingsGoal?.status || '-';
  }

  getGoalTarget(d: Deposit): number {
    return d?.savingsGoal?.targetAmount || 0;
  }

  getGoalCurrent(d: Deposit): number {
    return d?.savingsGoal?.currentAmount || 0;
  }

  /* =====================================================
     VIEW
  ===================================================== */

  openView(item: Deposit): void {

    this.selectedDeposit.set(item);
    this.showViewModal.set(true);

  }

  closeView(): void {

    this.showViewModal.set(false);
    this.selectedDeposit.set(null);

  }

  /* =====================================================
     DELETE
  ===================================================== */

  deleteDeposit(id: number): void {

    if (!confirm('Delete this deposit ?')) {
      return;
    }

    this.depositService.deleteDeposit(id)
      .subscribe({

        next: () => this.loadDeposits(),

        error: (err: any) =>
          console.error(err)

      });

  }

}
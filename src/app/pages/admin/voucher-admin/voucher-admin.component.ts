import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { VoucherService } from '../../../services/voucher.service';
import { Voucher } from '../../../models/voucher';

@Component({
  selector: 'app-voucher-admin',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './voucher-admin.component.html',
  styleUrl: './voucher-admin.component.css'
})
export class VoucherAdminComponent implements OnInit {

  loading = signal(true);
  vouchers = signal<Voucher[]>([]);
  search = signal('');

  constructor(private voucherService: VoucherService) {}

  ngOnInit(): void {
    this.loadVouchers();
  }

  loadVouchers(): void {
    this.loading.set(true);

    this.voucherService.getAllVouchers().subscribe({
      next: data => {
        this.vouchers.set(data || []);
        this.loading.set(false);
      },
      error: err => {
        console.error(err);
        this.loading.set(false);
      }
    });
  }

  filteredVouchers = computed(() => {
    const q = this.search().toLowerCase().trim();

    if (!q) return this.vouchers();

    return this.vouchers().filter(v =>
      v.id.toString().includes(q) ||
      (v.code || '').toLowerCase().includes(q) ||
      (v.status || '').toLowerCase().includes(q) ||
      (v.savingsGoal?.title || '').toLowerCase().includes(q) ||
      (v.savingsGoal?.user?.name || '').toLowerCase().includes(q)
    );
  });

  qrUrl(id: number): string {
    return this.voucherService.getVoucherQrUrl(id);
  }

  getUserName(v: Voucher): string {
    return v.savingsGoal?.user?.name || 'Yassmine';
  }

  getGoalTitle(v: Voucher): string {
    return v.savingsGoal?.title || 'No Goal';
  }

  copyCode(code: string): void {
    navigator.clipboard.writeText(code);
  }
}
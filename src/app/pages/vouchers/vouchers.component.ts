import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { VoucherService } from '../../services/voucher.service';
import { Voucher } from '../../models/voucher';

@Component({
  selector: 'app-vouchers',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './vouchers.component.html',
  styleUrls: ['./vouchers.component.css']
})
export class VouchersComponent implements OnInit {

  /* =====================================================
     STATE
  ===================================================== */

  loading = signal(true);

  vouchers = signal<Voucher[]>([]);

 
  constructor(
    private voucherService: VoucherService
  ) {}

  /* =====================================================
     INIT
  ===================================================== */

  userId: number = 0;

  ngOnInit(): void {
    this.userId = Number(localStorage.getItem('helma_user_id'));
    this.loadVouchers();
  }
  /* =====================================================
     LOAD
  ===================================================== */

  loadVouchers(): void {

    this.loading.set(true);

    this.voucherService.getUserVouchers(this.userId)
      .subscribe({

        next: (data: Voucher[]) => {

          this.vouchers.set(data || []);
          this.loading.set(false);

        },

        error: (err: any) => {

          console.error(err);
          this.loading.set(false);

        }

      });

  }

  /* =====================================================
     COMPUTED
  ===================================================== */

  totalRewards = computed(() =>

    this.vouchers().reduce(
      (sum, v) => sum + Number(v.value || 0),
      0
    )

  );

  totalCount = computed(() =>
    this.vouchers().length
  );

  availableRewards = computed(() =>

    this.vouchers().filter(v =>
      v.status === 'DISPONIBLE'
    )

  );

  expiredRewards = computed(() =>

    this.vouchers().filter(v =>
      v.status === 'EXPIRE'
    )

  );

  sortedVouchers = computed(() => {

    return [...this.vouchers()].sort(
      (a, b) =>
        new Date(b.creationDate).getTime() -
        new Date(a.creationDate).getTime()
    );

  });

  /* =====================================================
     HELPERS
  ===================================================== */

  isExpired(v: Voucher): boolean {

    if (!v.deadline) return false;

    return new Date(v.deadline) <
           new Date();

  }

  daysLeft(v: Voucher): number {

    if (!v.deadline) return 0;

    const today =
      new Date().getTime();

    const end =
      new Date(v.deadline).getTime();

    const diff =
      end - today;

    return Math.ceil(
      diff / (1000 * 60 * 60 * 24)
    );

  }

  getStatusClass(v: Voucher): string {

    if (this.isExpired(v)) {
      return 'expired';
    }

    switch (v.status) {

      case 'DISPONIBLE':
        return 'available';

      case 'USED':
        return 'used';

      default:
        return 'default';
    }

  }

  refresh(): void {
    this.loadVouchers();
  }

}
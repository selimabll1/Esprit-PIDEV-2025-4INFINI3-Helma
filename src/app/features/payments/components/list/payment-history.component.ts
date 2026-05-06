import { Component, Input } from '@angular/core';
import { LoanPayment } from '../../../../core/models';

@Component({
  selector: 'app-payment-history',
  templateUrl: './payment-history.component.html'
})
export class PaymentHistoryComponent {
  @Input() payments: LoanPayment[] = [];
}

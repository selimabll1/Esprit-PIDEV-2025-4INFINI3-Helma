import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Subject, debounceTime, filter, takeUntil } from 'rxjs';
import { SimulationResult } from '../../../../core/models';
import { ApiService } from '../../../../core/services/api.service';

@Component({
  selector: 'app-loan-simulation',
  templateUrl: './loan-simulation.component.html'
})
export class LoanSimulationComponent implements OnInit, OnDestroy {
  form!: FormGroup;
  simulationResult: SimulationResult | null = null;
  loading = false;
  error: string | null = null;

  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly fb: FormBuilder,
    private readonly apiService: ApiService
  ) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      principalAmount: [2500, [Validators.required, Validators.min(100)]],
      interestRate: [20, [Validators.required, Validators.min(0.1), Validators.max(100)]],
      durationMonths: [12, [Validators.required, Validators.min(1), Validators.max(240)]]
    });

    this.form.valueChanges
      .pipe(
        debounceTime(400),
        filter(() => this.form.valid),
        takeUntil(this.destroy$)
      )
      .subscribe(() => this.simulate());

    this.simulate();
  }

  simulate(): void {
    if (this.form.invalid) {
      return;
    }

    this.loading = true;
    this.error = null;

    this.apiService
      .simulateLoan(this.form.getRawValue())
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (result) => {
          this.simulationResult = result;
          this.loading = false;
        },
        error: () => {
          this.error = 'Simulation request failed.';
          this.loading = false;
        }
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}

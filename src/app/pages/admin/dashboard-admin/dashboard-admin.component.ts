import {
  Component,
  OnInit,
  signal
} from '@angular/core';

import { CommonModule } from '@angular/common';

import {
  Chart,
  registerables
} from 'chart.js';

import {
  StatsadminService,
  AdminDashboardStats
}  from '../../../services/statsadmin.service';
import { from } from 'rxjs';

Chart.register(...registerables);
 
@Component({
  selector: 'app-dashboard-admin',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard-admin.component.html',
  styleUrl: './dashboard-admin.component.css'
})
export class DashboardAdminComponent
implements OnInit {

  /* =====================================================
     STATE
  ===================================================== */

  loading =
    signal(true);

  aiLoading =
    signal(false);

  summarySize =
    signal<'short' | 'medium' | 'long'>(
      'medium'
    );

  aiSummary =
    signal(
      'Preparing executive summary...'
    );

  stats =
    signal<AdminDashboardStats | null>(
      null
    );

  monthlyChart: any;
  dailyChart: any;
  pieChart: any;

  constructor(
    private statsService:
      StatsadminService
  ) {}

  ngOnInit(): void {
    this.loadDashboard();
  }

  /* =====================================================
     LOAD DASHBOARD
  ===================================================== */

  loadDashboard(): void {

    this.loading.set(true);

    this.statsService
      .getDashboard()
      .subscribe({

        next: (data: AdminDashboardStats) => {
          this.stats.set(data);

          setTimeout(() => {

            this.destroyCharts();

            this.renderMonthlyChart();
            this.renderDailyChart();
            this.renderPieChart();

          }, 150);

          this.loadSummary();

          this.loading.set(false);

        },

        error: (err: any) => {
          console.error(err);
          this.loading.set(false);

        }

      });

  }

  /* =====================================================
     AI SUMMARY
  ===================================================== */

  loadSummary(): void {

    this.aiLoading.set(true);

    this.statsService
      .getAiSummary(
        this.summarySize()
      )
      .subscribe({

        next: (res : any) => {

          this.aiSummary.set(res);
          this.aiLoading.set(false);

        },

        error: () => {

          this.aiSummary.set(
            'Unable to generate summary.'
          );

          this.aiLoading.set(false);

        }

      });

  }

  setSummarySize(
    size: 'short' | 'medium' | 'long'
  ): void {

    this.summarySize.set(size);
    this.loadSummary();

  }

  refresh(): void {
    this.loadDashboard();
  }

  /* =====================================================
     DESTROY CHARTS
  ===================================================== */

  destroyCharts(): void {

    if (this.monthlyChart)
      this.monthlyChart.destroy();

    if (this.dailyChart)
      this.dailyChart.destroy();

    if (this.pieChart)
      this.pieChart.destroy();

  }

  /* =====================================================
     MONTHLY LINE
  ===================================================== */

  renderMonthlyChart(): void {

    const s = this.stats();
    if (!s) return;

    const canvas: any =
      document.getElementById(
        'monthlyChart'
      );

    const ctx =
      canvas.getContext('2d');

    const gradient =
      ctx.createLinearGradient(
        0, 0, 0, 320
      );

    gradient.addColorStop(
      0,
      'rgba(15,107,107,.30)'
    );

    gradient.addColorStop(
      1,
      'rgba(15,107,107,0)'
    );

    this.monthlyChart =
      new Chart('monthlyChart', {

      type: 'line',

      data: {

        labels:
          Object.keys(
            s.depositsByMonth
          ),

        datasets: [{

          data:
            Object.values(
              s.depositsByMonth
            ),

          borderColor:
            '#0F6B6B',

          backgroundColor:
            gradient,

          fill: true,
          tension: .45,
          borderWidth: 4,

          pointRadius: 5,
          pointHoverRadius: 7,

          pointBackgroundColor:
            '#fff',

          pointBorderColor:
            '#0F6B6B',

          pointBorderWidth: 3

        }]

      },

      options: {
        responsive: true,
        maintainAspectRatio: false,

        plugins: {
          legend: {
            display: false
          }
        }

      }

    });

  }

  /* =====================================================
     DAILY BAR
  ===================================================== */

  renderDailyChart(): void {

    const s = this.stats();
    if (!s) return;

    this.dailyChart =
      new Chart('dailyChart', {

      type: 'bar',

      data: {

        labels:
          Object.keys(
            s.depositsByDay
          ),

        datasets: [{

          data:
            Object.values(
              s.depositsByDay
            ),

          borderRadius: 14,
          borderSkipped: false,
          maxBarThickness: 42,

          backgroundColor: [
            '#D4A62A',
            '#f2ca63',
            '#D4A62A',
            '#f2ca63',
            '#D4A62A',
            '#f2ca63',
            '#b88d20'
          ]

        }]

      },

      options: {
        responsive: true,
        maintainAspectRatio: false,

        plugins: {
          legend: {
            display: false
          }
        }

      }

    });

  }

  /* =====================================================
     PIE
  ===================================================== */

  renderPieChart(): void {

    const s = this.stats();
    if (!s) return;

    this.pieChart =
      new Chart('pieChart', {

      type: 'doughnut',

      data: {

        labels:
          Object.keys(
            s.goalStatus
          ),

        datasets: [{

          data:
            Object.values(
              s.goalStatus
            ),

          backgroundColor: [
            '#0F6B6B',
            '#D4A62A',
            '#0B4C4C'
          ],

          borderWidth: 0,
          hoverOffset: 14,
 
        }]

      },

      options: {

        responsive: true,
        maintainAspectRatio: false,

        plugins: {

          legend: {
            position: 'bottom'
          }

        }

      }

    });

  }

}
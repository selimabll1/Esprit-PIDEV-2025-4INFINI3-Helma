// =====================================================
// dashboard.component.ts
// FULL PREMIUM VERSION
// =====================================================

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
  StatsService,
  DashboardStats
} from '../../services/stats.service';

Chart.register(...registerables);

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent implements OnInit {

  /* =====================================================
     STATE
  ===================================================== */

  loading = signal(true);

  stats =
    signal<DashboardStats | null>(null);

  aiLoading =
    signal(false);

  aiAdvice =
    signal(
      'Preparing financial intelligence...'
    );

  monthlyChart: any;
  dailyChart: any;
  pieChart: any;

  constructor(
    private statsService: StatsService
  ) {}

  ngOnInit(): void {
    this.loadStats();
  }

  /* =====================================================
     LOAD DASHBOARD
  ===================================================== */
  loadStats(): void {

    const userId = localStorage.getItem('helma_user_id');
  
    if (!userId) {
      console.error('User not found in localStorage');
      return;
    }
  
    this.loading.set(true);
  
    this.statsService
      .getUserDashboard(Number(userId)) // 🔥 FIX HERE
      .subscribe({

        next: (data) => {

          this.stats.set(data);

          setTimeout(() => {

            this.destroyCharts();

            this.renderMonthlyChart();
            this.renderDailyChart();
            this.renderPieChart();

          }, 150);

          this.loadGeneralAiAdvice();

          this.loading.set(false);

        },

        error: (err) => {

          console.error(err);
          this.loading.set(false);

        }

      });

  }

  /* =====================================================
     AI GENERAL RECOMMENDATION
  ===================================================== */

  loadGeneralAiAdvice(): void {

    const s = this.stats();

    if (!s) return;

    this.aiLoading.set(true);

    setTimeout(() => {

      const progress = s.progress;
      const goals = s.goalCount;
      const deposits = s.depositCount;
      const vouchers = s.totalVouchers;
      const saved = s.totalSaved;
      const target = s.totalTarget;

      let advice = '';

      if (progress >= 100) {

        advice =
          `Excellent performance. You reached your financial targets with ${saved} TND saved. Consider creating bigger goals or starting investments.`;

      }

      else if (progress >= 75) {

        advice =
          `Strong momentum detected. You're ${progress}% toward your targets. Maintain consistency and slightly increase deposits.`;

      }

      else if (progress >= 50) {

        advice =
          `Good progress overall. You're halfway there. Focus on priority goals and keep weekly deposits active.`;

      }

      else if (progress >= 25) {

        advice =
          `Savings started well, but stronger consistency would accelerate results. Consider automatic deposits.`;

      }

      else {

        advice =
          `Early-stage profile detected. Start with small recurring savings and build discipline over time.`;

      }

      if (goals >= 5) {

        advice +=
          ` You manage ${goals} goals. Prioritize your top 2 goals for faster wins.`;

      }

      if (deposits <= 2) {

        advice +=
          ` Deposit frequency is low. More frequent deposits can significantly improve progress.`;

      }

      if (vouchers > 0) {

        advice +=
          ` You unlocked ${vouchers} rewards. Great engagement level.`;

      }

      if ((target - saved) < 200 && progress < 100) {

        advice +=
          ` You are very close to completion. One strong deposit may finish a goal now.`;

      }

      this.aiAdvice.set(advice);

      this.aiLoading.set(false);

    }, 1200);

  }

  refreshAi(): void {
    this.loadGeneralAiAdvice();
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
     MONTHLY CHART
  ===================================================== */

  renderMonthlyChart(): void {

    const s = this.stats();
    if (!s) return;

    const canvas: any =
      document.getElementById('monthlyChart');

    const ctx =
      canvas.getContext('2d');

    const gradient =
      ctx.createLinearGradient(
        0, 0, 0, 350
      );

    gradient.addColorStop(
      0, 'rgba(15,107,107,.35)'
    );

    gradient.addColorStop(
      1, 'rgba(15,107,107,0)'
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

          borderColor: '#0F6B6B',
          backgroundColor: gradient,
          fill: true,
          tension: .45,
          borderWidth: 4,
          pointRadius: 5,
          pointHoverRadius: 8,
          pointBackgroundColor: '#fff',
          pointBorderColor: '#0F6B6B',
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
        },

        scales: {

          x: {
            grid: {
              display: false
            }
          },

          y: {
            beginAtZero: true,
            grid: {
              color:
                'rgba(15,107,107,.08)'
            }
          }

        }

      }

    });

  }

  /* =====================================================
     DAILY BAR CHART
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

          borderRadius: 16,
          borderSkipped: false,
          maxBarThickness: 42,

          backgroundColor: [
  '#0F6B6B',  // Deep Teal
  '#D4A62A',  // Dream Gold ✨
  '#168080',  // Medium Teal
  '#D4A62A',  // Dream Gold ✨
  '#0F6B6B',  // Deep Teal
  '#D4A62A',  // Dream Gold ✨
  '#0B4C4C'   // Dark Teal
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
        },

        scales: {

          x: {
            grid: {
              display: false
            }
          },

          y: {
            beginAtZero: true,
            grid: {
              color:
                'rgba(15,107,107,.08)'
            }
          }

        }

      }

    });

  }

  /* =====================================================
     PIE CHART
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
          '#0F6B6B',  // ACHIEVED - Deep Teal
          '#D4A62A',  // IN_PROGRESS - Dream Gold ✨
          '#DDF4EC'   // EXPIRED - Fresh Mint
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
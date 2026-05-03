import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import { GoalService } from '../../services/goal.service';
import { DepositService } from '../../services/deposit.service';
import { VoucherService } from '../../services/voucher.service';

import { Goal } from '../../models/goal';
import { CreateGoal } from '../../models/create-goal';
import { Voucher } from '../../models/voucher';

@Component({
  selector: 'app-goals',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './goals.component.html',
  styleUrls: ['./goals.component.css']
})
export class GoalsComponent implements OnInit {

  /* ================= DATA ================= */
  goals: Goal[] = [];
  totalSaved = 0;
  globalPercent = 0;
  showPrediction: Record<number, boolean> = {};

  selectedGoal!: Goal;
  selectedId = 0;
  remainingAmount = 0;
  depositAmount = 0;

  newGoal: CreateGoal = {
    title: '',
    targetAmount: 0,
    deadline: ''
  };

  editGoal: any = {};

  /* 🔥 CLEAN MODAL STATE */
  modal: 'add' | 'delete' | 'deposit' | 'edit' | 'prediction' | 'voucher' | null = null;

  voucherData: Voucher | null = null;
  voucherError: string | null = null;

  constructor(
    private goalService: GoalService,
    private depositService: DepositService,
    private voucherService: VoucherService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadGoals();
  }

  /* ================= LOAD ================= */
  loadGoals() {
    this.goalService.getGoals().subscribe(data => {
      this.goals = data;
      this.calculateGlobalStats();
    });
  }

  calculateGlobalStats() {
    let totalCurrent = 0;
    let totalTarget = 0;

    this.goals.forEach(g => {
      totalCurrent += g.currentAmount || 0;
      totalTarget += g.targetAmount || 0;
    });

    this.totalSaved = totalCurrent;
    this.globalPercent = totalTarget > 0 ? (totalCurrent * 100) / totalTarget : 0;
  }

  /* ================= HELPERS ================= */
  isGoalCompleted(g: Goal): boolean {
    return (g.currentAmount || 0) >= g.targetAmount;
  }

  remaining(g: Goal): number {
    return g.targetAmount - (g.currentAmount || 0);
  }

  progressWidth(g: Goal): number {
    const percent = ((g.currentAmount || 0) * 100) / g.targetAmount;
    return percent > 100 ? 100 : percent;
  }

  isExpired(goal: Goal): boolean {
  if (!goal.deadline) return false;

  const today = new Date();
  const deadline = new Date(goal.deadline);

  return deadline < today;
}

  /* ================= PREDICTION ================= */
  getPrediction(g: Goal): { 
    label: string; 
    color: string; 
    message: string; 
    remaining: number; 
    daysRemaining: number; 
    requiredPerDay: number;
    insight: string;
    estimatedCompletion?: string;
    icon: string;
  } {
  
    const now = new Date();
    const deadline = new Date(g.deadline);
    const creation = new Date(g.creationDate);
  
    const current = g.currentAmount || 0;
    const target = g.targetAmount;
  
    const daysRemaining = Math.floor((deadline.getTime() - now.getTime()) / 86400000);
    const daysSinceCreation = Math.max(
      1,
      Math.floor((now.getTime() - creation.getTime()) / 86400000)
    );
  
    const remaining = target - current;
  
    const actualPerDay = current / daysSinceCreation;
    const requiredPerDay = daysRemaining > 0 ? remaining / daysRemaining : 0;
  
    // 🔥 ESTIMATION
    let estimatedCompletion = '';
    if (actualPerDay > 0) {
      const daysToComplete = remaining / actualPerDay;
      const estDate = new Date();
      estDate.setDate(estDate.getDate() + daysToComplete);
  
      estimatedCompletion = estDate.toLocaleDateString('en-GB', {
        day: 'numeric',
        month: 'short',
        year: 'numeric'
      });
    } else {
      estimatedCompletion = 'Not enough data to estimate';
    }
  
    // 🎯 ACHIEVED
    if (g.status === 'ACHIEVED' || current >= target) {
      return {
        label: 'Goal Achieved',
        color: 'success',
        message: 'Congratulations! You have successfully reached your goal.',
        remaining: 0,
        daysRemaining: 0,
        requiredPerDay: 0,
        insight: 'Your objective is complete. You can now claim your reward!',
        estimatedCompletion: 'Completed',
        icon: 'bi-check-circle-fill'
      };
    }
  
    // ⛔ DEADLINE PASSED
    if (daysRemaining <= 0) {
      return {
        label: 'Deadline Passed',
        color: 'danger',
        message: 'The deadline has passed. Consider extending your goal.',
        remaining,
        daysRemaining: 0,
        requiredPerDay: 0,
        insight: 'Time is up! Re-evaluate your strategy.',
        estimatedCompletion: 'Overdue',
        icon: 'bi-exclamation-octagon-fill'
      };
    }
  
    // 🚀 ON TRACK
    if (actualPerDay >= requiredPerDay) {
      return {
        label: 'On Track',
        color: 'success',
        message: "You're on track. Keep saving at your current pace.",
        remaining,
        daysRemaining,
        requiredPerDay,
        insight: `You save ~${actualPerDay.toFixed(1)} TND/day, which is enough.`,
        estimatedCompletion,
        icon: 'bi-graph-up-arrow'
      };
    }
  
    // ⚠️ BEHIND
    return {
      label: 'Behind Schedule',
      color: 'warning',
      message: 'You are slightly behind schedule.',
      remaining,
      daysRemaining,
      requiredPerDay,
      insight: `You need +${(requiredPerDay - actualPerDay).toFixed(1)} TND/day.`,
      estimatedCompletion,
      icon: 'bi-exclamation-triangle-fill'
    };
  }
  /* ================= MODAL ================= */
  openAddModal() {
    this.newGoal = { title: '', targetAmount: 0, deadline: '' };
    this.modal = 'add';
  }

  openDeleteModal(id: number) {
    const goal = this.goals.find(g => g.id === id);
    if (goal && this.isGoalCompleted(goal)) {
      alert('Completed goals cannot be deleted.');
      return;
    }
    this.selectedId = id;
    this.modal = 'delete';
  }

  openDepositModal(goalId: number) {
  const goal = this.goals.find(g => g.id === goalId);
  if (!goal) return;

  if (this.isGoalCompleted(goal)) {
    alert('Goal already completed.');
    return;
  }

  if (this.isExpired(goal)) {
    alert('This goal has expired. Deposits are no longer allowed.');
    return;
  }

  this.selectedGoal = goal;
  this.selectedId = goalId;
  this.depositAmount = 0;
  this.remainingAmount = this.remaining(goal);

  this.modal = 'deposit';
}

  openEditModal(goal: Goal) {
    if (this.isGoalCompleted(goal)) {
      alert('Goal already completed.');
      return;
    }

    this.editGoal = {
      id: goal.id,
      title: goal.title,
      targetAmount: goal.targetAmount,
      deadline: goal.deadline?.substring(0, 10)
    };

    this.modal = 'edit';
  }

  openPredictionModal(goal: Goal) {
    this.selectedGoal = goal;
    this.modal = 'prediction';
  }

  closeModal() {
    this.modal = null;
    this.voucherData = null;
    this.voucherError = null;
  }

  /* ================= ACTIONS ================= */

  addGoal() {
    const userId = localStorage.getItem('helma_user_id');
  
    if (!userId) {
      alert('User not logged in');
      return;
    }
  
    const payload = {
      ...this.newGoal,
      userId: Number(userId)
    };
  
    this.goalService.addGoal(payload).subscribe({
      next: () => {
        this.closeModal();
        this.loadGoals();
      },
      error: err => {
        console.error(err);
        alert(err.error?.message || 'Error creating goal');
      }
    });
  }
  updateGoal() {
    if (!this.editGoal.title || this.editGoal.targetAmount <= 0) {
      alert('Invalid data');
      return;
    }

    this.goalService.updateGoal(this.editGoal.id, this.editGoal).subscribe(() => {
      this.closeModal();
      this.loadGoals();
    });
  }

  deleteGoal() {
    this.goalService.deleteGoal(this.selectedId).subscribe({
      next: () => {
        this.closeModal();
        this.loadGoals();
      },
      error: err => alert(err.error?.message || 'Delete failed')
    });
  }

  depositMoney() {
    if (this.depositAmount <= 0) {
      alert('Invalid amount');
      return;
    }

    if (this.depositAmount > this.remainingAmount) {
      alert(`Max: ${this.remainingAmount}`);
      return;
    }

    this.depositService.addDeposit(this.selectedId, this.depositAmount).subscribe({
      next: () => {
        this.closeModal();
        this.loadGoals();
      },
      error: err => alert(err.error?.message || 'Deposit failed')
    });
  }

  /* ================= NAV ================= */
  goToGoalHistory(goalId: number) {
    this.router.navigate(['/youth/history/goal', goalId]);
  }

  /* ================= VOUCHER ================= */
  claimVoucher(goalId: number) {
    this.voucherService.getVoucher(goalId).subscribe({
      next: (v: Voucher) => {
        this.voucherData = v;
        this.modal = 'voucher';
      },
      error: (err) => {
        this.voucherError = err.error?.message || 'Failed to claim voucher';
        this.modal = 'voucher';
      }
    });
  }










  downloadPdf() {
    const userId = localStorage.getItem('helma_user_id');
  
    if (!userId) {
      alert('User not found');
      return;
    }
  
    this.depositService.getStatementPdf(Number(userId)).subscribe({
      next: (blob: Blob) => {
        const url = window.URL.createObjectURL(blob);
  
        const a = document.createElement('a');
        a.href = url;
        a.download = 'statement.pdf'; // file name
        a.click();
  
        window.URL.revokeObjectURL(url);
      },
      error: () => {
        alert('Failed to download PDF');
      }
    });
  }
  
}
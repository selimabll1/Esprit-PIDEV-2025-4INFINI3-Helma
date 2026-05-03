import {
  Component,
  OnInit,
  computed,
  signal
} from '@angular/core';

import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { GoalService } from '../../../services/goal.service';
import { Goal } from '../../../models/goal';

@Component({
  selector: 'app-goals-admin-component',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './goals-admin-component.component.html',
  styleUrls: ['./goals-admin-component.component.css']
})
export class GoalsAdminComponentComponent implements OnInit {

  constructor(private goalService: GoalService) {}

  /* =====================================================
     MAIN STATE
  ===================================================== */

  loading = signal(true);

  goals = signal<Goal[]>([]);
  filteredGoals = signal<Goal[]>([]);

  search = signal('');
  selectedStatus = signal('ALL');
  sortBy = signal('deadline');

  currentPage = signal(1);
  pageSize = 6;

  /* =====================================================
     VIEW MODAL
  ===================================================== */

  showViewModal = signal(false);
  selectedGoal = signal<Goal | null>(null);

  /* =====================================================
     EDIT MODAL
  ===================================================== */

  showEditModal = signal(false);
  editingId = signal<number | null>(null);

  editForm = signal({
    title: '',
    targetAmount: 0,
    currentAmount: 0,
    deadline: '',
    status: 'IN_PROGRESS'
  });

  /* =====================================================
     DELETE MODAL
  ===================================================== */

  showDeleteModal = signal(false);
  selectedIdToDelete = signal<number | null>(null);

  /* =====================================================
     INIT
  ===================================================== */

  ngOnInit(): void {
    this.loadGoals();
  }

  /* =====================================================
     LOAD DATA
  ===================================================== */

  loadGoals(): void {

    this.loading.set(true);

    this.goalService.getGoalsAdmin().subscribe({
      next: (data) => {

        this.goals.set(data || []);
        this.applyFilters();

        this.loading.set(false);
      },

      error: (err) => {
        console.error(err);
        this.loading.set(false);
      }
    });

  }

  /* =====================================================
     FILTERS
  ===================================================== */

  applyFilters(): void {

    let data = [...this.goals()];

    const q = this.search().toLowerCase().trim();

    if (q) {
      data = data.filter(g =>
        (g.title || '').toLowerCase().includes(q) ||
        (g.id?.toString() || '').includes(q) ||
        this.getUserName(g).toLowerCase().includes(q)
      );
    }

    if (this.selectedStatus() !== 'ALL') {
      data = data.filter(g => g.status === this.selectedStatus());
    }

    switch (this.sortBy()) {

      case 'deadline':
        data.sort((a,b)=>
          new Date(a.deadline).getTime() -
          new Date(b.deadline).getTime()
        );
        break;

      case 'progress':
        data.sort((a,b)=>
          this.getProgress(b) - this.getProgress(a)
        );
        break;

      case 'target':
        data.sort((a,b)=>
          b.targetAmount - a.targetAmount
        );
        break;

      case 'title':
        data.sort((a,b)=>
          a.title.localeCompare(b.title)
        );
        break;
    }

    this.filteredGoals.set(data);
    this.currentPage.set(1);
  }

  /* =====================================================
     PAGINATION
  ===================================================== */

  paginatedGoals = computed(() => {

    const start =
      (this.currentPage() - 1) * this.pageSize;

    return this.filteredGoals().slice(
      start,
      start + this.pageSize
    );
  });

  totalPages = computed(() =>
    Math.ceil(
      this.filteredGoals().length / this.pageSize
    )
  );

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

  goToPage(page:number): void {
    this.currentPage.set(page);
  }

  pageNumbers(): number[] {

    return Array.from(
      { length: this.totalPages() },
      (_, i) => i + 1
    );

  }

  /* =====================================================
     STATS
  ===================================================== */

  totalGoals = computed(() =>
    this.goals().length
  );

  completedGoals = computed(() =>
    this.goals().filter(
      g => g.status === 'ACHIEVED'
    ).length
  );

  activeGoals = computed(() =>
    this.goals().filter(
      g => g.status === 'IN_PROGRESS'
    ).length
  );

  expiredGoals = computed(() =>
    this.goals().filter(
      g => g.status === 'EXPIRED'
    ).length
  );

  totalTarget = computed(() =>
    this.goals().reduce(
      (sum,g)=>sum + Number(g.targetAmount || 0),
      0
    )
  );

  totalSaved = computed(() =>
    this.goals().reduce(
      (sum,g)=>sum + Number(g.currentAmount || 0),
      0
    )
  );

  completionRate = computed(() => {

    if (!this.totalGoals()) return 0;

    return Math.round(
      (this.completedGoals() / this.totalGoals()) * 100
    );

  });

  /* =====================================================
     HELPERS
  ===================================================== */

  getProgress(goal: Goal): number {

    if (!goal.targetAmount) return 0;

    return Math.min(
      100,
      Math.round(
        (goal.currentAmount / goal.targetAmount) * 100
      )
    );
  }

  getDaysLeft(date:string): number {

    const today = new Date().getTime();
    const end = new Date(date).getTime();

    return Math.ceil(
      (end - today) / 86400000
    );
  }

  statusClass(goal: Goal): string {

    switch (goal.status) {

      case 'ACHIEVED':
        return 'done';

      case 'EXPIRED':
        return 'late';

      default:
        return 'active';
    }

  }

  statusLabel(status:string): string {

    switch(status){

      case 'ACHIEVED':
        return 'Achieved';

      case 'EXPIRED':
        return 'Expired';

      default:
        return 'In Progress';
    }

  }

  getUserName(goal:any): string {

    if (goal?.user?.name) {
      return goal.user.name;
    }

    return 'Unknown User';
  }

  /* =====================================================
     VIEW MODAL
  ===================================================== */

  openView(goal: Goal): void {

    this.selectedGoal.set(goal);
    this.showViewModal.set(true);

  }

  closeView(): void {

    this.showViewModal.set(false);
    this.selectedGoal.set(null);

  }

  /* =====================================================
     EDIT MODAL
  ===================================================== */

  openEdit(goal: Goal): void {

    this.editingId.set(goal.id!);

    this.editForm.set({
      title: goal.title,
      targetAmount: goal.targetAmount,
      currentAmount: goal.currentAmount,
      deadline: goal.deadline?.substring(0,10),
      status: goal.status
    });

    this.showEditModal.set(true);
  }

  closeEdit(): void {

    this.showEditModal.set(false);
    this.editingId.set(null);

  }

  updateEditField(field:string, value:any): void {

    this.editForm.update(data => ({
      ...data,
      [field]: value
    }));

  }

  saveEdit(): void {

    const id = this.editingId();

    if (!id) return;

    const payload = {
      title: this.editForm().title.trim(),
      targetAmount: Number(this.editForm().targetAmount),
      currentAmount: Number(this.editForm().currentAmount),
      deadline: this.editForm().deadline,
      status: this.editForm().status
    };

    if (!payload.title) {
      alert('Title required');
      return;
    }

    this.goalService.updateGoal(id, payload).subscribe({

      next: () => {
        this.closeEdit();
        this.loadGoals();
      },

      error: (err) => {
        console.error(err);
        alert('Update failed');
      }

    });

  }

  /* =====================================================
     QUICK ACTIONS
  ===================================================== */

  setGoalStatus(goal: Goal, status:string): void {

    const payload = {
      title: goal.title,
      targetAmount: goal.targetAmount,
      currentAmount: goal.currentAmount,
      deadline: goal.deadline,
      status: status
    };

    this.goalService.updateGoal(goal.id!, payload)
      .subscribe(() => this.loadGoals());

  }

  addProgress(goal: Goal, amount:number): void {

    const newAmount =
      Number(goal.currentAmount) + amount;

    const payload = {
      title: goal.title,
      targetAmount: goal.targetAmount,
      currentAmount: newAmount,
      deadline: goal.deadline,
      status: newAmount >= goal.targetAmount
        ? 'ACHIEVED'
        : 'IN_PROGRESS'
    };

    this.goalService.updateGoal(goal.id!, payload)
      .subscribe(() => this.loadGoals());

  }

  /* =====================================================
     DELETE MODAL
  ===================================================== */

  // Ouvre le modal de confirmation
  openDelete(id: number): void {
    this.selectedIdToDelete.set(id);
    this.showDeleteModal.set(true);
  }

  // Confirme et exécute la suppression
  confirmDelete(): void {
    const id = this.selectedIdToDelete();
    if (!id) return;

    this.goalService.deleteGoal(id).subscribe({
      next: () => {
        this.showDeleteModal.set(false);
        this.selectedIdToDelete.set(null);
        this.loadGoals();
      },
      error: (err) => {
        console.error('Delete failed:', err);
        alert('Delete failed');
      }
    });
  }

}
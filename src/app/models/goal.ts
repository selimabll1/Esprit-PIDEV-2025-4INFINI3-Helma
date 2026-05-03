export interface Goal {
  id?: number;
  title: string;
  targetAmount: number;
  currentAmount: number;
  deadline: string;
  status: string;
  creationDate: string;  // ← AJOUTE CETTE LIGNE
  userId?: number; // 👈 add this

}
export interface Deposit {
  id: number;
  amount: number;
  dateDeposit: string;

  savingsGoal?: {
    id: number;
    title: string;
    targetAmount: number;
    currentAmount: number;

    creationDate: string;
    deadline?: string | null;

    status: string;

    user?: {
      id: number;
      name: string;
    };
  };
}
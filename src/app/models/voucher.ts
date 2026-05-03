export interface Voucher {
  id: number;
  code: string;
  value: number;
  creationDate: string;
  deadline: string;
  status: string;
  claimed: boolean;

  savingsGoal?: {
    id: number;
    title: string;
    targetAmount: number;
    currentAmount: number;
    user?: {
      id: number;
      name: string;
    };
  };
}
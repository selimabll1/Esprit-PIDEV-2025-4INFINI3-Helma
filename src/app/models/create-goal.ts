export interface CreateGoal{
    title:string;
    targetAmount:number;
    deadline:string;
    userId?: number; // 👈 add this

  }
  
export interface RiskAnalysis {
  goalId: number;
  goalTitle: string;
  userName: string;
  riskLevel: 'HIGH' | 'MEDIUM' | 'LOW' | 'INACTIVE';
  reason: string;
  recommendation: string;
}

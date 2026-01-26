import { Response } from 'express';
import { AuthRequest } from '../middleware/auth';
export declare const getBudget: (req: AuthRequest, res: Response) => Promise<void>;
export declare const setBudgetLimit: (req: AuthRequest, res: Response) => Promise<void>;
export declare const recordExpense: (req: AuthRequest, res: Response) => Promise<void>;
export declare const getExpenses: (req: AuthRequest, res: Response) => Promise<void>;
//# sourceMappingURL=budget.d.ts.map
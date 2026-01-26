import { Response } from 'express';
import { AuthRequest } from '../middleware/auth';
export declare const createTask: (req: AuthRequest, res: Response) => Promise<void>;
export declare const getTasks: (req: AuthRequest, res: Response) => Promise<void>;
export declare const completeTask: (req: AuthRequest, res: Response) => Promise<Response<any, Record<string, any>> | undefined>;
export declare const updateTask: (req: AuthRequest, res: Response) => Promise<Response<any, Record<string, any>> | undefined>;
export declare const getTasksByFrequency: (req: AuthRequest, res: Response) => Promise<void>;
//# sourceMappingURL=tasks.d.ts.map
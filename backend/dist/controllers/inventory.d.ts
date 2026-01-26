import { Response } from 'express';
import { AuthRequest } from '../middleware/auth';
export declare const addInventoryItem: (req: AuthRequest, res: Response) => Promise<void>;
export declare const getInventory: (req: AuthRequest, res: Response) => Promise<void>;
export declare const getLowStockItems: (req: AuthRequest, res: Response) => Promise<void>;
export declare const updateInventoryItem: (req: AuthRequest, res: Response) => Promise<void>;
export declare const deleteInventoryItem: (req: AuthRequest, res: Response) => Promise<Response<any, Record<string, any>> | undefined>;
//# sourceMappingURL=inventory.d.ts.map
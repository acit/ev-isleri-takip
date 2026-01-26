import { Response } from 'express';
import { AuthRequest } from '../middleware/auth';
export declare const createShoppingList: (req: AuthRequest, res: Response) => Promise<void>;
export declare const getShoppingLists: (req: AuthRequest, res: Response) => Promise<void>;
export declare const generateShoppingListFromLowStock: (req: AuthRequest, res: Response) => Promise<Response<any, Record<string, any>> | undefined>;
export declare const getShoppingListItems: (req: AuthRequest, res: Response) => Promise<Response<any, Record<string, any>> | undefined>;
export declare const updateShoppingListItem: (req: AuthRequest, res: Response) => Promise<Response<any, Record<string, any>> | undefined>;
export declare const sendShoppingListByEmail: (req: AuthRequest, res: Response) => Promise<Response<any, Record<string, any>> | undefined>;
//# sourceMappingURL=shopping.d.ts.map
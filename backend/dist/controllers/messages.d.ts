import { Response } from 'express';
import { AuthRequest } from '../middleware/auth';
export declare const sendMessage: (req: AuthRequest, res: Response) => Promise<Response<any, Record<string, any>> | undefined>;
export declare const getMessages: (req: AuthRequest, res: Response) => Promise<void>;
export declare const markAsRead: (req: AuthRequest, res: Response) => Promise<Response<any, Record<string, any>> | undefined>;
export declare const getFamilyMembers: (req: AuthRequest, res: Response) => Promise<void>;
export declare const getUnreadCount: (req: AuthRequest, res: Response) => Promise<void>;
//# sourceMappingURL=messages.d.ts.map
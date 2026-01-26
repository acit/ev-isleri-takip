import { Response } from 'express';
import { AuthRequest } from '../middleware/auth';
export declare const loginWithPassword: (req: AuthRequest, res: Response) => Promise<Response<any, Record<string, any>> | undefined>;
export declare const setPassword: (req: AuthRequest, res: Response) => Promise<Response<any, Record<string, any>> | undefined>;
export declare const resetPassword: (req: AuthRequest, res: Response) => Promise<Response<any, Record<string, any>> | undefined>;
export declare const confirmPasswordReset: (req: AuthRequest, res: Response) => Promise<Response<any, Record<string, any>> | undefined>;
export declare const sendLoginCode: (req: AuthRequest, res: Response) => Promise<void>;
export declare const verifyLoginCode: (req: AuthRequest, res: Response) => Promise<Response<any, Record<string, any>> | undefined>;
export declare const inviteFamilyMember: (req: AuthRequest, res: Response) => Promise<Response<any, Record<string, any>> | undefined>;
export declare const acceptInvite: (req: AuthRequest, res: Response) => Promise<Response<any, Record<string, any>> | undefined>;
export declare const getEmailStatus: (req: AuthRequest, res: Response) => Promise<void>;
//# sourceMappingURL=auth.d.ts.map
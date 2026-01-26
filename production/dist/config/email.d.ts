import nodemailer from 'nodemailer';
export declare const sendEmail: (to: string, subject: string, html: string) => Promise<boolean>;
export declare const sendLoginCode: (email: string, code: string) => Promise<boolean>;
declare const _default: nodemailer.Transporter<import("nodemailer/lib/smtp-transport").SentMessageInfo, import("nodemailer/lib/smtp-transport").Options>;
export default _default;
//# sourceMappingURL=email.d.ts.map
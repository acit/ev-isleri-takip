"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.formatEmailList = exports.getEmailStatus = exports.checkEmailConfiguration = void 0;
const dotenv_1 = __importDefault(require("dotenv"));
dotenv_1.default.config();
const checkEmailConfiguration = () => {
    const hasEmailConfig = process.env.EMAIL_USER &&
        process.env.EMAIL_PASSWORD &&
        process.env.EMAIL_USER !== 'your_email@gmail.com' &&
        process.env.EMAIL_USER !== 'erhanfirat@gmail.com' &&
        process.env.EMAIL_USER !== 'erhan.koksa@gmail.com' &&
        process.env.EMAIL_PASSWORD !== 'your_gmail_app_password_here' &&
        process.env.EMAIL_PASSWORD !== 'your_real_gmail_app_password_here' &&
        process.env.EMAIL_PASSWORD !== 'abcdefghijklmnop' &&
        process.env.EMAIL_PASSWORD !== 'abcd efgh ijkl mnop';
    return {
        isConfigured: !!hasEmailConfig,
        user: process.env.EMAIL_USER,
        service: process.env.EMAIL_SERVICE || 'gmail',
        from: process.env.EMAIL_FROM || process.env.EMAIL_USER
    };
};
exports.checkEmailConfiguration = checkEmailConfiguration;
const getEmailStatus = () => {
    const config = (0, exports.checkEmailConfiguration)();
    if (config.isConfigured) {
        return {
            status: 'configured',
            message: `✅ E-posta sistemi aktif (${config.user})`,
            mode: 'production'
        };
    }
    else {
        return {
            status: 'not_configured',
            message: '⚠️ E-posta sistemi yapılandırılmamış - kodlar console\'da gösterilecek',
            mode: 'development'
        };
    }
};
exports.getEmailStatus = getEmailStatus;
const formatEmailList = (emails) => {
    return emails
        .map(email => email.trim().toLowerCase())
        .filter(email => email && /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email));
};
exports.formatEmailList = formatEmailList;
//# sourceMappingURL=emailHelper.js.map
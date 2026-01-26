"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.getEmailStatus = exports.testEmailConnection = void 0;
const email_1 = require("../config/email");
const testEmailConnection = async (req, res) => {
    try {
        console.log('🧪 Email bağlantısı test ediliyor...');
        // Test email gönder
        const testEmail = req.body.email || process.env.EMAIL_USER;
        const testCode = '123456';
        const success = await (0, email_1.sendLoginCode)(testEmail, testCode);
        if (success) {
            res.json({
                success: true,
                message: 'Email başarıyla gönderildi',
                email: testEmail,
                timestamp: new Date().toISOString()
            });
        }
        else {
            res.status(500).json({
                success: false,
                message: 'Email gönderilemedi - konsol loglarını kontrol edin',
                email: testEmail,
                timestamp: new Date().toISOString()
            });
        }
    }
    catch (error) {
        console.error('❌ Email test hatası:', error);
        res.status(500).json({
            success: false,
            message: error.message,
            error: error.code || 'UNKNOWN_ERROR',
            timestamp: new Date().toISOString()
        });
    }
};
exports.testEmailConnection = testEmailConnection;
const getEmailStatus = async (req, res) => {
    try {
        const emailConfig = {
            service: process.env.EMAIL_SERVICE || 'gmail',
            user: process.env.EMAIL_USER || 'NOT_SET',
            passwordSet: !!process.env.EMAIL_PASSWORD,
            from: process.env.EMAIL_FROM || 'NOT_SET'
        };
        const isConfigured = emailConfig.user !== 'NOT_SET' &&
            emailConfig.passwordSet &&
            emailConfig.user !== 'your_email@gmail.com';
        res.json({
            configured: isConfigured,
            config: {
                ...emailConfig,
                password: emailConfig.passwordSet ? '***SET***' : 'NOT_SET'
            },
            recommendations: isConfigured ? [] : [
                'Gmail hesabınızda 2FA aktif olmalı',
                'App Password oluşturun: https://myaccount.google.com/apppasswords',
                '.env dosyasında EMAIL_USER ve EMAIL_PASSWORD ayarlayın',
                'Backend\'i yeniden başlatın'
            ]
        });
    }
    catch (error) {
        console.error('❌ Email status hatası:', error);
        res.status(500).json({
            error: error.message,
            timestamp: new Date().toISOString()
        });
    }
};
exports.getEmailStatus = getEmailStatus;
//# sourceMappingURL=emailTest.js.map
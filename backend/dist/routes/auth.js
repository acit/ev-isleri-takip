"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || (function () {
    var ownKeys = function(o) {
        ownKeys = Object.getOwnPropertyNames || function (o) {
            var ar = [];
            for (var k in o) if (Object.prototype.hasOwnProperty.call(o, k)) ar[ar.length] = k;
            return ar;
        };
        return ownKeys(o);
    };
    return function (mod) {
        if (mod && mod.__esModule) return mod;
        var result = {};
        if (mod != null) for (var k = ownKeys(mod), i = 0; i < k.length; i++) if (k[i] !== "default") __createBinding(result, mod, k[i]);
        __setModuleDefault(result, mod);
        return result;
    };
})();
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
const express_1 = __importDefault(require("express"));
const auth_1 = require("../middleware/auth");
const authController = __importStar(require("../controllers/auth"));
const router = express_1.default.Router();
router.post('/login-code', async (req, res) => {
    await authController.sendLoginCode(req, res);
});
router.post('/verify-code', async (req, res) => {
    await authController.verifyLoginCode(req, res);
});
router.post('/login', async (req, res) => {
    await authController.loginWithPassword(req, res);
});
router.post('/reset-password', async (req, res) => {
    await authController.resetPassword(req, res);
});
router.post('/confirm-reset', async (req, res) => {
    await authController.confirmPasswordReset(req, res);
});
router.get('/email-status', auth_1.authMiddleware, async (req, res) => {
    await authController.getEmailStatus(req, res);
});
router.post('/set-password', auth_1.authMiddleware, async (req, res) => {
    await authController.setPassword(req, res);
});
router.get('/test-email-config', auth_1.authMiddleware, async (req, res) => {
    const { testEmailConfig } = require('../controllers/emailTest');
    await testEmailConfig(req, res);
});
router.post('/send-test-email', auth_1.authMiddleware, async (req, res) => {
    const { sendTestEmail } = require('../controllers/emailTest');
    await sendTestEmail(req, res);
});
router.post('/invite', auth_1.authMiddleware, async (req, res) => {
    await authController.inviteFamilyMember(req, res);
});
router.post('/accept-invite', async (req, res) => {
    await authController.acceptInvite(req, res);
});
exports.default = router;
//# sourceMappingURL=auth.js.map
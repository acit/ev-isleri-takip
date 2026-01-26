import express from 'express';
import { authMiddleware } from '../middleware/auth';
import * as authController from '../controllers/auth';

const router = express.Router();

router.post('/login-code', async (req, res) => {
  await authController.sendLoginCode(req as any, res);
});

router.post('/verify-code', async (req, res) => {
  await authController.verifyLoginCode(req as any, res);
});

router.post('/login', async (req, res) => {
  await authController.loginWithPassword(req as any, res);
});

router.post('/reset-password', async (req, res) => {
  await authController.resetPassword(req as any, res);
});

router.post('/confirm-reset', async (req, res) => {
  await authController.confirmPasswordReset(req as any, res);
});

router.get('/email-status', authMiddleware, async (req, res) => {
  await authController.getEmailStatus(req as any, res);
});

router.post('/set-password', authMiddleware, async (req, res) => {
  await authController.setPassword(req as any, res);
});

router.get('/test-email-config', authMiddleware, async (req, res) => {
  const { testEmailConfig } = require('../controllers/emailTest');
  await testEmailConfig(req as any, res);
});

router.post('/send-test-email', authMiddleware, async (req, res) => {
  const { sendTestEmail } = require('../controllers/emailTest');
  await sendTestEmail(req as any, res);
});

router.post('/invite', authMiddleware, async (req, res) => {
  await authController.inviteFamilyMember(req as any, res);
});

router.post('/accept-invite', async (req, res) => {
  await authController.acceptInvite(req as any, res);
});

export default router;

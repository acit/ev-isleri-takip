import express from 'express';
import { authMiddleware } from '../middleware/auth';
import * as messagesController from '../controllers/messages';

const router = express.Router();

router.use(authMiddleware);

router.post('/', async (req, res) => {
  await messagesController.sendMessage(req as any, res);
});

router.get('/', async (req, res) => {
  await messagesController.getMessages(req as any, res);
});

router.get('/family-members', async (req, res) => {
  await messagesController.getFamilyMembers(req as any, res);
});

router.get('/unread-count', async (req, res) => {
  await messagesController.getUnreadCount(req as any, res);
});

router.patch('/:messageId/read', async (req, res) => {
  await messagesController.markAsRead(req as any, res);
});

export default router;